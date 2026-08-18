package com.docket.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.docket.ui.theme.DocketDimens
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.theme.docketCardShadow

/**
 * Shared shell for the app's "list item card" components (FolderCard, ReceiptRow,
 * WarrantyCard): a leading icon slot, a content column, an optional trailing slot, all on a
 * softly-shadowed surface (no hard outline — see [com.docket.ui.theme.docketCardShadow]). Not
 * exported — it's layout plumbing, not a component in its own right per the brief.
 *
 * Tappable rows scale down very slightly on press — a cheap, universal micro-interaction that
 * makes the whole list feel responsive rather than static.
 */
@Composable
internal fun DocketListRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leading: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(120),
        label = "rowPressScale"
    )
    // One card radius across the app (see Shape.kt) — rows share it with document thumbnails
    // and grid cards rather than picking their own.
    val shape = MaterialTheme.shapes.medium

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = DocketDimens.minTouchTarget)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .docketCardShadow(shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = shape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DocketSpacing.space16, vertical = DocketSpacing.space16),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space12)
        ) {
            leading()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DocketSpacing.space4),
                content = content
            )
            trailing?.invoke()
        }
    }
}
