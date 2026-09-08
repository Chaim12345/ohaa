package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/*
 * Wire models for the OpenHands Cloud V1 REST API.
 * Field names mirror https://app.all-hands.dev/openapi.json exactly.
 */

// ---------------------------------------------------------------------------
// Shared content blocks
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class TextContentDto(
    @field:Json(name = "type") val type: String = "text",
    @field:Json(name = "text") val text: String = ""
)

/** SendMessageRequest / AppSendMessageRequest */
@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    @field:Json(name = "role") val role: String = "user",
    @field:Json(name = "content") val content: List<TextContentDto>,
    @field:Json(name = "run") val run: Boolean = true
) {
    companion object {
        fun text(text: String, run: Boolean = true) =
            SendMessageRequest(content = listOf(TextContentDto(text = text)), run = run)
    }
}

// ---------------------------------------------------------------------------
// Conversations
// ---------------------------------------------------------------------------

/** AppConversationStartRequest */
@JsonClass(generateAdapter = true)
data class StartConversationRequest(
    @field:Json(name = "initial_message") val initialMessage: SendMessageRequest? = null,
    @field:Json(name = "selected_repository") val selectedRepository: String? = null,
    @field:Json(name = "selected_branch") val selectedBranch: String? = null,
    @field:Json(name = "git_provider") val gitProvider: String? = null,
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "llm_model") val llmModel: String? = null,
    @field:Json(name = "agent_type") val agentType: String = "default",
    @field:Json(name = "agent_profile_id") val agentProfileId: String? = null,
    @field:Json(name = "sandbox_id") val sandboxId: String? = null,
    @field:Json(name = "conversation_id") val conversationId: String? = null,
    @field:Json(name = "parent_conversation_id") val parentConversationId: String? = null,
    @field:Json(name = "system_message_suffix") val systemMessageSuffix: String? = null,
    @field:Json(name = "public") val public: Boolean? = null,
    @field:Json(name = "pr_number") val prNumber: List<Int>? = null,
    @field:Json(name = "observability_span_name") val observabilitySpanName: String? = null,
    @field:Json(name = "observability_tags") val observabilityTags: List<String>? = null,
    @field:Json(name = "observability_metadata") val observabilityMetadata: Map<String, String>? = null,
    @field:Json(name = "secrets") val secrets: Map<String, String>? = null
)

/** AppConversationStartTask */
@JsonClass(generateAdapter = true)
data class StartTaskDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "status") val status: String = "WORKING",
    @field:Json(name = "detail") val detail: String? = null,
    @field:Json(name = "app_conversation_id") val appConversationId: String? = null,
    @field:Json(name = "sandbox_id") val sandboxId: String? = null,
    @field:Json(name = "agent_server_url") val agentServerUrl: String? = null,
    @field:Json(name = "created_at") val createdAt: String? = null,
    @field:Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class TokenUsageDto(
    @field:Json(name = "model") val model: String? = null,
    @field:Json(name = "prompt_tokens") val promptTokens: Long = 0,
    @field:Json(name = "completion_tokens") val completionTokens: Long = 0,
    @field:Json(name = "cache_read_tokens") val cacheReadTokens: Long = 0,
    @field:Json(name = "cache_write_tokens") val cacheWriteTokens: Long = 0,
    @field:Json(name = "reasoning_tokens") val reasoningTokens: Long = 0,
    @field:Json(name = "context_window") val contextWindow: Long = 0
)

/** MetricsSnapshot */
@JsonClass(generateAdapter = true)
data class MetricsDto(
    @field:Json(name = "model_name") val modelName: String? = null,
    @field:Json(name = "accumulated_cost") val accumulatedCost: Double = 0.0,
    @field:Json(name = "max_budget_per_task") val maxBudgetPerTask: Double? = null,
    @field:Json(name = "accumulated_token_usage") val accumulatedTokenUsage: TokenUsageDto? = null
)

/** AppConversation */
@JsonClass(generateAdapter = true)
data class AppConversationDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "created_by_user_id") val createdByUserId: String? = null,
    @field:Json(name = "sandbox_id") val sandboxId: String? = null,
    @field:Json(name = "selected_repository") val selectedRepository: String? = null,
    @field:Json(name = "selected_branch") val selectedBranch: String? = null,
    @field:Json(name = "git_provider") val gitProvider: String? = null,
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "trigger") val trigger: String? = null,
    @field:Json(name = "pr_number") val prNumber: List<Int>? = null,
    @field:Json(name = "llm_model") val llmModel: String? = null,
    @field:Json(name = "agent_kind") val agentKind: String? = null,
    @field:Json(name = "metrics") val metrics: MetricsDto? = null,
    @field:Json(name = "parent_conversation_id") val parentConversationId: String? = null,
    @field:Json(name = "sub_conversation_ids") val subConversationIds: List<String>? = null,
    @field:Json(name = "public") val public: Boolean? = null,
    @field:Json(name = "tags") val tags: Map<String, String>? = null,
    @field:Json(name = "created_at") val createdAt: String? = null,
    @field:Json(name = "updated_at") val updatedAt: String? = null,
    @field:Json(name = "sandbox_status") val sandboxStatus: String? = null,
    @field:Json(name = "execution_status") val executionStatus: String? = null,
    @field:Json(name = "conversation_url") val conversationUrl: String? = null,
    @field:Json(name = "acp_server") val acpServer: String? = null
)

/** AppConversationUpdateRequest */
@JsonClass(generateAdapter = true)
data class UpdateConversationRequest(
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "public") val public: Boolean? = null,
    @field:Json(name = "selected_repository") val selectedRepository: String? = null,
    @field:Json(name = "selected_branch") val selectedBranch: String? = null,
    @field:Json(name = "git_provider") val gitProvider: String? = null
)

/** AppSendMessageResponse */
@JsonClass(generateAdapter = true)
data class SendMessageResponseDto(
    @field:Json(name = "success") val success: Boolean = false,
    @field:Json(name = "sandbox_status") val sandboxStatus: String? = null,
    @field:Json(name = "message") val message: String? = null
)

/** PendingMessageResponse */
@JsonClass(generateAdapter = true)
data class PendingMessageResponseDto(
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "queued") val queued: Boolean = false,
    @field:Json(name = "position") val position: Int = 0
)

@JsonClass(generateAdapter = true)
data class SuccessDto(@field:Json(name = "success") val success: Boolean = true)

/** Generic page wrapper: AppConversationPage, EventPage, SandboxPage, RepositoryPage, ... */
@JsonClass(generateAdapter = true)
data class PageDto<T>(
    @field:Json(name = "items") val items: List<T> = emptyList(),
    @field:Json(name = "next_page_id") val nextPageId: String? = null
)

// ---------------------------------------------------------------------------
// Events
// ---------------------------------------------------------------------------

/**
 * Flattened union of every `Event` subtype (discriminated by `kind`).
 * Fields that belong to another subtype simply deserialize as null.
 * Nested tool payloads (`action`, `observation`, `llm_message`) are kept as
 * raw maps so that the ~60 tool variants can be rendered without 60 adapters.
 */
@JsonClass(generateAdapter = true)
data class EventDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "kind") val kind: String = "",
    @field:Json(name = "timestamp") val timestamp: String? = null,
    @field:Json(name = "source") val source: String? = null,
    @field:Json(name = "parent_id") val parentId: String? = null,
    // ActionEvent
    @field:Json(name = "thought") val thought: List<TextContentDto>? = null,
    @field:Json(name = "reasoning_content") val reasoningContent: String? = null,
    @field:Json(name = "action") val action: Map<String, Any?>? = null,
    @field:Json(name = "tool_name") val toolName: String? = null,
    @field:Json(name = "tool_call_id") val toolCallId: String? = null,
    @field:Json(name = "tool_call") val toolCall: Map<String, Any?>? = null,
    @field:Json(name = "security_risk") val securityRisk: String? = null,
    @field:Json(name = "summary") val summary: String? = null,
    // ObservationEvent / UserRejectObservation / AgentErrorEvent
    @field:Json(name = "observation") val observation: Map<String, Any?>? = null,
    @field:Json(name = "action_id") val actionId: String? = null,
    @field:Json(name = "rejection_reason") val rejectionReason: String? = null,
    @field:Json(name = "error") val error: String? = null,
    // MessageEvent
    @field:Json(name = "llm_message") val llmMessage: Map<String, Any?>? = null,
    @field:Json(name = "activated_skills") val activatedSkills: List<String>? = null,
    // ServerErrorEvent / ConversationErrorEvent
    @field:Json(name = "code") val code: String? = null,
    @field:Json(name = "detail") val detail: String? = null,
    // ConversationStateUpdateEvent
    @field:Json(name = "key") val key: String? = null,
    @field:Json(name = "value") val value: Any? = null,
    // SystemPromptEvent
    @field:Json(name = "system_prompt") val systemPrompt: TextContentDto? = null,
    // StreamingDeltaEvent
    @field:Json(name = "content") val content: Any? = null,
    // Condensation
    @field:Json(name = "forgotten_event_ids") val forgottenEventIds: List<String>? = null,
    // ACPToolCallEvent (conversations run by ACP agents such as Claude Code / Codex)
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "status") val status: String? = null,
    @field:Json(name = "tool_kind") val toolKind: String? = null,
    @field:Json(name = "raw_input") val rawInput: Map<String, Any?>? = null,
    @field:Json(name = "raw_output") val rawOutput: Any? = null,
    @field:Json(name = "is_error") val isError: Boolean? = null
)

// ---------------------------------------------------------------------------
// Sandboxes
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class ExposedUrlDto(
    @field:Json(name = "name") val name: String = "",
    @field:Json(name = "url") val url: String = "",
    @field:Json(name = "port") val port: Int = 0
)

/** SandboxInfo */
@JsonClass(generateAdapter = true)
data class SandboxDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "sandbox_spec_id") val sandboxSpecId: String? = null,
    @field:Json(name = "status") val status: String? = null,
    @field:Json(name = "status_detail") val statusDetail: String? = null,
    @field:Json(name = "exposed_urls") val exposedUrls: List<ExposedUrlDto>? = null,
    @field:Json(name = "created_at") val createdAt: String? = null
)

/** SandboxSpecInfo */
@JsonClass(generateAdapter = true)
data class SandboxSpecDto(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "working_dir") val workingDir: String? = null,
    @field:Json(name = "created_at") val createdAt: String? = null
)

// ---------------------------------------------------------------------------
// Git
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class RepositoryDto(
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "full_name") val fullName: String = "",
    @field:Json(name = "git_provider") val gitProvider: String? = null,
    @field:Json(name = "is_public") val isPublic: Boolean = false,
    @field:Json(name = "stargazers_count") val stargazersCount: Int? = null,
    @field:Json(name = "pushed_at") val pushedAt: String? = null,
    @field:Json(name = "main_branch") val mainBranch: String? = null,
    @field:Json(name = "owner_type") val ownerType: String? = null
)

@JsonClass(generateAdapter = true)
data class BranchDto(
    @field:Json(name = "name") val name: String = "",
    @field:Json(name = "commit_sha") val commitSha: String? = null,
    @field:Json(name = "protected") val protected: Boolean = false,
    @field:Json(name = "last_push_date") val lastPushDate: String? = null
)

@JsonClass(generateAdapter = true)
data class SuggestedTaskDto(
    @field:Json(name = "git_provider") val gitProvider: String? = null,
    @field:Json(name = "task_type") val taskType: String? = null,
    @field:Json(name = "repo") val repo: String = "",
    @field:Json(name = "issue_number") val issueNumber: Int = 0,
    @field:Json(name = "title") val title: String = ""
)

/** Item of GET /api/v1/app-conversations/{id}/git/changes (proxied runtime response). */
@JsonClass(generateAdapter = true)
data class GitChangeDto(
    @field:Json(name = "path") val path: String = "",
    @field:Json(name = "status") val status: String = ""
)

/** GET /api/v1/app-conversations/{id}/git/diff (proxied runtime response). */
@JsonClass(generateAdapter = true)
data class GitDiffDto(
    @field:Json(name = "original") val original: String? = null,
    @field:Json(name = "modified") val modified: String? = null
)

// ---------------------------------------------------------------------------
// User / account
// ---------------------------------------------------------------------------

/** SaasUserInfo (subset) */
@JsonClass(generateAdapter = true)
data class UserInfoDto(
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "email") val email: String? = null,
    @field:Json(name = "email_verified") val emailVerified: Boolean? = null,
    @field:Json(name = "git_user_name") val gitUserName: String? = null,
    @field:Json(name = "org_id") val orgId: String? = null,
    @field:Json(name = "org_name") val orgName: String? = null,
    @field:Json(name = "role") val role: String? = null,
    @field:Json(name = "permissions") val permissions: List<String>? = null,
    @field:Json(name = "max_budget_per_task") val maxBudgetPerTask: Double? = null,
    @field:Json(name = "v1_enabled") val v1Enabled: Boolean? = null,
    @field:Json(name = "default_sandbox_spec_id") val defaultSandboxSpecId: String? = null,
    @field:Json(name = "active_agent_profile_id") val activeAgentProfileId: String? = null,
    @field:Json(name = "language") val language: String? = null
)

/** CurrentApiKeyResponse */
@JsonClass(generateAdapter = true)
data class CurrentApiKeyDto(
    @field:Json(name = "id") val id: Long? = null,
    @field:Json(name = "name") val name: String? = null,
    @field:Json(name = "org_id") val orgId: String? = null,
    @field:Json(name = "user_id") val userId: String? = null,
    @field:Json(name = "auth_type") val authType: String? = null
)

/** GetCreditsResponse */
@JsonClass(generateAdapter = true)
data class CreditsDto(@field:Json(name = "credits") val credits: String? = null)

/** UserGitInfo */
@JsonClass(generateAdapter = true)
data class GitUserDto(
    @field:Json(name = "login") val login: String? = null,
    @field:Json(name = "name") val name: String? = null,
    @field:Json(name = "avatar_url") val avatarUrl: String? = null
)

/** LLM model entry from GET /api/v1/config/models/search */
@JsonClass(generateAdapter = true)
data class LlmModelDto(
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "name") val name: String? = null,
    @field:Json(name = "provider") val provider: String? = null,
    @field:Json(name = "verified") val verified: Boolean? = null
)

// ---------------------------------------------------------------------------
// Secrets / settings / skills / misc
// ---------------------------------------------------------------------------

/** CustomSecretWithoutValue */
@JsonClass(generateAdapter = true)
data class SecretDto(
    @field:Json(name = "name") val name: String = "",
    @field:Json(name = "description") val description: String? = null
)

/** CustomSecretCreate */
@JsonClass(generateAdapter = true)
data class CreateSecretRequest(
    @field:Json(name = "name") val name: String,
    @field:Json(name = "value") val value: String,
    @field:Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class SecretNamesDto(@field:Json(name = "secrets") val secrets: List<String> = emptyList())

/** POSTProviderModel: provider tokens keyed by provider type. */
@JsonClass(generateAdapter = true)
data class ProviderTokenDto(
    @field:Json(name = "token") val token: String? = null,
    @field:Json(name = "host") val host: String? = null,
    @field:Json(name = "user_id") val userId: String? = null
)

@JsonClass(generateAdapter = true)
data class SetProviderTokensRequest(
    @field:Json(name = "provider_tokens") val providerTokens: Map<String, ProviderTokenDto>
)

/** Entry of GET /api/v1/settings/profiles */
@JsonClass(generateAdapter = true)
data class LlmProfileDto(
    @field:Json(name = "name") val name: String = "",
    @field:Json(name = "model") val model: String? = null,
    @field:Json(name = "base_url") val baseUrl: String? = null,
    @field:Json(name = "api_key_set") val apiKeySet: Boolean? = null,
    @field:Json(name = "provider_connection_id") val providerConnectionId: String? = null
)

@JsonClass(generateAdapter = true)
data class LlmProfileListDto(
    @field:Json(name = "profiles") val profiles: List<LlmProfileDto> = emptyList(),
    @field:Json(name = "active_profile") val activeProfile: String? = null
)

@JsonClass(generateAdapter = true)
data class RenameProfileRequest(@field:Json(name = "new_name") val newName: String)

/** SwitchProfileRequest */
@JsonClass(generateAdapter = true)
data class SwitchProfileRequest(@field:Json(name = "profile_name") val profileName: String)

/** SwitchAcpModelRequest */
@JsonClass(generateAdapter = true)
data class SwitchAcpModelRequest(@field:Json(name = "model_id") val modelId: String)

/** Skill entry (GET /api/v1/skills/search and GET /app-conversations/{id}/skills) */
@JsonClass(generateAdapter = true)
data class SkillDto(
    @field:Json(name = "name") val name: String = "",
    @field:Json(name = "type") val type: String? = null,
    @field:Json(name = "source") val source: String? = null,
    @field:Json(name = "triggers") val triggers: List<String>? = null,
    @field:Json(name = "content") val content: String? = null
)

@JsonClass(generateAdapter = true)
data class SkillListDto(@field:Json(name = "skills") val skills: List<SkillDto> = emptyList())

@JsonClass(generateAdapter = true)
data class HookListDto(@field:Json(name = "hooks") val hooks: List<Any?> = emptyList())

/** GET /api/v1/config/providers/search item */
@JsonClass(generateAdapter = true)
data class LlmProviderDto(
    @field:Json(name = "name") val name: String = "",
    @field:Json(name = "verified") val verified: Boolean? = null
)

/** GET /api/v1/users/git-organizations */
@JsonClass(generateAdapter = true)
data class GitOrganizationsDto(
    @field:Json(name = "provider") val provider: String? = null,
    @field:Json(name = "organizations") val organizations: List<Any?> = emptyList()
)

/** GET /api/v1/web-client/config */
@JsonClass(generateAdapter = true)
data class WebClientConfigDto(
    @field:Json(name = "app_mode") val appMode: String? = null,
    @field:Json(name = "feature_flags") val featureFlags: Map<String, Any?>? = null
)

/** Error envelope. FastAPI returns {"detail": ...}; the auth layer returns {"error": "NoCredentialsError"}. */
@JsonClass(generateAdapter = true)
data class ApiErrorDto(
    @field:Json(name = "error") val error: String? = null,
    @field:Json(name = "detail") val detail: Any? = null,
    @field:Json(name = "message") val message: String? = null
)
