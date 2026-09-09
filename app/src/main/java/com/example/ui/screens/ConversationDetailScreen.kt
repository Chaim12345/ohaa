package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentEvent
import com.example.data.model.OpenHandsConversation
import com.example.ui.components.EventItemView
import com.example.ui.components.StatusChip
import com.example.ui.theme.CanvasBorder
import com.example.ui.theme.CanvasPrimary
import com.example.ui.theme.CanvasPrimaryDeep

@Composable
fun ConversationDetailScreen(
    conversation: OpenHandsConversation?,
    events: List<AgentEvent>,
    isSending: Boolean,
    isDemoMode: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSendMessage: (String) -> Unit,
    onDelete: () -> Unit
) {
    val listState = rememberLazyListState()
    var composerText by remember { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    // Keep the newest event visible as the transcript grows, but never yank
    // the user away from history they scrolled up to read.
    LaunchedEffect(events.size) {
        if (events.isEmpty()) return@LaunchedEffect
        val lastIndex = events.size - 1
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (lastVisible >= lastIndex - 1) listState.animateScrollToItem(lastIndex)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .testTag("conversation_detail_screen")
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 6.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(Modifier.weight(1f)) {
                Text(
                    conversation?.title ?: "Conversation",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    conversation?.let { StatusChip(it.uiStatus) }
                    conversation?.repository?.let { repo ->
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Folder,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            repo,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
            IconButton(onClick = { showDeleteConfirmation = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete conversation",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        // Timeline
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isDemoMode) {
                item {
                    DemoBanner()
                }
            }
            if (events.isEmpty()) {
                item {
                    Text(
                        "No events yet — the agent transcript will appear here as it works.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }
            items(events, key = { it.id }) { event ->
                EventItemView(event)
            }
            if (conversation?.uiStatus == com.example.data.model.ConversationUiStatus.WAITING_INPUT) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = com.example.ui.theme.CanvasSecondary.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "The agent needs your confirmation in the OpenHands web UI to continue.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            TextButton(onClick = {
                                conversation?.let {
                                    uriHandler.openUri("https://app.all-hands.dev/conversations/${it.id}")
                                }
                            }) {
                                Text("Open in web app")
                            }
                        }
                    }
                }
            }
        }

        // Composer
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            OutlinedTextField(
                value = composerText,
                onValueChange = { composerText = it },
                placeholder = { Text("Send a follow-up…") },
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CanvasPrimary,
                    unfocusedBorderColor = CanvasBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("composer_input")
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    onSendMessage(composerText.trim())
                    composerText = ""
                },
                enabled = !isSending && composerText.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = CanvasPrimaryDeep
                ),
                modifier = Modifier.testTag("btn_send")
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(17.dp))
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete conversation?") },
            text = { Text("This removes the cached transcript from this device and deletes the cloud conversation.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    modifier = Modifier.testTag("btn_confirm_delete")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DemoBanner() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "Demo conversation — connect your OpenHands Cloud API key in Settings to run live agents.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(10.dp)
        )
    }
}
