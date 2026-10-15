════════════ F01-first-run-and-sign-in.md

- `F01.first-run-gate`: with no `openhands-onboarded` flag in localStorage, any URL (including `/conversations`) shows only `first-run-onboarding-screen` with the onboarding modal. There is no sidebar. Finishing or skipping sets the flag, and the app stays revealed after a reload.
- `F01.telemetry-consent`: on a connected local backend whose `user_consents_to_analytics` is `null`, an elevated "Help improve OpenHands" modal names the backend and host. It has a checked-by-default "Send anonymous usage data" box and "Confirm preferences". Escape does not dismiss it. The choice is saved to backend settings, and the modal does not return after a reload.
- `F01.onboarding-modal`: a segmented progress bar (3 steps, or 4 with the backend step) over a sliding rail. Neither Escape nor a click on the backdrop dismisses it. "Skip for now" shows on every step except the last, and Back/Next move between steps.
- `F01.onboarding-backend-step`: when no reachable backend exists (public mode), step 0 "Add a backend" prefills the name `Local` and the page origin as host. Next stays disabled until an API key is typed. A wrong key shows "Could not connect to <host>" / "Invalid API key" inline. The right key saves the backend, and the flow collapses to 3 steps on "Choose your agent" with no Back button. The first paint must not raise an error toast.
- `F01.onboarding-choose-agent`: radio tiles for OpenHands (preselected), Claude Code, Codex and Gemini CLI, plus Pi and OpenCode on a local backend (#17758; a Cloud backend offers only the first four, and a hidden choice falls back to OpenHands). Next saves the agent kind with a "Settings saved" toast.
- `F01.onboarding-setup-llm`: on a first run the embedded LLM form is prefilled with `openai/gpt-5.6-sol`. Typing a custom model and key then Next saves them. On a local backend Next saves the form as an LLM profile named after the model (`deepseek/deepseek-flash` → `deepseek-flash`) and activates it, without writing the raw LLM settings.
- `F01.onboarding-repeat-endpoint`: onboarding again (a new browser, the same backend) fills the form from the active LLM profile, or from the one the `default` agent profile points at, model included. The All view prefills its Base URL and key even when the raw LLM settings hold another Base URL. Next without changes keeps that profile and saves no copy. Changing only the model creates a profile that keeps that Base URL and key, and the say-hello conversation reaches that endpoint.
- `F01.onboarding-acp-secrets`: choosing an ACP agent swaps the LLM step for "Add your API keys" with that provider's fields. When the host CLI is already logged in, a green "already signed in" banner makes them optional. OpenCode's fields (`OPENCODE_AUTH_CONTENT`, `OPENCODE_API_KEY`) are always optional; Pi's two fields (`PI_AUTH_JSON`, `ANTHROPIC_API_KEY`) are each labelled Optional, but without a host login at least one of them is needed: Next stays disabled under a warning, readable in light themes, until either is filled. Back returns to the agent step.
- `F01.onboarding-say-hello`: the last step has a prefilled message. An empty message disables send. Enter or the send button creates a conversation, navigates to `/conversations/<id>` and completes onboarding, and the agent replies. A recommended-automations list sits under an "Or" separator, and a "Skip Getting Started checklist" box sits below the modal.
- `F01.onboarding-hello-close`: the last step has Back and Close instead of "Skip for now". Close dismisses the modal without creating a conversation, sets the `openhands-onboarded` flag and leaves the app shell on `/`.
- `F01.onboarding-recommended-automation`: picking a card under "Start from a proven workflow" on the last step completes onboarding too. A card with its own setup form (for example "Daily news digest") navigates to `/automations/new/<id>` and opens that form. Nothing is created until the form is submitted.
- `F01.onboarding-skip-checklist`: the "Skip Getting Started checklist" box under the last step hides the sidebar **Getting started** card (F02) once the app is revealed. It is the same per-browser setting as the Settings → Application switch, and it is saved as soon as you tick it, preview mode included.
- `F01.onboarding-skip`: "Skip for now" closes the modal and sets the flag. In public mode, skipping before a key is stored lands on the API-key screen; skipping after the backend step lands in the app.
- `F01.onboarding-preview`: `?previewOnboardingStep=0..3` on any route opens the modal (`data-preview="true"`) on that phase, even after onboarding. Values outside 0-3 are ignored. Skip and Close do nothing, so leave by navigating to a URL without the parameter.
- `F01.onboarding-phone`: every onboarding step fits a 390 px viewport without horizontal overflow.
- `F01.api-key-entry`: in public mode with no usable key (onboarding skipped, the last backend removed, or a stored key the server now rejects), a full-screen "Add a backend" card appears. The host is read-only and set to the origin, and Connect stays disabled until the name and key are filled. A wrong key shows "Invalid API key. Please check the key and try again." The right key opens the app shell, which survives a reload, and the consent modal then names the new backend.
- `F01.onboarding-cloud-login`: the backend step's "OpenHands Cloud" column has "Connect to OpenHands" (device-flow login) and an "Advanced" toggle. The toggle reveals a Cloud Host field (placeholder `https://app.all-hands.dev`) for self-hosted Cloud deployments. The login itself is blocked: it needs an OpenHands Cloud account.
- `F01.route-error-boundary`: an unknown URL renders a "Page not found" page inside the app shell, with the sidebar and a **Home** link back to `/`, and logs no page errors.
- `F01.document-title`: the tab title is `OpenHands`. On a conversation it is `<status emoji> <conversation title> | OpenHands`.
- `F01.bootstrap-loading`: a centered spinner card shows while `/server_info` loads (transient, not-run).
- `F01.locked-cloud-first-run`: on a canvas locked to an OpenHands Cloud host, first run shows only the Cloud login (Add backend) without a progress bar or close button (blocked).
- `F01.cookie-auth-redirect`: OHE cookie-auth deployments probe the main-app session behind a spinner and redirect to its `/login` when it is missing (blocked).


════════════ F02-app-shell.md

- `F02.nav-links`: the logo and **New Chat** open `/conversations`. **Customize** opens `/customize`, which redirects to `/mcp` on desktop, and stays highlighted and marked `aria-current="page"` on `/skills`, `/plugins`, `/apps` and `/mcp`. **Automate** opens `/automations`. Canvas extension pages add their own rows (`sidebar-canvas-extension-<ext>-<id>`).
- `F02.pin-as-home`: hovering Customize or Automate reveals a pin. Pinning makes `/` redirect to that page, pinning the other one moves the pin, and unpinning restores the default home. The pin is stored per backend and org.
- `F02.sidebar-collapse`: the chevron collapses the rail to a 60 px icon rail with hover tooltips, a Settings icon and a backend icon with a status dot that opens a backend popover. The checklist is hidden while collapsed. The state survives a reload. To expand, click the chevron that appears when you hover the logo, or click empty rail space.
- `F02.settings-link`: the gear next to the backend selector, and the collapsed Settings icon, open `/settings` in the same tab for a local backend. On desktop that redirects to `/settings/agents`.
- `F02.conversation-list`: the expanded rail embeds the conversation list (`conversation-panel`). The list itself belongs to the conversations family.
- `F02.command-menu-open-close`: Control+k, Meta+k and the sidebar **Search commands** row (also the search icon on the collapsed rail) open a modal `Command menu` dialog with the input focused. Escape or the backdrop closes it, and the query is cleared on close. Escape returns focus to the element that had it before the menu opened (the **Search commands** row when it opened the menu). The row and the menu header show the shortcut as `Ctrl+K` off Apple platforms and `⌘K` on macOS and iOS.
- `F02.command-menu-focus`: the shortcut works while typing in another field (the composer) and moves focus to the search input. Tab moves focus to the first option. Escape closes the menu from any focused element inside it and returns focus to the composer.
- `F02.command-menu-items`: the menu shows Navigation (New chat, Customize, Automations, MCP servers), Settings (Settings, Agent settings, LLM profiles, Model Router, Condenser, Agent Context, Verification, Application, Secrets) and Actions (Toggle sidebar). Each **Go** item navigates to its route.
- `F02.command-menu-toggle-sidebar`: the **Toggle sidebar** action collapses or expands the desktop rail.
- `F02.command-menu-search`: every search term must match the title, description or keywords. **No commands found** is the empty state. The X button clears the query. Arrow keys wrap around the list, hovering an item selects it, Enter runs the selected item, and Enter does nothing when nothing matches.
- `F02.command-menu-new-tab`: a middle-click or modifier-click on a **Go** item opens the route in a new tab and leaves the menu open.
- `F02.command-menu-phone`: at 390 px the menu fits the screen and opens from the drawer's Search row. Escape closes the menu and leaves the drawer open. Running an item closes the drawer.
- `F02.mobile-drawer`: at phone width the rail is hidden and a hamburger (`sidebar-mobile-menu-toggle`) opens a 300 px drawer. The hamburger is in the top bar, or in the chat header on conversation pages. The drawer closes on its chevron, Escape, a backdrop click or any route change.
- `F02.mobile-top-bar`: at phone width, settings sub-pages and Customize detail pages (`/skills`, `/mcp`, `/plugins`, `/apps`) show a Back chevron that returns to `/settings` or `/customize`.
- `F02.checklist`: after onboarding, the expanded rail shows a **Getting started** card with **N complete** and six items. They link to `/settings/llm`, `/conversations`, `/automations`, `/settings/agents`, `/mcp` and the external Slack invite.
- `F02.checklist-item-links`: clicking an item's row navigates in the same tab to its route (for example **Schedule a task** opens `/automations`) without marking the item done.
- `F02.checklist-progress`: items get a check and a strikethrough when done. The LLM item completes when a profile is configured, Start your first chat when a conversation exists, Customize your agent after a visit to `/settings/agents`, and Join Slack on click.
- `F02.checklist-preview`: hovering an item shows a preview with a description, a **Documentation** link and an action button that navigates.
- `F02.checklist-minimize`: the card header minimizes and expands the card, and the state survives a reload.
- `F02.checklist-dismiss`: hovering or focusing the card (expanded or minimized) reveals an X (`Dismiss checklist`) in its header; touch devices always show it. Clicking it hides the card for good (it stays hidden after a reload) and turns off the **Show Getting Started checklist** switch in Settings → Application, which brings the card back in its previous expanded or minimized state.
- `F02.checklist-hide`: the **Show Getting Started checklist** switch in Settings → Application hides the card (it stays hidden after a reload) and can show it again.
- `F02.checklist-all-complete`: the card disappears once all six items are complete. Completion is recomputed from current data, so deleting the automation or the MCP server brings the card back.
- `F02.super-admin-setup-guide`: on an OpenHands Enterprise (cloud) backend with `ENABLE_SUPER_ADMIN`, the first Super Admin (`/me` permissions include `manage_super_admins`) sees a floating **Setup guide** panel in the lower-right corner with `n/4` progress, the steps Add an LLM, Choose an automation template, Add an integration, Invite users and Optional: SAML / instance, a `Next: <step>` status and a **Start** link for that step (the positive Enterprise path remains unverified here). Close collapses it to a pill that reopens it. While the guide loads or shows, the **Getting started** card is hidden. Local backends never show it and make no `/api/admin/setup-state` request (#17969).
- `F02.super-admin-setup-tour`: for the two setup steps done in Canvas (Choose an automation template, Add an integration), the guide walks the Super Admin through the page with a guided tour (`.oh-setup-tour-popover` with Back / Next / Done): the template step points at the recommended-automations section (a stop with no footer: no Back, Next or `1/2`, #18243), then the setup dialog; the integration step at the MCP library on `/mcp` (`mcp-marketplace-section`, "Pick a server from the library"), then the install dialog (#18261). A plain click on a Canvas step's row in the guide starts that step's tour and closes the panel (a modifier click follows the link), and while the tour waits for its next stop's element it hides its popover and overlay, so it never covers another dialog (#18261). Finishing the guide's next step in Canvas opens the step after it the way **Start** does (a Canvas step's tour or page; an enterprise step reopens the guide to point at Start, or navigates on a locked-Cloud canvas). An enterprise page that links back with `?setup_tour=<step id>` starts that step's tour once and drops the parameter. Local backends ignore `?setup_tour=` (no tour, no guide, the parameter stays) (#18138). The positive path is blocked here, with the same Enterprise prerequisite as the setup guide above.
- `F02.update-tile`: when npm `latest` is newer than the running version, a **New version** tile (`agent-canvas-version-tile`) opens an update modal with the npm and Docker commands. The tile is never shown in locked-Cloud mode.
- `F02.alert-banner`: when the server config reports maintenance, faulty models or an error message, a dismissible banner (`alert-banner`) appears at the top of the content area.
- `F02.settings-404-modal`: if the backend's settings return 404, the LLM settings modal opens by itself. Other settings fetch errors show a toast.
- `F02.error-toasts`: failed requests show a top-right error toast with the product or server message, and the toast closes by itself. Toasts stay readable in every colour theme.


════════════ F03-home.md

Starting work:

- `F03.home-entry-points`: `/`, `/conversations`, the sidebar **New Chat** and the command-menu item **New chat** show the launcher (`home-chat-launcher`) with the heading, subtitle, the Code / Automate toggle and the composer, whose Code-mode placeholder reads `What do you want to build?`.
- `F03.llm-not-configured-banner`: with no usable LLM, a `role=alert` banner reads `Your LLM isn't set up yet, so conversations won't run. Finish setup to get started.`; **Set up LLM** opens `/settings/llm`; the banner is gone once a profile with a key is active.
- `F03.composer-blocked-without-llm`: with no LLM, Send is disabled and Enter creates nothing (the text stays editable).
- `F03.launcher-mode-toggle`: a `Choose launcher mode` radio group above the composer offers **Code** (checked on every page load) and **Automate**. Automate changes the placeholder to `What work do you want to automate?` and greys out and disables the Open Workspace and Plugins row (`home-composer-actions` `aria-disabled="true"`); Code restores both. The active option stays readable in every colour theme.
- `F03.launch-automate-mode`: submitting in Automate mode creates a conversation and opens `/conversations/<id>` as an automation setup split view: the chat on the left and the automation form (`automation-setup-panel`) on the right, whose fields the agent fills with `✦ Filled by OpenHands` markers. Attached plugins are not sent, and nothing is created until **Create Automation**. The setup view itself is F22's.
- `F03.draft-persistence`: the home draft survives a reload and is cleared after a conversation is created.
- `F03.home-model-command`: `/model <profile>` sent from home activates that LLM profile without creating a conversation.
- `F03.launch-in-workspace`: submitting with a workspace picked shows `Creating conversation…`, opens `/conversations/<id>`, and the agent runs in that folder.
- `F03.launch-without-workspace`: with nothing picked, the conversation runs in a fresh per-conversation folder under the server home.
- `F03.create-error-toast`: when creation fails, the loading toast is replaced by one error toast and the prompt is not lost.
- `F03.plugin-picker`: **Plugins** opens **Attach plugins** (catalog, search, Add/Remove switch, `N selected`, Done and X); the pill then shows a count badge.
- `F03.launch-with-plugin`: a conversation started with a plugin attached loads it; the selection resets afterwards.

Workspaces (local backend):

- `F03.open-workspace-dialog`: **Open Workspace** opens a dialog with a workspace combobox; Confirm stays disabled until a pick; confirming swaps the pill for a preview bar that reopens the dialog; X and Escape close it.
- `F03.workspace-dropdown-search`: typing filters workspaces; with more than one group, workspaces are grouped by parent with an **Other** group; no match (or no workspaces) shows `No workspaces yet`.
- `F03.selection-after-reload`: after a reload the preview bar is gone, but the dialog preselects the last workspace picked in its dropdown, confirmed or not: a pick closed with the X survives leaving for another page and coming back, and a reload.
- `F03.workspace-mode`: the preview bar's mode menu switches **Local Repo** / **New Worktree**; the choice persists across reloads.
- `F03.folder-browser`: **+ Add Workspace** opens **Add workspaces** at the server home with Favorites/Locations, Up and the current path; **Add this directory** adds and selects the folder, **Add all subdirectories** adds a parent whose children become workspaces, **Cancel** adds nothing.
- `F03.folder-browser-locations`: the browser's **Favorites** (Home, plus `workspace` once a no-folder conversation exists) and **Locations** (`/`) jump to that folder; Up is disabled at `/`.
- `F03.manage-workspaces`: **Manage Workspaces** lists workspaces and parents (with children); every Remove asks for confirmation; Cancel keeps, Confirm removes for good and clears the selection; with nothing left it reads `You don't have any workspaces yet.`
- `F03.open-repository-dialog`: on a Cloud backend, **Open repository** picks provider, repository and branch (blocked locally).
- `F03.isolated-workspace-notice`: on an isolated-runtime backend a status line explains the new isolated workspace and offers **Clear host workspace** (blocked locally).
- `F03.workspaces-unsupported`: on an Agent Server without workspace APIs the pill is disabled with a tooltip (blocked locally).

Automations on home:

- `F03.recommended-automations-rail`: a horizontally scrolling rail of recommended automations with edge fades; cards with a setup form open `/automations/new/<id>`.
- `F03.recommended-responder-choice`: GitHub/Slack responder cards on a local backend first ask **Choose how your responder should run**.
- `F03.responder-cloud-option`: in that dialog **Open OpenHands Cloud integrations** opens `https://app.all-hands.dev/settings/integrations` in a new tab, closes the dialog and saves no secret; X closes it without a choice.
- `F03.recommended-missing-integration`: a card whose required integration is missing opens that integration's install dialog first.
- `F03.recommended-hides-added`: a catalog automation that already exists (matched by name, enabled or not) drops off the rail; deleting it brings the card back.
- `F03.recommended-prompt-launch`: a card with no setup form and no missing integration creates a conversation that sends the automation prompt (blocked locally).
- `F03.home-automations-list`: **Automations** lists enabled automations with schedule, last run and status, a sparkline, **+ Add**, and `No automations yet. Add one to automate repetitive work.` when empty.
- `F03.home-automation-links`: a row opens `/automations/<id>`, a sparkline bar opens `/automations/<id>?run=<runId>`, **+ Add** opens `/automations`.
- `F03.home-run-tooltip`: hovering a row shows its trigger, status, last run and task.
- `F03.home-run-outcome-health`: an automation whose latest Completed run reported a task outcome shows it on Home (#17682): `blocked` and `partial_success` read **Blocked** and **Partial**, `unknown` or an unrecognised status reads **Needs review**, each with an amber (`bg-warning`) health dot, in the row text, the row link's aria-label, the tooltip's Status line and the pinned card's strip. A finish response without a string status counts as success.
- `F03.home-row-menu`: the row kebab offers Run now, View, Edit, Turn off and Pin to dashboard; Run now toasts `Automation dispatched` and is disabled while a run is in flight; Edit opens the automation setup page `/automations/setup?automationId=<id>` with the form prefilled (no modal), where **Save** toasts `Automation updated`.
- `F03.cancel-in-flight-run`: **Cancel run** appears while a run is pending or running and cancels it (`Automation run cancelled`).
- `F03.turn-off-confirmation`: Turn off asks `Turn off "<name>"? It will stop running on its schedule until you turn it back on.`; Cancel keeps it on, Turn off disables it and drops it from home.
- `F03.pinned-automations-grid`: **Pin to dashboard** adds a card to **Pinned automations** (prompt, pills, status strip, Runs); the title opens the detail page; a deleted automation's card disappears.
- `F03.pinned-card-menu`: a pinned card's kebab has the same actions with **Unpin from dashboard**.
- `F03.pinned-card-links`: a pinned card's sparkline bar opens `/automations/<id>?run=<runId>`, and Enter on its focused title opens the detail page.
- `F03.pinned-reorder-drag`: dragging a pinned card's title row (`Reorder <name>`) onto another card reorders the pins, and the order survives a reload.
- `F03.home-automations-view-more`: past 10 rows **View more**/**View all**, past 6 pins **View more**/**View less** (not arranged).
- `F03.automations-backend-down`: with the automation service down, the installed list and pinned grid are hidden and the composer still works. The recommendation rail returns after loading from its static catalog; opening a setup card reaches its route and pre-setup dialog. The rail asks for the installed automations only once `/api/automation/health` answers ok (#17838), so home raises no error toast for the stopped service.
- `F03.phone`: at 390 px the banner, launcher, rail, pinned grid, list and the workspace dialogs fit without horizontal overflow, and folder names stay readable.


════════════ F04-conversation-list.md

- `F04.empty-state`: with no conversations (By date) the list shows `No conversations found`.
- `F04.view-menu`: the View menu lists the presets By workspace, Recent, Active and Compact (a fresh profile has Recent checked), Filter by tags (`No tags yet` when empty), More options and Delete all (disabled with no conversations); Escape closes it.
- `F04.new-thread-picker`: the folder-plus button lists No workspace, every known workspace, + Add Workspace and Manage Workspaces; picking a row starts a conversation there and opens it.
- `F04.card`: a row shows a status dot, the title and the relative time, links to `/conversations/<id>`, and the open conversation's row is highlighted.
- `F04.card-live-status`: while the agent runs, its row shows the working indicator; once it finishes the row switches to the check mark and the generated title on the next list refresh.
- `F04.list-header`: the header divider appears only while the list is scrolled.
- `F04.organize`: More options → Organize switches between folders (By workspace) and a flat list (By date); the choice survives a reload.
- `F04.grouped-folders`: By workspace shows one folder per known workspace (empty ones included) plus a No workspace folder while any loaded row has no workspace, in any browser.
- `F04.folder-collapse`: a folder heading collapses and expands that folder.
- `F04.collapse-all`: the Conversations header collapses every folder, then expands them all.
- `F04.folder-new-conversation`: a folder's `+` starts a conversation in that workspace and opens it.
- `F04.folder-preview-more`: a folder shows five rows; More shows every loaded row, Less returns to five.
- `F04.folder-reorder`: dragging a folder heading onto another reorders the folders and the order persists.
- `F04.thread-scope`: More options → Threads → Active only hides paused conversations; All threads shows them again.
- `F04.load-more`: 20 conversations load first; Load more fetches the rest and disappears when nothing is left.
- `F04.sort`: Sort by Updated or Created reorders the list.
- `F04.presets`: a preset is checked and survives a reload; Active and Compact hide paused conversations; any manual change reads `More options · Custom`.
- `F04.hide-older`: Hide old conversations offers four cutoffs, persists the choice and hides conversations older than it.
- `F04.automation-filter`: More options → Automations shows all, hides or keeps only automation runs, with its own empty message.
- `F04.automation-setup-tag`: a conversation started from the automation setup page shows an `Automation setup: Draft` chip (lightning icon) first in its tag row; Filter by tags offers a facet and filter chip of the same name, and the hover preview lists `Automation setup` `draft`.
- `F04.tag-filter`: choosing a tag facet narrows the list to rows with that tag (pins exempt) and survives a reload.
- `F04.filter-chips`: active filters show as chips above the list; a chip click removes one, Clear all removes every chip.
- `F04.metadata-toggles`: Repo and branch, Agent / model and Show tags add or remove row chips.
- `F04.hover-preview`: hovering a row for a second shows title, directory, model and creation time, unless Details on hover is off.
- `F04.pin`: the pin moves a row into a Pinned section above the list, survives a reload and unpins from there.
- `F04.pinned-preview-more`: the Pinned section previews five rows; its More shows every pinned row and Less returns to five.
- `F04.card-rename`: ⋮ → Rename edits the title inline; Enter saves, a toast confirms, and the title persists.
- `F04.edit-tags`: ⋮ → Edit tags adds and removes `key: value` tags with validation; Save shows `Tags updated` and the chips persist.
- `F04.edit-tags-remove`: in Edit tags each row's remove button drops that tag; Save persists the removal on the server.
- `F04.tag-overflow`: tags that do not fit on a row collapse into a `+N` chip (aria `Show N more tags`) whose popover lists the hidden tags without opening the conversation; Escape closes it.
- `F04.stop`: ⋮ → Stop Conversation asks for confirmation and pauses the conversation; the item is gone once paused.
- `F04.download`: ⋮ → Download conversation data saves `conversation_<id>.zip`.
- `F04.download-error`: a failed download shows one toast and closes the ⋮ menu. When the Agent Server is down behind a plain-text 502 ingress response, the toast uses the generic error fallback rather than raw transport text.
- `F04.archive`: ⋮ → Archive Conversation asks for confirmation, hides and unpins the row, and leaves the conversation if it was open.
- `F04.show-archived-unarchive`: Show archived brings archived rows back with an `Archived` chip; Unarchive restores a row without confirmation.
- `F04.delete`: ⋮ → Delete Conversation asks for confirmation, deletes it on the server and leaves it if it was open.
- `F04.delete-all`: View → Delete all asks for confirmation with a count and deletes every conversation.
- `F04.collapsed-rail`: the collapsed sidebar rail shows no conversation list; expanding brings it back.
- `F04.phone`: at 390 px the list lives in the sidebar drawer, fits the viewport, and picking a row closes the drawer.
- `F04.cloud-picker`: on a Cloud backend the folder-plus picker shows provider tabs and a repository search; a picked repository starts a conversation.
- `F04.start-task-cards`: on a Cloud backend in-progress start tasks show as cards linking to `/conversations/task-<id>`.


════════════ F05-composer.md

- `F05.send-message`: Send is disabled while the field is empty; Shift+Enter inserts a newline; Enter sends, clears the field and the message persists after reload.
- `F05.draft-persistence`: unsent text is restored per conversation (after navigation and reload) and on the home composer.
- `F05.composer-resize`: the field grows with its content and shrinks when cleared; a top-edge grip lets the user drag the height.
- `F05.plus-menu`: `+` opens a tools menu (Macros, Show skills/hooks/agent tools, Add Files and Images; Git tools in a repository); a macro fills the composer without sending.
- `F05.agent-profile-switch`: before a conversation starts, `+` > Switch agent profile lists agent profiles and links to Manage agent profiles; it is absent in a started conversation.
- `F05.attach-files`: picked files show as chips and images as thumbnails (with an Upload-as-file toggle), each removable; sent files land in the workspace and are named in the message.
- `F05.attach-size-limit`: a file over 3 MB, or a selection over 3 MB in total, is refused with an error toast.
- `F05.image-only-send`: an attached image alone, with an empty field, enables Send; the sent user message shows the thumbnail and carries the picture as an embedded data URL.
- `F05.paste`: pasted clipboard text is inserted as plain text; pasted images and dropped files attach.
- `F05.dictation`: the mic button dictates into the composer; without a microphone it toasts a failure and resets.
- `F05.llm-profile-picker`: the model pill switches the running conversation's LLM profile; the next reply uses the new model.
- `F05.profile-identity`: the pill names the profile the conversation was started with, also after a reload and when another profile uses the same model; changing the default profile in Settings changes the home pill, not an existing conversation's.
- `F05.overflow-menu`: at very narrow widths the model pill collapses into a "More input actions" menu whose Model submenu fits the viewport and switches the profile; Escape closes the menu and returns focus to its trigger.
- `F05.context-window-meter`: the ring shows context use; its popover opens the Usage tab and compacts the context.
- `F05.stop-resume`: Stop pauses a running agent (status `Stopped`, Play button); Play resumes it.
- `F05.queued-message`: a follow-up typed while the agent runs is accepted and answered.
- `F05.slash-menu`: `/` opens a filtered command list (built-ins and skills) with arrow, Tab/Enter and Escape handling.
- `F05.slash-model`: `/model` lists saved profiles; `/model <name>` switches profile.
- `F05.slash-btw`: `/btw <question>` answers a side question in a dismissible card without adding to the main stream; a bare `/btw` toasts that a question is needed.
- `F05.slash-goal`: `/goal [--max N] <objective>` runs a judged goal loop with a status banner, Stop and Resume; only the newest goal status offers Resume.
- `F05.slash-plan-code`: `/plan` switches to plan mode (a planner sub-conversation), `/code` switches back.
- `F05.slash-plan-code-task`: `/code <task>` switches to code mode and sends `<task>` to the code agent at once; `/plan <task>` switches to plan mode and sends `<task>` to the existing planner.
- `F05.plan-preview`: the planner's Plan.md card shows the plan with Read more, View (Planner tab) and Build.
- `F05.build-plan-shortcut`: Ctrl/Cmd+Enter in plan mode builds the plan with the code agent.
- `F05.phone`: the composer fits a 390 px viewport with every control inline.


════════════ F06-agent-activity.md

- `F06.empty-state-suggestions`: an empty conversation shows "Let's start building!" with four suggestion chips; a chip fills the composer with its prompt.
- `F06.user-and-agent-messages`: user and agent bubbles; a long user message is clipped with a gradient and expands on click ("View More").
- `F06.message-copy`: hovering a bubble shows Copy to clipboard; it copies the message's markdown source and the label flips to Copied to clipboard for 2 s.
- `F06.code-block-copy`: hovering a fenced code block in an agent reply shows its own Copy button, which copies only the code.
- `F06.timestamps`: hovering a bubble or tool title shows the event's local date and time.
- `F06.pending-messages`: a sent message shows "Sending..." until the server echoes it; a failed send shows Failed to send with Retry and Dismiss; Retry delivers it exactly once.
- `F06.clock-skew-send`: a message sent while the browser clock runs ahead of the server is still confirmed by the server echo: no lingering "Sending..." and no Retry.
- `F06.failed-send-persists`: a failed send keeps its Retry row after the backend returns and after leaving and reopening the conversation from the sidebar; sending the same text again succeeds without clearing it, and Retry then delivers that attempt exactly once.
- `F06.markdown-rendering`: agent replies render markdown: tables inside a horizontal scroller with edge fades, fenced code, inline code.
- `F06.workspace-path-links`: a workspace file path in an agent reply, and the path chip of a file-editor card, open that file in the Files tab.
- `F06.thinking`: model reasoning appears as a collapsed "Thinking" row that expands.
- `F06.event-groups`: consecutive tool calls collapse into "N actions completed" (or "k/N actions completed" with a spinner while one is pending); expanding lists one titled row per action.
- `F06.tool-visualizers`: expanding a tool row shows a rich body: terminal command and output (non-zero exit badge `exit N`), file-editor path chip and content.
- `F06.markdown-file-preview`: when the agent creates a Markdown file, its row stays expanded and ungrouped and shows a rendered, height-limited preview card with the file name and View, which opens the file in the Files tab.
- `F06.task-list`: task-tracker calls render a "Tasks" card with done / todo icons.
- `F06.events-match`: the stream shows one bubble per message event and one row per action, with no duplicates (compare with `conversation events`).
- `F06.load-older-history`: only the newest 50 events load first; the previous 50 are fetched when the user scrolls the transcript to its top (a "Fetching older messages…" row shows until they arrive) and automatically when the content is too short to scroll, so the first prompt and all actions appear.
- `F06.scroll-to-bottom`: scrolling up shows a scroll-to-bottom button; clicking it returns to the bottom and hides it.
- `F06.live-activity`: while the agent runs, a chip above the composer names the current action ("Thinking", then the action title) and the composer status reads Running.
- `F06.status-indicator`: while a new conversation starts, a "Starting" pill shows above the composer.
- `F06.stop-resume`: Stop in the composer interrupts the agent, including a running tool call (status Stopped, play button, an Agent error row for the interrupted call); Play resumes it to completion.
- `F06.reload-mid-run`: reloading while the agent runs reattaches to the live run; it finishes without duplicated events.
- `F06.reconnect`: when the Agent Server goes away the composer shows Disconnected; after it returns the page reconnects without a reload.
- `F06.error-banner`: losing the server shows "Unable to connect to server" with Retry, Copy and Close; Retry reconnects once the server is back.
- `F06.error-events`: a conversation error (ConversationErrorEvent) surfaces as a warning banner and the composer status Error.
- `F06.confirmation-mode`: with confirmation mode on, a lock chip sits above the composer and each action waits for "Do you want to continue with this action?" with Cancel and Continue (hints `⇧⌘⌫` / `⌘↩` on macOS, `Ctrl+⇧+⌫` / `Ctrl+↩` elsewhere, hidden below 640 px); a cancelled action is marked Rejected and counts as resolved in its group.
- `F06.confirmation-shortcuts`: Cmd+Enter or Ctrl+Enter continues and Shift+Cmd+Backspace or Shift+Ctrl+Backspace cancels a pending action.
- `F06.branch-from-here`: hovering a message shows Branch from here; on an agent message it forks inclusively; on a user message it forks without that message and pre-fills its text in the new composer, ready to send. The branch is titled "<title> (branch)" and keeps working.
- `F06.image-attachments`: images sent with a message show as thumbnails in the bubble; expand opens a lightbox that closes with X or Escape.
- `F06.skill-install-banner`: after the agent installs a skill into the workspace, a banner offers "Start new conversation with this skill" (same workspace) and Close.
- `F06.llm-not-configured-banner`: with no usable LLM, a conversation shows "Your LLM isn't set up yet..." with Set up LLM; suggestions are hidden and sending is disabled.
- `F06.critic-result`: with the critic enabled, agent messages and the finishing action each carry a "Critic: agent success likelihood" block (stars labelled `Score: <N.N>%`, one decimal, with the percentage beside them); Expand details lists the issue categories (Potential Issues, Infrastructure, Likely Follow-up).
- `F06.hook-events`: hook executions render as their own rows in the stream.
- `F06.corrective-nudge`: when the model answers with neither a message nor a tool call, the SDK's nudge (`Your last response did not include a function call or a message. …`) shows as a muted, italic `role=note` line with an info icon (`corrective-nudge-message`), not as a user bubble (#17864).
- `F06.phone`: at 390 px the transcript fits the viewport; wide tables scroll inside their own container.


════════════ F07-conversation-page.md

- `F07.open-by-url`: `/conversations/<id>` shows header, history and composer, and remembers the id as the backend's last conversation.
- `F07.missing-redirect`: an unknown id toasts "This conversation does not exist…" and redirects to `/conversations`.
- `F07.history-skeleton`: opening a conversation shows a message skeleton until its history has loaded.
- `F07.panel-route`: at phone width (up to 1024 px) the header panel button opens `/conversations/<id>/panel`, the tabs panel full width with a Back button; wider, that URL redirects to `/conversations/<id>` with the drawer open beside the chat.
- `F07.rename-inline`: double-click the title (or menu Rename), Enter or blur saves with a toast; an empty value reverts.
- `F07.status-menu`: hovering the status dot (or tapping it on touch) opens a menu with the state and Stop Runtime (active) or Start Runtime (paused).
- `F07.menu-open`: the "..." menu opens under the title; Escape closes it and returns focus to "...", and so does an outside click (without the focus move).
- `F07.menu-items-by-state`: Show Available Hooks and Stop Conversation appear only while the conversation is active.
- `F07.skills-modal`: Show Available Skills lists the loaded skills with expandable rows and Refresh.
- `F07.hooks-modal`: Show Available Hooks lists the workspace's `.openhands/hooks.json` hooks by event (read when it first opens for a conversation after a page load, and on Refresh; a reopen within 5 minutes of the last read shows the cached list), or "No hooks configured for this conversation."
- `F07.skills-modal-project`: a workspace with `.agents/skills/<name>/SKILL.md` adds a "Project skills" section above the public ones; each row expands to its triggers and body. The list is read with the page and on Refresh; opening the dialog within 10 minutes of the last read shows the cached list.
- `F07.agent-tools-modal`: Show Agent Tools & Metadata shows the system message and the tool list.
- `F07.export-transcript`: Export… downloads a Markdown or HTML transcript with optional tool details and timestamps.
- `F07.download-zip`: Download conversation data downloads `conversation_<id>.zip`.
- `F07.display-cost`: Display Usage and Cost opens the right panel on the Usage tab.
- `F07.stop-confirm`: Stop Conversation asks for confirmation; Confirm Close pauses the conversation and returns to `/conversations`.
- `F07.delete-confirm`: Delete Conversation names the title in a confirmation; Cancel keeps it, Confirm Delete removes it and returns to `/conversations`.
- `F07.branch-from-message`: Branch from here on a message forks the conversation as `<title> (branch)`; on a user message the message is left out and its text goes back into the composer.
- `F07.right-panel-toggle`: the header panel button shows and hides the right tabs panel (closed again after a reload).
- `F07.overview-toggle`: the header (i) button shows and hides the Overview column, which is exclusive with the right panel.
- `F07.git-actions-menu`: the header Git actions menu offers Commits, Pull, Push, Create PR and Create New Branch; each pre-fills the composer without sending, and Send becomes enabled so the user can submit or edit the prompt.
- `F07.git-control-bar`: under the composer, a folder workspace shows an inert workspace chip and the branch; with a GitHub `origin` the repo and branch chips link to GitHub and Pull, Push and Pull Request appear.
- `F07.git-control-bar-send`: the Pull, Push and Pull Request chips send their git prompt to the agent at once.
- `F07.cloud-only`: Share publicly, Connect Repo, `task-<uuid>` start URLs, paused-sandbox resume, archived read-only state and the shared-conversation redirect exist only on a Cloud backend.
- `F07.phone`: at 390 px the header keeps its controls and the info dialogs fit the viewport.


════════════ F08-workspace-files-and-changes.md

- `F08.panel-toggle`: the chat-header toggle (`Show panel`/`Hide panel`, `aria-pressed`) opens and closes the drawer; the first open shows Files.
- `F08.tab-bar`: the tab bar (Files, Commits, Planner, Terminal, Browser, Usage; Task list only when the agent has one) switches the drawer body, and only the active tab shows its label. Clicking the active tab closes the drawer.
- `F08.drawer-state-reload`: after a reload the drawer starts closed, and reopening it restores the last selected tab for that conversation.
- `F08.tabs-menu`: the ellipsis menu at the end of the tab bar lists every tab. Choosing a row opens that tab and closes the menu, and so does clicking outside.
- `F08.tabs-pin`: the pill on each menu row pins or unpins a tab. Unpinning the active tab selects another pinned one, and the choice persists across reloads.
- `F08.drawer-resize`: dragging the chat/drawer divider resizes both panes (chat 30–80 %, default 50 %, stored in `desktop-layout-panel-width`).
- `F08.vscode-link`: a `VSCode` button at the right of the tab bar opens the editor in a new window; it is hidden when the runtime has no editor. (#17660 removed it and #18048 restored it.)
- `F08.archived-disabled`: on an archived conversation the toggle is disabled with the tooltip `Unavailable for Archives`.
- `F08.phone-panel-page`: at 1024 px or narrower the toggle navigates to `/conversations/<id>/panel`, a full-screen page with a Back chevron, the same tab bar and the tab body.
- `F08.panel-direct-url`: opening `/conversations/<id>/panel` directly renders the panel page at 1024 px or narrower, and Back returns to the chat; wider, the URL is replaced with `/conversations/<id>` and the drawer opens beside the chat.
- `F08.files-workspace-path`: `Workspace: <working_dir>` with the full path in its tooltip; the copy button turns into `Copied to clipboard` for about 2 s.
- `F08.files-tree`: a tree of folders, collapsed at first, and files. Clicking a file selects it and adds it to the open-file strip.
- `F08.files-empty-workspace`: a workspace with no files shows `No files in workspace`.
- `F08.files-tree-toggle`: the list-tree button hides or shows the tree, and the choice persists per conversation. The tree is 224 px wide by default.
- `F08.files-tree-resize`: dragging the divider between the tree and the preview resizes the tree (160–480 px, default 224) and the width persists across reloads (`files-tab-tree-width` in localStorage).
- `F08.files-discovery-settings`: the Files toolbar's `File discovery` button opens a dialog (excluded directory patterns, maximum files, symbolic links) whose Save applies to every conversation on that workspace and backend; an excluded directory leaves the tree, Restore defaults brings the default patterns back, an invalid limit disables Save.
- `F08.files-open-tabs`: the open-file strip, each tab with a close X. Closing the selected file selects a neighbour. Open files and the selection survive a reload and do not leak into other conversations.
- `F08.files-no-selection`: with no file selected the pane reads `Select a file to view its contents` and the Rich/Plain toggle is absent.
- `F08.files-rich-plain`: Rich highlights code, renders Markdown and shows HTML in an iframe sandboxed without scripts. Plain shows highlighted source. The mode persists across reloads.
- `F08.files-fallbacks`: images render as `<img>` in Rich mode. Files with NUL bytes show `Binary file – preview not available`, and Office files show `Preview isn't available for Word files.` (or PowerPoint/Excel).
- `F08.files-load-error`: a selected file that can no longer be read shows the error (`Failed to read <path>: 404`).
- `F08.files-open-new-window`: the external-link icon opens the file from the workspace fileserver in a new browser tab.
- `F08.files-refresh`: `Refresh files` re-reads the file list and the selected file's content.
- `F08.files-auto-refresh`: files the agent writes appear without a manual refresh.
- `F08.files-open-from-chat`: clicking a workspace path in an agent (or user) message opens the drawer on Files with that file selected.
- `F08.files-open-from-tool-chip`: the path chip on a file-editor tool card (absolute path) opens the drawer on Files with that file selected.
- `F08.commits-states`: with no commits and no changes the Commits tab shows `No commits yet`. While the runtime is inactive it shows `Waiting for runtime to start...`, and while loading `Loading changes...`.
- `F08.uncommitted-changes`: the top row reads `-`, `Uncommitted` and a file count; expanding it lists the changed paths.
- `F08.commit-rows`: each commit row shows the short SHA, subject, author (only when there are several authors) and relative time. Rows are single-open accordions, and new agent commits appear without a reload.
- `F08.commits-cap`: the history loads the latest 50 commits; a longer history ends with `Showing the latest 50 commits`.
- `F08.diff-view-modes`: expanding a changed file shows its diff; the view-mode buttons (`Old version`, `Diff`, `New version`) switch between old, diff and new without page errors.
- `F08.diff-markdown-preview`: a Markdown file in new (or old) mode renders as a prose preview.
- `F08.diff-deleted-file`: a deleted working-tree file reads `This file was deleted. Its previous contents are no longer available for preview.`
- `F08.changes-from-overview`: the overview's Changes row opens the drawer on Commits with Uncommitted expanded and closes the overview.


════════════ F09-settings-shell.md

- `F09.entry-gear`: the sidebar gear opens Settings on the Agent page; the gear is marked active on every settings URL; at phone width the drawer's gear opens the hub and closes the drawer.
- `F09.entry-collapsed-rail`: with the sidebar collapsed, the rail's Settings icon opens the Agent page.
- `F09.entry-command-menu`: the command menu's Settings group (Settings, Agent settings, LLM profiles, Model Router, Condenser, Agent Context, Verification, Application, Secrets settings) opens the matching page.
- `F09.command-menu-coverage`: every page in the settings nav, including Model Router and Agent Context, can be found in the command menu and opens from it.
- `F09.entry-deep-links`: the Home "Set up LLM" banner, the checklist items "Add LLM API key" and "Customize your agent", the composer LLM picker's "LLM Profiles" link and the + menu's "Manage agent profiles" link each land on `/settings/llm` or `/settings/agents`.
- `F09.index-redirect`: on desktop, `/settings` replaces itself with `/settings/agents` (Back skips `/settings`).
- `F09.locked-cloud-landing`: on a canvas locked to a Cloud host, `/settings` lands on `/settings/app` instead.
- `F09.legacy-agent-redirect`: the retired `/settings/agent` URL replaces itself with `/settings/agents`.
- `F09.hidden-llm-redirect`: with the `hide_llm_settings` feature flag, the LLM link disappears and `/settings/llm` redirects to `/settings/agents`.
- `F09.desktop-nav`: the desktop nav lists the eight pages in order, exactly one link is `aria-current="page"`, and the nav stays pinned while the page content scrolls.
- `F09.page-header`: each page shows its nav title as an H2 with a grey subline; Secrets draws its own header instead, and the Agent profile editor hides the shell header until you leave it.
- `F09.landmarks`: the settings screen exposes a single `main` landmark, on desktop pages and on the phone hub.
- `F09.phone-hub`: below 1024 px (phones and tablets), `/settings` stays put and shows a "Settings" heading, the eight links, the version card and the synced note, with no page header and no horizontal overflow.
- `F09.hub-resize-redirect`: widening the window to ≥1024 px while the phone hub is open replaces `/settings` with `/settings/agents`; narrowing on a sub-page keeps its URL and adds the Back chevron.
- `F09.breakpoint-sweep`: at 767, 768, 820, 1023, 1024 and 1440 px, `/settings` is the hub below 1024 px and redirects to the desktop nav from 1024 px up, and the Application page's `main` is at least 340 px wide with no horizontal overflow. Below 1024 px the settings `<main>` fills the column down to the viewport bottom and scrolls itself, so a popover inside it (the tablet selector) is not clipped on a short page (#18256).
- `F09.tablet-selector`: at 768–1023 px, a Settings selector above the subpage opens the same visible section links as the desktop sidebar, including Cloud links when available. Selecting a sibling navigates directly and closes it; Tab reaches links, Escape closes and returns focus, and outside clicks dismiss it. The current section uses a slash-boundary match, so a trailing slash does not hide the selector or its selected link. It is absent on the hub, phones and desktops.
- `F09.phone-back`: at phone width each settings page shows a Back chevron (label "Settings") in the top bar that returns to the hub.
- `F09.synced-badge`: the nav footer reads "These settings are synced from Local backend (<origin>)" for the active backend.
- `F09.cloud-links`: on a Cloud backend only, the nav adds "Integrations" and "All Cloud Settings" external links; on a local backend neither exists.
- `F09.update-card`: the "Agent Canvas / Version x.y.z" card opens a dialog; its "Up to date" / "Update" badge appears only once the npm check succeeded.
- `F09.update-modal`: the dialog shows the version, a "Check for updates" button that re-runs the check, and an inline status; a failed check reads "Couldn't check for updates. Try again later."; Close and Escape dismiss it.
- `F09.update-up-to-date`: when the npm check succeeds and the client is current, the card badge reads "Up to date", the dialog status reads "You're running the latest version." with a "What's new" link to the GitHub releases page (new tab), and "Check for updates" briefly shows "Checking for updates…".
- `F09.update-available`: when npm has a newer version, the dialog shows "Version … is available.", a "What's new" link, and npm/Docker command tabs with a copy button.
- `F09.update-modal-phone`: the update dialog fits a 390 px (and a 320 px) viewport with its title and Close button on screen, and Close dismisses it.
- `F09.sidebar-version-tile`: the main sidebar shows an "Agent Canvas" version tile only when an update is available.
- `F09.unknown-subpath`: an unknown `/settings/<x>` URL shows the app's "Page not found" page inside the app shell (sidebar and a Home link), without the settings shell.


════════════ F10-llm-profiles.md

- `F10.list`: the "Available Profiles" list shows each row's name, model, a `Default` badge on the active profile and a `...` menu, sorted by name; reachable from the sidebar, the direct URL and the command menu.
- `F10.list-empty`: with no profiles the list reads `No profiles saved yet. Add a profile to save your LLM configurations.` and both Add buttons stay available.
- `F10.list-states`: a spinner shows while profiles load; a load failure shows `Failed to load profiles` in the theme's error colour.
- `F10.list-grouped`: once any profile links to a provider connection, rows group under the connection's name, with unlinked rows last under `Not linked`; with no linked rows the list is flat again.
- `F10.broken-link`: a profile whose linked connection no longer exists shows a warning `Broken link` badge whose tooltip says to edit the profile to re-link it; its editor offers the orphaned id in the Provider connection selector so it can be cleared.
- `F10.actions-menu`: the row menu offers Edit, Rename, Duplicate, Set as default (disabled on the Default row) and Delete; it focuses Edit on open (mouse or Enter), arrows cycle over the enabled items (skipping a disabled Set as default), and Escape, Tab or an outside click close it.
- `F10.set-default`: Set as default toasts `Switched to profile "<name>"`, moves the badge, persists and becomes the agent's active LLM.
- `F10.duplicate`: Duplicate saves `<name>-copy`, then `<name>-copy-1`, with the same model and stored key; the copy is not Default.
- `F10.rename`: the Rename modal is prefilled and focused; invalid names disable Rename; an unchanged name or Cancel just closes; Enter submits; success toasts and persists, and the Default badge follows a renamed default.
- `F10.rename-conflict`: renaming onto an existing name is refused with the server's message (`Profile '<name>' already exists`) and the modal stays open.
- `F10.delete`: Delete asks `Are you sure you want to delete the profile "<name>"? This action cannot be undone.` with Cancel focused; Cancel keeps the row, Delete removes it with a toast.
- `F10.delete-default`: deleting the Default profile promotes another remaining profile to Default.
- `F10.create`: Add LLM Profile → provider, model and API key → Save shows `Validating...` then `Saving...`, toasts `Profile "<name>" created` and returns to the list; the first profile on an empty list becomes Default.
- `F10.create-prefill`: a new editor prefills the backend default model with a blank key, base URL and connection; the other LLM options it shows come from the agent's current LLM settings, i.e. the last applied Default profile (the editor opens on the tab they need), and Save stores what the form shows.
- `F10.api-key-help`: under the API key field (Basic and Advanced), `Don't know your API key? Click here for instructions` opens the OpenHands docs on getting an API key in a new tab.
- `F10.name-autofill`: the Profile Name is auto-filled from the chosen model and follows later model changes until the user types a name; clearing the field resumes it.
- `F10.name-validation`: a name that breaks the rule (letter or digit first; letters, digits, `.`, `_`, `-`; max 64) disables Save and turns the rule text red; a name that already exists marks the field invalid, shows a taken-name message about LLM profiles and disables Save (Edit accepts the profile's own name).
- `F10.create-validation`: a Save whose validation call fails toasts the provider error and keeps the editor open with nothing saved.
- `F10.custom-model`: the Advanced tab takes a free-text model and Base URL; an empty model is refused with `Model is required`.
- `F10.model-picker`: the Basic provider and model comboboxes group options as Verified Models and Other Models; changing provider clears the model; the OpenHands provider shows account and key help.
- `F10.provider-list-complete`: the Basic provider combobox offers every provider the backend reports or has verified models for (over a hundred, `xai` included); picking a late-sorting one fills the field and loads its models.
- `F10.cloud-provider-pagination`: on a Cloud organization backend the provider list is fetched from `providers/search` page by page, so providers past the first page (`xai`, `openrouter`) are offered too (blocked here).
- `F10.edit`: Edit loads the profile (`Editing profile "<name>" - save to apply changes`) with the stored key masked; Save stays disabled until something changes; a changed name renames before saving; the stored key is kept.
- `F10.basic-save-keeps-base-url`: re-saving a profile from the Basic view without changing its model keeps the Base URL that view hides.
- `F10.edit-default-reapply`: saving the Default profile re-applies it to the agent's LLM settings.
- `F10.editor-discard`: Back and Cancel return to the list without saving and without an unsaved-changes prompt.
- `F10.schema-validation`: bad values in schema fields (Temperature over 2, Top P over 1, invalid JSON) are refused with a toast naming the field.
- `F10.connection-link`: with connections present the editor shows a Provider connection selector; linking hides API key and Base URL, skips validation on Save and regroups the row; None restores the key field.
- `F10.add-models`: "Add from provider connections" (or a connection row's **Bulk add**) creates one linked or keyless profile per checked model; models that already have a profile of that name are hidden.
- `F10.subscription`: Authentication → ChatGPT subscription swaps the model fields for a subscription model list and a device-login card (code, sign-in link, poll, cancel); switching back restores the API-key model.
- `F10.schema-unavailable`: a backend without the settings-schema endpoints shows `SDK settings schema unavailable.` with an upgrade or session-key hint.
- `F10.cloud-readonly`: Cloud org members without edit rights see the list read-only (no Add buttons, no row menus, no Provider connections section).
- `F10.phone`: list, row menu and editor fit a 390 px viewport without horizontal overflow.
- `F10.default-used`: a conversation started after creating a profile in the UI runs on the Default profile's model.


════════════ F11-provider-connections.md

- `F11.list-empty`: with no connections the section shows the heading `Provider connections`, the subline `Share one API key across multiple models.`, **Add connection** and `No provider connections yet. Add one to share an API key across models.`
- `F11.list`: each row shows the name, the provider, `<n> model(s)` (profiles linked to it), a green key-set icon and a `...` menu labelled `Provider connection menu`; the section is reachable from the settings nav, the command menu and the URL.
- `F11.create-validation`: in **Add provider connection** focus starts on Name; Save stays disabled until Name, a picked Provider and API Key are non-blank (whitespace does not count); a provider typed but not picked is cleared; Enter does not submit.
- `F11.provider-picker`: the Provider combobox lists `Verified Models` (OpenHands, Anthropic, OpenAI, …, deepseek, …) and `Other Models` groups and filters as you type.
- `F11.create`: Save creates the connection (toast `Connection "<name>" created`); the row persists after a reload and a backend restart; the key is stored but never shown.
- `F11.create-cancel`: Cancel, Escape and a backdrop click close the modal without creating anything; reopening starts empty.
- `F11.create-error`: a server rejection (for example a name over 128 characters) keeps the modal open and toasts a readable message.
- `F11.row-menu`: the `...` menu offers **Bulk add**, **Edit**, **Delete**; Escape, a click outside or a second trigger click close it; ArrowUp/ArrowDown move and wrap; Tab closes it.
- `F11.row-menu-focus`: opening the menu moves focus to its first item (**Bulk add**).
- `F11.edit`: **Edit** opens `Edit provider connection` prefilled (key blank with placeholder `<hidden>` and hint `Leave blank to keep the current key.`); rename, provider and base URL changes persist (toast `Connection "<name>" updated`), a cleared base URL is stored as none, a blank name disables Save, and a rename shows in the linked profiles' group header.
- `F11.add-models`: **Bulk add** opens `Add models as profiles` with the row's provider and connection preselected; the profiles it adds are linked and the row's count goes up.
- `F11.agent-uses-connection`: a conversation on a profile linked to the connection runs with the connection's key; an Edit with the key left blank keeps it working.
- `F11.rotate`: typing a new key rotates it for new conversations: an invalid key makes the next conversation fail with the provider's authentication error and no reply; rotating back restores replies.
- `F11.delete`: **Delete** asks `Are you sure you want to delete the connection "<name>"?` (focus on Cancel); Cancel and Escape keep it; Delete removes it (toast `Connection "<name>" deleted`).
- `F11.delete-referenced`: deleting a connection that profiles or the active settings reference is refused with the server's message naming them; the dialog stays open and the row stays.
- `F11.delete-stale-reference`: once its last linked profile is deleted (row shows `0 model(s)`) the connection can be deleted, or the UI says how to clear the remaining reference.
- `F11.phone`: at 390 px the rows, the row menu and the modals fit without horizontal overflow.
- `F11.load-error`: if listing connections fails (for example an unreadable connections store, `GET /api/llm/provider-connections` 400), the section shows `Failed to load provider connections.` (`provider-connections-load-error`) in red while the profiles list and **Add connection** still render.
- `F11.long-name`: a name at the 128-character limit is accepted; the row truncates the name and provider with an ellipsis, shows the full name as a hover tooltip (`title`) and keeps the `...` trigger in view at desktop and phone width.
- `F11.edit-unlisted-provider`: a connection whose provider is not in the provider catalog (created through the API) opens in Edit with that provider preselected and listed under `Other Models`, and saving keeps it.
- `F11.cloud`: on Cloud the section appears only with an organization bound and for members allowed to manage profiles; local users always see it.


════════════ F12-model-router.md

- `F12.page`: Settings → **Model Router** opens `/settings/meta-llm` with the subtitle `Meta-profiles that route each task to the right LLM profile.`; the command menu offers a **Model Router** command that opens it, like the other settings pages.
- `F12.empty-states`: with no LLM profiles the page says `Create LLM profiles first — meta-profiles route between your saved LLM profiles.`; with no routers it says `No meta-profiles yet. Add one to route tasks across your LLM profiles.` and **Run on first message** is off and disabled.
- `F12.load-error`: when only the meta-profile request fails, a red generic error shows under the header.
- `F12.unsupported`: an agent server without `/api/meta-profiles` (404) shows `This backend doesn't support model routing yet. Update the agent server to manage meta-profiles.`
- `F12.cloud`: a Cloud backend without an organization shows `Meta-profiles are only available for local backends.`; an organization-bound Cloud backend shows the full page.
- `F12.template-chooser`: **Add Model Router** opens `Choose Model Router template` (Add OpenHands Router Pro, Add OpenHands Router Flash, Custom, Cancel); Cancel, Escape and a backdrop click close it; Pro and Flash prefill the name, classifier `minimax-m3`, their own long routing prompt and the same 11-model table.
- `F12.template-custom`: **Custom** opens a blank editor; the placeholders show an example prompt and a two-line example model table.
- `F12.editor-validation`: Save stays disabled until the name is valid, a classifier is set and the prompt contains `{{ instance_text }}` (spaces inside the braces optional).
- `F12.duplicate-name`: a new router cannot take an existing router's name (`A meta-profile with this name already exists...`, Save disabled).
- `F12.add-connection`: **Add connection** in the create editor opens `Add provider connection`; saving it selects the new connection in the picker.
- `F12.add-connection-cancel`: **Cancel** in `Add provider connection` closes it without creating a connection and keeps the editor's values and picker choice.
- `F12.editor-cancel`: while the create or edit editor is open it replaces the list, **Add Model Router** and the switch; **Cancel** returns to the list without saving anything.
- `F12.router-profiles`: with a connection picked under **Create missing router LLM profiles using**, Save creates an LLM profile `<provider>/<name>` linked to that connection for every model-table name (and the classifier) that does not exist yet; **Don't create profiles** creates none.
- `F12.create-first`: saving the first router lists it, makes it Active and turns **Run on first message** on.
- `F12.create-more`: saving another router lists it and leaves the Active badge where it was.
- `F12.list`: each row shows the name, `<classifier> · Direct prompt`, an **Active** badge on the active router and a `...` menu; routers and the active choice survive a stack restart.
- `F12.row-menu`: the row menu offers Edit, Set active (disabled on the active row) and Delete, and closes on Escape.
- `F12.activate`: **Set active** toasts `Meta-profile "<name>" activated` and moves the badge.
- `F12.edit`: **Edit** opens `Edit meta-profile` with the name locked and no connection picker; changes persist.
- `F12.delete`: **Delete** asks `Are you sure you want to delete "<name>"? This cannot be undone.`; Cancel keeps the row, Delete removes it.
- `F12.delete-active`: deleting the active router leaves no router active; the switch shows off and disabled, and new conversations get neither the tool nor the first-message instruction.
- `F12.run-first-message-toggle`: the switch saves immediately (no Save button, no toast) and survives a reload.
- `F12.route-first-message`: with a router active and the switch on, a new conversation routes its first message (`route_task_to_model` action, `Switched to profile <name>` in the chat); with the switch off the tool is still attached but the instruction is absent.
- `F12.light-theme`: in a light colour theme the list heading and router names are readable.
- `F12.phone`: the list, template modal, editor and row menu fit a 390 px viewport without horizontal overflow.


════════════ F13-agent-profiles.md

- `F13.list`: under **Available Profiles**, rows (sorted by name) show the profile name, then its LLM profile (OpenHands) or `ACP (external subprocess)`, a **Default** badge on the active profile and a `...` menu (Edit, Set as active, Delete).
- `F13.create`: **Add agent profile** opens the editor; Save toasts `Profile "<name>" created` and the row is listed after a reload.
- `F13.name-validation`: an empty or malformed name marks the field invalid (red rule) and disables Save; a name another profile already has also marks the field invalid, shows a red taken-name message about agent profiles under it (the rule stays grey) and disables Save (Edit accepts the profile's own name).
- `F13.editor-no-llm`: with no LLM profiles, an OpenHands profile shows `Create an LLM profile first to define an OpenHands agent.` and Save toasts `Select an LLM profile for this agent.` without creating anything.
- `F13.openhands-options`: OpenHands profiles offer an LLM profile dropdown (the active LLM profile by default), a **System prompt** choice (`OpenHands default`, `Default + your instructions` or `Custom persona`, each saved on the profile), **Parallel tool calls** (default 1, minimum 1) and **Tools**: `Standard tools` (default) lists the server's standard set read-only, `Choose tools` turns the server's tool catalog into switches; values persist and reopen.
- `F13.edit`: Edit reopens the stored values with Save disabled until something changes; renaming keeps the profile id; Back and Cancel leave without saving.
- `F13.stale-llm-ref`: editing a profile whose LLM profile no longer exists preselects the active LLM profile and enables Save, so saving heals the reference.
- `F13.set-active`: **Set as active** toasts `Switched to profile "<name>"`, moves the Default badge and is disabled on the active row.
- `F13.active-drives-conversation`: a conversation started from Home after activation launches from that profile (its id and settings are stamped on the conversation).
- `F13.llm-pill-precedence`: when the active profile pins a different LLM profile than the Home LLM pill, the launch uses the pill's model and not the profile's other settings (by design).
- `F13.mcp-scope`: **MCP servers** offers **All servers** (every configured server listed, switched on and disabled) or **Choose servers** (a switch per configured server); the choice persists; a chosen server that was removed stays listed with a red warning, and launching with it is refused.
- `F13.secrets-scope`: **Secrets** offers **No profile restriction** (every switch on and disabled) or **Choose secrets**; on an ACP profile, Choose preselects that provider's secret names; the selection persists.
- `F13.acp-form`: choosing **ACP (external subprocess)** shows Preset, Command, Credentials and Model; each preset prefills its command and its model choice (`Agent default`, or Gemini CLI's preferred `gemini-2.5-pro`), **Custom** empties the command and disables Save until one is typed, typing a preset's exact command selects that preset, and the Model list's **Custom** opens a free-text model field.
- `F13.acp-local-presets`: on a local backend the Preset list also offers **Pi** and **OpenCode** (with their own commands and credentials); a profile saved on them stores `acp_server` `pi`/`opencode` with `acp_command` `null`. On Cloud both are hidden (blocked here).
- `F13.acp-model-discovery`: for a built-in preset with its default command the Model list comes from the agent: `Asking <agent> which models it offers…` while the agent-server asks it, then its models with `Agent default (<model>)` first; an agent that wants a login says `Sign in or add credentials above to see the models <agent> offers.`, one that cannot start says `<agent>'s models appear here after your first conversation with it. …`; a picked model is saved as `acp_model` and reopens selected.
- `F13.acp-credentials`: built-in presets show a Credentials section (password fields), an auth banner (signed in / configured), a conflict warning for mutually exclusive Claude credentials, and `Already saved — leave blank to keep` for stored values. Credentials are saved as global secrets.
- `F13.acp-preset-credentials`: the Credentials fields follow the preset: Claude Code `CLAUDE_CODE_OAUTH_TOKEN`, `ANTHROPIC_API_KEY`, `ANTHROPIC_BASE_URL`; Codex `OPENAI_API_KEY`, `CODEX_AUTH_JSON` (multi-line), `OPENAI_BASE_URL`; Gemini CLI `GOOGLE_APPLICATION_CREDENTIALS_JSON` (multi-line), `GOOGLE_CLOUD_PROJECT`, `GOOGLE_CLOUD_LOCATION`, `GOOGLE_GENAI_USE_VERTEXAI`, `GEMINI_API_KEY`, `GEMINI_BASE_URL`; Pi `PI_AUTH_JSON` (multi-line), `ANTHROPIC_API_KEY`; OpenCode `OPENCODE_AUTH_CONTENT` (multi-line), `OPENCODE_API_KEY`; **Custom** has no Credentials section. With no host login and no stored credential (Gemini here) no auth banner shows.
- `F13.menu-keyboard`: opening the row's `...` menu focuses Edit; ArrowDown/ArrowUp move focus between the enabled items (wrapping, and skipping the disabled **Set as active** on the active row), Enter runs the focused item, and Tab, Escape or a click outside close the menu.
- `F13.delete`: Delete asks for confirmation; Cancel keeps the row, Delete removes it. Deleting the active profile leaves none active; deleting the last profile makes the server re-seed `default`.
- `F13.phone`: the list, row menu and editor fit a 390 px viewport without horizontal overflow.
- `F13.acp-conversation`: a conversation launched from an active ACP profile runs the external agent. Blocked without an approved provider account.
- `F13.cloud-read-only`: on a Cloud backend, organization members see rows without the `...` menu or **Add agent profile**. Blocked without a Cloud account.


════════════ F14-secrets.md

- `F14.list`: the Secrets page lists each secret's name and description with Edit and Delete actions; the launcher-seeded `OPENHANDS_AUTOMATION_API_KEY` is always present.
- `F14.create`: adding a secret persists it; it is listed after a reload.
- `F14.create-validation`: duplicate names, names that break `^[a-zA-Z][a-zA-Z0-9_]{0,63}$` and empty values are refused with a message.
- `F14.edit`: editing the description with the value left blank keeps the stored value; Save stays disabled until something changes.
- `F14.delete`: deleting asks for confirmation; Cancel keeps the row, Confirm removes it for good.
- `F14.agent-access`: a conversation started after the secret was saved sees it as an environment variable.
- `F14.phone`: the page and its forms fit a 390 px viewport without horizontal overflow.
- `F14.form-back`: the Add and Edit forms replace the list with an **Add a Secret** / **Edit a Secret** title and a **Back** button; Back (like Cancel) returns to the list and discards the draft.
- `F14.rename`: the Edit form's name is editable; renaming with the value left blank keeps the stored value under the new name, and renaming onto an existing name is refused with `Secret already exists`.
- `F14.edit-value`: typing a value in the Edit form (labelled "Secret Value (leave blank to preserve the existing value)") replaces the stored value for new conversations.
- `F14.delete-escape`: Escape closes the delete confirmation without deleting.
- `F14.agent-request`: in a local conversation the agent's `request_secret` tool opens **The agent needs a secret** over the chat with its reason and the name prefilled; Add secret saves it, attaches it to the running conversation and reports `saved` to the agent, Cancel reports `not_saved`, and a name that is already saved is attached without a dialog. The value never reaches the agent's events.


════════════ F15-agent-behavior-settings.md

- `F15.condenser-page`: Condenser shows its subtitle, Basic and All tabs (no Advanced), and in Basic only the **Enable Memory Condensation** switch.
- `F15.condenser-all-view`: the All tab lists the eight condenser fields with Title Case labels, plain descriptions and their stored values (Max Size `240`, Keep First Events `2`, …).
- `F15.condenser-save`: changing Max Size and saving shows **Saving...**, then the saved toast; after a reload the page opens on All with the new value.
- `F15.condenser-dependents`: turning condensation off hides the six size/reset fields (Condenser Kind stays); turning it back on un-dirties Save.
- `F15.condenser-kind`: Condenser Kind is a dropdown (**LLM Summarizer**, **None**, stored as `llm_summarizing` and `no_op`); the choice persists, and switching back restores the summarizer defaults.
- `F15.save-dirty-state`: Save is disabled on load, enabled after an edit, and disabled again when the edit is reverted by hand.
- `F15.save-errors`: a non-integer Max Size is refused in the browser with a toast; a negative one is refused by the server with a 422 toast; nothing is stored.
- `F15.view-scoped-save`: saving from a narrower tier keeps the stored values of fields that tier does not show.
- `F15.agent-context-page`: Agent Context shows a single **Persistent Agent Memory** switch with its help text and no tab bar.
- `F15.agent-context-save`: toggling Persistent Agent Memory and saving persists after reload.
- `F15.verification-page`: Verification shows Basic, Advanced and All tabs; with defaults every tier shows only **Confirmation Mode** and **Enable Critic**.
- `F15.verification-critic-fields`: Confirmation Mode reveals Security Analyzer (Advanced); Enable Critic reveals Iterative Refinement and Critic API Key (Basic) plus mode, server URL and model (All); Iterative Refinement reveals threshold and max iterations.
- `F15.confirmation-mode-save`: Confirmation Mode and Security Analyzer (conversation settings) persist after reload, alongside the agent-settings fields on the same page.
- `F15.confirmation-mode-effect`: with Confirmation Mode on and Security Analyzer **None**, a new conversation stops before its first command with **Do you want to continue with this action?** and Cancel/Continue; Continue runs it.
- `F15.critic-tuning`: Critic Mode is a dropdown (`Finish and Message`, `All Actions`); the mode, Critic Threshold and Max Refinement Iterations persist after reload, and an out-of-range threshold (`1.5`) is refused with the 422 toast.
- `F15.critic-save`: critic text fields persist after reload; the Critic API Key comes back masked; clearing the fields and turning the critic off restores the defaults.
- `F15.agent-context-effect`: with Persistent Agent Memory on, a new conversation's system prompt carries the `.openhands/memory/` instructions; with it off, it does not.
- `F15.tier-after-save`: after a save the tab bar re-derives the tier from the stored values without a reload: saving the last minor field back to its default moves the page from All to Basic.
- `F15.unsaved-navigation`: leaving a page with unsaved edits shows no prompt and discards them.
- `F15.command-menu`: the command menu offers **Condenser settings**, **Agent Context** and **Verification settings**, and each opens its page.
- `F15.direct-url`: each page renders when opened by URL.
- `F15.phone`: at 390 px the fields stack in one column without horizontal overflow.
- `F15.schema-unavailable`: a backend without the settings-schema endpoints shows `SDK settings schema unavailable.` with an upgrade hint instead of the form (blocked: needs such a backend).


════════════ F16-application-settings.md

- `F16.open`: the page opens from the settings navigation, the command menu and the direct URL; every section renders and **Save Changes** is disabled while nothing has changed.
- `F16.language`: choosing a language and saving switches the whole UI at once and persists after a reload; `<html lang>` and `<html dir>` follow the chosen language (`rtl` for Arabic).
- `F16.color-theme`: choosing a colour theme applies it immediately without Save, persists after a reload and on other pages, and is never sent to the backend.
- `F16.analytics`: the "Send anonymous usage data" switch saves with the form and sets browser telemetry consent to granted/denied.
- `F16.analytics-cloud`: on a Cloud backend the analytics switch is forced on and disabled.
- `F16.sound`: the "Sound Notifications" switch saves with the form; with it on, a sound plays when the agent finishes or needs input.
- `F16.checklist`: the "Show Getting Started checklist" switch hides or shows the sidebar checklist immediately, without Save, and survives a reload.
- `F16.title-model`: the Title generation model dropdown offers "Automatic (recommended)" plus one `name · model` option per LLM profile; the choice persists and new conversations still get an automatic title.
- `F16.title-model-fallback`: if the chosen profile is deleted (from LLM settings, or outside the UI), the dropdown shows Automatic and the form stays clean.
- `F16.manage-profiles-link`: "Manage LLM profiles" goes to `/settings/llm`.
- `F16.voice-endpoint`: Voice input Base URL, API Key (masked) and Model save on every keystroke in this browser only, independently of Save; the key lives in session storage.
- `F16.git-identity`: Git Username and Git Email save with the form, persist after a reload, and are what the agent's commits use.
- `F16.git-identity-validation`: an email without `@` is refused by native validation; empty fields save the defaults `openhands` / `openhands@all-hands.dev`.
- `F16.save-states`: Save is enabled only while a saved field differs from the stored value; while a save is pending the whole form, Save included, is replaced by the `app-settings-skeleton` (so the "Saving..." label in the source never shows); success toasts "Settings saved", failure toasts the server's message (`An error occurred` when it has none) and should keep the user's edits for a retry.
- `F16.keyboard`: every control, including the three switches, is reachable with Tab and exposed to assistive technology.
- `F16.phone`: the form fits a 390 px viewport without horizontal overflow and Save is reachable by scrolling.
- `F16.title-model-clear`: the Title generation model combobox has a clear (×) button, shown on hover; it resets the choice to Automatic (recommended).
- `F16.dropdown-filter`: typing into the Language (or any) combobox filters its options; Escape restores the stored value and leaves Save disabled.
- `F16.backend-switch`: switching the active backend in the backend selector while this page is open shows the new backend's stored preferences (the footer already names it), also when switching back to a backend the page has already loaded; an unsaved edit is dropped on switch.
- `F16.browser-scope`: backend-saved preferences (language and the rest of the Save group) follow the user to a fresh browser profile, while theme and checklist visibility return to their defaults there.


════════════ F17-mcp-servers.md

- `F17.customize-redirect`: on desktop, the sidebar **Customize** link, the URL `/customize` and the command-menu items **Customize** and **MCP servers** all land on `/mcp`.
- `F17.desktop-subnav`: from 1024 px up, Customize pages show a left aside headed **Customize** with MCP Servers, Skills, Plugins and Apps, the active row marked, and the "synced from" backend badge.
- `F17.mobile-hub`: below 1024 px (phones and tablets), `/customize` is a hub with the same four rows and badge; at phone width detail pages show a Back chevron to the hub; widening the window to 1024 px or more on `/customize` redirects to `/mcp`.
- `F17.breakpoint-sweep`: at 767, 768, 820, 1023, 1024 and 1440 px, `/customize` is the hub below 1024 px and the desktop aside from 1024 px up, and the MCP page keeps **Add custom server**, a search box wider than 150 px and no horizontal overflow.
- `F17.tablet-selector`: at 768–1023 px, the compact Customize selector exposes the same MCP, Skills, Plugins and Apps links as the desktop nav; it follows Cloud visibility/external-link rules, dismisses on selection/outside click/Escape, and supports Tab navigation. It is absent on phones, desktops and the hub.
- `F17.cloud-nav`: with a Cloud backend active, Plugins and Apps are hidden and Skills becomes an external link to `<cloud host>/settings/skills` (blocked here).
- `F17.page-states`: `/mcp` shows the **Model Context Protocol (MCP)** header, **Add custom server**, the toolbar, an **Installed** section (empty-state copy when nothing is installed) and the **Library** grid, without page errors.
- `F17.search`: one search box filters the library and the installed list (catalog name, description and args count); no match shows `No matches for your search.` in each section; the X clears it.
- `F17.section-filter`: the All / Installed / Library dropdown shows only the chosen sections.
- `F17.library-catalog`: library cards (logo, name, transport, description, + toggle) open the install modal by click, + toggle or Enter; the modal closes with Cancel, X or Escape.
- `F17.install-remote`: a remote (HTTP) entry shows its fixed URL read-only and a `type="password"` credential field that is required.
- `F17.install-oauth`: an entry whose server runs OAuth itself shows the OAuth info panel instead of a key field.
- `F17.install-unsupported`: entries the local backend cannot install (provider-OAuth only, 12 today such as Sentry) should be hidden or explain themselves; today their modal is empty and Install does nothing (fail).
- `F17.install-args`: an entry with argument fields (Filesystem's **Paths (space separated)**) requires them and appends each whitespace-separated token to the stored command's args.
- `F17.install-stdio`: a STDIO entry shows its read-only command; Install shows `Verifying…`, `Saving...`, toasts `MCP server saved.` and adds a card with the catalog name and description.
- `F17.install-error`: a failed install test keeps the modal open with a red error and saves nothing.
- `F17.save-as-secret`: password credential fields carry **Also save as secret** (checked by default, with a tooltip); label clicks and Space change the native checkbox, while its help button leaves the choice unchanged. The selection follows the colour theme and long secret names fit on phones. After install the value is listed in Settings → Secrets.
- `F17.custom-validation`: the custom editor refuses bad input with an inline message (URL required/invalid/wrong protocol, name invalid or duplicate, command required or with spaces, env/header not `KEY=value`, header required, OAuth secret without client ID, timeout out of range). Malformed header lines use a header-specific message, distinct from environment-variable errors.
- `F17.test-connection`: **Test connection** in the editor shows `Verifying…`, then a green `Connected — N tool(s) available` or a red categorized error; Add/Save tests first and stays open on failure.
- `F17.custom-add`: **Add Server** saves and closes; the card is listed after a reload.
- `F17.custom-add-remote`: a custom Streamable HTTP (SHTTP) server with a **Bearer token** connects, saves as an `HTTP` card showing its URL, reopens with the token masked, and its tools reach the agent as `<server>_<tool>`.
- `F17.server-health`: each card's health row runs **Test connection** (`Checking connection…`), then shows a verdict with a coloured dot; a failure is red, worded for the transport (a STDIO command that cannot start, a remote server that cannot be reached) with the Agent Server's detail, and offers **Retry**.
- `F17.enable-disable`: the card toggle disables a server without removing its configuration; the state persists and a disabled server is withheld from new conversations.
- `F17.custom-edit`: clicking a card opens **Edit MCP server** prefilled (secrets shown as `**********`); Save tests and persists the change.
- `F17.stored-secret-reuse`: a saved server's env values and tokens come back from the settings API and in the editor as `**********`; the editor's Test connection swaps each placeholder for the stored encrypted value (never the placeholder text, never the plaintext) and Save leaves untouched secrets out of its sparse patch, so a Save from the edit form that keeps them changes only the edited fields.
- `F17.single-request-mutations`: adding, editing or deleting one server sends exactly one `POST`, `PATCH` or `DELETE` to `/api/settings/mcp/<name>` and leaves sibling servers and their stored credentials untouched.
- `F17.credential-probe`: a catalog entry whose server lists its tools with any credentials (Slack) also gets a read-only credential probe during Install and Test connection: a failed probe reads `Credential check failed: <provider error>` and saves nothing (Known failure, reproduced 2026-10-08: any errored probe call, a network `fetch failed` included, is worded as a credential failure; #18161). A hosted server that rejects the token at connect (GitHub) never reaches a probe and reads `Connection failed: … 401 Unauthorized …`, also saving nothing (Known failure, reproduced 2026-10-08: the Agent Server's `POST /api/mcp/test` returns the 401 as `error_kind` `unknown`, so the modal cannot call it a credential failure; OpenHands/software-agent-sdk#5607); a server the catalog does not know gets no probe.
- `F17.delete-server`: **Delete** in the editor asks for confirmation; Cancel keeps the server, Confirm removes it with `MCP server removed.`
- `F17.native-git-tabs`: on a Cloud backend, GitHub/GitLab/Bitbucket offer a recommended native-integration tab next to the MCP tab (blocked here).
- `F17.agent-uses-server`: a conversation started after the save can call the server's tools.
- `F17.phone`: the MCP page, the install modal and the custom editor fit a 390 px viewport.


════════════ F18-skills.md

- `F18.page`: the page shows the title, description, the notice "Skill changes apply to new conversations only.", an **Add skill** button, a result count, the facet rail and the card grid. (Empty state "No skills found." and loading skeletons are not reachable on a local backend.)
- `F18.search`: typing filters cards by name, description, content or trigger; after 300 ms the query is mirrored to `?q=` without a new history entry; the X clears it; no match shows "No skills match your search."
- `F18.search-history`: browser Back/Forward put the matching query back into the search box.
- `F18.facets`: the desktop facet rail (State, Recommendation, Source, Category, Type) filters with per-row counts, writes canonical URL params, disables zero-count rows and offers **Clear filters**; deep links apply and unknown values are dropped.
- `F18.filters-modal`: below 768 px the rail is replaced by a **Filters** button with an active-count badge that opens the same facets in a modal with Clear filters and Close.
- `F18.card`: each card shows icon, name, source, a two-line description and pills; extra pills collapse into a `+N` popover; click, Enter or Space opens the detail modal.
- `F18.copy-source`: personal and project skills (path sources) have a copy-path button on the card and in the modal that flips to "Copied to clipboard" for 2 s; built-in skills (source `public`) have none.
- `F18.toggle`: the card's plus/check toggle switches a built-in skill on or off; the choice survives a reload (`enabled_skills` allow-list on a local backend).
- `F18.toggle-local`: switching a personal/project skill off writes `disabled_skills`; new conversations then do not load it, and do again once it is back on.
- `F18.toggle-error`: a failed save shows an error toast and nothing is persisted.
- `F18.skill-in-chat`: an enabled skill is loaded into new conversations and fires on its trigger; a disabled one is not.
- `F18.project-skill`: a skill committed under `<workspace>/.agents/skills/<name>/SKILL.md` is loaded into every conversation started in that workspace, in Local Repo and New Worktree mode alike, and its trigger word activates it (the chat shows a **Skill Ready** row naming it); workspace skills are not listed on the Skills page.
- `F18.skill-deleted`: a personal skill whose folder was deleted disappears from the Skills page after a reload and is not loaded into new conversations: its trigger word activates nothing.
- `F18.personal-skill-dirs`: personal skills load from both user folders: `~/.agents/skills/<name>/SKILL.md` and the persistence folder's `skills/<name>/SKILL.md` (`~/.openhands/skills`, which a Canvas stack moves to `$OH_PERSISTENCE_DIR/skills`). Each shows on the Skills page with its path and fires on its trigger in new conversations.
- `F18.detail-modal`: the detail modal shows source, an Enabled/Disabled switch, description, pills and the read-only content; switching off disables **Use skill**.
- `F18.detail-close`: the X, **Close**, Escape and a backdrop click close the detail modal.
- `F18.detail-pills`: the modal shows the full pill set (type, category, recommended, license, compatibility, version, triggers); pills that do not fit collapse into a `+N` button whose popover lists the rest without closing the modal; Escape closes the popover first and returns focus to its trigger, and a second Escape closes the modal.
- `F18.use-skill`: **Use skill** closes the modal, opens `/conversations` and pre-fills the composer with `/<skill-name> `.
- `F18.add-skill-modal`: **Add skill** opens an instructions-only modal with a copyable `/add-skill` example (which must point at an existing skill), steps, URL formats, storage notes and a docs link; Close, X and Escape close it.
- `F18.install-banner`: after the agent installs a skill with `/add-skill`, the conversation shows "Installed to this workspace: …" with **Start new conversation with this skill** (same workspace) and a dismiss X (session-only).
- `F18.phone`: at 390 px the page, cards and both modals fit without horizontal overflow; `/customize` shows a hub that links to Skills and the header back button returns to it.
- `F18.cloud-link`: with a Cloud backend the Customize **Skills** item opens the Cloud skills page in a new tab instead of `/skills` (blocked here).


════════════ F19-plugins.md

- `F19.page`: `/plugins` shows the **Plugins** heading, the description "Browse, install, enable, and uninstall plugins. Enabled plugins load automatically into new conversations.", an enabled **Add plugin** button on a local backend, and the catalog as cards.
- `F19.search`: the search box filters by name, description, source, path and ref; a query matching nothing shows `No plugins match your search.`; the X clears the query.
- `F19.status-filter`: the All / Installed / Available / Local segments filter the grid; the active one has `aria-pressed="true"`; the filter resets to All on reload.
- `F19.detail-modal`: clicking a card (or Enter/Space on a focused card) opens a modal with name, source, description, **Skills in this plugin bundle**, **Files** and footer actions; X, **Close** and Escape close it.
- `F19.files-browser`: in the modal's Files tree, expanding a folder and selecting a file shows its highlighted content with line numbers; selecting it again closes the viewer.
- `F19.install-card`: **Install** on a card installs the plugin: toast `Plugin installed.`, the button becomes an enable switch, a version pill appears, the card sorts first and the install survives a reload.
- `F19.install-modal`: **Install** in the detail modal does the same for the open plugin; the modal stays open and switches to the installed footer.
- `F19.enable-toggle`: the card switch (named `Disable plugin` / `Enable plugin`) and the modal **Enabled/Disabled** switch turn an installed plugin off and on; the state persists after reload.
- `F19.enabled-autoload`: an installed, enabled plugin loads into every new conversation; disabled, it does not.
- `F19.refresh`: **Update** in an installed plugin's modal re-fetches it from its recorded source and shows `Plugin updated.`
- `F19.uninstall`: **Uninstall** removes the plugin at once (no confirmation), shows `Plugin uninstalled.`, closes the modal and returns the card to **Install**.
- `F19.add-plugin`: **Add plugin** opens **Add a plugin** with Source (required), Ref and Path (optional); Install stays disabled until Source has text; a valid source installs and closes the modal.
- `F19.add-plugin-cancel`: the Add modal's X, **Close**, Escape and a backdrop click close it without installing; reopening starts with empty fields.
- `F19.add-plugin-error`: an unusable source shows one error toast with the server's reason and keeps the modal open with the typed values; a plugin that is already installed (409) toasts `This plugin is already installed. Open its card to Update or Uninstall it.` instead (#18110).
- `F19.installed-coordinates`: a plugin installed with a source, ref and path shows that source on its card and in the modal header, and modal pills `v<version>`, `Ref <resolved commit>` and `Path <repo path>` next to the Enabled switch.
- `F19.local-plugin`: a plugin found in `~/.agents/plugins` shows a **Local** badge (no Install or switch), is listed under the Local filter, and its modal has a Local pill, Files, no Install/Uninstall and no Start Conversation.
- `F19.start-conversation`: **Start Conversation** in the detail modal opens `/launch?plugins=<base64 JSON>` for that plugin.
- `F19.launch-review`: `/launch` shows a modal titled `Launch <plugin>` (one plugin) or `Launch Plugin` (several), the optional `message` with HTML stripped and capped at 500 characters, and each plugin as `<source> / <path> @ <ref>`; X and Escape go to `/conversations`.
- `F19.launch-parameters`: plugins with `parameters` get an expanded, collapsible section with a text input, number input (blank becomes 0) or checkbox per value; edits are kept.
- `F19.launch-trust`: **Start Conversation** stays disabled until the trust checkbox (`I trust this plugin from <sources> …`) is ticked; then it creates the conversation (sending the message, if any), opens `/conversations/<id>`, and the plugin is loaded there.
- `F19.launch-dev-params`: `/launch?plugin_source=…&plugin_ref=…&plugin_repo_path=…&message=…` opens the same review for one plugin.
- `F19.launch-errors`: bare `/launch` or an empty array says `No plugins were specified…`; bad base64, invalid JSON, a non-array or an item without `source` says `The plugin configuration is invalid…`; **Go Home** opens `/conversations`.
- `F19.launch-creation-failed`: when conversation creation fails, the screen shows `Unable to Launch` / `Failed to Start Conversation` with **Go Home** and **Try Again**; Try Again reloads the page, which brings back the review modal with the trust box cleared.
- `F19.launch-unicode`: a deep link whose JSON contains non-ASCII text (UTF-8 base64) shows that text correctly.
- `F19.phone`: at 390 px the toolbar stacks, cards fit, and the detail, Add and launch modals stay inside the viewport.


════════════ F20-canvas-apps.md

- `F20.entry-points`: the Apps page is reached from the sidebar **Customize** link (lands on `/mcp`) then **Apps** in the Customize navigation, from the command menu's **Customize** command plus **Apps**, from the phone Customize hub, and by direct URL `/apps`; there is no bare `/extensions` page and no "Apps" command.
- `F20.page`: `/apps` shows the title **Apps for Agent Canvas**, a description, a **Build an app** docs link (new tab), an enabled **Add app** button, the amber trust notice, the **Installed apps** heading and, with nothing installed, `No apps are installed on this backend.`
- `F20.add-modal`: **Add app** opens a form with **App source** (required), **Ref** and **Path**; **Install** stays disabled while the source is empty or blank; **Close**, the X, Escape and a backdrop click close it.
- `F20.install`: installing from a local path shows **Installing…**, toasts `App installed. Review it here, then enable it when you are ready.`, closes the form and lists the app disabled (after reload too) with no rail entry; a local **App source** plus **Path** is joined into one path.
- `F20.install-error`: a failed install toasts the server's reason (`Could not read canvas extension source: …`) and keeps the form open with the input, for local paths and for unreachable Git sources alike.
- `F20.git-tree-url`: a Git host folder URL (`…/<owner>/<repo>/tree/<ref>/<path>`) pasted into **App source** is split into source, ref and path; the card shows the repo URL as source, **Ref** (resolved commit) and **Path**.
- `F20.git-ref-path`: the **Ref** and **Path** fields apply to a Git source, including the placeholder's `github:owner/repo` shorthand; the card keeps the source as typed and shows the resolved commit and the path. Enter in a field submits the form.
- `F20.install-duplicate`: installing an app whose name is already installed is refused (`409`): the form stays open with its input, the toast says the app is already installed and points to **Update** or **Uninstall** on its card, and the existing card is unchanged.
- `F20.card`: each card shows the display name, source, a `role=switch` (**Enable**/**Disable**), the description, `Disabled`/`Enabled`, `v<version>` and `Pages: N` pills, each page title, and **Update** and **Uninstall** buttons.
- `F20.enable-confirm`: switching a disabled app on (click or Space) opens a trust confirmation (`confirmation-modal`) with **Cancel** and **Enable trusted app**; Cancel, Escape and a backdrop click leave it disabled, confirming enables it (persists after reload).
- `F20.rail-entry`: an enabled app's page appears in the main sidebar rail as `sidebar-canvas-extension-<name>-<pageId>` linking to `/extensions/<name>/<path>`.
- `F20.rail-active`: on the app's page its rail entry is marked current (`aria-current="page"`, highlighted); with the sidebar collapsed the entry is an icon whose tooltip and `aria-label` carry the label. App pages are not listed in the command menu.
- `F20.rail-label`: the rail entry is labelled with the page's `nav_label` (`Extension demo` for the demo fixture), falling back to its title.
- `F20.page-render`: the rail entry and the direct URL mount the app into `main` named after the page title (demo: h1 `Hello from a Canvas Extension`); a deeper URL passes the rest of the path to the app (`Nested extension path: nested`).
- `F20.app-backend-view`: an app page learns from the host whether it has a backend view; the host offers one only when the Agent Server advertises the `canvas_app_backend_bridge_v1` capability with an app-backend ingress URL, so on the pinned server the demo's status line reads `App backend view unavailable` on every path of its page.
- `F20.page-unavailable`: an unknown app, an unknown page of a known app, or a disabled or uninstalled app shows **Not available** / `This app is disabled, missing, or does not provide this page.`
- `F20.phone`: at 390 px the Customize hub lists Apps, the Apps page, the Add form and an app page fit without horizontal overflow, and the app's entry is in the sidebar drawer.
- `F20.refresh`: **Update** re-installs from the recorded source, toasts `App updated.` and keeps the enabled state; a failed update toasts the error and leaves the card unchanged.
- `F20.persist-restart`: installed apps, their enabled state and rail entries survive a stack restart.
- `F20.disable`: switching an enabled app off happens immediately without confirmation, removes its rail entry and makes its page unavailable.
- `F20.uninstall`: **Uninstall** asks `Uninstall <display name> from this backend?`; Escape or Cancel keep the app, **Confirm** toasts `App uninstalled.` and removes the card and the rail entry.
- `F20.busy-lock`: while Update, Uninstall or Enable is pending, every card control is inert, the switch included.
- `F20.load-error`: when listing apps fails, the Installed section shows the error and a **Retry** button.
- `F20.unsupported`: on a Cloud backend, with no backend, or on an Agent Server without the apps API, the Installed section shows **Not available** with the reason and **Add app** is disabled; Cloud hides Apps from the Customize navigation.


════════════ F21-automations-dashboard.md

- `F21.sidebar-entry`: the sidebar **Automate** item opens `/automations` with the heading **Dashboard** and the Automate sub-page aside.
- `F21.command-menu`: the command menu offers **Automations** ("Review scheduled and webhook automations."), also found by the keywords automate, cron, schedule, webhook and jobs; choosing it opens `/automations`.
- `F21.pin-home`: the pin on the sidebar Automate item makes `/` redirect to `/automations`; unpinning restores the normal home.
- `F21.subpage-nav`: Dashboard and Templates sub-pages appear in a left aside from 1024 px up and in a horizontal strip below that (phones and tablets); the current page has `aria-current="page"`.
- `F21.manifest-404`: without an admitted automation interface manifest, `/automations` is a 404 from the layout error boundary, and all Automate entry points, including the Home checklist and recommendations, are hidden without error toasts. Known failure: Canvas hides the sidebar entry but leaves a dead Home checklist link and raises two manifest errors ([#18210](https://github.com/OpenHands/OpenHands/issues/18210); [draft fix PR #18212](https://github.com/OpenHands/OpenHands/pull/18212)).
- `F21.health-loading`: while the health check runs, the page shows the title and three card skeletons.
- `F21.list-loading`: once the backend is healthy, the header, tiles and controls render at once while three card skeletons stand in for the list until the first page arrives.
- `F21.backend-unavailable`: when the health check fails, the page shows **Automations Unavailable** with a Retry button.
- `F21.list-error`: when the list request fails with nothing cached, the page shows **Failed to load automations** with Retry.
- `F21.add-menu`: the header **Add Automation** menu offers **Create Automation** and **Import automation**; Create Automation opens the guided setup page at `/automations/setup` (F22).
- `F21.empty-state`: with zero automations and zero saved drafts the page shows **No automations configured**, the "Create an automation" instructions, a docs link, **Create Automation** (opens `/automations/setup`, F22, without creating a conversation) and the Recommended automations rail; the view toggle is disabled.
- `F21.empty-rail`: a Recommended automations card on the empty dashboard opens its template setup at `/automations/new/<template-id>` (F22 owns the setup flow).
- `F21.overview-tiles`: four tiles summarize the automations the groups list from the loaded org list (hidden DRAFT test automations from a draft Test are left out): Automations (`N active`), Needs attention (`Latest run failed` or `No latest-run failures`; an enabled automation counts when its latest run is Failed or its task outcome is `failed` or `blocked`, while Partial and Needs review do not count), Total runs and Average duration.
- `F21.search`: the search box narrows the visible automations client-side by name, prompt, repository or model, and the saved drafts by name, prompt, kind or trigger; the tiles do not change.
- `F21.filters`: the **Filters** popover filters by status (All / Active / Needs attention / Disabled) and trigger (All / Scheduled / Event-driven), counts non-default choices in a badge and offers **Reset all**; it stays open while the user changes several choices.
- `F21.sort`: Sort (Latest run / Most runs / Name) orders automations inside each group and counts as a non-default choice.
- `F21.filtered-empty`: when nothing matches, **No automations match these filters** and **Clear filters** appear; Clear filters resets search, status and trigger but keeps the sort.
- `F21.view-toggle`: the view-mode menu switches Grid and List; the choice survives a reload.
- `F21.card-grid`: a grid card shows name, prompt preview, trigger pills, sparkline, a status strip (latest run status, summary, relative time, or **No activity yet**) and Runs / Recent success / Avg. duration stats.
- `F21.list-row`: a list row shows a health dot, name, schedule or event, relative last run, run badge, sparkline, Play and kebab; its link is labelled `<name> <status>`.
- `F21.row-tooltip`: hovering a list row shows a preview card with the name, Trigger, Status and, once it has run, Last run and Task (the run summary); the Play button's tooltip reads **Run now**.
- `F21.summary-hovercard`: hovering a grid card's truncated run summary shows the full summary in a hovercard.
- `F21.sparkline-deeplink`: a sparkline bar links to `/automations/<id>?run=<runId>`, which opens the detail page with that run highlighted.
- `F21.open-detail`: kebab **View**, the row link, a card click and Enter on a focused card all open `/automations/<id>`.
- `F21.load-more`: past 50 automations a **Load more** button fetches the next page and disappears when everything is loaded.
- `F21.git-sync-button`: the header **Git Sync** button (manage permission; always on local) opens `/automations/git-sync` (see F24).
- `F21.kebab-menu`: the **Automation actions** menu lists Run now, View, Export, Edit, Turn on/Turn off and Delete, closes on Escape or an outside click, and flips above the trigger near the viewport bottom.
- `F21.run-now`: Play or kebab **Run now** on an enabled automation dispatches a run with the toast **Automation dispatched**; the card moves through Pending/Running to the result. Both are disabled on a disabled automation.
- `F21.run-now-error`: a failed dispatch shows exactly one error toast with the API message; when the automation is gone (`Automation not found`), its card then drops out of the dashboard without a reload.
- `F21.toggle`: kebab **Turn on** / **Turn off** flips the enabled flag immediately (no confirmation on the dashboard) and moves the automation between the Active and Inactive groups.
- `F21.edit`: kebab **Edit** opens the setup page in edit mode (`/automations/setup?automationId=<id>`) prefilled from the automation, with **Test**, **Save** and **Close** in its toolbar; Save sends one PATCH and toasts **Automation updated**, Close returns to `/automations` (field-level editing is F23). For a script (tarball) automation it should summarize the saved bundle (**Existing code bundle**). Known failure (reproduced 2026-10-13, #18341): from the kebab it shows generated starter Python and a default setup script instead; a cold load of the same URL shows the bundle summary.
- `F21.export`: kebab **Export** downloads `<name-slug>.automation.json` with `version`, `kind: "automation"` and the spec.
- `F21.delete`: kebab **Delete** asks `Delete "<name>"? This cannot be undone.`; Cancel and the X keep it, Delete removes it for good, and Escape dismisses the dialog.
- `F21.saved-drafts`: saved setup drafts (#17242) appear in a **Drafts** group above Active/Inactive, one row each with the name (Resume), a pencil (**Edit**), Play (**Test draft**, tooltip **Test Run**; disabled for event-triggered or non-dispatchable drafts) and a trash (**Delete draft**). Resume and Edit open `/automations/setup?draftId=<id>`; Test dispatches a test run (toast **Test run started.**) on a materialized DRAFT-state automation that the groups do not list; Delete asks **Delete draft?** (`Delete “<name>”? This removes the saved setup draft, but does not delete any final automations.`) with Cancel / **Delete draft** and toasts **Draft deleted.**, also removing the draft's materialized test automation. The confirmation is the named modal dialog **Delete draft?** (the automation delete dialog with draft copy): it opens with focus on Cancel, keeps Tab inside, closes on Escape, X, Cancel or a backdrop click and returns focus to the trash button.
- `F21.phone`: at 390 px the dashboard has no horizontal overflow, tiles wrap 2×2 and the kebab menu stays inside the viewport.


════════════ F22-automation-creation.md

- `F22.templates-page`: `/automations/templates` shows the h1 **Templates**, a description, a search box, **Start from a proven workflow** with a count badge (6) and a **Beta** section (17).
- `F22.template-card`: each card shows an icon or logo stack, name, category, two-line description, a plus badge and integration pills (`N MCPs to connect before launch`, `Needs external setup` for Jira/Bitbucket; `Connected` once installed, which needs a working integration and was not reached here); a `+N` overflow button opens a popover without launching the card.
- `F22.templates-search`: the search box narrows cards by name, category, description, launch prompt and integration names; a query that matches nothing should say so.
- `F22.template-launch-setup`: a card whose entry ships a setup form (custom-automation, news-digest, github-pr-reviewer, qa-changes, …) navigates to `/automations/new/<id>`.
- `F22.template-launch-conversation`: a card without a setup form and with no installable integration missing creates a conversation and pre-fills its composer with the entry's launch command (for example `/ticket-to-code-change:setup`); nothing is sent until the user presses send.
- `F22.template-mcp-install-queue`: a card with missing required integrations opens each one's install dialog in turn; Cancel abandons the launch without creating anything.
- `F22.responder-deployment-choice`: on a local backend a GitHub/Slack-only responder card first asks **Choose how your responder should run**; **Continue with local setup** creates the `OPENHANDS_URL` secret and continues, **Open OpenHands Cloud integrations** opens Cloud in a new tab, X and Escape cancel.
- `F22.responder-local-launch`: with the responder's integration already installed (Slack for **Slack standup digest**), **Continue with local setup** skips the install dialog and opens a new conversation whose composer is pre-filled with the entry's setup command (`/standup-digest:setup`). Nothing is sent until the user presses send; Send delivers it as the user message.
- `F22.setup-prerequisites`: **Before you start** lists the entry's integrations with the reason each is needed and **Manage integrations** (`/mcp`); Continue is disabled while a required integration is missing; an entry with nothing to check skips the step.
- `F22.setup-action-kind`: the **Action** combobox (custom-automation: Prompt / Plugin / Upload tarball) swaps the action fields and keeps shared values.
- `F22.setup-form-validation`: Continue checks fields locally (`This field is required.`, `Must be at least N characters.`, `Must be at most N.`) and a field blur sends the draft to the service, whose errors appear under the field (`Value error, Invalid cron expression: …`).
- `F22.setup-agent-profile`: the **Agent profile** combobox appears only for actions that create through `/v1` (Upload tarball and bundle entries), not for Prompt or Plugin; it lists `Default` and every saved agent profile and shows `Default` while no profile is chosen (also after a switch through Prompt or Plugin and after picking `Default`, #18111), picking a profile hides the **LLM profile** field, switching to Prompt or Plugin clears the choice, and the automation is created with that `agent_profile_id`.
- `F22.setup-plugin-create`: the **Plugin** action takes plugin sources and a prompt, and a picked **Timezone** carries through Review into the created automation (`preset_metadata.plugins`, `trigger.timezone`).
- `F22.setup-tarball-create`: the **Upload tarball** action requires a `.tar`/`.tar.gz` file, shows its file name in Review and creates an automation with the uploaded tarball and the entrypoint.
- `F22.setup-repository-list`: a repository list field has **Add** (disabled while the input is empty) and a **Remove** per row; an empty required list is refused.
- `F22.setup-review-confirm`: **Review** lists every value (`Not set` for blanks); **Back** keeps the values; **Confirm and create** creates the automation (enabled) and replaces the URL with its detail page.
- `F22.setup-bundle-create`: a bundle entry (Daily news digest) uploads its script tarball and creates an automation with the bundle's entrypoint, without any account.
- `F22.setup-unsupported`: an entry this deployment cannot run (qa-changes needs event triggers) shows **Not available**, the unmet requirements, **Close** and **Set up in a conversation**.
- `F22.setup-fallback-conversation`: **Set up in a conversation** creates a conversation seeded with the entry's setup message.
- `F22.setup-close`: X or Escape goes back in history; on a cold deep link it replaces the location with `/automations`.
- `F22.setup-unknown-id`: `/automations/new/<id>` for an id no catalog entry claims is a 404.
- `F22.agent-assisted-setup`: **Add Automation → Create Automation** opens `/automations/setup`, whose setup panel collects name, action, trigger, model/profile and custom code, can ask the agent for help, saves server-backed drafts, opens test runs behind a Back button and dispatches the finished automation.
- `F22.import-picker`: **Add Automation → Import automation** opens a dialog that explains the format (docs link), says the import is disabled, and offers a dropzone and **Choose file**.
- `F22.import-validation`: a file that is not JSON shows the toast `The selected file is not valid JSON.`; a JSON file that breaks the format shows one toast listing every issue; the dialog stays on the picker.
- `F22.import-preview`: a valid file switches the dialog to a preview (Name, Trigger, Prompt, Plugins) with the disabled notice, **Cancel** and **Import automation**.
- `F22.import-confirm`: **Import automation** reads `Importing...` while pending, closes the dialog, shows `Imported "<name>" as disabled. View automation`, and the automation is listed disabled; the toast link opens its detail page.
- `F22.import-choose-file`: **Choose file**, and a click anywhere on the dropzone, open the browser's file chooser; the chosen file is previewed.
- `F22.import-dropzone`: dragging a file over the dropzone highlights it (`data-active="true"`); dropping a `.json` file previews it like **Choose file**.
- `F22.phone`: the Templates grid, the setup dialog and the import dialog fit a 390 px viewport without horizontal overflow.


════════════ F23-automation-detail.md

- `F23.open-detail`: a dashboard card, a list row, the kebab **View** and a direct URL open `/automations/<id>`; browser Back from a run's conversation returns to it.
- `F23.back-link`: **Back to Automations** (on the page and on the not-found state) returns to `/automations`.
- `F23.loading`: `detail-skeleton` stands in while the automation loads.
- `F23.not-found`: an unknown id shows **Automation not found** with "The automation you're looking for doesn't exist or has been deleted." and the back link.
- `F23.backend-unavailable`: with the automation service down, the page shows **Automations Unavailable** and **Retry**; Retry recovers once the service is back.
- `F23.load-error`: when the health check is still good but the automation request fails, the page shows **Failed to load automations** and **Retry**.
- `F23.header`: the header shows the name, the Active/Inactive badge, **Run now** (disabled while inactive), the switch (`Turn on`/`Turn off`) and the **Automation actions** kebab with Export, Tarball, Edit, Turn on/Turn off and Delete. A DRAFT-state test automation shows a **Draft** badge (`active-status-badge-draft`), no switch, no paused banner and a kebab without Turn on/off.
- `F23.toggle`: the header switch flips the enabled flag at once (no confirmation); the badge, switch and Run now follow and survive a reload.
- `F23.disabled-reason`: after **Turn off**, the banner `automation-disabled-reason-banner` reads **Why this is paused** / **Turned off manually** / **Paused <time>**; it is gone while active.
- `F23.run-now`: **Run now** dispatches a run with the toast **Automation dispatched** and a Pending row appears in the log (the button reads **Starting…** only for the few ms of the request).
- `F23.run-now-error`: a failed dispatch shows exactly one error toast with the API message; when the automation is gone (`Automation not found`), the page then shows the **Automation not found** state.
- `F23.run-status-polling`: while a run is Pending or Running the log refetches every 3 s, so Pending → Running → Successful/Failed, the summary, the cost and the conversation link appear without a reload; the Activity card's **Last run** updates too.
- `F23.activity-log`: **Activity Log** shows **No activity yet** with Export disabled before the first run, then one row per run (newest first): start time, summary, cost (`$0.0000` format, `run-cost`), a logs icon, status badge (`run-status-icon-<status>`).
- `F23.run-task-outcome`: a Completed run whose agent finished with a task outcome other than success shows that outcome instead of **Successful**: `blocked` and `partial_success` read **Blocked** / **Partial** with `run-status-icon-warning`, `unknown` and any unrecognised status string read **Needs review** (`run-status-icon-needs-review`), and a finish response without a string `status` (summary only, custom fields only, a non-string status, or not an object) is expected to read **Successful**, and an `outcome_summary`, when it has one, is still shown (#17682); the row summary is the agent's `outcome_summary`, and the Logs dialog's Task section repeats it.
- `F23.activity-log-more`: with more than 20 runs a **Load more** button grows the page by 20.
- `F23.run-no-conversation`: a finished run without a conversation reads **No Conversation**; only a script run (no agent) gets "No conversation — this run executed a script. Use View logs for its output."
- `F23.run-linked-conversation`: a run row with a conversation is a link labelled `View conversation for run at <time>` to `/conversations/<conversation_id>`, whose title is `<automation name> — <UTC timestamp>`.
- `F23.run-logs-modal`: the row's terminal icon (**View logs**) opens the **Logs** dialog with Run, Task (status, summary, `automation-task-metadata`) and System sections and Output/Error tabs showing the run command's stdout/stderr; Escape and the X close it without navigating.
- `F23.debug-with-openhands`: in the Logs dialog of a **Failed** run, **Debug with OpenHands** starts a conversation seeded with the automation, the error and the run id, and opens it.
- `F23.activity-log-export`: **Export JSON** / **Export CSV** download `<name-slug>.activity-log.json|csv` with every run, including `conversation_url`.
- `F23.run-deeplink`: `?run=<runId>` highlights that run (`automation-run-highlight-<runId>`, class `bg-focus/10`) and scrolls to it; an unknown run id highlights nothing.
- `F23.prompt-collapse`: a prompt taller than about 10 lines is clipped to 240 px with a fade (`automation-prompt-fade`) and **View More**; **View Less** collapses it again.
- `F23.configuration`: a cron automation shows Trigger **Schedule**, the Schedule and **LLM profile** (`Active profile` or the profile name).
- `F23.configuration-event`: an event automation shows Trigger **Event**, **Event Source**, **Event Type** and **Filter**, truncated at 60 characters with **Show more**/**Show less**.
- `F23.plugins-repo`: an automation created with repositories and plugins shows **Repositories** (one `owner/repo` row each, with a branch badge for its ref) and a **Plugins** card with one chip per plugin.
- `F23.script-section`: a script (tarball) automation shows **Script** with the entrypoint and the bundle's files in place of the Prompt.
- `F23.export`: kebab **Export** downloads `<name-slug>.automation.json` (`version`, `kind: "automation"`, `spec`); an automation's first repository and its plugins go into `spec` as `repository`, `branch` and `plugins`.
- `F23.tarball`: kebab **Tarball** downloads the automation's code bundle.
- `F23.delete`: kebab **Delete** asks `Delete "<name>"? This cannot be undone.`; Cancel keeps it, Delete removes it and returns to `/automations`.
- `F23.edit-open`: kebab **Edit** opens the setup page `/automations/setup?automationId=<id>` (#17688; the **Edit automation** modal is gone) prefilled with Name, Prompt, agent profile and LLM profile pickers, the trigger (Schedule with Frequency, or Event), timezone and Timeout seconds, with **Test**, **Save** and **Close** in the toolbar and a docked composer; no conversation exists until a prompt is sent.
- `F23.edit-close`: **Close** leaves without saving and lands on `/automations` (the dashboard, not the detail page); Escape does nothing on the page.
- `F23.edit-validation`: an empty prompt is refused with the toast **Add a prompt before continuing.** (no request); a bad cron, a cron that never runs or a timeout above 1800 is sent to the API, refused with `422`, and its message is the single error toast (`Value error, Invalid cron expression: not a cron`, `Value error, Cron expression cannot produce any future fire times: 0 0 31 2 *`, `Value error, timeout must not exceed 1800 seconds (30 minutes)`); nothing is saved.
- `F23.edit-save`: Save sends one PATCH (also when nothing changed), shows **Saving…** then **Saved just now**, the toast **Automation updated**, and the detail page and API reflect it; an Active profile model stays `null`.
- `F23.edit-test`: on the edit page **Test** dispatches the automation as last saved (`POST /api/automation/v1/<id>/dispatch`, no PATCH first) and shows `Test run started.` on the form; unsaved edits are not part of that run. Whether it should test the current form is a product decision (Gotchas).
- `F23.edit-dirty-state`: the edit page shows no save state for unsaved edits (no **Unsaved changes**, which create mode shows, F28); the label appears only while saving (**Saving…**) and after (**Saved just now** or **Save failed**). **Close** discards the edits.
- `F23.edit-schedule`: Frequency offers Once, Hourly, Daily, Weekdays (Mon–Fri), Weekly and Custom; Weekly adds **Day of week** and **At**; they are saved as a cron expression and read back after a reload.
- `F23.edit-time-cleared`: clearing **At** on a preset schedule and saving should be refused with a message instead of being dropped or replaced. Known failure (reproduced 2026-10-13, #17948 item, new symptom since #17688): Save silently stores midnight (`0 0 * * 3`) and toasts **Automation updated**; Save draft in the create flow stores `0 0 * * *` the same way. Open PR #18260 (head `0f812ac68902d2c71896568311bffb8aa6f216a6`, checked 2026-10-15) targets the live setup panel: it rejects an invalid or cleared time before Save and Save draft, with an edit-mode regression test. It is not merged; the known failure above remains the observed behavior on this map’s baseline.
- `F23.edit-cron`: a custom cron schedule shows Frequency **Custom** with an editable **Custom** cron field and a timezone select.
- `F23.edit-event-readonly`: for an event automation the edit page shows Event selected with Event source, event key and filter controls (editable now, no longer read-only text) and no schedule fields.
- `F23.edit-save-error`: a failed save shows exactly one error toast with the API message (`Automation not found` for an automation deleted meanwhile), the save state reads **Save failed** and the edits stay on the page; the toolbar **Test** on that page also toasts `Automation not found`.
- `F23.phone`: at 390×844 the detail page has no horizontal overflow, and the Edit page fits with its title, Test, Save and Close reachable.


════════════ F24-git-sync.md

- `F24.open`: the **Git Sync** button on the Automate dashboard and the direct URL open the page (h1 `Git Sync`); **Back to Automations** returns to `/automations`.
- `F24.loading`: a skeleton (`git-sync-skeleton`) shows while health, permissions and status load.
- `F24.overview-unconfigured`: with nothing configured the card reads `Disabled`, `Not encrypted`, Repository `Not configured`, Branch `main`, Path `automations`, `Never synced` twice, `Manual only`, Pending `0`, and **Sync now** is disabled.
- `F24.form-dirty`: **Save Changes** and **Save and sync now** stay disabled until a field differs from the stored value (reverting disables them again); **Save and sync now** also needs **Enable Git Sync** on.
- `F24.check-failure`: a change to repository URL, branch or token is checked first (`Checking repository...`); an unreachable repo shows `Could not reach the repository with these settings` with git's output and the hint `Fix the settings above, or press Save again to store them anyway.`, saves nothing, and a second Save stores the values anyway.
- `F24.save-and-sync`: **Save and sync now** with a reachable repo saves (toast `Git Sync settings saved.`), then runs a cycle: the activity row goes `Syncing... started Ns ago` → `Sync complete`, and the card shows the short commit and `Last synced Ns ago`.
- `F24.sync-now`: a changed automation raises Pending changes (amber); **Sync now** shows `Syncing...` with `1 pending`, ends `Sync complete`, drops Pending to `0` and pushes `<path>/<slug>/automation.yaml` to the branch.
- `F24.sync-failure`: a failing cycle ends `Sync failed` and shows the **Last sync error** banner with git's message and its age; the banner survives a reload and disappears after the next successful cycle.
- `F24.encryption`: saving an encryption key turns the pill to `Encrypted` and the placeholder to `An encryption key is currently set`; automations exported afterwards are ciphertext; **Clear existing encryption key** disables the input and saving it returns to `Not encrypted`.
- `F24.token`: the access token is a password field that is never shown again; changing it runs the reachability check; **Clear existing token** disables the input and saves a cleared token.
- `F24.author`: commit author name and email are optional; an invalid email is refused by the browser before any request; saved values are used as the author of the next sync commit (the fields reload blank).
- `F24.interval`: **Sync every (seconds)** saves `N` as `Every Ns`, blank or `0` as `Manual only`, and refuses negative numbers.
- `F24.pause`: turning **Enable Git Sync** off and saving (no reachability check) shows `Disabled`, disables **Sync now** and keeps the configuration; turning it back on restores `Enabled`.
- `F24.sync-disabled-error`: **Sync now** on a page that still shows `Enabled` after sync was turned off elsewhere shows the toast `Enable Git Sync before triggering a sync.`; the pill catches up on the next idle poll (≤ 15 s).
- `F24.background-sync`: with an interval set, the backend runs cycles on its own and the open page picks up the new commit on its idle poll, without a reload.
- `F24.repo-link`: an http(s) repository is shown as a link (new tab) to the browsable URL without `.git` and without embedded credentials, and the link text drops the credentials too; a local path stays plain text.
- `F24.repo-link-ssh`: an scp-style ssh remote (`git@host:org/repo.git`) is also shown as a link, rebuilt over https (`https://host/org/repo`); the label keeps the configured value.
- `F24.sync-delete`: deleting a synced automation raises Pending changes; the next cycle commits the removal of its `<path>/<slug>/` folder.
- `F24.field-defaults`: emptying **Branch** or **Path** and saving clears the override instead of storing an empty value: the card and inputs fall back to `main` and `automations`.
- `F24.clear-repo`: emptying the repository URL is checked (`No repository URL is configured.`), and a second Save clears it: the card returns to `Not configured` and `Disabled`.
- `F24.backend-down`: with the automation service down the page shows **Automations Unavailable** with **Retry**; Retry after the service is back shows the page with the stored configuration.
- `F24.phone`: the page fits a 390 px viewport without horizontal overflow; the card's fields stay in two columns.
- `F24.unsupported`: an automation backend without the Git Sync API (status 404) shows `Git Sync is not available on this backend` with **Back to Automations**; the dashboard still shows the **Git Sync** button there.
- `F24.no-access`: a user without `manage_automations` (Cloud member) sees `Git Sync is managed by organization admins`, and the dashboard hides the **Git Sync** button.
- `F24.conflict`: saving a repository, branch and path that another organization already syncs is refused (409) with a toast naming the conflict.
- `F24.error-state`: any other status failure shows the generic automation error panel with **Retry**.


════════════ F25-backends-and-cloud.md

- `F25.selector-dropdown`: clicking (or hovering) the selector opens a listbox of backends, each option named `<status> <name>` with a status dot, and a footer with **Add Backend** and **Manage Backends**.
- `F25.add-backend-modal`: **Add Backend** opens "Choose how you want to connect" with a description, a **Learn more** link to the deployment docs and the tabs **OpenHands Cloud** (selected) and **Agent-server** (plus **Providers** with the bridge, see the Providers row). It takes keyboard focus on open (the close button), Tab and Shift+Tab wrap inside it, and at desktop width closing returns focus to the backend selector (at phone width the drawer closes with the modal and focus falls back to the page body). Escape does not close it; the X does.
- `F25.cloud-advanced-host`: on the Cloud tab, **Advanced** reveals a **Host** field (placeholder `https://app.all-hands.dev`) for a self-hosted Cloud.
- `F25.cloud-device-flow`: **Connect to OpenHands** opens a popup and shows `Starting authentication...`, then `Waiting for authorization...` with the verification link and **Cancel**; Cancel returns to the idle button and closes the popup. An unreachable host shows `Failed to start device flow: ...` with **Try again**. Approving the code (adds a backend named OpenHands Cloud) needs a Cloud account.
- `F25.agent-server-guidance`: the Agent-server tab has a **Local / Remote** toggle. Local shows a collapsible **Before you connect** with `agent-canvas --backend-only --port 8001` and a local-setup docs link; Remote shows **Recommended setup**, **Connection details** and a remote-setup docs link.
- `F25.add-agent-server`: **Connect** stays disabled until Host Name and a valid Host are filled; the API Key field is optional for Connect on Local and required on Remote, but a key-protected host still rejects an empty key. A dead host, an empty key or a wrong key shows `Could not connect to <host>` with the reason inline (`Disconnected (check URL or network)` for a dead host, `Invalid API key` for an empty or wrong key); a reachable host with the right key, entered on the default Local location, adds the backend, makes it active (BM-001) and redirects a `/conversations/<id>` page to `/conversations` without an error toast, also on the backend's first activation, without asking the new backend for the old conversation id.
- `F25.switch-backend`: choosing another option shows a full-screen `Switching to <name>` overlay for about a second, then the sidebar and pages show that backend's data; on `/conversations/<id>` the app moves to `/conversations` (BM-002).
- `F25.backend-pinned-url`: in-app links carry `?backend=<id>`; opening such a URL pins the tab to that backend; an unknown id is ignored.
- `F25.per-tab-backend`: the active backend is per browser tab: switching it in one tab leaves the other open tabs where they were, a new tab opened from a plain link starts from the last choice made in any tab, and a sidebar conversation row opened in a new tab (Control-click) lands on the backend that owns the conversation because its link carries `?backend=<owner>`.
- `F25.manage-backends`: Manage backends lists every backend with status dot, name, version (`v1.50.1`), host, status text and a LOCAL/CLOUD pill, plus Edit and Remove; it takes keyboard focus on open and keeps Tab inside; Escape, a backdrop click, X and **Done** close it and return focus to the selector.
- `F25.manage-add`: **Add Backend** in Manage backends opens the same Add form stacked over Manage; its X returns to Manage, and a successful add keeps Manage open with the new row while the new backend becomes active.
- `F25.manage-select`: clicking a healthy row makes that backend active and closes the modal; on a detail page it leaves it for that section's list, like the selector does (BM-002).
- `F25.edit-backend`: the pencil opens "Edit backend" pre-filled (key masked) with a `Connected · Local v<version>` badge; empty name and invalid host show `Name is required` and `Enter a valid URL (e.g. http://localhost:8080)` and disable Save; Save re-tests the connection, keeps the modal open with `Could not connect to <host>` on failure, and persists on success.
- `F25.edit-cancel`: **Cancel** and the X in Edit backend close only the Edit modal and discard the draft; Manage backends stays open with the stored values.
- `F25.edit-escape`: Escape in the Edit modal should not discard the draft (the modal disables Escape). Known failure, see Gotchas.
- `F25.health-status`: a backend that stops answering turns its dot red (`Disconnected`) within one health probe (every 30 s for local and remote agent servers; Cloud backends every 5 min and not on window focus or reconnect, #18128); its Manage row reads `Disconnected (check URL or network)` with a red detail line and cannot be selected.
- `F25.recovery-gate`: reloading while the active backend is down shows only `agent-server-onboarding-screen` with Manage backends in recovery mode (no X, no Done, Escape ignored, primary **Add Backend**); choosing a healthy row restores the app.
- `F25.remove-backend`: the trash icon asks `Remove backend "<name>"? If it is active, the app will switch back to Local.`; Cancel keeps it, Confirm removes it and an active removed backend falls back to Local (BM-003).
- `F25.collapsed-entry`: with the rail collapsed, the backend icon's popover offers the same Add Backend and Manage Backends items, and both modals open.
- `F25.phone`: at 390 px the drawer's selector opens Add and Manage inside the viewport without horizontal overflow. Edit backend fits at 320 and 390 px; on short viewports its form scrolls while the title and close control stay visible, and Cancel/Save remain reachable.
- `F25.device-verify-page`: `/oauth/device/verify` shows a "Device Authorization" code form; with `?user_code=` it shows "Device Authorization Request", the code, a Security Notice and Cancel / Authorize Device. On a local stack Authorize and Continue end in the `Error` card with **Try Again**.
- `F25.shared-conversation-view`: `/shared/conversations/<id>` is a standalone read-only page (no app shell); on a local backend it reads `Conversation not found`.
- `F25.share-publicly`: on a Cloud backend the conversation menu has a **Public Share** toggle with copy-link and open-link buttons; on a local backend the item is absent.
- `F25.cloud-org-rows`: each Cloud backend appears once per organization as `<name> – <org>` (`Personal Workspace` for the user's own), and choosing one scopes lists to that org.
- `F25.cloud-log-back-in`: a logged-out Cloud row shows `Logged out` and a **Log back in** button that reruns the device flow.
- `F25.cloud-settings-link`: with a Cloud backend active the gear next to the selector opens `<cloud host>/settings?org=<org>` in a new tab.
- `F25.cloud-sandbox-states`: Cloud conversations show waiting, archived and error sandbox states in chat.
- `F25.cloud-org-suspended`: when a Cloud call made for the selected organization (`X-Org-Id`) fails with 403 and `detail` `Organization is suspended` or `User membership is suspended`, the app is replaced by `<org> is suspended. Contact your administrator to restore access.` (or `Your access to <org> is suspended. Contact your administrator to restore it.`), then `Switch to another workspace` and one button per other organization (`Personal Workspace` for the user's own). A button selects that organization and the app returns; with no other organization only the message shows. A reload clears the in-memory record. Blocked here: needs an OpenHands Enterprise instance with `ENABLE_SUPER_ADMIN` where a Super Admin suspended the organization or this member's membership, signed in as a non-Super-Admin member with that organization selected (#17988).
- `F25.providers-tab`: only when the page has the Managed Agents bridge (`window.marsBridge`), Add Backend shows a third tab **Providers** ("Connect through DigitalOcean and more.") with a **DigitalOcean** card ("Your team's OpenHands agents") and `More providers coming soon.`. Every launcher-served web page has it (the ingress answers `/mars/health`); without it (`MARS_WEB=0`, or a non-loopback bind with no session key at all) there are only the Cloud and Agent-server tabs and no Managed Agents UI anywhere. A non-loopback bind that has a key (F26 `--host 0.0.0.0` with `LOCAL_BACKEND_API_KEY`) mounts the bridge behind that key and offers the tab too, although the page itself carries no key (see the sign-in bullet's Known failure).
- `F25.digitalocean-sign-in`: the DigitalOcean card opens "Connect your DigitalOcean team" with a Back button, a masked **Personal access token** field, a **Create a token** link to `https://cloud.digitalocean.com/account/api/tokens` and **Connect with token**, disabled until a token is typed (the **Sign in with DigitalOcean** OAuth button exists only in the desktop app). A token of the wrong shape reads `That does not look like a DigitalOcean personal access token.`; a well-formed token DigitalOcean rejects shows its HTTP error (`401 Unauthorized`). Back returns to the Providers tab.
- `F25.digitalocean-agents`: signed in, the panel lists the team's OpenHands agents and their sessions, **New Agent** (name and sandbox size), refresh and sign-out; connecting a session (or **Resume and connect** a paused one) adds it as the active backend, the sidebar's conversation list becomes that agent's session panel (`mars-session-panel`), and Manage Backends' Managed Agents view offers Pause, Stop, Disconnect and Open per session (blocked: needs a DigitalOcean account with Managed Agents).
- `F25.digitalocean-backend-removal`: removing a DigitalOcean backend in Manage Backends explains that it only disconnects and offers **Disconnect only** or **Stop session** (blocked: needs a connected DigitalOcean session).
- `F25.locked-cloud`: a Canvas served with `--lock-to-cloud <url>` shows only the Cloud login (no close) on first run, and its selector footer reads **Reconnect to Cloud** with no add, edit or remove.


════════════ F26-runtime-variants.md

- `F26.cli-version`: `--version` and `-v` print the package version and exit 0.
- `F26.cli-info`: `--info` prints the package version, the default agent-server and automation pins, the minimum agent-server version, the default ports and the override env vars.
- `F26.cli-help`: `--help` prints usage, auth modes, options, env vars and examples.
- `F26.cli-flag-conflicts`: `--frontend-only` with `--backend-only`, and `--public` with `--frontend-only`, exit 1 with a one-line error before anything starts.
- `F26.cli-public-needs-key`: `--public` without `LOCAL_BACKEND_API_KEY` exits 1 with guidance.
- `F26.cli-missing-build`: a package without `build/` exits 1 with `No build found` and build instructions.
- `F26.cli-port-in-use`: an occupied ingress (or service) port stops the launch with `Cannot start: the following ports are already in use` naming each busy port.
- `F26.loopback-bind-default`: without `--host` the ingress listens on `127.0.0.1` only: the machine's own non-loopback address refuses the connection while loopback serves the app.
- `F26.session-key-rotated`: in local mode a stale session key left in the browser (the launcher's key changed since the last visit) is replaced by the injected key on load: the app opens on Home without the API-key screen, and the stored `Local` backend holds the new key (public mode asks for it instead: F01's api-key-entry row).
- `F26.session-key-persist`: in local mode without `LOCAL_BACKEND_API_KEY` the launcher generates a session key, saves it under `$HOME/.openhands/agent-canvas/api-key.txt` and injects the same key again on the next start.
- `F26.session-key-pinned`: `LOCAL_BACKEND_API_KEY=<key>` in local mode injects exactly that key into the page and the API accepts only it (the saved generated key gets 401).
- `F26.frontend-only`: `--frontend-only` serves the SPA; `/server_info`, `/api/*`, `/sockets/*` and `/api/automation/*` answer 503, and a browser new to that origin opens on the **Add a backend** onboarding step without an error toast.
- `F26.frontend-only-returning`: on a frontend-only origin, a browser that already stores a backend for that origin (the `default-local` entry a full launcher on the same port seeded, or one added there earlier) gets the recovery gate instead of onboarding: `agent-server-onboarding-screen` with Manage backends in recovery mode and the `Local` row `Disconnected` with the 503 detail (the gate itself is F25's recovery-gate row); the failing probes raise generic `An error occurred` toasts there (Known failure, reproduced 2026-10-08: #18160).
- `F26.backend-only`: `--backend-only` serves the APIs (key-protected) and answers 503 `No backend configured for this route` for `/` and static assets.
- `F26.cross-connect`: a frontend-only UI connects to a separate backend-only instance through **Add a backend**; the shell then shows that backend as Connected and all API traffic goes to it.
- `F26.remote-backend`: from a normal Canvas, backend selector → **Add Backend** → **Agent-server** → **Remote** shows the self-hosting guidance and connects to another instance (stand-in for a self-hosted VM, `docs/SELF_HOSTING.md`).
- `F26.seeded-local-backend`: the first load of a launcher-served UI seeds one backend `Local` (id `default-local`) with the page origin and the injected key.
- `F26.host-bind-lan`: `--host 0.0.0.0` warns, listens on the machine's LAN address, and serves an index.html that carries `__AGENT_CANVAS_AUTH_REQUIRED__` and neither `__AGENT_CANVAS_SESSION_API_KEY__` nor the key itself (the loopback launcher's page carries the key marker); the UI behaves like `--public` (Add a backend with Next disabled; Skip leads to the API-key screen).
- `F26.host-bind-lan-optin`: the opt-in the launcher's warning recommends (`--allow-lan-session-key`) should restore key injection on a LAN bind. Known failure (reproduced 2026-10-08): the npm launcher ignores the flag and the page still carries only `__AGENT_CANVAS_AUTH_REQUIRED__`; #17949.
- `F26.host-bind-env`: `OH_BIND_HOST=0.0.0.0` (the environment form of `--host`) prints the same two "not loopback" warnings and does not inject the session key, even with `LOCAL_BACKEND_API_KEY` set.
- `F26.automation-startup-failure`: when the requested automation backend exits before it is ready, the launcher should print no success banner, stop the services it started and exit nonzero with the automation's exit reason. Known failure (reproduced 2026-10-13, #18362): it prints the **Main UI** banner and keeps the stack up with `/api/automation/health` answering `502`.
- `F26.mars-web-bridge`: every launcher-served web stack hosts the DigitalOcean Managed Agents bridge on its ingress by default (`MARS_WEB=0` opts out): `/mars/health` answers `{"ok":true,"authRequired":true}` on a full, `--backend-only`, `--frontend-only` and `--host 0.0.0.0` launcher, `/mars/rpc/*` answers `401` without the session key and works with it, the static child behind the ingress hosts none, and the Electron stack hosts none either (its preload provides `window.marsBridge`).
- `F26.runtime-services`: `/server_info.runtime_services` lists the agent-server, ingress, frontend and automation URLs as the agent sees them, and a new conversation's system prompt carries a matching `<RUNTIME_SERVICES>` block.
- `F26.runtime-services-agent-use`: with that block alone, the agent can reach the automation backend from its terminal: asked to, it reads the OpenAPI at the URL the block names, creates an enabled cron automation, and dispatches a run that completes; the automation and its run show up in the Automations UI.
- `F26.lib-build`: `npm run build:lib` produces `dist/` where every `package.json` `exports` target exists and the main entry exports `AgentServerUIProviders`, `AgentServerUIRoot`, `CloudOrganizationBoundary` and the telemetry helpers.
- `F26.lib-style-scope`: all bundled CSS is scoped under `[data-agent-server-ui]`, and theme tokens such as `--oh-color-base` live on that scope root (observable in the standalone app).
- `F26.lib-host-app`: mounted in a separate host app, Canvas styles stay inside the scope and `styleOverrides` restyle it. Blocked: no host-app example exists.
- `F26.docker-image`: `docker run ghcr.io/openhands/agent-canvas` serves Canvas at `/canvas`; without `AGENT_CANVAS_ALLOW_LAN_SESSION_KEY=true` the UI asks for the key. Blocked without a Docker daemon. On a host whose kernel has no IPv6 (no `/proc/net/if_inet6`) the entrypoint logs `IPv6 is not available; binding the frontend to 0.0.0.0.` and the container keeps running (#18180).
- `F26.docker-conversation-runtime`: `OH_CONVERSATION_RUNTIME=docker` runs each new conversation in its own container. Blocked without a Docker daemon.
- `F26.helm-chart`: `helm install agent-canvas ./helm/agent-canvas` runs the Docker image as a Kubernetes StatefulSet with a PVC and an Ingress. Blocked without `helm` and a cluster.
- `F26.desktop-boot-splash`: the Electron app first shows a dark splash (logo, **OpenHands Agent Canvas**, spinner, `Starting backend services…`, then live service-log lines, a first-launch note and **Show details**).
- `F26.desktop-main-window`: after boot a native window with a File/Edit/View/Window menu loads `http://localhost:8000` with the key injected (first run, no API-key screen).
- `F26.desktop-macos-titlebar`: on macOS the desktop window hides the native title bar and reserves a 28 px drag band (`titlebar-drag-region`, `aria-hidden`) above the shell, so the sidebar logo clears the traffic lights and the window can be dragged; the band is dropped in native fullscreen, also after a reload while fullscreen. Linux and Windows windows and browser tabs render no band (`window.desktopShell` is undefined in a tab; `platform` is `linux` on Linux). The band is rendered by the root layout, its error shell and the config-loading spinner, not by the first-run onboarding screen. Blocked without macOS (#17474).
- `F26.desktop-external-links`: external links in the desktop window open in the system browser for allowed schemes only. Not driven: the harness cannot click inside the Electron window.


════════════ F27-workspace-tools.md

- `F27.terminal-empty`: with the runtime active and no command run yet, the Terminal tab reads "No terminal output yet. Commands run by the agent will appear here."
- `F27.terminal-waiting`: while the runtime is starting or the conversation is in an error state, the Terminal tab reads "Waiting for runtime to start..."; a paused (stopped) conversation shows the empty message instead, by design.
- `F27.terminal-output`: the agent's commands appear as `$ <command>` followed by their output; typing into the terminal does nothing (stdin is disabled).
- `F27.terminal-history-reload`: after a page reload the earlier commands and output are still shown (issue #17566).
- `F27.terminal-live-append`: a command the agent runs while the page is open is appended live.
- `F27.browser-empty`: before any browsing the URL bar reads "No URL loaded", "Open in New Tab" is a disabled button, and the body reads "No page loaded yet. Ask OpenHands to open a URL. Example: "Open https://example.com"".
- `F27.browser-screenshot`: after the agent's browser tool ran, the tab shows the last screenshot under a bar with its URL and an enabled "Open in New Tab" link.
- `F27.planner-empty`: with no plan the Planner reads "There is currently no plan for this repo" with an enabled "Create a plan" button; the drawer's "Build ⌘↩" button is disabled.
- `F27.planner-create`: "Create a plan" toasts "Planning agent initialized", switches the composer to plan mode ("Let’s work on a plan") and disables itself, also after a reload.
- `F27.planner-plan`: when the workspace has `.agents_tmp/PLAN.md`, the Planner renders it as Markdown with an enabled "Build ⌘↩" bar.
- `F27.planner-build`: "Build ⌘↩" sends "Execute the plan based on the .agents_tmp/PLAN.md file." to the agent.
- `F27.tasklist`: the Task List tab appears only once the agent used `task_tracker`; it lists each task with its status icon and highlights the in-progress one; the drawer's tabs menu opens it too.
- `F27.usage-metrics`: the Usage tab shows the context meter ("X% used (Y% left)", tokens / window), Token Usage (Input, Cache Hit, Cache Write, Output, Total) and Total Cost.
- `F27.usage-empty`: before any metrics arrive the Usage tab reads "No metrics data available".
- `F27.usage-compact`: "Compact context" (agent idle) toasts "Context compaction started" then "Context compacted" (or "…freed N tokens"), and the agent server records a Condensation; the (i) tooltip explains it.
- `F27.usage-provider-balance`: the provider balance card shows only when the agent server reports a balance (OpenRouter); otherwise it is absent without an error message.
- `F27.composer-context-meter`: the composer's context ring opens a popover with the fill label, a Compact context button, tokens and a Usage item; the bar or Usage item opens the Usage tab; Escape (focus returns to the ring) or an outside click closes it.
- `F27.overview-peek`: while the drawer is open, hovering the chat header (i) button shows a peek of the Overview rows (with the drawer closed there is no peek; clicking opens the column).
- `F27.overview-pin`: the Overview "..." menu pins and unpins rows (Workspace, Git parts); an unpinned row stays hidden after a reload.
- `F27.overview-changes-link`: the Overview Changes row closes the Overview and opens the drawer on Commits.
- `F27.overview-menu-open`: the Overview "..." menu's Git > Commits item opens the drawer on Commits with Uncommitted collapsed; Git > Changes opens Commits with Uncommitted expanded; both close the Overview.
- `F27.overview-identity`: the Overview's Workspace row shows the attached folder's basename (`None` without a workspace), and once the workspace's `origin` is a GitHub remote the Git block adds an `owner/repo` link to the repository and a branch link.
- `F27.overview-drawer`: Overview menu > Pull Requests opens the secondary drawer with a close button; without a git repository it reads "Connect a git repository to view pull requests and issues".
- `F27.canvas-ui-open-tab`: the agent's `canvas_ui_control` `open_tab` opens the drawer on the requested tab (files, browser, terminal, planner, tasklist), and the choice persists like a user tab click.
- `F27.canvas-ui-navigate-file`: `canvas_ui_control` `navigate_to_file` (and clicking a file path in chat) opens Files with that file selected.
- `F27.canvas-ui-phone`: at phone width an agent `open_tab` makes the requested tab visible on `/conversations/<id>/panel`.
- `F27.launch-child`: the agent's `launch_child_conversation` (local) toasts "Launched a local child conversation" with an Open link; the child runs, and the launch result (`[child-conversation] {"status":"launched",…}` with the child id) is posted back to the parent agent as a user message.
- `F27.info-modals-phone`: the composer `+` menu's Skills, Hooks and Agent Tools & Metadata modals fit a 390 px viewport (issue #17562); content is covered by F07.
- `F27.plugins-modal`: a conversation started with plugins offers a plugins item that lists `active-plugin-<name>` rows.
- `F27.phone`: at 390 px the panel route shows the Usage, Task List and Planner tabs without overflow, and the tab row stays visible.


════════════ F28-automation-setup-page.md

- `F28.open-blank`: Create Automation opens `/automations/setup` with Name pre-filled **New Automation** (placeholder `Standup digest`), an empty Prompt, Schedule selected with Daily at `09:00` in the browser's time zone, the docked composer (`Need help creating an automation?`) and no new conversation. The save state already reads **Unsaved changes** and the toolbar **Test** is disabled.
- `F28.kind-toggle`: the button beside the Prompt label switches between Prompt and **Custom** (Custom Python). Custom shows a generated Python script that calls `AUTOMATION_CALLBACK_URL`, and **Entrypoint and setup** reveals Entrypoint `python3 main.py`, Setup script path `setup.sh` and a no-op setup script; switching back shows the Prompt again.
- `F28.schedule-presets`: in create mode the Frequency control (the same control as on the edit page, whose presets and saving F23 owns as edit-schedule and edit-cron) starts on Daily at `09:00`, and switching presets fills create-mode defaults: Once a date-time at the next whole hour today, Weekly Monday, Custom the last custom cron, `0 9 * * *` until one is typed (it is not derived from the preset: from Weekly on Monday it also reads `0 9 * * *`). Every preset keeps the 13-entry time zone select.
- `F28.event-trigger`: choosing **Event** replaces the schedule with Event source (default `github`; its menu lists the webhook sources registered on this backend plus **Custom**), Event key (default `issue_comment.created`), **Add Filter** (one free-text filter input), **Ask Agent**, the notice `Event sources need a public URL to deliver webhooks to this instance.`, and a **Test event payload** pre-filled with a JSON object for that source and key. A source the backend has not registered (menu **Custom**, then a typed name) adds **Register this as a custom webhook source**, checked by default.
- `F28.timeout`: **Add Timeout** adds an optional **Timeout seconds** number field with a remove button; `1800` saves and the draft reads **Ready to test**.
- `F28.draft-save`: **Save draft** works on any form, valid or not; the first save replaces the URL with `?draftId=<id>`, shows the **Draft saved** bar (relative time, **View test runs**, **Test**) and the toast `Draft saved in this conversation.`, and the save state reads **Saved just now**. The draft appears in the dashboard Drafts group (F21).
- `F28.draft-validation`: the server validates on save, not on blur. An invalid draft is still saved (`dispatchable: false`) and its first error replaces **Ready to test** in the draft bar: an empty prompt shows `Field required` (also under Prompt), a bad custom cron shows `Value error, Invalid cron expression: every day`. A field error stays until the next save.
- `F28.draft-save-error`: a draft the server refuses outright (a timeout above 1800 returns `422`) sets the save state to **Save failed** and shows one error toast with the reason. Known failure (reproduced 2026-10-14, not filed yet): the toast reads only `Draft body does not match endpoint schema`; the reason the API returns in `detail.errors` (`Value error, timeout must not exceed 1800 seconds (30 minutes)`) is never shown, and the draft bar keeps the previous save's **Ready to test**.
- `F28.required-before-test`: Test and Create check locally first and send nothing when a required field is empty: `Add a prompt before continuing.` (Prompt or Plugin kind), `Add a plugin source before continuing.`, `Add Python code before continuing.`, `Add an entrypoint command before continuing.`, `Add a setup script path before continuing.`, `Add a setup script before continuing.`; an event Test with a payload that is not a JSON object shows `Enter a valid JSON object before testing this event automation.` A blank Name is not an error: it is derived from the prompt.
- `F28.test-gated`: in create mode **Test** stays disabled until the draft is saved and unchanged since; editing any field after a save disables it again until the next **Save draft**.
- `F28.draft-test`: **Test** on a saved, valid draft opens **Test runs** (with **Back**), shows `Test run started.` and a Pending row; it runs a hidden disabled copy of the draft, not a dashboard automation. Expected: the row reaches Successful or Failed by itself. Known failure #18263 (reproduced again 2026-10-14 on a Custom Python draft): the row stays Pending and the page does not poll the runs after the first request.
- `F28.create`: **Create Automation** creates the automation enabled, navigates to `/automations/<id>` (a Custom Python automation shows **Script**), and removes the draft from the dashboard Drafts group.
- `F28.error-contrast`: the draft bar's error pill and the Test runs error alert are readable in every color theme. Known failure (computed 2026-10-14 from the Light+ theme tokens and confirmed live the same day for the draft pill, not filed yet): both use raw `text-red-300` on `bg-red-500/10`, about 1.5:1 against Light+'s `#F3F3F3` surface; the theme's `--oh-feedback-error` gives 4.6:1. Dark themes read about 7.8:1.


