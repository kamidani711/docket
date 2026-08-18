package com.docket.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * OCR output for one page — the plain source-of-truth row. [wordsBlob] is a lightweight
 * delimited serialization of per-word bounding boxes (`text\tleft\ttop\tright\tbottom`, one
 * word per line) rather than a separate per-word table or a JSON library dependency; see
 * `data/mlkit/PageOcrMapper.kt` for the (de)serialization. [language] is the
 * [com.docket.domain.model.OcrLanguage] enum name.
 */
@Entity(
    tableName = "page_ocr",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
data class PageOcrEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val pageIndex: Int,
    val text: String,
    val wordsBlob: String,
    val language: String,
    val recognizedAt: Long
)
