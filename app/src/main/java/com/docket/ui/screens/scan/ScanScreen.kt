package com.docket.ui.screens.scan

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.docket.R
import com.docket.ui.theme.DocketOnPrimaryDark
import com.docket.ui.theme.DocketPrimaryDark
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.theme.DocketSurfaceContainerLowestDark

/**
 * The one screen a scan flow starts from — folds what used to be two screens ("Scan a document"
 * with three stacked buttons, then a separate camera step) into a single dark, full-bleed
 * capture surface. Tapping the shutter goes straight to the camera; gallery/PDF import live as
 * small icon buttons in the top bar instead of their own rows.
 *
 * IMPORTANT — read [rememberDocumentScannerLauncher]'s doc before changing this screen. The
 * actual live camera preview belongs to a Google Play Services system Activity we don't render
 * or theme. Everything here — the "viewfinder" framing, the shutter, the mode chips — is scene-
 * setting around that handoff, not a real preview. That's a deliberate, documented v1 boundary,
 * not an oversight.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanSessionViewModel,
    onBack: () -> Unit,
    onContinueToReview: () -> Unit
) {
    val session by viewModel.session.collectAsState()
    val previewBitmaps by viewModel.previewBitmaps.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedMode by remember { mutableStateOf(ScanCaptureMode.DOCUMENT) }

    // Distinguishes "a page just came back from this screen's own shutter/import tap" from "a
    // draft session already existed when this screen first composed" (process-death recovery,
    // or backing out of Review without discarding). Only the former should jump straight to
    // Review — the latter shows the in-progress thumbnail stack instead, so a recovered draft
    // doesn't yank the user into Review with no chance to see what's there first.
    var justCaptured by remember { mutableStateOf(false) }

    val launchScanner = rememberDocumentScannerLauncher(
        onPagesCaptured = { uris ->
            justCaptured = true
            viewModel.onPagesCaptured(uris)
        },
        onError = { errorMessage = it }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            justCaptured = true
            viewModel.onPagesCaptured(uris)
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            justCaptured = true
            viewModel.onPdfPicked(uri)
        }
    }

    LaunchedEffect(session) {
        val current = session
        if (justCaptured && current != null && (current.pages.isNotEmpty() || current.isPdfImport)) {
            onContinueToReview()
        }
    }

    val currentSession = session
    val inProgressPages = currentSession?.pages.orEmpty()
    val haptics = LocalHapticFeedback.current

    Scaffold(
        containerColor = DocketSurfaceContainerLowestDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ---- Top bar: icon-only, no title, no back arrow chrome ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = DocketSpacing.space20, vertical = DocketSpacing.space8),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CaptureIconButton(
                        icon = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.common_close),
                        onClick = onBack
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space4)) {
                        CaptureIconButton(
                            icon = Icons.Filled.Image,
                            contentDescription = stringResource(R.string.scan_gallery_button),
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                        CaptureIconButton(
                            icon = Icons.Filled.PictureAsPdf,
                            contentDescription = stringResource(R.string.scan_pdf_button),
                            onClick = { pdfLauncher.launch(arrayOf("application/pdf")) }
                        )
                    }
                }

                // ---- "Viewfinder" framing — not a live preview, see the class doc ----
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(DocketSpacing.space24),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(DocketSpacing.space24))
                            .background(Color.White.copy(alpha = 0.03f)),
                        contentAlignment = Alignment.Center
                    ) {
                        ViewfinderCorners(modifier = Modifier.fillMaxSize())
                        Icon(
                            imageVector = Icons.Filled.DocumentScanner,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.28f),
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                errorMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DocketSpacing.space24, vertical = DocketSpacing.space8)
                    )
                }

                // ---- Mode chips, above the shutter ----
                ScanModeChipRow(
                    selected = selectedMode,
                    onSelect = { selectedMode = it },
                    modifier = Modifier.padding(bottom = DocketSpacing.space16)
                )

                // ---- Shutter ----
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = DocketSpacing.space24),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(DocketSpacing.space12)) {
                        ShutterButton(
                            onClick = {
                                // Haptic on capture — one of only three moments in the app that
                                // get one, per the design brief.
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                launchScanner()
                            }
                        )
                        Text(
                            text = stringResource(R.string.scan_shutter_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // ---- Bottom-left in-progress thumbnail stack (recovered draft only — see
            // `justCaptured` above) ----
            if (!justCaptured && inProgressPages.isNotEmpty()) {
                CapturedPagesStack(
                    pageCount = inProgressPages.size,
                    thumbnailPaths = inProgressPages.take(3).map { previewBitmaps[it.id] },
                    onClick = onContinueToReview,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .statusBarsPadding()
                        .padding(start = DocketSpacing.space20, bottom = DocketSpacing.space20)
                )
            }
        }
    }
}

/** Capture mode chips above the shutter. [ScanCaptureMode.DOCUMENT] and [ScanCaptureMode.RECEIPT]
 *  are the two the design brief calls "implemented for real"; here that means the selection is
 *  real and remembered for the session, but neither the crop aspect ratio nor any capture-time
 *  filter default actually changes yet, because the Google Play Services scanner Activity we
 *  hand off to (see [rememberDocumentScannerLauncher]) has no API to receive either from us. All
 *  four chips are fully wired UI; only the downstream effect is stubbed pending the CameraX
 *  rebuild. [ID_CARD]/[BOOK] are visual-only placeholders, as called out in the design brief. */
enum class ScanCaptureMode { DOCUMENT, RECEIPT, ID_CARD, BOOK }

@Composable
private fun ScanModeChipRow(
    selected: ScanCaptureMode,
    onSelect: (ScanCaptureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = remember {
        listOf(
            ScanCaptureMode.DOCUMENT,
            ScanCaptureMode.RECEIPT,
            ScanCaptureMode.ID_CARD,
            ScanCaptureMode.BOOK
        )
    }
    LazyRow(
        modifier = modifier.fillMaxWidth().selectableGroup(),
        contentPadding = PaddingValues(horizontal = DocketSpacing.space24),
        horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)
    ) {
        items(modes) { mode ->
            ScanModeChip(mode = mode, isSelected = mode == selected, onClick = { onSelect(mode) })
        }
    }
}

@Composable
private fun ScanModeChip(mode: ScanCaptureMode, isSelected: Boolean, onClick: () -> Unit) {
    val label = stringResource(
        when (mode) {
            ScanCaptureMode.DOCUMENT -> R.string.scan_mode_document
            ScanCaptureMode.RECEIPT -> R.string.scan_mode_receipt
            ScanCaptureMode.ID_CARD -> R.string.scan_mode_id_card
            ScanCaptureMode.BOOK -> R.string.scan_mode_book
        }
    )
    val backgroundColor = if (isSelected) DocketPrimaryDark else Color.Transparent
    val contentColor = if (isSelected) DocketOnPrimaryDark else Color.White.copy(alpha = 0.85f)
    val borderColor = if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.3f)

    Surface(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton),
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = DocketSpacing.space16, vertical = DocketSpacing.space12),
            contentAlignment = Alignment.Center
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge, color = contentColor)
        }
    }
}

/** Four corner brackets, reticle-style — reads as an active capture frame at a glance instead
 *  of a plain bordered rectangle, the same visual shorthand every modern camera/scanner app
 *  uses for "this is the capture area." A slow ambient opacity breathe (not a one-shot
 *  transition, so the <300ms motion-length rule doesn't apply) signals "live and waiting for
 *  you," honest about there being no real edge-detection feed underneath it — see the class
 *  doc for why that's a real handoff boundary, not a shortcut. */
@Composable
private fun ViewfinderCorners(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "viewfinderPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "viewfinderAlpha"
    )
    val color = Color.White.copy(alpha = alpha)
    Canvas(modifier = modifier) {
        val strokeWidth = 2.5.dp.toPx()
        val armLength = size.minDimension * 0.08f
        val inset = 20.dp.toPx()

        val corners = listOf(
            Offset(inset, inset) to (1 to 1), // top-left
            Offset(size.width - inset, inset) to (-1 to 1), // top-right
            Offset(inset, size.height - inset) to (1 to -1), // bottom-left
            Offset(size.width - inset, size.height - inset) to (-1 to -1) // bottom-right
        )
        corners.forEach { (point, direction) ->
            val (dx, dy) = direction
            drawLine(
                color = color,
                start = point,
                end = Offset(point.x + armLength * dx, point.y),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = point,
                end = Offset(point.x, point.y + armLength * dy),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    val description = stringResource(R.string.scan_shutter_cd)
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .padding(4.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(3.dp, DocketPrimaryDark, CircleShape)
            .clickable(onClickLabel = description, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PhotoCamera,
            contentDescription = description,
            tint = DocketPrimaryDark,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun CaptureIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White)
    }
}

@Composable
private fun CapturedPagesStack(
    pageCount: Int,
    thumbnailPaths: List<Bitmap?>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = pluralStringResource(R.plurals.scan_pages_in_progress, pageCount, pageCount)
    Box(
        modifier = modifier
            .clickable(onClickLabel = description, role = Role.Button, onClick = onClick)
            .padding(DocketSpacing.space4),
        contentAlignment = Alignment.BottomStart
    ) {
        thumbnailPaths.forEachIndexed { index, bitmap ->
            Box(
                modifier = Modifier
                    .offset(x = (index * 10).dp)
                    .width(44.dp)
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(DocketSpacing.space8))
                    .background(Color.White)
                    .border(1.dp, Color.Black.copy(alpha = 0.15f), RoundedCornerShape(DocketSpacing.space8))
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        Surface(
            modifier = Modifier
                .offset(x = (thumbnailPaths.size * 10 + 4).dp, y = (-4).dp)
                .align(Alignment.TopStart),
            color = DocketPrimaryDark,
            shape = CircleShape
        ) {
            Text(
                text = pageCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = DocketOnPrimaryDark,
                modifier = Modifier.padding(horizontal = DocketSpacing.space8, vertical = 2.dp)
            )
        }
    }
}
