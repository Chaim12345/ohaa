package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Conversation
import com.example.data.model.ConversationStatus
import com.example.ui.components.EmptyState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatCost
import com.example.ui.components.formatRelativeTime

@Composable
fun ConversationsScreen(
    conversations: List<Conversation>,
    searchQuery: String,
    statusFilter: ConversationStatus?,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    error: String?,
    lastSyncedAt: Long?,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (ConversationStatus?) -> Unit,
    onOpen: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onNew: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filtered = if (statusFilter == null) conversations else conversations.filter { it.status == statusFilter }
    var renameTarget by remember { mutableStateOf<Conversation?>(null) }
    var deleteTarget by remember { mutableStateOf<Conversation?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text("Conversations", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    if (lastSyncedAt != null) "Synced ${formatRelativeTime(lastSyncedAt)}" else "Not synced yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onRefresh, enabled = !isRefreshing, modifier = Modifier.testTag("refresh_conversations")) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onNew, modifier = Modifier.testTag("new_conversation_fab")) {
                    Icon(Icons.Default.Add, contentDescription = "New conversation", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Search titles on the server…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) IconButton(onClick = { onSearchChanged("") }) { Icon(Icons.Default.Clear, contentDescription = "Clear") }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().testTag("conversations_search")
        )

        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            item {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { onStatusFilterChanged(null) },
                    label = { Text("All (${conversations.size})") },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), selectedLabelColor = MaterialTheme.colorScheme.primary)
                )
            }
            val options = listOf(
                ConversationStatus.RUNNING, ConversationStatus.WAITING_FOR_CONFIRMATION, ConversationStatus.IDLE,
                ConversationStatus.FINISHED, ConversationStatus.PAUSED, ConversationStatus.ERROR, ConversationStatus.STUCK
            )
            items(options) { status ->
                val count = conversations.count { it.status == status }
                if (count > 0 || statusFilter == status) {
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { onStatusFilterChanged(if (statusFilter == status) null else status) },
                        label = { Text("${status.label} ($count)") },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), selectedLabelColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        if (isRefreshing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = MaterialTheme.colorScheme.primary)
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            ErrorBanner(error, actionLabel = "Retry", onAction = onRefresh)
        }
        Spacer(Modifier.height(6.dp))

        if (filtered.isEmpty() && !isRefreshing) {
            EmptyState(
                title = if (conversations.isEmpty()) "No conversations yet" else "Nothing matches this filter",
                subtitle = if (conversations.isEmpty()) "Start one and the agent will work in a cloud sandbox." else "Clear the filter to see everything.",
                modifier = Modifier.weight(1f)
            ) {
                Button(onClick = onNew, shape = RoundedCornerShape(12.dp), modifier = Modifier.testTag("empty_state_new")) {
                    Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("New conversation")
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { conv ->
                    ConversationCard(
                        conversation = conv,
                        onClick = { onOpen(conv.id) },
                        onRename = { renameTarget = conv },
                        onDelete = { deleteTarget = conv }
                    )
                }
                if (hasMore && statusFilter == null) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            if (isLoadingMore) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            else OutlinedButton(onClick = onLoadMore, shape = RoundedCornerShape(12.dp)) { Text("Load more") }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    renameTarget?.let { conv ->
        RenameDialog(
            current = conv.displayTitle,
            onDismiss = { renameTarget = null },
            onConfirm = { onRename(conv.id, it); renameTarget = null }
        )
    }
    deleteTarget?.let { conv ->
        ConfirmDialog(
            title = "Delete conversation?",
            text = "\"${conv.displayTitle}\" and its sandbox workspace will be deleted from OpenHands Cloud. This cannot be undone.",
            confirmLabel = "Delete",
            onDismiss = { deleteTarget = null },
            onConfirm = { onDelete(conv.id); deleteTarget = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationCard(
    conversation: Conversation,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
                .testTag("conversation_${conversation.id}")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        conversation.displayTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(status = conversation.status)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    if (!conversation.repository.isNullOrBlank()) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            conversation.repository + (conversation.branch?.let { "@$it" } ?: ""),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(10.dp))
                    } else {
                        Text("No repository", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Text(formatRelativeTime(conversation.updatedAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("ID ${conversation.id.take(8)}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(10.dp))
                    Text("Sandbox ${conversation.sandboxStatus.label.lowercase()}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text(formatCost(conversation.cost), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text("Open") }, onClick = { menuOpen = false; onClick() })
            DropdownMenuItem(text = { Text("Rename") }, onClick = { menuOpen = false; onRename() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
        }
    }
}

@Composable
fun RenameDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename conversation") },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("rename_input"))
        },
        confirmButton = { TextButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
