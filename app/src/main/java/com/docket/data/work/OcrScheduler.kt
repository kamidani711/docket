package com.docket.data.work

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.docket.domain.repository.BackgroundOcrScheduler
import javax.inject.Inject

class OcrScheduler @Inject constructor(
    private val workManager: WorkManager
) : BackgroundOcrScheduler {

    override fun scheduleOcr(documentId: Long) {
        val request = OneTimeWorkRequestBuilder<OcrWorker>()
            .setInputData(OcrWorker.inputData(documentId))
            .build()
        workManager.enqueue(request)
    }
}
