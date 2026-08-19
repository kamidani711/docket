package com.docket.ui.screens.review

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docket.R
import com.docket.ui.components.EmptyState
import com.docket.ui.icons.DocketIcons
import com.docket.ui.screens.scan.ScanSessionViewModel
import com.docket.ui.screens.scan.ScanUiEvent
import com.docket.ui.screens.scan.rememberDocumentScannerLauncher
import com.docket.ui.theme.DocketBackgroundDark
import com.docket.ui.theme.DocketCoralLight
import com.docket.ui.theme.DocketOnSurfaceLight
import com.docket.ui.theme.DocketOnSurfaceVariantLight
import com.docket.ui.theme.DocketOutlineVariantLight
import com.docket.ui.theme.DocketPillShape
import com.docket.ui.theme.DocketPrimaryLight
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.theme.DocketSurfaceContainerLowestDark
import com.docket.ui.theme.DocketTheme
import com.docket.ui.theme.docketCardShadow

/**
 * Screen 7: Review screen matching the prototype.
 * Forced dark (#181818), top bar with "Review (X pages)" and coral "Discard" button,
 * 3:4 document paper preview, Crop / Rotate / Retake pill actions,
 * 4-card filter grid, and "Add page" + "Save" bottom bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: ScanSessionViewModel,
    onBack: () -> Unit,
    onSaveComplete: (documentId: Long) -> Unit,
    onDiscard: () -> Unit = onBack,
    onOpenPremium: () -> Unit
) = DocketTheme(darkTheme = true) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val previewBitmaps by viewModel.previewBitmaps.collectAsStateWithLifecycle()
    val filterSwatches by viewModel.filterSwatches.collectAsStateWithLifecycle()
    val cropBaseImage by viewModel.cropBaseImage.collectAsStateWithLifecycle()
    val exportProgress by viewModel.exportProgress.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val defaultExportFormat by viewModel.defaultExportFormat.collectAsStateWithLifecycle()

    var selectedPageId by remember { mutableStateOf<String?>(null) }
    var isCropping by remember { mutableStateOf(false) }
    var showSaveSheet by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retakingPageId by remember { mutableStateOf<String?>(null) }

    val currentSession = session
    val pages = currentSession?.pages.orEmpty()
    val selectedPage = pages.firstOrNull { it.id == selectedPageId }

    LaunchedEffect(pages.map { it.id }) {
        if (selectedPageId == null || pages.none { it.id == selectedPageId }) {
            selectedPageId = pages.firstOrNull()?.id
        }
    }

    LaunchedEffect(selectedPage) {
        selectedPage?.let { viewModel.loadFilterSwatches(it) }
    }

    val haptics = LocalHapticFeedback.current
    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is ScanUiEvent.SaveComplete -> {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showSaveSheet = false
                    onSaveComplete(event.documentId)
                }
                is ScanUiEvent.Error -> errorMessage = event.message
                ScanUiEvent.PremiumRequired -> onOpenPremium()
            }
        }
    }

    val launchAddPage = rememberDocumentScannerLauncher(
        onPagesCaptured = { uris -> viewModel.onPagesCaptured(uris) },
        onError = { errorMessage = it }
    )

    val launchRetake = rememberDocumentScannerLauncher(
        onPagesCaptured = { uris ->
            val targetPage = pages.firstOrNull { it.id == retakingPageId }
            if (targetPage != null && uris.isNotEmpty()) {
                viewModel.onRetakeResult(targetPage, uris.first())
                if (uris.size > 1) viewModel.onPagesCaptured(uris.drop(1))
            }
            retakingPageId = null
        },
        onError = {
            errorMessage = it
            retakingPageId = null
        }
    )

    Scaffold(
        containerColor = DocketBackgroundDark
    ) { innerPadding ->
        when {
            currentSession == null -> {
                EmptyState(
                    title = stringResource(R.string.review_empty_title),
                    message = stringResource(R.string.review_empty_message),
                    modifier = Modifier.padding(innerPadding).fillMaxSize()
                )
            }

            isCropping && selectedPage != null -> {
                LaunchedEffect(selectedPage.id) { viewModel.loadCropBaseImage(selectedPage) }
                CropEditor(
                    baseImage = cropBaseImage,
                    initialCorners = selectedPage.corners,
                    onConfirm = { newCorners ->
                        viewModel.updateCorners(selectedPage.id, newCorners)
                        isCropping = false
                        viewModel.clearCropBaseImage()
                    },
                    onCancel = {
                        isCropping = false
                        viewModel.clearCropBaseImage()
                    },
                    modifier = Modifier.padding(innerPadding).fillMaxSize()
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .background(DocketBackgroundDark)
                ) {
                    // Top Bar from Prototype Screen 7
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    imageVector = DocketIcons.Back,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = if (pages.isNotEmpty()) "Review (${pages.size} pages)" else "Review",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }

                        Text(
                            text = "Discard",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = DocketCoralLight,
                            modifier = Modifier.clickable {
                                viewModel.discardSession()
                                onDiscard()
                            }
                        )
                    }

                    // Center Document Paper Preview (3:4 aspect ratio)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val paperShape = RoundedCornerShape(14.dp)
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                                .shadow(
                                    elevation = 20.dp,
                                    shape = paperShape,
                                    ambientColor = Color.Black.copy(alpha = 0.5f),
                                    spotColor = Color.Black.copy(alpha = 0.5f)
                                )
                                .clip(paperShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            val bitmap = selectedPage?.let { previewBitmaps[it.id] }
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // Stylized receipt / document placeholder lines matching prototype
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 22.dp, vertical = 26.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.55f)
                                            .height(12.dp)
                                            .background(DocketOnSurfaceLight, RoundedCornerShape(4.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .height(6.dp)
                                            .background(DocketOnSurfaceVariantLight, RoundedCornerShape(3.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.75f)
                                            .height(6.dp)
                                            .background(DocketOnSurfaceVariantLight, RoundedCornerShape(3.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.90f)
                                            .height(6.dp)
                                            .background(DocketOnSurfaceVariantLight, RoundedCornerShape(3.dp))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .padding(vertical = 6.dp)
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(DocketOutlineVariantLight)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.40f)
                                                .height(6.dp)
                                                .background(DocketOnSurfaceVariantLight, RoundedCornerShape(3.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.35f)
                                                .height(6.dp)
                                                .background(DocketPrimaryLight, RoundedCornerShape(3.dp))
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.50f)
                                                .height(6.dp)
                                                .background(DocketOnSurfaceVariantLight, RoundedCornerShape(3.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.25f)
                                                .height(6.dp)
                                                .background(DocketOnSurfaceVariantLight, RoundedCornerShape(3.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action Pill Buttons from Prototype: Crop, Rotate, Retake
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReviewActionPill(
                            icon = DocketIcons.Crop,
                            label = "Crop",
                            onClick = { isCropping = true }
                        )
                        ReviewActionPill(
                            icon = DocketIcons.Rotate,
                            label = "Rotate",
                            onClick = { selectedPage?.let { viewModel.rotatePage(it.id, 90) } }
                        )
                        ReviewActionPill(
                            icon = DocketIcons.Camera,
                            label = "Retake",
                            onClick = {
                                selectedPage?.let {
                                    retakingPageId = it.id
                                    launchRetake()
                                }
                            }
                        )
                    }

                    // Filter Picker Grid
                    if (selectedPage != null) {
                        FilterPicker(
                            swatches = filterSwatches,
                            selectedFilter = selectedPage.filter,
                            onFilterSelected = { viewModel.setFilter(selectedPage.id, it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    errorMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }

                    // Bottom Action Bar: Add page + Save
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = launchAddPage,
                            shape = DocketPillShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DocketSurfaceContainerLowestDark,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        ) {
                            Text(
                                text = "Add page",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Button(
                            onClick = { showSaveSheet = true },
                            shape = DocketPillShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DocketPrimaryLight,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(54.dp)
                                .docketCardShadow(
                                    elevation = 12.dp,
                                    shape = DocketPillShape,
                                    tint = DocketPrimaryLight,
                                    ambientAlpha = 0.18f,
                                    spotAlpha = 0.35f
                                )
                        ) {
                            Text(
                                text = "Save",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                if (showSaveSheet) {
                    SaveSheet(
                        // Blank, not a placeholder like the prototype's sample "Whirlpool fridge
                        // receipt" — suggestedName lands asynchronously (OCR or date/counter
                        // fallback, see ScanSessionViewModel's save()) and may not have arrived
                        // yet; save() re-applies the same fallback at save time regardless.
                        initialName = currentSession.suggestedName.orEmpty(),
                        folders = folders,
                        isPdfImport = currentSession.isPdfImport,
                        exportProgress = exportProgress,
                        defaultFormat = defaultExportFormat,
                        onCreateFolder = viewModel::createFolder,
                        onSave = { title, folderId, format -> viewModel.save(title, folderId, format) },
                        onDismiss = { showSaveSheet = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = DocketPillShape,
        color = DocketSurfaceContainerLowestDark
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White
            )
        }
    }
}
