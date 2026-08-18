package com.docket.domain.repository

import com.docket.domain.model.AnalyticsEventType
import com.docket.domain.model.AnalyticsSummary
import kotlinx.coroutines.flow.Flow

/**
 * Entirely local usage counters — backed by a Room table, never a network call, never a
 * third-party SDK. No row this repository writes is ever read by anything outside this app;
 * see the Settings > Usage screen (which surfaces exactly what's tracked, with a clear button)
 * and the privacy page for how this is disclosed.
 */
interface AnalyticsRepository {
    suspend fun logEvent(type: AnalyticsEventType, detail: String? = null)

    fun observeSummary(): Flow<AnalyticsSummary>

    suspend fun clearAll()
}
