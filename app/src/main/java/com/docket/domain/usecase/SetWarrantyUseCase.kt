package com.docket.domain.usecase

import com.docket.domain.model.WarrantyDuration
import com.docket.domain.repository.ReminderScheduler
import com.docket.domain.repository.WarrantyRepository
import java.util.Calendar
import javax.inject.Inject

class SetWarrantyUseCase @Inject constructor(
    private val warrantyRepository: WarrantyRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(
        documentId: Long,
        itemName: String,
        purchaseDate: Long,
        duration: WarrantyDuration
    ): Long {
        val expiryDate = addMonths(purchaseDate, duration.months)

        // Re-setting an existing warranty (e.g. correcting the duration): cancel its old
        // reminders first. saveWarranty replaces the row with a new id, so anything scheduled
        // under the old id would otherwise never get cancelled.
        warrantyRepository.getWarrantyForDocument(documentId)?.let { existing ->
            reminderScheduler.cancelWarrantyReminders(existing.id)
        }

        val warrantyId = warrantyRepository.saveWarranty(
            documentId = documentId,
            itemName = itemName,
            purchaseDate = purchaseDate,
            expiryDate = expiryDate,
            durationMonths = duration.months
        )
        reminderScheduler.scheduleWarrantyReminders(warrantyId, itemName, documentId, expiryDate)
        return warrantyId
    }

    private fun addMonths(epochMillis: Long, months: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = epochMillis
        calendar.add(Calendar.MONTH, months)
        return calendar.timeInMillis
    }
}
