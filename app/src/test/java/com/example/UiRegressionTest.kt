package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.ConversationUiStatus
import com.example.data.model.OpenHandsConversation
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
                    onPromptChanged = {},
                    onRepoChanged = {},
                    onSelectTemplate = {},
                    onLaunchTask = { launched = true }
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
                    onPromptChanged = {},
                    onRepoChanged = {},
                    onSelectTemplate = {},
                    onLaunchTask = {}
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
                    onApiKeyChanged = {},
                    onBaseUrlChanged = {},
                    onSaveConfig = {},
                    onTestConnection = { connectionTested = true },
                    onClearApiKey = {},
                    onClearCache = {}
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
                    onApiKeyChanged = {},
                    onBaseUrlChanged = {},
                    onSaveConfig = {},
                    onTestConnection = {},
                    onClearApiKey = {},
                    onClearCache = { cleared = true }
                )
            }
        }

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
                    onBack = {},
                    onRefresh = {},
                    onSendMessage = {},
                    onDelete = { deleted = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Delete conversation").performClick()
        composeTestRule.onNodeWithText("Delete conversation?").assertExists()
        assertTrue(!deleted)
        composeTestRule.onNodeWithTag("btn_confirm_delete").performClick()
        assertTrue(deleted)
    }
}
