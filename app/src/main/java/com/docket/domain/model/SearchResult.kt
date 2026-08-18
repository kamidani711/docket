package com.docket.domain.model

/**
 * [snippetText] carries the FTS4 `snippet()` output verbatim, with [snippetMatchStart]/
 * [snippetMatchMarkerEnd] as the literal marker strings used to bracket matches (e.g. "[", "]")
 * — the UI splits on these to build a highlighted [androidx.compose.ui.text.AnnotatedString]
 * rather than this layer taking a Compose dependency it has no business needing.
 */
data class SearchResult(
    val documentId: Long,
    val documentTitle: String,
    val pageIndex: Int,
    val snippetText: String,
    val snippetMatchStart: String,
    val snippetMatchEnd: String
)
