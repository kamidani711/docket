package com.docket.ui.screens.documentdetail

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.docket.domain.model.Document
import com.docket.domain.model.ExportFormat
import com.docket.domain.model.ImageExportFormat
import com.docket.domain.model.PdfPageSize
import com.docket.ui.components.PrimaryButton
import com.docket.ui.components.SecondaryButton
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.util.openFile
import com.docket.ui.util.saveFileToUri
import com.docket.ui.util.shareFiles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    document: Document,
    hasOcrText: Boolean,
    isPremium: Boolean,
    exportState: ExportUiState,
    onExportPdf: (PdfPageSize, Int, Boolean) -> Unit,
    onExportImages: (List<Int>, ImageExportFormat, Int) -> Unit,
    onOpenPremium: () -> Unit,
    onDismiss: () -> Unit
) {
    val isExporting = exportState is ExportUiState.Exporting

    ModalBottomSheet(onDismissRequest = { if (!isExporting) onDismiss() }) {
        Column(
            modifier = Modifier
                .padding(horizontal = DocketSpacing.space24)
                .padding(bottom = DocketSpacing.space32),
            verticalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
        ) {
            Text("Export", style = MaterialTheme.typography.headlineSmall)

            if (document.format == ExportFormat.PDF) {
                PdfExportOptions(
                    hasOcrText = hasOcrText,
                    isPremium = isPremium,
                    isExporting = isExporting,
                    onExport = onExportPdf
                )
            } else {
                ImageExportOptions(
                    pageCount = document.pageCount,
                    isPremium = isPremium,
                    isExporting = isExporting,
                    onExport = onExportImages
                )
            }

            when (exportState) {
                is ExportUiState.Failed -> {
                    Text(
                        text = exportState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    if (exportState.isPremiumRequired) {
                        PrimaryButton(
                            text = "Unlock Premium",
                            onClick = onOpenPremium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                is ExportUiState.Done -> {
                    ExportDoneActions(files = exportState.files, mimeType = exportState.mimeType)
                }
                else -> Unit
            }
        }
    }
}

/** What to actually do with the file now that it exists — this is the whole point of exporting,
 *  so it lives right here in the sheet rather than behind a "Share" button on the screen that
 *  sits under this (still-open) modal and would need the sheet dismissed to ever reach. */
@Composable
private fun ExportDoneActions(files: List<java.io.File>, mimeType: String) {
    val context = LocalContext.current
    val primaryFile = files.firstOrNull()
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(mimeType)
    ) { uri ->
        if (uri != null && primaryFile != null && !saveFileToUri(context, primaryFile, uri)) {
            Toast.makeText(context, "Couldn't save the file.", Toast.LENGTH_SHORT).show()
        }
    }

    Text(
        text = if (files.size > 1) "Exported ${files.size} files." else "Exported.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Tighter padding than SecondaryButton's default -- three across at this width wraps
        // a short label like "Share" onto two lines with the default 24dp horizontal padding.
        val compactPadding = PaddingValues(horizontal = DocketSpacing.space8, vertical = DocketSpacing.space12)
        SecondaryButton(
            text = "Share",
            leadingIcon = Icons.Filled.Share,
            onClick = { shareFiles(context, files, mimeType) },
            modifier = Modifier.weight(1f),
            contentPadding = compactPadding
        )
        if (primaryFile != null) {
            // Open/Save act on the primary file only -- a multi-image export (batch page export)
            // doesn't have one obvious target for either action; Share already handles that case
            // via ACTION_SEND_MULTIPLE.
            SecondaryButton(
                text = "Open",
                leadingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                onClick = { openFile(context, primaryFile, mimeType) },
                modifier = Modifier.weight(1f),
                contentPadding = compactPadding
            )
            SecondaryButton(
                text = "Save",
                leadingIcon = Icons.Filled.Save,
                onClick = { saveLauncher.launch(primaryFile.name) },
                modifier = Modifier.weight(1f),
                contentPadding = compactPadding
            )
        }
    }
}

@Composable
private fun PdfExportOptions(
    hasOcrText: Boolean,
    isPremium: Boolean,
    isExporting: Boolean,
    onExport: (PdfPageSize, Int, Boolean) -> Unit
) {
    var pageSize by remember { mutableStateOf(PdfPageSize.FIT_TO_CONTENT) }
    var quality by remember { mutableStateOf(90) }
    var searchable by remember { mutableStateOf(false) }

    Text("Page size", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
        FilterChip(
            selected = pageSize == PdfPageSize.FIT_TO_CONTENT,
            onClick = { pageSize = PdfPageSize.FIT_TO_CONTENT },
            label = { Text("Fit to content") },
            enabled = !isExporting
        )
        FilterChip(
            selected = pageSize == PdfPageSize.A4,
            onClick = { pageSize = PdfPageSize.A4 },
            label = { Text("A4") },
            enabled = !isExporting
        )
        FilterChip(
            selected = pageSize == PdfPageSize.LETTER,
            onClick = { pageSize = PdfPageSize.LETTER },
            label = { Text("Letter") },
            enabled = !isExporting
        )
    }

    Text("Quality", style = MaterialTheme.typography.titleMedium)
    QualityChips(quality = quality, onQualityChange = { quality = it }, enabled = !isExporting)

    PremiumToggleRow(
        title = "Searchable text layer",
        subtitle = if (!hasOcrText) {
            "Waiting on OCR for this document"
        } else {
            "Embed an invisible, searchable text layer over the image"
        },
        checked = searchable,
        onCheckedChange = { searchable = it },
        isPremium = isPremium,
        enabled = !isExporting && hasOcrText
    )

    PrimaryButton(
        text = if (isExporting) "Exporting…" else "Export PDF",
        onClick = { onExport(pageSize, quality, searchable && isPremium) },
        loading = isExporting,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ImageExportOptions(
    pageCount: Int,
    isPremium: Boolean,
    isExporting: Boolean,
    onExport: (List<Int>, ImageExportFormat, Int) -> Unit
) {
    var format by remember { mutableStateOf(ImageExportFormat.JPEG) }
    var quality by remember { mutableStateOf(90) }
    var exportAll by remember { mutableStateOf(false) }

    Text("Format", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
        FilterChip(
            selected = format == ImageExportFormat.JPEG,
            onClick = { format = ImageExportFormat.JPEG },
            label = { Text("JPEG") },
            enabled = !isExporting
        )
        FilterChip(
            selected = format == ImageExportFormat.PNG,
            onClick = { format = ImageExportFormat.PNG },
            label = { Text("PNG") },
            enabled = !isExporting
        )
    }

    if (format == ImageExportFormat.JPEG) {
        Text("Quality", style = MaterialTheme.typography.titleMedium)
        QualityChips(quality = quality, onQualityChange = { quality = it }, enabled = !isExporting)
    }

    if (pageCount > 1) {
        PremiumToggleRow(
            title = "Export all $pageCount pages",
            subtitle = if (exportAll) "Batch export" else "Only the first page will be exported",
            checked = exportAll,
            onCheckedChange = { exportAll = it },
            isPremium = isPremium,
            enabled = !isExporting
        )
    }

    PrimaryButton(
        text = if (isExporting) "Exporting…" else "Export images",
        onClick = {
            val pages = if (exportAll && isPremium) (0 until pageCount).toList() else listOf(0)
            onExport(pages, format, quality)
        },
        loading = isExporting,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun QualityChips(quality: Int, onQualityChange: (Int) -> Unit, enabled: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
        listOf("Low" to 50, "Medium" to 80, "High" to 95).forEach { (label, value) ->
            FilterChip(
                selected = quality == value,
                onClick = { onQualityChange(value) },
                label = { Text(label) },
                enabled = enabled
            )
        }
    }
}

@Composable
private fun PremiumToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isPremium: Boolean,
    enabled: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space4)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (!isPremium) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Premium feature",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked && isPremium,
            onCheckedChange = onCheckedChange,
            enabled = enabled && isPremium
        )
    }
}
