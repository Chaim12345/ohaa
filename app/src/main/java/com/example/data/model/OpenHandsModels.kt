package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ---------------------------------------------------------------------------
// Wire DTOs — verified against the live OpenHands Cloud OpenAPI contract
// (https://app.all-hands.dev/openapi.json, app v1.59.0) and live responses.
// Field names mirror the spec exactly; untyped proxied endpoints (git changes)
// keep tolerant legacy shapes because the runtime response has no schema.
// ---------------------------------------------------------------------------

// ------------------------------------------------------------------ shared

/** TextContent. `additionalProperties: false`, `text` required. */
@JsonClass(generateAdapter = true)
data class TextContentDto(
    @param:Json(name = "cache_prompt") val cachePrompt: Boolean = false,
    @param:Json(name = "type") val type: String = "text",
    @param:Json(name = "text") val text: String = ""
)

/** ImageContent block (part of message content arrays). */
@JsonClass(generateAdapter = true)
data class ImageContentDto(
    @param:Json(name = "cache_prompt") val cachePrompt: Boolean = false,
    @param:Json(name = "type") val type: String = "image",
    @param:Json(name = "image_urls") val imageUrls: List<String> = emptyList()
)

/** Message — payload of MessageEvent.llm_message and SendMessageRequest bodies. */
@JsonClass(generateAdapter = true)
data class MessageDto(
    @param:Json(name = "role") val role: String? = null,
    @param:Json(name = "content") val content: Any? = null,
    @param:Json(name = "tool_calls") val toolCalls: List<MessageToolCallDto>? = null,
    @param:Json(name = "tool_call_id") val toolCallId: String? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "reasoning_content") val reasoningContent: String? = null
) {
    /** Flattens the content blocks (or plain string) into displayable text. */
    fun contentText(): String? = MessagePayloads.contentToText(content)
}

/** MessageToolCall — the `tool_call` payload on ActionEvent. */
@JsonClass(generateAdapter = true)
data class MessageToolCallDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "responses_item_id") val responsesItemId: String? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "arguments") val arguments: String? = null,
    @param:Json(name = "origin") val origin: String? = null
)

/**
 * Flat renderer helpers for the heterogeneous nested event payloads
 * (observations, actions, llm messages). Kept as Moshi-friendly primitives so
 * the ~60 tool variants decode without one adapter each.
 */
object MessagePayloads {

    /** TextContent list / plain string / arbitrary map → displayable text. */
    fun contentToText(content: Any?): String? = when (content) {
        null -> null
        is String -> content.takeIf { it.isNotBlank() }
        is List<*> -> content.mapNotNull { block ->
            @Suppress("UNCHECKED_CAST")
            (block as? Map<String, Any?>)
                ?.let { extractBlockText(it) }
        }.joinToString("\n").takeIf { it.isNotBlank() }
        else -> null
    }

    private fun extractBlockText(block: Map<String, Any?>): String? = when {
        block["text"] is String -> block["text"] as String
        block["content"] != null -> contentToText(block["content"])
        else -> null
    }

    /** Best-effort extraction of a key from a JSON-arguments string or map. */
    fun argument(arguments: String?, key: String): String? {
        val parsed = parseJsonMap(arguments) ?: return null
        val v = parsed[key] ?: return null
        return when (v) {
            is String -> v
            is Number, is Boolean -> v.toString()
            else -> null
        }
    }

    fun parseJsonMap(raw: String?): Map<String, Any?>? {
        if (raw.isNullOrBlank()) return null
        return try {
            val adapter = com.squareup.moshi.Moshi.Builder()
                .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()
                .adapter<Map<String, Any?>>(
                    com.squareup.moshi.Types.newParameterizedType(
                        Map::class.java, String::class.java, Any::class.java
                    )
                )
            adapter.fromJson(raw)
        } catch (e: Exception) {
            null
        }
    }
}

// --------------------------------------------------------------- conversations

/** AppSendMessageRequest — POST /app-conversations/{id}/send-message. */
@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    @param:Json(name = "role") val role: String = "user",
    @param:Json(name = "content") val content: List<TextContentDto>,
    @param:Json(name = "run") val run: Boolean = true
) {
    companion object {
        fun text(text: String, run: Boolean = true) =
            SendMessageRequest(content = listOf(TextContentDto(text = text)), run = run)
    }
}

/** AppConversationStartRequest — POST /api/v1/app-conversations. */
@JsonClass(generateAdapter = true)
data class StartConversationRequest(
    @param:Json(name = "initial_message") val initialMessage: SendMessageRequest? = null,
    @param:Json(name = "selected_repository") val selectedRepository: String? = null,
    @param:Json(name = "selected_branch") val selectedBranch: String? = null,
    @param:Json(name = "git_provider") val gitProvider: String? = null,
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "llm_model") val llmModel: String? = null,
    @param:Json(name = "agent_type") val agentType: String = "default",
    @param:Json(name = "agent_profile_id") val agentProfileId: String? = null,
    @param:Json(name = "sandbox_id") val sandboxId: String? = null,
    @param:Json(name = "conversation_id") val conversationId: String? = null,
    @param:Json(name = "parent_conversation_id") val parentConversationId: String? = null,
    @param:Json(name = "system_message_suffix") val systemMessageSuffix: String? = null,
    @param:Json(name = "public") val public: Boolean? = null,
    @param:Json(name = "pr_number") val prNumber: List<Int>? = null,
    @param:Json(name = "observability_span_name") val observabilitySpanName: String? = null,
    @param:Json(name = "observability_tags") val observabilityTags: List<String>? = null,
    @param:Json(name = "observability_metadata") val observabilityMetadata: Map<String, String>? = null,
    @param:Json(name = "secrets") val secrets: Map<String, String>? = null
)

/** Legacy name kept for the existing UI/tests. */
typealias CreateConversationRequestDto = StartConversationRequest

/** AppConversationStartTask — returned by POST /app-conversations and polls. */
@JsonClass(generateAdapter = true)
data class StartTaskDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "created_by_user_id") val createdByUserId: String? = null,
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "detail") val detail: String? = null,
    @param:Json(name = "app_conversation_id") val appConversationId: String? = null,
    @param:Json(name = "sandbox_id") val sandboxId: String? = null,
    @param:Json(name = "agent_server_url") val agentServerUrl: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "updated_at") val updatedAt: String? = null
)

/** TokenUsage — nested in MetricsSnapshot. */
@JsonClass(generateAdapter = true)
data class TokenUsageDto(
    @param:Json(name = "model") val model: String? = "",
    @param:Json(name = "prompt_tokens") val promptTokens: Long = 0,
    @param:Json(name = "completion_tokens") val completionTokens: Long = 0,
    @param:Json(name = "cache_read_tokens") val cacheReadTokens: Long = 0,
    @param:Json(name = "cache_write_tokens") val cacheWriteTokens: Long = 0,
    @param:Json(name = "reasoning_tokens") val reasoningTokens: Long = 0,
    @param:Json(name = "context_window") val contextWindow: Long = 0,
    @param:Json(name = "per_turn_token") val perTurnToken: Long = 0,
    @param:Json(name = "response_id") val responseId: String? = ""
)

/** MetricsSnapshot — AppConversation.metrics. */
@JsonClass(generateAdapter = true)
data class MetricsDto(
    @param:Json(name = "model_name") val modelName: String? = "default",
    @param:Json(name = "accumulated_cost") val accumulatedCost: Double = 0.0,
    @param:Json(name = "max_budget_per_task") val maxBudgetPerTask: Double? = null,
    @param:Json(name = "accumulated_token_usage") val accumulatedTokenUsage: TokenUsageDto? = null
)

/**
 * AppConversation — the conversation resource (search + batch get).
 * `cost` is derived from metrics.accumulated_cost; `lastMessage` is a local
 * cache field (the spec has no top-level equivalent).
 */
@JsonClass(generateAdapter = true)
data class AppConversationDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "created_by_user_id") val createdByUserId: String? = null,
    @param:Json(name = "sandbox_id") val sandboxId: String? = null,
    @param:Json(name = "selected_repository") val selectedRepository: String? = null,
    @param:Json(name = "selected_branch") val selectedBranch: String? = null,
    @param:Json(name = "git_provider") val gitProvider: String? = null,
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "trigger") val trigger: String? = null,
    @param:Json(name = "pr_number") val prNumber: List<Int>? = null,
    @param:Json(name = "llm_model") val llmModel: String? = null,
    @param:Json(name = "agent_kind") val agentKind: String? = null,
    @param:Json(name = "metrics") val metrics: MetricsDto? = null,
    @param:Json(name = "parent_conversation_id") val parentConversationId: String? = null,
    @param:Json(name = "sub_conversation_ids") val subConversationIds: List<String>? = null,
    @param:Json(name = "public") val public: Boolean? = null,
    @param:Json(name = "tags") val tags: Map<String, String>? = null,
    @param:Json(name = "launched_agent_profile") val launchedAgentProfile: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "updated_at") val updatedAt: String? = null,
    @param:Json(name = "sandbox_status") val sandboxStatus: String? = null,
    @param:Json(name = "execution_status") val executionStatus: String? = null,
    @param:Json(name = "conversation_url") val conversationUrl: String? = null,
    @param:Json(name = "session_api_key") val sessionApiKey: String? = null,
    @param:Json(name = "acp_server") val acpServer: String? = null
) {
    val resolvedId: String get() = id ?: ""
    val cost: Double get() = metrics?.accumulatedCost ?: 0.0
}

/** Legacy name kept for existing call sites. */
typealias ConversationDto = AppConversationDto

/** AppConversationUpdateRequest — PATCH /app-conversations/{id}. */
@JsonClass(generateAdapter = true)
data class UpdateConversationRequest(
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "public") val public: Boolean? = null,
    @param:Json(name = "selected_repository") val selectedRepository: String? = null,
    @param:Json(name = "selected_branch") val selectedBranch: String? = null,
    @param:Json(name = "git_provider") val gitProvider: String? = null
)

/** AppSendMessageResponse — {success, sandbox_status, message}. */
@JsonClass(generateAdapter = true)
data class SendMessageResponseDto(
    @param:Json(name = "success") val success: Boolean = false,
    @param:Json(name = "sandbox_status") val sandboxStatus: String? = null,
    @param:Json(name = "message") val message: String? = null
)

/** PendingMessageResponse — POST /conversations/{id}/pending-messages → 201. */
@JsonClass(generateAdapter = true)
data class PendingMessageResponseDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "queued") val queued: Boolean = false,
    @param:Json(name = "position") val position: Int = 0
)

@JsonClass(generateAdapter = true)
data class SuccessDto(
    @param:Json(name = "success") val success: Boolean = true,
    @param:Json(name = "message") val message: String? = null
)

/** {message} envelope — secrets CRUD and provider-token endpoints. */
@JsonClass(generateAdapter = true)
data class MessageResponseDto(
    @param:Json(name = "message") val message: String = ""
)

/** Generic page wrapper: AppConversationPage, EventPage, SandboxPage, … */
@JsonClass(generateAdapter = true)
data class PageDto<T>(
    @param:Json(name = "items") val items: List<T> = emptyList(),
    @param:Json(name = "next_page_id") val nextPageId: String? = null
)

/** Legacy alias — the events page used to be parsed under this name. */
typealias EventsPageDto = PageDto<EventDto>

// --------------------------------------------------------------------- events

/**
 * Flattened union of every `Event` subtype (discriminated by `kind`), decoded
 * from live payloads. Fields belonging to another subtype deserialize as null.
 */
@JsonClass(generateAdapter = true)
data class EventDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "kind") val kind: String? = null,
    @param:Json(name = "timestamp") val timestamp: String? = null,
    @param:Json(name = "source") val source: String? = null,
    @param:Json(name = "parent_id") val parentId: String? = null,
    // ActionEvent
    @param:Json(name = "thought") val thought: Any? = null,
    @param:Json(name = "reasoning_content") val reasoningContent: String? = null,
    @param:Json(name = "action") val action: Any? = null,
    @param:Json(name = "tool_name") val toolName: String? = null,
    @param:Json(name = "tool_call_id") val toolCallId: String? = null,
    @param:Json(name = "tool_call") val toolCall: MessageToolCallDto? = null,
    @param:Json(name = "llm_response_id") val llmResponseId: String? = null,
    @param:Json(name = "security_risk") val securityRisk: String? = null,
    @param:Json(name = "summary") val summary: String? = null,
    // ObservationEvent / UserRejectObservation / AgentErrorEvent
    @param:Json(name = "observation") val observation: Any? = null,
    @param:Json(name = "action_id") val actionId: String? = null,
    @param:Json(name = "rejection_reason") val rejectionReason: String? = null,
    @param:Json(name = "error") val error: String? = null,
    // MessageEvent
    @param:Json(name = "llm_message") val llmMessage: MessageDto? = null,
    @param:Json(name = "activated_skills") val activatedSkills: List<String>? = null,
    @param:Json(name = "sender") val sender: String? = null,
    // ServerErrorEvent / ConversationErrorEvent
    @param:Json(name = "code") val code: String? = null,
    @param:Json(name = "detail") val detail: String? = null,
    // ConversationStateUpdateEvent
    @param:Json(name = "key") val key: String? = null,
    @param:Json(name = "value") val value: Any? = null,
    // SystemPromptEvent
    @param:Json(name = "system_prompt") val systemPrompt: TextContentDto? = null,
    // StreamingDeltaEvent / ACPToolCallEvent content
    @param:Json(name = "content") val content: Any? = null,
    // Condensation
    @param:Json(name = "forgotten_event_ids") val forgottenEventIds: List<String>? = null,
    // ACPToolCallEvent (Claude Code / Codex / Gemini CLI conversations)
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "tool_kind") val toolKind: String? = null,
    @param:Json(name = "raw_input") val rawInput: Any? = null,
    @param:Json(name = "raw_output") val rawOutput: Any? = null,
    @param:Json(name = "is_error") val isError: Boolean? = null,
    // Legacy flat payloads (older runtimes) — harmless if absent
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "args") val args: Map<String, Any?>? = null
) {
    val thoughtText: String? get() = MessagePayloads.contentToText(thought)
    val observationText: String? get() = MessagePayloads.contentToText(observation)
}

// ------------------------------------------------------------------- sandboxes

@JsonClass(generateAdapter = true)
data class ExposedUrlDto(
    @param:Json(name = "name") val name: String = "",
    @param:Json(name = "url") val url: String = "",
    @param:Json(name = "port") val port: Int = 0
)

/** SandboxInfo. */
@JsonClass(generateAdapter = true)
data class SandboxDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "created_by_user_id") val createdByUserId: String? = null,
    @param:Json(name = "sandbox_spec_id") val sandboxSpecId: String? = null,
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "session_api_key") val sessionApiKey: String? = null,
    @param:Json(name = "exposed_urls") val exposedUrls: List<ExposedUrlDto>? = null,
    @param:Json(name = "created_at") val createdAt: String? = null
)

/** SandboxSpecInfo. */
@JsonClass(generateAdapter = true)
data class SandboxSpecDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "command") val command: List<String>? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "initial_env") val initialEnv: Map<String, String>? = null,
    @param:Json(name = "working_dir") val workingDir: String? = null
)

// ------------------------------------------------------------------------ git

/** Repository — item of GET /api/v1/git/repositories/search. */
@JsonClass(generateAdapter = true)
data class RepositoryDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "full_name") val fullName: String = "",
    @param:Json(name = "git_provider") val gitProvider: String? = null,
    @param:Json(name = "is_public") val isPublic: Boolean = false,
    @param:Json(name = "stargazers_count") val stargazersCount: Int? = null,
    @param:Json(name = "pushed_at") val pushedAt: String? = null,
    @param:Json(name = "owner_type") val ownerType: String? = null,
    @param:Json(name = "main_branch") val mainBranch: String? = null
) {
    /** Legacy UI field: default branch of the repository. */
    val branch: String? get() = mainBranch
    val displayName: String get() = fullName.ifBlank { id ?: "unknown repo" }
}

/** Branch — item of GET /api/v1/git/branches/search. */
@JsonClass(generateAdapter = true)
data class BranchDto(
    @param:Json(name = "name") val name: String = "",
    @param:Json(name = "commit_sha") val commitSha: String? = null,
    @param:Json(name = "protected") val protected: Boolean = false,
    @param:Json(name = "last_push_date") val lastPushDate: String? = null
)

/** SuggestedTask — item of GET /api/v1/git/suggested-tasks/search. */
@JsonClass(generateAdapter = true)
data class SuggestedTaskDto(
    @param:Json(name = "git_provider") val gitProvider: String? = null,
    @param:Json(name = "task_type") val taskType: String? = null,
    @param:Json(name = "repo") val repo: String = "",
    @param:Json(name = "issue_number") val issueNumber: Int = 0,
    @param:Json(name = "title") val title: String = ""
)

/**
 * Item of GET /app-conversations/{id}/git/changes. The runtime response has no
 * OpenAPI schema; both the spec-era {path, status} and the legacy
 * {file, status, insertions, deletions} shapes are accepted.
 */
@JsonClass(generateAdapter = true)
data class GitChangeDto(
    @param:Json(name = "path") val path: String? = null,
    @param:Json(name = "file") val file: String? = null,
    @param:Json(name = "status") val status: String = "",
    @param:Json(name = "insertions") val insertions: Int = 0,
    @param:Json(name = "deletions") val deletions: Int = 0
) {
    val displayName: String get() = file ?: path ?: ""
}

/**
 * GET /app-conversations/{id}/git/diff — untyped proxied runtime response.
 * Tolerates {original, modified}, {diff}, and file-list variants.
 */
@JsonClass(generateAdapter = true)
data class GitDiffDto(
    @param:Json(name = "original") val original: String? = null,
    @param:Json(name = "modified") val modified: String? = null,
    @param:Json(name = "diff") val diff: String? = null,
    @param:Json(name = "files") val files: List<GitChangeDto>? = null
) {
    /** Best unified-diff text from whichever fields the runtime returned. */
    val diffText: String?
        get() = diff ?: buildString {
            if (original != null) appendLine(original)
            if (modified != null) append(modified)
        }.takeIf { it.isNotBlank() }
}

// ------------------------------------------------------------- user / account

/** SaasUserInfo (subset of the 30+ live fields). */
@JsonClass(generateAdapter = true)
data class UserInfoDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "email") val email: String? = null,
    @param:Json(name = "email_verified") val emailVerified: Boolean? = null,
    @param:Json(name = "git_user_name") val gitUserName: String? = null,
    @param:Json(name = "git_user_email") val gitUserEmail: String? = null,
    @param:Json(name = "org_id") val orgId: String? = null,
    @param:Json(name = "org_name") val orgName: String? = null,
    @param:Json(name = "role") val role: String? = null,
    @param:Json(name = "permissions") val permissions: List<String>? = null,
    @param:Json(name = "max_budget_per_task") val maxBudgetPerTask: Double? = null,
    @param:Json(name = "v1_enabled") val v1Enabled: Boolean? = null,
    @param:Json(name = "default_sandbox_spec_id") val defaultSandboxSpecId: String? = null,
    @param:Json(name = "active_agent_profile_id") val activeAgentProfileId: String? = null,
    @param:Json(name = "llm_model") val llmModel: String? = null,
    @param:Json(name = "language") val language: String? = null
)

/** CurrentApiKeyResponse — GET /api/keys/current. */
@JsonClass(generateAdapter = true)
data class CurrentApiKeyDto(
    @param:Json(name = "id") val id: Long? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "org_id") val orgId: String? = null,
    @param:Json(name = "bound_org_id") val boundOrgId: String? = null,
    @param:Json(name = "user_id") val userId: String? = null,
    @param:Json(name = "auth_type") val authType: String? = null
)

/** GetCreditsResponse — credits is a numeric string per the live spec. */
@JsonClass(generateAdapter = true)
data class CreditsDto(
    @param:Json(name = "credits") val credits: String? = null
)

/** UserGitInfo — GET /api/v1/users/git-info. */
@JsonClass(generateAdapter = true)
data class GitUserDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "login") val login: String? = null,
    @param:Json(name = "avatar_url") val avatarUrl: String? = null,
    @param:Json(name = "company") val company: String? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "email") val email: String? = null
)

/** LLM model entry — GET /api/v1/config/models/search. */
@JsonClass(generateAdapter = true)
data class LlmModelDto(
    @param:Json(name = "name") val name: String = "",
    @param:Json(name = "provider") val provider: String? = null,
    @param:Json(name = "verified") val verified: Boolean = false,
    @param:Json(name = "hidden") val hidden: Boolean = false,
    @param:Json(name = "canonical") val canonical: String? = null
)

// ------------------------------------------------- secrets / settings / skills

/** CustomSecretWithoutValue. */
@JsonClass(generateAdapter = true)
data class SecretDto(
    @param:Json(name = "name") val name: String = ""
)

/** CustomSecretCreate — name + value (+ optional description). */
@JsonClass(generateAdapter = true)
data class CreateSecretRequest(
    @param:Json(name = "name") val name: String,
    @param:Json(name = "value") val value: String,
    @param:Json(name = "description") val description: String? = null
)

/** SecretNamesResponse — items of {name}. */
@JsonClass(generateAdapter = true)
data class SecretNamesDto(
    @param:Json(name = "secrets") val secrets: List<SecretNameItemDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SecretNameItemDto(
    @param:Json(name = "name") val name: String = ""
)

/** POSTProviderModel: provider tokens keyed by provider type. */
@JsonClass(generateAdapter = true)
data class ProviderTokenDto(
    @param:Json(name = "token") val token: String? = null,
    @param:Json(name = "host") val host: String? = null,
    @param:Json(name = "user_id") val userId: String? = null
)

@JsonClass(generateAdapter = true)
data class SetProviderTokensRequest(
    @param:Json(name = "provider_tokens") val providerTokens: Map<String, ProviderTokenDto>
)

/** ProfileInfo — entry of GET /api/v1/settings/profiles. */
@JsonClass(generateAdapter = true)
data class LlmProfileDto(
    @param:Json(name = "name") val name: String = "",
    @param:Json(name = "model") val model: String? = null,
    @param:Json(name = "base_url") val baseUrl: String? = null,
    @param:Json(name = "api_key_set") val apiKeySet: Boolean = false,
    @param:Json(name = "provider_connection_id") val providerConnectionId: String? = null
)

/** ProfileListResponse. */
@JsonClass(generateAdapter = true)
data class LlmProfileListDto(
    @param:Json(name = "profiles") val profiles: List<LlmProfileDto> = emptyList(),
    @param:Json(name = "active_profile") val activeProfile: String? = null
)

/** Legacy alias — the local agent-profile UI name. */
typealias AgentProfileDto = LlmProfileDto

/** RenameProfileRequest — new_name is ^[A-Za-z0-9._-]{1,64}$. */
@JsonClass(generateAdapter = true)
data class RenameProfileRequest(
    @param:Json(name = "new_name") val newName: String
)

/** SwitchProfileRequest — {profile_name}. */
@JsonClass(generateAdapter = true)
data class SwitchProfileRequest(
    @param:Json(name = "profile_name") val profileName: String
)

/** SwitchAcpModelRequest — {model}. */
@JsonClass(generateAdapter = true)
data class SwitchAcpModelRequest(
    @param:Json(name = "model") val model: String
)

/** Skill entry — /skills/search and /app-conversations/{id}/skills. */
@JsonClass(generateAdapter = true)
data class SkillDto(
    @param:Json(name = "name") val name: String = "",
    @param:Json(name = "type") val type: String? = null,
    @param:Json(name = "source") val source: String? = null,
    @param:Json(name = "triggers") val triggers: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class SkillListDto(
    @param:Json(name = "skills") val skills: List<SkillDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class HookListDto(
    @param:Json(name = "hooks") val hooks: List<Any?> = emptyList()
)

/** LLM provider entry — GET /api/v1/config/providers/search. */
@JsonClass(generateAdapter = true)
data class LlmProviderDto(
    @param:Json(name = "name") val name: String = "",
    @param:Json(name = "verified") val verified: Boolean = false
)

/** GET /api/v1/users/git-organizations — organizations are plain strings. */
@JsonClass(generateAdapter = true)
data class GitOrganizationsDto(
    @param:Json(name = "provider") val provider: String? = null,
    @param:Json(name = "organizations") val organizations: List<String> = emptyList()
)

/** WebClientConfig (feature-flag subset). */
@JsonClass(generateAdapter = true)
data class WebClientConfigDto(
    @param:Json(name = "app_mode") val appMode: String? = null,
    @param:Json(name = "feature_flags") val featureFlags: Map<String, Any?>? = null
)

/** Error envelope: FastAPI {"detail"} and auth {"error"}. */
@JsonClass(generateAdapter = true)
data class ApiErrorDto(
    @param:Json(name = "error") val error: String? = null,
    @param:Json(name = "detail") val detail: Any? = null,
    @param:Json(name = "message") val message: String? = null
)

// ---------------------------------------------------------------------------
// Domain models (UI-facing)
// ---------------------------------------------------------------------------

data class OpenHandsConversation(
    val id: String,
    val title: String,
    val uiStatus: ConversationUiStatus,
    val sandboxStatus: String?,
    val executionStatus: String?,
    val repository: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val cost: Double,
    val lastMessage: String?
)

data class AgentEvent(
    val id: String,
    val conversationId: String,
    val timestamp: Long,
    val source: String,
    val actionType: String?,
    val content: String,
    val detail: String? = null,
    val isError: Boolean = false
)

data class PromptTemplate(
    val id: String,
    val title: String,
    val category: String,
    val prompt: String,
    val defaultRepo: String? = null
)

data class CloudCredits(val credits: Double?)

// ---------------------------------------------------------------------------
// Status mapping — see docs/API_SPEC.md §3
// ---------------------------------------------------------------------------

enum class ConversationUiStatus(val label: String, val isTerminal: Boolean, val isPendingStart: Boolean) {
    STARTING("Starting", isTerminal = false, isPendingStart = true),
    WORKING("Working", isTerminal = false, isPendingStart = false),
    RUNNING("Running", isTerminal = false, isPendingStart = false),
    WAITING_INPUT("Needs input", isTerminal = false, isPendingStart = false),
    PAUSED("Paused", isTerminal = false, isPendingStart = false),
    IDLE("Idle", isTerminal = false, isPendingStart = false),
    FINISHED("Finished", isTerminal = true, isPendingStart = false),
    ERROR("Error", isTerminal = true, isPendingStart = false),
    UNKNOWN("Unknown", isTerminal = false, isPendingStart = false)
}

object StatusMapper {

    private val startupStatuses = setOf(
        "WAITING_FOR_SANDBOX", "PREPARING_REPOSITORY", "SETTING_UP_SKILLS"
    )

    fun map(sandboxStatus: String?, executionStatus: String?, rawStatus: String? = null): ConversationUiStatus {
        val sandbox = sandboxStatus?.uppercase()?.trim()
        val exec = executionStatus?.lowercase()?.trim()
        val raw = rawStatus?.uppercase()?.trim()

        // Sandbox axis takes precedence when it signals trouble or startup
        when (sandbox) {
            "STARTING" -> return ConversationUiStatus.STARTING
            "ERROR", "MISSING" -> return ConversationUiStatus.ERROR
            "PAUSED" -> return ConversationUiStatus.PAUSED
        }

        // Startup statuses may come through the generic status field
        if (exec == null && raw in startupStatuses) return ConversationUiStatus.STARTING

        when (exec) {
            "running" -> return ConversationUiStatus.RUNNING
            "finished" -> return ConversationUiStatus.FINISHED
            "error", "stuck" -> return ConversationUiStatus.ERROR
            "paused" -> return ConversationUiStatus.PAUSED
            "waiting_for_confirmation" -> return ConversationUiStatus.WAITING_INPUT
            "idle" -> return ConversationUiStatus.IDLE
        }

        // Fallbacks for servers that only return one generic status string
        return when (raw ?: exec ?: sandbox) {
            "RUNNING", "WORKING" -> ConversationUiStatus.RUNNING
            "READY", "FINISHED", "COMPLETED", "SUCCESS" -> ConversationUiStatus.FINISHED
            "PAUSED" -> ConversationUiStatus.PAUSED
            "ERROR", "FAILED", "STUCK", "MISSING" -> ConversationUiStatus.ERROR
            else -> ConversationUiStatus.UNKNOWN
        }
    }
}
