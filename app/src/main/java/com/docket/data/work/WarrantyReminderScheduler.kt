package com.docket.data.work

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.docket.domain.repository.ReminderScheduler
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class WarrantyReminderScheduler @Inject constructor(
    private val workManager: WorkManager
) : ReminderScheduler {

    override fun scheduleWarrantyReminders(warrantyId: Long, itemName: String, documentId: Long, expiryDate: Long) {
        val tag = tagFor(warrantyId)
        val now = System.currentTimeMillis()

        for (reminderType in WarrantyReminderWorker.ReminderType.entries) {
            val triggerAt = triggerTimeFor(expiryDate, reminderType)
            val delayMs = triggerAt - now
            if (delayMs <= 0) continue // already past — e.g. a short custom duration set late

            val request = OneTimeWorkRequestBuilder<WarrantyReminderWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(WarrantyReminderWorker.inputData(warrantyId, documentId, itemName, reminderType))
                .addTag(tag)
                .build()
            workManager.enqueue(request)
        }
    }

    override fun cancelWarrantyReminders(warrantyId: Long) {
        workManager.cancelAllWorkByTag(tagFor(warrantyId))
    }

    /** All three reminders fire at 9am local time on their target day, not at the receipt's
     *  original purchase timestamp — a fixed sensible time reads better than a notification
     *  that might land at 2am because that's what time the purchase happened to be scanned. */
    private fun triggerTimeFor(expiryDate: Long, reminderType: WarrantyReminderWorker.ReminderType): Long {
        val daysBefore = when (reminderType) {
            WarrantyReminderWorker.ReminderType.THIRTY_DAYS -> 30
            WarrantyReminderWorker.ReminderType.SEVEN_DAYS -> 7
            WarrantyReminderWorker.ReminderType.DAY_OF -> 0
        }
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = expiryDate
        calendar.add(Calendar.DAY_OF_YEAR, -daysBefore)
        calendar.set(Calendar.HOUR_OF_DAY, REMINDER_HOUR_OF_DAY)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun tagFor(warrantyId: Long): String = "warranty_reminders_$warrantyId"

    private companion object {
        const val REMINDER_HOUR_OF_DAY = 9
    }
}
