package com.docket.domain.repository

import android.graphics.Bitmap
import com.docket.domain.model.Quad
import com.docket.domain.model.ScanFilter

/**
 * Bitmap operations behind an interface (implemented by `data/imaging/BitmapTransformer`) so
 * the export/preview use cases don't hardcode a specific image-processing approach — e.g. if
 * ML Kit's auto-crop ever turns out insufficient and we bring in OpenCV per the brief, only the
 * implementation changes.
 *
 * This is the one spot in `domain` that references `android.graphics.*` directly. Docket is an
 * Android-only app (no shared/multiplatform target), so a fully platform-agnostic domain layer
 * buys nothing here — same pragmatic call already made for Room usage elsewhere.
 */
interface ImageProcessor {
    /** Reads just the bounds (no pixel data) — cheap, used to size the crop editor canvas. */
    suspend fun readDimensions(imagePath: String): Pair<Int, Int>

    /** Decodes downsampled so the result's longest side is close to [maxDimension]. For previews. */
    suspend fun decodeDownsampled(imagePath: String, maxDimension: Int): Bitmap

    /** Decodes at full resolution. Caller is responsible for recycling promptly. For export only. */
    suspend fun decodeFullResolution(imagePath: String): Bitmap

    /** Perspective-crops [bitmap] to [corners] (normalized against [bitmap]'s own dimensions). */
    fun applyCrop(bitmap: Bitmap, corners: Quad): Bitmap

    fun applyRotation(bitmap: Bitmap, degrees: Int): Bitmap

    fun applyFilter(bitmap: Bitmap, filter: ScanFilter): Bitmap

    /** Writes [bitmap] to [destinationPath] as JPEG. Does not recycle [bitmap]. */
    suspend fun saveJpeg(bitmap: Bitmap, destinationPath: String, quality: Int = 90)
}
