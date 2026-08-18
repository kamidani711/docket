package com.docket.ui.screens.documentdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.docket.domain.model.ParsedLineItem
import com.docket.domain.model.ParsedReceipt
import com.docket.ui.components.PrimaryButton
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.util.formatMoney
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Every field the parser filled in, editable — per the brief: "a wrong value they can fix
 * beats a spinner that never resolves." The date field uses an explicit `YYYY-MM-DD` format
 * rather than trying to match whatever ambiguous format the parser guessed, so a correction is
 * always unambiguous even if the original guess wasn't.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptEditSheet(
    draft: ParsedReceipt,
    onSave: (ParsedReceipt) -> Unit,
    onDismiss: () -> Unit
) {
    var merchant by remember(draft) { mutableStateOf(draft.merchant ?: "") }
    var totalAmountText by remember(draft) { mutableStateOf(centsToText(draft.totalAmountCents)) }
    var currencyCode by remember(draft) { mutableStateOf(draft.currencyCode ?: "") }
    var dateText by remember(draft) { mutableStateOf(draft.purchaseDate?.let { DATE_FORMAT.format(Date(it)) } ?: "") }
    var paymentMethod by remember(draft) { mutableStateOf(draft.paymentMethod ?: "") }
    var lineItems by remember(draft) { mutableStateOf(draft.lineItems) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = DocketSpacing.space24)
                .padding(bottom = DocketSpacing.space32)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
        ) {
            Text("Receipt details", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Parsed automatically from the scanned text — check these before saving.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
                OutlinedTextField(
                    value = totalAmountText,
                    onValueChange = { totalAmountText = it },
                    label = { Text("Total") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = currencyCode,
                    onValueChange = { currencyCode = it.uppercase(Locale.getDefault()).take(3) },
                    label = { Text("Currency") },
                    singleLine = true,
                    modifier = Modifier.width(100.dp)
                )
            }

            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = { Text("Date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = paymentMethod,
                onValueChange = { paymentMethod = it },
                label = { Text("Payment method") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (lineItems.isNotEmpty()) {
                Text("Line items", style = MaterialTheme.typography.titleMedium)
                lineItems.forEachIndexed { index, item ->
                    LineItemRow(
                        item = item,
                        currencyCode = currencyCode.ifBlank { null },
                        onRemove = { lineItems = lineItems.toMutableList().apply { removeAt(index) } }
                    )
                }
            }

            PrimaryButton(
                text = "Save receipt",
                onClick = {
                    onSave(
                        ParsedReceipt(
                            merchant = merchant.trim().ifBlank { null },
                            totalAmountCents = textToCents(totalAmountText),
                            currencyCode = currencyCode.trim().ifBlank { null },
                            purchaseDate = parseEditedDate(dateText),
                            lineItems = lineItems,
                            paymentMethod = paymentMethod.trim().ifBlank { null }
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun LineItemRow(item: ParsedLineItem, currencyCode: String?, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)
    ) {
        Text(
            text = item.description,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(formatMoney(item.amountCents, currencyCode), style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, contentDescription = "Remove item")
        }
    }
}

private fun centsToText(cents: Long?): String =
    if (cents == null) "" else String.format(Locale.US, "%.2f", cents / 100.0)

private fun textToCents(text: String): Long? {
    val amount = text.trim().toDoubleOrNull() ?: return null
    return Math.round(amount * 100)
}

private fun parseEditedDate(text: String): Long? =
    runCatching { DATE_FORMAT.parse(text.trim())?.time }.getOrNull()

private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
