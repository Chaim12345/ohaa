package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ConversationUiStatus
import com.example.data.model.OpenHandsConversation
import com.example.data.model.PromptTemplate
import com.example.data.model.SuggestedTaskDto
import com.example.ui.components.StatCard
import com.example.ui.components.StatusChip
import com.example.ui.theme.CanvasPrimary
import com.example.ui.theme.CanvasPrimaryDeep
import com.example.ui.theme.CanvasSuccess
import com.example.util.formatUsd
import com.example.util.relativeTime

@OptIn(ExperimentalMaterial3Api::class)
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
    onOpenSettings: () -> Unit,
    suggestedTasks: List<SuggestedTaskDto> = emptyList(),
    onSelectSuggestedTask: (SuggestedTaskDto) -> Unit = {}
) {
    val active = conversations.filter {
        it.uiStatus == ConversationUiStatus.WORKING || it.uiStatus == ConversationUiStatus.RUNNING
    }
    val done = conversations.filter { it.uiStatus.isTerminal }
    val spend = conversations.sumOf { it.cost }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Agent Canvas", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (isDemoMode) "Demo data — connect Cloud in Settings"
                        else "OpenHands Cloud",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRefresh, modifier = Modifier.testTag("btn_refresh")) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        }
        item {
            ConnectionCard(
                isDemoMode = isDemoMode,
                healthy = connectionHealthy,
                message = connectionStatus,
                onOpenSettings = onOpenSettings,
                onRetry = onRefresh
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = "Active",
                    value = "${active.size}",
                    subtitle = "running now",
                    icon = Icons.Default.PlayArrow,
                    accent = CanvasSuccess,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Finished",
                    value = "${done.size}",
                    subtitle = "completed",
                    icon = Icons.Default.CloudDone,
                    accent = CanvasPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Spend",
                    value = formatUsd(spend),
                    subtitle = "this workspace",
                    icon = Icons.Default.Schedule,
                    accent = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (conversations.isEmpty()) {
            item {
                EmptyWorkspaceCard(
                    isDemoMode = isDemoMode,
                    onStartNewTask = onStartNewTask,
                    onOpenSettings = onOpenSettings
                )
            }
        }
        if (active.isNotEmpty()) {
            item {
                Column {
                    Text(
                        "Continue work",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        active.take(3).forEach { conv ->
                            ConversationRow(conv, onClick = { onSelectConversation(conv.id) })
                        }
                    }
                }
            }
        }
        if (suggestedTasks.isNotEmpty()) {
            item {
                Column {
                    Text(
                        "Suggested from your repos",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestedTasks.take(4).forEach { task ->
                            SuggestedTaskRow(task = task, onClick = { onSelectSuggestedTask(task) })
                        }
                    }
                }
            }
        }
        item {
            Text(
                "Start with a template",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(templates, key = { it.id }) { template ->
                    TemplateCard(template = template, onClick = { onSelectTemplate(template) })
                }
            }
        }
        item {
            Button(
                onClick = onStartNewTask,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_start_new_task"),
                colors = ButtonDefaults.buttonColors(containerColor = CanvasPrimaryDeep)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Launch a new agent task", fontWeight = FontWeight.SemiBold)
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
    }
}

@Composable
private fun ConnectionCard(
    isDemoMode: Boolean,
    healthy: Boolean?,
    message: String?,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    listOf(
                        CanvasPrimaryDeep.copy(alpha = 0.18f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Icon(
                imageVector = when {
                    isDemoMode -> Icons.Default.CloudOff
                    healthy == true -> Icons.Default.CloudDone
                    else -> Icons.Default.CloudOff
                },
                contentDescription = null,
                tint = if (!isDemoMode && healthy == true) CanvasSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = when {
                        isDemoMode -> "Demo mode"
                        healthy == true -> "Cloud connected"
                        healthy == false -> "Connection issue"
                        else -> "Not connected"
                    },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                message?.let {
                    Text(
                        it,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (!isDemoMode && healthy != true) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.testTag("btn_retry_connection")
                ) {
                    Text("Retry", fontSize = 12.sp)
                }
                Spacer(Modifier.width(6.dp))
            }
            OutlinedButton(onClick = onOpenSettings) {
                Text(if (isDemoMode) "Connect" else "Manage", fontSize = 12.sp)
            }
        }
    }
}

/** Shown on the dashboard when the cloud account has no conversations yet. */
@Composable
private fun EmptyWorkspaceCard(
    isDemoMode: Boolean,
    onStartNewTask: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_empty_state")
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                if (isDemoMode) "No sample conversations" else "No conversations yet",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (isDemoMode)
                    "Connect your OpenHands Cloud API key to see your real agent runs here."
                else
                    "Launch your first agent task and it will show up here with live status, diffs, and spend.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStartNewTask,
                    colors = ButtonDefaults.buttonColors(containerColor = CanvasPrimaryDeep)
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New task")
                }
                OutlinedButton(onClick = onOpenSettings) {
                    Text(if (isDemoMode) "Connect cloud" else "Settings")
                }
            }
        }
    }
}

/**
 * A provider-suggested task (failing check, open issue). Tapping it prefills the
 * New Task form so the user never has to retype a repo or title.
 */
@Composable
private fun SuggestedTaskRow(task: SuggestedTaskDto, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("suggested_task_${task.repo}_${task.issueNumber}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(CanvasPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Bolt,
                    null,
                    tint = CanvasPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    buildString {
                        append(task.repo)
                        val type = task.taskType?.replace('_', ' ')
                        if (!type.isNullOrBlank()) append(" · $type")
                        if (task.issueNumber > 0) append(" #${task.issueNumber}")
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * One conversation card. [onRename] / [onDelete] are optional so the dashboard
 * can reuse the row without an overflow menu while the task list offers both.
 */
@Composable
fun ConversationRow(
    conversation: OpenHandsConversation,
    onClick: () -> Unit,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("conversation_${conversation.id}")
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(conversation.uiStatus)
                if (onRename != null || onDelete != null) {
                    Box {
                        IconButton(
                            onClick = { menuOpen = true },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("conversation_menu_${conversation.id}")
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Conversation actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            onRename?.let { rename ->
                                DropdownMenuItem(
                                    text = { Text("Rename") },
                                    onClick = {
                                        menuOpen = false
                                        rename()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DriveFileRenameOutline,
                                            null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("menu_rename_${conversation.id}")
                                )
                            }
                            onDelete?.let { delete ->
                                DropdownMenuItem(
                                    text = {
                                        Text("Delete", color = MaterialTheme.colorScheme.error)
                                    },
                                    onClick = {
                                        menuOpen = false
                                        delete()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("menu_delete_${conversation.id}")
                                )
                            }
                        }
                    }
                }
            }
            conversation.repository?.let {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Folder,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        it,
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            conversation.lastMessage?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    it,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    relativeTime(conversation.updatedAt),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TemplateCard(template: PromptTemplate, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .width(230.dp)
            .clickable(onClick = onClick)
            .testTag("template_${template.id}")
    ) {
        Column(Modifier.padding(12.dp)) {
            AssistChip(
                onClick = onClick,
                label = { Text(template.category, fontSize = 10.sp) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = CanvasPrimary.copy(alpha = 0.12f),
                    labelColor = CanvasPrimary
                ),
                border = null
            )
            Spacer(Modifier.height(8.dp))
            Text(template.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                template.prompt,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 15.sp
            )
        }
    }
}
