package com.example.web

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import com.example.data.api.OpenHandsApiClient
import com.example.data.settings.AgentProfile
import com.example.ui.screens.ConversationScreen
import com.example.ui.screens.ConversationsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NewTaskScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.LaunchStage
import com.example.ui.viewmodel.SandboxUiState

/**
 * Entry point for the browser build.
 *
 * The wasm bundle is served by :server, which also proxies the Cloud API, so
 * every call is same-origin. A server-side key (when configured) is used
 * automatically; otherwise the user supplies one in Settings.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport("composeApp") {
        WebRoot()
    }
}

@Composable
fun WebRoot() {
    MyApplicationTheme {
        val api = remember {
            OpenHandsApiClient(
                apiBaseUrl = "/cloud",
                apiKey = { BrowserSession.apiKey() }
            )
        }
        val state = rememberWebAppState(api)

        LaunchedEffect(Unit) {
            BrowserSession.load()
            state.refresh()
            state.loadTemplates()
        }

        AppShell(state)
    }
}

@Composable
private fun AppShell(state: WebAppState) {
    if (state.selectedConversationId != null) {
        ConversationHost(state)
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = state.tab == tab,
                        onClick = { state.tab = tab },
                        icon = {},
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (state.tab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    conversations = state.conversations,
                    templates = state.templates,
                    connectionStatus = state.connectionStatus,
                    connectionHealthy = state.connectionHealthy,
                    isDemoMode = false,
                    isRefreshing = state.isRefreshing,
                    onStartNewTask = { state.tab = AppTab.NEW_TASK },
                    onSelectConversation = state::openConversation,
                    onSelectTemplate = state::selectTemplate,
                    onRefresh = state::refresh,
                    onOpenSettings = { state.tab = AppTab.SETTINGS }
                )

                AppTab.CONVERSATIONS -> ConversationsScreen(
                    conversations = state.conversations,
                    searchQuery = state.searchQuery,
                    statusFilter = state.statusFilter,
                    isRefreshing = state.isRefreshing,
                    onSearchChanged = state::search,
                    onStatusFilterChanged = state::filterByStatus,
                    onRefresh = state::refresh,
                    onSelectConversation = state::openConversation,
                    onStartNewTask = { state.tab = AppTab.NEW_TASK }
                )

                AppTab.NEW_TASK -> NewTaskScreen(
                    promptText = state.promptText,
                    repoText = state.repoText,
                    templates = state.templates,
                    isLaunching = state.launchStage == LaunchStage.SUBMITTING ||
                        state.launchStage == LaunchStage.WAITING_SANDBOX ||
                        state.launchStage == LaunchStage.PREPARING_REPO ||
                        state.launchStage == LaunchStage.SETTING_UP_SKILLS,
                    launchStage = state.launchStage,
                    launchStatusText = state.launchStatusText,
                    isDemoMode = false,
                    activeProfile = state.activeProfile,
                    repoResults = state.repoResults,
                    branchResults = emptyList(),
                    selectedBranch = "",
                    isSearchingRepos = state.isSearchingRepos,
                    onPromptChanged = { state.promptText = it },
                    onRepoChanged = state::searchRepositories,
                    onSelectTemplate = state::selectTemplate,
                    onLaunchTask = state::launchTask,
                    onSearchRepositories = { state.searchRepositories(state.repoText) },
                    onSelectRepository = { repo ->
                        state.repoText = repo.fullName
                        state.repoResults = emptyList()
                    },
                    onSelectBranch = {},
                    onModelChanged = { state.activeProfile = state.activeProfile.copy(model = it) },
                    suggestedTasks = state.suggestedTasks,
                    onLoadSuggestedTasks = state::loadTemplates,
                    onSelectSuggestedTask = state::applySuggestedTask
                )

                AppTab.SETTINGS -> SettingsScreen(
                    apiKey = BrowserSession.apiKey(),
                    baseUrl = BrowserSession.upstream(),
                    isLoading = state.isRefreshing,
                    healthMessage = state.connectionStatus,
                    healthOk = state.connectionHealthy,
                    credits = state.credits,
                    isDemoMode = false,
                    activeProfile = state.activeProfile,
                    account = state.account,
                    onApiKeyChanged = { BrowserSession.setApiKey(it) },
                    onBaseUrlChanged = {},
                    onSaveConfig = {
                        state.refresh()
                        state.loadTemplates()
                    },
                    onTestConnection = state::refresh,
                    onClearApiKey = {
                        BrowserSession.setApiKey("")
                        state.refresh()
                    },
                    onClearCache = {},
                    onModelChanged = { state.activeProfile = state.activeProfile.copy(model = it) },
                    onRefreshCloudSettings = state::refresh
                )
            }

            state.lastError?.let { error ->
                ErrorBanner(error, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun ConversationHost(state: WebAppState) {
    ConversationScreen(
        conversation = state.selectedConversation,
        events = state.events,
        isSending = state.isSending,
        activeProfile = state.activeProfile,
        gitDiff = state.gitDiff,
        isGitLoading = state.isGitLoading,
        onBack = state::closeConversation,
        onRefresh = state::refreshEvents,
        onSendMessage = state::sendMessage,
        onDelete = { state.selectedConversationId?.let(state::deleteConversation) },
        onRefreshGit = state::loadGitChanges,
        onModelChanged = { state.activeProfile = state.activeProfile.copy(model = it) },
        onResetProfile = { state.activeProfile = AgentProfile() },
        webBaseUrl = BrowserSession.upstream(),
        sandboxStatus = SandboxUiState(
            isWorking = state.isRefreshing,
            message = state.selectedConversation?.sandboxStatus
        ),
        onPauseSandbox = { sandboxId -> state.pauseSandbox(sandboxId) },
        onResumeSandbox = { sandboxId -> state.resumeSandbox(sandboxId) },
        onDeleteSandbox = { sandboxId -> state.deleteSandbox(sandboxId) },
        files = state.files,
        isFilesLoading = state.isFilesLoading,
        fileContents = state.fileContents,
        onLoadFiles = { state.loadFiles() },
        onReadFile = state::readFile,
        skills = state.skills,
        onLoadSkills = state::loadSkills
    )
}

@Composable
private fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(12.dp),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Center
        )
    }
}
