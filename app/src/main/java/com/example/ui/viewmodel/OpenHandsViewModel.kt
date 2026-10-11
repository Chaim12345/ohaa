package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ApiClient
import com.example.data.api.ApiSettings
import com.example.data.local.AppDatabase
import com.example.data.model.AgentEvent
import com.example.data.model.AgentProfileDto
import com.example.data.model.BranchDto
import com.example.data.model.CloudCredits
import com.example.data.model.ConversationUiStatus
import com.example.data.model.GitDiffDto
import com.example.data.model.LlmProfileDto
import com.example.data.model.OpenHandsConversation
import com.example.data.model.PromptTemplate
import com.example.data.model.RepositoryDto
import com.example.data.model.SecretDto
import com.example.data.model.StartTaskDto
import com.example.data.model.SuggestedTaskDto
import com.example.data.model.UserInfoDto
import com.example.data.repository.CloudError
import com.example.data.repository.OpenHandsRepository
import com.example.data.settings.AgentProfile
import com.example.data.settings.AgentProfileStore
import com.example.data.settings.OPENHANDS_PROVIDER
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppTab(val label: String) {
    DASHBOARD("Dashboard"),
    CONVERSATIONS("Conversations"),
    NEW_TASK("New Task"),
    SETTINGS("Settings")
}

/** A typed route stack replaces the old "tab + selected id" tangle. */
sealed class Route {
    data object Dashboard : Route()
    data object Conversations : Route()
    data object NewTask : Route()
    data object Settings : Route()
    data class ConversationDetail(val conversationId: String) : Route()
}

data class OpenHandsUiState(
    val currentTab: AppTab = AppTab.DASHBOARD,
    val detailConversationId: String? = null,
    val searchQuery: String = "",
    val statusFilter: ConversationUiStatus? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSending: Boolean = false,
    val isOnline: Boolean = false,
    val offlineMessage: String? = null,
    val isDemoMode: Boolean = false,
    val notification: String? = null,
    // New task
    val newTaskPrompt: String = "",
    val newTaskRepo: String = "",
    val isLaunching: Boolean = false,
    val launchStatusText: String? = null,
    val launchStage: LaunchStage = LaunchStage.IDLE,
    val suggestedTasks: List<SuggestedTaskDto> = emptyList(),
    val isLoadingSuggested: Boolean = false,
    // Live model catalogue (fetched from the cloud, not hardcoded)
    val availableModels: List<String> = emptyList(),
    val isLoadingModels: Boolean = false,
    // Settings
    val apiKeyInput: String = "",
    val baseUrlInput: String = ApiSettings.DEFAULT_BASE_URL,
    val healthMessage: String? = null,
    val healthOk: Boolean? = null,
    val credits: Double? = null,
    // Cloud account / profiles / secrets (Settings, live API key required)
    val account: UserInfoDto? = null,
    val cloudProfiles: List<LlmProfileDto> = emptyList(),
    val activeCloudProfile: String? = null,
    val secrets: List<SecretDto> = emptyList(),
    val isCloudSettingsLoading: Boolean = false,
    // Agent & LLM — the OpenHands agent with a chooseable free model.
    val activeProfile: AgentProfile = AgentProfile(),
    // Git integration — repos from the configured upstream provider
    val repoSearchResults: List<RepositoryDto> = emptyList(),
    val repoBranchResults: List<BranchDto> = emptyList(),
    val selectedBranch: String = "",
    val isSearchingRepos: Boolean = false,
    val gitDiff: GitDiffDto? = null,
    val isGitLoading: Boolean = false,
    // Sandbox / workspace inspection for the open conversation
    val sandboxStatus: SandboxUiState = SandboxUiState(),
    val files: List<String> = emptyList(),
    val isFilesLoading: Boolean = false,
    val fileContents: Map<String, String> = emptyMap(),
    val skills: List<String> = emptyList()
)

/** Sandbox action progress, so the UI can disable controls while a call is in flight. */
data class SandboxUiState(
    val isWorking: Boolean = false,
    val message: String? = null
)

enum class LaunchStage(val label: String) {
    IDLE(""),
    SUBMITTING("Submitting task…"),
    WAITING_SANDBOX("Waiting for sandbox…"),
    PREPARING_REPO("Preparing repository…"),
    SETTING_UP_SKILLS("Setting up skills…"),
    READY("Ready"),
    FAILED("Failed")
}

class OpenHandsViewModel(application: Application) :
    AndroidViewModel(application) {

    // Built from the Application handed to us by the default ViewModelProvider
    // factory (AndroidViewModel constructor) — no custom factory needed at the
    // call site, and it can never crash with "Cannot create an instance".
    private val repository: OpenHandsRepository = OpenHandsRepository(
        settings = ApiSettings(application),
        database = AppDatabase.getInstance(application),
        apiProvider = {
            val settings = ApiSettings(application)
            ApiClient.forConfig({ settings.baseUrl }, { settings.apiKey })
        }
    )

    /** Persists the default agent/LLM profile plus per-conversation overrides. */
    private val profileStore: AgentProfileStore = AgentProfileStore(application)

    private val _uiState = MutableStateFlow(
        OpenHandsUiState(
            apiKeyInput = "",
            isDemoMode = !repository.isConfigured(),
            activeProfile = profileStore.defaultProfile()
        )
    )
    val uiState: StateFlow<OpenHandsUiState> = _uiState.asStateFlow()

    val conversations: StateFlow<List<OpenHandsConversation>> =
        repository.conversationsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val templates: StateFlow<List<PromptTemplate>> =
        repository.templatesFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _detailEvents = MutableStateFlow<List<AgentEvent>>(emptyList())
    val detailEvents: StateFlow<List<AgentEvent>> = _detailEvents.asStateFlow()

    private var detailPollingJob: Job? = null
    private var detailObservationJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                repository.seedLocalData()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    notification = "Local data unavailable: ${e.message ?: "storage error"}"
                )
            }
            _uiState.value = _uiState.value.copy(isDemoMode = !repository.isConfigured())
            // Reflect the connection state immediately instead of leaving the
            // dashboard in a blank "Not connected" limbo.
            refreshConversations(showFeedback = false)
            if (repository.isConfigured()) {
                refreshCloudSettings()
                loadModels()
            }
        }
    }

    // ---- Navigation -----------------------------------------------------------

    fun setTab(tab: AppTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    /** Base URL for "open in web app" links, matching the configured server. */
    fun webBaseUrl(): String = repository.baseUrl()

    fun openDetail(conversationId: String) {
        _uiState.value = _uiState.value.copy(
            detailConversationId = conversationId,
            // Surface this conversation's agent/LLM (override or the default).
            activeProfile = profileStore.profileFor(conversationId)
        )
        startDetailObservation(conversationId)
    }

    fun closeDetail() {
        detailPollingJob?.cancel()
        detailObservationJob?.cancel()
        _detailEvents.value = emptyList()
        _uiState.value = _uiState.value.copy(
            detailConversationId = null,
            activeProfile = profileStore.defaultProfile()
        )
    }

    // ---- Dashboard / list -------------------------------------------------------

    fun refreshConversations(showFeedback: Boolean = true) {
        viewModelScope.launch {
            if (!repository.isConfigured()) {
                _uiState.value = _uiState.value.copy(
                    isOnline = false,
                    offlineMessage = "Not connected — add your OpenHands Cloud API key in Settings.",
                    isRefreshing = false
                )
                return@launch
            }
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            repository.refreshConversations()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        isOnline = true,
                        offlineMessage = null,
                        notification = if (showFeedback) "Synced with OpenHands Cloud" else null
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isRefreshing = false,
                        isOnline = false,
                        offlineMessage = err.toUserMessage()
                    )
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setStatusFilter(status: ConversationUiStatus?) {
        _uiState.value = _uiState.value.copy(statusFilter = status)
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
                .onSuccess {
                    if (_uiState.value.detailConversationId == conversationId) closeDetail()
                    _uiState.value = _uiState.value.copy(notification = "Conversation deleted")
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(notification = err.toUserMessage())
                }
        }
    }

    // ---- New task flow ------------------------------------------------------------

    fun updateNewTaskPrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(newTaskPrompt = prompt)
    }

    fun updateNewTaskRepo(repo: String) {
        _uiState.value = _uiState.value.copy(newTaskRepo = repo)
    }

    fun applyTemplate(template: PromptTemplate) {
        _uiState.value = _uiState.value.copy(
            newTaskPrompt = template.prompt,
            newTaskRepo = template.defaultRepo ?: _uiState.value.newTaskRepo,
            currentTab = AppTab.NEW_TASK
        )
    }

    /**
     * Launches a task following docs/API_SPEC.md: POST create → poll start-tasks
     * with stage feedback → open detail (which then polls execution status).
     */
    fun createNewTask() {
        val prompt = _uiState.value.newTaskPrompt.trim()
        if (prompt.isEmpty()) {
            _uiState.value = _uiState.value.copy(notification = "Describe the task first")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLaunching = true,
                launchStage = LaunchStage.SUBMITTING,
                launchStatusText = LaunchStage.SUBMITTING.label
            )
            val repo = _uiState.value.newTaskRepo.trim().ifEmpty { null }
            val profile = _uiState.value.activeProfile
            val llmModel = profile.model
            val agentType = "default"

            repository.createConversation(
                prompt,
                repo,
                branch = _uiState.value.selectedBranch.ifBlank { null },
                gitProvider = gitProviderLabel(repo),
                llmModel = llmModel,
                agentType = agentType
            )
                .onSuccess { conversationId ->
                    _uiState.value = _uiState.value.copy(
                        newTaskPrompt = "",
                        newTaskRepo = "",
                        launchStage = LaunchStage.WAITING_SANDBOX,
                        launchStatusText = LaunchStage.WAITING_SANDBOX.label
                    )
                    awaitStartTaskReady(conversationId)
                    _uiState.value = _uiState.value.copy(
                        isLaunching = false,
                        launchStage = LaunchStage.IDLE,
                        launchStatusText = null,
                        notification = "Task launched"
                    )
                    openDetail(conversationId)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isLaunching = false,
                        launchStage = LaunchStage.FAILED,
                        launchStatusText = null,
                        notification = err.toUserMessage()
                    )
                }
        }
    }

    private suspend fun awaitStartTaskReady(conversationId: String) {
        var attempts = 0
        while (attempts < 15 && kotlinx.coroutines.currentCoroutineContext().isActive) {
            delay(2_000)
            when (repository.pollStartTask(repository.startTaskIdFor(conversationId)).getOrNull()?.status?.uppercase()) {
                "READY" -> {
                    repository.refreshConversations()
                    return
                }
                "ERROR" -> {
                    _uiState.value = _uiState.value.copy(
                        notification = "Cloud reported an error starting this task"
                    )
                    return
                }
                "WAITING_FOR_SANDBOX" -> _uiState.value = _uiState.value.copy(
                    launchStatusText = LaunchStage.WAITING_SANDBOX.label
                )
                "PREPARING_REPOSITORY" -> _uiState.value = _uiState.value.copy(
                    launchStatusText = LaunchStage.PREPARING_REPO.label
                )
                "SETTING_UP_SKILLS" -> _uiState.value = _uiState.value.copy(
                    launchStatusText = LaunchStage.SETTING_UP_SKILLS.label
                )
            }
            attempts++
        }
    }

    // ---- Detail: events + polling ---------------------------------------------------

    fun refreshDetail() {
        val id = _uiState.value.detailConversationId ?: return
        viewModelScope.launch {
            repository.refreshEvents(id)
            repository.refreshConversations(showFeedback = false)
        }
    }

    fun sendFollowUp(message: String) {
        val id = _uiState.value.detailConversationId ?: return
        if (message.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            repository.sendMessage(id, message)
                .onSuccess { repository.refreshEvents(id) }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(notification = err.toUserMessage())
                }
            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }

    private fun startDetailObservation(conversationId: String) {
        detailPollingJob?.cancel()
        detailObservationJob?.cancel()
        detailObservationJob = viewModelScope.launch {
            repository.getEventsFlow(conversationId).collect { events ->
                _detailEvents.value = events
            }
        }
        detailPollingJob = viewModelScope.launch {
            // Initial fetch, then poll per docs/API_SPEC.md §4 while the detail is open.
            repository.refreshEvents(conversationId)
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                delay(10_000)
                repository.refreshEvents(conversationId)
                repository.refreshConversations(showFeedback = false)
            }
        }
    }

    // ---- Git integration (repos from upstream provider + sandbox changes) -----------

    /** Pulls repositories visible to the configured upstream git provider. */
    fun searchRepositories() {
        if (_uiState.value.isSearchingRepos) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearchingRepos = true)
            repository.searchRepositories(_uiState.value.newTaskRepo)
                .onSuccess { repos ->
                    _uiState.value = _uiState.value.copy(
                        repoSearchResults = repos,
                        isSearchingRepos = false
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isSearchingRepos = false,
                        notification = "Repositories: ${err.toUserMessage()}"
                    )
                }
        }
    }

    /** Picks a repository from the provider listing and loads its branches. */
    fun selectRepository(repo: RepositoryDto) {
        _uiState.value = _uiState.value.copy(
            newTaskRepo = repo.displayName,
            selectedBranch = repo.branch ?: "",
            repoBranchResults = emptyList()
        )
        viewModelScope.launch {
            repository.searchBranches(repo.displayName, repo.gitProvider ?: "github")
                .onSuccess { branches ->
                    _uiState.value = _uiState.value.copy(repoBranchResults = branches)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        repoBranchResults = emptyList(),
                        notification = "Branches: ${err.toUserMessage()}"
                    )
                }
        }
    }

    fun updateSelectedBranch(branch: String) {
        _uiState.value = _uiState.value.copy(selectedBranch = branch)
    }

    /** Pulls the working-tree git diff/changes for the open conversation. */
    fun refreshGitChanges() {
        val id = _uiState.value.detailConversationId ?: return
        if (_uiState.value.isGitLoading) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGitLoading = true)
            repository.gitChanges(id)
                .onSuccess { diff -> _uiState.value = _uiState.value.copy(gitDiff = diff, isGitLoading = false) }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isGitLoading = false,
                        notification = "Git: ${err.toUserMessage()}"
                    )
                }
        }
    }

    /** Best-effort upstream provider for a repo string (github by default). */
    private fun gitProviderLabel(repo: String?): String? {
        return repo?.let {
            when {
                it.startsWith("git@") -> it.substringAfter('@').substringBefore(':')
                it.contains("gitlab") -> "gitlab"
                it.contains("bitbucket") -> "bitbucket"
                else -> "github"
            }
        }
    }

    // ---- Sandbox control, workspace files, skills, export ------------------------

    private fun sandboxIdFor(conversationId: String): String? =
        conversations.value.find { it.id == conversationId }?.sandboxId

    /** Pauses the sandbox backing a conversation. */
    fun pauseSandbox(conversationId: String) {
        val sandboxId = sandboxIdFor(conversationId)
        if (sandboxId == null) {
            _uiState.value = _uiState.value.copy(notification = "No sandbox is attached to this conversation yet")
            return
        }
        runSandboxAction(conversationId, "Sandbox paused") { repository.pauseSandbox(sandboxId) }
    }

    /** Resumes a paused sandbox. */
    fun resumeSandbox(conversationId: String) {
        val sandboxId = sandboxIdFor(conversationId)
        if (sandboxId == null) {
            _uiState.value = _uiState.value.copy(notification = "No sandbox is attached to this conversation yet")
            return
        }
        runSandboxAction(conversationId, "Sandbox resumed") { repository.resumeSandbox(sandboxId) }
    }

    /** Deletes the sandbox (the conversation row stays). */
    fun deleteSandbox(conversationId: String) {
        val sandboxId = sandboxIdFor(conversationId)
        if (sandboxId == null) {
            _uiState.value = _uiState.value.copy(notification = "No sandbox is attached to this conversation yet")
            return
        }
        runSandboxAction(conversationId, "Sandbox deleted") { repository.deleteSandbox(sandboxId) }
    }

    private fun runSandboxAction(
        conversationId: String,
        successMessage: String,
        action: suspend () -> Result<Unit>
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(sandboxStatus = SandboxUiState(isWorking = true))
            action()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        sandboxStatus = SandboxUiState(isWorking = false),
                        notification = successMessage
                    )
                    repository.refreshConversations(showFeedback = false)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        sandboxStatus = SandboxUiState(isWorking = false),
                        notification = "Sandbox: ${err.toUserMessage()}"
                    )
                }
        }
    }

    /** Lists the workspace files for the open conversation's sandbox. */
    fun loadWorkspaceFiles(path: String? = null) {
        val id = _uiState.value.detailConversationId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFilesLoading = true)
            repository.listFiles(id, path)
                .onSuccess { files ->
                    _uiState.value = _uiState.value.copy(files = files, isFilesLoading = false)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isFilesLoading = false,
                        notification = "Files: ${err.toUserMessage()}"
                    )
                }
        }
    }

    /** Reads a workspace file and caches its contents for display. */
    fun readWorkspaceFile(path: String) {
        val id = _uiState.value.detailConversationId ?: return
        viewModelScope.launch {
            repository.readFile(id, path)
                .onSuccess { content ->
                    _uiState.value = _uiState.value.copy(
                        fileContents = _uiState.value.fileContents + (path to content.take(20_000))
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(notification = "File: ${err.toUserMessage()}")
                }
        }
    }

    /** Loads the skills active in the open conversation's sandbox. */
    fun loadConversationSkills() {
        val id = _uiState.value.detailConversationId ?: return
        viewModelScope.launch {
            repository.conversationSkills(id)
                .onSuccess { list ->
                    _uiState.value = _uiState.value.copy(skills = list.mapNotNull { it.name })
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(notification = "Skills: ${err.toUserMessage()}")
                }
        }
    }

    /** Fetches the trajectory JSON and hands it to the share sheet. */
    fun exportTrajectory(onReady: (String, String) -> Unit) {
        val id = _uiState.value.detailConversationId ?: return
        viewModelScope.launch {
            repository.downloadTrajectory(id)
                .onSuccess { json -> onReady("trajectory-$id.json", json) }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(notification = "Export: ${err.toUserMessage()}")
                }
        }
    }

    // ---- Live catalogues (models, suggested tasks) -------------------------------

    /** Fetches the free OpenHands-provider models so the picker offers real choices. */
    fun loadModels(query: String? = null) {
        if (!repository.isConfigured()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingModels = true)
            // Only the OpenHands provider is offered (`provider__eq`), and only
            // verified entries; these are the models the shared key can run.
            repository.searchModels(
                query = query,
                verifiedOnly = true,
                provider = OPENHANDS_PROVIDER,
                limit = 100
            )
                .onSuccess { models ->
                    val visible = models.filter { !it.hidden && it.name.isNotBlank() }
                    // Prefer free models; if none are flagged, keep the full set
                    // so the picker is never empty.
                    val freeModels = visible.filter { it.free }
                    val chosen = (freeModels.ifEmpty { visible })
                        .map {
                            val provider = it.provider.orEmpty()
                            if (provider.isBlank()) it.name else "$provider/${it.name}"
                        }
                        .distinct()
                        .sorted()
                    _uiState.value = _uiState.value.copy(
                        availableModels = chosen,
                        isLoadingModels = false
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingModels = false,
                        notification = "Models: ${err.toUserMessage()}"
                    )
                }
        }
    }

    /** Pulls suggested tasks (failing checks, open issues) for the New Task screen. */
    fun loadSuggestedTasks() {
        if (!repository.isConfigured() || _uiState.value.isLoadingSuggested) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingSuggested = true)
            repository.suggestedTasks()
                .onSuccess { tasks ->
                    _uiState.value = _uiState.value.copy(
                        suggestedTasks = tasks,
                        isLoadingSuggested = false
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingSuggested = false,
                        notification = "Suggested tasks: ${err.toUserMessage()}"
                    )
                }
        }
    }

    /** Seeds the composer from a suggested task and opens New Task. */
    fun applySuggestedTask(task: SuggestedTaskDto) {
        val prompt = buildString {
            append(task.title.ifBlank { "Investigate this task" })
            if (task.issueNumber > 0) {
                append("\n\nIssue #")
                append(task.issueNumber)
                if (task.repo.isNotBlank()) {
                    append(" in ")
                    append(task.repo)
                }
            }
        }
        _uiState.value = _uiState.value.copy(
            newTaskPrompt = prompt,
            newTaskRepo = task.repo.ifBlank { _uiState.value.newTaskRepo },
            currentTab = AppTab.NEW_TASK
        )
    }

    /** Server-side conversation search (title filter), with local fallback. */
    fun searchConversationsRemote(query: String) {
        if (!repository.isConfigured()) return
        viewModelScope.launch {
            repository.searchConversationsRemote(query)
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(notification = err.toUserMessage())
                }
        }
    }

    /** Renames a conversation on the server and refreshes the cached row. */
    fun renameConversation(conversationId: String, title: String) {
        if (title.isBlank()) {
            _uiState.value = _uiState.value.copy(notification = "Title cannot be empty")
            return
        }
        viewModelScope.launch {
            repository.renameConversation(conversationId, title.trim())
                .onSuccess {
                    _uiState.value = _uiState.value.copy(notification = "Conversation renamed")
                    repository.refreshConversations(showFeedback = false)
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(notification = err.toUserMessage())
                }
        }
    }

    // ---- Agent & LLM profile ------------------------------------------------------------

    /**
     * Updates the visible LLM selection. Inside an open conversation the change
     * is a **mid-session switch**: it is persisted as a per-conversation
     * override and takes effect for subsequent messages. Outside a
     * conversation (Settings / New Task) it updates the global default used
     * for new conversations.
     */
    private fun updateActiveProfile(transform: (AgentProfile) -> AgentProfile) {
        val updated = transform(_uiState.value.activeProfile)
        val conversationId = _uiState.value.detailConversationId
        if (conversationId != null) {
            profileStore.setProfileFor(conversationId, updated)
            _uiState.value = _uiState.value.copy(
                activeProfile = updated,
                notification = "Now using ${updated.shortLabel()} for this conversation"
            )
        } else {
            profileStore.saveDefaultProfile(updated)
            _uiState.value = _uiState.value.copy(activeProfile = updated)
        }
    }

    fun setModel(model: String) {
        if (model.isBlank()) return
        val conversationId = _uiState.value.detailConversationId
        updateActiveProfile { it.copy(model = model.trim()) }
        // Mid-session switch: push the model onto the running conversation via
        // a matching saved cloud profile (switch_profile), when one exists.
        if (conversationId != null) {
            viewModelScope.launch {
                repository.listProfiles().onSuccess { profiles ->
                    profiles.firstOrNull { it.model == model.trim() }?.name?.let { profileName ->
                        repository.switchProfile(conversationId, profileName).onFailure { err ->
                            _uiState.value = _uiState.value.copy(
                                notification = "Profile saved locally; cloud switch failed: ${err.toUserMessage()}"
                            )
                        }
                    }
                }
            }
        }
    }

    /** Drops this conversation's override; it falls back to the default profile. */
    fun resetConversationProfile() {
        val id = _uiState.value.detailConversationId ?: return
        profileStore.clearProfileFor(id)
        _uiState.value = _uiState.value.copy(
            activeProfile = profileStore.defaultProfile(),
            notification = "Reverted to default profile"
        )
    }

    // ---- Settings ---------------------------------------------------------------------

    fun updateApiKeyInput(key: String) {
        _uiState.value = _uiState.value.copy(apiKeyInput = key)
    }

    fun updateBaseUrlInput(url: String) {
        _uiState.value = _uiState.value.copy(baseUrlInput = url)
    }

    fun saveConfig() {
        val key = _uiState.value.apiKeyInput.trim()
        val url = _uiState.value.baseUrlInput.trim().ifEmpty { ApiSettings.DEFAULT_BASE_URL }
        // A blank field means "keep the stored key". The UI intentionally never
        // places the stored secret into the editable field.
        repository.updateConfig(
            newKey = key.ifEmpty { repository.storedApiKey() },
            newUrl = url
        )
        _uiState.value = _uiState.value.copy(
            isDemoMode = !repository.isConfigured(),
            notification = if (repository.isConfigured()) "API key saved" else "Running in demo mode"
        )
        testConnection()
    }

    fun clearApiKey() {
        repository.updateConfig("", _uiState.value.baseUrlInput)
        _uiState.value = _uiState.value.copy(
            apiKeyInput = "",
            isDemoMode = true,
            isOnline = false,
            healthOk = null,
            healthMessage = null,
            credits = null,
            account = null,
            cloudProfiles = emptyList(),
            activeCloudProfile = null,
            secrets = emptyList(),
            notification = "API key cleared — reconnect in Settings to sync"
        )
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                healthOk = null,
                healthMessage = "Testing connection…"
            )
            repository.checkHealth()
                .onSuccess { msg ->
                    val credits = repository.getCredits().getOrNull()?.credits
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        healthOk = true,
                        healthMessage = msg,
                        credits = credits,
                        isOnline = true,
                        offlineMessage = null
                    )
                    refreshCloudSettings()
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        healthOk = false,
                        healthMessage = err.toUserMessage()
                    )
                }
        }
    }

    // ---- Cloud account / profiles / secrets ----------------------------------

    /** Loads account, cloud profiles and secrets in one pass for the Settings screen. */
    fun refreshCloudSettings() {
        if (!repository.isConfigured()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCloudSettingsLoading = true)
            val account = repository.currentUser().getOrNull()
            val profiles = repository.profilesWithActive().getOrNull()
            val secrets = repository.searchSecrets().getOrNull().orEmpty()
            _uiState.value = _uiState.value.copy(
                isCloudSettingsLoading = false,
                account = account,
                cloudProfiles = profiles?.profiles.orEmpty(),
                activeCloudProfile = profiles?.activeProfile,
                secrets = secrets
            )
        }
    }

    fun activateCloudProfile(name: String) {
        viewModelScope.launch {
            repository.activateCloudProfile(name)
                .onSuccess { msg ->
                    val profiles = repository.profilesWithActive().getOrNull()
                    _uiState.value = _uiState.value.copy(
                        notification = msg,
                        cloudProfiles = profiles?.profiles.orEmpty(),
                        activeCloudProfile = profiles?.activeProfile ?: name
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(notification = err.toUserMessage())
                }
        }
    }

    fun deleteCloudProfile(name: String) {
        viewModelScope.launch {
            repository.deleteCloudProfile(name)
                .onSuccess { msg ->
                    val profiles = repository.profilesWithActive().getOrNull()
                    _uiState.value = _uiState.value.copy(
                        notification = msg,
                        cloudProfiles = profiles?.profiles.orEmpty(),
                        activeCloudProfile = profiles?.activeProfile?.takeIf { it != name }
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(notification = err.toUserMessage())
                }
        }
    }

    fun renameCloudProfile(name: String, newName: String) {
        if (newName.isBlank()) {
            _uiState.value = _uiState.value.copy(notification = "Profile name cannot be empty")
            return
        }
        viewModelScope.launch {
            repository.renameCloudProfile(name, newName.trim())
                .onSuccess { msg ->
                    val profiles = repository.profilesWithActive().getOrNull()
                    _uiState.value = _uiState.value.copy(
                        notification = msg,
                        cloudProfiles = profiles?.profiles.orEmpty(),
                        activeCloudProfile = if (_uiState.value.activeCloudProfile == name)
                            profiles?.activeProfile ?: newName else _uiState.value.activeCloudProfile
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(notification = err.toUserMessage())
                }
        }
    }

    fun addSecret(name: String, value: String, description: String?) {
        if (name.isBlank() || value.isBlank()) {
            _uiState.value = _uiState.value.copy(notification = "Secret needs both a name and a value")
            return
        }
        viewModelScope.launch {
            repository.createSecret(name.trim(), value, description?.trim()?.takeIf { it.isNotBlank() })
                .onSuccess { body ->
                    _uiState.value = _uiState.value.copy(
                        notification = body.message.ifBlank { "Secret '$name' created" },
                        secrets = repository.searchSecrets().getOrNull().orEmpty()
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(notification = err.toUserMessage())
                }
        }
    }

    fun deleteSecret(name: String) {
        viewModelScope.launch {
            repository.deleteSecret(name)
                .onSuccess { body ->
                    _uiState.value = _uiState.value.copy(
                        notification = body.message.ifBlank { "Secret '$name' deleted" },
                        secrets = _uiState.value.secrets.filterNot { it.name == name }
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(notification = err.toUserMessage())
                }
        }
    }

    fun clearLocalData() {
        viewModelScope.launch {
            repository.clearCache()
            _uiState.value = _uiState.value.copy(
                notification = "Cached conversations and transcripts cleared"
            )
        }
    }

    fun dismissNotification() {
        _uiState.value = _uiState.value.copy(notification = null)
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is CloudError.Unauthorized -> message ?: "Unauthorized"
        is CloudError.Api -> message ?: "API error"
        is CloudError.Network -> message ?: "Network error"
        is CloudError.Parse -> message ?: "Unexpected response"
        else -> message ?: "Unexpected error"
    }
}
