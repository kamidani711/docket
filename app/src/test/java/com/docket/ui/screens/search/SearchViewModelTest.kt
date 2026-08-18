package com.docket.ui.screens.search

import com.docket.domain.model.AnalyticsEventType
import com.docket.domain.model.AnalyticsSummary
import com.docket.domain.model.SearchResult
import com.docket.domain.repository.AnalyticsRepository
import com.docket.domain.repository.SearchRepository
import com.docket.domain.usecase.SearchDocumentsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Plain JVM test using [StandardTestDispatcher] as `Dispatchers.Main` (ViewModel scopes need a
 * Main dispatcher, which doesn't exist by default outside an Android runtime) plus hand-written
 * fakes — no Room, no real FTS query, no device needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `blank query clears results immediately without querying`() = runTest {
        val searchRepository = FakeSearchRepository(resultsToReturn = listOf(sampleResult()))
        val viewModel = SearchViewModel(SearchDocumentsUseCase(searchRepository), FakeAnalyticsRepository())

        viewModel.onQueryChanged("")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.results.value.isEmpty())
        assertEquals(0, searchRepository.queriesReceived.size)
    }

    @Test
    fun `debounces rapid typing into a single query`() = runTest {
        val searchRepository = FakeSearchRepository(resultsToReturn = listOf(sampleResult()))
        val analyticsRepository = FakeAnalyticsRepository()
        val viewModel = SearchViewModel(SearchDocumentsUseCase(searchRepository), analyticsRepository)

        // Simulates fast typing: each call cancels the previous debounce window.
        viewModel.onQueryChanged("r")
        testDispatcher.scheduler.advanceTimeBy(50)
        viewModel.onQueryChanged("re")
        testDispatcher.scheduler.advanceTimeBy(50)
        viewModel.onQueryChanged("receipt")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, searchRepository.queriesReceived.size)
        assertEquals("text:receipt*", searchRepository.queriesReceived.single())
        assertEquals(1, viewModel.results.value.size)
        assertEquals(1, analyticsRepository.loggedEvents.count { it.first == AnalyticsEventType.SEARCH_PERFORMED })
    }

    @Test
    fun `isSearching is true only while the query is actually in flight`() = runTest {
        // The fake's own delay is what makes "in flight" observable at all — without a real
        // suspension point here, the whole debounce-then-search chain would run to completion
        // in one scheduler step and there'd be no window in which isSearching is true.
        val searchRepository = FakeSearchRepository(resultsToReturn = emptyList(), searchDelayMs = 100)
        val viewModel = SearchViewModel(SearchDocumentsUseCase(searchRepository), FakeAnalyticsRepository())

        viewModel.onQueryChanged("invoice")
        assertFalse(viewModel.isSearching.value) // still inside the 250ms debounce window

        testDispatcher.scheduler.advanceTimeBy(260) // past debounce, into the fake's own 100ms delay
        assertTrue(viewModel.isSearching.value)

        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.isSearching.value)
    }

    private fun sampleResult() = SearchResult(
        documentId = 1L,
        documentTitle = "Receipt",
        pageIndex = 0,
        snippetText = "a receipt for groceries",
        snippetMatchStart = "",
        snippetMatchEnd = ""
    )

    private class FakeSearchRepository(
        private val resultsToReturn: List<SearchResult>,
        private val searchDelayMs: Long = 0
    ) : SearchRepository {
        val queriesReceived = mutableListOf<String>()
        override suspend fun search(query: String, limit: Int): List<SearchResult> {
            queriesReceived += query
            if (searchDelayMs > 0) delay(searchDelayMs)
            return resultsToReturn
        }
    }

    private class FakeAnalyticsRepository : AnalyticsRepository {
        val loggedEvents = mutableListOf<Pair<AnalyticsEventType, String?>>()
        override suspend fun logEvent(type: AnalyticsEventType, detail: String?) {
            loggedEvents += type to detail
        }
        override fun observeSummary() = flowOf(AnalyticsSummary())
        override suspend fun clearAll() = Unit
    }
}
