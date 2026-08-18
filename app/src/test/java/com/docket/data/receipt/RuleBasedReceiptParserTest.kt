package com.docket.data.receipt

import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-Kotlin unit tests — [RuleBasedReceiptParser] has no Android framework dependency, so this
 * runs as a plain JVM test (`./gradlew test`), no device/emulator needed.
 *
 * IMPORTANT — what this file is NOT: the brief asked for accuracy measured against "20 real
 * receipt images from different countries." This environment has no camera, no OCR runtime, and
 * no way to source real receipt photos, so that specific requirement was never met — there is
 * no dataset and no accuracy number to report honestly here, only that gap itself. What follows
 * instead is hand-written OCR-shaped text modeled on real receipt layouts from several
 * currencies/locales, which exercises the same code paths but is not a substitute for the real
 * thing: real OCR output has misreads, skew, and noise this text doesn't.
 */
class RuleBasedReceiptParserTest {

    private val parser = RuleBasedReceiptParser()

    // ---- Total ----

    @Test
    fun `total line wins over largest line item amount`() {
        val text = """
            Corner Store
            Milk                     4.50
            Bread                    2.25
            Total                   99.99
        """.trimIndent()
        assertEquals(9999L, parser.parse(text).totalAmountCents)
    }

    @Test
    fun `subtotal line is excluded even with a hyphen or space`() {
        val text = """
            Store
            Sub-total               10.00
            Sub Total               10.00
            Subtotal                10.00
            Total                   12.34
        """.trimIndent()
        assertEquals(1234L, parser.parse(text).totalAmountCents)
    }

    @Test
    fun `falls back to the largest amount when no total line exists`() {
        val text = """
            Store
            Milk                     4.50
            Bread                    2.25
        """.trimIndent()
        assertEquals(450L, parser.parse(text).totalAmountCents)
    }

    @Test
    fun `no amounts anywhere yields a null total, not a crash`() {
        assertNull(parser.parse("Just some text\nwith no numbers at all").totalAmountCents)
    }

    @Test
    fun `amount with thousands separator parses correctly`() {
        val text = "Store\nTotal                1,234.56"
        assertEquals(123456L, parser.parse(text).totalAmountCents)
    }

    @Test
    fun `comma-decimal european format is not misread as a huge amount`() {
        // Documented limitation (see the class doc): "12,34" (EUR-style comma-decimal) doesn't
        // match AMOUNT_REGEX at all, so it's correctly ignored rather than misparsed as 1234.00.
        val text = "Store\nTotal                12,34"
        assertNull(parser.parse(text).totalAmountCents)
    }

    // ---- Currency ----

    @Test
    fun `dollar sign detected as USD`() {
        assertEquals("USD", parser.parse("Store\nTotal \$12.34").currencyCode)
    }

    @Test
    fun `euro sign detected as EUR`() {
        assertEquals("EUR", parser.parse("Store\nTotal €12.34").currencyCode)
    }

    @Test
    fun `pound sign detected as GBP`() {
        assertEquals("GBP", parser.parse("Store\nTotal £12.34").currencyCode)
    }

    @Test
    fun `rupee sign detected as INR`() {
        assertEquals("INR", parser.parse("Store\nTotal ₹1234.00").currencyCode)
    }

    @Test
    fun `no currency symbol yields null, not a guess`() {
        assertNull(parser.parse("Store\nTotal 12.34").currencyCode)
    }

    // ---- Date ----

    @Test
    fun `iso date format parses`() {
        assertEquals(expectedMillis("yyyy-MM-dd", "2026-03-05"), parser.parse("Date: 2026-03-05").purchaseDate)
    }

    @Test
    fun `day-first slash date is preferred over month-first for an ambiguous value`() {
        // 13 can't be a month, so this also confirms day-first parsing directly for unambiguous
        // cases (13/01/2026 is definitely 13 Jan, not month 13).
        assertEquals(expectedMillis("dd/MM/yyyy", "13/01/2026"), parser.parse("Date: 13/01/2026").purchaseDate)
    }

    @Test
    fun `month-name date format parses`() {
        assertEquals(expectedMillis("MMM d, yyyy", "Aug 16, 2026"), parser.parse("Date: Aug 16, 2026").purchaseDate)
    }

    @Test
    fun `day-then-month-name date format parses`() {
        assertEquals(expectedMillis("d MMMM yyyy", "16 August 2026"), parser.parse("Date: 16 August 2026").purchaseDate)
    }

    @Test
    fun `no recognizable date yields null`() {
        assertNull(parser.parse("Store\nNo date here").purchaseDate)
    }

    // ---- Merchant ----

    @Test
    fun `first substantial line is treated as the merchant`() {
        assertEquals("Corner Grocery", parser.parse("Corner Grocery\nTotal 1.00").merchant)
    }

    @Test
    fun `boilerplate first lines are skipped in favor of the real merchant name`() {
        val text = "Receipt\nThank You\nCorner Grocery\nTotal 1.00"
        assertEquals("Corner Grocery", parser.parse(text).merchant)
    }

    @Test
    fun `mostly-numeric lines are skipped as merchant candidates`() {
        val text = "123456789\nCorner Grocery\nTotal 1.00"
        assertEquals("Corner Grocery", parser.parse(text).merchant)
    }

    @Test
    fun `no substantial line in the first few rows yields a null merchant`() {
        assertNull(parser.parse("111\n222\n333\n444\n555\nTotal 1.00").merchant)
    }

    // ---- Payment method ----

    @Test
    fun `visa keyword detected case-insensitively`() {
        assertEquals("Visa", parser.parse("Store\npaid by visa\nTotal 1.00").paymentMethod)
    }

    @Test
    fun `upi keyword detected`() {
        assertEquals("UPI", parser.parse("Store\nPAID VIA UPI\nTotal 1.00").paymentMethod)
    }

    @Test
    fun `no payment keyword yields null`() {
        assertNull(parser.parse("Store\nTotal 1.00").paymentMethod)
    }

    // ---- Line items ----

    @Test
    fun `a description-then-amount line is captured as a line item`() {
        val items = parser.parse("Store\nCoffee                    3.50\nTotal    3.50").lineItems
        assertEquals(1, items.size)
        assertEquals("Coffee", items.first().description)
        assertEquals(350L, items.first().amountCents)
    }

    @Test
    fun `summary lines like total and tax are excluded from line items`() {
        val text = """
            Store
            Coffee                    3.50
            Tax                       0.30
            Total                     3.80
        """.trimIndent()
        val items = parser.parse(text).lineItems
        assertEquals(1, items.size)
        assertEquals("Coffee", items.first().description)
    }

    @Test
    fun `too-short a description is not treated as a line item`() {
        // "a" alone, before an amount, is below MIN_ITEM_DESCRIPTION_LENGTH.
        val items = parser.parse("Store\na  1.00\nTotal 1.00").lineItems
        assertTrue(items.none { it.description == "a" })
    }

    // ---- End-to-end shape (synthetic, not real OCR — see class doc) ----

    @Test
    fun `synthetic US-style receipt parses all fields`() {
        val text = """
            Corner Grocery
            123 Main St
            Milk                      4.50
            Bread                     2.25
            Subtotal                  6.75
            Tax                       0.54
            Total                     7.29
            VISA
            08/16/2026
        """.trimIndent()
        val result = parser.parse(text)
        assertEquals("Corner Grocery", result.merchant)
        assertEquals(729L, result.totalAmountCents)
        assertEquals("Visa", result.paymentMethod)
        assertEquals(expectedMillis("MM/dd/yyyy", "08/16/2026"), result.purchaseDate)
    }

    @Test
    fun `synthetic Indian-style receipt parses currency and day-first date`() {
        val text = """
            Sharma General Store
            Rice 1kg                  85.00
            Total                    ₹85.00
            UPI
            16/08/2026
        """.trimIndent()
        val result = parser.parse(text)
        assertEquals("INR", result.currencyCode)
        assertEquals("UPI", result.paymentMethod)
        assertEquals(expectedMillis("dd/MM/yyyy", "16/08/2026"), result.purchaseDate)
    }

    private fun expectedMillis(pattern: String, value: String): Long =
        SimpleDateFormat(pattern, Locale.ENGLISH).apply { isLenient = false }.parse(value)!!.time
}
