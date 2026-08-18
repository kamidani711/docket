package com.docket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.docket.ui.theme.DocketDimens
import com.docket.ui.theme.DocketSpacing

/**
 * Used wherever a list/grid has nothing in it yet (empty Library, no receipts, no
 * warranties...). [actionLabel]/[onAction] are both required together or omitted together —
 * pass neither for a purely informational empty state.
 *
 * Renders [icon] inside a soft tinted circle by default — a plain glyph floating in whitespace
 * reads as unfinished, a contained one reads as designed. Pass [illustration] to replace that
 * with something more considered (Library's own hand-drawn page-stack, for instance) for the
 * empty states a user is likely to actually see.
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Inbox,
    illustration: (@Composable () -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DocketSpacing.space32, vertical = DocketSpacing.space48),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
    ) {
        if (illustration != null) {
            illustration()
        } else {
            Box(
                modifier = Modifier
                    .size(DocketDimens.heroIconSize + DocketSpacing.space24)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(DocketSpacing.space4)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (actionLabel != null && onAction != null) {
            PrimaryButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier
                    .padding(top = DocketSpacing.space8)
                    .fillMaxWidth(0.7f)
            )
        }
    }
}
