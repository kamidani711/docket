package com.docket.ui.lock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * `null` until the first DataStore read completes — [AppLockGate] treats that as "don't know
 * yet, show nothing" rather than defaulting to unlocked, so a locked app never flashes its
 * content for one frame while settings are still loading.
 */
@HiltViewModel
class AppLockViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {

    val appLockEnabled: StateFlow<Boolean?> = settingsRepository.appLockEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
