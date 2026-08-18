package com.docket.domain.model

/** Output of running OCR on one bitmap — not yet tied to a document/page, see [PageOcrData]. */
data class RecognizedText(
    val text: String,
    val words: List<OcrWord>,
    val language: OcrLanguage
)
