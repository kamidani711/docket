package com.docket.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.docket.ui.theme.DocketDimens
import com.docket.ui.theme.DocketSpacing

/**
 * One row in a [BottomSheetMenu]. [destructive] renders it in the error color (e.g. Delete).
 * [onClick] is deliberately the last constructor param (after the defaulted [destructive]) so
 * call sites can use trailing-lambda syntax: `DocketMenuAction("Share", icon) { ... }`.
 */
data class DocketMenuAction(
    val label: String,
    val icon: ImageVector,
    val destructive: Boolean = false,
    val onClick: () -> Unit
)

/**
 * Document action sheet (rename, move, share, delete, ...). Deliberately doesn't
 * auto-dismiss after a tap — call site owns `onDismissRequest`/visibility state, so a
 * multi-step action (e.g. one that opens a confirmation dialog) isn't cut short.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetMenu(
    actions: List<DocketMenuAction>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(bottom = DocketSpacing.space24, top = DocketSpacing.space8)) {
            actions.forEach { action ->
                val contentColor = if (action.destructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                val iconContainer = if (action.destructive) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = DocketDimens.minTouchTarget)
                        .clickable(onClick = action.onClick)
                        .padding(horizontal = DocketSpacing.space24, vertical = DocketSpacing.space8),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(iconContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(text = action.label, style = MaterialTheme.typography.bodyLarge, color = contentColor)
                }
            }
        }
    }
}
