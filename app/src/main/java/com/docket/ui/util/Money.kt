package com.docket.ui.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** [amountCents]/[currencyCode] mirror how money is stored everywhere in this app — see the
 *  doc on [com.docket.domain.model.Receipt] for why cents-as-Long instead of Double. */
fun formatMoney(amountCents: Long?, currencyCode: String?): String {
    if (amountCents == null) return "—"
    val amount = amountCents / 100.0
    if (currencyCode != null) {
        val formatted = runCatching {
            NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(currencyCode) }
                .format(amount)
        }.getOrNull()
        if (formatted != null) return formatted
    }
    return String.format(Locale.getDefault(), "%.2f", amount)
}
