package com.docket.domain.model

/** [yearMonth] is "YYYY-MM" — sortable as plain text, which is all the summary screen needs. */
data class MonthlySpend(
    val yearMonth: String,
    val totalCents: Long,
    val currencyCode: String?
)

/** All fields null/blank = no filter applied on that dimension. */
data class ReceiptFilter(
    val merchantQuery: String? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val minAmountCents: Long? = null,
    val maxAmountCents: Long? = null
)
