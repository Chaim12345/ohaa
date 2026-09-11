package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentEvent
import com.example.data.model.ConversationUiStatus
import com.example.ui.theme.CanvasBorder
import com.example.ui.theme.CanvasDanger
import com.example.ui.theme.CanvasPrimary
import com.example.ui.theme.CanvasSecondary
import com.example.ui.theme.CanvasSuccess
import com.example.ui.theme.CanvasTerminalBg
import com.example.ui.theme.CanvasWarning
import com.example.ui.theme.CodeMedium

fun statusColor(status: ConversationUiStatus): Color = when (status) {
    ConversationUiStatus.WORKING, ConversationUiStatus.RUNNING -> CanvasWarning
    ConversationUiStatus.STARTING -> CanvasPrimary
    ConversationUiStatus.WAITING_INPUT -> CanvasSecondary
    ConversationUiStatus.PAUSED -> CanvasBorder
    ConversationUiStatus.IDLE -> CanvasPrimary
    ConversationUiStatus.FINISHED -> CanvasSuccess
    ConversationUiStatus.ERROR -> CanvasDanger
    ConversationUiStatus.UNKNOWN -> CanvasBorder
}

@Composable
fun StatusChip(status: ConversationUiStatus, modifier: Modifier = Modifier) {
    val color = statusColor(status)
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier.testTag("status_chip_${status.name}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(6.dp))
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
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TerminalCodeBlock(
    codeText: String,
    title: String = "output",
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CanvasTerminalBg)
            .border(1.dp, CanvasBorder, RoundedCornerShape(10.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Dot(Color(0xFFFF5F56)); Spacer(Modifier.width(4.dp))
            Dot(Color(0xFFFFBD2E)); Spacer(Modifier.width(4.dp))
            Dot(Color(0xFF27C93F)); Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    clipboard.setText(AnnotatedString(codeText))
                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy output",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(10.dp)
        ) {
            Text(
                text = codeText,
                style = CodeMedium,
                color = Color(0xFF7EE787)
            )
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
}

@Composable
fun EventItemView(event: AgentEvent, modifier: Modifier = Modifier) {
    var expanded by remember(event.id) { mutableStateOf(true) }
    val source = event.source.lowercase()

    if (source == "user") {
        Row(
            modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Card(
                shape = RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp),
                colors = CardDefaults.cardColors(containerColor = CanvasPrimary.copy(alpha = 0.16f)),
                border = BorderStroke(1.dp, CanvasPrimary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(0.88f)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "You",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CanvasPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(event.content, fontSize = 14.sp, lineHeight = 19.sp)
                }
            }
        }
        return
    }

    val isThought = event.actionType?.equals("thought", ignoreCase = true) == true ||
        event.actionType?.equals("AgentThinking", ignoreCase = true) == true
    val isBash = event.actionType?.equals("bash", ignoreCase = true) == true ||
        event.actionType?.equals("execute", ignoreCase = true) == true
    val isEdit = event.actionType?.equals("edit", ignoreCase = true) == true ||
        event.actionType?.equals("str_replace_editor", ignoreCase = true) == true

    val (icon, badge, color) = when {
        isThought -> Triple(Icons.Default.Lightbulb, "AGENT REASONING", CanvasSecondary)
        isBash -> Triple(Icons.Default.Terminal, "TOOL: BASH", CanvasPrimary)
        isEdit -> Triple(Icons.Default.PlayArrow, "TOOL: EDIT", CanvasSuccess)
        event.isError -> Triple(Icons.Default.ErrorOutline, "ERROR", CanvasDanger)
        else -> Triple(Icons.Default.CheckCircle, "AGENT", CanvasPrimary)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .semantics {
                        role = Role.Button
                        stateDescription = if (expanded) "Expanded" else "Collapsed"
                    }
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = color,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp)
                )
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(top = 8.dp)) {
                    Text(
                        event.content,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    if (!event.detail.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        TerminalCodeBlock(
                            codeText = event.detail,
                            title = if (isBash) "terminal output" else if (isEdit) "diff" else "output"
                        )
                    }
                }
            }
        }
    }
}
