package com.docket.domain.usecase

import com.docket.domain.model.ParsedReceipt
import com.docket.domain.repository.DocumentRepository
import com.docket.domain.repository.OcrRepository
import com.docket.domain.repository.ReceiptParser
import javax.inject.Inject

/** Concatenates every page's OCR text (most receipts are one page, but this doesn't assume
 *  it) and hands it to the rules-based [ReceiptParser]. Returns a suggestion only — nothing is
 *  persisted here; see `SaveReceipt` in the receipt edit flow for that. */
class ParseReceiptUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val ocrRepository: OcrRepository,
    private val receiptParser: ReceiptParser
) {
    suspend operator fun invoke(documentId: Long): ParsedReceipt {
        val pageCount = documentRepository.getDocument(documentId)?.pageCount ?: 1
        val combinedText = (0 until pageCount)
            .mapNotNull { pageIndex -> ocrRepository.getPageOcr(documentId, pageIndex)?.text }
            .joinToString("\n")
        return receiptParser.parse(combinedText)
    }
}
