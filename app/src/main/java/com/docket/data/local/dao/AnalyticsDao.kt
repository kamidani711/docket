package com.docket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.docket.data.local.entity.AnalyticsEventEntity
import kotlinx.coroutines.flow.Flow

data class AnalyticsTypeCountRow(val type: String, val count: Int)
data class AnalyticsFeatureCountRow(val detail: String, val count: Int)

@Dao
interface AnalyticsDao {
    @Insert
    suspend fun insert(event: AnalyticsEventEntity)

    /** One row per event type except FEATURE_USED, which is broken down separately by
     *  [observeFeatureUsageCounts] since its meaningful grouping is [AnalyticsEventEntity.detail],
     *  not the type itself. */
    @Query("SELECT type, COUNT(*) as count FROM analytics_events WHERE type != 'FEATURE_USED' GROUP BY type")
    fun observeTypeCounts(): Flow<List<AnalyticsTypeCountRow>>

    @Query("SELECT detail, COUNT(*) as count FROM analytics_events WHERE type = 'FEATURE_USED' AND detail IS NOT NULL GROUP BY detail")
    fun observeFeatureUsageCounts(): Flow<List<AnalyticsFeatureCountRow>>

    @Query("DELETE FROM analytics_events")
    suspend fun clearAll()
}
