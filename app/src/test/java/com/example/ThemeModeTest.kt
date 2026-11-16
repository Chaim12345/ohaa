package com.example

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.OpenHandsBackgroundDark
import com.example.ui.theme.OpenHandsBackgroundLight
import com.example.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pins that the persisted [ThemeMode] actually selects a color scheme: forcing
 * LIGHT must yield the light background and DARK the dark one, regardless of the
 * device's own setting.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp", sdk = [36])
class ThemeModeTest {

    @get:Rule val composeTestRule = createComposeRule()

    private fun backgroundFor(mode: ThemeMode): Color {
        var captured = Color.Unspecified
        composeTestRule.setContent {
            MyApplicationTheme(themeMode = mode) {
                captured = MaterialTheme.colorScheme.background
            }
        }
        composeTestRule.waitForIdle()
        return captured
    }

    @Test
    fun `light mode uses the light background`() {
        assertEquals(OpenHandsBackgroundLight, backgroundFor(ThemeMode.LIGHT))
    }

    @Test
    fun `dark mode uses the dark background`() {
        assertEquals(OpenHandsBackgroundDark, backgroundFor(ThemeMode.DARK))
    }
}
