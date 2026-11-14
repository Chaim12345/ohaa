package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AdvancedRuntimeSheet
import com.example.ui.screens.ContextSheet
import com.example.ui.screens.FinalResponseSheet
import com.example.ui.screens.GoalSheet
import com.example.ui.screens.RuntimeSkillsSheet
import com.example.ui.screens.SubConversationSheet
import com.example.ui.screens.ToolsSheet
import com.example.ui.screens.VSCodeSheet
import com.example.ui.screens.ApiExplorerScreen
import com.example.ui.screens.ConversationDetailScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FilesSheet
import com.example.ui.screens.GitSheet
import com.example.ui.screens.ModelSheet
import com.example.ui.screens.TerminalSheet
import com.example.ui.screens.NewTaskScreen
import com.example.ui.screens.ResourcesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OpenHandsPrimaryDark
import com.example.ui.viewmodel.AppNavTab
import com.example.ui.viewmodel.OpenHandsViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        OpenHandsApp()
      }
    }
  }
}

@Composable
fun OpenHandsApp(
    viewModel: OpenHandsViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsState()
  val conversations by viewModel.conversations.collectAsState()
  val templates by viewModel.templates.collectAsState()
  val events by viewModel.events.collectAsState()
  val gitChanges by viewModel.gitChanges.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(uiState.notificationMessage) {
    uiState.notificationMessage?.let { msg ->
      snackbarHostState.showSnackbar(msg)
      viewModel.dismissNotification()
    }
  }

  val selectedConv = remember(uiState.selectedConversationId, conversations) {
    conversations.find { it.id == uiState.selectedConversationId }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize(),
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      bottomBar = {
        if (uiState.selectedConversationId == null) {
          NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              modifier = Modifier.testTag("bottom_nav_bar")
          ) {
            val tabs = listOf(
                Triple(AppNavTab.DASHBOARD, Icons.Default.Dashboard, "Dashboard"),
                Triple(AppNavTab.TASKS, Icons.AutoMirrored.Filled.FormatListBulleted, "Tasks"),
                Triple(AppNavTab.NEW_TASK, Icons.Default.AddCircle, "New Task"),
                Triple(AppNavTab.RESOURCES, Icons.Default.Folder, "Resources"),
                Triple(AppNavTab.EXPLORER, Icons.Default.Api, "API"),
                Triple(AppNavTab.SETTINGS, Icons.Default.Cloud, "Cloud API")
            )

            tabs.forEach { (tab, icon, title) ->
              val selected = uiState.currentTab == tab
              NavigationBarItem(
                  selected = selected,
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
      if (uiState.selectedConversationId != null) {
        var openSheet by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(uiState.selectedConversationId) { openSheet = null }

        ConversationDetailScreen(
            conversation = selectedConv,
            events = events,
            isSendingMessage = uiState.isSendingMessage,
            gitChanges = gitChanges,
            isLoadingChanges = uiState.isLoadingChanges,
            onRefreshChanges = { viewModel.refreshGitChangesPreferRuntime() },
            onBack = { viewModel.closeConversationDetail() },
            onRefresh = {
              uiState.selectedConversationId?.let { viewModel.refreshEvents(it) }
            },
            onSendMessage = { text ->
              uiState.selectedConversationId?.let { id ->
                viewModel.sendFollowUpMessage(id, text)
              }
            },
            onDelete = {
              uiState.selectedConversationId?.let { viewModel.deleteConversation(it) }
            },
            onResume = { viewModel.resumeSelectedSandbox() },
            isResuming = uiState.isResuming,
            onOpenModel = {
              viewModel.loadProfiles()
              viewModel.loadAgentProfiles()
              if (selectedConv?.isAcp != true) viewModel.loadModels()
              viewModel.loadConversationInfo()
              openSheet = "model"
            },
            onOpenContext = {
              viewModel.loadConversationInfo()
              openSheet = "context"
            },
            onOpenTerminal = { openSheet = "terminal" },
            onOpenFiles = { viewModel.loadFiles(null); openSheet = "files" },
            onOpenGit = {
              viewModel.loadGitCommits()
              openSheet = "git"
            },
            onStop = { viewModel.stopAgent() },
            onRun = { viewModel.runAgent() },
            onPause = { viewModel.pauseAgent() },
            onRename = { title ->
              uiState.selectedConversationId?.let { viewModel.renameConversation(it, title) }
            },
            onOpenAdvanced = {
              viewModel.loadRuntimeInfo()
              openSheet = "advanced"
            },
            onOpenGoal = { openSheet = "goal" },
            onOpenTools = {
              viewModel.loadToolCatalog()
              viewModel.loadBashEvents()
              openSheet = "tools"
            },
            onOpenVSCode = {
              viewModel.loadVSCodeUrl()
              openSheet = "vscode"
            },
            onOpenSkills = {
              viewModel.loadRuntimeSkills()
              viewModel.loadRuntimePlugins()
              openSheet = "skills"
            },
            onOpenFinalResponse = {
              viewModel.loadAgentFinalResponse()
              openSheet = "final"
            },
            onOpenSubConversations = {
              viewModel.loadChildConversations()
              openSheet = "subconv"
            },
            onConfirm = { accept -> viewModel.respondToConfirmation(accept) },
            onFork = { viewModel.forkConversation() }
        )

        when (openSheet) {
          "model" -> ModelSheet(
              isAcp = selectedConv?.isAcp == true,
              currentModel = selectedConv?.llmModel,
              profiles = uiState.profiles,
              acpModels = uiState.availableAcpModels,
              catalogModels = uiState.models,
              efforts = uiState.availableEfforts,
              currentEffort = uiState.conversationInfo?.currentEffort,
              agentProfiles = uiState.agentProfiles,
              isBusy = uiState.isRuntimeBusy,
              onSwitchProfile = { viewModel.switchProfile(it) },
              onSwitchModel = { viewModel.switchModel(it) },
              onSwitchEffort = { viewModel.switchEffort(it) },
              onActivateAgentProfile = { viewModel.activateAgentProfile(it) },
              onSearchModels = { viewModel.loadModels(it) },
              onDismiss = { openSheet = null }
          )
          "context" -> ContextSheet(
              info = uiState.conversationInfo,
              isBusy = uiState.isRuntimeBusy,
              onCompact = { viewModel.compactContext() },
              onDismiss = { openSheet = null }
          )
          "terminal" -> TerminalSheet(
              command = uiState.terminalCommand,
              output = uiState.terminalOutput,
              isBusy = uiState.isRuntimeBusy,
              onCommandChange = { viewModel.updateTerminalCommand(it) },
              onRun = { viewModel.runTerminalCommand() },
              onDismiss = { openSheet = null }
          )
          "files" -> FilesSheet(
              path = uiState.filesPath,
              entries = uiState.runtimeFiles,
              onOpen = { viewModel.loadFiles(it) },
              onDismiss = { openSheet = null }
          )
          "git" -> GitSheet(
              commits = uiState.gitCommits,
              diff = uiState.gitDiff,
              onLoadCommits = { viewModel.loadGitCommits() },
              onDismiss = { openSheet = null }
          )
          "advanced" -> AdvancedRuntimeSheet(
              runtimeInfo = uiState.runtimeInfo,
              isBusy = uiState.isRuntimeBusy,
              onSetConfirmationPolicy = { viewModel.setConfirmationPolicy(it) },
              onSetSecurityAnalyzer = { viewModel.setSecurityAnalyzer(it) },
              onReprovision = { viewModel.reprovisionRuntime() },
              onLoadRuntimeInfo = { viewModel.loadRuntimeInfo() },
              onDismiss = { openSheet = null }
          )
          "goal" -> GoalSheet(
              isBusy = uiState.isRuntimeBusy,
              onStartGoal = { viewModel.startGoal(it) },
              onResumeGoal = { viewModel.resumeGoal() },
              onStopGoal = { viewModel.stopGoal() },
              onDismiss = { openSheet = null }
          )
          "tools" -> ToolsSheet(
              tools = uiState.toolCatalog,
              bashEvents = uiState.bashEvents,
              onLoadTools = { viewModel.loadToolCatalog() },
              onLoadBashEvents = { viewModel.loadBashEvents() },
              onDismiss = { openSheet = null }
          )
          "vscode" -> VSCodeSheet(
              url = uiState.vscodeUrl,
              isLoading = uiState.isRuntimeBusy,
              onDismiss = { openSheet = null }
          )
          "skills" -> RuntimeSkillsSheet(
              skills = uiState.installedSkills,
              plugins = uiState.installedPlugins,
              settings = null,
              onLoad = {
                viewModel.loadRuntimeSkills()
                viewModel.loadRuntimePlugins()
              },
              onSync = { viewModel.syncRuntimeSkills() },
              onInstallSkill = { viewModel.installRuntimeSkill(it) },
              onDismiss = { openSheet = null }
          )
          "final" -> FinalResponseSheet(
              response = uiState.agentFinalResponse,
              onDismiss = { openSheet = null }
          )
          "subconv" -> SubConversationSheet(
              children = uiState.childConversations,
              workspaceInfo = uiState.workspaceInfo,
              isBusy = uiState.isRuntimeBusy,
              onStart = { viewModel.startSubConversation(it) },
              onLoad = { viewModel.loadChildConversations() },
              onLoadWorkspace = { viewModel.loadWorkspacePath() },
              onDismiss = { openSheet = null }
          )
        }
      } else {
        when (uiState.currentTab) {
          AppNavTab.DASHBOARD -> {
            DashboardScreen(
                conversations = conversations,
                templates = templates,
                connectionStatus = uiState.connectionStatus,
                connectionHealthy = uiState.connectionHealthy,
                isLoading = uiState.isLoading,
                onStartNewTask = { viewModel.setTab(AppNavTab.NEW_TASK) },
                onSelectConversation = { id -> viewModel.selectConversation(id) },
                onSelectTemplate = { template -> viewModel.applyTemplate(template) },
                onRefresh = { viewModel.refreshConversations() },
                onOpenSettings = { viewModel.setTab(AppNavTab.SETTINGS) }
            )
          }
          AppNavTab.TASKS -> {
            TasksScreen(
                conversations = conversations,
                searchQuery = uiState.searchQuery,
                statusFilter = uiState.filterStatus,
                onSearchChanged = { q -> viewModel.updateSearchQuery(q) },
                onStatusFilterChanged = { status -> viewModel.setStatusFilter(status) },
                onSelectConversation = { id -> viewModel.selectConversation(id) },
                onStartNewTask = { viewModel.setTab(AppNavTab.NEW_TASK) },
                onRefresh = { viewModel.refreshConversations() }
            )
          }
          AppNavTab.NEW_TASK -> {
            NewTaskScreen(
                promptText = uiState.newTaskPrompt,
                repoText = uiState.newTaskRepo,
                branchText = uiState.newTaskBranch,
                templates = templates,
                isLoading = uiState.isLoading,
                isProvisioning = uiState.isProvisioning,
                agentTypeSelection = uiState.agentTypeSelection,
                modelText = uiState.selectedModelForTask ?: "",
                profileText = uiState.llmProfileSelection ?: "",
                onPromptChanged = { p -> viewModel.updateNewTaskPrompt(p) },
                onRepoChanged = { r -> viewModel.updateNewTaskRepo(r) },
                onBranchChanged = { b -> viewModel.updateNewTaskBranch(b) },
                onAgentTypeChanged = { viewModel.setAgentTypeSelection(it) },
                onModelChanged = { viewModel.setSelectedModelForTask(it.ifBlank { null }) },
                onProfileChanged = { viewModel.setLlmProfileSelection(it.ifBlank { null }) },
                onSelectTemplate = { t -> viewModel.applyTemplate(t) },
                onLaunchTask = {
                  viewModel.createNewTask { convId ->
                    // Navigation to the conversation detail happens in the ViewModel.
                  }
                }
            )
          }
          AppNavTab.RESOURCES -> {
            ResourcesScreen(
                secrets = uiState.secrets,
                skills = uiState.skills,
                sandboxes = uiState.sandboxes,
                gitLogin = uiState.gitLogin,
                gitOrganizations = uiState.gitOrganizations,
                gitInstallations = uiState.gitInstallations,
                userSettings = uiState.userSettings,
                webClientConfig = uiState.webClientConfig,
                profiles = uiState.profiles,
                onLoadSecrets = { viewModel.loadSecrets() },
                onLoadSkills = { viewModel.loadSkills() },
                onLoadSandboxes = { viewModel.loadSandboxes() },
                onLoadGitAccount = { viewModel.loadGitAccount() },
                onLoadGitOrganizations = { viewModel.loadGitOrganizations() },
                onLoadGitInstallations = { viewModel.loadGitInstallations("github") },
                onLoadSettings = { viewModel.loadUserSettings() },
                onLoadWebClientConfig = { viewModel.loadWebClientConfig() },
                onSaveSettings = { viewModel.saveUserSettings(it) },
                onStoreGitToken = { provider, token -> viewModel.storeGitToken(provider, token) },
                onUnlinkGitProvider = { viewModel.unlinkGitProvider(it) },
                onCreateSecret = { n, v, d -> viewModel.createSecret(n, v, d) },
                onDeleteSecret = { viewModel.deleteSecret(it) },
                onDeleteSandbox = { viewModel.deleteSandbox(it) },
                onCreateSandbox = { viewModel.createSandbox(it) },
                onDeleteProfile = { viewModel.deleteProfile(it) },
                onActivateProfile = { viewModel.activateCloudProfile(it) },
                onSaveProfile = { name, model, base -> viewModel.saveProfile(name, model, base) },
                onLoadProfiles = { viewModel.loadProfiles() }
            )
          }
          AppNavTab.EXPLORER -> {
            ApiExplorerScreen(
                cloudPaths = viewModel.explorerCloudPaths,
                agentPaths = viewModel.explorerAgentPaths,
                response = uiState.explorerResponse,
                isLoading = uiState.explorerLoading,
                onCall = { surface, method, path, body ->
                  viewModel.callExplorer(surface, method, path, body)
                }
            )
          }
          AppNavTab.SETTINGS -> {
            SettingsScreen(
                apiKey = uiState.apiKey,
                baseUrl = uiState.baseUrl,
                isLoading = uiState.isLoading,
                connectionStatus = uiState.connectionStatus,
                connectionHealthy = uiState.connectionHealthy,
                onApiKeyChanged = { k -> viewModel.updateApiKeyInput(k) },
                onBaseUrlChanged = { u -> viewModel.updateBaseUrlInput(u) },
                onSaveConfig = { viewModel.saveConfig() },
                onTestConnection = { viewModel.testConnection() },
                onClearCache = { viewModel.clearLocalCache() },
                keepAliveEnabled = uiState.keepAliveEnabled,
                onKeepAliveChanged = { enabled -> viewModel.setKeepAliveEnabled(enabled) }
            )
          }
        }
      }
    }
  }
}

