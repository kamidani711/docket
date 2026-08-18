package com.docket.domain.usecase

import com.docket.domain.model.SearchResult
import com.docket.domain.repository.SearchRepository
import javax.inject.Inject

/**
 * Turns free-typed user input into an FTS4-safe MATCH query: tokens are stripped to letters/
 * digits only (unicode-aware, so CJK/Devanagari pass through untouched) — which sidesteps FTS
 * query-syntax injection entirely rather than trying to escape it — and the last token gets a
 * `*` suffix for prefix matching, so results start appearing before the user finishes typing.
 *
 * Every term is explicitly scoped to the `text:` column. The FTS table also carries
 * `documentId`/`pageIndex` columns for the search results' JOIN (see OcrDao) — FTS has no real
 * column typing, so an unscoped MATCH could in principle match a page index as if it were
 * search text. Astronomically unlikely to matter in practice, but free to rule out entirely.
 */
class SearchDocumentsUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) {
    suspend operator fun invoke(rawQuery: String): List<SearchResult> {
        val ftsQuery = buildFtsQuery(rawQuery) ?: return emptyList()
        return searchRepository.search(ftsQuery)
    }

    private fun buildFtsQuery(rawQuery: String): String? {
        val tokens = rawQuery.trim()
            .split(Regex("\\s+"))
            .map { token -> token.filter { it.isLetterOrDigit() } }
            .filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        return tokens.mapIndexed { index, token ->
            val term = if (index == tokens.lastIndex) "$token*" else token
            "text:$term"
        }.joinToString(" ")
    }
}
