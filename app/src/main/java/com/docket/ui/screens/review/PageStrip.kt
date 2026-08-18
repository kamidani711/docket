package com.docket.ui.screens.review

import android.graphics.Bitmap
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.docket.R
import com.docket.domain.model.ScanPage
import com.docket.ui.components.DocumentThumbnail
import com.docket.ui.theme.DocketDimens
import com.docket.ui.theme.DocketSpacing
import kotlin.math.roundToInt

/**
 * Long-press a thumbnail to drag-reorder it. Reordering happens against a *local* copy of the
 * id order — [onReorder] only fires (persisting to Room) when the drag ends — so a slow DB
 * round trip never fights the gesture mid-drag. NOTE: this hand-rolled drag implementation
 * hasn't been exercised on a real device — see the chat reply's perf/testing notes for exactly
 * what to check by hand before trusting it.
 */
@Composable
fun PageStrip(
    pages: List<ScanPage>,
    previewBitmaps: Map<String, Bitmap>,
    selectedPageId: String?,
    onSelect: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onAddPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val idsFromData = pages.map { it.id }
    var orderedIds by remember(idsFromData) { mutableStateOf(idsFromData) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragOffsetX by remember { mutableStateOf(0f) }

    val density = LocalDensity.current
    val itemStridePx = with(density) { (DocketDimens.thumbnailWidth + DocketSpacing.space8).toPx() }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8),
        contentPadding = PaddingValues(horizontal = DocketSpacing.space20)
    ) {
        itemsIndexed(orderedIds, key = { _, id -> id }) { _, id ->
            val page = pages.firstOrNull { it.id == id }
            if (page != null) {
                val isDragging = draggingId == id
                Box(
                    modifier = Modifier
                        .graphicsLayer { translationX = if (isDragging) dragOffsetX else 0f }
                        .zIndex(if (isDragging) 1f else 0f)
                        .pointerInput(id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingId = id
                                    dragOffsetX = 0f
                                },
                                onDrag = { change, dragAmount: Offset ->
                                    change.consume()
                                    dragOffsetX += dragAmount.x
                                    val currentIndex = orderedIds.indexOf(id)
                                    val targetIndex = (currentIndex + (dragOffsetX / itemStridePx).roundToInt())
                                        .coerceIn(0, orderedIds.lastIndex)
                                    if (targetIndex != currentIndex) {
                                        orderedIds = orderedIds.toMutableList().apply {
                                            removeAt(currentIndex)
                                            add(targetIndex, id)
                                        }
                                        dragOffsetX -= (targetIndex - currentIndex) * itemStridePx
                                    }
                                },
                                onDragEnd = {
                                    draggingId = null
                                    dragOffsetX = 0f
                                    onReorder(orderedIds)
                                },
                                onDragCancel = {
                                    draggingId = null
                                    dragOffsetX = 0f
                                }
                            )
                        }
                ) {
                    DocumentThumbnail(
                        pageCount = 1,
                        thumbnailModel = previewBitmaps[id],
                        selected = id == selectedPageId,
                        onClick = { onSelect(id) }
                    )
                }
            }
        }
        item(key = "add_page") {
            AddPageTile(onClick = onAddPage)
        }
    }
}

@Composable
private fun AddPageTile(onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    Surface(
        modifier = Modifier
            .width(DocketDimens.thumbnailWidth)
            .aspectRatio(DocketDimens.pageAspectRatio)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.review_add_page),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
