package com.docket.domain.repository

import android.graphics.Bitmap
import com.docket.domain.model.Quad
import com.docket.domain.model.ScanFilter

/** Bounded in-memory cache for rendered preview bitmaps — see `data/imaging/PreviewBitmapCache`. */
interface PreviewCache {
    fun get(key: String): Bitmap?
    fun put(key: String, bitmap: Bitmap)
    fun keyFor(pageId: String, corners: Quad, rotationDegrees: Int, filter: ScanFilter): String
}
