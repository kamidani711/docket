package com.docket.domain.usecase

import android.graphics.Bitmap
import com.docket.domain.model.ExportFormat
import com.docket.domain.model.OcrLanguage
import com.docket.domain.model.PageOcrData
import com.docket.domain.repository.DocumentRepository
import com.docket.domain.repository.ImageProcessor
import com.docket.domain.repository.MultiScriptTextRecognizer
import com.docket.domain.repository.OcrRepository
import com.docket.domain.repository.PdfWriter
import java.io.File
import javax.inject.Inject

/**
 * Runs OCR over every page of a saved document and stores the result — called from
 * [OcrWorker][com.docket.data.work.OcrWorker] in the background after save, never from UI code
 * directly. Decodes pages larger than preview size (OCR accuracy benefits from more detail than
 * a 800px thumbnail needs) but still well under full camera resolution.
 */
class RunDocumentOcrUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val imageProcessor: ImageProcessor,
    private val pdfWriter: PdfWriter,
    private val multiScriptTextRecognizer: MultiScriptTextRecognizer,
    private val ocrRepository: OcrRepository
) {
    suspend operator fun invoke(documentId: Long) {
        val document = documentRepository.getDocument(documentId) ?: return
        val languages = ocrRepository.installedLanguages()

        when (document.format) {
            ExportFormat.IMAGE_SET -> {
                document.pageImagePaths.forEachIndexed { index, path ->
                    recognizeAndSave(documentId, index, languages) {
                        imageProcessor.decodeDownsampled(path, OCR_MAX_DIMENSION)
                    }
                }
            }
            ExportFormat.PDF -> {
                val pdfPath = document.pdfFilePath ?: return
                val pdfFile = File(pdfPath)
                for (index in 0 until document.pageCount) {
                    recognizeAndSave(documentId, index, languages) {
                        pdfWriter.renderPage(pdfFile, index, OCR_MAX_DIMENSION)
                    }
                }
            }
        }
    }

    private suspend fun recognizeAndSave(
        documentId: Long,
        pageIndex: Int,
        languages: List<OcrLanguage>,
        decode: suspend () -> Bitmap
    ) {
        val bitmap = decode()
        val recognized = try {
            multiScriptTextRecognizer.recognize(bitmap, languages)
        } finally {
            bitmap.recycle()
        }
        ocrRepository.savePageOcr(
            PageOcrData(
                documentId = documentId,
                pageIndex = pageIndex,
                text = recognized.text,
                words = recognized.words,
                language = recognized.language
            )
        )
    }

    private companion object {
        const val OCR_MAX_DIMENSION = 1600
    }
}
