package com.docket.data.repository

import com.docket.data.local.dao.OcrDao
import com.docket.domain.model.SearchResult
import com.docket.domain.repository.SearchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val ocrDao: OcrDao
) : SearchRepository {

    override suspend fun search(query: String, limit: Int): List<SearchResult> =
        ocrDao.search(query = query, matchStart = MATCH_START, matchEnd = MATCH_END, limit = limit)
            .map { row ->
                SearchResult(
                    documentId = row.documentId,
                    documentTitle = row.documentTitle,
                    pageIndex = row.pageIndex,
                    snippetText = row.snippet,
                    snippetMatchStart = MATCH_START,
                    snippetMatchEnd = MATCH_END
                )
            }

    private companion object {
        // Built from their integer codes (SOH=1, STX=2) rather than embedded as literal
        // characters in the source — control bytes typed directly into a string literal here
        // have proven unreliable to keep intact through file edits, so this avoids the problem
        // outright instead of risking it silently turning into an empty string again.
        //
        // Not empty strings, not literal brackets: a scanned document's real text plausibly
        // contains "[" or "]", which would make those ambiguous as match markers once the UI
        // parses the snippet apart to build the highlighted result. SOH/STX are never going to
        // appear in real OCR output.
        val MATCH_START: String = 1.toChar().toString()
        val MATCH_END: String = 2.toChar().toString()
    }
}
