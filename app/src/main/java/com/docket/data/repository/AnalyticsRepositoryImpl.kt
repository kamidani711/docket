package com.docket.data.repository

import com.docket.data.local.dao.AnalyticsDao
import com.docket.data.local.entity.AnalyticsEventEntity
import com.docket.domain.model.AnalyticsEventType
import com.docket.domain.model.AnalyticsSummary
import com.docket.domain.repository.AnalyticsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val analyticsDao: AnalyticsDao
) : AnalyticsRepository {

    override suspend fun logEvent(type: AnalyticsEventType, detail: String?) {
        analyticsDao.insert(
            AnalyticsEventEntity(type = type.name, timestamp = System.currentTimeMillis(), detail = detail)
        )
    }

    override fun observeSummary(): Flow<AnalyticsSummary> =
        combine(analyticsDao.observeTypeCounts(), analyticsDao.observeFeatureUsageCounts()) { typeCounts, featureCounts ->
            val byType = typeCounts.associate { row ->
                runCatching { AnalyticsEventType.valueOf(row.type) }.getOrNull() to row.count
            }
            AnalyticsSummary(
                scansStarted = byType[AnalyticsEventType.SCAN_STARTED] ?: 0,
                documentsSaved = byType[AnalyticsEventType.DOCUMENT_SAVED] ?: 0,
                exportsCompleted = byType[AnalyticsEventType.DOCUMENT_EXPORTED] ?: 0,
                searchesPerformed = byType[AnalyticsEventType.SEARCH_PERFORMED] ?: 0,
                cleanSessions = byType[AnalyticsEventType.SESSION_CLEAN] ?: 0,
                crashedSessions = byType[AnalyticsEventType.SESSION_CRASHED] ?: 0,
                featureUsage = featureCounts.associate { it.detail to it.count }
            )
        }

    override suspend fun clearAll() {
        analyticsDao.clearAll()
    }
}
