package com.docket.ui.screens.analytics

import com.docket.domain.model.AnalyticsEventType
import com.docket.domain.model.AnalyticsSummary
import com.docket.domain.repository.AnalyticsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

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
    fun `summary reflects whatever the repository reports`() = runTest {
        val repository = FakeAnalyticsRepository()
        repository.summaryFlow.value = AnalyticsSummary(scansStarted = 4, documentsSaved = 3)
        val viewModel = AnalyticsViewModel(repository)

        // summary is stateIn'd with SharingStarted.WhileSubscribed, so it only starts collecting
        // the repository's flow once something actually subscribes — reading .value alone never
        // triggers that. backgroundScope's collector is the subscriber, and it's auto-cancelled
        // when the test ends.
        backgroundScope.launch { viewModel.summary.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(4, viewModel.summary.value.scansStarted)
        assertEquals(3, viewModel.summary.value.documentsSaved)
    }

    @Test
    fun `clearAll delegates to the repository`() = runTest {
        val repository = FakeAnalyticsRepository()
        val viewModel = AnalyticsViewModel(repository)

        viewModel.clearAll()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(repository.clearAllCalled)
    }

    private class FakeAnalyticsRepository : AnalyticsRepository {
        val summaryFlow = MutableStateFlow(AnalyticsSummary())
        var clearAllCalled = false

        override suspend fun logEvent(type: AnalyticsEventType, detail: String?) = Unit
        override fun observeSummary(): Flow<AnalyticsSummary> = summaryFlow
        override suspend fun clearAll() {
            clearAllCalled = true
        }
    }
}
