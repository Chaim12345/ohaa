package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ---------------------------------------------------------------------------
// Request payloads
// ---------------------------------------------------------------------------

/**
 * One item of a `Message.content` / `thought` array. The union includes
 * `ImageContent`, which has no `text`, so both fields must be optional or the
 * whole event page fails to parse.
 */
@JsonClass(generateAdapter = true)
data class TextContentDto(
    @param:Json(name = "type") val type: String? = "text",
    @param:Json(name = "text") val text: String? = null,
)

@JsonClass(generateAdapter = true)
data class SendMessagePayload(
    @param:Json(name = "role") val role: String = "user",
    @param:Json(name = "content") val content: List<TextContentDto>,
    @param:Json(name = "run") val run: Boolean = true,
)

/** Body for `POST /api/v1/app-conversations`. */
@JsonClass(generateAdapter = true)
data class StartConversationRequest(
    @param:Json(name = "initial_message") val initialMessage: SendMessagePayload,
    @param:Json(name = "selected_repository") val selectedRepository: String? = null,
    @param:Json(name = "selected_branch") val selectedBranch: String? = null,
    @param:Json(name = "llm_model") val llmModel: String? = null,
    @param:Json(name = "llm_profile") val llmProfile: String? = null,
    @param:Json(name = "agent_type") val agentType: String? = null,
    @param:Json(name = "agent_profile_id") val agentProfileId: String? = null,
    @param:Json(name = "git_provider") val gitProvider: String? = null,
    @param:Json(name = "title") val title: String? = null,
)

// ---------------------------------------------------------------------------
// Conversation wire models
// ---------------------------------------------------------------------------

/**
 * One item of `GET /api/v1/app-conversations/search` (schema `AppConversation`).
 *
 * The server exposes [sandboxStatus] and [executionStatus] — there is no
 * `status` field. `execution_status` is null until the sandbox is RUNNING.
 */
@JsonClass(generateAdapter = true)
data class ConversationResponse(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "selected_repository") val selectedRepository: String? = null,
    @param:Json(name = "selected_branch") val selectedBranch: String? = null,
    @param:Json(name = "sandbox_status") val sandboxStatus: String? = null,
    @param:Json(name = "execution_status") val executionStatus: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "updated_at") val updatedAt: String? = null,
    @param:Json(name = "conversation_ui_url") val conversationUiUrl: String? = null,
    @param:Json(name = "conversation_url") val conversationUrl: String? = null,
    @param:Json(name = "session_api_key") val sessionApiKey: String? = null,
    @param:Json(name = "sandbox_id") val sandboxId: String? = null,
    @param:Json(name = "agent_kind") val agentKind: String? = null,
    @param:Json(name = "llm_model") val llmModel: String? = null,
    @param:Json(name = "acp_server") val acpServer: String? = null,
    @param:Json(name = "acp_supports_runtime_model_switch") val acpSupportsRuntimeModelSwitch: Boolean? = null,
    @param:Json(name = "acp_available_models") val acpAvailableModels: List<AcpModelInfoDto>? = null,
) {
    val resolvedId: String
        get() = id ?: "unknown"
}

// ---------------------------------------------------------------------------
// Model / provider / profile catalog
// ---------------------------------------------------------------------------

/** One model the server offers (cloud `config/models/search`). */
@JsonClass(generateAdapter = true)
data class LlmModelDto(
    @param:Json(name = "provider") val provider: String? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "verified") val verified: Boolean = false,
    @param:Json(name = "free") val free: Boolean = false,
    @param:Json(name = "default") val default: Boolean = false,
    @param:Json(name = "hidden") val hidden: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class LlmModelPageResponse(
    @param:Json(name = "items") val items: List<LlmModelDto>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null,
)

@JsonClass(generateAdapter = true)
data class ProviderDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "verified") val verified: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class ProviderPageResponse(
    @param:Json(name = "items") val items: List<ProviderDto>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null,
)

/** Saved LLM profile summary (`GET /api/v1/settings/profiles`). */
@JsonClass(generateAdapter = true)
data class ProfileInfoDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "model") val model: String? = null,
    @param:Json(name = "base_url") val baseUrl: String? = null,
    @param:Json(name = "api_key_set") val apiKeySet: Boolean = false,
    @param:Json(name = "provider_connection_id") val providerConnectionId: String? = null,
)

@JsonClass(generateAdapter = true)
data class ProfileListResponse(
    @param:Json(name = "profiles") val profiles: List<ProfileInfoDto>? = null,
    @param:Json(name = "active_profile") val activeProfile: String? = null,
)

/** One ACP model the session offers (`acp_available_models` / `acp/models`). */
@JsonClass(generateAdapter = true)
data class AcpModelInfoDto(
    @param:Json(name = "model_id") val modelId: String? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "description") val description: String? = null,
)

/** One reasoning-effort level an ACP session offers. */
@JsonClass(generateAdapter = true)
data class AcpEffortOptionDto(
    @param:Json(name = "value") val value: String? = null,
    @param:Json(name = "name") val name: String? = null,
)

@JsonClass(generateAdapter = true)
data class SwitchAcpModelRequest(
    @param:Json(name = "model") val model: String,
)

@JsonClass(generateAdapter = true)
data class SwitchAcpEffortRequest(
    @param:Json(name = "effort") val effort: String,
)

@JsonClass(generateAdapter = true)
data class SwitchProfileRequest(
    @param:Json(name = "profile_name") val profileName: String,
)

/** Body for the agent-server `switch_llm` endpoint. */
@JsonClass(generateAdapter = true)
data class SwitchLlmRequest(
    @param:Json(name = "model") val model: String,
    @param:Json(name = "api_key") val apiKey: String? = null,
    @param:Json(name = "base_url") val baseUrl: String? = null,
)

/** Body for `POST /api/v1/app-conversations/{id}/send-message`. */
@JsonClass(generateAdapter = true)
data class PendingMessageRequest(
    @param:Json(name = "role") val role: String = "user",
    @param:Json(name = "content") val content: List<TextContentDto>,
)

// ---------------------------------------------------------------------------
// Agent-server (runtime) models — reached via `conversation_url` + X-Session-API-Key
// ---------------------------------------------------------------------------

/** `POST /api/bash/execute_bash_command` request. */
@JsonClass(generateAdapter = true)
data class ExecuteBashRequest(
    @param:Json(name = "command") val command: String,
    @param:Json(name = "cwd") val cwd: String? = null,
    @param:Json(name = "timeout") val timeout: Int = 300,
)

/** Result of an executed bash command. */
@JsonClass(generateAdapter = true)
data class BashOutputDto(
    @param:Json(name = "command_id") val commandId: String? = null,
    @param:Json(name = "stdout") val stdout: String? = null,
    @param:Json(name = "stderr") val stderr: String? = null,
    @param:Json(name = "exit_code") val exitCode: Int? = null,
    @param:Json(name = "kind") val kind: String? = null,
)

/** A started (still running) bash command. */
@JsonClass(generateAdapter = true)
data class BashCommandDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "command") val command: String? = null,
    @param:Json(name = "cwd") val cwd: String? = null,
    @param:Json(name = "kind") val kind: String? = null,
)

@JsonClass(generateAdapter = true)
data class HomeResponseDto(
    @param:Json(name = "home") val home: String? = null,
)

/** One entry in the sandbox file browser. */
@JsonClass(generateAdapter = true)
data class FileEntryDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "path") val path: String? = null,
    @param:Json(name = "is_directory") val isDirectory: Boolean = false,
    @param:Json(name = "size") val size: Long = 0,
    @param:Json(name = "modified_time") val modifiedTime: String? = null,
)

@JsonClass(generateAdapter = true)
data class SubdirectoryPageDto(
    @param:Json(name = "items") val items: List<FileEntryDto>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null,
)

@JsonClass(generateAdapter = true)
data class FileUploadResultDto(
    @param:Json(name = "success") val success: Boolean = true,
    @param:Json(name = "file_size") val fileSize: Long = 0,
)

/** One commit from the agent-server git log. */
@JsonClass(generateAdapter = true)
data class GitCommitDto(
    @param:Json(name = "sha") val sha: String? = null,
    @param:Json(name = "short_sha") val shortSha: String? = null,
    @param:Json(name = "subject") val subject: String? = null,
    @param:Json(name = "author") val author: String? = null,
    @param:Json(name = "timestamp") val timestamp: String? = null,
)

@JsonClass(generateAdapter = true)
data class GitCommitsPageDto(
    @param:Json(name = "commits") val commits: List<GitCommitDto>? = null,
    @param:Json(name = "has_more") val hasMore: Boolean = false,
)

/** Original/modified pair returned by `GET /api/git/diff`. */
@JsonClass(generateAdapter = true)
data class GitDiffDto(
    @param:Json(name = "original") val original: String? = null,
    @param:Json(name = "modified") val modified: String? = null,
)

/** Token accounting for the conversation (`metrics.accumulated_token_usage`). */
@JsonClass(generateAdapter = true)
data class TokenUsageDto(
    @param:Json(name = "model") val model: String? = null,
    @param:Json(name = "prompt_tokens") val promptTokens: Long = 0,
    @param:Json(name = "completion_tokens") val completionTokens: Long = 0,
    @param:Json(name = "cache_read_tokens") val cacheReadTokens: Long = 0,
    @param:Json(name = "cache_write_tokens") val cacheWriteTokens: Long = 0,
    @param:Json(name = "reasoning_tokens") val reasoningTokens: Long = 0,
    @param:Json(name = "context_window") val contextWindow: Long = 0,
    @param:Json(name = "per_turn_token") val perTurnToken: Long = 0,
) {
    val totalTokens: Long get() = promptTokens + completionTokens
}

@JsonClass(generateAdapter = true)
data class MetricsSnapshotDto(
    @param:Json(name = "model_name") val modelName: String? = null,
    @param:Json(name = "accumulated_cost") val accumulatedCost: Double = 0.0,
    @param:Json(name = "max_budget_per_task") val maxBudgetPerTask: Double? = null,
    @param:Json(name = "accumulated_token_usage") val accumulatedTokenUsage: TokenUsageDto? = null,
)

/** Runtime conversation state (`GET {conversation_url}/api/conversations/{id}`). */
@JsonClass(generateAdapter = true)
data class ConversationInfoDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "execution_status") val executionStatus: String? = null,
    @param:Json(name = "current_model_id") val currentModelId: String? = null,
    @param:Json(name = "available_models") val availableModels: List<AcpModelInfoDto>? = null,
    @param:Json(name = "current_effort") val currentEffort: String? = null,
    @param:Json(name = "available_efforts") val availableEfforts: List<AcpEffortOptionDto>? = null,
    @param:Json(name = "metrics") val metrics: MetricsSnapshotDto? = null,
    @param:Json(name = "max_iterations") val maxIterations: Int? = null,
    @param:Json(name = "supports_runtime_model_switch") val supportsRuntimeModelSwitch: Boolean = false,
)

/** `POST /api/conversations/{id}/ask_agent` request/response. */
@JsonClass(generateAdapter = true)
data class AskAgentRequest(
    @param:Json(name = "question") val question: String,
)

@JsonClass(generateAdapter = true)
data class AskAgentResponseDto(
    @param:Json(name = "response") val response: String? = null,
)

// ---------------------------------------------------------------------------
// Cloud resources: sandboxes, specs, skills, secrets, git, users
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class SandboxInfoDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "sandbox_spec_id") val sandboxSpecId: String? = null,
    @param:Json(name = "session_api_key") val sessionApiKey: String? = null,
    @param:Json(name = "exposed_urls") val exposedUrls: List<String>? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "status_detail") val statusDetail: String? = null,
)

@JsonClass(generateAdapter = true)
data class SandboxPageResponse(
    @param:Json(name = "items") val items: List<SandboxInfoDto>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null,
)

@JsonClass(generateAdapter = true)
data class SandboxSpecInfoDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "command") val command: String? = null,
    @param:Json(name = "working_dir") val workingDir: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class SandboxSpecPageResponse(
    @param:Json(name = "items") val items: List<SandboxSpecInfoDto>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null,
)

/** One skill in the cloud catalog (`GET /api/v1/skills/search`). */
@JsonClass(generateAdapter = true)
data class SkillInfoDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "type") val type: String? = null,
    @param:Json(name = "source") val source: String? = null,
    @param:Json(name = "description") val description: String? = null,
    @param:Json(name = "triggers") val triggers: List<String>? = null,
)

@JsonClass(generateAdapter = true)
data class SkillPageResponse(
    @param:Json(name = "items") val items: List<SkillInfoDto>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null,
)

/** A user secret; the value is never returned by the API. */
@JsonClass(generateAdapter = true)
data class SecretDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "description") val description: String? = null,
    @param:Json(name = "scope") val scope: String? = null,
    @param:Json(name = "system_managed") val systemManaged: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class SecretPageResponse(
    @param:Json(name = "items") val items: List<SecretDto>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null,
)

@JsonClass(generateAdapter = true)
data class CreateSecretRequest(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "description") val description: String? = null,
    @param:Json(name = "value") val value: String,
    @param:Json(name = "scope") val scope: String = "user",
)

@JsonClass(generateAdapter = true)
data class UpdateSecretRequest(
    @param:Json(name = "description") val description: String? = null,
    @param:Json(name = "value") val value: String? = null,
)

// ---- git ----------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class RepositoryDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "full_name") val fullName: String? = null,
    @param:Json(name = "git_provider") val gitProvider: String? = null,
    @param:Json(name = "main_branch") val mainBranch: String? = null,
    @param:Json(name = "is_public") val isPublic: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class RepositoryPageResponse(
    @param:Json(name = "items") val items: List<RepositoryDto>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null,
)

@JsonClass(generateAdapter = true)
data class BranchDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "commit_sha") val commitSha: String? = null,
    @param:Json(name = "protected") val protected: Boolean = false,
    @param:Json(name = "last_push_date") val lastPushDate: String? = null,
)

@JsonClass(generateAdapter = true)
data class BranchPageResponse(
    @param:Json(name = "items") val items: List<BranchDto>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null,
)

@JsonClass(generateAdapter = true)
data class SuggestedTaskDto(
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "repo") val repo: String? = null,
    @param:Json(name = "git_provider") val gitProvider: String? = null,
    @param:Json(name = "task_type") val taskType: String? = null,
    @param:Json(name = "issue_number") val issueNumber: Int? = null,
)

@JsonClass(generateAdapter = true)
data class SuggestedTaskPageResponse(
    @param:Json(name = "items") val items: List<SuggestedTaskDto>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null,
)

@JsonClass(generateAdapter = true)
data class UserGitInfoDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "login") val login: String? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "email") val email: String? = null,
    @param:Json(name = "company") val company: String? = null,
    @param:Json(name = "avatar_url") val avatarUrl: String? = null,
)

/** Update body for `PATCH /api/v1/app-conversations/{id}`. */
@JsonClass(generateAdapter = true)
data class ConversationUpdateRequest(
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "selected_branch") val selectedBranch: String? = null,
    @param:Json(name = "selected_repository") val selectedRepository: String? = null,
)

/** `GET /api/acp/models` response — ACP model discovery payload. */
@JsonClass(generateAdapter = true)
data class AcpModelDiscoveryDto(
    @param:Json(name = "models") val models: List<AcpModelInfoDto>? = null,
    @param:Json(name = "current_model_id") val currentModelId: String? = null,
)

/** One agent profile (OpenHands or ACP) from `GET /api/agent-profiles`. */
@JsonClass(generateAdapter = true)
data class AgentProfileInfoDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "agent_kind") val agentKind: String? = null,
    @param:Json(name = "revision") val revision: Int? = null,
    @param:Json(name = "llm_profile_ref") val llmProfileRef: String? = null,
    @param:Json(name = "acp_server") val acpServer: String? = null,
    @param:Json(name = "acp_model") val acpModel: String? = null,
    @param:Json(name = "acp_effort") val acpEffort: String? = null,
)

@JsonClass(generateAdapter = true)
data class AgentProfileListDto(
    @param:Json(name = "profiles") val profiles: List<AgentProfileInfoDto>? = null,
    @param:Json(name = "active_agent_profile_id") val activeAgentProfileId: String? = null,
)

/** `GET /server_info` — used to show live idle time and the reap threshold. */
@JsonClass(generateAdapter = true)
data class ServerInfoDto(
    @param:Json(name = "idle_time") val idleTime: Double? = null,
    @param:Json(name = "runtime_idle_timeout_seconds") val runtimeIdleTimeoutSeconds: Double? = null,
)

/**
 * Minimal view of a conversation used by the keep-alive worker: the sandbox it
 * runs in plus the two status fields that decide whether it needs a touch or a
 * resume. Deliberately separate from [ConversationResponse] so persistence
 * concerns do not leak into the background service.
 */
data class SandboxTarget(
    val conversationId: String,
    val sandboxId: String?,
    val sandboxStatus: String?,
    val status: ConversationStatus,
    val hasRuntimeAccess: Boolean = false,
)

/** Generic `{"success": true}` body returned by the sandbox lifecycle endpoints. */
@JsonClass(generateAdapter = true)
data class SuccessResponse(
    @param:Json(name = "success") val success: Boolean? = null,
)

/** Page envelope returned by the `/search` endpoints. */
@JsonClass(generateAdapter = true)
data class ConversationPageResponse(
    @param:Json(name = "items") val items: List<ConversationResponse>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null,
)

/**
 * One entry of `GET /api/v1/app-conversations/{id}/git/changes`.
 * `status` is the git porcelain code (ADDED / UPDATED / DELETED / ...).
 */
@JsonClass(generateAdapter = true)
data class GitChangeDto(
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "path") val path: String? = null,
)

/** UI model for one changed file in the workspace. */
data class GitChange(
    val status: String,
    val path: String
)

/**
 * `POST /api/v1/app-conversations` is asynchronous and returns a start task.
 * Poll `GET /api/v1/app-conversations/start-tasks?ids=...` until
 * [appConversationId] is populated.
 */
@JsonClass(generateAdapter = true)
data class StartTaskResponse(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "detail") val detail: String? = null,
    @param:Json(name = "app_conversation_id") val appConversationId: String? = null,
    @param:Json(name = "sandbox_id") val sandboxId: String? = null,
    @param:Json(name = "agent_server_url") val agentServerUrl: String? = null,
)

// ---------------------------------------------------------------------------
// Event wire models
// ---------------------------------------------------------------------------

/**
 * One item of `GET /api/v1/conversation/{id}/events/search` (schema `Event`).
 *
 * Events are a discriminated union on `kind`. Moshi cannot codegen `oneOf`, so
 * this single permissive DTO carries the fields of the shapes we render
 * (MessageEvent / ActionEvent / ObservationEvent). Unknown JSON keys are
 * ignored by Moshi.
 */
@JsonClass(generateAdapter = true)
data class EventResponse(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "timestamp") val timestamp: String? = null,
    @param:Json(name = "source") val source: String? = null,
    @param:Json(name = "kind") val kind: String? = null,
    @param:Json(name = "tool_name") val toolName: String? = null,
    @param:Json(name = "summary") val summary: String? = null,
    @param:Json(name = "thought") val thought: List<TextContentDto>? = null,
    @param:Json(name = "action") val action: ActionDto? = null,
    @param:Json(name = "observation") val observation: ObservationDto? = null,
    @param:Json(name = "llm_message") val llmMessage: MessageDto? = null,
    @param:Json(name = "system_prompt") val systemPrompt: TextContentDto? = null,
)

@JsonClass(generateAdapter = true)
data class MessageDto(
    @param:Json(name = "role") val role: String? = null,
    @param:Json(name = "content") val content: List<TextContentDto>? = null,
)

/** Common tool-call fields across `Action` variants (terminal, file editor, ...). */
@JsonClass(generateAdapter = true)
data class ActionDto(
    @param:Json(name = "kind") val kind: String? = null,
    @param:Json(name = "command") val command: String? = null,
    @param:Json(name = "path") val path: String? = null,
    @param:Json(name = "content") val content: String? = null,
    @param:Json(name = "old_str") val oldStr: String? = null,
    @param:Json(name = "new_str") val newStr: String? = null,
    @param:Json(name = "query") val query: String? = null,
    @param:Json(name = "url") val url: String? = null,
    @param:Json(name = "message") val message: String? = null,
)

/** Common tool-result fields across `Observation` variants. */
@JsonClass(generateAdapter = true)
data class ObservationDto(
    @param:Json(name = "kind") val kind: String? = null,
    @param:Json(name = "content") val content: List<TextContentDto>? = null,
    @param:Json(name = "is_error") val isError: Boolean? = null,
    @param:Json(name = "command") val command: String? = null,
    @param:Json(name = "exit_code") val exitCode: Int? = null,
    @param:Json(name = "stdout") val stdout: String? = null,
    @param:Json(name = "stderr") val stderr: String? = null,
    @param:Json(name = "message") val message: String? = null,
)

@JsonClass(generateAdapter = true)
data class EventPageResponse(
    @param:Json(name = "items") val items: List<EventResponse>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null,
)

// ---------------------------------------------------------------------------
// UI / domain models
// ---------------------------------------------------------------------------

data class OpenHandsConversation(
    val id: String,
    val title: String,
    val status: ConversationStatus,
    val repository: String?,
    val branch: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val conversationUrl: String?,
    val sandboxId: String? = null,
    val sandboxStatus: String? = null,
    val sessionApiKey: String? = null,
    val agentKind: String? = null,
    val llmModel: String? = null,
    val acpServer: String? = null,
    val acpSupportsRuntimeModelSwitch: Boolean = false,
    val acpAvailableModelsJson: String? = null,
    val executionStatus: String? = null,
) {
    /** True when the conversation runs an ACP backend (Claude Code, Codex, ...). */
    val isAcp: Boolean get() = agentKind.equals("acp", ignoreCase = true)

    /** Runtime calls need both a reachable URL and the session key. */
    val hasRuntimeAccess: Boolean
        get() = !conversationUrl.isNullOrBlank() && !sessionApiKey.isNullOrBlank()

    val isSandboxRunning: Boolean
        get() = sandboxStatus.equals("RUNNING", ignoreCase = true)
}

/**
 * Status shown in the UI. Derived from the API's `sandbox_status` +
 * `execution_status` (see [resolve]).
 */
enum class ConversationStatus(val label: String) {
    WAITING_FOR_SANDBOX("Starting Sandbox"),
    PREPARING_REPOSITORY("Preparing Repo"),
    SETTING_UP_SKILLS("Configuring Skills"),
    READY("Ready"),
    RUNNING("Running"),
    AWAITING_CONFIRMATION("Awaiting Confirmation"),
    FINISHED("Finished"),
    PAUSED("Paused"),
    ERROR("Error"),
    UNKNOWN("Unknown");

    companion object {
        /** Re-hydrate a status from its persisted [name]. */
        fun fromName(value: String?): ConversationStatus {
            return entries.firstOrNull { it.name == value } ?: UNKNOWN
        }

        /** Map an `AppConversation` (sandbox_status + execution_status) to a UI status. */
        fun resolve(sandboxStatus: String?, executionStatus: String?): ConversationStatus {
            return when (executionStatus?.trim()?.lowercase()) {
                "running" -> RUNNING
                "waiting_for_confirmation" -> AWAITING_CONFIRMATION
                "paused" -> PAUSED
                "finished" -> FINISHED
                "error", "stuck", "deleting" -> ERROR
                "idle" -> READY
                null, "" -> when (sandboxStatus?.trim()?.uppercase()) {
                    "STARTING" -> WAITING_FOR_SANDBOX
                    "RUNNING" -> READY
                    "PAUSED" -> PAUSED
                    "ERROR", "MISSING" -> ERROR
                    else -> UNKNOWN
                }
                else -> UNKNOWN
            }
        }

        /** Map an `AppConversationStartTaskStatus` to a UI status while a task is starting. */
        fun fromStartTask(status: String?): ConversationStatus {
            return when (status?.trim()?.uppercase()) {
                "WORKING" -> WAITING_FOR_SANDBOX
                "WAITING_FOR_SANDBOX" -> WAITING_FOR_SANDBOX
                "PREPARING_REPOSITORY" -> PREPARING_REPOSITORY
                "RUNNING_SETUP_SCRIPT", "SETTING_UP_GIT_HOOKS", "SETTING_UP_SKILLS" -> SETTING_UP_SKILLS
                "STARTING_CONVERSATION" -> WAITING_FOR_SANDBOX
                "READY" -> READY
                "ERROR" -> ERROR
                else -> WAITING_FOR_SANDBOX
            }
        }
    }
}

data class AgentEvent(
    val id: String,
    val conversationId: String,
    val timestamp: Long,
    val source: String, // "user", "agent", "environment", "system"
    val actionType: String?, // "message", "thought", "bash", "edit", "observation"
    val content: String,
    val detail: String? = null,
    val isError: Boolean = false,
)

data class PromptTemplate(
    val id: String,
    val title: String,
    val category: String,
    val prompt: String,
    val defaultRepo: String? = null,
)
