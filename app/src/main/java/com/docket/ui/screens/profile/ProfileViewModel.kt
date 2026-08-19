package com.docket.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.model.LocalProfile
import com.docket.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Backs Sign in / Sign up / Reset profile — all three are the same small local operation set,
 *  see [com.docket.domain.repository.ProfileRepository]'s class doc for why. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    val profile: StateFlow<LocalProfile?> = profileRepository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>()
    val events: SharedFlow<ProfileEvent> = _events

    fun signUp(displayName: String, email: String, passphrase: String) {
        if (displayName.isBlank() || email.isBlank() || passphrase.length < 4) {
            _error.value = "Enter a name, email, and a password of at least 4 characters."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            _error.value = null
            profileRepository.saveProfile(displayName.trim(), email.trim(), passphrase)
            _isBusy.value = false
            _events.emit(ProfileEvent.Done)
        }
    }

    fun signIn(email: String, passphrase: String) {
        if (email.isBlank() || passphrase.isEmpty()) {
            _error.value = "Enter your email and password."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            _error.value = null
            val success = profileRepository.signIn(email.trim(), passphrase)
            _isBusy.value = false
            if (success) {
                _events.emit(ProfileEvent.Done)
            } else {
                _error.value = "That email and password don't match this device's local profile."
            }
        }
    }

    /** The honest local equivalent of a password reset — there's no email to send a reset link
     *  through, so this just re-establishes the profile in place, same as [signUp]. */
    fun resetProfile(displayName: String, email: String, passphrase: String) = signUp(displayName, email, passphrase)

    fun updatePersonalInfo(displayName: String, email: String) {
        if (displayName.isBlank() || email.isBlank()) {
            _error.value = "Name and email can't be empty."
            return
        }
        viewModelScope.launch {
            profileRepository.updatePersonalInfo(displayName.trim(), email.trim())
            _events.emit(ProfileEvent.Done)
        }
    }

    fun deleteProfile() {
        viewModelScope.launch {
            profileRepository.deleteProfile()
            _events.emit(ProfileEvent.Done)
        }
    }

    fun dismissError() {
        _error.value = null
    }
}

sealed interface ProfileEvent {
    data object Done : ProfileEvent
}
