### Fixed
- **Web build authentication + proxy hardening.** `OpenHandsApiClient` now takes an
  `authHeaderName` (default `X-Access-Token` for the Cloud API; the Wasm build passes
  `X-OpenHands-Key`, the header the `:server` proxy accepts). The proxy echoes CORS only
  for localhost origins, keeps the env API key authoritative over any browser-supplied
  key, and passes upstream non-2xx responses through verbatim (`{"detail":"..."}` bodies
  included) instead of masking them as 502; unexpected failures return a generic
  `{"detail":"Bad gateway"}`.
- **HTTP failures now surface in the UI.** `expectSuccess = true` plus `ResponseException`
  mapping renders `HTTP <status>: <server detail>` (e.g. the boot banner shows the
  upstream 401 text when no key is configured).
- **`git/repositories/search` and `git/branches/search` fix.** Both now send the
  `provider` (`github`) and `query` fields upstream requires (`query` may be empty but
  must be present).
- **New Task wiring (web).** Selecting a repository now loads its branches, the selected
  branch is passed to `launchTask`, bottom-nav icons render in Wasm, and Settings is
  wired to Cloud profiles/secrets via `refreshCloudSettings()`.
- **`compileSdk` set to 37.2** (`minorApiLevel = 2`) for `:app` and `:shared`.
