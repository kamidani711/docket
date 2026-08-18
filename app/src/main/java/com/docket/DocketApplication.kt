package com.docket

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.docket.data.analytics.CrashSessionTracker
import com.docket.data.backup.PendingRestoreApplier
import com.docket.data.notification.NotificationChannels
import com.docket.data.work.WorkScheduling
import com.docket.domain.model.AnalyticsEventType
import com.docket.domain.repository.AnalyticsRepository
import com.docket.domain.repository.BillingRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application entry point. Annotated for Hilt so the dependency graph is available
 * everywhere below it (Activities, ViewModels, WorkManager workers, etc.).
 *
 * [Configuration.Provider] hands WorkManager a [HiltWorkerFactory] so [OcrWorker
 * ][com.docket.data.work.OcrWorker] and [WarrantyReminderWorker
 * ][com.docket.data.work.WarrantyReminderWorker] can get their dependencies injected like
 * anything else — WorkManager's own auto-initialization detects this automatically, no
 * manifest changes needed.
 *
 * [attachBaseContext] applies a staged backup restore, if one is pending, before Room or
 * anything else gets a chance to open the database — see [PendingRestoreApplier].
 *
 * Injecting [billingRepository] here (even though nothing reads it afterward) is what makes
 * "DataStore unlock state verified against Play on start" actually happen — Hilt constructs
 * the singleton the moment this field is injected, and its `init` block is what connects to
 * Play and reconciles the stored premium flag. See BillingRepositoryImpl.
 *
 * [analyticsScope] exists only because Application has no natural coroutine scope of its own
 * (unlike a ViewModel) — it's deliberately never cancelled, which is fine since this object
 * lives for the whole process anyway. See [CrashSessionTracker] for the crash-free-session
 * heuristic this class's [onCreate]/[MainActivity.onStop] pair implements.
 */
@HiltAndroidApp
class DocketApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var workScheduling: WorkScheduling

    @Inject
    lateinit var billingRepository: BillingRepository

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    private val analyticsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        PendingRestoreApplier.applyIfPending(this)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        workScheduling.schedulePeriodicPurge()

        val crashed = CrashSessionTracker.wasPreviousSessionCrashed(this)
        CrashSessionTracker.markSessionActive(this)
        analyticsScope.launch {
            analyticsRepository.logEvent(if (crashed) AnalyticsEventType.SESSION_CRASHED else AnalyticsEventType.SESSION_CLEAN)
        }
    }
}
