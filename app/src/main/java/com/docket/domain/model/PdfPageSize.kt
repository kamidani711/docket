package com.docket.domain.model

/**
 * PDF page dimensions in points (1/72 inch — the PDF spec's native unit, and what
 * `PdfDocument.PageInfo` expects). [FIT_TO_CONTENT] has no fixed size — the page is sized to
 * match each image's own pixel dimensions, same behavior as the original scan-export flow.
 */
enum class PdfPageSize(val widthPt: Int, val heightPt: Int) {
    A4(595, 842),
    LETTER(612, 792),
    FIT_TO_CONTENT(0, 0)
}
