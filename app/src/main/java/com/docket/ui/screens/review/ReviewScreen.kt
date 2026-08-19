package com.docket.ui.screens.review

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.docket.R
import com.docket.ui.components.DocketTextButton
import com.docket.ui.components.EmptyState
import com.docket.ui.components.PrimaryButton
import com.docket.ui.screens.scan.ScanSessionViewModel
import com.docket.ui.screens.scan.ScanUiEvent
import com.docket.ui.screens.scan.rememberDocumentScannerLauncher
import com.docket.ui.theme.DocketDimens
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.theme.DocketTheme

/**
 * Forced dark regardless of the system theme — Review is a capture surface (editing a page you
 * just photographed), not a content surface, and the design system draws a hard line between
 * the two: capture stays dark unconditionally so a bright system-light theme never fights the
 * page preview for the user's eye. [DocketTheme] already exposes a `darkTheme` override built
 * for exactly this, so every color role below (surfaces, text, the accent, the error color for
 * [errorMessage]) comes from the same tuned dark scheme the rest of the app uses in dark mode —
 * nothing here is a one-off hardcoded color.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: ScanSessionViewModel,
    onBack: () -> Unit,
    onSaveComplete: (documentId: Long) -> Unit,
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
                    // Haptic on successful save — one of only three moments in the app that
                    // get one, per the design brief.
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
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (pages.isNotEmpty()) {
                            pluralStringResource(R.plurals.review_title_pages, pages.size, pages.size)
                        } else {
                            stringResource(R.string.review_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (currentSession != null) {
                        // No confirmation dialog for v1 — deliberately simple. Without this,
                        // backing out of a scan leaves its captured JPEGs on disk forever: the
                        // session row gets replaced by the next scan, but nothing ever deletes
                        // the orphaned files. This is the only thing that does.
                        DocketTextButton(
                            text = stringResource(R.string.review_discard),
                            onClick = {
                                viewModel.discardSession()
                                onBack()
                            }
                        )
                    }
                }
            )
        }
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
                Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        when {
                            currentSession.isPdfImport -> {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Filled.PictureAsPdf,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            pluralStringResource(
                                                R.plurals.review_pdf_imported_pages,
                                                currentSession.importedPdfPageCount,
                                                currentSession.importedPdfPageCount
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            selectedPage != null -> {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val bitmap = previewBitmaps[selectedPage.id]
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize().padding(DocketSpacing.space16)
                                        )
                                    } else {
                                        CircularProgressIndicator()
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    ReviewActionButton(Icons.Filled.Crop, stringResource(R.string.review_action_crop)) { isCropping = true }
                                    ReviewActionButton(Icons.AutoMirrored.Filled.RotateRight, stringResource(R.string.review_action_rotate)) {
                                        viewModel.rotatePage(selectedPage.id, 90)
                                    }
                                    ReviewActionButton(Icons.Filled.CameraAlt, stringResource(R.string.review_action_retake)) {
                                        retakingPageId = selectedPage.id
                                        launchRetake()
                                    }
                                    ReviewActionButton(Icons.Filled.Delete, stringResource(R.string.common_delete)) {
                                        viewModel.deletePage(selectedPage)
                                    }
                                }

                                FilterPicker(
                                    swatches = filterSwatches,
                                    selectedFilter = selectedPage.filter,
                                    onFilterSelected = { viewModel.setFilter(selectedPage.id, it) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = DocketSpacing.space8)
                                )
                            }

                            else -> {
                                EmptyState(
                                    title = stringResource(R.string.review_no_pages_title),
                                    message = stringResource(R.string.review_no_pages_message),
                                    icon = Icons.Filled.Inbox,
                                    actionLabel = stringResource(R.string.review_add_page),
                                    onAction = launchAddPage,
                                    modifier = Modifier.weight(1f).fillMaxWidth()
                                )
                            }
                        }

                        if (!currentSession.isPdfImport) {
                            PageStrip(
                                pages = pages,
                                previewBitmaps = previewBitmaps,
                                selectedPageId = selectedPageId,
                                onSelect = { selectedPageId = it },
                                onReorder = viewModel::reorderPages,
                                onAddPage = launchAddPage,
                                modifier = Modifier.fillMaxWidth().padding(vertical = DocketSpacing.space8)
                            )
                        }

                        errorMessage?.let { message ->
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(
                                    horizontal = DocketSpacing.space20,
                                    vertical = DocketSpacing.space4
                                )
                            )
                        }

                        PrimaryButton(
                            text = stringResource(R.string.common_save),
                            onClick = { showSaveSheet = true },
                            enabled = pages.isNotEmpty() || currentSession.isPdfImport,
                            modifier = Modifier.fillMaxWidth().padding(DocketSpacing.space20)
                        )
                    }

                    if (showSaveSheet) {
                        SaveSheet(
                            initialName = currentSession.suggestedName ?: "",
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
}

@Composable
private fun ReviewActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .widthIn(min = DocketDimens.minTouchTarget)
            .padding(DocketSpacing.space8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DocketSpacing.space4)
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
