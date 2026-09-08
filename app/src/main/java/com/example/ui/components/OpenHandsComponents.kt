package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentEvent
import com.example.data.model.ConversationStatus
import com.example.data.model.EventCategory
import com.example.data.model.SandboxStatus
import com.example.ui.theme.OpenHandsTerminalBg
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPaused
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusWaiting
import com.example.ui.theme.StatusWorking
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ---------------------------------------------------------------------------
// Formatting helpers
// ---------------------------------------------------------------------------

fun formatRelativeTime(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    if (epochMillis <= 0L) return ""
    val diff = now - epochMillis
    val sec = diff / 1000
    return when {
        sec < 45 -> "just now"
        sec < 90 -> "1 min ago"
        sec < 3600 -> "${sec / 60} min ago"
        sec < 7200 -> "1 hour ago"
        sec < 86_400 -> "${sec / 3600} hours ago"
        sec < 172_800 -> "yesterday"
        sec < 7 * 86_400 -> "${sec / 86_400} days ago"
        else -> DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(epochMillis))
    }
}

fun formatClockTime(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(epochMillis))

fun formatCost(cost: Double): String {
    if (cost <= 0.0) return "$0.00"
    return if (cost < 0.01) String.format(Locale.US, "$%.4f", cost) else String.format(Locale.US, "$%.2f", cost)
}

fun formatTokens(tokens: Long): String = when {
    tokens < 1_000 -> tokens.toString()
    tokens < 1_000_000 -> String.format(Locale.US, "%.1fk", tokens / 1_000.0)
    else -> String.format(Locale.US, "%.2fM", tokens / 1_000_000.0)
}

fun formatCount(n: Int): String = NumberFormat.getIntegerInstance().format(n)

fun statusColor(status: ConversationStatus): Color = when (status) {
    ConversationStatus.RUNNING -> StatusWorking
    ConversationStatus.STARTING -> StatusWaiting
    ConversationStatus.WAITING_FOR_CONFIRMATION -> StatusWaiting
    ConversationStatus.IDLE, ConversationStatus.FINISHED -> StatusReady
    ConversationStatus.PAUSED, ConversationStatus.UNKNOWN -> StatusPaused
    ConversationStatus.STUCK, ConversationStatus.ERROR -> StatusError
}

fun sandboxColor(status: SandboxStatus): Color = when (status) {
    SandboxStatus.RUNNING -> StatusReady
    SandboxStatus.STARTING -> StatusWaiting
    SandboxStatus.PAUSED, SandboxStatus.UNKNOWN -> StatusPaused
    SandboxStatus.ERROR, SandboxStatus.MISSING -> StatusError
}

// ---------------------------------------------------------------------------
// Badges
// ---------------------------------------------------------------------------

@Composable
fun StatusBadge(status: ConversationStatus, modifier: Modifier = Modifier) {
    val color = statusColor(status)
    val icon = when (status) {
        ConversationStatus.RUNNING -> Icons.Default.PlayArrow
        ConversationStatus.STARTING -> Icons.Default.HourglassTop
        ConversationStatus.WAITING_FOR_CONFIRMATION -> Icons.Default.Warning
        ConversationStatus.IDLE, ConversationStatus.FINISHED -> Icons.Default.CheckCircle
        ConversationStatus.PAUSED -> Icons.Default.Pause
        ConversationStatus.STUCK -> Icons.Default.Block
        ConversationStatus.ERROR -> Icons.Default.Error
        ConversationStatus.UNKNOWN -> Icons.Default.BugReport
    }
    Pill(text = status.label, color = color, icon = icon, modifier = modifier.testTag("status_badge_${status.name}"))
}

@Composable
fun SandboxBadge(status: SandboxStatus, modifier: Modifier = Modifier) {
    Pill(text = "Sandbox ${status.label.lowercase()}", color = sandboxColor(status), icon = null, modifier = modifier)
}

@Composable
fun Pill(text: String, color: Color, icon: ImageVector?, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            } else {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
            }
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------------------
// Layout primitives
// ---------------------------------------------------------------------------

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        trailing?.invoke()
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = tint?.copy(alpha = 0.12f) ?: MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, tint?.copy(alpha = 0.4f) ?: MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(14.dp)) { content() }
    }
}

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    InfoCard(modifier = modifier, tint = StatusError) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Error, contentDescription = null, tint = StatusError, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 16.sp, modifier = Modifier.weight(1f))
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.width(8.dp))
                Button(onClick = onAction, shape = RoundedCornerShape(10.dp)) { Text(actionLabel, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun EmptyState(title: String, subtitle: String?, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Box(modifier = modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
            }
            if (action != null) {
                Spacer(Modifier.height(14.dp))
                action()
            }
        }
    }
}

@Composable
fun StatMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp)) }
                Spacer(Modifier.width(8.dp))
                Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ---------------------------------------------------------------------------
// Code block
// ---------------------------------------------------------------------------

@Composable
fun CodeBlock(
    text: String,
    label: String?,
    modifier: Modifier = Modifier,
    maxCollapsedLines: Int = 14,
    colorizeDiff: Boolean = false
) {
    val clipboard = LocalClipboardManager.current
    val lines = remember(text) { text.lines() }
    var expanded by remember(text) { mutableStateOf(lines.size <= maxCollapsedLines) }
    val visible = if (expanded) lines else lines.take(maxCollapsedLines)

    Surface(
        color = OpenHandsTerminalBg,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 2.dp, top = 2.dp, bottom = 0.dp)
            ) {
                Text(
                    text = label ?: "",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF8B949E)
                )
                Row {
                    if (lines.size > maxCollapsedLines) {
                        IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (expanded) "Collapse" else "Show all ${lines.size} lines",
                                tint = Color(0xFF8B949E),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(onClick = { clipboard.setText(AnnotatedString(text)) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFF8B949E), modifier = Modifier.size(14.dp))
                    }
                }
            }
            Column(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(start = 10.dp, end = 10.dp, bottom = 10.dp)) {
                visible.forEach { line ->
                    val color = when {
                        !colorizeDiff -> Color(0xFFE6EDF3)
                        line.startsWith("+") -> Color(0xFF7EE787)
                        line.startsWith("-") -> Color(0xFFFFA198)
                        line.startsWith("@@") -> Color(0xFF79C0FF)
                        else -> Color(0xFFE6EDF3)
                    }
                    Text(
                        text = line.ifEmpty { " " },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = color,
                        softWrap = false
                    )
                }
                if (!expanded) {
                    Text(
                        "… ${lines.size - maxCollapsedLines} more lines",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF8B949E),
                        modifier = Modifier.padding(top = 4.dp).clickable { expanded = true }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Event card
// ---------------------------------------------------------------------------

private data class EventStyle(val icon: ImageVector, val label: String, val color: Color)

@Composable
private fun styleFor(event: AgentEvent): EventStyle {
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant
    return when {
        event.isError -> EventStyle(Icons.Default.Error, "ERROR", StatusError)
        else -> when (event.category) {
            EventCategory.USER_MESSAGE -> EventStyle(Icons.Default.SmartToy, "YOU", MaterialTheme.colorScheme.primary)
            EventCategory.AGENT_MESSAGE -> EventStyle(Icons.Default.SmartToy, "AGENT", MaterialTheme.colorScheme.primary)
            EventCategory.THOUGHT -> EventStyle(Icons.Default.Lightbulb, "REASONING", MaterialTheme.colorScheme.secondary)
            EventCategory.COMMAND -> EventStyle(Icons.Default.Terminal, "TERMINAL", MaterialTheme.colorScheme.primary)
            EventCategory.FILE_EDIT -> EventStyle(Icons.Default.Edit, "FILE EDIT", MaterialTheme.colorScheme.tertiary)
            EventCategory.FILE_READ -> EventStyle(Icons.Default.Description, "FILES", onSurface)
            EventCategory.BROWSER -> EventStyle(Icons.Default.Language, "BROWSER", MaterialTheme.colorScheme.secondary)
            EventCategory.TOOL -> EventStyle(Icons.Default.Settings, event.toolName?.uppercase() ?: "TOOL", MaterialTheme.colorScheme.secondary)
            EventCategory.OBSERVATION -> EventStyle(Icons.Default.Terminal, "OUTPUT", onSurface)
            EventCategory.FINISH -> EventStyle(Icons.Default.Flag, "FINISHED", StatusReady)
            EventCategory.ERROR -> EventStyle(Icons.Default.Error, "ERROR", StatusError)
            EventCategory.CONTROL -> EventStyle(Icons.Default.Pause, "CONTROL", StatusWaiting)
            EventCategory.SYSTEM -> EventStyle(Icons.Default.Settings, "SYSTEM", onSurface)
        }
    }
}

@Composable
fun EventCard(event: AgentEvent, modifier: Modifier = Modifier) {
    if (event.category == EventCategory.USER_MESSAGE) {
        UserMessageBubble(event, modifier)
        return
    }
    val style = styleFor(event)
    val hasDetails = !event.code.isNullOrBlank()
    val startCollapsed = event.category == EventCategory.OBSERVATION && !event.isError && (event.code?.lines()?.size ?: 0) > 6 ||
        event.category == EventCategory.SYSTEM || event.category == EventCategory.FILE_READ
    var expanded by remember(event.id) { mutableStateOf(!startCollapsed) }

    val container = when {
        event.isError -> StatusError.copy(alpha = 0.08f)
        event.category == EventCategory.AGENT_MESSAGE || event.category == EventCategory.FINISH -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surface
    }
    val border = when {
        event.isError -> StatusError.copy(alpha = 0.5f)
        event.category == EventCategory.FINISH -> StatusReady.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, border),
        modifier = modifier.fillMaxWidth().testTag("event_${event.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().clickable(enabled = hasDetails) { expanded = !expanded }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(style.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = style.color, letterSpacing = 0.5.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        event.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(formatClockTime(event.timestamp), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (hasDetails) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (event.securityRisk != null && event.securityRisk != "LOW") {
                Spacer(Modifier.height(6.dp))
                Pill(text = "Risk: ${event.securityRisk}", color = if (event.securityRisk == "HIGH") StatusError else StatusWaiting, icon = Icons.Default.Warning)
            }

            if (!event.body.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(event.body, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 18.sp)
            }

            if (hasDetails) {
                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        CodeBlock(
                            text = event.code!!,
                            label = listOfNotNull(event.codeLabel, event.exitCode?.let { "exit $it" }).joinToString(" · "),
                            colorizeDiff = event.codeLabel == "diff" || event.codeLabel == "patch"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(event: AgentEvent, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Card(
            shape = RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp),
            colors = CardDefaults.cardColors(containerColor = if (event.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(event.title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
                    Text(formatClockTime(event.timestamp), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                }
                Spacer(Modifier.height(4.dp))
                Text(event.body ?: "", fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimary, lineHeight = 19.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Misc
// ---------------------------------------------------------------------------

@Composable
fun KeyValueRow(key: String, value: String, modifier: Modifier = Modifier, mono: Boolean = false) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            fontSize = 12.sp,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun LinkRow(label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp)
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
    }
}

/**
 * Dropdown selector rendered as a field-like surface. Used for config choices that mirror
 * the webui's select dropdowns (theme, LLM model, profile).
 */
@Composable
fun <T> DropdownSelector(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: (T) -> String,
    optionSubtitle: ((T) -> String)? = null,
    enabled: Boolean = true,
    emptyText: String? = null,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        Box {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = enabled) { expanded = true }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            selected?.let(optionLabel) ?: emptyText ?: "Select…",
                            fontSize = 13.sp,
                            color = if (selected != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = selected?.let { optionSubtitle?.invoke(it) }
                        if (!subtitle.isNullOrBlank()) {
                            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 320.dp)) {
                if (options.isEmpty()) {
                    DropdownMenuItem(text = { Text(emptyText ?: "Nothing available", fontSize = 13.sp) }, onClick = { expanded = false }, enabled = false)
                }
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(optionLabel(option), fontSize = 13.sp, fontWeight = if (option == selected) FontWeight.SemiBold else FontWeight.Normal)
                                    optionSubtitle?.invoke(option)?.let {
                                        Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (option == selected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = { expanded = false; onSelect(option) }
                    )
                }
            }
        }
    }
}
