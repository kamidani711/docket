package com.docket.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.docket.domain.model.PdfExportPageContent
import com.docket.domain.model.PdfExportRequest
import com.docket.domain.model.PdfPageSize
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the export bug report: exporting a saved document produced a file that existed, was
 * non-zero, and parsed as structurally valid (right page count, valid header/xref/EOF) — but
 * whose embedded page images rendered as garbage or solid black in a real PDF viewer.
 * `PdfExporterTest` only ever exercises [PdfExporter.writePdf] (the save path) with an existence
 * check, never [PdfExporter.writeExportPdf] (the export path) with a look at pixel content, so it
 * never would have caught this.
 *
 * Root cause: `RunDocumentOcrUseCase` (background OCR, scheduled right after every save) and
 * `ExportDocumentUseCase` (a manual export, easy to trigger while that OCR job is still running)
 * both call [PdfWriter.renderPage] against the same saved PDF file via `android.graphics.pdf
 * .PdfRenderer`, which Android documents as not thread-safe. Fixed in [PdfExporter] with a
 * process-wide mutex around every real `PdfDocument`/`PdfRenderer` call — see that class's doc.
 */
@RunWith(AndroidJUnit4::class)
class PdfExportCorruptionTest {

    private val exporter = PdfExporter()
    private lateinit var originalFile: File
    private lateinit var exportedFile: File

    @Before
    fun setUp() {
        val cacheDir = ApplicationProvider.getApplicationContext<Context>().cacheDir
        originalFile = File(cacheDir, "original_saved.pdf").apply { delete() }
        exportedFile = File(cacheDir, "export_corruption_test.pdf").apply { delete() }
    }

    @After
    fun tearDown() {
        originalFile.delete()
        exportedFile.delete()
    }

    /** The literal ask: export a multi-page document and check the file exists, is non-zero, has
     *  the right page count, and starts with a real PDF header. */
    @Test
    fun writeExportPdf_producesValidNonEmptyMultiPagePdf_withCorrectHeader() = runBlocking {
        val pageCount = 3
        val request = PdfExportRequest(
            pageCount = pageCount,
            destination = exportedFile,
            pageSize = PdfPageSize.FIT_TO_CONTENT,
            jpegQuality = 90,
            embedTextLayer = false,
            renderPage = { index ->
                val bitmap = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(if (index % 2 == 0) Color.WHITE else Color.LTGRAY)
                }
                PdfExportPageContent(bitmap)
            }
        )

        exporter.writeExportPdf(request)

        assertTrue("expected the export to produce a file", exportedFile.exists())
        assertTrue("expected a non-empty file", exportedFile.length() > 0)

        val header = ByteArray(5)
        exportedFile.inputStream().use { it.read(header) }
        assertEquals("expected a real PDF header", "%PDF-", String(header, Charsets.US_ASCII))

        ParcelFileDescriptor.open(exportedFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                assertEquals("expected all pages present", pageCount, renderer.pageCount)
            }
        }
    }

    /** Matches a real photographed page's dimensions (1240x1754) -- deliberately *below*
     *  [EXPORT_RENDER_MAX_DIMENSION], so re-rasterizing at export time actually upscales, same
     *  as the real flow does. Has real high-frequency detail -- many thin lines -- everywhere
     *  except a clean band at the top reserved for a corruption-free color sample (lines are
     *  exactly where JPEG block artifacts concentrate, which would make a same-spot sample
     *  confound "real corruption" with "expected softening near a hard edge"). */
    private fun detailedBitmap(baseColor: Int, width: Int = 1240, height: Int = 1754): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(baseColor)
        val linePaint = Paint().apply { color = Color.DKGRAY; strokeWidth = 1f }
        var y = SAMPLE_BAND_HEIGHT + 50f
        while (y < height - 100) {
            canvas.drawLine(80f, y, width - 80f, y, linePaint)
            y += 30f
        }
        return bitmap
    }

    /** Decodes one of the bundled real repro PNGs (`androidTest/assets`) the same way
     *  `BitmapTransformer.decodeFullResolution` does: `BitmapFactory` with no special options.
     *  These assets ship in the *test* APK, so they come from the instrumentation context, not
     *  `ApplicationProvider.getApplicationContext()` (that's the app-under-test's own context). */
    private fun decodeRealAsset(name: String): Bitmap {
        val context = InstrumentationRegistry.getInstrumentation().context
        return context.assets.open(name).use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: error("failed to decode asset $name")
    }

    /** The real pipeline for exporting an already-saved PDF document is NOT "encode a fresh
     *  bitmap once" -- it's write -> *re-rasterize the already-saved PDF via [PdfRenderer]* ->
     *  encode again ([com.docket.domain.usecase.ExportDocumentUseCase.renderPageBitmap]). This
     *  reproduces exactly that: [PdfExporter.writePdf] (the save path) writes an original PDF,
     *  [PdfExporter.renderPage] reads it back (the same call `renderPageBitmap` makes), and
     *  those re-rasterized bitmaps -- not fresh ones -- are what feeds [PdfExporter.writeExportPdf]
     *  (the export path). Known-color source content lets this assert exact pixel correctness,
     *  not just "didn't throw."
     */
    @Test
    fun exportedPdf_matchesSourceColor_afterTheRealSaveThenExportRoundTrip() = runBlocking {
        val colors = listOf(Color.RED, Color.GREEN, Color.BLUE)

        // Save, exactly like ExportScanSessionUseCase.exportPages does for PDF format -- no
        // quality reencoding, direct draw.
        exporter.writePdf(
            pageCount = colors.size,
            destination = originalFile,
            renderPage = { index -> detailedBitmap(colors[index]) }
        )
        assertTrue("original save produced no file", originalFile.exists() && originalFile.length() > 0)

        // Export the saved document, exactly like ExportDocumentUseCase.exportPdf does.
        val request = PdfExportRequest(
            pageCount = colors.size,
            destination = exportedFile,
            pageSize = PdfPageSize.FIT_TO_CONTENT,
            jpegQuality = 90,
            embedTextLayer = false,
            renderPage = { index ->
                val rerasterized = exporter.renderPage(originalFile, index, EXPORT_RENDER_MAX_DIMENSION)
                PdfExportPageContent(rerasterized)
            }
        )
        exporter.writeExportPdf(request)
        assertTrue("export produced no file", exportedFile.exists() && exportedFile.length() > 0)

        ParcelFileDescriptor.open(exportedFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                assertEquals("expected all 3 pages present", colors.size, renderer.pageCount)
                for (i in colors.indices) {
                    renderer.openPage(i).use { page ->
                        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        // Sampled from the clean band at the top -- no lines nearby, so any large
                        // color mismatch here is real corruption, not JPEG edge softening.
                        val sampleY = (SAMPLE_BAND_HEIGHT / 2 * (bitmap.height.toFloat() / 2000)).toInt()
                        val sample = bitmap.getPixel(bitmap.width / 2, sampleY.coerceIn(0, bitmap.height - 1))
                        val expected = colors[i]

                        assertTrue(
                            "page $i: expected ~${Integer.toHexString(expected)}, " +
                                "rendered ${Integer.toHexString(sample)} -- embedded image is corrupted",
                            colorDistance(sample, expected) < COLOR_TOLERANCE
                        )
                    }
                }
            }
        }
    }

    /** Same round trip, but with the real repro PNGs (actual photographed-page dimensions and
     *  content, decoded exactly the way the app decodes them) rather than synthetic bitmaps --
     *  covers the real decode path even though it can't assert exact colors against text/line
     *  content the way the synthetic version can. */
    @Test
    fun exportedPdf_roundTripsRealPageAssets_withoutThrowing() = runBlocking {
        val assetNames = listOf("export_test_page1.png", "export_test_page2.png", "export_test_page3.png")

        exporter.writePdf(
            pageCount = assetNames.size,
            destination = originalFile,
            renderPage = { index -> decodeRealAsset(assetNames[index]) }
        )
        assertTrue("original save produced no file", originalFile.exists() && originalFile.length() > 0)

        val request = PdfExportRequest(
            pageCount = assetNames.size,
            destination = exportedFile,
            pageSize = PdfPageSize.FIT_TO_CONTENT,
            jpegQuality = 90,
            embedTextLayer = false,
            renderPage = { index ->
                val rerasterized = exporter.renderPage(originalFile, index, EXPORT_RENDER_MAX_DIMENSION)
                PdfExportPageContent(rerasterized)
            }
        )
        exporter.writeExportPdf(request)
        assertTrue("export produced no file", exportedFile.exists() && exportedFile.length() > 0)

        ParcelFileDescriptor.open(exportedFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                assertEquals(assetNames.size, renderer.pageCount)
            }
        }
    }

    /** Regression guard for the actual root cause: two coroutines calling [PdfWriter.renderPage]
     *  on the same file at the same time, mimicking OCR and export overlapping. Before the
     *  [PdfExporter] mutex fix this produced visibly wrong pixel content; this asserts it no
     *  longer does. Dumps any mismatch it finds to external files for visual inspection, since a
     *  bare pixel-distance number is hard to debug a regression from. */
    @Test
    fun concurrentRenderPageCalls_onSameFile_doNotCorruptOutput() = runBlocking {
        val assetNames = listOf("export_test_page1.png", "export_test_page2.png", "export_test_page3.png")
        exporter.writePdf(
            pageCount = assetNames.size,
            destination = originalFile,
            renderPage = { index -> decodeRealAsset(assetNames[index]) }
        )

        val dumpDir = ApplicationProvider.getApplicationContext<Context>().getExternalFilesDir(null)
        var mismatches = 0

        // Mimic the real timing: many concurrent renderPage calls against the same file, as if
        // OCR (looping pages 0..2) and export (looping pages 0..2) were both mid-flight.
        coroutineScope {
            val jobs = (0 until 6).map { attempt ->
                launch(Dispatchers.Default) {
                    val pageIndex = attempt % assetNames.size
                    val bitmap = exporter.renderPage(originalFile, pageIndex, EXPORT_RENDER_MAX_DIMENSION)
                    val expectedBaseline = decodeRealAsset(assetNames[pageIndex])
                    val scaled = Bitmap.createScaledBitmap(expectedBaseline, bitmap.width, bitmap.height, true)

                    val sampleY = (bitmap.height * 0.02f).toInt().coerceIn(0, bitmap.height - 1)
                    val sampleX = bitmap.width / 2
                    val actual = bitmap.getPixel(sampleX, sampleY)
                    val expected = scaled.getPixel(sampleX, sampleY)

                    if (colorDistance(actual, expected) >= COLOR_TOLERANCE) {
                        synchronized(this@PdfExportCorruptionTest) { mismatches++ }
                        File(dumpDir, "race_mismatch_attempt${attempt}_page$pageIndex.png").outputStream().use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                    }
                }
            }
            jobs.forEach { it.join() }
        }

        assertTrue(
            "expected no corrupted renders from concurrent renderPage calls, got $mismatches/6 -- " +
                "PdfRenderer is not thread-safe and OCR + export both call PdfWriter.renderPage() " +
                "against the same file; see PdfExporter's pdfApiMutex",
            mismatches == 0
        )
    }

    private fun colorDistance(a: Int, b: Int): Int =
        kotlin.math.abs(Color.red(a) - Color.red(b)) +
            kotlin.math.abs(Color.green(a) - Color.green(b)) +
            kotlin.math.abs(Color.blue(a) - Color.blue(b))

    private companion object {
        const val SAMPLE_BAND_HEIGHT = 300f
        const val EXPORT_RENDER_MAX_DIMENSION = 2000

        // JPEG at quality 90 is lossy but nowhere near enough to move a solid, line-free color
        // sample this far -- this tolerance catches real corruption, not compression noise.
        const val COLOR_TOLERANCE = 60
    }
}
