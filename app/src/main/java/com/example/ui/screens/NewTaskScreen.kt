package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PromptTemplate
import com.example.data.model.RepositoryDto
import com.example.data.model.BranchDto
import com.example.data.model.SuggestedTaskDto
import com.example.data.settings.AgentProfile
import com.example.ui.components.AgentProfileDialog
import com.example.ui.components.ProfileSummaryCard
import com.example.ui.theme.CanvasPrimary
import com.example.ui.theme.CanvasPrimaryDeep
import com.example.ui.viewmodel.LaunchStage

@Composable
fun NewTaskScreen(
    promptText: String,
    repoText: String,
    templates: List<PromptTemplate>,
    isLaunching: Boolean,
    launchStage: LaunchStage,
    launchStatusText: String?,
    isDemoMode: Boolean,
    activeProfile: AgentProfile,
    repoResults: List<RepositoryDto>,
    branchResults: List<BranchDto>,
    selectedBranch: String,
    isSearchingRepos: Boolean,
    onPromptChanged: (String) -> Unit,
    onRepoChanged: (String) -> Unit,
    onSelectTemplate: (PromptTemplate) -> Unit,
    onLaunchTask: () -> Unit,
    onSearchRepositories: () -> Unit,
    onSelectRepository: (RepositoryDto) -> Unit,
    onSelectBranch: (String) -> Unit,
    onModelChanged: (String) -> Unit,
    suggestedTasks: List<SuggestedTaskDto> = emptyList(),
    isLoadingSuggested: Boolean = false,
    availableModels: List<String> = emptyList(),
    onLoadSuggestedTasks: () -> Unit = {},
    onSelectSuggestedTask: (SuggestedTaskDto) -> Unit = {}
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    // Load suggested tasks the first time this screen is shown.
    androidx.compose.runtime.LaunchedEffect(Unit) { onLoadSuggestedTasks() }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp)
            .testTag("new_task_screen")
    ) {
        Text("New agent task", style = MaterialTheme.typography.titleLarge)
        Text(
            "OpenHands will work in its own sandbox and report back with a summary, diffs, and test results.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
        )

        OutlinedTextField(
            value = promptText,
            onValueChange = onPromptChanged,
            placeholder = { Text("Describe the task — e.g. \"Fix the failing checkout test and open a PR\"") },
            minLines = 5,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CanvasPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("prompt_input")
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = repoText,
            onValueChange = onRepoChanged,
            placeholder = { Text("owner/repository (optional)") },
            leadingIcon = { Icon(Icons.Default.Folder, null, Modifier.size(18.dp)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CanvasPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("repo_input")
        )

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = onSearchRepositories,
            enabled = !isSearchingRepos,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_pull_repos")
        ) {
            if (isSearchingRepos) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Pulling from provider…")
            } else {
                Icon(Icons.Default.Folder, null, Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pull repositories from git provider")
            }
        }

        if (repoResults.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Repositories",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(
                Modifier.padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repoResults.take(8).forEach { repo ->
                    AssistChip(
                        onClick = { onSelectRepository(repo) },
                        label = { Text(repo.displayName, fontSize = 11.sp, maxLines = 1) },
                        leadingIcon = {
                            Icon(Icons.Default.Folder, null, Modifier.size(14.dp), tint = CanvasPrimary)
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        if (branchResults.isNotEmpty() || selectedBranch.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Branch",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                items(branchResults, key = { it.name }) { branch ->
                    FilterChip(
                        selected = selectedBranch == branch.name,
                        onClick = { onSelectBranch(branch.name) },
                        label = { Text(branch.name, fontSize = 11.sp, maxLines = 1) }
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Suggested from your repos", style = MaterialTheme.typography.titleMedium)
            if (isLoadingSuggested) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
            }
        }
        if (!isLoadingSuggested && suggestedTasks.isEmpty()) {
            Text(
                "No failing checks or open issues found on connected repositories.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            suggestedTasks.take(6).forEach { task ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .testTag("suggested_task_row")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                task.title.ifBlank { "Untitled task" },
                                fontSize = 13.sp,
                                maxLines = 2
                            )
                            Text(
                                buildString {
                                    if (task.repo.isNotBlank()) append(task.repo)
                                    if (task.issueNumber > 0) {
                                        if (isNotEmpty()) append(" · ")
                                        append("#")
                                        append(task.issueNumber)
                                    }
                                    task.taskType?.takeIf { it.isNotBlank() }?.let {
                                        if (isNotEmpty()) append(" · ")
                                        append(it)
                                    }
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = { onSelectSuggestedTask(task) },
                            modifier = Modifier.testTag("btn_use_suggested_task")
                        ) {
                            Text("Use", color = CanvasPrimaryDeep)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("Quick fill", style = MaterialTheme.typography.titleMedium)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            items(templates, key = { it.id }) { template ->
                AssistChip(
                    onClick = { onSelectTemplate(template) },
                    label = { Text(template.title, fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.AutoAwesome,
                            null,
                            Modifier.size(14.dp),
                            tint = CanvasPrimary
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        if (isLaunching) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = CanvasPrimary.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("launch_progress")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        launchStatusText ?: LaunchStage.WAITING_SANDBOX.label,
                        fontSize = 13.sp,
                        color = CanvasPrimary
                    )
                }
            }
        }

        if (isDemoMode) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    "Not connected — add your API key in Settings to launch a live cloud agent.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        ProfileSummaryCard(
            profile = activeProfile,
            onEdit = { showProfileDialog = true }
        )

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onLaunchTask,
            enabled = !isLaunching && promptText.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CanvasPrimaryDeep),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_launch")
        ) {
            Icon(Icons.Default.RocketLaunch, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                when {
                    isLaunching -> "Launching…"
                    isDemoMode -> "Connect to launch"
                    else -> "Launch task"
                },
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showProfileDialog) {
        AgentProfileDialog(
            profile = activeProfile,
            onDismiss = { showProfileDialog = false },
            onModelChanged = onModelChanged,
            title = "Agent & LLM · new task",
            availableModels = availableModels
        )
    }
}
