package com.docket.data.imaging

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Per-pixel document cleanup for the Black & White and Enhance filters. Original and Greyscale
 * stay on the fast [ScanFilters] ColorMatrix path — they're deliberately "don't touch the photo"
 * options, so there's nothing here for them to do.
 *
 * No OpenCV: every step is plain Kotlin over [Bitmap.getPixels]/[Bitmap.setPixels] IntArrays, per
 * the brief ("OpenCV only if this proves insufficient, and imgproc-only if so"). The one thing
 * that makes this fast enough for a large-radius shadow blur is [boxBlur] — a sliding-window box
 * blur whose cost per pixel is O(1) regardless of radius, not O(radius). Three box-blur passes
 * approximate a true Gaussian closely enough to estimate a page's lighting/shadow field, which is
 * the standard trick most fast image editors use for large blurs instead of a literal Gaussian
 * kernel (which would be O(radius) per pixel and far too slow at this radius/resolution).
 *
 * Pipeline (matches the brief's numbered steps, applied in this order):
 *  1. Crop-corner inset — see [insetQuadFraction] usage in [BitmapTransformer.applyCrop], not here.
 *  2. Shadow/illumination removal — [removeShadow]: divide by a large-blur "background" estimate.
 *  3. [blackAndWhite]: Sauvola local adaptive threshold ([sauvolaThreshold]) on the shadow-corrected
 *     image, sharpened first (see the note on step 5 below).
 *  4. [enhance]: shadow removal + percentile contrast stretch ([contrastStretch]), color preserved.
 *  5. Unsharp mask ([unsharpMask]) — applied to the continuous-tone image *before* binarization in
 *     the B&W path (sharpening a hard 0/255 image is a no-op at best, ringing at worst — sharpening
 *     the grayscale first gives Sauvola crisper edges to threshold against), and *after* the
 *     contrast stretch in the Enhance path (a literal last step there, as asked).
 *
 * Memory/perf: everything above [ENHANCEMENT_MAX_DIMENSION] gets downscaled first. At that cap
 * (2000px longest side, ~5MP), peak working set for the whole pipeline is a handful of IntArray/
 * FloatArray/LongArray buffers around 20-40MB apiece — comfortably inside a 2GB-class device's
 * per-app heap, and released as soon as each function returns. Measured wall-clock time on this
 * project's x86_64 emulator (not a real mid-range ARM phone — that's an honest caveat, not a
 * substitute for on-device profiling) is in [DocumentEnhancerTest].
 */
object DocumentEnhancer {

    /** 1.5% -- shrinks the crop quad toward its own centroid before perspective correction, so a
     *  slightly-loose ML Kit crop doesn't leave a sliver of desk/background baked into the page.
     *  Used by [BitmapTransformer.applyCrop]. */
    const val CROP_INSET_FRACTION = 0.015f

    /** Cap on the working resolution for the per-pixel steps below. 2000px longest side is well
     *  past what on-screen viewing or OCR needs (ML Kit itself recommends ~1000-2000px for OCR),
     *  and bounding it here is what keeps memory and time predictable regardless of how many
     *  megapixels the source camera shoots. */
    private const val ENHANCEMENT_MAX_DIMENSION = 2000

    /** Radius of the large blur used to estimate the page's lighting/shadow field, as a fraction
     *  of the image's longest side. Large enough to smooth away text and creases (leaving just
     *  the slow-varying shadow/lighting signal), small enough not to degenerate into one flat
     *  value across the whole page. */
    private const val SHADOW_BLUR_RADIUS_FRACTION = 1f / 8f

    /** Sauvola's own sensitivity constant. 0.34 is the commonly-cited default in the literature. */
    private const val SAUVOLA_K = 0.34f

    /** Dynamic range of the local standard deviation for 8-bit images -- part of Sauvola's
     *  standard formula, not a tunable in the usual sense. */
    private const val SAUVOLA_R = 128f

    /** Local window radius for Sauvola, as a fraction of the image's longest side, floored so
     *  small images still get a sane neighborhood. */
    private const val SAUVOLA_WINDOW_FRACTION = 1f / 60f
    private const val SAUVOLA_MIN_WINDOW_RADIUS = 8

    /** Percentile clipped from each end of the luminance histogram before stretching contrast --
     *  guards against a few near-black/near-white outlier pixels blowing out the whole stretch. */
    private const val CONTRAST_CLIP_PERCENTILE = 0.01f

    private const val UNSHARP_RADIUS = 2
    private const val UNSHARP_AMOUNT = 0.6f

    /** Cap on the resolution the shadow/background field is *estimated* at. The signal is
     *  inherently low-frequency (shadows and lighting vary slowly across a page), so estimating
     *  it on a small downscaled copy and upsampling back loses nothing visible while cutting the
     *  three blur passes from full-resolution to a fraction of a percent of the pixel count —
     *  this is the single biggest lever for staying under the 2s/page budget. */
    private const val SHADOW_ESTIMATE_MAX_DIMENSION = 300

    /** Black & white: shadow removal -> unsharp mask -> Sauvola adaptive threshold. Output is a
     *  true per-pixel binary image (0 or 255), not a high-contrast grayscale. */
    fun blackAndWhite(source: Bitmap): Bitmap {
        val working = downscaleIfNeeded(source)
        val width = working.width
        val height = working.height
        val pixels = IntArray(width * height)
        working.getPixels(pixels, 0, width, 0, 0, width, height)

        val gray = toLuminance(pixels)
        val corrected = removeShadowGray(gray, width, height)
        val sharpened = unsharpMask(corrected, width, height)
        val binary = sauvolaThreshold(sharpened, width, height)

        return binaryToBitmap(binary, width, height)
    }

    /** Enhance: shadow removal + percentile contrast stretch, color kept, then unsharp mask as
     *  the final step. */
    fun enhance(source: Bitmap): Bitmap {
        val working = downscaleIfNeeded(source)
        val width = working.width
        val height = working.height
        val pixels = IntArray(width * height)
        working.getPixels(pixels, 0, width, 0, 0, width, height)

        val gray = toLuminance(pixels)
        val background = estimateShadowBackground(gray, width, height)
        val correctedRgb = removeShadowColor(pixels, gray, background)
        val stretchedRgb = contrastStretchColor(correctedRgb)
        val sharpenedRgb = unsharpMaskColor(stretchedRgb, width, height)

        return Bitmap.createBitmap(sharpenedRgb, width, height, Bitmap.Config.ARGB_8888)
    }

    /** One named, timed snapshot of the pipeline's internal state — for the
     *  [com.docket.BuildConfig.DEBUG]-only pipeline inspector screen, never touched by the
     *  shipped [blackAndWhite]/[enhance] path (materializing every intermediate step as its own
     *  Bitmap is a real allocation cost that path has no reason to pay). [elapsedMs] is the time
     *  *since the previous stage*, not cumulative — that's what actually answers "which stage is
     *  slow," a running total doesn't. */
    data class PipelineStage(val label: String, val bitmap: Bitmap, val elapsedMs: Long)

    /** Debug-only mirror of [blackAndWhite] — same steps, same order, same private helper
     *  functions (nothing here re-implements the algorithm), but returns every intermediate
     *  bitmap instead of only the last one. Keep the step sequence below in sync with
     *  [blackAndWhite] if that ever changes. */
    fun blackAndWhiteDebugStages(source: Bitmap): List<PipelineStage> {
        val stages = mutableListOf<PipelineStage>()
        var mark = System.nanoTime()
        fun stage(label: String, bitmap: Bitmap) {
            val now = System.nanoTime()
            stages += PipelineStage(label, bitmap, (now - mark) / 1_000_000)
            mark = now
        }

        val working = downscaleIfNeeded(source)
        val width = working.width
        val height = working.height
        val pixels = IntArray(width * height)
        working.getPixels(pixels, 0, width, 0, 0, width, height)
        stage("Original (working copy, ${width}x$height)", working)

        val gray = toLuminance(pixels)
        val corrected = removeShadowGray(gray, width, height)
        stage("Illumination normalized", grayToBitmap(corrected, width, height))

        val sharpened = unsharpMask(corrected, width, height)
        stage("Sharpened (pre-threshold)", grayToBitmap(sharpened, width, height))

        val binary = sauvolaThreshold(sharpened, width, height)
        stage("Sauvola binarized (final)", binaryToBitmap(binary, width, height))

        return stages
    }

    /** Debug-only mirror of [enhance] — see [blackAndWhiteDebugStages]'s doc; same relationship. */
    fun enhanceDebugStages(source: Bitmap): List<PipelineStage> {
        val stages = mutableListOf<PipelineStage>()
        var mark = System.nanoTime()
        fun stage(label: String, bitmap: Bitmap) {
            val now = System.nanoTime()
            stages += PipelineStage(label, bitmap, (now - mark) / 1_000_000)
            mark = now
        }

        val working = downscaleIfNeeded(source)
        val width = working.width
        val height = working.height
        val pixels = IntArray(width * height)
        working.getPixels(pixels, 0, width, 0, 0, width, height)
        stage("Original (working copy, ${width}x$height)", working)

        val gray = toLuminance(pixels)
        val background = estimateShadowBackground(gray, width, height)
        val correctedRgb = removeShadowColor(pixels, gray, background)
        stage("Illumination normalized", Bitmap.createBitmap(correctedRgb, width, height, Bitmap.Config.ARGB_8888))

        val stretchedRgb = contrastStretchColor(correctedRgb)
        stage("Contrast stretched", Bitmap.createBitmap(stretchedRgb, width, height, Bitmap.Config.ARGB_8888))

        val sharpenedRgb = unsharpMaskColor(stretchedRgb, width, height)
        stage("Sharpened (final)", Bitmap.createBitmap(sharpenedRgb, width, height, Bitmap.Config.ARGB_8888))

        return stages
    }

    private fun grayToBitmap(gray: FloatArray, width: Int, height: Int): Bitmap {
        val out = IntArray(gray.size)
        for (i in gray.indices) {
            val v = gray[i].toInt().coerceIn(0, 255)
            out[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        return Bitmap.createBitmap(out, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun binaryToBitmap(binary: IntArray, width: Int, height: Int): Bitmap {
        val out = IntArray(binary.size)
        for (i in binary.indices) {
            val v = binary[i]
            out[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        return Bitmap.createBitmap(out, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun downscaleIfNeeded(source: Bitmap): Bitmap {
        val longestSide = max(source.width, source.height)
        if (longestSide <= ENHANCEMENT_MAX_DIMENSION) return source
        val scale = ENHANCEMENT_MAX_DIMENSION.toFloat() / longestSide
        val newWidth = max(1, (source.width * scale).roundToIntSafe())
        val newHeight = max(1, (source.height * scale).roundToIntSafe())
        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }

    private fun Float.roundToIntSafe(): Int = (this + 0.5f).toInt()

    private fun shadowBlurRadius(width: Int, height: Int): Int =
        (max(width, height) * SHADOW_BLUR_RADIUS_FRACTION).toInt().coerceAtLeast(4)

    private fun toLuminance(pixels: IntArray): FloatArray {
        val out = FloatArray(pixels.size)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            out[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }
        return out
    }

    /** Grayscale flat-field correction: divide each pixel by a large-blur estimate of the local
     *  "background" (paper + shadow, with text/creases smoothed away), then rescale so the result
     *  sits at a proper white level. This is what flattens uneven lighting and paper shadows. */
    private fun removeShadowGray(gray: FloatArray, width: Int, height: Int): FloatArray {
        val background = estimateShadowBackground(gray, width, height)
        val out = FloatArray(gray.size)
        for (i in gray.indices) {
            val bg = background[i].coerceAtLeast(1f)
            out[i] = (gray[i] / bg * 255f).coerceIn(0f, 255f)
        }
        return out
    }

    /** Color-preserving version: the correction factor comes from luminance alone (one blur, not
     *  three independent per-channel blurs), then the same factor scales all three channels at
     *  each pixel. That preserves hue/relative color instead of shifting it, and is cheaper than
     *  blurring every channel separately. */
    private fun removeShadowColor(pixels: IntArray, gray: FloatArray, background: FloatArray): IntArray {
        val out = IntArray(pixels.size)
        for (i in pixels.indices) {
            val bg = background[i].coerceAtLeast(1f)
            val factor = 255f / bg
            val p = pixels[i]
            val r = (((p shr 16) and 0xFF) * factor).coerceIn(0f, 255f).toInt()
            val g = (((p shr 8) and 0xFF) * factor).coerceIn(0f, 255f).toInt()
            val b = ((p and 0xFF) * factor).coerceIn(0f, 255f).toInt()
            out[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return out
    }

    /** Separable box blur via a running sum ("sliding window") -- O(width*height) total per pass,
     *  independent of [radius]. Edge pixels clamp to the array bounds. */
    private fun boxBlur(src: FloatArray, width: Int, height: Int, radius: Int): FloatArray {
        if (radius < 1) return src.copyOf()
        val temp = FloatArray(src.size)
        val dst = FloatArray(src.size)
        val windowSize = (radius * 2 + 1).toFloat()

        for (y in 0 until height) {
            val rowStart = y * width
            var sum = 0f
            for (x in -radius..radius) sum += src[rowStart + x.coerceIn(0, width - 1)]
            for (x in 0 until width) {
                temp[rowStart + x] = sum / windowSize
                val addX = rowStart + (x + radius + 1).coerceAtMost(width - 1)
                val subX = rowStart + (x - radius).coerceAtLeast(0)
                sum += src[addX] - src[subX]
            }
        }
        for (x in 0 until width) {
            var sum = 0f
            for (y in -radius..radius) sum += temp[y.coerceIn(0, height - 1) * width + x]
            for (y in 0 until height) {
                dst[y * width + x] = sum / windowSize
                val addY = (y + radius + 1).coerceAtMost(height - 1) * width + x
                val subY = (y - radius).coerceAtLeast(0) * width + x
                sum += temp[addY] - temp[subY]
            }
        }
        return dst
    }

    /** Three box-blur passes ~= one Gaussian blur -- the standard fast approximation. */
    private fun approximateGaussianBlur(src: FloatArray, width: Int, height: Int, radius: Int): FloatArray {
        var result = boxBlur(src, width, height, radius)
        result = boxBlur(result, width, height, radius)
        result = boxBlur(result, width, height, radius)
        return result
    }

    /** Estimates the page's lighting/shadow field: downscale (nearest-neighbor -- fine here since
     *  the blur right after smooths out any aliasing), blur small, upsample back to [width]x
     *  [height] with bilinear interpolation. See [SHADOW_ESTIMATE_MAX_DIMENSION]'s doc for why
     *  this is safe to do at low resolution. */
    private fun estimateShadowBackground(gray: FloatArray, width: Int, height: Int): FloatArray {
        val longestSide = max(width, height)
        if (longestSide <= SHADOW_ESTIMATE_MAX_DIMENSION) {
            return approximateGaussianBlur(gray, width, height, shadowBlurRadius(width, height))
        }
        val scale = SHADOW_ESTIMATE_MAX_DIMENSION.toFloat() / longestSide
        val smallWidth = max(1, (width * scale).roundToIntSafe())
        val smallHeight = max(1, (height * scale).roundToIntSafe())

        val small = downsampleNearest(gray, width, height, smallWidth, smallHeight)
        val blurredSmall = approximateGaussianBlur(
            small, smallWidth, smallHeight, shadowBlurRadius(smallWidth, smallHeight)
        )
        return upsampleBilinear(blurredSmall, smallWidth, smallHeight, width, height)
    }

    private fun downsampleNearest(src: FloatArray, srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int): FloatArray {
        val out = FloatArray(dstWidth * dstHeight)
        for (y in 0 until dstHeight) {
            val sy = (y * srcHeight / dstHeight).coerceIn(0, srcHeight - 1)
            for (x in 0 until dstWidth) {
                val sx = (x * srcWidth / dstWidth).coerceIn(0, srcWidth - 1)
                out[y * dstWidth + x] = src[sy * srcWidth + sx]
            }
        }
        return out
    }

    private fun upsampleBilinear(src: FloatArray, srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int): FloatArray {
        val out = FloatArray(dstWidth * dstHeight)
        val scaleX = srcWidth.toFloat() / dstWidth
        val scaleY = srcHeight.toFloat() / dstHeight
        for (y in 0 until dstHeight) {
            val sy = y * scaleY
            val y0 = sy.toInt().coerceIn(0, srcHeight - 1)
            val y1 = (y0 + 1).coerceAtMost(srcHeight - 1)
            val fy = sy - y0
            for (x in 0 until dstWidth) {
                val sx = x * scaleX
                val x0 = sx.toInt().coerceIn(0, srcWidth - 1)
                val x1 = (x0 + 1).coerceAtMost(srcWidth - 1)
                val fx = sx - x0
                val v00 = src[y0 * srcWidth + x0]
                val v01 = src[y0 * srcWidth + x1]
                val v10 = src[y1 * srcWidth + x0]
                val v11 = src[y1 * srcWidth + x1]
                val top = v00 + (v01 - v00) * fx
                val bottom = v10 + (v11 - v10) * fx
                out[y * dstWidth + x] = top + (bottom - top) * fy
            }
        }
        return out
    }

    /** Sauvola local adaptive threshold via integral images (summed-area tables) for the local
     *  mean and variance, so every pixel's threshold is O(1) to compute regardless of window size
     *  -- O(width*height) total, not O(width*height*window). Real per-pixel binarization, unlike
     *  the old ColorMatrix "high contrast grayscale" this replaces: this is what makes creases and
     *  shadow residue actually disappear instead of just fading. */
    private fun sauvolaThreshold(gray: FloatArray, width: Int, height: Int): IntArray {
        val sum = LongArray((width + 1) * (height + 1))
        val sumSq = DoubleArray((width + 1) * (height + 1))
        val stride = width + 1
        for (y in 0 until height) {
            var rowSum = 0L
            var rowSumSq = 0.0
            for (x in 0 until width) {
                val v = gray[y * width + x]
                rowSum += v.toLong()
                rowSumSq += (v * v).toDouble()
                val idx = (y + 1) * stride + (x + 1)
                sum[idx] = sum[idx - stride] + rowSum
                sumSq[idx] = sumSq[idx - stride] + rowSumSq
            }
        }

        val windowRadius = (max(width, height) * SAUVOLA_WINDOW_FRACTION).toInt()
            .coerceAtLeast(SAUVOLA_MIN_WINDOW_RADIUS)

        val out = IntArray(width * height)
        for (y in 0 until height) {
            val y0 = (y - windowRadius).coerceAtLeast(0)
            val y1 = (y + windowRadius).coerceAtMost(height - 1)
            for (x in 0 until width) {
                val x0 = (x - windowRadius).coerceAtLeast(0)
                val x1 = (x + windowRadius).coerceAtMost(width - 1)
                val count = (x1 - x0 + 1).toLong() * (y1 - y0 + 1).toLong()

                val a = y0 * stride + x0
                val b = y0 * stride + (x1 + 1)
                val c = (y1 + 1) * stride + x0
                val d = (y1 + 1) * stride + (x1 + 1)

                val windowSum = sum[d] - sum[b] - sum[c] + sum[a]
                val windowSumSq = sumSq[d] - sumSq[b] - sumSq[c] + sumSq[a]

                val mean = windowSum.toDouble() / count
                val variance = (windowSumSq / count) - (mean * mean)
                val stddev = sqrt(max(0.0, variance))

                val threshold = mean * (1.0 + SAUVOLA_K * (stddev / SAUVOLA_R - 1.0))
                out[y * width + x] = if (gray[y * width + x] > threshold) 255 else 0
            }
        }
        return out
    }

    /** Percentile-based auto contrast stretch on luminance, then the same linear remap applied to
     *  all three channels so color/hue is preserved rather than independently stretched. */
    private fun contrastStretchColor(pixels: IntArray): IntArray {
        val histogram = IntArray(256)
        val luminance = IntArray(pixels.size)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val l = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
            luminance[i] = l
            histogram[l]++
        }

        val totalPixels = pixels.size
        val clipCount = (totalPixels * CONTRAST_CLIP_PERCENTILE).toInt()
        var low = 0
        var acc = 0
        while (low < 255) {
            acc += histogram[low]
            if (acc > clipCount) break
            low++
        }
        var high = 255
        acc = 0
        while (high > 0) {
            acc += histogram[high]
            if (acc > clipCount) break
            high--
        }
        if (high <= low) return pixels.copyOf()

        val scale = 255f / (high - low)
        val out = IntArray(pixels.size)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (((((p shr 16) and 0xFF) - low) * scale)).coerceIn(0f, 255f).toInt()
            val g = (((((p shr 8) and 0xFF) - low) * scale)).coerceIn(0f, 255f).toInt()
            val b = ((((p and 0xFF) - low) * scale)).coerceIn(0f, 255f).toInt()
            out[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return out
    }

    /** `sharpened = original + amount * (original - blur(original))` -- classic unsharp mask,
     *  small radius so it only fights genuine softness, not the page's own shape. */
    private fun unsharpMask(gray: FloatArray, width: Int, height: Int): FloatArray {
        val blurred = boxBlur(gray, width, height, UNSHARP_RADIUS)
        val out = FloatArray(gray.size)
        for (i in gray.indices) {
            out[i] = (gray[i] + UNSHARP_AMOUNT * (gray[i] - blurred[i])).coerceIn(0f, 255f)
        }
        return out
    }

    /** Sharpens via the luminance channel only, then applies the same delta to R/G/B at each
     *  pixel — one blur pass instead of three independent per-channel blurs (3x fewer full-image
     *  passes, which matters for the 2s/page budget), and it avoids the color fringing that
     *  sharpening each channel independently can introduce. */
    private fun unsharpMaskColor(pixels: IntArray, width: Int, height: Int): IntArray {
        val gray = toLuminance(pixels)
        val blurredGray = boxBlur(gray, width, height, UNSHARP_RADIUS)

        val out = IntArray(pixels.size)
        for (i in pixels.indices) {
            val delta = UNSHARP_AMOUNT * (gray[i] - blurredGray[i])
            val p = pixels[i]
            val r = (((p shr 16) and 0xFF) + delta).coerceIn(0f, 255f).toInt()
            val g = (((p shr 8) and 0xFF) + delta).coerceIn(0f, 255f).toInt()
            val b = ((p and 0xFF) + delta).coerceIn(0f, 255f).toInt()
            out[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return out
    }
}
