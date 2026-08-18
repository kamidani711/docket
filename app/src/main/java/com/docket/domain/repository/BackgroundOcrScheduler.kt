package com.docket.domain.repository

/** Schedules background OCR for a just-saved document — see `data/work/OcrScheduler.kt`
 *  (WorkManager) for the implementation. Kept behind an interface for the same domain/data
 *  reason as everything else in this file, not because WorkManager is likely to be swapped. */
interface BackgroundOcrScheduler {
    fun scheduleOcr(documentId: Long)
}
