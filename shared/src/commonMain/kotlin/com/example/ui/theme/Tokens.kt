package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing, shape and sizing tokens.
 *
 * Screens previously hard-coded `8.dp`/`12.dp`/`RoundedCornerShape(12.dp)` in
 * dozens of places, which is why padding drifted between screens. Everything
 * visual now reads from here so the rebuild stays consistent.
 */
object Spacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp

    /** Standard screen gutter. */
    val screen: Dp = 16.dp

    /** Vertical gap between stacked cards. */
    val cardGap: Dp = Spacing.md
}

/** Corner radii. Larger for surfaces, smaller for chips/controls. */
object Shapes {
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val pill = RoundedCornerShape(percent = 50)
}

/**
 * Minimum interactive size. Android's accessibility guidance is 48dp; anything
 * smaller is hard to hit one-handed. Icon buttons in the old UI were 16dp icons
 * with no padding, which is why they were fiddly on a phone.
 */
object Sizes {
    val touchTarget: Dp = 48.dp
    val iconSmall: Dp = 16.dp
    val iconMedium: Dp = 20.dp
    val iconLarge: Dp = 24.dp
    val statusDot: Dp = 8.dp
    val avatar: Dp = 32.dp
}

/**
 * Semantic colours resolved from the active Material scheme, so light/dark both
 * work without screens branching on theme.
 */
object CanvasColors {
    val border: androidx.compose.ui.graphics.Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outline

    val borderSubtle: androidx.compose.ui.graphics.Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.outlineVariant

    val surfaceRaised: androidx.compose.ui.graphics.Color
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceContainerHigh

    val success: androidx.compose.ui.graphics.Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) CanvasSuccess else CanvasSuccessLight

    // No dedicated light warning token exists yet, so the dark constant is used
    // in both themes rather than inventing one here.
    val warning: androidx.compose.ui.graphics.Color
        @Composable @ReadOnlyComposable get() = CanvasWarning

    val danger: androidx.compose.ui.graphics.Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) CanvasDanger else MaterialTheme.colorScheme.error
}
