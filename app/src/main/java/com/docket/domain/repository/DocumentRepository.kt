package com.docket.domain.repository

import com.docket.domain.model.Document
import com.docket.domain.model.DocumentSort
import com.docket.domain.model.DocumentTypeFilter
import com.docket.domain.model.ExportFormat
import com.docket.domain.model.Folder
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun observeFolders(parentFolderId: Long?): Flow<List<Folder>>
    suspend fun getFolder(folderId: Long): Folder?
    suspend fun createFolder(name: String, parentFolderId: Long?): Long
    suspend fun folderCount(): Int
    /** Active (non-deleted) document count per folder id — folders with none don't appear. */
    fun observeFolderDocumentCounts(): Flow<Map<Long, Int>>

    /** [folderId] null = root. */
    fun observeLibrary(folderId: Long?, typeFilter: DocumentTypeFilter, sortBy: DocumentSort): Flow<List<Document>>

    /** Most recent documents across every folder, newest first — Home's "Recent Files". */
    fun observeRecentDocuments(limit: Int): Flow<List<Document>>

    /** Total active (non-deleted) document count across the whole library — Files' header
     *  count line ("Total N files · X MB"). */
    fun observeLibrarySize(): Flow<Int>

    fun observeDocument(documentId: Long): Flow<Document?>
    suspend fun getDocument(documentId: Long): Document?

    /** Titles of non-deleted documents created within [dayStart, dayEnd) (epoch millis,
     *  [dayStart] inclusive, [dayEnd] exclusive) — used to append a same-day counter to the
     *  default "Scan – <date>" name when more than one untitled scan lands on the same day. */
    suspend fun getTitlesCreatedBetween(dayStart: Long, dayEnd: Long): List<String>

    fun observeDeletedDocuments(): Flow<List<Document>>
    suspend fun softDeleteDocuments(documentIds: List<Long>)
    suspend fun restoreDocument(documentId: Long)
    suspend fun moveDocuments(documentIds: List<Long>, folderId: Long?)
    /** [title] is stored as-is — trimming/blank-rejection is the caller's job (see
     *  DocumentDetailViewModel.renameDocument), same division of labor as the save flow's
     *  readable-name-vs-filesystem-safe-name split. */
    suspend fun renameDocument(documentId: Long, title: String)

    /** Documents soft-deleted before [cutoff] — the Recently Deleted bin's 30-day purge. */
    suspend fun getPurgeableDocuments(cutoff: Long): List<Document>
    suspend fun hardDeleteDocument(documentId: Long)

    /** Persists a finished document row (+ page rows for IMAGE_SET). Files are already on disk.
     *  [pageCount] is explicit rather than inferred (e.g. from [pageImagePaths].size) because a
     *  PDF's page count isn't derivable from what this call receives for PDF documents. */
    suspend fun insertDocument(
        title: String,
        folderId: Long?,
        format: ExportFormat,
        pageCount: Int,
        pdfFilePath: String?,
        pageImagePaths: List<String>,
        thumbnailPath: String?,
        sizeBytes: Long
    ): Long
}
