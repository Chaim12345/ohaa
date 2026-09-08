package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.AgentEvent
import com.example.data.model.Conversation
import com.example.data.model.EventCategory
import com.example.data.model.EventKind
import com.example.data.model.ExecutionStatus
import com.example.data.model.PromptTemplate
import com.example.data.model.SandboxStatus

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val selectedRepository: String?,
    val selectedBranch: String?,
    val gitProvider: String?,
    val sandboxId: String?,
    val sandboxStatus: String,
    val executionStatus: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val cost: Double = 0.0,
    val promptTokens: Long = 0,
    val completionTokens: Long = 0,
    val llmModel: String? = null,
    val conversationUrl: String? = null,
    val isPublic: Boolean = false,
    val parentConversationId: String? = null,
    val prNumbers: String = "",
    val trigger: String? = null,
    /** Local bookkeeping: when this row was last synced from the server. */
    val syncedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Conversation = Conversation(
        id = id,
        title = title,
        repository = selectedRepository,
        branch = selectedBranch,
        gitProvider = gitProvider,
        sandboxId = sandboxId,
        sandboxStatus = SandboxStatus.from(sandboxStatus),
        executionStatus = ExecutionStatus.from(executionStatus),
        createdAt = createdAt,
        updatedAt = updatedAt,
        cost = cost,
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        llmModel = llmModel,
        conversationUrl = conversationUrl,
        isPublic = isPublic,
        parentConversationId = parentConversationId,
        prNumbers = prNumbers.split(',').mapNotNull { it.trim().toIntOrNull() },
        trigger = trigger
    )
}

@Entity(
    tableName = "events",
    indices = [Index(value = ["conversationId", "timestamp"])]
)
data class AgentEventEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val timestamp: Long,
    val timestampIso: String?,
    val kind: String,
    val category: String,
    val source: String,
    val title: String,
    val body: String?,
    val code: String?,
    val codeLabel: String?,
    val isError: Boolean = false,
    val toolName: String?,
    val actionId: String?,
    val toolCallId: String?,
    val filePath: String?,
    val exitCode: Int?,
    val securityRisk: String?
) {
    fun toDomain(): AgentEvent = AgentEvent(
        id = id,
        conversationId = conversationId,
        timestamp = timestamp,
        timestampIso = timestampIso,
        kind = EventKind.entries.firstOrNull { it.name == kind } ?: EventKind.UNKNOWN,
        category = EventCategory.entries.firstOrNull { it.name == category } ?: EventCategory.SYSTEM,
        source = source,
        title = title,
        body = body,
        code = code,
        codeLabel = codeLabel,
        isError = isError,
        toolName = toolName,
        actionId = actionId,
        toolCallId = toolCallId,
        filePath = filePath,
        exitCode = exitCode,
        securityRisk = securityRisk
    )

    companion object {
        fun fromDomain(e: AgentEvent): AgentEventEntity = AgentEventEntity(
            id = e.id,
            conversationId = e.conversationId,
            timestamp = e.timestamp,
            timestampIso = e.timestampIso,
            kind = e.kind.name,
            category = e.category.name,
            source = e.source,
            title = e.title,
            body = e.body,
            code = e.code,
            codeLabel = e.codeLabel,
            isError = e.isError,
            toolName = e.toolName,
            actionId = e.actionId,
            toolCallId = e.toolCallId,
            filePath = e.filePath,
            exitCode = e.exitCode,
            securityRisk = e.securityRisk
        )
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
    fun toDomain(): PromptTemplate = PromptTemplate(id, title, category, prompt, defaultRepo)

    companion object {
        fun fromDomain(t: PromptTemplate) = SavedTemplateEntity(t.id, t.title, t.category, t.prompt, t.defaultRepo)
    }
}
