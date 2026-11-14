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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentEvent
import com.example.data.model.ConversationStatus
import com.example.data.model.GitChange
import com.example.data.model.OpenHandsConversation
import com.example.ui.components.EventItemView
import com.example.ui.components.EdgeFadeRow
import com.example.ui.components.StatusBadge
import com.example.ui.theme.OpenHandsPrimaryDark
import com.example.ui.theme.StatusPaused as StatusPausedColor

@Composable
fun ConversationDetailScreen(
    conversation: OpenHandsConversation?,
    events: List<AgentEvent>,
    isSendingMessage: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSendMessage: (String) -> Unit,
    onDelete: () -> Unit,
    onResume: () -> Unit = {},
    isResuming: Boolean = false,
    runtimeUnavailableReason: String? = null,
    onOpenModel: () -> Unit = {},
    onOpenContext: () -> Unit = {},
    onOpenTerminal: () -> Unit = {},
    onOpenFiles: () -> Unit = {},
    onOpenGit: () -> Unit = {},
    onStop: () -> Unit = {},
    onRun: () -> Unit = {},
    onPause: () -> Unit = {},
    onRename: (String) -> Unit = {},
    onOpenAdvanced: () -> Unit = {},
    onOpenGoal: () -> Unit = {},
    onOpenTools: () -> Unit = {},
    onOpenVSCode: () -> Unit = {},
    onOpenSkills: () -> Unit = {},
    onOpenSubConversations: () -> Unit = {},
    onOpenFinalResponse: () -> Unit = {},
    onClearEvents: () -> Unit = {},
    onCheckInit: () -> Unit = {},
    onUnknownCommand: (String) -> Unit = {},
    onConfirm: (Boolean) -> Unit = {},
    onFork: () -> Unit = {},
    gitChanges: List<GitChange> = emptyList(),
    isLoadingChanges: Boolean = false,
    onRefreshChanges: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var followUpText by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var showChanges by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()

    val quickSuggestions = listOf(
        "Run pytest test suite",
        "Show current git status & diff",
        "Commit changes and prepare PR",
        "Explain the bug cause and fix"
    )

    // Slash commands map to real actions; unknown commands are rejected.
    val slashCommands = remember {
        mapOf(
            "/compact" to "Compact the conversation context",
            "/model" to "Switch model",
            "/context" to "Show token & cost usage",
            "/terminal" to "Open the sandbox terminal",
            "/files" to "Browse sandbox files",
            "/diff" to "Show git changes",
            "/stop" to "Stop the agent",
            "/run" to "Run the agent",
            "/pause" to "Pause the agent",
            "/resume" to "Resume the sandbox",
            "/advanced" to "Advanced runtime controls",
            "/goal" to "Start / resume / stop a goal",
            "/tools" to "Browse sandbox tools",
            "/vscode" to "Open VS Code in the sandbox",
            "/skills" to "Skills & plugins",
            "/final" to "Show agent final response",
            "/fork" to "Fork this conversation",
            "/subconv" to "Sub-conversations",
            "/accept" to "Accept a pending confirmation",
            "/reject" to "Reject a pending confirmation",
            "/clear" to "Clear sandbox bash events",
            "/init" to "Show agent server init state",
        )
    }
    val showSlashPalette = followUpText.startsWith("/") && !followUpText.contains(" ")
    val filteredSlashCommands = remember(followUpText) {
        val prefix = followUpText.trimStart('/').lowercase()
        if (prefix.isEmpty()) slashCommands.entries.toList()
        else slashCommands.entries.filter { it.key.removePrefix("/").startsWith(prefix) }
    }

    fun submit(text: String) {
        val trimmed = text.trim()
        when {
            trimmed.equals("/compact", true) -> onOpenContext()
            trimmed.equals("/model", true) -> onOpenModel()
            trimmed.equals("/context", true) -> onOpenContext()
            trimmed.equals("/terminal", true) -> onOpenTerminal()
            trimmed.equals("/files", true) -> onOpenFiles()
            trimmed.equals("/diff", true) -> onOpenGit()
            trimmed.equals("/stop", true) -> onStop()
            trimmed.equals("/run", true) -> onRun()
            trimmed.equals("/pause", true) -> onPause()
            trimmed.equals("/resume", true) -> onResume()
            trimmed.equals("/advanced", true) -> onOpenAdvanced()
            trimmed.equals("/goal", true) -> onOpenGoal()
            trimmed.equals("/tools", true) -> onOpenTools()
            trimmed.equals("/vscode", true) -> onOpenVSCode()
            trimmed.equals("/skills", true) -> onOpenSkills()
            trimmed.equals("/final", true) -> onOpenFinalResponse()
            trimmed.equals("/fork", true) -> onFork()
            trimmed.equals("/subconv", true) -> onOpenSubConversations()
            trimmed.equals("/accept", true) -> onConfirm(true)
            trimmed.equals("/reject", true) -> onConfirm(false)
            trimmed.equals("/clear", true) -> onClearEvents()
            trimmed.equals("/init", true) -> onCheckInit()
            trimmed.startsWith("/") -> onUnknownCommand(trimmed)
            else -> onSendMessage(trimmed)
        }
        followUpText = ""
    }

    LaunchedEffect(events.size) {
        if (events.isNotEmpty()) {
            listState.animateScrollToItem(events.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top app bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_from_detail_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(modifier = Modifier.padding(start = 4.dp)) {
                        Text(
                            text = conversation?.title ?: "OpenHands Agent Session",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!conversation?.repository.isNullOrBlank()) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = OpenHandsPrimaryDark,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = conversation?.repository ?: "",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = OpenHandsPrimaryDark
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = conversation?.sandboxId
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { "Sandbox: ${it.take(8)}" }
                                    ?: "Conversation: ${(conversation?.id ?: "").take(8)}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    conversation?.let { StatusBadge(status = it.status) }
                    Box {
                        IconButton(
                            onClick = { showOverflow = true },
                            modifier = Modifier.testTag("overflow_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflow,
                            onDismissRequest = { showOverflow = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Changed files") },
                                onClick = {
                                    showOverflow = false
                                    showChanges = !showChanges
                                    if (showChanges && gitChanges.isEmpty()) {
                                        onRefreshChanges()
                                    }
                                },
                                modifier = Modifier.testTag("toggle_changes_button")
                            )
                            DropdownMenuItem(
                                text = { Text("Open in browser") },
                                enabled = !conversation?.conversationUrl.isNullOrBlank(),
                                onClick = {
                                    showOverflow = false
                                    conversation?.conversationUrl?.let { url ->
                                        runCatching { uriHandler.openUri(url) }
                                    }
                                },
                                modifier = Modifier.testTag("open_in_browser_button")
                            )
                            DropdownMenuItem(
                                text = { Text("Refresh events") },
                                onClick = {
                                    showOverflow = false
                                    onRefresh()
                                },
                                modifier = Modifier.testTag("refresh_events_button")
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = {
                                    showOverflow = false
                                    renameText = conversation?.title ?: ""
                                    showRename = true
                                },
                                modifier = Modifier.testTag("rename_conversation_button")
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showOverflow = false
                                    showDeleteConfirm = true
                                },
                                modifier = Modifier.testTag("delete_conversation_button")
                            )
                        }
                    }
                }
            }
        }

        if (runtimeUnavailableReason != null) {
            Surface(
                color = StatusPausedColor.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("runtime_unavailable_banner")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = StatusPausedColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = runtimeUnavailableReason,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    val canResume = conversation?.sandboxStatus?.let {
                        it.equals("PAUSED", true) || it.equals("MISSING", true)
                    } == true
                    if (canResume) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onResume,
                            enabled = !isResuming,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OpenHandsPrimaryDark),
                            modifier = Modifier.testTag("runtime_resume_button")
                        ) {
                            if (isResuming) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text("Resume", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Shown only when the runtime banner above is not already covering the
        // paused case, so the user never sees two Resume buttons.
        if (runtimeUnavailableReason == null && conversation?.status == ConversationStatus.PAUSED) {
            Surface(
                color = StatusPausedColor.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("paused_banner")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PauseCircleOutline,
                        contentDescription = null,
                        tint = StatusPausedColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sandbox paused after inactivity. Resume to keep working.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onResume,
                        enabled = !isResuming,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OpenHandsPrimaryDark),
                        modifier = Modifier.testTag("resume_sandbox_button")
                    ) {
                        if (isResuming) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text("Resume", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Runtime tools: model, context, terminal, files, git
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            val toolsRowState = rememberLazyListState()
            EdgeFadeRow(
                state = toolsRowState,
                modifier = Modifier.padding(vertical = 6.dp),
            ) {
                val tools = listOf(
                    Triple("Model", "model", onOpenModel),
                    Triple("Context", "context", onOpenContext),
                    Triple("Terminal", "terminal", onOpenTerminal),
                    Triple("Files", "files", onOpenFiles),
                    Triple("Git", "git", onOpenGit),
                    Triple("Run", "run", onRun),
                    Triple("Pause", "pause", onPause),
                    Triple("Stop", "stop", onStop),
                    Triple("Advanced", "advanced", onOpenAdvanced),
                    Triple("Goal", "goal", onOpenGoal),
                    Triple("Tools", "tools", onOpenTools),
                    Triple("VS Code", "vscode", onOpenVSCode),
                    Triple("Skills", "skills", onOpenSkills),
                    Triple("Final", "final", onOpenFinalResponse),
                    Triple("Fork", "fork", onFork),
                    Triple("Sub-conv", "subconv", onOpenSubConversations),
                )
                // Fork and sub-conversations are cloud operations; the rest need a
                // live sandbox, so they are disabled while the runtime is blocked.
                val runtimeTools = setOf(
                    "model", "context", "terminal", "files", "git", "run",
                    "pause", "stop", "advanced", "goal", "tools", "vscode",
                    "skills", "final",
                )
                val runtimeBlocked = runtimeUnavailableReason != null
                items(tools) { (label, tag, action) ->
                    val enabled = !(runtimeBlocked && tag in runtimeTools)
                    Surface(
                        color = when (label) {
                            "Stop" -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                            "Run" -> OpenHandsPrimaryDark.copy(alpha = 0.12f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clickable(enabled = enabled, onClick = action)
                            .testTag("tool_$tag")
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                label == "Stop" -> MaterialTheme.colorScheme.error
                                label == "Run" -> OpenHandsPrimaryDark
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        if (showChanges) {
            ChangesPanel(
                changes = gitChanges,
                isLoading = isLoadingChanges,
                onRefresh = onRefreshChanges
            )
        }

        // Events stream
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (events.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val terminal = conversation?.status in setOf(
                            ConversationStatus.FINISHED,
                            ConversationStatus.ERROR
                        )
                        if (terminal) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "This conversation has ended with no agent events.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            CircularProgressIndicator(
                                color = OpenHandsPrimaryDark,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Awaiting OpenHands Agent Events...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(events) { event ->
                        EventItemView(event = event)
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        // Quick Suggestion Chips
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                val suggestionsRowState = rememberLazyListState()
                EdgeFadeRow(state = suggestionsRowState) {
                    items(quickSuggestions) { suggestion ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable {
                                followUpText = suggestion
                            }
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Input bar
                if (showSlashPalette) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .testTag("slash_palette")
                    ) {
                        if (filteredSlashCommands.isEmpty()) {
                            item {
                                Text(
                                    text = "No matching command — press send to try \"$followUpText\"",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                        items(filteredSlashCommands) { entry ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.clickable {
                                    if (slashCommands.containsKey(entry.key)) submit(entry.key)
                                }
                            ) {
                                Text(
                                    text = "${entry.key}  ${entry.value}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = followUpText,
                        onValueChange = { followUpText = it },
                        placeholder = { Text("Send instruction to OpenHands agent...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OpenHandsPrimaryDark,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("follow_up_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (followUpText.isNotBlank()) {
                                submit(followUpText)
                            }
                        },
                        enabled = followUpText.isNotBlank() && !isSendingMessage,
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (followUpText.isNotBlank()) OpenHandsPrimaryDark else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(14.dp)
                            )
                            .testTag("send_follow_up_button")
                    ) {
                        if (isSendingMessage) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (followUpText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete conversation?") },
            text = {
                Text(
                    "This permanently removes the conversation and its sandbox from " +
                        "OpenHands Cloud. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename conversation", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    placeholder = { Text("Conversation title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRename = false
                        onRename(renameText.trim())
                    },
                    enabled = renameText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = OpenHandsPrimaryDark),
                    modifier = Modifier.testTag("confirm_rename_button")
                ) { Text("Save", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showRename = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ChangesPanel(
    changes: List<GitChange>,
    isLoading: Boolean,
    onRefresh: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Changed Files (${changes.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("refresh_changes_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = OpenHandsPrimaryDark,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh changed files",
                            tint = OpenHandsPrimaryDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (changes.isEmpty() && !isLoading) {
                Text(
                    text = "No files changed in this workspace yet.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(changes) { change ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = change.status.take(3),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = statusColor(change.status),
                                modifier = Modifier.width(34.dp)
                            )
                            Text(
                                text = change.path,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun statusColor(status: String): Color = when (status.uppercase()) {
    "ADDED", "A", "UNTRACKED" -> Color(0xFF34D399)
    "DELETED", "D" -> Color(0xFFF87171)
    "RENAMED", "R" -> Color(0xFF60A5FA)
    else -> Color(0xFFFBBF24)
}
