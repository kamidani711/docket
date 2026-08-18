package com.docket.ui.screens.review

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.stringResource
import com.docket.R
import com.docket.domain.model.ExportFormat
import com.docket.domain.model.ExportProgress
import com.docket.domain.model.Folder
import com.docket.ui.components.DocketTextButton
import com.docket.ui.components.PrimaryButton
import com.docket.ui.theme.DocketSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveSheet(
    initialName: String,
    folders: List<Folder>,
    isPdfImport: Boolean,
    exportProgress: ExportProgress?,
    defaultFormat: ExportFormat,
    onCreateFolder: (String) -> Unit,
    onSave: (title: String, folderId: Long?, format: ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var format by remember { mutableStateOf(defaultFormat) }
    var showNewFolderField by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    val isSaving = exportProgress != null

    ModalBottomSheet(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Column(
            modifier = Modifier
                .padding(horizontal = DocketSpacing.space24)
                .padding(bottom = DocketSpacing.space32),
            verticalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
        ) {
            Text(stringResource(R.string.save_sheet_title), style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.save_name_label)) },
                singleLine = true,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Text(stringResource(R.string.save_folder_label), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)
            ) {
                FilterChip(
                    selected = selectedFolderId == null,
                    onClick = { selectedFolderId = null },
                    label = { Text(stringResource(R.string.save_no_folder)) },
                    enabled = !isSaving
                )
                folders.forEach { folder ->
                    FilterChip(
                        selected = selectedFolderId == folder.id,
                        onClick = { selectedFolderId = folder.id },
                        label = { Text(folder.name) },
                        enabled = !isSaving
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { showNewFolderField = true },
                    label = { Text(stringResource(R.string.save_new_folder_chip)) },
                    enabled = !isSaving
                )
            }

            if (showNewFolderField) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)
                ) {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text(stringResource(R.string.save_new_folder_name_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    DocketTextButton(
                        text = stringResource(R.string.save_add_folder),
                        onClick = {
                            if (newFolderName.isNotBlank()) {
                                onCreateFolder(newFolderName.trim())
                                newFolderName = ""
                                showNewFolderField = false
                            }
                        }
                    )
                }
            }

            if (!isPdfImport) {
                Text(stringResource(R.string.save_format_label), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
                    FilterChip(
                        selected = format == ExportFormat.PDF,
                        onClick = { format = ExportFormat.PDF },
                        label = { Text(stringResource(R.string.save_format_pdf)) },
                        enabled = !isSaving
                    )
                    FilterChip(
                        selected = format == ExportFormat.IMAGE_SET,
                        onClick = { format = ExportFormat.IMAGE_SET },
                        label = { Text(stringResource(R.string.save_format_images)) },
                        enabled = !isSaving
                    )
                }
            }

            if (exportProgress != null) {
                Column(verticalArrangement = Arrangement.spacedBy(DocketSpacing.space4)) {
                    val fraction = if (exportProgress.totalPages > 0) {
                        exportProgress.currentPage / exportProgress.totalPages.toFloat()
                    } else {
                        0f
                    }
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = progressLabel(exportProgress),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            PrimaryButton(
                text = if (isSaving) stringResource(R.string.save_button_saving) else stringResource(R.string.common_save),
                onClick = { onSave(name, selectedFolderId, format) },
                loading = isSaving,
                enabled = name.isNotBlank() && !isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun progressLabel(progress: ExportProgress): String = when (progress.stage) {
    ExportProgress.Stage.PROCESSING_PAGE ->
        stringResource(R.string.save_progress_processing_page, progress.currentPage, progress.totalPages)
    ExportProgress.Stage.WRITING_FILE -> stringResource(R.string.save_progress_writing_file)
    ExportProgress.Stage.SAVING_TO_LIBRARY -> stringResource(R.string.save_progress_saving_library)
}
