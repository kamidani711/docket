package com.docket.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The single in-progress scan draft. There is at most one row in this table ever — starting a
 * new scan deletes whatever was here first (see ScanSessionDao.deleteAll). That single-row
 * invariant is what makes "recover after process death" trivial: Room already wrote every edit
 * to disk, so on relaunch there's just one row to check for.
 */
@Entity(tableName = "scan_sessions")
data class ScanSessionEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val importedPdfPath: String? = null,
    val importedPdfPageCount: Int = 0,
    val suggestedName: String? = null
)
