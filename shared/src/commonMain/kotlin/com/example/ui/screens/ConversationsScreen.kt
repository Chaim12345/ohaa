package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.sp
import com.example.data.model.ConversationUiStatus
import com.example.data.model.OpenHandsConversation
import com.example.ui.components.EmptyState
import com.example.ui.theme.CanvasBorder
import com.example.ui.theme.CanvasPrimary
import com.example.ui.theme.Shapes
import com.example.ui.theme.Sizes
import com.example.ui.theme.Spacing

/**
 * Conversation list with search and status filters.
 *
 * Rebuilt to reuse [ConversationCard] so rows match the dashboard exactly, and
 * so a row change lands in one place. Filter chips scroll horizontally rather
 * than wrapping, which keeps the list above the fold on a phone.
 */
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
    onStartNewTask: () -> Unit
) {
    val filtered = conversations
        .filter { statusFilter == null || it.uiStatus == statusFilter }
        .filter { q ->
            searchQuery.isBlank() ||
                q.title.contains(searchQuery, ignoreCase = true) ||
                (q.repository?.contains(searchQuery, ignoreCase = true) ?: false)
        }
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("conversations_screen")
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Search conversations") },
            singleLine = true,
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChanged("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            shape = Shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CanvasPrimary,
                unfocusedBorderColor = CanvasBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screen, vertical = Spacing.sm)
                .testTag("search_input")
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen)
        ) {
            FilterChip(
                selected = statusFilter == null,
                onClick = { onStatusFilterChanged(null) },
                label = { Text("All", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CanvasPrimary.copy(alpha = 0.18f)
                ),
                modifier = Modifier.testTag("filter_all")
            )
            ConversationUiStatus.entries
                .filter { it != ConversationUiStatus.UNKNOWN }
                .forEach { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = {
                            onStatusFilterChanged(if (statusFilter == status) null else status)
                        },
                        label = { Text(status.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CanvasPrimary.copy(alpha = 0.18f)
                        ),
                        modifier = Modifier.testTag("filter_${status.name.lowercase()}")
                    )
                }
        }

        Spacer(Modifier.height(Spacing.sm))

        if (filtered.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Search,
                title = if (conversations.isEmpty()) "No conversations" else "No matches",
                message = if (conversations.isEmpty()) {
                    "Launch a task to start working with an agent."
                } else {
                    "Try a different search term or clear the filters."
                }
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = Spacing.screen,
                    end = Spacing.screen,
                    bottom = Spacing.screen
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.cardGap),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("conversations_list")
            ) {
                item {
                    Text(
                        "${filtered.size} conversation${if (filtered.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(filtered, key = { it.id }) { conv ->
                    ConversationCard(conv) { onSelectConversation(conv.id) }
                }
            }
        }
    }
}
