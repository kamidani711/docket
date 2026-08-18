package com.docket.domain.repository

import android.graphics.Bitmap
import com.docket.domain.model.PdfExportRequest
import com.docket.domain.model.PdfExportResult
import java.io.File

/**
 * PDF assembly, behind an interface for the same reason as [ImageProcessor]: domain depends on
 * domain-owned contracts, data implements them — even here, where `android.graphics.pdf
 * .PdfDocument` (implemented by `data/pdf/PdfExporter`) is the brief's one mandated choice and
 * a second implementation is unlikely. Consistency with the rest of this layer is worth more
 * than the few lines saved by special-casing this one dependency.
 */
interface PdfWriter {
    /**
     * Pull-based on purpose: this — not the caller — controls when each page's bitmap is
     * requested, so it can enforce "decode/draw/recycle one page before requesting the next"
     * regardless of what the caller does elsewhere.
     */
    suspend fun writePdf(pageCount: Int, destination: File, renderPage: suspend (index: Int) -> Bitmap)

    /** Rasterizes just the first page of an existing PDF, downsampled — for library thumbnails. */
    suspend fun renderThumbnail(pdfFile: File, maxDimension: Int): Bitmap

    /** Rasterizes an arbitrary page of an existing PDF, downsampled — for OCR over PDF documents. */
    suspend fun renderPage(pdfFile: File, pageIndex: Int, maxDimension: Int): Bitmap

    /** The richer document-export path: page sizing, quality, and an optional invisible OCR
     *  text layer. */
    suspend fun writeExportPdf(request: PdfExportRequest): PdfExportResult
}
