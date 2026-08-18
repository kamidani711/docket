package com.docket.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A saved document. [format] is "PDF" or "IMAGE_SET" (stored as plain String rather than via a
 * Room TypeConverter for one column — mapped to/from [com.docket.domain.model.ExportFormat] in
 * the repository). PDF documents use [pdfFilePath]; image-set documents use [DocumentPageEntity]
 * rows instead.
 *
 * [deletedAt] is soft-delete (Library's Recently Deleted bin, auto-purged after 30 days) — a
 * null value means "active, shows in the normal library." [sizeBytes] is computed once at save
 * time (summed file size on disk) rather than recomputed on every list query.
 */
@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("folderId"), Index("deletedAt")]
)
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val folderId: Long?,
    val createdAt: Long,
    val pageCount: Int,
    val format: String,
    val pdfFilePath: String?,
    val thumbnailPath: String?,
    val sizeBytes: Long = 0,
    val deletedAt: Long? = null
)
