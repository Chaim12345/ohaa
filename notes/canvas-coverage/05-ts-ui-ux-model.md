# 05 — TypeScript Canvas: UI/UX Model (parity target)

Source snapshot (read-only): `/opt/openhands` at commit `38fe0c0` (2026-10-15),
package `@openhands/agent-canvas` v1.27.1 (`package.json:2-3`; `git
rev-parse` = `38fe0c0`). Every claim below is descriptive and cites `file:line`
inside that snapshot. This document models the current web canvas as the
parity target for an Android + KMP client; it does not prescribe or judge.

---

## 1. App shell & navigation

### 1.1 Route tree (authoritative)

`src/routes.ts:7-50` declares one root layout and a flat route table
(`satisfies RouteConfig`, `src/routes.ts:50`):

| Path | Route module | Notes |
|---|---|---|
| layout `root-layout.tsx` | `routes/root-layout.tsx` | wraps every route below (`src/routes.ts:8`) |
| `/` (index) | `routes/index-home.tsx` | loader redirect to pinned route, else renders Home (`src/routes.ts:9`) |
| `/conversations` | `routes/home.tsx` | the Home screen; conversation list lives in the sidebar (`src/routes.ts:10`) |
| `/conversations/:conversationId/panel` | `routes/conversation-panel.tsx` | full-screen mobile panel page (`src/routes.ts:11-14`) |
| `/conversations/:conversationId` | `routes/conversation.tsx` | conversation screen (`src/routes.ts:15`) |
| `/launch` | `routes/launch.tsx` | plugin deeplink launcher (`src/routes.ts:16`) |
| `/customize` | `routes/extensions-hub.tsx` | Customize hub: mobile hub, desktop redirect → `/mcp` (`src/routes.ts:17`) |
| `/skills` | `routes/skills-settings.tsx` | Skills library (`src/routes.ts:18`) |
| `/plugins` | `routes/skills-plugins.tsx` | Plugins marketplace (`src/routes.ts:19`) |
| `/apps` | `routes/canvas-extensions.tsx` | Canvas Extensions (Apps) (`src/routes.ts:20`) |
| `/extensions/:extensionName/*` | `routes/canvas-extension-page.tsx` | SPA pages contributed by extensions (`src/routes.ts:21`) |
| `/mcp` | `routes/mcp.tsx` | MCP servers (`src/routes.ts:22`) |
| `/settings` + children | `routes/settings.tsx` | layout with index + 9 children (`src/routes.ts:23-35`) |
| `/oauth/device/verify` | `routes/device-verify.tsx` | device-flow authorization page (`src/routes.ts:36`) |
| `/automations` | `routes/automations-list.tsx` | manifest-gated (`src/routes.ts:37`) |
| `/automations/git-sync` | `routes/automation-git-sync.tsx` | (`src/routes.ts:38`) |
| `/automations/templates` | `routes/automation-templates.tsx` | (`src/routes.ts:39`) |
| `/automations/new/:automationId` | `routes/automation-setup-route.tsx` | per-catalog-entry setup dialog (`src/routes.ts:40`) |
| `/automations/setup` | `routes/automation-setup-new.tsx` | new-automation form before a conversation exists (`src/routes.ts:41`) |
| `/automations/:automationId` | `routes/automation-detail.tsx` | (`src/routes.ts:42`) |
| `*` | `routes/not-found.tsx` | inside-shell 404 (`src/routes.ts:44`) |
| `/shared/conversations/:conversationId` | `routes/shared-conversation.tsx` | **outside** the app shell (`src/routes.ts:46-49`) |

Settings children (`src/routes.ts:25-35`): index (`settings-index.tsx`), `llm`,
`meta-llm`, `agent`, `agents`, `condenser`, `agent-context`, `verification`,
`app`, `secrets`.

### 1.2 Root layout composition

`routes/root-layout.tsx` renders (top→bottom):

- Shell container `data-testid="root-layout"`, title bar drag region, `Sidebar`
  (`root-layout.tsx:167-176`).
- Conditional mobile menu bar — omitted on `/conversations/:id`-style paths via
  regex `/^\/conversations\/[^/]+/` so chat headers own the chrome
  (`root-layout.tsx:156-160,179`).
- Lazy `AlertBanner` gated on config data (`maintenance_start_time`,
  `faulty_models`, `error_message`) (`root-layout.tsx:37-42,180-193`).
- `#root-outlet` scroll container with lazy `Outlet` (`root-layout.tsx:194-199`).
- `EnvironmentSwitchOverlay`, `CommandMenu`, `SuperAdminSetupGuide`, and
  onboarding (preview only) (`root-layout.tsx:202-209`).
- Providers: `ReactRouterNavigationProvider`,
  `CanvasExtensionsRuntimeProvider`, `SidebarMobileNavProvider`
  (`root-layout.tsx:164-166`).
- Language sync: `settings.language` → `i18n.changeLanguage` plus
  `useSyncDocumentLanguage()` (`root-layout.tsx:140-145`).

`AlertBanner` shows maintenance time formatting, faulty model list, error
message; dismissal is per-banner `updatedAt` stored at localStorage key
`alert_banner_dismissed_at` (`components/features/alerts/alert-banner.tsx:11-28,45-74`).

### 1.3 Sidebar rail (primary navigation)

`components/features/sidebar/sidebar-rail-body.tsx`:

- "New chat" → `/conversations` (`sidebar-rail-body.tsx:207-209`).
- Customize → `CUSTOMIZE_PATH` = `/customize` (`sidebar-rail-body.tsx:215-216`;
  `hooks/use-pinned-home-route.ts:17`).
- Automations link rendered only when `hasAutomationInterface()`; label comes
  from the interface manifest copy (`sidebar-rail-body.tsx:251-263`).
- One nav entry per enabled canvas-extension page (`sidebar-rail-body.tsx:264-277`).
- `SidebarConversationList` — the conversations list lives inside the rail
  (`sidebar-rail-body.tsx:279`).
- Settings link (expanded and collapsed variants; active when path starts with
  `/settings`) (`sidebar-rail-body.tsx:294-326`).
- Backend selector + Manage entry with hover-close timer
  (`sidebar-rail-body.tsx:330-373`); collapsed rail is rendered from
  `sidebar-rail-body.tsx:286-290` and the collapse toggle block from `:133-153`.
- Pin-as-home toggle per nav entry; pin state keyed per backend + org
  (`sidebar-rail-body.tsx:99-113`; `hooks/use-pinned-home-route.ts:24-30`).

Collapsed rail is used at ≤767 px (`SIDEBAR_RAIL_COLLAPSE_MAX_WIDTH`,
`hooks/use-breakpoint.ts:14`); the shell switches to a mobile layout at
≤1024 px (`MOBILE_BREAKPOINT`, `hooks/use-breakpoint.ts:3,26-42`); settings use
a compact form at ≤1023 px (`SETTINGS_COMPACT_MAX_WIDTH`,
`hooks/use-breakpoint.ts:6`).

### 1.4 Home-route pinning and `/` redirect

`hooks/use-pinned-home-route.ts`: storage key `oh:pinned-home-route`
(`:16`), only `/customize` and the automations list are pinnable — the latter
only while the manifest is admitted (`:41-45`). `readPinnedHomeRoute` is a
synchronous localStorage reader for loaders (`:57-77`). `routes/index-home.tsx`
redirects `/` to a valid pin, otherwise falls through to `home.tsx`
(`index-home.tsx:8-14`).

### 1.5 Settings navigation

`constants/settings-nav.tsx:15-83` — `OSS_NAV_ITEMS` (order): Agent
(`/settings/agents`, the agent-profile library; `:22-34`), LLM (`/settings/llm`,
`:35-40`), Meta LLM (`/settings/meta-llm`, `:41-46`), Condenser
(`/settings/condenser`, `:47-52`), Agent context (`/settings/agent-context`,
`:53-65`), Verification (`/settings/verification`, `:66-71`), Application
(`/settings/app`, `:72-77`), Secrets (`/settings/secrets`, `:78-83`).
Locked-to-cloud deployments list only `/settings/app`
(`LOCKED_CLOUD_SETTINGS_NAV_PATH`, `constants/settings-nav.tsx:83`; filtered in
`hooks/use-settings-nav-items.ts:26-31`).

`routes/settings.tsx` performs the locked-cloud redirect and hides
feature-flag-disabled pages in a `clientLoader` (`:38-53`), resolves the
current section title/subtitle for the header (`:60-80`), and composes the
`SettingsLayout` (`:1-16`; `components/features/settings/settings-layout.tsx:23-27`).

### 1.6 Command menu (global palette)

- Opens on ⌘K / Ctrl+K from anywhere (`components/features/command-menu/command-menu.tsx:74-86`),
  closes on Escape (`:89`), restores focus to the previously focused element on
  close (`:100-120`), ArrowUp/Down + Enter navigation within the list
  (`:168-190`).
- Trigger button sits in the sidebar rail (`command-menu-trigger.tsx:17-52`).
- Groups: `navigation`, `settings`, `actions`
  (`command-menu-items.tsx:102-115`).
- Items (`command-menu-items.tsx:119-250`): new-chat; customize; automations
  (only with manifest); mcp; settings; agent-settings; llm-settings;
  meta-llm-settings; condenser-settings; agent-context-settings;
  verification-settings; app-settings; secrets-settings; toggle-sidebar action.
  Locked-to-cloud filters out non-Application entries
  (`:255-262`).

### 1.7 Onboarding

Mounted from the root layout in preview mode only
(`root-layout.tsx:209`); preview step via `?previewOnboardingStep` 0-3
(`components/features/onboarding/onboarding-host.tsx:41,68`). Steps directory:
`check-backend-step.tsx`, `setup-llm-step.tsx`, `choose-agent-step.tsx`,
`setup-acp-secrets-step.tsx`, `say-hello-step.tsx`
(`components/features/onboarding/steps/`), rendered through
`onboarding-modal.tsx` with `onboarding-progress-bar.tsx`.

---

## 2. Screen inventory

### 2.1 Home / landing (`/conversations`)

`routes/home.tsx` composes the LLM-not-configured banner, the chat launcher,
and the onboarding host (unless preview) (`home.tsx:3-26`). The launcher
`components/features/home/home-chat-launcher.tsx` is the main surface:

- Launcher mode state `"code" | "automate"` (default `"code"`), toggled by
  `HomeLauncherModeToggle` (`home-chat-launcher.tsx:70-72,297-299`;
  `home-launcher-mode-toggle.tsx:7-11,107` carries the animated toggle).
- `CustomChatInput` submits; a successful create passes entry point
  `"home_chat_launcher"` (`home-chat-launcher.tsx:85,140-143`).
- Workspace/repo are optional; local backends pick workspace mode and repository
  (`home-chat-launcher.tsx:110-171`), with `OpenWorkspaceDialog` /
  `OpenRepositoryDialog` (`:48-49,381-397`).
- Explicit plugin selection rides on top of ambient plugins
  (`home-chat-launcher.tsx:167-171`).
- Attachments are deferred: an attachment snapshot is taken, the conversation is
  created first, then images/files are sent (`home-chat-launcher.tsx:129-140,219-239`).
- Automate mode lists `RunningAutomationsList` (`home-chat-launcher.tsx:373`;
  `home-launcher-mode-toggle.tsx` for mode copy).

### 2.2 Conversations list (sidebar panel)

`components/features/conversation-panel/conversation-panel.tsx` is the full
list model:

- Pinned section, group folders, tag facets, automation-name facets, archived
  toggle, compact rows, delete/archive/stop confirmations, edit-tags modal,
  delete-all (`conversation-panel.tsx:11-61,93-198`).
- Preferences store persists show-archived, tag metadata, automation filter
  mode, selected tag facets, group order (`:112-158`).
- "Load more" driver: floor-based single-click driver with
  `MAX_PAGES_PER_LOAD_MORE_CLICK`, hiding in compact mode
  (`:576-679`).
- Older-conversation cutoff partitioning helpers
  (`:44-59`).
- List refetch cadence: paginated conversations query uses
  `refetchInterval: 30_000`, disabled on the automations route
  (`hooks/query/use-paginated-conversations.ts:56-57`); the search API cursors
  are `/api/conversations/search` (`conversation-panel.tsx:585`).
- New-thread pickers: cloud (`cloud-new-conversation-menu.tsx:169`) and local
  (`local-new-conversation-menu.tsx:119`) variants.

### 2.3 Launch deeplink (`/launch`)

Parses a base64 `plugins` query param, validates the plugin list, and has three
error types `no_plugins | invalid_format | creation_failed`
(`routes/launch.tsx:9-82`). On success it opens `PluginLaunchModal`
(`:196`) and creates a conversation with entry point `"launch_deeplink"`
(`:156-159`); errors render `ErrorDisplay` (`:85-111,177-191`).

### 2.4 Conversation screen

- Route module `routes/conversation.tsx` resolves backend pinning
  (`?backend=`/`?org=` from URL) to avoid cross-backend fetches
  (`conversation.tsx:55-95`), mounts `WebSocketProviderWrapper`
  (`:36`), `ConversationMain` (`:30`), the mobile panel page (`:31`), task
  polling controller (`:27`), `SecretRequestModal` (`:29`), and
  `useCloudSandboxAutoResume` (`:39`).
- `routes/conversation-panel.tsx` re-exports the same component purely to give
  `/conversations/:id/panel` a distinct route id (`conversation-panel.tsx:1-4`).
- Mobile panel behavior `hooks/use-conversation-panel-route.ts`: at phone width
  the drawer is the full-screen `/panel` page; `open_tab` / `navigate_to_file`
  reveals route to it (`:20-26`); a `/panel` URL on desktop falls back to the
  split layout with the drawer open (`:29-38`); returns whether to render the
  mobile page (`:40`). Rendered by
  `conversation-mobile-panel-page.tsx` (`conversation.tsx:31`).
- `conversation-main.tsx`: chat + resizable right panel via
  `useResizablePanels` with default 50%, min 30%, max 80%, storage key
  `desktop-layout-panel-width` (`conversation-main.tsx:116-120`;
  `hooks/use-resizable-panels.ts:15-18`); automation-setup mode forces the
  panel open (`:109-133`) and supports a mobile form/view toggle
  (`:200,343-395`).

### 2.5 Conversation tabs (right drawer)

- Tab ids: `files, commits, planner, tasklist, terminal, browser, usage`
  (`conversation-tabs/conversation-tab-ids.ts:17-25`), each with an i18n label
  (`:12-25`) and shared panel id `conversation-tab-panel` (`:10`).
- Strip behavior in `conversation-tabs.tsx`: overflow measurement and an
  overflow menu for narrow drawers (`:358-430`), roving tab index for keyboard
  navigation (`:264-282,396`), unpinned-tabs persistence (`:168-178`),
  `aria-label` on the strip (`:365`), per-tab `aria-controls`
  (`conversation-tab-ids.ts:5-10`).
- Planner Build bar: shown when planner tab active and not compact
  (`conversation-tabs.tsx:434-450`), disabled while agent running or no plan
  (`:178`); build handler `useHandleBuildPlanClick` (`:53`); tooltip ⌘↩.
- VSCode link in the drawer (`:19,427-430`; `drawer-vscode-link.tsx`).
- Content routing: lazy imports for `files-tab`, `commits-tab`,
  `browser-tab`, `planner-tab`, `task-list-tab`, `usage-tab`, and the terminal
  component, mapped per tab id
  (`conversation-tab-content/conversation-tab-content.tsx:14-29`).
- Per-tab screens:
  - Files: tree with persisted width constants
    (`routes/files-tab.tsx` imports
    `files-tab-tree.constants.ts`, `conversation-local-storage.ts`).
  - Commits: newest-first commit history, expandable per-file diffs, rendered
    only when the server supports the commits API (`routes/commits-tab.tsx:2-9`).
  - Browser: `BrowserPanel` (`routes/browser-tab.tsx:3-7`).
  - Planner: PLAN.md reader with its own scroll container; prefers the planner
    helper conversation id (`routes/planner-tab.tsx:1-25`).
  - Task list: hidden unless a task list exists (`hooks/use-task-list.ts` via
    `routes/task-list-tab.tsx:5-17`); empty state `COMMON$NO_TASKS` (`:13-17`);
    in-progress rows highlighted (`:25-33`).
  - Usage: `UsagePanel` (`routes/usage-tab.tsx:1-5`).
  - Terminal: `components/features/terminal/terminal`
    (`conversation-tab-content.tsx:20`).

### 2.6 Automations suite (manifest-gated)

All automations pages 404 without an admitted interface manifest
(`manifests/automation-interface.ts`, `hasAutomationInterface()`), and the
sidebar/command-menu entries disappear (`sidebar-rail-body.tsx:251-263`;
`command-menu-items.tsx:134-153`).

- **List** `routes/automations-list.tsx`: search, dashboard mode derived from the
  manifest, overview tiles, status/trigger/createdBy filters, sort, persisted
  view toggle, active/inactive groups, saved-drafts group (resume/test/delete),
  run-now, export/import `.automation.json`, git-sync button gated on
  `canManage`, load-more.
- **Detail** `routes/automation-detail.tsx`: header actions toggle/edit/delete/
  export/download-tarball/run-now, `DisabledReasonBanner`, Prompt/Script +
  Configuration + Plugins + Activity + ActivityLog sections with `?run=`
  highlighting, "runs as" creator, permission model `canManage || isOwner`.
- **Templates** `routes/automation-templates.tsx`: manifest-gated
  `RecommendedAutomationsLauncher` + search.
- **Data model** `types/automation.ts`: triggers cron/schedule/event + filter,
  state `ACTIVE/INACTIVE/DRAFT`, `disabled_reason/detail`,
  `agent_profile_id`, timeout, `preset_metadata`; runs carry phases/cost/
  `bash_command_id`.
- **Permissions** `hooks/use-automation-permissions.ts`: `view_automations` /
  `manage_automations` from cloud `/me` (`:13-14`), local always both
  (`:59-61`), loading hides write controls (`:21-27`), creator check
  `useIsAutomationOwner` (`:91-102`), creator-filter id only for cloud team
  workspaces (`:110-118`).

**Git sync** `routes/automation-git-sync.tsx` (org-level, `canManage` only,
`:54-58`): health gate (`:63-68`), status polling 3 s while running / 15 s idle
(`:27-28`), follow cycles not triggered here (`:82-87`), completion detection by
newer timestamps (`:92-102`), 30 s blind-cycle window (`:32,107-114`), not-found
vs error states with 404 → unsupported (`:162-174`), "sync now" follows the
activity row instead of a toast (`:176-193`), page layout with
`GitSyncOverviewSection` + `GitSyncConfigForm` (`:195-232`).

**Setup form**

- `routes/automation-setup-new.tsx`: opens with the agent drawer hidden
  (`:25-30`), loads a blank draft / existing automation / server draft
  (`:56-94`), title from draft name (`:96`), docks `InteractiveChatBox` via a
  portal into a toolbar-less layout; sending the prompt is what creates the
  conversation (`:147-165`).
- `routes/automation-setup-route.tsx`: per catalog entry; 404 when id unknown
  or manifest not admitted (`:18-28`); dismiss lands on the list for cold deep
  links, else back (`:38-44`); renders `SetupDialog` (`:46`).
- `components/features/automations/setup/automation-setup-panel.tsx` —
  3 560-line form: field render order `kind → name → prompt → … → timeoutSeconds`
  (`:164-190`), kind = prompt | custom code with crossfade
  (`:1960-2003,2385-2418`), starter Python template (`:322-345`), cron
  construction for once/hourly/weekdays/weekly/custom (`:307-319,620-634`),
  custom webhook state incl. signature schemes
  hmac_sha256_hex/standard_webhooks/slack_v0 (`:191-215`), draft status card
  with validity/Test/View-test-runs (`:456-512`), test-runs page (`:558-617`),
  save state machine idle/saving/saved/error + "saved just now"/"unsaved
  changes" text (`:695-698,1678-1688`), Save-draft/Test/Create buttons
  (`:1795-1842`), agent-streamed field highlighting (`:1174`).
- `hooks/use-start-automation-setup.ts`: entry points
  `automations_add | automation_edit | automation_draft_resume` (`:44-63`),
  navigates to `/conversations/{id}` on success and hands the form session over
  (`:64-75`). `hooks/use-open-automation-editor.ts` seeds the session and opens
  `/automations/setup?automationId=…` (`:19-30`).

### 2.7 Customize hub and its children

- `/customize` (`extensions-hub.tsx`): mobile → `ExtensionsMobileHub`
  (`:12-14`), desktop → redirect to `/mcp` (`:16`).
- Customize nav (`components/features/skills/extensions-navigation.tsx:33-69`):
  MCP Servers `/mcp`, Skills `/skills`, Plugins `/plugins`, Apps `/apps`.
- **Skills** `routes/skills-settings.tsx`: title/description
  (`:144-151`), "new conversation" notice (`:154-157`), Add button (`:167`),
  empty state (`:184-188`), result summary + clear-filters (`:213-225`), no-match
  (`:232-236`); cards expose name/source/description with copy-path action
  (`components/features/skills/skill-card.tsx:67-131`); facet rail/filters
  (`skill-facet-rail.tsx`, `skill-filters-modal.tsx`, `skills-toolbar.tsx`).
- **Plugins** `routes/skills-plugins.tsx`: marketplace + installed + local
  merge (`:44-62`), install/update by source (`:92-121`), title (`:138-144`),
  Add (`:155`), empty/no-match (`:172-195`).
- **Apps (Canvas Extensions)** `routes/canvas-extensions.tsx`: enable/uninstall
  confirmations (`:81-83`), title/description (`:99-103`), build-docs link
  (`:111`), Add button (`:125`), trust notice (`:130`), installed section
  (`:134`), unsupported state (`:140`), error + retry (`:166-174`), empty
  (`:206`), enable action (`:223`). Runtime
  `components/features/canvas-extensions/canvas-extensions-runtime.tsx`:
  host-API version constant (`:11`), page href builder and segment validation
  (`:50-85`), module loading via blobs + `mountCanvasExtensionAppView`
  (`:6-7,99`), only enabled extensions registered (`:120-122`), error map +
  activating state (`:29-33`). `routes/canvas-extension-page.tsx` renders a
  contributed page or an unavailable state (`:8-95`).
- **MCP** `routes/mcp.tsx`: page + search/filter of installed servers
  (`:42-69`), edit (`:77-79`), enable/disable toggle keeps secrets
  (`:81-88`), Add-custom (`:133-137`), Installed section (`:154-156`),
  marketplace install modal (`:174-179`), custom server editor (`:183-187`).
  Component kit (`components/features/mcp-page/`): installed-server cards,
  marketplace cards, health, toolbar, native-integration panel,
  save-as-secret toggle, section filter.
- `/settings/mcp-settings.tsx` is a backwards-compat re-export of the same page
  (`mcp-settings.tsx:1-10`).

### 2.8 Settings pages (index + 9 children)

- **Index** `routes/settings-index.tsx`: mobile shows `SettingsMobileHub`
  (`:18-19`); desktop redirects to the first available page, or to
  `/settings/app` when locked-to-cloud (`:24-29`).
- **Agent profiles / Agent** `/settings/agents`
  (`routes/agent-profiles-settings.tsx:1-15`): a library of named agent setups
  reusing the Agent settings form as editor; backend-agnostic.
- **Agent settings** `/settings/agent` (`routes/agent-settings.tsx`, 1 405
  lines): agent type OpenHands | ACP (`:827-835`), system-prompt modes
  standard/append/custom with hints (`:863-943`), tools
  standard/choose + load-failure retry (`:963-1055`), MCP
  all/choose/none + dangling warnings (`:1055-1122`), secrets scope
  (`:1130-1183`), preset custom command (`:1194-1263`), model
  default/custom + discovery states (`:1289-1382`), `AcpCredentialsSection`
  (`:9,1272`).
- **LLM** `/settings/llm` (`routes/llm-settings.tsx`): provider connections
  (`:160-165`), auth type api-key|subscription (`:173-177,387-408`),
  subscription model picker (`:410-445`), basic/advanced forms with custom model
  + base URL (`:448-518`), OpenHands API-key help (`:105-120`), OS-payload
  builder for save (`:560-605`), rendered via `SdkSectionPage` (`:624`).
- **Meta LLM** `/settings/meta-llm`: cloud without org shows an unsupported
  message; otherwise `MetaLlmSettingsView` (`routes/meta-llm-settings.tsx:20-31`);
  meta-profiles feed the `route_task_to_model` tool (doc comment `:6-13`).
- **Condenser** `/settings/condenser`: `SdkSectionPage` over
  `agent_settings.condenser` (`routes/condenser-settings.tsx:1-14`).
- **Agent context** `/settings/agent-context`: `SdkSectionPage` over
  `agent_settings.agent_context` (today: persistent memory)
  (`routes/agent-context-settings.tsx:1-14`).
- **Verification** `/settings/verification`: merges
  `conversation_settings.verification` + `agent_settings.verification`,
  excluding deprecated duplicated fields (`confirmation_mode`,
  `security_analyzer`) (`routes/verification-settings.tsx:14-42`).
- **Application** `/settings/app` (`routes/app-settings.tsx`): anonymous
  analytics toggle (`:229`), sound notifications (`:238`), conversation-title
  generation + model/profile selection (`:245-273`), git username/email with
  save-changes/saving text (`:282-317`).
- **Secrets** `/settings/secrets` (`routes/secrets-settings.tsx`):
  add/edit secret modal (`:99-100`), table with name/description/actions
  (`:168-181`), delete confirmation (`:222`), empty state (`:149-152`).

### 2.9 Device verify (`/oauth/device/verify`)

`routes/device-verify.tsx`: reads `user_code` from the query (`:19`), posts to
`/oauth/device/verify-authenticated` with `credentials: include` (`:25-32`);
success → success message, non-ok → failed, throw → error occurred
(`:34-49`); processing spinner (`:128-139`); confirmation card with code,
security notice, Cancel (`window.close`) and Authorize (`:141-188`); manual
code-entry form when authed without a code (`:190-227`); sign-in-required state
(`:240-248`).

### 2.10 Shared conversation (public, outside shell)

`routes/shared-conversation.tsx`: fetches conversation + events
(`:34-46`), flattens pages and reconstructs UI events via `handleEventForUI`
(`:52-67`), infinite scroll at the bottom (`:70-74`), header shows title,
branch, repository, model, creator (cloud member lookup with raw-id fallback)
(`:23-27,109-141`), read-only `Messages` or no-history state (`:151-161`).

### 2.11 Not found

`routes/not-found.tsx`: catch-all rendered inside the root layout so the
sidebar stays usable (`:6-10`), icon + title + message, "Home" back button to
`/` (`:14-31`).

---

## 3. Conversation UX model

### 3.1 Chat stream

- Rendering `components/conversation-events/chat/messages.tsx`: receives UI
  events (actions replaced by observations) + full history (`:17-22`); builds
  `actionById` map (`:41-47`), model-message anchors (`:50-65`), folds
  consecutive action/observation runs into collapsible groups
  (`:70-79` via `groupEvents`), renders confirmation buttons after the stream
  (`:17`, `:80+`). Task tracking lives under
  `components/conversation-events/chat/task-tracking/`.
- Chat suggestions on the empty state from `SUGGESTIONS.repo`, titled
  `LANDING$TITLE` (`components/features/chat/chat-suggestions.tsx:5,20-33`).

### 3.2 Composer

- `components/features/chat/interactive-chat-box.tsx` chains interceptors
  around submit: model → goal → plan-mode → btw → store callback
  (`:55-72`). Interceptors:
  - `/btw <question>` routes through the ask-agent side channel
    (`hooks/chat/use-btw-interceptor.ts:9-46`);
  - `/plan [task]` / `/code [task]` toggle conversation mode or
    switch-and-send, gated on running agents and socket state
    (`hooks/chat/use-plan-mode-interceptor.ts:16-82`); `/plan` uses the
    planner socket, `/code` gates only the main socket (`:23-27,59-82`);
  - model and goal interceptors (`use-model-interceptor.ts`,
    `use-goal-interceptor.ts`; bare `/goal` → `GOAL$OBJECTIVE_REQUIRED`
    at `use-goal-interceptor.ts:47`).
- Slash-command autocomplete (`hooks/chat/use-slash-command.ts`): built-in
  commands from `BUILT_IN_COMMANDS` + skills + `/model <profile>` entries
  (`:51-96`); `/new` is cloud-only (`:58-60`); ArrowDown/Up/Enter/Tab/Escape
  handling (`:276-298`). Built-ins: `/new`, `/btw` (`utils/constants.ts:54-75`);
  `MODEL_COMMAND = "/model"` (`constants.ts:42`).
- Code/Plan toggle in the composer row
  (`components/features/chat/components/chat-input-actions.tsx:349-398`);
  confirmation-mode lock badge (`confirmation-mode-enabled.tsx:8-26`).
- File attachments: files are uploaded before send and appended as a
  "Files uploaded" block to the prompt
  (`chat-interface.tsx:385-394`); optimistic enqueue follows
  (`:396-410`).

### 3.3 Sending: optimistic messages and queue

- `stores/optimistic-user-message-store.ts`: status `sending | error`
  (`:7`), `PENDING_MESSAGE_TIMEOUT_MS = 150_000` (`:18`), watchdog flips a
  still-sending entry to error (`:65-68,154-162`), submit carries
  `afterTimestamp` for echo matching (`:40,144-148`), echo matching prefers
  sending attempts but accepts a late echo on errored ones, requires
  `afterTimestamp` ordering + content match (`:204-213`), retry re-arms
  sending with a fresh timestamp (`:76-78,182-184`), and pending messages can
  be reassigned between conversations (`:90,234`).
- Chat interface consumes the queue: `pendingMessages` selector
  (`chat-interface.tsx:116-118`), renders `PendingUserMessages` (`:43`),
  `handleSubmit` enqueues and scrolls to bottom, then calls
  `createChatMessage`; failure marks the entry errored with a retry link
  (`:396-428`).
- Home launch defers attachments through a dedicated pending store and flushes
  them after task readiness (`home-chat-launcher.tsx:129-140`;
  `utils/flush-pending-task-attachments`; `use-task-polling.ts:160`).

### 3.4 Confirmations, risk alerts, shortcuts

- `components/shared/buttons/conversation-confirmation-buttons.tsx`: only
  shown when `curAgentState === AWAITING_USER_CONFIRMATION` and the latest
  agent event has not been answered (`:30-40,104-110`); submit-once guard via
  `submittedEventIds` (`:48-51`); highest-risk action renders `RiskAlert`
  severity high (`:113-127`; `shared/risk-alert.tsx:17-34`); buttons are
  reject + confirm tooltips (`:129-143`).
- Keyboard: Continue = ⌘/Ctrl+Enter, Reject = Shift+⌘/Ctrl+Backspace, ignored
  once submitted and on key repeat (`:64-101`).

### 3.5 Error banner and read-only states

- `components/features/chat/error-message-banner.tsx`: data-testids for banner,
  header, content, reauth, toggle (view more/less), retry, copy, dismiss
  (`:101-206`); reauth shows only for ACP credential updates
  (`:142-149`); retry forwards the message retry (`:173-180`).
- Chat interface read-only gating: cloud conversations with sandbox
  MISSING/ERROR are read-only (`chat-interface.tsx:138`); sending is blocked
  in a resumed conversation without a usable LLM (`:146`); archived
  conversations detected via `useIsArchivedConversation` (`:28`).
- Chat status pill `chat-status-indicator.tsx` animates status text/color
  transitions (`:10-47`), mounted at `chat-interface.tsx:699`.

### 3.6 Agent state machine

- `types/agent-state.tsx`: 13 values — LOADING, INIT, RUNNING,
  AWAITING_USER_INPUT, PAUSED, STOPPED, FINISHED, REJECTED, ERROR,
  RATE_LIMITED, AWAITING_USER_CONFIRMATION, USER_CONFIRMED, USER_REJECTED
  (`:2-14`); runtime starting = INIT/LOADING (`:17`); inactive list (`:19+`).
- Server → UI mapping in `hooks/use-agent-state.ts`: IDLE →
  AWAITING_USER_INPUT, RUNNING → RUNNING, PAUSED → PAUSED (`:24-25`),
  WAITING_FOR_CONFIRMATION → AWAITING_USER_CONFIRMATION, FINISHED → FINISHED,
  ERROR → ERROR, STUCK → ERROR (`:12-34`). Effective status = live socket
  value, else route conversation value (`:58-73`); planning agent state
  exposed separately (`:100`).

### 3.7 Event pipeline (REST + WebSocket)

- `contexts/conversation-websocket-context.tsx`:
  - Socket URL `/sockets/session/{id}` frame router: `item_started` / `delta`
    / `item_aborted` update streaming slots only; `durable` and `transient`
    frames are unwrapped into normal events; a `sync` frame needs nothing
    (`:158-208`).
  - Resume cursors: one per main and planning socket, created via
    `createSeqCursor` (`:275-277`); `after_seq` replaces legacy
    resend_mode/after_timestamp (`:1142-1151`); planning socket replays from
    its own cursor since its history is not REST-preloaded (`:1195-1200`).
  - REST history preload seeds events and gates socket startup until
    `isPending` is false (`:375-395,473-485`).
  - Held messages: sends for conversations without a runtime (sandbox
    PAUSED/STARTING) are queued per target conversation, deduped, and flushed
    when the socket opens (`:100-108,1284-1314,1445-1463`).
- `stores/use-event-store.ts`: `events` + `uiEvents` (`:62`), sort by
  timestamp with missing-timestamp events last (`:25-38`), incremental sort
  only when out-of-order (`:42-56`), upsert dedupe + re-sort (`:71-76`), slots
  are UI-only and not deduped against `events` (`:95-109`), streaming slot
  lifecycle handlers (`:208-240`).
- Reconnect stack:
  - `hooks/use-websocket.ts`: exponential backoff 1 s → 30 s cap (`:25-27`),
    ≤30% jitter (`:133`), reconnect gated per socket instance and
    `maxAttempts` (`:118-142`), handshake watchdog (`:3`).
  - `hooks/use-page-visibility-reconnect.ts`: 3 s hide debounce (`:11,78`),
    freeze (Chromium) / pagehide / pageshow listeners, immediate close on
    freeze (`:97-115`), avoiding stale retry bursts (`:43`).
- Cloud sandbox auto-resume `hooks/mutation/use-cloud-sandbox-auto-resume.ts`:
  10 s retry delay (`:15`), only for `sandbox_status === "PAUSED"` with a
  resume key (`:81-99`), per-attempt dedupe (`:41-66,100-112`), suppression
  utilities imported (`:8-10`).

### 3.8 Task bootstrap route (`task-{uuid}`)

`hooks/query/use-task-polling.ts`: identifies ids prefixed `task-` (`:66-68`),
polls the V1 start-task API every 3 s until READY/ERROR (`:47-88`), the
controller flushes pending attachments and navigates to the real conversation
id on READY (`:116-175`, `:160,173`).

### 3.9 Scrollback and reconnection UX

- Lazy older-event pagination in `chat-interface.tsx`: triggers on scroll near
  top, wheel at `scrollTop=0`, and when content is shorter than the viewport
  (`:207-268`); scroll geometry is captured and restored after prepending so
  the viewport does not jump (`:217-227,437-439`); pending history discarded on
  conversation switch (`:236-238`).
- `ScrollToBottomButton` (`:31`) and `useScrollToBottom` (`:18`); auto-scroll
  suppressed when the user is scrolled up, re-armed on send (`:414-416`).

---

## 4. Cross-cutting concerns

### 4.1 Themes

- Registry `themes/color-theme/definitions/index.ts`: five themes —
  `openhands-deepsea`, `openhands-neutral` (default), `openhands-neo`,
  `light-plus`, `solarized-light` (`:14-24`); each definition carries `label`
  and `appearance` (`themes/color-theme/types.ts:13-14`); `AVAILABLE_COLOR_THEMES`
  is derived for pickers (`definitions/index.ts:24-26`).
- Runtime `themes/color-theme/runtime.ts`: persisted key in localStorage
  (`:8-24`), apply+persist atomic (`:40-43`), `color-scheme` on the root
  (`:120`), bootstrap script that pre-applies the stored theme before React
  (`:128-133`), scoped CSS emitted for the agent-server UI wrapper and HeroUI
  dark maps (`:53-99`).
- Public entrypoint re-exports definitions/runtime
  (`themes/color-themes.ts:10-31`).

### 4.2 Modals, drawers, focus

- `components/shared/modals/modal-backdrop.tsx`: portal to `document.body`
  (`:51`), `role="dialog" aria-modal="true"` with `tabIndex=-1` and shared
  focus hook `useModalFocus` (`:27,56-62`), Escape close (`:30-37`),
  backdrop-click close only when the target is the backdrop (`:39-43`),
  `elevated` z-layer for stacked modals (`:15,64`).
- `hooks/use-close-on-escape.ts` is the shared document-level Escape handler
  (`:26`); many menus/popovers use it or their own handler (e.g.
  `conversation-name-context-menu.tsx:161`, `kebab-menu.tsx:90`,
  `image-lightbox.tsx:28`).

### 4.3 Toasts

- `utils/custom-toast-handlers.tsx`: top-right position, themed surface style
  spec TN-001 (`:20-35`), icon+message content component with ResizeObserver
  alignment (`:38-90`), exports `displayErrorToast` (`:104`),
  `displayApiErrorToast` (`:120`), `displaySuccessToast` (`:129`),
  `displaySuccessToastWithLink` (`:139`), `displayWarningToast` (`:164`).
- Duration is reading-speed based, clamped to [min, 10 s]
  (`utils/toast-duration.ts:12-30`).

### 4.4 Keyboard shortcuts (observed)

| Shortcut | Action | Cite |
|---|---|---|
| ⌘K / Ctrl+K | open command menu | `command-menu.tsx:74-86` |
| ⌘/Ctrl+Enter | Build plan from chat / Continue confirmation | `chat-interface.tsx:157-181`; `conversation-confirmation-buttons.tsx:83-88` |
| ⌘↩ | planner tab Build bar (tooltip) | `conversation-tabs.tsx:434-450` |
| Shift+⌘/Ctrl+Backspace | Reject confirmation | `conversation-confirmation-buttons.tsx:72-81` |
| Escape | close modals/menus (shared hook) | `modal-backdrop.tsx:30-37`; `use-close-on-escape.ts:26` |
| ↑/↓ + Enter | command-menu list navigation | `command-menu.tsx:168-190` |

### 4.5 Command menu (described in 1.6) and alert banner (1.2)

### 4.6 Optimistic patterns

Two stores embody the optimistic pattern: the pending user-message queue with
a 150 s watchdog and content+timestamp echo matching (3.3), and deferred home
attachments flushed by the task poller (3.8). Pending UI is rendered inline in
the chat stream before the server echo (`chat-interface.tsx:43,116-118`).

### 4.7 Accessibility (recent commits, evidence from `git log`)

`git log` on the snapshot contains these a11y-focused commits (hashes +
subjects): `8b9823a` keyboard-accessible settings switches; `3e686de` modal
focus moved into and trapped within `ModalBackdrop`; `724c5a3` agent profile
row menu keyboard-operable; `ff1a1d6` composer More-actions menu closes on
Escape; `9f05599` tools/status/tabs menus close on Escape; `790e4b2`
icon-only composer and diff-viewer buttons named; `3de2aff` menus/popovers
Escape + focus return; `321d492` LLM profile row menu keyboard-operable;
`9cbf09d` single Settings main landmark, Customize `aria-current`, synced
`html lang`. Landmarks/labels in code: `aria-label` on the tab strip
(`conversation-tabs.tsx:365`), `aria-controls` per tab
(`conversation-tab-ids.ts:5-10`), `role="dialog"`/`aria-modal`
(`modal-backdrop.tsx:57-60`), `aria-label` on icon-only buttons
(`command-menu-trigger.tsx:33-34`; `sidebar-rail-body.tsx:303,317`).

### 4.8 Internationalization

- 16 languages: en, ja, zh-CN, zh-TW, ko-KR, no, ar, de, fr, it, pt, es, ca,
  tr, uk, id (`i18n/index.ts:15-38`); fallback `en`, namespace `openhands`
  (`:43-49`); resources fetched at runtime from
  `/locales/{{lng}}/{{ns}}.json` via `i18next-http-backend` (`:50-54`);
  interpolation escaping disabled because every string renders through React
  (`:55-62`); a proxy routes `t`/`exists` through the active instance
  (`:95-133`). Embedded `translationResources` exist for library consumers
  (`i18n/resources.ts:1-40`). Language sync from settings in the root layout
  (`root-layout.tsx:140-145`).

---

## 5. Parity deltas (web canvas vs. Android 5-screen set)

Grouped as requested: **NEW screens**, **sub-features within existing
screens**, **conversation tabs**, **settings pages**, **platform features**.
Each entry mirrors a web capability that exists in this snapshot.

### 5.1 NEW screens (not in Dashboard / Conversations / Conversation / NewTask / Settings)

1. **Automations suite** — list, detail, templates, setup (new + per-entry),
   git-sync dashboard (`src/routes.ts:37-42`; §2.6).
2. **Customize hub** — `/customize` plus children: Skills, Plugins, Apps,
   MCP (`extensions-navigation.tsx:33-69`; §2.7).
3. **Canvas extension pages** — `/extensions/:extensionName/*` runtime hosting
   third-party SPA pages (`canvas-extensions-runtime.tsx:50-90`).
4. **Launch deeplink** — `/launch?plugins=<base64>` validation + plugin modal
   (`launch.tsx:9-82`).
5. **OAuth device verification** — `/oauth/device/verify` confirmation and
   manual code entry (`device-verify.tsx`).
6. **Public shared conversation** — `/shared/conversations/:id`, read-only,
   outside the shell (`shared-conversation.tsx`; `src/routes.ts:46-49`).
7. **Not-found screen** — in-shell catch-all (`not-found.tsx`).
8. **Settings shell** — index + 9 sub-pages (§5.4).
9. **Conversation mobile panel route** — `/conversations/:id/panel`
   (`conversation-panel.tsx:1-4`; `use-conversation-panel-route.ts:13-46`).

### 5.2 Sub-features within the 5 existing screens

- **Home/Dashboard**: code↔automate launcher mode toggle
  (`home-launcher-mode-toggle.tsx`); workspace/repo pickers
  (`home-chat-launcher.tsx:48-49,381-397`); explicit plugin picker
  (`:167-171`); deferred attachments on cloud create (`:129-140,219-239`);
  running-automations list (`:373`); LLM-not-configured banner
  (`home.tsx:21`); onboarding host (`home.tsx:26`).
- **Conversations list (sidebar)**: pinned section, group folders, tag facets,
  automation-name filter, archived toggle, compact rows, saved preferences,
  delete/archive/stop flows, load-more driver, 30 s refetch, no visible
  free-text search field (search cursors on `/api/conversations/search`)
  (`conversation-panel.tsx:11-61,112-158,576-679,585`;
  `use-paginated-conversations.ts:56-57`).
- **Conversation**: resizable drawer (50/30/80, persisted) and mobile
  full-screen panel; chat empty-state suggestions; lazy older-event pagination
  with scroll preservation; optimistic sends + 150 s watchdog + retry;
  confirmation buttons with shortcuts and high-risk alert; error banner with
  retry/reauth/copy/dismiss; read-only states (archived, sandbox MISSING/ERROR,
  no-LLM resumed); agent-state pill; `task-{uuid}` bootstrap polling; held
  messages for paused sandboxes; cloud auto-resume; WS backoff + visibility
  reconnect; scroll-to-bottom button (§3).
- **NewTask**: the web equivalent is the home launcher + `/launch`, plus the
  automation-setup new form (docked composer creates the conversation on first
  send) (`automation-setup-new.tsx:25-30,147-165`).
- **Settings**: see §5.4; also locked-to-cloud behavior and feature-flag page
  hiding (`settings.tsx:38-53`; `settings-nav.tsx:83`).

### 5.3 Conversation tabs

Web tabs (in strip order semantics, not fixed order): **files, commits,
planner, tasklist (only with a task list), terminal, browser, usage**
(`conversation-tab-ids.ts:17-25`; `conversation-tab-content.tsx:14-29`).
Extras: overflow menu + roving tabindex (`conversation-tabs.tsx:264-282,358-430`),
planner Build bar with ⌘↩ (`:434-450`), VSCode drawer link (`:19,427-430`),
per-tab persisted width (files) and lazy loading.

### 5.4 Settings pages

Index (hub/redirect), Agent profiles library, Agent form, LLM (auth type,
provider connections, subscription, custom model/base URL), Meta LLM
(route_task_to_model profiles), Condenser, Agent context (memory),
Verification (confirmation mode, analyzer), Application (analytics, sound,
title generation, git identity), Secrets (`src/routes.ts:25-35`; §2.8).

### 5.5 Platform features

- Global command menu with three groups and locked-cloud filtering
  (`command-menu-items.tsx:102-262`).
- Keyboard shortcut set (§4.4).
- Five color themes + persisted bootstrap (`color-theme/definitions/index.ts:14-26`;
  `runtime.ts:128-133`).
- Modal system (portal, escape, backdrop-click, focus trap, stacked z-layers)
  (`modal-backdrop.tsx`).
- Toast system with reading-speed durations (`custom-toast-handlers.tsx`;
  `toast-duration.ts`).
- i18n: 16 languages, runtime-fetched resources, `en` fallback
  (`i18n/index.ts:21-67`).
- Responsive breakpoints 1024/767/1023 and the mobile panel route
  (`use-breakpoint.ts:3-14`; `use-conversation-panel-route.ts`).
- Home-route pinning (sidebar pin, per backend+org) (`use-pinned-home-route.ts`).
- Maintenance/faulty-model alert banner (`alert-banner.tsx`;
  `root-layout.tsx:180-193`).
- Cloud/local backend selector and backend-scoped conversation ids
  (`sidebar-rail-body.tsx:330-373`; `conversation.tsx:55-95`).
- Device-flow OAuth verification page (`device-verify.tsx`).
- Canvas-extension runtime: versioned host API, enabled-only activation,
  module loading, per-extension error map (`canvas-extensions-runtime.tsx:11,29-33,120-122`).
- Public shared-conversation view with reconstructed UI events
  (`shared-conversation.tsx:52-67`).

---

*End of model. Snapshot: `/opt/openhands` @ `38fe0c0`, `@openhands/agent-canvas`
v1.27.1. Descriptive only; every claim carries a `file:line` citation from the
snapshot above.*
