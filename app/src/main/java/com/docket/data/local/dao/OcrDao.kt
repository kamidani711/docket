package com.docket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.docket.data.local.entity.PageOcrEntity
import com.docket.data.local.entity.PageOcrFtsEntity
import kotlinx.coroutines.flow.Flow

/** Row shape for [OcrDao.search] — column aliases match these property names exactly. */
data class SearchResultRow(
    val documentId: Long,
    val documentTitle: String,
    val pageIndex: Int,
    val snippet: String
)

@Dao
interface OcrDao {
    @Insert
    suspend fun insertPlain(entity: PageOcrEntity)

    @Insert
    suspend fun insertFts(entity: PageOcrFtsEntity)

    /** Writes the plain row and its FTS mirror together — see PageOcrFtsEntity for why there
     *  are two tables instead of one external-content FTS table. */
    @Transaction
    suspend fun savePageText(entity: PageOcrEntity) {
        insertPlain(entity)
        insertFts(
            PageOcrFtsEntity(documentId = entity.documentId, pageIndex = entity.pageIndex, text = entity.text)
        )
    }

    @Query("SELECT * FROM page_ocr WHERE documentId = :documentId AND pageIndex = :pageIndex LIMIT 1")
    suspend fun getPageOcr(documentId: Long, pageIndex: Int): PageOcrEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM page_ocr WHERE documentId = :documentId)")
    fun observeHasOcrText(documentId: Long): Flow<Boolean>

    // The `matchStart`/`matchEnd` markers passed through to snippet() are the UI's chosen
    // highlight delimiters, not user input — see SearchRepositoryImpl.
    @Query(
        """
        SELECT d.id AS documentId, d.title AS documentTitle, f.pageIndex AS pageIndex,
               snippet(page_ocr_fts, -1, :matchStart, :matchEnd, '…', 12) AS snippet
        FROM page_ocr_fts f
        JOIN documents d ON d.id = f.documentId
        WHERE page_ocr_fts MATCH :query
        LIMIT :limit
        """
    )
    suspend fun search(query: String, matchStart: String, matchEnd: String, limit: Int): List<SearchResultRow>
}
