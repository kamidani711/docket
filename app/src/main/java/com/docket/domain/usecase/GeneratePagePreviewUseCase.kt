package com.docket.domain.usecase

import android.graphics.Bitmap
import com.docket.domain.model.Quad
import com.docket.domain.model.ScanFilter
import com.docket.domain.model.ScanPage
import com.docket.domain.repository.ImageProcessor
import com.docket.domain.repository.PreviewCache
import javax.inject.Inject

/**
 * Renders (or returns the cached) downsampled preview for one page: decode small → crop →
 * rotate → filter. Separate from [ExportScanSessionUseCase]'s full-resolution render — same
 * shape, deliberately not shared, since one processes a handful of small bitmaps repeatedly and
 * the other processes large ones exactly once each.
 */
class GeneratePagePreviewUseCase @Inject constructor(
    private val imageProcessor: ImageProcessor,
    private val previewCache: PreviewCache
) {
    suspend operator fun invoke(page: ScanPage, maxDimension: Int): Bitmap {
        val key = previewCache.keyFor(page.id, page.corners, page.rotationDegrees, page.filter)
        previewCache.get(key)?.let { return it }

        var bitmap = imageProcessor.decodeDownsampled(page.originalImagePath, maxDimension)
        if (page.corners != Quad.FullFrame) {
            bitmap = bitmap.replaceWith(imageProcessor.applyCrop(bitmap, page.corners))
        }
        if (page.rotationDegrees != 0) {
            bitmap = bitmap.replaceWith(imageProcessor.applyRotation(bitmap, page.rotationDegrees))
        }
        if (page.filter != ScanFilter.ORIGINAL) {
            bitmap = bitmap.replaceWith(imageProcessor.applyFilter(bitmap, page.filter))
        }
        previewCache.put(key, bitmap)
        return bitmap
    }

    private fun Bitmap.replaceWith(next: Bitmap): Bitmap {
        if (this !== next) recycle()
        return next
    }
}
