package com.docket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.docket.ui.theme.DocketDimens

@Composable
fun FolderCard(
    name: String,
    documentCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DocketListRow(
        modifier = modifier,
        onClick = onClick,
        leading = {
            Box(
                modifier = Modifier
                    .size(DocketDimens.rowIconSize)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailing = {
            // Chevron flips automatically under RTL — AutoMirrored icon + Compose's own
            // layout-direction-aware placement, no manual mirroring needed.
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // NOTE: plain English pluralization for now — this is the layout/RTL pass, not the
        // localization pass. Route through string resources (with proper plural rules for
        // Arabic/Urdu) before ship.
        Text(
            text = if (documentCount == 1) "1 document" else "$documentCount documents",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
