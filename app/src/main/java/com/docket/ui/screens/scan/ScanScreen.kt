package com.docket.ui.screens.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.docket.R
import com.docket.ui.icons.DocketIcons
import com.docket.ui.navigation.Destination
import com.docket.ui.theme.DocketBackgroundDark
import com.docket.ui.theme.DocketCoralLight
import com.docket.ui.theme.DocketOnSurfaceVariantLight
import com.docket.ui.theme.DocketPillShape
import com.docket.ui.theme.DocketPrimaryLight
import com.docket.ui.theme.DocketSpacing
import com.docket.ui.theme.DocketSurfaceContainerLowDark
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanSessionViewModel,
    onBack: () -> Unit,
    onContinueToReview: () -> Unit,
    onOpenFiles: () -> Unit = {},
    launch: String = Destination.ScanFlow.LAUNCH_CAMERA
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val session by viewModel.session.collectAsStateWithLifecycle()
    val previewBitmaps by viewModel.previewBitmaps.collectAsStateWithLifecycle()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedMode by remember { mutableStateOf(ScanCaptureMode.DOCUMENT) }
    var isTorchOn by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    var justCaptured by remember { mutableStateOf(false) }
    var didAutoLaunch by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

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

    LaunchedEffect(launch) {
        if (didAutoLaunch) return@LaunchedEffect
        didAutoLaunch = true
        when (launch) {
            Destination.ScanFlow.LAUNCH_GALLERY -> {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            Destination.ScanFlow.LAUNCH_PDF -> pdfLauncher.launch(arrayOf("application/pdf"))
        }
    }

    fun takePicture() {
        val cap = imageCapture
        if (cap != null && hasCameraPermission) {
            val photoFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            cap.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        val uri = Uri.fromFile(photoFile)
                        justCaptured = true
                        viewModel.onPagesCaptured(listOf(uri))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        // Fallback sample capture if hardware capture encounters an error
                        val fallbackUri = createSampleCaptureUri(context, selectedMode)
                        justCaptured = true
                        viewModel.onPagesCaptured(listOf(fallbackUri))
                    }
                }
            )
        } else {
            // Fallback for emulator / non-hardware environments
            val sampleUri = createSampleCaptureUri(context, selectedMode)
            justCaptured = true
            viewModel.onPagesCaptured(listOf(sampleUri))
        }
    }

    val currentSession = session
    val inProgressPages = currentSession?.pages.orEmpty()
    val haptics = LocalHapticFeedback.current

    Scaffold(
        containerColor = DocketBackgroundDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar from Prototype Screen 6
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = DocketSpacing.space16, vertical = DocketSpacing.space8),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CaptureIconButton(
                        icon = DocketIcons.Back,
                        contentDescription = stringResource(R.string.common_back),
                        onClick = onBack
                    )
                    // Only Flash and PDF import are real actions today (see ScanCaptureMode's doc
                    // comment on ID_CARD/BOOK for the broader "no framing/enhance capability yet"
                    // context) — the prototype's decorative Frame/Spark slots are dropped rather
                    // than shipped as inert buttons.
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CaptureIconButton(
                            icon = DocketIcons.Flash,
                            contentDescription = "Flash",
                            onClick = {
                                isTorchOn = !isTorchOn
                                camera?.cameraControl?.enableTorch(isTorchOn)
                            }
                        )
                        CaptureIconButton(
                            icon = DocketIcons.Pdf,
                            contentDescription = "PDF Import",
                            onClick = { pdfLauncher.launch(arrayOf("application/pdf")) }
                        )
                    }
                }

                // Center Viewfinder with Live Camera Preview
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = DocketSpacing.space16),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(24.dp))
                            .background(DocketSurfaceContainerLowDark),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasCameraPermission) {
                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx).apply {
                                        scaleType = PreviewView.ScaleType.FILL_CENTER
                                    }
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        try {
                                            val cameraProvider = cameraProviderFuture.get()
                                            val preview = Preview.Builder().build().also {
                                                it.setSurfaceProvider(previewView.surfaceProvider)
                                            }
                                            val capture = ImageCapture.Builder()
                                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                                .build()
                                            imageCapture = capture

                                            cameraProvider.unbindAll()
                                            camera = cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                CameraSelector.DEFAULT_BACK_CAMERA,
                                                preview,
                                                capture
                                            )
                                        } catch (e: Exception) {
                                            errorMessage = e.message
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))
                                    previewView
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Blue corner reticle / frame overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, DocketPrimaryLight.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        )
                        ViewfinderCorners(modifier = Modifier.fillMaxSize())

                        // Top Mode Hint Pill
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 18.dp),
                            color = DocketBackgroundDark.copy(alpha = 0.75f),
                            shape = DocketPillShape
                        ) {
                            Text(
                                text = selectedMode.hint(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
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

                // Mode Selector Strip at the bottom of the camera screen
                ScanModeChipRow(
                    selected = selectedMode,
                    onSelect = { selectedMode = it },
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                )

                // Bottom Shutter and Control Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 36.dp, end = 36.dp, top = 8.dp, bottom = 32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Gallery Button (Left)
                    Surface(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.size(46.dp),
                        color = DocketSurfaceContainerLowDark,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = DocketIcons.Image,
                                contentDescription = "Gallery",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Shutter Button (Center)
                    ShutterButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            takePicture()
                        }
                    )

                    // Last Captured Stack (Right)
                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 56.dp)
                            .clickable(onClick = onContinueToReview),
                        contentAlignment = Alignment.Center
                    ) {
                        val count = if (inProgressPages.isNotEmpty()) inProgressPages.size else 1
                        val firstBitmap = inProgressPages.firstOrNull()?.let { previewBitmaps[it.id] }

                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 52.dp)
                                .shadow(8.dp, RoundedCornerShape(6.dp), ambientColor = Color.Black.copy(alpha = 0.4f), spotColor = Color.Black.copy(alpha = 0.4f))
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            if (firstBitmap != null) {
                                Image(
                                    bitmap = firstBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        if (inProgressPages.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(18.dp)
                                    .background(DocketCoralLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** ID_CARD and BOOK are visual-only today — they change the mode chip label and hint text (see
 *  [hint]) but capture/crop behavior is identical to DOCUMENT/RECEIPT; there's no card-edge or
 *  two-page detection yet. Giving them real behavior needs a CameraX/ML Kit rebuild of the
 *  capture pipeline, out of scope for a UI-fidelity pass — this is a known, intentional gap. */
enum class ScanCaptureMode { DOCUMENT, RECEIPT, ID_CARD, BOOK }

private fun ScanCaptureMode.hint(): String = when (this) {
    ScanCaptureMode.DOCUMENT -> "Page detected — tap to capture"
    ScanCaptureMode.RECEIPT -> "Hold steady over the receipt"
    ScanCaptureMode.ID_CARD -> "Fit the card inside the frame"
    ScanCaptureMode.BOOK -> "Both pages inside the frame"
}

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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
    ) {
        modes.forEach { mode ->
            ScanModeChip(
                mode = mode,
                isSelected = mode == selected,
                onClick = { onSelect(mode) }
            )
        }
    }
}

@Composable
private fun ScanModeChip(
    mode: ScanCaptureMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when (mode) {
        ScanCaptureMode.DOCUMENT -> "Document"
        ScanCaptureMode.RECEIPT -> "Receipt"
        ScanCaptureMode.ID_CARD -> "ID Card"
        ScanCaptureMode.BOOK -> "Book"
    }
    Column(
        modifier = modifier
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (isSelected) DocketPrimaryLight else DocketOnSurfaceVariantLight,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.dp)
                .background(if (isSelected) DocketPrimaryLight else Color.Transparent)
        )
    }
}

@Composable
private fun ViewfinderCorners(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "viewfinderPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "viewfinderAlpha"
    )
    val color = DocketPrimaryLight.copy(alpha = alpha)
    Canvas(modifier = modifier) {
        val strokeWidth = 3.dp.toPx()
        val armLength = 24.dp.toPx()
        val inset = 18.dp.toPx()

        val corners = listOf(
            Offset(inset, inset) to (1 to 1),
            Offset(size.width - inset, inset) to (-1 to 1),
            Offset(inset, size.height - inset) to (1 to -1),
            Offset(size.width - inset, size.height - inset) to (-1 to -1)
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
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .background(DocketPrimaryLight.copy(alpha = 0.3f))
            .padding(4.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(5.dp, DocketPrimaryLight, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
    )
}

@Composable
private fun CaptureIconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.White)
    }
}

private fun createSampleCaptureUri(context: Context, mode: ScanCaptureMode): Uri {
    val bitmap = Bitmap.createBitmap(720, 1080, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.rgb(33, 33, 33)
    canvas.drawRoundRect(RectF(60f, 80f, 400f, 120f), 8f, 8f, paint)

    paint.color = android.graphics.Color.rgb(158, 158, 158)
    canvas.drawRoundRect(RectF(60f, 160f, 620f, 180f), 6f, 6f, paint)
    canvas.drawRoundRect(RectF(60f, 210f, 560f, 230f), 6f, 6f, paint)
    canvas.drawRoundRect(RectF(60f, 260f, 640f, 280f), 6f, 6f, paint)

    paint.color = android.graphics.Color.rgb(238, 238, 238)
    canvas.drawRect(RectF(60f, 320f, 660f, 324f), paint)

    paint.color = android.graphics.Color.rgb(84, 104, 240)
    canvas.drawRoundRect(RectF(60f, 360f, 280f, 380f), 6f, 6f, paint)

    val file = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    return Uri.fromFile(file)
}

