package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.example.ui.screens.ConversationDetailScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins the slash-command palette filtering: typing a partial command must narrow
 * the palette to the matching commands instead of always listing every command.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp", sdk = [36])
class SlashPaletteFilterTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun setScreen() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ConversationDetailScreen(
                    conversation = null,
                    events = emptyList(),
                    isSendingMessage = false,
                    onBack = {},
                    onRefresh = {},
                    onSendMessage = {},
                    onDelete = {},
                )
            }
        }
    }

    @Test
    fun `typing a prefix narrows the palette to matching commands`() {
        setScreen()
        composeTestRule.onNodeWithTag("follow_up_input").performTextInput("/mo")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("slash_palette").assertIsDisplayed()
        composeTestRule.onNodeWithText("/model  Switch model").assertIsDisplayed()
        composeTestRule.onNodeWithText("/compact  Compact the conversation context").assertDoesNotExist()
    }

    @Test
    fun `a bare slash lists commands starting from the first`() {
        setScreen()
        composeTestRule.onNodeWithTag("follow_up_input").performTextInput("/")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("/compact  Compact the conversation context").assertIsDisplayed()
    }

    @Test
    fun `an unmatched prefix shows the no-match hint`() {
        setScreen()
        composeTestRule.onNodeWithTag("follow_up_input").performTextInput("/zzz")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(
            "No matching command — press send to try \"/zzz\""
        ).assertIsDisplayed()
    }
}
