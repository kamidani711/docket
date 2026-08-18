package com.docket.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per tracked action. [type] mirrors [com.docket.domain.model.AnalyticsEventType]'s
 * name as a plain string (Room-friendly, no TypeConverter needed) rather than an enum column.
 * [detail] is only used by [com.docket.domain.model.AnalyticsEventType.FEATURE_USED] to name
 * which feature — null for every other type.
 *
 * This table, and everything that reads it, is 100% local: no row here is ever transmitted
 * anywhere. See the Settings > Usage screen and the privacy page for how this is surfaced and
 * disclosed.
 */
@Entity(tableName = "analytics_events")
data class AnalyticsEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val timestamp: Long,
    val detail: String? = null
)
