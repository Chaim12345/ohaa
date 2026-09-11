package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConversationUiStatus
import com.example.data.model.OpenHandsConversation
import com.example.ui.theme.CanvasPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    conversations: List<OpenHandsConversation>,
    searchQuery: String,
    statusFilter: ConversationUiStatus?,
    isRefreshing: Boolean,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (ConversationUiStatus?) -> Unit,
    onRefresh: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onStartNewTask: () -> Unit,
    onRenameConversation: (String, String) -> Unit = { _, _ -> },
    onDeleteConversation: (String) -> Unit = {}
) {
    var renamingConversation by remember { mutableStateOf<OpenHandsConversation?>(null) }
    var deletingConversation by remember { mutableStateOf<OpenHandsConversation?>(null) }
    val hasActiveFilters = searchQuery.isNotBlank() || statusFilter != null
    val filtered = conversations
        .filter { statusFilter == null || it.uiStatus == statusFilter }
        .filter { q ->
            searchQuery.isBlank() ||
                q.title.contains(searchQuery, ignoreCase = true) ||
                (q.repository?.contains(searchQuery, ignoreCase = true) ?: false)
        }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("conversations_screen")
    ) {
        Text(
            "Conversations",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Search tasks or repositories…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CanvasPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("search_field")
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
                .testTag("status_filters")
        ) {
            items(
                listOf(
                    ConversationUiStatus.RUNNING,
                    ConversationUiStatus.WORKING,
                    ConversationUiStatus.STARTING,
                    ConversationUiStatus.WAITING_INPUT,
                    ConversationUiStatus.FINISHED,
                    ConversationUiStatus.ERROR
                )
            ) { status ->
                FilterChip(
                    selected = statusFilter == status,
                    onClick = {
                        onStatusFilterChanged(if (statusFilter == status) null else status)
                    },
                    label = { Text(status.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CanvasPrimary.copy(alpha = 0.18f),
                        selectedLabelColor = CanvasPrimary
                    )
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "${filtered.size} of ${conversations.size} tasks",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (hasActiveFilters) {
                TextButton(
                    onClick = {
                        onSearchChanged("")
                        onStatusFilterChanged(null)
                    },
                    modifier = Modifier.testTag("btn_clear_filters")
                ) {
                    Text("Clear filters", fontSize = 12.sp)
                }
            }
        }

        // Native pull-to-refresh gesture over the list area (filters stay fixed).
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("conversations_refresh")
        ) {
            if (filtered.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CloudOff,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(40.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (searchQuery.isBlank() && statusFilter == null) "No conversations yet"
                        else "Nothing matches your filters",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    if (hasActiveFilters) {
                        TextButton(
                            onClick = {
                                onSearchChanged("")
                                onStatusFilterChanged(null)
                            },
                            modifier = Modifier.testTag("btn_clear_filters_empty")
                        ) {
                            Text("Clear filters")
                        }
                    } else {
                        Button(onClick = onStartNewTask) {
                            Icon(Icons.Default.Add, null)
                            Text("  Launch your first task")
                        }
                    }
                }
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.id }) { conv ->
                        ConversationRow(
                            conversation = conv,
                            onClick = { onSelectConversation(conv.id) },
                            onRename = { renamingConversation = conv },
                            onDelete = { deletingConversation = conv }
                        )
                    }
                }
            }
        }
    }

    renamingConversation?.let { conv ->
        RenameConversationDialog(
            currentTitle = conv.title,
            onDismiss = { renamingConversation = null },
            onConfirm = { newTitle ->
                renamingConversation = null
                onRenameConversation(conv.id, newTitle)
            }
        )
    }

    deletingConversation?.let { conv ->
        AlertDialog(
            onDismissRequest = { deletingConversation = null },
            title = { Text("Delete conversation?") },
            text = {
                Text(
                    "'${conv.title}' and its cached transcript will be removed " +
                        "from this device and from OpenHands Cloud."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletingConversation = null
                        onDeleteConversation(conv.id)
                    },
                    modifier = Modifier.testTag("btn_confirm_delete_conversation")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingConversation = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun RenameConversationDialog(
    currentTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember(currentTitle) { mutableStateOf(currentTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename conversation") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CanvasPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("field_rename_conversation")
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title.trim()) },
                enabled = title.isNotBlank() && title.trim() != currentTitle,
                modifier = Modifier.testTag("btn_confirm_rename_conversation")
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}