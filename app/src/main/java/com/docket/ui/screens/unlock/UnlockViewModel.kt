package com.docket.ui.screens.unlock

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.common.Result
import com.docket.domain.repository.BillingRepository
import com.docket.domain.repository.PremiumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class UnlockViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    premiumRepository: PremiumRepository
) : ViewModel() {

    val isPremium: StateFlow<Boolean> = premiumRepository.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val price: StateFlow<String?> = billingRepository.premiumPrice
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            billingRepository.purchaseErrors.collect { error -> _message.value = error }
        }
    }

    fun purchase(activity: Activity) {
        viewModelScope.launch {
            _isBusy.value = true
            _message.value = null
            val result = billingRepository.launchPurchaseFlow(activity)
            // A successful launch just means Play's own purchase sheet took over — there's
            // nothing to show here either way; the outcome arrives later through
            // BillingRepository.purchaseErrors or the isPremium flow flipping to true.
            if (result is Result.Error) _message.value = result.message ?: "Couldn't start the purchase."
            _isBusy.value = false
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _isBusy.value = true
            _message.value = null
            when (val result = billingRepository.syncPurchases()) {
                is Result.Success -> _message.value = if (result.data) {
                    "Premium restored."
                } else {
                    "No previous purchase found for this Google account."
                }
                is Result.Error -> _message.value = result.message ?: "Couldn't check for previous purchases."
                Result.Loading -> Unit
            }
            _isBusy.value = false
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
