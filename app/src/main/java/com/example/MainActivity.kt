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
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ConversationDetailScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NewTaskScreen
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
        ConversationDetailScreen(
            conversation = selectedConv,
            events = events,
            isSendingMessage = uiState.isSendingMessage,
            onBack = { viewModel.closeConversationDetail() },
            onRefresh = { viewModel.refreshConversations() },
            onSendMessage = { text ->
              uiState.selectedConversationId?.let { id ->
                viewModel.sendFollowUpMessage(id, text)
              }
            }
        )
      } else {
        when (uiState.currentTab) {
          AppNavTab.DASHBOARD -> {
            DashboardScreen(
                conversations = conversations,
                templates = templates,
                connectionStatus = uiState.connectionStatus,
                connectionHealthy = uiState.connectionHealthy,
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
                templates = templates,
                isLoading = uiState.isLoading,
                onPromptChanged = { p -> viewModel.updateNewTaskPrompt(p) },
                onRepoChanged = { r -> viewModel.updateNewTaskRepo(r) },
                onSelectTemplate = { t -> viewModel.applyTemplate(t) },
                onLaunchTask = {
                  viewModel.createNewTask { convId ->
                    // Navigate to conversation detail
                  }
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
                onClearCache = { viewModel.clearLocalCache() }
            )
          }
        }
      }
    }
  }
}

