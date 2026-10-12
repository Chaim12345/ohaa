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

### Changed
- `.gitignore` now excludes per-module `build/` directories.
