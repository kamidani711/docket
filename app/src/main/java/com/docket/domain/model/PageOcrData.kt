package com.docket.domain.model

/**
 * One recognized word's bounding box, normalized to [0,1] against the *OCR'd image's* own
 * dimensions — same normalization approach as [Quad], so a word position can be rescaled onto
 * whatever final PDF page size an export ends up using regardless of the source resolution.
 */
data class OcrWord(
    val text: String,
    val leftFrac: Float,
    val topFrac: Float,
    val rightFrac: Float,
    val bottomFrac: Float
)

/**
 * OCR output for one page. [words] carries positions (needed for the searchable-PDF invisible
 * text layer); [text] is the flat concatenation used for the FTS search index — kept as a
 * separate field rather than derived from [words] at query time since that's the far more
 * common read path.
 */
data class PageOcrData(
    val documentId: Long,
    val pageIndex: Int,
    val text: String,
    val words: List<OcrWord>,
    val language: OcrLanguage
)
