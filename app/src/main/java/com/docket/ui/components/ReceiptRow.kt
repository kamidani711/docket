package com.docket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.docket.ui.icons.DocketIcons
import com.docket.ui.theme.DocketDimens

/**
 * [amount] and [date] are pre-formatted strings — currency/number/date formatting is a
 * locale concern that belongs in the domain/data layer, not hardcoded into this component.
 */
@Composable
fun ReceiptRow(
    merchant: String,
    amount: String,
    date: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    DocketListRow(
        modifier = modifier,
        onClick = onClick,
        leading = {
            // Secondary container distinguishes receipts from folders (primaryContainer) without
            // introducing a new hue — reuses the existing secondary role.
            Box(
                modifier = Modifier
                    .size(DocketDimens.rowIconSize)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = DocketIcons.Receipt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        },
        trailing = {
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    ) {
        Text(
            text = merchant,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
