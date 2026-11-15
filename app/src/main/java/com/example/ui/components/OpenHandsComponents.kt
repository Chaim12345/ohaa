package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentEvent
import com.example.data.model.ConversationStatus
import com.example.ui.theme.OpenHandsBorderDark
import com.example.ui.theme.OpenHandsPrimaryDark
import com.example.ui.theme.LocalAppIsDark
import com.example.ui.theme.StatusColors
import com.example.ui.theme.OpenHandsSecondaryDark
import com.example.ui.theme.OpenHandsSurfaceVariantDark
import com.example.ui.theme.OpenHandsTerminalBg
import com.example.ui.theme.OpenHandsTextPrimaryDark
import com.example.ui.theme.OpenHandsTertiaryDark
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusPaused
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusWaiting
import com.example.ui.theme.StatusWorking

@Composable
fun StatusBadge(
    status: ConversationStatus,
    modifier: Modifier = Modifier
) {
    val (color, icon) = when (status) {
        ConversationStatus.RUNNING -> Pair(StatusColors.working, Icons.Default.PlayArrow)
        ConversationStatus.READY, ConversationStatus.FINISHED -> Pair(StatusColors.ready, Icons.Default.CheckCircle)
        ConversationStatus.WAITING_FOR_SANDBOX, ConversationStatus.PREPARING_REPOSITORY,
        ConversationStatus.SETTING_UP_SKILLS -> Pair(StatusColors.waiting, Icons.Default.HourglassTop)
        ConversationStatus.AWAITING_CONFIRMATION -> Pair(StatusColors.waiting, Icons.Default.HourglassTop)
        ConversationStatus.ERROR -> Pair(StatusColors.error, Icons.Default.Error)
        ConversationStatus.PAUSED -> Pair(StatusColors.paused, Icons.Default.BugReport)
        ConversationStatus.UNKNOWN -> Pair(StatusColors.paused, Icons.Default.BugReport)
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier.testTag("status_badge_${status.name}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = status.label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TerminalCodeBlock(
    codeText: String,
    title: String = "stdout",
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(OpenHandsTerminalBg)
            .border(1.dp, OpenHandsBorderDark, RoundedCornerShape(10.dp))
    ) {
        // Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .background(OpenHandsSurfaceVariantDark)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = OpenHandsPrimaryDark,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = OpenHandsTextPrimaryDark.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            IconButton(
                onClick = {
                    clipboard.setText(AnnotatedString(codeText))
                    Toast.makeText(context, "Output copied to clipboard", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = OpenHandsTextPrimaryDark.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            Text(
                text = codeText,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = if (LocalAppIsDark.current) Color(0xFF56D364) else Color(0xFF1A7F37),
                lineHeight = 16.sp
            )
        }
    }
}
@Composable
fun EventItemView(
    event: AgentEvent,
    modifier: Modifier = Modifier
) {
    var isExpanded by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    when (event.source.lowercase()) {
        "user" -> {
            // User prompt: subtle tinted block, right-aligned, no label chrome.
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp),
                    modifier = Modifier.fillMaxWidth(0.88f)
                ) {
                    Text(
                        text = event.content,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 19.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        else -> {
            val isThought = event.actionType == "thought"
            val isBash = event.actionType == "bash" || event.content.startsWith("$")
            val isEdit = event.actionType == "edit" || "FileEdit" in event.content
            val isObservation = event.actionType == "observation" ||
                event.source.equals("environment", ignoreCase = true)
            val isMessage = !isThought && !isBash && !isEdit && !isObservation

            when {
                // Assistant message: plain text, no card, no badge.
                isMessage -> {
                    SelectionContainer {
                        Text(
                            text = event.content,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp,
                            modifier = modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        )
                    }
                }
                // Thinking: one muted line that expands inline.
                isThought -> {
                    Column(modifier = modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { isExpanded = !isExpanded }
                                .semantics {
                                    stateDescription = if (isExpanded) "Expanded" else "Collapsed"
                                    role = Role.Button
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isExpanded) "Thinking" else "Thinking · " + event.content.lineSequence().firstOrNull().orEmpty(),
                                fontSize = 12.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        AnimatedVisibility(visible = isExpanded) {
                            Text(
                                text = event.content,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(start = 20.dp, bottom = 6.dp)
                            )
                        }
                    }
                }
                // Tool activity: one-line collapsible row - icon + name + short arg.
                else -> {
                    val (icon, toolLabel, tint) = when {
                        isBash -> Triple(Icons.Default.Terminal, "Bash", MaterialTheme.colorScheme.primary)
                        isEdit -> Triple(Icons.Default.Edit, "Edit", MaterialTheme.colorScheme.tertiary)
                        else -> Triple(Icons.Default.Article, "Output", MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val shortArg = event.content.lineSequence().firstOrNull().orEmpty().take(80)
                    Column(modifier = modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { isExpanded = !isExpanded }
                                .semantics {
                                    stateDescription = if (isExpanded) "Expanded" else "Collapsed"
                                    role = Role.Button
                                }
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = toolLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = tint
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = shortArg,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(start = 20.dp, bottom = 6.dp)) {
                                if (event.content != shortArg || event.content.contains('\n')) {
                                    Text(
                                        text = event.content,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 17.sp
                                    )
                                }
                                if (!event.detail.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    TerminalCodeBlock(
                                        codeText = (if (isBash) "\$ " + event.content + "\n" else "") + event.detail,
                                        title = if (isBash) "terminal" else "detail"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

/**
 * A horizontally scrolling row of chips with an edge fade that hints there is
 * more content off-screen.
 *
 * A plain [LazyRow] clips its last chip with no cue that it can be swiped, which
 * reads as a broken/truncated label on a phone. The fade is drawn only on the
 * side(s) that still have hidden items, and it disappears once the list is fully
 * scrolled to that edge.
 */
@Composable
fun EdgeFadeRow(
    state: LazyListState,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(6.dp),
    content: LazyListScope.() -> Unit,
) {
    val showStart by remember {
        derivedStateOf { state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0 }
    }
    val showEnd by remember {
        derivedStateOf {
            val last = state.layoutInfo.visibleItemsInfo.lastOrNull()
            last != null && last.index < state.layoutInfo.totalItemsCount - 1
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            state = state,
            horizontalArrangement = horizontalArrangement,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxWidth(),
            content = content,
        )
        ScrollEdgeFade(
            visible = showStart,
            alignment = Alignment.CenterStart,
        )
        ScrollEdgeFade(
            visible = showEnd,
            alignment = Alignment.CenterEnd,
        )
    }
}

@Composable
private fun BoxScope.ScrollEdgeFade(
    visible: Boolean,
    alignment: Alignment,
) {
    val background = MaterialTheme.colorScheme.surface
    // matchParentSize() sizes to the row without contributing to the Box's
    // measured height; using fillMaxHeight() here would inflate the wrap-content
    // Box to the incoming max height and push sibling content off-screen.
    Box(modifier = Modifier.matchParentSize()) {
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(alignment).fillMaxHeight().width(20.dp),
        ) {
            val brush = if (alignment == Alignment.CenterEnd) {
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(Color.Transparent, background)
                )
            } else {
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(background, Color.Transparent)
                )
            }
            Box(modifier = Modifier.fillMaxSize().background(brush))
        }
    }
}
