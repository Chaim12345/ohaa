package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ChangesScreen
import com.example.ui.screens.ConversationDetailScreen
import com.example.ui.screens.ConversationsScreen
import com.example.ui.screens.FilesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NewConversationScreen
import com.example.ui.screens.SandboxesScreen
import com.example.ui.screens.SecretsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OpenHandsPrimaryDark
import com.example.ui.viewmodel.HomeTab
import com.example.ui.viewmodel.OpenHandsViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MyApplicationTheme { OpenHandsApp() } }
    }
}

@Composable
fun OpenHandsApp(viewModel: OpenHandsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val events by viewModel.events.collectAsState()
    val selectedConversation by viewModel.selectedConversation.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.notification) {
        uiState.notification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissNotification()
        }
    }

    BackHandler(enabled = uiState.screenStack.isNotEmpty()) { viewModel.goBack() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (uiState.screenStack.isEmpty()) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, modifier = Modifier.testTag("bottom_nav_bar")) {
                    val tabs = listOf(
                        Triple(HomeTab.HOME, Icons.Default.Home, "Home"),
                        Triple(HomeTab.CONVERSATIONS, Icons.AutoMirrored.Filled.FormatListBulleted, "Conversations"),
                        Triple(HomeTab.NEW, Icons.Default.AddCircle, "New"),
                        Triple(HomeTab.SETTINGS, Icons.Default.Settings, "Settings")
                    )
                    tabs.forEach { (tab, icon, title) ->
                        NavigationBarItem(
                            selected = uiState.currentTab == tab,
                            onClick = { viewModel.setTab(tab) },
                            icon = { Icon(imageVector = icon, contentDescription = title) },
                            label = { Text(title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = OpenHandsPrimaryDark,
                                selectedTextColor = OpenHandsPrimaryDark,
                                indicatorColor = OpenHandsPrimaryDark.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val screen = uiState.topScreen) {
                is Screen.ConversationDetail -> ConversationDetailScreen(
                    conversation = selectedConversation,
                    events = events,
                    isSyncing = uiState.isSyncingEvents,
                    isSending = uiState.isSendingMessage,
                    isMutating = uiState.isMutatingConversation,
                    showNoise = uiState.showNoiseEvents,
                    error = uiState.detailError,
                    onBack = { viewModel.goBack() },
                    onRefresh = { viewModel.refreshDetail() },
                    onSend = { viewModel.sendMessage(it) },
                    onPauseSandbox = { viewModel.pauseSandbox(it) },
                    onResumeSandbox = { viewModel.resumeSandbox(it) },
                    onRename = { viewModel.renameConversation(screen.conversationId, it) },
                    onDelete = { viewModel.deleteConversation(screen.conversationId) },
                    onOpenFiles = { viewModel.openFiles(screen.conversationId) },
                    onOpenChanges = { viewModel.openChanges(screen.conversationId) },
                    onToggleNoise = { viewModel.toggleNoiseEvents() },
                    llmProfiles = uiState.llmProfiles,
                    onLoadProfiles = { viewModel.loadLlmProfiles() },
                    onSwitchProfile = { viewModel.switchConversationProfile(screen.conversationId, it) },
                    skills = uiState.conversationSkills,
                    onLoadSkills = { viewModel.loadConversationSkills(screen.conversationId) },
                    onDismissSkills = { viewModel.clearConversationSkills() }
                )
                is Screen.Files -> FilesScreen(
                    conversation = selectedConversation,
                    state = uiState.workspace,
                    onBack = { viewModel.goBack() },
                    onBrowse = { viewModel.browseFiles(screen.conversationId, it) },
                    onOpenFile = { viewModel.openFile(screen.conversationId, it) },
                    onCloseFile = { viewModel.closeFile() }
                )
                is Screen.Changes -> ChangesScreen(
                    conversation = selectedConversation,
                    state = uiState.workspace,
                    onBack = { viewModel.goBack() },
                    onRepoRootChange = { viewModel.setRepoRoot(it) },
                    onReload = { viewModel.loadChanges(screen.conversationId) },
                    onOpenDiff = { viewModel.openDiff(screen.conversationId, it) },
                    onCloseDiff = { viewModel.closeDiff() }
                )
                Screen.Secrets -> SecretsScreen(
                    secrets = uiState.secrets,
                    isLoading = uiState.isLoadingSecrets,
                    onBack = { viewModel.goBack() },
                    onRefresh = { viewModel.loadSecrets() },
                    onCreate = { n, v, d -> viewModel.createSecret(n, v, d) },
                    onDelete = { viewModel.deleteSecret(it) }
                )
                Screen.Sandboxes -> SandboxesScreen(
                    sandboxes = uiState.sandboxes,
                    conversations = conversations,
                    isLoading = uiState.isLoadingSandboxes,
                    isMutating = uiState.isMutatingConversation,
                    onBack = { viewModel.goBack() },
                    onRefresh = { viewModel.loadSandboxes() },
                    onPause = { viewModel.pauseSandbox(it) },
                    onResume = { viewModel.resumeSandbox(it) },
                    onDelete = { viewModel.deleteSandbox(it) },
                    onOpenConversation = { viewModel.openConversation(it) }
                )
                null -> when (uiState.currentTab) {
                    HomeTab.HOME -> HomeScreen(
                        conversations = conversations,
                        templates = templates,
                        account = uiState.account,
                        accountError = uiState.accountError,
                        needsApiKey = uiState.needsApiKey,
                        isRefreshing = uiState.isRefreshing,
                        listError = uiState.listError,
                        onNewConversation = { viewModel.setTab(HomeTab.NEW) },
                        onOpenConversation = { viewModel.openConversation(it) },
                        onOpenAllConversations = { viewModel.setTab(HomeTab.CONVERSATIONS) },
                        onSelectTemplate = { viewModel.applyTemplate(it) },
                        onOpenSandboxes = { viewModel.openSandboxes() },
                        onRefresh = { viewModel.refreshConversations(); viewModel.loadAccount() },
                        onOpenSettings = { viewModel.setTab(HomeTab.SETTINGS) }
                    )
                    HomeTab.CONVERSATIONS -> ConversationsScreen(
                        conversations = conversations,
                        searchQuery = uiState.searchQuery,
                        statusFilter = uiState.statusFilter,
                        isRefreshing = uiState.isRefreshing,
                        isLoadingMore = uiState.isLoadingMore,
                        hasMore = uiState.hasMoreConversations,
                        error = uiState.listError,
                        lastSyncedAt = uiState.lastSyncedAt,
                        onSearchChanged = { viewModel.updateSearchQuery(it) },
                        onStatusFilterChanged = { viewModel.setStatusFilter(it) },
                        onOpen = { viewModel.openConversation(it) },
                        onRefresh = { viewModel.refreshConversations() },
                        onLoadMore = { viewModel.loadMoreConversations() },
                        onRename = { id, title -> viewModel.renameConversation(id, title) },
                        onDelete = { viewModel.deleteConversation(it) },
                        onNew = { viewModel.setTab(HomeTab.NEW) }
                    )
                    HomeTab.NEW -> NewConversationScreen(
                        form = uiState.form,
                        templates = templates,
                        startProgress = uiState.startProgress,
                        hasApiKey = !uiState.needsApiKey,
                        onFormChange = { viewModel.updateForm(it) },
                        onRepoQueryChange = { viewModel.searchRepositories(it) },
                        onRepoProviderChange = { viewModel.setRepoProvider(it) },
                        onSelectRepository = { viewModel.selectRepository(it) },
                        onSearchBranches = { q -> viewModel.loadBranches(uiState.form.repository, uiState.form.gitProvider, q) },
                        onSelectTemplate = { viewModel.applyTemplate(it) },
                        onSelectSuggestedTask = { viewModel.applySuggestedTask(it) },
                        onLoadSuggestedTasks = { viewModel.loadSuggestedTasks() },
                        onStart = { viewModel.startConversation() },
                        onCancelStart = { viewModel.cancelStart() },
                        onDismissStartError = { viewModel.dismissStartError() },
                        onOpenSettings = { viewModel.setTab(HomeTab.SETTINGS) }
                    )
                    HomeTab.SETTINGS -> SettingsScreen(
                        apiKey = uiState.apiKeyInput,
                        baseUrl = uiState.baseUrlInput,
                        isUsingBuildTimeKey = uiState.isUsingBuildTimeKey,
                        account = uiState.account,
                        accountError = uiState.accountError,
                        isTesting = uiState.isTestingConnection,
                        showNoiseEvents = uiState.showNoiseEvents,
                        onApiKeyChanged = { viewModel.updateApiKeyInput(it) },
                        onBaseUrlChanged = { viewModel.updateBaseUrlInput(it) },
                        onSave = { viewModel.saveSettings() },
                        onTestConnection = { viewModel.loadAccount() },
                        onToggleNoise = { viewModel.toggleNoiseEvents() },
                        onClearCache = { viewModel.clearLocalCache() },
                        onOpenSandboxes = { viewModel.openSandboxes() },
                        onOpenSecrets = { viewModel.openSecrets() },
                        llmProfiles = uiState.llmProfiles,
                        onLoadProfiles = { viewModel.loadLlmProfiles() },
                        onActivateProfile = { viewModel.activateLlmProfile(it) }
                    )
                }
            }
        }
    }
}
