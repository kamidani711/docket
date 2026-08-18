package com.docket.domain.model

/**
 * The single in-progress scan draft. There's only ever one at a time — starting a new scan
 * replaces whatever was there — which is what makes "recover after process death" simple: on
 * launch, just ask the repository whether a session exists.
 *
 * A session is *either* a set of [pages] captured via camera/gallery (the normal edit flow:
 * Scan → Review), *or* an [importedPdfPath] (an already-finished PDF picked from storage,
 * which skips straight to naming/saving since there's nothing to crop). Never both.
 */
data class ScanSession(
    val id: String,
    val pages: List<ScanPage> = emptyList(),
    val importedPdfPath: String? = null,
    val importedPdfPageCount: Int = 0,
    val suggestedName: String? = null
) {
    val isPdfImport: Boolean get() = importedPdfPath != null
}
