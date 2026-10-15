package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.data.model.ConversationUiStatus
import com.example.data.model.LlmProfileDto
import com.example.data.model.OpenHandsConversation
import com.example.data.model.GitChangeDto
import com.example.data.model.GitDiffDto
import com.example.data.model.SecretDto
import com.example.data.model.UserInfoDto
import com.example.data.model.BranchDto
import com.example.data.model.RepositoryDto
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
                    onModelChanged = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("new_task_screen")
            .performScrollToNode(hasTestTag("btn_launch"))
        composeTestRule.onNodeWithTag("btn_launch").assertIsDisplayed().performClick()
        assertTrue(launched)
        composeTestRule.onNodeWithText("Not connected — add your API key in Settings to launch a live cloud agent.")
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
                    onModelChanged = {},
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
                    onModelChanged = {},
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
                    onModelChanged = {},
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

    private fun settingsTestContent(
        isDemoMode: Boolean = false,
        account: UserInfoDto? = null,
        cloudProfiles: List<LlmProfileDto> = emptyList(),
        activeCloudProfile: String? = null,
        secrets: List<SecretDto> = emptyList(),
        onActivateCloudProfile: (String) -> Unit = {},
        onDeleteCloudProfile: (String) -> Unit = {},
        onRenameCloudProfile: (String, String) -> Unit = { _, _ -> },
        onAddSecret: (String, String, String?) -> Unit = { _, _, _ -> },
        onDeleteSecret: (String) -> Unit = {}
    ): @Composable () -> Unit = {
        MyApplicationTheme {
            SettingsScreen(
                apiKey = "key",
                baseUrl = "https://app.all-hands.dev",
                isLoading = false,
                healthMessage = null,
                healthOk = null,
                credits = null,
                isDemoMode = isDemoMode,
                activeProfile = AgentProfile(),
                account = account,
                cloudProfiles = cloudProfiles,
                activeCloudProfile = activeCloudProfile,
                secrets = secrets,
                onApiKeyChanged = {},
                onBaseUrlChanged = {},
                onSaveConfig = {},
                onTestConnection = {},
                onClearApiKey = {},
                onClearCache = {},
                onModelChanged = {},
                onActivateCloudProfile = onActivateCloudProfile,
                onDeleteCloudProfile = onDeleteCloudProfile,
                onRenameCloudProfile = onRenameCloudProfile,
                onAddSecret = onAddSecret,
                onDeleteSecret = onDeleteSecret
            )
        }
    }

    @Test
    fun `account card shows signed-in email and organization`() {
        composeTestRule.setContent(settingsTestContent(
            account = UserInfoDto(email = "dev@example.com", orgName = "Acme", role = "member")
        ))
        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("account_section"))
        composeTestRule.onNodeWithText("Signed in as dev@example.com").assertExists()
        composeTestRule.onNodeWithText("Organization: Acme · member").assertExists()
    }

    @Test
    fun `cloud profile rows render with active badge and activate fires callback`() {
        var activated: String? = null
        composeTestRule.setContent(settingsTestContent(
            cloudProfiles = listOf(
                LlmProfileDto(name = "default", model = "anthropic/claude-sonnet-4"),
                LlmProfileDto(name = "cheap", model = "openai/gpt-5-mini")
            ),
            activeCloudProfile = "default",
            onActivateCloudProfile = { activated = it }
        ))
        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("btn_activate_profile_cheap"))
        composeTestRule.onNodeWithText("default").assertExists()
        composeTestRule.onNodeWithText("cheap").assertExists()
        composeTestRule.onNodeWithTag("active_profile_badge").assertExists()
        // The active profile shows no Activate button of its own.
        composeTestRule.onNodeWithTag("btn_activate_profile_default").assertDoesNotExist()
        composeTestRule.onNodeWithTag("btn_activate_profile_cheap").performClick()
        assertTrue(activated == "cheap")
    }

    @Test
    fun `deleting a cloud profile requires confirmation`() {
        var deleted: String? = null
        composeTestRule.setContent(settingsTestContent(
            cloudProfiles = listOf(LlmProfileDto(name = "staging")),
            onDeleteCloudProfile = { deleted = it }
        ))
        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("btn_delete_profile_staging"))
        composeTestRule.onNodeWithTag("btn_delete_profile_staging").performClick()
        composeTestRule.onNodeWithText("Delete cloud profile?").assertExists()
        assertTrue(deleted == null)
        composeTestRule.onNodeWithTag("btn_confirm_delete_profile").performClick()
        assertTrue(deleted == "staging")
    }

    @Test
    fun `renaming a cloud profile submits the new name`() {
        var renamed: Pair<String, String>? = null
        composeTestRule.setContent(settingsTestContent(
            cloudProfiles = listOf(LlmProfileDto(name = "old-name")),
            onRenameCloudProfile = { from, to -> renamed = from to to }
        ))
        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("btn_rename_profile_old-name"))
        composeTestRule.onNodeWithTag("btn_rename_profile_old-name").performClick()
        composeTestRule.onNodeWithText("Rename profile").assertExists()
        composeTestRule.onNodeWithTag("field_rename_profile").performTextClearance()
        composeTestRule.onNodeWithTag("field_rename_profile").performTextInput("old-name-v2")
        composeTestRule.onNodeWithTag("btn_confirm_rename").performClick()
        assertTrue(renamed == "old-name" to "old-name-v2")
    }

    @Test
    fun `add secret dialog collects name value and description`() {
        var created: Triple<String, String, String?>? = null
        composeTestRule.setContent(settingsTestContent(
            secrets = emptyList(),
            onAddSecret = { n, v, d -> created = Triple(n, v, d) }
        ))
        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("btn_add_secret"))
        composeTestRule.onNodeWithTag("btn_add_secret").performClick()
        composeTestRule.onNodeWithTag("btn_confirm_add_secret").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("field_secret_name").performTextInput("API_KEY")
        composeTestRule.onNodeWithTag("field_secret_value").performTextInput("hunter2")
        composeTestRule.onNodeWithTag("field_secret_description").performTextInput("Deploy key")
        composeTestRule.onNodeWithTag("btn_confirm_add_secret").performClick()
        assertTrue(created == Triple("API_KEY", "hunter2", "Deploy key"))
    }

    @Test
    fun `deleting a secret requires confirmation`() {
        var deleted: String? = null
        composeTestRule.setContent(settingsTestContent(
            secrets = listOf(SecretDto(name = "API_KEY", description = "Deploy key")),
            onDeleteSecret = { deleted = it }
        ))
        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("btn_delete_secret_API_KEY"))
        composeTestRule.onNodeWithText("Deploy key").assertExists()
        composeTestRule.onNodeWithTag("btn_delete_secret_API_KEY").performClick()
        composeTestRule.onNodeWithText("Delete secret?").assertExists()
        assertTrue(deleted == null)
        composeTestRule.onNodeWithTag("btn_confirm_delete_secret").performClick()
        assertTrue(deleted == "API_KEY")
    }

    @Test
    fun `add secret dialog gates Add on a valid name and a non-blank value`() {
        var created: Triple<String, String, String?>? = null
        composeTestRule.setContent(settingsTestContent(
            secrets = emptyList(),
            onAddSecret = { n, v, d -> created = Triple(n, v, d) }
        ))
        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("btn_add_secret"))
        composeTestRule.onNodeWithTag("btn_add_secret").performClick()

        // Empty form: nothing to submit.
        composeTestRule.onNodeWithTag("btn_confirm_add_secret").assertIsNotEnabled()

        // The cloud rejects anything outside [A-Za-z0-9_]; a hyphen must keep Add disabled.
        composeTestRule.onNodeWithTag("field_secret_name").performTextInput("API-KEY")
        composeTestRule.onNodeWithTag("field_secret_value").performTextInput("hunter2")
        composeTestRule.onNodeWithTag("btn_confirm_add_secret").assertIsNotEnabled()

        // A valid name enables Add, and confirming delivers name/value/description.
        composeTestRule.onNodeWithTag("field_secret_name").performTextClearance()
        composeTestRule.onNodeWithTag("field_secret_name").performTextInput("API_KEY")
        composeTestRule.onNodeWithTag("btn_confirm_add_secret").assertIsEnabled()
        composeTestRule.onNodeWithTag("btn_confirm_add_secret").performClick()
        assertTrue(created == Triple("API_KEY", "hunter2", ""))
    }

    @Test
    fun `delete secret confirmation names the specific secret`() {
        var deleted: String? = null
        composeTestRule.setContent(settingsTestContent(
            secrets = listOf(
                SecretDto(name = "API_KEY", description = "Deploy key"),
                SecretDto(name = "OTHER_KEY")
            ),
            onDeleteSecret = { deleted = it }
        ))
        composeTestRule.onNodeWithTag("settings_screen")
            .performScrollToNode(hasTestTag("btn_delete_secret_OTHER_KEY"))
        composeTestRule.onNodeWithTag("btn_delete_secret_OTHER_KEY").performClick()
        composeTestRule.onNodeWithText("Delete secret?").assertExists()
        // The dialog must name the specific secret so a mis-tap can be caught.
        composeTestRule
            .onNodeWithText("'OTHER_KEY' will be removed from your account. This cannot be undone.")
            .assertExists()
        assertTrue(deleted == null)
        composeTestRule.onNodeWithTag("btn_confirm_delete_secret").performClick()
        assertTrue(deleted == "OTHER_KEY")
    }

    @Test
    fun `cloud sections stay hidden in demo mode`() {
        composeTestRule.setContent(settingsTestContent(isDemoMode = true))
        composeTestRule.onNodeWithTag("btn_add_secret").assertDoesNotExist()
        composeTestRule.onNodeWithTag("btn_refresh_cloud").assertDoesNotExist()
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
                    onModelChanged = {},
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
    fun `model selection is visible and switchable from the chat header`() {
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

        var currentModel by mutableStateOf("openhands/deepseek-v4.1-flash")
        composeTestRule.setContent {
            MyApplicationTheme {
                ConversationDetailScreen(
                    conversation = conversation,
                    events = emptyList(),
                    isSending = false,
                    isDemoMode = false,
                    activeProfile = AgentProfile(model = currentModel),
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
                    onModelChanged = { modelChanged = it; currentModel = it },
                    onResetProfile = {},
                    availableModels = listOf("openhands/deepseek-v4-flash", "openhands/deepseek-v4.1-flash")
                )
            }
        }

        // The header exposes the active model.
        composeTestRule.onNodeWithTag("agent_profile_badge").assertExists()
        composeTestRule.onNodeWithTag("agent_profile_label")
            .assertTextEquals("deepseek-v4.1-flash")

        // Tapping opens the switcher; choosing a catalogue model calls through.
        composeTestRule.onNodeWithTag("agent_profile_badge").performClick()
        composeTestRule.onNodeWithText("Agent & LLM · this conversation").assertExists()
        composeTestRule.onNodeWithText("deepseek-v4-flash").performClick()
        composeTestRule.onNodeWithTag("btn_profile_done").performClick()
        assertTrue(modelChanged == "openhands/deepseek-v4-flash")
    }
}
