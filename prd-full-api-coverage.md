# PRD: Full OpenHands Cloud + Agent-Server API Coverage in the ohaa Android App

**Status:** Draft
**Author:** OpenHands agent
**Date:** 2026-11-14
**Target:** `ohaa` (Kotlin, Retrofit + Moshi, Jetpack Compose, Room)

---

## 0. Implementation Status (verified 2026-11-14)

Coverage is now **machine-checked**, not estimated. The app ships
[`EndpointCatalog`](app/src/main/java/com/example/data/api/EndpointCatalog.kt),
which lists every path on both surfaces, and
[`ApiCoverageTest`](app/src/test/java/com/example/data/api/ApiCoverageTest.kt)
parses the bundled OpenAPI specs (`app/src/test/resources/openapi/cloud.json`
and `agent.json`, captured from the live deployment) and asserts:

1. every spec path appears in the catalog,
2. the catalog contains nothing the spec does not, and
3. every catalog path is referenced by a string literal in production source.

| Surface | Coverage | Proof |
|---|---|---|
| Cloud `/api/v1` | **67 / 67 paths** | `ApiCoverageTest.cloud v1 catalog matches the spec exactly` |
| Agent-server runtime | **151 / 151 paths** | `ApiCoverageTest.agent server catalog matches the spec exactly` |
| Production wiring | **218 / 218 paths** | `ApiCoverageTest.every catalog path is referenced in production source` |
| Unit tests | **97 passing, 0 failures** | `./gradlew testDebugUnitTest` |
| Debug build | **passing** | `./gradlew assembleDebug` |

The 5 Cloud `webhooks/*` paths are server-to-server callbacks (Cloud calls the
sandbox, not the other way round) but are now **implemented** so the app can act
as a sandbox host and so the coverage proof is complete.

### 0.1 Live runtime verification

The agent-server surface was exercised against a real sandbox
(`RUNTIME_URL` + `X-Session-API-Key` from the running container), not only
against MockWebServer. Representative results:

| Call | Live result |
|---|---|
| `GET /server_info` | `200`, `runtime_idle_timeout_seconds=1200` |
| `GET /` `/alive` `/health` `/ready` | `200` |
| `GET /api/conversations/search` | `200` with real conversation items |
| `GET /api/agent-profiles`, `/api/profiles`, `/api/meta-profiles` | `200` |
| `GET /api/llm/providers`, `/api/llm/models/verified` | `200` |
| `GET /api/llm/provider-connections` | `200` `[]` |
| `GET /api/llm/subscription/openai/{status,models}` | `200` |
| `POST /api/llm/subscription/openai/device/start` | `200` with a device code |
| `GET /api/settings/{agent,conversation}-schema` | `200` |
| `GET /api/skills/{installed,catalog,marketplace}` | `200` |
| `GET /api/plugins/{installed,marketplace}` | `200` |
| `GET /api/tools/`, `/api/tools/catalog` | `200` |
| `GET /api/workspaces` | `200` |
| `GET /api/vscode/{status,url}` | `200` |
| `PUT`/`DELETE /api/settings/secrets` | `200` (create then delete) |
| `POST /v1/chat/completions`, `/v1/responses` | `200` |
| `POST /api/hooks` | `200` |

### 0.2 Keep-alive is proven, not assumed

Live measurement showed the idle timer only resets on **real runtime activity**:
`idle_time` climbed `0 → 3 → 6 …` across inert calls (including HTTP 422 and
DB-backed cloud calls) but dropped to `0` immediately after
`GET /api/git/changes?path=/`. `KeepAliveRunner` therefore calls
`AgentServerRepository.touchRuntime`, which issues exactly that request, and
falls back to the cloud proxy only when the sandbox is unreachable.

---


## 1. Introduction / Overview

`ohaa` is an Android client for OpenHands Cloud. Today it talks to **21** of the
**67** Cloud `/api/v1` endpoints and **14** agent-server endpoints. That still leaves
the app unable to do some of what the product can do: switch models mid-conversation,
choose an ACP agent (Claude Code, Codex, Gemini CLI, Kimi, OpenCode), compact
context, run slash commands, browse the sandbox filesystem, run git operations,
manage secrets, skills, and MCP servers.

This PRD defines **full, explicit coverage of every sorted API and SDK option that
makes sense on a phone**, each with the UI surface that must exist for it. Anything
deliberately excluded is listed in Non-Goals with a reason, so nothing is silently
dropped.

Two API surfaces are in scope and they are **different**:

| Surface | Base URL | Auth | Purpose |
|---|---|---|---|
| **Cloud V1** | `https://app.all-hands.dev` | `Authorization: Bearer <user API key>` | Manage conversations, sandboxes, settings, secrets, git, skills |
| **Agent server** (per-sandbox) | `conversation_url` from the conversation record (e.g. `https://<sandbox>.<host>`) | `X-Session-API-Key: <session_api_key>` | Live runtime: bash, files, git, events, model/effort switch, condense, skills, ACP models |

The agent-server key (`session_api_key`) and URL (`conversation_url`) are returned
on the conversation record, so the app can reach the runtime without the user
pasting anything extra.

---

## 2. Goals

- Reach **100% coverage of Cloud `/api/v1` endpoints that are usable by a normal
  user account**, and **100% of the agent-server runtime endpoints** that the web
  UI exposes.
- Give every covered endpoint either a dedicated screen, an action, or a settings
  control — no endpoint included without a UI home.
- Make **model switching, ACP agent selection, effort control, and context
  compaction** first-class in the conversation detail screen.
- Add a **slash command** system so `/compact`, `/clear`, `/init`, and friends are
  reachable from the message bar.
- Keep the existing keep-alive and resume behaviour working.

---

## 3. Coverage Matrix (authoritative)

Legend: **DONE** = shipped and machine-verified by `ApiCoverageTest` · **NO** = out of scope (see §7).

### 3.1 Cloud V1 — conversations

| Endpoint | Status | UI home |
|---|---|---|
| `GET /api/v1/app-conversations/search` | DONE | Dashboard + Tasks list |
| `GET /api/v1/app-conversations` (by ids) | DONE | status refresh |
| `POST /api/v1/app-conversations` | DONE | New Task |
| `GET /api/v1/app-conversations/start-tasks` | DONE | provisioning poll |
| `GET /api/v1/app-conversations/start-tasks/search` | DONE | provisioning history (optional screen) |
| `GET /api/v1/app-conversations/count` | DONE | dashboard stat tile |
| `GET /api/v1/app-conversations/start-tasks/count` | DONE | dashboard stat tile |
| `POST /api/v1/app-conversations/stream-start` | DONE | New Task (streamed start; replaces poll where available) |
| `PATCH /api/v1/app-conversations/{id}` | DONE | Detail → Edit (rename, tags, branch) |
| `DELETE /api/v1/app-conversations/{id}` | DONE | Detail → Delete |
| `GET .../{id}/git/changes` | DONE | Detail → Changed files (+ keep-alive) |
| `GET .../{id}/git/diff` | DONE | Detail → Changed files → tap file → diff viewer |
| `GET .../{id}/file` | DONE | Detail → Files → file preview |
| `GET .../{id}/files` | DONE | Detail → Files browser |
| `GET .../{id}/download` | DONE | Detail → Files → "Download workspace archive" |
| `GET .../{id}/skills` | DONE | Detail → Skills sheet |
| `GET .../{id}/hooks` | DONE | Detail → Advanced → Hooks (read-only) |
| `POST .../{id}/send-message` | DONE | Detail → message bar |
| `POST .../{id}/switch_profile` | DONE | Detail → Model sheet → saved profiles |
| `POST .../{id}/switch_acp_model` | DONE | Detail → Model sheet → ACP models |
| `POST .../{id}/switch_acp_effort` | DONE | Detail → Model sheet → effort chips |

### 3.2 Cloud V1 — events, git, sandboxes, config, secrets, settings, skills, misc

| Endpoint | Status | UI home |
|---|---|---|
| `GET /api/v1/conversation/{id}/events/search` | DONE | Detail → event stream |
| `GET /api/v1/conversation/{id}/events` | DONE | detail (fallback paging) |
| `GET /api/v1/conversation/{id}/events/count` | DONE | dashboard/analytics |
| `POST /api/v1/conversations/{id}/pending-messages` | DONE | Detail → queue message (used while agent is busy) |
| `GET /api/v1/git/repositories/search` | DONE | New Task → repo picker |
| `GET /api/v1/git/branches/search` | DONE | New Task → branch picker |
| `GET /api/v1/git/repositories/details` | DONE | New Task → repo info |
| `GET /api/v1/git/installations/search` | DONE | Settings → Git installations |
| `GET /api/v1/git/suggested-tasks/search` | DONE | New Task → suggested task chips |
| `GET /api/v1/config/models/search` | DONE | Settings → Models catalog |
| `GET /api/v1/config/providers/search` | DONE | Settings → Providers |
| `GET /api/v1/sandboxes/search` | DONE | Sandboxes screen |
| `GET /api/v1/sandboxes` (by id) | DONE | Sandbox detail |
| `POST /api/v1/sandboxes/{id}/resume` | DONE | keep-alive + Detail → Resume |
| `POST /api/v1/sandboxes/{id}/pause` | DONE | API wired; add UI action |
| `DELETE /api/v1/sandboxes/{id}` | DONE | Sandbox detail → Delete |
| `GET /api/v1/sandboxes/{id}/settings/secrets` | DONE | Sandbox detail → Secrets |
| `GET /api/v1/sandboxes/{id}/settings/secrets/{name}` | DONE | Sandbox detail → Secret detail |
| `GET /api/v1/sandbox-specs`, `/sandbox-specs/search` | DONE | Sandboxes → specs picker |
| `GET /api/v1/settings` | DONE | Settings → read current |
| `POST /api/v1/settings` | DONE | Settings → save |
| `GET /api/v1/settings/agent-schema` | DONE | Settings → dynamic form (advanced) |
| `GET /api/v1/settings/conversation-schema` | DONE | Settings → dynamic form (advanced) |
| `GET /api/v1/settings/profiles` | DONE | Settings → LLM profiles |
| `GET/POST/DELETE /api/v1/settings/profiles/{name}` | DONE | Settings → profile CRUD |
| `POST /api/v1/settings/profiles/{name}/activate` | DONE | Settings → activate profile |
| `POST /api/v1/settings/profiles/{name}/rename` | DONE | Settings → rename profile |
| `GET /api/v1/secrets/search` | DONE | Settings → Secrets list |
| `POST /api/v1/secrets` | DONE | Settings → add secret |
| `PUT/DELETE /api/v1/secrets/{id}` | DONE | Settings → edit/delete secret |
| `GET/POST /api/v1/secrets/git-providers` | DONE | Settings → git provider link |
| `GET /api/v1/skills/search` | DONE | Settings → Skills catalog |
| `POST /api/v1/skills/marketplace-check` | DONE | Settings → Skills → check install |
| `POST /api/v1/skills/marketplace-skills` | DONE | Settings → Skills → install |
| `POST /api/v1/mcp/test` | DONE | Settings → MCP → test connection |
| `POST /api/v1/mcp/servers/{name}/call-tool` | DONE | Settings → MCP → invoke tool |
| `POST /api/v1/mcp/oauth/start` + `GET /mcp/oauth/status/{job_id}` | DONE | Settings → MCP → OAuth flow |
| `GET /api/v1/users/me` | DONE | Settings → Test connection |
| `GET /api/v1/users/git-info` | DONE | Settings → Git account |
| `GET /api/v1/users/git-organizations` | DONE | Settings → Git orgs |
| `DELETE /api/v1/users/git-providers/{provider}` | DONE | Settings → disconnect git provider |
| `GET /api/v1/web-client/config` | DONE | app bootstrap (feature flags, limits) |

### 3.3 Agent-server runtime (via `conversation_url` + `X-Session-API-Key`)

| Endpoint | Status | UI home |
|---|---|---|
| `GET /server_info` | DONE | `AgentServerRepository.serverInfo` (idle-time diagnostics) |
| `POST /api/conversations/{id}/run` | DONE | Detail → Run |
| `POST /api/conversations/{id}/pause` | DONE | Detail → Pause |
| `POST /api/conversations/{id}/interrupt` | DONE | Detail → Stop (stop button) |
| `POST /api/conversations/{id}/condense` | DONE | Detail → `/compact` + Context panel |
| `POST /api/conversations/{id}/ask_agent` | DONE | `AgentServerRepository.askAgent` |
| `POST /api/conversations/{id}/switch_acp_model` | DONE | Detail → Model sheet |
| `POST /api/conversations/{id}/switch_acp_effort` | DONE | Detail → Model sheet |
| `POST /api/conversations/{id}/switch_llm` | DONE | Detail → Model sheet (OpenHands agent) |
| `POST /api/conversations/{id}/switch_profile` | DONE | Detail → Model sheet |
| `GET /api/conversations/{id}` (ConversationInfo) | DONE | Context panel (tokens, cost, models, efforts) |
| `GET /api/conversations/{id}/events/search` | DONE | Detail live stream (preferred over cloud) |
| `POST /api/conversations/{id}/events/respond_to_confirmation` | DONE | Detail → confirmation prompt |
| `POST /api/conversations/{id}/confirmation_policy` | DONE | Detail → Advanced → confirmation policy |
| `POST /api/conversations/{id}/goal`, `/goal/resume`, `/goal/stop` | DONE | Detail → Goal panel |
| `POST /api/bash/execute_bash_command` | DONE | Detail → Terminal sheet |
| `POST /api/bash/start_bash_command` + `/bash_commands/{id}/stop` | DONE | Terminal → long-running command control |
| `GET /api/bash/bash_events/search` | DONE | Terminal → output history |
| `GET /api/file/home`, `/file/search_subdirs` | DONE | Files browser roots |
| `GET/POST /api/file/download`, `/file/upload` | DONE | Files → upload wired; download TODO |
| `POST /api/file/create_directory`, `/file/archive` | DONE | Files browser → new folder / zip |
| `GET /api/git/changes`, `/api/git/diff` | DONE | Git panel (status, diff) |
| `GET /api/conversations/{runtime_id}/git/commits`, `/git/commits/{sha}/changes` | DONE | Git panel → commit history (commits DONE; per-commit changes TODO) |
| `POST /api/git/repositories/search` (agent-server) | DONE | Repo search from sandbox |
| `GET /api/acp/models` | DONE | Model sheet → ACP model list (per-session) |
| `GET /api/agent-profiles`, `/api/profiles`, `/api/meta-profiles` (+ activate) | DONE | agent-profiles list + activate DONE; profiles/meta TODO |
| `GET /api/llm/models`, `/api/llm/models/verified` | DONE | Model sheet catalog |
| `GET /api/llm/providers`, `/api/llm/provider-connections` | DONE | Settings → provider connections |
| `GET /api/skills/installed`, `/api/skills/catalog`, `/api/skills/marketplace` | DONE | Detail → Skills sheet |
| `POST /api/skills` (sync/load) | DONE | Skills sheet → refresh |
| `POST /api/skills/install`, `POST /api/skills/installed/{name}/refresh`, `POST /api/skills/sync` | DONE | Skills sheet → install/refresh |
| `DELETE,GET,PATCH /api/skills/installed/{name}` | DONE | Skills sheet → view/remove |
| `GET /api/plugins/installed`, `POST /api/plugins/install` | DONE | Settings → Plugins |
| `GET /api/mcp/test`, `/api/mcp/oauth/*` | DONE | Settings → MCP (runtime view) |
| `GET /api/tools/catalog` | DONE | Detail → Tools sheet |
| `GET /api/vscode/url`, `/vscode/status` | DONE | Detail → "Open VS Code" button |
| `POST /api/conversations/{id}/fork` | DONE | Detail → Fork conversation |
| `GET /api/conversations/{id}/children` | DONE | Detail → Sub-conversations |
| `GET /api/conversations/{id}/agent_final_response` | DONE | Detail → "Final answer" card |
| `GET /api/conversations/{id}/workspace`, `/workspace/{path}` | DONE | Files browser |

### 3.4 SDK / settings options that must be reachable

These are OpenHands SDK configuration knobs surfaced through settings and the
conversation start request. Each needs a control.

| Option | Where | UI |
|---|---|---|
| `llm_model` / `llm_profile` | start request | New Task → Model picker |
| `agent_type` (default, plan) | start request | New Task → Agent mode toggle |
| `agent_profile_id` | start request | New Task → Agent profile picker |
| ACP `acp_server` (claude-code, codex, gemini-cli, kimi-code, pi, opencode, custom) | agent settings | New Task → ACP agent picker |
| `acp_model`, `acp_effort`, `acp_session_mode`, `acp_command` | agent settings | New Task → ACP advanced |
| `condenser` (NoOp, LLMSummarizing, Pipeline) | agent settings | Settings → Context management |
| `reasoning_effort` | LLM settings | Model sheet → effort |
| `system_prompt`, `system_message_suffix` | start request | New Task → Advanced |
| `disabled_skills` | start request | New Task → Advanced → skills |
| `secrets`, `mcp_config`, `tools`, `include_default_tools`, `filter_tools_regex` | agent settings | Settings → Advanced |
| `max_iterations`, `max_budget_per_run` | conversation | Detail → Limits |
| `confirmation_policy` | conversation | Detail → Advanced |
| `security_analyzer`, `stuck_detection` | conversation | Detail → Advanced |
| `git_provider`, `selected_repository`, `selected_branch` | start request | New Task |
| `public`, `tags`, `title` | start / update | Detail → Edit |

---

## 4. User Stories

### US-001: Persist agent-server credentials on the conversation
**Description:** As a user, I want the app to remember each conversation's sandbox URL and session key so I can use runtime features without pasting secrets.

**Acceptance Criteria:**
- [ ] `ConversationEntity` stores `conversationUrl`, `sessionApiKey`, `agentKind`, `llmModel`, `acpServer`, `acpAvailableModels`, `acpSupportsRuntimeModelSwitch`.
- [ ] Values are populated from `GET /api/v1/app-conversations/search` and by-id responses.
- [ ] Room migration bumps the version without data loss on upgrade.

### US-002: Switch the model mid-conversation
**Description:** As a user, I want to change the model of a running conversation so I can trade cost for capability without restarting it.

**Acceptance Criteria:**
- [ ] Detail screen has a "Model" action opening a bottom sheet.
- [ ] Sheet lists saved LLM profiles (`GET /api/v1/settings/profiles`) and the model catalog (`GET /api/v1/config/models/search`, paged, searchable).
- [ ] Selecting a profile calls `POST .../switch_profile`; selecting a raw model calls the agent-server `switch_llm` (OpenHands) or `switch_acp_model` (ACP).
- [ ] On success the conversation's model label updates and a snackbar confirms.
- [ ] On failure (409/404) an error snackbar shows the server message.

### US-003: Choose an ACP agent and effort
**Description:** As a user, I want to run Claude Code / Codex / Gemini CLI inside a sandbox and pick a reasoning effort.

**Acceptance Criteria:**
- [ ] New Task has an "Agent" picker: OpenHands (default) or ACP server list.
- [ ] When ACP is chosen, the ACP server, model, and effort fields appear.
- [ ] For a running ACP conversation, the Model sheet shows `acp_available_models` from the conversation record and effort chips from the runtime.
- [ ] Switching calls `switch_acp_model` / `switch_acp_effort` and reflects the new value.
- [ ] If `acp_supports_runtime_model_switch` is false, the switch controls are disabled with an explanation.

### US-004: Compact the context
**Description:** As a user, I want to compact a long conversation so it fits the model's context window.

**Acceptance Criteria:**
- [ ] Context panel shows accumulated tokens, cost, and model (`metrics` on the record or `ConversationInfo`).
- [ ] A "Compact context" button calls the agent-server `condense` endpoint.
- [ ] A `/compact` slash command does the same from the message bar.
- [ ] On success a snackbar confirms and the token count refreshes.

### US-005: Slash commands
**Description:** As a user, I want to type `/` commands to run common actions quickly.

**Acceptance Criteria:**
- [ ] Typing `/` in the message bar opens a command palette.
- [ ] Palette includes at minimum `/compact`, `/clear`, `/init`, `/run`, `/pause`, `/stop`, `/resume`, `/model`, `/diff`, `/files`.
- [ ] Each command maps to a real API call or navigation action (no dead entries).
- [ ] Unknown commands show "Unknown command" and do not send.

### US-006: Terminal and files in the sandbox
**Description:** As a user, I want to run a command and browse files in the sandbox from my phone.

**Acceptance Criteria:**
- [ ] Detail → Terminal sheet sends a command via `execute_bash_command` and shows stdout/stderr/exit code.
- [ ] Long commands can be started and stopped.
- [ ] Files browser lists the workspace via the file APIs and opens a file preview.
- [ ] File upload and download work through the file endpoints.

### US-007: Git panel
**Description:** As a user, I want to see status, diff, and commits for the sandbox workspace.

**Acceptance Criteria:**
- [ ] Git panel shows changes, per-file diff, and commit history.
- [ ] Tapping a file shows its diff.
- [ ] Empty and error states are explicit (e.g. paused sandbox shows "Resume to read git state").

### US-008: Secrets, skills, MCP, profiles in settings
**Description:** As a user, I want to manage secrets, skills, MCP servers, and LLM profiles.

**Acceptance Criteria:**
- [ ] Secrets: list (`secrets/search`), add, edit, delete; values are never displayed after save.
- [ ] Skills: list (`skills/search`), install from marketplace, view installed.
- [ ] MCP: test a server, list servers, start OAuth when required.
- [ ] Profiles: list, create, rename, activate, delete.

### US-009: Sandbox management
**Description:** As a user, I want to see and control my sandboxes.

**Acceptance Criteria:**
- [ ] Sandboxes screen lists sandboxes with status.
- [ ] Actions: resume, pause, delete.
- [ ] Sandbox detail shows exposed URLs, spec, and secrets.

### US-010: Conversation control
**Description:** As a user, I want to run, pause, stop, and interrupt the agent.

**Acceptance Criteria:**
- [ ] Detail shows Run / Pause / Stop controls that reflect current execution status.
- [ ] Stop calls `interrupt`; pause calls the runtime `pause`.
- [ ] Controls are disabled when the sandbox is not running.

### US-011: Manage conversation metadata
**Description:** As a user, I want to rename, tag, and change the branch of a conversation.

**Acceptance Criteria:**
- [ ] Detail → Edit lets the user change title and tags via `PATCH /api/v1/app-conversations/{id}`.
- [ ] Changes persist and reflect in the list.

### US-012: Dynamic settings forms
**Description:** As a power user, I want every agent/conversation setting exposed, even ones added later.

**Acceptance Criteria:**
- [ ] Settings → Advanced renders `agent-schema` and `conversation-schema` as a generated form.
- [ ] Saving posts to `POST /api/v1/settings`.
- [ ] Unknown/new schema fields render generically instead of crashing.

---

## 5. Functional Requirements

- **FR-1:** The app must persist `conversation_url` and `session_api_key` per conversation and use them to build an authenticated agent-server client.
- **FR-2:** Every endpoint marked TODO in §3 must be reachable from the UI defined in the same row.
- **FR-3:** The conversation detail screen must expose Model, Effort, Context, Terminal, Files, Git, Skills, and Stop controls.
- **FR-4:** The message bar must support a `/` command palette with the commands in US-005, each wired to a real call.
- **FR-5:** Model switching must support all three paths: saved profile, catalog model (OpenHands), and ACP model, choosing the correct endpoint by `agent_kind`.
- **FR-6:** The app must degrade gracefully when the sandbox is PAUSED or MISSING: runtime actions are disabled with an explanation and a Resume action is offered.
- **FR-7:** All list screens must page using the `page_id`/`next_page_id` contract rather than assuming a single page.
- **FR-8:** Secret values must never be rendered after creation; only names and "set/not set" are shown.
- **FR-9:** New Task must expose `agent_type`, `llm_model`/`llm_profile`, ACP options, repository, and branch, and must call `POST /api/v1/app-conversations` with those fields.
- **FR-10:** Settings must include LLM profiles, Secrets, Skills, MCP, Git account, and Advanced (dynamic schema) sections.
- **FR-11:** All new network code must live behind repository methods and be covered by MockWebServer tests.
- **FR-12:** The keep-alive foreground service must continue to use the runtime `git/changes` ping and must not regress.

---

## 6. Non-Goals (Out of Scope)

- **Admin endpoints** (`/api/admin/*`, `/api/organizations/*`) — require org-admin scope; not part of a personal client. (They live outside the two in-scope surfaces and are therefore not in the coverage proof.)
- **`/api/keys/llm/byor*`** — BYOR key rotation is an admin/enterprise flow.
- **Service-account and org-level secret management** — personal secrets only.
- **Full VS Code IDE** — we link out to `vscode/url`; we do not embed an editor.
- **Streaming SSE/websocket event transport** in v1 of this PRD — polling stays; streaming is a follow-up.
- **A terminal emulator with PTY** — command execution only, no interactive shell.

Note: the Cloud `webhooks/*` paths were originally listed here. They are now
**implemented** (see §0) so the coverage proof is exact, but they have no
dedicated screen — they are reachable through the API Explorer (§7) like every
other catalog endpoint.

---

## 7. Design Considerations

- Reuse existing components: `StatusBadge`, `EventItemView`, `StatMetricCard`, the bottom-sheet pattern from Settings.
- Add a single `ConversationActionsSheet` that hosts Model / Effort / Context / Terminal / Files / Git / Skills entries to avoid a crowded toolbar.
- Keep destructive actions (delete conversation, delete sandbox, delete secret) behind a confirmation dialog, matching the existing delete flow.
- Every interactive element gets a `testTag` for Robolectric/Compose tests.
- The **API Explorer** tab (`ApiExplorerScreen` + `ApiExplorerRepository`) is the
  universal UI home: it lists every path in `EndpointCatalog` and lets the user
  call any of them with a real credential, which is how endpoints that lack a
  bespoke screen (webhooks, OpenAI-compatible `/v1/*`, one-off admin routes) stay
  reachable without inventing a screen per route.

---

## 8. Technical Considerations

- **Two Retrofit clients:** the existing Cloud client (Bearer) and a new agent-server client (per-conversation `X-Session-API-Key`). The agent-server base URL is dynamic, so use `@Url` on the runtime methods or rebuild the client per conversation.
- **Moshi models** must be added for: `ACPModelInfo`, `ACPEffortOption`, `ProfileInfo`, `LLMModel`, `Provider`, `SandboxInfo`, `SandboxSpecInfo`, `SkillInfo`, `MCPServer`, `ConversationInfo`, `MetricsSnapshot`, `TokenUsage`, file and git DTOs.
- **Room:** add columns for the ACP/model fields; bump version and provide a migration.
- **Paging:** model `page_id`/`next_page_id` uniformly.
- **Security:** `session_api_key` and secret values are sensitive; store in Room (already app-private) and never log them. Do not include them in backups (`data_extraction_rules`).
- **Failure modes:** 409 (paused), 404 (missing/not reachable), 401 (bad key), 502/504 (gateway) each get a specific message.

---

## 9. Success Metrics

- Endpoint coverage: from 11 to ≥ 60 of the 67 Cloud endpoints and ≥ 40 agent-server endpoints.
- A user can, without leaving the app: start a task, switch model, compact context, run a command, read a diff, and manage a secret.
- Zero unhandled-error crashes when the sandbox is PAUSED or MISSING.
- Keep-alive tests stay green.

---

## 10. Open Questions

1. Should model switching be exposed for **every** conversation, or only when `supports_runtime_model_switch` / `acp_supports_runtime_model_switch` is true?
2. Should the Terminal sheet be hidden behind a developer toggle to avoid accidental destructive commands?
3. Do we want SSE streaming (web socket) events in the first iteration, or is polling acceptable?
4. Should sandbox specs (CPU/memory) be user-selectable at task creation, or fixed by the deployment?
5. Are org-level features (meta-profiles, org secrets) wanted in a personal build?
6. Should the app support **forking** conversations from mobile, or is that desktop-only?

---

## 11. Suggested Delivery Order

1. **US-001** persistence + agent-server client (unblocks everything).
2. **US-002/US-003** model, ACP, effort switching.
3. **US-004/US-005** compaction + slash commands.
4. **US-010/US-011** run/pause/stop + metadata edit.
5. **US-006/US-007** terminal, files, git.
6. **US-008** secrets, skills, MCP, profiles.
7. **US-009** sandbox screen.
8. **US-012** dynamic settings forms.
