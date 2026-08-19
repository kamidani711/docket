package com.docket.ui.screens.receipts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.docket.domain.model.MonthlySpend
import com.docket.domain.model.Receipt
import com.docket.domain.model.ReceiptFilter
import com.docket.ui.components.EmptyState
import com.docket.ui.components.PrimaryButton
import com.docket.ui.components.ReceiptRow
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.util.formatDisplayDate
import com.docket.ui.util.formatMoney
import com.docket.ui.util.formatMonthYear
import com.docket.ui.util.shareFiles
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptsScreen(
    onBack: () -> Unit,
    onOpenDocument: (Long) -> Unit,
    viewModel: ReceiptsViewModel = hiltViewModel()
) {
    val receipts by viewModel.receipts.collectAsStateWithLifecycle()
    val monthlySpend by viewModel.monthlySpend.collectAsStateWithLifecycle()
    val csvFile by viewModel.csvFile.collectAsStateWithLifecycle()
    val isExportingCsv by viewModel.isExportingCsv.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var merchantQuery by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf(DatePreset.ALL_TIME) }
    var minAmountText by remember { mutableStateOf("") }
    var maxAmountText by remember { mutableStateOf("") }

    LaunchedEffect(merchantQuery, selectedPreset, minAmountText, maxAmountText) {
        val (start, end) = dateRangeFor(selectedPreset)
        viewModel.updateFilter(
            ReceiptFilter(
                merchantQuery = merchantQuery.ifBlank { null },
                startDate = start,
                endDate = end,
                minAmountCents = minAmountText.toDoubleOrNull()?.let { (it * 100).toLong() },
                maxAmountCents = maxAmountText.toDoubleOrNull()?.let { (it * 100).toLong() }
            )
        )
    }

    // CSV export writes a file then hands it straight to the share sheet — there's no separate
    // "saved to Downloads" destination in this app, sharing IS the delivery mechanism.
    LaunchedEffect(csvFile) {
        val file = csvFile ?: return@LaunchedEffect
        shareFiles(context, listOf(file), "text/csv")
        viewModel.clearCsvFile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receipts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(DocketSpacing.space20),
            verticalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
        ) {
            if (monthlySpend.isNotEmpty()) {
                Text("Monthly spend", style = MaterialTheme.typography.titleLarge)
                Column(verticalArrangement = Arrangement.spacedBy(DocketSpacing.space4)) {
                    monthlySpend.forEach { spend -> MonthlySpendRow(spend) }
                }
                HorizontalDivider()
            }

            Text("Filter", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = merchantQuery,
                onValueChange = { merchantQuery = it },
                label = { Text("Merchant") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
                DatePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = selectedPreset == preset,
                        onClick = { selectedPreset = preset },
                        label = { Text(preset.label) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
                OutlinedTextField(
                    value = minAmountText,
                    onValueChange = { minAmountText = it },
                    label = { Text("Min amount") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxAmountText,
                    onValueChange = { maxAmountText = it },
                    label = { Text("Max amount") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            PrimaryButton(
                text = if (isExportingCsv) "Exporting…" else "Export to CSV",
                onClick = viewModel::exportCsv,
                loading = isExportingCsv,
                enabled = receipts.isNotEmpty() && !isExportingCsv,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            Text("Receipts (${receipts.size})", style = MaterialTheme.typography.titleLarge)
            if (receipts.isEmpty()) {
                EmptyState(
                    title = "No receipts match",
                    message = "Try adjusting the filters, or tag a scan as a receipt from its document page.",
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
                    receipts.forEach { receipt -> ReceiptRowItem(receipt, onClick = { onOpenDocument(receipt.documentId) }) }
                }
            }
        }
    }
}

@Composable
private fun MonthlySpendRow(spend: MonthlySpend) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(formatYearMonth(spend.yearMonth), style = MaterialTheme.typography.bodyLarge)
        Text(formatMoney(spend.totalCents, spend.currencyCode), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ReceiptRowItem(receipt: Receipt, onClick: () -> Unit) {
    ReceiptRow(
        merchant = receipt.merchant,
        amount = formatMoney(receipt.totalAmountCents, receipt.currencyCode),
        date = receipt.purchaseDate?.let { formatDate(it) } ?: "No date",
        onClick = onClick
    )
}

private enum class DatePreset(val label: String) {
    ALL_TIME("All time"),
    THIS_MONTH("This month"),
    LAST_MONTH("Last month"),
    LAST_3_MONTHS("Last 3 months")
}

private fun dateRangeFor(preset: DatePreset): Pair<Long?, Long?> {
    if (preset == DatePreset.ALL_TIME) return null to null
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    when (preset) {
        DatePreset.LAST_MONTH -> calendar.add(Calendar.MONTH, -1)
        DatePreset.LAST_3_MONTHS -> calendar.add(Calendar.MONTH, -3)
        else -> Unit
    }
    return calendar.timeInMillis to System.currentTimeMillis()
}

private fun formatYearMonth(yearMonth: String): String {
    val parts = yearMonth.split("-")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return yearMonth
    val month = parts.getOrNull(1)?.toIntOrNull() ?: return yearMonth
    val calendar = Calendar.getInstance()
    calendar.set(year, month - 1, 1)
    return formatMonthYear(calendar.timeInMillis)
}

private fun formatDate(epochMillis: Long): String = formatDisplayDate(epochMillis)
