package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.AgentProfileInfoDto
import com.example.data.model.PromptTemplate
import com.example.ui.screens.NewTaskScreen
import com.example.ui.screens.ResourcesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class TempPr11ScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun new_task_with_acp_profiles() {
    composeTestRule.setContent {
      MyApplicationTheme {
        NewTaskScreen(
            promptText = "",
            repoText = "",
            templates = listOf(
                PromptTemplate("1", "Fix a failing test", "Bugfix", "Run the tests and fix them"),
                PromptTemplate("2", "Add a feature", "Feature", "Implement the feature")
            ),
            isLoading = false,
            agentProfiles = listOf(
                AgentProfileInfoDto(id = "p1", name = "Claude Code (ACP)", agentKind = "acp", acpServer = "claude-code", acpModel = "claude-sonnet-4-5"),
                AgentProfileInfoDto(id = "p2", name = "Codex (ACP)", agentKind = "acp", acpServer = "codex", acpModel = "gpt-5-codex"),
                AgentProfileInfoDto(id = "p3", name = "OpenHands Default", agentKind = "default")
            ),
            onPromptChanged = {},
            onRepoChanged = {},
            onSelectTemplate = {},
            onLaunchTask = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage("app/src/test/screenshots/temp_newtask.png")
  }

  @Test
  fun settings_screen() {
    composeTestRule.setContent {
      MyApplicationTheme {
        SettingsScreen(
            apiKey = "",
            baseUrl = "https://app.all-hands.dev",
            isLoading = false,
            connectionStatus = "Cloud API V1: Ready",
            connectionHealthy = true,
            onApiKeyChanged = {},
            onBaseUrlChanged = {},
            onSaveConfig = {},
            onTestConnection = {},
            onClearCache = {},
            keepAliveEnabled = true,
            themeMode = "SYSTEM"
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage("app/src/test/screenshots/temp_settings.png")
  }

  @Test
  fun resources_screen() {
    composeTestRule.setContent {
      MyApplicationTheme {
        ResourcesScreen(
            secrets = emptyList(),
            skills = emptyList(),
            sandboxes = emptyList(),
            gitLogin = null,
            onLoadSecrets = {},
            onLoadSkills = {},
            onLoadSandboxes = {},
            onLoadGitAccount = {},
            onLoadGitOrganizations = {},
            onLoadGitInstallations = {},
            onLoadSettings = {},
            onLoadWebClientConfig = {},
            onSaveSettings = {},
            onStoreGitToken = { _, _ -> },
            onUnlinkGitProvider = {},
            onCreateSecret = { _, _, _ -> },
            onDeleteSecret = {},
            onDeleteSandbox = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage("app/src/test/screenshots/temp_resources.png")
  }
}
