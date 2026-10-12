package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.example.data.model.ConversationUiStatus
import com.example.data.model.OpenHandsConversation
import com.example.data.model.PromptTemplate
import com.example.ui.components.CanvasCard
import com.example.ui.components.EmptyState
import com.example.ui.components.MetaChip
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusDot
import com.example.ui.theme.CanvasColors
import com.example.ui.theme.CanvasPrimaryDeep
import com.example.ui.theme.Shapes
import com.example.ui.theme.Sizes
import com.example.ui.theme.Spacing
import com.example.util.relativeTime

/**
 * Dashboard — the app's front door.
 *
 * Rebuilt around what a returning user needs first: are my agents running, and
 * what needs me? The old layout led with decorative stat tiles; this leads with
 * the work in progress.
 */
@Composable
fun DashboardScreen(
    conversations: List<OpenHandsConversation>,
    templates: List<PromptTemplate>,
    connectionStatus: String?,
    connectionHealthy: Boolean?,
    isDemoMode: Boolean = false,
    isRefreshing: Boolean = false,
    onStartNewTask: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onSelectTemplate: (PromptTemplate) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val running = conversations.filter {
        it.uiStatus == ConversationUiStatus.WORKING ||
            it.uiStatus == ConversationUiStatus.RUNNING
    }
    val needsYou = conversations.filter {
        it.uiStatus == ConversationUiStatus.WAITING_INPUT
    }
    val recent = conversations.take(6)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)
    ) {
        item { ConnectionBanner(connectionHealthy, connectionStatus, isDemoMode, onOpenSettings) }

        if (needsYou.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Needs you",
                    subtitle = "${needsYou.size} waiting on input"
                )
            }
            items(needsYou, key = { "needs-${it.id}" }) { conv ->
                ConversationCard(conv, highlight = true) { onSelectConversation(conv.id) }
            }
        }

        if (running.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Running",
                    subtitle = "${running.size} active",
                    trailing = { RefreshButton(isRefreshing, onRefresh) }
                )
            }
            items(running, key = { "run-${it.id}" }) { conv ->
                ConversationCard(conv) { onSelectConversation(conv.id) }
            }
        }

        item {
            Button(
                onClick = onStartNewTask,
                shape = Shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = CanvasPrimaryDeep),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Sizes.touchTarget)
                    .testTag("btn_new_task")
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(Sizes.iconMedium)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text("New task", fontWeight = FontWeight.Medium)
            }
        }

        if (templates.isNotEmpty()) {
            item { SectionHeader(title = "Quick start", subtitle = "Saved prompts") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(templates, key = { it.id }) { template ->
                        TemplateChip(template) { onSelectTemplate(template) }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Recent",
                trailing = { RefreshButton(isRefreshing, onRefresh) }
            )
        }

        if (recent.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.CloudOff,
                    title = "No conversations yet",
                    message = if (isDemoMode) {
                        "Add your OpenHands Cloud API key in Settings to get started."
                    } else {
                        "Launch a task and it will appear here."
                    }
                )
            }
        } else {
            items(recent, key = { "recent-${it.id}" }) { conv ->
                ConversationCard(conv) { onSelectConversation(conv.id) }
            }
        }
    }
}

@Composable
private fun RefreshButton(isRefreshing: Boolean, onRefresh: () -> Unit) {
    IconButton(
        onClick = onRefresh,
        enabled = !isRefreshing,
        modifier = Modifier.testTag("btn_refresh")
    ) {
        Icon(
            Icons.Default.Refresh,
            contentDescription = "Refresh",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Sizes.iconMedium)
        )
    }
}

@Composable
private fun ConnectionBanner(
    healthy: Boolean?,
    status: String?,
    isDemoMode: Boolean,
    onOpenSettings: () -> Unit
) {
    val connected = healthy == true && !isDemoMode
    CanvasCard(
        onClick = onOpenSettings,
        accent = if (connected) CanvasColors.success else CanvasColors.warning
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (connected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                contentDescription = null,
                tint = if (connected) CanvasColors.success else CanvasColors.warning,
                modifier = Modifier.size(Sizes.iconMedium)
            )
            Spacer(Modifier.width(Spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(
                    if (connected) "OpenHands Cloud" else "Not connected",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 15.sp
                )
                Text(
                    status ?: if (connected) "Syncing conversations" else "Tap to add an API key",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!connected) {
                Text(
                    "Connect",
                    style = MaterialTheme.typography.labelMedium,
                    color = CanvasColors.warning
                )
            }
        }
    }
}

@Composable
private fun TemplateChip(template: PromptTemplate, onClick: () -> Unit) {
    Box(
        Modifier
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, Shapes.medium)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        Column {
            Text(template.title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                template.category,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Conversation row. Status is shown as a dot *and* a label so it is not
 * conveyed by colour alone.
 */
@Composable
fun ConversationCard(
    conversation: OpenHandsConversation,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    val color = when (conversation.uiStatus) {
        ConversationUiStatus.WAITING_INPUT -> CanvasColors.warning
        ConversationUiStatus.RUNNING, ConversationUiStatus.WORKING -> CanvasColors.success
        ConversationUiStatus.ERROR -> CanvasColors.danger
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    CanvasCard(
        onClick = onClick,
        accent = if (highlight) CanvasColors.warning else null,
        modifier = Modifier.testTag("conversation_row")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                conversation.title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(Spacing.sm))
            StatusDot(color = color, label = conversation.uiStatus.label)
        }
        Spacer(Modifier.height(Spacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            conversation.repository?.takeIf { it.isNotBlank() }?.let { repo ->
                MetaChip(repo, modifier = Modifier.testTag("conversation_repo"))
                Spacer(Modifier.width(Spacing.xs))
            }
            val msg = conversation.lastMessage?.takeIf { it.isNotBlank() }
            if (msg != null) {
                Text(
                    msg,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.width(Spacing.sm))
            Text(
                relativeTime(conversation.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
