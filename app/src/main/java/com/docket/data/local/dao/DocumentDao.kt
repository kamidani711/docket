package com.docket.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.docket.data.local.entity.DocumentEntity
import com.docket.data.local.entity.DocumentPageEntity
import kotlinx.coroutines.flow.Flow

data class DocumentWithPages(
    @Embedded val document: DocumentEntity,
    @Relation(parentColumn = "id", entityColumn = "documentId")
    val pages: List<DocumentPageEntity>
)

/** Lightweight row for the Library list — no page relation (the list only needs
 *  [thumbnailPath]), plus the two computed flags the type filter needs. */
data class DocumentRow(
    val id: Long,
    val title: String,
    val folderId: Long?,
    val createdAt: Long,
    val pageCount: Int,
    val format: String,
    val pdfFilePath: String?,
    val thumbnailPath: String?,
    val sizeBytes: Long,
    val deletedAt: Long?,
    val hasReceipt: Boolean,
    val hasWarranty: Boolean
)

@Dao
interface DocumentDao {
    @Insert
    suspend fun insert(document: DocumentEntity): Long

    @Insert
    suspend fun insertPages(pages: List<DocumentPageEntity>)

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :documentId")
    fun observeWithPages(documentId: Long): Flow<DocumentWithPages?>

    @Transaction
    @Query("SELECT * FROM documents WHERE id = :documentId")
    suspend fun getWithPages(documentId: Long): DocumentWithPages?

    /** [folderId] null means "root" (documents with no folder), not "any folder" — an exact
     *  match either way. [typeFilter] is one of ALL/PLAIN/RECEIPT/WARRANTY; [sortBy] one of
     *  DATE/NAME/SIZE — see [com.docket.domain.model.DocumentTypeFilter]/[com.docket.domain
     *  .model.DocumentSort]. Only one of the three ORDER BY expressions is ever non-null for a
     *  given [sortBy], so the other two don't affect ordering. */
    @Query(
        """
        SELECT d.*,
               EXISTS(SELECT 1 FROM receipts r WHERE r.documentId = d.id) AS hasReceipt,
               EXISTS(SELECT 1 FROM warranties w WHERE w.documentId = d.id) AS hasWarranty
        FROM documents d
        WHERE d.deletedAt IS NULL
          AND ((:folderId IS NULL AND d.folderId IS NULL) OR d.folderId = :folderId)
          AND (
            :typeFilter = 'ALL'
            OR (:typeFilter = 'PLAIN' AND NOT EXISTS(SELECT 1 FROM receipts r2 WHERE r2.documentId = d.id))
            OR (:typeFilter = 'RECEIPT' AND EXISTS(SELECT 1 FROM receipts r3 WHERE r3.documentId = d.id))
            OR (:typeFilter = 'WARRANTY' AND EXISTS(SELECT 1 FROM warranties w2 WHERE w2.documentId = d.id))
          )
        ORDER BY
          CASE WHEN :sortBy = 'NAME' THEN d.title END ASC,
          CASE WHEN :sortBy = 'SIZE' THEN d.sizeBytes END DESC,
          CASE WHEN :sortBy = 'DATE' THEN d.createdAt END DESC
        """
    )
    fun observeLibraryRows(folderId: Long?, typeFilter: String, sortBy: String): Flow<List<DocumentRow>>

    @Query("SELECT * FROM documents WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<DocumentEntity>>

    @Query("UPDATE documents SET deletedAt = :timestamp WHERE id IN (:documentIds)")
    suspend fun softDelete(documentIds: List<Long>, timestamp: Long)

    @Query("UPDATE documents SET deletedAt = NULL WHERE id = :documentId")
    suspend fun restore(documentId: Long)

    @Query("UPDATE documents SET folderId = :folderId WHERE id IN (:documentIds)")
    suspend fun moveToFolder(documentIds: List<Long>, folderId: Long?)

    @Query("UPDATE documents SET title = :title WHERE id = :documentId")
    suspend fun rename(documentId: Long, title: String)

    @Query("SELECT * FROM documents WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun getPurgeable(cutoff: Long): List<DocumentEntity>

    @Query("DELETE FROM documents WHERE id = :documentId")
    suspend fun hardDelete(documentId: Long)

    @Query("SELECT folderId, COUNT(*) AS count FROM documents WHERE deletedAt IS NULL AND folderId IS NOT NULL GROUP BY folderId")
    fun observeFolderCounts(): Flow<List<FolderCountRow>>

    /** Titles of non-deleted documents created within [dayStart, dayEnd) — backs the same-day
     *  counter suffix on the default "Scan – <date>" name (see SuggestDocumentNameUseCase). */
    @Query("SELECT title FROM documents WHERE deletedAt IS NULL AND createdAt >= :dayStart AND createdAt < :dayEnd")
    suspend fun getTitlesCreatedBetween(dayStart: Long, dayEnd: Long): List<String>
}

data class FolderCountRow(val folderId: Long, val count: Int)
