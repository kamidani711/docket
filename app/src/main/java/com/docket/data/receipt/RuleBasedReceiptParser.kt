package com.docket.data.receipt

import com.docket.domain.model.ParsedLineItem
import com.docket.domain.model.ParsedReceipt
import com.docket.domain.repository.ReceiptParser
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

/**
 * Regex + heuristics over OCR text, entirely offline — no network call, no ML model, just
 * pattern matching. This WILL get things wrong; every field is a best-effort suggestion the UI
 * always shows in an editable form before saving (per the brief). Known, deliberate limitations
 * are called out at each function below rather than hidden — worth reading before trusting a
 * field this parser fills in.
 */
class RuleBasedReceiptParser @Inject constructor() : ReceiptParser {

    override fun parse(ocrText: String): ParsedReceipt {
        val lines = ocrText.lines()
        return ParsedReceipt(
            merchant = findMerchant(lines),
            totalAmountCents = findTotalCents(lines),
            currencyCode = detectCurrency(ocrText),
            purchaseDate = findPurchaseDateMillis(ocrText),
            lineItems = findLineItems(lines),
            paymentMethod = findPaymentMethod(ocrText)
        )
    }

    // ---- Merchant ----

    private fun findMerchant(lines: List<String>): String? {
        for (line in lines.take(MERCHANT_SEARCH_LINE_COUNT)) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.length > MAX_MERCHANT_LENGTH) continue
            if (!trimmed.any { it.isLetter() }) continue
            if (trimmed.lowercase(Locale.getDefault()) in BOILERPLATE_FIRST_LINES) continue
            val digitCount = trimmed.count { it.isDigit() }
            val letterCount = trimmed.count { it.isLetter() }
            if (digitCount > letterCount) continue // mostly-numeric line (a phone/receipt number)
            return trimmed
        }
        return null
    }

    // ---- Total ----

    /**
     * Prefers amounts on lines containing "total" (but not "subtotal") — receipts usually print
     * subtotal, tax, and total as separate lines, and the grand total is what "total" without
     * qualification means. If no such line exists, falls back to the single largest amount
     * anywhere on the receipt (weak — a line item bigger than the total would win — but a
     * documented fallback beats returning nothing).
     */
    private fun findTotalCents(lines: List<String>): Long? {
        val totalLineAmounts = lines
            .filter { line -> TOTAL_LINE_REGEX.containsMatchIn(line) && !SUBTOTAL_REGEX.containsMatchIn(line) }
            .flatMap { line -> findAllAmountsCents(line) }
        if (totalLineAmounts.isNotEmpty()) return totalLineAmounts.max()

        return lines.flatMap { findAllAmountsCents(it) }.maxOrNull()
    }

    // ---- Currency ----

    /** Symbol-based only — "$" defaults to USD, which is wrong for CAD/AUD/etc. receipts.
     *  No symbol found = null; the edit form lets the user set it directly. */
    private fun detectCurrency(text: String): String? =
        CURRENCY_SYMBOLS.entries.firstOrNull { (symbol, _) -> text.contains(symbol) }?.value

    // ---- Date ----

    /**
     * Numeric dates (`12/08/2026`) are inherently ambiguous without knowing the receipt's
     * locale. This tries day-first (`dd/MM/yyyy`) before month-first — matching this app's
     * actual target markets (India, Pakistan, Indonesia, Nigeria, Brazil, MENA all read dates
     * day-first), NOT US convention. That means US receipts with an ambiguous date will
     * sometimes parse wrong — again, the edit form is the safety net, not a guess we hide.
     */
    private fun findPurchaseDateMillis(text: String): Long? {
        val candidate = DATE_CANDIDATE_REGEX.find(text)?.value ?: return null
        for (pattern in DATE_FORMATS) {
            val format = SimpleDateFormat(pattern, Locale.ENGLISH).apply { isLenient = false }
            val parsed = runCatching { format.parse(candidate) }.getOrNull()
            if (parsed != null) return parsed.time
        }
        return null
    }

    // ---- Payment method ----

    private fun findPaymentMethod(text: String): String? {
        val upper = text.uppercase(Locale.getDefault())
        return PAYMENT_KEYWORDS.firstOrNull { (keyword, _) -> upper.contains(keyword) }?.second
    }

    // ---- Line items ----

    /**
     * "Where the layout allows": a line is treated as an item when it ends in something that
     * looks like a price — the standard receipt layout of `description .......... $price`.
     * Lines matching summary keywords (total/tax/change/...) are excluded so the totals row
     * doesn't also show up as a line item. This misses items whose price wraps to the next OCR
     * line, and can't tell a genuine line item from any other trailing-amount line (e.g. a
     * "loyalty points: 4.50" line) — it's a layout heuristic, not real receipt understanding.
     */
    private fun findLineItems(lines: List<String>): List<ParsedLineItem> =
        lines.mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || SUMMARY_LINE_KEYWORDS.containsMatchIn(trimmed)) return@mapNotNull null

            val amountMatch = AMOUNT_REGEX.findAll(trimmed).lastOrNull() ?: return@mapNotNull null
            val trailingCharsAfterAmount = trimmed.length - amountMatch.range.last - 1
            if (trailingCharsAfterAmount > MAX_TRAILING_CHARS_AFTER_AMOUNT) return@mapNotNull null

            val description = trimmed.substring(0, amountMatch.range.first).trim(' ', '-', '.', ':')
            if (description.length < MIN_ITEM_DESCRIPTION_LENGTH) return@mapNotNull null

            val amountCents = parseAmountToCents(amountMatch.value) ?: return@mapNotNull null
            ParsedLineItem(description = description, amountCents = amountCents)
        }

    // ---- Shared amount parsing ----

    /** US/UK convention only: comma thousands separator, period decimal, exactly 2 decimal
     *  digits (`1,234.56`). Comma-decimal locales (many EU receipts print `12,34`) are not
     *  matched — a real limitation, not an oversight; see the class doc. */
    private fun findAllAmountsCents(text: String): List<Long> =
        AMOUNT_REGEX.findAll(text).mapNotNull { parseAmountToCents(it.value) }.toList()

    private fun parseAmountToCents(matchText: String): Long? {
        val cleaned = matchText.replace(",", "")
        val parts = cleaned.split(".")
        if (parts.size != 2 || parts[1].length != 2) return null
        val whole = parts[0].toLongOrNull() ?: return null
        val cents = parts[1].toLongOrNull() ?: return null
        return whole * 100 + cents
    }

    private companion object {
        const val MERCHANT_SEARCH_LINE_COUNT = 5
        const val MAX_MERCHANT_LENGTH = 40
        const val MAX_TRAILING_CHARS_AFTER_AMOUNT = 3
        const val MIN_ITEM_DESCRIPTION_LENGTH = 2

        val BOILERPLATE_FIRST_LINES = setOf(
            "receipt", "invoice", "tax invoice", "official receipt", "thank you", "welcome"
        )

        val AMOUNT_REGEX = Regex("""\d{1,3}(?:,\d{3})*\.\d{2}""")
        val TOTAL_LINE_REGEX = Regex("""\btotal\b""", RegexOption.IGNORE_CASE)
        val SUBTOTAL_REGEX = Regex("""sub[\s-]?total""", RegexOption.IGNORE_CASE)
        val SUMMARY_LINE_KEYWORDS = Regex(
            """\b(total|subtotal|sub-total|tax|vat|gst|change|cash|card|balance|due|tender|amount)\b""",
            RegexOption.IGNORE_CASE
        )

        val CURRENCY_SYMBOLS = mapOf(
            '$' to "USD",
            '€' to "EUR",
            '£' to "GBP",
            '¥' to "JPY",
            '₹' to "INR"
        )

        val PAYMENT_KEYWORDS = listOf(
            "AMERICAN EXPRESS" to "American Express",
            "MASTERCARD" to "Mastercard",
            "DISCOVER" to "Discover",
            "PAYPAL" to "PayPal",
            "APPLE PAY" to "Apple Pay",
            "GOOGLE PAY" to "Google Pay",
            "VISA" to "Visa",
            "AMEX" to "American Express",
            "DEBIT" to "Debit card",
            "CREDIT" to "Credit card",
            "UPI" to "UPI",
            "CASH" to "Cash"
        )

        val DATE_CANDIDATE_REGEX = Regex(
            """\b\d{4}-\d{1,2}-\d{1,2}\b""" +
                """|\b\d{1,2}[/.-]\d{1,2}[/.-]\d{2,4}\b""" +
                """|\b[A-Za-z]{3,9}\.?\s+\d{1,2},?\s+\d{4}\b""" +
                """|\b\d{1,2}\s+[A-Za-z]{3,9}\.?,?\s+\d{4}\b"""
        )

        // Order matters: tried top to bottom, first successful parse wins.
        val DATE_FORMATS = listOf(
            "yyyy-MM-dd",
            "dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy",
            "MM/dd/yyyy", "MM-dd-yyyy",
            "dd/MM/yy", "MM/dd/yy",
            "MMM d, yyyy", "MMMM d, yyyy",
            "d MMM yyyy", "d MMMM yyyy"
        )
    }
}
