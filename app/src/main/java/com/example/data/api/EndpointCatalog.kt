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
}
