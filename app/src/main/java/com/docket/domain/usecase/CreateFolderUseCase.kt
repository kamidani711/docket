package com.docket.domain.usecase

import com.docket.domain.common.PremiumRequiredException
import com.docket.domain.common.Result
import com.docket.domain.model.FREE_FOLDER_LIMIT
import com.docket.domain.model.PremiumFeature
import com.docket.domain.repository.DocumentRepository
import com.docket.domain.repository.PremiumRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Free tier caps total folders at [FREE_FOLDER_LIMIT]; Premium removes the cap entirely. The
 * check lives here — one place both the Library screen and the save-time folder picker go
 * through — rather than duplicated in each ViewModel or pushed into [DocumentRepository], which
 * has no reason to know about premium gating.
 */
class CreateFolderUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val premiumRepository: PremiumRepository
) {
    suspend operator fun invoke(name: String, parentFolderId: Long?): Result<Long> {
        val isPremium = premiumRepository.isPremium.first()
        if (!isPremium && documentRepository.folderCount() >= FREE_FOLDER_LIMIT) {
            return Result.Error(PremiumRequiredException(PremiumFeature.UNLIMITED_FOLDERS))
        }
        return Result.Success(documentRepository.createFolder(name, parentFolderId))
    }
}
