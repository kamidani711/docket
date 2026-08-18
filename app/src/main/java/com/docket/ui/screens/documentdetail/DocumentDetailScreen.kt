package com.docket.ui.screens.documentdetail

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.docket.domain.model.Document
import com.docket.ui.components.DocketTextButton
import com.docket.ui.components.SecondaryButton
import com.docket.ui.components.WarrantyCard
import com.docket.ui.theme.DocketDimens
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.theme.docketCardShadow
import com.docket.ui.util.formatDisplayDate
import com.docket.ui.util.formatMoney
import com.docket.ui.util.shareFiles
import java.util.concurrent.TimeUnit

/**
 * The screen a document's Library card opens into. Previously mostly empty space below a
 * single centered thumbnail; now the page strip and the action row do the work of filling it,
 * rather than padding it out with decoration. See the design skill's "empty space" guidance.
 *
 * Deferred: a shared-element transition from the Library card's thumbnail into the page strip
 * here. That's a real cross-screen motion change (touches DocketNavHost's transitions, not just
 * this file) — bundled into the GLOBAL motion pass rather than this screen's redesign, so it
 * can be done once for every screen that deserves it instead of bespoke here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    onBack: () -> Unit,
    onOpenPremium: () -> Unit,
    viewModel: DocumentDetailViewModel = hiltViewModel()
) {
    val document by viewModel.document.collectAsState()
    val hasOcrText by viewModel.hasOcrText.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val receipt by viewModel.receipt.collectAsState()
    val warranty by viewModel.warranty.collectAsState()
    val receiptDraft by viewModel.receiptDraft.collectAsState()
    val isParsingReceipt by viewModel.isParsingReceipt.collectAsState()
    val pageBitmaps by viewModel.pageBitmaps.collectAsState()

    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var showExportSheet by remember { mutableStateOf(false) }
    var showWarrantySheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isEditingName by remember { mutableStateOf(false) }
    var viewerInitialPage by remember { mutableStateOf<Int?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Reminders still get scheduled either way — WarrantyReminderWorker just no-ops on a
          notification it can't post if this was denied. See that file. */ }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is DocumentDetailEvent.ShareFiles -> shareFiles(context, event.files, event.mimeType)
                DocumentDetailEvent.Deleted -> onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (document != null && receipt == null) {
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More actions")
                            }
                            DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(if (isParsingReceipt) "Reading receipt…" else "Tag as receipt") },
                                    enabled = hasOcrText && !isParsingReceipt,
                                    onClick = {
                                        viewModel.startTaggingAsReceipt()
                                        showOverflowMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        val currentDocument = document
        if (currentDocument == null) {
            Box(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DocketSpacing.space24)
            ) {
                DocumentPageStrip(
                    document = currentDocument,
                    pageBitmaps = pageBitmaps,
                    onEnsureBitmap = { index -> viewModel.ensurePageBitmap(index, PAGE_STRIP_MAX_DIMENSION) },
                    onPageTap = { index -> viewerInitialPage = index }
                )

                Column(
                    modifier = Modifier.padding(horizontal = DocketSpacing.space20),
                    verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)
                ) {
                    EditableDocumentName(
                        title = currentDocument.title,
                        isEditing = isEditingName,
                        onStartEditing = { isEditingName = true },
                        onCommit = { newTitle ->
                            viewModel.renameDocument(newTitle)
                            isEditingName = false
                        },
                        onCancel = { isEditingName = false }
                    )
                    Text(
                        text = documentSecondaryLine(currentDocument),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SearchableBadge(hasOcrText = hasOcrText)
                }

                DetailActionRow(
                    onShare = viewModel::shareDocument,
                    onExport = { showExportSheet = true },
                    onRename = { isEditingName = true },
                    onDelete = { showDeleteConfirm = true },
                    modifier = Modifier.padding(horizontal = DocketSpacing.space20)
                )

                val currentReceipt = receipt
                if (currentReceipt != null) {
                    HorizontalDivider()
                    Column(
                        modifier = Modifier.padding(horizontal = DocketSpacing.space20),
                        verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)
                    ) {
                        Text("Receipt", style = MaterialTheme.typography.titleMedium)
                        Column(verticalArrangement = Arrangement.spacedBy(DocketSpacing.space4)) {
                            Text(currentReceipt.merchant, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = buildString {
                                    append(formatMoney(currentReceipt.totalAmountCents, currentReceipt.currencyCode))
                                    currentReceipt.purchaseDate?.let { append(" · ${formatDate(it)}") }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DocketTextButton(text = "Edit receipt", onClick = { viewModel.editExistingReceipt() })
                    }
                }

                HorizontalDivider()
                Column(
                    modifier = Modifier.padding(horizontal = DocketSpacing.space20, vertical = DocketSpacing.space4),
                    verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)
                ) {
                    Text("Warranty", style = MaterialTheme.typography.titleMedium)
                    val currentWarranty = warranty
                    if (currentWarranty == null) {
                        SecondaryButton(
                            text = "Set warranty",
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                showWarrantySheet = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        WarrantyCard(
                            itemName = currentWarranty.itemName,
                            expiryDateLabel = "Expires ${formatDate(currentWarranty.expiryDate)}",
                            daysRemaining = daysUntil(currentWarranty.expiryDate)
                        )
                        DocketTextButton(text = "Remove warranty", onClick = viewModel::deleteWarranty)
                    }
                }

                Spacer(modifier = Modifier.height(DocketSpacing.space16))
            }
        }

        if (showExportSheet && currentDocument != null) {
            ExportSheet(
                document = currentDocument,
                hasOcrText = hasOcrText,
                isPremium = isPremium,
                exportState = exportState,
                onExportPdf = viewModel::exportPdf,
                onExportImages = viewModel::exportImages,
                onOpenPremium = onOpenPremium,
                onDismiss = {
                    showExportSheet = false
                    viewModel.clearExportState()
                }
            )
        }

        val currentReceiptDraft = receiptDraft
        if (currentReceiptDraft != null) {
            ReceiptEditSheet(
                draft = currentReceiptDraft,
                onSave = viewModel::saveReceipt,
                onDismiss = viewModel::cancelReceiptEdit
            )
        }

        if (showWarrantySheet && currentDocument != null) {
            WarrantySheet(
                suggestedItemName = currentDocument.title,
                onSave = { itemName, purchaseDate, duration ->
                    viewModel.setWarranty(itemName, purchaseDate, duration)
                    showWarrantySheet = false
                },
                onDismiss = { showWarrantySheet = false }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete this document?") },
                text = { Text("It moves to Recently Deleted and stays there for 30 days before it's gone for good.") },
                confirmButton = {
                    DocketTextButton(
                        text = "Delete",
                        onClick = {
                            // Haptic on destructive confirmation — one of only three moments
                            // in the app that get one, per the design brief.
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            showDeleteConfirm = false
                            viewModel.deleteDocument()
                        }
                    )
                },
                dismissButton = {
                    DocketTextButton(text = "Cancel", onClick = { showDeleteConfirm = false })
                }
            )
        }

        val viewerPage = viewerInitialPage
        if (viewerPage != null && currentDocument != null) {
            FullScreenPageViewer(
                document = currentDocument,
                initialPage = viewerPage,
                pageBitmaps = pageBitmaps,
                onEnsureBitmap = { index -> viewModel.ensurePageBitmap(index, PAGE_STRIP_MAX_DIMENSION) },
                onDismiss = { viewerInitialPage = null }
            )
        }
    }
}

private fun documentSecondaryLine(document: Document): String {
    val pages = if (document.pageCount == 1) "1 page" else "${document.pageCount} pages"
    return "$pages · ${formatDate(document.createdAt)} · ${formatFileSize(document.sizeBytes)}"
}

/** Tap the name to rename it in place — no separate screen, no dialog. [onCommit] fires on IME
 *  "Done" or when focus leaves the field; an empty/unchanged result is the caller's job to
 *  ignore (see DocumentDetailViewModel.renameDocument). */
@Composable
private fun EditableDocumentName(
    title: String,
    isEditing: Boolean,
    onStartEditing: () -> Unit,
    onCommit: (String) -> Unit,
    onCancel: () -> Unit
) {
    if (isEditing) {
        var value by remember(title) { mutableStateOf(title) }
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    onCommit(value)
                    keyboardController?.hide()
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
    } else {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onStartEditing)
        )
    }
}

/** A quiet, neutral badge — not primary-colored, not underlined, nothing that reads as a link
 *  a user could tap and have something happen. It was exactly that fake-tappable look the
 *  design brief called out; this is purely a status indicator. */
@Composable
private fun SearchableBadge(hasOcrText: Boolean) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = if (hasOcrText) "Text searchable" else "Extracting text…",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = DocketSpacing.space12, vertical = DocketSpacing.space4)
        )
    }
}

/** Four equal-weight icon buttons — no primary/secondary hierarchy between them, per the design
 *  brief. Share is first and reachable in one tap (no sheet, no confirmation). */
@Composable
private fun DetailActionRow(
    onShare: () -> Unit,
    onExport: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DetailActionButton(Icons.Filled.Share, "Share", onShare)
        DetailActionButton(Icons.Filled.FileDownload, "Export", onExport)
        DetailActionButton(Icons.Filled.Edit, "Rename", onRename)
        DetailActionButton(Icons.Filled.Delete, "Delete", onDelete)
    }
}

@Composable
private fun DetailActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .widthIn(min = DocketDimens.minTouchTarget)
            .padding(DocketSpacing.space8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Large horizontally-scrollable page thumbnails — the thing this screen was 70% empty without.
 *  Tapping one opens [FullScreenPageViewer]. Bitmaps decode on demand as each tile enters
 *  composition (see [DocumentDetailViewModel.ensurePageBitmap]), not all up front. */
@Composable
private fun DocumentPageStrip(
    document: Document,
    pageBitmaps: Map<Int, Bitmap>,
    onEnsureBitmap: (Int) -> Unit,
    onPageTap: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = DocketSpacing.space20),
        horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space12)
    ) {
        itemsIndexed((0 until document.pageCount).toList(), key = { _, page -> page }) { _, page ->
            LaunchedEffect(page) { onEnsureBitmap(page) }
            val bitmap = pageBitmaps[page]
            val pageShape = MaterialTheme.shapes.medium
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .aspectRatio(DocketDimens.pageAspectRatio)
                    .docketCardShadow(elevation = DocketSpacing.space4, shape = pageShape)
                    .clip(pageShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .clickable { onPageTap(page) },
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Page ${page + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

/** Full-screen swipe-between-pages viewer with pinch-to-zoom, opened from [DocumentPageStrip].
 *  A real Dialog rather than a nav destination — this is a transient overlay, not a place with
 *  its own back-stack identity. */
@Composable
private fun FullScreenPageViewer(
    document: Document,
    initialPage: Int,
    pageBitmaps: Map<Int, Bitmap>,
    onEnsureBitmap: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val pagerState = rememberPagerState(initialPage = initialPage) { document.pageCount }
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                LaunchedEffect(page) { onEnsureBitmap(page) }
                ZoomableImage(bitmap = pageBitmaps[page], contentDescription = "Page ${page + 1}")
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(DocketSpacing.space8)
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }

            if (document.pageCount > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(DocketSpacing.space16),
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${document.pageCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = DocketSpacing.space12, vertical = DocketSpacing.space4)
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(bitmap: Bitmap?, contentDescription: String?) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale
                    offset = if (newScale <= 1f) Offset.Zero else offset + pan
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1f) 1f else 2.5f
                        offset = Offset.Zero
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
        } else {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

private fun daysUntil(expiryMillis: Long): Int =
    TimeUnit.MILLISECONDS.toDays(expiryMillis - System.currentTimeMillis()).toInt()

private fun formatDate(epochMillis: Long): String = formatDisplayDate(epochMillis)

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}

private const val PAGE_STRIP_MAX_DIMENSION = 1200
