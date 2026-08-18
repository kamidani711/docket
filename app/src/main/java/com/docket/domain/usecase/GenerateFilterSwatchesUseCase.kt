package com.docket.domain.usecase

import android.graphics.Bitmap
import com.docket.domain.model.Quad
import com.docket.domain.model.ScanFilter
import com.docket.domain.model.ScanPage
import com.docket.domain.repository.ImageProcessor
import javax.inject.Inject

/**
 * One small base render (crop + rotation applied, no filter) → all four filters applied to
 * copies of it, for the Review screen's live filter-picker thumbnails. Not cached — these are
 * tiny and only computed when the filter picker for a given page is actually open.
 */
class GenerateFilterSwatchesUseCase @Inject constructor(
    private val imageProcessor: ImageProcessor
) {
    suspend operator fun invoke(page: ScanPage, maxDimension: Int): Map<ScanFilter, Bitmap> {
        var base = imageProcessor.decodeDownsampled(page.originalImagePath, maxDimension)
        if (page.corners != Quad.FullFrame) {
            base = base.replaceWith(imageProcessor.applyCrop(base, page.corners))
        }
        if (page.rotationDegrees != 0) {
            base = base.replaceWith(imageProcessor.applyRotation(base, page.rotationDegrees))
        }
        // `base` is handed out as the ORIGINAL entry rather than recycled — caller owns every
        // bitmap in the returned map from here on.
        return ScanFilter.entries.associateWith { filter ->
            if (filter == ScanFilter.ORIGINAL) base else imageProcessor.applyFilter(base, filter)
        }
    }

    private fun Bitmap.replaceWith(next: Bitmap): Bitmap {
        if (this !== next) recycle()
        return next
    }
}
