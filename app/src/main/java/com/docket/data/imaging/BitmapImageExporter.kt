package com.docket.data.imaging

import android.graphics.Bitmap
import com.docket.domain.model.ImageExportFormat
import com.docket.domain.model.ImageExportRequest
import com.docket.domain.model.ImageExportResult
import com.docket.domain.repository.ImageExporter
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BitmapImageExporter @Inject constructor() : ImageExporter {

    override suspend fun exportImages(request: ImageExportRequest): ImageExportResult =
        withContext(Dispatchers.IO) {
            request.destinationDir.mkdirs()
            val extension = if (request.format == ImageExportFormat.PNG) "png" else "jpg"
            val compressFormat = if (request.format == ImageExportFormat.PNG) {
                Bitmap.CompressFormat.PNG
            } else {
                Bitmap.CompressFormat.JPEG
            }

            val files = request.pages.map { page ->
                val file = File(request.destinationDir, "${page.suggestedFileName}.$extension")
                file.outputStream().use { out -> page.bitmap.compress(compressFormat, request.quality, out) }
                page.bitmap.recycle()
                file
            }
            ImageExportResult(files)
        }
}
