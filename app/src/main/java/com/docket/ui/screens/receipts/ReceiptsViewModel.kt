package com.docket.ui.screens.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.model.MonthlySpend
import com.docket.domain.model.Receipt
import com.docket.domain.model.ReceiptFilter
import com.docket.domain.repository.ReceiptRepository
import com.docket.domain.usecase.ExportReceiptsCsvUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReceiptsViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val exportReceiptsCsvUseCase: ExportReceiptsCsvUseCase
) : ViewModel() {

    private val _filter = MutableStateFlow(ReceiptFilter())
    val filter: StateFlow<ReceiptFilter> = _filter.asStateFlow()

    val receipts: StateFlow<List<Receipt>> = _filter
        .flatMapLatest { currentFilter -> receiptRepository.observeAllReceipts(currentFilter) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val monthlySpend: StateFlow<List<MonthlySpend>> = receiptRepository.observeMonthlySpend()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val merchants: StateFlow<List<String>> = receiptRepository.observeMerchants()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _csvFile = MutableStateFlow<File?>(null)
    val csvFile: StateFlow<File?> = _csvFile.asStateFlow()

    private val _isExportingCsv = MutableStateFlow(false)
    val isExportingCsv: StateFlow<Boolean> = _isExportingCsv.asStateFlow()

    fun updateFilter(newFilter: ReceiptFilter) {
        _filter.value = newFilter
    }

    fun exportCsv() {
        viewModelScope.launch {
            _isExportingCsv.value = true
            _csvFile.value = exportReceiptsCsvUseCase(_filter.value)
            _isExportingCsv.value = false
        }
    }

    fun clearCsvFile() {
        _csvFile.value = null
    }
}
