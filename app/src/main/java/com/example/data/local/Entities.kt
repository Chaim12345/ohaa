package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AgentEvent
import com.example.data.model.ConversationStatus
import com.example.data.model.OpenHandsConversation
import com.example.data.model.PromptTemplate

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val status: String,
    val selectedRepository: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val cost: Double = 0.0,
    val messageCount: Int = 1,
    val lastMessage: String? = null
) {
    fun toDomain(): OpenHandsConversation {
        return OpenHandsConversation(
            id = id,
            title = title,
            status = ConversationStatus.fromString(status),
            repository = selectedRepository,
            createdAt = createdAt,
            updatedAt = updatedAt,
            cost = cost,
            messageCount = messageCount,
            lastMessage = lastMessage
        )
    }

    companion object {
        fun fromDomain(model: OpenHandsConversation): ConversationEntity {
            return ConversationEntity(
                id = model.id,
                title = model.title,
                status = model.status.name,
                selectedRepository = model.repository,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt,
                cost = model.cost,
                messageCount = model.messageCount,
                lastMessage = model.lastMessage
            )
        }
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
    val detail: String?,
    val isError: Boolean = false
) {
    fun toDomain(): AgentEvent {
        return AgentEvent(
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

    companion object {
        fun fromDomain(domain: AgentEvent): AgentEventEntity {
            return AgentEventEntity(
                id = domain.id,
                conversationId = domain.conversationId,
                timestamp = domain.timestamp,
                source = domain.source,
                actionType = domain.actionType,
                content = domain.content,
                detail = domain.detail,
                isError = domain.isError
            )
        }
    }
}

@Entity(tableName = "templates")
data class SavedTemplateEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val prompt: String,
    val defaultRepo: String?
) {
    fun toDomain(): PromptTemplate {
        return PromptTemplate(
            id = id,
            title = title,
            category = category,
            prompt = prompt,
            defaultRepo = defaultRepo
        )
    }

    companion object {
        fun fromDomain(domain: PromptTemplate): SavedTemplateEntity {
            return SavedTemplateEntity(
                id = domain.id,
                title = domain.title,
                category = domain.category,
                prompt = domain.prompt,
                defaultRepo = domain.defaultRepo
            )
        }
    }
}
