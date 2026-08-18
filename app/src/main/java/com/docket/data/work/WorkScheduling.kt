package com.docket.data.work

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** One-shot setup call from DocketApplication.onCreate() — schedules the app's recurring
 *  background jobs. KEEP policy means calling this on every app start doesn't reschedule or
 *  duplicate an already-pending periodic job. */
@Singleton
class WorkScheduling @Inject constructor(private val workManager: WorkManager) {

    fun schedulePeriodicPurge() {
        val request = PeriodicWorkRequestBuilder<PurgeWorker>(1, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(PURGE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private companion object {
        const val PURGE_WORK_NAME = "recently_deleted_purge"
    }
}
