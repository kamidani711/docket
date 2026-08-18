package com.docket.domain.model

/**
 * A saved, finished document — the output of the scan flow (or a straight PDF import).
 * [pageImagePaths] is only populated for [ExportFormat.IMAGE_SET]; PDF documents live entirely
 * in [pdfFilePath]. [hasReceipt]/[hasWarranty] back the Library's type filter — computed via a
 * join at query time (see DocumentDao), not stored redundantly. [deletedAt] non-null means the
 * document is in the Recently Deleted bin.
 */
data class Document(
    val id: Long,
    val title: String,
    val folderId: Long?,
    val createdAt: Long,
    val pageCount: Int,
    val format: ExportFormat,
    val pdfFilePath: String?,
    val pageImagePaths: List<String>,
    val thumbnailPath: String?,
    val sizeBytes: Long = 0,
    val deletedAt: Long? = null,
    val hasReceipt: Boolean = false,
    val hasWarranty: Boolean = false
)

enum class DocumentSort { DATE, NAME, SIZE }

enum class DocumentTypeFilter { ALL, PLAIN, RECEIPT, WARRANTY }
