package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dashboard
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ConversationDetailScreen
import com.example.ui.screens.ConversationsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NewTaskScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.CanvasPrimary
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.OpenHandsViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    // Native splash screen (Android 12+; compat on older versions).
    installSplashScreen()
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        OpenHandsApp()
      }
    }
  }
}

/** Writes text to the cache dir and hands it to the system share sheet. */
private fun shareText(context: android.content.Context, filename: String, content: String) {
  try {
    val dir = java.io.File(context.cacheDir, "exports").apply { mkdirs() }
    val file = java.io.File(dir, filename)
    file.writeText(content)
    val uri = androidx.core.content.FileProvider.getUriForFile(
      context,
      "${context.packageName}.fileprovider",
      file
    )
    val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
      type = "application/json"
      putExtra(android.content.Intent.EXTRA_STREAM, uri)
      putExtra(android.content.Intent.EXTRA_TITLE, filename)
      addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(
      android.content.Intent.createChooser(share, "Export trajectory")
        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    )
  } catch (e: Exception) {
    android.widget.Toast.makeText(
      context,
      "Could not export: ${e.message ?: "unknown error"}",
      android.widget.Toast.LENGTH_LONG
    ).show()
  }
}

@Composable
fun OpenHandsApp(viewModel: OpenHandsViewModel = viewModel()) {
  val uiState by viewModel.uiState.collectAsState()
  val conversations by viewModel.conversations.collectAsState()
  val templates by viewModel.templates.collectAsState()
  val events by viewModel.detailEvents.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current

  LaunchedEffect(uiState.notification) {
    uiState.notification?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.dismissNotification()
    }
  }

  val selectedConversation = remember(uiState.detailConversationId, conversations) {
    conversations.find { it.id == uiState.detailConversationId }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize(),
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        // Detail is pushed over the tabs — the nav bar hides like a real stack.
        if (uiState.detailConversationId == null) {
          NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              modifier = Modifier.testTag("bottom_nav_bar")
          ) {
            listOf(
                Triple(AppTab.DASHBOARD, Icons.Default.Dashboard, "Dashboard"),
                Triple(AppTab.CONVERSATIONS, Icons.AutoMirrored.Filled.FormatListBulleted, "Tasks"),
                Triple(AppTab.NEW_TASK, Icons.Default.AddCircle, "New Task"),
                Triple(AppTab.SETTINGS, Icons.Default.Cloud, "Cloud API")
            ).forEach { (tab, icon, title) ->
              val selected = uiState.currentTab == tab
              NavigationBarItem(
                  selected = selected,
                  onClick = { viewModel.setTab(tab) },
                  icon = { Icon(imageVector = icon, contentDescription = title) },
                  label = { Text(title) },
                  colors = NavigationBarItemDefaults.colors(
                      selectedIconColor = CanvasPrimary,
                      selectedTextColor = CanvasPrimary,
                      indicatorColor = CanvasPrimary.copy(alpha = 0.15f)
                  ),
                  modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
              )
            }
          }
        }
      }
  ) { innerPadding ->
    Box(
        Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
      if (uiState.detailConversationId != null) {
        // Detail is a pushed route: the system back gesture pops it, matching
        // native stack behavior (with predictive back on Android 13+).
        BackHandler { viewModel.closeDetail() }
        ConversationDetailScreen(
            conversation = selectedConversation,
            events = events,
            isSending = uiState.isSending,
            isDemoMode = uiState.isDemoMode,
            activeProfile = uiState.activeProfile,
            gitDiff = uiState.gitDiff,
            isGitLoading = uiState.isGitLoading,
            onBack = { viewModel.closeDetail() },
            onRefresh = { viewModel.refreshDetail() },
            onSendMessage = { text -> viewModel.sendFollowUp(text) },
            onDelete = { viewModel.deleteConversation(uiState.detailConversationId!!) },
            onRefreshGit = { viewModel.refreshGitChanges() },
            onModelChanged = { model -> viewModel.setModel(model) },
            onResetProfile = { viewModel.resetConversationProfile() },
            webBaseUrl = viewModel.webBaseUrl(),
            availableModels = uiState.availableModels,
            onRename = { id, title -> viewModel.renameConversation(id, title) },
            sandboxStatus = uiState.sandboxStatus,
            onPauseSandbox = { id -> viewModel.pauseSandbox(id) },
            onResumeSandbox = { id -> viewModel.resumeSandbox(id) },
            onDeleteSandbox = { id -> viewModel.deleteSandbox(id) },
            files = uiState.files,
            isFilesLoading = uiState.isFilesLoading,
            fileContents = uiState.fileContents,
            onLoadFiles = { path -> viewModel.loadWorkspaceFiles(path) },
            onReadFile = { path -> viewModel.readWorkspaceFile(path) },
            skills = uiState.skills,
            onLoadSkills = { viewModel.loadConversationSkills() },
            onExportTrajectory = {
                viewModel.exportTrajectory { filename, json ->
                    shareText(context, filename, json)
                }
            }
        )
      } else {
        when (uiState.currentTab) {
          AppTab.DASHBOARD -> DashboardScreen(
              conversations = conversations,
              templates = templates,
              connectionStatus = uiState.offlineMessage ?: uiState.healthMessage,
              connectionHealthy = uiState.healthOk,
              isDemoMode = uiState.isDemoMode,
              isRefreshing = uiState.isRefreshing,
              onStartNewTask = { viewModel.setTab(AppTab.NEW_TASK) },
              onSelectConversation = { id -> viewModel.openDetail(id) },
              onSelectTemplate = { template -> viewModel.applyTemplate(template) },
              onRefresh = { viewModel.refreshConversations() },
              onOpenSettings = { viewModel.setTab(AppTab.SETTINGS) }
          )
          AppTab.CONVERSATIONS -> ConversationsScreen(
              conversations = conversations,
              searchQuery = uiState.searchQuery,
              statusFilter = uiState.statusFilter,
              isRefreshing = uiState.isRefreshing,
              onSearchChanged = { viewModel.updateSearchQuery(it) },
              onStatusFilterChanged = { viewModel.setStatusFilter(it) },
              onRefresh = { viewModel.refreshConversations(showFeedback = false) },
              onSelectConversation = { id -> viewModel.openDetail(id) },
              onStartNewTask = { viewModel.setTab(AppTab.NEW_TASK) }
          )
          AppTab.NEW_TASK -> NewTaskScreen(
              promptText = uiState.newTaskPrompt,
              repoText = uiState.newTaskRepo,
              templates = templates,
              isLaunching = uiState.isLaunching,
              launchStage = uiState.launchStage,
              launchStatusText = uiState.launchStatusText,
              isDemoMode = uiState.isDemoMode,
              activeProfile = uiState.activeProfile,
              repoResults = uiState.repoSearchResults,
              branchResults = uiState.repoBranchResults,
              selectedBranch = uiState.selectedBranch,
              isSearchingRepos = uiState.isSearchingRepos,
              onPromptChanged = { viewModel.updateNewTaskPrompt(it) },
              onRepoChanged = { viewModel.updateNewTaskRepo(it) },
              onSelectTemplate = { viewModel.applyTemplate(it) },
              onLaunchTask = {
                if (uiState.isDemoMode) viewModel.setTab(AppTab.SETTINGS)
                else viewModel.createNewTask()
              },
              onSearchRepositories = { viewModel.searchRepositories() },
              onSelectRepository = { repo -> viewModel.selectRepository(repo) },
              onSelectBranch = { branch -> viewModel.updateSelectedBranch(branch) },
                onModelChanged = { model -> viewModel.setModel(model) },
                suggestedTasks = uiState.suggestedTasks,
              isLoadingSuggested = uiState.isLoadingSuggested,
              availableModels = uiState.availableModels,
              onLoadSuggestedTasks = { viewModel.loadSuggestedTasks() },
              onSelectSuggestedTask = { task -> viewModel.applySuggestedTask(task) }
          )
          AppTab.SETTINGS -> SettingsScreen(
              apiKey = uiState.apiKeyInput,
              baseUrl = uiState.baseUrlInput,
              isLoading = uiState.isLoading,
              healthMessage = uiState.healthMessage,
              healthOk = uiState.healthOk,
              credits = uiState.credits,
              isDemoMode = uiState.isDemoMode,
              activeProfile = uiState.activeProfile,
              account = uiState.account,
              cloudProfiles = uiState.cloudProfiles,
              activeCloudProfile = uiState.activeCloudProfile,
              secrets = uiState.secrets,
              isCloudSettingsLoading = uiState.isCloudSettingsLoading,
              availableModels = uiState.availableModels,
              onApiKeyChanged = { viewModel.updateApiKeyInput(it) },
              onBaseUrlChanged = { viewModel.updateBaseUrlInput(it) },
              onSaveConfig = { viewModel.saveConfig() },
              onTestConnection = { viewModel.testConnection() },
              onClearApiKey = { viewModel.clearApiKey() },
              onClearCache = { viewModel.clearLocalData() },
                onModelChanged = { model -> viewModel.setModel(model) },
                onRefreshCloudSettings = { viewModel.refreshCloudSettings() },
              onActivateCloudProfile = { name -> viewModel.activateCloudProfile(name) },
              onDeleteCloudProfile = { name -> viewModel.deleteCloudProfile(name) },
              onRenameCloudProfile = { name, newName -> viewModel.renameCloudProfile(name, newName) },
              onAddSecret = { name, value, description -> viewModel.addSecret(name, value, description) },
              onDeleteSecret = { name -> viewModel.deleteSecret(name) }
          )
        }
      }
    }
  }
}
