package com.docket.data.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.docket.domain.model.OcrWord
import com.docket.domain.model.PdfExportPageContent
import com.docket.domain.model.PdfExportRequest
import com.docket.domain.model.PdfExportResult
import com.docket.domain.model.PdfPageSize
import com.docket.domain.repository.PdfWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * [PdfWriter] implementation using `android.graphics.pdf.PdfDocument` — no third-party PDF
 * library, per the brief.
 *
 * No password protection: `PdfDocument` has no encryption API, and hand-rolling PDF standard
 * security (or bringing in a PDF library capable of it — itext7's newer licensing is AGPL/
 * commercial, PDFBox-Android is a real size/complexity jump) isn't a call to make silently.
 * Earlier drafts exposed a password field in the UI that this class silently ignored — removed
 * entirely rather than shipped half-working; see the chat reply for the actual tradeoff writeup.
 * If this ever gets implemented for real, it needs its own dependency decision first.
 *
 * The searchable-PDF text layer, by contrast, IS achievable with plain `PdfDocument`:
 * `Canvas.drawText()` on a `PdfDocument.Page`'s canvas emits real vector text into the page's
 * PDF content stream (this is a Skia PDF-backend capability, not a raster overlay) — drawing
 * each OCR'd word at its bounding box with a fully transparent `Paint` gives a genuinely
 * selectable/searchable invisible layer under the visible image. Alignment is approximate
 * (scaled to each word's box via `textSize`/`textScaleX`, not font-metric-exact), which is
 * normal for this technique — it's what search/copy tooling actually relies on, not pixel-
 * perfect selection highlighting.
 *
 * Concurrency: `android.graphics.pdf.PdfRenderer` is documented as not thread-safe, and
 * `PdfDocument` shares the same underlying native PDF library. Two independent call paths in
 * this app both end up calling into these APIs, sometimes against the very same file at nearly
 * the same time — `RunDocumentOcrUseCase` (background OCR, scheduled right after every save) and
 * `ExportDocumentUseCase` (a manual export, easy to trigger while OCR on that same document is
 * still running) — and that race produced genuinely corrupted embedded page images in testing: a
 * structurally valid PDF whose image content renders as garbage or solid black. [pdfApiMutex]
 * serializes every real `PdfDocument`/`PdfRenderer` call across the whole process to close that
 * race — see its own doc for why it's process-wide rather than a field on this class.
 */
class PdfExporter @Inject constructor() : PdfWriter {

    override suspend fun writePdf(
        pageCount: Int,
        destination: File,
        renderPage: suspend (index: Int) -> Bitmap
    ) = withContext(Dispatchers.Default) {
        val document = PdfDocument()
        try {
            for (index in 0 until pageCount) {
                // renderPage runs *outside* the lock -- see writeExportPdf below for why that
                // matters even though this particular caller never re-enters PdfExporter.
                val bitmap = renderPage(index)
                try {
                    pdfApiMutex.withLock {
                        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                        val page = document.startPage(pageInfo)
                        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        document.finishPage(page)
                    }
                } finally {
                    bitmap.recycle()
                }
            }
            pdfApiMutex.withLock {
                FileOutputStream(destination).use { out -> document.writeTo(out) }
            }
        } finally {
            pdfApiMutex.withLock { document.close() }
        }
    }

    override suspend fun writeExportPdf(request: PdfExportRequest): PdfExportResult =
        withContext(Dispatchers.Default) {
            val document = PdfDocument()
            try {
                for (index in 0 until request.pageCount) {
                    // Deliberately outside pdfApiMutex: for a PDF-format document,
                    // ExportDocumentUseCase's renderPage callback calls back into renderPage()
                    // below, on this same PdfWriter instance, to re-rasterize the already-saved
                    // PDF. pdfApiMutex is not reentrant, so holding it here while invoking that
                    // callback would deadlock every single PDF export.
                    val content = request.renderPage(index)
                    try {
                        pdfApiMutex.withLock {
                            writeExportPage(document, content, index, request.pageSize, request.jpegQuality, request.embedTextLayer)
                        }
                    } finally {
                        content.bitmap.recycle()
                    }
                }
                pdfApiMutex.withLock {
                    FileOutputStream(request.destination).use { out -> document.writeTo(out) }
                }
            } finally {
                pdfApiMutex.withLock { document.close() }
            }
            PdfExportResult(file = request.destination)
        }

    private fun writeExportPage(
        document: PdfDocument,
        content: PdfExportPageContent,
        index: Int,
        pageSize: PdfPageSize,
        jpegQuality: Int,
        embedTextLayer: Boolean
    ) {
        val qualityAdjusted = reencodeAtQuality(content.bitmap, jpegQuality)
        try {
            val (pageWidth, pageHeight) = pageDimensions(pageSize, qualityAdjusted)
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val page = document.startPage(pageInfo)
            drawPage(page.canvas, qualityAdjusted, content.words, pageWidth, pageHeight, embedTextLayer)
            document.finishPage(page)
        } finally {
            if (qualityAdjusted !== content.bitmap) qualityAdjusted.recycle()
        }
    }

    private fun pageDimensions(pageSize: PdfPageSize, bitmap: Bitmap): Pair<Int, Int> =
        if (pageSize == PdfPageSize.FIT_TO_CONTENT) {
            bitmap.width to bitmap.height
        } else {
            pageSize.widthPt to pageSize.heightPt
        }

    /** Draws the image scaled uniformly to fit the page (letterboxed if aspect ratios differ),
     *  then optionally an invisible text layer positioned against that same scale/offset. */
    private fun drawPage(
        canvas: Canvas,
        bitmap: Bitmap,
        words: List<OcrWord>,
        pageWidth: Int,
        pageHeight: Int,
        embedTextLayer: Boolean
    ) {
        val scale = minOf(pageWidth.toFloat() / bitmap.width, pageHeight.toFloat() / bitmap.height)
        val drawWidth = bitmap.width * scale
        val drawHeight = bitmap.height * scale
        val left = (pageWidth - drawWidth) / 2f
        val top = (pageHeight - drawHeight) / 2f

        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(
            bitmap,
            null,
            RectF(left, top, left + drawWidth, top + drawHeight),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )

        if (embedTextLayer && words.isNotEmpty()) {
            drawInvisibleTextLayer(canvas, words, left, top, drawWidth, drawHeight)
        }
    }

    private fun drawInvisibleTextLayer(
        canvas: Canvas,
        words: List<OcrWord>,
        offsetLeft: Float,
        offsetTop: Float,
        drawWidth: Float,
        drawHeight: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = 0 // invisible — real vector text in the content stream, just not painted
        }
        for (word in words) {
            if (word.text.isBlank()) continue
            val boxLeft = offsetLeft + word.leftFrac * drawWidth
            val boxTop = offsetTop + word.topFrac * drawHeight
            val boxRight = offsetLeft + word.rightFrac * drawWidth
            val boxBottom = offsetTop + word.bottomFrac * drawHeight
            val boxWidth = boxRight - boxLeft
            val boxHeight = boxBottom - boxTop
            if (boxWidth <= 0f || boxHeight <= 0f) continue

            paint.textScaleX = 1f
            paint.textSize = boxHeight * 0.9f
            val naturalWidth = paint.measureText(word.text)
            if (naturalWidth > 0f) {
                paint.textScaleX = boxWidth / naturalWidth
            }
            canvas.drawText(word.text, boxLeft, boxBottom, paint)
        }
    }

    /** Forces a real lossy round-trip at [quality] before drawing — the only reliable way to
     *  control embedded-image size/quality, since PdfDocument's own internal image encoding
     *  when serializing the page isn't something the Canvas API exposes control over. */
    private fun reencodeAtQuality(bitmap: Bitmap, quality: Int): Bitmap {
        if (quality >= 100) return bitmap
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), stream)
        val bytes = stream.toByteArray()
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: bitmap
    }

    override suspend fun renderThumbnail(pdfFile: File, maxDimension: Int): Bitmap =
        renderPdfPage(pdfFile, 0, maxDimension)

    override suspend fun renderPage(pdfFile: File, pageIndex: Int, maxDimension: Int): Bitmap =
        renderPdfPage(pdfFile, pageIndex, maxDimension)

    private suspend fun renderPdfPage(pdfFile: File, pageIndex: Int, maxDimension: Int): Bitmap =
        withContext(Dispatchers.IO) {
            pdfApiMutex.withLock {
                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    PdfRenderer(fd).use { renderer ->
                        renderer.openPage(pageIndex).use { page ->
                            val scale = maxDimension.toFloat() / maxOf(page.width, page.height)
                            val width = (page.width * scale).roundToInt().coerceAtLeast(1)
                            val height = (page.height * scale).roundToInt().coerceAtLeast(1)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmap
                        }
                    }
                }
            }
        }

    private companion object {
        /** Process-wide, not a field on this class: Hilt provides [PdfExporter] unscoped (no
         *  `@Singleton`), so `RunDocumentOcrUseCase` and `ExportDocumentUseCase` each get their
         *  own instance -- a per-instance lock would not actually exclude them from each other.
         *  The native PDF library state this guards is shared at the process level regardless of
         *  how many Kotlin wrapper instances exist, so the lock has to be too. */
        private val pdfApiMutex = Mutex()
    }
}
