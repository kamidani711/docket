package com.docket.domain.usecase

import com.docket.domain.repository.ReminderScheduler
import com.docket.domain.repository.WarrantyRepository
import javax.inject.Inject

class DeleteWarrantyUseCase @Inject constructor(
    private val warrantyRepository: WarrantyRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(warrantyId: Long) {
        reminderScheduler.cancelWarrantyReminders(warrantyId)
        warrantyRepository.deleteWarranty(warrantyId)
    }
}
