package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BranchDto
import com.example.data.model.PromptTemplate
import com.example.data.model.RepositoryDto
import com.example.data.model.SuggestedTaskDto
import com.example.data.settings.AgentProfile
import com.example.ui.components.AgentProfileDialog
import com.example.ui.components.CanvasCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.CanvasBorder
import com.example.ui.theme.CanvasColors
import com.example.ui.theme.CanvasPrimary
import com.example.ui.theme.CanvasPrimaryDeep
import com.example.ui.theme.Shapes
import com.example.ui.theme.Sizes
import com.example.ui.theme.Spacing
import com.example.ui.viewmodel.LaunchStage

/**
 * New task composer.
 *
 * Rebuilt so the prompt is the first and largest thing on screen — the previous
 * version buried it under a repo picker and template row. Repo, branch and
 * suggestions are now progressive: they appear as you use them.
 */
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
    LaunchedEffect(Unit) { onLoadSuggestedTasks() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(Spacing.screen)
            .testTag("new_task_screen")
    ) {
        Text("New task", style = MaterialTheme.typography.titleLarge)
        Text(
            "Describe the work. The agent runs in its own sandbox and reports back.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.lg)
        )

        OutlinedTextField(
            value = promptText,
            onValueChange = onPromptChanged,
            placeholder = { Text("Fix the failing checkout test and open a PR") },
            minLines = 5,
            shape = Shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CanvasPrimary,
                unfocusedBorderColor = CanvasBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("field_prompt")
        )

        Spacer(Modifier.height(Spacing.md))

        // Model selection is a compact summary row, not a form field.
        CanvasCard(onClick = { showProfileDialog = true }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = CanvasPrimary,
                    modifier = Modifier.size(Sizes.iconSmall)
                )
                Spacer(Modifier.width(Spacing.sm))
                Column(Modifier.weight(1f)) {
                    Text("Model", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        activeProfile.shortLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    "Change",
                    style = MaterialTheme.typography.labelMedium,
                    color = CanvasPrimary
                )
            }
        }

        Spacer(Modifier.height(Spacing.md))

        OutlinedTextField(
            value = repoText,
            onValueChange = onRepoChanged,
            placeholder = { Text("owner/repo (optional)") },
            singleLine = true,
            leadingIcon = {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            shape = Shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CanvasPrimary,
                unfocusedBorderColor = CanvasBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("field_repo")
        )

        Spacer(Modifier.height(Spacing.sm))
        OutlinedButton(
            onClick = onSearchRepositories,
            enabled = !isSearchingRepos,
            shape = Shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_search_repos")
        ) {
            if (isSearchingRepos) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(Spacing.sm))
            }
            Text("Browse my repositories")
        }

        if (repoResults.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.sm))
            CanvasCard {
                SectionHeader("Repositories", subtitle = "${repoResults.size} found")
                Spacer(Modifier.height(Spacing.sm))
                repoResults.take(8).forEach { repo ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectRepository(repo) }
                            .padding(vertical = Spacing.sm)
                            .testTag("repo_row")
                    ) {
                        Text(
                            repo.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        repo.gitProvider?.let {
                            Text(it, fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (branchResults.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.md))
            SectionHeader("Branch", subtitle = selectedBranch.ifBlank { "default" })
            Spacer(Modifier.height(Spacing.sm))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(branchResults, key = { it.name }) { branch ->
                    FilterChip(
                        selected = selectedBranch == branch.name,
                        onClick = { onSelectBranch(branch.name) },
                        label = { Text(branch.name, fontSize = 11.sp, maxLines = 1) },
                        modifier = Modifier.testTag("branch_chip")
                    )
                }
            }
        }

        if (isLoadingSuggested || suggestedTasks.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.lg))
            SectionHeader(
                title = "Suggested from your repos",
                subtitle = "Failing checks and open issues",
                trailing = {
                    if (isLoadingSuggested) {
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                    }
                }
            )
            Spacer(Modifier.height(Spacing.sm))
            suggestedTasks.take(5).forEach { task ->
                CanvasCard(
                    onClick = { onSelectSuggestedTask(task) },
                    modifier = Modifier.padding(bottom = Spacing.sm).testTag("suggested_task_row")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            tint = CanvasColors.warning,
                            modifier = Modifier.size(Sizes.iconSmall)
                        )
                        Spacer(Modifier.width(Spacing.sm))
                        Column(Modifier.weight(1f)) {
                            Text(
                                task.title.ifBlank { "Untitled task" },
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                buildString {
                                    if (task.repo.isNotBlank()) append(task.repo)
                                    if (task.issueNumber > 0) {
                                        if (isNotEmpty()) append(" · ")
                                        append("#").append(task.issueNumber)
                                    }
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (templates.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.lg))
            SectionHeader("Quick fill", subtitle = "Saved prompts")
            Spacer(Modifier.height(Spacing.sm))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                items(templates, key = { it.id }) { template ->
                    AssistChip(
                        onClick = { onSelectTemplate(template) },
                        label = { Text(template.title, fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.lg))

        if (isLaunching) {
            CanvasCard(accent = CanvasPrimary) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(Spacing.md))
                    Text(
                        launchStatusText ?: launchStage.label.ifBlank { "Launching…" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
        }

        if (isDemoMode) {
            CanvasCard(accent = CanvasColors.warning) {
                Text(
                    "Not connected — add your API key in Settings to launch a live agent.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(Spacing.md))
        }

        Button(
            onClick = onLaunchTask,
            enabled = !isLaunching && promptText.isNotBlank(),
            shape = Shapes.medium,
            colors = ButtonDefaults.buttonColors(containerColor = CanvasPrimaryDeep),
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizes.touchTarget)
                .testTag("btn_launch")
        ) {
            Icon(
                Icons.Default.RocketLaunch,
                contentDescription = null,
                modifier = Modifier.size(Sizes.iconMedium)
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                when {
                    isLaunching -> "Launching…"
                    isDemoMode -> "Connect to launch"
                    else -> "Launch task"
                },
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(Spacing.xl))
    }

    if (showProfileDialog) {
        AgentProfileDialog(
            profile = activeProfile,
            onDismiss = { showProfileDialog = false },
            onModelChanged = onModelChanged,
            title = "Model",
            availableModels = availableModels
        )
    }
}

