package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.ui.screens.ConversationDetailScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins slash-command dispatch in the conversation input: recognised commands run
 * their real action, plain text is sent to the agent, and an unknown command is
 * reported instead of being silently dropped or sent as a message.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp", sdk = [36])
class ConversationSlashCommandTest {

    @get:Rule val composeTestRule = createComposeRule()

    private var sent: String? = null
    private var cleared = false
    private var checkedInit = false
    private var unknown: String? = null

    private fun setScreen() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ConversationDetailScreen(
                    conversation = null,
                    events = emptyList(),
                    isSendingMessage = false,
                    onBack = {},
                    onRefresh = {},
                    onSendMessage = { sent = it },
                    onDelete = {},
                    onClearEvents = { cleared = true },
                    onCheckInit = { checkedInit = true },
                    onUnknownCommand = { unknown = it },
                )
            }
        }
    }

    private fun typeAndSend(text: String) {
        composeTestRule.onNodeWithTag("follow_up_input").performTextInput(text)
        composeTestRule.onNodeWithTag("send_follow_up_button").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `slash clear triggers the clear action`() {
        setScreen()
        typeAndSend("/clear")
        assertEquals(true, cleared)
        assertEquals(null, sent)
    }

    @Test
    fun `slash init triggers the init check`() {
        setScreen()
        typeAndSend("/init")
        assertEquals(true, checkedInit)
    }

    @Test
    fun `unknown slash command is reported not sent`() {
        setScreen()
        typeAndSend("/bogus")
        assertEquals("/bogus", unknown)
        assertEquals(null, sent)
    }

    @Test
    fun `plain text is sent to the agent`() {
        setScreen()
        typeAndSend("run the tests")
        assertEquals("run the tests", sent)
    }
}
