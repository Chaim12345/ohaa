# Handoff — Compose Multiplatform migration + live transcript fixes

Branch: `redesign/canvas-v2` · Repo: `Chaim12345/ohaa`
Date: 2026-09-10

## What this change set is

The native Android client is being migrated to **Compose Multiplatform** with a
**Kotlin/Wasm** target so the UI can be exercised in a desktop browser
(`wasmJsBrowserDevelopmentRun`). Three Gradle modules were added alongside the
existing `:app`:

| Module    | Purpose |
|-----------|---------|
| `:shared` | `commonMain` — DTOs, Ktor `OpenHandsApiClient`, Compose screens/components, theme. Shared by `:app` and `:web`. |
| `:web`    | `wasmJsMain` — browser entry point (`Main.kt`), `WebAppState`, served by `:server`. |
| `:server` | Ktor/JVM host. Serves the Wasm distribution at `/` and reverse-proxies `/cloud/{path...}` to `app.all-hands.dev` so the browser build has same-origin API access (no CORS, no key in the client). |

Run the browser build:

```bash
./gradlew :web:wasmJsBrowserDistribution
./gradlew :server:installDist && server/build/install/server/bin/server
# then open http://localhost:12000
```

## Bugs found and fixed during browser UI testing

### 1. Transcript paginated from the oldest event (fixed)

`OpenHandsApiClient.searchEvents()` called `GET /events/search?limit=100` with the
server default sort. The live conversation had **5,089 events**, so the client only
ever fetched the first 100 — the Chat/Terminal tabs showed the *start* of the
conversation and never any recent activity. That is what looked like "chat only
shows terminal": the visible slice happened to be almost all tool events.

Fix: request `sort_order=TIMESTAMP_DESC` and reverse the page so the newest
`limit` events are returned in chronological order.

### 2. `users/me` failed to decode → account/Settings empty (fixed)

`UserInfoDto` typed two fields as scalars that the live API returns as objects:

- `secrets_store` — declared `String?`, actually `{"provider_tokens":{}}`
- `agent_settings` — declared `Map<String,String>?`, actually a nested object
  (`{"schema_version":6,"agent":"CodeActAgent","llm":{...}}`)

Any mismatch fails the whole `Json` decode, so `getUserInfo()` returned an error
and the account never populated. Both fields are now `JsonElement?`, matching the
open schema in `openapi.json`. (This is the same class of bug already documented
in `AGENTS.md` for `AppConversationDto.launched_agent_profile`.)

## Known remaining issues

- **`agent_settings` shape is not modelled.** The field is now permissive
  (`JsonElement?`) so decoding succeeds, but nothing reads the nested agent/LLM
  configuration yet. If Settings should display it, add a typed DTO.
- **Only the newest 100 events are shown.** Pagination is not implemented; a
  long conversation can't be scrolled back beyond the newest page. `PageDto`
  carries `next_page_id`, so the hook is there.
- **Overflow-menu clicks are flaky under automation.** The whole app is one
  Compose `canvas`, so element indices shift between snapshots; retry after a
  reload. This is a test-harness limitation, not an app bug.
- Assistant chat messages surface as transcript blocks (Terminal/Generic) rather
  than distinct assistant bubbles, because the agent's narration arrives as
  `ActionEvent`s whose `tool_name` is `terminal`/`browser_*`. Classification is
  driven by `tool_name`, so the narrative text is not distinguishable from the
  tool call it precedes.

## Verification

- `./gradlew :web:wasmJsBrowserDistribution` — BUILD SUCCESSFUL.
- Live `users/me` payload decoded against the updated DTOs (field types compared
  against `https://app.all-hands.dev/openapi.json`).
- Browser: opening the `Clone Chaim12345/ohaa…` conversation now renders the
  newest events, including the user's latest message, instead of the first page.
