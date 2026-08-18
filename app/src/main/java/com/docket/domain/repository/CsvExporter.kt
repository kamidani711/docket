package com.docket.domain.repository

import com.docket.domain.model.Receipt
import java.io.File

interface CsvExporter {
    suspend fun exportReceiptsToCsv(receipts: List<Receipt>, destination: File): File
}
