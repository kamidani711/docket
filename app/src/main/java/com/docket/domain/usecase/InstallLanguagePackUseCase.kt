package com.docket.domain.usecase

import com.docket.domain.common.PremiumRequiredException
import com.docket.domain.common.Result
import com.docket.domain.model.OcrLanguage
import com.docket.domain.model.PremiumFeature
import com.docket.domain.repository.OcrRepository
import com.docket.domain.repository.PremiumRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class InstallLanguagePackUseCase @Inject constructor(
    private val ocrRepository: OcrRepository,
    private val premiumRepository: PremiumRepository
) {
    suspend operator fun invoke(language: OcrLanguage): Result<Unit> {
        if (language.alwaysAvailable) return Result.Success(Unit) // Latin — nothing to install

        val isPremium = premiumRepository.isPremium.first()
        if (!isPremium) {
            return Result.Error(PremiumRequiredException(PremiumFeature.ADDITIONAL_OCR_LANGUAGES))
        }
        // installLanguagePack throws on a failed download trigger (see OcrRepositoryImpl) —
        // caught here rather than left to crash the caller's coroutine.
        return try {
            ocrRepository.installLanguagePack(language)
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(t)
        }
    }
}
