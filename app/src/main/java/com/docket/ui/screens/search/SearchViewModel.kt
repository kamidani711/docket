package com.docket.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.model.AnalyticsEventType
import com.docket.domain.model.SearchResult
import com.docket.domain.repository.AnalyticsRepository
import com.docket.domain.usecase.SearchDocumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchDocumentsUseCase: SearchDocumentsUseCase,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results: StateFlow<List<SearchResult>> = _results.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _results.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS) // avoid a query per keystroke
            _isSearching.value = true
            _results.value = searchDocumentsUseCase(newQuery)
            _isSearching.value = false
            // Logged post-debounce, once per query actually run — not once per keystroke.
            analyticsRepository.logEvent(AnalyticsEventType.SEARCH_PERFORMED)
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 250L
    }
}
