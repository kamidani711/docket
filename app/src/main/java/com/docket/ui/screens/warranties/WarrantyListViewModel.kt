package com.docket.ui.screens.warranties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.model.Warranty
import com.docket.domain.repository.WarrantyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class WarrantyListViewModel @Inject constructor(
    warrantyRepository: WarrantyRepository
) : ViewModel() {

    val warranties: StateFlow<List<Warranty>> = warrantyRepository.observeWarrantiesSortedByExpiry()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
