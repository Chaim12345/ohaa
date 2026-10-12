package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CanvasColors
import com.example.ui.theme.Shapes
import com.example.ui.theme.Sizes
import com.example.ui.theme.Spacing

/**
 * Shared UI primitives.
 *
 * The pre-rebuild screens each re-implemented their own card, section header,
 * empty state and status dot, which drifted in padding and corner radius. These
 * are the single definitions the rebuilt screens use.
 */

/** Standard bordered surface used for every card in the app. */
@Composable
fun CanvasCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accent: Color? = null,
    content: @Composable () -> Unit
) {
    val base = modifier
        .fillMaxWidth()
        .then(
            if (accent != null) {
                Modifier.background(accent.copy(alpha = 0.06f), Shapes.medium)
            } else Modifier
        )
        .border(1.dp, CanvasColors.borderSubtle, Shapes.medium)
        .background(MaterialTheme.colorScheme.surface, Shapes.medium)

    Box(
        base.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Column(Modifier.padding(Spacing.md)) { content() }
    }
}

/** Section title with optional trailing action. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * Small coloured dot + label, used for agent/conversation status.
 * The label is always present so status is not conveyed by colour alone.
 */
@Composable
fun StatusDot(
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
    pulsing: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            Modifier
                .size(Sizes.statusDot)
                .background(color, Shapes.pill)
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

/** Compact label chip (tags, counts, modes). */
@Composable
fun MetaChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null
) {
    val tint = color ?: MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier
            .background(tint.copy(alpha = 0.12f), Shapes.pill)
            .padding(horizontal = Spacing.sm, vertical = 2.dp)
    ) {
        Text(text, fontSize = 10.sp, color = tint, maxLines = 1)
    }
}

/** Centred empty state with an optional action. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.xl)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(Spacing.md))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.lg)
        )
        if (action != null) {
            Spacer(Modifier.height(Spacing.lg))
            action()
        }
    }
}

/** Inline error/notice strip with a retry affordance. */
@Composable
fun InlineNotice(
    message: String,
    modifier: Modifier = Modifier,
    isError: Boolean = true,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val tint = if (isError) CanvasColors.danger else CanvasColors.warning
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(tint.copy(alpha = 0.10f), Shapes.small)
            .border(1.dp, tint.copy(alpha = 0.35f), Shapes.small)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.width(Spacing.sm))
            Text(
                actionLabel,
                style = MaterialTheme.typography.labelMedium,
                color = tint,
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .padding(Spacing.xs)
                    .testTag("notice_action")
            )
        }
    }
}

/** Key/value row for detail panels. */
@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    monospace: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(104.dp)
        )
        Text(
            value,
            style = if (monospace) MaterialTheme.typography.bodySmall.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            ) else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Thin separator matching the app border colour. */
@Composable
fun CanvasDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CanvasColors.borderSubtle)
    )
}
