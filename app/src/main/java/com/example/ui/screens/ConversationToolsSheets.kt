package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AcpAgentProfileDto
import com.example.data.model.AcpEffortOptionDto
import com.example.data.model.AcpModelInfoDto
import com.example.data.model.AgentProfileInfoDto
import com.example.data.model.ConversationInfoDto
import com.example.data.model.FileEntryDto
import com.example.data.model.GitCommitDto
import com.example.data.model.GitDiffDto
import com.example.data.model.LlmModelDto
import com.example.data.model.ProfileInfoDto
import com.example.ui.theme.OpenHandsPrimaryDark

private val sheetShape = RoundedCornerShape(18.dp)

@Composable
private fun SheetScaffold(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("sheet_close")) {
                Text("Close")
            }
        },
        title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) { content() }
        },
        shape = sheetShape,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
    )
}

@Composable
private fun SelectableRow(
    label: String,
    sub: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) OpenHandsPrimaryDark.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!sub.isNullOrBlank()) {
                Text(
                    text = sub,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Model switching sheet. Offers saved LLM profiles (cloud) and, for ACP
 * conversations, the session's own model list. Falls back to the cloud model
 * catalog for plain OpenHands agents.
 */
@Composable
fun ModelSheet(
    isAcp: Boolean,
    currentModel: String?,
    profiles: List<ProfileInfoDto>,
    acpModels: List<AcpModelInfoDto>,
    catalogModels: List<LlmModelDto>,
    efforts: List<AcpEffortOptionDto>,
    currentEffort: String?,
    agentProfiles: List<AgentProfileInfoDto> = emptyList(),
    cloudAgentProfiles: List<AgentProfileInfoDto> = emptyList(),
    activeCloudAgentProfileId: String? = null,
    cloudProfileDetail: AcpAgentProfileDto? = null,
    acpSupportsRuntimeModelSwitch: Boolean = true,
    isBusy: Boolean,
    onSwitchProfile: (String) -> Unit,
    onSwitchModel: (String) -> Unit,
    onSwitchEffort: (String) -> Unit,
    onActivateAgentProfile: (String) -> Unit = {},
    onActivateCloudAgentProfile: (String) -> Unit = {},
    onPreviewCloudAgentProfile: (String) -> Unit = {},
    onSearchModels: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    SheetScaffold(title = "Model & effort", onDismiss = onDismiss) {
        LazyColumn {
            if (cloudAgentProfiles.isNotEmpty()) {
                item { SectionLabel("Cloud agent profiles (ACP)") }
                items(cloudAgentProfiles) { p ->
                    SelectableRow(
                        label = p.name ?: "(unnamed)",
                        sub = listOfNotNull(p.agentKind, p.acpServer, p.acpModel).joinToString(" · "),
                        selected = p.id != null && p.id == activeCloudAgentProfileId,
                        testTag = "cloud_agent_profile_${p.id}"
                    ) {
                        p.id?.let(onActivateCloudAgentProfile)
                        p.name?.let(onPreviewCloudAgentProfile)
                    }
                }
                cloudProfileDetail?.let { d ->
                    item {
                        SelectionContainer {
                            Text(
                                text = "Preview: ${listOfNotNull(d.agentKind, d.acpServer, d.acpModel, d.acpEffort).joinToString(" · ")}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (agentProfiles.isNotEmpty()) {
                item { SectionLabel("Agent profiles (ACP)") }
                items(agentProfiles) { p ->
                    SelectableRow(
                        label = p.name ?: "(unnamed)",
                        sub = listOfNotNull(p.agentKind, p.acpServer, p.acpModel).joinToString(" · "),
                        testTag = "agent_profile_${p.id}"
                    ) { p.id?.let(onActivateAgentProfile) }
                }
            }

            if (profiles.isNotEmpty()) {
                item { SectionLabel("Saved profiles") }
                items(profiles) { p ->
                    SelectableRow(
                        label = p.name ?: "(unnamed)",
                        sub = p.model,
                        selected = p.name == currentModel,
                        testTag = "profile_${p.name}"
                    ) { onSwitchProfile(p.name ?: return@SelectableRow) }
                }
            }

            if (isAcp && acpModels.isNotEmpty()) {
                item { SectionLabel("ACP models") }
                if (!acpSupportsRuntimeModelSwitch) {
                    item {
                        Text(
                            text = "This ACP agent does not support switching the model at runtime.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                items(acpModels) { m ->
                    SelectableRow(
                        label = m.name ?: m.modelId ?: "?",
                        sub = m.modelId,
                        selected = m.modelId == currentModel,
                        enabled = acpSupportsRuntimeModelSwitch,
                        testTag = "acp_model_${m.modelId}"
                    ) { onSwitchModel(m.modelId ?: return@SelectableRow) }
                }
            }

            if (efforts.isNotEmpty()) {
                item { SectionLabel("Reasoning effort") }
                if (!acpSupportsRuntimeModelSwitch) {
                    item {
                        Text(
                            text = "Effort switching is unavailable for this ACP agent.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(efforts) { e ->
                            val selected = e.value == currentEffort
                            Surface(
                                color = if (selected) OpenHandsPrimaryDark else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .clickable(enabled = acpSupportsRuntimeModelSwitch) { e.value?.let(onSwitchEffort) }
                                    .testTag("effort_${e.value}")
                            ) {
                                Text(
                                    text = e.name ?: e.value ?: "?",
                                    fontSize = 12.sp,
                                    color = if (selected) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = if (acpSupportsRuntimeModelSwitch) 1f else 0.5f
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!isAcp) {
                item { SectionLabel("Model catalog") }
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            onSearchModels(it)
                        },
                        placeholder = { Text("Search models…") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("model_search_input")
                    )
                }
                items(catalogModels) { m ->
                    val full = listOfNotNull(m.provider, m.name).joinToString("/")
                    SelectableRow(
                        label = m.name ?: "?",
                        sub = m.provider,
                        selected = full == currentModel || m.name == currentModel,
                        testTag = "catalog_model_${m.name}"
                    ) { onSwitchModel(m.name ?: return@SelectableRow) }
                }
            }

            if (isBusy) {
                item {
                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = OpenHandsPrimaryDark, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

/** Context panel: token/cost metrics plus a compaction action. */
@Composable
fun ContextSheet(
    info: ConversationInfoDto?,
    isBusy: Boolean,
    onCompact: () -> Unit,
    onDismiss: () -> Unit,
) {
    val usage = info?.metrics?.accumulatedTokenUsage
    SheetScaffold(title = "Context", onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            MetricRow("Model", info?.currentModelId ?: info?.metrics?.modelName ?: "—")
            MetricRow("Prompt tokens", usage?.promptTokens?.toString() ?: "—")
            MetricRow("Completion tokens", usage?.completionTokens?.toString() ?: "—")
            MetricRow("Context window", usage?.contextWindow?.takeIf { it > 0 }?.toString() ?: "—")
            MetricRow("Accumulated cost", info?.metrics?.accumulatedCost?.let { "$${it}" } ?: "—")
            MetricRow("Max iterations", info?.maxIterations?.toString() ?: "—")
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onCompact,
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = OpenHandsPrimaryDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("compact_context_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                } else {
                    Text("Compact context", color = Color.White)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Compaction summarises the conversation so it fits the model's context window.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
    }
}

/** Sandbox terminal: run a one-shot command and show its output. */
@Composable
fun TerminalSheet(
    command: String,
    output: String?,
    isBusy: Boolean,
    onCommandChange: (String) -> Unit,
    onRun: () -> Unit,
    onRunAsync: (String) -> Unit = {},
    onDismiss: () -> Unit,
) {
    SheetScaffold(title = "Terminal", onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = command,
                onValueChange = onCommandChange,
                placeholder = { Text("e.g. git status") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("terminal_input")
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onRun,
                    enabled = command.isNotBlank() && !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = OpenHandsPrimaryDark),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("terminal_run_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Run", color = Color.White)
                    }
                }
                OutlinedButton(
                    onClick = { if (command.isNotBlank()) onRunAsync(command) },
                    enabled = command.isNotBlank() && !isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("terminal_run_async_button")
                ) { Text("Run in background") }
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                color = Color(0xFF0D1117),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SelectionContainer {
                    Text(
                        text = output ?: "Output appears here.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFC9D1D9),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

/** Sandbox file browser. Tapping a directory descends into it. */
@Composable
fun FilesSheet(
    path: String?,
    entries: List<FileEntryDto>,
    onOpen: (String?) -> Unit,
    onDownload: (String) -> Unit = {},
    onDismiss: () -> Unit,
) {
    SheetScaffold(title = "Files", onDismiss = onDismiss) {
        LazyColumn {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!path.isNullOrBlank()) {
                        TextButton(
                            onClick = { onOpen(null) },
                            modifier = Modifier.testTag("files_up_button")
                        ) { Text("↑ Home") }
                    }
                    Text(
                        text = path ?: "~",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            items(entries) { entry ->
                SelectableRow(
                    label = (if (entry.isDirectory) "📁 " else "📄 ") + (entry.name ?: "?"),
                    sub = if (entry.isDirectory) null else "${entry.size} bytes",
                    testTag = "file_${entry.path}"
                ) {
                    if (entry.isDirectory) onOpen(entry.path)
                    else entry.path?.let(onDownload)
                }
            }
            if (entries.isEmpty()) {
                item {
                    Text(
                        "No entries. Tap Home to load the workspace root.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

/** Git panel: recent commits and a diff viewer. */
@Composable
fun GitSheet(
    commits: List<GitCommitDto>,
    diff: GitDiffDto?,
    onLoadCommits: () -> Unit,
    onLoadWorkingDiff: () -> Unit = {},
    onSelectCommit: (String) -> Unit = {},
    onDismiss: () -> Unit,
) {
    SheetScaffold(title = "Git", onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onLoadCommits,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("git_load_commits_button")
                ) { Text("Recent commits") }
                OutlinedButton(
                    onClick = onLoadWorkingDiff,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("git_load_working_diff_button")
                ) { Text("Working tree") }
            }

            if (commits.isNotEmpty()) {
                SectionLabel("Commits")
                LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                    items(commits) { c ->
                        SelectableRow(
                            label = c.subject ?: "(no subject)",
                            sub = "${c.shortSha ?: ""} · ${c.author ?: ""}",
                            testTag = "commit_${c.sha}"
                        ) { c.sha?.let(onSelectCommit) }
                    }
                }
            }

            diff?.let {
                SectionLabel("Diff")
                Surface(
                    color = Color(0xFF0D1117),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SelectionContainer {
                        Text(
                            text = buildString {
                                it.original?.let { o -> append("--- original\n").append(o.take(1500)).append("\n\n") }
                                it.modified?.let { m -> append("+++ modified\n").append(m.take(1500)) }
                            }.ifBlank { "No diff available." },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFFC9D1D9),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}
