package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GitBranch
import com.example.data.model.GitRepository
import com.example.data.model.PromptTemplate
import com.example.data.model.StartTaskProgress
import com.example.data.model.StartTaskStatus
import com.example.data.model.SuggestedTask
import com.example.ui.components.InfoCard
import com.example.ui.components.SectionTitle
import com.example.ui.theme.OpenHandsPrimaryDark
import com.example.ui.theme.StatusError
import com.example.ui.viewmodel.NewConversationForm

@Composable
fun NewConversationScreen(
    form: NewConversationForm,
    templates: List<PromptTemplate>,
    startProgress: StartTaskProgress?,
    hasApiKey: Boolean,
    onFormChange: ((NewConversationForm) -> NewConversationForm) -> Unit,
    onRepoQueryChange: (String) -> Unit,
    onRepoProviderChange: (String) -> Unit,
    onSelectRepository: (GitRepository) -> Unit,
    onSearchBranches: (String) -> Unit,
    onSelectTemplate: (PromptTemplate) -> Unit,
    onSelectSuggestedTask: (SuggestedTask) -> Unit,
    onLoadSuggestedTasks: () -> Unit,
    onStart: () -> Unit,
    onCancelStart: () -> Unit,
    onDismissStartError: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRepoPicker by remember { mutableStateOf(false) }
    var showBranchPicker by remember { mutableStateOf(false) }

    LaunchedEffect(hasApiKey) { if (hasApiKey) onLoadSuggestedTasks() }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(OpenHandsPrimaryDark.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = OpenHandsPrimaryDark, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("New conversation", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text("The agent runs in an OpenHands Cloud sandbox", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (!hasApiKey) {
            Spacer(Modifier.height(14.dp))
            InfoCard(tint = StatusError) {
                Column {
                    Text("No API key configured", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text("Add your OpenHands Cloud API key before starting a conversation.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onOpenSettings) { Text("Open settings") }
                }
            }
        }

        // Templates
        if (templates.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            SectionTitle("Start from a template")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(templates, key = { it.id }) { t ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.clickable { onSelectTemplate(t) }.testTag("template_${t.id}")
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(t.title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            Text(t.category, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Suggested tasks from git providers
        if (form.suggestedTasks.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            SectionTitle("Suggested from your repositories")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(form.suggestedTasks.take(20)) { task ->
                    Surface(
                        color = OpenHandsPrimaryDark.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.clickable { onSelectSuggestedTask(task) }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).width(220.dp)) {
                            Text(task.title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${task.repo} #${task.issueNumber}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = OpenHandsPrimaryDark)
                        }
                    }
                }
            }
        }

        // Prompt
        Spacer(Modifier.height(18.dp))
        SectionTitle("Task")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = form.prompt,
            onValueChange = { v -> onFormChange { it.copy(prompt = v) } },
            placeholder = { Text("Describe what the agent should do. Be specific about the outcome you want and how to verify it.") },
            minLines = 5,
            maxLines = 12,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OpenHandsPrimaryDark, unfocusedBorderColor = MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().testTag("prompt_input")
        )

        // Repository + branch
        Spacer(Modifier.height(18.dp))
        SectionTitle("Repository (optional)")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = form.repository,
            onValueChange = { v -> onFormChange { it.copy(repository = v, branch = "", gitProvider = null, branchResults = emptyList()) } },
            placeholder = { Text("owner/repo") },
            leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, tint = OpenHandsPrimaryDark) },
            trailingIcon = {
                IconButton(onClick = { showRepoPicker = true; onRepoQueryChange(form.repository) }, enabled = hasApiKey) {
                    Icon(Icons.Default.Search, contentDescription = "Browse repositories")
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OpenHandsPrimaryDark, unfocusedBorderColor = MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().testTag("repo_input")
        )
        if (form.repository.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = form.branch,
                onValueChange = { v -> onFormChange { it.copy(branch = v) } },
                placeholder = { Text("Branch (default branch if empty)") },
                leadingIcon = { Icon(Icons.Default.AccountTree, contentDescription = null, tint = OpenHandsPrimaryDark) },
                trailingIcon = {
                    if (form.isLoadingBranches) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else IconButton(onClick = { showBranchPicker = true; onSearchBranches("") }, enabled = hasApiKey) {
                        Icon(Icons.Default.Search, contentDescription = "Browse branches")
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OpenHandsPrimaryDark, unfocusedBorderColor = MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().testTag("branch_input")
            )
            if (form.branchResults.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(form.branchResults.take(15), key = { it.name }) { b ->
                        FilterChip(
                            selected = form.branch == b.name,
                            onClick = { onFormChange { it.copy(branch = b.name) } },
                            label = { Text(b.name, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                            leadingIcon = if (b.isProtected) ({ Icon(Icons.Default.Lock, contentDescription = "protected", modifier = Modifier.size(12.dp)) }) else null,
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = OpenHandsPrimaryDark.copy(alpha = 0.2f), selectedLabelColor = OpenHandsPrimaryDark)
                        )
                    }
                }
            }
        }

        // Agent type
        Spacer(Modifier.height(18.dp))
        SectionTitle("Agent mode")
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            listOf("default" to "Build", "plan" to "Plan").forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = form.agentType == value,
                    onClick = { onFormChange { it.copy(agentType = value) } },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                    icon = { if (form.agentType == value) Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    label = { Text(label) }
                )
            }
        }
        Text(
            if (form.agentType == "plan") "Plan mode: the agent investigates and proposes a plan without changing files."
            else "Build mode: the agent edits code, runs commands and verifies its work.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )

        // Advanced
        Spacer(Modifier.height(14.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { onFormChange { it.copy(showAdvanced = !it.showAdvanced) } }.padding(vertical = 6.dp)
        ) {
            Text("Advanced options", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Icon(if (form.showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (form.showAdvanced) {
            val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OpenHandsPrimaryDark, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
            OutlinedTextField(
                value = form.title, onValueChange = { v -> onFormChange { it.copy(title = v) } },
                label = { Text("Title") }, placeholder = { Text("Auto-generated if empty") },
                singleLine = true, shape = RoundedCornerShape(12.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = form.llmModel, onValueChange = { v -> onFormChange { it.copy(llmModel = v) } },
                label = { Text("LLM model override") }, placeholder = { Text("e.g. anthropic/claude-sonnet-4-5") },
                singleLine = true, shape = RoundedCornerShape(12.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = form.spanName, onValueChange = { v -> onFormChange { it.copy(spanName = v) } },
                label = { Text("Observability span name") },
                singleLine = true, shape = RoundedCornerShape(12.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = form.tags, onValueChange = { v -> onFormChange { it.copy(tags = v) } },
                label = { Text("Observability tags (comma separated)") },
                singleLine = true, shape = RoundedCornerShape(12.dp), colors = fieldColors, modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onStart,
            enabled = form.prompt.isNotBlank() && startProgress == null && hasApiKey,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OpenHandsPrimaryDark),
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("start_conversation_button")
        ) {
            Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Start conversation", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showRepoPicker) {
        RepositoryPickerDialog(
            query = form.repoQuery,
            provider = form.repoProvider,
            results = form.repoResults,
            isSearching = form.isSearchingRepos,
            onQueryChange = onRepoQueryChange,
            onProviderChange = onRepoProviderChange,
            onSelect = { onSelectRepository(it); showRepoPicker = false },
            onDismiss = { showRepoPicker = false }
        )
    }
    if (showBranchPicker) {
        BranchPickerDialog(
            branches = form.branchResults,
            isLoading = form.isLoadingBranches,
            onSearch = onSearchBranches,
            onSelect = { onFormChange { f -> f.copy(branch = it.name) }; showBranchPicker = false },
            onDismiss = { showBranchPicker = false }
        )
    }
    startProgress?.let { StartProgressDialog(it, onCancel = onCancelStart, onDismissError = onDismissStartError) }
}

@Composable
private fun RepositoryPickerDialog(
    query: String,
    provider: String,
    results: List<GitRepository>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onProviderChange: (String) -> Unit,
    onSelect: (GitRepository) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a repository") },
        text = {
            Column {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    items(listOf("github", "gitlab", "bitbucket", "azure_devops", "forgejo")) { p ->
                        FilterChip(
                            selected = provider == p,
                            onClick = { onProviderChange(p) },
                            label = { Text(p.replace('_', ' '), fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = query, onValueChange = onQueryChange, singleLine = true,
                    placeholder = { Text("Search your repositories") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("repo_search_input")
                )
                Spacer(Modifier.height(8.dp))
                if (isSearching) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
                Column(modifier = Modifier.height(320.dp).verticalScroll(rememberScrollState())) {
                    if (results.isEmpty() && !isSearching) {
                        Text("No repositories found. Make sure the OpenHands GitHub/GitLab app is installed for your account.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    results.forEach { repo ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(repo) }.padding(vertical = 8.dp)
                        ) {
                            Icon(
                                if (repo.isPublic) Icons.Default.Public else Icons.Default.Lock,
                                contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(repo.fullName, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    listOfNotNull(repo.provider, repo.mainBranch?.let { "default: $it" }, repo.stars?.let { "★ $it" }).joinToString(" · "),
                                    fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun BranchPickerDialog(
    branches: List<GitBranch>,
    isLoading: Boolean,
    onSearch: (String) -> Unit,
    onSelect: (GitBranch) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a branch") },
        text = {
            Column {
                OutlinedTextField(
                    value = query, onValueChange = { query = it; onSearch(it) }, singleLine = true,
                    placeholder = { Text("Filter branches") }, modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
                Column(modifier = Modifier.height(300.dp).verticalScroll(rememberScrollState())) {
                    branches.forEach { b ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(b) }.padding(vertical = 8.dp)
                        ) {
                            Text(b.name, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                            if (b.isProtected) Icon(Icons.Default.Lock, contentDescription = "protected", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun StartProgressDialog(progress: StartTaskProgress, onCancel: () -> Unit, onDismissError: () -> Unit) {
    val failed = progress as? StartTaskProgress.Failed
    AlertDialog(
        onDismissRequest = { if (failed != null) onDismissError() },
        title = { Text(if (failed != null) "Could not start" else "Starting conversation") },
        text = {
            Column {
                when (progress) {
                    is StartTaskProgress.Submitting -> {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Text("Submitting request…", fontSize = 13.sp)
                    }
                    is StartTaskProgress.InProgress -> {
                        val fraction = (progress.status.step.toFloat() / StartTaskStatus.MAX_STEP).coerceIn(0.05f, 0.95f)
                        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Text(progress.status.label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        if (!progress.detail.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(progress.detail, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Task ${progress.taskId.take(8)} · sandbox provisioning can take a minute", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is StartTaskProgress.Ready -> {
                        LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Text("Ready — opening conversation", fontSize = 13.sp)
                    }
                    is StartTaskProgress.Failed -> {
                        Text(progress.message, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            if (failed != null) TextButton(onClick = onDismissError) { Text("OK") }
            else TextButton(onClick = onCancel) { Text("Stop waiting") }
        }
    )
}
