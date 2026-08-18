package com.docket.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.docket.domain.usecase.RunDocumentOcrUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs OCR for one document, off the main thread and outside the app's own process lifetime if
 * needed (WorkManager survives process death) — the "run in the background after save, not
 * blocking the user" half of the OCR requirement. Enqueued by [OcrScheduler], never by UI code.
 */
@HiltWorker
class OcrWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val runDocumentOcrUseCase: RunDocumentOcrUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val documentId = inputData.getLong(KEY_DOCUMENT_ID, -1L)
        if (documentId <= 0L) return Result.failure()

        return try {
            runDocumentOcrUseCase(documentId)
            Result.success()
        } catch (t: Throwable) {
            // OCR is an enhancement layered on an already-saved document, not the save itself —
            // retry a couple of times for transient issues, then give up quietly rather than
            // surfacing a failure notification for a background nicety.
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_DOCUMENT_ID = "documentId"
        private const val MAX_ATTEMPTS = 3

        fun inputData(documentId: Long): Data = workDataOf(KEY_DOCUMENT_ID to documentId)
    }
}
