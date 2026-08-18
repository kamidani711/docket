package com.docket.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.docket.ui.theme.DocketDimens
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.theme.docketCardShadow

/*
 * Three button tiers, and a deliberate rule: the accent color (MaterialTheme.colorScheme
 * .primary) appears ONLY on [PrimaryButton] — a flat fill, never a gradient. SecondaryButton
 * and [DocketTextButton] both use neutral ink (onSurface) for their border/text instead of
 * M3's stock behavior of tinting outlined/text buttons with primary — that stock behavior is
 * exactly the "everything is brand-colored" clutter the design brief calls "overdesigned" for
 * a document app; restraint is the whole visual position here, not a flourish to work around.
 *
 * [PrimaryButton] does carry a soft tinted shadow and a subtle (<150ms) press-scale — motion
 * and elevation read as "well-made instrument," not decoration, so they stay.
 */

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val isEnabled = enabled && !loading
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && isEnabled) 0.97f else 1f,
        animationSpec = tween(120),
        label = "primaryButtonScale"
    )
    val shape = MaterialTheme.shapes.medium

    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = DocketDimens.primaryTouchTarget)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (isEnabled) {
                    Modifier.docketCardShadow(elevation = DocketSpacing.space8, shape = shape)
                } else {
                    Modifier
                }
            ),
        enabled = isEnabled,
        shape = shape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        elevation = null,
        contentPadding = PaddingValues(
            horizontal = DocketSpacing.space24,
            vertical = DocketSpacing.space16
        )
    ) {
        DocketButtonContent(text = text, loading = loading, leadingIcon = leadingIcon)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    // Default matches the original fixed padding; overridable for tight layouts (e.g. three
    // buttons in a row) where the 24dp default wraps a short label like "Share" onto two lines.
    contentPadding: PaddingValues = PaddingValues(
        horizontal = DocketSpacing.space24,
        vertical = DocketSpacing.space12
    )
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "secondaryButtonScale"
    )
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = DocketDimens.minTouchTarget)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        interactionSource = interactionSource,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = contentPadding
    ) {
        DocketButtonContent(text = text, loading = loading, leadingIcon = leadingIcon)
    }
}

/**
 * Named `DocketTextButton` (not `TextButton`) to avoid colliding with M3's own composable of
 * that name, which this wraps.
 */
@Composable
fun DocketTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = DocketDimens.minTouchTarget),
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(
            horizontal = DocketSpacing.space16,
            vertical = DocketSpacing.space12
        )
    ) {
        DocketButtonContent(text = text, loading = loading, leadingIcon = leadingIcon)
    }
}

@Composable
private fun DocketButtonContent(text: String, loading: Boolean, leadingIcon: ImageVector?) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = LocalContentColor.current
        )
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)
        ) {
            if (leadingIcon != null) {
                Icon(imageVector = leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
