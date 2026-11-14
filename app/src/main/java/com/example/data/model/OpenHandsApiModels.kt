package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ---------------------------------------------------------------------------
// Cloud V1 — settings, profiles, users, git, sandboxes, web client
// Schemas mirror the live OpenAPI (v1.92.0) at https://app.all-hands.dev.
// ---------------------------------------------------------------------------

/** `GET /api/v1/settings` (schema `GETSettingsModel`). Only the fields the UI reads. */
@JsonClass(generateAdapter = true)
data class UserSettingsDto(
    @param:Json(name = "email") val email: String? = null,
    @param:Json(name = "language") val language: String? = null,
    @param:Json(name = "git_user_name") val gitUserName: String? = null,
    @param:Json(name = "git_user_email") val gitUserEmail: String? = null,
    @param:Json(name = "llm_api_key_set") val llmApiKeySet: Boolean = false,
    @param:Json(name = "max_budget_per_task") val maxBudgetPerTask: Double? = null,
    @param:Json(name = "enable_sound_notifications") val enableSoundNotifications: Boolean? = null,
    @param:Json(name = "v1_enabled") val v1Enabled: Boolean? = null,
    @param:Json(name = "llm_profiles") val llmProfiles: Map<String, Any>? = null,
    @param:Json(name = "disabled_skills") val disabledSkills: List<String>? = null,
    @param:Json(name = "secrets_store") val secretsStore: Map<String, Any>? = null,
    @param:Json(name = "active_agent_profile_id") val activeAgentProfileId: String? = null,
    @param:Json(name = "default_sandbox_spec_id") val defaultSandboxSpecId: String? = null,
    @param:Json(name = "sandbox_grouping_strategy") val sandboxGroupingStrategy: String? = null,
)

/** Body for `POST /api/v1/settings` (a partial update; schema is a free object). */
@JsonClass(generateAdapter = true)
data class UpdateUserSettingsRequest(
    @param:Json(name = "language") val language: String? = null,
    @param:Json(name = "git_user_name") val gitUserName: String? = null,
    @param:Json(name = "git_user_email") val gitUserEmail: String? = null,
    @param:Json(name = "max_budget_per_task") val maxBudgetPerTask: Double? = null,
    @param:Json(name = "enable_sound_notifications") val enableSoundNotifications: Boolean? = null,
)

/** `GET /api/v1/settings/profiles/{name}` (schema `..._ProfileDetailResponse`). */
@JsonClass(generateAdapter = true)
data class ProfileDetailDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "api_key_set") val apiKeySet: Boolean = false,
    @param:Json(name = "config") val config: ProfileConfigDto? = null,
)

/** The `config` block of a saved LLM profile. */
@JsonClass(generateAdapter = true)
data class ProfileConfigDto(
    @param:Json(name = "model") val model: String? = null,
    @param:Json(name = "base_url") val baseUrl: String? = null,
    @param:Json(name = "provider_connection_id") val providerConnectionId: String? = null,
)

/** Body for `POST /api/v1/settings/profiles/{name}` (schema `SaveProfileRequest`). */
@JsonClass(generateAdapter = true)
data class SaveProfileRequest(
    @param:Json(name = "llm") val llm: ProfileConfigDto? = null,
    @param:Json(name = "include_secrets") val includeSecrets: Boolean = false,
    @param:Json(name = "preserve_existing_api_key") val preserveExistingApiKey: Boolean = false,
)

/** Body for `POST /api/v1/settings/profiles/{name}/rename`. */
@JsonClass(generateAdapter = true)
data class RenameProfileRequest(
    @param:Json(name = "new_name") val newName: String,
)

/** Generic profile mutation reply (`{message, name}`). */
@JsonClass(generateAdapter = true)
data class ProfileMutationDto(
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "model") val model: String? = null,
)

/** `GET /api/v1/users/git-organizations` (schema `GitOrganizationsResponse`). */
@JsonClass(generateAdapter = true)
data class GitOrganizationsDto(
    @param:Json(name = "provider") val provider: String? = null,
    @param:Json(name = "organizations") val organizations: List<String>? = null,
)

/** `GET /api/v1/git/installations/search` — items are installation **IDs** (strings). */
@JsonClass(generateAdapter = true)
data class GitInstallationPageResponse(
    @param:Json(name = "items") val items: List<String>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null,
)

/** `GET /api/v1/git/repositories/details` (schema `Repository`). */
@JsonClass(generateAdapter = true)
data class RepositoryDetailDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "full_name") val fullName: String? = null,
    @param:Json(name = "git_provider") val gitProvider: String? = null,
    @param:Json(name = "main_branch") val mainBranch: String? = null,
    @param:Json(name = "is_public") val isPublic: Boolean = false,
    @param:Json(name = "owner_type") val ownerType: String? = null,
    @param:Json(name = "stargazers_count") val stargazersCount: Int = 0,
    @param:Json(name = "pushed_at") val pushedAt: String? = null,
)

/** One secret name+description (`SecretNameItem`); the value is never returned. */
@JsonClass(generateAdapter = true)
data class SecretNameItemDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "description") val description: String? = null,
)

/** `GET /api/v1/sandboxes/{id}/settings/secrets` (schema `SecretNamesResponse`). */
@JsonClass(generateAdapter = true)
data class SecretNamesDto(
    @param:Json(name = "secrets") val secrets: List<SecretNameItemDto>? = null,
)

/** One ACP provider the web client offers (`ACPProviderConfig`). */
@JsonClass(generateAdapter = true)
data class AcpProviderConfigDto(
    @param:Json(name = "key") val key: String? = null,
    @param:Json(name = "display_name") val displayName: String? = null,
    @param:Json(name = "default_command") val defaultCommand: List<String>? = null,
    @param:Json(name = "api_key_env_var") val apiKeyEnvVar: String? = null,
)

/** `GET /api/v1/web-client/config` (schema `WebClientConfig`). */
@JsonClass(generateAdapter = true)
data class WebClientConfigDto(
    @param:Json(name = "app_mode") val appMode: String? = null,
    @param:Json(name = "auth_url") val authUrl: String? = null,
    @param:Json(name = "github_app_slug") val githubAppSlug: String? = null,
    @param:Json(name = "feature_flags") val featureFlags: Map<String, Boolean>? = null,
    @param:Json(name = "providers_configured") val providersConfigured: List<String>? = null,
    @param:Json(name = "acp_providers") val acpProviders: List<AcpProviderConfigDto>? = null,
    @param:Json(name = "faulty_models") val faultyModels: List<String>? = null,
    @param:Json(name = "maintenance_start_time") val maintenanceStartTime: String? = null,
)

/** `GET /api/v1/app-conversations/start-tasks/search` page. */
@JsonClass(generateAdapter = true)
data class StartTaskPageResponse(
    @param:Json(name = "items") val items: List<StartTaskResponse>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null,
)

/** Body for `POST /api/v1/secrets/git-providers` (schema `POSTProviderModel`). */
@JsonClass(generateAdapter = true)
data class GitProviderTokenRequest(
    @param:Json(name = "provider_tokens") val providerTokens: Map<String, String>? = null,
    @param:Json(name = "mcp_config") val mcpConfig: Map<String, Any>? = null,
)

/** Body for `POST /api/v1/mcp/servers/{name}/call-tool` (schema `MCPToolCallSpec`). */
@JsonClass(generateAdapter = true)
data class McpToolCallRequest(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "arguments") val arguments: Map<String, Any>? = null,
)

@JsonClass(generateAdapter = true)
data class McpToolCallResultDto(
    @param:Json(name = "is_error") val isError: Boolean = false,
    @param:Json(name = "text") val text: String? = null,
)

/** `{"message": "..."}` reply used by several edit endpoints. */
@JsonClass(generateAdapter = true)
data class EditResponseDto(
    @param:Json(name = "message") val message: String? = null,
)

// ---------------------------------------------------------------------------
// Agent server — runtime extras (goal, fork, workspace, vscode, bash events)
// ---------------------------------------------------------------------------

/** `POST /api/conversations/{id}/goal` (schema `StartGoalRequest`). */
@JsonClass(generateAdapter = true)
data class StartGoalRequest(
    @param:Json(name = "goal") val goal: String? = null,
    @param:Json(name = "max_iterations") val maxIterations: Int? = null,
)

/** `POST /api/conversations/{id}/confirmation_policy` (schema `ConfirmationPolicy`). */
@JsonClass(generateAdapter = true)
data class ConfirmationPolicyRequest(
    @param:Json(name = "policy") val policy: String? = null,
)

/** `POST /api/conversations/{id}/events/respond_to_confirmation`. */
@JsonClass(generateAdapter = true)
data class ConfirmationResponseRequest(
    @param:Json(name = "accept") val accept: Boolean = true,
    @param:Json(name = "reason") val reason: String? = null,
)

/** `POST /api/conversations/{id}/fork` (schema `ForkConversationRequest`). */
@JsonClass(generateAdapter = true)
data class ForkConversationRequest(
    @param:Json(name = "include_skills") val includeSkills: Boolean = true,
)

/** `GET /api/conversations/{id}/agent_final_response`. */
@JsonClass(generateAdapter = true)
data class AgentFinalResponseDto(
    @param:Json(name = "response") val response: String? = null,
    @param:Json(name = "status") val status: String? = null,
)

/** `GET /api/conversations/{id}/runtime` (schema `ConversationRuntimeInfo`). */
@JsonClass(generateAdapter = true)
data class ConversationRuntimeDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "workspace_dir") val workspaceDir: String? = null,
    @param:Json(name = "sandbox_id") val sandboxId: String? = null,
)

/** `GET /api/vscode/url` (schema `VSCodeUrlResponse`). */
@JsonClass(generateAdapter = true)
data class VSCodeUrlDto(
    @param:Json(name = "url") val url: String? = null,
    @param:Json(name = "vscode_url") val vscodeUrl: String? = null,
)

/** `GET /api/file/archive` — a base64 zip of the requested paths. */
@JsonClass(generateAdapter = true)
data class FileArchiveDto(
    @param:Json(name = "path") val path: String? = null,
    @param:Json(name = "base64") val base64: String? = null,
)

/** One runtime bash event (`BashEvent` variants carry `kind`). */
@JsonClass(generateAdapter = true)
data class BashEventDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "kind") val kind: String? = null,
    @param:Json(name = "command") val command: String? = null,
    @param:Json(name = "command_id") val commandId: String? = null,
    @param:Json(name = "exit_code") val exitCode: Int? = null,
    @param:Json(name = "stdout") val stdout: String? = null,
    @param:Json(name = "stderr") val stderr: String? = null,
    @param:Json(name = "timestamp") val timestamp: String? = null,
)

@JsonClass(generateAdapter = true)
data class BashEventPageDto(
    @param:Json(name = "items") val items: List<BashEventDto>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null,
)

/** `POST /api/conversations/{id}/secrets` (schema `UpdateSecretsRequest`). */
@JsonClass(generateAdapter = true)
data class UpdateSecretsRequest(
    @param:Json(name = "secrets") val secrets: Map<String, String>? = null,
)

/** `POST /api/conversations/{id}/security_analyzer`. */
@JsonClass(generateAdapter = true)
data class SetSecurityAnalyzerRequest(
    @param:Json(name = "enabled") val enabled: Boolean = true,
)

// ---------------------------------------------------------------------------
// Agent server — skills / plugins / MCP / settings
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class InstalledSkillDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "description") val description: String? = null,
    @param:Json(name = "enabled") val enabled: Boolean = true,
    @param:Json(name = "source") val source: String? = null,
    @param:Json(name = "version") val version: String? = null,
)

@JsonClass(generateAdapter = true)
data class InstalledSkillsResponseDto(
    @param:Json(name = "skills") val skills: List<InstalledSkillDto>? = null,
)

@JsonClass(generateAdapter = true)
data class SkillCatalogDto(
    @param:Json(name = "skills") val skills: List<InstalledSkillDto>? = null,
)

@JsonClass(generateAdapter = true)
data class InstallSkillRequest(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "source") val source: String? = null,
)

@JsonClass(generateAdapter = true)
data class InstalledPluginDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "version") val version: String? = null,
    @param:Json(name = "enabled") val enabled: Boolean = true,
    @param:Json(name = "source") val source: String? = null,
)

@JsonClass(generateAdapter = true)
data class InstalledPluginsResponseDto(
    @param:Json(name = "plugins") val plugins: List<InstalledPluginDto>? = null,
)

@JsonClass(generateAdapter = true)
data class InstallPluginRequest(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "source") val source: String? = null,
)

@JsonClass(generateAdapter = true)
data class McpTestRequest(
    @param:Json(name = "server") val server: String? = null,
    @param:Json(name = "url") val url: String? = null,
)

@JsonClass(generateAdapter = true)
data class RuntimeSettingsDto(
    @param:Json(name = "agent_settings") val agentSettings: Map<String, Any>? = null,
    @param:Json(name = "conversation_settings") val conversationSettings: Map<String, Any>? = null,
    @param:Json(name = "mcp_config") val mcpConfig: Map<String, Any>? = null,
)

@JsonClass(generateAdapter = true)
data class UpdateRuntimeSettingsRequest(
    @param:Json(name = "agent_settings") val agentSettings: Map<String, Any>? = null,
    @param:Json(name = "conversation_settings") val conversationSettings: Map<String, Any>? = null,
)

@JsonClass(generateAdapter = true)
data class LlmProviderDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "verified") val verified: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class LlmProvidersResponseDto(
    @param:Json(name = "providers") val providers: List<LlmProviderDto>? = null,
)

@JsonClass(generateAdapter = true)
data class RuntimeLlmModelDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "provider") val provider: String? = null,
)

@JsonClass(generateAdapter = true)
data class RuntimeModelsResponseDto(
    @param:Json(name = "models") val models: List<RuntimeLlmModelDto>? = null,
)

@JsonClass(generateAdapter = true)
data class ToolCatalogEntryDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "description") val description: String? = null,
)

@JsonClass(generateAdapter = true)
data class ToolCatalogResponseDto(
    @param:Json(name = "tools") val tools: List<ToolCatalogEntryDto>? = null,
)

// ---------------------------------------------------------------------------
// MCP OAuth + marketplace + plugins + sub-agents
// ---------------------------------------------------------------------------

/** Body for the MCP OAuth start calls (schema `MCPTestRequestBody`/`MCPTestRequest`). */
@JsonClass(generateAdapter = true)
data class McpOAuthStartRequest(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "server") val server: Map<String, Any>? = null,
    @param:Json(name = "timeout") val timeout: Double? = null,
)

@JsonClass(generateAdapter = true)
data class McpOAuthStartResponseDto(
    @param:Json(name = "ok") val ok: Boolean = false,
    @param:Json(name = "job_id") val jobId: String? = null,
    @param:Json(name = "authorization_url") val authorizationUrl: String? = null,
    @param:Json(name = "error") val error: String? = null,
    @param:Json(name = "error_kind") val errorKind: String? = null,
)

@JsonClass(generateAdapter = true)
data class McpOAuthStatusResponseDto(
    @param:Json(name = "ok") val ok: Boolean = false,
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "job_id") val jobId: String? = null,
    @param:Json(name = "authorization_url") val authorizationUrl: String? = null,
    @param:Json(name = "callback_ready") val callbackReady: Boolean = false,
    @param:Json(name = "error") val error: String? = null,
)

/** A marketplace registration (schema `MarketplaceRegistration`). */
@JsonClass(generateAdapter = true)
data class MarketplaceRegistrationDto(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "source") val source: String,
    @param:Json(name = "ref") val ref: String? = null,
    @param:Json(name = "repo_path") val repoPath: String? = null,
    @param:Json(name = "auto_load") val autoLoad: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class MarketplaceCheckResponseDto(
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "reason") val reason: String? = null,
    @param:Json(name = "plugin_count") val pluginCount: Int = 0,
    @param:Json(name = "skill_count") val skillCount: Int = 0,
)

@JsonClass(generateAdapter = true)
data class MarketplaceSkillsPreviewDto(
    @param:Json(name = "skills") val skills: List<String>? = null,
    @param:Json(name = "plugins") val plugins: List<String>? = null,
    @param:Json(name = "errors") val errors: List<String>? = null,
    @param:Json(name = "conflicts") val conflicts: List<String>? = null,
)

/** Body for `POST /api/plugins` (schema `PluginsRequest`). */
@JsonClass(generateAdapter = true)
data class PluginsRequestDto(
    @param:Json(name = "load_user") val loadUser: Boolean = true,
    @param:Json(name = "load_project") val loadProject: Boolean = true,
    @param:Json(name = "project_dir") val projectDir: String? = null,
)

/** Body for `POST /api/plugins/fetch` (schema `FetchPluginRequest`). */
@JsonClass(generateAdapter = true)
data class FetchPluginRequestDto(
    @param:Json(name = "source") val source: String,
    @param:Json(name = "ref") val ref: String? = null,
    @param:Json(name = "repo_path") val repoPath: String? = null,
)

@JsonClass(generateAdapter = true)
data class FetchPluginResponseDto(
    @param:Json(name = "path") val path: String? = null,
    @param:Json(name = "resolved_ref") val resolvedRef: String? = null,
    @param:Json(name = "source") val source: String? = null,
    @param:Json(name = "skills") val skills: List<String>? = null,
    @param:Json(name = "commands") val commands: List<String>? = null,
)

/** Body for `POST /api/skills` (schema `SkillsRequest`). */
@JsonClass(generateAdapter = true)
data class SkillsRequestDto(
    @param:Json(name = "load_public") val loadPublic: Boolean = true,
    @param:Json(name = "load_user") val loadUser: Boolean = true,
    @param:Json(name = "load_project") val loadProject: Boolean = true,
    @param:Json(name = "load_org") val loadOrg: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class SkillsResponseDto(
    @param:Json(name = "skills") val skills: List<InstalledSkillDto>? = null,
)

/** Body for `POST /api/sub-agents` (schema `SubAgentsRequest`). */
@JsonClass(generateAdapter = true)
data class SubAgentsRequestDto(
    @param:Json(name = "load_user") val loadUser: Boolean = true,
    @param:Json(name = "load_project") val loadProject: Boolean = true,
    @param:Json(name = "load_builtin") val loadBuiltin: Boolean = true,
    @param:Json(name = "project_dir") val projectDir: String? = null,
)

@JsonClass(generateAdapter = true)
data class SubAgentDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "description") val description: String? = null,
)

@JsonClass(generateAdapter = true)
data class SubAgentsResponseDto(
    @param:Json(name = "agents") val agents: List<SubAgentDto>? = null,
)

/** Body for `POST /api/conversations/{id}/load_plugin`. */
@JsonClass(generateAdapter = true)
data class LoadPluginRequest(
    @param:Json(name = "source") val source: String,
    @param:Json(name = "ref") val ref: String? = null,
    @param:Json(name = "repo_path") val repoPath: String? = null,
)

/** `POST /api/settings/mcp/{settings_key}` (schema `MCPServer-Input`). */
@JsonClass(generateAdapter = true)
data class McpServerInputDto(
    @param:Json(name = "url") val url: String? = null,
    @param:Json(name = "transport") val transport: String? = null,
    @param:Json(name = "command") val command: String? = null,
    @param:Json(name = "args") val args: List<String>? = null,
    @param:Json(name = "env") val env: Map<String, String>? = null,
    @param:Json(name = "description") val description: String? = null,
    @param:Json(name = "enabled") val enabled: Boolean = true,
)

/** `GET /api/v1/skills/search` page (schema `SkillPage`). */
@JsonClass(generateAdapter = true)
data class SkillPageDto(
    @param:Json(name = "items") val items: List<InstalledSkillDto>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null,
)

/** `POST /api/conversations/{id}/children` (schema `StartChildConversationRequest`). */
@JsonClass(generateAdapter = true)
data class StartChildConversationRequest(
    @param:Json(name = "task") val task: String,
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "isolation") val isolation: String? = null,
)

/** `POST /api/conversations/{id}/navigate` (schema `NavigateConversationRequest`). */
@JsonClass(generateAdapter = true)
data class NavigateConversationRequest(
    @param:Json(name = "event_id") val eventId: String? = null,
)
