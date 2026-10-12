package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentEvent
import com.example.data.model.GitChangeDto
import com.example.data.model.GitDiffDto
import com.example.data.model.OpenHandsConversation
import com.example.data.settings.AgentProfile
import com.example.ui.components.AgentProfileBadge
import com.example.ui.components.AgentProfileDialog
import com.example.ui.components.CanvasCard
import com.example.ui.components.EmptyState
import com.example.ui.components.KeyValueRow
import com.example.ui.components.SectionHeader
import com.example.ui.components.TranscriptEvent
import com.example.ui.theme.CanvasBorder
import com.example.ui.theme.CanvasColors
import com.example.ui.theme.CanvasPrimary
import com.example.ui.theme.CanvasPrimaryDeep
import com.example.ui.theme.CanvasTerminalBg
import com.example.ui.theme.CodeSmall
import com.example.ui.theme.Shapes
import com.example.ui.theme.Sizes
import com.example.ui.theme.Spacing
import com.example.ui.viewmodel.SandboxUiState
import com.example.util.formatDecimals

/** The right-hand panel tabs Canvas exposes, reduced to what mobile needs. */
enum class ConversationTab(val label: String) {
    CHAT("Chat"),
    FILES("Files"),
    CHANGES("Changes"),
    TERMINAL("Terminal"),
    USAGE("Usage")
}

/**
 * Conversation screen with Canvas's tabbed layout.
 *
 * Chat is the default and the only tab that streams; the others load on first
 * open and keep their data, so switching tabs does not refetch.
 */
@Composable
fun ConversationScreen(
    conversation: OpenHandsConversation?,
    events: List<AgentEvent>,
    isSending: Boolean,
    activeProfile: AgentProfile,
    gitDiff: GitDiffDto?,
    isGitLoading: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSendMessage: (String) -> Unit,
    onDelete: () -> Unit,
    onRefreshGit: () -> Unit,
    onModelChanged: (String) -> Unit,
    onResetProfile: () -> Unit,
    webBaseUrl: String = "https://app.all-hands.dev",
    availableModels: List<String> = emptyList(),
    onRename: (String, String) -> Unit = { _, _ -> },
    sandboxStatus: SandboxUiState = SandboxUiState(),
    onPauseSandbox: (String) -> Unit = {},
    onResumeSandbox: (String) -> Unit = {},
    onDeleteSandbox: (String) -> Unit = {},
    files: List<String> = emptyList(),
    isFilesLoading: Boolean = false,
    fileContents: Map<String, String> = emptyMap(),
    onLoadFiles: (String?) -> Unit = {},
    onReadFile: (String) -> Unit = {},
    skills: List<String> = emptyList(),
    onLoadSkills: () -> Unit = {},
    onExportTrajectory: () -> Unit = {}
) {
    var tab by remember(conversation?.id) { mutableStateOf(ConversationTab.CHAT) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .testTag("conversation_screen")
    ) {
        ConversationHeader(
            conversation = conversation,
            activeProfile = activeProfile,
            isGitLoading = isGitLoading,
            sandboxStatus = sandboxStatus,
            showOverflow = showOverflow,
            onOverflowChange = { showOverflow = it },
            onBack = onBack,
            onRefresh = onRefresh,
            onRefreshGit = onRefreshGit,
            onOpenProfile = { showProfileDialog = true },
            onRename = { showRenameDialog = true },
            onDelete = { showDeleteConfirm = true },
            onLoadFiles = { onLoadFiles(null) },
            onLoadSkills = onLoadSkills,
            onExport = onExportTrajectory,
            onPauseSandbox = onPauseSandbox,
            onResumeSandbox = onResumeSandbox,
            onDeleteSandbox = onDeleteSandbox,
            conversationId = conversation?.id
        )

        TabRow(
            selectedTabIndex = tab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = CanvasPrimary,
            modifier = Modifier.testTag("conversation_tabs")
        ) {
            ConversationTab.entries.forEach { entry ->
                Tab(
                    selected = tab == entry,
                    onClick = {
                        tab = entry
                        // Lazy per-tab load: only fetch when first opened.
                        if (entry == ConversationTab.FILES && files.isEmpty()) onLoadFiles(null)
                    },
                    text = { Text(entry.label, fontSize = 12.sp, maxLines = 1) },
                    modifier = Modifier.testTag("tab_${entry.name.lowercase()}")
                )
            }
        }

        Box(Modifier.weight(1f)) {
            when (tab) {
                ConversationTab.CHAT -> ChatTab(
                    events = events,
                    isSending = isSending,
                    isWaiting = conversation?.uiStatus ==
                        com.example.data.model.ConversationUiStatus.WAITING_INPUT,
                    webBaseUrl = webBaseUrl,
                    conversationId = conversation?.id,
                    onSendMessage = onSendMessage
                )
                ConversationTab.FILES -> FilesTab(
                    files = files,
                    isLoading = isFilesLoading,
                    fileContents = fileContents,
                    onReadFile = onReadFile,
                    onReload = { onLoadFiles(null) }
                )
                ConversationTab.CHANGES -> ChangesTab(
                    gitDiff = gitDiff,
                    isLoading = isGitLoading,
                    onRefresh = onRefreshGit
                )
                ConversationTab.TERMINAL -> TerminalTab(events = events)
                ConversationTab.USAGE -> UsageTab(
                    conversation = conversation,
                    skills = skills
                )
            }
        }
    }

    if (showRenameDialog) {
        RenameDialog(
            current = conversation?.title.orEmpty(),
            onDismiss = { showRenameDialog = false },
            onConfirm = { newTitle ->
                showRenameDialog = false
                conversation?.id?.let { onRename(it, newTitle) }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete conversation?") },
            text = { Text("This removes the cached transcript and deletes the cloud conversation.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    modifier = Modifier.testTag("btn_confirm_delete")
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showProfileDialog) {
        AgentProfileDialog(
            profile = activeProfile,
            onDismiss = { showProfileDialog = false },
            onModelChanged = onModelChanged,
            title = "Model",
            availableModels = availableModels
        )
        TextButton(
            onClick = { showProfileDialog = false; onResetProfile() },
            modifier = Modifier.testTag("btn_reset_profile")
        ) { Text("Reset to default") }
    }
}

@Composable
private fun ConversationHeader(
    conversation: OpenHandsConversation?,
    activeProfile: AgentProfile,
    isGitLoading: Boolean,
    sandboxStatus: SandboxUiState,
    showOverflow: Boolean,
    onOverflowChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshGit: () -> Unit,
    onOpenProfile: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onLoadFiles: () -> Unit,
    onLoadSkills: () -> Unit,
    onExport: () -> Unit,
    onPauseSandbox: (String) -> Unit,
    onResumeSandbox: (String) -> Unit,
    onDeleteSandbox: (String) -> Unit,
    conversationId: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = Spacing.xs, vertical = Spacing.sm)
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
                conversation?.repository?.takeIf { it.isNotBlank() }?.let { repo ->
                    Text(
                        repo,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        AgentProfileBadge(profile = activeProfile, onOpen = onOpenProfile)
        IconButton(
            onClick = onRefresh,
            enabled = !isGitLoading,
            modifier = Modifier.testTag("btn_refresh")
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
        }
        Box {
            IconButton(
                onClick = { onOverflowChange(true) },
                modifier = Modifier.testTag("btn_overflow")
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "More actions")
            }
            DropdownMenu(
                expanded = showOverflow,
                onDismissRequest = { onOverflowChange(false) }
            ) {
                val sandboxId = conversation?.sandboxId
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = { onOverflowChange(false); onRename() },
                    modifier = Modifier.testTag("menu_rename")
                )
                DropdownMenuItem(
                    text = { Text("Workspace files") },
                    onClick = { onOverflowChange(false); onLoadFiles() },
                    modifier = Modifier.testTag("menu_files")
                )
                DropdownMenuItem(
                    text = { Text("Git changes") },
                    onClick = { onOverflowChange(false); onRefreshGit() },
                    modifier = Modifier.testTag("menu_git")
                )
                DropdownMenuItem(
                    text = { Text("Skills") },
                    onClick = { onOverflowChange(false); onLoadSkills() },
                    modifier = Modifier.testTag("menu_skills")
                )
                DropdownMenuItem(
                    text = { Text("Export trajectory") },
                    onClick = { onOverflowChange(false); onExport() },
                    modifier = Modifier.testTag("menu_export")
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Pause sandbox") },
                    enabled = sandboxId != null && !sandboxStatus.isWorking,
                    onClick = { onOverflowChange(false); sandboxId?.let(onPauseSandbox) },
                    modifier = Modifier.testTag("menu_pause_sandbox")
                )
                DropdownMenuItem(
                    text = { Text("Resume sandbox") },
                    enabled = sandboxId != null && !sandboxStatus.isWorking,
                    onClick = { onOverflowChange(false); sandboxId?.let(onResumeSandbox) },
                    modifier = Modifier.testTag("menu_resume_sandbox")
                )
                DropdownMenuItem(
                    text = { Text("Delete sandbox") },
                    enabled = sandboxId != null && !sandboxStatus.isWorking,
                    onClick = { onOverflowChange(false); sandboxId?.let(onDeleteSandbox) },
                    modifier = Modifier.testTag("menu_delete_sandbox")
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Delete conversation", color = MaterialTheme.colorScheme.error) },
                    onClick = { onOverflowChange(false); onDelete() },
                    modifier = Modifier.testTag("menu_delete_conversation")
                )
            }
        }
    }
}

// ------------------------------------------------------------------ chat

/**
 * State-update / pause / system-prompt events carry no user-facing content —
 * they exist for internal bookkeeping and render as empty cards, so they are
 * excluded from the transcript. User/agent messages, errors and anything with
 * real content are always kept.
 */
internal fun isRenderableTranscriptEvent(event: AgentEvent): Boolean {
    val action = event.actionType?.lowercase().orEmpty()
    val noiseActions = setOf("conversationstateupdate", "pause", "systemprompt")
    val hasNoContent = event.content.isBlank() || event.content == "(no content)"
    // An observation event carries its output in `detail`, not `content`, so it
    // must be judged on both: dropping it on empty content would hide the
    // command output that is the most useful part of the transcript.
    val hasDetail = !event.detail.isNullOrBlank()
    return !(hasNoContent && !hasDetail && action in noiseActions)
}

@Composable
private fun ChatTab(
    events: List<AgentEvent>,
    isSending: Boolean,
    isWaiting: Boolean,
    webBaseUrl: String,
    conversationId: String?,
    onSendMessage: (String) -> Unit
) {
    val listState = rememberLazyListState()
    var composer by remember { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    val renderableEvents = remember(events) { events.filter(::isRenderableTranscriptEvent) }

    // Follow the newest event, but never yank the user out of history they
    // scrolled back to read.
    LaunchedEffect(renderableEvents.size) {
        if (renderableEvents.isEmpty()) return@LaunchedEffect
        val last = renderableEvents.size - 1
        val visible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        if (visible >= last - 2) listState.animateScrollToItem(last)
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("transcript"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
        ) {
            if (renderableEvents.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Description,
                        title = "No messages yet",
                        message = "The agent's transcript appears here as it works."
                    )
                }
            }
            items(renderableEvents, key = { it.id }) { event ->
                TranscriptEvent(event)
            }
            if (isWaiting) {
                item { WaitingForInputCard(webBaseUrl, conversationId, uriHandler) }
            }
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
        ) {
            OutlinedTextField(
                value = composer,
                onValueChange = { composer = it },
                placeholder = { Text("Send a follow-up…") },
                maxLines = 4,
                shape = Shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CanvasPrimary,
                    unfocusedBorderColor = CanvasBorder
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("composer_input")
            )
            Spacer(Modifier.width(Spacing.sm))
            IconButton(
                onClick = { onSendMessage(composer.trim()); composer = "" },
                enabled = !isSending && composer.isNotBlank(),
                modifier = Modifier
                    .size(Sizes.touchTarget)
                    .background(CanvasPrimaryDeep, Shapes.medium)
                    .testTag("btn_send")
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        Modifier.size(Sizes.iconSmall),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun WaitingForInputCard(
    webBaseUrl: String,
    conversationId: String?,
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    CanvasCard(accent = CanvasColors.warning) {
        Text("The agent is waiting on you", fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "Approve or reject its pending action to continue.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (conversationId != null) {
            Spacer(Modifier.height(Spacing.sm))
            TextButton(onClick = {
                uriHandler.openUri("${webBaseUrl.trimEnd('/')}/conversations/$conversationId")
            }) { Text("Open in web app") }
        }
    }
}

// ----------------------------------------------------------------- files

@Composable
private fun FilesTab(
    files: List<String>,
    isLoading: Boolean,
    fileContents: Map<String, String>,
    onReadFile: (String) -> Unit,
    onReload: () -> Unit
) {
    when {
        isLoading && files.isEmpty() -> LoadingPane("Loading workspace…")
        files.isEmpty() -> EmptyState(
            icon = Icons.Default.Description,
            title = "No files listed",
            message = "The sandbox may still be starting.",
            action = { TextButton(onClick = onReload) { Text("Reload") } }
        )
        else -> LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.fillMaxSize().testTag("files_list")
        ) {
            items(files, key = { it }) { path ->
                val open = fileContents[path]
                CanvasCard(onClick = { if (open == null) onReadFile(path) }) {
                    Text(
                        path,
                        style = CodeSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("file_row")
                    )
                    if (open != null) {
                        Spacer(Modifier.height(Spacing.sm))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(CanvasTerminalBg, Shapes.small)
                                .padding(Spacing.sm)
                        ) {
                            Text(
                                open.take(4000),
                                style = CodeSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------- changes

@Composable
private fun ChangesTab(
    gitDiff: GitDiffDto?,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    val files = gitDiff?.files.orEmpty()
    val diffText = gitDiff?.diffText
    when {
        isLoading -> LoadingPane("Loading changes…")
        files.isEmpty() && diffText.isNullOrBlank() -> EmptyState(
            icon = Icons.Default.Sync,
            title = "No changes",
            message = "Working-tree changes appear here once the agent edits files.",
            action = { TextButton(onClick = onRefresh) { Text("Refresh") } }
        )
        else -> LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.fillMaxSize().testTag("changes_list")
        ) {
            if (files.isNotEmpty()) {
                item {
                    CanvasCard {
                        SectionHeader("Changed files", subtitle = "${files.size} file(s)")
                        Spacer(Modifier.height(Spacing.sm))
                        files.forEach { ChangeRow(it) }
                    }
                }
            }
            diffText?.takeIf { it.isNotBlank() }?.let { text ->
                item {
                    CanvasCard {
                        SectionHeader("Diff")
                        Spacer(Modifier.height(Spacing.sm))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(CanvasTerminalBg, Shapes.small)
                                .padding(Spacing.sm)
                        ) {
                            DiffBody(text)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChangeRow(change: GitChangeDto) {
    val statusColor = when (change.status.lowercase()) {
        "added", "untracked", "new" -> CanvasColors.success
        "deleted", "removed" -> CanvasColors.danger
        "modified", "changed" -> CanvasColors.warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            change.status.uppercase().take(1).ifBlank { "?" },
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor,
            modifier = Modifier.width(16.dp)
        )
        Text(
            change.displayName,
            style = CodeSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (change.insertions > 0) {
            Text("+${change.insertions}", fontSize = 11.sp, color = CanvasColors.success)
            Spacer(Modifier.width(Spacing.xs))
        }
        if (change.deletions > 0) {
            Text("-${change.deletions}", fontSize = 11.sp, color = CanvasColors.danger)
        }
    }
}

@Composable
private fun DiffBody(text: String) {
    val added = CanvasColors.success
    val removed = CanvasColors.danger
    val hunk = CanvasPrimary
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant
    val annotated = remember(text, added, removed, hunk, neutral) {
        buildAnnotatedString {
            text.lineSequence().forEachIndexed { i, line ->
                if (i > 0) append("\n")
                val style = when {
                    line.startsWith("+") && !line.startsWith("+++") ->
                        SpanStyle(color = added)
                    line.startsWith("-") && !line.startsWith("---") ->
                        SpanStyle(color = removed)
                    line.startsWith("@@") ->
                        SpanStyle(color = hunk)
                    else -> SpanStyle(color = neutral)
                }
                withStyle(style) { append(line) }
            }
        }
    }
    Text(annotated, style = CodeSmall, modifier = Modifier.testTag("diff_body"))
}

// -------------------------------------------------------------- terminal

@Composable
private fun TerminalTab(events: List<AgentEvent>) {
    val commands = remember(events) {
        events.filter {
            it.actionType?.lowercase()?.let { a ->
                a.contains("bash") || a.contains("command") || a.contains("terminal")
            } == true
        }
    }
    if (commands.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Terminal,
            title = "No terminal activity",
            message = "Commands the agent runs and their output appear here."
        )
    } else {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.fillMaxSize().testTag("terminal_list")
        ) {
            items(commands, key = { it.id }) { event ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(CanvasTerminalBg, Shapes.small)
                        .border(1.dp, CanvasColors.borderSubtle, Shapes.small)
                        .padding(Spacing.md)
                ) {
                    Text("$ ${event.content}", style = CodeSmall, color = CanvasColors.success)
                    event.detail?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(Spacing.sm))
                        Text(it, style = CodeSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------- usage

@Composable
private fun UsageTab(conversation: OpenHandsConversation?, skills: List<String>) {
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxSize().testTag("usage_list")
    ) {
        item {
            CanvasCard {
                SectionHeader("Conversation")
                Spacer(Modifier.height(Spacing.sm))
                KeyValueRow("Title", conversation?.title ?: "—")
                KeyValueRow("Status", conversation?.uiStatus?.label ?: "—")
                conversation?.repository?.let { KeyValueRow("Repository", it, monospace = true) }
                conversation?.sandboxId?.let { KeyValueRow("Sandbox", it, monospace = true) }
                KeyValueRow("Cost", "$${(conversation?.cost ?: 0.0).formatDecimals(4)}")
            }
        }
        item {
            CanvasCard {
                SectionHeader("Sandbox")
                Spacer(Modifier.height(Spacing.sm))
                KeyValueRow("Sandbox status", conversation?.sandboxStatus ?: "—")
                KeyValueRow("Execution", conversation?.executionStatus ?: "—")
            }
        }
        item {
            CanvasCard {
                SectionHeader("Skills", subtitle = "${skills.size} active")
                Spacer(Modifier.height(Spacing.sm))
                if (skills.isEmpty()) {
                    Text(
                        "Load skills from the overflow menu to see what the sandbox has.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    skills.forEach {
                        Text(it, style = CodeSmall, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------- parts

@Composable
private fun LoadingPane(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            Spacer(Modifier.height(Spacing.md))
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RenameDialog(
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var title by remember(current) { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename conversation") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                shape = Shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CanvasPrimary,
                    unfocusedBorderColor = CanvasBorder
                ),
                modifier = Modifier.fillMaxWidth().testTag("field_conversation_title")
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title) },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("btn_confirm_rename")
            ) { Text("Save", color = CanvasPrimaryDeep) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
