package com.docket.domain.repository

import com.docket.domain.model.ParsedReceipt

/** Pure, offline, rules-based (regex + heuristics) — see `data/receipt/RuleBasedReceiptParser`.
 *  No suspend: there's no I/O here, just string processing. */
interface ReceiptParser {
    fun parse(ocrText: String): ParsedReceipt
}
