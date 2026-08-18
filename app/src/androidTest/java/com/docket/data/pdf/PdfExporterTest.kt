package com.docket.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `android.graphics.pdf.PdfDocument`/`PdfRenderer` need a real Android runtime, so this is an
 * instrumented test (`./gradlew connectedAndroidTest`) — not runnable in this environment; see
 * the chat write-up. Round-trips a written PDF back through `PdfRenderer` rather than just
 * checking the file exists, since a truncated/malformed PDF would still "exist."
 */
@RunWith(AndroidJUnit4::class)
class PdfExporterTest {

    private val exporter = PdfExporter()
    private lateinit var outputFile: File

    @Before
    fun setUp() {
        outputFile = File(ApplicationProvider.getApplicationContext<Context>().cacheDir, "test_export.pdf")
        outputFile.delete()
    }

    @After
    fun tearDown() {
        outputFile.delete()
    }

    @Test
    fun writesAValidMultiPagePdf() = runBlocking {
        val pageCount = 3
        exporter.writePdf(pageCount, outputFile) { index ->
            Bitmap.createBitmap(200, 300, Bitmap.Config.ARGB_8888).apply {
                eraseColor(if (index % 2 == 0) Color.WHITE else Color.LTGRAY)
            }
        }

        assertTrue(outputFile.exists())
        assertTrue(outputFile.length() > 0)

        ParcelFileDescriptor.open(outputFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                assertEquals(pageCount, renderer.pageCount)
            }
        }
    }

    @Test
    fun eachPageMatchesItsSourceBitmapDimensions() = runBlocking {
        exporter.writePdf(1, outputFile) { Bitmap.createBitmap(150, 400, Bitmap.Config.ARGB_8888) }

        ParcelFileDescriptor.open(outputFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                renderer.openPage(0).use { page ->
                    assertEquals(150, page.width)
                    assertEquals(400, page.height)
                }
            }
        }
    }
}
