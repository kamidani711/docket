package com.docket.data.imaging

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import com.docket.domain.model.NormalizedPoint
import com.docket.domain.model.Quad
import com.docket.domain.model.ScanFilter
import com.docket.domain.repository.ImageProcessor
import java.io.File
import javax.inject.Inject
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [ImageProcessor] implementation. No OpenCV: [applyCrop]'s perspective correction uses
 * `android.graphics.Matrix.setPolyToPoly`, which solves a full projective transform for the
 * 4-point case — plain Android graphics, no extra dependency, exactly per the brief ("OpenCV
 * only if ML Kit's edge detection proves insufficient").
 */
class BitmapTransformer @Inject constructor() : ImageProcessor {

    override suspend fun readDimensions(imagePath: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imagePath, options)
        options.outWidth to options.outHeight
    }

    override suspend fun decodeDownsampled(imagePath: String, maxDimension: Int): Bitmap =
        withContext(Dispatchers.Default) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(imagePath, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, maxDimension)
            }
            BitmapFactory.decodeFile(imagePath, options)
                ?: error("Failed to decode image at $imagePath")
        }

    override suspend fun decodeFullResolution(imagePath: String): Bitmap = withContext(Dispatchers.Default) {
        BitmapFactory.decodeFile(imagePath) ?: error("Failed to decode image at $imagePath")
    }

    override fun applyCrop(bitmap: Bitmap, corners: Quad): Bitmap {
        if (corners == Quad.FullFrame) return bitmap
        val width = bitmap.width.toFloat()
        val height = bitmap.height.toFloat()

        // Shrink the quad toward its own centroid by CROP_INSET_FRACTION before mapping to pixel
        // space, so a slightly-loose ML Kit crop doesn't leave a sliver of desk/background baked
        // into every saved page — see DocumentEnhancer's class doc for the full pipeline this is
        // step 1 of.
        val inset = insetQuad(corners, DocumentEnhancer.CROP_INSET_FRACTION)

        // The quad in the *bitmap's own* pixel space — corners are normalized against whatever
        // resolution this particular bitmap was decoded at (preview or full-res), so this works
        // unchanged for both.
        val srcQuad = floatArrayOf(
            inset.topLeft.x * width, inset.topLeft.y * height,
            inset.topRight.x * width, inset.topRight.y * height,
            inset.bottomRight.x * width, inset.bottomRight.y * height,
            inset.bottomLeft.x * width, inset.bottomLeft.y * height
        )

        // Output size follows the quad's own (average) side lengths rather than the source
        // bitmap's size, so a corner drag that trims a sliver off the page actually shrinks the
        // output instead of stretching a smaller region back up to full size.
        val outWidth = maxOf(
            distance(srcQuad[0], srcQuad[1], srcQuad[2], srcQuad[3]),
            distance(srcQuad[6], srcQuad[7], srcQuad[4], srcQuad[5])
        ).roundToInt().coerceAtLeast(1)
        val outHeight = maxOf(
            distance(srcQuad[0], srcQuad[1], srcQuad[6], srcQuad[7]),
            distance(srcQuad[2], srcQuad[3], srcQuad[4], srcQuad[5])
        ).roundToInt().coerceAtLeast(1)

        val dstRect = floatArrayOf(
            0f, 0f,
            outWidth.toFloat(), 0f,
            outWidth.toFloat(), outHeight.toFloat(),
            0f, outHeight.toFloat()
        )

        val matrix = Matrix()
        matrix.setPolyToPoly(srcQuad, 0, dstRect, 0, 4)

        val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(bitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        return output
    }

    override fun applyRotation(bitmap: Bitmap, degrees: Int): Bitmap {
        if (((degrees % 360) + 360) % 360 == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun applyFilter(bitmap: Bitmap, filter: ScanFilter): Bitmap = when (filter) {
        // Real per-pixel processing, not a ColorMatrix approximation — shadow removal, adaptive
        // binarization, contrast stretch, and unsharp mask. See DocumentEnhancer's class doc.
        ScanFilter.BLACK_AND_WHITE -> DocumentEnhancer.blackAndWhite(bitmap)
        ScanFilter.ENHANCE -> DocumentEnhancer.enhance(bitmap)
        // Original/Greyscale are deliberately "don't touch the photo" options — stay on the fast,
        // hardware-backed ColorMatrix path.
        ScanFilter.ORIGINAL, ScanFilter.GREYSCALE -> {
            val colorMatrix = ScanFilters.colorMatrixFor(filter) ?: return bitmap
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(colorMatrix)
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            output
        }
    }

    /** Shrinks [quad] toward its own centroid by [fraction] of its own size, uniformly on all 4
     *  corners — this is what keeps a sliver of background out of the crop regardless of the
     *  quad's shape or rotation. */
    private fun insetQuad(quad: Quad, fraction: Float): Quad {
        val cx = (quad.topLeft.x + quad.topRight.x + quad.bottomRight.x + quad.bottomLeft.x) / 4f
        val cy = (quad.topLeft.y + quad.topRight.y + quad.bottomRight.y + quad.bottomLeft.y) / 4f
        fun shrink(p: NormalizedPoint) = NormalizedPoint(
            x = p.x + (cx - p.x) * fraction,
            y = p.y + (cy - p.y) * fraction
        )
        return Quad(
            topLeft = shrink(quad.topLeft),
            topRight = shrink(quad.topRight),
            bottomRight = shrink(quad.bottomRight),
            bottomLeft = shrink(quad.bottomLeft)
        )
    }

    override suspend fun saveJpeg(bitmap: Bitmap, destinationPath: String, quality: Int) =
        withContext(Dispatchers.IO) {
            File(destinationPath).outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            Unit
        }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float =
        hypot(x2 - x1, y2 - y1)

    /** BitmapFactory only honors power-of-2 sample sizes; this is the standard doubling search. */
    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        var longestSide = maxOf(width, height)
        while (longestSide / 2 >= maxDimension) {
            sampleSize *= 2
            longestSide /= 2
        }
        return sampleSize
    }
}
