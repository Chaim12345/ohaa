package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgentEvent
import com.example.data.model.Conversation
import com.example.data.model.ConversationStatus
import com.example.data.model.LlmProfile
import com.example.data.model.SandboxStatus
import com.example.data.model.Skill
import com.example.ui.components.ErrorBanner
import com.example.ui.components.EventCard
import com.example.ui.components.InfoCard
import com.example.ui.components.Pill
import com.example.ui.components.SandboxBadge
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatCost
import com.example.ui.components.formatTokens
import com.example.ui.theme.OpenHandsPrimaryDark
import com.example.ui.theme.StatusWaiting

@Composable
fun ConversationDetailScreen(
    conversation: Conversation?,
    events: List<AgentEvent>,
    isSyncing: Boolean,
    isSending: Boolean,
    isMutating: Boolean,
    showNoise: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSend: (String) -> Unit,
    onPauseSandbox: (String) -> Unit,
    onResumeSandbox: (String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenChanges: () -> Unit,
    onToggleNoise: () -> Unit,
    llmProfiles: List<LlmProfile> = emptyList(),
    onLoadProfiles: () -> Unit = {},
    onSwitchProfile: (String) -> Unit = {},
    skills: List<Skill>? = null,
    onLoadSkills: () -> Unit = {},
    onDismissSkills: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showProfiles by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(events.size) {
        if (events.isNotEmpty()) listState.animateScrollToItem(events.size - 1)
    }

    val quickReplies = listOf("Continue", "Run the tests", "Show me the diff", "Commit and open a PR", "Explain what you changed")

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top bar
        Surface(color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            conversation?.displayTitle ?: "Conversation",
                            fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!conversation?.repository.isNullOrBlank()) {
                                Icon(Icons.Default.Code, contentDescription = null, tint = OpenHandsPrimaryDark, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    conversation?.repository + (conversation?.branch?.let { "@$it" } ?: ""),
                                    fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = OpenHandsPrimaryDark,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("ID ${(conversation?.id ?: "").take(8)}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onRefresh, enabled = !isSyncing, modifier = Modifier.testTag("detail_refresh")) {
                        if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("detail_menu")) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Browse files") }, onClick = { menuOpen = false; onOpenFiles() })
                            DropdownMenuItem(text = { Text("Git changes & diffs") }, onClick = { menuOpen = false; onOpenChanges() })
                            conversation?.sandboxId?.let { sid ->
                                when (conversation.sandboxStatus) {
                                    SandboxStatus.RUNNING, SandboxStatus.STARTING ->
                                        DropdownMenuItem(text = { Text("Pause sandbox") }, enabled = !isMutating, onClick = { menuOpen = false; onPauseSandbox(sid) })
                                    SandboxStatus.PAUSED, SandboxStatus.MISSING ->
                                        DropdownMenuItem(text = { Text("Resume sandbox") }, enabled = !isMutating, onClick = { menuOpen = false; onResumeSandbox(sid) })
                                    else -> Unit
                                }
                            }
                            conversation?.webUrl?.let { url ->
                                DropdownMenuItem(text = { Text("Open in browser") }, onClick = {
                                    menuOpen = false
                                    context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                                })
                            }
                            DropdownMenuItem(text = { Text("Active skills") }, onClick = { menuOpen = false; onLoadSkills() })
                            DropdownMenuItem(text = { Text("Switch LLM profile") }, enabled = !isMutating, onClick = { menuOpen = false; onLoadProfiles(); showProfiles = true })
                            DropdownMenuItem(text = { Text(if (showNoise) "Hide system events" else "Show system events") }, onClick = { menuOpen = false; onToggleNoise() })
                            DropdownMenuItem(text = { Text("Rename") }, onClick = { menuOpen = false; showRename = true })
                            DropdownMenuItem(text = { Text("Delete conversation") }, onClick = { menuOpen = false; showDelete = true })
                        }
                    }
                }
                // Status strip
                if (conversation != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                    ) {
                        StatusBadge(conversation.status)
                        SandboxBadge(conversation.sandboxStatus)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${formatCost(conversation.cost)} · ${formatTokens(conversation.promptTokens + conversation.completionTokens)} tok",
                            fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (isSyncing) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp), color = OpenHandsPrimaryDark)
            }
        }

        // Events
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isSyncing || conversation?.status?.isActive == true) {
                            CircularProgressIndicator(color = OpenHandsPrimaryDark, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.height(10.dp))
                        }
                        Text(
                            if (error != null) "Could not load events" else "No events yet",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Spacer(Modifier.height(8.dp)) }
                    items(events, key = { it.id }) { event -> EventCard(event) }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }

        // Notices
        if (error != null) {
            ErrorBanner(error, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), actionLabel = "Retry", onAction = onRefresh)
        }
        if (conversation?.status == ConversationStatus.WAITING_FOR_CONFIRMATION) {
            InfoCard(tint = StatusWaiting, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                Column {
                    Text("The agent is waiting for your confirmation", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Approving or rejecting a risky action is only available in the web UI. Reply here to give new instructions instead.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    conversation.webUrl?.let { url ->
                        TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }) { Text("Open in browser") }
                    }
                }
            }
        }
        if (conversation?.sandboxStatus == SandboxStatus.PAUSED) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Pill(text = "Sandbox paused — sending a message resumes it", color = StatusWaiting, icon = null)
            }
        }

        // Composer
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)) {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                ) {
                    items(quickReplies) { s ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable { draft = s }
                        ) { Text(s, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                    val sandboxStarting = conversation?.sandboxStatus == SandboxStatus.STARTING
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text(if (sandboxStarting) "Queue a message while the sandbox starts…" else "Message the agent…") },
                        maxLines = 4,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OpenHandsPrimaryDark, unfocusedBorderColor = MaterialTheme.colorScheme.outline),
                        modifier = Modifier.weight(1f).testTag("message_input")
                    )
                    Spacer(Modifier.width(8.dp))
                    val canSend = draft.isNotBlank() && !isSending
                    IconButton(
                        onClick = { if (canSend) { onSend(draft); draft = "" } },
                        enabled = canSend,
                        modifier = Modifier.size(44.dp)
                            .background(if (canSend) OpenHandsPrimaryDark else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                            .testTag("send_button")
                    ) {
                        if (isSending) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (showRename && conversation != null) {
        RenameDialog(current = conversation.displayTitle, onDismiss = { showRename = false }, onConfirm = { onRename(it); showRename = false })
    }
    if (showProfiles) {
        AlertDialog(
            onDismissRequest = { showProfiles = false },
            title = { Text("Switch LLM profile") },
            text = {
                Column {
                    if (llmProfiles.isEmpty()) Text("Loading profiles…", fontSize = 13.sp)
                    llmProfiles.forEach { p ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onSwitchProfile(p.name); showProfiles = false }.padding(vertical = 8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name + if (p.isActive) "  (default)" else "", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(listOfNotNull(p.model, p.baseUrl).joinToString(" · "), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!p.apiKeySet) Pill(text = "no key", color = StatusWaiting, icon = null)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showProfiles = false }) { Text("Close") } }
        )
    }
    if (skills != null) {
        AlertDialog(
            onDismissRequest = onDismissSkills,
            title = { Text("Active skills (${skills.size})") },
            text = {
                Column(modifier = Modifier.height(360.dp).verticalScroll(rememberScrollState())) {
                    if (skills.isEmpty()) Text("No skills are active in this sandbox.", fontSize = 13.sp)
                    skills.forEach { s ->
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(s.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(listOfNotNull(s.type, s.source, s.triggers.takeIf { it.isNotEmpty() }?.joinToString(", ", prefix = "triggers: ")).joinToString(" · "), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!s.content.isNullOrBlank()) Text(s.content.take(300), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismissSkills) { Text("Close") } }
        )
    }
    if (showDelete) {
        ConfirmDialog(
            title = "Delete conversation?",
            text = "This deletes the conversation and its sandbox workspace from OpenHands Cloud.",
            confirmLabel = "Delete",
            onDismiss = { showDelete = false },
            onConfirm = { showDelete = false; onDelete() }
        )
    }
}
