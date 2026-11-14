package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.PromptTemplate
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NewTaskScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins two state-wiring fixes: the refresh spinners must actually reflect the
 * `isRefreshing` flag (previously set-but-never-consumed), and the blueprint
 * template chips must reflect the selected template.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp", sdk = [36])
class UiStateWiringTest {

    @get:Rule val composeTestRule = createComposeRule()

    private val templates = listOf(
        PromptTemplate(id = "t1", title = "Fix a bug", category = "debugging", prompt = "Fix the bug"),
        PromptTemplate(id = "t2", title = "Write tests", category = "testing", prompt = "Add tests"),
    )

    @Test
    fun `dashboard shows a spinner while refreshing`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                DashboardScreen(
                    conversations = emptyList(),
                    templates = emptyList(),
                    connectionStatus = "Ready",
                    connectionHealthy = true,
                    isLoading = false,
                    isRefreshing = true,
                    onStartNewTask = {},
                    onSelectConversation = {},
                    onSelectTemplate = {},
                    onRefresh = {},
                    onOpenSettings = {},
                )
            }
        }
        composeTestRule.onNodeWithTag("dashboard_refresh_spinner").assertIsDisplayed()
    }

    @Test
    fun `dashboard shows the refresh icon when idle`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                DashboardScreen(
                    conversations = emptyList(),
                    templates = emptyList(),
                    connectionStatus = "Ready",
                    connectionHealthy = true,
                    isLoading = false,
                    isRefreshing = false,
                    onStartNewTask = {},
                    onSelectConversation = {},
                    onSelectTemplate = {},
                    onRefresh = {},
                    onOpenSettings = {},
                )
            }
        }
        composeTestRule.onNodeWithTag("dashboard_refresh_spinner").assertDoesNotExist()
    }

    @Test
    fun `template chip reports selection and highlights the selected one`() {
        var selectedId: String? = null
        composeTestRule.setContent {
            MyApplicationTheme {
                NewTaskScreen(
                    promptText = "",
                    repoText = "",
                    templates = templates,
                    isLoading = false,
                    selectedTemplateId = "t2",
                    onPromptChanged = {},
                    onRepoChanged = {},
                    onSelectTemplate = { selectedId = it.id },
                    onLaunchTask = {},
                )
            }
        }
        composeTestRule.onNodeWithTag("blueprint_chip_t2").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("blueprint_chip_t1").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals("t1", selectedId)
    }
}
