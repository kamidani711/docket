package com.docket.ui.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Locale-aware "medium" date display — e.g. "Aug 16, 2026" in en-US, "16 ago 2026" in es,
 * correct digit shaping and word order in ar/ur/hi. Deliberately NOT a fixed `SimpleDateFormat`
 * pattern string like `"MMM d, yyyy"`: a fixed pattern hardcodes US month-day-year order into
 * every locale, translating the month name but not the layout. [DateFormat.getDateInstance]
 * adapts both.
 *
 * Not used for machine-readable dates (CSV export rows, editable `yyyy-MM-dd` text fields, OCR
 * date parsing) — those are intentionally locale-independent; see the comments at their call
 * sites.
 */
fun formatDisplayDate(epochMillis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date(epochMillis))

fun formatDisplayDateTime(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
        .format(Date(epochMillis))

/**
 * Month + year only, for group headers (the receipt summary's monthly groups). `java.text
 * .DateFormat` has no built-in "month and year" style, so this still goes through a pattern
 * string — the month name and digits are locale-correct, only the Month-before-Year ordering
 * isn't locale-adaptive. A narrower compromise than [formatDisplayDate], not a full fix.
 */
fun formatMonthYear(epochMillis: Long): String =
    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(epochMillis))
