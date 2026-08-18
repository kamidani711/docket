package com.docket.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import com.docket.domain.common.Result
import com.docket.domain.model.AnalyticsEventType
import com.docket.domain.model.ExportFormat
import com.docket.domain.model.ExportProgress
import com.docket.domain.model.Quad
import com.docket.domain.model.ScanFilter
import com.docket.domain.model.ScanPage
import com.docket.domain.model.ScanSession
import com.docket.domain.repository.AnalyticsRepository
import com.docket.domain.repository.BackgroundOcrScheduler
import com.docket.domain.repository.DocumentRepository
import com.docket.domain.repository.ImageProcessor
import com.docket.domain.repository.PdfWriter
import com.docket.domain.repository.ScanSessionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Turns an edited [ScanSession] into a saved [com.docket.domain.model.Document]: renders each
 * page's crop/rotation/filter onto its full-resolution original, writes the result (PDF or a
 * JPEG per page), inserts the Room row, then clears the draft session.
 *
 * Memory discipline is the whole point of this class: [renderFinalBitmap] holds at most one
 * full-resolution [Bitmap] at a time (each step recycles the previous one before making the
 * next), and pages are processed strictly one at a time, in order — never batched. See the
 * perf write-up in the chat reply for what this does and doesn't buy us on a 20-page document.
 *
 * PDF assembly goes through [PdfWriter] (implemented by `data/pdf/PdfExporter`) — same
 * domain/data split as [ImageProcessor], even though `android.graphics.pdf.PdfDocument` is the
 * brief's one mandated choice for it.
 */
class ExportScanSessionUseCase @Inject constructor(
    private val imageProcessor: ImageProcessor,
    private val pdfWriter: PdfWriter,
    private val documentRepository: DocumentRepository,
    private val scanSessionRepository: ScanSessionRepository,
    private val backgroundOcrScheduler: BackgroundOcrScheduler,
    private val analyticsRepository: AnalyticsRepository,
    @ApplicationContext private val appContext: Context
) {
    suspend operator fun invoke(
        session: ScanSession,
        title: String,
        folderId: Long?,
        format: ExportFormat,
        onProgress: (ExportProgress) -> Unit
    ): Result<Long> = withContext(Dispatchers.Default) {
        try {
            val documentId = if (session.isPdfImport) {
                exportPdfImport(session, title, folderId, onProgress)
            } else {
                exportPages(session, title, folderId, format, onProgress)
            }
            scanSessionRepository.clearSession(session.id)
            // Fire-and-forget: OCR is a background enhancement on an already-saved document,
            // never something the save flow waits on. See OcrWorker/OcrScheduler.
            backgroundOcrScheduler.scheduleOcr(documentId)
            analyticsRepository.logEvent(AnalyticsEventType.DOCUMENT_SAVED)
            Result.Success(documentId)
        } catch (t: Throwable) {
            Result.Error(t)
        }
    }

    private suspend fun exportPages(
        session: ScanSession,
        title: String,
        folderId: Long?,
        format: ExportFormat,
        onProgress: (ExportProgress) -> Unit
    ): Long {
        val totalPages = session.pages.size
        val documentDir = File(appContext.filesDir, "documents/${UUID.randomUUID()}").apply { mkdirs() }

        val documentId = when (format) {
            ExportFormat.PDF -> {
                val pdfFile = File(documentDir, "document.pdf")
                pdfWriter.writePdf(
                    pageCount = totalPages,
                    destination = pdfFile,
                    renderPage = { index ->
                        onProgress(ExportProgress(index + 1, totalPages, ExportProgress.Stage.PROCESSING_PAGE))
                        renderFinalBitmap(session.pages[index])
                    }
                )
                onProgress(ExportProgress(totalPages, totalPages, ExportProgress.Stage.SAVING_TO_LIBRARY))
                documentRepository.insertDocument(
                    title = title,
                    folderId = folderId,
                    format = ExportFormat.PDF,
                    pageCount = totalPages,
                    pdfFilePath = pdfFile.absolutePath,
                    pageImagePaths = emptyList(),
                    thumbnailPath = writeThumbnail(session.pages.first(), documentDir),
                    sizeBytes = pdfFile.length()
                )
            }

            ExportFormat.IMAGE_SET -> {
                val pagePaths = ArrayList<String>(totalPages)
                session.pages.forEachIndexed { index, page ->
                    onProgress(ExportProgress(index + 1, totalPages, ExportProgress.Stage.PROCESSING_PAGE))
                    val bitmap = renderFinalBitmap(page)
                    val outFile = File(documentDir, "page_%02d.jpg".format(index))
                    imageProcessor.saveJpeg(bitmap, outFile.absolutePath)
                    bitmap.recycle()
                    pagePaths += outFile.absolutePath
                }
                onProgress(ExportProgress(totalPages, totalPages, ExportProgress.Stage.SAVING_TO_LIBRARY))
                documentRepository.insertDocument(
                    title = title,
                    folderId = folderId,
                    format = ExportFormat.IMAGE_SET,
                    pageCount = pagePaths.size,
                    pdfFilePath = null,
                    pageImagePaths = pagePaths,
                    thumbnailPath = pagePaths.firstOrNull(),
                    sizeBytes = pagePaths.sumOf { path -> File(path).length() }
                )
            }
        }

        // Pages are now flattened into the saved document — the temp captures they came from
        // are no longer needed. (PDF-import sessions have no `pages`, so this is a no-op there;
        // the imported PDF file itself is the permanent document, not a temp capture.)
        session.pages.forEach { File(it.originalImagePath).delete() }

        return documentId
    }

    private suspend fun exportPdfImport(
        session: ScanSession,
        title: String,
        folderId: Long?,
        onProgress: (ExportProgress) -> Unit
    ): Long {
        onProgress(ExportProgress(1, 1, ExportProgress.Stage.SAVING_TO_LIBRARY))
        val pdfFile = File(requireNotNull(session.importedPdfPath) { "PDF-import session has no path" })
        val thumbnailPath = runCatching {
            val bitmap = pdfWriter.renderThumbnail(pdfFile, THUMBNAIL_MAX_DIMENSION)
            val thumbFile = File(pdfFile.parentFile, "thumbnail.jpg")
            imageProcessor.saveJpeg(bitmap, thumbFile.absolutePath, quality = 80)
            bitmap.recycle()
            thumbFile.absolutePath
        }.getOrNull()

        return documentRepository.insertDocument(
            title = title,
            folderId = folderId,
            format = ExportFormat.PDF,
            pageCount = session.importedPdfPageCount,
            pdfFilePath = pdfFile.absolutePath,
            pageImagePaths = emptyList(),
            thumbnailPath = thumbnailPath,
            sizeBytes = pdfFile.length()
        )
    }

    /**
     * decode → crop → rotate → filter, one full-resolution bitmap alive at a time: each step
     * recycles its input before returning its output.
     */
    private suspend fun renderFinalBitmap(page: ScanPage): Bitmap {
        var bitmap = imageProcessor.decodeFullResolution(page.originalImagePath)
        if (page.corners != Quad.FullFrame) {
            bitmap = bitmap.replaceWith(imageProcessor.applyCrop(bitmap, page.corners))
        }
        if (page.rotationDegrees != 0) {
            bitmap = bitmap.replaceWith(imageProcessor.applyRotation(bitmap, page.rotationDegrees))
        }
        if (page.filter != ScanFilter.ORIGINAL) {
            bitmap = bitmap.replaceWith(imageProcessor.applyFilter(bitmap, page.filter))
        }
        return bitmap
    }

    private suspend fun writeThumbnail(firstPage: ScanPage, documentDir: File): String {
        var bitmap = imageProcessor.decodeDownsampled(firstPage.originalImagePath, THUMBNAIL_MAX_DIMENSION)
        if (firstPage.corners != Quad.FullFrame) {
            bitmap = bitmap.replaceWith(imageProcessor.applyCrop(bitmap, firstPage.corners))
        }
        if (firstPage.rotationDegrees != 0) {
            bitmap = bitmap.replaceWith(imageProcessor.applyRotation(bitmap, firstPage.rotationDegrees))
        }
        if (firstPage.filter != ScanFilter.ORIGINAL) {
            bitmap = bitmap.replaceWith(imageProcessor.applyFilter(bitmap, firstPage.filter))
        }
        val thumbFile = File(documentDir, "thumbnail.jpg")
        imageProcessor.saveJpeg(bitmap, thumbFile.absolutePath, quality = 80)
        bitmap.recycle()
        return thumbFile.absolutePath
    }

    /** Recycles `this` and returns [next] — small helper so the recycle-then-replace dance above
     *  reads as one step instead of three lines at every stage. */
    private fun Bitmap.replaceWith(next: Bitmap): Bitmap {
        recycle()
        return next
    }

    private companion object {
        const val THUMBNAIL_MAX_DIMENSION = 480
    }
}
