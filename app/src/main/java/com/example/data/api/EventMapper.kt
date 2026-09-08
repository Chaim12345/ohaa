package com.example.data.api

import com.example.data.model.AgentEvent
import com.example.data.model.EventCategory
import com.example.data.model.EventDto
import com.example.data.model.EventKind
import com.example.data.model.IsoTime
import com.example.data.model.TextContentDto

/**
 * Turns the raw, kind-discriminated event union into a flat [AgentEvent] the UI can render.
 * Tool payloads (`action` / `observation`) are inspected by their own `kind` discriminator
 * (e.g. TerminalAction, FileEditorObservation) so we don't need one Moshi adapter per tool.
 */
object EventMapper {

    fun map(dto: EventDto, conversationId: String, fallbackTimestamp: Long = System.currentTimeMillis()): AgentEvent {
        val kind = EventKind.from(dto.kind)
        val source = dto.source ?: defaultSource(kind)
        val ts = IsoTime.toEpochMillis(dto.timestamp) ?: fallbackTimestamp
        val base = AgentEvent(
            id = dto.id,
            conversationId = conversationId,
            timestamp = ts,
            timestampIso = dto.timestamp,
            kind = kind,
            category = EventCategory.SYSTEM,
            source = source,
            title = dto.kind.ifBlank { "Event" },
            body = null,
            code = null,
            codeLabel = null,
            isError = false,
            toolName = dto.toolName,
            actionId = dto.actionId,
            toolCallId = dto.toolCallId,
            filePath = null,
            exitCode = null,
            securityRisk = dto.securityRisk
        )

        return when (kind) {
            EventKind.MESSAGE -> mapMessage(dto, base)
            EventKind.ACTION -> mapAction(dto, base)
            EventKind.OBSERVATION -> mapObservation(dto, base)
            EventKind.AGENT_ERROR -> base.copy(
                category = EventCategory.ERROR,
                title = "Agent error" + (dto.toolName?.let { " in $it" } ?: ""),
                body = dto.error ?: dto.detail,
                isError = true
            )
            EventKind.SERVER_ERROR, EventKind.CONVERSATION_ERROR -> base.copy(
                category = EventCategory.ERROR,
                title = dto.code?.ifBlank { null } ?: "Server error",
                body = dto.detail,
                isError = true
            )
            EventKind.USER_REJECT -> base.copy(
                category = EventCategory.CONTROL,
                title = "Action rejected",
                body = dto.rejectionReason ?: "User rejected the action"
            )
            EventKind.PAUSE -> base.copy(category = EventCategory.CONTROL, title = "Paused")
            EventKind.INTERRUPT -> base.copy(category = EventCategory.CONTROL, title = "Interrupted")
            EventKind.ACP_TOOL_CALL -> mapAcpToolCall(dto, base)
            EventKind.SYSTEM_PROMPT -> base.copy(
                category = EventCategory.SYSTEM,
                title = "System prompt",
                code = dto.systemPrompt?.text?.take(4000),
                codeLabel = "system prompt"
            )
            EventKind.STATE_UPDATE -> base.copy(
                category = EventCategory.SYSTEM,
                title = "State: ${dto.key ?: "update"}",
                code = dto.value?.let { prettify(it) }?.take(2000),
                codeLabel = "state"
            )
            EventKind.CONDENSATION, EventKind.CONDENSATION_REQUEST, EventKind.CONDENSATION_SUMMARY -> base.copy(
                category = EventCategory.SYSTEM,
                title = "Context condensed",
                body = dto.summary ?: dto.forgottenEventIds?.let { "${it.size} earlier events summarized" }
            )
            EventKind.STREAMING_DELTA -> base.copy(
                category = EventCategory.SYSTEM,
                title = "Streaming",
                body = (dto.content as? String) ?: dto.reasoningContent
            )
            EventKind.TOKEN, EventKind.LLM_COMPLETION_LOG, EventKind.HOOK_EXECUTION -> base.copy(
                category = EventCategory.SYSTEM,
                title = humanize(dto.kind)
            )
            EventKind.UNKNOWN -> base.copy(
                category = EventCategory.SYSTEM,
                title = dto.kind.ifBlank { "Unknown event" },
                body = dto.summary ?: dto.detail ?: dto.error
            )
        }
    }

    // ------------------------------------------------------------------ message

    private fun mapMessage(dto: EventDto, base: AgentEvent): AgentEvent {
        val message = dto.llmMessage
        val text = joinContent(message?.get("content"))
        val images = countImages(message?.get("content"))
        val body = buildString {
            append(text)
            if (images > 0) {
                if (isNotEmpty()) append("\n")
                append("[$images image${if (images > 1) "s" else ""} attached]")
            }
        }.ifBlank { null }
        val fromUser = base.source.equals("user", ignoreCase = true)
        return base.copy(
            category = if (fromUser) EventCategory.USER_MESSAGE else EventCategory.AGENT_MESSAGE,
            title = if (fromUser) "You" else "Agent",
            body = body ?: (if (fromUser) "(empty message)" else null)
        )
    }

    // ------------------------------------------------------------------- action

    private fun mapAction(dto: EventDto, base: AgentEvent): AgentEvent {
        val action = dto.action ?: emptyMap()
        val actionKind = action["kind"] as? String ?: ""
        // Most agents fill `thought`; reasoning models put their chain-of-thought in `reasoning_content` instead.
        val thought = dto.thought?.joinToString("\n") { it.text }?.ifBlank { null }
            ?: dto.reasoningContent?.trim()?.ifEmpty { null }?.take(MAX_REASONING_CHARS)
        val toolName = dto.toolName ?: actionKind.removeSuffix("Action").ifBlank { null }
        val summary = dto.summary?.ifBlank { null }
        val risk = dto.securityRisk?.takeIf { it != "UNKNOWN" && it != "LOW" }
        val b = base.copy(toolName = toolName, securityRisk = risk)

        return when {
            actionKind == "TerminalAction" || toolName.equals("terminal", true) || toolName.equals("execute_bash", true) -> {
                val cmd = action["command"] as? String
                val isInput = action["is_input"] == true
                b.copy(
                    category = EventCategory.COMMAND,
                    title = if (isInput) "Sent input to terminal" else (summary ?: "Ran command"),
                    body = thought,
                    code = cmd,
                    codeLabel = "bash"
                )
            }
            actionKind == "ThinkAction" -> b.copy(
                category = EventCategory.THOUGHT,
                title = "Thinking",
                body = listOfNotNull(thought, action["thought"] as? String).joinToString("\n\n").ifBlank { null }
            )
            actionKind == "FinishAction" -> b.copy(
                category = EventCategory.FINISH,
                title = "Task finished",
                body = listOfNotNull(thought, action["message"] as? String).joinToString("\n\n").ifBlank { null }
            )
            actionKind == "FileEditorAction" -> {
                val cmd = action["command"] as? String ?: "edit"
                val path = action["path"] as? String
                val code = when (cmd) {
                    "create" -> action["file_text"] as? String
                    "str_replace" -> diffBlock(action["old_str"] as? String, action["new_str"] as? String)
                    "insert" -> action["new_str"] as? String
                    else -> null
                }
                b.copy(
                    category = if (cmd == "view") EventCategory.FILE_READ else EventCategory.FILE_EDIT,
                    title = summary ?: when (cmd) {
                        "view" -> "Viewed file"
                        "create" -> "Created file"
                        "str_replace" -> "Edited file"
                        "insert" -> "Inserted into file"
                        "undo_edit" -> "Undid edit"
                        else -> "File $cmd"
                    },
                    body = listOfNotNull(thought, path).joinToString("\n").ifBlank { null },
                    code = code,
                    codeLabel = if (cmd == "str_replace") "diff" else path?.substringAfterLast('/'),
                    filePath = path
                )
            }
            actionKind == "EditAction" -> {
                val path = action["file_path"] as? String ?: action["path"] as? String
                b.copy(
                    category = EventCategory.FILE_EDIT,
                    title = summary ?: "Edited file",
                    body = listOfNotNull(thought, path).joinToString("\n").ifBlank { null },
                    code = diffBlock(action["old_string"] as? String, action["new_string"] as? String),
                    codeLabel = "diff",
                    filePath = path
                )
            }
            actionKind == "ApplyPatchAction" -> b.copy(
                category = EventCategory.FILE_EDIT,
                title = summary ?: "Applied patch",
                body = thought,
                code = action["patch"] as? String,
                codeLabel = "patch"
            )
            actionKind == "WriteFileAction" -> {
                val path = action["file_path"] as? String
                b.copy(
                    category = EventCategory.FILE_EDIT,
                    title = summary ?: "Wrote file",
                    body = listOfNotNull(thought, path).joinToString("\n").ifBlank { null },
                    code = action["content"] as? String,
                    codeLabel = path?.substringAfterLast('/'),
                    filePath = path
                )
            }
            actionKind == "ReadFileAction" || actionKind == "ListDirectoryAction" -> {
                val path = action["file_path"] as? String ?: action["path"] as? String
                b.copy(
                    category = EventCategory.FILE_READ,
                    title = summary ?: if (actionKind == "ReadFileAction") "Read file" else "Listed directory",
                    body = listOfNotNull(thought, path).joinToString("\n").ifBlank { null },
                    filePath = path
                )
            }
            actionKind == "GrepAction" || actionKind == "GlobAction" -> b.copy(
                category = EventCategory.FILE_READ,
                title = summary ?: if (actionKind == "GrepAction") "Searched file contents" else "Searched file names",
                body = thought,
                code = listOfNotNull(action["pattern"] as? String, action["path"] as? String, action["include"] as? String)
                    .joinToString("  "),
                codeLabel = "pattern"
            )
            actionKind.startsWith("Browser") -> b.copy(
                category = EventCategory.BROWSER,
                title = summary ?: humanize(actionKind.removeSuffix("Action")),
                body = thought,
                code = (action["url"] as? String) ?: (action["text"] as? String)
            )
            actionKind == "TaskTrackerAction" -> {
                val tasks = action["task_list"] as? List<*>
                val list = tasks?.mapNotNull { t ->
                    (t as? Map<*, *>)?.let { m -> "[${m["status"] ?: " "}] ${m["title"] ?: m["notes"] ?: ""}" }
                }?.joinToString("\n")
                b.copy(
                    category = EventCategory.TOOL,
                    title = summary ?: "Task list ${action["command"] ?: ""}".trim(),
                    body = thought,
                    code = list,
                    codeLabel = "tasks"
                )
            }
            actionKind == "MCPToolAction" -> b.copy(
                category = EventCategory.TOOL,
                title = summary ?: "MCP tool: ${toolName ?: "call"}",
                body = thought,
                code = (action["data"])?.let { prettify(it) },
                codeLabel = "arguments"
            )
            else -> {
                val args = (dto.toolCall?.get("arguments") as? String)
                    ?: action.filterKeys { it != "kind" }.takeIf { it.isNotEmpty() }?.let { prettify(it) }
                b.copy(
                    category = if (action.isEmpty()) EventCategory.AGENT_MESSAGE else EventCategory.TOOL,
                    title = summary ?: toolName?.let { humanize(it) } ?: "Agent step",
                    body = thought,
                    code = args,
                    codeLabel = toolName?.let { "$it arguments" }
                )
            }
        }
    }

    // ------------------------------------------------------------ ACP tool call

    /**
     * ACP agents emit one event when a tool call starts (`status=pending`, empty raw_input) and a second one with
     * the same `tool_call_id` when it finishes (`completed`/`failed`, with raw_input/raw_output filled in).
     */
    private fun mapAcpToolCall(dto: EventDto, base: AgentEvent): AgentEvent {
        val status = dto.status?.lowercase() ?: ""
        val pending = status == "pending" || status == "in_progress"
        val input = dto.rawInput ?: emptyMap()
        val toolKind = dto.toolKind?.lowercase() ?: ""
        val category = when (toolKind) {
            "execute" -> EventCategory.COMMAND
            "read", "search" -> EventCategory.FILE_READ
            "edit", "delete", "move" -> EventCategory.FILE_EDIT
            "fetch" -> EventCategory.BROWSER
            "think" -> EventCategory.THOUGHT
            else -> EventCategory.TOOL
        }
        val output: String? = when (val raw = dto.rawOutput) {
            null -> joinAcpContent(dto.content).ifBlank { null }
            is String -> raw
            else -> prettify(raw)
        }
        val exitCode = output?.let { Regex("^Exit code (-?\\d+)").find(it)?.groupValues?.get(1)?.toIntOrNull() }
        val command = input["command"] as? String
        val filePath = (input["file_path"] ?: input["path"]) as? String
        val description = input["description"] as? String
        val isError = dto.isError == true || status == "failed" || (exitCode != null && exitCode != 0)
        val title = when {
            pending -> "${dto.title ?: humanize(toolKind).ifBlank { "Tool" }} — running…"
            isError -> (dto.title ?: "Tool call") + " (failed)"
            else -> dto.title ?: humanize(toolKind).ifBlank { "Tool call" }
        }
        val code = when {
            pending -> null
            command != null -> listOfNotNull("$ $command", output).joinToString("\n")
            output != null -> output
            input.isNotEmpty() -> prettify(input)
            else -> null
        }
        return base.copy(
            category = category,
            title = title,
            body = if (pending) null else description,
            code = code?.take(MAX_CODE_CHARS),
            codeLabel = when {
                pending -> AgentEvent.PENDING_LABEL
                command != null -> "bash"
                filePath != null -> filePath.substringAfterLast('/')
                else -> toolKind.ifBlank { null }
            },
            isError = isError,
            toolName = dto.toolKind ?: dto.title,
            toolCallId = dto.toolCallId,
            filePath = filePath,
            exitCode = exitCode
        )
    }

    /** ACP content blocks are nested: [{content: {type: text, text: ...}}]. */
    private fun joinAcpContent(content: Any?): String = when (content) {
        is List<*> -> content.mapNotNull { block ->
            when (block) {
                is Map<*, *> -> (block["content"] as? Map<*, *>)?.get("text") as? String ?: block["text"] as? String
                is String -> block
                else -> null
            }
        }.joinToString("\n")
        else -> joinContent(content)
    }

    private const val MAX_CODE_CHARS = 20_000
    private const val MAX_REASONING_CHARS = 4_000

    // -------------------------------------------------------------- observation

    private fun mapObservation(dto: EventDto, base: AgentEvent): AgentEvent {
        val obs = dto.observation ?: emptyMap()
        val obsKind = obs["kind"] as? String ?: ""
        val isError = obs["is_error"] == true
        val output = joinContent(obs["content"]).ifBlank { null }
        val toolName = dto.toolName ?: obsKind.removeSuffix("Observation").ifBlank { null }
        val b = base.copy(toolName = toolName, isError = isError, category = EventCategory.OBSERVATION)

        return when (obsKind) {
            "TerminalObservation" -> {
                val exit = (obs["exit_code"] as? Number)?.toInt()
                val timedOut = obs["timeout"] == true
                b.copy(
                    title = when {
                        timedOut -> "Command timed out"
                        exit == null -> "Command output"
                        exit == 0 -> "Command output"
                        else -> "Command failed (exit $exit)"
                    },
                    isError = isError || (exit != null && exit != 0),
                    code = output,
                    codeLabel = "output",
                    exitCode = exit
                )
            }
            "FileEditorObservation", "EditObservation", "ApplyPatchObservation", "WriteFileObservation" -> {
                val path = (obs["path"] ?: obs["file_path"]) as? String
                val oldC = obs["old_content"] as? String
                val newC = obs["new_content"] as? String
                val diff = if (oldC != null || newC != null) diffBlock(oldC, newC) else null
                b.copy(
                    category = EventCategory.FILE_EDIT,
                    title = if (isError) "Edit failed" else (path?.let { "Edited ${it.substringAfterLast('/')}" } ?: "Edit result"),
                    body = if (diff != null) output else null,
                    code = diff ?: output,
                    codeLabel = if (diff != null) "diff" else "output",
                    filePath = path
                )
            }
            "ReadFileObservation", "ListDirectoryObservation", "GlobObservation", "GrepObservation" -> b.copy(
                category = EventCategory.FILE_READ,
                title = humanize(obsKind.removeSuffix("Observation")) + " result",
                code = output,
                codeLabel = "output"
            )
            "FinishObservation" -> b.copy(category = EventCategory.FINISH, title = "Finished", body = output)
            "ThinkObservation" -> b.copy(category = EventCategory.SYSTEM, title = "Thought recorded", body = output)
            "BrowserObservation" -> b.copy(
                category = EventCategory.BROWSER,
                title = "Browser result",
                code = output,
                codeLabel = "page"
            )
            else -> b.copy(
                title = toolName?.let { "${humanize(it)} result" } ?: "Tool result",
                code = output,
                codeLabel = "output"
            )
        }
    }

    // ------------------------------------------------------------------ helpers

    private fun defaultSource(kind: EventKind): String = when (kind) {
        EventKind.ACTION, EventKind.AGENT_ERROR, EventKind.SYSTEM_PROMPT, EventKind.STREAMING_DELTA -> "agent"
        EventKind.PAUSE, EventKind.INTERRUPT -> "user"
        else -> "environment"
    }

    /** Joins the text parts of a `content` list ([TextContent|ImageContent]). Accepts a plain string too. */
    internal fun joinContent(content: Any?): String = when (content) {
        null -> ""
        is String -> content
        is TextContentDto -> content.text
        is List<*> -> content.mapNotNull { part ->
            when (part) {
                is String -> part
                is TextContentDto -> part.text
                is Map<*, *> -> if (part["type"] == "image") null else part["text"] as? String
                else -> null
            }
        }.joinToString("\n")
        else -> content.toString()
    }

    private fun countImages(content: Any?): Int = (content as? List<*>)?.count { part ->
        (part as? Map<*, *>)?.get("type") == "image"
    } ?: 0

    private fun diffBlock(old: String?, new: String?): String? {
        if (old == null && new == null) return null
        val sb = StringBuilder()
        old?.lines()?.forEach { sb.append("- ").append(it).append('\n') }
        new?.lines()?.forEach { sb.append("+ ").append(it).append('\n') }
        return sb.toString().trimEnd()
    }

    internal fun humanize(name: String): String {
        if (name.isBlank()) return name
        val spaced = name
            .replace('_', ' ')
            .replace(Regex("([a-z0-9])([A-Z])"), "$1 $2")
            .trim()
        return spaced.replaceFirstChar { it.uppercase() }
    }

    /** Minimal JSON-ish pretty printer for Map/List values decoded by Moshi's Any adapter. */
    internal fun prettify(value: Any?, indent: String = ""): String = when (value) {
        null -> "null"
        is Map<*, *> -> if (value.isEmpty()) "{}" else buildString {
            append("{\n")
            value.entries.forEachIndexed { i, (k, v) ->
                append(indent).append("  ").append('"').append(k).append("\": ").append(prettify(v, "$indent  "))
                if (i < value.size - 1) append(',')
                append('\n')
            }
            append(indent).append('}')
        }
        is List<*> -> if (value.isEmpty()) "[]" else buildString {
            append("[\n")
            value.forEachIndexed { i, v ->
                append(indent).append("  ").append(prettify(v, "$indent  "))
                if (i < value.size - 1) append(',')
                append('\n')
            }
            append(indent).append(']')
        }
        is String -> '"' + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + '"'
        is Double -> if (value == Math.floor(value) && !value.isInfinite()) value.toLong().toString() else value.toString()
        else -> value.toString()
    }
}
