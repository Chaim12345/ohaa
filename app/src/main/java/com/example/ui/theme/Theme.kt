package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Whether the active color scheme is the dark one, readable deep in the tree. */
val LocalAppIsDark = staticCompositionLocalOf { true }

/**
 * Semantic status colors that follow the active theme. The base constants in
 * Color.kt are tuned for the dark surface; the *Light variants meet contrast on
 * the light surface. Read these in composables instead of the raw constants.
 */
object StatusColors {
  val working: Color @Composable get() = if (LocalAppIsDark.current) StatusWorking else StatusWorkingLight
  val ready: Color @Composable get() = if (LocalAppIsDark.current) StatusReady else StatusReadyLight
  val waiting: Color @Composable get() = if (LocalAppIsDark.current) StatusWaiting else StatusWaitingLight
  val error: Color @Composable get() = if (LocalAppIsDark.current) StatusError else StatusErrorLight
  val paused: Color @Composable get() = if (LocalAppIsDark.current) StatusPaused else StatusPausedLight
}

/** User-selectable appearance. [SYSTEM] follows the device setting. */
enum class ThemeMode { SYSTEM, DARK, LIGHT }

/** True when the app is currently rendering with the dark color scheme. */
val LocalAppIsDark = staticCompositionLocalOf { false }

/** Per-theme status colors so light mode gets AA-passing variants. */
object StatusColors {
  val working @Composable get() = if (LocalAppIsDark.current) StatusWorking else StatusWorkingLight
  val ready @Composable get() = if (LocalAppIsDark.current) StatusReady else StatusReadyLight
  val waiting @Composable get() = if (LocalAppIsDark.current) StatusWaiting else StatusWaitingLight
  val error @Composable get() = if (LocalAppIsDark.current) StatusError else StatusErrorLight
  val paused @Composable get() = if (LocalAppIsDark.current) StatusPaused else StatusPausedLight
}

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

