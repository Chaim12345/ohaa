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
    @param:Json(name = "thinking_blocks") val thinkingBlocks: List<ThinkingBlockDto>? = null,
    @param:Json(name = "responses_reasoning_item") val responsesReasoningItem: ReasoningItemModelDto? = null,
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
    @param:Json(name = "suggested_task") val suggestedTask: SuggestedTaskDto? = null,
    @param:Json(name = "trigger") val trigger: String? = null,
    @param:Json(name = "pr_number") val prNumber: List<Int>? = null,
    @param:Json(name = "public") val public: Boolean? = null,
    @param:Json(name = "observability_metadata") val observabilityMetadata: Map<String, String>? = null,
    @param:Json(name = "observability_tags") val observabilityTags: List<String>? = null,
    @param:Json(name = "observability_span_name") val observabilitySpanName: String? = null,
    @param:Json(name = "plugins") val plugins: List<PluginSpecDto>? = null,
    @param:Json(name = "secrets") val secrets: Map<String, String>? = null
    // `processors` (EventCallbackProcessor) omitted: empty schema in the spec.
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
    @param:Json(name = "request") val request: AppConversationStartRequestOutputDto? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "updated_at") val updatedAt: String? = null
)

/**
 * AppConversationStartRequest-Output — the echoed `request` on StartTask.
 * The Input twin is StartConversationRequest below; both share wire names.
 */
@JsonClass(generateAdapter = true)
data class AppConversationStartRequestOutputDto(
    @param:Json(name = "sandbox_id") val sandboxId: String? = null,
    @param:Json(name = "conversation_id") val conversationId: String? = null,
    @param:Json(name = "initial_message") val initialMessage: SendMessageRequest? = null,
    @param:Json(name = "system_message_suffix") val systemMessageSuffix: String? = null,
    @param:Json(name = "llm_model") val llmModel: String? = null,
    @param:Json(name = "agent_profile_id") val agentProfileId: String? = null,
    @param:Json(name = "selected_repository") val selectedRepository: String? = null,
    @param:Json(name = "selected_branch") val selectedBranch: String? = null,
    @param:Json(name = "git_provider") val gitProvider: String? = null,
    @param:Json(name = "suggested_task") val suggestedTask: SuggestedTaskDto? = null,
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "trigger") val trigger: String? = null,
    @param:Json(name = "pr_number") val prNumber: List<Int>? = null,
    @param:Json(name = "parent_conversation_id") val parentConversationId: String? = null,
    @param:Json(name = "agent_type") val agentType: String? = null,
    @param:Json(name = "public") val public: Boolean? = null,
    @param:Json(name = "observability_metadata") val observabilityMetadata: Map<String, String>? = null,
    @param:Json(name = "observability_tags") val observabilityTags: List<String>? = null,
    @param:Json(name = "observability_span_name") val observabilitySpanName: String? = null,
    @param:Json(name = "plugins") val plugins: List<PluginSpecDto>? = null,
    @param:Json(name = "secrets") val secrets: Map<String, String>? = null
    // `processors` (EventCallbackProcessor) omitted: empty schema in the spec.
)

/** PluginSpec — plugin reference in start-request payloads. */
@JsonClass(generateAdapter = true)
data class PluginSpecDto(
    @param:Json(name = "source") val source: String? = null,
    @param:Json(name = "ref") val ref: String? = null,
    @param:Json(name = "repo_path") val repoPath: String? = null,
    @param:Json(name = "parameters") val parameters: Map<String, String>? = null
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
 * Metrics (agent-server, full variant) — richer sibling of MetricsSnapshot used
 * in webhook ConversationInfo payloads. Moshi ignores absent members.
 */
@JsonClass(generateAdapter = true)
data class MetricsFullDto(
    @param:Json(name = "model_name") val modelName: String? = null,
    @param:Json(name = "accumulated_cost") val accumulatedCost: Double? = null,
    @param:Json(name = "max_budget_per_task") val maxBudgetPerTask: Double? = null,
    @param:Json(name = "accumulated_token_usage") val accumulatedTokenUsage: TokenUsageDto? = null,
    @param:Json(name = "costs") val costs: List<CostDto>? = null,
    @param:Json(name = "response_latencies") val responseLatencies: List<ResponseLatencyDto>? = null,
    @param:Json(name = "token_usages") val tokenUsages: List<TokenUsageDto>? = null
)

/** Cost — one entry of MetricsFull.costs. */
@JsonClass(generateAdapter = true)
data class CostDto(
    @param:Json(name = "model") val model: String? = null,
    @param:Json(name = "cost") val cost: Double? = null,
    @param:Json(name = "timestamp") val timestamp: Double? = null
)

/** ResponseLatency — one entry of MetricsFull.response_latencies. */
@JsonClass(generateAdapter = true)
data class ResponseLatencyDto(
    @param:Json(name = "model") val model: String? = null,
    @param:Json(name = "latency") val latency: Double? = null,
    @param:Json(name = "response_id") val responseId: String? = null
)

/** ThinkingBlock — assistant reasoning blocks (Message.thinking_blocks). */
@JsonClass(generateAdapter = true)
data class ThinkingBlockDto(
    @param:Json(name = "type") val type: String? = null,
    @param:Json(name = "thinking") val thinking: String? = null,
    @param:Json(name = "signature") val signature: String? = null
)

/** RedactedThinkingBlock — encrypted variant; `data` holds the payload. */
@JsonClass(generateAdapter = true)
data class RedactedThinkingBlockDto(
    @param:Json(name = "type") val type: String? = null,
    @param:Json(name = "data") val data: String? = null
)

/** ReasoningItemModel — OpenAI Responses-style reasoning summary. */
@JsonClass(generateAdapter = true)
data class ReasoningItemModelDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "summary") val summary: List<String>? = null,
    @param:Json(name = "content") val content: List<String>? = null,
    @param:Json(name = "encrypted_content") val encryptedContent: String? = null,
    @param:Json(name = "status") val status: String? = null
)

/** ConversationStats — ConversationInfo.stats. */
@JsonClass(generateAdapter = true)
data class ConversationStatsDto(
    @param:Json(name = "usage_to_metrics") val usageToMetrics: Map<String, MetricsFullDto>? = null
)

/** ACPModelInfo — ConversationInfo.available_models entries. */
@JsonClass(generateAdapter = true)
data class ACPModelInfoDto(
    @param:Json(name = "model_id") val modelId: String? = null,
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "description") val description: String? = null
)

/** ToolAnnotations — MCP-style hints on ClientToolSpec. */
@JsonClass(generateAdapter = true)
data class ToolAnnotationsDto(
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "readOnlyHint") val readOnlyHint: Boolean? = null,
    @param:Json(name = "destructiveHint") val destructiveHint: Boolean? = null,
    @param:Json(name = "idempotentHint") val idempotentHint: Boolean? = null,
    @param:Json(name = "openWorldHint") val openWorldHint: Boolean? = null
)

/** ClientToolSpec — ConversationInfo.client_tools entries. */
@JsonClass(generateAdapter = true)
data class ClientToolSpecDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "description") val description: String? = null,
    @param:Json(name = "parameters") val parameters: Map<String, String>? = null,
    @param:Json(name = "annotations") val annotations: ToolAnnotationsDto? = null
)

/** LaunchedAgentProfile — AppConversation.launched_agent_profile (live: object, not string). */
@JsonClass(generateAdapter = true)
data class LaunchedAgentProfileDto(
    @param:Json(name = "agent_profile_id") val agentProfileId: String? = null,
    @param:Json(name = "revision") val revision: Int? = null
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
    @param:Json(name = "launched_agent_profile") val launchedAgentProfile: LaunchedAgentProfileDto? = null,
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
    @param:Json(name = "status_detail") val statusDetail: String? = null,
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
    @param:Json(name = "link_header") val linkHeader: String? = null,
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
    @param:Json(name = "active_agent_profile_revision") val activeAgentProfileRevision: Int? = null,
    @param:Json(name = "llm_model") val llmModel: String? = null,
    @param:Json(name = "language") val language: String? = null,
    @param:Json(name = "user_version") val userVersion: Int? = null,
    @param:Json(name = "remote_runtime_resource_factor") val remoteRuntimeResourceFactor: Int? = null,
    @param:Json(name = "secrets_store") val secretsStore: String? = null,
    @param:Json(name = "enable_sound_notifications") val enableSoundNotifications: Boolean? = null,
    @param:Json(name = "enable_proactive_conversation_starters") val enableProactiveConversationStarters: Boolean? = null,
    @param:Json(name = "user_consents_to_analytics") val userConsentsToAnalytics: Boolean? = null,
    @param:Json(name = "sandbox_base_container_image") val sandboxBaseContainerImage: String? = null,
    @param:Json(name = "sandbox_runtime_container_image") val sandboxRuntimeContainerImage: String? = null,
    @param:Json(name = "disabled_skills") val disabledSkills: List<String>? = null,
    @param:Json(name = "search_api_key") val searchApiKey: String? = null,
    @param:Json(name = "sandbox_api_key") val sandboxApiKey: String? = null,
    @param:Json(name = "title_llm_profile") val titleLlmProfile: String? = null,
    @param:Json(name = "git_full_clone") val gitFullClone: Boolean? = null,
    @param:Json(name = "agent_settings") val agentSettings: Map<String, String>? = null,
    @param:Json(name = "conversation_settings") val conversationSettings: ConversationSettingsDto? = null,
    @param:Json(name = "sandbox_grouping_strategy") val sandboxGroupingStrategy: String? = null,
    @param:Json(name = "llm_profiles") val llmProfiles: LLMProfilesDto? = null,
    @param:Json(name = "registered_marketplaces") val registeredMarketplaces: List<MarketplaceRegistrationDto>? = null,
    @param:Json(name = "inherited_marketplaces") val inheritedMarketplaces: List<MarketplaceRegistrationDto>? = null
)

/** LLMProfiles — SaasUserInfo.llm_profiles. */
@JsonClass(generateAdapter = true)
data class LLMProfilesDto(
    @param:Json(name = "profiles") val profiles: Map<String, StrictLLMDto>? = null,
    @param:Json(name = "active") val active: String? = null
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
    @param:Json(name = "free") val free: Boolean = false,
    @param:Json(name = "hidden") val hidden: Boolean = false,
    @param:Json(name = "canonical") val canonical: String? = null
)

// ------------------------------------------------- secrets / settings / skills

/** CustomSecretWithoutValue. */
@JsonClass(generateAdapter = true)
data class SecretDto(
    @param:Json(name = "name") val name: String = "",
    @param:Json(name = "description") val description: String? = null
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
    @param:Json(name = "name") val name: String = "",
    @param:Json(name = "description") val description: String? = null
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
    @param:Json(name = "posthog_client_key") val posthogClientKey: String? = null,
    @param:Json(name = "feature_flags") val featureFlags: WebClientFeatureFlagsDto? = null,
    @param:Json(name = "db_feature_flags") val dbFeatureFlags: Map<String, Boolean>? = null,
    @param:Json(name = "providers_configured") val providersConfigured: List<String>? = null,
    @param:Json(name = "maintenance_start_time") val maintenanceStartTime: String? = null,
    @param:Json(name = "auth_url") val authUrl: String? = null,
    @param:Json(name = "recaptcha_site_key") val recaptchaSiteKey: String? = null,
    @param:Json(name = "faulty_models") val faultyModels: List<String>? = null,
    @param:Json(name = "error_message") val errorMessage: String? = null,
    @param:Json(name = "updated_at") val updatedAt: String? = null,
    @param:Json(name = "github_app_slug") val githubAppSlug: String? = null,
    @param:Json(name = "gitlab_enabled") val gitlabEnabled: Boolean? = null,
    @param:Json(name = "provider_default_hosts") val providerDefaultHosts: Map<String, String>? = null,
    @param:Json(name = "slack_enabled") val slackEnabled: Boolean? = null,
    @param:Json(name = "email_enabled") val emailEnabled: Boolean? = null,
    @param:Json(name = "email_change_enabled") val emailChangeEnabled: Boolean? = null,
    @param:Json(name = "acp_providers") val acpProviders: List<ACPProviderConfigDto>? = null,
    @param:Json(name = "jira_dc_oauth_host") val jiraDcOauthHost: String? = null,
    @param:Json(name = "jira_dc_service_account_managed") val jiraDcServiceAccountManaged: Boolean? = null,
    @param:Json(name = "jira_dc_service_account_email") val jiraDcServiceAccountEmail: String? = null,
    @param:Json(name = "jira_dc_service_account_config_error") val jiraDcServiceAccountConfigError: String? = null,
    @param:Json(name = "jira_oauth_enabled") val jiraOauthEnabled: Boolean? = null,
    @param:Json(name = "kind") val kind: String? = null
)

/** WebClientFeatureFlags — deployment feature toggles. */
@JsonClass(generateAdapter = true)
data class WebClientFeatureFlagsDto(
    @param:Json(name = "enable_billing") val enableBilling: Boolean? = null,
    @param:Json(name = "hide_llm_settings") val hideLlmSettings: Boolean? = null,
    @param:Json(name = "enable_jira") val enableJira: Boolean? = null,
    @param:Json(name = "enable_jira_dc") val enableJiraDc: Boolean? = null,
    @param:Json(name = "enable_linear") val enableLinear: Boolean? = null,
    @param:Json(name = "hide_users_page") val hideUsersPage: Boolean? = null,
    @param:Json(name = "hide_billing_page") val hideBillingPage: Boolean? = null,
    @param:Json(name = "hide_integrations_page") val hideIntegrationsPage: Boolean? = null,
    @param:Json(name = "hide_personal_workspaces") val hidePersonalWorkspaces: Boolean? = null,
    @param:Json(name = "allow_user_llm_configuration") val allowUserLlmConfiguration: Boolean? = null,
    @param:Json(name = "enable_acp") val enableAcp: Boolean? = null,
    @param:Json(name = "deployment_mode") val deploymentMode: String? = null,
    @param:Json(name = "enable_onboarding") val enableOnboarding: Boolean? = null,
    @param:Json(name = "enable_automations") val enableAutomations: Boolean? = null,
    @param:Json(name = "enable_agent_canvas_banner") val enableAgentCanvasBanner: Boolean? = null,
    @param:Json(name = "enable_byor_export") val enableByorExport: Boolean? = null
)

/** ACPModelOption — model choice offered by an ACP provider. */
@JsonClass(generateAdapter = true)
data class ACPModelOptionDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "label") val label: String? = null
)

/** ACPProviderConfig — WebClientConfig.acp_providers entries. */
@JsonClass(generateAdapter = true)
data class ACPProviderConfigDto(
    @param:Json(name = "key") val key: String? = null,
    @param:Json(name = "display_name") val displayName: String? = null,
    @param:Json(name = "default_command") val defaultCommand: List<String>? = null,
    @param:Json(name = "default_model") val defaultModel: String? = null,
    @param:Json(name = "available_models") val availableModels: List<ACPModelOptionDto>? = null,
    @param:Json(name = "api_key_env_var") val apiKeyEnvVar: String? = null,
    @param:Json(name = "base_url_env_var") val baseUrlEnvVar: String? = null
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
    val lastMessage: String?,
    val sandboxId: String? = null,
    /** Runtime Agent Server base URL, e.g. https://xxx.prod-runtime.all-hands.dev. */
    val conversationUrl: String? = null,
    /** Ephemeral key for runtime calls; sent as X-Session-API-Key. */
    val sessionApiKey: String? = null
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


// ==========================================================================
// Full OpenAPI surface v1.59.0 — generated from live spec schemas
// (organizations, members, budgets, profiles, agent-profiles, API keys,
//  billing, quota, admin, integrations, shared conversations, device auth)
// ==========================================================================

@JsonClass(generateAdapter = true)
data class OrgPageDto(
    @param:Json(name = "items") val items: List<OrgResponseDto>?,
    @param:Json(name = "next_page_id") val nextPageId: String?,
    @param:Json(name = "current_org_id") val currentOrgId: String?
)

@JsonClass(generateAdapter = true)
data class OrgResponseDto(
    @param:Json(name = "id") val id: String?,
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "contact_name") val contactName: String?,
    @param:Json(name = "contact_email") val contactEmail: String?,
    @param:Json(name = "conversation_expiration") val conversationExpiration: Int?,
    @param:Json(name = "remote_runtime_resource_factor") val remoteRuntimeResourceFactor: Int?,
    @param:Json(name = "billing_margin") val billingMargin: Double?,
    @param:Json(name = "enable_proactive_conversation_starters") val enableProactiveConversationStarters: Boolean?,
    @param:Json(name = "sandbox_base_container_image") val sandboxBaseContainerImage: String?,
    @param:Json(name = "sandbox_runtime_container_image") val sandboxRuntimeContainerImage: String?,
    @param:Json(name = "org_version") val orgVersion: Int?,
    @param:Json(name = "agent_settings") val agentSettings: String?,
    @param:Json(name = "conversation_settings") val conversationSettings: ConversationSettingsDto?,
    @param:Json(name = "search_api_key") val searchApiKey: String?,
    @param:Json(name = "sandbox_api_key") val sandboxApiKey: String?,
    @param:Json(name = "max_budget_per_task") val maxBudgetPerTask: Double?,
    @param:Json(name = "v1_enabled") val v1Enabled: Boolean?,
    @param:Json(name = "credits") val credits: Double?,
    @param:Json(name = "credits_available") val creditsAvailable: Boolean?,
    @param:Json(name = "is_personal") val isPersonal: Boolean?
)

@JsonClass(generateAdapter = true)
data class OrgCreateDto(
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "contact_name") val contactName: String?,
    @param:Json(name = "contact_email") val contactEmail: String?
)

@JsonClass(generateAdapter = true)
data class OrgUpdateDto(
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "contact_name") val contactName: String?,
    @param:Json(name = "contact_email") val contactEmail: String?,
    @param:Json(name = "conversation_expiration") val conversationExpiration: Int?,
    @param:Json(name = "remote_runtime_resource_factor") val remoteRuntimeResourceFactor: Int?,
    @param:Json(name = "billing_margin") val billingMargin: Double?,
    @param:Json(name = "enable_proactive_conversation_starters") val enableProactiveConversationStarters: Boolean?,
    @param:Json(name = "sandbox_base_container_image") val sandboxBaseContainerImage: String?,
    @param:Json(name = "sandbox_runtime_container_image") val sandboxRuntimeContainerImage: String?,
    @param:Json(name = "sandbox_api_key") val sandboxApiKey: String?,
    @param:Json(name = "max_budget_per_task") val maxBudgetPerTask: Double?,
    @param:Json(name = "v1_enabled") val v1Enabled: Boolean?,
    @param:Json(name = "search_api_key") val searchApiKey: String?,
    @param:Json(name = "llm_api_key") val llmApiKey: String?,
    @param:Json(name = "agent_settings_diff") val agentSettingsDiff: Map<String, String>?,
    @param:Json(name = "conversation_settings_diff") val conversationSettingsDiff: Map<String, String>?
)

@JsonClass(generateAdapter = true)
data class OrgMemberPageDto(
    @param:Json(name = "items") val items: List<OrgMemberResponseDto>?,
    @param:Json(name = "current_page") val currentPage: Int?,
    @param:Json(name = "per_page") val perPage: Int?
)

@JsonClass(generateAdapter = true)
data class OrgMemberResponseDto(
    @param:Json(name = "user_id") val userId: String?,
    @param:Json(name = "email") val email: String?,
    @param:Json(name = "role_id") val roleId: Int?,
    @param:Json(name = "role") val role: String?,
    @param:Json(name = "role_rank") val roleRank: Int?,
    @param:Json(name = "status") val status: String?
)

@JsonClass(generateAdapter = true)
data class InvitationCreateDto(
    @param:Json(name = "emails") val emails: List<String>?,
    @param:Json(name = "role") val role: String?
)

@JsonClass(generateAdapter = true)
data class BatchInvitationResponseDto(
    @param:Json(name = "successful") val successful: List<InvitationResponseDto>?,
    @param:Json(name = "failed") val failed: List<InvitationFailureDto>?,
    @param:Json(name = "email_delivery_configured") val emailDeliveryConfigured: Boolean?
)

@JsonClass(generateAdapter = true)
data class InvitationResponseDto(
    @param:Json(name = "id") val id: Int?,
    @param:Json(name = "email") val email: String?,
    @param:Json(name = "role") val role: String?,
    @param:Json(name = "status") val status: String?,
    @param:Json(name = "created_at") val createdAt: String?,
    @param:Json(name = "expires_at") val expiresAt: String?,
    @param:Json(name = "inviter_email") val inviterEmail: String?,
    @param:Json(name = "invite_url") val inviteUrl: String?
)

@JsonClass(generateAdapter = true)
data class PendingInvitationsResponseDto(
    @param:Json(name = "items") val items: List<InvitationResponseDto>?,
    @param:Json(name = "email_delivery_configured") val emailDeliveryConfigured: Boolean?,
    @param:Json(name = "auto_add_enabled") val autoAddEnabled: Boolean?
)

@JsonClass(generateAdapter = true)
data class OrgMemberUpdateDto(
    @param:Json(name = "role") val role: String?
)

@JsonClass(generateAdapter = true)
data class OrgMemberFinancialPageDto(
    @param:Json(name = "items") val items: List<OrgMemberFinancialResponseDto>?,
    @param:Json(name = "current_page") val currentPage: Int?,
    @param:Json(name = "per_page") val perPage: Int?,
    @param:Json(name = "next_page_id") val nextPageId: String?
)

@JsonClass(generateAdapter = true)
data class OrgMemberFinancialResponseDto(
    @param:Json(name = "user_id") val userId: String?,
    @param:Json(name = "email") val email: String?,
    @param:Json(name = "lifetime_spend") val lifetimeSpend: Double?,
    @param:Json(name = "current_budget") val currentBudget: Double?,
    @param:Json(name = "max_budget") val maxBudget: Double?
)

@JsonClass(generateAdapter = true)
data class AcceptInvitationRequestDto(
    @param:Json(name = "token") val token: String?
)

@JsonClass(generateAdapter = true)
data class AcceptInvitationResponseDto(
    @param:Json(name = "success") val success: Boolean?,
    @param:Json(name = "org_id") val orgId: String?,
    @param:Json(name = "org_name") val orgName: String?,
    @param:Json(name = "role") val role: String?
)

@JsonClass(generateAdapter = true)
data class MeResponseDto(
    @param:Json(name = "org_id") val orgId: String?,
    @param:Json(name = "user_id") val userId: String?,
    @param:Json(name = "email") val email: String?,
    @param:Json(name = "role") val role: String?,
    @param:Json(name = "permissions") val permissions: List<String>?,
    @param:Json(name = "llm_api_key") val llmApiKey: String?,
    @param:Json(name = "llm_api_key_for_byor") val llmApiKeyForByor: String?,
    @param:Json(name = "agent_settings_diff") val agentSettingsDiff: Map<String, String>?,
    @param:Json(name = "conversation_settings_diff") val conversationSettingsDiff: Map<String, String>?,
    @param:Json(name = "status") val status: String?
)

@JsonClass(generateAdapter = true)
data class OrgConversationPageDto(
    @param:Json(name = "items") val items: List<OrgConversationResponseDto>?,
    @param:Json(name = "total_items") val totalItems: Int?,
    @param:Json(name = "page") val page: Int?,
    @param:Json(name = "per_page") val perPage: Int?,
    @param:Json(name = "total_pages") val totalPages: Int?,
    @param:Json(name = "pagination_accurate") val paginationAccurate: Boolean?
)

@JsonClass(generateAdapter = true)
data class OrgConversationResponseDto(
    @param:Json(name = "id") val id: String?,
    @param:Json(name = "title") val title: String?,
    @param:Json(name = "llm_model") val llmModel: String?,
    @param:Json(name = "agent_kind") val agentKind: String?,
    @param:Json(name = "user_id") val userId: String?,
    @param:Json(name = "user_email") val userEmail: String?,
    @param:Json(name = "created_at") val createdAt: String?,
    @param:Json(name = "updated_at") val updatedAt: String?,
    @param:Json(name = "sandbox_id") val sandboxId: String?,
    @param:Json(name = "sandbox_status") val sandboxStatus: String?,
    @param:Json(name = "runtime_url") val runtimeUrl: String?,
    @param:Json(name = "execution_status") val executionStatus: String?,
    @param:Json(name = "selected_repository") val selectedRepository: String?,
    @param:Json(name = "selected_branch") val selectedBranch: String?,
    @param:Json(name = "git_provider") val gitProvider: String?,
    @param:Json(name = "trigger") val trigger: String?,
    @param:Json(name = "pr_number") val prNumber: List<Int>?,
    @param:Json(name = "pr_merged") val prMerged: Boolean?,
    @param:Json(name = "tags") val tags: Map<String, String>?,
    @param:Json(name = "accumulated_cost") val accumulatedCost: Double?,
    @param:Json(name = "prompt_tokens") val promptTokens: Int?,
    @param:Json(name = "completion_tokens") val completionTokens: Int?,
    @param:Json(name = "total_tokens") val totalTokens: Int?,
    @param:Json(name = "cache_read_tokens") val cacheReadTokens: Int?,
    @param:Json(name = "cache_write_tokens") val cacheWriteTokens: Int?
)

@JsonClass(generateAdapter = true)
data class OrgConversationStatsDto(
    @param:Json(name = "active_conversations") val activeConversations: Int?,
    @param:Json(name = "running_runtimes") val runningRuntimes: Int?,
    @param:Json(name = "completed_24h") val completed24H: Int?,
    @param:Json(name = "completed_7d") val completed7D: Int?,
    @param:Json(name = "completed_30d") val completed30D: Int?,
    @param:Json(name = "total_cost") val totalCost: Double?,
    @param:Json(name = "total_prompt_tokens") val totalPromptTokens: Int?,
    @param:Json(name = "total_completion_tokens") val totalCompletionTokens: Int?,
    @param:Json(name = "total_tokens") val totalTokens: Int?
)

@JsonClass(generateAdapter = true)
data class OrgUsageStatsDto(
    @param:Json(name = "active_users") val activeUsers: Int?,
    @param:Json(name = "agent_runs") val agentRuns: Int?,
    @param:Json(name = "usage_conversation_count") val usageConversationCount: Int?,
    @param:Json(name = "total_tokens") val totalTokens: Int?,
    @param:Json(name = "estimated_spend") val estimatedSpend: Double?,
    @param:Json(name = "daily_usage") val dailyUsage: List<DailyUsageDataDto>?,
    @param:Json(name = "team_usage") val teamUsage: List<TeamUsageDataDto>?,
    @param:Json(name = "model_usage") val modelUsage: List<ModelUsageDataDto>?,
    @param:Json(name = "agent_usage") val agentUsage: List<AgentUsageDataDto>?
)

@JsonClass(generateAdapter = true)
data class OrgUserUsageStatsDto(
    @param:Json(name = "items") val items: List<OrgUserUsageRowDto>?,
    @param:Json(name = "has_more") val hasMore: Boolean?
)

@JsonClass(generateAdapter = true)
data class OrgUserUsageRowDto(
    @param:Json(name = "user_id") val userId: String?,
    @param:Json(name = "user_email") val userEmail: String?,
    @param:Json(name = "user_name") val userName: String?,
    @param:Json(name = "conversation_count") val conversationCount: Int?,
    @param:Json(name = "first_conversation_at") val firstConversationAt: String?,
    @param:Json(name = "last_conversation_at") val lastConversationAt: String?,
    @param:Json(name = "first_login_at") val firstLoginAt: String?,
    @param:Json(name = "last_login_at") val lastLoginAt: String?,
    @param:Json(name = "spend_mtd") val spendMtd: Double?,
    @param:Json(name = "spend_ytd") val spendYtd: Double?,
    @param:Json(name = "spend_lifetime") val spendLifetime: Double?,
    @param:Json(name = "budget_monthly_limit") val budgetMonthlyLimit: Double?,
    @param:Json(name = "budget_is_disabled") val budgetIsDisabled: Boolean?,
    @param:Json(name = "prs_merged") val prsMerged: Int?
)

@JsonClass(generateAdapter = true)
data class OrgBudgetSettingsResponseDto(
    @param:Json(name = "enabled") val enabled: Boolean?,
    @param:Json(name = "monthly_limit") val monthlyLimit: Double?,
    @param:Json(name = "litellm_last_sync_at") val litellmLastSyncAt: String?,
    @param:Json(name = "litellm_last_sync_status") val litellmLastSyncStatus: String?,
    @param:Json(name = "litellm_last_sync_error") val litellmLastSyncError: String?,
    @param:Json(name = "reset_day") val resetDay: Int?,
    @param:Json(name = "slack_channel") val slackChannel: String?,
    @param:Json(name = "slack_team_id") val slackTeamId: String?,
    @param:Json(name = "default_user_monthly_limit") val defaultUserMonthlyLimit: Double?,
    @param:Json(name = "cycle_start_at") val cycleStartAt: String?,
    @param:Json(name = "cycle_end_at") val cycleEndAt: String?,
    @param:Json(name = "spend_status") val spendStatus: String?,
    @param:Json(name = "spend_observed_at") val spendObservedAt: String?,
    @param:Json(name = "current_spend") val currentSpend: Double?,
    @param:Json(name = "current_spend_percentage") val currentSpendPercentage: Double?,
    @param:Json(name = "unmapped_spend") val unmappedSpend: Double?,
    @param:Json(name = "unmapped_member_count") val unmappedMemberCount: Int?,
    @param:Json(name = "thresholds") val thresholds: List<OrgBudgetThresholdResponseDto>?,
    @param:Json(name = "users") val users: List<OrgBudgetUserResponseDto>?,
    @param:Json(name = "users_total") val usersTotal: Int?,
    @param:Json(name = "users_page") val usersPage: Int?,
    @param:Json(name = "users_per_page") val usersPerPage: Int?
)

@JsonClass(generateAdapter = true)
data class OrgBudgetSettingsUpdateDto(
    @param:Json(name = "enabled") val enabled: Boolean?,
    @param:Json(name = "monthly_limit") val monthlyLimit: Double?,
    @param:Json(name = "reset_day") val resetDay: Int?,
    @param:Json(name = "default_user_monthly_limit") val defaultUserMonthlyLimit: Double?,
    @param:Json(name = "slack_channel") val slackChannel: String?,
    @param:Json(name = "slack_team_id") val slackTeamId: String?,
    @param:Json(name = "thresholds") val thresholds: List<OrgBudgetThresholdUpdateDto>?
)

@JsonClass(generateAdapter = true)
data class OrgBudgetUserResponseDto(
    @param:Json(name = "user_id") val userId: String?,
    @param:Json(name = "user_email") val userEmail: String?,
    @param:Json(name = "user_name") val userName: String?,
    @param:Json(name = "current_spend") val currentSpend: Double?,
    @param:Json(name = "monthly_limit") val monthlyLimit: Double?,
    @param:Json(name = "effective_monthly_limit") val effectiveMonthlyLimit: Double?,
    @param:Json(name = "is_disabled") val isDisabled: Boolean?,
    @param:Json(name = "is_override") val isOverride: Boolean?
)

@JsonClass(generateAdapter = true)
data class OrgBudgetUserOverrideUpdateDto(
    @param:Json(name = "monthly_limit") val monthlyLimit: Double?,
    @param:Json(name = "is_disabled") val isDisabled: Boolean?
)

@JsonClass(generateAdapter = true)
data class ProviderConnectionListResponseDto(
    @param:Json(name = "connections") val connections: List<ProviderConnectionResponseDto>?
)

@JsonClass(generateAdapter = true)
data class ProviderConnectionResponseDto(
    @param:Json(name = "id") val id: String?,
    @param:Json(name = "display_name") val displayName: String?,
    @param:Json(name = "provider") val provider: String?,
    @param:Json(name = "base_url") val baseUrl: String?,
    @param:Json(name = "created_at") val createdAt: Int?,
    @param:Json(name = "updated_at") val updatedAt: Int?,
    @param:Json(name = "api_key_set") val apiKeySet: Boolean?
)

@JsonClass(generateAdapter = true)
data class ProviderConnectionCreateRequestDto(
    @param:Json(name = "display_name") val displayName: String?,
    @param:Json(name = "provider") val provider: String?,
    @param:Json(name = "api_key") val apiKey: String?,
    @param:Json(name = "base_url") val baseUrl: String?
)

@JsonClass(generateAdapter = true)
data class ProviderConnectionUpdateRequestDto(
    @param:Json(name = "display_name") val displayName: String?,
    @param:Json(name = "provider") val provider: String?,
    @param:Json(name = "api_key") val apiKey: String?,
    @param:Json(name = "base_url") val baseUrl: String?
)

@JsonClass(generateAdapter = true)
data class GitOrgClaimRequestDto(
    @param:Json(name = "provider") val provider: String?,
    @param:Json(name = "git_organization") val gitOrganization: String?
)

@JsonClass(generateAdapter = true)
data class GitOrgClaimResponseDto(
    @param:Json(name = "id") val id: String?,
    @param:Json(name = "org_id") val orgId: String?,
    @param:Json(name = "provider") val provider: String?,
    @param:Json(name = "git_organization") val gitOrganization: String?,
    @param:Json(name = "claimed_by") val claimedBy: String?,
    @param:Json(name = "claimed_at") val claimedAt: String?
)

@JsonClass(generateAdapter = true)
data class AgentProfileListResponseDto(
    @param:Json(name = "profiles") val profiles: List<AgentProfileInfoDto>?,
    @param:Json(name = "active_agent_profile_id") val activeAgentProfileId: String?
)

@JsonClass(generateAdapter = true)
data class AgentProfileInfoDto(
    @param:Json(name = "id") val id: String?,
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "agent_kind") val agentKind: String?,
    @param:Json(name = "revision") val revision: Int?,
    @param:Json(name = "llm_profile_ref") val llmProfileRef: String?,
    @param:Json(name = "mcp_server_refs") val mcpServerRefs: List<String>?
)

@JsonClass(generateAdapter = true)
data class AgentProfileDetailResponseDto(
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "profile") val profile: String?
)

@JsonClass(generateAdapter = true)
data class AgentProfileDiagnosticsDto(
    @param:Json(name = "agent_kind") val agentKind: String?,
    @param:Json(name = "valid") val valid: Boolean?,
    @param:Json(name = "errors") val errors: List<String>?,
    @param:Json(name = "llm_profile_ref") val llmProfileRef: String?,
    @param:Json(name = "llm_profile_resolved") val llmProfileResolved: Boolean?,
    @param:Json(name = "llm_api_key_set") val llmApiKeySet: Boolean?,
    @param:Json(name = "mcp_server_refs") val mcpServerRefs: List<String>?,
    @param:Json(name = "resolved_mcp_config_keys") val resolvedMcpConfigKeys: List<String>?,
    @param:Json(name = "dangling_mcp_server_refs") val danglingMcpServerRefs: List<String>?,
    @param:Json(name = "disabled_skills") val disabledSkills: List<String>?,
    @param:Json(name = "resolved_skills") val resolvedSkills: List<String>?,
    @param:Json(name = "acp_api_key_secret_name") val acpApiKeySecretName: String?,
    @param:Json(name = "acp_base_url_secret_name") val acpBaseUrlSecretName: String?,
    @param:Json(name = "acp_file_secret_names") val acpFileSecretNames: List<String>?,
    @param:Json(name = "resolved_settings") val resolvedSettings: Map<String, String>?
)

@JsonClass(generateAdapter = true)
data class ActivateAgentProfileResponseDto(
    @param:Json(name = "id") val id: String?,
    @param:Json(name = "message") val message: String?,
    @param:Json(name = "agent_settings_applied") val agentSettingsApplied: Boolean?
)

@JsonClass(generateAdapter = true)
data class RenameAgentProfileRequestDto(
    @param:Json(name = "new_name") val newName: String?
)

@JsonClass(generateAdapter = true)
data class AgentProfileMutationResponseDto(
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "message") val message: String?
)

@JsonClass(generateAdapter = true)
data class ProfileListResponseDto(
    @param:Json(name = "profiles") val profiles: List<ProfileInfoDto>?,
    @param:Json(name = "active_profile") val activeProfile: String?
)

@JsonClass(generateAdapter = true)
data class ProfileInfoDto(
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "model") val model: String?,
    @param:Json(name = "base_url") val baseUrl: String?,
    @param:Json(name = "api_key_set") val apiKeySet: Boolean?,
    @param:Json(name = "provider_connection_id") val providerConnectionId: String?
)

/** org-profiles ProfileDetailResponse — GET /api/organizations/{org_id}/profiles/{name}. */
@JsonClass(generateAdapter = true)
data class OrgProfileDetailResponseDto(
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "llm") val llm: Map<String, String>?
)

@JsonClass(generateAdapter = true)
data class SaveProfileRequestDto(
    @param:Json(name = "include_secrets") val includeSecrets: Boolean?,
    @param:Json(name = "llm") val llm: StrictLLMDto?,
    @param:Json(name = "preserve_existing_api_key") val preserveExistingApiKey: Boolean?
)

@JsonClass(generateAdapter = true)
data class ProfileMutationResponseDto(
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "message") val message: String?
)

@JsonClass(generateAdapter = true)
data class ActivateProfileResponseDto(
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "message") val message: String?,
    @param:Json(name = "model") val model: String?,
    @param:Json(name = "llm") val llm: Map<String, String>?
)

@JsonClass(generateAdapter = true)
data class ProfileDetailResponseDto(
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "config") val config: Map<String, String>?,
    @param:Json(name = "api_key_set") val apiKeySet: Boolean?,
    @param:Json(name = "llm") val llm: Map<String, String>?
)

/** MarketplacePluginPreview — entry of MarketplaceSkillsPreviewResponse.plugins. */
@JsonClass(generateAdapter = true)
data class MarketplacePluginPreviewDto(
    @param:Json(name = "name") val name: String? = null,
    @param:Json(name = "description") val description: String? = null,
    @param:Json(name = "source") val source: String? = null,
    @param:Json(name = "marketplace") val marketplace: String? = null
)

/** MarketplaceSkillsPreviewResponse — POST /api/v1/skills/marketplace-skills. */
@JsonClass(generateAdapter = true)
data class MarketplaceSkillsPreviewResponseDto(
    @param:Json(name = "skills") val skills: List<SkillDto>? = null,
    @param:Json(name = "plugins") val plugins: List<MarketplacePluginPreviewDto>? = null,
    @param:Json(name = "marketplace_skills") val marketplaceSkills: Map<String, List<String>>? = null,
    @param:Json(name = "errors") val errors: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ApiKeyCreateDto(
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "not_before") val notBefore: String?,
    @param:Json(name = "expires_at") val expiresAt: String?,
    @param:Json(name = "org_id") val orgId: String?
)

@JsonClass(generateAdapter = true)
data class ApiKeyCreateResponseDto(
    @param:Json(name = "id") val id: Int?,
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "created_at") val createdAt: String?,
    @param:Json(name = "last_used_at") val lastUsedAt: String?,
    @param:Json(name = "not_before") val notBefore: String?,
    @param:Json(name = "expires_at") val expiresAt: String?,
    @param:Json(name = "org_id") val orgId: String?,
    @param:Json(name = "key") val key: String?
)

@JsonClass(generateAdapter = true)
data class ApiKeyResponseDto(
    @param:Json(name = "id") val id: Int?,
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "created_at") val createdAt: String?,
    @param:Json(name = "last_used_at") val lastUsedAt: String?,
    @param:Json(name = "not_before") val notBefore: String?,
    @param:Json(name = "expires_at") val expiresAt: String?,
    @param:Json(name = "org_id") val orgId: String?
)

@JsonClass(generateAdapter = true)
data class CreateCheckoutSessionRequestDto(
    @param:Json(name = "amount") val amount: Int?
)

@JsonClass(generateAdapter = true)
data class CreateBillingSessionResponseDto(
    @param:Json(name = "redirect_url") val redirectUrl: String?
)

@JsonClass(generateAdapter = true)
data class SubscriptionAccessResponseDto(
    @param:Json(name = "start_at") val startAt: String?,
    @param:Json(name = "end_at") val endAt: String?,
    @param:Json(name = "created_at") val createdAt: String?,
    @param:Json(name = "cancelled_at") val cancelledAt: String?,
    @param:Json(name = "stripe_subscription_id") val stripeSubscriptionId: String?
)

@JsonClass(generateAdapter = true)
data class QuotaStatusResponseDto(
    @param:Json(name = "daily_limit") val dailyLimit: Int?,
    @param:Json(name = "used_today") val usedToday: Int?,
    @param:Json(name = "remaining") val remaining: Int?,
    @param:Json(name = "reset_at") val resetAt: String?,
    @param:Json(name = "work_email") val workEmail: String?,
    @param:Json(name = "work_email_verified") val workEmailVerified: Boolean?,
    @param:Json(name = "latest_request_status") val latestRequestStatus: String?,
    @param:Json(name = "latest_request_requested_limit") val latestRequestRequestedLimit: Int?
)

@JsonClass(generateAdapter = true)
data class OnboardingSubmissionDto(
    @param:Json(name = "selections") val selections: Map<String, String>?
)

@JsonClass(generateAdapter = true)
data class EmailUpdateDto(
    @param:Json(name = "email") val email: String?
)

@JsonClass(generateAdapter = true)
data class ResendEmailVerificationRequestDto(
    @param:Json(name = "user_id") val userId: String?,
    @param:Json(name = "is_auth_flow") val isAuthFlow: Boolean?
)

@JsonClass(generateAdapter = true)
data class TokenResponseDto(
    @param:Json(name = "token") val token: String?
)

@JsonClass(generateAdapter = true)
data class UserAppSettingsResponseDto(
    @param:Json(name = "language") val language: String?,
    @param:Json(name = "user_consents_to_analytics") val userConsentsToAnalytics: Boolean?,
    @param:Json(name = "enable_sound_notifications") val enableSoundNotifications: Boolean?,
    @param:Json(name = "git_user_name") val gitUserName: String?,
    @param:Json(name = "git_user_email") val gitUserEmail: String?,
    @param:Json(name = "git_full_clone") val gitFullClone: Boolean?
)

@JsonClass(generateAdapter = true)
data class UserAppSettingsUpdateDto(
    @param:Json(name = "language") val language: String?,
    @param:Json(name = "enable_sound_notifications") val enableSoundNotifications: Boolean?,
    @param:Json(name = "git_user_name") val gitUserName: String?,
    @param:Json(name = "git_user_email") val gitUserEmail: String?,
    @param:Json(name = "git_full_clone") val gitFullClone: Boolean?
)

@JsonClass(generateAdapter = true)
data class OrgAppSettingsResponseDto(
    @param:Json(name = "enable_proactive_conversation_starters") val enableProactiveConversationStarters: Boolean?,
    @param:Json(name = "max_budget_per_task") val maxBudgetPerTask: Double?,
    @param:Json(name = "registered_marketplaces") val registeredMarketplaces: List<MarketplaceRegistrationDto>?,
    @param:Json(name = "updated_at") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class OrgAppSettingsUpdateDto(
    @param:Json(name = "enable_proactive_conversation_starters") val enableProactiveConversationStarters: Boolean?,
    @param:Json(name = "max_budget_per_task") val maxBudgetPerTask: Double?,
    @param:Json(name = "registered_marketplaces") val registeredMarketplaces: List<MarketplaceRegistrationDto>?,
    @param:Json(name = "agent_settings_diff") val agentSettingsDiff: Map<String, String>?,
    @param:Json(name = "last_known_updated_at") val lastKnownUpdatedAt: String?
)

@JsonClass(generateAdapter = true)
data class OrgDefaultsSettingsResponseDto(
    @param:Json(name = "agent_settings") val agentSettings: String?,
    @param:Json(name = "conversation_settings") val conversationSettings: ConversationSettingsDto?,
    @param:Json(name = "llm_api_key_set") val llmApiKeySet: Boolean?,
    @param:Json(name = "search_api_key") val searchApiKey: String?
)

@JsonClass(generateAdapter = true)
data class ConversationSettingsDto(
    @param:Json(name = "schema_version") val schemaVersion: Int?,
    @param:Json(name = "max_iterations") val maxIterations: Int?,
    @param:Json(name = "confirmation_mode") val confirmationMode: Boolean?,
    @param:Json(name = "security_analyzer") val securityAnalyzer: String?
)

@JsonClass(generateAdapter = true)
data class MarketplaceRegistrationDto(
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "source") val source: String?,
    @param:Json(name = "ref") val ref: String?,
    @param:Json(name = "repo_path") val repoPath: String?,
    @param:Json(name = "auto_load") val autoLoad: Boolean?,
    @param:Json(name = "scope") val scope: String?
)

@JsonClass(generateAdapter = true)
data class CreateUserApiKeyRequestDto(
    @param:Json(name = "name") val name: String?
)

@JsonClass(generateAdapter = true)
data class CreateUserApiKeyResponseDto(
    @param:Json(name = "key") val key: String?,
    @param:Json(name = "user_id") val userId: String?,
    @param:Json(name = "org_id") val orgId: String?,
    @param:Json(name = "name") val name: String?
)

@JsonClass(generateAdapter = true)
data class FlagModelDto(
    @param:Json(name = "key") val key: String?,
    @param:Json(name = "description") val description: String?,
    @param:Json(name = "enabled") val enabled: Boolean?,
    @param:Json(name = "rules") val rules: List<FlagRuleModelDto>?
)

@JsonClass(generateAdapter = true)
data class FlagRuleModelDto(
    @param:Json(name = "id") val id: Int?,
    @param:Json(name = "effect") val effect: String?,
    @param:Json(name = "user_id") val userId: String?,
    @param:Json(name = "org_id") val orgId: String?,
    @param:Json(name = "email_pattern") val emailPattern: String?,
    @param:Json(name = "percentage") val percentage: Double?,
    @param:Json(name = "priority") val priority: Int?
)

@JsonClass(generateAdapter = true)
data class CreateFlagRequestDto(
    @param:Json(name = "key") val key: String?,
    @param:Json(name = "description") val description: String?,
    @param:Json(name = "enabled") val enabled: Boolean?
)

@JsonClass(generateAdapter = true)
data class UpdateFlagRequestDto(
    @param:Json(name = "description") val description: String?,
    @param:Json(name = "enabled") val enabled: Boolean?
)

@JsonClass(generateAdapter = true)
data class EvaluateRequestDto(
    @param:Json(name = "user_id") val userId: String?,
    @param:Json(name = "org_id") val orgId: String?,
    @param:Json(name = "email") val email: String?
)

@JsonClass(generateAdapter = true)
data class EvaluateResponseDto(
    @param:Json(name = "enabled") val enabled: Boolean?
)

@JsonClass(generateAdapter = true)
data class CreateRuleRequestDto(
    @param:Json(name = "effect") val effect: String?,
    @param:Json(name = "user_id") val userId: String?,
    @param:Json(name = "org_id") val orgId: String?,
    @param:Json(name = "email_pattern") val emailPattern: String?,
    @param:Json(name = "percentage") val percentage: Double?,
    @param:Json(name = "priority") val priority: Int?
)

@JsonClass(generateAdapter = true)
data class OrgQuotaUpdateRequestDto(
    @param:Json(name = "daily_conversation_limit") val dailyConversationLimit: Int?
)

@JsonClass(generateAdapter = true)
data class OrgQuotaResponseDto(
    @param:Json(name = "org_id") val orgId: String?,
    @param:Json(name = "org_name") val orgName: String?,
    @param:Json(name = "daily_conversation_limit") val dailyConversationLimit: Int?
)

@JsonClass(generateAdapter = true)
data class QuotaIncreaseRequestResponseDto(
    @param:Json(name = "id") val id: Int?,
    @param:Json(name = "status") val status: String?,
    @param:Json(name = "work_email") val workEmail: String?,
    @param:Json(name = "baseline_limit") val baselineLimit: Int?,
    @param:Json(name = "requested_limit") val requestedLimit: Int?,
    @param:Json(name = "reason") val reason: String?,
    @param:Json(name = "created_at") val createdAt: String?
)

@JsonClass(generateAdapter = true)
data class SuperAdminListResponseDto(
    @param:Json(name = "super_admins") val superAdmins: List<SuperAdminResponseDto>?
)

@JsonClass(generateAdapter = true)
data class SuperAdminResponseDto(
    @param:Json(name = "user_id") val userId: String?,
    @param:Json(name = "email") val email: String?
)

@JsonClass(generateAdapter = true)
data class GrantSuperAdminRequestDto(
    @param:Json(name = "user_id") val userId: String?,
    @param:Json(name = "email") val email: String?
)

@JsonClass(generateAdapter = true)
data class UserLifecycleResponseDto(
    @param:Json(name = "user_id") val userId: String?,
    @param:Json(name = "email") val email: String?,
    @param:Json(name = "warnings") val warnings: List<String>?
)

@JsonClass(generateAdapter = true)
data class VerifiedModelDto(
    @param:Json(name = "model_name") val modelName: String?,
    @param:Json(name = "provider") val provider: String?,
    @param:Json(name = "is_enabled") val isEnabled: Boolean?,
    @param:Json(name = "id") val id: Int?,
    @param:Json(name = "created_at") val createdAt: String?,
    @param:Json(name = "updated_at") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class VerifiedModelPageDto(
    @param:Json(name = "items") val items: List<VerifiedModelDto>?,
    @param:Json(name = "next_page_id") val nextPageId: String?
)

@JsonClass(generateAdapter = true)
data class VerifiedModelCreateDto(
    @param:Json(name = "model_name") val modelName: String?,
    @param:Json(name = "provider") val provider: String?,
    @param:Json(name = "is_enabled") val isEnabled: Boolean?
)

@JsonClass(generateAdapter = true)
data class VerifiedModelUpdateDto(
    @param:Json(name = "is_enabled") val isEnabled: Boolean?
)

@JsonClass(generateAdapter = true)
data class DeviceAuthorizationResponseDto(
    @param:Json(name = "device_code") val deviceCode: String?,
    @param:Json(name = "user_code") val userCode: String?,
    @param:Json(name = "verification_uri") val verificationUri: String?,
    @param:Json(name = "verification_uri_complete") val verificationUriComplete: String?,
    @param:Json(name = "expires_in") val expiresIn: Int?,
    @param:Json(name = "interval") val interval: Int?
)

@JsonClass(generateAdapter = true)
data class JiraUserResponseDto(
    @param:Json(name = "id") val id: Int?,
    @param:Json(name = "keycloak_user_id") val keycloakUserId: String?,
    @param:Json(name = "jira_workspace_id") val jiraWorkspaceId: Int?,
    @param:Json(name = "status") val status: String?,
    @param:Json(name = "created_at") val createdAt: String?,
    @param:Json(name = "updated_at") val updatedAt: String?,
    @param:Json(name = "workspace") val workspace: JiraWorkspaceResponseDto?
)

@JsonClass(generateAdapter = true)
data class JiraWorkspaceResponseDto(
    @param:Json(name = "id") val id: Int?,
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "jira_cloud_id") val jiraCloudId: String?,
    @param:Json(name = "status") val status: String?,
    @param:Json(name = "editable") val editable: Boolean?,
    @param:Json(name = "created_at") val createdAt: String?,
    @param:Json(name = "updated_at") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class JiraWorkspaceCreateDto(
    @param:Json(name = "workspace_name") val workspaceName: String?,
    @param:Json(name = "webhook_secret") val webhookSecret: String?,
    @param:Json(name = "svc_acc_email") val svcAccEmail: String?,
    @param:Json(name = "svc_acc_api_key") val svcAccApiKey: String?,
    @param:Json(name = "is_active") val isActive: Boolean?
)

@JsonClass(generateAdapter = true)
data class JiraLinkCreateDto(
    @param:Json(name = "workspace_name") val workspaceName: String?
)

@JsonClass(generateAdapter = true)
data class JiraInstanceStatusResponseDto(
    @param:Json(name = "configured") val configured: Boolean?,
    @param:Json(name = "host") val host: String?
)

@JsonClass(generateAdapter = true)
data class JiraValidateWorkspaceResponseDto(
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "status") val status: String?,
    @param:Json(name = "message") val message: String?
)

@JsonClass(generateAdapter = true)
data class ReinstallWebhookRequestDto(
    @param:Json(name = "resource") val resource: ResourceIdentifierDto?
)

@JsonClass(generateAdapter = true)
data class ResourceInstallationResultDto(
    @param:Json(name = "resource_id") val resourceId: String?,
    @param:Json(name = "resource_type") val resourceType: String?,
    @param:Json(name = "success") val success: Boolean?,
    @param:Json(name = "error") val error: String?
)

@JsonClass(generateAdapter = true)
data class ResourceWithWebhookStatusDto(
    @param:Json(name = "id") val id: String?,
    @param:Json(name = "name") val name: String?,
    @param:Json(name = "full_path") val fullPath: String?,
    @param:Json(name = "type") val type: String?,
    @param:Json(name = "webhook_installed") val webhookInstalled: Boolean?,
    @param:Json(name = "webhook_uuid") val webhookUuid: String?,
    @param:Json(name = "last_synced") val lastSynced: String?
)

@JsonClass(generateAdapter = true)
data class ResourceIdentifierDto(
    @param:Json(name = "type") val type: String?,
    @param:Json(name = "id") val id: String?
)

@JsonClass(generateAdapter = true)
data class SharedConversationDto(
    @param:Json(name = "id") val id: String?,
    @param:Json(name = "created_by_user_id") val createdByUserId: String?,
    @param:Json(name = "sandbox_id") val sandboxId: String?,
    @param:Json(name = "selected_repository") val selectedRepository: String?,
    @param:Json(name = "selected_branch") val selectedBranch: String?,
    @param:Json(name = "git_provider") val gitProvider: String?,
    @param:Json(name = "title") val title: String?,
    @param:Json(name = "pr_number") val prNumber: List<Int>?,
    @param:Json(name = "llm_model") val llmModel: String?,
    @param:Json(name = "metrics") val metrics: String?,
    @param:Json(name = "parent_conversation_id") val parentConversationId: String?,
    @param:Json(name = "sub_conversation_ids") val subConversationIds: List<String>?,
    @param:Json(name = "created_at") val createdAt: String?,
    @param:Json(name = "updated_at") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class EventPageDto(
    @param:Json(name = "items") val items: List<EventDto>?,
    @param:Json(name = "next_page_id") val nextPageId: String?
)

@JsonClass(generateAdapter = true)
data class LlmApiKeyResponseDto(
    @param:Json(name = "key") val key: String?
)

@JsonClass(generateAdapter = true)
data class ByorPermittedResponseDto(
    @param:Json(name = "permitted") val permitted: Boolean?
)

@JsonClass(generateAdapter = true)
data class ManagedLlmApiKeyRefreshResponseDto(
    @param:Json(name = "refreshed") val refreshed: Boolean?
)

@JsonClass(generateAdapter = true)
data class StrictLLMDto(
    @param:Json(name = "model") val model: String?,
    @param:Json(name = "api_key") val apiKey: String?,
    @param:Json(name = "provider_connection_id") val providerConnectionId: String?,
    @param:Json(name = "auth_type") val authType: String?,
    @param:Json(name = "subscription_vendor") val subscriptionVendor: String?,
    @param:Json(name = "base_url") val baseUrl: String?,
    @param:Json(name = "api_version") val apiVersion: String?,
    @param:Json(name = "aws_access_key_id") val awsAccessKeyId: String?,
    @param:Json(name = "aws_secret_access_key") val awsSecretAccessKey: String?,
    @param:Json(name = "aws_session_token") val awsSessionToken: String?,
    @param:Json(name = "aws_region_name") val awsRegionName: String?,
    @param:Json(name = "aws_profile_name") val awsProfileName: String?,
    @param:Json(name = "aws_role_name") val awsRoleName: String?,
    @param:Json(name = "aws_session_name") val awsSessionName: String?,
    @param:Json(name = "aws_bedrock_runtime_endpoint") val awsBedrockRuntimeEndpoint: String?,
    @param:Json(name = "openrouter_site_url") val openrouterSiteUrl: String?,
    @param:Json(name = "openrouter_app_name") val openrouterAppName: String?,
    @param:Json(name = "num_retries") val numRetries: Int?,
    @param:Json(name = "retry_multiplier") val retryMultiplier: Double?,
    @param:Json(name = "retry_min_wait") val retryMinWait: Int?,
    @param:Json(name = "retry_max_wait") val retryMaxWait: Int?,
    @param:Json(name = "timeout") val timeout: Int?,
    @param:Json(name = "max_message_chars") val maxMessageChars: Int?,
    @param:Json(name = "temperature") val temperature: Double?,
    @param:Json(name = "top_p") val topP: Double?,
    @param:Json(name = "top_k") val topK: Double?,
    @param:Json(name = "max_input_tokens") val maxInputTokens: Int?,
    @param:Json(name = "max_output_tokens") val maxOutputTokens: Int?,
    @param:Json(name = "model_canonical_name") val modelCanonicalName: String?,
    @param:Json(name = "api_mode") val apiMode: String?,
    @param:Json(name = "capability_overrides") val capabilityOverrides: Map<String, String>?,
    @param:Json(name = "extra_headers") val extraHeaders: Map<String, String>?,
    @param:Json(name = "input_cost_per_token") val inputCostPerToken: Double?,
    @param:Json(name = "output_cost_per_token") val outputCostPerToken: Double?,
    @param:Json(name = "ollama_base_url") val ollamaBaseUrl: String?,
    @param:Json(name = "stream") val stream: Boolean?,
    @param:Json(name = "drop_params") val dropParams: Boolean?,
    @param:Json(name = "modify_params") val modifyParams: Boolean?,
    @param:Json(name = "disable_vision") val disableVision: Boolean?,
    @param:Json(name = "disable_stop_word") val disableStopWord: Boolean?,
    @param:Json(name = "caching_prompt") val cachingPrompt: Boolean?,
    @param:Json(name = "log_completions") val logCompletions: Boolean?,
    @param:Json(name = "log_completions_folder") val logCompletionsFolder: String?,
    @param:Json(name = "custom_tokenizer") val customTokenizer: String?,
    @param:Json(name = "native_tool_calling") val nativeToolCalling: Boolean?,
    @param:Json(name = "force_string_serializer") val forceStringSerializer: Boolean?,
    @param:Json(name = "inline_image_urls") val inlineImageUrls: Boolean?,
    @param:Json(name = "reasoning_effort") val reasoningEffort: String?,
    @param:Json(name = "reasoning_summary") val reasoningSummary: String?,
    @param:Json(name = "enable_encrypted_reasoning") val enableEncryptedReasoning: Boolean?,
    @param:Json(name = "prompt_cache_retention") val promptCacheRetention: String?,
    @param:Json(name = "extended_thinking_budget") val extendedThinkingBudget: Int?,
    @param:Json(name = "seed") val seed: Int?,
    @param:Json(name = "usage_id") val usageId: String?,
    @param:Json(name = "litellm_extra_body") val litellmExtraBody: Map<String, String>?,
    @param:Json(name = "fallback_strategy") val fallbackStrategy: FallbackStrategyDto?
)

@JsonClass(generateAdapter = true)
data class AgentUsageDataDto(
    @param:Json(name = "agent_name") val agentName: String?,
    @param:Json(name = "conversation_count") val conversationCount: Int?,
    @param:Json(name = "total_cost") val totalCost: Double?
)

@JsonClass(generateAdapter = true)
data class DailyUsageDataDto(
    @param:Json(name = "date") val date: String?,
    @param:Json(name = "tokens") val tokens: Int?,
    @param:Json(name = "conversations") val conversations: Int?
)

@JsonClass(generateAdapter = true)
data class ModelUsageDataDto(
    @param:Json(name = "model_name") val modelName: String?,
    @param:Json(name = "conversation_count") val conversationCount: Int?,
    @param:Json(name = "total_tokens") val totalTokens: Int?,
    @param:Json(name = "total_cost") val totalCost: Double?
)

@JsonClass(generateAdapter = true)
data class TeamUsageDataDto(
    @param:Json(name = "user_id") val userId: String?,
    @param:Json(name = "user_email") val userEmail: String?,
    @param:Json(name = "user_name") val userName: String?,
    @param:Json(name = "conversation_count") val conversationCount: Int?,
    @param:Json(name = "total_tokens") val totalTokens: Int?,
    @param:Json(name = "percentage") val percentage: Double?
)

@JsonClass(generateAdapter = true)
data class InvitationFailureDto(
    @param:Json(name = "email") val email: String?,
    @param:Json(name = "error") val error: String?
)

@JsonClass(generateAdapter = true)
data class FallbackStrategyDto(
    @param:Json(name = "fallback_llms") val fallbackLlms: List<String>?,
    @param:Json(name = "profile_store_dir") val profileStoreDir: String?
)

@JsonClass(generateAdapter = true)
data class OrgBudgetThresholdResponseDto(
    @param:Json(name = "id") val id: Int?,
    @param:Json(name = "percentage") val percentage: Int?,
    @param:Json(name = "email_enabled") val emailEnabled: Boolean?,
    @param:Json(name = "slack_enabled") val slackEnabled: Boolean?
)

@JsonClass(generateAdapter = true)
data class OrgBudgetThresholdUpdateDto(
    @param:Json(name = "percentage") val percentage: Int?,
    @param:Json(name = "email_enabled") val emailEnabled: Boolean?,
    @param:Json(name = "slack_enabled") val slackEnabled: Boolean?
)

@JsonClass(generateAdapter = true)
data class CreatePrButtonClickedEventDto(
    @param:Json(name = "event_type") val eventType: String?,
    @param:Json(name = "git_provider") val gitProvider: String?
)

@JsonClass(generateAdapter = true)
data class AnalyticsEventResponseDto(
    @param:Json(name = "status") val status: String?
)

@JsonClass(generateAdapter = true)
data class GitLabResourcesResponseDto(
    @param:Json(name = "resources") val resources: List<ResourceWithWebhookStatusDto>?
)

// Webhook payload — discriminated-union sub-objects (workspace/agent) are kept
// untyped; only flat fields common to the app contract are modeled.
@JsonClass(generateAdapter = true)
data class ConversationInfoDto(
    @param:Json(name = "id") val id: String?,
    @param:Json(name = "persistence_dir") val persistenceDir: String?,
    @param:Json(name = "max_iterations") val maxIterations: Int?,
    @param:Json(name = "stuck_detection") val stuckDetection: Boolean?,
    @param:Json(name = "execution_status") val executionStatus: String?,
    @param:Json(name = "activated_knowledge_skills") val activatedKnowledgeSkills: List<String>?,
    @param:Json(name = "invoked_skills") val invokedSkills: List<String>?,
    @param:Json(name = "blocked_actions") val blockedActions: Map<String, String>?,
    @param:Json(name = "blocked_messages") val blockedMessages: Map<String, String>?,
    @param:Json(name = "last_user_message_id") val lastUserMessageId: String?,
    @param:Json(name = "leaf_event_id") val leafEventId: String?,
    @param:Json(name = "title") val title: String?,
    @param:Json(name = "created_at") val createdAt: String?,
    @param:Json(name = "updated_at") val updatedAt: String?,
    @param:Json(name = "forked_from_conversation_id") val forkedFromConversationId: String?,
    @param:Json(name = "forked_from_event_id") val forkedFromEventId: String?,
    @param:Json(name = "parent_conversation_id") val parentConversationId: String?,
    @param:Json(name = "sub_conversation_ids") val subConversationIds: List<String>?,
    @param:Json(name = "tags") val tags: Map<String, String>?,
    @param:Json(name = "current_model_id") val currentModelId: String?,
    @param:Json(name = "available_models") val availableModels: List<ACPModelInfoDto>?,
    @param:Json(name = "supports_runtime_model_switch") val supportsRuntimeModelSwitch: Boolean?,
    @param:Json(name = "launched_agent_profile") val launchedAgentProfile: LaunchedAgentProfileDto?,
    @param:Json(name = "client_tools") val clientTools: List<ClientToolSpecDto>?,
    @param:Json(name = "stats") val stats: ConversationStatsDto?,
    @param:Json(name = "agent_state") val agentState: String?,
    @param:Json(name = "metrics") val metrics: MetricsFullDto?
    // Spec fields intentionally not modeled (Moshi cannot codegen oneOf/discriminated
    // unions): workspace, agent, confirmation_policy, security_analyzer,
    // secret_registry, hook_config. Raw payloads still decode; those keys are dropped.
)

// ---------------------------------------------------------------------------
// Runtime Agent Server API (per-conversation host from `conversation_url`)
//
// Canvas talks to two hosts: the Cloud App API (v1) for history/management and
// the conversation's runtime Agent Server for live control. These models cover
// the runtime half — sent with `X-Session-API-Key`, not the cloud bearer key.
// ---------------------------------------------------------------------------

/** Body of POST /conversations/{id}/events/respond_to_confirmation. */
@JsonClass(generateAdapter = true)
data class ConfirmationResponseRequestDto(
    @param:Json(name = "accept") val accept: Boolean,
    /** Canvas sends only `{accept}`; `reason` is optional and omitted when null. */
    @param:Json(name = "reason") val reason: String? = null
)

/** Runtime POST responses are `{"success": true}`. */
@JsonClass(generateAdapter = true)
data class RuntimeSuccessDto(
    @param:Json(name = "success") val success: Boolean = true
)

/** Runtime local conversation resource (GET /conversations/{id}). */
@JsonClass(generateAdapter = true)
data class RuntimeConversationDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "agent_state") val agentState: String? = null,
    @param:Json(name = "execution_status") val executionStatus: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "updated_at") val updatedAt: String? = null,
    @param:Json(name = "max_iterations") val maxIterations: Int? = null,
    @param:Json(name = "persistence_dir") val persistenceDir: String? = null,
    @param:Json(name = "current_model_id") val currentModelId: String? = null,
    @param:Json(name = "stats") val stats: ConversationStatsDto? = null
)

/** Body of POST /conversations/{id}/switch_llm. */
@JsonClass(generateAdapter = true)
data class SwitchLlmRequestDto(
    @param:Json(name = "model") val model: String? = null,
    @param:Json(name = "profile_name") val profileName: String? = null
)

/** Body of POST /conversations/{id}/goal. */
@JsonClass(generateAdapter = true)
data class StartGoalRequestDto(
    @param:Json(name = "goal") val goal: String
)

/** Body of POST /conversations/{id}/ask_agent. */
@JsonClass(generateAdapter = true)
data class AskAgentRequestDto(
    @param:Json(name = "question") val question: String
)

/** Body of PATCH /conversations/{id}. */
@JsonClass(generateAdapter = true)
data class UpdateRuntimeConversationRequestDto(
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "public") val public: Boolean? = null,
    @param:Json(name = "tags") val tags: Map<String, String>? = null
)

/** VSCode URL from GET /vscode/url. */
@JsonClass(generateAdapter = true)
data class VSCodeUrlDto(
    @param:Json(name = "url") val url: String? = null
)

/** GET /conversations/{id}/runtime — where the runtime actually lives. */
@JsonClass(generateAdapter = true)
data class ConversationRuntimeDto(
    @param:Json(name = "url") val url: String? = null,
    @param:Json(name = "host") val host: String? = null,
    @param:Json(name = "session_api_key") val sessionApiKey: String? = null,
    @param:Json(name = "status") val status: String? = null
)

/** One entry from GET /tools/catalog. */
@JsonClass(generateAdapter = true)
data class ToolInfoDto(
    @param:Json(name = "name") val name: String = "",
    @param:Json(name = "description") val description: String? = null
)
