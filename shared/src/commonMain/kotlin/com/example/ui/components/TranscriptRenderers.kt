package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentEvent
import com.example.ui.theme.CanvasColors
import com.example.ui.theme.CanvasPrimary
import com.example.ui.theme.CanvasSecondary
import com.example.ui.theme.CanvasSuccess
import com.example.ui.theme.CanvasTerminalBg
import com.example.ui.theme.CodeSmall
import com.example.ui.theme.Shapes
import com.example.ui.theme.Sizes
import com.example.ui.theme.Spacing
import com.mikepenz.markdown.m3.Markdown
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage

/**
 * Transcript event rendering.
 *
 * The old renderer printed every non-user event as flat text, so agent output
 * (markdown, code, diffs, command output) was unreadable. Each event class now
 * gets a renderer suited to its content:
 *
 *  - user message      → right-aligned bubble
 *  - agent message     → markdown (headings, lists, code fences, links)
 *  - thought           → muted, collapsible
 *  - bash/terminal     → terminal-style block with copy
 *  - file edit         → path header + unified diff with add/remove colouring
 *  - error             → danger-tinted block
 *  - everything else   → generic card
 */

/**
 * Sentinel the repository writes when no field carried user-facing content.
 * Renderers must treat it as "empty" rather than printing it.
 */
private const val NO_CONTENT = "(no content)"

/** Classifies an event so the right renderer is chosen. */
private enum class EventKind { USER, MESSAGE, THOUGHT, COMMAND, EDIT, ERROR, GENERIC }

private fun AgentEvent.classify(): EventKind {
    val src = source.lowercase()
    val action = actionType?.lowercase().orEmpty()
    return when {
        src == "user" -> EventKind.USER
        isError -> EventKind.ERROR
        action.contains("thought") || action.contains("reason") -> EventKind.THOUGHT
        action.contains("bash") || action.contains("command") || action.contains("terminal") ->
            EventKind.COMMAND
        action.contains("edit") || action.contains("write") || action.contains("patch") ||
            action.contains("create") -> EventKind.EDIT
        src == "agent" || src == "assistant" || action.contains("message") -> EventKind.MESSAGE
        else -> EventKind.GENERIC
    }
}

/** True when [text] looks like a unified diff worth colouring line-by-line. */
private fun looksLikeDiff(text: String): Boolean =
    text.contains("\n@@") || text.startsWith("@@") || text.lineSequence().any { it.startsWith("+++ ") }

@Composable
fun TranscriptEvent(event: AgentEvent, modifier: Modifier = Modifier) {
    when (event.classify()) {
        EventKind.USER -> UserMessageBubble(event, modifier)
        EventKind.THOUGHT -> ThoughtBlock(event, modifier)
        EventKind.COMMAND -> TerminalBlock(event, modifier)
        EventKind.EDIT -> EditBlock(event, modifier)
        EventKind.ERROR -> ErrorBlock(event, modifier)
        EventKind.MESSAGE -> AgentMessageBlock(event, modifier)
        EventKind.GENERIC -> GenericBlock(event, modifier)
    }
}

// ---------------------------------------------------------------- user

@Composable
private fun UserMessageBubble(event: AgentEvent, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            Modifier
                .fillMaxWidth(0.88f)
                .background(CanvasPrimary.copy(alpha = 0.14f), Shapes.large)
                .border(1.dp, CanvasPrimary.copy(alpha = 0.32f), Shapes.large)
                .padding(Spacing.md)
        ) {
            Text(
                "You",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CanvasPrimary
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(event.content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ------------------------------------------------------------- agent

@Composable
private fun AgentMessageBlock(event: AgentEvent, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
        Markdown(
            event.content,
            modifier = Modifier.fillMaxWidth().testTag("markdown_body")
        )
    }
}

// ------------------------------------------------------------ thought

@Composable
private fun ThoughtBlock(event: AgentEvent, modifier: Modifier = Modifier) {
    var expanded by remember(event.id) { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xxs)
            .background(CanvasSecondary.copy(alpha = 0.07f), Shapes.small)
            .border(1.dp, CanvasSecondary.copy(alpha = 0.22f), Shapes.small)
            .clickable { expanded = !expanded }
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Psychology,
                contentDescription = null,
                tint = CanvasSecondary,
                modifier = Modifier.size(Sizes.iconSmall)
            )
            Spacer(Modifier.width(Spacing.xs))
            Text(
                "Thinking",
                style = MaterialTheme.typography.labelMedium,
                color = CanvasSecondary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = CanvasSecondary,
                modifier = Modifier.size(Sizes.iconSmall)
            )
        }
        if (expanded) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                event.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ----------------------------------------------------------- terminal

@Composable
private fun TerminalBlock(event: AgentEvent, modifier: Modifier = Modifier) {
    // Two shapes arrive for terminal events:
    //  - an ActionEvent, where `content` is the command the agent ran;
    //  - an ObservationEvent, where `content` is empty and `detail` holds the
    //    command output. Verified against live data: 36 of 73 terminal events
    //    are observations with no content, so rendering `content` alone showed
    //    "$ (no content)" for half the transcript.
    val hasCommand = event.content.isNotBlank() && event.content != NO_CONTENT
    val output = event.detail?.takeIf { it.isNotBlank() }
    val commandLine = if (hasCommand) "$ ${event.content}" else null

    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
            .background(CanvasTerminalBg, Shapes.small)
            .border(1.dp, CanvasColors.borderSubtle, Shapes.small)
    ) {
        BlockHeader(
            icon = Icons.Default.Terminal,
            label = "Terminal",
            copyText = listOfNotNull(commandLine, output).joinToString("\n"),
            tint = CanvasSuccess
        )
        Column(Modifier.padding(Spacing.md)) {
            commandLine?.let {
                Text(it, style = CodeSmall, color = CanvasSuccess)
                if (output != null) Spacer(Modifier.height(Spacing.sm))
            }
            output?.let {
                Text(
                    it,
                    style = CodeSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("terminal_output")
                )
            }
        }
    }
}

// --------------------------------------------------------------- edit

@Composable
private fun EditBlock(event: AgentEvent, modifier: Modifier = Modifier) {
    val hasContent = event.content.isNotBlank() && event.content != NO_CONTENT
    val body = event.detail?.takeIf { it.isNotBlank() }
        ?: event.content.takeIf { hasContent }
        ?: return
    val isDiff = looksLikeDiff(body)
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, Shapes.small)
            .border(1.dp, CanvasColors.borderSubtle, Shapes.small)
    ) {
        BlockHeader(
            icon = Icons.Default.Build,
            label = if (hasContent) event.content else "File change",
            copyText = body,
            tint = CanvasPrimary
        )
        Column(Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm)) {
            if (isDiff) {
                DiffText(body)
            } else {
                Text(body, style = CodeSmall)
            }
        }
    }
}

/** Colours a unified diff line-by-line: + green, - red, @@ hunk header muted. */
@Composable
private fun DiffText(diff: String) {
    val added = Color(0xFF3FB950)
    val removed = Color(0xFFF85149)
    val hunk = CanvasSecondary
    val annotated = remember(diff) {
        buildAnnotatedString {
            diff.lineSequence().forEachIndexed { index, line ->
                if (index > 0) append("\n")
                val style = when {
                    line.startsWith("+++") || line.startsWith("---") -> SpanStyle(
                        color = hunk, fontWeight = FontWeight.Medium
                    )
                    line.startsWith("+") -> SpanStyle(color = added)
                    line.startsWith("-") -> SpanStyle(color = removed)
                    line.startsWith("@@") -> SpanStyle(color = hunk)
                    else -> SpanStyle(color = Color.Unspecified)
                }
                withStyle(style) { append(line) }
            }
        }
    }
    Text(
        annotated,
        style = CodeSmall,
        modifier = Modifier.testTag("diff_body")
    )
}

// -------------------------------------------------------------- error

@Composable
private fun ErrorBlock(event: AgentEvent, modifier: Modifier = Modifier) {
    val danger = CanvasColors.danger
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
            .background(danger.copy(alpha = 0.10f), Shapes.small)
            .border(1.dp, danger.copy(alpha = 0.35f), Shapes.small)
            .padding(Spacing.md)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = danger,
                modifier = Modifier.size(Sizes.iconSmall)
            )
            Spacer(Modifier.width(Spacing.xs))
            Text("Error", style = MaterialTheme.typography.labelMedium, color = danger)
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(event.content, style = MaterialTheme.typography.bodyMedium)
        event.detail?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(Spacing.xs))
            Text(it, style = CodeSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ------------------------------------------------------------ generic

@Composable
private fun GenericBlock(event: AgentEvent, modifier: Modifier = Modifier) {
    // Skip the literal sentinel: an event with no user-facing content should not
    // render a card that says "(no content)".
    val body = event.content.takeIf { it.isNotBlank() && it != NO_CONTENT } ?: return
    Column(
        modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, Shapes.small)
            .border(1.dp, CanvasColors.borderSubtle, Shapes.small)
            .padding(Spacing.md)
    ) {
        event.actionType?.takeIf { it.isNotBlank() }?.let {
            Text(
                it.replaceFirstChar { c -> c.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(Spacing.xs))
        }
        Text(body, style = MaterialTheme.typography.bodyMedium)
        event.detail?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(Spacing.xs))
            Text(it, style = CodeSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// -------------------------------------------------------------- parts

@Composable
private fun BlockHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    copyText: String,
    tint: Color
) {
    val clipboard = LocalClipboardManager.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.10f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .padding(start = Spacing.md, end = Spacing.xs, top = Spacing.xs, bottom = Spacing.xs)
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(Sizes.iconSmall))
        Spacer(Modifier.width(Spacing.xs))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        Box(
            Modifier
                .size(Sizes.touchTarget)
                .clickable { clipboard.setText(AnnotatedString(copyText)) }
                .testTag("btn_copy_block"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = tint,
                modifier = Modifier.size(Sizes.iconSmall)
            )
        }
    }
}

/**
 * Renders a fenced code block body with syntax highlighting.
 *
 * Kept as a small helper rather than a dependency on the markdown renderer's
 * code styling so the transcript can colour code the same way everywhere.
 */
@Composable
fun HighlightedCode(code: String, language: String?, modifier: Modifier = Modifier) {
    val annotated = remember(code, language) {
        val syntax = language?.let { lang ->
            runCatching { SyntaxLanguage.valueOf(lang.uppercase()) }.getOrNull()
        }
        runCatching {
            val highlights = Highlights.Builder()
                .code(code)
                .language(syntax ?: SyntaxLanguage.DEFAULT)
                .build()
            buildAnnotatedString {
                var cursor = 0
                // The highlighter emits spans in arbitrary order; the cursor
                // logic below only reconstructs the source correctly when spans
                // are visited left-to-right.
                highlights.getHighlights().sortedBy { it.location.start }.forEach { hl ->
                    // Only colour spans carry an rgb; bold spans are skipped.
                    val rgb = (hl as? ColorHighlight)?.rgb ?: return@forEach
                    val start = hl.location.start
                    val end = hl.location.end
                    if (start > cursor) append(code.substring(cursor, start.coerceAtMost(code.length)))
                    if (start < code.length && end <= code.length && end > start) {
                        withStyle(SpanStyle(color = Color(rgb))) {
                            append(code.substring(start, end))
                        }
                        cursor = end
                    }
                }
                if (cursor < code.length) append(code.substring(cursor))
            }
        }.getOrElse { AnnotatedString(code) }
    }
    Text(
        annotated,
        style = CodeSmall.copy(fontFamily = FontFamily.Monospace),
        modifier = modifier.testTag("highlighted_code")
    )
}
