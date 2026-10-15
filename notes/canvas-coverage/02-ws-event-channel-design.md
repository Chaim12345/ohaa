<!-- tm_board_write · 2026-10-15T14:03:16.610Z · role=architect · session=20261015-170316 -->
# Live agent-event channel (P0) — design

Scope: coverage-audit rows **R1** (live streaming), **R2** (auth/replay/dedupe/backoff), **R3** (event→UI), **R4** (WS send — decision only), **R7** (scrollback), **R8** (transcript render model).
Reference implementation: `/opt/openhands` (@openhands/agent-canvas). Ours: `/tmp/opencode/ohaa-kmp`.
All line references below were read from the actual sources in this run — none are quoted from the prior research report unverified.

---

## 1. Overview

Our KMP web client has no live channel: `WebAppState.openConversation()` → `refreshEvents()` → one REST `events/search` call, and again after every `sendMessage` (`web/src/wasmJsMain/kotlin/com/example/web/WebAppState.kt:240-262`). Canvas instead opens a **per-conversation WebSocket to the conversation's runtime host** (`ws|wss://{host}{pathPrefix}/sockets/events/{id}`), authenticates with a **first-frame** `{"type":"auth","session_api_key":…}`, subscribes with `resend_mode=since&after_timestamp=<newest REST event ts>`, and then applies three classes of local effect: transcript append (deduped by `event.id`), `execution_status`/metrics state, and transient side effects (terminal, browser, error banner).

Design in one paragraph: add an **expect/actual socket client in `:shared`** (wasmJs actual over the browser's native `WebSocket`; no new browser dependency), drive it from a **`ConversationEventStream` state machine** that owns the URL build, 10 s handshake watchdog, 1 s→30 s jittered backoff, and the replay anchor, and connect it through **a new WebSocket proxy route in `:server` (`/cloud-ws/{conversationId}`)** that resolves `conversation_url`+`session_api_key` server-side from the existing cloud key and pumps frames verbatim. The browser talks to exactly one origin, as it already does for REST (`Main.kt:58-62`, `Server.kt:32-44`), and no session key ever enters the bundle. Effects shipped now: transcript (deduped, timestamp-sorted), `execution_status` → conversation status, connection/live indicator + error banner. Terminal, browser screenshots, metrics, deltas and the planner socket are explicitly deferred (they need event shapes we have not yet observed on the wire).

---

## 2. Components

| # | File (new/modified) | Responsibility |
|---|---|---|
| C1 | `shared/src/commonMain/kotlin/com/example/data/socket/EventSocket.kt` **(new)** | `interface EventSocket` + `expect fun openEventSocket(url, handshakeTimeoutMs)`. Pure transport abstraction; no protocol knowledge. |
| C2 | `shared/src/wasmJsMain/kotlin/com/example/data/socket/EventSocket.wasmJs.kt` **(new)** | Actual: browser `WebSocket` (native API). Owns `readyState`→`StateFlow`, 10 s CONNECTING watchdog, close-code capture. |
| C3 | `shared/src/commonMain/kotlin/com/example/data/socket/ConversationEventStream.kt` **(new)** | Protocol state machine: URL/query build, connect gating, auth (no-op client-side), backoff+jitter, replay anchor recompute per reconnect, frame parse, dedupe consult, `LiveConnection` state. |
| C4 | `shared/src/commonMain/kotlin/com/example/data/socket/EventStoreView.kt` **(new)** | Narrow interface the stream writes through: `newestTimestamp()`, `contains(id)`, `add(AgentEvent)`, `setExecutionStatus(String)`. One dedupe authority, mirroring canvas's store (`use-event-store.ts:57,100`). |
| C5 | `web/src/wasmJsMain/kotlin/com/example/web/WebAppState.kt` **(modify)** | Implements C4 over its existing `events: List<AgentEvent>`; adds `seenEventIds: MutableSet<String>`, `executionStatus`, `liveConnection`; starts/stops C3 in `openConversation`/`closeConversation`. |
| C6 | `server/src/main/kotlin/com/example/server/CloudSocketProxy.kt` **(new)** | Ktor WebSocket route `/cloud-ws/{conversationId}`: Origin check, resolve runtime host + session key, open upstream socket, send upstream auth frame, pump frames both ways. |
| C7 | `server/src/main/kotlin/com/example/server/Server.kt` **(modify)**: `install(WebSockets)` + `route { cloudSocketProxy(...) }` | 4-line wiring; `isLocalhostOrigin` (Server.kt:64-68) reused for the WS Origin gate. |
| C8 | `gradle/libs.versions.toml` **(modify)** | `ktor-server-websockets`, `ktor-client-websockets` (version.ref = existing `ktor = "3.3.1"`). |
| C9 | `shared/src/commonTest/kotlin/com/example/data/socket/ConversationEventStreamTest.kt` **(new)** | Fake `EventSocket`: asserts backoff sequence, anchor switch `all`→`since`, dedupe skip, stop() suppresses reconnect. |

Reused as-is (do **not** duplicate): `EventDto`/`toAgentEvent()` (`OpenHandsApiClient.kt:391`), `OpenHandsApiClient.json` (`:349`), `AppConversationDto.conversationUrl`/`sessionApiKey` (`OpenHandsModels.kt:350-351`), `ConversationRuntimeDto` (`OpenHandsModels.kt:2094-2101`), the transcript renderer in `ConversationScreen.kt`.

---

## 3. Interfaces

```kotlin
// C1 — transport only.
enum class SocketState { CONNECTING, OPEN, CLOSED }
data class CloseInfo(val code: Int, val reason: String)

interface EventSocket {
    val state: StateFlow<SocketState>
    /** Inbound text frames; the flow completes on close. */
    val incoming: Flow<String>
    /** Completes when the handshake settles: null = the 10 s watchdog closed it. */
    suspend fun awaitReady(): CloseInfo?
    fun send(text: String)
    fun close(code: Int = 1000, reason: String = "")
}
expect fun openEventSocket(url: String, handshakeTimeoutMs: Long = 10_000): EventSocket

// C4 — the only place ids are remembered.
interface EventStoreView {
    fun newestTimestamp(): String?              // ISO-8601 of the newest known event, null if none
    fun contains(eventId: String): Boolean      // dedupe authority
    fun add(event: AgentEvent)                  // append + sort by timestamp
    fun setExecutionStatus(status: String)
}

// C3
enum class LiveConnection { IDLE, CONNECTING, LIVE, RECONNECTING, FAILED, GAVE_UP }

class ConversationEventStream(
    private val store: EventStoreView,
    private val connect: (url: String) -> EventSocket = ::openEventSocket,
    private val basePath: String = "/cloud-ws",          // our proxy prefix
    private val backoff: Backoff = Backoff(1_000, 30_000, jitter = 0.3),
) {
    val connection: StateFlow<LiveConnection>
    fun start(conversationId: String)   // no-op until the first REST page has landed
    fun stop()                          // intentional: disables reconnect (parity: use-websocket.ts:164-188)
}
```

```kotlin
// C6 — server route.
// GET /cloud-ws/{conversationId}?resend_mode=since&after_timestamp=<ISO>   (or resend_mode=all)
fun Route.cloudSocketProxy(
    cloud: HttpClient,                 // existing proxy client (Server.kt:71)
    cloudBase: String,                 // upstreamBase (Server.kt:50-51)
    serverKey: String?,                // OPENHANDS_API_KEY (Server.kt:54)
    allowedOrigin: (String) -> Boolean // isLocalhostOrigin (Server.kt:64-68)
)
// close codes: 1008 policy (bad Origin / no credential) · 1011 upstream failure · 1013 upstream busy
```

---

## 4. Data flow

```mermaid
sequenceDiagram
  participant B as Browser (wasmJs)
  participant S as :server /cloud-ws
  participant C as Cloud API (app.all-hands.dev)
  participant R as Runtime Agent Server
  B->>S: (1) REST GET /cloud/api/v1/conversation/{id}/events/search
  Note over B: anchor = newest event timestamp (or null)
  B->>S: (2) WS upgrade /cloud-ws/{id}?resend_mode=since&after_timestamp=<ts>
  S->>S: (3) Origin check (localhost only)
  S->>C: (4) GET /api/v1/app-conversations?ids={id}  (bearer = server key)
  C-->>S: conversation_url + session_api_key
  S->>R: (5) WS upgrade wss://{baseHost}{pathPrefix}/sockets/events/{id}?...
  S->>R: (6) first frame {"type":"auth","session_api_key":<key>}
  R-->>S: event frames (JSON text)
  S-->>B: (7) frames verbatim, same order
  B->>B: (8) parse → contains(id)? → skip : add+sets; execution_status → status
```

Reconnect path: close (or 10 s CONNECTING watchdog) → `LiveConnection.RECONNECTING` → wait `1s·2^(n-1) +` up to 30 % jitter, cap 30 s → **recompute the anchor from `store.newestTimestamp()`** → reconnect with `resend_mode=since` (or `all` when the store is still empty). The upstream replays the backlog; the client drops everything `contains()` already knows — including its non-idempotent effects, which is why the dedupe check must happen *before* any effect is applied (canvas parity: `conversation-websocket-context.tsx:569-581`).

---

## 5. Task breakdown (ordered; each step has its own gate)

| # | Step | Depends on | Verification gate |
|---|---|---|---|
| 1 | **Spike: browser WS in wasmJs.** Compile a throwaway `openEventSocket` using `org.w3c.dom.WebSocket`; if the symbol is missing for `wasmJs`, declare `external class WebSocket` + `js("new WebSocket(url)")` in the actual. | — | `./gradlew :web:compileKotlinWasmJs` green; note in the PR which of the two forms was needed. |
| 2 | **Server proxy.** C8 deps, C6 route, C7 wiring. Do **not** put the route under `/cloud` — the existing `route("/cloud/{proxyPath...}")` catch-all (`Server.kt:118`) would swallow the upgrade as a plain REST request. | — | `./gradlew :server:build` green; browser console on `http://localhost:8080`: `new WebSocket("ws://localhost:8080/cloud-ws/<real-conv-id>?resend_mode=all")` → `onopen` + ≥1 event frame within 30 s; a non-localhost Origin is refused (1008). Screenshot the console. |
| 3 | **Transport + stream.** C1, C2, C3, C4. | 1 | `./gradlew :web:compileKotlinWasmJs` green. |
| 4 | **Stream unit tests.** C9 (fake socket): backoff `1000,2000,4000…30000` + jitter bound, `all`→`since` anchor switch, duplicate id skipped including effects, `stop()` no reconnect, watchdog-close path. | 3 | `./gradlew :shared:jvmTest` green (new tests listed in the log). |
| 5 | **Wire into the UI.** C5 + `ConversationScreen.kt`: live transcript without manual refresh, status chip from `execution_status`, `LIVE/RECONNECTING` indicator, error banner on `onError`-after-first-open (parity: `conversation-websocket-context.tsx:1016-1022`). Gate the first connect on the REST page landing (parity: `:380-403`). | 2, 4 | `:web:wasmJsBrowserDevelopmentRun`; browser evidence: (a) send a message → assistant events appear with **no** `refreshEvents()` call, (b) `execution_status` chip changes while the agent works, (c) restart the server → indicator shows reconnecting then live, transcript has no duplicates. Screenshot + console log. |
| 6 | **Wire-shape check (feeds the LATER list).** Log the first ~20 raw frames (browser console) and diff their field names against `EventDto` (`OpenHandsModels.kt`). | 5 | Written verdict in the PR: frames parse into `EventDto` as-is, or a `RuntimeEventDto` is required (see risk R2). |
| 7 | **Regression sweep.** | 5 | `./gradlew :server:build :shared:jvmTest :web:compileKotlinWasmJs` all green; REST-only path still works with `WEB_SOCKET=off` (or server key absent). |

Out of scope for this plan: R4 (`run:true` send over WS), R7 (scrollback — it is REST, `use-load-older-events.ts:125-161`, and independent), R9/R13/R14/R15 (terminal, planner socket, browser, tasklist), Android actual for C1/C2.

---

## 6. Assumptions

| # | Assumption | Confidence |
|---|---|---|
| A1 | The runtime socket accepts `resend_mode`/`after_timestamp` from any client that authenticates with the session key (it is the same agent-server canvas targets). | High — canvas's URL+params are verified verbatim; the gate is step 2. |
| A2 | The cloud API returns `conversation_url` + `session_api_key` for an id we hold a bearer key for (server-side resolve in step 4 of the sequence). | High — `OpenHandsModels.kt:350-351`, and note `ConversationRuntimeDto` at `:2094-2101` exists as the alternative source. |
| A3 | Vendor WS frames are JSON text (canvas does `JSON.parse(messageEvent.data)`, `:554`), so no binary framing path is needed now; `EventSocket` still surfaces raw text. | High |
| A4 | `ktor-server-websockets` + `ktor-client-websockets` (3.3.1) work on Netty server / CIO client — CIO is already a server dep (`server/build.gradle.kts:24`). | Medium — standard Ktor support, but unbuilt here; step 2 is the proof. |
| A5 | `org.w3c.dom.WebSocket` is available in the wasmJs stdlib for Kotlin 2.4.21. | Medium — step 1 has the `external` fallback. |
| A6 | One upstream socket per open tab is acceptable load (no multiplexing/sharing). | Medium |
| A7 | `conversation_url` is `null` while `sandbox_status == "PAUSED"` (prior report §3, `websocket-provider-wrapper.tsx:96-106`) — I did **not** read that file this run, so the pause guard must be implemented defensively (reconnect with backoff, no crash on a null host). | Low — **verify before relying on it**. |
| A8 | Scrollback (R7) stays REST; no socket-side history API is needed. | High |

---

## 7. Risks & open questions

| ID | Risk / question | Mitigation |
|---|---|---|
| **R1** | **Transport choice is the one reversible decision with the biggest cost asymmetry.** Chosen: **(a) server proxy**. *Why (a) over (b) direct connect:* (i) the app's entire upstream traffic already goes through this one origin by design (`Server.kt:32-44`, `Main.kt:59`), and the existing catch-all cannot serve the runtime host because `upstreamBase` is hardcoded to the cloud host (`Server.kt:50-51`); (ii) with (a) the browser needs **no** credential — the server resolves `session_api_key` from its own key — whereas (b) drags the session key into the bundle and repeats canvas's fragile hostname-swap/path-prefix hacks (`websocket-url.ts:15-22, 37-48`) against a runtime host that may be cluster-private; (iii) direct connect failure (Origin refusal vs DNS vs 1006) is indistinguishable from a flaky link, so its bugs are unreproducible in CI. **Abort criterion:** if step 2's probe fails (no 101, or no frame in 30 s), switch to (b) — it is a change of `basePath` + a 5-line URL builder, because C3 owns the URL and C2 owns the transport. | Cost: ~90 lines + 2 deps. Do not merge (a) and (b) "just in case". |
| **R2** | **Unknown: do WS frames match our `EventDto` field names?** Our `EventDto` is shaped for the *cloud* `events/search` response (`OpenHandsApiClient.kt:155-159`); canvas guards on agent-server v1 event shapes (`:560`). | Step 6 prints the verdict; if it differs, add `RuntimeEventDto` + a second `toAgentEvent` overload rather than editing `EventDto` (which the REST path depends on). |
| **R3** | **Browser-supplied key mode (Settings) cannot send a header on a WS handshake.** `Server.kt:134-144` accepts the cloud key from `X-OpenHands-Key`; a WS upgrade cannot carry it. | Rule for now: the WS route uses only `OPENHANDS_API_KEY`; when it is absent, close **1008** and the client keeps today's REST refresh (no user-visible breakage). LATER: mirror canvas's idiom — accept a client→proxy first frame `{"type":"auth","api_key":…}`, consume it (never forwarded), then send the *upstream* frame with the session key. Never put a key in the WS query string (it lands in `CallLogging`, `Server.kt:73`). |
| **R4** | **SSRF surface.** The proxy resolves the upstream host itself; the browser must never be able to name it. | Route takes only `{conversationId}` + `resend_mode`/`after_timestamp` (whitelist; drop every other query param); upstream host comes from the cloud API response only. |
| **R5** | Backpressure/close-code fidelity in the pump (binary, ping/pong, half-close). | Use raw frames both sides, forward `Ping`/`Pong`/`Close` unchanged, and propagate the upstream close code to the browser; unit-test nothing here, verify in step 5 (c). |
| **R6** | Session-key rotation → upstream auth close; a naive retry loop hammers the backend. | On close 1008/4401 the **server** re-resolves `session_api_key` once per reconnect attempt, and the client's attempt budget/backoff still applies (no server-side retry loop). |
| **R7** | Duplicate/immediate re-render storms: Compose state writes per frame (`events = events + e`) can thrash on token deltas. | NOW: dedupe by id and coalesce UI writes per frame-batch (append batches, sort once). Deltas are LATER (R8) and need a batcher like `mainDeltaBatcherRef` (`:562-567`). |
| **R8** | Paused sandboxes / `conversation_url == null` (A7) would open a socket to a dead host. | Treat a null host as "not live": show the indicator as idle and leave REST refresh in place. |

### [M] → [H] re-verification ledger (prior report §3 claims)

| Prior claim (§3) | Status | Source read this run |
|---|---|---|
| URL `{ws\|wss}://{host}{pathPrefix}/sockets/events/{id}` | **[H] CONFIRMED** | `/opt/openhands/src/utils/websocket-url.ts:109-134` |
| Protocol = wss iff page https OR target https; hostname swap for localhost; pathPrefix = before `/api/conversations` | **[H] CONFIRMED** (the prior report did not mention the protocol rule or the prefix derivation) | `websocket-url.ts:6-29, 37-48, 129-132` |
| Second socket `/sockets/bash-events` | **[H] CONFIRMED** | `websocket-url.ts:89-101` |
| Auth = first frame `{type:"auth",session_api_key}`, sent from `onopen`, skipped when the key is absent | **[H] CONFIRMED** | `websocket-auth.ts:1-17`; `use-websocket.ts:66-74` |
| `resend_mode=since&after_timestamp=<newest REST ts>`, fallback `all` | **[H] CONFIRMED** | `conversation-websocket-context.tsx:1000-1002`, anchor `:371-376` |
| Planner socket uses `resend_all=true` (a *different* param name) | **[H] CONFIRMED** | `conversation-websocket-context.tsx:1035-1037` |
| Backoff 1 s→30 s, +0-30 % jitter, maxAttempts ∞ | **[H] CONFIRMED** (line refs in the report had drifted: it is `:19-20, 126-136`, not `:16-18, 88-145`) | `use-websocket.ts:19-20, 112-136` |
| Handshake watchdog aborts CONNECTING sockets | **[H] CONFIRMED — 10 s, and the *reason* is Chrome's per-host handshake serialisation** | `websocket-handshake.ts:5, 17-25` |
| Dedupe by `event.id` in the store; replay skips non-idempotent effects | **[H] CONFIRMED** (`eventIds` set; deltas excluded from id tracking) | `use-event-store.ts:57, 95-107, 172-179`; `:569-581` |
| Connect gated on the first REST history load | **[H] CONFIRMED**, incl. the reason it is *not* gated on `isFetching` | `conversation-websocket-context.tsx:380-403` |
| Scrollback is REST `events/search` with `timestampLt`, `TIMESTAMP_DESC`, reverse-merge | **[H] CONFIRMED** | `use-load-older-events.ts:125-161` |
| `conversation_url` is `null` while `sandbox_status == "PAUSED"` | **NOT RE-VERIFIED** — that file was out of this run's read set (A7) | — |
| `proxy.ts` two-host model (`authMode: bearer \| session-api-key`, `hostOverride`) | **[H] CONFIRMED** — matches our planned split: cloud bearer for resolve, session key for the socket | `src/api/cloud/proxy.ts:5-38` |

Ambiguity left open: the WS frame schema (R2) and whether the runtime validates `Origin` for a `http://localhost` page (only matters for the (b) fallback). Both are closed by steps 2/6, not by reading more source.

### Event → UI mapping (verified, canvas side)

| Frame / event | Effect | Canvas source | Ours now |
|---|---|---|---|
| any non-delta agent event | transcript append (store dedupes by id, re-sorts by ISO ts) | `conversation-websocket-context.tsx:560-581`; `use-event-store.ts:100-128` | **NOW** (add + sort) |
| streaming delta event | buffered, flushed before the next non-delta, merged per sender | `:562-567`; `handle-event-for-ui.ts:20-37` | LATER (R8) |
| `ConversationStateUpdateEvent` (full state / agent-status) | `execution_status` → status chip | `:652-664` | **NOW** |
| `ConversationStateUpdateEvent` (stats) | metrics panel | `:665-667` | LATER |
| `ConversationStateUpdateEvent` (goal) | goal status store | `:671-673` | LATER (R13) |
| displayable error event | error banner (`detail`, `code`, `classification`); agent errors stay inline | `:585-621` | **NOW** (banner text only) |
| user message echo | clears optimistic pending message + draft | `:628-637` | LATER (we have no optimistic send) |
| `ActionEvent` | query-cache invalidation | `:640-648` | not applicable (no query cache) |
| `ExecuteBashAction` / `ExecuteBashObservation` | terminal `appendInput`/`appendOutput` | `:677-689` | LATER (R9) |
| `BrowserObservation` | browser-store screenshot | `:692-694` | LATER (R14) |
| socket open / error | connection state; error banner **only after a prior successful connect** | `:1008-1022` | **NOW** |
