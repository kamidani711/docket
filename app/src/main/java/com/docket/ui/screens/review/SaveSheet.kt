package com.docket.ui.screens.review

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docket.R
import com.docket.domain.model.ExportFormat
import com.docket.domain.model.ExportProgress
import com.docket.domain.model.Folder
import com.docket.ui.components.DocketTextButton
import com.docket.ui.components.PrimaryButton
import com.docket.ui.theme.DocketFieldLabelStyle
import com.docket.ui.theme.DocketInk2Light
import com.docket.ui.theme.DocketOnSurfaceLight
import com.docket.ui.theme.DocketOnSurfaceVariantLight
import com.docket.ui.theme.DocketOutlineVariantLight
import com.docket.ui.theme.DocketPrimaryContainerLight
import com.docket.ui.theme.DocketPrimaryLight
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.theme.DocketSurfaceContainerLowestLight

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
    // No dummy-name fallback (the prototype's sample "Whirlpool fridge receipt") — a blank field
    // resolves to a real date/counter name at save time, same as ScanSessionViewModel.save()'s
    // own title fallback, so Save stays enabled rather than blocked on typing something first.
    var name by remember(initialName) { mutableStateOf(initialName) }
    var selectedFolderId by remember { mutableStateOf<Long?>(null) }
    var format by remember { mutableStateOf(defaultFormat) }
    var showNewFolderField by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    val isSaving = exportProgress != null

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        containerColor = androidx.compose.ui.graphics.Color.White,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Save document",
                style = MaterialTheme.typography.headlineSmall,
                color = DocketOnSurfaceLight
            )

            com.docket.ui.components.DocketUnderlineField(
                value = name,
                onValueChange = { name = it },
                label = "Document name"
            )

            Text(
                text = "Folder",
                style = DocketFieldLabelStyle,
                color = DocketOnSurfaceVariantLight
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FolderChip(
                    label = "No folder",
                    isSelected = selectedFolderId == null,
                    onClick = { selectedFolderId = null }
                )
                folders.forEach { folder ->
                    FolderChip(
                        label = folder.name,
                        isSelected = selectedFolderId == folder.id,
                        onClick = { selectedFolderId = folder.id }
                    )
                }
                FolderChip(
                    label = "+ New",
                    isSelected = false,
                    onClick = { showNewFolderField = true }
                )
            }

            if (showNewFolderField) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                Text(
                    text = "Format",
                    style = DocketFieldLabelStyle,
                    color = DocketOnSurfaceVariantLight
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FormatChoiceChip(
                        label = "PDF",
                        isSelected = format == ExportFormat.PDF,
                        onClick = { format = ExportFormat.PDF },
                        modifier = Modifier.weight(1f)
                    )
                    FormatChoiceChip(
                        label = "Separate images",
                        isSelected = format == ExportFormat.IMAGE_SET,
                        onClick = { format = ExportFormat.IMAGE_SET },
                        modifier = Modifier.weight(1f)
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    shape = com.docket.ui.theme.DocketPillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DocketPrimaryContainerLight,
                        contentColor = DocketPrimaryLight
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Button(
                    onClick = { onSave(name, selectedFolderId, format) },
                    enabled = !isSaving,
                    shape = com.docket.ui.theme.DocketPillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DocketPrimaryLight,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(54.dp)
                ) {
                    Text(
                        text = if (isSaving) "Saving…" else "Save to library",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val chipShape = com.docket.ui.theme.DocketPillShape
    val primaryColor = DocketPrimaryLight
    val bgColor = if (isSelected) DocketPrimaryContainerLight else Color.Transparent
    val borderColor = if (isSelected) primaryColor else DocketOutlineVariantLight
    val textColor = if (isSelected) primaryColor else DocketInk2Light

    Surface(
        onClick = onClick,
        shape = chipShape,
        color = bgColor,
        modifier = Modifier.border(1.5.dp, borderColor, chipShape)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun FormatChoiceChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chipShape = RoundedCornerShape(14.dp)
    val primaryColor = DocketPrimaryLight
    val bgColor = if (isSelected) DocketPrimaryContainerLight else DocketSurfaceContainerLowestLight
    val borderColor = if (isSelected) primaryColor else DocketOutlineVariantLight
    val textColor = if (isSelected) primaryColor else DocketOnSurfaceLight

    Surface(
        onClick = onClick,
        shape = chipShape,
        color = bgColor,
        modifier = modifier.border(1.5.dp, borderColor, chipShape)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                color = textColor
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
