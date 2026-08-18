package com.docket.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * Plain (non "external content") FTS4 table. Room does not auto-generate the insert/update/
 * delete sync triggers external-content FTS4 tables need to stay in sync with a separate
 * content table, so rather than hand-writing and maintaining that trigger SQL, this table just
 * stores the searchable text directly — written alongside [PageOcrEntity] in the same
 * transaction (see `OcrDao.savePageText`). A little duplicated text; a page of OCR output is a
 * few KB, so at real-world scale (hundreds of documents) this is noise.
 *
 * `rowid` is Room's required FTS primary key convention — `Int`, not `Long`.
 */
@Fts4
@Entity(tableName = "page_ocr_fts")
data class PageOcrFtsEntity(
    @PrimaryKey @ColumnInfo(name = "rowid") val rowId: Int = 0,
    val documentId: Long,
    val pageIndex: Int,
    val text: String
)
