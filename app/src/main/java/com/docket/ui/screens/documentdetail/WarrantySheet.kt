package com.docket.ui.screens.documentdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.docket.domain.model.WarrantyDuration
import com.docket.ui.components.PrimaryButton
import com.docket.ui.theme.DocketSpacing
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** [suggestedItemName] pre-fills from the document title (often the OCR'd first line — a
 *  reasonable starting guess for a warranty card scan) but is always editable. Purchase date
 *  defaults to today rather than trying to reuse a receipt's parsed date, since a warranty can
 *  be set on a document with no receipt tagged at all. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarrantySheet(
    suggestedItemName: String,
    onSave: (itemName: String, purchaseDate: Long, duration: WarrantyDuration) -> Unit,
    onDismiss: () -> Unit
) {
    var itemName by remember { mutableStateOf(suggestedItemName) }
    var purchaseDateText by remember { mutableStateOf(DATE_FORMAT.format(Date())) }
    var selectedDuration by remember { mutableStateOf<WarrantyDuration>(WarrantyDuration.OneYear) }
    var customMonthsText by remember { mutableStateOf("") }

    val isCustomSelected = selectedDuration is WarrantyDuration.Custom

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = DocketSpacing.space24)
                .padding(bottom = DocketSpacing.space32),
            verticalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
        ) {
            Text("Set a warranty", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = { Text("Item name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = purchaseDateText,
                onValueChange = { purchaseDateText = it },
                label = { Text("Purchase date (YYYY-MM-DD)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Warranty length", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
                WarrantyDuration.presets.forEach { preset ->
                    FilterChip(
                        selected = selectedDuration == preset,
                        onClick = { selectedDuration = preset },
                        label = { Text(preset.label) }
                    )
                }
                FilterChip(
                    selected = isCustomSelected,
                    onClick = { selectedDuration = WarrantyDuration.Custom(customMonthsText.toIntOrNull() ?: 1) },
                    label = { Text("Custom") }
                )
            }
            if (isCustomSelected) {
                OutlinedTextField(
                    value = customMonthsText,
                    onValueChange = { text ->
                        customMonthsText = text
                        text.toIntOrNull()?.let { months -> selectedDuration = WarrantyDuration.Custom(months) }
                    },
                    label = { Text("Months") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            PrimaryButton(
                text = "Set warranty",
                onClick = {
                    val purchaseDate = runCatching { DATE_FORMAT.parse(purchaseDateText.trim())?.time }
                        .getOrNull() ?: System.currentTimeMillis()
                    onSave(itemName.trim().ifBlank { "Untitled item" }, purchaseDate, selectedDuration)
                },
                enabled = !isCustomSelected || (selectedDuration as? WarrantyDuration.Custom)?.months?.let { it > 0 } == true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
