package com.docket.domain.model

import android.graphics.Bitmap
import java.io.File

enum class ImageExportFormat { JPEG, PNG }

data class ImageExportPage(
    val bitmap: Bitmap,
    val suggestedFileName: String
)

data class ImageExportRequest(
    val pages: List<ImageExportPage>,
    val format: ImageExportFormat,
    val quality: Int,
    val destinationDir: File
)

data class ImageExportResult(val files: List<File>)
