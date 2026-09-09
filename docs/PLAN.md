# Canvas V2 — Redesign Plan & Progress

Goal: rebuild the OpenHands Cloud Android client as a **pocket Agent Canvas** — the
same concepts as the [Agent Canvas](https://docs.openhands.dev/openhands/usage/agent-canvas/overview)
browser client (conversations, workspaces/repositories, live status, event timeline),
designed for one-handed mobile use.

Branch: `redesign/canvas-v2`

## 1. Problems with V1

1. **Untyped API layer.** `createConversation`, `listConversations`, `getEvents` all returned
   `ResponseBody`, then hand-rolled JSON sniffing in `ApiClient.parse*`. Fragile and untestable.
2. **Wrong conversation model.** No `start-tasks` polling, no `sandbox_status` /
   `execution_status`; the UI showed made-up `WORKING → READY` transitions.
3. **Silent fake agent.** On any API failure the repo inserted a scripted "agent" transcript
   (fake bash, fake diffs) and marked tasks READY. Misleading and unfixable in production.
4. **Wrong navigation.** Conversation detail rendered *inside* the bottom-nav box, so the nav
   bar never hid, detail state reset per tab, and there was no proper back stack.
5. **Stale status.** Only refreshed on manual pull; no polling; statuses never converged.

## 2. Target architecture (layered, offline-first)

```
ui/            Compose screens + components (Material 3, dark developer theme)
ui/navigation/ Typed routes, NavHost, bottom bar, detail pushed over tabs
ui/viewmodel/  OpenHandsViewModel (state machine + lifecycle-aware polling)
data/repository/OpenHandsRepository   Result-based; Room = source of truth
data/api/      Retrofit service (typed DTOs), ApiClient, settings store
data/local/    Room v2: conversations, events, templates
docs/          API_SPEC.md (contract) · PLAN.md (this file)
```

Data flow: `API → repository (merge) → Room → Flow → ViewModel → Compose`.
Network failures degrade to cache + banner; **never** to fabricated data.

## 3. UX design (canvas-inspired)

- **Dashboard** — connection card, live stat cards (active / done / spend), "Continue work"
  recents, template gallery.
- **Conversations** — searchable, filterable list of agent conversations with status chips and
  swipe-free delete via detail overflow.
- **New Task** — prompt editor, repository picker field, template quick-fill, launch progress
  (start-task statuses streamed into the UI while polling §2.3).
- **Conversation Detail** — full-screen: header with status + deep-link button, event timeline
  (user messages, agent reasoning, bash blocks, edit diffs), follow-up composer. Pushed over
  tabs with back stack; polls per API_SPEC §4.
- **Settings** — API key, base URL, health check with explicit status, demo-data reset.

Theme: dark-first developer palette (deep slate `#0D1117`, cyan accent, semantic status
colors), monospace for commands/diffs, Material 3 chips/cards, edge-to-edge.

## 4. Status mapping

Single `ConversationUiStatus` derived from `sandbox_status` + `execution_status` + startup
statuses (see API_SPEC §3). Drives chips, polling cadence, and detail header actions.

## 5. Test plan

| Layer | Tool | Coverage |
|---|---|---|
| Mappers / status logic | JUnit | every sandbox/execution/startup combination |
| DTO parsing | JUnit + Moshi | real-shaped cloud JSON (list, start-task, events page) |
| Repository | MockWebServer + Room in-memory | success, 401, network-down → cache, demo mode |
| ViewModel | kotlinx-coroutines-test | launch → poll → refresh, messaging, demo seeding |

Run: `./gradlew testDebugUnitTest` (CI uses the wrapper — no local JDK needed).

## 6. Build plan

- `./gradlew assembleDebug` → APK (existing `build_apk.sh` keeps working; wrapper added).
- CI (`.github/workflows/build-apk.yml`): setup-java 17 + gradle wrapper validation, run
  `./gradlew assembleDebug --no-daemon`, upload APK artifact.
- `.env.example` retains `OPENHANDS_API_KEY` for the Secrets Gradle plugin; users can also
  set the key at runtime in Settings.

## 7. Progress log

- [x] Branch `redesign/canvas-v2`
- [x] API spec (`docs/API_SPEC.md`)
- [x] Plan (this file)
- [x] Data layer rewrite (typed DTOs, Retrofit, settings store, Room v2)
- [x] Repository rewrite (offline-first, no fake agent, demo mode)
- [x] Navigation + ViewModel rewrite (routes, pushed detail, polling)
- [x] Theme + components redesign
- [x] Screen redesigns (Dashboard, Conversations, New Task, Detail, Settings)
- [x] Unit tests
- [x] CI + Gradle wrapper
- [x] Verification pass (cross-checked screen signatures vs MainActivity, restored toolchain to AGP 9.1.1/Kotlin 2.2.10, minSdk-safe ISO parser replacing java.time, cached Retrofit client, start-task id mapping, null-body guard; tests added: StatusMapper, DTO parsing, repository (MockWebServer+Room), timestamp parser; local Gradle runs impossible (no JDK in sandbox) — `testDebugUnitTest` + `assembleDebug` run in CI on push)
- [x] UI audit + native adaptation (system back pops the pushed detail route with predictive-back opt-in, theme follows system dark/light with a completed light palette and scheme-aware surfaces, native splash screen via `core-splashscreen` + `installSplashScreen()`, smart auto-scroll that never yanks readers out of history, IME padding on New Task, demo-mode launch deep-links to Settings, startup refresh so the dashboard shows demo/offline state immediately; regression tests cover launch gating, key visibility, and destructive-action confirmations)
- [x] Mobile-agent inspiration pass + APK CI hardening (researched Replit/Copilot/Cursor mobile patterns → pull-to-refresh on Dashboard + Conversations via M3 `PullToRefreshBox`, relative "5m ago"/"2d ago" timestamps on conversation rows (`RelativeTime`, minSdk-safe, unit-tested), search field IME Search action + clear button; **fixed `assembleDebug` CI failure**: removed the debug signing config that pointed at a gitignored, never-existing `debug.keystore` so AGP uses its auto-generated debug key; verified wrapper exec bit, branch triggers, and that no gitignore rule hides the wrapper binaries)
- [x] CI compile-error fix (first CI run caught 5 Kotlin errors: `searchConversationsRemote` returned Room entities instead of domain models, a glued `}` + `AnimatedVisibility` line broke the composable parse in `EventItemView`, and `FilterChip`s were emitted from a non-composable `forEach` inside `LazyRow` — replaced with `items()`; the `ksp.com.intellij` AWT NPE in the log is a known harmless headless-KSP thread crash)
