package com.docket.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.docket.data.local.DocketDatabase
import com.docket.domain.model.AnalyticsEventType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real Room, in-memory — same pattern as [com.docket.SearchBenchmarkTest]. Requires a device/
 * emulator (`./gradlew connectedAndroidTest`), same as every other `androidTest` in this project;
 * not runnable in this environment. See the chat write-up.
 */
@RunWith(AndroidJUnit4::class)
class AnalyticsRepositoryImplTest {

    private lateinit var db: DocketDatabase
    private lateinit var repository: AnalyticsRepositoryImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DocketDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AnalyticsRepositoryImpl(db.analyticsDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun summaryGroupsCountsByEventType() = runBlocking {
        repository.logEvent(AnalyticsEventType.SCAN_STARTED)
        repository.logEvent(AnalyticsEventType.SCAN_STARTED)
        repository.logEvent(AnalyticsEventType.DOCUMENT_SAVED)
        repository.logEvent(AnalyticsEventType.SESSION_CLEAN)
        repository.logEvent(AnalyticsEventType.SESSION_CRASHED)

        val summary = repository.observeSummary().first()

        assertEquals(2, summary.scansStarted)
        assertEquals(1, summary.documentsSaved)
        assertEquals(0, summary.exportsCompleted)
        assertEquals(1, summary.cleanSessions)
        assertEquals(1, summary.crashedSessions)
        assertEquals(2, summary.totalSessions)
        assertEquals(0.5f, summary.crashFreeSessionRate, 0.001f)
    }

    @Test
    fun featureUsageIsGroupedByDetailNotByTheSharedFeatureUsedType() = runBlocking {
        repository.logEvent(AnalyticsEventType.FEATURE_USED, "receipt_tagged")
        repository.logEvent(AnalyticsEventType.FEATURE_USED, "receipt_tagged")
        repository.logEvent(AnalyticsEventType.FEATURE_USED, "backup_created")

        val summary = repository.observeSummary().first()

        assertEquals(2, summary.featureUsage["receipt_tagged"])
        assertEquals(1, summary.featureUsage["backup_created"])
        // FEATURE_USED itself never shows up as a top-level count — only its details do.
        assertEquals(0, summary.scansStarted + summary.documentsSaved + summary.exportsCompleted)
    }

    @Test
    fun crashFreeRateIsFullWhenThereIsNoSessionHistoryYet() = runBlocking {
        val summary = repository.observeSummary().first()
        assertEquals(0, summary.totalSessions)
        assertEquals(1f, summary.crashFreeSessionRate, 0.001f)
    }

    @Test
    fun clearAllRemovesEverything() = runBlocking {
        repository.logEvent(AnalyticsEventType.SCAN_STARTED)
        repository.logEvent(AnalyticsEventType.FEATURE_USED, "warranty_set")

        repository.clearAll()
        val summary = repository.observeSummary().first()

        assertEquals(0, summary.scansStarted)
        assertTrue(summary.featureUsage.isEmpty())
    }
}
