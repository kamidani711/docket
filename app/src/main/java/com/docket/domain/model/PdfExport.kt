package com.docket.domain.model

import android.graphics.Bitmap
import java.io.File

/** One rendered page handed to the PDF writer: the visual bitmap, plus OCR word boxes if an
 *  invisible text layer should be embedded under/over it (empty when not, or not premium). */
data class PdfExportPageContent(
    val bitmap: Bitmap,
    val words: List<OcrWord> = emptyList()
)

data class PdfExportRequest(
    val pageCount: Int,
    val destination: File,
    val pageSize: PdfPageSize,
    val jpegQuality: Int,
    val embedTextLayer: Boolean,
    val renderPage: suspend (index: Int) -> PdfExportPageContent
)

data class PdfExportResult(
    val file: File
)
