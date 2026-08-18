package com.docket.data.csv

import com.docket.domain.model.Receipt
import com.docket.domain.repository.CsvExporter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One row per receipt (not per line item) — the standard shape for importing into a
 *  spreadsheet or budget tool, which is what "export the whole set to CSV" is for. */
class CsvReceiptExporter @Inject constructor() : CsvExporter {

    override suspend fun exportReceiptsToCsv(receipts: List<Receipt>, destination: File): File =
        withContext(Dispatchers.IO) {
            destination.parentFile?.mkdirs()
            destination.bufferedWriter().use { writer ->
                writer.appendLine(HEADER)
                receipts.forEach { receipt -> writer.appendLine(receipt.toCsvRow()) }
            }
            destination
        }

    private fun Receipt.toCsvRow(): String {
        val dateText = purchaseDate?.let { DATE_FORMAT.format(Date(it)) } ?: ""
        // Locale.US explicitly — this becomes a field in a comma-delimited file, so a
        // locale that formats decimals with a comma (many do) would silently corrupt the row.
        val amountText = totalAmountCents?.let { cents -> String.format(Locale.US, "%.2f", cents / 100.0) } ?: ""
        return listOf(merchant, dateText, amountText, currencyCode ?: "", paymentMethod ?: "")
            .joinToString(",") { field -> csvEscape(field) }
    }

    private fun csvEscape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' }) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }

    private companion object {
        const val HEADER = "Merchant,Date,Total,Currency,Payment Method"
        val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
