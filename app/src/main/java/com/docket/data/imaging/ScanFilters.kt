package com.docket.data.imaging

import android.graphics.ColorMatrix
import com.docket.domain.model.ScanFilter

/**
 * ORIGINAL and GREYSCALE only — both a single [ColorMatrix] applied via `Canvas` +
 * `ColorMatrixColorFilter`, one hardware-backed draw call, not a per-pixel Kotlin loop. They're
 * deliberately "don't touch the photo" options (greyscale is just desaturation), so the fast path
 * is the right one for them.
 *
 * BLACK_AND_WHITE and ENHANCE need real per-pixel work — shadow removal, adaptive binarization,
 * contrast stretch, unsharp mask — and are handled by [DocumentEnhancer] instead; see
 * [BitmapTransformer.applyFilter] for the dispatch.
 */
internal object ScanFilters {

    fun colorMatrixFor(filter: ScanFilter): ColorMatrix? = when (filter) {
        ScanFilter.ORIGINAL -> null
        ScanFilter.GREYSCALE -> ColorMatrix().apply { setSaturation(0f) }
        ScanFilter.BLACK_AND_WHITE, ScanFilter.ENHANCE ->
            error("$filter is handled by DocumentEnhancer, not ScanFilters — see BitmapTransformer.applyFilter")
    }
}
