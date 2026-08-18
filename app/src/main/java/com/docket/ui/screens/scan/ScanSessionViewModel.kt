package com.docket.ui.screens.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.common.PremiumRequiredException
import com.docket.domain.common.Result
import com.docket.domain.model.AnalyticsEventType
import com.docket.domain.model.ExportFormat
import com.docket.domain.model.ExportProgress
import com.docket.domain.model.Folder
import com.docket.domain.model.Quad
import com.docket.domain.model.ScanFilter
import com.docket.domain.model.ScanPage
import com.docket.domain.model.ScanSession
import com.docket.domain.repository.AnalyticsRepository
import com.docket.domain.repository.DocumentRepository
import com.docket.domain.repository.ScanSessionRepository
import com.docket.domain.repository.SettingsRepository
import com.docket.domain.usecase.CreateFolderUseCase
import com.docket.domain.usecase.ExportScanSessionUseCase
import com.docket.domain.usecase.GenerateFilterSwatchesUseCase
import com.docket.domain.usecase.GeneratePagePreviewUseCase
import com.docket.domain.usecase.LoadCropBaseImageUseCase
import com.docket.domain.usecase.SuggestDocumentNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

sealed interface ScanUiEvent {
    data class SaveComplete(val documentId: Long) : ScanUiEvent
    data class Error(val message: String) : ScanUiEvent
    data object PremiumRequired : ScanUiEvent
}

/**
 * Shared by [com.docket.ui.screens.scan.ScanScreen] and
 * [com.docket.ui.screens.review.ReviewScreen] — scoped to their shared nav sub-graph (see
 * `DocketNavHost`) so both screens see the same in-progress session.
 *
 * [session] does not hold any state of its own beyond what [ScanSessionRepository] already
 * persists to Room — it's a direct mirror of the DB. That's the entire process-death recovery
 * mechanism: every edit is already a disk write, so there's nothing extra to save or restore.
 * [previewBitmaps] is the one piece of state here that's legitimately ephemeral: derived,
 * regenerable from disk at any time, and deliberately never persisted.
 */
@HiltViewModel
class ScanSessionViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val scanSessionRepository: ScanSessionRepository,
    private val documentRepository: DocumentRepository,
    private val settingsRepository: SettingsRepository,
    private val generatePagePreviewUseCase: GeneratePagePreviewUseCase,
    private val generateFilterSwatchesUseCase: GenerateFilterSwatchesUseCase,
    private val loadCropBaseImageUseCase: LoadCropBaseImageUseCase,
    private val suggestDocumentNameUseCase: SuggestDocumentNameUseCase,
    private val exportScanSessionUseCase: ExportScanSessionUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val analyticsRepository: AnalyticsRepository
) : ViewModel() {

    val session: StateFlow<ScanSession?> = scanSessionRepository.observeSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Save-time folder picker only offers top-level folders — creating/picking a subfolder
    // happens from the Library screen itself, where there's folder context to nest under.
    val folders: StateFlow<List<Folder>> = documentRepository.observeFolders(parentFolderId = null)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val defaultExportFormat: StateFlow<ExportFormat> = settingsRepository.defaultExportFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExportFormat.PDF)

    private val _previewBitmaps = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val previewBitmaps: StateFlow<Map<String, Bitmap>> = _previewBitmaps.asStateFlow()

    private val _exportProgress = MutableStateFlow<ExportProgress?>(null)
    val exportProgress: StateFlow<ExportProgress?> = _exportProgress.asStateFlow()

    private val _filterSwatches = MutableStateFlow<Map<ScanFilter, Bitmap>>(emptyMap())
    val filterSwatches: StateFlow<Map<ScanFilter, Bitmap>> = _filterSwatches.asStateFlow()
    private var filterSwatchJob: Job? = null

    private val _cropBaseImage = MutableStateFlow<Bitmap?>(null)
    val cropBaseImage: StateFlow<Bitmap?> = _cropBaseImage.asStateFlow()
    private var cropBaseJob: Job? = null

    private val _uiEvents = MutableSharedFlow<ScanUiEvent>()
    val uiEvents: SharedFlow<ScanUiEvent> = _uiEvents.asSharedFlow()

    // Bounds how many pages decode concurrently when a session with many pages first loads
    // (e.g. recovering a 20-page draft after process death) — see the perf write-up.
    private val previewSemaphore = Semaphore(PREVIEW_CONCURRENCY)
    private val activePreviewKeys = mutableSetOf<String>()
    // The most recently *requested* preview key per page — checked when a decode finishes so an
    // edit made while that decode was still running (rotate again, adjust crop again...) can't
    // have its result clobbered by an older decode completing after it. See ensurePreview.
    private val latestPreviewKeyForPage = mutableMapOf<String, String>()
    private var lastSeenSessionId: String? = null

    init {
        viewModelScope.launch {
            session.collectLatest { current ->
                if (current?.id != lastSeenSessionId) {
                    activePreviewKeys.clear()
                    latestPreviewKeyForPage.clear()
                    lastSeenSessionId = current?.id
                }
                current?.pages?.forEach { ensurePreview(it) }
                if (current != null && current.suggestedName == null) {
                    if (!current.isPdfImport && current.pages.isNotEmpty()) {
                        launch { suggestName(current) }
                    } else if (current.isPdfImport) {
                        // No page image to run OCR on without rendering the PDF first — still
                        // gets a real name (not a blank field) via the same date/counter
                        // fallback OCR falls back to.
                        launch { suggestDefaultNameForPdfImport(current) }
                    }
                }
            }
        }
    }

    // ---- Capture / import ----

    fun onPagesCaptured(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val paths = uris.map { copyToCaptures(it) }
            val current = session.value
            if (current == null || current.isPdfImport) {
                scanSessionRepository.startImageSession(paths)
                analyticsRepository.logEvent(AnalyticsEventType.SCAN_STARTED)
            } else {
                scanSessionRepository.appendPages(current.id, paths)
            }
        }
    }

    fun onPdfPicked(uri: Uri) {
        viewModelScope.launch {
            runCatching { copyPdfAndCountPages(uri) }
                .onSuccess { (path, pageCount) ->
                    scanSessionRepository.startPdfImportSession(path, pageCount)
                    analyticsRepository.logEvent(AnalyticsEventType.SCAN_STARTED)
                }
                .onFailure { _uiEvents.emit(ScanUiEvent.Error("Couldn't import that PDF.")) }
        }
    }

    // ---- Review edits ----

    fun reorderPages(orderedPageIds: List<String>) {
        val sessionId = session.value?.id ?: return
        viewModelScope.launch { scanSessionRepository.reorderPages(sessionId, orderedPageIds) }
    }

    fun deletePage(page: ScanPage) {
        viewModelScope.launch {
            scanSessionRepository.deletePage(page.id)
            File(page.originalImagePath).delete()
        }
    }

    fun updateCorners(pageId: String, corners: Quad) {
        viewModelScope.launch { scanSessionRepository.updateCorners(pageId, corners) }
    }

    /** Loads the unrotated, unfiltered base image the crop editor draws its overlay on top of. */
    fun loadCropBaseImage(page: ScanPage) {
        cropBaseJob?.cancel()
        _cropBaseImage.value = null
        cropBaseJob = viewModelScope.launch {
            _cropBaseImage.value = loadCropBaseImageUseCase(page, CROP_EDITOR_MAX_DIMENSION)
        }
    }

    fun clearCropBaseImage() {
        cropBaseJob?.cancel()
        _cropBaseImage.value = null
    }

    // Takes a pageId, not a ScanPage — deliberately, so there's no `page.rotationDegrees` lying
    // around to read from a possibly-stale snapshot. The repository applies deltaDegrees as an
    // atomic SQL increment on whatever's actually stored, so rapid taps can't race each other
    // and lose an increment. See ScanPageDao.rotateBy for the full story.
    fun rotatePage(pageId: String, deltaDegrees: Int) {
        viewModelScope.launch {
            scanSessionRepository.rotatePage(pageId, deltaDegrees)
        }
    }

    fun setFilter(pageId: String, filter: ScanFilter) {
        viewModelScope.launch { scanSessionRepository.updateFilter(pageId, filter) }
    }

    /** Refreshes the filter-picker swatches for [page] — call when it becomes the selected page
     *  and whenever its crop/rotation changes underneath the picker. */
    fun loadFilterSwatches(page: ScanPage) {
        filterSwatchJob?.cancel()
        filterSwatchJob = viewModelScope.launch {
            _filterSwatches.value = generateFilterSwatchesUseCase(page, FILTER_SWATCH_MAX_DIMENSION)
        }
    }

    fun onRetakeResult(page: ScanPage, newImageUri: Uri) {
        viewModelScope.launch {
            val newPath = copyToCaptures(newImageUri)
            scanSessionRepository.replacePageImage(page.id, newPath)
            File(page.originalImagePath).delete()
        }
    }

    // ---- Save ----

    fun save(title: String, folderId: Long?, format: ExportFormat) {
        val current = session.value ?: return
        viewModelScope.launch {
            // Falls all the way back to the same date/counter naming the OCR path uses — never
            // a placeholder like "Untitled scan" the user wouldn't recognise as theirs.
            val resolvedTitle = title.ifBlank {
                current.suggestedName ?: suggestDocumentNameUseCase.suggestDefaultName()
            }
            _exportProgress.value = ExportProgress.Idle
            val result = exportScanSessionUseCase(current, resolvedTitle, folderId, format) { progress ->
                _exportProgress.value = progress
            }
            _exportProgress.value = null
            when (result) {
                is Result.Success -> _uiEvents.emit(ScanUiEvent.SaveComplete(result.data))
                is Result.Error -> _uiEvents.emit(
                    ScanUiEvent.Error(result.message ?: "Couldn't save this document.")
                )
                Result.Loading -> Unit
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val result = createFolderUseCase(name, parentFolderId = null)
            if (result is Result.Error && result.throwable is PremiumRequiredException) {
                _uiEvents.emit(ScanUiEvent.PremiumRequired)
            }
        }
    }

    fun discardSession() {
        val current = session.value ?: return
        viewModelScope.launch {
            current.pages.forEach { File(it.originalImagePath).delete() }
            // PDF-import sessions have no `pages` — the copied PDF (and its containing
            // directory) is the thing to clean up instead.
            current.importedPdfPath?.let { File(it).parentFile?.deleteRecursively() }
            scanSessionRepository.clearSession(current.id)
        }
    }

    // ---- Previews ----

    /** No-op if this exact page/corners/rotation/filter combination is already cached or has
     *  a render already in flight — cheap to call on every session emission. */
    private fun ensurePreview(page: ScanPage) {
        val key = previewKeyOf(page)
        latestPreviewKeyForPage[page.id] = key
        if (!activePreviewKeys.add(key)) return
        viewModelScope.launch {
            previewSemaphore.withPermit {
                val bitmap = generatePagePreviewUseCase(page, PREVIEW_MAX_DIMENSION)
                // A newer edit (another rotate/crop/filter tap) may have superseded this decode
                // while it was in flight. If so, this result is stale — discard it rather than
                // let an out-of-order completion overwrite the newer one still in progress.
                if (latestPreviewKeyForPage[page.id] == key) {
                    _previewBitmaps.update { it + (page.id to bitmap) }
                } else {
                    bitmap.recycle()
                }
            }
        }
    }

    private fun previewKeyOf(page: ScanPage): String =
        "${page.id}:${page.corners}:${page.rotationDegrees}:${page.filter}"

    private suspend fun suggestName(currentSession: ScanSession) {
        val name = suggestDocumentNameUseCase(currentSession.pages.first().originalImagePath)
        scanSessionRepository.updateSuggestedName(currentSession.id, name)
    }

    private suspend fun suggestDefaultNameForPdfImport(currentSession: ScanSession) {
        val name = suggestDocumentNameUseCase.suggestDefaultName()
        scanSessionRepository.updateSuggestedName(currentSession.id, name)
    }

    // ---- Storage ----

    private suspend fun copyToCaptures(uri: Uri): String = withContext(Dispatchers.IO) {
        val dir = File(appContext.filesDir, "scan_captures").apply { mkdirs() }
        val destFile = File(dir, "${UUID.randomUUID()}.jpg")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not read picked image")
        destFile.absolutePath
    }

    private suspend fun copyPdfAndCountPages(uri: Uri): Pair<String, Int> = withContext(Dispatchers.IO) {
        val documentDir = File(appContext.filesDir, "documents/${UUID.randomUUID()}").apply { mkdirs() }
        val destFile = File(documentDir, "document.pdf")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not read picked PDF")

        val pageCount = ParcelFileDescriptor.open(destFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { it.pageCount }
        }
        destFile.absolutePath to pageCount
    }

    private companion object {
        const val PREVIEW_MAX_DIMENSION = 800
        const val PREVIEW_CONCURRENCY = 2
        const val FILTER_SWATCH_MAX_DIMENSION = 200
        const val CROP_EDITOR_MAX_DIMENSION = 1000
    }
}
