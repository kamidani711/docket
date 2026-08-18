package com.docket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.docket.data.local.entity.ScanSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanSessionDao {
    // There's only ever one row (see ScanSessionEntity); LIMIT 1 is defensive, not load-bearing.
    @Query("SELECT * FROM scan_sessions LIMIT 1")
    fun observeSession(): Flow<ScanSessionEntity?>

    @Insert
    suspend fun insert(session: ScanSessionEntity)

    @Query("UPDATE scan_sessions SET suggestedName = :name, updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun updateSuggestedName(sessionId: String, name: String, updatedAt: Long)

    @Query("DELETE FROM scan_sessions")
    suspend fun deleteAll()

    @Query("DELETE FROM scan_sessions WHERE id = :sessionId")
    suspend fun delete(sessionId: String)
}
