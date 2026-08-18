package com.docket.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.common.PremiumRequiredException
import com.docket.domain.common.Result
import com.docket.domain.model.AnalyticsEventType
import com.docket.domain.model.DocumentSort
import com.docket.domain.model.ExportFormat
import com.docket.domain.model.LanguagePackState
import com.docket.domain.model.OcrLanguage
import com.docket.domain.model.StorageBreakdown
import com.docket.domain.repository.AnalyticsRepository
import com.docket.domain.repository.OcrRepository
import com.docket.domain.repository.PremiumRepository
import com.docket.domain.repository.SettingsRepository
import com.docket.domain.repository.StorageRepository
import com.docket.domain.usecase.InstallLanguagePackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val ocrRepository: OcrRepository,
    private val premiumRepository: PremiumRepository,
    private val settingsRepository: SettingsRepository,
    private val storageRepository: StorageRepository,
    private val installLanguagePackUseCase: InstallLanguagePackUseCase,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    val languagePacks: StateFlow<List<LanguagePackState>> = ocrRepository.observeLanguagePacks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isPremium: StateFlow<Boolean> = premiumRepository.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val defaultExportFormat: StateFlow<ExportFormat> = settingsRepository.defaultExportFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExportFormat.PDF)

    val defaultLibrarySort: StateFlow<DocumentSort> = settingsRepository.defaultLibrarySort
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DocumentSort.DATE)

    val appLockEnabled: StateFlow<Boolean> = settingsRepository.appLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _installingLanguage = MutableStateFlow<OcrLanguage?>(null)
    val installingLanguage: StateFlow<OcrLanguage?> = _installingLanguage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _storageBreakdown = MutableStateFlow<StorageBreakdown?>(null)
    val storageBreakdown: StateFlow<StorageBreakdown?> = _storageBreakdown.asStateFlow()

    private val _isClearingCache = MutableStateFlow(false)
    val isClearingCache: StateFlow<Boolean> = _isClearingCache.asStateFlow()

    init {
        refreshStorageBreakdown()
    }

    fun refreshStorageBreakdown() {
        viewModelScope.launch { _storageBreakdown.value = storageRepository.getStorageBreakdown() }
    }

    fun clearCache() {
        viewModelScope.launch {
            _isClearingCache.value = true
            storageRepository.clearCache()
            _storageBreakdown.value = storageRepository.getStorageBreakdown()
            _isClearingCache.value = false
        }
    }

    fun installLanguage(language: OcrLanguage) {
        viewModelScope.launch {
            _installingLanguage.value = language
            when (val result = installLanguagePackUseCase(language)) {
                is Result.Error -> _errorMessage.value = if (result.throwable is PremiumRequiredException) {
                    "Unlock Premium to install additional language packs."
                } else {
                    result.message ?: "Couldn't install that language pack."
                }
                else -> Unit
            }
            _installingLanguage.value = null
        }
    }

    fun removeLanguage(language: OcrLanguage) {
        viewModelScope.launch { ocrRepository.removeLanguagePack(language) }
    }

    fun setDefaultExportFormat(format: ExportFormat) {
        viewModelScope.launch { settingsRepository.setDefaultExportFormat(format) }
    }

    fun setDefaultLibrarySort(sort: DocumentSort) {
        viewModelScope.launch { settingsRepository.setDefaultLibrarySort(sort) }
    }

    /** Only called after the caller (the biometric prompt in [SettingsScreen]) has already
     *  confirmed the toggle's target state actually works — see the comment there for why this
     *  ViewModel doesn't do that confirmation itself. */
    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAppLockEnabled(enabled)
            if (enabled) analyticsRepository.logEvent(AnalyticsEventType.FEATURE_USED, "app_lock_enabled")
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }
}
