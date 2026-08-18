package com.docket.domain.usecase

import com.docket.domain.repository.DocumentRepository
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** The Recently Deleted bin's 30-day auto-purge — finds everything soft-deleted before the
 *  cutoff and hands each one to [HardDeleteDocumentUseCase] (same deletion path "Delete
 *  Forever" uses, so there's exactly one place that does the actual file/row cleanup). */
class PurgeDeletedDocumentsUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val hardDeleteDocumentUseCase: HardDeleteDocumentUseCase
) {
    suspend operator fun invoke() {
        val cutoff = System.currentTimeMillis() - RETENTION_DAYS_MILLIS
        val purgeable = documentRepository.getPurgeableDocuments(cutoff)
        purgeable.forEach { document -> hardDeleteDocumentUseCase(document.id) }
    }

    private companion object {
        val RETENTION_DAYS_MILLIS = TimeUnit.DAYS.toMillis(30)
    }
}
