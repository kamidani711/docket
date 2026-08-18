package com.docket.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.docket.domain.usecase.PurgeDeletedDocumentsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Runs the Recently Deleted bin's 30-day purge. Scheduled as a daily periodic job from
 *  DocketApplication — see [WorkScheduling]. */
@HiltWorker
class PurgeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val purgeDeletedDocumentsUseCase: PurgeDeletedDocumentsUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        purgeDeletedDocumentsUseCase()
        Result.success()
    } catch (t: Throwable) {
        Result.retry()
    }
}
