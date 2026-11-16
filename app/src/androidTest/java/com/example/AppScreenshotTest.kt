package com.example

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Drives the real [MainActivity] and writes a PNG of every reachable screen to
 * the app's external files dir, where CI pulls them for the pull-request gallery.
 *
 * Only offline-reachable screens are captured: the conversation-scoped sheets
 * (model/context/terminal) need a live credential, so they are out of scope here.
 * Captures are best-effort — a missing tag skips that image instead of failing
 * the run, so the gallery is still produced when a screen changes.
 */
@RunWith(AndroidJUnit4::class)
class AppScreenshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val outDir: File by lazy {
        File(
            InstrumentationRegistry.getInstrumentation()
                .targetContext.getExternalFilesDir(null),
            "screenshots"
        ).apply { mkdirs() }
    }

    private fun capture(name: String) = runCatching {
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        File(outDir, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    /** True when the node is inside a scrollable container (safe to scroll to). */
    private fun isInScrollable(tag: String): Boolean = runCatching {
        val node = composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode()
        var parent: androidx.compose.ui.semantics.SemanticsNode? = node.parent
        while (parent != null) {
            if (parent.config.contains(androidx.compose.ui.semantics.SemanticsActions.ScrollBy) ||
                parent.config.contains(androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange)
            ) return@runCatching true
            parent = parent.parent
        }
        false
    }.getOrDefault(false)

    private fun tap(tag: String) = runCatching {
        val node = composeRule.onNodeWithTag(tag)
        // performScrollTo throws on nodes with no scrollable ancestor; only
        // scroll when the node actually sits inside one.
        if (isInScrollable(tag)) node.performScrollTo()
        node.performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun captureAllScreens() {
        val tabs = listOf(
            "dashboard", "tasks", "new_task", "resources", "explorer", "settings"
        )
        for (tab in tabs) {
            tap("nav_$tab")
            capture("tab_$tab")
        }

        // New Task, scrolled down to the ACP agent-profile picker.
        tap("nav_new_task")
        if (isInScrollable("new_task_acp_picker")) {
            runCatching { composeRule.onNodeWithTag("new_task_acp_picker").performScrollTo() }
            composeRule.waitForIdle()
        }
        capture("new_task_acp")

        // Resources, one capture per section.
        tap("nav_resources")
        for (section in listOf(
            "Secrets", "Skills", "Sandboxes", "Git", "Profiles", "Settings"
        )) {
            tap("resources_tab_$section")
            capture("resources_${section.lowercase()}")
        }
    }
}
