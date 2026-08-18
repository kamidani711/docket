package com.docket.domain.usecase

import com.docket.domain.repository.DocumentRepository
import java.io.File
import javax.inject.Inject

/**
 * Permanently deletes one document: its files (the whole parent directory of whichever file
 * path is available — PDF, thumbnail, or first page image, since every document's files live
 * together under `filesDir/documents/<uuid>/`, see ExportScanSessionUseCase) and its DB row.
 * Used directly by "Delete Forever" in Recently Deleted, and by [PurgeDeletedDocumentsUseCase]
 * for the scheduled 30-day sweep — one place doing the actual deletion so both can't drift.
 */
class HardDeleteDocumentUseCase @Inject constructor(
    private val documentRepository: DocumentRepository
) {
    suspend operator fun invoke(documentId: Long) {
        val document = documentRepository.getDocument(documentId) ?: return
        val anyFilePath = document.pdfFilePath ?: document.thumbnailPath ?: document.pageImagePaths.firstOrNull()
        anyFilePath?.let { path -> File(path).parentFile?.deleteRecursively() }
        documentRepository.hardDeleteDocument(documentId)
    }
}
