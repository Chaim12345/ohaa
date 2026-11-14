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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BashEventDto
import com.example.data.model.ConversationInfoDto
import com.example.data.model.ConversationRuntimeDto
import com.example.data.model.InstalledPluginDto
import com.example.data.model.InstalledSkillDto
import com.example.data.model.LlmProviderDto
import com.example.data.model.RuntimeLlmModelDto
import com.example.data.model.RuntimeSettingsDto
import com.example.data.model.SubAgentDto
import com.example.data.model.ToolCatalogEntryDto
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
            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)) { content() }
        },
        shape = sheetShape,
    )
}

@Composable
private fun MetricLine(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value ?: "—",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ActionChip(label: String, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        color = if (enabled) OpenHandsPrimaryDark.copy(alpha = 0.12f)
        else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(tag)
    ) {
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (enabled) OpenHandsPrimaryDark else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

/** Advanced runtime controls: confirmation policy, security analyzer, reprovision, secrets. */
@Composable
fun AdvancedRuntimeSheet(
    runtimeInfo: ConversationRuntimeDto?,
    isBusy: Boolean,
    onSetConfirmationPolicy: (String) -> Unit,
    onSetSecurityAnalyzer: (Boolean) -> Unit,
    onReprovision: () -> Unit,
    onLoadRuntimeInfo: () -> Unit,
    runtimeProviders: List<LlmProviderDto> = emptyList(),
    runtimeModels: List<RuntimeLlmModelDto> = emptyList(),
    onLoadRuntimeProviders: () -> Unit = {},
    onLoadRuntimeModels: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    SheetScaffold(title = "Advanced runtime", onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            MetricLine("Runtime id", runtimeInfo?.id)
            MetricLine("Status", runtimeInfo?.status)
            MetricLine("Workspace", runtimeInfo?.workspaceDir)
            MetricLine("Sandbox", runtimeInfo?.sandboxId)

            Text(
                "Confirmation policy",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf("always", "never", "auto")) { policy ->
                    ActionChip(policy, "confirm_policy_$policy", enabled = !isBusy) {
                        onSetConfirmationPolicy(policy)
                    }
                }
            }

            Text(
                "Security analyzer",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(listOf(true to "Enable", false to "Disable")) { (value, label) ->
                    ActionChip(label, "security_analyzer_$value", enabled = !isBusy) {
                        onSetSecurityAnalyzer(value)
                    }
                }
            }

            Text(
                "Runtime maintenance",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionChip("Refresh info", "runtime_refresh", enabled = !isBusy, onClick = onLoadRuntimeInfo)
                ActionChip("Reprovision", "runtime_reprovision", enabled = !isBusy, onClick = onReprovision)
            }

            Text(
                "Runtime LLM providers",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionChip("Load providers", "runtime_providers_load", enabled = !isBusy, onClick = onLoadRuntimeProviders)
                ActionChip("Load models", "runtime_models_load", enabled = !isBusy, onClick = onLoadRuntimeModels)
            }
            if (runtimeProviders.isNotEmpty()) {
                runtimeProviders.forEach { p ->
                    Text(
                        "• ${p.name ?: "?"}${if (p.verified) " ✓" else ""}",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (runtimeModels.isNotEmpty()) {
                Text(
                    "Models",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
                runtimeModels.forEach { m ->
                    Text("• ${m.name ?: "?"} (${m.provider ?: "?"})", fontSize = 12.sp)
                }
            }

            if (isBusy) {
                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = OpenHandsPrimaryDark, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

/** Goal lifecycle: start a goal, resume or stop the running one. */
@Composable
fun GoalSheet(
    isBusy: Boolean,
    onStartGoal: (String) -> Unit,
    onResumeGoal: () -> Unit,
    onStopGoal: () -> Unit,
    onDismiss: () -> Unit,
) {
    var goal by remember { mutableStateOf("") }
    SheetScaffold(title = "Goal", onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                label = { Text("Goal description") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("goal_input")
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionChip("Start", "goal_start", enabled = !isBusy && goal.isNotBlank()) {
                    onStartGoal(goal)
                }
                ActionChip("Resume", "goal_resume", enabled = !isBusy, onClick = onResumeGoal)
                ActionChip("Stop", "goal_stop", enabled = !isBusy, onClick = onStopGoal)
            }
        }
    }
}

/** Sandbox tools catalog plus recent bash events. */
@Composable
fun ToolsSheet(
    tools: List<ToolCatalogEntryDto>,
    bashEvents: List<BashEventDto>,
    onLoadTools: () -> Unit,
    onLoadBashEvents: () -> Unit,
    subAgents: List<SubAgentDto> = emptyList(),
    onLoadSubAgents: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    SheetScaffold(title = "Sandbox tools", onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionChip("Refresh tools", "tools_refresh", onClick = onLoadTools)
                ActionChip("Bash history", "bash_refresh", onClick = onLoadBashEvents)
                ActionChip("Sub-agents", "subagents_load", onClick = onLoadSubAgents)
            }
            Spacer(Modifier.height(8.dp))
            if (subAgents.isNotEmpty()) {
                Text(
                    "Sub-agents",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                subAgents.forEach { a ->
                    Text("• ${a.name ?: "?"}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (!a.description.isNullOrBlank()) {
                        Text(a.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            if (tools.isEmpty()) {
                Text("No tools loaded.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                tools.forEach { tool ->
                    Text(
                        "• ${tool.name ?: "?"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (!tool.description.isNullOrBlank()) {
                        Text(tool.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (bashEvents.isNotEmpty()) {
                Text(
                    "Bash history",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                bashEvents.forEach { ev ->
                    Text(
                        "${ev.kind ?: "?"}: ${ev.command ?: ev.stdout ?: ev.stderr ?: ""}".take(160),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Skills and plugins installed in the running sandbox. */
@Composable
fun RuntimeSkillsSheet(
    skills: List<InstalledSkillDto>,
    plugins: List<InstalledPluginDto>,
    settings: RuntimeSettingsDto?,
    onLoad: () -> Unit,
    onSync: () -> Unit,
    onInstallSkill: (String) -> Unit,
    cloudSkills: List<InstalledSkillDto> = emptyList(),
    marketplaceSkills: List<String> = emptyList(),
    marketplacePlugins: List<String> = emptyList(),
    mcpOAuthUrl: String? = null,
    mcpOAuthStatus: String? = null,
    mcpToolOutput: String? = null,
    onLoadCloudCatalog: () -> Unit = {},
    onPreviewMarketplace: (String, String) -> Unit = { _, _ -> },
    onStartMcpOAuth: (String) -> Unit = {},
    onCheckMcpOAuth: () -> Unit = {},
    onCallMcpTool: (String, String) -> Unit = { _, _ -> },
    onTestMcpServer: (String, String) -> Unit = { _, _ -> },
    onDismiss: () -> Unit,
) {
    var installName by remember { mutableStateOf("") }
    var marketplaceName by remember { mutableStateOf("") }
    var marketplaceSource by remember { mutableStateOf("") }
    var mcpName by remember { mutableStateOf("") }
    var mcpToolName by remember { mutableStateOf("") }
    var mcpServerJson by remember { mutableStateOf("") }
    SheetScaffold(title = "Skills & plugins", onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionChip("Refresh", "skills_refresh", onClick = onLoad)
                ActionChip("Sync", "skills_sync", onClick = onSync)
                ActionChip("Cloud catalog", "skills_cloud_catalog", onClick = onLoadCloudCatalog)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = installName,
                onValueChange = { installName = it },
                label = { Text("Install skill by name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("skill_install_input")
            )
            Spacer(Modifier.height(6.dp))
            ActionChip("Install", "skill_install", enabled = installName.isNotBlank()) {
                onInstallSkill(installName)
            }
            Text(
                "Installed skills",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            if (skills.isEmpty()) {
                Text("None", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                skills.forEach { s ->
                    Text("• ${s.name ?: "?"} ${s.version ?: ""}", fontSize = 12.sp)
                }
            }
            Text(
                "Installed plugins",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            if (plugins.isEmpty()) {
                Text("None", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                plugins.forEach { p -> Text("• ${p.name ?: "?"} ${p.version ?: ""}", fontSize = 12.sp) }
            }
            if (cloudSkills.isNotEmpty()) {
                Text(
                    "Cloud skill catalog",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
                cloudSkills.forEach { s ->
                    Text("• ${s.name ?: "?"} ${s.source ?: ""}", fontSize = 12.sp)
                }
            }

            Text(
                "Marketplace preview",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = marketplaceName,
                    onValueChange = { marketplaceName = it },
                    label = { Text("Marketplace name") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("marketplace_name_input")
                )
                OutlinedTextField(
                    value = marketplaceSource,
                    onValueChange = { marketplaceSource = it },
                    label = { Text("Source") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("marketplace_source_input")
                )
            }
            Spacer(Modifier.height(6.dp))
            ActionChip(
                "Preview",
                "marketplace_preview",
                enabled = marketplaceName.isNotBlank() && marketplaceSource.isNotBlank()
            ) { onPreviewMarketplace(marketplaceName, marketplaceSource) }
            if (marketplaceSkills.isNotEmpty() || marketplacePlugins.isNotEmpty()) {
                Text(
                    "Skills: ${marketplaceSkills.joinToString(", ")}",
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text("Plugins: ${marketplacePlugins.joinToString(", ")}", fontSize = 11.sp)
            }

            Text(
                "MCP OAuth",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            OutlinedTextField(
                value = mcpName,
                onValueChange = { mcpName = it },
                label = { Text("MCP server name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("mcp_name_input")
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionChip("Start OAuth", "mcp_start", enabled = mcpName.isNotBlank()) {
                    onStartMcpOAuth(mcpName)
                }
                ActionChip("Check status", "mcp_status", onClick = onCheckMcpOAuth)
            }
            mcpOAuthUrl?.let {
                SelectionContainer {
                    Text(it, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
            mcpOAuthStatus?.let { Text("Status: $it", fontSize = 11.sp) }

            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = mcpServerJson,
                onValueChange = { mcpServerJson = it },
                label = { Text("MCP server JSON ({\"command\":\"...\"})") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth().testTag("mcp_server_json_input")
            )
            Spacer(Modifier.height(6.dp))
            ActionChip(
                "Test server",
                "mcp_test",
                enabled = mcpName.isNotBlank() && mcpServerJson.isNotBlank()
            ) { onTestMcpServer(mcpName, mcpServerJson) }

            Text(
                "Invoke MCP tool",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            OutlinedTextField(
                value = mcpToolName,
                onValueChange = { mcpToolName = it },
                label = { Text("Tool name (on the server above)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("mcp_tool_name_input")
            )
            Spacer(Modifier.height(6.dp))
            ActionChip(
                "Call tool",
                "mcp_call_tool",
                enabled = mcpName.isNotBlank() && mcpToolName.isNotBlank()
            ) { onCallMcpTool(mcpName, mcpToolName) }
            mcpToolOutput?.let {
                Spacer(Modifier.height(6.dp))
                SelectionContainer {
                    Text(it, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

/** Opens the in-sandbox VS Code URL in the system browser. */
@Composable
fun VSCodeSheet(url: String?, isLoading: Boolean, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    SheetScaffold(title = "VS Code", onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            if (isLoading) {
                CircularProgressIndicator(color = OpenHandsPrimaryDark, modifier = Modifier.size(24.dp))
            } else if (url.isNullOrBlank()) {
                Text("VS Code is not available for this sandbox.", fontSize = 12.sp)
            } else {
                SelectionContainer {
                    Text(url, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { runCatching { uriHandler.openUri(url) } },
                    colors = ButtonDefaults.buttonColors(containerColor = OpenHandsPrimaryDark),
                    modifier = Modifier.testTag("vscode_open")
                ) { Text("Open in browser", color = Color.White) }
            }
        }
    }
}

/** Read-only view of an agent's final response. */
@Composable
fun FinalResponseSheet(response: String?, onDismiss: () -> Unit) {
    SheetScaffold(title = "Agent final response", onDismiss = onDismiss) {
        SelectionContainer {
            Text(
                response ?: "No final response available.",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/** Spawn and list sub-conversations (children) of the open conversation. */
@Composable
fun SubConversationSheet(
    children: List<ConversationInfoDto>,
    workspaceInfo: String?,
    isBusy: Boolean,
    onStart: (String) -> Unit,
    onLoad: () -> Unit,
    onLoadWorkspace: () -> Unit,
    onDismiss: () -> Unit,
) {
    var task by remember { mutableStateOf("") }
    SheetScaffold(title = "Sub-conversations", onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = task,
                onValueChange = { task = it },
                label = { Text("Child task brief") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("child_task_input")
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ActionChip("Start child", "child_start", enabled = !isBusy && task.isNotBlank()) {
                    onStart(task)
                }
                ActionChip("Refresh", "child_refresh", enabled = !isBusy, onClick = onLoad)
                ActionChip("Workspace", "child_workspace", enabled = !isBusy, onClick = onLoadWorkspace)
            }
            if (workspaceInfo != null) {
                Spacer(Modifier.height(8.dp))
                SelectionContainer {
                    Text(workspaceInfo, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
            Text(
                "Children",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            if (children.isEmpty()) {
                Text("No sub-conversations.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                children.forEach { child ->
                    Text("• ${child.id?.take(8) ?: "?"} ${child.title ?: ""}", fontSize = 12.sp)
                }
            }
        }
    }
}

