package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end UI smoke test that drives the real [MainActivity] on a device or
 * emulator. It asserts the app is navigable and that credential entry persists,
 * without any live network: every screen must render and stay responsive when
 * the configured endpoint is unreachable.
 */
@RunWith(AndroidJUnit4::class)
class AppUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun tag(t: String) = composeRule.onNodeWithTag(t)

    /** Asserts the tagged node is present in the semantics tree (on- or off-screen). */
    private fun androidx.compose.ui.test.SemanticsNodeInteraction.onNodeExists() {
        fetchSemanticsNode()
    }

    @Test
    fun bottomNavigationSwitchesBetweenAllScreens() {
        val destinations = listOf(
            "nav_dashboard" to "hero_banner_card",
            "nav_tasks" to "tasks_search_input",
            "fab_new_task" to "task_prompt_input",
            "nav_resources" to "resources_refresh",
            "nav_settings" to "settings_api_key_input",
        )
        tag("bottom_nav_bar").assertIsDisplayed()
        for ((navTag, landmark) in destinations) {
            tag(navTag).performClick()
            composeRule.waitForIdle()
            // Landmarks may sit inside scrollable lists, so assert the node
            // exists in the semantics tree rather than requiring it on-screen.
            tag(landmark).onNodeExists()
        }
        // API Explorer is reached from Settings > Developer, not the bottom bar.
        tag("nav_explorer").performScrollTo().performClick()
        composeRule.waitForIdle()
        tag("explorer_surface_cloud").onNodeExists()
    }

    @Test
    fun credentialsArePersistedWhenSaved() {
        // Point at an unroutable host so the save-triggered health check fails
        // fast and offline instead of touching the real cloud.
        val prefs = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getSharedPreferences("openhands_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        tag("nav_settings").performClick()
        composeRule.waitForIdle()

        tag("settings_api_key_input").performScrollTo().performTextReplacement("oh-test-key")
        tag("settings_base_url_input").performScrollTo()
            .performTextReplacement("http://127.0.0.1:9")
        tag("save_config_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals("oh-test-key", prefs.getString("api_key", null))
        assertEquals("http://127.0.0.1:9", prefs.getString("base_url", null))
    }

    @Test
    fun resourcesProfilesExposesAcpAgentProfileEditor() {
        tag("nav_resources").performClick()
        composeRule.waitForIdle()
        // The tab row is a LazyRow; "Profiles" may start off-screen, so scroll
        // the row container until the chip is brought into view, then click it.
        tag("resources_tabs").performScrollToNode(hasTestTag("resources_tab_Profiles"))
        tag("resources_tab_Profiles").performClick()
        composeRule.waitForIdle()

        tag("agent_profile_name_input").performScrollTo().assertIsDisplayed()
        tag("agent_profile_name_input").performTextInput("claude-code")
        tag("agent_profile_server_input").performScrollTo()
            .performTextInput("claude-code-acp")
        tag("agent_profile_model_input").performScrollTo()
            .performTextInput("claude-sonnet-4-5")
        tag("agent_profile_save").performScrollTo().assertIsDisplayed()
    }
}
