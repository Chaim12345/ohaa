package com.example.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive


// ---------------------------------------------------------------------------
// Wire DTOs — verified against the live OpenHands Cloud OpenAPI contract
// (https://app.all-hands.dev/openapi.json, app v1.59.0) and live responses.
// Field names mirror the spec exactly; untyped proxied endpoints (git changes)
// keep tolerant legacy shapes because the runtime response has no schema.
// ---------------------------------------------------------------------------

// ------------------------------------------------------------------ shared

/** TextContent. `additionalProperties: false`, `text` required. */
@Serializable
data class TextContentDto(
    @SerialName("cache_prompt") val cachePrompt: Boolean = false,
    @SerialName("type") val type: String = "text",
    @SerialName("text") val text: String = ""
)

/** ImageContent block (part of message content arrays). */
@Serializable
data class ImageContentDto(
    @SerialName("cache_prompt") val cachePrompt: Boolean = false,
    @SerialName("type") val type: String = "image",
    @SerialName("image_urls") val imageUrls: List<String> = emptyList()
)

/** Message — payload of MessageEvent.llm_message and SendMessageRequest bodies. */
@Serializable
data class MessageDto(
    @SerialName("role") val role: String? = null,
    @SerialName("content") val content: JsonElement? = null,
    @SerialName("thinking_blocks") val thinkingBlocks: List<ThinkingBlockDto>? = null,
    @SerialName("responses_reasoning_item") val responsesReasoningItem: ReasoningItemModelDto? = null,
    @SerialName("tool_calls") val toolCalls: List<MessageToolCallDto>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null
) {
    /** Flattens the content blocks (or plain string) into displayable text. */
    fun contentText(): String? = MessagePayloads.contentToText(content)
}

/** MessageToolCall — the `tool_call` payload on ActionEvent. */
@Serializable
data class MessageToolCallDto(
    @SerialName("id") val id: String? = null,
    @SerialName("responses_item_id") val responsesItemId: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("arguments") val arguments: String? = null,
    @SerialName("origin") val origin: String? = null
)

/**
 * Flat renderer helpers for the heterogeneous nested event payloads
 * (observations, actions, llm messages). Kept as JSON-friendly primitives so
 * the ~60 tool variants decode without one adapter each.
 */
object MessagePayloads {

    /** TextContent list / plain string / arbitrary JSON → displayable text. */
    fun contentToText(content: JsonElement?): String? = when (content) {
        null -> null
        is JsonPrimitive -> content.contentOrNull?.takeIf { it.isNotBlank() }
        is JsonArray -> content.mapNotNull { block ->
            (block as? JsonObject)?.let { extractBlockText(it) }
        }.joinToString("\n").takeIf { it.isNotBlank() }
        else -> null
    }

    private fun extractBlockText(block: JsonObject): String? = when {
        block["text"] is JsonPrimitive -> block["text"]?.jsonPrimitive?.contentOrNull
        block["content"] != null -> contentToText(block["content"])
        else -> null
    }

    /** Best-effort extraction of a key from a JSON-arguments string or map. */
    fun argument(arguments: String?, key: String): String? {
        val parsed = parseJsonMap(arguments) ?: return null
        return parsed[key]?.jsonPrimitive?.contentOrNull
    }

    fun parseJsonMap(raw: String?): JsonObject? {
        if (raw.isNullOrBlank()) return null
        return try {
            Json.parseToJsonElement(raw) as? JsonObject
        } catch (e: Exception) {
            null
        }
    }
}

// --------------------------------------------------------------- conversations

/** AppSendMessageRequest — POST /app-conversations/{id}/send-message. */
@Serializable
data class SendMessageRequest(
    @SerialName("role") val role: String = "user",
    @SerialName("content") val content: List<TextContentDto>,
    @SerialName("run") val run: Boolean = true
) {
    companion object {
        fun text(text: String, run: Boolean = true) =
            SendMessageRequest(content = listOf(TextContentDto(text = text)), run = run)
    }
}

/** AppConversationStartRequest — POST /api/v1/app-conversations. */
@Serializable
data class StartConversationRequest(
    @SerialName("initial_message") val initialMessage: SendMessageRequest? = null,
    @SerialName("selected_repository") val selectedRepository: String? = null,
    @SerialName("selected_branch") val selectedBranch: String? = null,
    @SerialName("git_provider") val gitProvider: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("llm_model") val llmModel: String? = null,
    @SerialName("agent_type") val agentType: String = "default",
    @SerialName("agent_profile_id") val agentProfileId: String? = null,
    @SerialName("sandbox_id") val sandboxId: String? = null,
    @SerialName("conversation_id") val conversationId: String? = null,
    @SerialName("parent_conversation_id") val parentConversationId: String? = null,
    @SerialName("system_message_suffix") val systemMessageSuffix: String? = null,
    @SerialName("suggested_task") val suggestedTask: SuggestedTaskDto? = null,
    @SerialName("trigger") val trigger: String? = null,
    @SerialName("pr_number") val prNumber: List<Int>? = null,
    @SerialName("public") val public: Boolean? = null,
    @SerialName("observability_metadata") val observabilityMetadata: Map<String, String>? = null,
    @SerialName("observability_tags") val observabilityTags: List<String>? = null,
    @SerialName("observability_span_name") val observabilitySpanName: String? = null,
    @SerialName("plugins") val plugins: List<PluginSpecDto>? = null,
    @SerialName("secrets") val secrets: Map<String, String>? = null
    // `processors` (EventCallbackProcessor) omitted: empty schema in the spec.
)

/** Legacy name kept for the existing UI/tests. */
typealias CreateConversationRequestDto = StartConversationRequest

/** AppConversationStartTask — returned by POST /app-conversations and polls. */
@Serializable
data class StartTaskDto(
    @SerialName("id") val id: String? = null,
    @SerialName("created_by_user_id") val createdByUserId: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("detail") val detail: String? = null,
    @SerialName("app_conversation_id") val appConversationId: String? = null,
    @SerialName("sandbox_id") val sandboxId: String? = null,
    @SerialName("agent_server_url") val agentServerUrl: String? = null,
    @SerialName("request") val request: AppConversationStartRequestOutputDto? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

/**
 * AppConversationStartRequest-Output — the echoed `request` on StartTask.
 * The Input twin is StartConversationRequest below; both share wire names.
 */
@Serializable
data class AppConversationStartRequestOutputDto(
    @SerialName("sandbox_id") val sandboxId: String? = null,
    @SerialName("conversation_id") val conversationId: String? = null,
    @SerialName("initial_message") val initialMessage: SendMessageRequest? = null,
    @SerialName("system_message_suffix") val systemMessageSuffix: String? = null,
    @SerialName("llm_model") val llmModel: String? = null,
    @SerialName("agent_profile_id") val agentProfileId: String? = null,
    @SerialName("selected_repository") val selectedRepository: String? = null,
    @SerialName("selected_branch") val selectedBranch: String? = null,
    @SerialName("git_provider") val gitProvider: String? = null,
    @SerialName("suggested_task") val suggestedTask: SuggestedTaskDto? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("trigger") val trigger: String? = null,
    @SerialName("pr_number") val prNumber: List<Int>? = null,
    @SerialName("parent_conversation_id") val parentConversationId: String? = null,
    @SerialName("agent_type") val agentType: String? = null,
    @SerialName("public") val public: Boolean? = null,
    @SerialName("observability_metadata") val observabilityMetadata: Map<String, String>? = null,
    @SerialName("observability_tags") val observabilityTags: List<String>? = null,
    @SerialName("observability_span_name") val observabilitySpanName: String? = null,
    @SerialName("plugins") val plugins: List<PluginSpecDto>? = null,
    @SerialName("secrets") val secrets: Map<String, String>? = null
    // `processors` (EventCallbackProcessor) omitted: empty schema in the spec.
)

/** PluginSpec — plugin reference in start-request payloads. */
@Serializable
data class PluginSpecDto(
    @SerialName("source") val source: String? = null,
    @SerialName("ref") val ref: String? = null,
    @SerialName("repo_path") val repoPath: String? = null,
    @SerialName("parameters") val parameters: Map<String, String>? = null
)

/** TokenUsage — nested in MetricsSnapshot. */
@Serializable
data class TokenUsageDto(
    @SerialName("model") val model: String? = "",
    @SerialName("prompt_tokens") val promptTokens: Long = 0,
    @SerialName("completion_tokens") val completionTokens: Long = 0,
    @SerialName("cache_read_tokens") val cacheReadTokens: Long = 0,
    @SerialName("cache_write_tokens") val cacheWriteTokens: Long = 0,
    @SerialName("reasoning_tokens") val reasoningTokens: Long = 0,
    @SerialName("context_window") val contextWindow: Long = 0,
    @SerialName("per_turn_token") val perTurnToken: Long = 0,
    @SerialName("response_id") val responseId: String? = ""
)

/** MetricsSnapshot — AppConversation.metrics. */
@Serializable
data class MetricsDto(
    @SerialName("model_name") val modelName: String? = "default",
    @SerialName("accumulated_cost") val accumulatedCost: Double = 0.0,
    @SerialName("max_budget_per_task") val maxBudgetPerTask: Double? = null,
    @SerialName("accumulated_token_usage") val accumulatedTokenUsage: TokenUsageDto? = null
)

/**
 * Metrics (agent-server, full variant) — richer sibling of MetricsSnapshot used
 * in webhook ConversationInfo payloads. Unknown members are ignored.
 */
@Serializable
data class MetricsFullDto(
    @SerialName("model_name") val modelName: String? = null,
    @SerialName("accumulated_cost") val accumulatedCost: Double? = null,
    @SerialName("max_budget_per_task") val maxBudgetPerTask: Double? = null,
    @SerialName("accumulated_token_usage") val accumulatedTokenUsage: TokenUsageDto? = null,
    @SerialName("costs") val costs: List<CostDto>? = null,
    @SerialName("response_latencies") val responseLatencies: List<ResponseLatencyDto>? = null,
    @SerialName("token_usages") val tokenUsages: List<TokenUsageDto>? = null
)

/** Cost — one entry of MetricsFull.costs. */
@Serializable
data class CostDto(
    @SerialName("model") val model: String? = null,
    @SerialName("cost") val cost: Double? = null,
    @SerialName("timestamp") val timestamp: Double? = null
)

/** ResponseLatency — one entry of MetricsFull.response_latencies. */
@Serializable
data class ResponseLatencyDto(
    @SerialName("model") val model: String? = null,
    @SerialName("latency") val latency: Double? = null,
    @SerialName("response_id") val responseId: String? = null
)

/** ThinkingBlock — assistant reasoning blocks (Message.thinking_blocks). */
@Serializable
data class ThinkingBlockDto(
    @SerialName("type") val type: String? = null,
    @SerialName("thinking") val thinking: String? = null,
    @SerialName("signature") val signature: String? = null
)

/** RedactedThinkingBlock — encrypted variant; `data` holds the payload. */
@Serializable
data class RedactedThinkingBlockDto(
    @SerialName("type") val type: String? = null,
    @SerialName("data") val data: String? = null
)

/** ReasoningItemModel — OpenAI Responses-style reasoning summary. */
@Serializable
data class ReasoningItemModelDto(
    @SerialName("id") val id: String? = null,
    @SerialName("summary") val summary: List<String>? = null,
    @SerialName("content") val content: List<String>? = null,
    @SerialName("encrypted_content") val encryptedContent: String? = null,
    @SerialName("status") val status: String? = null
)

/** ConversationStats — ConversationInfo.stats. */
@Serializable
data class ConversationStatsDto(
    @SerialName("usage_to_metrics") val usageToMetrics: Map<String, MetricsFullDto>? = null
)

/** ACPModelInfo — ConversationInfo.available_models entries. */
@Serializable
data class ACPModelInfoDto(
    @SerialName("model_id") val modelId: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null
)

/** ToolAnnotations — MCP-style hints on ClientToolSpec. */
@Serializable
data class ToolAnnotationsDto(
    @SerialName("title") val title: String? = null,
    @SerialName("readOnlyHint") val readOnlyHint: Boolean? = null,
    @SerialName("destructiveHint") val destructiveHint: Boolean? = null,
    @SerialName("idempotentHint") val idempotentHint: Boolean? = null,
    @SerialName("openWorldHint") val openWorldHint: Boolean? = null
)

/** ClientToolSpec — ConversationInfo.client_tools entries. */
@Serializable
data class ClientToolSpecDto(
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("parameters") val parameters: Map<String, String>? = null,
    @SerialName("annotations") val annotations: ToolAnnotationsDto? = null
)

/** LaunchedAgentProfile — AppConversation.launched_agent_profile (live: object, not string). */
@Serializable
data class LaunchedAgentProfileDto(
    @SerialName("agent_profile_id") val agentProfileId: String? = null,
    @SerialName("revision") val revision: Int? = null
)

/**
 * AppConversation — the conversation resource (search + batch get).
 * `cost` is derived from metrics.accumulated_cost; `lastMessage` is a local
 * cache field (the spec has no top-level equivalent).
 */
@Serializable
data class AppConversationDto(
    @SerialName("id") val id: String? = null,
    @SerialName("created_by_user_id") val createdByUserId: String? = null,
    @SerialName("sandbox_id") val sandboxId: String? = null,
    @SerialName("selected_repository") val selectedRepository: String? = null,
    @SerialName("selected_branch") val selectedBranch: String? = null,
    @SerialName("git_provider") val gitProvider: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("trigger") val trigger: String? = null,
    @SerialName("pr_number") val prNumber: List<Int>? = null,
    @SerialName("llm_model") val llmModel: String? = null,
    @SerialName("agent_kind") val agentKind: String? = null,
    @SerialName("metrics") val metrics: MetricsDto? = null,
    @SerialName("parent_conversation_id") val parentConversationId: String? = null,
    @SerialName("sub_conversation_ids") val subConversationIds: List<String>? = null,
    @SerialName("public") val public: Boolean? = null,
    @SerialName("tags") val tags: Map<String, String>? = null,
    @SerialName("launched_agent_profile") val launchedAgentProfile: LaunchedAgentProfileDto? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("sandbox_status") val sandboxStatus: String? = null,
    @SerialName("execution_status") val executionStatus: String? = null,
    @SerialName("conversation_url") val conversationUrl: String? = null,
    @SerialName("session_api_key") val sessionApiKey: String? = null,
    @SerialName("acp_server") val acpServer: String? = null
) {
    val resolvedId: String get() = id ?: ""
    val cost: Double get() = metrics?.accumulatedCost ?: 0.0
}

/** Legacy name kept for existing call sites. */
typealias ConversationDto = AppConversationDto

/** AppConversationUpdateRequest — PATCH /app-conversations/{id}. */
@Serializable
data class UpdateConversationRequest(
    @SerialName("title") val title: String? = null,
    @SerialName("public") val public: Boolean? = null,
    @SerialName("selected_repository") val selectedRepository: String? = null,
    @SerialName("selected_branch") val selectedBranch: String? = null,
    @SerialName("git_provider") val gitProvider: String? = null
)

/** AppSendMessageResponse — {success, sandbox_status, message}. */
@Serializable
data class SendMessageResponseDto(
    @SerialName("success") val success: Boolean = false,
    @SerialName("sandbox_status") val sandboxStatus: String? = null,
    @SerialName("message") val message: String? = null
)

/** PendingMessageResponse — POST /conversations/{id}/pending-messages → 201. */
@Serializable
data class PendingMessageResponseDto(
    @SerialName("id") val id: String? = null,
    @SerialName("queued") val queued: Boolean = false,
    @SerialName("position") val position: Int = 0
)

@Serializable
data class SuccessDto(
    @SerialName("success") val success: Boolean = true,
    @SerialName("message") val message: String? = null
)

/** {message} envelope — secrets CRUD and provider-token endpoints. */
@Serializable
data class MessageResponseDto(
    @SerialName("message") val message: String = ""
)

/** Generic page wrapper: AppConversationPage, EventPage, SandboxPage, … */
@Serializable
data class PageDto<T>(
    @SerialName("items") val items: List<T> = emptyList(),
    @SerialName("next_page_id") val nextPageId: String? = null
)

/** Legacy alias — the events page used to be parsed under this name. */
typealias EventsPageDto = PageDto<EventDto>

// --------------------------------------------------------------------- events

/**
 * Flattened union of every `Event` subtype (discriminated by `kind`), decoded
 * from live payloads. Fields belonging to another subtype deserialize as null.
 */
@Serializable
data class EventDto(
    @SerialName("id") val id: String? = null,
    @SerialName("kind") val kind: String? = null,
    @SerialName("timestamp") val timestamp: String? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("parent_id") val parentId: String? = null,
    // ActionEvent
    @SerialName("thought") val thought: JsonElement? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null,
    @SerialName("action") val action: JsonElement? = null,
    @SerialName("tool_name") val toolName: String? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("tool_call") val toolCall: MessageToolCallDto? = null,
    @SerialName("llm_response_id") val llmResponseId: String? = null,
    @SerialName("security_risk") val securityRisk: String? = null,
    @SerialName("summary") val summary: String? = null,
    // ObservationEvent / UserRejectObservation / AgentErrorEvent
    @SerialName("observation") val observation: JsonElement? = null,
    @SerialName("action_id") val actionId: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("error") val error: String? = null,
    // MessageEvent
    @SerialName("llm_message") val llmMessage: MessageDto? = null,
    @SerialName("activated_skills") val activatedSkills: List<String>? = null,
    @SerialName("sender") val sender: String? = null,
    // ServerErrorEvent / ConversationErrorEvent
    @SerialName("code") val code: String? = null,
    @SerialName("detail") val detail: String? = null,
    // ConversationStateUpdateEvent
    @SerialName("key") val key: String? = null,
    @SerialName("value") val value: JsonElement? = null,
    // SystemPromptEvent
    @SerialName("system_prompt") val systemPrompt: TextContentDto? = null,
    // StreamingDeltaEvent / ACPToolCallEvent content
    @SerialName("content") val content: JsonElement? = null,
    // Condensation
    @SerialName("forgotten_event_ids") val forgottenEventIds: List<String>? = null,
    // ACPToolCallEvent (Claude Code / Codex / Gemini CLI conversations)
    @SerialName("title") val title: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("tool_kind") val toolKind: String? = null,
    @SerialName("raw_input") val rawInput: JsonElement? = null,
    @SerialName("raw_output") val rawOutput: JsonElement? = null,
    @SerialName("is_error") val isError: Boolean? = null,
    // Legacy flat payloads (older runtimes) — harmless if absent
    @SerialName("message") val message: String? = null,
    @SerialName("args") val args: Map<String, JsonElement>? = null
) {
    val thoughtText: String? get() = MessagePayloads.contentToText(thought)
    val observationText: String? get() = MessagePayloads.contentToText(observation)
}

// ------------------------------------------------------------------- sandboxes

@Serializable
data class ExposedUrlDto(
    @SerialName("name") val name: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("port") val port: Int = 0
)

/** SandboxInfo. */
@Serializable
data class SandboxDto(
    @SerialName("id") val id: String? = null,
    @SerialName("created_by_user_id") val createdByUserId: String? = null,
    @SerialName("sandbox_spec_id") val sandboxSpecId: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("status_detail") val statusDetail: String? = null,
    @SerialName("session_api_key") val sessionApiKey: String? = null,
    @SerialName("exposed_urls") val exposedUrls: List<ExposedUrlDto>? = null,
    @SerialName("created_at") val createdAt: String? = null
)

/** SandboxSpecInfo. */
@Serializable
data class SandboxSpecDto(
    @SerialName("id") val id: String? = null,
    @SerialName("command") val command: List<String>? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("initial_env") val initialEnv: Map<String, String>? = null,
    @SerialName("working_dir") val workingDir: String? = null
)

// ------------------------------------------------------------------------ git

/** Repository — item of GET /api/v1/git/repositories/search. */
@Serializable
data class RepositoryDto(
    @SerialName("id") val id: String? = null,
    @SerialName("full_name") val fullName: String = "",
    @SerialName("git_provider") val gitProvider: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("stargazers_count") val stargazersCount: Int? = null,
    @SerialName("link_header") val linkHeader: String? = null,
    @SerialName("pushed_at") val pushedAt: String? = null,
    @SerialName("owner_type") val ownerType: String? = null,
    @SerialName("main_branch") val mainBranch: String? = null
) {
    /** Legacy UI field: default branch of the repository. */
    val branch: String? get() = mainBranch
    val displayName: String get() = fullName.ifBlank { id ?: "unknown repo" }
}

/** Branch — item of GET /api/v1/git/branches/search. */
@Serializable
data class BranchDto(
    @SerialName("name") val name: String = "",
    @SerialName("commit_sha") val commitSha: String? = null,
    @SerialName("protected") val protected: Boolean = false,
    @SerialName("last_push_date") val lastPushDate: String? = null
)

/** SuggestedTask — item of GET /api/v1/git/suggested-tasks/search. */
@Serializable
data class SuggestedTaskDto(
    @SerialName("git_provider") val gitProvider: String? = null,
    @SerialName("task_type") val taskType: String? = null,
    @SerialName("repo") val repo: String = "",
    @SerialName("issue_number") val issueNumber: Int = 0,
    @SerialName("title") val title: String = ""
)

/**
 * Item of GET /app-conversations/{id}/git/changes. The runtime response has no
 * OpenAPI schema; both the spec-era {path, status} and the legacy
 * {file, status, insertions, deletions} shapes are accepted.
 */
@Serializable
data class GitChangeDto(
    @SerialName("path") val path: String? = null,
    @SerialName("file") val file: String? = null,
    @SerialName("status") val status: String = "",
    @SerialName("insertions") val insertions: Int = 0,
    @SerialName("deletions") val deletions: Int = 0
) {
    val displayName: String get() = file ?: path ?: ""
}

/**
 * GET /app-conversations/{id}/git/diff — untyped proxied runtime response.
 * Tolerates {original, modified}, {diff}, and file-list variants.
 */
@Serializable
data class GitDiffDto(
    @SerialName("original") val original: String? = null,
    @SerialName("modified") val modified: String? = null,
    @SerialName("diff") val diff: String? = null,
    @SerialName("files") val files: List<GitChangeDto>? = null
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
@Serializable
data class UserInfoDto(
    @SerialName("id") val id: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("email_verified") val emailVerified: Boolean? = null,
    @SerialName("git_user_name") val gitUserName: String? = null,
    @SerialName("git_user_email") val gitUserEmail: String? = null,
    @SerialName("org_id") val orgId: String? = null,
    @SerialName("org_name") val orgName: String? = null,
    @SerialName("role") val role: String? = null,
    @SerialName("permissions") val permissions: List<String>? = null,
    @SerialName("max_budget_per_task") val maxBudgetPerTask: Double? = null,
    @SerialName("v1_enabled") val v1Enabled: Boolean? = null,
    @SerialName("default_sandbox_spec_id") val defaultSandboxSpecId: String? = null,
    @SerialName("active_agent_profile_id") val activeAgentProfileId: String? = null,
    @SerialName("active_agent_profile_revision") val activeAgentProfileRevision: Int? = null,
    @SerialName("llm_model") val llmModel: String? = null,
    @SerialName("language") val language: String? = null,
    @SerialName("user_version") val userVersion: Int? = null,
    @SerialName("remote_runtime_resource_factor") val remoteRuntimeResourceFactor: Int? = null,
    // Free-form object in the live API (e.g. {"provider_tokens":{}}), despite
    // the spec leaving the schema open. Typing it as String crashed decoding of
    // the whole response and left Settings without an account.
    @SerialName("secrets_store") val secretsStore: JsonElement? = null,
    @SerialName("enable_sound_notifications") val enableSoundNotifications: Boolean? = null,
    @SerialName("enable_proactive_conversation_starters") val enableProactiveConversationStarters: Boolean? = null,
    @SerialName("user_consents_to_analytics") val userConsentsToAnalytics: Boolean? = null,
    @SerialName("sandbox_base_container_image") val sandboxBaseContainerImage: String? = null,
    @SerialName("sandbox_runtime_container_image") val sandboxRuntimeContainerImage: String? = null,
    @SerialName("disabled_skills") val disabledSkills: List<String>? = null,
    @SerialName("search_api_key") val searchApiKey: String? = null,
    @SerialName("sandbox_api_key") val sandboxApiKey: String? = null,
    @SerialName("title_llm_profile") val titleLlmProfile: String? = null,
    @SerialName("git_full_clone") val gitFullClone: Boolean? = null,
    // Live API returns a nested object here (schema_version, agent, llm, ...).
    @SerialName("agent_settings") val agentSettings: JsonElement? = null,
    @SerialName("conversation_settings") val conversationSettings: ConversationSettingsDto? = null,
    @SerialName("sandbox_grouping_strategy") val sandboxGroupingStrategy: String? = null,
    @SerialName("llm_profiles") val llmProfiles: LLMProfilesDto? = null,
    @SerialName("registered_marketplaces") val registeredMarketplaces: List<MarketplaceRegistrationDto>? = null,
    @SerialName("inherited_marketplaces") val inheritedMarketplaces: List<MarketplaceRegistrationDto>? = null
)

/** LLMProfiles — SaasUserInfo.llm_profiles. */
@Serializable
data class LLMProfilesDto(
    @SerialName("profiles") val profiles: Map<String, StrictLLMDto>? = null,
    @SerialName("active") val active: String? = null
)

/** CurrentApiKeyResponse — GET /api/keys/current. */
@Serializable
data class CurrentApiKeyDto(
    @SerialName("id") val id: Long? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("org_id") val orgId: String? = null,
    @SerialName("bound_org_id") val boundOrgId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("auth_type") val authType: String? = null
)

/** GetCreditsResponse — credits is a numeric string per the live spec. */
@Serializable
data class CreditsDto(
    @SerialName("credits") val credits: String? = null
)

/** UserGitInfo — GET /api/v1/users/git-info. */
@Serializable
data class GitUserDto(
    @SerialName("id") val id: String? = null,
    @SerialName("login") val login: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("company") val company: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("email") val email: String? = null
)

/** LLM model entry — GET /api/v1/config/models/search. */
@Serializable
data class LlmModelDto(
    @SerialName("name") val name: String = "",
    @SerialName("provider") val provider: String? = null,
    @SerialName("verified") val verified: Boolean = false,
    @SerialName("free") val free: Boolean = false,
    @SerialName("hidden") val hidden: Boolean = false,
    @SerialName("canonical") val canonical: String? = null
)

// ------------------------------------------------- secrets / settings / skills

/** CustomSecretWithoutValue. */
@Serializable
data class SecretDto(
    @SerialName("name") val name: String = "",
    @SerialName("description") val description: String? = null
)

/** CustomSecretCreate — name + value (+ optional description). */
@Serializable
data class CreateSecretRequest(
    @SerialName("name") val name: String,
    @SerialName("value") val value: String,
    @SerialName("description") val description: String? = null
)

/** SecretNamesResponse — items of {name}. */
@Serializable
data class SecretNamesDto(
    @SerialName("secrets") val secrets: List<SecretNameItemDto> = emptyList()
)

@Serializable
data class SecretNameItemDto(
    @SerialName("name") val name: String = "",
    @SerialName("description") val description: String? = null
)

/** POSTProviderModel: provider tokens keyed by provider type. */
@Serializable
data class ProviderTokenDto(
    @SerialName("token") val token: String? = null,
    @SerialName("host") val host: String? = null,
    @SerialName("user_id") val userId: String? = null
)

@Serializable
data class SetProviderTokensRequest(
    @SerialName("provider_tokens") val providerTokens: Map<String, ProviderTokenDto>
)

/** ProfileInfo — entry of GET /api/v1/settings/profiles. */
@Serializable
data class LlmProfileDto(
    @SerialName("name") val name: String = "",
    @SerialName("model") val model: String? = null,
    @SerialName("base_url") val baseUrl: String? = null,
    @SerialName("api_key_set") val apiKeySet: Boolean = false,
    @SerialName("provider_connection_id") val providerConnectionId: String? = null
)

/** ProfileListResponse. */
@Serializable
data class LlmProfileListDto(
    @SerialName("profiles") val profiles: List<LlmProfileDto> = emptyList(),
    @SerialName("active_profile") val activeProfile: String? = null
)

/** Legacy alias — the local agent-profile UI name. */
typealias AgentProfileDto = LlmProfileDto

/** RenameProfileRequest — new_name is ^[A-Za-z0-9._-]{1,64}$. */
@Serializable
data class RenameProfileRequest(
    @SerialName("new_name") val newName: String
)

/** SwitchProfileRequest — {profile_name}. */
@Serializable
data class SwitchProfileRequest(
    @SerialName("profile_name") val profileName: String
)

/** SwitchAcpModelRequest — {model}. */
@Serializable
data class SwitchAcpModelRequest(
    @SerialName("model") val model: String
)

/** Skill entry — /skills/search and /app-conversations/{id}/skills. */
@Serializable
data class SkillDto(
    @SerialName("name") val name: String = "",
    @SerialName("type") val type: String? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("triggers") val triggers: List<String>? = null
)

@Serializable
data class SkillListDto(
    @SerialName("skills") val skills: List<SkillDto> = emptyList()
)

@Serializable
data class HookListDto(
    @SerialName("hooks") val hooks: List<JsonElement> = emptyList()
)

/** LLM provider entry — GET /api/v1/config/providers/search. */
@Serializable
data class LlmProviderDto(
    @SerialName("name") val name: String = "",
    @SerialName("verified") val verified: Boolean = false
)

/** GET /api/v1/users/git-organizations — organizations are plain strings. */
@Serializable
data class GitOrganizationsDto(
    @SerialName("provider") val provider: String? = null,
    @SerialName("organizations") val organizations: List<String> = emptyList()
)

/** WebClientConfig (feature-flag subset). */
@Serializable
data class WebClientConfigDto(
    @SerialName("app_mode") val appMode: String? = null,
    @SerialName("posthog_client_key") val posthogClientKey: String? = null,
    @SerialName("feature_flags") val featureFlags: WebClientFeatureFlagsDto? = null,
    @SerialName("db_feature_flags") val dbFeatureFlags: Map<String, Boolean>? = null,
    @SerialName("providers_configured") val providersConfigured: List<String>? = null,
    @SerialName("maintenance_start_time") val maintenanceStartTime: String? = null,
    @SerialName("auth_url") val authUrl: String? = null,
    @SerialName("recaptcha_site_key") val recaptchaSiteKey: String? = null,
    @SerialName("faulty_models") val faultyModels: List<String>? = null,
    @SerialName("error_message") val errorMessage: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("github_app_slug") val githubAppSlug: String? = null,
    @SerialName("gitlab_enabled") val gitlabEnabled: Boolean? = null,
    @SerialName("provider_default_hosts") val providerDefaultHosts: Map<String, String>? = null,
    @SerialName("slack_enabled") val slackEnabled: Boolean? = null,
    @SerialName("email_enabled") val emailEnabled: Boolean? = null,
    @SerialName("email_change_enabled") val emailChangeEnabled: Boolean? = null,
    @SerialName("acp_providers") val acpProviders: List<ACPProviderConfigDto>? = null,
    @SerialName("jira_dc_oauth_host") val jiraDcOauthHost: String? = null,
    @SerialName("jira_dc_service_account_managed") val jiraDcServiceAccountManaged: Boolean? = null,
    @SerialName("jira_dc_service_account_email") val jiraDcServiceAccountEmail: String? = null,
    @SerialName("jira_dc_service_account_config_error") val jiraDcServiceAccountConfigError: String? = null,
    @SerialName("jira_oauth_enabled") val jiraOauthEnabled: Boolean? = null,
    @SerialName("kind") val kind: String? = null
)

/** WebClientFeatureFlags — deployment feature toggles. */
@Serializable
data class WebClientFeatureFlagsDto(
    @SerialName("enable_billing") val enableBilling: Boolean? = null,
    @SerialName("hide_llm_settings") val hideLlmSettings: Boolean? = null,
    @SerialName("enable_jira") val enableJira: Boolean? = null,
    @SerialName("enable_jira_dc") val enableJiraDc: Boolean? = null,
    @SerialName("enable_linear") val enableLinear: Boolean? = null,
    @SerialName("hide_users_page") val hideUsersPage: Boolean? = null,
    @SerialName("hide_billing_page") val hideBillingPage: Boolean? = null,
    @SerialName("hide_integrations_page") val hideIntegrationsPage: Boolean? = null,
    @SerialName("hide_personal_workspaces") val hidePersonalWorkspaces: Boolean? = null,
    @SerialName("allow_user_llm_configuration") val allowUserLlmConfiguration: Boolean? = null,
    @SerialName("enable_acp") val enableAcp: Boolean? = null,
    @SerialName("deployment_mode") val deploymentMode: String? = null,
    @SerialName("enable_onboarding") val enableOnboarding: Boolean? = null,
    @SerialName("enable_automations") val enableAutomations: Boolean? = null,
    @SerialName("enable_agent_canvas_banner") val enableAgentCanvasBanner: Boolean? = null,
    @SerialName("enable_byor_export") val enableByorExport: Boolean? = null
)

/** ACPModelOption — model choice offered by an ACP provider. */
@Serializable
data class ACPModelOptionDto(
    @SerialName("id") val id: String? = null,
    @SerialName("label") val label: String? = null
)

/** ACPProviderConfig — WebClientConfig.acp_providers entries. */
@Serializable
data class ACPProviderConfigDto(
    @SerialName("key") val key: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("default_command") val defaultCommand: List<String>? = null,
    @SerialName("default_model") val defaultModel: String? = null,
    @SerialName("available_models") val availableModels: List<ACPModelOptionDto>? = null,
    @SerialName("api_key_env_var") val apiKeyEnvVar: String? = null,
    @SerialName("base_url_env_var") val baseUrlEnvVar: String? = null
)

/** Error envelope: FastAPI {"detail"} and auth {"error"}. */
@Serializable
data class ApiErrorDto(
    @SerialName("error") val error: String? = null,
    @SerialName("detail") val detail: JsonElement? = null,
    @SerialName("message") val message: String? = null
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

@Serializable
data class OrgPageDto(
    @SerialName("items") val items: List<OrgResponseDto>?,
    @SerialName("next_page_id") val nextPageId: String?,
    @SerialName("current_org_id") val currentOrgId: String?
)

@Serializable
data class OrgResponseDto(
    @SerialName("id") val id: String?,
    @SerialName("name") val name: String?,
    @SerialName("contact_name") val contactName: String?,
    @SerialName("contact_email") val contactEmail: String?,
    @SerialName("conversation_expiration") val conversationExpiration: Int?,
    @SerialName("remote_runtime_resource_factor") val remoteRuntimeResourceFactor: Int?,
    @SerialName("billing_margin") val billingMargin: Double?,
    @SerialName("enable_proactive_conversation_starters") val enableProactiveConversationStarters: Boolean?,
    @SerialName("sandbox_base_container_image") val sandboxBaseContainerImage: String?,
    @SerialName("sandbox_runtime_container_image") val sandboxRuntimeContainerImage: String?,
    @SerialName("org_version") val orgVersion: Int?,
    @SerialName("agent_settings") val agentSettings: String?,
    @SerialName("conversation_settings") val conversationSettings: ConversationSettingsDto?,
    @SerialName("search_api_key") val searchApiKey: String?,
    @SerialName("sandbox_api_key") val sandboxApiKey: String?,
    @SerialName("max_budget_per_task") val maxBudgetPerTask: Double?,
    @SerialName("v1_enabled") val v1Enabled: Boolean?,
    @SerialName("credits") val credits: Double?,
    @SerialName("credits_available") val creditsAvailable: Boolean?,
    @SerialName("is_personal") val isPersonal: Boolean?
)

@Serializable
data class OrgCreateDto(
    @SerialName("name") val name: String?,
    @SerialName("contact_name") val contactName: String?,
    @SerialName("contact_email") val contactEmail: String?
)

@Serializable
data class OrgUpdateDto(
    @SerialName("name") val name: String?,
    @SerialName("contact_name") val contactName: String?,
    @SerialName("contact_email") val contactEmail: String?,
    @SerialName("conversation_expiration") val conversationExpiration: Int?,
    @SerialName("remote_runtime_resource_factor") val remoteRuntimeResourceFactor: Int?,
    @SerialName("billing_margin") val billingMargin: Double?,
    @SerialName("enable_proactive_conversation_starters") val enableProactiveConversationStarters: Boolean?,
    @SerialName("sandbox_base_container_image") val sandboxBaseContainerImage: String?,
    @SerialName("sandbox_runtime_container_image") val sandboxRuntimeContainerImage: String?,
    @SerialName("sandbox_api_key") val sandboxApiKey: String?,
    @SerialName("max_budget_per_task") val maxBudgetPerTask: Double?,
    @SerialName("v1_enabled") val v1Enabled: Boolean?,
    @SerialName("search_api_key") val searchApiKey: String?,
    @SerialName("llm_api_key") val llmApiKey: String?,
    @SerialName("agent_settings_diff") val agentSettingsDiff: Map<String, String>?,
    @SerialName("conversation_settings_diff") val conversationSettingsDiff: Map<String, String>?
)

@Serializable
data class OrgMemberPageDto(
    @SerialName("items") val items: List<OrgMemberResponseDto>?,
    @SerialName("current_page") val currentPage: Int?,
    @SerialName("per_page") val perPage: Int?
)

@Serializable
data class OrgMemberResponseDto(
    @SerialName("user_id") val userId: String?,
    @SerialName("email") val email: String?,
    @SerialName("role_id") val roleId: Int?,
    @SerialName("role") val role: String?,
    @SerialName("role_rank") val roleRank: Int?,
    @SerialName("status") val status: String?
)

@Serializable
data class InvitationCreateDto(
    @SerialName("emails") val emails: List<String>?,
    @SerialName("role") val role: String?
)

@Serializable
data class BatchInvitationResponseDto(
    @SerialName("successful") val successful: List<InvitationResponseDto>?,
    @SerialName("failed") val failed: List<InvitationFailureDto>?,
    @SerialName("email_delivery_configured") val emailDeliveryConfigured: Boolean?
)

@Serializable
data class InvitationResponseDto(
    @SerialName("id") val id: Int?,
    @SerialName("email") val email: String?,
    @SerialName("role") val role: String?,
    @SerialName("status") val status: String?,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("expires_at") val expiresAt: String?,
    @SerialName("inviter_email") val inviterEmail: String?,
    @SerialName("invite_url") val inviteUrl: String?
)

@Serializable
data class PendingInvitationsResponseDto(
    @SerialName("items") val items: List<InvitationResponseDto>?,
    @SerialName("email_delivery_configured") val emailDeliveryConfigured: Boolean?,
    @SerialName("auto_add_enabled") val autoAddEnabled: Boolean?
)

@Serializable
data class OrgMemberUpdateDto(
    @SerialName("role") val role: String?
)

@Serializable
data class OrgMemberFinancialPageDto(
    @SerialName("items") val items: List<OrgMemberFinancialResponseDto>?,
    @SerialName("current_page") val currentPage: Int?,
    @SerialName("per_page") val perPage: Int?,
    @SerialName("next_page_id") val nextPageId: String?
)

@Serializable
data class OrgMemberFinancialResponseDto(
    @SerialName("user_id") val userId: String?,
    @SerialName("email") val email: String?,
    @SerialName("lifetime_spend") val lifetimeSpend: Double?,
    @SerialName("current_budget") val currentBudget: Double?,
    @SerialName("max_budget") val maxBudget: Double?
)

@Serializable
data class AcceptInvitationRequestDto(
    @SerialName("token") val token: String?
)

@Serializable
data class AcceptInvitationResponseDto(
    @SerialName("success") val success: Boolean?,
    @SerialName("org_id") val orgId: String?,
    @SerialName("org_name") val orgName: String?,
    @SerialName("role") val role: String?
)

@Serializable
data class MeResponseDto(
    @SerialName("org_id") val orgId: String?,
    @SerialName("user_id") val userId: String?,
    @SerialName("email") val email: String?,
    @SerialName("role") val role: String?,
    @SerialName("permissions") val permissions: List<String>?,
    @SerialName("llm_api_key") val llmApiKey: String?,
    @SerialName("llm_api_key_for_byor") val llmApiKeyForByor: String?,
    @SerialName("agent_settings_diff") val agentSettingsDiff: Map<String, String>?,
    @SerialName("conversation_settings_diff") val conversationSettingsDiff: Map<String, String>?,
    @SerialName("status") val status: String?
)

@Serializable
data class OrgConversationPageDto(
    @SerialName("items") val items: List<OrgConversationResponseDto>?,
    @SerialName("total_items") val totalItems: Int?,
    @SerialName("page") val page: Int?,
    @SerialName("per_page") val perPage: Int?,
    @SerialName("total_pages") val totalPages: Int?,
    @SerialName("pagination_accurate") val paginationAccurate: Boolean?
)

@Serializable
data class OrgConversationResponseDto(
    @SerialName("id") val id: String?,
    @SerialName("title") val title: String?,
    @SerialName("llm_model") val llmModel: String?,
    @SerialName("agent_kind") val agentKind: String?,
    @SerialName("user_id") val userId: String?,
    @SerialName("user_email") val userEmail: String?,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("updated_at") val updatedAt: String?,
    @SerialName("sandbox_id") val sandboxId: String?,
    @SerialName("sandbox_status") val sandboxStatus: String?,
    @SerialName("runtime_url") val runtimeUrl: String?,
    @SerialName("execution_status") val executionStatus: String?,
    @SerialName("selected_repository") val selectedRepository: String?,
    @SerialName("selected_branch") val selectedBranch: String?,
    @SerialName("git_provider") val gitProvider: String?,
    @SerialName("trigger") val trigger: String?,
    @SerialName("pr_number") val prNumber: List<Int>?,
    @SerialName("pr_merged") val prMerged: Boolean?,
    @SerialName("tags") val tags: Map<String, String>?,
    @SerialName("accumulated_cost") val accumulatedCost: Double?,
    @SerialName("prompt_tokens") val promptTokens: Int?,
    @SerialName("completion_tokens") val completionTokens: Int?,
    @SerialName("total_tokens") val totalTokens: Int?,
    @SerialName("cache_read_tokens") val cacheReadTokens: Int?,
    @SerialName("cache_write_tokens") val cacheWriteTokens: Int?
)

@Serializable
data class OrgConversationStatsDto(
    @SerialName("active_conversations") val activeConversations: Int?,
    @SerialName("running_runtimes") val runningRuntimes: Int?,
    @SerialName("completed_24h") val completed24H: Int?,
    @SerialName("completed_7d") val completed7D: Int?,
    @SerialName("completed_30d") val completed30D: Int?,
    @SerialName("total_cost") val totalCost: Double?,
    @SerialName("total_prompt_tokens") val totalPromptTokens: Int?,
    @SerialName("total_completion_tokens") val totalCompletionTokens: Int?,
    @SerialName("total_tokens") val totalTokens: Int?
)

@Serializable
data class OrgUsageStatsDto(
    @SerialName("active_users") val activeUsers: Int?,
    @SerialName("agent_runs") val agentRuns: Int?,
    @SerialName("usage_conversation_count") val usageConversationCount: Int?,
    @SerialName("total_tokens") val totalTokens: Int?,
    @SerialName("estimated_spend") val estimatedSpend: Double?,
    @SerialName("daily_usage") val dailyUsage: List<DailyUsageDataDto>?,
    @SerialName("team_usage") val teamUsage: List<TeamUsageDataDto>?,
    @SerialName("model_usage") val modelUsage: List<ModelUsageDataDto>?,
    @SerialName("agent_usage") val agentUsage: List<AgentUsageDataDto>?
)

@Serializable
data class OrgUserUsageStatsDto(
    @SerialName("items") val items: List<OrgUserUsageRowDto>?,
    @SerialName("has_more") val hasMore: Boolean?
)

@Serializable
data class OrgUserUsageRowDto(
    @SerialName("user_id") val userId: String?,
    @SerialName("user_email") val userEmail: String?,
    @SerialName("user_name") val userName: String?,
    @SerialName("conversation_count") val conversationCount: Int?,
    @SerialName("first_conversation_at") val firstConversationAt: String?,
    @SerialName("last_conversation_at") val lastConversationAt: String?,
    @SerialName("first_login_at") val firstLoginAt: String?,
    @SerialName("last_login_at") val lastLoginAt: String?,
    @SerialName("spend_mtd") val spendMtd: Double?,
    @SerialName("spend_ytd") val spendYtd: Double?,
    @SerialName("spend_lifetime") val spendLifetime: Double?,
    @SerialName("budget_monthly_limit") val budgetMonthlyLimit: Double?,
    @SerialName("budget_is_disabled") val budgetIsDisabled: Boolean?,
    @SerialName("prs_merged") val prsMerged: Int?
)

@Serializable
data class OrgBudgetSettingsResponseDto(
    @SerialName("enabled") val enabled: Boolean?,
    @SerialName("monthly_limit") val monthlyLimit: Double?,
    @SerialName("litellm_last_sync_at") val litellmLastSyncAt: String?,
    @SerialName("litellm_last_sync_status") val litellmLastSyncStatus: String?,
    @SerialName("litellm_last_sync_error") val litellmLastSyncError: String?,
    @SerialName("reset_day") val resetDay: Int?,
    @SerialName("slack_channel") val slackChannel: String?,
    @SerialName("slack_team_id") val slackTeamId: String?,
    @SerialName("default_user_monthly_limit") val defaultUserMonthlyLimit: Double?,
    @SerialName("cycle_start_at") val cycleStartAt: String?,
    @SerialName("cycle_end_at") val cycleEndAt: String?,
    @SerialName("spend_status") val spendStatus: String?,
    @SerialName("spend_observed_at") val spendObservedAt: String?,
    @SerialName("current_spend") val currentSpend: Double?,
    @SerialName("current_spend_percentage") val currentSpendPercentage: Double?,
    @SerialName("unmapped_spend") val unmappedSpend: Double?,
    @SerialName("unmapped_member_count") val unmappedMemberCount: Int?,
    @SerialName("thresholds") val thresholds: List<OrgBudgetThresholdResponseDto>?,
    @SerialName("users") val users: List<OrgBudgetUserResponseDto>?,
    @SerialName("users_total") val usersTotal: Int?,
    @SerialName("users_page") val usersPage: Int?,
    @SerialName("users_per_page") val usersPerPage: Int?
)

@Serializable
data class OrgBudgetSettingsUpdateDto(
    @SerialName("enabled") val enabled: Boolean?,
    @SerialName("monthly_limit") val monthlyLimit: Double?,
    @SerialName("reset_day") val resetDay: Int?,
    @SerialName("default_user_monthly_limit") val defaultUserMonthlyLimit: Double?,
    @SerialName("slack_channel") val slackChannel: String?,
    @SerialName("slack_team_id") val slackTeamId: String?,
    @SerialName("thresholds") val thresholds: List<OrgBudgetThresholdUpdateDto>?
)

@Serializable
data class OrgBudgetUserResponseDto(
    @SerialName("user_id") val userId: String?,
    @SerialName("user_email") val userEmail: String?,
    @SerialName("user_name") val userName: String?,
    @SerialName("current_spend") val currentSpend: Double?,
    @SerialName("monthly_limit") val monthlyLimit: Double?,
    @SerialName("effective_monthly_limit") val effectiveMonthlyLimit: Double?,
    @SerialName("is_disabled") val isDisabled: Boolean?,
    @SerialName("is_override") val isOverride: Boolean?
)

@Serializable
data class OrgBudgetUserOverrideUpdateDto(
    @SerialName("monthly_limit") val monthlyLimit: Double?,
    @SerialName("is_disabled") val isDisabled: Boolean?
)

@Serializable
data class ProviderConnectionListResponseDto(
    @SerialName("connections") val connections: List<ProviderConnectionResponseDto>?
)

@Serializable
data class ProviderConnectionResponseDto(
    @SerialName("id") val id: String?,
    @SerialName("display_name") val displayName: String?,
    @SerialName("provider") val provider: String?,
    @SerialName("base_url") val baseUrl: String?,
    @SerialName("created_at") val createdAt: Int?,
    @SerialName("updated_at") val updatedAt: Int?,
    @SerialName("api_key_set") val apiKeySet: Boolean?
)

@Serializable
data class ProviderConnectionCreateRequestDto(
    @SerialName("display_name") val displayName: String?,
    @SerialName("provider") val provider: String?,
    @SerialName("api_key") val apiKey: String?,
    @SerialName("base_url") val baseUrl: String?
)

@Serializable
data class ProviderConnectionUpdateRequestDto(
    @SerialName("display_name") val displayName: String?,
    @SerialName("provider") val provider: String?,
    @SerialName("api_key") val apiKey: String?,
    @SerialName("base_url") val baseUrl: String?
)

@Serializable
data class GitOrgClaimRequestDto(
    @SerialName("provider") val provider: String?,
    @SerialName("git_organization") val gitOrganization: String?
)

@Serializable
data class GitOrgClaimResponseDto(
    @SerialName("id") val id: String?,
    @SerialName("org_id") val orgId: String?,
    @SerialName("provider") val provider: String?,
    @SerialName("git_organization") val gitOrganization: String?,
    @SerialName("claimed_by") val claimedBy: String?,
    @SerialName("claimed_at") val claimedAt: String?
)

@Serializable
data class AgentProfileListResponseDto(
    @SerialName("profiles") val profiles: List<AgentProfileInfoDto>?,
    @SerialName("active_agent_profile_id") val activeAgentProfileId: String?
)

@Serializable
data class AgentProfileInfoDto(
    @SerialName("id") val id: String?,
    @SerialName("name") val name: String?,
    @SerialName("agent_kind") val agentKind: String?,
    @SerialName("revision") val revision: Int?,
    @SerialName("llm_profile_ref") val llmProfileRef: String?,
    @SerialName("mcp_server_refs") val mcpServerRefs: List<String>?
)

@Serializable
data class AgentProfileDetailResponseDto(
    @SerialName("name") val name: String?,
    @SerialName("profile") val profile: String?
)

@Serializable
data class AgentProfileDiagnosticsDto(
    @SerialName("agent_kind") val agentKind: String?,
    @SerialName("valid") val valid: Boolean?,
    @SerialName("errors") val errors: List<String>?,
    @SerialName("llm_profile_ref") val llmProfileRef: String?,
    @SerialName("llm_profile_resolved") val llmProfileResolved: Boolean?,
    @SerialName("llm_api_key_set") val llmApiKeySet: Boolean?,
    @SerialName("mcp_server_refs") val mcpServerRefs: List<String>?,
    @SerialName("resolved_mcp_config_keys") val resolvedMcpConfigKeys: List<String>?,
    @SerialName("dangling_mcp_server_refs") val danglingMcpServerRefs: List<String>?,
    @SerialName("disabled_skills") val disabledSkills: List<String>?,
    @SerialName("resolved_skills") val resolvedSkills: List<String>?,
    @SerialName("acp_api_key_secret_name") val acpApiKeySecretName: String?,
    @SerialName("acp_base_url_secret_name") val acpBaseUrlSecretName: String?,
    @SerialName("acp_file_secret_names") val acpFileSecretNames: List<String>?,
    @SerialName("resolved_settings") val resolvedSettings: Map<String, String>?
)

@Serializable
data class ActivateAgentProfileResponseDto(
    @SerialName("id") val id: String?,
    @SerialName("message") val message: String?,
    @SerialName("agent_settings_applied") val agentSettingsApplied: Boolean?
)

@Serializable
data class RenameAgentProfileRequestDto(
    @SerialName("new_name") val newName: String?
)

@Serializable
data class AgentProfileMutationResponseDto(
    @SerialName("name") val name: String?,
    @SerialName("message") val message: String?
)

@Serializable
data class ProfileListResponseDto(
    @SerialName("profiles") val profiles: List<ProfileInfoDto>?,
    @SerialName("active_profile") val activeProfile: String?
)

@Serializable
data class ProfileInfoDto(
    @SerialName("name") val name: String?,
    @SerialName("model") val model: String?,
    @SerialName("base_url") val baseUrl: String?,
    @SerialName("api_key_set") val apiKeySet: Boolean?,
    @SerialName("provider_connection_id") val providerConnectionId: String?
)

/** org-profiles ProfileDetailResponse — GET /api/organizations/{org_id}/profiles/{name}. */
@Serializable
data class OrgProfileDetailResponseDto(
    @SerialName("name") val name: String?,
    @SerialName("llm") val llm: Map<String, String>?
)

@Serializable
data class SaveProfileRequestDto(
    @SerialName("include_secrets") val includeSecrets: Boolean?,
    @SerialName("llm") val llm: StrictLLMDto?,
    @SerialName("preserve_existing_api_key") val preserveExistingApiKey: Boolean?
)

@Serializable
data class ProfileMutationResponseDto(
    @SerialName("name") val name: String?,
    @SerialName("message") val message: String?
)

@Serializable
data class ActivateProfileResponseDto(
    @SerialName("name") val name: String?,
    @SerialName("message") val message: String?,
    @SerialName("model") val model: String?,
    @SerialName("llm") val llm: Map<String, String>?
)

@Serializable
data class ProfileDetailResponseDto(
    @SerialName("name") val name: String?,
    @SerialName("config") val config: Map<String, String>?,
    @SerialName("api_key_set") val apiKeySet: Boolean?,
    @SerialName("llm") val llm: Map<String, String>?
)

/** MarketplacePluginPreview — entry of MarketplaceSkillsPreviewResponse.plugins. */
@Serializable
data class MarketplacePluginPreviewDto(
    @SerialName("name") val name: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("marketplace") val marketplace: String? = null
)

/** MarketplaceSkillsPreviewResponse — POST /api/v1/skills/marketplace-skills. */
@Serializable
data class MarketplaceSkillsPreviewResponseDto(
    @SerialName("skills") val skills: List<SkillDto>? = null,
    @SerialName("plugins") val plugins: List<MarketplacePluginPreviewDto>? = null,
    @SerialName("marketplace_skills") val marketplaceSkills: Map<String, List<String>>? = null,
    @SerialName("errors") val errors: List<String>? = null
)

@Serializable
data class ApiKeyCreateDto(
    @SerialName("name") val name: String?,
    @SerialName("not_before") val notBefore: String?,
    @SerialName("expires_at") val expiresAt: String?,
    @SerialName("org_id") val orgId: String?
)

@Serializable
data class ApiKeyCreateResponseDto(
    @SerialName("id") val id: Int?,
    @SerialName("name") val name: String?,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("last_used_at") val lastUsedAt: String?,
    @SerialName("not_before") val notBefore: String?,
    @SerialName("expires_at") val expiresAt: String?,
    @SerialName("org_id") val orgId: String?,
    @SerialName("key") val key: String?
)

@Serializable
data class ApiKeyResponseDto(
    @SerialName("id") val id: Int?,
    @SerialName("name") val name: String?,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("last_used_at") val lastUsedAt: String?,
    @SerialName("not_before") val notBefore: String?,
    @SerialName("expires_at") val expiresAt: String?,
    @SerialName("org_id") val orgId: String?
)

@Serializable
data class CreateCheckoutSessionRequestDto(
    @SerialName("amount") val amount: Int?
)

@Serializable
data class CreateBillingSessionResponseDto(
    @SerialName("redirect_url") val redirectUrl: String?
)

@Serializable
data class SubscriptionAccessResponseDto(
    @SerialName("start_at") val startAt: String?,
    @SerialName("end_at") val endAt: String?,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("cancelled_at") val cancelledAt: String?,
    @SerialName("stripe_subscription_id") val stripeSubscriptionId: String?
)

@Serializable
data class QuotaStatusResponseDto(
    @SerialName("daily_limit") val dailyLimit: Int?,
    @SerialName("used_today") val usedToday: Int?,
    @SerialName("remaining") val remaining: Int?,
    @SerialName("reset_at") val resetAt: String?,
    @SerialName("work_email") val workEmail: String?,
    @SerialName("work_email_verified") val workEmailVerified: Boolean?,
    @SerialName("latest_request_status") val latestRequestStatus: String?,
    @SerialName("latest_request_requested_limit") val latestRequestRequestedLimit: Int?
)

@Serializable
data class OnboardingSubmissionDto(
    @SerialName("selections") val selections: Map<String, String>?
)

@Serializable
data class EmailUpdateDto(
    @SerialName("email") val email: String?
)

@Serializable
data class ResendEmailVerificationRequestDto(
    @SerialName("user_id") val userId: String?,
    @SerialName("is_auth_flow") val isAuthFlow: Boolean?
)

@Serializable
data class TokenResponseDto(
    @SerialName("token") val token: String?
)

@Serializable
data class UserAppSettingsResponseDto(
    @SerialName("language") val language: String?,
    @SerialName("user_consents_to_analytics") val userConsentsToAnalytics: Boolean?,
    @SerialName("enable_sound_notifications") val enableSoundNotifications: Boolean?,
    @SerialName("git_user_name") val gitUserName: String?,
    @SerialName("git_user_email") val gitUserEmail: String?,
    @SerialName("git_full_clone") val gitFullClone: Boolean?
)

@Serializable
data class UserAppSettingsUpdateDto(
    @SerialName("language") val language: String?,
    @SerialName("enable_sound_notifications") val enableSoundNotifications: Boolean?,
    @SerialName("git_user_name") val gitUserName: String?,
    @SerialName("git_user_email") val gitUserEmail: String?,
    @SerialName("git_full_clone") val gitFullClone: Boolean?
)

@Serializable
data class OrgAppSettingsResponseDto(
    @SerialName("enable_proactive_conversation_starters") val enableProactiveConversationStarters: Boolean?,
    @SerialName("max_budget_per_task") val maxBudgetPerTask: Double?,
    @SerialName("registered_marketplaces") val registeredMarketplaces: List<MarketplaceRegistrationDto>?,
    @SerialName("updated_at") val updatedAt: String?
)

@Serializable
data class OrgAppSettingsUpdateDto(
    @SerialName("enable_proactive_conversation_starters") val enableProactiveConversationStarters: Boolean?,
    @SerialName("max_budget_per_task") val maxBudgetPerTask: Double?,
    @SerialName("registered_marketplaces") val registeredMarketplaces: List<MarketplaceRegistrationDto>?,
    @SerialName("agent_settings_diff") val agentSettingsDiff: Map<String, String>?,
    @SerialName("last_known_updated_at") val lastKnownUpdatedAt: String?
)

@Serializable
data class OrgDefaultsSettingsResponseDto(
    @SerialName("agent_settings") val agentSettings: String?,
    @SerialName("conversation_settings") val conversationSettings: ConversationSettingsDto?,
    @SerialName("llm_api_key_set") val llmApiKeySet: Boolean?,
    @SerialName("search_api_key") val searchApiKey: String?
)

@Serializable
data class ConversationSettingsDto(
    @SerialName("schema_version") val schemaVersion: Int?,
    @SerialName("max_iterations") val maxIterations: Int?,
    @SerialName("confirmation_mode") val confirmationMode: Boolean?,
    @SerialName("security_analyzer") val securityAnalyzer: String?
)

@Serializable
data class MarketplaceRegistrationDto(
    @SerialName("name") val name: String?,
    @SerialName("source") val source: String?,
    @SerialName("ref") val ref: String?,
    @SerialName("repo_path") val repoPath: String?,
    @SerialName("auto_load") val autoLoad: Boolean?,
    @SerialName("scope") val scope: String?
)

@Serializable
data class CreateUserApiKeyRequestDto(
    @SerialName("name") val name: String?
)

@Serializable
data class CreateUserApiKeyResponseDto(
    @SerialName("key") val key: String?,
    @SerialName("user_id") val userId: String?,
    @SerialName("org_id") val orgId: String?,
    @SerialName("name") val name: String?
)

@Serializable
data class FlagModelDto(
    @SerialName("key") val key: String?,
    @SerialName("description") val description: String?,
    @SerialName("enabled") val enabled: Boolean?,
    @SerialName("rules") val rules: List<FlagRuleModelDto>?
)

@Serializable
data class FlagRuleModelDto(
    @SerialName("id") val id: Int?,
    @SerialName("effect") val effect: String?,
    @SerialName("user_id") val userId: String?,
    @SerialName("org_id") val orgId: String?,
    @SerialName("email_pattern") val emailPattern: String?,
    @SerialName("percentage") val percentage: Double?,
    @SerialName("priority") val priority: Int?
)

@Serializable
data class CreateFlagRequestDto(
    @SerialName("key") val key: String?,
    @SerialName("description") val description: String?,
    @SerialName("enabled") val enabled: Boolean?
)

@Serializable
data class UpdateFlagRequestDto(
    @SerialName("description") val description: String?,
    @SerialName("enabled") val enabled: Boolean?
)

@Serializable
data class EvaluateRequestDto(
    @SerialName("user_id") val userId: String?,
    @SerialName("org_id") val orgId: String?,
    @SerialName("email") val email: String?
)

@Serializable
data class EvaluateResponseDto(
    @SerialName("enabled") val enabled: Boolean?
)

@Serializable
data class CreateRuleRequestDto(
    @SerialName("effect") val effect: String?,
    @SerialName("user_id") val userId: String?,
    @SerialName("org_id") val orgId: String?,
    @SerialName("email_pattern") val emailPattern: String?,
    @SerialName("percentage") val percentage: Double?,
    @SerialName("priority") val priority: Int?
)

@Serializable
data class OrgQuotaUpdateRequestDto(
    @SerialName("daily_conversation_limit") val dailyConversationLimit: Int?
)

@Serializable
data class OrgQuotaResponseDto(
    @SerialName("org_id") val orgId: String?,
    @SerialName("org_name") val orgName: String?,
    @SerialName("daily_conversation_limit") val dailyConversationLimit: Int?
)

@Serializable
data class QuotaIncreaseRequestResponseDto(
    @SerialName("id") val id: Int?,
    @SerialName("status") val status: String?,
    @SerialName("work_email") val workEmail: String?,
    @SerialName("baseline_limit") val baselineLimit: Int?,
    @SerialName("requested_limit") val requestedLimit: Int?,
    @SerialName("reason") val reason: String?,
    @SerialName("created_at") val createdAt: String?
)

@Serializable
data class SuperAdminListResponseDto(
    @SerialName("super_admins") val superAdmins: List<SuperAdminResponseDto>?
)

@Serializable
data class SuperAdminResponseDto(
    @SerialName("user_id") val userId: String?,
    @SerialName("email") val email: String?
)

@Serializable
data class GrantSuperAdminRequestDto(
    @SerialName("user_id") val userId: String?,
    @SerialName("email") val email: String?
)

@Serializable
data class UserLifecycleResponseDto(
    @SerialName("user_id") val userId: String?,
    @SerialName("email") val email: String?,
    @SerialName("warnings") val warnings: List<String>?
)

@Serializable
data class VerifiedModelDto(
    @SerialName("model_name") val modelName: String?,
    @SerialName("provider") val provider: String?,
    @SerialName("is_enabled") val isEnabled: Boolean?,
    @SerialName("id") val id: Int?,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("updated_at") val updatedAt: String?
)

@Serializable
data class VerifiedModelPageDto(
    @SerialName("items") val items: List<VerifiedModelDto>?,
    @SerialName("next_page_id") val nextPageId: String?
)

@Serializable
data class VerifiedModelCreateDto(
    @SerialName("model_name") val modelName: String?,
    @SerialName("provider") val provider: String?,
    @SerialName("is_enabled") val isEnabled: Boolean?
)

@Serializable
data class VerifiedModelUpdateDto(
    @SerialName("is_enabled") val isEnabled: Boolean?
)

@Serializable
data class DeviceAuthorizationResponseDto(
    @SerialName("device_code") val deviceCode: String?,
    @SerialName("user_code") val userCode: String?,
    @SerialName("verification_uri") val verificationUri: String?,
    @SerialName("verification_uri_complete") val verificationUriComplete: String?,
    @SerialName("expires_in") val expiresIn: Int?,
    @SerialName("interval") val interval: Int?
)

@Serializable
data class JiraUserResponseDto(
    @SerialName("id") val id: Int?,
    @SerialName("keycloak_user_id") val keycloakUserId: String?,
    @SerialName("jira_workspace_id") val jiraWorkspaceId: Int?,
    @SerialName("status") val status: String?,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("updated_at") val updatedAt: String?,
    @SerialName("workspace") val workspace: JiraWorkspaceResponseDto?
)

@Serializable
data class JiraWorkspaceResponseDto(
    @SerialName("id") val id: Int?,
    @SerialName("name") val name: String?,
    @SerialName("jira_cloud_id") val jiraCloudId: String?,
    @SerialName("status") val status: String?,
    @SerialName("editable") val editable: Boolean?,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("updated_at") val updatedAt: String?
)

@Serializable
data class JiraWorkspaceCreateDto(
    @SerialName("workspace_name") val workspaceName: String?,
    @SerialName("webhook_secret") val webhookSecret: String?,
    @SerialName("svc_acc_email") val svcAccEmail: String?,
    @SerialName("svc_acc_api_key") val svcAccApiKey: String?,
    @SerialName("is_active") val isActive: Boolean?
)

@Serializable
data class JiraLinkCreateDto(
    @SerialName("workspace_name") val workspaceName: String?
)

@Serializable
data class JiraInstanceStatusResponseDto(
    @SerialName("configured") val configured: Boolean?,
    @SerialName("host") val host: String?
)

@Serializable
data class JiraValidateWorkspaceResponseDto(
    @SerialName("name") val name: String?,
    @SerialName("status") val status: String?,
    @SerialName("message") val message: String?
)

@Serializable
data class ReinstallWebhookRequestDto(
    @SerialName("resource") val resource: ResourceIdentifierDto?
)

@Serializable
data class ResourceInstallationResultDto(
    @SerialName("resource_id") val resourceId: String?,
    @SerialName("resource_type") val resourceType: String?,
    @SerialName("success") val success: Boolean?,
    @SerialName("error") val error: String?
)

@Serializable
data class ResourceWithWebhookStatusDto(
    @SerialName("id") val id: String?,
    @SerialName("name") val name: String?,
    @SerialName("full_path") val fullPath: String?,
    @SerialName("type") val type: String?,
    @SerialName("webhook_installed") val webhookInstalled: Boolean?,
    @SerialName("webhook_uuid") val webhookUuid: String?,
    @SerialName("last_synced") val lastSynced: String?
)

@Serializable
data class ResourceIdentifierDto(
    @SerialName("type") val type: String?,
    @SerialName("id") val id: String?
)

@Serializable
data class SharedConversationDto(
    @SerialName("id") val id: String?,
    @SerialName("created_by_user_id") val createdByUserId: String?,
    @SerialName("sandbox_id") val sandboxId: String?,
    @SerialName("selected_repository") val selectedRepository: String?,
    @SerialName("selected_branch") val selectedBranch: String?,
    @SerialName("git_provider") val gitProvider: String?,
    @SerialName("title") val title: String?,
    @SerialName("pr_number") val prNumber: List<Int>?,
    @SerialName("llm_model") val llmModel: String?,
    @SerialName("metrics") val metrics: String?,
    @SerialName("parent_conversation_id") val parentConversationId: String?,
    @SerialName("sub_conversation_ids") val subConversationIds: List<String>?,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("updated_at") val updatedAt: String?
)

@Serializable
data class EventPageDto(
    @SerialName("items") val items: List<EventDto>?,
    @SerialName("next_page_id") val nextPageId: String?
)

@Serializable
data class LlmApiKeyResponseDto(
    @SerialName("key") val key: String?
)

@Serializable
data class ByorPermittedResponseDto(
    @SerialName("permitted") val permitted: Boolean?
)

@Serializable
data class ManagedLlmApiKeyRefreshResponseDto(
    @SerialName("refreshed") val refreshed: Boolean?
)

@Serializable
data class StrictLLMDto(
    @SerialName("model") val model: String?,
    @SerialName("api_key") val apiKey: String?,
    @SerialName("provider_connection_id") val providerConnectionId: String?,
    @SerialName("auth_type") val authType: String?,
    @SerialName("subscription_vendor") val subscriptionVendor: String?,
    @SerialName("base_url") val baseUrl: String?,
    @SerialName("api_version") val apiVersion: String?,
    @SerialName("aws_access_key_id") val awsAccessKeyId: String?,
    @SerialName("aws_secret_access_key") val awsSecretAccessKey: String?,
    @SerialName("aws_session_token") val awsSessionToken: String?,
    @SerialName("aws_region_name") val awsRegionName: String?,
    @SerialName("aws_profile_name") val awsProfileName: String?,
    @SerialName("aws_role_name") val awsRoleName: String?,
    @SerialName("aws_session_name") val awsSessionName: String?,
    @SerialName("aws_bedrock_runtime_endpoint") val awsBedrockRuntimeEndpoint: String?,
    @SerialName("openrouter_site_url") val openrouterSiteUrl: String?,
    @SerialName("openrouter_app_name") val openrouterAppName: String?,
    @SerialName("num_retries") val numRetries: Int?,
    @SerialName("retry_multiplier") val retryMultiplier: Double?,
    @SerialName("retry_min_wait") val retryMinWait: Int?,
    @SerialName("retry_max_wait") val retryMaxWait: Int?,
    @SerialName("timeout") val timeout: Int?,
    @SerialName("max_message_chars") val maxMessageChars: Int?,
    @SerialName("temperature") val temperature: Double?,
    @SerialName("top_p") val topP: Double?,
    @SerialName("top_k") val topK: Double?,
    @SerialName("max_input_tokens") val maxInputTokens: Int?,
    @SerialName("max_output_tokens") val maxOutputTokens: Int?,
    @SerialName("model_canonical_name") val modelCanonicalName: String?,
    @SerialName("api_mode") val apiMode: String?,
    @SerialName("capability_overrides") val capabilityOverrides: Map<String, String>?,
    @SerialName("extra_headers") val extraHeaders: Map<String, String>?,
    @SerialName("input_cost_per_token") val inputCostPerToken: Double?,
    @SerialName("output_cost_per_token") val outputCostPerToken: Double?,
    @SerialName("ollama_base_url") val ollamaBaseUrl: String?,
    @SerialName("stream") val stream: Boolean?,
    @SerialName("drop_params") val dropParams: Boolean?,
    @SerialName("modify_params") val modifyParams: Boolean?,
    @SerialName("disable_vision") val disableVision: Boolean?,
    @SerialName("disable_stop_word") val disableStopWord: Boolean?,
    @SerialName("caching_prompt") val cachingPrompt: Boolean?,
    @SerialName("log_completions") val logCompletions: Boolean?,
    @SerialName("log_completions_folder") val logCompletionsFolder: String?,
    @SerialName("custom_tokenizer") val customTokenizer: String?,
    @SerialName("native_tool_calling") val nativeToolCalling: Boolean?,
    @SerialName("force_string_serializer") val forceStringSerializer: Boolean?,
    @SerialName("inline_image_urls") val inlineImageUrls: Boolean?,
    @SerialName("reasoning_effort") val reasoningEffort: String?,
    @SerialName("reasoning_summary") val reasoningSummary: String?,
    @SerialName("enable_encrypted_reasoning") val enableEncryptedReasoning: Boolean?,
    @SerialName("prompt_cache_retention") val promptCacheRetention: String?,
    @SerialName("extended_thinking_budget") val extendedThinkingBudget: Int?,
    @SerialName("seed") val seed: Int?,
    @SerialName("usage_id") val usageId: String?,
    @SerialName("litellm_extra_body") val litellmExtraBody: Map<String, String>?,
    @SerialName("fallback_strategy") val fallbackStrategy: FallbackStrategyDto?
)

@Serializable
data class AgentUsageDataDto(
    @SerialName("agent_name") val agentName: String?,
    @SerialName("conversation_count") val conversationCount: Int?,
    @SerialName("total_cost") val totalCost: Double?
)

@Serializable
data class DailyUsageDataDto(
    @SerialName("date") val date: String?,
    @SerialName("tokens") val tokens: Int?,
    @SerialName("conversations") val conversations: Int?
)

@Serializable
data class ModelUsageDataDto(
    @SerialName("model_name") val modelName: String?,
    @SerialName("conversation_count") val conversationCount: Int?,
    @SerialName("total_tokens") val totalTokens: Int?,
    @SerialName("total_cost") val totalCost: Double?
)

@Serializable
data class TeamUsageDataDto(
    @SerialName("user_id") val userId: String?,
    @SerialName("user_email") val userEmail: String?,
    @SerialName("user_name") val userName: String?,
    @SerialName("conversation_count") val conversationCount: Int?,
    @SerialName("total_tokens") val totalTokens: Int?,
    @SerialName("percentage") val percentage: Double?
)

@Serializable
data class InvitationFailureDto(
    @SerialName("email") val email: String?,
    @SerialName("error") val error: String?
)

@Serializable
data class FallbackStrategyDto(
    @SerialName("fallback_llms") val fallbackLlms: List<String>?,
    @SerialName("profile_store_dir") val profileStoreDir: String?
)

@Serializable
data class OrgBudgetThresholdResponseDto(
    @SerialName("id") val id: Int?,
    @SerialName("percentage") val percentage: Int?,
    @SerialName("email_enabled") val emailEnabled: Boolean?,
    @SerialName("slack_enabled") val slackEnabled: Boolean?
)

@Serializable
data class OrgBudgetThresholdUpdateDto(
    @SerialName("percentage") val percentage: Int?,
    @SerialName("email_enabled") val emailEnabled: Boolean?,
    @SerialName("slack_enabled") val slackEnabled: Boolean?
)

@Serializable
data class CreatePrButtonClickedEventDto(
    @SerialName("event_type") val eventType: String?,
    @SerialName("git_provider") val gitProvider: String?
)

@Serializable
data class AnalyticsEventResponseDto(
    @SerialName("status") val status: String?
)

@Serializable
data class GitLabResourcesResponseDto(
    @SerialName("resources") val resources: List<ResourceWithWebhookStatusDto>?
)

// Webhook payload — discriminated-union sub-objects (workspace/agent) are kept
// untyped; only flat fields common to the app contract are modeled.
@Serializable
data class ConversationInfoDto(
    @SerialName("id") val id: String?,
    @SerialName("persistence_dir") val persistenceDir: String?,
    @SerialName("max_iterations") val maxIterations: Int?,
    @SerialName("stuck_detection") val stuckDetection: Boolean?,
    @SerialName("execution_status") val executionStatus: String?,
    @SerialName("activated_knowledge_skills") val activatedKnowledgeSkills: List<String>?,
    @SerialName("invoked_skills") val invokedSkills: List<String>?,
    @SerialName("blocked_actions") val blockedActions: Map<String, String>?,
    @SerialName("blocked_messages") val blockedMessages: Map<String, String>?,
    @SerialName("last_user_message_id") val lastUserMessageId: String?,
    @SerialName("leaf_event_id") val leafEventId: String?,
    @SerialName("title") val title: String?,
    @SerialName("created_at") val createdAt: String?,
    @SerialName("updated_at") val updatedAt: String?,
    @SerialName("forked_from_conversation_id") val forkedFromConversationId: String?,
    @SerialName("forked_from_event_id") val forkedFromEventId: String?,
    @SerialName("parent_conversation_id") val parentConversationId: String?,
    @SerialName("sub_conversation_ids") val subConversationIds: List<String>?,
    @SerialName("tags") val tags: Map<String, String>?,
    @SerialName("current_model_id") val currentModelId: String?,
    @SerialName("available_models") val availableModels: List<ACPModelInfoDto>?,
    @SerialName("supports_runtime_model_switch") val supportsRuntimeModelSwitch: Boolean?,
    @SerialName("launched_agent_profile") val launchedAgentProfile: LaunchedAgentProfileDto?,
    @SerialName("client_tools") val clientTools: List<ClientToolSpecDto>?,
    @SerialName("stats") val stats: ConversationStatsDto?,
    @SerialName("agent_state") val agentState: String?,
    @SerialName("metrics") val metrics: MetricsFullDto?
    // Spec fields intentionally not modeled (kotlinx.serialization cannot codegen oneOf/discriminated
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
@Serializable
data class ConfirmationResponseRequestDto(
    @SerialName("accept") val accept: Boolean,
    @SerialName("reason") val reason: String = "User rejected the action."
)

/** Runtime POST responses are `{"success": true}`. */
@Serializable
data class RuntimeSuccessDto(
    @SerialName("success") val success: Boolean = true
)

/** Runtime local conversation resource (GET /conversations/{id}). */
@Serializable
data class RuntimeConversationDto(
    @SerialName("id") val id: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("agent_state") val agentState: String? = null,
    @SerialName("execution_status") val executionStatus: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("max_iterations") val maxIterations: Int? = null,
    @SerialName("persistence_dir") val persistenceDir: String? = null,
    @SerialName("current_model_id") val currentModelId: String? = null,
    @SerialName("stats") val stats: ConversationStatsDto? = null
)

/** Body of POST /conversations/{id}/switch_llm. */
@Serializable
data class SwitchLlmRequestDto(
    @SerialName("model") val model: String? = null,
    @SerialName("profile_name") val profileName: String? = null
)

/** Body of POST /conversations/{id}/goal. */
@Serializable
data class StartGoalRequestDto(
    @SerialName("goal") val goal: String
)

/** Body of POST /conversations/{id}/ask_agent. */
@Serializable
data class AskAgentRequestDto(
    @SerialName("question") val question: String
)

/** Body of PATCH /conversations/{id}. */
@Serializable
data class UpdateRuntimeConversationRequestDto(
    @SerialName("title") val title: String? = null,
    @SerialName("public") val public: Boolean? = null,
    @SerialName("tags") val tags: Map<String, String>? = null
)

/** VSCode URL from GET /vscode/url. */
@Serializable
data class VSCodeUrlDto(
    @SerialName("url") val url: String? = null
)

/** GET /conversations/{id}/runtime — where the runtime actually lives. */
@Serializable
data class ConversationRuntimeDto(
    @SerialName("url") val url: String? = null,
    @SerialName("host") val host: String? = null,
    @SerialName("session_api_key") val sessionApiKey: String? = null,
    @SerialName("status") val status: String? = null
)

/** One entry from GET /tools/catalog. */
@Serializable
data class ToolInfoDto(
    @SerialName("name") val name: String = "",
    @SerialName("description") val description: String? = null
)
