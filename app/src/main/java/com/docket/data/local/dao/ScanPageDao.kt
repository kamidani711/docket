package com.docket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.docket.data.local.entity.ScanPageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanPageDao {
    @Query("SELECT * FROM scan_pages WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    fun observePages(sessionId: String): Flow<List<ScanPageEntity>>

    @Insert
    suspend fun insertAll(pages: List<ScanPageEntity>)

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM scan_pages WHERE sessionId = :sessionId")
    suspend fun maxOrderIndex(sessionId: String): Int

    @Query("DELETE FROM scan_pages WHERE id = :pageId")
    suspend fun delete(pageId: String)

    @Query(
        """
        UPDATE scan_pages SET
            cropTlX = :tlX, cropTlY = :tlY,
            cropTrX = :trX, cropTrY = :trY,
            cropBrX = :brX, cropBrY = :brY,
            cropBlX = :blX, cropBlY = :blY
        WHERE id = :pageId
        """
    )
    suspend fun updateCorners(
        pageId: String,
        tlX: Float,
        tlY: Float,
        trX: Float,
        trY: Float,
        brX: Float,
        brY: Float,
        blX: Float,
        blY: Float
    )

    /** Atomic relative rotation — the new value is computed by SQLite from whatever
     *  `rotationDegrees` is in the row *right now*, never from a value read into Kotlin first.
     *  That's deliberate: reading the current rotation into the ViewModel, adding 90, and
     *  writing the sum back raced under rapid taps (tap twice before the first write's Flow
     *  update reaches a fresh `ScanPage`, and the second tap adds 90 to the same stale base the
     *  first one did) — the page would visibly rotate once and then "snap back" instead of
     *  landing on 180. Doing the `+ :deltaDegrees` inside the UPDATE itself closes that window:
     *  every tap composes onto whatever the previous write actually committed. */
    @Query("UPDATE scan_pages SET rotationDegrees = ((rotationDegrees + :deltaDegrees) % 360 + 360) % 360 WHERE id = :pageId")
    suspend fun rotateBy(pageId: String, deltaDegrees: Int)

    @Query("UPDATE scan_pages SET filter = :filter WHERE id = :pageId")
    suspend fun updateFilter(pageId: String, filter: String)

    @Query(
        """
        UPDATE scan_pages SET
            originalImagePath = :path,
            cropTlX = 0, cropTlY = 0, cropTrX = 1, cropTrY = 0,
            cropBrX = 1, cropBrY = 1, cropBlX = 0, cropBlY = 1,
            rotationDegrees = 0, filter = 'ORIGINAL'
        WHERE id = :pageId
        """
    )
    suspend fun replaceImageAndResetEdits(pageId: String, path: String)

    @Query("UPDATE scan_pages SET orderIndex = :index WHERE id = :pageId")
    suspend fun updateOrderIndex(pageId: String, index: Int)

    /** Applies a full new ordering in one transaction so observers never see a half-reordered list. */
    @Transaction
    suspend fun reorder(orderedPageIds: List<String>) {
        orderedPageIds.forEachIndexed { index, pageId -> updateOrderIndex(pageId, index) }
    }
}
