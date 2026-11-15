package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.AgentEvent
import com.example.data.model.ConversationStatus
import com.example.data.model.OpenHandsConversation
import com.example.ui.screens.ConversationDetailScreen
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
class TempConversationScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  private fun conv(status: ConversationStatus) = OpenHandsConversation(
      id = "conv-1",
      title = "Fix login redirect loop",
      status = status,
      repository = "octocat/Hello-World",
      branch = "main",
      createdAt = 1731600000000,
      updatedAt = 1731603600000,
      conversationUrl = null,
      llmModel = "claude-sonnet-4-5"
  )

  private val sampleEvents = listOf(
      AgentEvent("e1", "conv-1", 1731600000000, "user", "message",
          "The login flow redirects in a loop after OAuth callback. Can you fix it?"),
      AgentEvent("e2", "conv-1", 1731600060000, "agent", "thought",
          "I'll start by looking at the auth callback handler to see how the redirect target is computed."),
      AgentEvent("e3", "conv-1", 1731600120000, "agent", "bash",
          "grep -rn 'redirect' src/auth/ | head -20", detail = "exit 0"),
      AgentEvent("e4", "conv-1", 1731600180000, "environment", "observation",
          "src/auth/callback.ts:42: const target = req.query.next ?? '/login'\nsrc/auth/callback.ts:88: res.redirect(target)"),
      AgentEvent("e5", "conv-1", 1731600240000, "agent", "edit",
          "Edit src/auth/callback.ts: default next target changed from /login to /dashboard"),
      AgentEvent("e6", "conv-1", 1731600300000, "agent", "message",
          "Found it - when the OAuth callback has no `next` param it defaulted to /login, which bounces authenticated users back into the flow. Fixed the default to /dashboard and added a regression test.")
  )

  @Test
  fun conversation_running_populated() {
    composeTestRule.setContent {
      MyApplicationTheme {
        ConversationDetailScreen(
            conversation = conv(ConversationStatus.RUNNING),
            events = sampleEvents,
            isSendingMessage = false,
            onBack = {},
            onRefresh = {},
            onSendMessage = {},
            onDelete = {},
            onResume = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage("app/src/test/screenshots/temp_conv_running.png")
  }

  @Test
  fun conversation_awaiting_confirmation() {
    composeTestRule.setContent {
      MyApplicationTheme {
        ConversationDetailScreen(
            conversation = conv(ConversationStatus.AWAITING_CONFIRMATION),
            events = sampleEvents,
            isSendingMessage = false,
            onBack = {},
            onRefresh = {},
            onSendMessage = {},
            onDelete = {},
            onResume = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage("app/src/test/screenshots/temp_conv_confirm.png")
  }

  @Test
  fun conversation_empty_running() {
    composeTestRule.setContent {
      MyApplicationTheme {
        ConversationDetailScreen(
            conversation = conv(ConversationStatus.RUNNING),
            events = emptyList(),
            isSendingMessage = false,
            onBack = {},
            onRefresh = {},
            onSendMessage = {},
            onDelete = {},
            onResume = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage("app/src/test/screenshots/temp_conv_empty.png")
  }
}
