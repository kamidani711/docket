package com.docket.domain.model

/**
 * Output of [com.docket.domain.repository.ReceiptParser] — a *suggestion*, never auto-saved.
 * Every field is nullable/best-effort on purpose: the parser is regex-and-heuristics over OCR
 * text, not a model, and it will be wrong sometimes. The UI always shows these in an editable
 * form before anything is persisted — see the brief: "a wrong value they can fix beats a
 * spinner that never resolves."
 *
 * Amounts are integer minor units (cents) — see [Receipt] for why.
 */
data class ParsedLineItem(
    val description: String,
    val amountCents: Long?
)

data class ParsedReceipt(
    val merchant: String?,
    val totalAmountCents: Long?,
    val currencyCode: String?,
    val purchaseDate: Long?,
    val lineItems: List<ParsedLineItem>,
    val paymentMethod: String?
)
