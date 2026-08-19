package com.docket.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * `null` until the first DataStore read completes — [OnboardingGate] treats that as "don't know
 * yet, show nothing" rather than defaulting to either state, mirroring [com.docket.ui.lock
 * .AppLockViewModel] exactly.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val hasSeenOnboarding: StateFlow<Boolean?> = settingsRepository.hasSeenOnboarding
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _showWelcomeAuth = MutableStateFlow(false)
    val showWelcomeAuth: StateFlow<Boolean> = _showWelcomeAuth.asStateFlow()

    fun markSeen() {
        _showWelcomeAuth.value = true
        viewModelScope.launch { settingsRepository.setOnboardingSeen() }
    }

    fun finishWelcomeAuth() {
        _showWelcomeAuth.value = false
    }
}
