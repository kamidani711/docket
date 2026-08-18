package com.docket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.docket.ui.theme.DocketDimens
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.theme.docketExtendedColors

/**
 * Urgency tier for a warranty's days-remaining indicator. Kept here (UI-only) for now since
 * it only drives label/color; if reminder scheduling (WorkManager) ends up needing the same
 * thresholds, promote this to domain so both sides can't drift apart.
 */
enum class WarrantyUrgency { SAFE, WARNING, URGENT, EXPIRED }

fun warrantyUrgencyOf(daysRemaining: Int): WarrantyUrgency = when {
    daysRemaining < 0 -> WarrantyUrgency.EXPIRED
    daysRemaining <= 7 -> WarrantyUrgency.URGENT
    daysRemaining <= 30 -> WarrantyUrgency.WARNING
    else -> WarrantyUrgency.SAFE
}

private fun warrantyStatusLabel(daysRemaining: Int): String = when {
    daysRemaining < 0 -> "Expired"
    daysRemaining == 0 -> "Expires today"
    daysRemaining == 1 -> "1 day left"
    else -> "$daysRemaining days left"
}

@Composable
fun WarrantyCard(
    itemName: String,
    expiryDateLabel: String,
    daysRemaining: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val urgency = warrantyUrgencyOf(daysRemaining)
    val (chipContainer, chipContent) = urgencyChipColors(urgency)

    DocketListRow(
        modifier = modifier,
        onClick = onClick,
        leading = {
            Box(
                modifier = Modifier
                    .size(DocketDimens.rowIconSize)
                    .background(chipContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = chipContent
                )
            }
        },
        trailing = {
            Surface(color = chipContainer, shape = MaterialTheme.shapes.extraSmall) {
                Text(
                    text = warrantyStatusLabel(daysRemaining),
                    style = MaterialTheme.typography.labelSmall,
                    color = chipContent,
                    modifier = Modifier.padding(
                        horizontal = DocketSpacing.space12,
                        vertical = DocketSpacing.space4
                    )
                )
            }
        }
    ) {
        Text(
            text = itemName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = expiryDateLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun urgencyChipColors(urgency: WarrantyUrgency): Pair<Color, Color> {
    val extended = docketExtendedColors()
    return when (urgency) {
        WarrantyUrgency.SAFE -> extended.safeContainer to extended.onSafeContainer
        WarrantyUrgency.WARNING -> extended.warningContainer to extended.onWarningContainer
        // Urgent and expired both borrow M3's own error semantic rather than adding a third
        // custom color role — see ExtendedColor.kt.
        WarrantyUrgency.URGENT, WarrantyUrgency.EXPIRED ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
}
