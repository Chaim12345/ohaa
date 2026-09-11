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
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentEvent
import com.example.data.model.GitChangeDto
import com.example.data.model.GitDiffDto
import com.example.data.model.OpenHandsConversation
import com.example.data.settings.AgentKind
import com.example.data.settings.AgentProfile
import com.example.ui.components.AgentProfileBadge
import com.example.ui.components.AgentProfileDialog
import com.example.ui.components.EventItemView
import com.example.ui.components.StatusChip
import com.example.ui.theme.CanvasBorder
import com.example.ui.theme.CanvasPrimary
import com.example.ui.theme.CanvasPrimaryDeep
import com.example.util.formatUsd
import com.example.util.relativeTime

@Composable
fun ConversationDetailScreen(
    conversation: OpenHandsConversation?,
    events: List<AgentEvent>,
    isSending: Boolean,
    isDemoMode: Boolean,
    activeProfile: AgentProfile,
    gitDiff: GitDiffDto?,
    isGitLoading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSendMessage: (String) -> Unit,
    onDelete: () -> Unit,
    onRefreshGit: () -> Unit,
    onAgentKindChanged: (AgentKind) -> Unit,
    onModelChanged: (String) -> Unit,
    onAcpCommandChanged: (String) -> Unit,
    onResetProfile: () -> Unit,
    // Per-file unified diffs, loaded on demand when a changed file is expanded.
    fileDiffs: Map<String, String> = emptyMap(),
    loadingDiffPath: String? = null,
    onLoadFileDiff: (String) -> Unit = {}
) {
    val listState = rememberLazyListState()
    var composerText by remember { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
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
                conversation?.let { conv ->
                    val meta = buildList {
                        if (conv.createdAt > 0L) add("Started ${relativeTime(conv.createdAt)}")
                        if (conv.cost > 0.0) add(formatUsd(conv.cost))
                    }
                    if (meta.isNotEmpty()) {
                        Text(
                            meta.joinToString(" · "),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("conversation_meta")
                        )
                    }
                }
            }
            AgentProfileBadge(profile = activeProfile, onOpen = { showProfileDialog = true })
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
            IconButton(
                onClick = onRefreshGit,
                enabled = !isGitLoading,
                modifier = Modifier.testTag("btn_git_changes")
            ) {
                Icon(Icons.Default.Sync, contentDescription = "Pull git changes")
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
            gitDiff?.files?.takeIf { it.isNotEmpty() }?.let { files ->
                item {
                    GitChangesCard(
                        files = files,
                        fileDiffs = fileDiffs,
                        loadingPath = loadingDiffPath,
                        onRequestDiff = onLoadFileDiff
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

    if (showProfileDialog) {
        AgentProfileDialog(
            profile = activeProfile,
            onDismiss = { showProfileDialog = false },
            onAgentKindChanged = onAgentKindChanged,
            onModelChanged = onModelChanged,
            onAcpCommandChanged = onAcpCommandChanged,
            title = "Agent & LLM · this conversation",
            onReset = onResetProfile
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

/**
 * Compact list of working-tree changes. Each file row is tappable: expanding it
 * lazily fetches that file's unified diff from the runtime and shows it inline,
 * so a large change list never blocks on downloading every diff up front.
 */
@Composable
private fun GitChangesCard(
    files: List<GitChangeDto>,
    fileDiffs: Map<String, String>,
    loadingPath: String?,
    onRequestDiff: (String) -> Unit
) {
    var expandedPath by remember { mutableStateOf<String?>(null) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("git_changes_card")
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Sync,
                    null,
                    tint = CanvasPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Git changes · ${files.size} file(s)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "tap a file for its diff",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            files.forEach { change ->
                val path = change.displayName
                val isExpanded = expandedPath == path
                val diff = fileDiffs[path]
                val isLoadingThis = loadingPath == path
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                expandedPath = if (isExpanded) null else path
                                if (!isExpanded && diff == null) onRequestDiff(path)
                            }
                            .padding(vertical = 3.dp)
                            .testTag("git_file_$path")
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Hide diff" else "Show diff",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            path,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            change.status.uppercase(),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "+${change.insertions} −${change.deletions}",
                            fontSize = 10.sp,
                            color = CanvasPrimary
                        )
                    }
                    if (isExpanded) {
                        when {
                            isLoadingThis -> Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 19.dp, top = 2.dp, bottom = 6.dp)
                            ) {
                                CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Loading diff…",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            diff != null -> Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 19.dp, top = 2.dp, bottom = 6.dp)
                            ) {
                                Text(
                                    diff,
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .testTag("git_file_diff")
                                )
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}
