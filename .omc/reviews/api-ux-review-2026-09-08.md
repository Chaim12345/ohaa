# OpenHands Cloud Android client — API coverage & UI/UX review

Date: 2026-09-08 · Scope: the code in `/opt/ohaa` on branch `feat/cloud-v1-mobile-client` as of 11:50 IDT, reviewed against the live OpenAPI spec at `https://app.all-hands.dev/openapi.json` (fetched today). Independent of PR 1 / PR 2.

The tree is mid-rewrite by the implementation session, so line numbers are omitted and findings are stated against the design that is on disk (data layer, ViewModel, screens). Items are tagged **Fix** (bug or correctness gap), **Add** (missing capability), **Improve** (works but weak).

> **Addendum (12:10 IDT).** The implementation session reports that after this snapshot it declared 64 of 68 endpoints, replaced the Dashboard with a Home screen, added Workspace, Sandboxes and Secrets screens, ACP tool-call event support, and unit tests, with the build and tests passing. Section 1 therefore describes the 11:50 state; the §1.3 list is largely addressed at the service level, while the wiring, correctness (§2), and UX (§3) findings still need checking against the new screens. Priorities in §4 are unchanged.

---

## 1. API coverage

Spec: 70 operations that matter to a client (all `/api/v1/*` plus `/api/keys*`, `/api/billing/credits`, `/api/service/health`). Declared in `OpenHandsApiService`: **34**. Wired to UI: **~24**.

### 1.1 Covered and wired (conversation lifecycle is complete)

| Group | Endpoints | UI |
|---|---|---|
| Start | `POST app-conversations`, `GET start-tasks?ids` (polled to READY/ERROR) | New conversation, progress card |
| List | `GET app-conversations/search` (title filter, `next_page_id`), `GET app-conversations?ids` | Conversations list, detail refresh |
| Mutate | `PATCH {id}` (title, public), `DELETE {id}` | Rename / delete |
| Messages | `POST {id}/send-message` (auto-resume when sandbox PAUSED) | Composer |
| Events | `GET events/search` (incremental by `timestamp__gte`, paginated), `events/count`, `events?id` batch | Timeline |
| Sandbox | `sandboxes/search`, `sandboxes?id`, `pause`, `resume`, `DELETE` | Detail menu, Sandboxes screen |
| Workspace | `{id}/files`, `{id}/file`, `git/changes`, `git/diff` | Files & Changes screens |
| Git | `repositories/search`, `branches/search`, `suggested-tasks/search` | Repo/branch pickers, suggestions |
| Account | `users/me`, `keys/current`, `billing/credits` | Settings / Home |

### 1.2 Declared but not wired to any screen

`start-tasks/search`, `app-conversations/count`, `{id}/download` (trajectory zip), `pending-messages`, `sandboxes` POST (create), `sandbox-specs/search`, `users/git-info`, `config/models/search`, `service/health`. Each is either dead code or a feature waiting for a button — see §3 for which ones deserve UI.

### 1.3 Missing from the service (36 operations)

Grouped by whether a mobile client should have them.

**Add — high value for this app**

| Endpoint | Why it matters |
|---|---|
| `POST app-conversations/stream-start` | Returns start-task updates on one open connection. Replaces the 2 s polling loop and shows phase changes the moment they happen. |
| `GET settings`, `GET settings/profiles`, `GET settings/profiles/{name}` | The user's saved LLM profiles and active profile. Needed to replace the free-text "LLM model override" field with a picker of things that actually work for this account. |
| `POST app-conversations/{id}/switch_profile` | Switch the running conversation's LLM to a saved profile mid-conversation (web UI has this). |
| `POST app-conversations/{id}/switch_acp_model` | Same for ACP-backed agents (`agent_kind != "openhands"`). |
| `GET config/providers/search` + `config/models/search` (declared, unwired) | Model/provider picker instead of typing `anthropic/...` by hand. |
| `GET git/installations/search` | GitHub App installations (per org). Without it, `repositories/search` cannot be scoped to an org and large accounts get truncated results. |
| `GET users/git-organizations` | Org chips in the repo picker. |
| `GET secrets/search`, `POST secrets`, `PUT/DELETE secrets/{id}` | Custom secrets that the agent can use (API tokens, deploy keys). The start request already accepts `secrets`; the app cannot manage them. |
| `GET sandboxes/{id}/settings/secrets` | Show which secrets are exposed in a sandbox. |
| `GET app-conversations/{id}/skills`, `{id}/hooks` | Read-only "what is loaded in this conversation" panel: skills, hooks. Cheap and useful for debugging odd agent behaviour. |
| `GET web-client/config` | Feature flags, `providers_configured`, `gitlab_enabled`, `acp_providers`, `maintenance_start_time`. Lets the app hide providers that are not enabled and warn about maintenance windows. |
| `GET api/keys`, `POST api/keys`, `DELETE api/keys/{id}` | Manage API keys from the phone; lets the user rotate the leaked key from PR 2 without a laptop. |

**Add — nice to have**

`start-tasks/count`, `sandbox-specs?id` (batch), `skills/search`, `skills/marketplace-skills`, `settings/agent-schema`, `settings/conversation-schema`, `POST settings`, profile save/delete/rename/activate.

**Skip — not for a mobile client**

`webhooks/*` (server-to-server callbacks), `secrets/git-providers` (OAuth token store; provider linking must happen in the web UI anyway), `/api/admin/*`, `/api/billing/create-*`.

### 1.4 Endpoints the app calls that need care

- **`GET api/service/health`** exists in the spec but says nothing about the key. Connection test correctly uses `users/me`; drop `health()` or use it only for "server reachable" vs "key valid" distinction.
- **`POST pending-messages`** has no request-body schema in the spec. The app sends `SendMessageRequest`; verify against the server before relying on it.
- **`GET git/changes` requires the absolute repo root**. The app defaults to `/workspace`. The default `SandboxSpecInfo.working_dir` is `/home/openhands/workspace` and the repo is cloned into `<working_dir>/<repo-name>`. Derive the root from the sandbox spec plus `selected_repository` instead of a constant, or the Changes screen will always be empty.
- **`GET {id}/file`** returns a JSON string literal (the app unquotes it) and an **empty string for a missing file**, so "file not found" cannot be distinguished from "empty file".
- **`DELETE sandboxes/{id}`** takes the id as a query parameter; the app sends both, which matches the spec.

---

## 2. Implementation review (data layer, sync, security)

### 2.1 Correctness

1. **Fix — confirmation flow is not actually supported, and `conversation_url` is misused.** When `execution_status == waiting_for_confirmation` the UI tells the user to "reply to approve, adjust, or reject". A text message does not approve a pending action. The Cloud API has no confirm/reject endpoint; the web UI does it against the **agent server**. `AppConversation.conversation_url` is that agent-server endpoint (the `send-message` description says to call it "directly using the `conversation_url`"), paired with `session_api_key`; it is **not** the `app.all-hands.dev/conversations/<id>` web page. The detail screen's "Open in browser" therefore opens the agent server, not the web UI: build the web URL from the configured base URL plus the conversation id, and keep `conversation_url` + `session_api_key` for agent-server calls only (WebSocket events, confirm/reject, agent pause/resume). Verify the agent-server routes at `{conversation_url}/docs` before implementing.
2. **Fix — sending while the sandbox is STARTING.** `send-message` requires RUNNING. The repository handles PAUSED (resume then wait) but not STARTING; the call will fail. Either wait for RUNNING (same loop as resume) or route to `pending-messages`.
3. **Fix — pruning stale conversations only works for ≤ 50 conversations.** `deleteSyncedBefore(now)` runs only when `next_page_id == null`. Accounts with more conversations never see deletions made elsewhere.
4. **Fix — optimistic user message may never be replaced.** The local `local-*` row is removed only when a server user message with byte-identical body arrives. Server-side trimming or a message that is rejected without an error leaves a duplicate. Match on `tool_call_id`/order or drop the optimistic row after the first successful sync following the send.
5. **Fix — no 429 handling.** Polling loops (start task 2 s, detail 3 s, list 30 s) do not back off on 429/5xx. Add exponential backoff and honour `Retry-After`.
6. **Improve — label for a missing sandbox.** `SandboxStatus.MISSING` + idle/paused renders as "Paused"; "Stopped" is more accurate since it cannot be resumed.
7. **Improve — `ConversationStatus.derive` ignores `agent_kind`**. ACP agents report status differently; at minimum surface `agent_kind` in the header.
8. **Improve — Room `fallbackToDestructiveMigration(true)`** wipes the cache on every schema bump; acceptable for a cache, but the templates table is user data (saved templates). Move templates to their own DB or add a real migration.
9. **Improve — events are re-mapped on every sync page** (`EventMapper.map` per DTO) even when the row already exists. Cheap today; add `WHERE id NOT IN` or compare `updated` if event volume grows (conversations reach thousands of events).
10. **Improve — `IsoTime.toEpochMillis` fallbacks**: a naive timestamp is treated as UTC. Server timestamps are UTC, so fine; document it.

### 2.2 Security & privacy

1. **Fix — build-time default API key.** The user asked for the key in `.env.example` to be the default. Consequences to state plainly: it ends up in `BuildConfig` inside every APK, and the workflow now uploads the debug APK to temp.sh, a public URL. Anyone with the link can extract the key with `apktool`. Mitigations that keep the convenience: inject via a repository secret (`OPENHANDS_API_KEY` → `.env` at build time) so the key is not in git; rotate on a schedule; and treat the PR 2 key (`.env.example` + `DEFAULT_API_KEY`) as compromised — it is in git history on the remote.
2. **Fix — `HttpLoggingInterceptor.BODY` in debug builds** logs full event payloads to logcat, which includes file contents and terminal output from the sandbox (and any secret the agent echoed). Headers are redacted; bodies are not. Use `HEADERS` level, or `BODY` behind a settings toggle.
3. **Improve — API key at rest** is plain `SharedPreferences`. Backup exclusion was added (good). Next step: `EncryptedSharedPreferences` or Android Keystore, plus optional biometric gate on app open.
4. **Improve — `usesCleartextTraffic="true"`** for self-hosted servers. Restrict with a `network_security_config` that allows cleartext only for user-entered hosts, or at least warn when the base URL is `http://`.
5. **Improve — `public` toggle** exists in the repository (`setConversationPublic`) but no UI; if added, confirm with a dialog because it exposes the trajectory.

### 2.3 Build & CI

- Workflow now: JDK 21, unit tests, APK → temp.sh, link in step summary and as a tiny artifact, APK artifact `continue-on-error`. Correct given the account's exhausted artifact quota (398 artifacts, ~6.3 GB; cleanup run today deleted 364 + earlier batch; 2 failed on `ltimep`).
- **Improve** — temp.sh links expire and are unauthenticated; for a private repo, GitHub Releases (`softprops/action-gh-release` with a rolling `nightly` tag) gives a permanent, auth-protected download and does not count against artifact storage.
- **Improve** — `minSdk` raised to 26 for `java.time`; fine (covers the large majority of active devices). Mention in README.
- **Fix** — `GreetingScreenshotTest` still calls the old `DashboardScreen` signature and will not compile once the new screen lands; either update or delete with the Roborazzi dependency.
- **Add** — no tests exist for the new layer. Minimum set: `EventMapper` per kind (Message, Action/Terminal, Action/FileEditor, Observation/Terminal with exit code, AgentError, ServerError, StateUpdate noise), `ConversationStatus.derive` matrix, start-task polling against `MockWebServer` (READY, ERROR, timeout), 401 body → friendly message.

---

## 3. UI / UX review

### 3.1 Structure & navigation

- Four tabs (Home, Conversations, New, Settings) plus a stack for Detail → Files/Changes and Sandboxes is right for this app. **Fix**: `MainActivity` still renders the old `TasksScreen`/`NewTaskScreen` and does not handle system back for the stack; wire `BackHandler` to `goBack()`.
- **Add — deep links.** Register an intent filter for `app.all-hands.dev/conversations/<id>` (verify the exact web path) so links from Slack/GitHub open the conversation in the app. Build that web URL from base URL + id; do not use `conversation_url`, which is the agent-server endpoint (§2.1.1).
- **Add — share target.** Accept `ACTION_SEND` text/URLs (an issue link, a stack trace) and prefill the New conversation prompt. This is the single most "mobile" entry point and costs one manifest entry.
- **Add — app shortcuts / widget** for "New conversation" and "Conversations needing you".

### 3.2 The thing a phone is for: being told when to act

- **Add — background polling + notifications.** Nothing notifies the user when a conversation finishes, fails, or hits `waiting_for_confirmation`. A `WorkManager` periodic job (15 min minimum) polling `app-conversations/search?updated_at__gte=<last>` and posting a notification per transition is the biggest missing feature. While the app is foregrounded on a running conversation, keep the 3 s poll.
- **Add — "Needs you" section on Home** listing `waiting_for_confirmation`, `stuck`, and `error` conversations first.

### 3.3 Conversation detail

- **Fix — auto-scroll hijack.** `LaunchedEffect(events.size)` scrolls to the bottom on every new event, yanking the user while they read history. Only auto-scroll when already at the bottom; otherwise show a "N new events ↓" pill.
- **Fix — confirmation UX** (see §2.1.1): show Approve / Reject buttons, not a text hint, once the agent-server call is wired.
- **Add — kind filter chips** (Messages · Commands · Edits · Errors · All). The server supports `kind__eq`; a client-side filter over the cached list is enough.
- **Add — Markdown rendering** for agent messages (`MessageEvent` bodies are Markdown with code fences and lists). Plain `Text` today. Use a small Compose Markdown renderer or the `Html.fromHtml` route.
- **Add — image content.** `ImageContent.image_urls` in messages and observations is dropped ("[1 image attached]"). Render with Coil (already in the version catalog, commented out).
- **Add — attach an image when sending.** `SendMessageRequest.content` accepts `ImageContent`; a camera/gallery picker → base64 data URL is a natural mobile feature (screenshots of a bug).
- **Add — pause/resume the agent, not only the sandbox.** Sandbox pause stops the VM; users usually want "stop the agent, keep the sandbox". That is an agent-server call (`conversation_url`), same prerequisite as confirmation.
- **Add — date separators and "agent working for 4m 12s"** on the timeline; only clock time is shown.
- **Add — PR link.** `pr_number` is rendered as text; build the URL from `git_provider` + `selected_repository` and make it tappable.
- **Add — sub-conversations.** `parent_conversation_id` / `sub_conversation_ids` are stored but not navigable; show a "Part of …" breadcrumb and a "Sub-agents (n)" list.
- **Add — budget bar.** `metrics.max_budget_per_task` vs `accumulated_cost` (and `users/me.max_budget_per_task`) as a thin bar in the status strip.
- **Add — exposed URLs.** `SandboxInfo.exposed_urls` typically includes VS Code Web and app previews. One tap to open in Chrome is high value and is a single `GET sandboxes?id`.
- **Add — download trajectory** (`{id}/download`) → share sheet. Declared, unwired.
- **Improve — per-event actions**: long-press to copy body/code, share, "re-run this command" (sends the command as a message).
- **Improve — error events** should offer a one-tap "Retry / Continue" quick reply.
- **Improve — quick replies** (Continue, Run the tests, …) should be hidden while the agent is RUNNING; they read as inviting interruption.

### 3.4 New conversation

- **Improve — repository picker**: add recent repos (from cached conversations), provider/org filter (`git/installations/search`, `users/git-organizations`), and default-branch display. Manual `owner/repo` entry should validate the pattern.
- **Improve — model selection**: replace the free-text override with profiles (`settings/profiles`) and `config/models/search`, showing the account default.
- **Improve — Plan vs Build** copy is good; add a one-line example of what each produces.
- **Add — templates management**: repository supports save/delete but there is no UI to create a template from the current prompt or delete one.
- **Add — "Start from an issue"**: suggested tasks exist; also accept a pasted GitHub/GitLab issue URL and derive `selected_repository` + prompt.
- **Improve — validation**: warn when `llm_model` looks malformed, when the branch does not exist in the fetched list, and when the prompt is under ~15 characters.
- **Improve — progress card**: step labels are good; add elapsed time and the `detail` string prominently when the phase exceeds ~60 s (sandbox provisioning is the slow step).

### 3.5 Conversations list

- **Fix — the search box does not filter the list.** Both versions of `ConversationsScreen` render every cached row filtered only by status, while `updateSearchQuery` upserts the server's title matches into Room. Typing a query therefore adds rows and hides nothing. Apply the `title__contains` filter client-side as well, or show the server result page separately from the cache.
- **Improve — sorting/filters**: server supports `created_at`/`updated_at` ranges and `sandbox_id__eq`; expose "Today / This week" and "Needs you" quick filters.
- **Improve — swipe actions** (delete / pause) in addition to long-press menu; long-press is undiscoverable.
- **Improve — cost column**: show tokens on the card only when > 0; currently blank space.
- **Add — multi-select** for bulk delete of finished conversations (each is one `DELETE`).

### 3.6 Settings

- **Fix** — `SettingsScreen` on disk is the old signature (api key, base URL, "test connection" string). It should show the account block (`email`, `org_name`, `role`, credits, key name), the "using build-time key" indicator, and a "Show system events" toggle.
- **Add — paste-from-clipboard** for the key, and a "Create a key" link to `app.all-hands.dev/settings/api-keys`.
- **Add — API key management** via `api/keys` (list, create, revoke).
- **Improve — base URL**: validate scheme, trim trailing slashes, and show a warning badge when the host is not `app.all-hands.dev` (self-hosted).
- **Add — theme toggle** (system / dark / light) and text size; the app now follows the system theme only.

### 3.7 Files & Changes

- **Fix — repo root default** (§1.4). Also let the user pick the root from the `files` listing.
- **Improve — file browser**: `files` returns a flat list of every regular file; group into a tree with breadcrumbs and a filter box; large repos return thousands of paths.
- **Improve — diff view**: `git/diff` returns `original`/`modified`; compute a unified diff client-side with line numbers and +/− colouring instead of two blobs.
- **Add — syntax highlighting** for file view (a lightweight highlighter or `Highlight.js` in a WebView).

### 3.8 Accessibility, form factors, localisation

- Several labels are 9–11 sp (badges, meta rows). Minimum body text should be 12 sp; use `MaterialTheme.typography` scales so user font scaling applies.
- Copy buttons in code blocks are 32 dp; interactive targets should be ≥ 48 dp.
- Grey-on-dark meta text (`#8B949E` on `#161B22`) is about 5.6:1, which passes AA; the problem is size, not contrast — the 9–11 sp labels are below the readable floor and ignore the user's font scaling.
- All strings are hardcoded; move to `strings.xml` (only `app_name` exists today).
- No landscape/tablet layout: on ≥ 600 dp width use list-detail (conversations left, timeline right).
- Add `contentDescription` for status badges (currently decorative icons with text, acceptable) and for the pill colours (colour alone encodes sandbox state in `SandboxChip`).

---

## 4. Priorities

If only five things get done next:

1. Notifications via WorkManager for finished / error / needs-confirmation (§3.2).
2. Agent-server wiring (`conversation_url` + `session_api_key`): approve/reject confirmation, pause/resume agent, WebSocket events (§2.1.1, §3.3).
3. Profiles + model picker (`settings/profiles`, `config/models/search`, `switch_profile`) replacing free-text model entry (§1.3, §3.4).
4. Fix the git-changes repo root and the STARTING-sandbox send path (§1.4, §2.1.2).
5. Move the default key out of git into a CI secret and rotate the PR 2 key (§2.2.1).

## 5. Verification notes

- Endpoint inventory computed by diffing `@GET/@POST/...` annotations in `OpenHandsApiService.kt` against `openapi.json` paths (34 declared, all present in the spec; 36 client-relevant operations absent).
- Schema facts (`SandboxSpecInfo.working_dir` default, `ImageContent.image_urls`, `WebClientConfig` fields, `ProfileListResponse`, `pending-messages` having no body schema, `file` returning empty string for missing files) read from the spec today.
- The agent-server confirmation/pause routes are inferred from the Cloud API's own `send-message` description ("interact with the agent server directly using the `conversation_url`") and the SDK's remote conversation behaviour; they were not exercised against a live sandbox and must be verified before implementation.
- No live API calls were made with the user's key during this review.
