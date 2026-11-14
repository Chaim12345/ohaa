package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.keepalive.KeepAliveService
import com.example.data.local.AppDatabase
import com.example.data.model.AcpEffortOptionDto
import com.example.data.model.BashEventDto
import com.example.data.model.SubAgentDto
import com.example.data.model.ConversationRuntimeDto
import com.example.data.model.InstalledPluginDto
import com.example.data.model.InstalledSkillDto
import com.example.data.model.LlmProviderDto
import com.example.data.model.ProfileConfigDto
import com.example.data.model.ProfileDetailDto
import com.example.data.model.RuntimeLlmModelDto
import com.example.data.model.ToolCatalogEntryDto
import com.example.data.model.UpdateUserSettingsRequest
import com.example.data.model.UserSettingsDto
import com.example.data.model.WebClientConfigDto

import com.example.data.model.AcpModelInfoDto
import com.example.data.model.AgentProfileInfoDto
import com.example.data.model.AgentEvent
import com.example.data.model.ConversationInfoDto
import com.example.data.model.ConversationStatus
import com.example.data.model.FileEntryDto
import com.example.data.model.GitChange
import com.example.data.model.GitCommitDto
import com.example.data.model.GitDiffDto
import com.example.data.model.LlmModelDto
import com.example.data.model.OpenHandsConversation
import com.example.data.model.ProfileInfoDto
import com.example.data.model.PromptTemplate
import com.example.data.model.SandboxInfoDto
import com.example.data.model.SecretDto
import com.example.data.model.SkillInfoDto
import com.example.data.repository.AgentServerRepository
import com.example.data.repository.OpenHandsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppNavTab(val label: String) {
    DASHBOARD("Dashboard"),
    TASKS("Active Tasks"),
    NEW_TASK("New Agent"),
    RESOURCES("Resources"),
    SETTINGS("Cloud API")
}

data class OpenHandsUiState(
    val currentTab: AppNavTab = AppNavTab.DASHBOARD,
    val selectedConversationId: String? = null,
    val searchQuery: String = "",
    val filterStatus: ConversationStatus? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isProvisioning: Boolean = false,
    val isSendingMessage: Boolean = false,
    val gitChanges: List<GitChange> = emptyList(),
    val isLoadingChanges: Boolean = false,
    val connectionStatus: String? = null,
    val connectionHealthy: Boolean? = null,
    val notificationMessage: String? = null,
    val newTaskPrompt: String = "",
    val newTaskRepo: String = "",
    val newTaskBranch: String = "",
    val selectedTemplate: PromptTemplate? = null,
    val apiKey: String = "",
    val baseUrl: String = "https://app.all-hands.dev",
    val keepAliveEnabled: Boolean = false,
    val isResuming: Boolean = false,
    // ---- runtime (agent-server) state ----
    val isRuntimeBusy: Boolean = false,
    val terminalCommand: String = "",
    val terminalOutput: String? = null,
    val runtimeFiles: List<FileEntryDto> = emptyList(),
    val filesPath: String? = null,
    val gitCommits: List<GitCommitDto> = emptyList(),
    val gitDiff: GitDiffDto? = null,
    val conversationInfo: ConversationInfoDto? = null,
    val availableEfforts: List<AcpEffortOptionDto> = emptyList(),
    val availableAcpModels: List<AcpModelInfoDto> = emptyList(),
    // ---- settings catalogs ----
    val profiles: List<ProfileInfoDto> = emptyList(),
    val models: List<LlmModelDto> = emptyList(),
    val modelQuery: String = "",
    val secrets: List<SecretDto> = emptyList(),
    val skills: List<SkillInfoDto> = emptyList(),
    val sandboxes: List<SandboxInfoDto> = emptyList(),
    val gitLogin: String? = null,
    val agentProfiles: List<AgentProfileInfoDto> = emptyList(),
    // ---- new-task options ----
    val agentTypeSelection: String = "default",
    val llmProfileSelection: String? = null,
    val agentProfileSelection: String? = null,
    val selectedModelForTask: String? = null,
    // ---- cloud settings / git / web-client ----
    val userSettings: UserSettingsDto? = null,
    val gitOrganizations: List<String> = emptyList(),
    val gitInstallations: List<String> = emptyList(),
    val webClientConfig: WebClientConfigDto? = null,
    val agentSchema: String? = null,
    val conversationSchema: String? = null,
    // ---- runtime extras ----
    val runtimeInfo: ConversationRuntimeDto? = null,
    val agentFinalResponse: String? = null,
    val vscodeUrl: String? = null,
    val bashEvents: List<BashEventDto> = emptyList(),
    val installedSkills: List<InstalledSkillDto> = emptyList(),
    val installedPlugins: List<InstalledPluginDto> = emptyList(),
    val toolCatalog: List<ToolCatalogEntryDto> = emptyList(),
    val runtimeProviders: List<LlmProviderDto> = emptyList(),
    val runtimeModels: List<RuntimeLlmModelDto> = emptyList(),
    val selectedProfileDetail: ProfileDetailDto? = null,
    val lastToolOutput: String? = null,
    val subAgents: List<SubAgentDto> = emptyList(),
    val cloudSkillCatalog: List<InstalledSkillDto> = emptyList(),
    val marketplaceSkills: List<String> = emptyList(),
    val marketplacePlugins: List<String> = emptyList(),
    val mcpOAuthUrl: String? = null,
    val mcpOAuthJobId: String? = null,
    val mcpOAuthStatus: String? = null,
    val childConversations: List<com.example.data.model.ConversationInfoDto> = emptyList(),
    val workspaceInfo: String? = null,
)

class OpenHandsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = OpenHandsRepository(application, database)
    private val runtimeRepository = AgentServerRepository(database.conversationDao())

    private val _uiState = MutableStateFlow(
        OpenHandsUiState(
            apiKey = repository.apiKey,
            baseUrl = repository.baseUrl,
            keepAliveEnabled = repository.keepAliveEnabled
        )
    )
    val uiState: StateFlow<OpenHandsUiState> = _uiState.asStateFlow()

    val conversations: StateFlow<List<OpenHandsConversation>> =
        repository.conversationsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val templates: StateFlow<List<PromptTemplate>> =
        repository.templatesFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _events = MutableStateFlow<List<AgentEvent>>(emptyList())
    val events: StateFlow<List<AgentEvent>> = _events.asStateFlow()

    private val _gitChanges = MutableStateFlow<List<GitChange>>(emptyList())
    val gitChanges: StateFlow<List<GitChange>> = _gitChanges.asStateFlow()

    private var eventObservationJob: Job? = null
    private var pollingJob: Job? = null
    private var startTaskJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            if (repository.keepAliveEnabled) {
                KeepAliveService.start(getApplication())
            }
            if (repository.apiKey.isNotBlank()) {
                // Load real conversations as soon as the app starts.
                refreshConversations()
            } else {
                _uiState.value = _uiState.value.copy(
                    connectionStatus = "Add your OpenHands API key in Cloud API settings to connect.",
                    connectionHealthy = false
                )
            }
        }
    }

    fun setTab(tab: AppNavTab) {
        _uiState.value = _uiState.value.copy(
            currentTab = tab,
            selectedConversationId = null
        )
    }

    fun selectConversation(id: String) {
        conversations.value.find { it.id == id }?.let { resumeIfPaused(it) }
        _uiState.value = _uiState.value.copy(selectedConversationId = id)
        // Drop the previous conversation's timeline before the Room flow emits.
        _events.value = emptyList()
        _gitChanges.value = emptyList()
        resetRuntimeState()
        observeConversationEvents(id)
        // Pull runtime state (models/efforts/metrics) once the sandbox is reachable.
        loadConversationInfo()
    }

    fun closeConversationDetail() {
        _uiState.value = _uiState.value.copy(selectedConversationId = null)
        _events.value = emptyList()
        _gitChanges.value = emptyList()
        resetRuntimeState()
        eventObservationJob?.cancel()
        pollingJob?.cancel()
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setStatusFilter(status: ConversationStatus?) {
        _uiState.value = _uiState.value.copy(filterStatus = status)
    }

    fun updateNewTaskPrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(newTaskPrompt = prompt)
    }

    fun updateNewTaskRepo(repo: String) {
        _uiState.value = _uiState.value.copy(newTaskRepo = repo)
    }

    fun updateNewTaskBranch(branch: String) {
        _uiState.value = _uiState.value.copy(newTaskBranch = branch)
    }

    fun applyTemplate(template: PromptTemplate) {
        _uiState.value = _uiState.value.copy(
            selectedTemplate = template,
            newTaskPrompt = template.prompt,
            newTaskRepo = template.defaultRepo ?: _uiState.value.newTaskRepo,
            currentTab = AppNavTab.NEW_TASK
        )
    }

    fun updateApiKeyInput(key: String) {
        _uiState.value = _uiState.value.copy(apiKey = key)
    }

    fun updateBaseUrlInput(url: String) {
        _uiState.value = _uiState.value.copy(baseUrl = url)
    }

    fun saveConfig() {
        val key = _uiState.value.apiKey.trim()
        val url = _uiState.value.baseUrl.trim().ifEmpty { "https://app.all-hands.dev" }
        repository.updateConfig(key, url)
        _uiState.value = _uiState.value.copy(
            notificationMessage = "Saved API credentials & Cloud endpoint"
        )
        testConnection()
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, connectionStatus = "Testing connection...")
            val result = repository.checkHealth()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    connectionStatus = result.getOrNull() ?: "Connected to OpenHands Cloud",
                    connectionHealthy = true
                )
                // First successful handshake should populate the dashboard.
                refreshConversations()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    connectionStatus = "Notice: ${result.exceptionOrNull()?.message ?: "Check URL or API Key"}",
                    connectionHealthy = false
                )
            }
        }
    }

    fun refreshConversations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val result = repository.refreshConversations()
            _uiState.value = _uiState.value.copy(
                isRefreshing = false,
                notificationMessage = if (result.isSuccess) "Synchronized with OpenHands Cloud" else result.exceptionOrNull()?.message
            )
        }
    }

    fun createNewTask(onSuccess: (String) -> Unit) {
        val prompt = _uiState.value.newTaskPrompt.trim()
        if (prompt.isEmpty()) {
            _uiState.value = _uiState.value.copy(notificationMessage = "Please enter a task description")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val repo = _uiState.value.newTaskRepo.trim().ifEmpty { null }
            val branch = _uiState.value.newTaskBranch.trim().ifEmpty { null }
            val result = repository.startConversation(
                prompt = prompt,
                repository = repo,
                branch = branch,
                llmModel = _uiState.value.selectedModelForTask,
                llmProfile = _uiState.value.llmProfileSelection,
                agentProfileId = _uiState.value.agentProfileSelection,
                agentType = _uiState.value.agentTypeSelection,
            )
            _uiState.value = _uiState.value.copy(isLoading = false)

            val handle = result.getOrElse {
                _uiState.value = _uiState.value.copy(
                    notificationMessage = it.message ?: "Failed to start conversation"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                newTaskPrompt = "",
                newTaskRepo = "",
                newTaskBranch = "",
                selectedTemplate = null,
                notificationMessage = "Task launched in OpenHands sandbox!"
            )

            val conversationId = handle.conversationId
            if (conversationId != null) {
                selectConversation(conversationId)
                onSuccess(conversationId)
            } else {
                // Provisioning is asynchronous; await it without freezing the UI.
                awaitStartTask(handle.taskId, onSuccess)
            }
        }
    }

    private fun awaitStartTask(taskId: String, onSuccess: (String) -> Unit) {
        startTaskJob?.cancel()
        startTaskJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProvisioning = true,
                connectionStatus = "Provisioning sandbox..."
            )
            val result = repository.awaitConversationId(taskId)
            _uiState.value = _uiState.value.copy(isProvisioning = false)
            result.fold(
                onSuccess = { conversationId ->
                    _uiState.value = _uiState.value.copy(
                        notificationMessage = "Sandbox ready. Agent is working.",
                        connectionStatus = null
                    )
                    selectConversation(conversationId)
                    onSuccess(conversationId)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        notificationMessage = error.message ?: "Sandbox failed to start",
                        connectionStatus = null
                    )
                }
            )
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            val result = repository.deleteConversation(conversationId)
            if (result.isSuccess) {
                if (_uiState.value.selectedConversationId == conversationId) {
                    closeConversationDetail()
                }
                _uiState.value = _uiState.value.copy(notificationMessage = "Conversation deleted")
            } else {
                _uiState.value = _uiState.value.copy(
                    notificationMessage = result.exceptionOrNull()?.message ?: "Failed to delete conversation"
                )
            }
        }
    }

    fun sendFollowUpMessage(conversationId: String, message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSendingMessage = true)
            val result = repository.sendMessage(conversationId, message)
            _uiState.value = _uiState.value.copy(
                isSendingMessage = false,
                notificationMessage = if (result.isSuccess) {
                    _uiState.value.notificationMessage
                } else {
                    result.exceptionOrNull()?.message ?: "Failed to send message"
                }
            )
        }
    }

    fun refreshGitChanges(conversationId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingChanges = true)
            val result = repository.getGitChanges(conversationId)
            _uiState.value = _uiState.value.copy(
                isLoadingChanges = false,
                notificationMessage = if (result.isFailure) {
                    result.exceptionOrNull()?.message ?: "Failed to load changed files"
                } else {
                    _uiState.value.notificationMessage
                }
            )
            _gitChanges.value = result.getOrDefault(emptyList())
        }
    }

    fun refreshEvents(conversationId: String, silent: Boolean = false) {
        viewModelScope.launch {
            // Manual/initial refresh walks the full history; the 8s poll only needs the newest page.
            val result = repository.refreshEvents(conversationId, deep = !silent)
            if (result.isFailure && !silent) {
                _uiState.value = _uiState.value.copy(
                    notificationMessage = result.exceptionOrNull()?.message ?: "Failed to load events"
                )
            }
        }
    }

    private fun observeConversationEvents(conversationId: String) {
        eventObservationJob?.cancel()
        pollingJob?.cancel()

        eventObservationJob = viewModelScope.launch {
            repository.getEventsFlow(conversationId).collect { eventList ->
                _events.value = eventList
            }
        }

        // Pull latest from cloud API
        refreshEvents(conversationId)
        viewModelScope.launch { refreshConversationStatus(conversationId) }

        // Auto polling for live conversation
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(8000)
                if (_uiState.value.selectedConversationId == conversationId) {
                    refreshEvents(conversationId, silent = true)
                    if (!refreshConversationStatus(conversationId)) break
                } else {
                    break
                }
            }
        }
    }

    /** Refreshes the conversation row; returns false once polling is no longer useful. */
    private suspend fun refreshConversationStatus(conversationId: String): Boolean {
        val result = repository.refreshConversation(conversationId)
        val status = result.getOrNull()?.status
        // A successful fetch that returns nothing means the conversation is gone.
        if (result.isSuccess && status == null) return false
        return status !in TERMINAL_STATUSES
    }

    /**
     * Turns the background keep-alive service on or off. Requires a valid key;
     * without one the service would have nothing to ping.
     */
    fun setKeepAliveEnabled(enabled: Boolean) {
        if (enabled && repository.apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(
                notificationMessage = "Add your OpenHands API key before enabling keep-alive"
            )
            return
        }
        repository.keepAliveEnabled = enabled
        _uiState.value = _uiState.value.copy(
            keepAliveEnabled = enabled,
            notificationMessage = if (enabled) {
                "Background keep-alive enabled"
            } else {
                "Background keep-alive disabled"
            }
        )
        if (enabled) {
            KeepAliveService.start(getApplication())
        } else {
            KeepAliveService.stop(getApplication())
        }
    }

    /**
     * Manually resumes the currently open paused sandbox. Surfaces a message if
     * the sandbox is gone (deleted) rather than silently doing nothing.
     */
    fun resumeSelectedSandbox() {
        val conversation = conversations.value.find { it.id == _uiState.value.selectedConversationId }
            ?: return
        val sandboxId = conversation.sandboxId
        if (sandboxId.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                notificationMessage = "No sandbox is attached to this conversation"
            )
            return
        }
        _uiState.value = _uiState.value.copy(isResuming = true)
        viewModelScope.launch {
            val result = repository.resumeSandbox(sandboxId)
            _uiState.value = _uiState.value.copy(
                isResuming = false,
                notificationMessage = if (result.isSuccess) {
                    "Resuming sandbox…"
                } else {
                    "Could not resume sandbox: ${result.exceptionOrNull()?.message ?: "unknown error"}"
                }
            )
            if (result.isSuccess) refreshConversationStatus(conversation.id)
        }
    }

    /**
     * Safety net for the case where a sandbox paused anyway (idle timer lost a
     * race, or the app was killed): resume it before the user starts reading,
     * so the workspace and git state come back.
     */
    private fun resumeIfPaused(conversation: OpenHandsConversation) {
        if (conversation.status != ConversationStatus.PAUSED) return
        val sandboxId = conversation.sandboxId ?: return
        viewModelScope.launch {
            repository.resumeSandbox(sandboxId)
            refreshConversationStatus(conversation.id)
        }
    }

    fun dismissNotification() {
        _uiState.value = _uiState.value.copy(notificationMessage = null)
    }

    fun clearLocalCache() {
        viewModelScope.launch {
            repository.clearCache()
            repository.seedInitialDataIfEmpty()
            _uiState.value = _uiState.value.copy(notificationMessage = "Local database cache refreshed")
            // Re-pull from the cloud so the wiped list repopulates immediately.
            if (repository.apiKey.isNotBlank()) refreshConversations()
        }
    }

    // ---------------------------------------------------------------------
    // Runtime (agent-server) actions
    // ---------------------------------------------------------------------

    private fun selectedConversation(): OpenHandsConversation? =
        conversations.value.find { it.id == _uiState.value.selectedConversationId }

    /** A short message that explains why a runtime action is unavailable. */
    fun runtimeUnavailableReason(): String? {
        val c = selectedConversation() ?: return "No conversation selected"
        if (!c.hasRuntimeAccess) return "Runtime not ready — refresh this conversation"
        if (c.sandboxStatus.equals("PAUSED", true)) return "Sandbox is paused — resume it first"
        if (c.sandboxStatus.equals("MISSING", true)) return "Sandbox no longer exists"
        return null
    }

    private fun notify(message: String?) {
        _uiState.value = _uiState.value.copy(notificationMessage = message)
    }

    /** Loads runtime state (models, efforts, token/cost metrics) for the open conversation. */
    fun loadConversationInfo() {
        val c = selectedConversation() ?: return
        if (!c.hasRuntimeAccess) return
        viewModelScope.launch {
            val result = runtimeRepository.conversationInfo(c.id)
            result.getOrNull()?.let { info ->
                _uiState.value = _uiState.value.copy(
                    conversationInfo = info,
                    availableEfforts = info.availableEfforts.orEmpty(),
                    availableAcpModels = info.availableModels.orEmpty(),
                )
            }
        }
    }

    fun loadProfiles() {
        viewModelScope.launch {
            repository.listProfiles().getOrNull()?.let {
                _uiState.value = _uiState.value.copy(profiles = it)
            }
        }
    }

    fun loadModels(query: String = "") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(modelQuery = query)
            repository.searchModels(query).getOrNull()?.let {
                _uiState.value = _uiState.value.copy(models = it)
            }
        }
    }

    /**
     * Switches the model of the open conversation. Routes to the correct
     * endpoint by agent kind: ACP conversations use `switch_acp_model`, plain
     * OpenHands agents use `switch_llm`; both have a cloud-profile path too.
     */
    fun switchModel(model: String) {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRuntimeBusy = true)
            val result = if (c.isAcp) {
                runtimeRepository.switchAcpModel(c.id, model)
            } else {
                runtimeRepository.switchLlm(c.id, model)
            }
            _uiState.value = _uiState.value.copy(isRuntimeBusy = false)
            notify(if (result.isSuccess) "Model switched to $model" else result.exceptionOrNull()?.message)
            if (result.isSuccess) {
                refreshConversationStatus(c.id)
                loadConversationInfo()
            }
        }
    }

    fun switchProfile(profileName: String) {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRuntimeBusy = true)
            val result = runtimeRepository.switchProfile(c.id, profileName)
            _uiState.value = _uiState.value.copy(isRuntimeBusy = false)
            notify(if (result.isSuccess) "Switched to profile '$profileName'" else result.exceptionOrNull()?.message)
            if (result.isSuccess) refreshConversationStatus(c.id)
        }
    }

    fun switchEffort(effort: String) {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRuntimeBusy = true)
            val result = runtimeRepository.switchAcpEffort(c.id, effort)
            _uiState.value = _uiState.value.copy(isRuntimeBusy = false)
            notify(if (result.isSuccess) "Reasoning effort set to $effort" else result.exceptionOrNull()?.message)
            if (result.isSuccess) loadConversationInfo()
        }
    }

    fun compactContext() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRuntimeBusy = true)
            val result = runtimeRepository.condense(c.id)
            _uiState.value = _uiState.value.copy(isRuntimeBusy = false)
            notify(if (result.isSuccess) "Context compacted" else result.exceptionOrNull()?.message)
            if (result.isSuccess) loadConversationInfo()
        }
    }

    fun runAgent() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = runtimeRepository.run(c.id)
            notify(if (result.isSuccess) "Agent resumed" else result.exceptionOrNull()?.message)
        }
    }

    fun pauseAgent() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = runtimeRepository.pause(c.id)
            notify(if (result.isSuccess) "Agent paused" else result.exceptionOrNull()?.message)
        }
    }

    fun stopAgent() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = runtimeRepository.interrupt(c.id)
            notify(if (result.isSuccess) "Stop requested" else result.exceptionOrNull()?.message)
        }
    }

    // ---- terminal ---------------------------------------------------------

    fun updateTerminalCommand(command: String) {
        _uiState.value = _uiState.value.copy(terminalCommand = command)
    }

    fun runTerminalCommand() {
        val c = selectedConversation() ?: return
        val command = _uiState.value.terminalCommand.trim()
        if (command.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRuntimeBusy = true, terminalOutput = "Running…")
            val result = runtimeRepository.executeBash(c.id, command)
            val output = result.getOrNull()?.let { dto ->
                buildString {
                    dto.stdout?.takeIf { it.isNotBlank() }?.let { append(it) }
                    dto.stderr?.takeIf { it.isNotBlank() }?.let {
                        if (isNotEmpty()) append("\n")
                        append("[stderr]\n").append(it)
                    }
                    dto.exitCode?.let { append("\n[exit ").append(it).append("]") }
                }.ifBlank { "(no output)" }
            }
            _uiState.value = _uiState.value.copy(
                isRuntimeBusy = false,
                terminalOutput = output ?: result.exceptionOrNull()?.message ?: "Command failed"
            )
        }
    }

    // ---- files ------------------------------------------------------------

    fun loadFiles(path: String? = null) {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = runtimeRepository.listFiles(c.id, path)
            result.getOrNull()?.let {
                _uiState.value = _uiState.value.copy(runtimeFiles = it, filesPath = path)
            }
            if (result.isFailure) notify(result.exceptionOrNull()?.message)
        }
    }

    // ---- git ---------------------------------------------------------------

    fun loadGitDiff(path: String?) {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = runtimeRepository.gitDiff(c.id, path)
            _uiState.value = _uiState.value.copy(gitDiff = result.getOrNull())
            if (result.isFailure) notify(result.exceptionOrNull()?.message)
        }
    }

    fun loadGitCommits() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = runtimeRepository.gitCommits(c.id)
            result.getOrNull()?.let {
                _uiState.value = _uiState.value.copy(gitCommits = it.commits.orEmpty())
            }
            if (result.isFailure) notify(result.exceptionOrNull()?.message)
        }
    }

    /** Prefers the runtime git API, falling back to the cloud proxy. */
    fun refreshGitChangesPreferRuntime() {
        val c = selectedConversation() ?: return
        if (c.hasRuntimeAccess && c.isSandboxRunning) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoadingChanges = true)
                val result = runtimeRepository.gitChanges(c.id)
                _uiState.value = _uiState.value.copy(isLoadingChanges = false)
                if (result.isSuccess) {
                    _gitChanges.value = result.getOrDefault(emptyList())
                } else {
                    refreshGitChanges(c.id)
                }
            }
        } else {
            refreshGitChanges(c.id)
        }
    }

    // ---- settings catalogs -------------------------------------------------

    fun loadSecrets() {
        viewModelScope.launch {
            repository.searchSecrets().getOrNull()?.let {
                _uiState.value = _uiState.value.copy(secrets = it)
            }
        }
    }

    fun createSecret(name: String, value: String, description: String?) {
        viewModelScope.launch {
            val result = repository.createSecret(name, value, description)
            notify(if (result.isSuccess) "Secret saved" else result.exceptionOrNull()?.message)
            if (result.isSuccess) loadSecrets()
        }
    }

    fun deleteSecret(secretId: String) {
        viewModelScope.launch {
            val result = repository.deleteSecret(secretId)
            notify(if (result.isSuccess) "Secret deleted" else result.exceptionOrNull()?.message)
            if (result.isSuccess) loadSecrets()
        }
    }

    fun loadSkills() {
        viewModelScope.launch {
            repository.searchSkills().getOrNull()?.let {
                _uiState.value = _uiState.value.copy(skills = it)
            }
        }
    }

    fun loadSandboxes() {
        viewModelScope.launch {
            repository.searchSandboxes().getOrNull()?.let {
                _uiState.value = _uiState.value.copy(sandboxes = it)
            }
        }
    }

    fun deleteSandbox(sandboxId: String) {
        viewModelScope.launch {
            val result = repository.deleteSandbox(sandboxId)
            notify(if (result.isSuccess) "Sandbox deleted" else result.exceptionOrNull()?.message)
            if (result.isSuccess) loadSandboxes()
        }
    }

    fun loadGitAccount() {
        viewModelScope.launch {
            repository.getGitInfo().getOrNull()?.let {
                _uiState.value = _uiState.value.copy(gitLogin = it.login)
            }
        }
    }

    // ---- conversation metadata --------------------------------------------

    fun renameConversation(conversationId: String, title: String) {
        viewModelScope.launch {
            val result = repository.updateConversation(conversationId, title = title)
            notify(if (result.isSuccess) "Renamed conversation" else result.exceptionOrNull()?.message)
        }
    }

    // ---- new-task options --------------------------------------------------

    fun setAgentTypeSelection(kind: String) {
        _uiState.value = _uiState.value.copy(agentTypeSelection = kind)
    }

    fun setLlmProfileSelection(profile: String?) {
        _uiState.value = _uiState.value.copy(llmProfileSelection = profile)
    }

    fun setAgentProfileSelection(profileId: String?) {
        _uiState.value = _uiState.value.copy(agentProfileSelection = profileId)
    }

    fun setSelectedModelForTask(model: String?) {
        _uiState.value = _uiState.value.copy(selectedModelForTask = model)
    }

    /** Loads the agent profiles (ACP backends like claude-code, codex, ...). */
    fun loadAgentProfiles() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            runtimeRepository.agentProfiles(c.id).getOrNull()?.let {
                _uiState.value = _uiState.value.copy(agentProfiles = it)
            }
        }
    }

    /** Activates a different agent profile inside the running sandbox. */
    fun activateAgentProfile(profileId: String) {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRuntimeBusy = true)
            val result = runtimeRepository.activateAgentProfile(c.id, profileId)
            _uiState.value = _uiState.value.copy(isRuntimeBusy = false)
            notify(if (result.isSuccess) "Agent profile activated" else result.exceptionOrNull()?.message)
            if (result.isSuccess) {
                refreshConversationStatus(c.id)
                loadConversationInfo()
            }
        }
    }

    /** Clears per-conversation runtime state when leaving the detail screen. */
    private fun resetRuntimeState() {
        _uiState.value = _uiState.value.copy(
            terminalOutput = null,
            terminalCommand = "",
            runtimeFiles = emptyList(),
            filesPath = null,
            gitCommits = emptyList(),
            gitDiff = null,
            conversationInfo = null,
            availableEfforts = emptyList(),
            availableAcpModels = emptyList(),
            runtimeInfo = null,
            agentFinalResponse = null,
            vscodeUrl = null,
            bashEvents = emptyList(),
            installedSkills = emptyList(),
            installedPlugins = emptyList(),
            toolCatalog = emptyList(),
            runtimeProviders = emptyList(),
            runtimeModels = emptyList(),
            lastToolOutput = null,
        )
    }

    // ---------------------------------------------------------------------
    // Cloud settings / profiles / git / web client
    // ---------------------------------------------------------------------

    fun loadUserSettings() {
        viewModelScope.launch {
            repository.getSettings().getOrNull()?.let {
                _uiState.value = _uiState.value.copy(userSettings = it)
            }
        }
    }

    fun saveUserSettings(request: UpdateUserSettingsRequest) {
        viewModelScope.launch {
            val result = repository.updateSettings(request)
            notify(if (result.isSuccess) "Settings saved" else result.exceptionOrNull()?.message)
            if (result.isSuccess) loadUserSettings()
        }
    }

    fun loadGitOrganizations() {
        viewModelScope.launch {
            repository.getGitOrganizations().getOrNull()?.let {
                _uiState.value = _uiState.value.copy(gitOrganizations = it.organizations.orEmpty())
            }
        }
    }

    fun loadGitInstallations(provider: String) {
        viewModelScope.launch {
            repository.searchGitInstallations(provider).getOrNull()?.let {
                _uiState.value = _uiState.value.copy(gitInstallations = it)
            }
        }
    }

    fun loadWebClientConfig() {
        viewModelScope.launch {
            repository.getWebClientConfig().getOrNull()?.let {
                _uiState.value = _uiState.value.copy(webClientConfig = it)
            }
        }
    }

    fun loadSchemas() {
        viewModelScope.launch {
            val agent = repository.getAgentSchema().getOrNull()
            val conv = repository.getConversationSchema().getOrNull()
            _uiState.value = _uiState.value.copy(agentSchema = agent, conversationSchema = conv)
        }
    }

    fun loadProfileDetail(name: String) {
        viewModelScope.launch {
            repository.getProfile(name).getOrNull()?.let {
                _uiState.value = _uiState.value.copy(selectedProfileDetail = it)
            }
        }
    }

    fun saveProfile(name: String, model: String?, baseUrl: String?) {
        viewModelScope.launch {
            val result = repository.saveProfile(
                name, ProfileConfigDto(model = model, baseUrl = baseUrl), includeSecrets = false
            )
            notify(if (result.isSuccess) "Profile '$name' saved" else result.exceptionOrNull()?.message)
            if (result.isSuccess) loadProfiles()
        }
    }

    fun deleteProfile(name: String) {
        viewModelScope.launch {
            val result = repository.deleteProfile(name)
            notify(if (result.isSuccess) "Profile deleted" else result.exceptionOrNull()?.message)
            if (result.isSuccess) loadProfiles()
        }
    }

    fun activateCloudProfile(name: String) {
        viewModelScope.launch {
            val result = repository.activateProfile(name)
            notify(if (result.isSuccess) "Active profile: $name" else result.exceptionOrNull()?.message)
            if (result.isSuccess) loadProfiles()
        }
    }

    fun renameCloudProfile(name: String, newName: String) {
        viewModelScope.launch {
            val result = repository.renameProfile(name, newName)
            notify(if (result.isSuccess) "Profile renamed" else result.exceptionOrNull()?.message)
            if (result.isSuccess) loadProfiles()
        }
    }

    fun storeGitToken(provider: String, token: String) {
        viewModelScope.launch {
            val result = repository.storeGitProviderTokens(provider, token)
            notify(result.getOrElse { it.message ?: "Failed to store token" })
        }
    }

    fun unlinkGitProvider(provider: String) {
        viewModelScope.launch {
            val result = repository.unlinkGitProvider(provider)
            notify(if (result.isSuccess) "Unlinked $provider" else result.exceptionOrNull()?.message)
        }
    }

    fun callMcpTool(serverName: String, toolName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRuntimeBusy = true)
            val result = repository.callMcpTool(serverName, toolName, null)
            _uiState.value = _uiState.value.copy(
                isRuntimeBusy = false,
                lastToolOutput = result.getOrElse { it.message ?: "MCP call failed" }
            )
        }
    }

    fun createSandbox(sandboxSpecId: String) {
        viewModelScope.launch {
            val result = repository.createSandbox(sandboxSpecId)
            notify(if (result.isSuccess) "Sandbox created" else result.exceptionOrNull()?.message)
            if (result.isSuccess) loadSandboxes()
        }
    }

    // ---------------------------------------------------------------------
    // Runtime extras — goal, confirmation, fork, final response, vscode
    // ---------------------------------------------------------------------

    fun startGoal(goal: String) {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = runtimeRepository.startGoal(c.id, goal)
            notify(if (result.isSuccess) "Goal started" else result.exceptionOrNull()?.message)
        }
    }

    fun resumeGoal() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = runtimeRepository.resumeGoal(c.id)
            notify(if (result.isSuccess) "Goal resumed" else result.exceptionOrNull()?.message)
        }
    }

    fun stopGoal() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = runtimeRepository.stopGoal(c.id)
            notify(if (result.isSuccess) "Goal stopped" else result.exceptionOrNull()?.message)
        }
    }

    fun respondToConfirmation(accept: Boolean) {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = runtimeRepository.respondToConfirmation(c.id, accept)
            notify(if (result.isSuccess) (if (accept) "Confirmed" else "Rejected") else result.exceptionOrNull()?.message)
            if (result.isSuccess) refreshConversationStatus(c.id)
        }
    }

    fun setConfirmationPolicy(policy: String) {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = runtimeRepository.setConfirmationPolicy(c.id, policy)
            notify(if (result.isSuccess) "Confirmation policy: $policy" else result.exceptionOrNull()?.message)
        }
    }

    fun forkConversation() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = runtimeRepository.forkConversation(c.id)
            notify(if (result.isSuccess) "Fork created" else result.exceptionOrNull()?.message)
        }
    }

    fun loadAgentFinalResponse() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            runtimeRepository.agentFinalResponse(c.id).getOrNull()?.let {
                _uiState.value = _uiState.value.copy(agentFinalResponse = it.response)
            }
        }
    }

    fun loadRuntimeInfo() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            runtimeRepository.runtimeInfo(c.id).getOrNull()?.let {
                _uiState.value = _uiState.value.copy(runtimeInfo = it)
            }
        }
    }

    fun reprovisionRuntime() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRuntimeBusy = true)
            val result = runtimeRepository.reprovisionRuntime(c.id)
            _uiState.value = _uiState.value.copy(isRuntimeBusy = false)
            notify(if (result.isSuccess) "Runtime reprovisioned" else result.exceptionOrNull()?.message)
        }
    }

    fun setSecurityAnalyzer(enabled: Boolean) {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = runtimeRepository.setSecurityAnalyzer(c.id, enabled)
            notify(if (result.isSuccess) "Security analyzer ${if (enabled) "enabled" else "disabled"}" else result.exceptionOrNull()?.message)
        }
    }

    fun loadVSCodeUrl() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = runtimeRepository.vscodeUrl(c.id)
            result.getOrNull()?.let { dto ->
                _uiState.value = _uiState.value.copy(vscodeUrl = dto.url ?: dto.vscodeUrl)
            }
            if (result.isFailure) notify(result.exceptionOrNull()?.message)
        }
    }

    fun loadBashEvents() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            runtimeRepository.bashEvents(c.id).getOrNull()?.let {
                _uiState.value = _uiState.value.copy(bashEvents = it.items.orEmpty())
            }
        }
    }

    // ---------------------------------------------------------------------
    // Runtime skills / plugins / tools / settings
    // ---------------------------------------------------------------------

    fun loadRuntimeSkills() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            runtimeRepository.installedSkills(c.id).getOrNull()?.let {
                _uiState.value = _uiState.value.copy(installedSkills = it)
            }
        }
    }

    fun installRuntimeSkill(name: String) {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = runtimeRepository.installSkill(c.id, name)
            notify(if (result.isSuccess) "Skill installed" else result.exceptionOrNull()?.message)
            if (result.isSuccess) loadRuntimeSkills()
        }
    }

    fun syncRuntimeSkills() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = runtimeRepository.syncSkills(c.id)
            notify(if (result.isSuccess) "Skills synced" else result.exceptionOrNull()?.message)
            if (result.isSuccess) loadRuntimeSkills()
        }
    }

    fun loadRuntimePlugins() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            runtimeRepository.installedPlugins(c.id).getOrNull()?.let {
                _uiState.value = _uiState.value.copy(installedPlugins = it)
            }
        }
    }

    fun installRuntimePlugin(name: String) {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = runtimeRepository.installPlugin(c.id, name)
            notify(if (result.isSuccess) "Plugin installed" else result.exceptionOrNull()?.message)
            if (result.isSuccess) loadRuntimePlugins()
        }
    }

    fun loadToolCatalog() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            runtimeRepository.toolCatalog(c.id).getOrNull()?.let {
                _uiState.value = _uiState.value.copy(toolCatalog = it)
            }
        }
    }

    fun loadRuntimeProviders() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            runtimeRepository.runtimeProviders(c.id).getOrNull()?.let {
                _uiState.value = _uiState.value.copy(runtimeProviders = it)
            }
        }
    }

    fun loadRuntimeModels() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            runtimeRepository.runtimeModels(c.id).getOrNull()?.let {
                _uiState.value = _uiState.value.copy(runtimeModels = it)
            }
        }
    }

    fun downloadConversationTrajectory(target: java.io.File) {
        val id = _uiState.value.selectedConversationId ?: return
        viewModelScope.launch {
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    java.io.FileOutputStream(target).use { out ->
                        repository.downloadConversation(id, out)
                    }
                } catch (e: Exception) {
                    Result.failure<Long>(e)
                }
            }
            notify(
                if (result.isSuccess) "Trajectory saved to ${target.name}"
                else result.exceptionOrNull()?.message
            )
        }
    }

    fun downloadSandboxFile(path: String, target: java.io.File) {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    java.io.FileOutputStream(target).use { out ->
                        runtimeRepository.downloadFile(c.id, path, out)
                    }
                } catch (e: Exception) {
                    Result.failure<Long>(e)
                }
            }
            notify(
                if (result.isSuccess) "Saved ${target.name}"
                else result.exceptionOrNull()?.message
            )
        }
    }

    fun loadSubAgents() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            runtimeRepository.listSubAgents(c.id).getOrNull()?.let {
                _uiState.value = _uiState.value.copy(subAgents = it)
            }
        }
    }

    fun startMcpOAuth(name: String) {
        viewModelScope.launch {
            val result = repository.startMcpOAuth(name)
            val dto = result.getOrNull()
            _uiState.value = _uiState.value.copy(
                mcpOAuthUrl = dto?.authorizationUrl,
                mcpOAuthJobId = dto?.jobId,
                notificationMessage = result.exceptionOrNull()?.message
                    ?: dto?.error
                    ?: "MCP OAuth started — open the authorization URL"
            )
        }
    }

    fun pollMcpOAuthStatus() {
        val jobId = _uiState.value.mcpOAuthJobId ?: return
        viewModelScope.launch {
            val result = repository.getMcpOAuthStatus(jobId)
            val dto = result.getOrNull()
            _uiState.value = _uiState.value.copy(
                mcpOAuthStatus = dto?.status,
                notificationMessage = result.exceptionOrNull()?.message
                    ?: dto?.error
                    ?: "MCP OAuth status: ${dto?.status ?: "unknown"}"
            )
        }
    }

    fun checkMarketplace(name: String, source: String) {
        viewModelScope.launch {
            val result = repository.checkMarketplace(name, source)
            val dto = result.getOrNull()
            notify(
                result.exceptionOrNull()?.message
                    ?: dto?.let { "Marketplace ${it.status}: ${it.skillCount} skills, ${it.pluginCount} plugins" }
            )
        }
    }

    fun previewMarketplace(name: String, source: String) {
        viewModelScope.launch {
            val result = repository.previewMarketplaceSkills(name, source)
            val dto = result.getOrNull()
            _uiState.value = _uiState.value.copy(
                marketplaceSkills = dto?.skills.orEmpty(),
                marketplacePlugins = dto?.plugins.orEmpty(),
                notificationMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun loadCloudSkillCatalog() {
        viewModelScope.launch {
            repository.searchSkillsCatalog().getOrNull()?.let {
                _uiState.value = _uiState.value.copy(cloudSkillCatalog = it)
            }
        }
    }

    fun callRuntimeBash(conversationId: String, command: String) {
        viewModelScope.launch {
            val result = runtimeRepository.startBash(conversationId, command)
            notify(
                result.exceptionOrNull()?.message
                    ?: "Started bash command ${result.getOrNull()?.id ?: ""}"
            )
        }
    }

    /** Spawns a sub-conversation (child) of the open conversation. */
    fun startSubConversation(task: String, title: String? = null) {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRuntimeBusy = true)
            val result = runtimeRepository.startChildConversation(c.id, task, title)
            _uiState.value = _uiState.value.copy(isRuntimeBusy = false)
            notify(
                result.exceptionOrNull()?.message
                    ?: "Sub-conversation started (${result.getOrNull()?.id?.take(8) ?: "?"})"
            )
        }
    }

    fun loadChildConversations() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            runtimeRepository.children(c.id).getOrNull()?.let {
                _uiState.value = _uiState.value.copy(childConversations = it)
            }
        }
    }

    fun loadWorkspacePath() {
        val c = selectedConversation() ?: return
        viewModelScope.launch {
            val result = runtimeRepository.workspace(c.id)
            _uiState.value = _uiState.value.copy(
                workspaceInfo = result.getOrNull(),
                notificationMessage = result.exceptionOrNull()?.message
            )
        }
    }

    private companion object {
        val TERMINAL_STATUSES = setOf(
            ConversationStatus.FINISHED,
            ConversationStatus.ERROR
        )
    }
}
