# ohaa — OpenHands Android client

Android app (Kotlin, Retrofit + Moshi) for OpenHands Cloud.

## Build environment
- JDK 21 (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`), Android SDK at `/opt/android-sdk` (set in `local.properties`).
- The sandbox may reset between sessions: reinstall `openjdk-21-jdk-headless` and the SDK cmdline-tools; install **`platforms;android-36.1`** (the project's `compileSdk` uses the minor-version platform), `platforms;android-36`, `build-tools;36.0.0`, then `chown -R openhands /opt/android-sdk` so Gradle can write to it.
- `./gradlew testDebugUnitTest` (83 tests), `assembleDebug` all expected green.

## Runtime behaviour (live mode, no demo data)
- The client is always live: there is **no demo/sample data**. Conversations come only from the cloud; `seedLocalData()` seeds only the device-local prompt templates.
- `isDemoMode` is true only when no API key is configured; in that state the UI shows "Not connected" and makes no cloud calls.
- Models: only the **`openhands` provider's free/verified** models are offered (`GET /api/v1/config/models/search?verified__eq=true&provider__eq=openhands`, limit ≤ 100). The wire value is `provider/model`, e.g. `openhands/deepseek-v4.1-flash`. `free` models on this account: `deepseek-v4-flash`, `deepseek-v4.1-flash`.
- There is **no ACP agent** — the only backend is the OpenHands agent (`agent_type="default"`). `AgentProfile` carries just `model`.


## API layer architecture
- `app/src/main/java/com/example/data/api/OpenHandsApiService.kt` — Retrofit interface, **all 211 operations** of the live spec (https://app.all-hands.dev/openapi.json, v1.59.1).
- `app/src/main/java/com/example/data/model/OpenHandsModels.kt` — 163 Moshi `@JsonClass` DTOs (`*Dto` naming, `@param:Json` wire names). Enum schemas are typed as `String`.
- `ApiSurfaceParityTest` asserts every spec op is present with the right HTTP verb; its expected list is generated from the service — regenerate after adding/renaming methods (script pattern in git history, commit ffeb69a).
- `/workspace/scratch/gen_service.py` + `service_inject.kt` — spec-driven generator for new methods (not part of repo; `/tmp` is wiped on resets, `/workspace/scratch` persists longer).
- Wire conventions: optional query params are `= null` nullable (Retrofit omits nulls); batch ids params are `List<String>`; streaming/raw endpoints return `ResponseBody`; discriminated-union schemas (e.g. webhook `workspace`/`agent`) stay untyped or flat-typed since Moshi can't codegen `oneOf`.

## Gotchas learned
- `UserInfoDto.secrets_store` and `agent_settings` are **objects**, not scalars (`{"provider_tokens":{}}`, `{"schema_version":6,"agent":"CodeActAgent","llm":{...}}`). Typing them as `String?` / `Map<String,String>?` fails the whole `users/me` decode and silently leaves Settings without an account. Same failure class as `launched_agent_profile` below — model open-schema fields as `JsonElement?`.
- `GET .../events/search` sorts **ascending** by default and caps `limit` at 100. A long conversation therefore returns only its first page unless you pass `sort_order=TIMESTAMP_DESC` (then reverse). This is what made the Chat tab look like it only showed terminal output.
- `AppConversationDto.launched_agent_profile` is an **object** (`agent_profile_id`, `revision`), not a string — typing it as String crashed Moshi with "Expected a string but was BEGIN_OBJECT". Regression test: `launched_agent_profile parses as object not string`.
- Method-name collisions: org-profile endpoints vs user-profile endpoints share op names — org variants are prefixed `org*` (orgListProfiles, orgGetProfile, ...). The parity test maps by method name, so overloads break it.
- Live API quirk: `GET /api/v1/app-conversations?ids=...` returns a **bare JSON array**, not a page object.
- Service file uses explicit per-DTO imports plus one wildcard `com.example.data.model.*` for generated sections.

## Repo workflow
- Work happens on `redesign/canvas-v2`; PR #10 tracks the API-parity effort. Push updates to that branch/PR rather than opening new ones.
- Commit style: conventional commits (`feat(api): ...`) with `Co-authored-by: openhands <openhands@all-hands.dev>`.
