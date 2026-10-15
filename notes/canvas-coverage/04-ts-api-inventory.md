# 04 — Canvas (TS) API-usage inventory — evidence-backed

**Source of truth**: read-only clone `/opt/openhands` @ `38fe0c0` (2026-10-15), package `@openhands/agent-canvas` v1.27.1.
**Method**: static reads of `src/**` only; every path claim carries a `file:line` reference. Dynamic path bases (`${base}/...`) were resolved from the constants/comments in the same file (cited). Automation endpoint literals were resolved live from the pinned `@openhands/extensions` manifest (`AUTOMATION_INTERFACE`, package.json:26 = `0.30.0`; lockfile package-lock.json:4139 agrees; installed copy 0.19.0 carries byte-identical endpoints — both verified with `node`).
**Version note**: `@openhands/typescript-client` is pinned at `1.55.0` (package.json:27) while the local `node_modules` contains `1.39.0` (`npm ls` marks it invalid) — all SDK path mappings below were re-derived from the `1.55.0` tarball fetched from npm, not from the stale install. `CanvasExtensionsClient` exists only in 1.55.0 and is imported at `src/api/canvas-extensions-service.ts:3`.

**Route legend** (used in every table):

| Tag | Meaning |
|---|---|
| `CLOUD` | Cloud App API at `backend.host` (bearer auth, through `callCloudProxy` → `src/api/cloud/proxy.ts` unless noted) |
| `CLOUD/SDK` | Cloud call performed by an SDK `CloudClient` method (url built inside the SDK; canvas calls the method) |
| `MAIN-APP` | Same-origin relative call on the web host the page is served from (`src/api/main-app-auth.ts`), cookie auth |
| `AUTO` | `/api/automation/*` — hosted by the **local agent server** on a local backend, by the **cloud host** on a cloud backend (dual routing; see §1.6) |
| `AUTO-LOCAL` | `/api/automation/*` literal that canvas only calls on a **local** backend |
| `RUNTIME` | Per-conversation Agent Server at `conversation.conversation_url` (auth `X-Session-API-Key`) — full inventory in §2 |

---

## 1. Cloud API calls (verb + path)

97 rows (96 distinct verb+path pairs — `PATCH /api/v1/app-conversations/{id}` appears twice, rows 6–7, for sharing-toggle vs rename). Evidence column = the canvas call site(s) that prove the call; "Purpose" paraphrases the docblock/function at that site.

### 1.1 Conversations & sandboxes — `src/api/cloud/conversation-service.api.ts` (`CLOUD`)

| # | Verb | Path | Evidence (file:line) | Purpose |
|---|------|------|----------------------|---------|
| 1 | GET | `/api/v1/app-conversations/search` | `src/api/cloud/conversation-service.api.ts:102-103` | Search/paginate app-conversations (cloud history list) |
| 2 | GET | `/api/v1/app-conversations?ids=` | `src/api/cloud/conversation-service.api.ts:127-128` | Batch fetch conversations by id (bare JSON array quirk noted at :131) |
| 3 | POST | `/api/v1/app-conversations` | `src/api/cloud/conversation-service.api.ts:158-159` | Start a conversation (start task) on the cloud |
| 4 | GET | `/api/v1/app-conversations/{id}/download` | `src/api/cloud/conversation-service.api.ts:179-180` | Download conversation trajectory archive (`responseType: blob` at :182) |
| 5 | DELETE | `/api/v1/app-conversations/{id}` | `src/api/cloud/conversation-service.api.ts:198-199` | Delete an app-conversation |
| 6 | PATCH | `/api/v1/app-conversations/{id}` | `src/api/cloud/conversation-service.api.ts:216-217` | Toggle public-sharing flag (`{ public }`, :209-227) |
| 7 | PATCH | `/api/v1/app-conversations/{id}` | `src/api/cloud/conversation-service.api.ts:235-236` | Rename conversation title (title belongs to cloud resource, :228-245) |
| 8 | POST | `/api/v1/sandboxes/{id}/pause` | `src/api/cloud/conversation-service.api.ts:251-252` | Pause the cloud sandbox backing a conversation |
| 9 | POST | `/api/v1/sandboxes/{id}/resume` | `src/api/cloud/conversation-service.api.ts:269-270` | Resume the sandbox |
| 10 | GET | `/api/v1/app-conversations/{id}/file` | `src/api/cloud/conversation-service.api.ts:289-290` | Read one file from the cloud conversation workspace |
| 11 | GET | `/api/v1/app-conversations/{id}/files` | `src/api/cloud/conversation-service.api.ts:312-313` | List conversation workspace files |
| 12 | GET | `/api/v1/app-conversations/start-tasks?ids=` | `src/api/cloud/conversation-service.api.ts:332-333` | Poll start-task status for pending launches |
| 13 | GET | `/api/v1/sandboxes?ids=` | `src/api/cloud/sandbox-service.api.ts:35-36` | Batch sandbox lookup (status/backing info) |

### 1.2 Secrets, settings, git, config, MCP, skills, shared — `CLOUD`

| # | Verb | Path | Evidence (file:line) | Purpose |
|---|------|------|----------------------|---------|
| 14 | GET | `/api/v1/secrets/search` | `src/api/cloud/secrets-service.api.ts:39-40` | Search secrets (cloud secret store) |
| 15 | POST | `/api/v1/secrets` | `src/api/cloud/secrets-service.api.ts:103-104` | Create secret |
| 16 | PUT | `/api/v1/secrets/{name}` | `src/api/cloud/secrets-service.api.ts:92-93` | Update secret (used when a previous name exists) |
| 17 | DELETE | `/api/v1/secrets/{name}` | `src/api/cloud/secrets-service.api.ts:115-116` | Delete secret |
| 18 | GET | `/api/v1/settings` | `src/api/cloud/settings-service.api.ts:138-139` | Load flat cloud settings |
| 19 | POST | `/api/v1/settings` | `src/api/cloud/settings-service.api.ts:208-209` | Save cloud settings |
| 20 | GET | `/api/v1/settings/agent-schema` | `src/api/cloud/settings-service.api.ts:218-219` | Agent settings JSON schema |
| 21 | GET | `/api/v1/settings/conversation-schema` | `src/api/cloud/settings-service.api.ts:227-228` | Conversation settings JSON schema |
| 22 | GET | `/api/v1/web-client/config` | `src/api/cloud/settings-service.api.ts:249-250` | Web-client config (feature flags/limits) |
| 23 | GET | `/api/v1/git/repositories/search` | `src/api/cloud/git-service.api.ts:40-41` | Search repos the user's cloud git connections can access |
| 24 | GET | `/api/v1/git/installations/search` | `src/api/cloud/git-service.api.ts:66-67` | Search git app installations |
| 25 | GET | `/api/v1/git/branches/search` | `src/api/cloud/git-service.api.ts:96-97` | Search branches of a repo |
| 26 | GET | `/api/v1/config/models/search` | `src/api/config-service/config-service.api.ts:83-84` | Cloud model catalog search |
| 27 | GET | `/api/v1/config/providers/search` | `src/api/config-service/config-service.api.ts:161-162` | Cloud provider catalog search |
| 28 | POST | `/api/v1/mcp/test` | `src/api/cloud/mcp-service.api.ts:38-39` | Test an MCP server via the cloud (timeout `2×server+5`, :35) |
| 29 | POST | `/api/v1/mcp/oauth/start` | `src/api/cloud/mcp-service.api.ts:58-59` | Start cloud-hosted MCP OAuth job |
| 30 | GET | `/api/v1/mcp/oauth/status/{jobId}` | `src/api/cloud/mcp-service.api.ts:71-72` | Poll cloud MCP OAuth job status |
| 31 | GET | `/api/v1/skills/search` | `src/api/cloud/skills-service.api.ts:40-41` | Search cloud skill catalog |
| 32 | GET | `/api/v1/app-conversations/{id}/skills` | `src/api/cloud/skills-service.api.ts:78-79` | Skills loaded into a cloud conversation |
| 33 | GET | `/api/shared-conversations` | `src/api/cloud/shared-conversation-service.api.ts:33-34` | Load a shared conversation (public viewer) |
| 34 | GET | `/api/shared-events/search` | `src/api/cloud/shared-conversation-service.api.ts:56-57` | Page shared conversation events |

### 1.3 Organizations, profiles, agent/meta profiles, provider connections, setup-state — `CLOUD`

| # | Verb | Path | Evidence (file:line) | Purpose |
|---|------|------|----------------------|---------|
| 35 | GET | `/api/organizations` | `src/api/cloud/organization-service.api.ts:50-51` | List orgs for a cloud backend |
| 36 | GET | `/api/keys/current` | `src/api/cloud/organization-service.api.ts:74-75` | Current API key metadata (key↔org binding; legacy 400 handling at :80-100) |
| 37 | GET | `/api/organizations/{orgId}/me` | `src/api/cloud/organization-service.api.ts:116-117` | Current user's membership/role in org |
| 38 | GET | `/api/organizations/{orgId}/members/{userId}` | `src/api/cloud/organization-service.api.ts:144-145` | Look up an org member |
| 39 | GET | `/api/admin/setup-state` | `src/api/cloud/setup-state-service.api.ts:18-19` | Cloud onboarding/setup state |
| 40 | GET | `/api/agent-profiles` | `src/api/cloud/agent-profiles-service.api.ts:27,38-41` (`BASE = "/api/agent-profiles"`) | List agent profiles |
| 41 | GET | `/api/agent-profiles/{name}` | `src/api/cloud/agent-profiles-service.api.ts:48-51` | Get agent profile |
| 42 | POST | `/api/agent-profiles/{name}` | `src/api/cloud/agent-profiles-service.api.ts:59-62` | Save agent profile |
| 43 | DELETE | `/api/agent-profiles/{name}` | `src/api/cloud/agent-profiles-service.api.ts:70-73` | Delete agent profile |
| 44 | POST | `/api/agent-profiles/{name}/rename` | `src/api/cloud/agent-profiles-service.api.ts:81-84` | Rename agent profile |
| 45 | POST | `/api/agent-profiles/{profileId}/activate` | `src/api/cloud/agent-profiles-service.api.ts:92-95` | Activate agent profile |
| 46 | GET | `/api/organizations/{orgId}/profiles` **or** `/api/v1/settings/profiles` | `src/api/cloud/profiles-service.api.ts:27,34-47` (base = org route when org bound, else settings route) | List LLM profiles |
| 47 | GET | `{base}/{name}` | `src/api/cloud/profiles-service.api.ts:68-70` | Get LLM profile |
| 48 | POST | `{base}` | `src/api/cloud/profiles-service.api.ts:84-87` | Save LLM profile |
| 49 | DELETE | `{base}/{name}` | `src/api/cloud/profiles-service.api.ts:96-99` | Delete LLM profile |
| 50 | POST | `{base}/{name}/rename` | `src/api/cloud/profiles-service.api.ts:108-111` | Rename LLM profile |
| 51 | POST | `{base}/{name}/activate` | `src/api/cloud/profiles-service.api.ts:124-132` | Activate LLM profile |
| 52 | GET | `/api/organizations/{orgId}/meta-profiles` | `src/api/cloud/meta-profiles-service.api.ts:17-23,27-30` | List Model-Router meta profiles |
| 53 | GET | `/api/organizations/{orgId}/meta-profiles/{name}` | `src/api/cloud/meta-profiles-service.api.ts:38-41` | Get meta profile |
| 54 | POST | `/api/organizations/{orgId}/meta-profiles/{name}` | `src/api/cloud/meta-profiles-service.api.ts:50-53` | Save meta profile |
| 55 | DELETE | `/api/organizations/{orgId}/meta-profiles/{name}` | `src/api/cloud/meta-profiles-service.api.ts:62-65` | Delete meta profile |
| 56 | POST | `/api/organizations/{orgId}/meta-profiles/{name}/activate` | `src/api/cloud/meta-profiles-service.api.ts:73-76` | Activate meta profile |
| 57 | GET | `/api/organizations/{orgId}/provider-connections` | `src/api/cloud/provider-connections-service.api.ts:35-49,57-60` | List provider connections (org-scoped only) |
| 58 | POST | `/api/organizations/{orgId}/provider-connections` | `src/api/cloud/provider-connections-service.api.ts:69-72` | Create provider connection |
| 59 | PATCH | `/api/organizations/{orgId}/provider-connections/{id}` | `src/api/cloud/provider-connections-service.api.ts:82-85` | Update provider connection |
| 60 | DELETE | `/api/organizations/{orgId}/provider-connections/{id}` | `src/api/cloud/provider-connections-service.api.ts:94-97` | Delete provider connection |

### 1.4 Cloud per-conversation controls (App API, not runtime) — `CLOUD`

| # | Verb | Path | Evidence (file:line) | Purpose |
|---|------|------|----------------------|---------|
| 61 | POST | `/api/v1/app-conversations/{id}/switch_profile` | `src/api/conversation-service/agent-server-conversation-service.api.ts:1186-1191` | Per-conversation LLM profile swap (server resolves swap; home page falls back to profile activate, :1177-1181) |
| 62 | POST | `/api/v1/app-conversations/{id}/switch_acp_model` | `src/api/conversation-service/agent-server-conversation-service.api.ts:1242-1247` | Per-conversation ACP model swap |
| 63 | GET | `/api/v1/conversation/{id}/events/search` | `src/api/event-service/event-service.api.ts:110-113` | Cloud event **history** (persisted by App API; survives the sandbox) |
| 64 | GET | `/api/v1/app-conversations/{id}/git/changes` | `src/api/git-service/agent-server-git-service.api.ts:112-115` | Cloud git changes for the conversation workspace |
| 65 | GET | `/api/v1/app-conversations/{id}/git/diff` | `src/api/git-service/agent-server-git-service.api.ts:249-252` | Cloud git diff for one path |

### 1.5 Main-app (same-origin) endpoints — `MAIN-APP`

| # | Verb | Path | Evidence (file:line) | Purpose |
|---|------|------|----------------------|---------|
| 66 | POST | `/api/authenticate` | `src/api/main-app-auth.ts:5,49-55` | Exchange the main-app session cookie for canvas auth (`withCredentials: true`; 401 = not authed) |
| 67 | POST | `/api/analytics/events` | `src/api/main-app-auth.ts:6,36-44` | Fire `canvas_authenticated` analytics event (cookie auth) |
| 68 | POST | `{cloud host}/oauth/device/authorize` | canvas wrapper `src/api/device-flow-client.ts:21-26`; call site `src/hooks/use-device-flow.ts:98`; path defined in SDK `device-flow-client.js` (1.55.0) | Cloud device-flow onboarding start |
| 69 | POST | `{cloud host}/oauth/device/token` | canvas wrapper `src/api/device-flow-client.ts:28-36`; call site `src/hooks/use-device-flow.ts:129`; SDK `device-flow-client.js` | Poll device-flow token |

### 1.6 Automation service — `/api/automation/*` — `AUTO` (dual-routed), one `AUTO-LOCAL`

Base path `AUTOMATION_BASE_PATH = "/api/automation"` (`src/api/automation-service/automation-service.api.ts:60`). Endpoint literals come from the pinned manifest (keys `list/detail/dispatch/runs/tarball/health/capabilities/validate/createPrompt/createPlugin/createBundle/uploads`, resolved live, above) or are literal in code where the manifest has no key. Cloud routing uses `callCloudProxy`; local routing uses `localAutomationAxios` (:122-123). Upload on a cloud backend posts the gzip straight to the cloud host with a direct `axios.post` because the proxy client would JSON-serialize the bytes (comment :1013-1022).

| # | Verb | Path | Evidence (file:line) | Purpose |
|---|------|------|----------------------|---------|
| 70 | GET | `/api/automation/v1` | `:445` (+list) | List automations |
| 71 | POST | `/api/automation/v1` | `:806` + manifest `createBundle` (`:88-100`) | Create bundle automation |
| 72 | POST | `/api/automation/v1/preset/prompt` | manifest `createPrompt` (`:88-100`) | Create-from-prompt |
| 73 | POST | `/api/automation/v1/preset/plugin` | `:92` + manifest `createPlugin` | Create-from-plugin |
| 74 | GET | `/api/automation/v1/{id}` | `:476` | Get automation |
| 75 | PATCH | `/api/automation/v1/{id}` | `:516` (update), `:672-675` (toggle→update) | Update automation / enable-disable |
| 76 | DELETE | `/api/automation/v1/{id}` | `:587` | Delete automation |
| 77 | POST | `/api/automation/v1/{id}/dispatch` | `:604` + manifest `dispatch` | Dispatch (run now) |
| 78 | GET | `/api/automation/v1/{id}/runs` | `:640-655` + manifest `runs` | List runs |
| 79 | POST | `/api/automation/v1/runs/{runId}/cancel` | `:625` (literal path; comment :621) | Cancel a run |
| 80 | GET | `/api/automation/v1/{id}/tarball` | `:682` + manifest `tarball` (`responseType: arrayBuffer`) | Fetch bundle tarball bytes |
| 81 | GET | `/api/automation/v1/capabilities` | `:717` + manifest `capabilities` | Deployment capabilities (feature flags e.g. drafts) |
| 82 | POST | `/api/automation/v1/validate` | `:774` + manifest `validate` | Validate draft (200-with-errors contract, comment :769-771) |
| 83 | POST | `/api/automation/v1/webhooks` | `:737-746` (literal) | Create custom webhook |
| 84 | POST | `/api/automation/v1/drafts` | `:839` (literal) | Create server-backed draft |
| 85 | GET | `/api/automation/v1/drafts` | `:941` (literal) | List server-backed drafts |
| 86 | GET | `/api/automation/v1/drafts/{id}` | `:872` (literal) | Get draft |
| 87 | PATCH | `/api/automation/v1/drafts/{id}` | `:894` (literal) | Update draft |
| 88 | DELETE | `/api/automation/v1/drafts/{id}` | `:921` (literal) | Delete draft |
| 89 | POST | `/api/automation/v1/drafts/{id}/dispatch` | `:971` (literal) | Dispatch draft for a test run |
| 90 | POST | `/api/automation/v1/uploads` | `:1006` + manifest `uploads` | Upload bundle archive (cloud: direct axios, :1023-1049) |
| 91 | GET | `/api/automation/v1/git-sync/status` | `:1062` (literal) | Git-sync status |
| 92 | PUT | `/api/automation/v1/git-sync/config` | `:1081` (literal, method PUT) | Save git-sync config |
| 93 | POST | `/api/automation/v1/git-sync/check` | `:1106` (literal) | Validate git-sync config |
| 94 | POST | `/api/automation/v1/git-sync/sync` | `:1127` (literal) | Trigger git sync |
| 95 | GET | `/api/automation/health` | `:1145` + manifest `health` | Health check |
| 96 | GET | `/api/automation/sdk-version` | `:404` (literal) | Automation SDK version probe (5s timeout, :408) |
| 97 | POST | `/api/automation/v1/telemetry/consent` | `:390` (**local-only** — `if (active.backend.kind !== "local") return`, :386) | Telemetry consent sync (`AUTO-LOCAL`) |

**Cloud-reachable vs local-only**: rows 70–96 are dual-routed (`active.kind === "cloud"` → cloud host; else local agent server). Row 97 is local-only by construction. Row 90 takes the direct-axios path only in cloud mode (`:1023-1049`).

---

## 2. Agent Server runtime capabilities used

### 2.1 How the runtime host & key are resolved

- Client options come from `getAgentServerClientOptions(...)` (`src/api/agent-server-client-options.ts`), built from the active backend registry entry; cloud conversations override `host`/`apiKey` from the conversation resource — `conversation.conversation_url` + `conversation.session_api_key` (SDK docs mirrored in `src/api/agent-server-config.ts:17-23`; used at `src/api/event-service/event-service.api.ts:26-29`).
- Base URL/protocol derivation from a conversation URL: `buildHttpBaseUrl()` (`src/utils/websocket-url.ts:55`) — host fallback to `window.location.host`, path-prefix extraction for proxied runtimes (`/runtime/{port}`) at `:32-53`.
- SDK: `@openhands/typescript-client` 1.55.0 (`package.json:27`), imported mostly from the `.../clients` subpath.
- `src/api/no-direct-agent-server-calls.test.ts` enforces that runtime calls go through the SDK client/service layer (cited as a rule, not inventoried).

### 2.2 SDK clients instantiated by canvas and their endpoints

All "endpoint" paths below were read from the pinned 1.55.0 SDK tarball (`dist/client/*.js`, `dist/workspace/remote-workspace.js`); evidence on the canvas side is the instantiation/call site.

| # | SDK client | Method(s) used by canvas (evidence) | Runtime endpoints hit | 
|---|-----------|-------------------------------------|------------------------|
| 1 | `ConversationClient` | `createConversation` (`src/hooks/mutation/resolve-conversation-launch.ts:192`; `agent-server-conversation-service.api.ts:632-634,715-717`), `sendEvent` (`agent-server-conversation-service.api.ts:503-505`), `respondToConfirmation` (`event-service.api.ts:46-51`), `getEventCount` (`:62-67`), `interruptConversation` (`conversation-mutation-utils.ts:65-67`), `askAgent` (`:79-81`), `startGoal/stopGoal/resumeGoal` (`:96-98,:110-112,:119-121`), `updateSecrets` (`:140-142`), `runConversation` (`:166-168`), `deleteConversation` (`agent-server-conversation-service.api.ts:1046,1059`), `updateConversation` (`:1073,:1094`), `forkConversation` (`:1125`), `condenseConversation` (`:999-1006`), `switchLLM` (`:1212`), `switchAcpModel` (`:1251`), `getEvent` (`agent-server-conversation-service.api.ts:1150`) | `POST/GET /api/conversations`, `/api/conversations/{id}`, `…/interrupt`, `…/run`, `…/pause`, `…/condense`, `…/fork`, `…/ask_agent`, `…/goal`, `…/goal/stop|resume`, `…/secrets`, `…/switch_llm`, `…/switch_acp_model`, `…/switch_profile`, `…/events` (+`/search`, `/count`, `/respond_to_confirmation`, `/{eventId}`) |
| 2 | `FileClient` | `searchSubdirectories` (`src/hooks/query/use-search-subdirs.ts:23,35`), `getHome` (`src/api/agent-server-home.ts:63`; `use-search-subdirs.ts:69`), `downloadFile` (`src/api/plugins-service.ts:137-139`), `downloadTextFile` (`agent-server-conversation-service.api.ts:923`), `downloadTrajectory` (`:931`) | `/api/file/search_subdirs`, `/api/file/home`, `/api/file/download`, `/api/file/download-trajectory/{conversationId}` |
| 3 | `RemoteWorkspace` | `executeCommand` (`src/api/runtime-service/agent-server-runtime-service.ts:48-52`), `fileUpload` (`src/api/conversation-file-upload.api.ts:127-149`), `gitChanges` (`src/api/git-service/git-service.api.ts:190-192`; `agent-server-git-service.api.ts:136-142`), `gitDiff` (`git-service.api.ts:208-210`; `agent-server-git-service.api.ts:263-269`), `startWorkspaceSession` (`src/hooks/query/use-workspace-session.ts:70-76`) | `POST /api/bash/execute_bash_command`, `POST /api/file/upload`, `GET /api/git/changes`, `GET /api/git/diff`, `POST /api/auth/workspace-session` (+ `DELETE` at SDK for teardown) |
| 4 | `VSCodeClient` | `getUrl` (`src/api/conversation-service/conversation-service.api.ts:41-49`; `agent-server-conversation-service.api.ts:805`), `getStatus` (`agent-server-conversation-service.api.ts:829-838`) | `/api/vscode/url`, `/api/vscode/status` |
| 5 | `BashClient` | `executeCommand` (`src/hooks/use-bash-command-runner.ts:22-27`), `searchEvents` (`src/api/bash-service/bash-service.api.ts:97-102`) | `/api/bash/execute_bash_command`, `/api/bash/bash_events/search` (SDK also exposes start/stop/event-by-id variants) |
| 6 | `ACPClient` | `discoverModels` (`src/api/acp-service/acp-service.api.ts:129`) | `/api/acp/models` |
| 7 | `MCPClient` | `testServer` (`src/api/mcp-service/mcp-service.api.ts:216-219`), `startOAuth` (`:267`), `getOAuthStatus` (`:278`), `submitOAuthCallback` (`:292-298`) | `/api/mcp/test`, `/api/mcp/oauth/start`, `/api/mcp/oauth/status/{jobId}`, `/api/mcp/oauth/callback/{jobId}` |
| 8 | `SettingsClient` | `getSettings` (`src/api/settings-service/settings-service.api.ts:615,718,727`), `updateSettings` (`:751,898`), `patchMcpServer` (`:768`), `createMcpServer` (`:785`), `deleteMcpServer` (`:799`), `getSecret` (`src/api/git-provider-items-service.ts:48`) | `/api/settings`, `/api/settings/agent-schema`, `/api/settings/conversation-schema`, `/api/settings/secrets`, `/api/settings/secrets/{name}`, `/api/settings/mcp/{key}` |
| 9 | `ProfilesClient` | `listProfiles/getProfile/saveProfile/deleteProfile/renameProfile/activateProfile` (`src/api/profiles-service/profiles-service.api.ts:93-141`), `getProfile`+`activateProfile` in the local switch path (`agent-server-conversation-service.api.ts:1196-1204`) | `/api/profiles`, `/api/profiles/{name}` (+`/activate`, `/rename`, `/validate`) |
| 10 | `AgentProfilesClient` | `agent-profiles-service.api.ts:66-124` (list/get/save/delete/rename/activate) | `/api/agent-profiles`, `/api/agent-profiles/{name}` (+`/materialize`, `/rename`, `/activate`) |
| 11 | `MetaProfilesClient` | `src/api/meta-profiles-service/meta-profiles-service.api.ts:50+` | `/api/meta-profiles`, `/api/meta-profiles/{name}` (+`/activate`) |
| 12 | `SkillsClient` | `getSkills` (`src/api/skills-service.ts:51-58`) | `/api/skills` (SDK also exposes installed/install/marketplace/sync) |
| 13 | `PluginsClient` | `getPluginsMarketplace` (`src/api/plugins-service.ts:82`), `getPlugins` (`:110`) | `/api/plugins/marketplace`, `/api/plugins` |
| 14 | `WorkspacesClient` | `listWorkspaces/addWorkspaces/deleteWorkspace/addWorkspaceParents/removeWorkspaceParent` (`src/api/workspaces-service/workspaces-service.api.ts:26,42-60`) | `/api/workspaces`, `/api/workspaces/parents` |
| 15 | `SharedClient` | `searchSharedEvents` (`src/hooks/query/use-shared-conversation-events.ts:21-24`), shared conversation fetch (`src/hooks/query/use-shared-conversation.ts:24`) | `/api/shared-conversations`, `/api/shared-events/search` |
| 16 | `ServerClient` | `getServerInfo` (`src/api/agent-server-compatibility.ts:303-304,358`; `src/api/canvas-extensions-service.ts:128-129`) | `/server_info` (+`/alive`, `/health`, `/ready` in SDK) |
| 17 | `LLMMetadataClient` | `getVerifiedModels` (`src/hooks/query/use-verified-models.ts:20-21`) | `/api/llm/models/verified` (SDK also providers/models) |
| 18 | `HooksClient` | `loadHooks` (`src/api/hooks-service.ts:54-57`) | `POST /api/hooks` |
| 19 | `AgentServerClient` (raw) | `request` for the balance endpoint (`src/api/llm-balance-service.ts:72`; path constant `src/constants/llm-balance.ts:6` = `/api/llm/balance`); backend-bridge info (`canvas-extensions-service.ts:91,101`) | `/api/llm/balance`; `/server_info` (via `.server`); OpenAI subscription paths `/api/llm/subscription/openai/*` (SDK) |
| 20 | `CanvasExtensionsClient` | app-backend session create/revoke (`src/api/canvas-extensions-service.ts:146+`) | `/api/canvas-extensions/installed/{name}/backend/app-backends/{name}/session` (1.55.0 SDK) |

### 2.3 Live event stream — WebSocket `/sockets/session/{id}`

- URL builder: `buildWebSocketUrl()` — `ws(s)://{host}{pathPrefix}/sockets/session/{conversationId}` (`src/utils/websocket-url.ts:87-114`; protocol follows page/target scheme, :107-113). The query cursor is supplied by the caller, not the builder (`websocket-url.ts:102`): the main connection sends `after_seq` = newest-seen seq (`-1` = whole log) — `src/contexts/conversation-websocket-context.tsx:1142-1151`; the planner sub-conversation connection sends the same at `:1195-1201`.
- Connection + auth frame: `new WebSocket(wsUrl)` (`src/hooks/use-websocket.ts:64`) then `sendWebSocketAuth(ws, sessionApiKey)` on open (`:76`), which sends `{type: "auth", session_api_key: …}` (`src/utils/websocket-auth.ts:4-18`).
- Envelope protocol mirrored in `src/types/agent-server/session-frames.ts:1-71`: frames `sync | durable | transient | item_started | delta | item_aborted | error` over the session socket; "unknown frame types must be ignored" (the envelope is allowed to grow). This is a local mirror of the agent-server `session_protocol.py`, pending SDK types (comment :1-10).
- The legacy `/sockets/events` endpoint is frozen; the session socket is the live one (`websocket-url.ts:98-101`).

### 2.4 Workspace static-file session & static file serving

- `POST /api/auth/workspace-session` mints an `oh_workspace_session_key` cookie scoped to `/api/conversations` (jsdoc in SDK `remote-workspace.js:35-52`; call `use-workspace-session.ts:70-76`), letting `<iframe src>`/`<img src>` load workspace artifacts; `DELETE` teardown exists in the SDK.
- Served base: `{host}/api/conversations/{id}/workspace/` (SDK return value; consumed by `src/hooks/query/use-workspace-file-content.ts`).

---

## 3. Auth & backend model

### 3.1 Backend registry

- `Backend = { id, name, host, apiKey, kind: "local"|"cloud", authMode?: "api-key"|"cookie", connectionRevision?, marsSessionId?, marsConfigId? }` — `src/api/backend-registry/types.ts:1-24`.
- Persistence: `openhands-backends` (localStorage) and `openhands-active-backend` (sessionStorage first, localStorage fallback) — `src/api/backend-registry/storage.ts:13-14,212-243`.
- Seeds: local backend `default-local` from `VITE_*`/baked session key (`default-backend.ts:39-68`); locked cloud backend `locked-cloud` with `authMode: "cookie"` from the locked host (`default-backend.ts:22-37`, `LOCKED_CLOUD_BACKEND_ID` :20). A locked deployment never auto-seeds Local (`:57-60`).
- Active selection is tab-scoped; cross-tab links pin backend+org in the URL via `backend`/`org` query params (`src/api/backend-registry/url-selection.ts:20-21,28-55`).

### 3.2 Auth headers

- `buildAuthHeaders(backend)`: cloud → `Authorization: Bearer {apiKey}`; local → `X-Session-API-Key`; cloud+cookie → `{}`; empty apiKey → `{}` — `src/api/backend-registry/auth.ts:11-20`.
- `X-Org-Id` is added **only when the backend is the active one**: `activeOrgForBackend()` compares ids (`src/api/cloud/client.ts:24-27`), applied in `callCloudProxy` (`proxy.ts:32-34`) and in the SDK's direct/proxy header builders.
- Runtime calls: `X-Session-API-Key` from the conversation's `session_api_key` (SDK `http-client`; mirrored in `event-service.api.ts:21-29`).

### 3.3 Cloud request routing — direct vs proxy

`CloudClient.request()` routes on `hostOverride` (SDK `cloud-client.js:19-22`):
- **Direct** (no `hostOverride`): `GET/POST… {backend.host}{path}` with upstream headers `Authorization: Bearer` + `X-Org-Id` (+ per-call headers).
- **Proxied** (with `hostOverride`): `POST {proxy.host}/api/cloud-proxy` with headers `X-Session-API-Key` (local agent-server proxy) + `Content-Type: application/json`, envelope `{ host, method, path, headers, body, timeout_seconds? }` (SDK `cloud-client.js:328-367`).
- Proxy base selection: standalone Canvas uses its local Agent Server as the runtime proxy (`getAgentServerBaseUrl()`); SaaS Canvas falls back to the selected cloud host, which exposes the same `/api/cloud-proxy` endpoint (comment `src/api/cloud/client.ts:33-36`).
- `hostOverride` / `authMode` / `sessionApiKey` are passthrough fields on `CloudProxyRequest` (`src/api/cloud/proxy.ts:16-18,32-41`), and a `hostOverride` request requires the proxy via `createCloudClientForRuntime` (`client.ts:53-60`). **No current canvas call site passes them** — a repo-wide grep finds only the declarations in `proxy.ts` — so live cloud traffic takes the direct path today; the proxy envelope is the wired-but-unused mechanism for host-override requests (e.g. talking to a different cloud/runtime host with per-request credentials), and the proxy base remains the local agent server in standalone Canvas per the `client.ts:33-36` comment.
- Org-suspension bookkeeping on failure: `recordOrganizationSuspension` (`proxy.ts:42`).

### 3.4 Runtime request routing

All §2 calls go **directly** to the per-conversation runtime host from the browser (both local and cloud): CORS allowlist `OH_ALLOW_CORS_ORIGINS` + `X-Session-API-Key` preflight pass (comment `event-service.api.ts:25-32`; `runtime-service.ts:14-21`). No cloud proxy is involved on this leg.

### 3.5 Cookie-based auth surfaces

- **Main app**: `POST /api/authenticate` with `withCredentials` (main-app-auth.ts:49-55); used only when `getLockedCloudAuthMode() === "cookie"` and not a local browser host (`shouldUseMainAppCookieAuth`, :28-30). Failure → 401 handled, other errors rethrown (`:56-61`), then `canvas_authenticated` analytics.
- **Workspace static files**: `oh_workspace_session_key` HttpOnly cookie minted via `/api/auth/workspace-session` (§2.4).

### 3.6 Automation call headers & pinning

- Common headers: client-version headers + telemetry distinct-id header (`buildAutomationRequestHeaders`, `automation-service.api.ts:124-133`).
- Cloud calls pin the active backend's credentials at call time (`buildPinnedCloudHeaders` adds `X-Org-Id`, :375-379) so a mid-flight backend switch cannot reroute a request; local calls pin the local backend's config too (`buildPinnedLocalConfig`, :365-373). The tarball upload replicates the direct-path headers by hand (`Bearer` + `X-Org-Id`, :1035-1045).

---

## 4. Delta vs our Kotlin coverage

Kotlin sources compared (both copies are byte-identical, md5 `81c678c0…`):
- `/tmp/opencode/ohaa-kmp/app/src/main/java/com/example/data/api/OpenHandsApiService.kt` (211 ops; also at `/root/ohaa/...`),
- `/tmp/opencode/ohaa-kmp/app/src/main/java/com/example/data/api/AgentServerApiService.kt` (23 ops).
Comparison is normalized (params → `{p}`, query strings stripped) on **verb + path**; script + outputs kept at `/tmp/opencode/diffscript.mjs`.

### 4.1 Cloud endpoints canvas uses with **no normalized match** in `OpenHandsApiService.kt`

| Verb | Path | Canvas evidence | Note |
|------|------|-----------------|------|
| POST | `/api/v1/mcp/test` | `src/api/cloud/mcp-service.api.ts:38-39` | Cloud-hosted MCP test |
| POST | `/api/v1/mcp/oauth/start` | `src/api/cloud/mcp-service.api.ts:58-59` | Cloud MCP OAuth |
| GET | `/api/v1/mcp/oauth/status/{jobId}` | `src/api/cloud/mcp-service.api.ts:71-72` | Cloud MCP OAuth poll |
| GET | `/api/admin/setup-state` | `src/api/cloud/setup-state-service.api.ts:18-19` | Cloud onboarding state |
| GET | `/api/organizations/{orgId}/meta-profiles` | `src/api/cloud/meta-profiles-service.api.ts:27-30` | Model-Router profiles |
| GET | `/api/organizations/{orgId}/meta-profiles/{name}` | same, :38-41 | |
| POST | `/api/organizations/{orgId}/meta-profiles/{name}` | same, :50-53 | |
| DELETE | `/api/organizations/{orgId}/meta-profiles/{name}` | same, :62-65 | |
| POST | `/api/organizations/{orgId}/meta-profiles/{name}/activate` | same, :73-76 | |
| POST | `/api/organizations/{orgId}/profiles` | `src/api/cloud/profiles-service.api.ts:84-87` | Org-scoped save; Kotlin has only the settings-route `POST api/v1/settings/profiles/{name}` — **shape mismatch, verify upstream accepts both** |
| POST | `/api/automation/v1/{id}/dispatch` | automation table row 77 | |
| GET | `/api/automation/v1/{id}/runs` | row 78 | |
| GET | `/api/automation/v1/{id}/tarball` | row 80 | |
| POST | `/api/automation/v1/runs/{runId}/cancel` | row 79 | |
| GET | `/api/automation/v1/capabilities` | row 81 | |
| POST | `/api/automation/v1/validate` | row 82 | |
| POST | `/api/automation/v1/webhooks` | row 83 | |
| POST/PATCH/DELETE | `/api/automation/v1/{id}` (create/update/delete) | rows 71,75,76 | create=POST via presets/bundle |
| POST | `/api/automation/v1/preset/prompt`, `/v1/preset/plugin`, `/v1` (bundle) | rows 72,73,71 | |
| GET/POST | `/api/automation/v1` (list/create) | rows 70,71 | |
| GET/POST/PATCH/DELETE | `/api/automation/v1/drafts*` (+`/{id}/dispatch`) | rows 84-89 | |
| POST | `/api/automation/v1/uploads` | row 90 | |
| GET/PUT/POST | `/api/automation/v1/git-sync/{status,config,check,sync}` | rows 91-94 | |
| GET | `/api/automation/health`, `/api/automation/sdk-version` | rows 95,96 | |
| POST | `/api/automation/v1/telemetry/consent` | row 97 | local-agent-server-only route |

Count: **3 MCP + 1 setup-state + 5 meta-profiles + 1 org-profile-save + 28 automation rows = 38 missing cloud-side rows** (automation 28 includes the local-only telemetry route; all automation routes need a Kotlin client for the automation service regardless of host).

Everything else canvas calls on the cloud **matched** (normalized verb+path), including: app-conversations CRUD/search/download/file/files/start-tasks, sandbox pause/resume/list, secrets CRUD, settings+schema+web-client config, git repositories/installations/branches search, config models/providers, skills search + conversation skills, shared conversations/events, organizations/keys/me/members, agent profiles (all 6), LLM profiles settings-route (GET/DELETE/rename/activate), provider connections (all 4), switch_profile/switch_acp_model, conversation events search, git changes/diff, and the two main-app auth endpoints.

### 4.2 Near-matches to double-check (not counted as missing)

- `POST /api/v1/settings/profiles` (canvas save-profile fallback, `profiles-service.api.ts:84-87`) vs Kotlin `POST api/v1/settings/profiles/{name}` (`OpenHandsApiService.kt:366`) — different URL shape.
- `GET /api/v1/settings/profiles/{name}` with `exposeSecrets=encrypted` (canvas `profiles-service.api.ts:68-70`, consumed at `agent-server-conversation-service.api.ts:1204`) — Kotlin has the path (`:363`); query-param support must be verified.

### 4.3 Runtime capabilities canvas uses with **no counterpart in `AgentServerApiService.kt`**

Kotlin runtime coverage today (23 ops): conversations get/list/patch/delete/pause/interrupt/run/runtime; goal start/stop/resume; ask_agent; switch_llm; condense; fork; events search/count/get/send/respond_to_confirmation; vscode url/status; workspace file read. Everything below is used by canvas and **missing**:

| Capability | Runtime endpoint(s) | Canvas evidence |
|------------|---------------------|-----------------|
| Live session channel | `ws(s) …/sockets/session/{id}` + auth frame + envelope frames (`sync/durable/transient/item_started/delta/item_aborted/error`) | `websocket-url.ts:87-114`, `use-websocket.ts:64-76`, `session-frames.ts:1-71` |
| Conversation create | `POST /api/conversations` (`ConversationClient.createConversation`) | `resolve-conversation-launch.ts:192`, `agent-server-conversation-service.api.ts:632-634,715-717` |
| ACP model switch | `POST /api/conversations/{id}/switch_acp_model` | `agent-server-conversation-service.api.ts:1251` |
| Per-conversation secrets push | `POST /api/conversations/{id}/secrets` | `conversation-mutation-utils.ts:140-142` |
| File ops | `/api/file/search_subdirs`, `/api/file/home`, `/api/file/download`, `/api/file/download-trajectory/{id}`, `POST /api/file/upload` | §2.2 rows 2-3; Kotlin's single `{id}/workspace/{path}` read is not a substitute |
| Runtime git | `GET /api/git/changes`, `GET /api/git/diff` | `git-service.api.ts:190-210`, `agent-server-git-service.api.ts:136-142,263-269` |
| Bash | `/api/bash/execute_bash_command`, `/api/bash/start_bash_command`, `/api/bash/bash_commands/{id}/stop`, `/api/bash/bash_events` (+search, +id) | `use-bash-command-runner.ts:22-27`, `bash-service.api.ts:97-102` |
| MCP client | `/api/mcp/test`, `/api/mcp/oauth/start`, `/api/mcp/oauth/status/{jobId}`, `/api/mcp/oauth/callback/{jobId}` | `mcp-service.api.ts:216-298` |
| ACP discovery | `/api/acp/models` | `acp-service.api.ts:129` |
| Settings (runtime) | `/api/settings`, `/api/settings/agent-schema`, `/api/settings/conversation-schema`, `/api/settings/secrets`, `/api/settings/secrets/{name}`, `/api/settings/mcp/{key}` | `settings-service.api.ts:615-898`, `git-provider-items-service.ts:48` |
| Profiles (runtime) | `/api/profiles` (+`/{name}`, activate/rename/validate) | `profiles-service.api.ts:93-141` |
| Agent profiles (runtime) | `/api/agent-profiles` (+`/{name}`, materialize/rename/activate) | `agent-profiles-service.api.ts:66-124` |
| Meta profiles (runtime) | `/api/meta-profiles` (+`/{name}`, activate) | `meta-profiles-service.api.ts:50+` |
| Skills / Plugins / Workspaces | `/api/skills`, `/api/plugins` (+marketplace/installed), `/api/workspaces` (+parents) | `skills-service.ts:51`, `plugins-service.ts:82,110`, `workspaces-service.api.ts:26-60` |
| Shared client | `/api/shared-conversations`, `/api/shared-events/search` | `use-shared-conversation.ts:24`, `use-shared-conversation-events.ts:21` |
| Hooks | `POST /api/hooks` | `hooks-service.ts:54-57` |
| LLM metadata & balance | `/api/llm/models`, `/api/llm/models/verified`, `/api/llm/providers`, `/api/llm/subscription/openai/*`, `/api/llm/balance` | `use-verified-models.ts:20-21`, `llm-balance-service.ts:72`, `constants/llm-balance.ts:6` |
| Server info | `/server_info` (+`/alive`, `/health`, `/ready` in SDK) | `agent-server-compatibility.ts:303-304`, `canvas-extensions-service.ts:128-129` |
| Workspace session cookie | `POST/DELETE /api/auth/workspace-session` | `use-workspace-session.ts:70-76` |
| Canvas extensions | `/api/canvas-extensions/installed/{name}/backend/app-backends/{name}/session` | `canvas-extensions-service.ts:146+` |

### 4.4 Reverse-direction (Kotlin-only; canvas does not call) — informational

- `POST api/v1/app-conversations/stream-start` (Kotlin `OpenHandsApiService.kt:82`) — canvas starts conversations via `POST /api/v1/app-conversations` + start-tasks polling instead.
- `GET api/v1/git/suggested-tasks/search` (Kotlin `:295`) — no canvas call site found.
- `GET api/v1/app-conversations/{id}/hooks` (Kotlin `:188`) — canvas's hooks service targets the runtime (`POST /api/hooks`), not this cloud path. Also present Kotlin webhook management routes (`api/v1/webhooks/...` `:1082-1093`) that canvas does not call.
- Device-flow endpoints (`oauth/device/authorize|token`) exist in Kotlin and in canvas — no gap.

### 4.5 Counts

| Metric | Count |
|--------|-------|
| §1 cloud-side rows (canvas) | 97 (incl. 28 automation, 1 local-only; 65 cloud + 2 main-app + 2 device-flow counted above) |
| §2 SDK clients instantiated (runtime) | 20 (incl. `AgentServerClient` raw + `CanvasExtensionsClient`) |
| §4.1 cloud rows missing in Kotlin | 38 (rows: 3 MCP + 1 setup + 5 meta-profiles + 1 org-profile-save + 28 automation) |
| §4.2 near-matches to verify | 2 |
| §4.3 runtime capability families missing | 20 (each spanning 1-6 endpoints) |

**Caveats**: (1) verb+path equality is necessary but not sufficient for behavioral parity — payload shapes were not compared; (2) the two §4.2 near-matches need a live API check; (3) the automation surface is versioned against `@openhands/extensions` 0.30.0 and may grow with that package; (4) canvas reads `X-Org-Id`-scoped org data, so Kotlin's org handling must match the active-backend rule (`client.ts:24-27`).
