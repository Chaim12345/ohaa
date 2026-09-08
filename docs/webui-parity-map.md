# OpenHands Web UI Structural Map (Android parity reference)

Source of truth: `/opt/openhands/src` (React + TypeScript), read-only investigation, 2026-09-08.
All paths below are relative to `/opt/openhands/`. No files under `/opt/openhands` were modified.

## 1. Home / landing screen

Route: `src/routes/index-home.tsx` redirects `/` to a pinned home route if set, else re-exports
`src/routes/home.tsx`. The built-in home also lives at `/conversations`.

Top-to-bottom (`HomeScreen`, testid `home-screen`):

1. **LlmNotConfiguredBanner** (`src/components/features/home/llm-not-configured-banner.tsx`)
   - Warning icon + message + primary button -> `/settings/llm`.
   - Renders only when no usable LLM is configured.
2. **HomeChatLauncher** (`src/components/features/home/home-chat-launcher.tsx`),
   centered column max-w 800px, pushed ~28vh down:
   1. `HomeHeaderTitle` — H1 "Let's start building", centered.
   2. `CustomChatInput` — contentEditable composer: file-attach icon, image/file drag-drop +
      paste, top-grip resize, slash-command menu, draft persistence, send button.
      Submit -> `useCreateConversation` with `{ query, entryPoint: "home_chat_launcher",
      repository{name,gitProvider,branch}? | workingDir+workspaceMode?, plugins? }`,
      then navigate to `/conversations/{id}`. Attachments deferred via pending-task stores
      for async cloud sandboxes. `/model NAME` typed here activates an LLM profile instead
      of becoming a message (`useModelInterceptor`).
   3. Selection pill row: `OpenLauncherButton` ("Open Workspace" local / "Open Repository"
      cloud) -> dialog; once selected, `HomeGitControlBarPreview` (repo pill +
      `WorkspaceModeSelector` + branch pill, click reopens dialog). Next to it
      `PluginPickerTrigger` (puzzle icon + count badge) -> `PluginPickerModal`.
   4. Automations block: `RecommendedAutomationsLauncher` (rail variant, cards can install
      MCP integrations and launch conversations), `PinnedAutomationsDashboard`,
      `RunningAutomationsList`.
   5. `OnboardingHost` below (hidden in preview mode).
3. Dialogs: cloud -> `OpenRepositoryDialog` (hosts `RepositorySelectionForm`);
   local -> `OpenWorkspaceDialog` (hosts `WorkspaceSelectionForm`).

No suggested-task card grid on home; suggestion cards live in the conversation empty state.

## 2. New-conversation creation flow

Two paths:

- **Chat-input path** (HomeChatLauncher): free text + optional attachments + optional
  repo/workspace + optional plugins -> create -> navigate.
- **RepositorySelectionForm** (`src/components/features/home/repo-selection-form.tsx`), used
  inline and inside OpenRepositoryDialog:
  - `GitProviderDropdown` — rendered only when >1 provider connected; auto-selects the sole
    provider or the last-used one (persisted in home-store); switching resets repo + branch.
  - `GitRepoDropdown` — placeholder "user/repo", disabled until provider chosen.
  - `GitBranchDropdown` — disabled until repo chosen; default = `repository.main_branch`.
  - Button: "Launch" (inline; creates `{repository:{name, gitProvider||"github",
    branch||"main"}, entryPoint:"home_repo_form"}` and navigates) or "Confirm" (dialog mode;
    returns selection to the launcher). Repo saved to recent-repositories on confirm.

Defaults: no selection = "start from scratch" (no working dir); branch = repo main_branch;
provider = sole or last-used.

## 3. Conversation screen

Route `src/routes/conversation.tsx` -> `ConversationMain`
(`src/components/features/conversation/conversation-main/conversation-main.tsx`), wrapped in
EventHandler + WebSocketProviderWrapper + ConversationOverviewDrawerProvider. Handles
per-conversation state resets, `task-{id}` polling + error redirect, and auto-resume of
PAUSED cloud sandboxes (`POST /api/v1/sandboxes/{id}/resume`).
URL `/conversations/:id/panel` (`src/routes/conversation-panel.tsx` re-exports the same
module) renders `ConversationMobilePanelPage` instead.

### Chat pane

- **Header** (h-10, testid `chat-pane-header`) — `ConversationNameWithStatus`:
  - Status dot (hover/click menu: runtime status, stop/resume execution).
  - `ConversationName`: double-click inline rename (PUT updateConversation); ellipsis menu:
    Rename, Delete, Stop, Display cost, Agent tools, Skills, Hooks, Make public/private,
    Copy share link, Export transcript (modal), Download conversation; confirm modals.
  - Right toggles: `ConversationGitActionsToggle` (commit/pull/push/PR/new-branch as chat
    prompts), `ConversationOverviewToggle` (info panel + hover peek), `RightPanelToggle`
    (desktop show/hide; mobile navigates to the /panel route).
- **ChatInterface** (`src/components/features/chat/chat-interface.tsx`), thread max-w 800,
  optional `ConversationOverviewPanel` column (240px: diffs row, git section, workspace row,
  pin menu):
  - Empty state: `ChatSuggestions` — headline + 4 cards from `SUGGESTIONS.repo` (click fills
    composer).
  - Scroll stream: skeleton/spinner; "fetching older messages" pagination at top;
    `ModelMessages` (/model switch entries); `Messages` (folds consecutive action+observation
    events into collapsible `EventGroup`s, hoists thoughts, PlanPreview per user turn);
    `PendingUserMessages` (optimistic faded bubbles with error + retry); `GoalStatusBanner`.
  - Above composer: SkillInstallRestartBanner, BtwMessages, ErrorMessageBanner (reconnect /
    reauth), LlmNotConfiguredBanner, archived / sandbox-ERROR read-only notice.
  - Composer overlay: `ConfirmationModeEnabled` lock chip (settings.confirmation_mode),
    `ChatStatusIndicator` while starting, `ScrollToBottomButton` when scrolled up,
    `TypingIndicator` while agent runs.
  - `InteractiveChatBox` = `CustomChatInput` + **GitControlBar**
    (`src/components/features/chat/git-control-bar.tsx`): repo pill (opens clone modal),
    branch pill, Pull / Push / Create-PR buttons (send chat prompts; disabled without
    repo / provider tokens / WS ready).

### Slash commands (`src/utils/constants.ts` BUILT_IN_COMMANDS)

`/new` (cloud only, new conversation same runtime), `/btw` (side question), `/model`
(list/switch LLM profile), `/goal [objective | --max n]`, `/plan` (plan mode), `/code`,
plus skills as `/name`. Intercepted before sending.

### Right panel

`ConversationTabs` (`src/components/features/conversation/conversation-tabs/conversation-tabs.tsx`)
— tab order: **files, commits, planner, [tasklist if agent uses tasks], terminal, browser,
usage**. Pinned tabs always visible; unpinned hidden unless active (context menu pin/unpin);
overflow collapses into ellipsis menu; `DrawerVSCodeLink` (probes /api/vscode/status);
last tab persisted per-conversation in localStorage; default "files" when panel opens.
`ConversationTabContent` lazy-loads via TAB_CONFIG with crossfade + agent-loading overlay.

- **files** -> `src/routes/files-tab.tsx`: WorkspacePath (working_dir) header,
  `FileQuickRow` (open-file tab strip + tree toggle + refresh), resizable `FileTreeView`
  aside, content viewer with Rich|Plain `SegmentedToggle` + open-in-new-window static link,
  `NoFileSelectedMessage` empty state. Per-conversation selection persistence; auto-refresh
  on agent edits.
- **commits** -> `src/routes/commits-tab.tsx`: `CommitList` newest-first + uncommitted-changes
  section (<=100, auto-expand) + hasMore; distinct waiting-for-runtime / loading / no-commits
  states.
- **planner** -> `src/routes/planner-tab.tsx`: PLAN.md (read-conversation-file; planner id
  preferred) rendered as markdown, auto-scroll; empty state "Create a plan" button.
- **tasklist** -> `src/routes/task-list-tab.tsx`: `TaskItem` rows, in_progress highlighted.
- **terminal** -> lazy xterm Terminal.
- **browser** -> `src/routes/browser-tab.tsx` -> `BrowserPanel`: chrome bar (url) + base64
  PNG screenshot (`BrowserSnapshot`) or `EmptyBrowserMessage`; driven by WS browser events.
- **usage** -> `src/routes/usage-tab.tsx` -> `UsagePanel`: context meter (per-turn tokens /
  context window) + CompactContextButton, token/cost stats, provider balance card; live WS
  + 30s REST poll.

`ConversationOverviewDrawer`: bottom sheet on mobile (max-h min(50vh,420px)) or resizable
right drawer on desktop.

Mobile `/panel` route (`ConversationMobilePanelPage`): back chevron + compact tab bar + the
same tab content, full-screen.

## 4. Gaps vs ohaa Android app (9 screens)

Screens: Home, Conversations, New, Detail, Files, Changes, Sandboxes, Secrets, Settings.
Items marked [I] are inferred from app sessions (verify against current code).

1. Home: no automation rails (templates/pinned/running, MCP install queue). [I]
2. Home: no plugin picker (attach plugins at creation). [I]
3. No slash-command system: `/new`, `/btw`, `/model`, `/goal`, `/plan`, `/code`, skills.
4. Model picker not wired in New conversation screen (modelCatalog param dead). [I]
5. Detail: no inline title rename (UpdateConversationRequest.title). [I]
6. Detail: no runtime status-dot stop/resume menu (pause/resume conversation).
7. Detail: no ellipsis actions: display cost, agent tools, skills, hooks, system message,
   make public + copy share link, export transcript, download conversation.
8. Detail: no GitControlBar (repo clone modal, branch pill, pull/push/PR prompt buttons).
9. Missing right-panel tabs: Planner (PLAN.md + create plan), TaskList, Browser, Terminal,
   Usage (context meter, compact-context, cost, provider balance).
10. Files: no Rich|Plain toggle, no open-file tab strip, no resizable tree, no raw-URL
    open-in-new-window, no per-conversation selection persistence.
11. Changes: no uncommitted-changes auto-expand section, no runtime-waiting vs no-commits
    distinct empty states.
12. No ConversationOverview side panel/drawer (workspace, git, diffs summary with pinning).
13. No confirmation-mode lock chip (display-only; StartConversationRequest has no such field
    per OpenHandsApiService.kt mirror).
14. No PAUSED-sandbox auto-resume (`POST /api/v1/sandboxes/{id}/resume`) when opening a
    conversation.
15. Chat: no suggestion cards on empty conversation, no optimistic pending-bubble queue with
    retry. [I]

Wire-contract note: the webUI's unified git hooks (useUnifiedGitCommits /
useUnifiedGetGitChanges) hit the same REST endpoints already mirrored in
`app/src/main/java/com/example/data/api/OpenHandsApiService.kt`; parity work can reuse the
existing repository functions.
