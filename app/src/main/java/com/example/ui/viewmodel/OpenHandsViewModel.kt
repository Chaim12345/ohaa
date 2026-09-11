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
import com.example.data.model.UserInfoDto
import com.example.data.repository.CloudError
import com.example.data.repository.OpenHandsRepository
import com.example.data.settings.AgentKind
import com.example.data.settings.AgentProfile
import com.example.data.settings.AgentProfileStore
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
    val isDemoMode: Boolean = true,
    val notification: String? = null,
    // New task
    val newTaskPrompt: String = "",
    val newTaskRepo: String = "",
    val isLaunching: Boolean = false,
    val launchStatusText: String? = null,
    val launchStage: LaunchStage = LaunchStage.IDLE,
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
    // Agent & LLM — what the user sees as "active" right now. Defaults to the
    // plain OpenHands agent; ACP is only ever used when explicitly chosen.
    val activeProfile: AgentProfile = AgentProfile(),
    // Git integration — repos from the configured upstream provider
    val repoSearchResults: List<RepositoryDto> = emptyList(),
    val repoBranchResults: List<BranchDto> = emptyList(),
    val selectedBranch: String = "",
    val isSearchingRepos: Boolean = false,
    val gitDiff: GitDiffDto? = null,
    val isGitLoading: Boolean = false
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
            // Reflect the connection state immediately (demo notice or cloud sync)
            // instead of leaving the dashboard in a blank "Not connected" limbo.
            refreshConversations(showFeedback = false)
        }
    }

    // ---- Navigation -----------------------------------------------------------

    fun setTab(tab: AppTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

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
                    offlineMessage = "Demo mode — add your OpenHands Cloud API key in Settings to go live.",
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
            val llmModel = if (profile.kind == AgentKind.OPENHANDS) profile.model else null
            val agentType = when (profile.kind) {
                AgentKind.OPENHANDS -> "default"
                AgentKind.ACP -> "acp"
            }

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
                .onFailure { }
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

    // ---- Agent & LLM profile ------------------------------------------------------------

    /**
     * Updates the visible agent/LLM selection. Inside an open conversation the
     * change is a **mid-session switch**: it is persisted as a per-conversation
     * override and takes effect for subsequent messages. Outside a
     * conversation (Settings / New Task) it updates the global default used
     * for new conversations. ACP is only ever active when explicitly chosen.
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

    fun setAgentKind(kind: AgentKind) = updateActiveProfile { it.copy(kind = kind) }

    fun setModel(model: String) {
        if (model.isBlank()) return
        val conversationId = _uiState.value.detailConversationId
        updateActiveProfile { it.copy(model = model.trim()) }
        // Mid-session switch: push the model onto the running conversation via
        // a matching saved cloud profile (switch_profile), when one exists.
        if (conversationId != null && _uiState.value.activeProfile.kind == AgentKind.OPENHANDS) {
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

    fun setAcpCommand(command: String) {
        if (command.isBlank()) return
        updateActiveProfile { it.copy(acpCommand = command.trim()) }
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
            notification = "API key cleared — demo mode enabled"
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
            repository.seedLocalData()
            _uiState.value = _uiState.value.copy(notification = "Local data cleared")
        }
    }

    fun dismissNotification() {
        _uiState.value = _uiState.value.copy(notification = null)
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is CloudError.Unauthorized -> message ?: "Unauthorized"
        is CloudError.Api -> message ?: "API error"
        is CloudError.Network -> message ?: "Network error"
        else -> message ?: "Unexpected error"
    }
}
