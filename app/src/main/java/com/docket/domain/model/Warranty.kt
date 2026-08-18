package com.docket.domain.model

/** [documentId] is the linked receipt document — reminder notifications deep-link straight
 *  back to it (see `WarrantyReminderWorker`). */
data class Warranty(
    val id: Long,
    val documentId: Long,
    val itemName: String,
    val purchaseDate: Long,
    val expiryDate: Long,
    val durationMonths: Int,
    val createdAt: Long
)
