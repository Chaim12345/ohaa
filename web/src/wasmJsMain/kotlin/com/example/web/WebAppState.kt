package com.example.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.data.api.OpenHandsApiClient
import com.example.data.api.toAgentEvent
import com.example.data.model.AgentEvent
import com.example.data.model.BranchDto
import com.example.data.model.ConversationUiStatus
import com.example.data.model.GitDiffDto
import com.example.data.model.LlmProfileDto
import com.example.data.model.OpenHandsConversation
import com.example.data.model.PromptTemplate
import com.example.data.model.RepositoryDto
import com.example.data.model.SecretDto
import com.example.data.model.SuggestedTaskDto
import com.example.ui.viewmodel.SandboxUiState
import com.example.data.model.UserInfoDto
import com.example.data.settings.AgentProfile
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.LaunchStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Application state for the web client.
 *
 * Mirrors the Android ViewModel's observable surface but holds no Android
 * dependency. Every value here is populated from the real API via
 * [OpenHandsApiClient]; nothing is seeded with sample data.
 */
class WebAppState(
    val api: OpenHandsApiClient,
    private val scope: CoroutineScope
) {
    var tab by mutableStateOf(AppTab.DASHBOARD)
    var conversations by mutableStateOf<List<OpenHandsConversation>>(emptyList())
    var templates by mutableStateOf<List<PromptTemplate>>(emptyList())
    var suggestedTasks by mutableStateOf<List<SuggestedTaskDto>>(emptyList())
    var account by mutableStateOf<UserInfoDto?>(null)

    var isRefreshing by mutableStateOf(false)
    var connectionStatus by mutableStateOf<String?>(null)
    var connectionHealthy by mutableStateOf<Boolean?>(null)

    var searchQuery by mutableStateOf("")
    var repoText by mutableStateOf("")
    var promptText by mutableStateOf("")
    var repoResults by mutableStateOf<List<RepositoryDto>>(emptyList())
    var isSearchingRepos by mutableStateOf(false)

    var branchResults by mutableStateOf<List<BranchDto>>(emptyList())
    var selectedBranch by mutableStateOf("")
    var isSearchingBranches by mutableStateOf(false)

    var launchStage by mutableStateOf(LaunchStage.IDLE)
    var launchStatusText by mutableStateOf<String?>(null)
    var selectedConversationId by mutableStateOf<String?>(null)
    var statusFilter by mutableStateOf<ConversationUiStatus?>(null)
    var activeProfile by mutableStateOf(AgentProfile())
    var credits by mutableStateOf<Double?>(null)

    // Settings: cloud profiles and secrets, loaded from the live API.
    var cloudProfiles by mutableStateOf<List<LlmProfileDto>>(emptyList())
    var secrets by mutableStateOf<List<SecretDto>>(emptyList())
    var isCloudSettingsLoading by mutableStateOf(false)

    /** Transcript of [selectedConversationId], refreshed from the events API. */
    var events by mutableStateOf<List<AgentEvent>>(emptyList())
    var isSending by mutableStateOf(false)

    var gitDiff by mutableStateOf<GitDiffDto?>(null)
    var isGitLoading by mutableStateOf(false)
    var files by mutableStateOf<List<String>>(emptyList())
    var isFilesLoading by mutableStateOf(false)
    var fileContents by mutableStateOf<Map<String, String>>(emptyMap())
    var skills by mutableStateOf<List<String>>(emptyList())

    val selectedConversation: OpenHandsConversation?
        get() = conversations.firstOrNull { it.id == selectedConversationId }

    /** Surfaces any failed call so the UI can show a real error rather than a spinner. */
    var lastError by mutableStateOf<String?>(null)

    private fun <T> onResult(result: Result<T>, onSuccess: (T) -> Unit) {
        result.fold(
            onSuccess = { lastError = null; onSuccess(it) },
            onFailure = { lastError = it.message ?: it::class.simpleName ?: "Request failed" }
        )
    }

    fun refresh() {
        scope.launch {
            isRefreshing = true
            onResult(api.checkHealth()) {
                connectionHealthy = true
                connectionStatus = it
            }
            onResult(api.listConversations()) { conversations = it }
            onResult(api.getCredits()) { credits = it.credits }
            onResult(api.getUserInfo()) { account = it }
            isRefreshing = false
            refreshCloudSettings()
        }
    }

    fun loadTemplates() {
        scope.launch {
            onResult(api.templates()) { templates = it }
            onResult(api.suggestedTasks()) { suggestedTasks = it }
        }
    }

    /** Cloud profiles and secrets backing the Settings screen. */
    fun refreshCloudSettings() {
        scope.launch {
            isCloudSettingsLoading = true
            onResult(api.listProfiles()) { cloudProfiles = it }
            onResult(api.listSecrets()) { secrets = it }
            isCloudSettingsLoading = false
        }
    }

    fun search(query: String) {
        searchQuery = query
        scope.launch {
            if (query.isBlank()) {
                onResult(api.listConversations()) { conversations = it }
            } else {
                onResult(api.searchConversations(query)) { conversations = it }
            }
        }
    }

    fun filterByStatus(status: ConversationUiStatus?) {
        statusFilter = if (statusFilter == status) null else status
    }

    fun deleteConversation(id: String) {
        scope.launch {
            onResult(api.deleteConversation(id)) {
                if (selectedConversationId == id) closeConversation()
                refresh()
            }
        }
    }

    fun searchRepositories(query: String) {
        repoText = query
        if (query.isBlank()) {
            repoResults = emptyList()
            return
        }
        scope.launch {
            isSearchingRepos = true
            onResult(api.searchRepositories(query)) { repoResults = it }
            isSearchingRepos = false
        }
    }

    /** Branches for the selected repo; empty repo ⇒ empty branch list. */
    fun searchBranches(query: String) {
        val repository = repoText.trim()
        if (repository.isEmpty()) {
            branchResults = emptyList()
            return
        }
        scope.launch {
            isSearchingBranches = true
            onResult(api.searchBranches(repository, query.ifBlank { null })) {
                branchResults = it
            }
            isSearchingBranches = false
        }
    }

    fun launchTask() {
        val prompt = promptText.trim()
        if (prompt.isEmpty()) {
            lastError = "Enter a task description first"
            return
        }
        scope.launch {
            launchStage = LaunchStage.SUBMITTING
            launchStatusText = null
            api.startConversation(
                prompt = prompt,
                repository = repoText.trim().ifEmpty { null },
                branch = selectedBranch.trim().ifEmpty { null },
                llmModel = activeProfile.model
            ).fold(
                onSuccess = { task ->
                    launchStage = LaunchStage.WAITING_SANDBOX
                    pollStartTask(task.id.orEmpty())
                },
                onFailure = {
                    launchStage = LaunchStage.FAILED
                    launchStatusText = it.message ?: "Could not start the task"
                }
            )
        }
    }

    private suspend fun pollStartTask(startTaskId: String) {
        if (startTaskId.isBlank()) {
            launchStage = LaunchStage.FAILED
            launchStatusText = "The server did not return a start-task id"
            return
        }
        // The sandbox takes tens of seconds to come up; poll until it reports an
        // app conversation id rather than blocking the UI.
        repeat(60) {
            val task = api.getStartTask(startTaskId).getOrElse { error ->
                launchStage = LaunchStage.FAILED
                launchStatusText = error.message ?: "Start task lookup failed"
                return
            }
            val conversationId = task.appConversationId
            if (!conversationId.isNullOrBlank()) {
                launchStage = LaunchStage.READY
                launchStatusText = "Sandbox ready"
                promptText = ""
                selectedConversationId = conversationId
                tab = AppTab.CONVERSATIONS
                refresh()
                return
            }
            launchStatusText = task.detail ?: "Waiting for sandbox…"
            delay(2_000)
        }
        launchStage = LaunchStage.FAILED
        launchStatusText = "Timed out waiting for the sandbox"
    }

    fun openConversation(id: String) {
        selectedConversationId = id
        refreshEvents()
    }

    fun refreshEvents() {
        val id = selectedConversationId ?: return
        scope.launch {
            onResult(api.searchEvents(id)) { list ->
                events = list.mapNotNull { it.toAgentEvent(id) }
            }
        }
    }

    fun sendMessage(text: String) {
        val id = selectedConversationId ?: return
        if (text.isBlank()) return
        scope.launch {
            isSending = true
            onResult(api.sendMessage(id, text)) { refreshEvents() }
            isSending = false
        }
    }

    fun closeConversation() {
        selectedConversationId = null
        events = emptyList()
        gitDiff = null
        files = emptyList()
        fileContents = emptyMap()
        skills = emptyList()
    }

    fun loadGitChanges() {
        val id = selectedConversationId ?: return
        if (isGitLoading) return
        scope.launch {
            isGitLoading = true
            onResult(api.gitChanges(id)) { gitDiff = it }
            isGitLoading = false
        }
    }

    fun loadFiles() {
        val id = selectedConversationId ?: return
        scope.launch {
            isFilesLoading = true
            onResult(api.listFiles(id)) { files = it }
            isFilesLoading = false
        }
    }

    fun readFile(path: String) {
        val id = selectedConversationId ?: return
        scope.launch {
            onResult(api.readFile(id, path)) { fileContents = fileContents + (path to it) }
        }
    }

    fun loadSkills() {
        val id = selectedConversationId ?: return
        scope.launch {
            onResult(api.conversationSkills(id)) { skills = it.map { skill -> skill.name } }
        }
    }

    fun pauseSandbox(sandboxId: String) {
        scope.launch { onResult(api.pauseSandbox(sandboxId)) { refresh() } }
    }

    fun resumeSandbox(sandboxId: String) {
        scope.launch { onResult(api.resumeSandbox(sandboxId)) { refresh() } }
    }

    fun deleteSandbox(sandboxId: String) {
        scope.launch { onResult(api.deleteSandbox(sandboxId)) { refresh() } }
    }

    fun selectTemplate(template: PromptTemplate) {
        promptText = template.prompt
        template.defaultRepo?.let { repoText = it }
        tab = AppTab.NEW_TASK
    }

    fun applySuggestedTask(task: SuggestedTaskDto) {
        promptText = buildString {
            append(task.title.ifBlank { "Investigate this task" })
            if (task.issueNumber > 0) {
                append("\n\nIssue #").append(task.issueNumber)
                if (task.repo.isNotBlank()) append(" in ").append(task.repo)
            }
        }
        if (task.repo.isNotBlank()) repoText = task.repo
        tab = AppTab.NEW_TASK
    }
}

@Composable
fun rememberWebAppState(api: OpenHandsApiClient): WebAppState {
    val scope = rememberCoroutineScope()
    return remember(api) { WebAppState(api, scope) }
}
