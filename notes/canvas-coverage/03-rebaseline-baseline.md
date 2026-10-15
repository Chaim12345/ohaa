# Canvas re-baseline — repo freshness + model sources (lead notes)

Date: 2026-10-15 (evening). Part of the "app covers everything canvas does (all API + all UI)" effort.

## The baseline moved (this is why the old 47-row audit is not enough)

| | |
|---|---|
| Reference repo | `github.com/OpenHands/OpenHands` (local clone `/opt/openhands`, remote `All-Hands-AI/OpenHands` → redirects) |
| Package | `@openhands/agent-canvas` v1.27.1 |
| Old checkout (used for the first audit) | `ea7a85c` — 2026-09-08 |
| New checkout (today, fetched + fast-forwarded) | `38fe0c0` — 2026-10-15 10:56 EDT |
| Delta | **437 commits** |
| Feature map baseline | `main@8b9823a…` (2026-10-14) per the repo's own map |

Moved areas in the delta (scope histogram): automations (18+5 commits), settings (14), chat (10),
MCP (6), a11y (6), themes (5), canvas (5), onboarding (4), agent-profiles (3), verify-openhands (22 docs).

## What the repo itself says the app is (authoritative sources)

1. **Route table** — `src/routes.ts` (React Router "framework mode", 30 routes):
   - `/` index-home, `/conversations` (list), `/conversations/:id`, `/conversations/:id/panel` (phone drawer page)
   - `/launch` (plugin launch review), `/customize` → redirects `/mcp` (desktop), `/skills`, `/plugins`, `/apps`, `/extensions/:name/*`
   - `/mcp`, `/oauth/device/verify`
   - `/settings` + 10 children: index, `llm`, `meta-llm` (Model Router), `agent` (legacy redirect), `agents` (agent profiles), `condenser`, `agent-context`, `verification`, `app`, `secrets`
   - `/automations` + `git-sync`, `templates`, `new/:automationId`, `setup`, `:automationId`
   - `/shared/conversations/:id` (standalone read-only)
   - `*` not-found (inside the app shell)

2. **Feature map (their own maintained verification spec)** — `.agents/skills/verify-openhands/references/feature-map/`:
   - `F01` first-run/onboarding/sign-in · `F02` app shell/sidebar/command menu · `F03` home/launcher ·
     `F04` conversation list (folders/pins/tags/archive/delete-all) · `F05` composer (slash commands/plan mode) ·
     `F06` agent activity (messages/lifecycle/confirmations/branching) · `F07` conversation page (header/menus/export) ·
     `F08` workspace drawer (files/commits/diffs) · `F09` settings shell · `F10` LLM profiles ·
     `F11` provider connections · `F12` model router (meta-profiles) · `F13` agent profiles (OpenHands + ACP presets) ·
     `F14` secrets (+ agent `request_secret` modal) · `F15` condenser/agent-context/verification ·
     `F16` application settings · `F17` MCP servers (custom + library + OAuth) · `F18` skills ·
     `F19` plugins · `F20` canvas apps (extensions) · `F21` automations dashboard · `F22` automation creation ·
     `F23` automation detail (runs/logs) · `F24` automation git sync · `F25` backends/cloud (device flow, org, DO providers) ·
     `F26` runtime variants (CLI/Docker/desktop/library) · `F27` workspace tools (terminal/browser/planner/tasks/usage/overview) ·
     `F28` automation setup page (drafts/test)
   - All sub-feature rows extracted to `00-feature-map-subfeatures.md` (898 lines) in this directory — this is the parity checklist.

3. **API layer** — `src/api/**` (113 files): cloud services (`conversation-service`, `git-service`, `secrets-service`,
   `settings-service`, `skills-service`, `sandbox-service`, `mcp-service`, `agent-profiles`, `meta-profiles`,
   `organization`, `provider-connections`, `setup-state`, `shared-conversation`, `device-flow`) + local agent-server
   services (`event-service`, `bash-service`, `acp-service`, `conversation-file-upload`, workspace/vscode) +
   `automation-service` (`/api/automation/*` on the local stack) + `src/api/mars/*` (DigitalOcean managed agents bridge).
   Typed client: `node_modules/@openhands/typescript-client@1.39.0` (`CloudClient` + agent-server clients).
   Transport: `callCloudProxy({ backend, method, path })` → direct or through the local agent server's `/api/cloud-proxy`
   (the same envelope-tunnel pattern our `:server` implements).

## In-flight deep dives (docs land beside this one)

- `04-ts-api-inventory.md` — subagent: every cloud path + runtime capability used by the canvas, auth/backend model,
  and the delta vs our Kotlin `OpenHandsApiService.kt` / `AgentServerApiService.kt`. (in progress)
- `05-ts-ui-ux-model.md` — subagent: app shell, screen-by-screen model, conversation workbench, cross-cutting UX. (in progress)
- Next after those: parity matrix = `F01–F28` rows × our app (Dashboard/Conversations/Conversation/NewTask/Settings),
  priority-ranked close-list.

## Immediate scale observation

Our app today: **5 screens**, a cloud-proxy, live REST + (proposed) WS channel. The canvas at today's main:
**30 routes / 28 feature families**, an automations subsystem, an extensions/MCP/skills/plugins/apps hub, LLM/agent
profile management, multi-backend management, device-flow login, and an 8-tab conversation workbench. The parity gap
is far larger than the original 47-row audit suggested; the feature map is now the authoritative target list, and the
close-list must be re-ranked against it.
