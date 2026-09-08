package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateConversationRequest(
    @field:Json(name = "initial_message") val initialMessage: InitialMessage,
    @field:Json(name = "selected_repository") val selectedRepository: String? = null,
    @field:Json(name = "observability_span_name") val observabilitySpanName: String? = null,
    @field:Json(name = "observability_tags") val observabilityTags: List<String>? = null,
    @field:Json(name = "observability_metadata") val observabilityMetadata: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class InitialMessage(
    @field:Json(name = "content") val content: List<MessageContent>
)

@JsonClass(generateAdapter = true)
data class MessageContent(
    @field:Json(name = "type") val type: String = "text",
    @field:Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    @field:Json(name = "content") val content: List<MessageContent>
)

@JsonClass(generateAdapter = true)
data class ConversationResponse(
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "app_conversation_id") val appConversationId: String? = null,
    @field:Json(name = "conversation_id") val conversationId: String? = null,
    @field:Json(name = "title") val title: String? = null,
    @field:Json(name = "status") val status: String? = null,
    @field:Json(name = "sandbox_status") val sandboxStatus: String? = null,
    @field:Json(name = "execution_status") val executionStatus: String? = null,
    @field:Json(name = "selected_repository") val selectedRepository: String? = null,
    @field:Json(name = "created_at") val createdAt: String? = null,
    @field:Json(name = "updated_at") val updatedAt: String? = null,
    @field:Json(name = "cost") val cost: Double? = null,
    @field:Json(name = "message_count") val messageCount: Int? = null,
    @field:Json(name = "last_message") val lastMessage: String? = null,
    @field:Json(name = "sandbox_id") val sandboxId: String? = null,
    @field:Json(name = "error") val error: String? = null
) {
    val resolvedId: String
        get() = appConversationId ?: id ?: conversationId ?: "unknown"
}

@JsonClass(generateAdapter = true)
data class StartTaskResponse(
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "status") val status: String? = null,
    @field:Json(name = "app_conversation_id") val appConversationId: String? = null,
    @field:Json(name = "sandbox_id") val sandboxId: String? = null,
    @field:Json(name = "created_at") val createdAt: String? = null,
    @field:Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class ConversationListResponse(
    @field:Json(name = "conversations") val conversations: List<ConversationResponse>? = null,
    @field:Json(name = "items") val items: List<ConversationResponse>? = null,
    @field:Json(name = "total") val total: Int? = null,
    @field:Json(name = "next_page_id") val nextPageId: String? = null
)

@JsonClass(generateAdapter = true)
data class EventSearchResponse(
    @field:Json(name = "items") val items: List<EventResponse>? = null,
    @field:Json(name = "next_page_id") val nextPageId: String? = null
)

@JsonClass(generateAdapter = true)
data class EventCountResponse(
    @field:Json(name = "count") val count: Int? = null
)

@JsonClass(generateAdapter = true)
data class EventResponse(
    @field:Json(name = "id") val id: String? = null,
    @field:Json(name = "timestamp") val timestamp: String? = null,
    @field:Json(name = "source") val source: String? = null,
    @field:Json(name = "kind") val kind: String? = null,
    @field:Json(name = "code") val code: String? = null,
    @field:Json(name = "detail") val detail: String? = null,
    @field:Json(name = "parent_id") val parentId: String? = null,
    @field:Json(name = "action") val action: String? = null,
    @field:Json(name = "thought") val thought: String? = null,
    @field:Json(name = "observation") val observation: String? = null,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "event_type") val eventType: String? = null,
    @field:Json(name = "type") val type: String? = null
)

@JsonClass(generateAdapter = true)
data class ServiceHealth(
    @field:Json(name = "status") val status: String? = "ok",
    @field:Json(name = "version") val version: String? = null,
    @field:Json(name = "healthy") val healthy: Boolean? = true
)

// UI / Domain model
data class OpenHandsConversation(
    val id: String,
    val title: String,
    val status: ConversationStatus,
    val sandboxStatus: String? = null,
    val executionStatus: String? = null,
    val repository: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val cost: Double,
    val messageCount: Int,
    val lastMessage: String?
)

enum class ConversationStatus(val label: String) {
    WORKING("Working"),
    WAITING_FOR_SANDBOX("Starting Sandbox"),
    PREPARING_REPOSITORY("Preparing Repo"),
    SETTING_UP_SKILLS("Configuring Skills"),
    READY("Ready"),
    RUNNING("Running"),
    FINISHED("Finished"),
    PAUSED("Paused"),
    WAITING_FOR_CONFIRMATION("Waiting for User"),
    STUCK("Stuck"),
    ERROR("Error"),
    UNKNOWN("Unknown");

    companion object {
        fun fromString(value: String?, sandboxStatus: String? = null, executionStatus: String? = null): ConversationStatus {
            if (executionStatus != null) {
                when (executionStatus.lowercase().trim()) {
                    "finished" -> return FINISHED
                    "running" -> return RUNNING
                    "paused" -> return PAUSED
                    "waiting_for_confirmation" -> return WAITING_FOR_CONFIRMATION
                    "stuck" -> return STUCK
                    "error" -> return ERROR
                    "idle" -> return READY
                }
            }
            if (sandboxStatus != null) {
                when (sandboxStatus.uppercase().trim()) {
                    "STARTING" -> return WAITING_FOR_SANDBOX
                    "RUNNING" -> if (value == null) return READY
                    "PAUSED" -> return PAUSED
                    "ERROR", "MISSING" -> return ERROR
                }
            }
            if (value == null) return UNKNOWN
            return when (value.uppercase().trim()) {
                "WORKING" -> WORKING
                "WAITING_FOR_SANDBOX" -> WAITING_FOR_SANDBOX
                "PREPARING_REPOSITORY" -> PREPARING_REPOSITORY
                "SETTING_UP_SKILLS" -> SETTING_UP_SKILLS
                "READY" -> READY
                "RUNNING" -> RUNNING
                "FINISHED", "COMPLETED", "SUCCESS" -> FINISHED
                "PAUSED" -> PAUSED
                "WAITING_FOR_CONFIRMATION" -> WAITING_FOR_CONFIRMATION
                "STUCK" -> STUCK
                "ERROR", "FAILED" -> ERROR
                else -> UNKNOWN
            }
        }
    }
}

data class AgentEvent(
    val id: String,
    val conversationId: String,
    val timestamp: Long,
    val source: String, // "user", "agent", "environment", "system"
    val actionType: String?, // "bash", "edit", "browse", "view", "thought", "message", "ServerErrorEvent", "ActionEvent", etc.
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
