---
name: ohaa-design
description: Design system for the ohaa Android app (OpenHands Cloud client, Jetpack Compose). Use for ANY UI work in this repo - screens, components, themes, fixes. Pins the Claude Code mobile look, the review decisions from the 2026-11-15 specialist pass, theme rules, and the accessibility floor.
---

# ohaa Design System

ohaa is a Kotlin/Jetpack Compose client for OpenHands Cloud. Its visual benchmark is the
Claude Code mobile app: a calm, single-column, text-first agent console. No hero art, no
dashboard chrome competing with the content, no segmented control strips.

This skill is the spec. When a design question is not answered here, ask: "what would
Claude Code mobile do?" - flat, quiet, content-first.

## Foundations (from wshobson/agents mobile-android-design + android/skills)

- All colors come from `MaterialTheme.colorScheme`. Never use `OpenHands*Dark`/`OpenHands*Light`
  palette constants or `Color(0x...)` literals inside screens or components. The palettes
  exist only to define the two schemes in `theme/Color.kt` + `theme/Theme.kt`.
- Status colors come from `StatusColors` (theme-aware, AA-passing in both themes).
  Never use the raw `Status*` constants in composables.
- Every screen's content inherits the root themed Surface. If a screen can be shown
  outside the main Scaffold (tests, previews, dialogs), wrap its root in
  `Surface(color = colorScheme.background, contentColor = colorScheme.onBackground)`.
- Lists are `LazyColumn`, never scrollable `Column`. Horizontal scroll rows use the
  shared `EdgeFadeRow` so clipped content has a fade cue.
- State: hoist to the ViewModel; `rememberSaveable` for UI state that must survive
  recomposition/rotation (tab pickers, expanded rows).
- Touch targets >= 48dp. Interactive icons get a `contentDescription`; decorative ones
  pass `null`.

## Accessibility floor (from wshobson accessibility-compliance, WCAG 2.2 AA)

- Text contrast >= 4.5:1, large text >= 3:1, UI icons/borders >= 3:1, in BOTH themes.
- Never override `onPrimary`/content colors with `Color.White` on filled buttons -
  use the scheme's `onPrimary` (dark text on light-blue passes, white fails).
- Streaming/dynamic content (agent events) announces via `LiveRegionMode.Polite`.
- Expandable rows carry `stateDescription` ("expanded"/"collapsed") and a role.
- Custom chips/tabs expose `Role.Tab`/`Role.Button` + selected state semantics.
- Diagnostic ids (conversation ids, sandbox ids) are hidden from screen readers
  (`clearAndSetSemantics`) and never shown on user surfaces; they live in a debug sheet.

## Navigation (Material limit: 3-5 destinations)

- Bottom nav has exactly 4 tabs: Home, Tasks, Resources, Settings.
- New Task is a FAB on Home and Tasks, not a tab.
- API Explorer lives under Settings > Developer.
- No duplicate surfaces: one Settings, one Profiles picker (in New Task), one Git section
  (inside Resources > Git only).

## The conversation screen (the heart of the app)

Structure, top to bottom:

1. **Header**: two lines. Line 1: conversation title (single line, ellipsis). Line 2:
   muted meta - status pill + repo `owner/repo - branch`. No conversation id, no sandbox
   id. Back arrow left, overflow (three-dot) right. All sheet/runtime actions
   (Model, Context, Terminal, Files, Git, Run/Pause/Stop, VS Code, Skills...) live in
   the overflow menu - there is NO action chip row under the header.
2. **Event stream**: flat, full-width, single column.
   - Assistant messages: plain text on the background, no card, no badge.
   - User messages: plain text in a subtle surface-tinted block, right-aligned or
     full-width; no "You (Prompt)" label.
   - Thinking: one muted italic line ("Thinking..." + first line of thought), expandable
     inline.
   - Tool calls (bash/edit/read/write): one-line collapsible rows -
     icon + tool name + short argument (e.g. `Bash  grep -rn 'redirect' src/auth/`).
     Expanded shows the detail; bash output renders in the terminal block.
   - Terminal block: dark mono block containing the `$ command`, the output, and the
     exit code, with a 48dp copy affordance.
   - While running: a small "agent is working" indicator pinned after the last event.
3. **Confirmation**: when status is AWAITING_CONFIRMATION, an anchored action bar with
   Reject / Accept buttons sits directly above the composer. It is never an event card
   in the stream.
4. **Composer**: rounded text field with an in-field circular send button. Send is
   enabled only when the field has text; while the agent runs, the affordance becomes
   a stop button. Quick-suggestion chips appear only when the conversation is empty.

## Other screens

- **Dashboard (Home)**: greeting/status line (no hero artwork), stat row with fixed
  32dp icon chips (icon above or beside a one-line label, never squeezed into the
  label), recent conversations as flat rows: title, one muted meta line
  (repo - branch - relative time), status pill. Sections with no content are hidden,
  not rendered as bare headings.
- **Tasks**: filter chips with short labels (All, Running, Needs you, Done). Ready and
  Finished use distinct hues. Empty states distinguish "no tasks yet" (CTA to New Task)
  from "no matches - clear filters".
- **Resources**: section tabs in an EdgeFadeRow; selection survives recomposition;
  delete actions use an outlined error-color icon + confirmation dialog; the Git
  account line lives only in the Git section.
- **API Explorer**: method is a compact dropdown chip, "Show schemas" a text button,
  "Call" the single filled primary button. Response block is mono, same surface as the
  terminal block, with a copy action. Path rows are tappable and fill the Path field.
- **Settings** is the model screen: labeled fields, helper text, described toggles.
  Follow it.

## Testing

- Roborazzi/Robolectric screenshot tests live in `app/src/test`. Every capture wraps
  content in the themed root Surface (see Foundations) - captures without it show
  false contrast bugs (default content color is Black, window background is dark).
- Run `./gradlew recordRoborazziDebug --tests '<class>' --no-daemon` to recapture.
  This sandbox needs heap caps: `org.gradle.jvmargs=-Xmx700m`, test `maxHeapSize 600m`.
- `./gradlew testDebugUnitTest` must stay green (140 tests at the time of writing).

## Anti-patterns (all seen and fixed once; do not reintroduce)

- Action chip rows between header and content (the "strange split").
- A badge/card around every event; environment output labeled "AGENT RESPONSE".
- Hardcoded dark-palette colors used in light theme (1.9-3.4:1 failures).
- 6-item bottom nav; diagnostic ids in headers and list rows.
- Empty section headings with no empty state.
- Destructive actions with no confirmation.
- Buttons with `Color.White` text on light-primary fills.

## Sources

This skill folds in guidance from:

- https://github.com/android/skills - Google's official Android agent skills (jetpack-compose, navigation, testing): platform correctness, adaptive layouts, edge-to-edge, performance, test setup.
- https://github.com/wshobson/agents/tree/main/plugins/ui-design/skills - ui-design plugin: mobile-android-design (Material 3 + Compose theming, colorScheme, dark mode, adaptive) and accessibility-compliance (WCAG contrast, touch targets, semantics).
- https://github.com/anthropics/skills - Anthropic's official skills repo: skill-creator conventions for how this skill is structured.
