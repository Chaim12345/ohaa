package com.example.data.api

/**
 * Every API path the app knows about, grouped by surface.
 *
 * This is the single source of truth for coverage: a unit test parses the
 * bundled OpenAPI specs (`src/test/resources/openapi/ (cloud.json and agent.json)`) and asserts that
 * every path in the spec appears here, and vice versa. Keeping the list in
 * production code (not the test) means a newly added endpoint cannot silently
 * miss the coverage check.
 *
 * Paths are stored with `{}` placeholders for path parameters so they compare
 * directly against the normalized spec paths.
 */
object EndpointCatalog {

    /** Paths under the Cloud V1 surface (`https://app.all-hands.dev`). */
    val CLOUD_V1: Set<String> = setOf(
        // conversations
        "/api/v1/app-conversations",
        "/api/v1/app-conversations/count",
        "/api/v1/app-conversations/search",
        "/api/v1/app-conversations/start-tasks",
        "/api/v1/app-conversations/start-tasks/count",
        "/api/v1/app-conversations/start-tasks/search",
        "/api/v1/app-conversations/stream-start",
        "/api/v1/app-conversations/{}",
        "/api/v1/app-conversations/{}/download",
        "/api/v1/app-conversations/{}/file",
        "/api/v1/app-conversations/{}/files",
        "/api/v1/app-conversations/{}/git/changes",
        "/api/v1/app-conversations/{}/git/diff",
        "/api/v1/app-conversations/{}/hooks",
        "/api/v1/app-conversations/{}/send-message",
        "/api/v1/app-conversations/{}/skills",
        "/api/v1/app-conversations/{}/switch_acp_effort",
        "/api/v1/app-conversations/{}/switch_acp_model",
        "/api/v1/app-conversations/{}/switch_profile",
        // config
        "/api/v1/config/models/search",
        "/api/v1/config/providers/search",
        // events
        "/api/v1/conversation/{}/events",
        "/api/v1/conversation/{}/events/count",
        "/api/v1/conversation/{}/events/search",
        "/api/v1/conversations/{}/pending-messages",
        // git
        "/api/v1/git/branches/search",
        "/api/v1/git/installations/search",
        "/api/v1/git/repositories/details",
        "/api/v1/git/repositories/search",
        "/api/v1/git/suggested-tasks/search",
        // mcp
        "/api/v1/mcp/oauth/start",
        "/api/v1/mcp/oauth/status/{}",
        "/api/v1/mcp/servers/{}/call-tool",
        "/api/v1/mcp/test",
        // sandboxes
        "/api/v1/sandbox-specs",
        "/api/v1/sandbox-specs/search",
        "/api/v1/sandboxes",
        "/api/v1/sandboxes/search",
        "/api/v1/sandboxes/{}",
        "/api/v1/sandboxes/{}/pause",
        "/api/v1/sandboxes/{}/resume",
        "/api/v1/sandboxes/{}/settings/secrets",
        "/api/v1/sandboxes/{}/settings/secrets/{}",
        // secrets
        "/api/v1/secrets",
        "/api/v1/secrets/git-providers",
        "/api/v1/secrets/search",
        "/api/v1/secrets/{}",
        // settings
        "/api/v1/settings",
        "/api/v1/settings/agent-schema",
        "/api/v1/settings/conversation-schema",
        "/api/v1/settings/profiles",
        "/api/v1/settings/profiles/{}",
        "/api/v1/settings/profiles/{}/activate",
        "/api/v1/settings/profiles/{}/rename",
        // skills
        "/api/v1/skills/marketplace-check",
        "/api/v1/skills/marketplace-skills",
        "/api/v1/skills/search",
        // users
        "/api/v1/users/git-info",
        "/api/v1/users/git-organizations",
        "/api/v1/users/git-providers/{}",
        "/api/v1/users/me",
        // misc
        "/api/v1/web-client/config",
        // webhooks (server-to-server callbacks)
        "/api/v1/webhooks/conversations",
        "/api/v1/webhooks/conversations/{}/children",
        "/api/v1/webhooks/events/{}",
        "/api/v1/webhooks/mcp-oauth-state",
        "/api/v1/webhooks/secrets",
    )

    /** Paths under a sandbox's agent-server surface (`conversation_url`). */
    val AGENT_SERVER: Set<String> = setOf(
        // health / meta
        "/",
        "/alive",
        "/health",
        "/ready",
        "/server_info",
        // init / hooks
        "/api/init",
        "/api/hooks",
        // conversations (bare)
        "/api/conversations",
        "/api/conversations/count",
        "/api/conversations/search",
        // agent profiles
        "/api/agent-profiles",
        "/api/agent-profiles/{}",
        "/api/agent-profiles/{}/activate",
        "/api/agent-profiles/{}/materialize",
        "/api/agent-profiles/{}/rename",
        // auth
        "/api/auth/workspace-session",
        // acp
        "/api/acp/models",
        // bash
        "/api/bash/bash_commands/{}/stop",
        "/api/bash/bash_events",
        "/api/bash/bash_events/",
        "/api/bash/bash_events/search",
        "/api/bash/bash_events/{}",
        "/api/bash/execute_bash_command",
        "/api/bash/start_bash_command",
        // canvas extensions
        "/api/canvas-extensions/install",
        "/api/canvas-extensions/installed",
        "/api/canvas-extensions/installed/{}",
        "/api/canvas-extensions/installed/{}/backend",
        "/api/canvas-extensions/installed/{}/backend/data",
        "/api/canvas-extensions/installed/{}/backend/logs",
        "/api/canvas-extensions/installed/{}/backend/prepare",
        "/api/canvas-extensions/installed/{}/backend/secret-grant",
        "/api/canvas-extensions/installed/{}/backend/start",
        "/api/canvas-extensions/installed/{}/backend/stop",
        "/api/canvas-extensions/installed/{}/bundle",
        "/api/canvas-extensions/installed/{}/icon",
        // conversation-scoped runtime
        "/api/conversations/{}",
        "/api/conversations/{}/agent_final_response",
        "/api/conversations/{}/ask_agent",
        "/api/conversations/{}/bash/bash_commands/{}/stop",
        "/api/conversations/{}/bash/bash_events",
        "/api/conversations/{}/bash/bash_events/",
        "/api/conversations/{}/bash/bash_events/search",
        "/api/conversations/{}/bash/bash_events/{}",
        "/api/conversations/{}/bash/execute_bash_command",
        "/api/conversations/{}/bash/start_bash_command",
        "/api/conversations/{}/children",
        "/api/conversations/{}/condense",
        "/api/conversations/{}/confirmation_policy",
        "/api/conversations/{}/events",
        "/api/conversations/{}/events/count",
        "/api/conversations/{}/events/respond_to_confirmation",
        "/api/conversations/{}/events/search",
        "/api/conversations/{}/events/{}",
        "/api/conversations/{}/file/archive",
        "/api/conversations/{}/file/create_directory",
        "/api/conversations/{}/file/download",
        "/api/conversations/{}/file/download-trajectory/{}",
        "/api/conversations/{}/file/upload",
        "/api/conversations/{}/fork",
        "/api/conversations/{}/git/changes",
        "/api/conversations/{}/git/commits",
        "/api/conversations/{}/git/commits/{}/changes",
        "/api/conversations/{}/git/diff",
        "/api/conversations/{}/goal",
        "/api/conversations/{}/goal/resume",
        "/api/conversations/{}/goal/stop",
        "/api/conversations/{}/interrupt",
        "/api/conversations/{}/load_plugin",
        "/api/conversations/{}/navigate",
        "/api/conversations/{}/pause",
        "/api/conversations/{}/run",
        "/api/conversations/{}/runtime",
        "/api/conversations/{}/runtime/reprovision",
        "/api/conversations/{}/secrets",
        "/api/conversations/{}/security_analyzer",
        "/api/conversations/{}/switch_acp_effort",
        "/api/conversations/{}/switch_acp_model",
        "/api/conversations/{}/switch_llm",
        "/api/conversations/{}/switch_profile",
        "/api/conversations/{}/vscode/status",
        "/api/conversations/{}/vscode/url",
        "/api/conversations/{}/workspace",
        "/api/conversations/{}/workspace/{}",
        // files (bare)
        "/api/file/archive",
        "/api/file/create_directory",
        "/api/file/download",
        "/api/file/download-trajectory/{}",
        "/api/file/home",
        "/api/file/search_subdirs",
        "/api/file/upload",
        // git (bare)
        "/api/git/changes",
        "/api/git/commits",
        "/api/git/commits/{}/changes",
        "/api/git/diff",
        "/api/git/repositories/search",
        "/api/git/repositories/{}/{}/pull-requests/{}",
        // llm
        "/api/llm/models",
        "/api/llm/models/verified",
        "/api/llm/provider-connections",
        "/api/llm/provider-connections/{}",
        "/api/llm/providers",
        "/api/llm/subscription/openai/device/poll",
        "/api/llm/subscription/openai/device/start",
        "/api/llm/subscription/openai/logout",
        "/api/llm/subscription/openai/models",
        "/api/llm/subscription/openai/status",
        // mcp
        "/api/mcp/oauth/callback/{}",
        "/api/mcp/oauth/start",
        "/api/mcp/oauth/status/{}",
        "/api/mcp/test",
        // meta profiles
        "/api/meta-profiles",
        "/api/meta-profiles/{}",
        "/api/meta-profiles/{}/activate",
        // plugins
        "/api/plugins",
        "/api/plugins/fetch",
        "/api/plugins/install",
        "/api/plugins/installed",
        "/api/plugins/installed/{}",
        "/api/plugins/installed/{}/refresh",
        "/api/plugins/marketplace",
        // profiles (LLM)
        "/api/profiles",
        "/api/profiles/{}",
        "/api/profiles/{}/activate",
        "/api/profiles/{}/rename",
        "/api/profiles/{}/validate",
        // settings
        "/api/settings",
        "/api/settings/agent-schema",
        "/api/settings/conversation-schema",
        "/api/settings/mcp/{}",
        "/api/settings/secrets",
        "/api/settings/secrets/{}",
        // skills
        "/api/skills",
        "/api/skills/catalog",
        "/api/skills/install",
        "/api/skills/installed",
        "/api/skills/installed/{}",
        "/api/skills/installed/{}/refresh",
        "/api/skills/marketplace",
        "/api/skills/sync",
        // sub-agents
        "/api/sub-agents",
        // tools
        "/api/tools/",
        "/api/tools/catalog",
        // vscode
        "/api/vscode/status",
        "/api/vscode/url",
        // workspaces
        "/api/workspaces",
        "/api/workspaces/parents",
        // canvas app backends
        "/app-backends/{}/session",
        // OpenAI-compatible
        "/v1/chat/completions",
        "/v1/models",
        "/v1/responses",
    )

    /**
     * Cloud paths outside `/api/v1` that this client *does* implement, with a
     * real call site. Kept separate from [CLOUD_V1] because the spec-parity test
     * pins [CLOUD_V1] to exactly the `/api/v1` prefix.
     *
     *  - `/api/agent-profiles*` — the agent-profile CRUD surface (OpenHands and
     *    ACP). The New Task screen lists it to offer an ACP backend (Claude Code,
     *    Codex, Gemini CLI, Kimi, OpenCode, custom) and to read the profile body
     *    for the ACP server/model/effort. Reached with the Cloud Bearer token.
     */
    val CLOUD_EXTRA: Set<String> = setOf(
        "/api/agent-profiles",
        "/api/agent-profiles/{}",
        "/api/agent-profiles/{}/activate",
        "/api/agent-profiles/{}/deactivate",
        "/api/agent-profiles/{}/materialize",
        "/api/agent-profiles/{}/rename",
    )

    // Cloud paths that are deliberately NOT implemented by this client, with the
    // reason. This is not a dumping ground: a test asserts that every non-v1 path
    // in the bundled cloud spec is either in CLOUD_EXTRA or here, so a new server
    // path cannot silently slip out of scope.
    //
    // Out of scope for a personal, sandbox-centric phone client:
    //  - admin, organizations, service, keys, license, quota, billing, analytics:
    //    org-admin / billing / enterprise management surfaces.
    //  - email, idp, authenticate, logout, oauth, saml, keycloak, accept_tos,
    //    complete_onboarding, onboarding_status, saas: interactive web sign-in /
    //    onboarding flows a phone client delegates to the browser.
    //  - integration, slack: inbound webhook receivers for server-to-server use.
    //  - shared-conversations, shared-events: anonymous public share views.
    //  - tools/catalog, users/app: also served by the per-sandbox agent-server
    //    surface, which this app uses directly (see AGENT_SERVER).
    //  - alive, health, ready, server_info: liveness probes polled on the
    //    agent-server surface instead.
    val OUT_OF_SCOPE_CLOUD: Set<String> = setOf(
        "/alive",
        "/api/accept_tos",
        "/api/admin/directory/users",
        "/api/admin/directory/users/{}",
        "/api/admin/directory/users/{}/groups",
        "/api/admin/feature-flags",
        "/api/admin/feature-flags/{}",
        "/api/admin/feature-flags/{}/evaluate",
        "/api/admin/feature-flags/{}/rules",
        "/api/admin/feature-flags/{}/rules/{}",
        "/api/admin/instance-settings",
        "/api/admin/oauth-providers",
        "/api/admin/oauth-providers/saml-metadata-preview",
        "/api/admin/oauth-providers/{}",
        "/api/admin/oauth-providers/{}/users",
        "/api/admin/oauth-providers/{}/users/{}",
        "/api/admin/organizations",
        "/api/admin/organizations/{}",
        "/api/admin/quota/orgs/{}/quota",
        "/api/admin/quota/requests",
        "/api/admin/quota/requests/{}/approve",
        "/api/admin/quota/requests/{}/reject",
        "/api/admin/quota/users/{}/quota",
        "/api/admin/setup-state",
        "/api/admin/super-admins",
        "/api/admin/super-admins/me",
        "/api/admin/super-admins/{}",
        "/api/admin/users",
        "/api/admin/users/count",
        "/api/admin/users/{}",
        "/api/admin/users/{}/disable",
        "/api/admin/users/{}/enable",
        "/api/admin/verified-models",
        "/api/admin/verified-models/{}/{}",
        "/api/analytics/events",
        "/api/authenticate",
        "/api/billing/cancel",
        "/api/billing/create-checkout-session",
        "/api/billing/create-customer-setup-session",
        "/api/billing/credits",
        "/api/billing/has-payment-method",
        "/api/billing/subscription-access",
        "/api/billing/success",
        "/api/complete_onboarding",
        "/api/email",
        "/api/email/resend",
        "/api/email/verified",
        "/api/idp/password",
        "/api/idp/signup-links",
        "/api/idp/status",
        "/api/keycloak/link-context",
        "/api/keys",
        "/api/keys/current",
        "/api/keys/llm/byor",
        "/api/keys/llm/byor/permitted",
        "/api/keys/llm/byor/refresh",
        "/api/keys/llm/managed/refresh",
        "/api/keys/{}",
        "/api/license",
        "/api/license/status",
        "/api/license/usage",
        "/api/logout",
        "/api/onboarding_status",
        "/api/organizations",
        "/api/organizations/app",
        "/api/organizations/llm",
        "/api/organizations/members/invite/accept",
        "/api/organizations/{}",
        "/api/organizations/{}/budgets",
        "/api/organizations/{}/budgets/me",
        "/api/organizations/{}/budgets/overrides/{}",
        "/api/organizations/{}/conversations",
        "/api/organizations/{}/conversations/export",
        "/api/organizations/{}/conversations/my-usage",
        "/api/organizations/{}/conversations/stats",
        "/api/organizations/{}/conversations/usage-stats",
        "/api/organizations/{}/conversations/user-usage",
        "/api/organizations/{}/conversations/{}",
        "/api/organizations/{}/conversations/{}/stop",
        "/api/organizations/{}/git-claims",
        "/api/organizations/{}/git-claims/{}",
        "/api/organizations/{}/mcp-servers",
        "/api/organizations/{}/mcp-servers/{}",
        "/api/organizations/{}/mcp-servers/{}/connection",
        "/api/organizations/{}/me",
        "/api/organizations/{}/members",
        "/api/organizations/{}/members/count",
        "/api/organizations/{}/members/financial",
        "/api/organizations/{}/members/invite",
        "/api/organizations/{}/members/invite/{}",
        "/api/organizations/{}/members/{}",
        "/api/organizations/{}/meta-profiles",
        "/api/organizations/{}/meta-profiles/{}",
        "/api/organizations/{}/meta-profiles/{}/activate",
        "/api/organizations/{}/meta-profiles/{}/deactivate",
        "/api/organizations/{}/profiles",
        "/api/organizations/{}/profiles/{}",
        "/api/organizations/{}/profiles/{}/activate",
        "/api/organizations/{}/profiles/{}/rename",
        "/api/organizations/{}/provider-connections",
        "/api/organizations/{}/provider-connections/{}",
        "/api/organizations/{}/secrets",
        "/api/organizations/{}/secrets/{}",
        "/api/organizations/{}/secrets/{}/value",
        "/api/organizations/{}/secrets/{}/version",
        "/api/organizations/{}/service-accounts",
        "/api/organizations/{}/service-accounts/{}",
        "/api/organizations/{}/service-accounts/{}/disable",
        "/api/organizations/{}/service-accounts/{}/enable",
        "/api/organizations/{}/service-accounts/{}/secrets",
        "/api/organizations/{}/service-accounts/{}/secrets/{}",
        "/api/organizations/{}/settings",
        "/api/organizations/{}/switch",
        "/api/quota/increase-request",
        "/api/quota/status",
        "/api/quota/verify",
        "/api/service/health",
        "/api/service/users/{}/orgs/{}/api-keys",
        "/api/service/users/{}/orgs/{}/api-keys/{}",
        "/api/service/users/{}/orgs/{}/membership",
        "/api/service/users/{}/orgs/{}/secret-lookups",
        "/api/shared-conversations",
        "/api/shared-events",
        "/api/shared-events/count",
        "/api/shared-events/search",
        "/api/shared-events/{}/{}",
        "/api/tools/catalog",
        "/api/users/app",
        "/health",
        "/integration/azure-devops/events",
        "/integration/azure-devops/organizations",
        "/integration/azure-devops/reinstall-webhook",
        "/integration/azure-devops/resources",
        "/integration/azure-devops/uninstall-webhook",
        "/integration/bitbucket-dc/connections/{}/events",
        "/integration/bitbucket-dc/enroll-webhook",
        "/integration/bitbucket-dc/reinstall-webhook",
        "/integration/bitbucket-dc/resources",
        "/integration/bitbucket-dc/uninstall-webhook",
        "/integration/bitbucket-dc/webhook-id",
        "/integration/bitbucket/events",
        "/integration/github/events",
        "/integration/gitlab/events",
        "/integration/gitlab/reinstall-webhook",
        "/integration/gitlab/resources",
        "/integration/jira/callback",
        "/integration/jira/events",
        "/integration/jira/workspaces",
        "/integration/jira/workspaces/link",
        "/integration/jira/workspaces/status",
        "/integration/jira/workspaces/unlink",
        "/integration/jira/workspaces/validate/{}",
        "/oauth/device/authorize",
        "/oauth/device/token",
        "/oauth/device/verify-authenticated",
        "/oauth/git-providers",
        "/oauth/git-providers/{}/check",
        "/oauth/github/callback",
        "/oauth/idp-login",
        "/oauth/idp-providers",
        "/oauth/idp/invite",
        "/oauth/idp/login",
        "/oauth/idp/signup",
        "/oauth/keycloak/callback",
        "/oauth/keycloak/offline/callback",
        "/oauth/providers",
        "/oauth/{}",
        "/oauth/{}/callback",
        "/oauth/{}/link",
        "/oauth/{}/login",
        "/ready",
        "/saas",
        "/saml/{}/acs",
        "/saml/{}/login",
        "/saml/{}/metadata",
        "/server_info",
        "/slack/install",
        "/slack/install-callback",
        "/slack/keycloak-callback",
        "/slack/link-callback",
        "/slack/on-event",
        "/slack/on-form-interaction",
        "/slack/on-options-load",
        "/slack/status",
    )

    // Paths that exist in the agent-server spec but cannot be called by an OkHttp
    // client, with the reason. Kept in AGENT_SERVER so spec parity stays exact; the
    // wiring test excludes exactly this set (and fails if an entry becomes stale or
    // if any OTHER path is unwired).
    //
    //  - `/api/bash/bash_events/` is a GET that requires a JSON array request body.
    //    OkHttp rejects GET-with-body ("method GET must not have a request body").
    //    The same events are readable via `/api/bash/bash_events/search` (GET) and
    //    `/api/bash/bash_events/{event_id}`.
    val UNWIRED_BY_TRANSPORT: Set<String> = setOf(
        "/api/bash/bash_events/",
    )
}
