package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.ApiException
import com.example.data.local.AppDatabase
import com.example.data.model.AccountInfo
import com.example.data.model.AgentEvent
import com.example.data.model.Conversation
import com.example.data.model.ConversationStatus
import com.example.data.model.CustomSecret
import com.example.data.model.LlmProfile
import com.example.data.model.Skill
import com.example.data.model.GitBranch
import com.example.data.model.GitChange
import com.example.data.model.GitDiff
import com.example.data.model.GitRepository
import com.example.data.model.PromptTemplate
import com.example.data.model.Sandbox
import com.example.data.model.SandboxStatus
import com.example.data.model.StartTaskProgress
import com.example.data.model.SuggestedTask
import com.example.data.repository.OpenHandsRepository
import com.example.data.repository.StartConversationParams
import com.example.data.repository.friendlyMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import com.example.data.model.LlmModel
import kotlinx.coroutines.launch


/** Theme preference persisted in the app; mirrors webui appearance choices. */
enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }
enum class HomeTab(val label: String) {
    HOME("Home"),
    CONVERSATIONS("Conversations"),
    NEW("New"),
    SETTINGS("Settings")
}

/** Screens layered above the tab bar. Back pops the top one. */
sealed class Screen {
    data class ConversationDetail(val conversationId: String) : Screen()
    data class Files(val conversationId: String) : Screen()
    data class Changes(val conversationId: String) : Screen()
    data object Sandboxes : Screen()
    data object Secrets : Screen()
}

data class NewConversationForm(
    val prompt: String = "",
    val repository: String = "",
    val branch: String = "",
    val gitProvider: String? = null,
    val title: String = "",
    val agentType: String = "default",
    val llmModel: String = "",
    val showAdvanced: Boolean = false,
    val spanName: String = "",
    val tags: String = "",
    /** Selected LLM profile for the new conversation; llmModel override wins when non-blank. */
    val llmProfile: String? = null,
    val repoQuery: String = "",
    val repoProvider: String = "github",
    val repoResults: List<GitRepository> = emptyList(),
    val isSearchingRepos: Boolean = false,
    val repoNextPageId: String? = null,
    val isLoadingMoreRepos: Boolean = false,
    val repoOrganizations: List<String> = emptyList(),
    val selectedOrganization: String? = null,
    val branchResults: List<GitBranch> = emptyList(),
    val isLoadingBranches: Boolean = false,
    val suggestedTasks: List<SuggestedTask> = emptyList()
)

data class WorkspaceState(
    val path: String? = null,
    val entries: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val openFilePath: String? = null,
    val openFileContent: String? = null,
    val changes: List<GitChange> = emptyList(),
    val repoRoot: String = "/workspace",
    val selectedDiff: GitDiff? = null,
    val error: String? = null
)

data class OpenHandsUiState(
    val currentTab: HomeTab = HomeTab.HOME,
    val screenStack: List<Screen> = emptyList(),

    // conversations list
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreConversations: Boolean = false,
    val searchQuery: String = "",
    val statusFilter: ConversationStatus? = null,
    val listError: String? = null,
    val lastSyncedAt: Long? = null,

    // new conversation
    val form: NewConversationForm = NewConversationForm(),
    val startProgress: StartTaskProgress? = null,

    // detail
    val isSyncingEvents: Boolean = false,
    val isSendingMessage: Boolean = false,
    val showNoiseEvents: Boolean = false,
    val detailError: String? = null,
    val isMutatingConversation: Boolean = false,
    val workspace: WorkspaceState = WorkspaceState(),

    // sandboxes
    val sandboxes: List<Sandbox> = emptyList(),
    val isLoadingSandboxes: Boolean = false,

    // secrets / profiles / skills
    val secrets: List<CustomSecret> = emptyList(),
    val isLoadingSecrets: Boolean = false,
    val llmProfiles: List<LlmProfile> = emptyList(),
    val conversationSkills: List<Skill>? = null,

    // account / settings
    val apiKeyInput: String = "",
    val baseUrlInput: String = "",
    val isUsingBuildTimeKey: Boolean = false,
    /** Git author used for commits made by the agent; mirrors webui Application settings. */
    val gitUserNameInput: String = "",
    val gitUserEmailInput: String = "",
    /** Per-conversation spending cap in USD; blank = account default. */
    val maxBudgetInput: String = "",
    /** App theme selection. */
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Cached model catalog from GET /config/models/search for the model dropdown. */
    val modelCatalog: List<LlmModel> = emptyList(),
    val isLoadingModels: Boolean = false,
    val account: AccountInfo? = null,
    val accountError: String? = null,
    val isTestingConnection: Boolean = false,
    val needsApiKey: Boolean = false,

    val notification: String? = null
) {
    val topScreen: Screen? get() = screenStack.lastOrNull()
    val selectedConversationId: String?
        get() = when (val s = topScreen) {
            is Screen.ConversationDetail -> s.conversationId
            is Screen.Files -> s.conversationId
            is Screen.Changes -> s.conversationId
            else -> null
        }
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class OpenHandsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OpenHandsRepository(application, AppDatabase.getInstance(application))

    private val _uiState = MutableStateFlow(
        OpenHandsUiState(
            apiKeyInput = repository.apiKey,
            baseUrlInput = repository.baseUrl,
            isUsingBuildTimeKey = repository.isUsingBuildTimeKey,
            showNoiseEvents = repository.showNoiseEvents,
            themeMode = runCatching { ThemeMode.valueOf(repository.themeMode) }.getOrDefault(ThemeMode.SYSTEM),
            needsApiKey = repository.apiKey.isBlank()
        )
    )
    val uiState: StateFlow<OpenHandsUiState> = _uiState.asStateFlow()

    val conversations: StateFlow<List<Conversation>> = repository.conversationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val templates: StateFlow<List<PromptTemplate>> = repository.templatesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Conversation currently open in the detail screen (live from Room). */
    val selectedConversation: StateFlow<Conversation?> = _uiState
        .map { it.selectedConversationId }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(null) else repository.conversationFlow(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Events for the open conversation, filtered by the noise toggle. */
    val events: StateFlow<List<AgentEvent>> = _uiState
        .map { it.selectedConversationId to it.showNoiseEvents }
        .distinctUntilChanged()
        .flatMapLatest { (id, showNoise) ->
            if (id == null) flowOf(emptyList())
            else repository.eventsFlow(id).map { list ->
                val completedCalls = list.filter { !it.isPendingToolCall }.mapNotNull { it.toolCallId }.toSet()
                list.filterNot { e ->
                    (!showNoise && e.isNoise) || (e.isPendingToolCall && e.toolCallId in completedCalls)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var detailPollJob: Job? = null
    private var listPollJob: Job? = null
    private var startJob: Job? = null
    private val repoSearchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch { repository.seedTemplatesIfEmpty() }
        viewModelScope.launch {
            repoSearchQuery.debounce(350).distinctUntilChanged().collect { q -> runRepoSearch(q) }
        }
        _uiState.update {
            it.copy(
                gitUserNameInput = repository.gitUserName ?: "",
                gitUserEmailInput = repository.gitUserEmail ?: "",
                maxBudgetInput = repository.maxBudgetPerTask?.toString() ?: ""
            )
        }
        if (repository.apiKey.isNotBlank()) {
            refreshConversations()
            loadAccount()
            viewModelScope.launch { loadModelCatalog() }
            loadRepoOrganizations()
            viewModelScope.launch { runRepoSearch("") }
        }
        startListPolling()
    }

    // ---------------------------------------------------------------- navigation

    fun setTab(tab: HomeTab) {
        _uiState.update { it.copy(currentTab = tab, screenStack = emptyList()) }
        if (tab == HomeTab.NEW && repository.apiKey.isNotBlank() && _uiState.value.form.repoResults.isEmpty()) {
            loadRepoOrganizations()
            viewModelScope.launch { runRepoSearch(_uiState.value.form.repoQuery) }
        }
    }

    fun openConversation(id: String) {
        _uiState.update { it.copy(screenStack = listOf(Screen.ConversationDetail(id)), detailError = null) }
        startDetailPolling(id)
    }

    fun openFiles(conversationId: String) {
        _uiState.update { it.copy(screenStack = it.screenStack + Screen.Files(conversationId)) }
        browseFiles(conversationId, null)
    }

    /** Opens the Files screen directly on a file (web file-chip "View" action). */
    fun openFileInFiles(conversationId: String, path: String) {
        _uiState.update { it.copy(screenStack = it.screenStack + Screen.Files(conversationId)) }
        openFile(conversationId, path)
    }

    fun openChanges(conversationId: String) {
        viewModelScope.launch {
            val root = repository.conversationFlow(conversationId).firstOrNull()?.defaultRepoRoot
            _uiState.update {
                it.copy(
                    screenStack = it.screenStack + Screen.Changes(conversationId),
                    workspace = it.workspace.copy(repoRoot = root ?: it.workspace.repoRoot)
                )
            }
            loadChanges(conversationId)
        }
    }

    fun openSandboxes() {
        _uiState.update { it.copy(screenStack = it.screenStack + Screen.Sandboxes) }
        loadSandboxes()
    }

    fun openSecrets() {
        _uiState.update { it.copy(screenStack = it.screenStack + Screen.Secrets) }
        loadSecrets()
    }

    /** Returns false when there was nothing to pop (caller should let the system handle back). */
    fun goBack(): Boolean {
        val stack = _uiState.value.screenStack
        if (stack.isEmpty()) return false
        val remaining = stack.dropLast(1)
        _uiState.update {
            it.copy(
                screenStack = remaining,
                workspace = if (remaining.none { s -> s is Screen.Files || s is Screen.Changes }) WorkspaceState() else it.workspace
            )
        }
        if (remaining.none { it is Screen.ConversationDetail }) {
            detailPollJob?.cancel()
        }
        return true
    }

    fun dismissNotification() = _uiState.update { it.copy(notification = null) }

    private fun notify(message: String) = _uiState.update { it.copy(notification = message) }

    // --------------------------------------------------------------- conversations

    fun refreshConversations() {
        if (repository.apiKey.isBlank()) {
            _uiState.update { it.copy(needsApiKey = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, listError = null) }
            val query = _uiState.value.searchQuery.takeIf { it.isNotBlank() }
            repository.refreshConversations(query)
                .onSuccess { hasMore ->
                    _uiState.update {
                        it.copy(isRefreshing = false, hasMoreConversations = hasMore, lastSyncedAt = System.currentTimeMillis(), needsApiKey = false)
                    }
                }
                .onFailure { e -> handleListFailure(e) }
        }
    }

    fun loadMoreConversations() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMoreConversations) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            repository.loadMoreConversations()
                .onSuccess { hasMore -> _uiState.update { it.copy(isLoadingMore = false, hasMoreConversations = hasMore) } }
                .onFailure { e -> _uiState.update { it.copy(isLoadingMore = false) }; notify(e.friendlyMessage()) }
        }
    }

    private var searchJob: Job? = null

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            refreshConversations()
        }
    }

    fun setStatusFilter(status: ConversationStatus?) = _uiState.update { it.copy(statusFilter = status) }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutatingConversation = true) }
            repository.deleteConversation(id)
                .onSuccess {
                    if (_uiState.value.selectedConversationId == id) {
                        detailPollJob?.cancel()
                        _uiState.update { it.copy(screenStack = emptyList()) }
                    }
                    notify("Conversation deleted")
                }
                .onFailure { notify(it.friendlyMessage()) }
            _uiState.update { it.copy(isMutatingConversation = false) }
        }
    }

    fun renameConversation(id: String, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isMutatingConversation = true) }
            repository.renameConversation(id, title)
                .onSuccess { notify("Renamed") }
                .onFailure { notify(it.friendlyMessage()) }
            _uiState.update { it.copy(isMutatingConversation = false) }
        }
    }

    private fun handleListFailure(e: Throwable) {
        val authProblem = e is ApiException && e.isAuthError
        _uiState.update { it.copy(isRefreshing = false, listError = e.friendlyMessage(), needsApiKey = authProblem) }
    }

    private fun startListPolling() {
        listPollJob?.cancel()
        listPollJob = viewModelScope.launch {
            while (true) {
                delay(30_000)
                val s = _uiState.value
                val anyActive = conversations.value.any { it.status.isActive }
                if (s.screenStack.isEmpty() && s.currentTab != HomeTab.SETTINGS && anyActive && repository.apiKey.isNotBlank()) {
                    repository.refreshConversations(s.searchQuery.takeIf { it.isNotBlank() })
                }
            }
        }
    }

    // ------------------------------------------------------------- new conversation

    fun updateForm(transform: (NewConversationForm) -> NewConversationForm) =
        _uiState.update { it.copy(form = transform(it.form)) }

    fun applyTemplate(template: PromptTemplate) {
        _uiState.update {
            it.copy(
                currentTab = HomeTab.NEW,
                screenStack = emptyList(),
                form = it.form.copy(
                    prompt = template.prompt,
                    repository = template.defaultRepo ?: it.form.repository
                )
            )
        }
    }

    fun applySuggestedTask(task: SuggestedTask) {
        updateForm {
            it.copy(
                repository = task.repo,
                gitProvider = task.provider,
                title = task.title,
                prompt = "Work on ${task.taskType?.lowercase()?.replace('_', ' ') ?: "issue"} #${task.issueNumber} in ${task.repo}: ${task.title}"
            )
        }
    }

    fun searchRepositories(query: String) {
        updateForm { it.copy(repoQuery = query) }
        repoSearchQuery.value = query
    }

    fun setRepoProvider(provider: String) {
        updateForm { it.copy(repoProvider = provider, repoResults = emptyList(), repoNextPageId = null, selectedOrganization = null) }
        viewModelScope.launch {
            loadRepoOrganizations()
            runRepoSearch(_uiState.value.form.repoQuery)
        }
    }

    fun selectRepoOrganization(org: String?) {
        updateForm { it.copy(selectedOrganization = org, repoResults = emptyList(), repoNextPageId = null) }
        viewModelScope.launch { runRepoSearch(_uiState.value.form.repoQuery) }
    }

    fun loadRepoOrganizations() {
        if (repository.apiKey.isBlank()) return
        viewModelScope.launch {
            repository.getGitOrganizations()
                .onSuccess { orgs -> updateForm { it.copy(repoOrganizations = orgs) } }
        }
    }

    private suspend fun runRepoSearch(query: String) {
        if (repository.apiKey.isBlank()) return
        updateForm { it.copy(isSearchingRepos = true) }
        val form = _uiState.value.form
        val effectiveQuery = if (!form.selectedOrganization.isNullOrBlank()) {
            if (query.isBlank()) "${form.selectedOrganization}/" else "${form.selectedOrganization}/$query"
        } else query
        repository.searchRepositoriesPaged(effectiveQuery, form.repoProvider, pageId = null)
            .onSuccess { (repos, next) ->
                updateForm { it.copy(repoResults = repos, repoNextPageId = next, isSearchingRepos = false) }
            }
            .onFailure { e -> updateForm { it.copy(isSearchingRepos = false) }; notify(e.friendlyMessage()) }
    }

    fun loadMoreRepositories() {
        val form = _uiState.value.form
        val next = form.repoNextPageId ?: return
        if (form.isLoadingMoreRepos) return
        viewModelScope.launch {
            updateForm { it.copy(isLoadingMoreRepos = true) }
            val effectiveQuery = if (!form.selectedOrganization.isNullOrBlank()) {
                if (form.repoQuery.isBlank()) "${form.selectedOrganization}/" else "${form.selectedOrganization}/${form.repoQuery}"
            } else form.repoQuery
            repository.searchRepositoriesPaged(effectiveQuery, form.repoProvider, pageId = next)
                .onSuccess { (repos, newNext) ->
                    updateForm {
                        it.copy(
                            repoResults = it.repoResults + repos,
                            repoNextPageId = newNext,
                            isLoadingMoreRepos = false
                        )
                    }
                }
                .onFailure { e ->
                    updateForm { it.copy(isLoadingMoreRepos = false) }
                    notify(e.friendlyMessage())
                }
        }
    }
    fun selectRepository(repo: GitRepository) {
        updateForm { it.copy(repository = repo.fullName, gitProvider = repo.provider, branch = "", branchResults = emptyList()) }
        loadBranches(repo.fullName, repo.provider, null)
    }

    fun loadBranches(repository: String, provider: String?, query: String?) {
        if (repository.isBlank()) return
        viewModelScope.launch {
            updateForm { it.copy(isLoadingBranches = true) }
            this@OpenHandsViewModel.repository.searchBranches(repository, provider, query)
                .onSuccess { branches -> updateForm { it.copy(branchResults = branches, isLoadingBranches = false) } }
                .onFailure { updateForm { it.copy(isLoadingBranches = false, branchResults = emptyList()) } }
        }
    }

    fun loadSuggestedTasks() {
        if (repository.apiKey.isBlank()) return
        viewModelScope.launch {
            repository.suggestedTasks().onSuccess { tasks -> updateForm { it.copy(suggestedTasks = tasks) } }
        }
    }

    fun startConversation() {
        val form = _uiState.value.form
        if (form.prompt.isBlank()) {
            notify("Describe what the agent should do first.")
            return
        }
        if (repository.apiKey.isBlank()) {
            _uiState.update { it.copy(needsApiKey = true, currentTab = HomeTab.SETTINGS) }
            notify("Add your OpenHands API key first.")
            return
        }
        startJob?.cancel()
        startJob = viewModelScope.launch {
            val params = StartConversationParams(
                prompt = form.prompt,
                repository = form.repository,
                branch = form.branch,
                gitProvider = form.gitProvider,
                title = form.title,
                agentType = form.agentType,
                llmModel = form.llmModel,
                llmProfile = form.llmProfile,
                observabilitySpanName = form.spanName,
                observabilityTags = form.tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            )
            repository.startConversation(params).collect { progress ->
                _uiState.update { it.copy(startProgress = progress) }
                when (progress) {
                    is StartTaskProgress.Ready -> {
                        _uiState.update {
                            it.copy(form = NewConversationForm(suggestedTasks = it.form.suggestedTasks), startProgress = null)
                        }
                        notify("Conversation started")
                        openConversation(progress.conversationId)
                        refreshConversations()
                    }
                    is StartTaskProgress.Failed -> notify(progress.message)
                    else -> Unit
                }
            }
        }
    }

    fun cancelStart() {
        startJob?.cancel()
        _uiState.update { it.copy(startProgress = null) }
    }

    fun dismissStartError() = _uiState.update { it.copy(startProgress = null) }

    // --------------------------------------------------------------------- detail

    private fun startDetailPolling(conversationId: String) {
        detailPollJob?.cancel()
        detailPollJob = viewModelScope.launch {
            syncEvents(conversationId, full = false)
            repository.refreshConversation(conversationId)
            while (true) {
                val conv = selectedConversation.value
                val interval = when {
                    conv == null -> 5_000L
                    conv.status.isActive || _uiState.value.isSendingMessage -> 3_000L
                    conv.status == ConversationStatus.WAITING_FOR_CONFIRMATION -> 5_000L
                    else -> 15_000L
                }
                delay(interval)
                if (_uiState.value.selectedConversationId != conversationId) break
                repository.refreshConversation(conversationId).onFailure { e ->
                    if (e is ApiException && e.code == 404) {
                        notify("This conversation no longer exists.")
                        _uiState.update { it.copy(screenStack = emptyList()) }
                    }
                }
                syncEvents(conversationId, full = false)
            }
        }
    }

    fun refreshDetail() {
        val id = _uiState.value.selectedConversationId ?: return
        viewModelScope.launch {
            repository.refreshConversation(id)
            syncEvents(id, full = true)
        }
    }

    private suspend fun syncEvents(conversationId: String, full: Boolean) {
        _uiState.update { it.copy(isSyncingEvents = true) }
        repository.syncEvents(conversationId, full)
            .onSuccess { _uiState.update { it.copy(isSyncingEvents = false, detailError = null) } }
            .onFailure { e -> _uiState.update { it.copy(isSyncingEvents = false, detailError = e.friendlyMessage()) } }
    }

    fun sendMessage(text: String) {
        val id = _uiState.value.selectedConversationId ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingMessage = true) }
            repository.sendMessage(id, text)
                .onSuccess { outcome ->
                    if (outcome.resumedSandbox) notify("Sandbox resumed and message sent")
                    if (outcome.sandboxStatus != SandboxStatus.RUNNING && outcome.message != null) notify(outcome.message)
                }
                .onFailure { notify(it.friendlyMessage()) }
            _uiState.update { it.copy(isSendingMessage = false) }
            repository.refreshConversation(id)
        }
    }

    fun toggleNoiseEvents() {
        val next = !_uiState.value.showNoiseEvents
        repository.showNoiseEvents = next
        _uiState.update { it.copy(showNoiseEvents = next) }
    }

    fun pauseSandbox(sandboxId: String) = sandboxAction("Sandbox paused") { repository.pauseSandbox(sandboxId) }
    fun resumeSandbox(sandboxId: String) = sandboxAction("Sandbox resuming") { repository.resumeSandbox(sandboxId) }
    fun deleteSandbox(sandboxId: String) = sandboxAction("Sandbox deleted") { repository.deleteSandbox(sandboxId) }

    private fun sandboxAction(successMessage: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutatingConversation = true) }
            block()
                .onSuccess {
                    notify(successMessage)
                    _uiState.value.selectedConversationId?.let { repository.refreshConversation(it) }
                    if (_uiState.value.topScreen is Screen.Sandboxes) loadSandboxes()
                }
                .onFailure { notify(it.friendlyMessage()) }
            _uiState.update { it.copy(isMutatingConversation = false) }
        }
    }

    // ------------------------------------------------------------------ workspace

    fun browseFiles(conversationId: String, path: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(workspace = it.workspace.copy(isLoading = true, error = null, openFilePath = null, openFileContent = null)) }
            repository.listFiles(conversationId, path)
                .onSuccess { entries ->
                    _uiState.update { it.copy(workspace = it.workspace.copy(path = path, entries = entries.sorted(), isLoading = false)) }
                }
                .onFailure { e -> _uiState.update { it.copy(workspace = it.workspace.copy(isLoading = false, error = e.friendlyMessage())) } }
        }
    }

    fun openFile(conversationId: String, filePath: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(workspace = it.workspace.copy(isLoading = true, error = null)) }
            repository.readFile(conversationId, filePath)
                .onSuccess { content ->
                    _uiState.update { it.copy(workspace = it.workspace.copy(isLoading = false, openFilePath = filePath, openFileContent = content)) }
                }
                .onFailure { e -> _uiState.update { it.copy(workspace = it.workspace.copy(isLoading = false, error = e.friendlyMessage())) } }
        }
    }

    fun closeFile() = _uiState.update { it.copy(workspace = it.workspace.copy(openFilePath = null, openFileContent = null)) }

    fun setRepoRoot(root: String) = _uiState.update { it.copy(workspace = it.workspace.copy(repoRoot = root)) }

    fun loadChanges(conversationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(workspace = it.workspace.copy(isLoading = true, error = null, selectedDiff = null)) }
            val root = _uiState.value.workspace.repoRoot
            repository.gitChanges(conversationId, root)
                .onSuccess { changes -> _uiState.update { it.copy(workspace = it.workspace.copy(changes = changes, isLoading = false)) } }
                .onFailure { e -> _uiState.update { it.copy(workspace = it.workspace.copy(isLoading = false, error = e.friendlyMessage())) } }
        }
    }

    fun openDiff(conversationId: String, change: GitChange) {
        viewModelScope.launch {
            _uiState.update { it.copy(workspace = it.workspace.copy(isLoading = true, error = null)) }
            val root = _uiState.value.workspace.repoRoot.trimEnd('/')
            val absolute = if (change.path.startsWith("/")) change.path else "$root/${change.path}"
            repository.gitDiff(conversationId, absolute)
                .onSuccess { diff -> _uiState.update { it.copy(workspace = it.workspace.copy(isLoading = false, selectedDiff = diff)) } }
                .onFailure { e -> _uiState.update { it.copy(workspace = it.workspace.copy(isLoading = false, error = e.friendlyMessage())) } }
        }
    }

    fun closeDiff() = _uiState.update { it.copy(workspace = it.workspace.copy(selectedDiff = null)) }

    // ------------------------------------------------------------------ sandboxes

    fun loadSandboxes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSandboxes = true) }
            repository.listSandboxes()
                .onSuccess { list -> _uiState.update { it.copy(sandboxes = list, isLoadingSandboxes = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoadingSandboxes = false) }; notify(e.friendlyMessage()) }
        }
    }
    fun createSandbox(specId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSandboxes = true) }
            repository.createSandbox(specId)
                .onSuccess { sb ->
                    notify("Sandbox ${sb.id.take(8)} created")
                    loadSandboxes()
                }
                .onFailure { notify(it.friendlyMessage()) }
            _uiState.update { it.copy(isLoadingSandboxes = false) }
        }
    }


    // ------------------------------------------------------------------- secrets

    fun loadSecrets() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSecrets = true) }
            repository.listSecrets()
                .onSuccess { list -> _uiState.update { it.copy(secrets = list, isLoadingSecrets = false) } }
                .onFailure { e -> _uiState.update { it.copy(isLoadingSecrets = false) }; notify(e.friendlyMessage()) }
        }
    }

    fun createSecret(name: String, value: String, description: String?) {
        viewModelScope.launch {
            repository.createSecret(name, value, description)
                .onSuccess { notify("Secret saved"); loadSecrets() }
                .onFailure { notify(it.friendlyMessage()) }
        }
    }

    fun deleteSecret(name: String) {
        viewModelScope.launch {
            repository.deleteSecret(name)
                .onSuccess { notify("Secret deleted"); loadSecrets() }
                .onFailure { notify(it.friendlyMessage()) }
        }
    }

    // ------------------------------------------------------------ profiles / skills

    fun loadLlmProfiles() {
        viewModelScope.launch {
            repository.listLlmProfiles()
                .onSuccess { list -> _uiState.update { it.copy(llmProfiles = list) } }
                .onFailure { notify(it.friendlyMessage()) }
        }
    }

    fun activateLlmProfile(name: String) {
        viewModelScope.launch {
            repository.activateLlmProfile(name)
                .onSuccess { notify("Default profile: $name"); loadLlmProfiles() }
                .onFailure { notify(it.friendlyMessage()) }
        }
    }

    fun switchConversationProfile(conversationId: String, profileName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutatingConversation = true) }
            repository.switchConversationProfile(conversationId, profileName)
                .onSuccess { notify("Switched to profile $profileName"); repository.refreshConversation(conversationId) }
                .onFailure { notify(it.friendlyMessage()) }
            _uiState.update { it.copy(isMutatingConversation = false) }
        }
    }

    /** Publishes or unpublishes a conversation (webui "share" toggle). */
    fun setConversationPublic(conversationId: String, public: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMutatingConversation = true) }
            repository.setConversationPublic(conversationId, public)
                .onSuccess { notify(if (public) "Conversation is now public" else "Conversation is private again") }
                .onFailure { notify(it.friendlyMessage()) }
            _uiState.update { it.copy(isMutatingConversation = false) }
        }
    }
    fun createLlmProfile(name: String, model: String, baseUrl: String? = null, apiKey: String? = null) {
        if (name.isBlank() || model.isBlank()) {
            notify("Profile name and model are required.")
            return
        }
        viewModelScope.launch {
            repository.saveLlmProfile(name, model, baseUrl, apiKey)
                .onSuccess {
                    notify("Profile $name saved")
                    loadLlmProfiles()
                }
                .onFailure { notify(it.friendlyMessage()) }
        }
    }

    fun deleteLlmProfile(name: String) {
        viewModelScope.launch {
            repository.deleteLlmProfile(name)
                .onSuccess {
                    notify("Profile $name deleted")
                    loadLlmProfiles()
                }
                .onFailure { notify(it.friendlyMessage()) }
        }
    }

    fun renameLlmProfile(name: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.renameLlmProfile(name, newName)
                .onSuccess {
                    notify("Profile renamed to $newName")
                    loadLlmProfiles()
                }
                .onFailure { notify(it.friendlyMessage()) }
        }
    }

    fun setGitProviderToken(provider: String, token: String) {
        if (token.isBlank()) return
        viewModelScope.launch {
            repository.setGitProviderToken(provider, token)
                .onSuccess { notify("Git token saved for $provider") }
                .onFailure { notify(it.friendlyMessage()) }
        }
    }

    fun clearGitProviderTokens() {
        viewModelScope.launch {
            repository.clearGitProviderTokens()
                .onSuccess { notify("Git provider tokens cleared") }
                .onFailure { notify(it.friendlyMessage()) }
        }
    }

    suspend fun downloadTrajectory(conversationId: String): Result<ByteArray> {
        return repository.downloadTrajectory(conversationId)
    }


    fun loadConversationSkills(conversationId: String) {
        viewModelScope.launch {
            repository.conversationSkills(conversationId)
                .onSuccess { list -> _uiState.update { it.copy(conversationSkills = list) } }
                .onFailure { e -> _uiState.update { it.copy(conversationSkills = emptyList()) }; notify(e.friendlyMessage()) }
        }
    }

    fun clearConversationSkills() = _uiState.update { it.copy(conversationSkills = null) }

    // ------------------------------------------------------------------- settings

    fun updateApiKeyInput(value: String) = _uiState.update { it.copy(apiKeyInput = value) }
    fun updateBaseUrlInput(value: String) = _uiState.update { it.copy(baseUrlInput = value) }
    fun updateGitUserNameInput(value: String) = _uiState.update { it.copy(gitUserNameInput = value) }
    fun updateGitUserEmailInput(value: String) = _uiState.update { it.copy(gitUserEmailInput = value) }
    fun updateMaxBudgetInput(value: String) = _uiState.update { it.copy(maxBudgetInput = value.filter { c -> c.isDigit() || c == '.' }) }

    fun setThemeMode(mode: ThemeMode) {
        repository.themeMode = mode.name
        _uiState.update { it.copy(themeMode = mode) }
    }

    /** Loads the verified model catalog for dropdowns; safe to call repeatedly. */
    fun loadModelCatalog() {
        if (_uiState.value.isLoadingModels) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingModels = true) }
            repository.searchModels()
                .onSuccess { models -> _uiState.update { it.copy(modelCatalog = models, isLoadingModels = false) } }
                .onFailure { _uiState.update { it.copy(isLoadingModels = false) } }
        }
    }

    fun saveSettings() {
        val key = _uiState.value.apiKeyInput.trim()
        val url = _uiState.value.baseUrlInput.trim()
        val gitName = _uiState.value.gitUserNameInput.trim()
        val gitEmail = _uiState.value.gitUserEmailInput.trim()
        val maxBudget = _uiState.value.maxBudgetInput.trim().toDoubleOrNull()
        repository.updateConfig(key, url)
        repository.gitUserName = gitName.ifEmpty { null }
        repository.gitUserEmail = gitEmail.ifEmpty { null }
        repository.maxBudgetPerTask = maxBudget
        _uiState.update {
            it.copy(
                apiKeyInput = repository.apiKey,
                baseUrlInput = repository.baseUrl,
                isUsingBuildTimeKey = repository.isUsingBuildTimeKey,
                gitUserNameInput = gitName,
                gitUserEmailInput = gitEmail,
                maxBudgetInput = maxBudget?.toString() ?: "",
                needsApiKey = repository.apiKey.isBlank(),
                account = null,
                accountError = null
            )
        }
        viewModelScope.launch { repository.clearCache() }
        notify("Settings saved")
        if (repository.apiKey.isNotBlank()) {
            loadAccount()
            refreshConversations()
            loadRepoOrganizations()
            viewModelScope.launch { runRepoSearch("") }
        }
    }

    fun loadAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, accountError = null) }
            repository.fetchAccount()
                .onSuccess { info -> _uiState.update { it.copy(account = info, isTestingConnection = false, needsApiKey = false) } }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            account = null,
                            isTestingConnection = false,
                            accountError = e.friendlyMessage(),
                            needsApiKey = e is ApiException && e.isAuthError
                        )
                    }
                }
        }
    }

    fun clearLocalCache() {
        viewModelScope.launch {
            repository.clearCache()
            notify("Local cache cleared")
            refreshConversations()
        }
    }

    fun saveTemplate(template: PromptTemplate) {
        viewModelScope.launch { repository.saveTemplate(template); notify("Template saved") }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch { repository.deleteTemplate(id) }
    }
}
