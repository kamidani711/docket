package com.docket.domain.repository

/** WorkManager-backed (`data/work/WarrantyReminderScheduler`): 30/7/0 days before [expiryDate],
 *  skipping any that have already passed. */
interface ReminderScheduler {
    fun scheduleWarrantyReminders(warrantyId: Long, itemName: String, documentId: Long, expiryDate: Long)
    fun cancelWarrantyReminders(warrantyId: Long)
}
