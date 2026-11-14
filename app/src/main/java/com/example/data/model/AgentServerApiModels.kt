package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ---------------------------------------------------------------------------
// Health / init / hooks
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class HealthStatusDto(
    @param:Json(name = "status") val status: String? = null,
)

@JsonClass(generateAdapter = true)
data class InitRequestDto(
    @param:Json(name = "session_api_keys") val sessionApiKeys: List<String>? = null,
    @param:Json(name = "web_url") val webUrl: String? = null,
    @param:Json(name = "conversations_path") val conversationsPath: String? = null,
    @param:Json(name = "bash_events_dir") val bashEventsDir: String? = null,
    @param:Json(name = "max_concurrent_runs") val maxConcurrentRuns: Int? = null,
)

@JsonClass(generateAdapter = true)
data class InitStatusDto(
    @param:Json(name = "state") val state: String? = null,
    @param:Json(name = "error") val error: String? = null,
)

@JsonClass(generateAdapter = true)
data class HooksRequestDto(
    @param:Json(name = "project_dir") val projectDir: String? = null,
    @param:Json(name = "load_project_hooks") val loadProjectHooks: Boolean? = null,
    @param:Json(name = "load_user_hooks") val loadUserHooks: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class HooksResponseDto(
    @param:Json(name = "hook_config") val hookConfig: Map<String, Any>? = null,
)

// ---------------------------------------------------------------------------
// Conversations (bare agent-server routes)
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class AgentConversationPageDto(
    @param:Json(name = "items") val items: List<ConversationInfoDto>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null,
)

@JsonClass(generateAdapter = true)
data class SendMessageRequestDto(
    @param:Json(name = "role") val role: String? = null,
    @param:Json(name = "content") val content: List<TextContentDto>? = null,
    @param:Json(name = "run") val run: Boolean? = null,
)

// ---------------------------------------------------------------------------
// git (agent-server, bare routes)
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class GitRepositoryPageDto(
    @param:Json(name = "items") val items: List<RepositoryDetailDto>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null,
    @param:Json(name = "missing_token") val missingToken: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class GitPullRequestStatusDto(
    @param:Json(name = "git_provider") val gitProvider: String? = null,
    @param:Json(name = "repository") val repository: String? = null,
    @param:Json(name = "number") val number: Int? = null,
    @param:Json(name = "state") val state: String? = null,
    @param:Json(name = "draft") val draft: Boolean? = null,
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "url") val url: String? = null,
    @param:Json(name = "head_sha") val headSha: String? = null,
)

// ---------------------------------------------------------------------------
// LLM provider connections + OpenAI subscription
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class ProviderConnectionDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "display_name") val displayName: String? = null,
    @param:Json(name = "provider") val provider: String? = null,
    @param:Json(name = "base_url") val baseUrl: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "updated_at") val updatedAt: String? = null,
    @param:Json(name = "api_key_set") val apiKeySet: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class ProviderConnectionCreateRequest(
    @param:Json(name = "display_name") val displayName: String? = null,
    @param:Json(name = "provider") val provider: String? = null,
    @param:Json(name = "api_key") val apiKey: String? = null,
    @param:Json(name = "base_url") val baseUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class SubscriptionStatusDto(
    @param:Json(name = "vendor") val vendor: String? = null,
    @param:Json(name = "connected") val connected: Boolean = false,
    @param:Json(name = "account_email") val accountEmail: String? = null,
    @param:Json(name = "expires_at") val expiresAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class SubscriptionDeviceStartDto(
    @param:Json(name = "device_code") val deviceCode: String? = null,
    @param:Json(name = "user_code") val userCode: String? = null,
    @param:Json(name = "verification_uri") val verificationUri: String? = null,
    @param:Json(name = "verification_uri_complete") val verificationUriComplete: String? = null,
    @param:Json(name = "expires_at") val expiresAt: String? = null,
    @param:Json(name = "interval_seconds") val intervalSeconds: Int? = null,
)

@JsonClass(generateAdapter = true)
data class SubscriptionDevicePollRequest(
    @param:Json(name = "device_code") val deviceCode: String,
)

@JsonClass(generateAdapter = true)
data class SubscriptionModelsDto(
    @param:Json(name = "vendor") val vendor: String? = null,
    @param:Json(name = "models") val models: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class VerifiedModelsDto(
    @param:Json(name = "models") val models: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class McpOAuthCallbackRequest(
    @param:Json(name = "callback_url") val callbackUrl: String,
)

// ---------------------------------------------------------------------------
// Profiles (agent-server LLM profiles) + agent profiles + meta profiles
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class RuntimeProfileListDto(
    @param:Json(name = "profiles") val profiles: List<String>? = null,
    @param:Json(name = "active_profile") val activeProfile: String? = null,
)

@JsonClass(generateAdapter = true)
data class RuntimeProfileDetailDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "config") val config: Map<String, Any>? = null,
    @param:Json(name = "api_key_set") val apiKeySet: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class SaveRuntimeProfileRequest(
    @param:Json(name = "llm") val llm: Map<String, Any>? = null,
)

@JsonClass(generateAdapter = true)
data class RenameAgentProfileRequest(
    @param:Json(name = "new_name") val newName: String,
)

@JsonClass(generateAdapter = true)
data class ProfileMutationResponseDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "message") val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class ActivateProfileResponseDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "llm_applied") val llmApplied: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class ValidateProfileResponseDto(
    @param:Json(name = "valid") val valid: Boolean = false,
    @param:Json(name = "error") val error: String? = null,
)

@JsonClass(generateAdapter = true)
data class AgentProfileDetailDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "profile") val profile: Map<String, Any>? = null,
)

@JsonClass(generateAdapter = true)
data class AgentProfileMutationDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "message") val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class AgentProfileDiagnosticsDto(
    @param:Json(name = "agent_kind") val agentKind: String? = null,
    @param:Json(name = "valid") val valid: Boolean = false,
    @param:Json(name = "errors") val errors: List<String>? = null,
    @param:Json(name = "unusable_tools") val unusableTools: List<String>? = null,
    @param:Json(name = "resolved_skills") val resolvedSkills: List<String>? = null,
    @param:Json(name = "dangling_mcp_server_refs") val danglingMcpServerRefs: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class ActivateAgentProfileDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "agent_settings_applied") val agentSettingsApplied: Boolean? = null,
)

@JsonClass(generateAdapter = true)
data class MetaProfileListDto(
    @param:Json(name = "meta_profiles") val metaProfiles: List<String>? = null,
    @param:Json(name = "active_meta_profile") val activeMetaProfile: String? = null,
)

@JsonClass(generateAdapter = true)
data class MetaProfileDetailDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "config") val config: MetaProfileConfigDto? = null,
)

@JsonClass(generateAdapter = true)
data class MetaProfileConfigDto(
    @param:Json(name = "classifier_model") val classifierModel: String? = null,
    @param:Json(name = "classes") val classes: List<String>? = null,
    @param:Json(name = "prompt_template") val promptTemplate: String? = null,
)

@JsonClass(generateAdapter = true)
data class ActivateMetaProfileDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "applied") val applied: Boolean? = null,
)

// ---------------------------------------------------------------------------
// Plugins / skills (installed-item operations)
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class InstalledPluginDetailDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "version") val version: String? = null,
    @param:Json(name = "description") val description: String? = null,
    @param:Json(name = "enabled") val enabled: Boolean = true,
    @param:Json(name = "source") val source: String? = null,
    @param:Json(name = "resolved_ref") val resolvedRef: String? = null,
    @param:Json(name = "install_path") val installPath: String? = null,
)

@JsonClass(generateAdapter = true)
data class UpdatePluginStateRequest(
    @param:Json(name = "enabled") val enabled: Boolean,
)

@JsonClass(generateAdapter = true)
data class UpdatePluginStateDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "enabled") val enabled: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class UpdatePluginDto(
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "plugin") val plugin: InstalledPluginDetailDto? = null,
)

@JsonClass(generateAdapter = true)
data class InstalledSkillDetailDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "version") val version: String? = null,
    @param:Json(name = "description") val description: String? = null,
    @param:Json(name = "enabled") val enabled: Boolean = true,
    @param:Json(name = "source") val source: String? = null,
    @param:Json(name = "install_path") val installPath: String? = null,
)

@JsonClass(generateAdapter = true)
data class UpdateSkillStateRequest(
    @param:Json(name = "enabled") val enabled: Boolean,
)

@JsonClass(generateAdapter = true)
data class UpdateSkillStateDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "enabled") val enabled: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class UpdateSkillDto(
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "skill") val skill: InstalledSkillDetailDto? = null,
)

// ---------------------------------------------------------------------------
// Settings schemas + secrets (agent-server)
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class SettingsSchemaDto(
    @param:Json(name = "model_name") val modelName: String? = null,
    @param:Json(name = "sections") val sections: List<Map<String, Any>>? = null,
)

@JsonClass(generateAdapter = true)
data class RuntimeSecretItemDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "description") val description: String? = null,
    @param:Json(name = "scope") val scope: String? = null,
    @param:Json(name = "scope_description") val scopeDescription: String? = null,
)

@JsonClass(generateAdapter = true)
data class RuntimeSecretsListDto(
    @param:Json(name = "secrets") val secrets: List<RuntimeSecretItemDto>? = null,
)

@JsonClass(generateAdapter = true)
data class SecretCreateRequest(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "value") val value: String,
    @param:Json(name = "description") val description: String? = null,
)

// ---------------------------------------------------------------------------
// Workspaces
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class WorkspacesListDto(
    @param:Json(name = "workspaces") val workspaces: List<String>? = null,
    @param:Json(name = "workspaceParents") val workspaceParents: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class AddWorkspacesRequest(
    @param:Json(name = "workspaces") val workspaces: List<String>,
)

@JsonClass(generateAdapter = true)
data class AddWorkspaceParentsRequest(
    @param:Json(name = "parents") val parents: List<String>,
)

@JsonClass(generateAdapter = true)
data class DeleteResponseDto(
    @param:Json(name = "deleted") val deleted: Boolean? = null,
)

// ---------------------------------------------------------------------------
// Canvas extension app backends + OpenAI-compatible surface
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class AppBackendSessionDto(
    @param:Json(name = "ingress_url") val ingressUrl: String? = null,
    @param:Json(name = "expires_at") val expiresAt: String? = null,
    @param:Json(name = "iframe_sandbox") val iframeSandbox: String? = null,
)

@JsonClass(generateAdapter = true)
data class OpenAiModelListDto(
    @param:Json(name = "object") val objectType: String? = null,
    @param:Json(name = "data") val data: List<Map<String, Any>>? = null,
)
