package com.docket.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One page of the in-progress scan draft. Crop corners are 8 explicit Float columns rather than
 * a serialized blob behind a TypeConverter — more verbose, but queryable/debuggable, and this
 * table is small (a handful of rows) so there's no real cost to it. Defaults are the full,
 * uncropped frame — [com.docket.domain.model.Quad.FullFrame].
 */
@Entity(
    tableName = "scan_pages",
    foreignKeys = [
        ForeignKey(
            entity = ScanSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class ScanPageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val orderIndex: Int,
    val originalImagePath: String,
    val cropTlX: Float = 0f,
    val cropTlY: Float = 0f,
    val cropTrX: Float = 1f,
    val cropTrY: Float = 0f,
    val cropBrX: Float = 1f,
    val cropBrY: Float = 1f,
    val cropBlX: Float = 0f,
    val cropBlY: Float = 1f,
    val rotationDegrees: Int = 0,
    // Matches ScanPage's own default -- see that class's doc for why Enhance, not Original. Plain
    // Kotlin default (no @ColumnInfo(defaultValue=...)), so this doesn't touch the SQL schema —
    // no migration needed, it only affects what newPageEntity() writes for pages created from now on.
    val filter: String = "ENHANCE"
)
