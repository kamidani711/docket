package com.docket.domain.repository

import com.docket.domain.model.LanguagePackState
import com.docket.domain.model.OcrLanguage
import com.docket.domain.model.PageOcrData
import kotlinx.coroutines.flow.Flow

interface OcrRepository {
    /** All five languages with their current install status, for the Settings screen. */
    fun observeLanguagePacks(): Flow<List<LanguagePackState>>

    /** Which languages OCR should actually run with right now (Latin + whatever's installed). */
    suspend fun installedLanguages(): List<OcrLanguage>

    suspend fun installLanguagePack(language: OcrLanguage)
    suspend fun removeLanguagePack(language: OcrLanguage)

    suspend fun savePageOcr(result: PageOcrData)
    suspend fun getPageOcr(documentId: Long, pageIndex: Int): PageOcrData?
    fun observeHasOcrText(documentId: Long): Flow<Boolean>
}
