package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CanvasPrimary,
    onPrimary = CanvasBackground,
    primaryContainer = CanvasPrimaryDeep.copy(alpha = 0.25f),
    onPrimaryContainer = CanvasPrimary,
    secondary = CanvasSecondary,
    onSecondary = CanvasBackground,
    secondaryContainer = CanvasSecondary.copy(alpha = 0.2f),
    onSecondaryContainer = CanvasSecondary,
    tertiary = CanvasSuccess,
    onTertiary = CanvasBackground,
    error = CanvasDanger,
    background = CanvasBackground,
    onBackground = CanvasTextPrimary,
    surface = CanvasSurface,
    onSurface = CanvasTextPrimary,
    surfaceVariant = CanvasSurfaceVariant,
    onSurfaceVariant = CanvasTextSecondary,
    surfaceContainer = CanvasSurface,
    surfaceContainerHigh = CanvasSurfaceHigh,
    surfaceContainerHighest = CanvasSurfaceHigh,
    outline = CanvasBorder,
    outlineVariant = CanvasBorder.copy(alpha = 0.6f)
)

private val LightColorScheme = lightColorScheme(
    primary = CanvasPrimaryLight,
    onPrimary = CanvasSurfaceLight,
    primaryContainer = CanvasPrimaryLight.copy(alpha = 0.14f),
    onPrimaryContainer = CanvasPrimaryLight,
    secondary = CanvasSecondaryLight,
    onSecondary = CanvasSurfaceLight,
    secondaryContainer = CanvasSecondaryLight.copy(alpha = 0.14f),
    onSecondaryContainer = CanvasSecondaryLight,
    tertiary = CanvasSuccessLight,
    onTertiary = CanvasSurfaceLight,
    error = CanvasDanger,
    background = CanvasBackgroundLight,
    onBackground = CanvasTextPrimaryLight,
    surface = CanvasSurfaceLight,
    onSurface = CanvasTextPrimaryLight,
    surfaceVariant = CanvasSurfaceVariantLight,
    onSurfaceVariant = CanvasTextSecondaryLight,
    surfaceContainer = CanvasSurfaceLight,
    surfaceContainerHigh = CanvasSurfaceVariantLight,
    surfaceContainerHighest = CanvasSurfaceVariantLight,
    outline = CanvasBorderLight,
    outlineVariant = CanvasBorderLight.copy(alpha = 0.6f)
)

@Composable
fun MyApplicationTheme(
    // Follows the system appearance by default; tests/previews can force a mode.
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}
