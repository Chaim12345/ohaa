# ohaa — UI testing strategy (Android + Compose Multiplatform web)

Date: 2026-10-15
Scope: how we verify the shared Compose UI, the Android app, and the wasm web build.

## 1. The taxonomy, mapped to this project

The official Android UI-testing taxonomy has five kinds of testing and two tool
families (native/automated and cross-platform/flow). Mapped to ohaa:

| Kind | Tool | Status in ohaa | Notes |
|---|---|---|---|
| Functional behaviour (hierarchy, states, interactions) | **Jetpack Compose testing APIs** + **Robolectric** | **Wired, in use** | `app/src/test/java/com/example/UiRegressionTest.kt` (Robolectric + `createComposeRule`, ~480 lines). Fastest, deterministic, no device/browser. |
| Visual / screenshot regression | **Roborazzi** | **Wired** | `libs.versions.toml` (`io.github.takahirom.roborazzi` 1.59.0, `roborazzi-compose`, `roborazzi-junit-rule`) + `GreetingScreenshotTest.kt`. |
| Usability / human-centric | manual + agent-driven exploration | ad-hoc | Browser walkthroughs (agent-browser / Playwright) for friction, copy, ergonomics. |
| Accessibility (contrast, touch targets, screen reader) | Compose a11y checks + lint | **Gap** | No `androidx.compose.ui:ui-test-accessibility` yet; add `AccessibilityValidator` + `enabledAccessibilityChecks` (API 34+), plus contrast/touch-target lint. |
| Performance / jank | Macrobenchmark (Android), Lighthouse/CDP (web) | **Gap** | Macrobenchmark needs an emulator; web perf via CDP traces. |

Tool families:
- **Espresso / UIAutomator** — Espresso targets Android *Views*; ohaa is all-Compose,
  so Espresso is only useful through Compose interop. UIAutomator is for cross-app /
  system UI (permissions, notifications) — not needed yet.
- **Appium / Maestro** — optional device-level E2E. Neither covers the wasm web build.
- **Robolectric** — the decisive win: it runs the Compose APIs on the JVM in seconds.

## 2. Already covered (do not re-invent)

`UiRegressionTest.kt` exercises the **shared** UI (`com.example.ui.screens.*`) with
`composeTestRule` + `testTag` finders:

- demo-mode launch guidance (NewTaskScreen), chat header model switcher
- settings: account card, cloud-profile activate/delete/rename (with confirmation)
- **secrets: add dialog collects name/value/description; delete requires confirmation**
- demo-mode hides cloud sections; detail delete requires confirmation

Added 2026-10-15 (this session):

- `add secret dialog gates Add on a valid name and a non-blank value`
  — empty → disabled; `API-KEY` (hyphen, rejected by the cloud) → still disabled;
  `API_KEY` → enabled and delivers `(name, value, description)`.
- `delete secret confirmation names the specific secret`
  — asserts the dialog body text `'OTHER_KEY' will be removed …`, so a mis-tap is
  caught before anything is deleted.

These two are exactly the behaviours the browser semantics harness **cannot**
verify (see §4).

**Payoff, first run:** `add secret dialog gates Add …` failed — and it was right to.
The Android module carries its **own copy** of `SettingsScreen.kt`
(`app/src/main/java/com/example/ui/screens/SettingsScreen.kt`) which had
`enabled = name.isNotBlank() && value.isNotBlank()` — the P0 `[A-Za-z0-9_]` guard
had only been applied to the **shared** copy used by the web build. So the Android
app would submit `API-KEY` and surface a server 422, while the web app blocked it.
The app copy now carries the same `nameValid` predicate, `isError` state, hint text
and `enabled = nameValid && value.isNotBlank()` (only the danger token differs by
module: `CanvasDanger` vs `CanvasColors.danger`). This is exactly the class of bug
the Compose layer is there to catch: it is invisible to a browser test of a
single build.

## 3. Layer model

```
L1  Compose functional (Robolectric, JVM)        ← behaviour, fast, authoritative for UI logic
L2  Roborazzi screenshot                         ← layout/theme regression
L3  Browser E2E on the wasm build + LIVE API     ← only layer that proves the deployed web app
L4  Accessibility checks                         ← semantics, contrast, touch targets
L5  Performance (Macrobenchmark / Lighthouse)    ← jank, startup, load
```

L1/L2 run in `./gradlew :app:testDebugUnitTest` and gate every change.
L3 is slow and comparatively fragile, so it is reserved for integration truth:
does the **deployed** web bundle talk to the **live** API and reflect the result.

## 4. Browser E2E (L3): what works, what does not

The web build renders to a **canvas**, but Compose Multiplatform publishes a real
**semantics DOM inside a shadow root** (roles + `aria-label`/text + exact bounding
boxes). `document.querySelectorAll` never sees it (shadow boundary) — a
shadow-piercing walk does. `semantics.mjs` wraps that:

- `collect(page)` → `[{kind, role, name, x, y, w, h}]`
- `byName` / `clickName` → find by role+name, click the rect **centre** with real
  mouse events (the a11y overlay is `pointer-events:none`, which is why
  Playwright's synthetic `click @ref` hangs — coordinate clicks reach the canvas).
- `typeInto`, `waitFor`

Confirmed working end to end:
- navigation by name (Settings), finding `Add secret` @ (1228,719) etc.
- **live API**: `201 POST /cloud/api/v1/secrets`, `200 DELETE /cloud/api/v1/secrets/OHAAGUITEST`,
  verified in the browser network log — the app does the right thing.

Hard limitation found (2026-10-15): **after any dialog closes, Compose's semantics
DOM collapses to 2 nodes and never rebuilds** (`page.evaluate` + CDP
`Accessibility.getFullAXTree` pulse + mouse move + canvas click all fail to revive
it). Measured: 20 nodes → 8 while the dialog is open → **2 after dismiss**.

```
settings   20 nodes: … button:Add secret | button:Delete secret | …
dialog-open 8 nodes: textbox | textbox | button:Cancel | button:Add
after-cancel 2 nodes: generic: | presentation:      ← dead for the rest of the page session
```

This is a **Compose-web semantics-layer defect, not an app bug**: a screenshot taken
after Cancel shows the Settings screen rendering perfectly, a secret created in the
same session was visible in the list, and the API responses were correct.

Consequences / workarounds:
1. **One dialog interaction per page load.** A reload restores a fresh tree and is
   cheap — a cold load reaches a populated tree in **~2.3 s** (the harness's old
   fixed 25 s wait was ~10x too cautious).
2. **Readiness anchor** must be a *populated* tree (`roles >= 10`, or a nav button
   found by **text**, not `aria-label` — nav buttons carry their name as text).
3. Post-dialog assertions (row present/gone, error states) belong in **L1**, not L3.
4. Worth reporting upstream: *CMP web — semantics DOM not rebuilt after
   `AlertDialog` dismissal* (repro: open/close any dialog, then measure
   `document.querySelectorAll('[role]')` inside the shadow root).

Cleanup discipline: L3 tests must not leave state behind (leftover test secrets were
removed via the dev server proxy:
`DELETE http://localhost:12001/cloud/api/v1/secrets/<name>`; list via
`GET /cloud/api/v1/secrets/search`).

## 5. Recommendations

1. Keep **L1 as the gate** for UI behaviour; add a case whenever a behaviour is
   found "in the wild" (the two added today came from the browser session).
2. Add **L4**: `androidTestImplementation("androidx.compose.ui:ui-test-accessibility")`
   + `enableAccessibilityChecks()` for the settings/conversation flows; assert
   `contentDescription` on icon buttons (e.g. `Add secret`, `Delete secret`) and
   minimum touch targets.
3. Add **L2 baselines** with Roborazzi for Dashboard / Conversations / New Task /
   Settings / Conversation detail, in light + dark.
4. Keep **L3** for live-API integration only, with the reload-per-dialog rule and
   the fast readiness poll; assert on the **network log** for API truth and on L1
   for UI truth.
5. File the CMP semantics issue (upstream) with the reproduction above.
