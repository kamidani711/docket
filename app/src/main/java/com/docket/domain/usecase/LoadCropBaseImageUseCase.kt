package com.docket.domain.usecase

import android.graphics.Bitmap
import com.docket.domain.model.ScanPage
import com.docket.domain.repository.ImageProcessor
import javax.inject.Inject

/**
 * The crop editor needs the page in its *native, unrotated* orientation — corners are
 * normalized against the original decode, before rotation or filter are applied — so this
 * deliberately does not call [ImageProcessor.applyRotation] or `.applyFilter`, unlike
 * [GeneratePagePreviewUseCase]/[ExportScanSessionUseCase]. A trivial one-line delegation, but
 * kept as its own use case so the ViewModel never touches [ImageProcessor] directly.
 */
class LoadCropBaseImageUseCase @Inject constructor(
    private val imageProcessor: ImageProcessor
) {
    suspend operator fun invoke(page: ScanPage, maxDimension: Int): Bitmap =
        imageProcessor.decodeDownsampled(page.originalImagePath, maxDimension)
}
