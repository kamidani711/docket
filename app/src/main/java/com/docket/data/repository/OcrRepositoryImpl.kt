package com.docket.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.docket.data.local.dao.OcrDao
import com.docket.data.local.entity.PageOcrEntity
import com.docket.data.mlkit.MultiScriptTextRecognizerImpl
import com.docket.data.mlkit.PageOcrMapper
import com.docket.data.settings.SettingsKeys
import com.docket.domain.model.LanguagePackState
import com.docket.domain.model.LanguagePackStatus
import com.docket.domain.model.OcrLanguage
import com.docket.domain.model.PageOcrData
import com.docket.domain.repository.OcrRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class OcrRepositoryImpl @Inject constructor(
    private val ocrDao: OcrDao,
    private val dataStore: DataStore<Preferences>,
    // Concrete type, not the MultiScriptTextRecognizer domain interface — `warmUp` is a
    // download-trigger quirk specific to this one implementation, not a general OCR concept.
    private val multiScriptTextRecognizer: MultiScriptTextRecognizerImpl
) : OcrRepository {

    override fun observeLanguagePacks(): Flow<List<LanguagePackState>> =
        dataStore.data.map { prefs ->
            val installed = prefs[SettingsKeys.INSTALLED_OCR_LANGUAGES].orEmpty()
            OcrLanguage.entries.map { language ->
                val status = if (language.alwaysAvailable || language.name in installed) {
                    LanguagePackStatus.INSTALLED
                } else {
                    LanguagePackStatus.NOT_INSTALLED
                }
                LanguagePackState(language, status)
            }
        }

    override suspend fun installedLanguages(): List<OcrLanguage> {
        val installed = dataStore.data.first()[SettingsKeys.INSTALLED_OCR_LANGUAGES].orEmpty()
        return OcrLanguage.entries.filter { it.alwaysAvailable || it.name in installed }
    }

    override suspend fun installLanguagePack(language: OcrLanguage) {
        if (language.alwaysAvailable) return
        val succeeded = multiScriptTextRecognizer.warmUp(language)
        if (!succeeded) {
            error("Couldn't install ${language.displayName} — check your connection and try again.")
        }
        dataStore.edit { prefs ->
            val current = prefs[SettingsKeys.INSTALLED_OCR_LANGUAGES].orEmpty()
            prefs[SettingsKeys.INSTALLED_OCR_LANGUAGES] = current + language.name
        }
    }

    override suspend fun removeLanguagePack(language: OcrLanguage) {
        if (language.alwaysAvailable) return
        dataStore.edit { prefs ->
            val current = prefs[SettingsKeys.INSTALLED_OCR_LANGUAGES].orEmpty()
            prefs[SettingsKeys.INSTALLED_OCR_LANGUAGES] = current - language.name
        }
        // Deliberately doesn't try to evict the underlying Play-services model — it's a shared
        // module other apps may also use, and this app has no business deleting it out from
        // under them even if an API for that exists. This just stops treating it as installed.
    }

    override suspend fun savePageOcr(result: PageOcrData) {
        ocrDao.savePageText(
            PageOcrEntity(
                documentId = result.documentId,
                pageIndex = result.pageIndex,
                text = result.text,
                wordsBlob = PageOcrMapper.serializeWords(result.words),
                language = result.language.name,
                recognizedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun getPageOcr(documentId: Long, pageIndex: Int): PageOcrData? {
        val entity = ocrDao.getPageOcr(documentId, pageIndex) ?: return null
        return PageOcrData(
            documentId = entity.documentId,
            pageIndex = entity.pageIndex,
            text = entity.text,
            words = PageOcrMapper.deserializeWords(entity.wordsBlob),
            language = OcrLanguage.valueOf(entity.language)
        )
    }

    override fun observeHasOcrText(documentId: Long): Flow<Boolean> =
        ocrDao.observeHasOcrText(documentId)
}
