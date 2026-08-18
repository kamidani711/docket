package com.docket.ui.screens.debug

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.data.imaging.DocumentEnhancer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PipelineKind { BLACK_AND_WHITE, ENHANCE }

sealed interface PipelineDebugState {
    data object Idle : PipelineDebugState
    data object Loading : PipelineDebugState
    data class Loaded(
        val sourceWidth: Int,
        val sourceHeight: Int,
        val stages: List<DocumentEnhancer.PipelineStage>,
        val totalMs: Long,
        val kind: PipelineKind
    ) : PipelineDebugState
    data class Failed(val message: String) : PipelineDebugState
}

/**
 * QA tool for the "why does this scan look wrong" question — never shown to real users (only
 * reachable from Settings' `if (BuildConfig.DEBUG)` block). Picks any image via the system photo
 * picker, decodes it at full resolution deliberately (no downsampling before handing it to the
 * pipeline) so timing here reflects a real capture's actual cost, including
 * `DocumentEnhancer`'s own internal downscale step — not a pre-shrunk stand-in for it.
 */
@HiltViewModel
class PipelineDebugViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow<PipelineDebugState>(PipelineDebugState.Idle)
    val state: StateFlow<PipelineDebugState> = _state.asStateFlow()

    private var lastSourceFile: File? = null

    fun onImagePicked(uri: Uri, kind: PipelineKind) {
        viewModelScope.launch {
            _state.value = PipelineDebugState.Loading
            try {
                val file = withContext(Dispatchers.IO) { copyToDebugFile(uri) }
                lastSourceFile = file
                runPipeline(file, kind)
            } catch (t: Throwable) {
                _state.value = PipelineDebugState.Failed(t.message ?: "Failed to load image")
            }
        }
    }

    /** Re-runs the other pipeline (B&W vs Enhance) against the same already-picked image,
     *  without re-launching the picker — makes comparing the two on one test photo fast. */
    fun rerun(kind: PipelineKind) {
        val file = lastSourceFile ?: return
        viewModelScope.launch {
            _state.value = PipelineDebugState.Loading
            runPipeline(file, kind)
        }
    }

    private suspend fun runPipeline(file: File, kind: PipelineKind) {
        try {
            val result = withContext(Dispatchers.Default) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    ?: error("Could not decode the picked image")
                val stages = when (kind) {
                    PipelineKind.BLACK_AND_WHITE -> DocumentEnhancer.blackAndWhiteDebugStages(bitmap)
                    PipelineKind.ENHANCE -> DocumentEnhancer.enhanceDebugStages(bitmap)
                }
                Triple(bitmap.width, bitmap.height, stages)
            }
            _state.value = PipelineDebugState.Loaded(
                sourceWidth = result.first,
                sourceHeight = result.second,
                stages = result.third,
                totalMs = result.third.sumOf { it.elapsedMs },
                kind = kind
            )
        } catch (t: Throwable) {
            _state.value = PipelineDebugState.Failed(t.message ?: "Pipeline failed")
        }
    }

    private fun copyToDebugFile(uri: Uri): File {
        val dir = File(appContext.cacheDir, "pipeline_debug").apply { mkdirs() }
        val destFile = File(dir, "${UUID.randomUUID()}.jpg")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not read the picked image")
        return destFile
    }
}
