# Android UI Review — OpenHands Cloud client (feat/cloud-v1-mobile-client)
Scope: MainActivity.kt, ui/screens/*, ui/components/OpenHandsComponents.kt, ui/viewmodel/OpenHandsViewModel.kt, ui/theme/*. Read-only review; all evidence verified in source.

## Verdict
No CRASH/CRITICAL defects. 3 MAJOR, 11 MINOR, 3 NIT. Full list below.

## MAJOR (fix next cycle)

**M1. Detail "Switch LLM model" sends model IDs to the profile-switch endpoint** — `ConversationDetailScreen.kt:402-408`, `OpenHandsApiService.kt:175-180`
Model rows pass `m.id` (e.g. `anthropic/claude-sonnet-4-5`) into `switchConversationProfile` → POST `switch_profile`, whose contract takes a profile NAME from `GET /settings/profiles`. Selecting any catalog model fails server-side after the dialog already closed; model switching never works. The `switchAcpModel` endpoint exists but is unused.

**M2. Trajectory export swallows all failures, leaves empty file** — `ConversationDetailScreen.kt:130-144`
Launcher callback has `onSuccess {}` only; the SAF write is bare `runCatching` with no reporting. Download failure or stream write failure → user picked a destination in the system picker and gets a 0-byte/partial file with zero feedback. The menu promise "Download trajectory" silently no-ops.

**M3. Light theme: bottom-nav colors hardcoded to dark-palette `OpenHandsPrimaryDark`** — `MainActivity.kt:104-108`, `ui/theme/Color.kt:10`
`NavigationBarItemDefaults.colors` uses `#58A6FF` for selected icon/text/indicator in both themes: ~2.7:1 label contrast on white (below WCAG 4.5:1), inconsistent with `LightColorScheme.primary` `#0969DA`. The earlier hardcoded-color sweep missed MainActivity.

## MINOR

**m1. Home hero shows "Connecting…" forever on non-auth account failure** — `HomeScreen.kt:150-158` — `accountError` is rendered only in the `needsApiKey` branch (118-135); a network/5xx failure leaves the dot claiming "Connecting…" indefinitely. Error state lies.

**m2. Max-budget field silently discards invalid input on save** — `OpenHandsViewModel.kt:868, 891-903` — input filter allows `1.2.3`; `toDoubleOrNull()` → null → field blanked under a "Settings saved" snackbar.

**m3. Git provider token form accepts arbitrary provider strings** — `SettingsScreen.kt:328-338`, `OpenHandsRepository.kt:657-659` — provider is free text, not trimmed/lowercased before use as the payload key (`GitHub` → dead key). Add-LLM-profile Base URL is also unvalidated.

**m4. BranchPickerDialog renders a blank 300dp body** — `NewConversationScreen.kt:629-639` — no empty-state branch; failed/empty branch search (VM clears results, `OpenHandsViewModel.kt:505`) shows only blank space, no message or retry.

**m5. Model pickers show misleading text when the catalog never loaded** — `NewConversationScreen.kt:585-588, 348-349`; `ConversationDetailScreen.kt:373-379` — "No models match. The catalog holds verified models only." is shown for load failures too (VM `loadModelCatalog` failure only clears the spinner, `OpenHandsViewModel.kt:882`); the detail dialog sticks on "Loading profiles and models…" forever with no retry.

**m6. Selected-repo pill can overflow without ellipsis** — `NewConversationScreen.kt:220-226`; `OpenHandsComponents.kt:166-183` — Pill text is `maxLines=1` with no overflow handling and no width cap next to the fixed button; long `owner/repo@branch` clips or pushes past the edge.

**m7. Touch targets under 48dp** — `OpenHandsComponents.kt:322,331` (28dp CodeBlock expand/copy), `ConversationsScreen.kt:248` (32dp card menu), `ConversationDetailScreen.kt:311-316` (quick-reply chips ~26dp), `SettingsScreen.kt:289-292,366-374` and `HomeScreen.kt:199` (12sp clickable text).

**m8. Send button hardcodes `Color.White`** — `ConversationDetailScreen.kt:338-339` — white on dark-theme primary `#58A6FF` ≈ 1.9:1 contrast; should be `colorScheme.onPrimary`.

**m9. Duplicated sandbox status on detail screen** — `ConversationDetailScreen.kt:244, 300, 345` — paused state restated three times (Pill, composer label, SandboxBadge) plus card row text; the composer label adds nothing.

**m10. Event list auto-scroll yanks user to bottom on every new event** — `ConversationDetailScreen.kt:146-148` — unconditional `animateScrollToItem(last)` on every 3-15s poll insertion; no "user is reading" guard.

**m11. "Stop waiting" cancels only local polling** — `OpenHandsViewModel.kt:559-562` — submit already sent; the conversation may still be created server-side while the form retains the prompt → easy duplicate start, unexplained late appearance in list.

## NIT

**n1. Hidden long-press menu duplicates the visible menu button** — `ConversationsScreen.kt:230-233` — no affordance/hint; the visible ⋮ (248) opens the identical menu.

**n2. Failed start reported twice** — `OpenHandsViewModel.kt:552` + `NewConversationScreen.kt:677-679` — same message in StartProgressDialog body and snackbar simultaneously.

**n3. "Synced … ago" timestamp never re-renders** — `ConversationsScreen.kt:96-100` — `formatRelativeTime` evaluated at composition with no ticker; freezes at "just now".
