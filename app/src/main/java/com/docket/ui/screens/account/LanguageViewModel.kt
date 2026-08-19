package com.docket.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.common.PremiumRequiredException
import com.docket.domain.common.Result
import com.docket.domain.model.LanguagePackState
import com.docket.domain.model.OcrLanguage
import com.docket.domain.repository.OcrRepository
import com.docket.domain.repository.PremiumRepository
import com.docket.domain.usecase.InstallLanguagePackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** OCR recognition-language packs — folded onto the Language screen alongside the app's own
 *  display language (see [com.docket.ui.screens.account.LanguageScreen]): both concerns are
 *  "which languages does Docket handle," just for different parts of the app. */
@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val ocrRepository: OcrRepository,
    private val premiumRepository: PremiumRepository,
    private val installLanguagePackUseCase: InstallLanguagePackUseCase
) : ViewModel() {

    val languagePacks: StateFlow<List<LanguagePackState>> = ocrRepository.observeLanguagePacks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isPremium: StateFlow<Boolean> = premiumRepository.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _installingLanguage = MutableStateFlow<OcrLanguage?>(null)
    val installingLanguage: StateFlow<OcrLanguage?> = _installingLanguage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

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

    fun dismissError() {
        _errorMessage.value = null
    }
}
