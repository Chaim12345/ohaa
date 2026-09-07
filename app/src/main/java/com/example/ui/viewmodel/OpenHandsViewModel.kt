package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AgentEvent
import com.example.data.model.ConversationStatus
import com.example.data.model.OpenHandsConversation
import com.example.data.model.PromptTemplate
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
    SETTINGS("Cloud API")
}

data class OpenHandsUiState(
    val currentTab: AppNavTab = AppNavTab.DASHBOARD,
    val selectedConversationId: String? = null,
    val searchQuery: String = "",
    val filterStatus: ConversationStatus? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSendingMessage: Boolean = false,
    val connectionStatus: String? = null,
    val connectionHealthy: Boolean? = null,
    val notificationMessage: String? = null,
    val newTaskPrompt: String = "",
    val newTaskRepo: String = "",
    val selectedTemplate: PromptTemplate? = null,
    val apiKey: String = "",
    val baseUrl: String = "https://app.all-hands.dev"
)

class OpenHandsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = OpenHandsRepository(application, database)

    private val _uiState = MutableStateFlow(
        OpenHandsUiState(
            apiKey = repository.apiKey,
            baseUrl = repository.baseUrl
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

    private var eventObservationJob: Job? = null
    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    fun setTab(tab: AppNavTab) {
        _uiState.value = _uiState.value.copy(
            currentTab = tab,
            selectedConversationId = null
        )
    }

    fun selectConversation(id: String) {
        _uiState.value = _uiState.value.copy(selectedConversationId = id)
        observeConversationEvents(id)
    }

    fun closeConversationDetail() {
        _uiState.value = _uiState.value.copy(selectedConversationId = null)
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
            val result = repository.createConversation(prompt, repo)
            _uiState.value = _uiState.value.copy(isLoading = false)

            if (result.isSuccess) {
                val convId = result.getOrThrow()
                _uiState.value = _uiState.value.copy(
                    newTaskPrompt = "",
                    newTaskRepo = "",
                    selectedTemplate = null,
                    notificationMessage = "Task launched in OpenHands sandbox!"
                )
                selectConversation(convId)
                onSuccess(convId)
            } else {
                _uiState.value = _uiState.value.copy(
                    notificationMessage = result.exceptionOrNull()?.message ?: "Failed to start conversation"
                )
            }
        }
    }

    fun sendFollowUpMessage(conversationId: String, message: String) {
        if (message.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSendingMessage = true)
            repository.sendMessage(conversationId, message)
            _uiState.value = _uiState.value.copy(isSendingMessage = false)
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
        viewModelScope.launch {
            repository.refreshEvents(conversationId)
        }

        // Auto polling for live conversation
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(8000)
                if (_uiState.value.selectedConversationId == conversationId) {
                    repository.refreshEvents(conversationId)
                } else {
                    break
                }
            }
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
        }
    }
}
