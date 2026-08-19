package com.docket.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.model.AnalyticsEventType
import com.docket.domain.repository.AnalyticsRepository
import com.docket.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    val appLockEnabled: StateFlow<Boolean> = settingsRepository.appLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Only called after the caller (the biometric prompt in [SecurityScreen]) has already
     *  confirmed the toggle's target state actually works — see the comment there for why this
     *  ViewModel doesn't do that confirmation itself. */
    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAppLockEnabled(enabled)
            if (enabled) analyticsRepository.logEvent(AnalyticsEventType.FEATURE_USED, "app_lock_enabled")
        }
    }
}
