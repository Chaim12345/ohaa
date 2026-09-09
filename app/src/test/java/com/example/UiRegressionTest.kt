package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.data.model.ConversationUiStatus
import com.example.data.model.OpenHandsConversation
import com.example.data.model.GitChangeDto
import com.example.data.model.GitDiffDto
import com.example.data.model.BranchDto
import com.example.data.model.RepositoryDto
import com.example.data.settings.AgentKind
import com.example.data.settings.AgentProfile
import com.example.data.settings.DEFAULT_OPENHANDS_MODEL
import com.example.ui.screens.ConversationDetailScreen
import com.example.ui.screens.NewTaskScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.LaunchStage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UiRegressionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `demo mode launch button guides to settings instead of a live task`() {
        var launched = false
        composeTestRule.setContent {
            MyApplicationTheme {
                NewTaskScreen(
                    promptText = "Fix the failing test",
                    repoText = "owner/repo",
                    templates = emptyList(),
                    isLaunching = false,
                    launchStage = LaunchStage.IDLE,
                    launchStatusText = null,
                    isDemoMode = true,
                    activeProfile = AgentProfile(),
                    repoResults = emptyList(),
                    branchResults = emptyList(),
                    selectedBranch = "",
                    isSearchingRepos = false,
                    onPromptChanged = {},
                    onRepoChanged = {},
                    onSelectTemplate = {},
                    onLaunchTask = { launched = true },
                    onSearchRepositories = {},
                    onSelectRepository = {},
                    onSelectBranch = {},
                    onAgentKindChanged = {},
                    onModelChanged = {},
                    onAcpCommandChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("new_task_screen")
            .performScrollToNode(hasTestTag("btn_launch"))
        composeTestRule.onNodeWithTag("btn_launch").assertIsDisplayed().performClick()
        assertTrue(launched)
        composeTestRule.onNodeWithText("Demo mode is read-only. Add an API key in Settings to launch a live cloud agent.")
            .assertExists()
    }

    @Test
    fun `launch stays disabled until a prompt is entered`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                NewTaskScreen(
                    promptText = "",
                    repoText = "",
                    templates = emptyList(),
                    isLaunching = false,
                    launchStage = LaunchStage.IDLE,
                    launchStatusText = null,
                    isDemoMode = false,
                    activeProfile = AgentProfile(),
                    repoResults = emptyList(),
                    branchResults = emptyList(),
                    selectedBranch = "",
                    isSearchingRepos = false,
                    onPromptChanged = {},
                    onRepoChanged = {},
                    onSelectTemplate = {},
                    onLaunchTask = {},
                    onSearchRepositories = {},
                    onSelectRepository = {},
                    onSelectBranch = {},
                    onAgentKindChanged = {},
                    onModelChanged = {},
                    onAcpCommandChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("btn_launch").assertIsNotEnabled()
    }

    @Test
    fun `settings exposes key visibility and connection action`() {
        var connectionTested = false
        composeTestRule.setContent {
            MyApplicationTheme {
                SettingsScreen(
                    apiKey = "secret-key",
                    baseUrl = "https://app.all-hands.dev",
                    isLoading = false,
                    healthMessage = null,
                    healthOk = null,
                    credits = null,
                    isDemoMode = false,
                    activeProfile = AgentProfile(),
                    onApiKeyChanged = {},
                    onBaseUrlChanged = {},
                    onSaveConfig = {},
                    onTestConnection = { connectionTested = true },
                    onClearApiKey = {},
                    onClearCache = {},
                    onAgentKindChanged = {},
                    onModelChanged = {},
                    onAcpCommandChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("api_key_input").assertExists()
        composeTestRule.onNodeWithContentDescription("Show API key").assertExists().performClick()
        composeTestRule.onNodeWithContentDescription("Hide API key").assertExists()
        composeTestRule.onNodeWithTag("btn_test_connection").performClick()

        assertTrue(connectionTested)
    }

    @Test
    fun `clear cache requires confirmation`() {
        var cleared = false
        composeTestRule.setContent {
            MyApplicationTheme {
                SettingsScreen(
                    apiKey = "",
                    baseUrl = "https://app.all-hands.dev",
                    isLoading = false,
                    healthMessage = null,
                    healthOk = null,
                    credits = null,
                    isDemoMode = true,
                    activeProfile = AgentProfile(),
                    onApiKeyChanged = {},
                    onBaseUrlChanged = {},
                    onSaveConfig = {},
                    onTestConnection = {},
                    onClearApiKey = {},
                    onClearCache = { cleared = true },
                    onAgentKindChanged = {},
                    onModelChanged = {},
                    onAcpCommandChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("btn_clear_cache"))
        composeTestRule.onNodeWithTag("btn_clear_cache").performClick()
        composeTestRule.onNodeWithText("Clear local data?").assertExists()
        assertTrue(!cleared)
        composeTestRule.onNodeWithTag("btn_confirm_clear_cache").performClick()
        assertTrue(cleared)
    }

    @Test
    fun `detail delete requires confirmation`() {
        var deleted = false
        val conversation = OpenHandsConversation(
            id = "conversation-1",
            title = "Fix the failing test",
            uiStatus = ConversationUiStatus.FINISHED,
            sandboxStatus = "RUNNING",
            executionStatus = "finished",
            repository = "owner/repo",
            createdAt = 0L,
            updatedAt = 0L,
            cost = 0.0,
            lastMessage = null
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                ConversationDetailScreen(
                    conversation = conversation,
                    events = emptyList(),
                    isSending = false,
                    isDemoMode = false,
                    activeProfile = AgentProfile(),
                    gitDiff = null,
                    isGitLoading = false,
                    onBack = {},
                    onRefresh = {},
                    onSendMessage = {},
                    onDelete = { deleted = true },
                    onRefreshGit = {},
                    onAgentKindChanged = {},
                    onModelChanged = {},
                    onAcpCommandChanged = {},
                    onResetProfile = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Delete conversation").performClick()
        composeTestRule.onNodeWithText("Delete conversation?").assertExists()
        assertTrue(!deleted)
        composeTestRule.onNodeWithTag("btn_confirm_delete").performClick()
        assertTrue(deleted)
    }

    @Test
    fun `agent profile is visible and switchable from the chat header`() {
        var kindChanged = AgentKind.OPENHANDS
        var modelChanged = ""
        val conversation = OpenHandsConversation(
            id = "conversation-1",
            title = "Fix the failing test",
            uiStatus = ConversationUiStatus.FINISHED,
            sandboxStatus = "RUNNING",
            executionStatus = "finished",
            repository = "owner/repo",
            createdAt = 0L,
            updatedAt = 0L,
            cost = 0.0,
            lastMessage = null
        )

        var currentKind by mutableStateOf(AgentKind.OPENHANDS)
        var currentModel by mutableStateOf("openai/gpt-4o")
        var currentAcpCommand by mutableStateOf("")
        composeTestRule.setContent {
            MyApplicationTheme {
                ConversationDetailScreen(
                    conversation = conversation,
                    events = emptyList(),
                    isSending = false,
                    isDemoMode = false,
                    activeProfile = AgentProfile(
                        kind = currentKind,
                        model = currentModel,
                        acpCommand = currentAcpCommand
                    ),
                    gitDiff = GitDiffDto(
                        diff = "diff --git a/x b/x",
                        files = listOf(
                            GitChangeDto(file = "app/Main.kt", status = "modified", insertions = 3, deletions = 1)
                        )
                    ),
                    isGitLoading = false,
                    onBack = {},
                    onRefresh = {},
                    onSendMessage = {},
                    onDelete = {},
                    onRefreshGit = {},
                    onAgentKindChanged = { kindChanged = it; currentKind = it },
                    onModelChanged = { modelChanged = it; currentModel = it },
                    onAcpCommandChanged = { currentAcpCommand = it },
                    onResetProfile = {}
                )
            }
        }

        // The header exposes the active agent + LLM.
        composeTestRule.onNodeWithTag("agent_profile_badge").assertExists()
        composeTestRule.onNodeWithTag("agent_profile_label")
            .assertTextEquals("OpenHands · gpt-4o")

        // Tapping opens the switcher; toggling ACP calls through (the dialog is
        // stateless — the test host holds profile state exactly like MainActivity).
        composeTestRule.onNodeWithTag("agent_profile_badge").performClick()
        composeTestRule.onNodeWithText("Agent & LLM · this conversation").assertExists()
        composeTestRule.onNodeWithTag("agent_kind_ACP").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("field_acp_command")
            .performTextInput("npx -y custom-acp")
        composeTestRule.onNodeWithTag("btn_profile_done").performClick()
    }
}
