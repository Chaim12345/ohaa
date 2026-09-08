package com.example.data.model

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/*
 * UI-facing domain models. These are what the ViewModel and Compose screens consume.
 */

// ---------------------------------------------------------------------------
// Status enums (mirror the server enums, plus UNKNOWN for forward-compat)
// ---------------------------------------------------------------------------

/** SandboxStatus */
enum class SandboxStatus(val label: String) {
    STARTING("Starting"),
    RUNNING("Running"),
    PAUSED("Paused"),
    ERROR("Error"),
    MISSING("Missing"),
    UNKNOWN("Unknown");

    companion object {
        fun from(value: String?): SandboxStatus =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: UNKNOWN
    }
}

/** ConversationExecutionStatus */
enum class ExecutionStatus(val label: String) {
    IDLE("Idle"),
    RUNNING("Running"),
    PAUSED("Paused"),
    WAITING_FOR_CONFIRMATION("Needs confirmation"),
    FINISHED("Finished"),
    ERROR("Error"),
    STUCK("Stuck"),
    DELETING("Deleting"),
    UNKNOWN("Unknown");

    val isTerminal: Boolean
        get() = this == FINISHED || this == ERROR || this == STUCK || this == WAITING_FOR_CONFIRMATION

    companion object {
        fun from(value: String?): ExecutionStatus =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: UNKNOWN
    }
}

/** AppConversationStartTaskStatus */
enum class StartTaskStatus(val label: String, val step: Int) {
    WORKING("Queued", 0),
    WAITING_FOR_SANDBOX("Provisioning sandbox", 1),
    PREPARING_REPOSITORY("Cloning repository", 2),
    RUNNING_SETUP_SCRIPT("Running setup script", 3),
    SETTING_UP_GIT_HOOKS("Installing git hooks", 4),
    SETTING_UP_SKILLS("Loading skills", 5),
    STARTING_CONVERSATION("Starting agent", 6),
    READY("Ready", 7),
    ERROR("Failed", 7),
    UNKNOWN("Working", 0);

    val isTerminal: Boolean get() = this == READY || this == ERROR

    companion object {
        const val MAX_STEP = 7
        fun from(value: String?): StartTaskStatus =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: UNKNOWN
    }
}

/**
 * Single status shown on list cards / badges. Combines sandbox + execution status the way the
 * OpenHands web UI does: sandbox lifecycle wins while it is not RUNNING, otherwise execution status.
 */
enum class ConversationStatus(val label: String) {
    STARTING("Starting"),
    RUNNING("Running"),
    WAITING_FOR_CONFIRMATION("Needs you"),
    IDLE("Idle"),
    FINISHED("Finished"),
    PAUSED("Paused"),
    STUCK("Stuck"),
    ERROR("Error"),
    UNKNOWN("Unknown");

    val isActive: Boolean get() = this == RUNNING || this == STARTING

    companion object {
        fun derive(sandbox: SandboxStatus, execution: ExecutionStatus): ConversationStatus {
            return when (sandbox) {
                SandboxStatus.STARTING -> STARTING
                SandboxStatus.PAUSED -> PAUSED
                SandboxStatus.ERROR -> ERROR
                SandboxStatus.MISSING -> when (execution) {
                    ExecutionStatus.FINISHED -> FINISHED
                    ExecutionStatus.ERROR -> ERROR
                    else -> PAUSED
                }
                SandboxStatus.RUNNING, SandboxStatus.UNKNOWN -> when (execution) {
                    ExecutionStatus.RUNNING -> RUNNING
                    ExecutionStatus.WAITING_FOR_CONFIRMATION -> WAITING_FOR_CONFIRMATION
                    ExecutionStatus.IDLE -> IDLE
                    ExecutionStatus.FINISHED -> FINISHED
                    ExecutionStatus.PAUSED -> PAUSED
                    ExecutionStatus.STUCK -> STUCK
                    ExecutionStatus.ERROR -> ERROR
                    ExecutionStatus.DELETING -> UNKNOWN
                    ExecutionStatus.UNKNOWN -> if (sandbox == SandboxStatus.RUNNING) IDLE else UNKNOWN
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Conversation
// ---------------------------------------------------------------------------

data class Conversation(
    val id: String,
    val title: String,
    val repository: String?,
    val branch: String?,
    val gitProvider: String?,
    val sandboxId: String?,
    val sandboxStatus: SandboxStatus,
    val executionStatus: ExecutionStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val cost: Double,
    val promptTokens: Long,
    val completionTokens: Long,
    val llmModel: String?,
    val conversationUrl: String?,
    val isPublic: Boolean,
    val parentConversationId: String?,
    val prNumbers: List<Int>,
    val trigger: String?
) {
    val status: ConversationStatus get() = ConversationStatus.derive(sandboxStatus, executionStatus)
    val displayTitle: String get() = title.ifBlank { "Conversation ${id.take(8)}" }
    val webUrl: String? get() = conversationUrl
}

/** Progress of POST /api/v1/app-conversations → GET /start-tasks polling. */
sealed class StartTaskProgress {
    abstract val taskId: String?

    data class Submitting(override val taskId: String? = null) : StartTaskProgress()
    data class InProgress(
        override val taskId: String,
        val status: StartTaskStatus,
        val detail: String?
    ) : StartTaskProgress()
    data class Ready(
        override val taskId: String,
        val conversationId: String,
        val sandboxId: String?
    ) : StartTaskProgress()
    data class Failed(override val taskId: String?, val message: String) : StartTaskProgress()
}

// ---------------------------------------------------------------------------
// Events
// ---------------------------------------------------------------------------

/** Server `kind` discriminator of an Event. */
enum class EventKind(val wire: String, val isNoise: Boolean = false) {
    MESSAGE("MessageEvent"),
    ACTION("ActionEvent"),
    OBSERVATION("ObservationEvent"),
    AGENT_ERROR("AgentErrorEvent"),
    SERVER_ERROR("ServerErrorEvent"),
    CONVERSATION_ERROR("ConversationErrorEvent"),
    USER_REJECT("UserRejectObservation"),
    PAUSE("PauseEvent"),
    INTERRUPT("InterruptEvent"),
    ACP_TOOL_CALL("ACPToolCallEvent"),
    HOOK_EXECUTION("HookExecutionEvent", isNoise = true),
    CONDENSATION("Condensation", isNoise = true),
    CONDENSATION_REQUEST("CondensationRequest", isNoise = true),
    CONDENSATION_SUMMARY("CondensationSummaryEvent", isNoise = true),
    STATE_UPDATE("ConversationStateUpdateEvent", isNoise = true),
    SYSTEM_PROMPT("SystemPromptEvent", isNoise = true),
    STREAMING_DELTA("StreamingDeltaEvent", isNoise = true),
    TOKEN("TokenEvent", isNoise = true),
    LLM_COMPLETION_LOG("LLMCompletionLogEvent", isNoise = true),
    UNKNOWN("", isNoise = false);

    companion object {
        fun from(wire: String?): EventKind = entries.firstOrNull { it.wire == wire } ?: UNKNOWN
    }
}

/** How the event card should be rendered. */
enum class EventCategory {
    USER_MESSAGE,
    AGENT_MESSAGE,
    THOUGHT,
    COMMAND,
    FILE_EDIT,
    FILE_READ,
    BROWSER,
    TOOL,
    OBSERVATION,
    FINISH,
    ERROR,
    CONTROL,
    SYSTEM
}

data class AgentEvent(
    val id: String,
    val conversationId: String,
    val timestamp: Long,
    val timestampIso: String?,
    val kind: EventKind,
    val category: EventCategory,
    val source: String,
    val title: String,
    /** Free text (message body, thought, error detail). */
    val body: String?,
    /** Monospace block (command, output, diff, patch, file content). */
    val code: String?,
    val codeLabel: String?,
    val isError: Boolean,
    val toolName: String?,
    val actionId: String?,
    val toolCallId: String?,
    val filePath: String?,
    val exitCode: Int?,
    val securityRisk: String?
) {
    val isNoise: Boolean get() = kind.isNoise
    val isFromUser: Boolean get() = source.equals("user", ignoreCase = true)
    /** ACP tool calls arrive twice (pending, then completed/failed) under the same tool_call_id. */
    val isPendingToolCall: Boolean get() = kind == EventKind.ACP_TOOL_CALL && codeLabel == PENDING_LABEL

    companion object {
        const val PENDING_LABEL = "pending"
    }
}

// ---------------------------------------------------------------------------
// Sandbox / Git / Account
// ---------------------------------------------------------------------------

data class Sandbox(
    val id: String,
    val specId: String?,
    val status: SandboxStatus,
    val statusDetail: String?,
    val exposedUrls: List<Pair<String, String>>,
    val createdAt: Long
)

data class GitRepository(
    val fullName: String,
    val provider: String?,
    val isPublic: Boolean,
    val mainBranch: String?,
    val stars: Int?,
    val pushedAt: Long?
)

data class GitBranch(val name: String, val commitSha: String?, val isProtected: Boolean)

data class SuggestedTask(val repo: String, val issueNumber: Int, val title: String, val taskType: String?, val provider: String?)

data class GitChange(val path: String, val status: String) {
    /** The runtime reports UPDATED/ADDED/DELETED/RENAMED/UNTRACKED; git porcelain letters are accepted too. */
    val statusLabel: String
        get() = when (status.uppercase()) {
            "M", "UPDATED", "MODIFIED" -> "Modified"
            "A", "ADDED" -> "Added"
            "D", "DELETED" -> "Deleted"
            "R", "RENAMED" -> "Renamed"
            "U", "??", "UNTRACKED" -> "Untracked"
            else -> status.lowercase().replaceFirstChar { it.uppercase() }
        }
}

data class GitDiff(val path: String, val original: String, val modified: String)

data class CustomSecret(val name: String, val description: String?)

data class LlmProfile(val name: String, val model: String?, val baseUrl: String?, val apiKeySet: Boolean, val isActive: Boolean)

data class Skill(val name: String, val type: String?, val source: String?, val triggers: List<String>, val content: String?)

data class AccountInfo(
    val email: String?,
    val userId: String?,
    val orgName: String?,
    val orgId: String?,
    val role: String?,
    val gitUserName: String?,
    val apiKeyName: String?,
    val apiKeyAuthType: String?,
    val credits: String?,
    val maxBudgetPerTask: Double?,
    val v1Enabled: Boolean?
)

/** Local-only prompt template (not part of the cloud API). */
data class PromptTemplate(
    val id: String,
    val title: String,
    val category: String,
    val prompt: String,
    val defaultRepo: String?
)

// ---------------------------------------------------------------------------
// Time helpers
// ---------------------------------------------------------------------------

object IsoTime {
    /** Parses an ISO-8601 timestamp (with or without offset) into epoch millis. Returns null if unparseable. */
    fun toEpochMillis(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(value).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            try {
                Instant.parse(value).toEpochMilli()
            } catch (_: DateTimeParseException) {
                try {
                    // Naive datetime without zone, e.g. "2026-09-08T07:31:29.652635" – treat as UTC.
                    OffsetDateTime.parse(value + "Z").toInstant().toEpochMilli()
                } catch (_: DateTimeParseException) {
                    null
                }
            }
        }
    }

    fun fromEpochMillis(millis: Long): String = Instant.ofEpochMilli(millis).toString()
}
