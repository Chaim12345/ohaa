package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** User-selectable appearance. [SYSTEM] follows the device setting. */
enum class ThemeMode { SYSTEM, DARK, LIGHT }

private val DarkColorScheme =
  darkColorScheme(
    primary = OpenHandsPrimaryDark,
    secondary = OpenHandsSecondaryDark,
    tertiary = OpenHandsTertiaryDark,
    background = OpenHandsBackgroundDark,
    surface = OpenHandsSurfaceDark,
    surfaceVariant = OpenHandsSurfaceVariantDark,
    onPrimary = OpenHandsBackgroundDark,
    onSecondary = OpenHandsBackgroundDark,
    onTertiary = OpenHandsBackgroundDark,
    onBackground = OpenHandsTextPrimaryDark,
    onSurface = OpenHandsTextPrimaryDark,
    onSurfaceVariant = OpenHandsTextSecondaryDark,
    outline = OpenHandsBorderDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = OpenHandsPrimaryLight,
    secondary = OpenHandsSecondaryLight,
    tertiary = OpenHandsTertiaryLight,
    background = OpenHandsBackgroundLight,
    surface = OpenHandsSurfaceLight,
    surfaceVariant = OpenHandsSurfaceVariantLight,
    onPrimary = OpenHandsSurfaceLight,
    onSecondary = OpenHandsSurfaceLight,
    onTertiary = OpenHandsSurfaceLight,
    onBackground = OpenHandsTextPrimaryLight,
    onSurface = OpenHandsTextPrimaryLight,
    onSurfaceVariant = OpenHandsTextSecondaryLight,
    outline = OpenHandsBorderLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  CompositionLocalProvider(LocalAppIsDark provides darkTheme) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}

/** Resolves a user [ThemeMode] (including [ThemeMode.SYSTEM]) to a color scheme. */
@Composable
fun MyApplicationTheme(
  themeMode: ThemeMode,
  content: @Composable () -> Unit,
) {
  val dark = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
  }
  MyApplicationTheme(darkTheme = dark, content = content)
}

