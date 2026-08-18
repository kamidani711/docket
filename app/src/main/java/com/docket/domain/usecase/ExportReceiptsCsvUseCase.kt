package com.docket.domain.usecase

import android.content.Context
import com.docket.domain.model.AnalyticsEventType
import com.docket.domain.model.ReceiptFilter
import com.docket.domain.repository.AnalyticsRepository
import com.docket.domain.repository.CsvExporter
import com.docket.domain.repository.ReceiptRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class ExportReceiptsCsvUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    private val csvExporter: CsvExporter,
    private val analyticsRepository: AnalyticsRepository,
    @ApplicationContext private val appContext: Context
) {
    suspend operator fun invoke(filter: ReceiptFilter): File {
        val receipts = receiptRepository.observeAllReceipts(filter).first()
        val exportDir = File(appContext.filesDir, "exports/${UUID.randomUUID()}").apply { mkdirs() }
        val file = csvExporter.exportReceiptsToCsv(receipts, File(exportDir, "receipts.csv"))
        analyticsRepository.logEvent(AnalyticsEventType.FEATURE_USED, "csv_exported")
        return file
    }
}
