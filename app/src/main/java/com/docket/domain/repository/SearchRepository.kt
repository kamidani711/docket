package com.docket.domain.repository

import com.docket.domain.model.SearchResult

interface SearchRepository {
    suspend fun search(query: String, limit: Int = 50): List<SearchResult>
}
