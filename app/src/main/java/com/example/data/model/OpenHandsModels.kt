package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ---------------------------------------------------------------------------
// Wire DTOs — mirror docs/API_SPEC.md (OpenHands Cloud v1)
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class TextContentDto(
    @param:Json(name = "type") val type: String = "text",
    @param:Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class CreateConversationRequestDto(
    @param:Json(name = "initial_message") val initialMessage: InitialMessageDto,
    @param:Json(name = "selected_repository") val selectedRepository: String? = null
)

@JsonClass(generateAdapter = true)
data class InitialMessageDto(
    @param:Json(name = "content") val content: List<TextContentDto>
)

@JsonClass(generateAdapter = true)
data class SendMessageRequestDto(
    @param:Json(name = "content") val content: List<TextContentDto>
)

@JsonClass(generateAdapter = true)
data class ServiceHealthDto(
    @param:Json(name = "status") val status: String? = null
)

/** Response of POST /api/v1/app-conversations — a *start task*, not the conversation. */
@JsonClass(generateAdapter = true)
data class StartTaskDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "app_conversation_id") val appConversationId: String? = null,
    @param:Json(name = "sandbox_id") val sandboxId: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class StartTaskListDto(
    @param:Json(name = "tasks") val tasks: List<StartTaskDto>? = null
)

@JsonClass(generateAdapter = true)
data class ConversationDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "app_conversation_id") val appConversationId: String? = null,
    @param:Json(name = "conversation_id") val conversationId: String? = null,
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "sandbox_status") val sandboxStatus: String? = null,
    @param:Json(name = "execution_status") val executionStatus: String? = null,
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "selected_repository") val selectedRepository: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "updated_at") val updatedAt: String? = null,
    @param:Json(name = "cost") val cost: Double? = null,
    @param:Json(name = "last_message") val lastMessage: String? = null
) {
    val resolvedId: String
        get() = id ?: appConversationId ?: conversationId ?: ""
}

@JsonClass(generateAdapter = true)
data class ConversationListDto(
    @param:Json(name = "conversations") val conversations: List<ConversationDto>? = null,
    @param:Json(name = "items") val items: List<ConversationDto>? = null,
    @param:Json(name = "results") val results: List<ConversationDto>? = null
)

@JsonClass(generateAdapter = true)
data class EventDto(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "kind") val kind: String? = null,
    @param:Json(name = "type") val type: String? = null,
    @param:Json(name = "action") val action: String? = null,
    @param:Json(name = "source") val source: String? = null,
    @param:Json(name = "thought") val thought: String? = null,
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "observation") val observation: String? = null,
    @param:Json(name = "content") val content: String? = null,
    @param:Json(name = "timestamp") val timestamp: String? = null,
    @param:Json(name = "args") val args: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class EventsPageDto(
    @param:Json(name = "events") val events: List<EventDto>? = null,
    @param:Json(name = "items") val items: List<EventDto>? = null,
    @param:Json(name = "next_page_id") val nextPageId: String? = null
)

@JsonClass(generateAdapter = true)
data class CreditsDto(
    @param:Json(name = "credits") val credits: Double? = null
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
