package com.docket.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One page of an IMAGE_SET document — the already-exported, final JPEG for that page. */
@Entity(
    tableName = "document_pages",
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
data class DocumentPageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val orderIndex: Int,
    val imagePath: String
)
