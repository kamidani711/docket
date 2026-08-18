package com.docket.domain.model

/** The four presets plus an arbitrary custom length, all expressed in months so expiry is a
 *  simple calendar-month addition to the purchase date (see `SetWarrantyUseCase`). */
sealed class WarrantyDuration(val months: Int, val label: String) {
    data object SixMonths : WarrantyDuration(6, "6 months")
    data object OneYear : WarrantyDuration(12, "1 year")
    data object TwoYears : WarrantyDuration(24, "2 years")
    data object ThreeYears : WarrantyDuration(36, "3 years")
    data class Custom(val customMonths: Int) : WarrantyDuration(
        months = customMonths,
        label = if (customMonths == 1) "1 month" else "$customMonths months"
    )

    companion object {
        val presets: List<WarrantyDuration> = listOf(SixMonths, OneYear, TwoYears, ThreeYears)
    }
}
