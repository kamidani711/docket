package com.docket.domain.model

/**
 * A saved, user-confirmed receipt tied to a scanned [Document]. Amounts are stored as integer
 * minor units (cents) rather than `Double`/`Float` — the standard way to avoid floating-point
 * rounding drift in money math, at the small cost of treating zero-decimal currencies (JPY, ...)
 * as if they had cents too; see `formatMoney` for where that gets undone for display.
 */
data class ReceiptLineItem(
    val id: Long,
    val description: String,
    val amountCents: Long?,
    val orderIndex: Int
)

data class Receipt(
    val id: Long,
    val documentId: Long,
    val merchant: String,
    val totalAmountCents: Long?,
    val currencyCode: String?,
    val purchaseDate: Long?,
    val paymentMethod: String?,
    val lineItems: List<ReceiptLineItem>,
    val createdAt: Long
)
