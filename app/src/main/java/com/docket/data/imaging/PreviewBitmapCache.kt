package com.docket.data.imaging

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import com.docket.domain.model.Quad
import com.docket.domain.model.ScanFilter
import com.docket.domain.repository.PreviewCache
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [PreviewCache] implementation. Caches the small, downsampled preview bitmaps the Review
 * screen shows — NOT full-resolution export bitmaps, which are never cached (see
 * `ExportScanSessionUseCase`, which processes and immediately recycles them one at a time).
 *
 * Sized as a fraction of the app's own memory class so a page strip full of thumbnails can't
 * itself become the reason a 2GB device runs low on memory.
 */
@Singleton
class PreviewBitmapCache @Inject constructor(@ApplicationContext context: Context) : PreviewCache {

    private val cache = object : LruCache<String, Bitmap>(cacheSizeKb(context)) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    override fun get(key: String): Bitmap? = cache.get(key)

    override fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    /** Changes whenever anything that affects the rendered preview changes, so a stale bitmap
     *  is never served for a page whose crop/rotation/filter just changed. */
    override fun keyFor(pageId: String, corners: Quad, rotationDegrees: Int, filter: ScanFilter): String =
        "$pageId:$corners:$rotationDegrees:$filter"

    private companion object {
        fun cacheSizeKb(context: Context): Int {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            // An eighth of the app's memory class (MB), in KB — generous for a full page strip
            // of thumbnails on a 20-page document without being able to pressure the device on
            // its own. See the perf write-up for why this bound matters more than it looks.
            return (activityManager.memoryClass * 1024) / 8
        }
    }
}
