# Changelog

## Unreleased

### Added
- Compose Multiplatform `:shared` module (`commonMain`) holding DTOs, the Ktor
  `OpenHandsApiClient`, and all Compose screens/components, so the Android app
  and the browser build render from one UI codebase.
- Kotlin/Wasm `:web` target with a browser entry point, exercised through
  `wasmJsBrowserDistribution`.
- Ktor/JVM `:server` module that serves the Wasm distribution and reverse-proxies
  `/cloud/{path...}` to the OpenHands Cloud API, giving the browser build
  same-origin access without shipping an API key to the client.
- `docs/HANDOFF.md` describing the migration, the two transcript bugs, and the
  remaining known issues.

### Fixed
- **Transcript only showed the oldest events.** `searchEvents()` relied on the
  server's default ascending sort, so a 5,089-event conversation returned only
  its first 100 events and the Chat tab never showed recent activity. It now
  requests `sort_order=TIMESTAMP_DESC` and reverses the page to return the newest
  events chronologically.
- **`GET /api/v1/users/me` failed to decode**, leaving Settings without an
  account. `UserInfoDto.secrets_store` was typed `String?` and
  `agent_settings` `Map<String,String>?`, but the live API returns objects for
  both (`{"provider_tokens":{}}` and a nested agent/LLM config). Both are now
  `JsonElement?`, matching the open schema in `openapi.json`.
- **Web build authentication + proxy hardening.** `OpenHandsApiClient` now takes an
  `authHeaderName` (default `X-Access-Token` for the Cloud API; the Wasm build passes
  `X-OpenHands-Key`, the header the `:server` proxy accepts). The proxy echoes CORS only
  for localhost origins, keeps the env API key authoritative over any browser-supplied
  key, and passes upstream non-2xx responses through verbatim (`{"detail":"..."}` bodies
  included) instead of masking them as 502; unexpected failures return a generic
  `{"detail":"Bad gateway"}`.
- **HTTP failures now surface in the UI.** `expectSuccess = true` plus `ResponseException`
  mapping renders `HTTP <status>: <server detail>` (e.g. the upstream 401 text when no key
  is configured).
- **`git/repositories/search` and `git/branches/search` returned 422.** Both now send the
  `provider` (`github`) and `query` fields upstream requires (`query` may be empty but must
  be present).
- **New Task wiring (web).** Selecting a repository now loads its branches, the selected
  branch is passed to `launchTask`, bottom-nav icons render in Wasm, and Settings is wired
  to Cloud profiles/secrets via `refreshCloudSettings()`.
- **`compileSdk` set to 37.2** (`minorApiLevel = 2`) for `:app` and `:shared`.

### Changed
- `.gitignore` now excludes per-module `build/` directories.
