package com.docket.ui.screens.review

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.docket.R
import com.docket.domain.model.NormalizedPoint
import com.docket.domain.model.Quad
import com.docket.ui.components.PrimaryButton
import com.docket.ui.components.SecondaryButton
import com.docket.ui.theme.DocketSpacing

/**
 * Draggable-corner re-crop. The image container is forced to the bitmap's *own* aspect ratio
 * (`Modifier.aspectRatio`), so the rendered image exactly fills its box with no letterboxing —
 * that's what lets touch coordinates map straight to normalized [0,1] space with no separate
 * "where did ContentScale.Fit actually place the image" math.
 */
@Composable
fun CropEditor(
    baseImage: Bitmap?,
    initialCorners: Quad,
    onConfirm: (Quad) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (baseImage == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var corners by remember(baseImage) { mutableStateOf(initialCorners) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(baseImage.width.toFloat() / baseImage.height.toFloat())
                .onSizeChanged { canvasSize = Size(it.width.toFloat(), it.height.toFloat()) }
                .pointerInput(baseImage) {
                    var activeHandle: CropHandle? = null
                    detectDragGestures(
                        onDragStart = { start ->
                            activeHandle = corners.nearestHandle(start, canvasSize, HANDLE_TOUCH_SLOP_PX)
                        },
                        onDragEnd = { activeHandle = null },
                        onDragCancel = { activeHandle = null },
                        onDrag = { change, _ ->
                            change.consume()
                            val handle = activeHandle ?: return@detectDragGestures
                            if (canvasSize.width <= 0f || canvasSize.height <= 0f) return@detectDragGestures
                            val normalized = NormalizedPoint(
                                x = (change.position.x / canvasSize.width).coerceIn(0f, 1f),
                                y = (change.position.y / canvasSize.height).coerceIn(0f, 1f)
                            )
                            corners = corners.withHandle(handle, normalized)
                        }
                    )
                }
        ) {
            Image(
                bitmap = baseImage.asImageBitmap(),
                contentDescription = stringResource(R.string.review_page_being_cropped_desc),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCropOverlay(corners)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DocketSpacing.space20),
            horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space12)
        ) {
            SecondaryButton(text = "Cancel", onClick = onCancel, modifier = Modifier.weight(1f))
            PrimaryButton(text = "Done", onClick = { onConfirm(corners) }, modifier = Modifier.weight(1f))
        }
    }
}

private enum class CropHandle { TOP_LEFT, TOP_RIGHT, BOTTOM_RIGHT, BOTTOM_LEFT }

private fun Quad.nearestHandle(point: Offset, canvasSize: Size, thresholdPx: Float): CropHandle? {
    val candidates = listOf(
        CropHandle.TOP_LEFT to topLeft,
        CropHandle.TOP_RIGHT to topRight,
        CropHandle.BOTTOM_RIGHT to bottomRight,
        CropHandle.BOTTOM_LEFT to bottomLeft
    )
    var closest: CropHandle? = null
    var closestDistanceSq = Float.MAX_VALUE
    for ((handle, normalizedPoint) in candidates) {
        val dx = normalizedPoint.x * canvasSize.width - point.x
        val dy = normalizedPoint.y * canvasSize.height - point.y
        val distanceSq = dx * dx + dy * dy
        if (distanceSq < closestDistanceSq) {
            closestDistanceSq = distanceSq
            closest = handle
        }
    }
    return closest.takeIf { closestDistanceSq <= thresholdPx * thresholdPx }
}

private fun Quad.withHandle(handle: CropHandle, point: NormalizedPoint): Quad = when (handle) {
    CropHandle.TOP_LEFT -> copy(topLeft = point)
    CropHandle.TOP_RIGHT -> copy(topRight = point)
    CropHandle.BOTTOM_RIGHT -> copy(bottomRight = point)
    CropHandle.BOTTOM_LEFT -> copy(bottomLeft = point)
}

private fun DrawScope.drawCropOverlay(corners: Quad) {
    val points = listOf(corners.topLeft, corners.topRight, corners.bottomRight, corners.bottomLeft)
        .map { Offset(it.x * size.width, it.y * size.height) }

    for (i in points.indices) {
        drawLine(
            color = Color.White,
            start = points[i],
            end = points[(i + 1) % points.size],
            strokeWidth = 3.dp.toPx()
        )
    }
    points.forEach { point ->
        drawCircle(color = Color.White, radius = 14.dp.toPx(), center = point, style = Stroke(width = 3.dp.toPx()))
        drawCircle(color = Color.White, radius = 6.dp.toPx(), center = point)
    }
}

private const val HANDLE_TOUCH_SLOP_PX = 96f
