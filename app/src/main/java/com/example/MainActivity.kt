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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.AppNavTab
import com.example.ui.viewmodel.OpenHandsViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val rootViewModel: OpenHandsViewModel = viewModel()
      val rootState by rootViewModel.uiState.collectAsState()
      MyApplicationTheme(
          themeMode = runCatching { ThemeMode.valueOf(rootState.themeMode) }
              .getOrDefault(ThemeMode.SYSTEM)
      ) {
        OpenHandsApp(viewModel = rootViewModel)
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
      floatingActionButton = {
        if (uiState.selectedConversationId == null) {
          FloatingActionButton(
              onClick = { viewModel.setTab(AppNavTab.NEW_TASK) },
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.testTag("fab_new_task")
          ) {
            Icon(Icons.Default.AddCircle, contentDescription = "New Task")
          }
        }
      },
      bottomBar = {
        if (uiState.selectedConversationId == null) {
          NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              modifier = Modifier.testTag("bottom_nav_bar")
          ) {
            val tabs = listOf(
                Triple(AppNavTab.DASHBOARD, Icons.Default.Dashboard, "Home"),
                Triple(AppNavTab.TASKS, Icons.AutoMirrored.Filled.FormatListBulleted, "Tasks"),
                Triple(AppNavTab.RESOURCES, Icons.Default.Folder, "Resources"),
                Triple(AppNavTab.SETTINGS, Icons.Default.Settings, "Settings")
            )

            tabs.forEach { (tab, icon, title) ->
              val selected = uiState.currentTab == tab
              NavigationBarItem(
                  selected = selected,
                  onClick = { viewModel.setTab(tab) },
                  icon = { Icon(imageVector = icon, contentDescription = title) },
                  label = {
                    Text(
                      text = title,
                      fontSize = 10.sp,
                      maxLines = 1,
                      softWrap = false,
                      overflow = TextOverflow.Ellipsis
                    )
                  },
                  colors = NavigationBarItemDefaults.colors(
                      selectedIconColor = MaterialTheme.colorScheme.primary,
                      selectedTextColor = MaterialTheme.colorScheme.primary,
                      indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
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
              modelQuery = uiState.modelQuery,
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
              eventCount = uiState.eventCount,
              onLoadEventCount = { viewModel.loadEventCount() },
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
              mcpToolOutput = uiState.lastToolOutput,
              onLoadCloudCatalog = { viewModel.loadCloudSkillCatalog() },
              onPreviewMarketplace = { name, source -> viewModel.previewMarketplace(name, source) },
              onStartMcpOAuth = { viewModel.startMcpOAuth(it) },
              onCheckMcpOAuth = { viewModel.pollMcpOAuthStatus() },
              onCallMcpTool = { server, tool -> viewModel.callMcpTool(server, tool) },
              onTestMcpServer = { name, json -> viewModel.testMcpServer(name, json) },
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
                totalConversationCount = uiState.totalConversationCount,
                isLoading = uiState.isLoading,
                isRefreshing = uiState.isRefreshing,
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
                onRefresh = { viewModel.refreshConversations() },
                isRefreshing = uiState.isRefreshing
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
                selectedTemplateId = uiState.selectedTemplate?.id,
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
                sandboxSecrets = uiState.sandboxSecrets,
                revealedSecret = uiState.revealedSecret,
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
                onClearGitTokens = { viewModel.clearGitProviderTokens() },
                onCreateSecret = { n, v, d -> viewModel.createSecret(n, v, d) },
                onDeleteSecret = { viewModel.deleteSecret(it) },
                onDeleteSandbox = { viewModel.deleteSandbox(it) },
                onCreateSandbox = { viewModel.createSandbox(it) },
                onPauseSandbox = { viewModel.pauseSandbox(it) },
                onResumeSandbox = { viewModel.resumeSandbox(it) },
                onLoadSandboxSecrets = { viewModel.loadSandboxSecrets(it) },
                onRevealSandboxSecret = { id, name -> viewModel.revealSandboxSecret(id, name) },
                onClearRevealedSecret = { viewModel.clearRevealedSecret() },
                onDeleteProfile = { viewModel.deleteProfile(it) },
                onActivateProfile = { viewModel.activateCloudProfile(it) },
                onSaveProfile = { name, model, base -> viewModel.saveProfile(name, model, base) },
                onLoadProfiles = { viewModel.loadProfiles() },
                selectedProfileDetail = uiState.selectedProfileDetail,
                onPreviewProfile = { viewModel.loadProfileDetail(it) },
                onRenameProfile = { name, newName -> viewModel.renameCloudProfile(name, newName) },
                onInstallPlugin = { viewModel.installRuntimePlugin(it) },
                agentProfiles = uiState.agentProfileCatalog,
                activeAgentProfileId = uiState.activeAgentProfileId,
                onLoadAgentProfiles = { viewModel.loadCloudAgentProfiles() },
                onSaveAgentProfile = { name, kind, server, model, effort ->
                    viewModel.saveAgentProfile(name, kind, server, model, effort)
                },
                onDeleteAgentProfile = { viewModel.deleteAgentProfile(it) },
                onActivateCloudAgentProfile = { viewModel.activateCloudAgentProfile(it) }
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
                onOpenApiExplorer = { viewModel.setTab(AppNavTab.EXPLORER) },
                keepAliveEnabled = uiState.keepAliveEnabled,
                onKeepAliveChanged = { enabled -> viewModel.setKeepAliveEnabled(enabled) },
                themeMode = uiState.themeMode,
                onThemeModeChanged = { mode -> viewModel.setThemeMode(mode) }
            )
          }
        }
      }
    }
  }
}

