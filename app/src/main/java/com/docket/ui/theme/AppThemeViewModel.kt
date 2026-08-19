package com.docket.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Feeds `DocketTheme(darkTheme = ...)` in MainActivity — see SettingsRepository
 *  .darkModeOverride's doc for the "null = not yet set, fall back to system" convention. */
@HiltViewModel
class AppThemeViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {
    val darkModeOverride: StateFlow<Boolean?> = settingsRepository.darkModeOverride
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
