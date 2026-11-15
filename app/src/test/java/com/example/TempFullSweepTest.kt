package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.AgentEvent
import com.example.data.model.ConversationStatus
import com.example.data.model.OpenHandsConversation
import com.example.data.model.PromptTemplate
import com.example.data.model.SandboxInfoDto
import com.example.data.model.SecretDto
import com.example.data.model.SkillInfoDto
import com.example.ui.screens.ApiExplorerScreen
import com.example.ui.screens.ConversationDetailScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ResourcesScreen
import com.example.ui.screens.TasksScreen
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
class TempFullSweepTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun conv(id: String, title: String, status: ConversationStatus, repo: String?) =
      OpenHandsConversation(
          id = id, title = title, status = status, repository = repo, branch = "main",
          createdAt = 1731600000000, updatedAt = 1731603600000, conversationUrl = null,
          llmModel = "claude-sonnet-4-5")

  private val conversations = listOf(
      conv("c1", "Fix login redirect loop", ConversationStatus.RUNNING, "octocat/Hello-World"),
      conv("c2", "Add rate limiting middleware", ConversationStatus.FINISHED, "pallets/flask"),
      conv("c3", "Write API docs", ConversationStatus.READY, null),
      conv("c4", "Migrate to kotlinx.serialization", ConversationStatus.PAUSED, "torvalds/linux")
  )

  private val templates = listOf(
      PromptTemplate("1", "Fix a failing test", "Bugfix", "Run the tests and fix them"),
      PromptTemplate("2", "Add a feature", "Feature", "Implement the feature"),
      PromptTemplate("3", "Write docs", "Docs", "Document the module")
  )

  private val sampleEvents = listOf(
      AgentEvent("e1", "c1", 1731600000000, "user", "message",
          "The login flow redirects in a loop after OAuth callback. Can you fix it?"),
      AgentEvent("e2", "c1", 1731600060000, "agent", "thought",
          "I'll start by looking at the auth callback handler to see how the redirect target is computed."),
      AgentEvent("e3", "c1", 1731600120000, "agent", "bash",
          "grep -rn 'redirect' src/auth/ | head -20", detail = "exit 0"),
      AgentEvent("e4", "c1", 1731600180000, "environment", "observation",
          "src/auth/callback.ts:42: const target = req.query.next ?? '/login'\nsrc/auth/callback.ts:88: res.redirect(target)"),
      AgentEvent("e5", "c1", 1731600240000, "agent", "edit",
          "Edit src/auth/callback.ts: default next target changed from /login to /dashboard"),
      AgentEvent("e6", "c1", 1731600300000, "agent", "message",
          "Found it - when the OAuth callback has no `next` param it defaulted to /login, which bounces authenticated users back into the flow. Fixed the default to /dashboard and added a regression test.")
  )

  private fun shot(name: String, dark: Boolean = true, content: @androidx.compose.runtime.Composable () -> Unit) {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = dark) {
        // Mirror MainActivity's Scaffold: themed background + content color,
        // so isolated screen captures match on-device rendering.
        androidx.compose.material3.Surface(
            color = androidx.compose.material3.MaterialTheme.colorScheme.background,
            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
        ) { content() }
      }
    }
    composeTestRule.onRoot().captureRoboImage("app/src/test/screenshots/sweep_$name.png")
  }

  @Test fun dashboard_populated() = shot("dashboard_populated") {
    DashboardScreen(conversations, templates, "Cloud API V1: Ready", true,
        onStartNewTask = {}, onSelectConversation = {}, onSelectTemplate = {},
        onRefresh = {}, onOpenSettings = {})
  }

  @Test fun dashboard_populated_light() = shot("dashboard_light", dark = false) {
    DashboardScreen(conversations, templates, "Cloud API V1: Ready", true,
        onStartNewTask = {}, onSelectConversation = {}, onSelectTemplate = {},
        onRefresh = {}, onOpenSettings = {})
  }

  @Test fun tasks_populated() = shot("tasks_populated") {
    TasksScreen(conversations, "", null, {}, {}, {}, {}, {})
  }

  @Test fun tasks_empty() = shot("tasks_empty") {
    TasksScreen(emptyList(), "", null, {}, {}, {}, {}, {})
  }

  @Test fun resources_populated() = shot("resources_populated") {
    ResourcesScreen(
        secrets = listOf(
            SecretDto("s1", "GITHUB_TOKEN", "Personal access token", "repo"),
            SecretDto("s2", "NPM_TOKEN", null, "global")),
        skills = listOf(
            SkillInfoDto("commit", "skill", "cloud", "Write conventional commits"),
            SkillInfoDto("review", "skill", "marketplace", "PR review helper")),
        sandboxes = listOf(
            SandboxInfoDto("sb-1", "RUNNING", "spec-default", createdAt = "2026-11-15T08:00:00Z"),
            SandboxInfoDto("sb-2", "PAUSED", "spec-default", createdAt = "2026-11-14T08:00:00Z")),
        gitLogin = "Chaim12345",
        onLoadSecrets = {}, onLoadSkills = {}, onLoadSandboxes = {},
        onLoadGitAccount = {}, onLoadGitOrganizations = {}, onLoadGitInstallations = {},
        onLoadSettings = {}, onLoadWebClientConfig = {}, onSaveSettings = {},
        onStoreGitToken = { _, _ -> }, onUnlinkGitProvider = {},
        onCreateSecret = { _, _, _ -> }, onDeleteSecret = {}, onDeleteSandbox = {})
  }

  @Test fun resources_light() = shot("resources_light", dark = false) {
    ResourcesScreen(
        secrets = listOf(SecretDto("s1", "GITHUB_TOKEN", "Personal access token", "repo")),
        skills = listOf(SkillInfoDto("commit", "skill", "cloud", "Write conventional commits")),
        sandboxes = listOf(SandboxInfoDto("sb-1", "RUNNING", "spec-default")),
        gitLogin = "Chaim12345",
        onLoadSecrets = {}, onLoadSkills = {}, onLoadSandboxes = {},
        onLoadGitAccount = {}, onLoadGitOrganizations = {}, onLoadGitInstallations = {},
        onLoadSettings = {}, onLoadWebClientConfig = {}, onSaveSettings = {},
        onStoreGitToken = { _, _ -> }, onUnlinkGitProvider = {},
        onCreateSecret = { _, _, _ -> }, onDeleteSecret = {}, onDeleteSandbox = {})
  }

  @Test fun api_explorer() = shot("api_explorer") {
    ApiExplorerScreen(
        cloudPaths = listOf("/api/v1/app-conversations", "/api/v1/conversations/{id}", "/api/v1/skills"),
        cloudAdminPaths = listOf("/api/v1/admin/users"),
        agentPaths = listOf("/api/conversations", "/api/events"),
        response = "{\n  \"id\": \"conv-1\",\n  \"status\": \"RUNNING\"\n}",
        isLoading = false,
        onCall = { _, _, _, _ -> })
  }

  @Test fun conversation_light() = shot("conv_light", dark = false) {
    ConversationDetailScreen(
        conversation = conv("c1", "Fix login redirect loop", ConversationStatus.RUNNING, "octocat/Hello-World"),
        events = sampleEvents, isSendingMessage = false,
        onBack = {}, onRefresh = {}, onSendMessage = {}, onDelete = {}, onResume = {})
  }
}
