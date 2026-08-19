package com.docket.ui.screens.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.model.DocumentSort
import com.docket.domain.model.ExportFormat
import com.docket.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ScanPreferencesViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val defaultExportFormat: StateFlow<ExportFormat> = settingsRepository.defaultExportFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExportFormat.PDF)

    val defaultLibrarySort: StateFlow<DocumentSort> = settingsRepository.defaultLibrarySort
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DocumentSort.DATE)

    fun setDefaultExportFormat(format: ExportFormat) {
        viewModelScope.launch { settingsRepository.setDefaultExportFormat(format) }
    }

    fun setDefaultLibrarySort(sort: DocumentSort) {
        viewModelScope.launch { settingsRepository.setDefaultLibrarySort(sort) }
    }
}
