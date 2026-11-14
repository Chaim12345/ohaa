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
    val selectedBranch: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val conversationUrl: String? = null,
    val sandboxId: String? = null,
    /** Raw `sandbox_status` (STARTING/RUNNING/PAUSED/ERROR/MISSING) for lifecycle decisions. */
    val sandboxStatus: String? = null,
    /** Agent-server key for runtime calls (`X-Session-API-Key`). Sensitive. */
    val sessionApiKey: String? = null,
    /** "openhands" or "acp". Drives which switch endpoint the UI uses. */
    val agentKind: String? = null,
    val llmModel: String? = null,
    val acpServer: String? = null,
    val acpSupportsRuntimeModelSwitch: Boolean = false,
    /** Comma-free JSON array of ACPModelInfo as returned by the server. */
    val acpAvailableModelsJson: String? = null,
    val executionStatus: String? = null,
) {
    fun toDomain(): OpenHandsConversation {
        return OpenHandsConversation(
            id = id,
            title = title,
            status = ConversationStatus.fromName(status),
            repository = selectedRepository,
            branch = selectedBranch,
            createdAt = createdAt,
            updatedAt = updatedAt,
            conversationUrl = conversationUrl,
            sandboxId = sandboxId,
            sandboxStatus = sandboxStatus,
            sessionApiKey = sessionApiKey,
            agentKind = agentKind,
            llmModel = llmModel,
            acpServer = acpServer,
            acpSupportsRuntimeModelSwitch = acpSupportsRuntimeModelSwitch,
            acpAvailableModelsJson = acpAvailableModelsJson,
            executionStatus = executionStatus,
        )
    }

    companion object {
        fun fromDomain(model: OpenHandsConversation): ConversationEntity {
            return ConversationEntity(
                id = model.id,
                title = model.title,
                status = model.status.name,
                selectedRepository = model.repository,
                selectedBranch = model.branch,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt,
                conversationUrl = model.conversationUrl,
                sandboxId = model.sandboxId,
                sandboxStatus = model.sandboxStatus,
                sessionApiKey = model.sessionApiKey,
                agentKind = model.agentKind,
                llmModel = model.llmModel,
                acpServer = model.acpServer,
                acpSupportsRuntimeModelSwitch = model.acpSupportsRuntimeModelSwitch,
                acpAvailableModelsJson = model.acpAvailableModelsJson,
                executionStatus = model.executionStatus,
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
    val isError: Boolean = false,
    /**
     * Raw ISO-8601 wire timestamp. Millisecond [timestamp] collides for events
     * emitted in the same second, so this lexicographic key breaks ties in the
     * original chronological order.
     */
    val sortKey: String = ""
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
