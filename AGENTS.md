# ohaa — OpenHands Android client

Android app (Kotlin, Retrofit + Moshi) for OpenHands Cloud.

## Build environment
- JDK 21 (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`), Android SDK at `/opt/android-sdk` (set in `local.properties`).
- The sandbox may reset between sessions: reinstall `openjdk-21-jdk-headless` and the SDK cmdline-tools (`platforms;android-36`, `build-tools;36.0.0`) if `JAVA_HOME`/`ANDROID_HOME` become invalid.
- `./gradlew testDebugUnitTest` (71 tests), `assembleDebug`, `lintDebug` all expected green.

## API layer architecture
- `app/src/main/java/com/example/data/api/OpenHandsApiService.kt` — Retrofit interface, **all 211 operations** of the live spec (https://app.all-hands.dev/openapi.json, v1.59.1).
- `app/src/main/java/com/example/data/model/OpenHandsModels.kt` — 163 Moshi `@JsonClass` DTOs (`*Dto` naming, `@param:Json` wire names). Enum schemas are typed as `String`.
- `ApiSurfaceParityTest` asserts every spec op is present with the right HTTP verb; its expected list is generated from the service — regenerate after adding/renaming methods (script pattern in git history, commit ffeb69a).
- `/workspace/scratch/gen_service.py` + `service_inject.kt` — spec-driven generator for new methods (not part of repo; `/tmp` is wiped on resets, `/workspace/scratch` persists longer).
- Wire conventions: optional query params are `= null` nullable (Retrofit omits nulls); batch ids params are `List<String>`; streaming/raw endpoints return `ResponseBody`; discriminated-union schemas (e.g. webhook `workspace`/`agent`) stay untyped or flat-typed since Moshi can't codegen `oneOf`.

## Gotchas learned
- `AppConversationDto.launched_agent_profile` is an **object** (`agent_profile_id`, `revision`), not a string — typing it as String crashed Moshi with "Expected a string but was BEGIN_OBJECT". Regression test: `launched_agent_profile parses as object not string`.
- Method-name collisions: org-profile endpoints vs user-profile endpoints share op names — org variants are prefixed `org*` (orgListProfiles, orgGetProfile, ...). The parity test maps by method name, so overloads break it.
- Live API quirk: `GET /api/v1/app-conversations?ids=...` returns a **bare JSON array**, not a page object.
- Service file uses explicit per-DTO imports plus one wildcard `com.example.data.model.*` for generated sections.

## Repo workflow
- Work happens on `redesign/canvas-v2`; PR #10 tracks the API-parity effort. Push updates to that branch/PR rather than opening new ones.
- Commit style: conventional commits (`feat(api): ...`) with `Co-authored-by: openhands <openhands@all-hands.dev>`.
