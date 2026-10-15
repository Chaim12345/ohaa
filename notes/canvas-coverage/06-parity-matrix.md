# 06 — Parity matrix: does ohaa cover everything the OpenHands Canvas does natively?

| | |
|---|---|
| **Date** | 2026-10-15 |
| **Question** | Does the ohaa app cover everything the official OpenHands Canvas does natively — all API and all UI? |
| **Canvas reference** | `/opt/openhands` @ `38fe0c0` (fetch 2026-10-15), package `@openhands/agent-canvas` v1.27.1 (`package.json:2-3`) |
| **Feature-map baseline** | `main@8b9823a…` (2026-10-14) — F01–F28, 778 sub-feature rows, extracted to `00-feature-map-subfeatures.md` |
| **API inventory** | `04-ts-api-inventory.md` — 97 cloud rows + 20 runtime capability families |
| **UI/UX model** | `05-ts-ui-ux-model.md` — 30 routes, 9 new screens, 8-tab workbench |
| **WS channel** | `02-ws-event-channel-design.md` — designed, **not implemented** |
| **App under test** | `/tmp/opencode/ohaa-kmp` @ `71d9bb4` (branch `redesign/canvas-v2-kmp`) |
| **Canonical API surface** | `shared/src/commonMain/kotlin/com/example/data/api/OpenHandsApiClient.kt` (Ktor, 29 public methods — what BOTH Android and the wasm web build render) |
| **Legacy Android surface** | `app/src/main/java/com/example/data/api/OpenHandsApiService.kt` (Retrofit, 211 ops) + `app/src/main/java/com/example/data/api/AgentServerApiService.kt` (23 ops) |
| **Dev proxy** | `server/src/main/kotlin/com/example/server/Server.kt` — `/cloud/{path…}` + `POST /api/cloud-proxy` runtime tunnel |
| **Method** | Static inspection only. Every row below carries `path:line` or an explicit "not found in code". Sub-feature counts are grounded: a row is credited only when its observable behaviour is reachable in the shipped UI. No live API calls, no device runs. |

---

## 2. Scoreboard

### 2.1 UI (F01–F28, 778 sub-feature rows)

| Metric | Count | Note |
|---|---|---|
| Families at **functional parity** (`covered`) | **0 / 28** | no F-family reaches parity |
| Families **partially** implemented | **14 / 28** | F01, F02, F03, F04, F05, F06, F07, F08, F09, F10, F13, F14, F25, F27 |
| Families **absent** (`missing`) | **14 / 28** | F11, F12, F15, F16, F17, F18, F19, F20, F21, F22, F23, F24, F26, F28 |
| Sub-features **implemented (any degree)** | **≈ 42 / 778 (5.4 %)** | see per-family counts in §3 |
| Sub-features **absent** | **≈ 736 / 778** | |

### 2.2 API

| Metric | Canvas | Android (Retrofit) | Web/shared (Ktor) |
|---|---|---|---|
| Cloud rows covered | 97 | **59 / 97** (57 matched + 2 near-match) | **20 / 97** |
| Cloud rows missing | — | **38** | **77** |
| Runtime capability families (§2.2) | 20 | **0 / 20** of the §4.3 list; ConversationClient surface ≈ 85 % declared but **0 % wired to UI** | **1 / 20** (respond_to_confirmation only) |
| Live event channel | WS `/sockets/session/{id}` | **absent** | **absent** |
| Automation service | 28 rows | **absent** | **absent** |

### 2.3 Verdict

> **No.** The app covers the *core* cloud conversation lifecycle (list → start → poll → read transcript → send follow-up → git changes → files → skills → secrets → profiles) on **Android only**, and even there without live streaming. The **web build renders a strictly smaller API surface than the Android app** (20 vs 59 cloud rows) and has several dead UI controls. **Everything canvas groups as "Customize" (MCP/skills/plugins/apps), "Automate" (the whole automations subsystem, 5 F-families), Model Router, provider connections, agent-profile management, condenser/verification/application settings, backends-and-cloud, and the 8-tab conversation workbench is absent.** The single largest structural blocker is the unimplemented WebSocket live-event channel (`02-ws-event-channel-design.md`), which is why the app's chat is poll-based (Android) or refresh-only (web) against canvas's live stream.

---

## 3. UI matrix — one row per F-family

`where` = the code that implements (part of) the family. Counts are sub-features credited.

| F-id | Family (rows) | Canvas behaviour (1 line) | Status | Where in our code | What is missing |
|---|---|---|---|---|---|
| F01 | First run & sign-in (22) | Onboarding modal, backend step, agent + LLM setup, say-hello, API-key gate | `partial 1/22` | API-key + base-URL form and Test/Save: `shared/src/commonMain/kotlin/com/example/ui/screens/SettingsScreen.kt:147-207`; key persisted to `localStorage`: `web/src/wasmJsMain/kotlin/com/example/web/BrowserSession.kt:18-33` | No first-run gate (no `onboarded` flag anywhere), no onboarding modal/steps, no telemetry-consent modal, no backend step, no agent picker, no LLM setup, no say-hello, no `?previewOnboardingStep`, no route error boundary, no `<title>`, no bootstrap spinner, no locked-cloud, no cookie-auth redirect. Only a full-screen gate equivalent is the "Not connected / Tap to add an API key" banner: `shared/…/DashboardScreen.kt:213-232` |
| F02 | App shell (28) | Sidebar rail, pin-as-home, collapse, command menu, checklist, alert banner | `partial 2/28` | 4-tab bottom navigation: `web/…/Main.kt:87-108`, `app/src/main/java/com/example/MainActivity.kt:112-131`; error surface: web banner `web/…/Main.kt:249-260`, Android snackbar `app/…/MainActivity.kt:95-103,111` | No sidebar rail, no pin-as-home, no collapse/60 px rail, no ⌘K command menu, no Getting-started checklist, no super-admin setup guide/tour, no update tile, no maintenance/faulty-model alert banner, no settings-404 modal, no per-banner dismiss |
| F03 | Home / launcher (42) | `/conversations` launcher, Code↔Automate toggle, workspace + plugin pickers, automations rail + pinned grid | `partial 3/42` | Dashboard front door + "New task" entry: `shared/…/DashboardScreen.kt:60-174`; new-task composer: `shared/…/NewTaskScreen.kt:77-260`; create failure surfaces via `launchStage`/`launchStatusText`: `web/…/WebAppState.kt:224-228` | No `/conversations` route launcher, no Code/Automate mode toggle, no `home-composer-actions`, no workspace picker / folder browser / manage workspaces, no repository dialog (we have a repo text field + browse button: `NewTaskScreen.kt:172-208`), no plugin picker, no recommended-automations rail, no automations list/pins, no LLM-not-configured banner, no `/model` command |
| F04 | Conversation list (41) | Sidebar panel: folders, pins, tags, archive, presets, load-more, kebab | `partial 3/41` | Rows with status dot + title + relative time: `shared/…/DashboardScreen.kt:261-316`; list screen with search + status filter chips: `shared/…/ConversationsScreen.kt:55-184` | No folders/grouping, no pinned section, no tag facets or tag editing, no archive/show-archived, no view menu/presets, no sort, no hide-older, no load-more (single 50-row page: `OpenHandsApiClient.kt:133-140`), no ⋮ kebab (rename/stop/download/archive/delete), no delete-all, no cloud repo picker, no start-task cards. (Our free-text search box is *extra* — canvas has no visible search field, `05-ts-ui-ux-model.md` §5.2) |
| F05 | Composer (25) | Enter/Shift+Enter, growth + drag-resize, `+` menu, attachments, dictation, slash commands, plan mode | `partial 3/25` | Send button gated on non-blank: `shared/…/ConversationScreen.kt:489-525`; model pill → dialog: `shared/…/ConversationScreen.kt:260-267`, `shared/src/commonMain/kotlin/com/example/ui/components/AgentProfileDialog.kt:44-76` | No draft persistence (composer is `remember { mutableStateOf("") }`, `ConversationScreen.kt:434`), no field growth/drag grip, no `+` menu, no agent-profile switch, no file/image attachment, no size limit, no paste handling, no dictation, no per-conversation LLM-profile switching, no context-window meter, no stop/resume, no queued messages, no slash menu / `/model` / `/btw` / `/goal` / `/plan` / `/code` |
| F06 | Agent activity (35) | Live stream, message bubbles, tool visualizers, event groups, confirmations, retries | `partial 5/35` | Transcript renderer by event class (user / message markdown / thought / terminal / edit / error): `shared/src/commonMain/kotlin/com/example/ui/components/TranscriptRenderers.kt:86-120,153-155`; per-block copy: `TranscriptRenderers.kt:384-411`; accept/reject card (shared only): `shared/…/ConversationScreen.kt:469-479,530-576` | **No live stream at all** — refresh is a manual REST call (`web/…/WebAppState.kt:269-276`) or a 10 s poll (`app/…/OpenHandsViewModel.kt:407-415`). No suggestion chips, no clipping/View More, no timestamps, no pending-message states or retry, no clock-skew handling, no event grouping, no tool visualizers, no Markdown-file preview card, no task list, no older-history pagination (`docs/HANDOFF.md:58-60`), no scroll-to-bottom, no live-activity chip, no Starting pill, no reconnect, no error banner with retry, no confirmation-mode lock chip or shortcuts, no branch-from-here, no image thumbnails/lightbox, no skill-install banner, no critic block, no hook rows, no corrective nudge. Known defect: assistant text renders as tool blocks, not bubbles (`docs/HANDOFF.md:64-68`) |
| F07 | Conversation page (25) | Header, status menu, `…` menu, hooks/skills/agent-tools modals, export, git actions | `partial 5/25` | Header with title/repo/profile/refresh/overflow: `shared/…/ConversationScreen.kt:276-400`; rename dialog `:853-885`; delete confirm `:243-258`; sandbox pause/resume/delete `:372-390`; skills via overflow `:362-366` | No panel route, no status-dot menu with Stop/Start Runtime, no menu items gated on state, no hooks modal, no agent-tools/metadata modal, no Markdown/HTML transcript export (only a JSON trajectory share, `app/…/OpenHandsViewModel.kt:603-612`), no usage-panel entry, no git-actions menu (Commits/Pull/Push/PR/new branch), no git control bar, no Share publicly, no Connect Repo, no `task-<uuid>` URLs, no paused-sandbox resume |
| F08 | Workspace files & changes (34) | Drawer with Files/Commits/Planner/Terminal/Browser/Usage tabs, tree, diffs, resize | `partial 5/34` | 5-tab strip: `shared/…/ConversationScreen.kt:91-97,177-195`; Files list + read `:581-630`; uncommitted changes + `+/-` rows `:634-720`; terminal output `:750-788`; usage `:792-836`; lazy per-tab load `:186-190` | Drawer is **full-screen tab replacement**, not a resizable chat+drawer split. Missing: Commits tab (we only show uncommitted changes), Planner, Browser, Task list tabs; file tree (flat list only, `FilesTab` has no folders), open-file strip, Rich/Plain toggle, file-discovery dialog, refresh/auto-refresh, workspace-path header + copy, path links from chat, diff view-mode buttons, deleted-file notice, commits cap, tree/drawer resize, VSCode link, archived-disabled state, panel route |
| F09 | Settings shell (26) | Settings layout with nav, landmarks, phone hub, version card | `partial 2/26` | Single scrolling settings screen with cards: `shared/…/SettingsScreen.kt:124-395`; reachable from the nav bar: `web/…/Main.kt:98-100` | Not a shell: no nav rail, no index/redirect, no `aria-current`, no `main` landmark, no phone hub, no tablet selector, no Back chevron, no version card, no unknown-subpath 404, no synced-from-backend footer |
| F10 | LLM profiles (34) | Profile library with create/edit, validation, model picker, connections | `partial 3/34` | List with ACTIVE badge: `shared/…/SettingsScreen.kt:288-326,482-553`; activate/rename/delete dialogs `:429-468,591-626` | Read-mostly. No create/edit (no save-profile call exists at all), no duplicate, no set-default semantics (we POST activate), no name validation, no validation call, no Basic/Advanced tabs, no provider/model combobox, no custom model + base URL, no connection link, no subscription auth, no add-from-connections, no cloud read-only mode. **Web build cannot even mutate** — see §6 D2 |
| F11 | Provider connections (21) | Shared-API-key connections with bulk-add, edit, delete | `missing 0/21` | not found in code (no screen, no client method, no DTO use) | Entire family |
| F12 | Model router / meta-profiles (25) | Meta-profile routers that route tasks to LLM profiles | `missing 0/25` | not found in code | Entire family (no `meta-profiles` route in either API surface) |
| F13 | Agent profiles (22) | Library of named agent setups (OpenHands + ACP presets) | `partial 1/22` | A local `AgentProfile` model + dialog: `shared/src/commonMain/kotlin/com/example/ui/components/AgentProfileDialog.kt:44-76`, `shared/…/ConversationScreen.kt:260-267` | The cloud agent-profile CRUD (`api/agent-profiles/*`, `OpenHandsApiService.kt:573-616`) has no UI. No list, no create/edit, no name validation, no OpenHands options (system prompt, parallel tool calls, tools), no MCP/secrets scopes, no ACP form/presets/credentials/discovery, no row-menu keyboard support |
| F14 | Secrets (12) | Secrets page with add/edit/delete, validation, agent request flow | `partial 4/12` | List + description: `shared/…/SettingsScreen.kt:328-361,556-589`; add dialog with name guard: `:629-726`; delete confirm `:455-468` | No edit/rename form, no `^[a-zA-Z][a-zA-Z0-9_]{0,63}$` rule (our guard is `[A-Za-z0-9_]` only, `SettingsScreen.kt:639-641`), no `Secret already exists` message, no Back-replaces-list form, no agent `request_secret` modal, no phone check |
| F15 | Agent behaviour settings (23) | Condenser, Agent Context, Verification pages | `missing 0/23` | not found in code | Entire family. The Retrofit service already declares the schemas (`OpenHandsApiService.kt:355-358`) but nothing calls them |
| F16 | Application settings (20) | Language, theme, analytics, sound, title model, git identity | `missing 0/20` | not found in code | Entire family |
| F17 | MCP servers (31) | Custom + library MCP install, OAuth, health, save-as-secret | `missing 0/31` | not found in code | Entire family |
| F18 | Skills (22) | Skills library with facets, toggles, detail modal | `missing 0/22` | Only "skills loaded in a conversation" is listed: `shared/…/ConversationScreen.kt:362-366,818-834` | No Skills page, no search/facets, no enable/disable, no detail modal, no `Use skill`, no project-skill discovery, no install banner |
| F19 | Plugins (25) | Plugin marketplace: install, enable, update, `/launch` deeplink | `missing 0/25` | not found in code | Entire family |
| F20 | Canvas apps / extensions (24) | Install/enable third-party canvas apps, rail entries, page hosting | `missing 0/24` | not found in code | Entire family |
| F21 | Automations dashboard (35) | Dashboard with tiles, grid/list, filters, run-now, drafts | `missing 0/35` | not found in code | Entire family |
| F22 | Automation creation (29) | Templates, setup dialog, import | `missing 0/29` | not found in code | Entire family |
| F23 | Automation detail (41) | Detail page, activity log, run logs, edit page | `missing 0/41` | not found in code | Entire family |
| F24 | Automation git sync (26) | Git-sync config page, cycles, encryption | `missing 0/26` | not found in code | Entire family |
| F25 | Backends & cloud (33) | Backend registry, selector, device flow, org rows, DO providers | `partial 2/33` | One fixed cloud host (`OPENHANDS_BASE_URL` default `https://app.all-hands.dev`, `Server.kt:56-57`); org + role surfaced in the account card: `shared/…/SettingsScreen.kt:239-266` | No backend registry/selector, no Add/Edit/Manage/Remove backends, no health probes, no recovery gate, no Cloud device flow (the Kotlin service has no `oauth/device/*` call), no per-tab backends, no org switching, no `?backend=`/`?org=` pinning, no sandbox-state chat variants, no org-suspension screen, no DigitalOcean providers bridge |
| F26 | Runtime variants (34) | Canvas CLI, `--public`, Docker, Helm, Electron desktop | `missing 0/34` | not applicable by design — we ship a KMP client + our own dev proxy (`server/…/Server.kt`), not the canvas launcher | Entire family. Explicit product decision required: canvas's launcher/Docker/desktop surface is a *host deployment*, not a client feature |
| F27 | Workspace tools (30) | Terminal, browser, planner, task list, usage, overview column | `partial 3/30` | Terminal tab: `shared/…/ConversationScreen.kt:750-788`; usage tab `:792-836`; files tab `:581-630` | No browser tab, no planner tab, no task list, no overview column/peek/pins, no context-window meter ring, no "Compact context" (the runtime op exists in Retrofit only, `AgentServerApiService.kt:124-127`), no provider-balance card, no `canvas_ui_control` open_tab/navigate_to_file, no child-conversation launch, no plugins modal |
| F28 | Automation setup page (13) | Setup form with drafts, test runs, kind toggle | `missing 0/13` | not found in code | Entire family |

---

## 4. API matrix

### 4.0 First, a **drift report** against `04-ts-api-inventory.md` §4

| # | Claim in 04 | Re-verified this run | Verdict |
|---|---|---|---|
| 1 | "38 cloud rows missing in Kotlin" | Re-checked all 38 against `OpenHandsApiService.kt` by grep for `automation`, `mcp`, `meta-profiles`, `setup-state`, `organizations/{org_id}/profiles`. **Zero matches** for automation / MCP / meta-profiles / setup-state; the org-profile route exists only in the `{name}` form (`OpenHandsApiService.kt:937`). | **CONFIRMED for the Android app** |
| 2 | "Everything else canvas calls matched (normalized verb+path)" | True **only for the Android Retrofit service**. The inventory never compared `shared/…/OpenHandsApiClient.kt`. | **DRIFT — see 4.1** |
| 3 | Kotlin runtime coverage "23 ops" | Confirmed: `AgentServerApiService.kt` declares 23 endpoints, all conversations/events/goal/ask_agent/switch_llm/condense/fork/vscode/workspace-file. | CONFIRMED |
| 4 | 2 near-matches to verify | Confirmed both: `GET api/v1/settings/profiles/{name}` has **no** query params, so `exposeSecrets=encrypted` cannot be sent (`OpenHandsApiService.kt:363-364`); and there is **no** `POST api/v1/settings/profiles` without `{name}` (`:366`). | CONFIRMED (both are real gaps) |

**The material drift:** the web build's API surface is a **strict subset** of the Android app's. 04's "matched" list is an Android-only claim.

### 4.1 Cloud rows by target

| Target | Matched | Missing | Notes |
|---|---|---|---|
| **Android** `app/…/OpenHandsApiService.kt` (211 ops) | 59 / 97 | 38 | the 38 in §4.2; plus 2 near-matches in §4.3. Matched set includes app-conversations CRUD/search/download/file/files/start-tasks, sandbox pause/resume/list, secrets CRUD (incl. `PUT` at `:329`), settings + schemas + web-client config, git repositories/branches, config models/providers, skills search + conversation skills, shared conversations/events, organizations/keys/me/members, agent profiles (6), LLM profiles settings-route (5/6), provider connections (4), switch_profile/switch_acp_model, events search, git changes/diff |
| **Web / shared** `shared/…/OpenHandsApiClient.kt` (29 methods) | **20 / 97** | **77** | matched rows: 1, 2, 3, 5, 7, 8, 9, 10, 11, 12, 14, 15, 17, 23, 25, 32, 46 (settings-route form only), 63, 64, 65. **Not present at all:** app-conversations `{id}/download`; `sandboxes?ids=`; `PUT secrets/{name}`; `GET/POST settings`; both settings schemas; `web-client/config`; `git/installations/search`; `config/models|providers/search`; `skills/search`; shared conversations/events; organizations; `keys/current`; agent profiles (6); LLM profile get/save/delete/rename/activate (5); meta-profiles (5); provider connections (4); switch_profile / switch_acp_model; cloud MCP (3); setup-state; main-app auth (2); device flow (2); **all 28 automation rows** |

Extra Kotlin-only ops (informational, canvas does not call them): `POST api/v1/app-conversations/stream-start` (`OpenHandsApiService.kt:82`), `POST api/v1/app-conversations/{id}/send-message` (`:137`), `GET api/v1/git/suggested-tasks/search` (`:295`), `GET api/v1/app-conversations/{id}/hooks` (`:188`), `POST api/complete_onboarding` (`:648`, **dead surface — no caller**), `api/v1/webhooks/*` (`:1082-1093`).

### 4.2 Cloud rows with **no** implementation in the Android client (38) — all re-verified absent

**Cloud MCP (3)**

| Verb | Path | Our client | Evidence |
|---|---|---|---|
| POST | `/api/v1/mcp/test` | not found | no `mcp` string in `OpenHandsApiService.kt` |
| POST | `/api/v1/mcp/oauth/start` | not found | ″ |
| GET | `/api/v1/mcp/oauth/status/{jobId}` | not found | ″ |

**Cloud setup state (1)**

| GET | `/api/admin/setup-state` | not found | ″ |

**Model-Router meta-profiles (5)**

| Verb | Path |
|---|---|
| GET | `/api/organizations/{orgId}/meta-profiles` |
| GET | `/api/organizations/{orgId}/meta-profiles/{name}` |
| POST | `/api/organizations/{orgId}/meta-profiles/{name}` |
| DELETE | `/api/organizations/{orgId}/meta-profiles/{name}` |
| POST | `/api/organizations/{orgId}/meta-profiles/{name}/activate` |

**Org-scoped LLM-profile save (1)**

| POST | `/api/organizations/{orgId}/profiles` | shape mismatch — Kotlin only has `POST api/organizations/{org_id}/profiles/{name}` (`OpenHandsApiService.kt:937`) |

**Automation service (28)** — `/api/automation/*`; **zero** occurrences of "automat" anywhere in `app/src`, `shared/src`, `web/src`, `server/src` except the DTO field `enable_automations` (`OpenHandsModels.kt:824` shared, `:826` app) and a CHANGELOG line. Client-side there is no automation code at all.

| Verb | Path |
|---|---|
| GET / POST | `/api/automation/v1` (list / create bundle) |
| POST | `/api/automation/v1/preset/prompt` |
| POST | `/api/automation/v1/preset/plugin` |
| GET / PATCH / DELETE | `/api/automation/v1/{id}` |
| POST | `/api/automation/v1/{id}/dispatch` |
| GET | `/api/automation/v1/{id}/runs` |
| POST | `/api/automation/v1/runs/{runId}/cancel` |
| GET | `/api/automation/v1/{id}/tarball` |
| GET | `/api/automation/v1/capabilities` |
| POST | `/api/automation/v1/validate` |
| POST | `/api/automation/v1/webhooks` |
| POST / GET / PATCH / DELETE | `/api/automation/v1/drafts`, `/drafts/{id}`, `/drafts/{id}/dispatch` |
| POST | `/api/automation/v1/uploads` |
| GET / PUT / POST | `/api/automation/v1/git-sync/{status,config,check,sync}` |
| GET | `/api/automation/health` |
| GET | `/api/automation/sdk-version` |
| POST | `/api/automation/v1/telemetry/consent` (local-agent-server-only) |

### 4.3 Near-matches that still break parity

| Canvas call | Ours | Gap |
|---|---|---|
| `GET /api/v1/settings/profiles/{name}?exposeSecrets=encrypted` (04 §4.2) | `OpenHandsApiService.kt:363-364` — no query params | cannot read an encrypted profile |
| `POST /api/v1/settings/profiles` (canvas save fallback) | only the `{name}` form, `:366` | save shape differs; verify upstream accepts both |
| `POST /api/organizations/{orgId}/profiles` | only `…/{name}`, `:937` | same |

### 4.4 Runtime capability families — all 20 from 04 §4.3 are missing

| # | Capability | Android (Retrofit) | Web (Ktor) |
|---|---|---|---|
| 1 | Live session channel `ws…/sockets/session/{id}` + auth frame + envelope | absent | absent |
| 2 | `POST /api/conversations` (create) | absent | absent |
| 3 | `…/switch_acp_model` | absent | absent |
| 4 | `…/secrets` (push) | absent | absent |
| 5 | File ops (`search_subdirs`, `home`, `download`, `download-trajectory`, `upload`) | absent (we only have `GET {id}/workspace/{path}`, `AgentServerApiService.kt:182-186`) | absent |
| 6 | Runtime git (`/api/git/changes`, `/api/git/diff`) | absent | absent (git goes via cloud rows 64/65) |
| 7 | Bash (`execute_bash_command`, start/stop, `bash_events`) | absent | absent |
| 8 | MCP client (test/oauth) | absent | absent |
| 9 | ACP discovery (`/api/acp/models`) | absent | absent |
| 10 | Runtime settings (`/api/settings*`, `/api/settings/mcp/{key}`) | absent | absent |
| 11 | Runtime profiles `/api/profiles*` | absent | absent |
| 12 | Runtime agent profiles `/api/agent-profiles*` | absent | absent |
| 13 | Runtime meta profiles `/api/meta-profiles*` | absent | absent |
| 14 | Skills / plugins / workspaces | absent | absent |
| 15 | Shared client | absent | absent |
| 16 | `POST /api/hooks` | absent | absent |
| 17 | LLM metadata & balance | absent | absent |
| 18 | `/server_info` (+`/alive`,`/health`,`/ready`) | absent (we probe cloud `/api/service/health`, `OpenHandsApiClient.kt:114-116`) | absent |
| 19 | Workspace session cookie (`/api/auth/workspace-session`) | absent | absent |
| 20 | Canvas extensions session | absent | absent |

The one runtime family Android *declares* but never uses: `ConversationClient`-style control (interrupt/run/pause/goal/ask_agent/condense/fork/switch_llm, `AgentServerApiService.kt:65-134`). A grep of the ViewModel finds **no call** to any of `repository.respondToConfirmation|interruptConversation|runConversation|pauseConversation|startGoal|stopGoal|resumeGoal|askAgent|condenseConversation|vscodeUrl|forkConversation|readRuntimeFile|runtimeConversation`. Verified 2026-10-15: generic plumbing exists (`OpenHandsRepository.kt:444 runtimeFor`, `:462 runtimeCall`) but **`runtimeCall` has no caller anywhere in the app** — so 0 / 23 runtime controls are reachable from the UI. The web's single runtime call is `POST /api/cloud-proxy` → `…/events/respond_to_confirmation` (`OpenHandsApiClient.kt:227-254`).

### 4.5 Cross-cutting gaps (auth / proxy / WS)

| Concern | Canvas | Ours |
|---|---|---|
| Cloud auth | `Authorization: Bearer` + `X-Org-Id` only for the active backend (`04` §3.2) | Bearer injected server-side (`Server.kt:172`); **no `X-Org-Id` ever** — org-scoped rows (35–60) cannot be targeted. Android sends `X-Access-Token` (`OpenHandsApiClient.kt:75`) |
| Runtime auth | `X-Session-API-Key` per conversation, direct from the browser (CORS allowlist) | Envelope tunnel `POST /api/cloud-proxy` with host allowlist `*.all-hands.dev` only (`Server.kt:81-85`) |
| Cloud proxy | dual direct/proxy on `hostOverride`; envelopes pass `host`/`method`/`path`/`headers`/`body`/`timeout_seconds` | One POST route; envelope has **no `timeout_seconds` and no `authMode`/`sessionApiKey` passthrough** (`Server.kt:205-259`) |
| Browser key | injected session key / `__AGENT_CANVAS_*` markers | `X-OpenHands-Key` header or server env key (`Server.kt:151-153`) |
| Cookie auth (main app) | `POST /api/authenticate` `withCredentials` | absent — and our CORS config allows no credentials (`Server.kt:91-105`) |
| Live events | WebSocket with backoff 1 s→30 s + jitter, `after_seq` cursor, dedupe by id | **absent**; design only (`02-ws-event-channel-design.md` §4-5). Android fallback = 10 s REST poll (`OpenHandsViewModel.kt:407-415`); **web fallback = manual refresh only** (`WebAppState.kt:269-276`) |

---

## 5. Blocker map

Which missing items are blocked by what.

| Missing item | (a) reachable with existing HTTP client | (b) blocked on WS live channel | (c) blocked on runtime tunnel | (d) pure UI work |
|---|---|---|---|---|
| 28 automation cloud rows | ✅ add methods + `/cloud` prefix already proxies them | | | |
| F21–F24, F28 (automations UI, 144 rows) | | | | ✅ |
| Cloud MCP (3 rows) + F17 page | ✅ | | ✅ local MCP test/oauth need the runtime tunnel | ✅ |
| Meta-profiles (5) + F12 | ✅ | | | ✅ |
| Provider connections (4) + F11 | ✅ | | | ✅ |
| LLM profile create/edit/save (F10 depth) | ✅ (fix the URL shapes first) | | | ✅ |
| Settings schemas + F15/F16 pages | ✅ (`OpenHandsApiService.kt:355-358` already declares them) | | | ✅ |
| Live transcript, live status, reconnect, deltas (F06 depth) | | ✅ **primary blocker** | | |
| Terminal live append, browser screenshots, task list, metrics, goal status, planner socket (F27) | | ✅ (need event shapes not yet observed on the wire) | | |
| Agent-profile CRUD (F13 depth) | ✅ (`api/agent-profiles/*` declared, unwired) | | | ✅ |
| Commits tab / commit history (F08) | | | ✅ needs `GET /api/git/changes` on the runtime (or a cloud equivalent) | ✅ |
| File upload / folder browser / `Add Workspace` (F03, F08) | | | ✅ needs `POST /api/file/upload` + `/api/file/search_subdirs` | ✅ |
| Runtime settings, profiles, skills/plugins/workspaces clients (F17–F19) | | | ✅ | ✅ |
| Device-flow login, cookie auth, org switching (F25) | | | | ❌ neither — needs a new server surface (no `withCredentials` route exists) |
| Onboarding / first-run gate / checklist (F01, F02) | | | | ✅ |
| Conversation-list management (F04 depth) | ✅ (`PATCH`/`DELETE` exist) | | | ✅ |
| Composer depth — slash commands, `/plan`, `/goal`, `/btw` (F05) | ✅ for `/model` (`switch_profile` is wired, `OpenHandsViewModel.kt:749-761`) | ✅ for plan-mode socket + goal status + `/btw` (`ask_agent` declared, unwired) | | ✅ |
| F26 runtime variants | — | — | — | ❌ product decision, not a code blocker |

---

## 6. Priority-ranked close-list

`P0` breaks a core flow or blocks trust · `P1` meaningful parity gap · `P2` polish. Sizes are honest estimates for one engineer on this codebase.

### P0

| # | Item | Why it matters (user-visible) | Blocker | Size | Evidence |
|---|---|---|---|---|---|
| 1 | **Live agent-event channel** — implement `02-ws-event-channel-design.md` (C1–C9) | On web the transcript only changes when the user hits refresh; the user cannot tell whether the agent is working, finished, or failed. Canvas's entire chat model is the live stream | (b) WS | **L** | `02-ws-event-channel-design.md` §2, §5; current behaviour `web/…/WebAppState.kt:269-276,278-286`; canvas `use-websocket.ts:64-76` |
| 2 | **Web: wire the cloud-profile actions** — pass `onActivateCloudProfile` / `onDeleteCloudProfile` / `onRenameCloudProfile` / `activeCloudProfile` | The web Settings screen renders Use/Rename/Delete buttons that silently do nothing; users believe their model changed when it did not | (d) nothing | **S** | params omitted at `web/…/Main.kt:172-201`; defaults at `shared/…/SettingsScreen.kt:110-112`; working wiring `app/…/MainActivity.kt:266-268` |
| 3 | **Web: wire conversation rename + export trajectory** | The Rename dialog and "Export trajectory" menu item are no-ops in the browser build | (d) nothing | **S** | `web/…/Main.kt` `ConversationHost` (`:212-246`) never passes `onRename`/`onExportTrajectory`; defaults `shared/…/ConversationScreen.kt:122,134`; working wiring `app/…/MainActivity.kt:169,181-185` |
| 4 | **Android: implement the confirmation accept/reject card** | A blocked conversation on Android tells the user to open the web app — the agent cannot be unblocked from the app at all | (d) nothing — the client and repository call already exist | **M** | Android dead-ends to the browser: `app/…/ConversationDetailScreen.kt:295-322`; shared implements it: `shared/…/ConversationScreen.kt:469-479,530-576`; unused client `OpenHandsRepository.kt:498-513`, `AgentServerApiService.kt:166-170` |
| 5 | **Collapse the app/`shared` screen duplication and move the tests onto `shared`** | The two copies already diverged: the secret-name guard existed only in `shared` (fixed in `6c857a5`), the accept/reject card exists only in `shared`, the loading spinner only in `app`, and `UiRegressionTest` still targets the **app** copies so regressions in `shared` are invisible | (d) nothing | **M** | `app/src/test/java/com/example/UiRegressionTest.kt:51,121,403` bind to `NewTaskScreen`/`SettingsScreen`/`ConversationDetailScreen` from `app/…`; `shared/…/SettingsScreen.kt:100` accepts `isCloudSettingsLoading` and never uses it (app uses it at `app/…/SettingsScreen.kt:298`); `app/…/ConversationDetailScreen.kt:295-322` vs `shared/…/ConversationScreen.kt:469-479` |
| 6 | **Interim: poll events on web while WS is unimplemented** | Until item 1 lands the browser build must at least auto-refresh like Android does, or a long agent run looks frozen | (d) nothing | **S** | Android poll `app/…/OpenHandsViewModel.kt:407-415`; web has none |

### P1

| # | Item | Why it matters | Blocker | Size | Evidence |
|---|---|---|---|---|---|
| 7 | **Automation service client (28 cloud rows) + F21–F24, F28** | Canvas's second-biggest subsystem (5 of 28 families, 144 sub-features): scheduled + webhook automations, runs, logs, drafts, git-sync. Completely absent here | (a) + (d) | **L** (client) + **XL** (UI) | §4.2 automation table; zero "automat" hits in `app/src`, `shared/src`, `web/src`, `server/src` |
| 8 | **Settings shell F09 + F15 (condenser / agent-context / verification) + F16 (application)** | Users cannot tune confirmation mode, the critic, memory condensation or app preferences — behaviours the agent actually honours | (a) + (d) | **L** | schemas already declared and unused: `app/…/OpenHandsApiService.kt:355-358`; no caller |
| 9 | **MCP servers F17 + cloud MCP rows (3)** | MCP is how users extend the agent; canvas ships a marketplace, OAuth, health checks and save-as-secret | (a) + (c) for local MCP, (d) for the page | **L** | §4.2; §5 row |
| 10 | **Model router F12 + meta-profiles (5 cloud rows)** | Task-routing across profiles is a headline canvas feature | (a) + (d) | **M** | §4.2 |
| 11 | **Provider connections F11 + LLM profile create/edit F10** | Users cannot create or edit a model profile from the app at all — only activate/rename/delete an existing one | (a) + (d) | **L** | `shared/…/SettingsScreen.kt:288-326`; no save-profile method in `OpenHandsApiClient.kt` |
| 12 | **Conversation workbench: F08 depth + F27** (commits tab, planner, browser, task list, file tree, Rich/Plain, resize, overview column) | Ours replaces the chat with full-screen tabs, so reading the transcript and inspecting the workspace are mutually exclusive | (b) + (c) + (d) | **L** | `shared/…/ConversationScreen.kt:91-97,177-195` |
| 13 | **App shell F02** (sidebar rail, ⌘K command menu, pin-as-home, Getting-started checklist, alert banner) | Navigation is a 4-tab bottom bar; there is no way to reach 24 of 28 canvas families even if they existed | (d) | **L** | `web/…/Main.kt:87-108`; `app/…/MainActivity.kt:112-131` |
| 14 | **Agent-profile CRUD F13** | The typed cloud surface (`api/agent-profiles/*`) is fully declared and has no UI — users cannot pick a persona, tools, MCP scope or an ACP agent | (a) + (d) | **L** | `app/…/OpenHandsApiService.kt:573-616` (unused); local-only dialog `shared/…/AgentProfileDialog.kt:44-76` |

### P2

| # | Item | Why it matters | Blocker | Size | Evidence |
|---|---|---|---|---|---|
| 15 | **F01 onboarding / first-run gate** | First-run users see a bare "Not connected" banner instead of guided setup | (d) | **M** | `shared/…/DashboardScreen.kt:213-232`; the `POST api/complete_onboarding` route is dead surface (`OpenHandsApiService.kt:648`) |
| 16 | **F04 conversation-list management** (folders, pins, tags, archive, delete-all, ⋮ kebab, load-more) | Long-lived users cannot organize or bulk-manage conversations | (a) + (d) | **M** | `shared/…/ConversationsScreen.kt:55-184` |
| 17 | **F05 composer depth** (attachments, slash commands, plan mode, dictation, context meter, draft persistence) | The composer is a single-line-ish text field with a send button | (a) for `/model`, (b) for plan/goal/btw, (d) rest | **L** | `shared/…/ConversationScreen.kt:489-525` |
| 18 | **F18 skills / F19 plugins / F20 canvas apps** | The whole "Customize" hub | (a) + (c) + (d) | **L** | §3 rows F18–F20 |
| 19 | **F25 backends & cloud** (backend registry, device flow, org switching, manage/edit/remove) | One hardcoded cloud host; no multi-tenant or self-hosted story | (d) + a new server surface for cookie auth | **L** | `server/…/Server.kt:56-57`; `shared/…/SettingsScreen.kt:147-207` |
| 20 | **F26 runtime variants** | Decide explicitly whether the canvas launcher/Docker/desktop deployment is in scope; today it is neither implemented nor claimed | product decision | — | `server/…/Server.kt` is our own dev proxy |

---

## 7. Confidence & gaps — what could NOT be verified from code

| # | Open item | Why unverified | How to close it |
|---|---|---|---|
| 1 | **Payload shapes for all 97 cloud rows.** Comparison is normalized verb+path only; request/response bodies were never diffed (04's own caveat). A "matched" row may still fail at runtime (e.g. `launched_agent_profile` object-vs-string class of bug, already documented in `AGENTS.md`). | Static inventory, no live calls | One integration pass per matched row against a real key; start with the 59 Android matches |
| 2 | **The two §4.2 near-matches** — does upstream accept `POST /api/v1/settings/profiles` (no `{name}`) and `GET /api/v1/settings/profiles/{name}?exposeSecrets=encrypted`? | Not observable from our code | Two curl calls against `app.all-hands.dev` |
| 3 | **WS frame schema.** `02-ws-event-channel-design.md` risk R2 is unresolved: our `EventDto` is shaped for the cloud `events/search` response, not for the agent-server session socket. Every (b)-blocked row depends on this. | No socket exists to sample | Implement design step 2 (server proxy) then log the first ~20 frames |
| 4 | **`conversation_url == null` while `sandbox_status == "PAUSED"`** (design assumption A7, marked *Low* confidence). | The canvas file was outside the read set | Read `websocket-provider-wrapper.tsx` or observe a paused sandbox live |
| 5 | **Whether the wasm `WebSocket` symbol is available** for Kotlin 2.4.21 in this toolchain (design A5, *Medium*). | Not compiled | `./gradlew :web:compileKotlinWasmJs` with a throwaway `openEventSocket` |
| 6 | **Automation endpoint set** is versioned against `@openhands/extensions` 0.30.0 and may have grown (04's caveat 3). | Manifest pin | Re-read the pinned manifest before writing the client |
| 7 | **Visual/phone-layout claims.** Every "fits a 390 px viewport" sub-feature row is unverified — no device or browser run was performed. | Static inspection only | Screenshot sweep at 390/768/1024/1440 px on both targets |
| 8 | **Runtime-only scenes** (accreditation of the F04-cloud rows, F20 app backend bridge, F25 DigitalOcean, F13 ACP agents, F11 rotate) need accounts/infrastructure we do not have. | Blocked externally | Marked "blocked" in the feature map itself; not closable here |
| 9 | **The 20/97 web number assumes the Ktor client is the surface the web build actually uses.** Verified: `WebAppState` calls only `OpenHandsApiClient` methods, and `Main.kt:57-65` constructs it with `apiBaseUrl = "/cloud"`. No second client exists in `web/src`. | — | Confident |
| 10 | **`F01.api-key-entry` counted as partial.** Equivalent behaviour exists (Settings form + persistent key + Test), but not as the full-screen origin-locked gate canvas shows. | — | Confident |

---

*End of matrix. Inputs: `00-feature-map-subfeatures.md`, `04-ts-api-inventory.md`, `05-ts-ui-ux-model.md`, `02-ws-event-channel-design.md`; app read-only at `/tmp/opencode/ohaa-kmp` @ `71d9bb4`.*
