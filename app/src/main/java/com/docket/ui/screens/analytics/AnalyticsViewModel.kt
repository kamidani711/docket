package com.docket.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.model.AnalyticsSummary
import com.docket.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    val summary: StateFlow<AnalyticsSummary> = analyticsRepository.observeSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsSummary())

    fun clearAll() {
        viewModelScope.launch { analyticsRepository.clearAll() }
    }
}
