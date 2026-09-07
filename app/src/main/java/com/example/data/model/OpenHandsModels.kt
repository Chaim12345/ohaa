package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateConversationRequest(
    @param:Json(name = "initial_message") val initialMessage: InitialMessage,
    @param:Json(name = "selected_repository") val selectedRepository: String? = null
)

@JsonClass(generateAdapter = true)
data class InitialMessage(
    @param:Json(name = "content") val content: List<MessageContent>
)

@JsonClass(generateAdapter = true)
data class MessageContent(
    @param:Json(name = "type") val type: String = "text",
    @param:Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    @param:Json(name = "content") val content: List<MessageContent>
)

@JsonClass(generateAdapter = true)
data class ConversationResponse(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "app_conversation_id") val appConversationId: String? = null,
    @param:Json(name = "conversation_id") val conversationId: String? = null,
    @param:Json(name = "title") val title: String? = null,
    @param:Json(name = "status") val status: String? = null,
    @param:Json(name = "selected_repository") val selectedRepository: String? = null,
    @param:Json(name = "created_at") val createdAt: String? = null,
    @param:Json(name = "updated_at") val updatedAt: String? = null,
    @param:Json(name = "cost") val cost: Double? = null,
    @param:Json(name = "message_count") val messageCount: Int? = null,
    @param:Json(name = "last_message") val lastMessage: String? = null
) {
    val resolvedId: String
        get() = id ?: appConversationId ?: conversationId ?: "unknown"
}

@JsonClass(generateAdapter = true)
data class ConversationListResponse(
    @param:Json(name = "conversations") val conversations: List<ConversationResponse>? = null,
    @param:Json(name = "items") val items: List<ConversationResponse>? = null,
    @param:Json(name = "total") val total: Int? = null
)

@JsonClass(generateAdapter = true)
data class EventResponse(
    @param:Json(name = "id") val id: String? = null,
    @param:Json(name = "timestamp") val timestamp: String? = null,
    @param:Json(name = "source") val source: String? = null,
    @param:Json(name = "action") val action: String? = null,
    @param:Json(name = "thought") val thought: String? = null,
    @param:Json(name = "observation") val observation: String? = null,
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "event_type") val eventType: String? = null,
    @param:Json(name = "type") val type: String? = null,
    @param:Json(name = "args") val args: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class ServiceHealth(
    @param:Json(name = "status") val status: String? = "ok",
    @param:Json(name = "version") val version: String? = null,
    @param:Json(name = "healthy") val healthy: Boolean? = true
)

// UI / Domain model
data class OpenHandsConversation(
    val id: String,
    val title: String,
    val status: ConversationStatus,
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
    ERROR("Error"),
    UNKNOWN("Unknown");

    companion object {
        fun fromString(value: String?): ConversationStatus {
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
    val actionType: String?, // "bash", "edit", "browse", "view", "thought", "message"
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
