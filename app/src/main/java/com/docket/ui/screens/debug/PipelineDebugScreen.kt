package com.docket.ui.screens.debug

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.navigation.compose.hiltViewModel
import com.docket.ui.components.PrimaryButton
import com.docket.ui.components.SecondaryButton
import com.docket.ui.theme.DocketSpacing

/**
 * BuildConfig.DEBUG-only — reachable from Settings → "Pipeline debug", never in a release build.
 * Picks any image, runs it through [com.docket.data.imaging.DocumentEnhancer]'s instrumented
 * stage functions, and shows every intermediate step with its own elapsed time so a quality
 * regression can be pinned to a specific stage instead of guessed at from the final output alone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PipelineDebugScreen(
    onBack: () -> Unit,
    viewModel: PipelineDebugViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var pendingKind by remember { mutableStateOf(PipelineKind.ENHANCE) }

    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) viewModel.onImagePicked(uri, pendingKind)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pipeline debug") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(DocketSpacing.space16),
            verticalArrangement = Arrangement.spacedBy(DocketSpacing.space16)
        ) {
            Text(
                text = "Pick any image and see every stage of the enhancement pipeline, timed " +
                    "individually — this build only, never shown to real users.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8), modifier = Modifier.fillMaxWidth()) {
                PrimaryButton(
                    text = "Pick → B & W",
                    onClick = {
                        pendingKind = PipelineKind.BLACK_AND_WHITE
                        pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = "Pick → Enhance",
                    onClick = {
                        pendingKind = PipelineKind.ENHANCE
                        pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            when (val current = state) {
                PipelineDebugState.Idle -> {
                    Text("No image picked yet.", style = MaterialTheme.typography.bodyMedium)
                }
                PipelineDebugState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = DocketSpacing.space32),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is PipelineDebugState.Failed -> {
                    Text(
                        text = "Failed: ${current.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is PipelineDebugState.Loaded -> {
                    Column(verticalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
                        Text(
                            text = "Source ${current.sourceWidth}x${current.sourceHeight} · " +
                                "${current.kind.name} · total ${current.totalMs}ms",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(DocketSpacing.space8)) {
                            SecondaryButton(
                                text = "Re-run as B & W",
                                onClick = { viewModel.rerun(PipelineKind.BLACK_AND_WHITE) }
                            )
                            SecondaryButton(
                                text = "Re-run as Enhance",
                                onClick = { viewModel.rerun(PipelineKind.ENHANCE) }
                            )
                        }
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(DocketSpacing.space16),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        items(current.stages) { stage ->
                            Column(verticalArrangement = Arrangement.spacedBy(DocketSpacing.space4)) {
                                Text(
                                    text = "${stage.label} — ${stage.elapsedMs}ms",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Image(
                                    bitmap = stage.bitmap.asImageBitmap(),
                                    contentDescription = stage.label,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        item { Spacer(modifier = Modifier.height(DocketSpacing.space32)) }
                    }
                }
            }
        }
    }
}
