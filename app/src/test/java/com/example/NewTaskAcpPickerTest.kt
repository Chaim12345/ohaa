package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.AgentProfileInfoDto
import com.example.data.model.PromptTemplate
import com.example.ui.screens.NewTaskScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins the New Task ACP picker: it must load profiles on open, show only the ACP
 * ones, and report the tapped profile id so `agent_profile_id` reaches the start
 * request.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp", sdk = [36])
class NewTaskAcpPickerTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val profiles = listOf(
        AgentProfileInfoDto(id = "p1", name = "claude", agentKind = "acp", acpServer = "claude-code"),
        AgentProfileInfoDto(id = "p2", name = "codex", agentKind = "acp", acpServer = "codex"),
        AgentProfileInfoDto(id = "p3", name = "default", agentKind = "openhands"),
    )

    private fun setScreen(
        onChanged: (String?) -> Unit = {},
        onLoad: () -> Unit = {},
        selection: String? = null,
    ) {
        composeTestRule.setContent {
            MyApplicationTheme {
                NewTaskScreen(
                    promptText = "",
                    repoText = "",
                    templates = emptyList<PromptTemplate>(),
                    isLoading = false,
                    agentProfiles = profiles,
                    agentProfileSelection = selection,
                    onPromptChanged = {},
                    onRepoChanged = {},
                    onSelectTemplate = {},
                    onLaunchTask = {},
                    onAgentProfileChanged = onChanged,
                    onLoadAgentProfiles = onLoad,
                )
            }
        }
    }

    @Test
    fun `picker loads profiles on open`() {
        var loaded = false
        setScreen(onLoad = { loaded = true })
        composeTestRule.waitForIdle()
        assertTrue("loadAgentProfiles was not invoked", loaded)
    }

    @Test
    fun `only acp profiles are offered`() {
        setScreen()
        composeTestRule.onNodeWithTag("new_task_acp_profile_p1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("new_task_acp_profile_p2").performScrollTo().assertIsDisplayed()
        // The OpenHands profile must not appear as an ACP choice.
        composeTestRule.onNodeWithTag("new_task_acp_profile_p3").assertDoesNotExist()
    }

    @Test
    fun `tapping a profile reports its id`() {
        var selected: String? = null
        setScreen(onChanged = { selected = it })
        composeTestRule.onNodeWithTag("new_task_acp_profile_p1").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("p1", selected)
    }

    @Test
    fun `tapping the selected profile clears the selection`() {
        var selected: String? = "p1"
        setScreen(onChanged = { selected = it }, selection = "p1")
        composeTestRule.onNodeWithTag("new_task_acp_profile_p1").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(null, selected)
    }
}
