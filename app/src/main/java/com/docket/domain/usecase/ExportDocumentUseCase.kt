package com.docket.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import com.docket.domain.common.PremiumRequiredException
import com.docket.domain.common.Result
import com.docket.domain.model.Document
import com.docket.domain.model.ExportFormat
import com.docket.domain.model.ImageExportFormat
import com.docket.domain.model.ImageExportPage
import com.docket.domain.model.ImageExportRequest
import com.docket.domain.model.ImageExportResult
import com.docket.domain.model.PdfExportPageContent
import com.docket.domain.model.PdfExportRequest
import com.docket.domain.model.PdfExportResult
import com.docket.domain.model.PdfPageSize
import com.docket.domain.model.AnalyticsEventType
import com.docket.domain.model.PremiumFeature
import com.docket.domain.repository.AnalyticsRepository
import com.docket.domain.repository.DocumentRepository
import com.docket.domain.repository.ImageExporter
import com.docket.domain.repository.ImageProcessor
import com.docket.domain.repository.OcrRepository
import com.docket.domain.repository.PdfWriter
import com.docket.domain.repository.PremiumRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Re-exports an already-saved [Document] with export-time options (page size, quality,
 * searchable text layer, batch image export) — distinct from [ExportScanSessionUseCase], which
 * only ever flattens a fresh scan session once. Every export writes fresh files under
 * `filesDir/exports/<uuid>/` rather than touching the permanent stored document.
 */
class ExportDocumentUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val ocrRepository: OcrRepository,
    private val imageProcessor: ImageProcessor,
    private val pdfWriter: PdfWriter,
    private val imageExporter: ImageExporter,
    private val premiumRepository: PremiumRepository,
    private val analyticsRepository: AnalyticsRepository,
    @ApplicationContext private val appContext: Context
) {
    suspend fun exportPdf(
        documentId: Long,
        pageSize: PdfPageSize,
        jpegQuality: Int,
        searchable: Boolean
    ): Result<PdfExportResult> {
        val document = documentRepository.getDocument(documentId)
            ?: return Result.Error(IllegalStateException("Document $documentId not found"))

        val isPremium = premiumRepository.isPremium.first()
        val embedTextLayer = searchable && isPremium

        val exportDir = newExportDir()
        val destination = File(exportDir, "${sanitizeFileName(document.title)}.pdf")

        val request = PdfExportRequest(
            pageCount = document.pageCount,
            destination = destination,
            pageSize = pageSize,
            jpegQuality = jpegQuality,
            embedTextLayer = embedTextLayer,
            renderPage = { index ->
                val bitmap = renderPageBitmap(document, index, EXPORT_RENDER_MAX_DIMENSION)
                val words = if (embedTextLayer) {
                    ocrRepository.getPageOcr(documentId, index)?.words.orEmpty()
                } else {
                    emptyList()
                }
                PdfExportPageContent(bitmap, words)
            }
        )

        val result = pdfWriter.writeExportPdf(request)
        analyticsRepository.logEvent(AnalyticsEventType.DOCUMENT_EXPORTED)
        return Result.Success(result)
    }

    suspend fun exportImages(
        documentId: Long,
        pageIndexes: List<Int>,
        format: ImageExportFormat,
        quality: Int
    ): Result<ImageExportResult> {
        val document = documentRepository.getDocument(documentId)
            ?: return Result.Error(IllegalStateException("Document $documentId not found"))

        if (pageIndexes.size > 1) {
            val isPremium = premiumRepository.isPremium.first()
            if (!isPremium) return Result.Error(PremiumRequiredException(PremiumFeature.BATCH_EXPORT))
        }

        val exportDir = newExportDir()
        val pages = pageIndexes.map { index ->
            val bitmap = renderPageBitmap(document, index, EXPORT_RENDER_MAX_DIMENSION)
            ImageExportPage(bitmap, "${sanitizeFileName(document.title)}_page${index + 1}")
        }
        val request = ImageExportRequest(pages, format, quality, exportDir)
        val result = imageExporter.exportImages(request)
        analyticsRepository.logEvent(AnalyticsEventType.DOCUMENT_EXPORTED)
        return Result.Success(result)
    }

    private suspend fun renderPageBitmap(document: Document, index: Int, maxDimension: Int): Bitmap =
        when (document.format) {
            ExportFormat.IMAGE_SET -> imageProcessor.decodeFullResolution(document.pageImagePaths[index])
            ExportFormat.PDF -> pdfWriter.renderPage(
                File(requireNotNull(document.pdfFilePath) { "PDF document has no file path" }),
                index,
                maxDimension
            )
        }

    private fun newExportDir(): File =
        File(appContext.filesDir, "exports/${UUID.randomUUID()}").apply { mkdirs() }

    private fun sanitizeFileName(name: String): String =
        name.map { if (it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_') it else '_' }
            .joinToString("")
            .trim()
            .ifBlank { "document" }

    private companion object {
        const val EXPORT_RENDER_MAX_DIMENSION = 2000
    }
}
