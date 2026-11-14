package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConversationStatus
import com.example.data.model.OpenHandsConversation
import com.example.ui.components.StatusBadge
import com.example.ui.formatRelativeTime
import com.example.ui.theme.OpenHandsPrimaryDark

@Composable
fun TasksScreen(
    conversations: List<OpenHandsConversation>,
    searchQuery: String,
    statusFilter: ConversationStatus?,
    onSearchChanged: (String) -> Unit,
    onStatusFilterChanged: (ConversationStatus?) -> Unit,
    onSelectConversation: (String) -> Unit,
    onStartNewTask: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filtered = conversations.filter { conv ->
        val matchesSearch = searchQuery.isBlank() ||
                conv.title.contains(searchQuery, ignoreCase = true) ||
                (conv.repository?.contains(searchQuery, ignoreCase = true) == true)
        val matchesStatus = statusFilter == null || conv.status == statusFilter
        matchesSearch && matchesStatus
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Title row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Agent Tasks",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Active sandboxes & execution history",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("refresh_tasks_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh tasks",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Search by task title or repository...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = OpenHandsPrimaryDark,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tasks_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { onStatusFilterChanged(null) },
                    label = { Text("All (${conversations.size})") },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OpenHandsPrimaryDark.copy(alpha = 0.2f),
                        selectedLabelColor = OpenHandsPrimaryDark
                    )
                )
            }
            val filterOptions = listOf(
                ConversationStatus.RUNNING,
                ConversationStatus.AWAITING_CONFIRMATION,
                ConversationStatus.READY,
                ConversationStatus.PAUSED,
                ConversationStatus.FINISHED,
                ConversationStatus.ERROR
            )
            items(filterOptions) { status ->
                val count = conversations.count { it.status == status }
                FilterChip(
                    selected = statusFilter == status,
                    onClick = {
                        onStatusFilterChanged(if (statusFilter == status) null else status)
                    },
                    label = { Text("${status.label} ($count)") },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = OpenHandsPrimaryDark.copy(alpha = 0.2f),
                        selectedLabelColor = OpenHandsPrimaryDark
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No matching agent tasks",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Try clearing filters or start a new task in OpenHands.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onStartNewTask,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("empty_state_new_task")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Task")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { conv ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectConversation(conv.id) }
                            .testTag("task_item_${conv.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = conv.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                StatusBadge(status = conv.status)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (!conv.repository.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Code,
                                            contentDescription = null,
                                            tint = OpenHandsPrimaryDark,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = conv.repository,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = OpenHandsPrimaryDark
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                                Text(
                                    text = "ID: ${conv.id.take(8)}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                Text(
                                    text = formatRelativeTime(conv.updatedAt),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!conv.branch.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "branch: ${conv.branch}",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
