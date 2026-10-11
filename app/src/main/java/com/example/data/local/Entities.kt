package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AgentEvent
import com.example.data.model.ConversationUiStatus
import com.example.data.model.OpenHandsConversation
import com.example.data.model.PromptTemplate
import com.example.data.model.StatusMapper

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sandboxStatus: String?,
    val executionStatus: String?,
    val rawStatus: String?,
    val repository: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val cost: Double = 0.0,
    val lastMessage: String? = null,
    val sandboxId: String? = null
) {
    fun toDomain(): OpenHandsConversation = OpenHandsConversation(
        id = id,
        title = title,
        uiStatus = StatusMapper.map(sandboxStatus, executionStatus, rawStatus),
        sandboxStatus = sandboxStatus,
        executionStatus = executionStatus,
        repository = repository,
        createdAt = createdAt,
        updatedAt = updatedAt,
        cost = cost,
        lastMessage = lastMessage,
        sandboxId = sandboxId
    )

    companion object {
        fun fromDomain(model: OpenHandsConversation): ConversationEntity = ConversationEntity(
            id = model.id,
            title = model.title,
            sandboxStatus = model.sandboxStatus,
            executionStatus = model.executionStatus,
            rawStatus = null,
            repository = model.repository,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt,
            cost = model.cost,
            lastMessage = model.lastMessage,
            sandboxId = model.sandboxId
        )
    }
}

@Entity(tableName = "events")
data class AgentEventEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val timestamp: Long,
    val source: String,
    val actionType: String?,
    val content: String,
    val detail: String? = null,
    val isError: Boolean = false
) {
    fun toDomain(): AgentEvent = AgentEvent(
        id = id,
        conversationId = conversationId,
        timestamp = timestamp,
        source = source,
        actionType = actionType,
        content = content,
        detail = detail,
        isError = isError
    )
}

@Entity(tableName = "templates")
data class SavedTemplateEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val prompt: String,
    val defaultRepo: String? = null
) {
    fun toDomain(): PromptTemplate = PromptTemplate(
        id = id,
        title = title,
        category = category,
        prompt = prompt,
        defaultRepo = defaultRepo
    )
}
