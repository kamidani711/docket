package com.docket.ui.screens.scan

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.common.PremiumRequiredException
import com.docket.domain.common.Result
import com.docket.domain.model.Document
import com.docket.domain.model.DocumentSort
import com.docket.domain.model.DocumentTypeFilter
import com.docket.domain.model.ExportFormat
import com.docket.domain.model.Folder
import com.docket.domain.model.PremiumFeature
import com.docket.domain.model.Quad
import com.docket.domain.model.ScanFilter
import com.docket.domain.model.ScanPage
import com.docket.domain.model.ScanSession
import com.docket.domain.repository.AnalyticsRepository
import com.docket.domain.repository.BackgroundOcrScheduler
import com.docket.domain.repository.DocumentRepository
import com.docket.domain.repository.ImageProcessor
import com.docket.domain.repository.PdfWriter
import com.docket.domain.repository.PremiumRepository
import com.docket.domain.repository.PreviewCache
import com.docket.domain.repository.ScanSessionRepository
import com.docket.domain.repository.SettingsRepository
import com.docket.domain.repository.TextRecognizer
import com.docket.domain.usecase.CreateFolderUseCase
import com.docket.domain.usecase.ExportScanSessionUseCase
import com.docket.domain.usecase.GenerateFilterSwatchesUseCase
import com.docket.domain.usecase.GeneratePagePreviewUseCase
import com.docket.domain.usecase.LoadCropBaseImageUseCase
import com.docket.domain.usecase.SuggestDocumentNameUseCase
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * Plain JVM test, same conventions as [com.docket.ui.screens.library.LibraryViewModelTest] and
 * [com.docket.ui.screens.documentdetail.DocumentDetailViewModelTest]. [ExportScanSessionUseCase]
 * needs a real `Context` in its constructor — [mock] stands in for it *purely for wiring*, since
 * every test here deliberately avoids calling [ScanSessionViewModel.save] (the only method that
 * would actually touch it; see [DocumentDetailViewModelTest]'s class doc for the same call).
 *
 * The `init` block auto-generates a preview for every page in whatever session is active (and a
 * suggested name, if one isn't set yet) — [FakeImageProcessor]/[FakePreviewCache]/
 * [FakeTextRecognizer] exist mainly so that machinery has something safe to run against, not
 * because these tests assert on rendered pixels.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScanSessionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val createdViewModels = mutableListOf<ViewModel>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        createdViewModels.forEach { it.viewModelScope.cancel() }
        Dispatchers.resetMain()
    }

    @Test
    fun `session mirrors whatever the repository reports`() = runTest {
        val session = sampleSession(pages = listOf(samplePage("p1")))
        val scanSessionRepository = FakeScanSessionRepository(session)
        val viewModel = viewModel(scanSessionRepository = scanSessionRepository)

        backgroundScope.launch { viewModel.session.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(session.id, viewModel.session.value?.id)
        assertEquals(1, viewModel.session.value?.pages?.size)
    }

    @Test
    fun `reorderPages delegates to the repository with the current session id`() = runTest {
        val session = sampleSession(pages = listOf(samplePage("p1"), samplePage("p2")))
        val scanSessionRepository = FakeScanSessionRepository(session)
        val viewModel = viewModel(scanSessionRepository = scanSessionRepository)
        backgroundScope.launch { viewModel.session.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.reorderPages(listOf("p2", "p1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(session.id to listOf("p2", "p1"), scanSessionRepository.reorderedCalls.single())
    }

    @Test
    fun `reorderPages with no active session touches the repository not at all`() = runTest {
        val scanSessionRepository = FakeScanSessionRepository(initialSession = null)
        val viewModel = viewModel(scanSessionRepository = scanSessionRepository)
        backgroundScope.launch { viewModel.session.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.reorderPages(listOf("p2", "p1"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(scanSessionRepository.reorderedCalls.isEmpty())
    }

    @Test
    fun `deletePage removes it from the repository and deletes the original file`() = runTest {
        val tempFile = File.createTempFile("docket_test_page", ".jpg").apply { deleteOnExit() }
        val page = samplePage("p1", originalImagePath = tempFile.absolutePath)
        val session = sampleSession(pages = listOf(page))
        val scanSessionRepository = FakeScanSessionRepository(session)
        val viewModel = viewModel(scanSessionRepository = scanSessionRepository)
        backgroundScope.launch { viewModel.session.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(tempFile.exists())
        viewModel.deletePage(page)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("p1"), scanSessionRepository.deletedPageIds)
        assertFalse(tempFile.exists())
    }

    @Test
    fun `updateCorners delegates to the repository`() = runTest {
        val session = sampleSession(pages = listOf(samplePage("p1")))
        val scanSessionRepository = FakeScanSessionRepository(session)
        val viewModel = viewModel(scanSessionRepository = scanSessionRepository)
        backgroundScope.launch { viewModel.session.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val newCorners = Quad.FullFrame
        viewModel.updateCorners("p1", newCorners)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("p1" to newCorners, scanSessionRepository.updatedCorners.single())
    }

    @Test
    fun `rotatePage sends the delta, not an absolute angle`() = runTest {
        val session = sampleSession(pages = listOf(samplePage("p1")))
        val scanSessionRepository = FakeScanSessionRepository(session)
        val viewModel = viewModel(scanSessionRepository = scanSessionRepository)
        backgroundScope.launch { viewModel.session.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.rotatePage("p1", 90)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("p1" to 90, scanSessionRepository.rotateCalls.single())
    }

    @Test
    fun `setFilter delegates to the repository`() = runTest {
        val session = sampleSession(pages = listOf(samplePage("p1")))
        val scanSessionRepository = FakeScanSessionRepository(session)
        val viewModel = viewModel(scanSessionRepository = scanSessionRepository)
        backgroundScope.launch { viewModel.session.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setFilter("p1", ScanFilter.GREYSCALE)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("p1" to ScanFilter.GREYSCALE, scanSessionRepository.filterCalls.single())
    }

    @Test
    fun `discardSession deletes every page file and clears the session`() = runTest {
        val fileA = File.createTempFile("docket_test_a", ".jpg").apply { deleteOnExit() }
        val fileB = File.createTempFile("docket_test_b", ".jpg").apply { deleteOnExit() }
        val session = sampleSession(
            pages = listOf(
                samplePage("p1", originalImagePath = fileA.absolutePath),
                samplePage("p2", originalImagePath = fileB.absolutePath)
            )
        )
        val scanSessionRepository = FakeScanSessionRepository(session)
        val viewModel = viewModel(scanSessionRepository = scanSessionRepository)
        backgroundScope.launch { viewModel.session.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.discardSession()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(fileA.exists())
        assertFalse(fileB.exists())
        assertEquals(listOf(session.id), scanSessionRepository.clearedSessionIds)
    }

    @Test
    fun `discardSession with no active session is a no-op`() = runTest {
        val scanSessionRepository = FakeScanSessionRepository(initialSession = null)
        val viewModel = viewModel(scanSessionRepository = scanSessionRepository)
        backgroundScope.launch { viewModel.session.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.discardSession()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(scanSessionRepository.clearedSessionIds.isEmpty())
    }

    @Test
    fun `createFolder at the free limit for a non-premium user emits PremiumRequired`() = runTest {
        val documentRepository = FakeDocumentRepository(folderCount = 3) // FREE_FOLDER_LIMIT
        val viewModel = viewModel(documentRepository = documentRepository, isPremium = false)

        val eventDeferred = async { viewModel.uiEvents.first() }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createFolder("One Too Many")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(eventDeferred.await() is ScanUiEvent.PremiumRequired)
        assertTrue(documentRepository.createdFolders.isEmpty())
    }

    @Test
    fun `createFolder under the free limit creates it without emitting PremiumRequired`() = runTest {
        val documentRepository = FakeDocumentRepository(folderCount = 0)
        val viewModel = viewModel(documentRepository = documentRepository, isPremium = false)

        var sawPremiumRequired = false
        backgroundScope.launch {
            viewModel.uiEvents.collect { if (it is ScanUiEvent.PremiumRequired) sawPremiumRequired = true }
        }

        viewModel.createFolder("Receipts")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(sawPremiumRequired)
        assertEquals(listOf("Receipts" to null), documentRepository.createdFolders)
    }

    @Test
    fun `loadFilterSwatches populates swatches for every filter`() = runTest {
        val page = samplePage("p1")
        val session = sampleSession(pages = listOf(page))
        val viewModel = viewModel(scanSessionRepository = FakeScanSessionRepository(session))
        backgroundScope.launch { viewModel.session.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadFilterSwatches(page)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(ScanFilter.entries.toSet(), viewModel.filterSwatches.value.keys)
    }

    @Test
    fun `loadCropBaseImage then clearCropBaseImage leaves it null`() = runTest {
        val page = samplePage("p1")
        val session = sampleSession(pages = listOf(page))
        val viewModel = viewModel(scanSessionRepository = FakeScanSessionRepository(session))
        backgroundScope.launch { viewModel.session.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.loadCropBaseImage(page)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.cropBaseImage.value != null)

        viewModel.clearCropBaseImage()
        assertTrue(viewModel.cropBaseImage.value == null)
    }

    @Test
    fun `an active session with pages gets a preview generated automatically`() = runTest {
        val page = samplePage("p1")
        val session = sampleSession(pages = listOf(page))
        val viewModel = viewModel(scanSessionRepository = FakeScanSessionRepository(session))

        backgroundScope.launch { viewModel.session.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.previewBitmaps.value.containsKey("p1"))
    }

    // ---- Fixture wiring ----

    private fun viewModel(
        scanSessionRepository: ScanSessionRepository = FakeScanSessionRepository(null),
        documentRepository: DocumentRepository = FakeDocumentRepository(),
        settingsRepository: SettingsRepository = FakeSettingsRepository(),
        analyticsRepository: AnalyticsRepository = FakeAnalyticsRepository(),
        isPremium: Boolean = false,
        imageProcessor: ImageProcessor = FakeImageProcessor(),
        previewCache: PreviewCache = FakePreviewCache(),
        textRecognizer: TextRecognizer = FakeTextRecognizer(),
        pdfWriter: PdfWriter = FakePdfWriter(),
        backgroundOcrScheduler: BackgroundOcrScheduler = FakeBackgroundOcrScheduler()
    ): ScanSessionViewModel {
        val premiumRepository = FakePremiumRepository(isPremium)
        return ScanSessionViewModel(
            appContext = mock(Context::class.java),
            scanSessionRepository = scanSessionRepository,
            documentRepository = documentRepository,
            settingsRepository = settingsRepository,
            generatePagePreviewUseCase = GeneratePagePreviewUseCase(imageProcessor, previewCache),
            generateFilterSwatchesUseCase = GenerateFilterSwatchesUseCase(imageProcessor),
            loadCropBaseImageUseCase = LoadCropBaseImageUseCase(imageProcessor),
            suggestDocumentNameUseCase = SuggestDocumentNameUseCase(imageProcessor, textRecognizer, documentRepository),
            exportScanSessionUseCase = ExportScanSessionUseCase(
                imageProcessor = imageProcessor,
                pdfWriter = pdfWriter,
                documentRepository = documentRepository,
                scanSessionRepository = scanSessionRepository,
                backgroundOcrScheduler = backgroundOcrScheduler,
                analyticsRepository = analyticsRepository,
                appContext = mock(Context::class.java)
            ),
            createFolderUseCase = CreateFolderUseCase(documentRepository, premiumRepository),
            analyticsRepository = analyticsRepository
        ).also { createdViewModels += it }
    }

    private fun sampleSession(
        id: String = "session-1",
        pages: List<ScanPage> = emptyList(),
        suggestedName: String? = "Existing name"
    ) = ScanSession(id = id, pages = pages, suggestedName = suggestedName)

    private fun samplePage(id: String, originalImagePath: String = "/tmp/$id.jpg") = ScanPage(
        id = id,
        originalImagePath = originalImagePath,
        orderIndex = 0
    )

    // ---- Fakes ----

    private class FakeAnalyticsRepository : AnalyticsRepository {
        val loggedEvents = mutableListOf<com.docket.domain.model.AnalyticsEventType>()
        override suspend fun logEvent(type: com.docket.domain.model.AnalyticsEventType, detail: String?) {
            loggedEvents += type
        }
        override fun observeSummary() = flowOf(com.docket.domain.model.AnalyticsSummary())
        override suspend fun clearAll() = Unit
    }

    private class FakePremiumRepository(isPremium: Boolean) : PremiumRepository {
        override val isPremium: Flow<Boolean> = flowOf(isPremium)
        override fun isFeatureUnlocked(feature: PremiumFeature): Flow<Boolean> = isPremium
        override suspend fun setPremiumUnlocked(unlocked: Boolean) = Unit
    }

    private class FakeSettingsRepository : SettingsRepository {
        override val defaultExportFormat: Flow<ExportFormat> = flowOf(ExportFormat.PDF)
        override suspend fun setDefaultExportFormat(format: ExportFormat) = Unit
        override val defaultLibrarySort: Flow<DocumentSort> = flowOf(DocumentSort.DATE)
        override suspend fun setDefaultLibrarySort(sort: DocumentSort) = Unit
        override val appLockEnabled: Flow<Boolean> = flowOf(false)
        override suspend fun setAppLockEnabled(enabled: Boolean) = Unit
    }

    private class FakeImageProcessor : ImageProcessor {
        override suspend fun readDimensions(imagePath: String): Pair<Int, Int> = 100 to 100
        override suspend fun decodeDownsampled(imagePath: String, maxDimension: Int): Bitmap = mock(Bitmap::class.java)
        override suspend fun decodeFullResolution(imagePath: String): Bitmap = mock(Bitmap::class.java)
        override fun applyCrop(bitmap: Bitmap, corners: Quad): Bitmap = bitmap
        override fun applyRotation(bitmap: Bitmap, degrees: Int): Bitmap = bitmap
        override fun applyFilter(bitmap: Bitmap, filter: ScanFilter): Bitmap = bitmap
        override suspend fun saveJpeg(bitmap: Bitmap, destinationPath: String, quality: Int) = Unit
    }

    private class FakePreviewCache : PreviewCache {
        private val cache = mutableMapOf<String, Bitmap>()
        override fun get(key: String): Bitmap? = cache[key]
        override fun put(key: String, bitmap: Bitmap) {
            cache[key] = bitmap
        }
        override fun keyFor(pageId: String, corners: Quad, rotationDegrees: Int, filter: ScanFilter): String =
            "$pageId:$corners:$rotationDegrees:$filter"
    }

    private class FakeTextRecognizer : TextRecognizer {
        override suspend fun recognizeText(bitmap: Bitmap): String = ""
    }

    private class FakePdfWriter : PdfWriter {
        override suspend fun writePdf(pageCount: Int, destination: File, renderPage: suspend (Int) -> Bitmap) = Unit
        override suspend fun renderThumbnail(pdfFile: File, maxDimension: Int): Bitmap = mock(Bitmap::class.java)
        override suspend fun renderPage(pdfFile: File, pageIndex: Int, maxDimension: Int): Bitmap = mock(Bitmap::class.java)
        override suspend fun writeExportPdf(request: com.docket.domain.model.PdfExportRequest) =
            throw UnsupportedOperationException("not exercised by these tests — see the class doc")
    }

    private class FakeBackgroundOcrScheduler : BackgroundOcrScheduler {
        override fun scheduleOcr(documentId: Long) = Unit
    }

    private class FakeScanSessionRepository(initialSession: ScanSession?) : ScanSessionRepository {
        private val sessionFlow = MutableStateFlow(initialSession)
        val reorderedCalls = mutableListOf<Pair<String, List<String>>>()
        val updatedCorners = mutableListOf<Pair<String, Quad>>()
        val rotateCalls = mutableListOf<Pair<String, Int>>()
        val filterCalls = mutableListOf<Pair<String, ScanFilter>>()
        val deletedPageIds = mutableListOf<String>()
        val clearedSessionIds = mutableListOf<String>()

        override fun observeSession(): Flow<ScanSession?> = sessionFlow

        override suspend fun startImageSession(originalImagePaths: List<String>): String {
            val id = "new-session"
            sessionFlow.value = ScanSession(
                id = id,
                pages = originalImagePaths.mapIndexed { index, path ->
                    ScanPage(id = "p$index", originalImagePath = path, orderIndex = index)
                }
            )
            return id
        }

        override suspend fun startPdfImportSession(pdfPath: String, pageCount: Int): String {
            val id = "new-pdf-session"
            sessionFlow.value = ScanSession(id = id, importedPdfPath = pdfPath, importedPdfPageCount = pageCount)
            return id
        }

        override suspend fun appendPages(sessionId: String, originalImagePaths: List<String>) = Unit

        override suspend fun updateCorners(pageId: String, corners: Quad) {
            updatedCorners += pageId to corners
            sessionFlow.update { s -> s?.copy(pages = s.pages.map { if (it.id == pageId) it.copy(corners = corners) else it }) }
        }

        override suspend fun rotatePage(pageId: String, deltaDegrees: Int) {
            rotateCalls += pageId to deltaDegrees
        }

        override suspend fun updateFilter(pageId: String, filter: ScanFilter) {
            filterCalls += pageId to filter
        }

        override suspend fun reorderPages(sessionId: String, orderedPageIds: List<String>) {
            reorderedCalls += sessionId to orderedPageIds
        }

        override suspend fun deletePage(pageId: String) {
            deletedPageIds += pageId
            sessionFlow.update { s -> s?.copy(pages = s.pages.filterNot { it.id == pageId }) }
        }

        override suspend fun replacePageImage(pageId: String, newOriginalImagePath: String) = Unit

        override suspend fun updateSuggestedName(sessionId: String, name: String) {
            sessionFlow.update { s -> if (s?.id == sessionId) s.copy(suggestedName = name) else s }
        }

        override suspend fun clearSession(sessionId: String) {
            clearedSessionIds += sessionId
            sessionFlow.value = null
        }
    }

    private class FakeDocumentRepository(private val folderCount: Int = 0) : DocumentRepository {
        val createdFolders = mutableListOf<Pair<String, Long?>>()
        private var nextFolderId = 100L

        override fun observeFolders(parentFolderId: Long?): Flow<List<Folder>> = flowOf(emptyList())
        override suspend fun getFolder(folderId: Long): Folder? = null
        override suspend fun createFolder(name: String, parentFolderId: Long?): Long {
            createdFolders += name to parentFolderId
            return nextFolderId++
        }
        override suspend fun folderCount(): Int = folderCount
        override fun observeFolderDocumentCounts(): Flow<Map<Long, Int>> = flowOf(emptyMap())
        override fun observeLibrary(
            folderId: Long?,
            typeFilter: DocumentTypeFilter,
            sortBy: DocumentSort
        ): Flow<List<Document>> = flowOf(emptyList())
        override fun observeDocument(documentId: Long): Flow<Document?> = flowOf(null)
        override suspend fun getDocument(documentId: Long): Document? = null
        override suspend fun getTitlesCreatedBetween(dayStart: Long, dayEnd: Long): List<String> = emptyList()
        override fun observeDeletedDocuments(): Flow<List<Document>> = flowOf(emptyList())
        override suspend fun softDeleteDocuments(documentIds: List<Long>) = Unit
        override suspend fun restoreDocument(documentId: Long) = Unit
        override suspend fun moveDocuments(documentIds: List<Long>, folderId: Long?) = Unit
        override suspend fun renameDocument(documentId: Long, title: String) = Unit
        override suspend fun getPurgeableDocuments(cutoff: Long): List<Document> = emptyList()
        override suspend fun hardDeleteDocument(documentId: Long) = Unit
        override suspend fun insertDocument(
            title: String,
            folderId: Long?,
            format: ExportFormat,
            pageCount: Int,
            pdfFilePath: String?,
            pageImagePaths: List<String>,
            thumbnailPath: String?,
            sizeBytes: Long
        ): Long = 0L
    }
}
