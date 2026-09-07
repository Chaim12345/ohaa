package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
  darkTheme: Boolean = true, // Default to rich developer dark theme, toggleable
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

