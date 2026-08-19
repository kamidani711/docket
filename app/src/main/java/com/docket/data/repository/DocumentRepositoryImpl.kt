package com.docket.data.repository

import com.docket.data.local.dao.DocumentDao
import com.docket.data.local.dao.DocumentRow
import com.docket.data.local.dao.DocumentWithPages
import com.docket.data.local.dao.FolderDao
import com.docket.data.local.entity.DocumentEntity
import com.docket.data.local.entity.DocumentPageEntity
import com.docket.data.local.entity.FolderEntity
import com.docket.domain.model.Document
import com.docket.domain.model.DocumentSort
import com.docket.domain.model.DocumentTypeFilter
import com.docket.domain.model.ExportFormat
import com.docket.domain.model.Folder
import com.docket.domain.repository.DocumentRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val documentDao: DocumentDao,
    private val folderDao: FolderDao
) : DocumentRepository {

    override fun observeFolders(parentFolderId: Long?): Flow<List<Folder>> =
        folderDao.observeByParent(parentFolderId).map { folders -> folders.map { it.toDomain() } }

    override suspend fun getFolder(folderId: Long): Folder? = folderDao.get(folderId)?.toDomain()

    override suspend fun createFolder(name: String, parentFolderId: Long?): Long =
        folderDao.insert(FolderEntity(name = name, parentFolderId = parentFolderId, createdAt = System.currentTimeMillis()))

    override suspend fun folderCount(): Int = folderDao.count()

    override fun observeFolderDocumentCounts(): Flow<Map<Long, Int>> =
        documentDao.observeFolderCounts().map { rows -> rows.associate { it.folderId to it.count } }

    override suspend fun getTitlesCreatedBetween(dayStart: Long, dayEnd: Long): List<String> =
        documentDao.getTitlesCreatedBetween(dayStart, dayEnd)

    override fun observeLibrary(
        folderId: Long?,
        typeFilter: DocumentTypeFilter,
        sortBy: DocumentSort
    ): Flow<List<Document>> =
        documentDao.observeLibraryRows(folderId, typeFilter.name, sortBy.name)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeRecentDocuments(limit: Int): Flow<List<Document>> =
        documentDao.observeRecent(limit).map { rows -> rows.map { it.toDomain() } }

    override fun observeLibrarySize(): Flow<Int> = documentDao.observeLibrarySize()

    override fun observeDocument(documentId: Long): Flow<Document?> =
        documentDao.observeWithPages(documentId).map { it?.toDomain() }

    override suspend fun getDocument(documentId: Long): Document? =
        documentDao.getWithPages(documentId)?.toDomain()

    override fun observeDeletedDocuments(): Flow<List<Document>> =
        documentDao.observeDeleted().map { entities -> entities.map { it.toDomain(hasReceipt = false, hasWarranty = false) } }

    override suspend fun softDeleteDocuments(documentIds: List<Long>) {
        documentDao.softDelete(documentIds, System.currentTimeMillis())
    }

    override suspend fun restoreDocument(documentId: Long) {
        documentDao.restore(documentId)
    }

    override suspend fun moveDocuments(documentIds: List<Long>, folderId: Long?) {
        documentDao.moveToFolder(documentIds, folderId)
    }

    override suspend fun renameDocument(documentId: Long, title: String) {
        documentDao.rename(documentId, title)
    }

    override suspend fun getPurgeableDocuments(cutoff: Long): List<Document> =
        documentDao.getPurgeable(cutoff).map { it.toDomain(hasReceipt = false, hasWarranty = false) }

    override suspend fun hardDeleteDocument(documentId: Long) {
        documentDao.hardDelete(documentId)
    }

    override suspend fun insertDocument(
        title: String,
        folderId: Long?,
        format: ExportFormat,
        pageCount: Int,
        pdfFilePath: String?,
        pageImagePaths: List<String>,
        thumbnailPath: String?,
        sizeBytes: Long
    ): Long {
        val documentId = documentDao.insert(
            DocumentEntity(
                title = title,
                folderId = folderId,
                createdAt = System.currentTimeMillis(),
                pageCount = pageCount,
                format = format.name,
                pdfFilePath = pdfFilePath,
                thumbnailPath = thumbnailPath,
                sizeBytes = sizeBytes
            )
        )
        if (format == ExportFormat.IMAGE_SET && pageImagePaths.isNotEmpty()) {
            documentDao.insertPages(
                pageImagePaths.mapIndexed { index, path ->
                    DocumentPageEntity(documentId = documentId, orderIndex = index, imagePath = path)
                }
            )
        }
        return documentId
    }
}

private fun FolderEntity.toDomain(): Folder = Folder(id = id, name = name, parentFolderId = parentFolderId, createdAt = createdAt)

private fun DocumentRow.toDomain(): Document = Document(
    id = id,
    title = title,
    folderId = folderId,
    createdAt = createdAt,
    pageCount = pageCount,
    format = ExportFormat.valueOf(format),
    pdfFilePath = pdfFilePath,
    pageImagePaths = emptyList(),
    thumbnailPath = thumbnailPath,
    sizeBytes = sizeBytes,
    deletedAt = deletedAt,
    hasReceipt = hasReceipt,
    hasWarranty = hasWarranty
)

private fun DocumentWithPages.toDomain(): Document = document.toDomain(
    pageImagePaths = pages.sortedBy { it.orderIndex }.map { it.imagePath },
    hasReceipt = false,
    hasWarranty = false
)

private fun DocumentEntity.toDomain(hasReceipt: Boolean, hasWarranty: Boolean): Document = toDomain(
    pageImagePaths = emptyList(),
    hasReceipt = hasReceipt,
    hasWarranty = hasWarranty
)

private fun DocumentEntity.toDomain(pageImagePaths: List<String>, hasReceipt: Boolean, hasWarranty: Boolean): Document = Document(
    id = id,
    title = title,
    folderId = folderId,
    createdAt = createdAt,
    pageCount = pageCount,
    format = ExportFormat.valueOf(format),
    pdfFilePath = pdfFilePath,
    pageImagePaths = pageImagePaths,
    thumbnailPath = thumbnailPath,
    sizeBytes = sizeBytes,
    deletedAt = deletedAt,
    hasReceipt = hasReceipt,
    hasWarranty = hasWarranty
)
