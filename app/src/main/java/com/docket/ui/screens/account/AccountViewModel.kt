package com.docket.ui.screens.account

import android.content.Context
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.model.LocalProfile
import com.docket.domain.model.StorageBreakdown
import com.docket.domain.repository.PremiumRepository
import com.docket.domain.repository.ProfileRepository
import com.docket.domain.repository.SettingsRepository
import com.docket.domain.repository.StorageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val premiumRepository: PremiumRepository,
    private val settingsRepository: SettingsRepository,
    private val storageRepository: StorageRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val profile: StateFlow<LocalProfile?> = profileRepository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isSignedIn: StateFlow<Boolean> = profileRepository.isSignedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isPremium: StateFlow<Boolean> = premiumRepository.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val darkModeOverride: StateFlow<Boolean?> = settingsRepository.darkModeOverride
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _storageBreakdown = MutableStateFlow<StorageBreakdown?>(null)
    val storageBreakdown: StateFlow<StorageBreakdown?> = _storageBreakdown.asStateFlow()

    private val _isClearingCache = MutableStateFlow(false)
    val isClearingCache: StateFlow<Boolean> = _isClearingCache.asStateFlow()

    /** Real device-storage free space (`StatFs` on the app's own data partition), not a
     *  fabricated quota — Docket doesn't have or need a storage cap of its own. */
    private val _deviceFreeBytes = MutableStateFlow(0L)
    val deviceFreeBytes: StateFlow<Long> = _deviceFreeBytes.asStateFlow()

    init {
        refreshStorage()
    }

    fun refreshStorage() {
        viewModelScope.launch {
            _storageBreakdown.value = storageRepository.getStorageBreakdown()
            val stat = StatFs(appContext.filesDir.path)
            _deviceFreeBytes.value = stat.availableBytes
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            _isClearingCache.value = true
            storageRepository.clearCache()
            _storageBreakdown.value = storageRepository.getStorageBreakdown()
            _isClearingCache.value = false
        }
    }

    fun setDarkModeOverride(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDarkModeOverride(enabled) }
    }

    /** Clears the signed-in flag only — the stored profile stays put. */
    fun signOut() {
        viewModelScope.launch { profileRepository.signOut() }
    }
}
