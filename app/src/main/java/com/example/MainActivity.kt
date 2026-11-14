package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
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

  // Android 13+ requires an explicit grant before the keep-alive foreground
  // service can post its ongoing notification.
  val context = LocalContext.current
  val notificationPermission = rememberLauncherForActivityResult(
      ActivityResultContracts.RequestPermission()
  ) { /* The service still runs; only the notification is affected. */ }
  LaunchedEffect(Unit) {
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
          context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
          android.content.pm.PackageManager.PERMISSION_GRANTED
      ) {
          notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
      }
  }

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
            runtimeUnavailableReason = viewModel.runtimeUnavailableReason(),
            onOpenModel = {
              viewModel.loadProfiles()
              viewModel.loadAgentProfiles()
              viewModel.loadCloudAgentProfiles()
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
              viewModel.loadRuntimeProviders()
              viewModel.loadRuntimeModels()
              openSheet = "advanced"
            },
            onOpenGoal = { openSheet = "goal" },
            onOpenTools = {
              viewModel.loadToolCatalog()
              viewModel.loadBashEvents()
              viewModel.loadSubAgents()
              openSheet = "tools"
            },
            onOpenVSCode = {
              viewModel.loadVSCodeUrl()
              openSheet = "vscode"
            },
            onOpenSkills = {
              viewModel.loadRuntimeSkills()
              viewModel.loadRuntimePlugins()
              viewModel.loadCloudSkillCatalog()
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
            onFork = { viewModel.forkConversation() },
            onClearEvents = { viewModel.clearRuntimeEvents() },
            onCheckInit = { viewModel.checkInitStatus() },
            onUnknownCommand = { cmd -> viewModel.showMessage("Unknown command: $cmd") }
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
              cloudAgentProfiles = uiState.agentProfileCatalog,
              activeCloudAgentProfileId = uiState.activeAgentProfileId,
              cloudProfileDetail = uiState.selectedAgentProfileDetail,
              acpSupportsRuntimeModelSwitch = selectedConv?.acpSupportsRuntimeModelSwitch ?: true,
              isBusy = uiState.isRuntimeBusy,
              onSwitchProfile = { viewModel.switchProfile(it) },
              onSwitchModel = { viewModel.switchModel(it) },
              onSwitchEffort = { viewModel.switchEffort(it) },
              onActivateAgentProfile = { viewModel.activateAgentProfile(it) },
              onActivateCloudAgentProfile = { viewModel.activateCloudAgentProfile(it) },
              onPreviewCloudAgentProfile = { viewModel.loadAgentProfileDetail(it) },
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
              onRunAsync = { cmd ->
                uiState.selectedConversationId?.let { viewModel.callRuntimeBash(it, cmd) }
              },
              onDismiss = { openSheet = null }
          )
          "files" -> FilesSheet(
              path = uiState.filesPath,
              entries = uiState.runtimeFiles,
              onOpen = { viewModel.loadFiles(it) },
              onDownload = { path -> viewModel.downloadSandboxFileToDownloads(path) },
              onDismiss = { openSheet = null }
          )
          "git" -> GitSheet(
              commits = uiState.gitCommits,
              diff = uiState.gitDiff,
              onLoadCommits = { viewModel.loadGitCommits() },
              onLoadWorkingDiff = { viewModel.loadGitDiff() },
              onSelectCommit = { sha -> viewModel.loadGitDiff(commit = sha) },
              onDismiss = { openSheet = null }
          )
          "advanced" -> AdvancedRuntimeSheet(
              runtimeInfo = uiState.runtimeInfo,
              isBusy = uiState.isRuntimeBusy,
              onSetConfirmationPolicy = { viewModel.setConfirmationPolicy(it) },
              onSetSecurityAnalyzer = { viewModel.setSecurityAnalyzer(it) },
              onReprovision = { viewModel.reprovisionRuntime() },
              onLoadRuntimeInfo = { viewModel.loadRuntimeInfo() },
              runtimeProviders = uiState.runtimeProviders,
              runtimeModels = uiState.runtimeModels,
              onLoadRuntimeProviders = { viewModel.loadRuntimeProviders() },
              onLoadRuntimeModels = { viewModel.loadRuntimeModels() },
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
              subAgents = uiState.subAgents,
              onLoadSubAgents = { viewModel.loadSubAgents() },
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
              cloudSkills = uiState.cloudSkillCatalog,
              marketplaceSkills = uiState.marketplaceSkills,
              marketplacePlugins = uiState.marketplacePlugins,
              mcpOAuthUrl = uiState.mcpOAuthUrl,
              mcpOAuthStatus = uiState.mcpOAuthStatus,
              onLoadCloudCatalog = { viewModel.loadCloudSkillCatalog() },
              onPreviewMarketplace = { name, source -> viewModel.previewMarketplace(name, source) },
              onStartMcpOAuth = { viewModel.startMcpOAuth(it) },
              onCheckMcpOAuth = { viewModel.pollMcpOAuthStatus() },
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
                agentProfiles = uiState.agentProfileCatalog,
                agentProfileSelection = uiState.agentProfileSelection,
                onPromptChanged = { p -> viewModel.updateNewTaskPrompt(p) },
                onRepoChanged = { r -> viewModel.updateNewTaskRepo(r) },
                onBranchChanged = { b -> viewModel.updateNewTaskBranch(b) },
                onAgentTypeChanged = { viewModel.setAgentTypeSelection(it) },
                onModelChanged = { viewModel.setSelectedModelForTask(it.ifBlank { null }) },
                onProfileChanged = { viewModel.setLlmProfileSelection(it.ifBlank { null }) },
                onAgentProfileChanged = { viewModel.setAgentProfileSelection(it) },
                onLoadAgentProfiles = { viewModel.loadCloudAgentProfiles() },
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
                onLoadProfiles = { viewModel.loadProfiles() },
                selectedProfileDetail = uiState.selectedProfileDetail,
                onPreviewProfile = { viewModel.loadProfileDetail(it) },
                onRenameProfile = { name, newName -> viewModel.renameCloudProfile(name, newName) },
                onInstallPlugin = { viewModel.installRuntimePlugin(it) }
            )
          }
          AppNavTab.EXPLORER -> {
            ApiExplorerScreen(
                cloudPaths = viewModel.explorerCloudPaths,
                cloudAdminPaths = viewModel.explorerCloudAdminPaths,
                agentPaths = viewModel.explorerAgentPaths,
                response = uiState.explorerResponse,
                isLoading = uiState.explorerLoading,
                onCall = { surface, method, path, body ->
                  viewModel.callExplorer(surface, method, path, body)
                },
                agentSchema = uiState.agentSchema,
                conversationSchema = uiState.conversationSchema,
                onLoadSchemas = { viewModel.loadSchemas() }
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

