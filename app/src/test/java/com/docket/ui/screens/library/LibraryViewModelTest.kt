package com.docket.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.model.Document
import com.docket.domain.model.DocumentSort
import com.docket.domain.model.DocumentTypeFilter
import com.docket.domain.model.ExportFormat
import com.docket.domain.model.Folder
import com.docket.domain.model.PremiumFeature
import com.docket.domain.repository.DocumentRepository
import com.docket.domain.repository.PremiumRepository
import com.docket.domain.repository.SettingsRepository
import com.docket.domain.usecase.CreateFolderUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

/**
 * Plain JVM test — hand-written fakes for both repository interfaces plus a real
 * [CreateFolderUseCase] wired against the same fake [DocumentRepository], same convention as
 * [com.docket.domain.usecase.CreateFolderUseCaseTest] and
 * [com.docket.ui.screens.search.SearchViewModelTest]. [FakeDocumentRepository] is reactive
 * (backed by [MutableStateFlow]) rather than a fixed snapshot, since most of what's worth
 * testing here is that the exposed `StateFlow`s actually react to folder/filter/sort changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // viewModelScope is Dispatchers.Main.immediate — a *live* global delegate, looked up at
    // dispatch time rather than frozen at launch time. A ViewModel's WhileSubscribed sharing
    // coroutines run for as long as its scope is alive, by design (same as in production) —
    // left uncancelled, one test's leftover coroutine can still be sitting on that delegate
    // when the next test's @Before repoints Main at a *different* TestDispatcher, and get
    // executed as part of *that* test's advanceUntilIdle(), stepping on unrelated state.
    // Registering every ViewModel this file creates and cancelling them here, before
    // Dispatchers.resetMain(), is what actually prevents that leak.
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
    fun `documents reflects the current folder`() = runTest {
        val docRoot = sampleDocument(id = 1, folderId = null, title = "Root doc")
        val docInFolder = sampleDocument(id = 2, folderId = 10L, title = "Foldered doc")
        val documentRepository = FakeDocumentRepository(documents = listOf(docRoot, docInFolder))
        val viewModel = viewModel(documentRepository)

        backgroundScope.launch { viewModel.documents.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(docRoot), viewModel.documents.value)

        viewModel.openFolder(10L)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(docInFolder), viewModel.documents.value)

        assertTrue(viewModel.navigateUp())
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf(docRoot), viewModel.documents.value)
    }

    @Test
    fun `navigateUp on an already-empty stack returns false and is a no-op`() = runTest {
        val viewModel = viewModel(FakeDocumentRepository())
        assertFalse(viewModel.navigateUp())
        assertTrue(viewModel.folderStack.value.isEmpty())
    }

    @Test
    fun `opening a folder clears any active selection`() = runTest {
        val documentRepository = FakeDocumentRepository(documents = listOf(sampleDocument(id = 1, folderId = null)))
        val viewModel = viewModel(documentRepository)

        viewModel.toggleSelection(1L)
        assertEquals(setOf(1L), viewModel.selection.value)

        viewModel.openFolder(10L)
        assertTrue(viewModel.selection.value.isEmpty())
    }

    @Test
    fun `toggleSelection adds and removes independently`() = runTest {
        val viewModel = viewModel(FakeDocumentRepository())

        viewModel.toggleSelection(1L)
        viewModel.toggleSelection(2L)
        assertEquals(setOf(1L, 2L), viewModel.selection.value)

        viewModel.toggleSelection(1L)
        assertEquals(setOf(2L), viewModel.selection.value)
    }

    @Test
    fun `deleteDocuments soft-deletes, clears selection, and emits an undo event`() = runTest {
        val documentRepository = FakeDocumentRepository()
        val viewModel = viewModel(documentRepository)
        viewModel.toggleSelection(1L)
        viewModel.toggleSelection(2L)

        var undoEvent: LibraryEvent.UndoDelete? = null
        backgroundScope.launch {
            viewModel.events.collect { if (it is LibraryEvent.UndoDelete) undoEvent = it }
        }

        viewModel.deleteDocuments(listOf(1L, 2L))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(1L, 2L), documentRepository.softDeletedIds)
        assertTrue(viewModel.selection.value.isEmpty())
        assertEquals(listOf(1L, 2L), undoEvent?.documentIds)
    }

    @Test
    fun `deleteDocuments with an empty list touches the repository not at all`() = runTest {
        val documentRepository = FakeDocumentRepository()
        val viewModel = viewModel(documentRepository)

        viewModel.deleteDocuments(emptyList())
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(documentRepository.softDeletedIds.isEmpty())
    }

    @Test
    fun `undoDelete restores every id it's given`() = runTest {
        val documentRepository = FakeDocumentRepository()
        val viewModel = viewModel(documentRepository)

        viewModel.undoDelete(listOf(5L, 6L))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(5L, 6L), documentRepository.restoredIds)
    }

    @Test
    fun `moveSelectedTo moves the selection then clears it`() = runTest {
        val documentRepository = FakeDocumentRepository()
        val viewModel = viewModel(documentRepository)
        viewModel.toggleSelection(3L)

        viewModel.moveSelectedTo(99L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(3L) to 99L, documentRepository.movedCalls.single())
        assertTrue(viewModel.selection.value.isEmpty())
    }

    @Test
    fun `moveSelectedTo with nothing selected touches the repository not at all`() = runTest {
        val documentRepository = FakeDocumentRepository()
        val viewModel = viewModel(documentRepository)

        viewModel.moveSelectedTo(99L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(documentRepository.movedCalls.isEmpty())
    }

    @Test
    fun `shareDocument emits pdf mime type for a PDF document`() = runTest {
        val pdfDoc = sampleDocument(id = 1, folderId = null, format = ExportFormat.PDF, pdfFilePath = "/docs/a.pdf")
        val documentRepository = FakeDocumentRepository(documents = listOf(pdfDoc))
        val viewModel = viewModel(documentRepository)
        backgroundScope.launch { viewModel.documents.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        // async{ flow.first() } rather than backgroundScope.launch{ flow.collect{} } — with a
        // second WhileSubscribed StateFlow (documents) already active in the background, a
        // plain collect-into-a-var subscriber reliably missed this SharedFlow's one emission
        // under StandardTestDispatcher; first() does not.
        val eventDeferred = async { viewModel.events.first() as LibraryEvent.ShareFiles }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.shareDocument(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        val shareEvent = eventDeferred.await()
        assertEquals("application/pdf", shareEvent.mimeType)
        // Built via File(...).path rather than a raw string literal — java.io.File normalizes
        // separators per-OS (Windows rewrites "/" to "\"), so the literal wouldn't round-trip.
        assertEquals(listOf(java.io.File("/docs/a.pdf").path), shareEvent.files.map { it.path })
    }

    @Test
    fun `shareSelected emits image mime type when any selected document is an image set`() = runTest {
        val imageDoc = sampleDocument(
            id = 1,
            folderId = null,
            format = ExportFormat.IMAGE_SET,
            pdfFilePath = null,
            pageImagePaths = listOf("/docs/p1.jpg", "/docs/p2.jpg")
        )
        val documentRepository = FakeDocumentRepository(documents = listOf(imageDoc))
        val viewModel = viewModel(documentRepository)
        backgroundScope.launch { viewModel.documents.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.toggleSelection(1L)

        val eventDeferred = async { viewModel.events.first() as LibraryEvent.ShareFiles }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.shareSelected()
        testDispatcher.scheduler.advanceUntilIdle()

        val shareEvent = eventDeferred.await()
        assertEquals("image/jpeg", shareEvent.mimeType)
        assertEquals(2, shareEvent.files.size)
    }

    @Test
    fun `createFolder under the free limit never emits PremiumRequired`() = runTest {
        val documentRepository = FakeDocumentRepository(folderCount = 1)
        val viewModel = viewModel(documentRepository, isPremium = false)

        var sawPremiumRequired = false
        backgroundScope.launch {
            viewModel.events.collect { if (it is LibraryEvent.PremiumRequired) sawPremiumRequired = true }
        }

        viewModel.createFolder("Taxes")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(sawPremiumRequired)
        assertEquals(listOf("Taxes" to null), documentRepository.createdFolders)
    }

    @Test
    fun `createFolder at the free limit for a non-premium user emits PremiumRequired`() = runTest {
        val documentRepository = FakeDocumentRepository(folderCount = 3) // FREE_FOLDER_LIMIT
        val viewModel = viewModel(documentRepository, isPremium = false)

        val eventDeferred = async { viewModel.events.first() }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createFolder("One Too Many")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(eventDeferred.await() is LibraryEvent.PremiumRequired)
        assertTrue(documentRepository.createdFolders.isEmpty())
    }

    @Test
    fun `sortBy is seeded from the settings default on init`() = runTest {
        val viewModel = LibraryViewModel(
            documentRepository = FakeDocumentRepository(),
            settingsRepository = FakeSettingsRepository(defaultSort = DocumentSort.NAME),
            createFolderUseCase = CreateFolderUseCase(FakeDocumentRepository(), FakePremiumRepository(false))
        ).also { createdViewModels += it }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(DocumentSort.NAME, viewModel.sortBy.value)
    }

    @Test
    fun `setSortBy and setTypeFilter and setViewMode update their own state only`() = runTest {
        val viewModel = viewModel(FakeDocumentRepository())

        viewModel.setSortBy(DocumentSort.SIZE)
        viewModel.setTypeFilter(DocumentTypeFilter.RECEIPT)
        viewModel.setViewMode(LibraryViewMode.LIST)

        assertEquals(DocumentSort.SIZE, viewModel.sortBy.value)
        assertEquals(DocumentTypeFilter.RECEIPT, viewModel.typeFilter.value)
        assertEquals(LibraryViewMode.LIST, viewModel.viewMode.value)
    }

    private fun viewModel(
        documentRepository: FakeDocumentRepository,
        isPremium: Boolean = false
    ) = LibraryViewModel(
        documentRepository = documentRepository,
        settingsRepository = FakeSettingsRepository(),
        createFolderUseCase = CreateFolderUseCase(documentRepository, FakePremiumRepository(isPremium))
    ).also { createdViewModels += it }

    private fun sampleDocument(
        id: Long,
        folderId: Long?,
        title: String = "Doc $id",
        format: ExportFormat = ExportFormat.PDF,
        pdfFilePath: String? = "/docs/$id.pdf",
        pageImagePaths: List<String> = emptyList()
    ) = Document(
        id = id,
        title = title,
        folderId = folderId,
        createdAt = 0L,
        pageCount = 1,
        format = format,
        pdfFilePath = pdfFilePath,
        pageImagePaths = pageImagePaths,
        thumbnailPath = null,
        sizeBytes = 0
    )

    private class FakeSettingsRepository(private val defaultSort: DocumentSort = DocumentSort.DATE) : SettingsRepository {
        override val defaultExportFormat: Flow<ExportFormat> = flowOf(ExportFormat.PDF)
        override suspend fun setDefaultExportFormat(format: ExportFormat) = Unit
        override val defaultLibrarySort: Flow<DocumentSort> = flowOf(defaultSort)
        override suspend fun setDefaultLibrarySort(sort: DocumentSort) = Unit
        override val appLockEnabled: Flow<Boolean> = flowOf(false)
        override suspend fun setAppLockEnabled(enabled: Boolean) = Unit
    }

    private class FakePremiumRepository(isPremium: Boolean) : PremiumRepository {
        override val isPremium: Flow<Boolean> = flowOf(isPremium)
        override fun isFeatureUnlocked(feature: PremiumFeature): Flow<Boolean> = isPremium
        override suspend fun setPremiumUnlocked(unlocked: Boolean) = Unit
    }

    /** Reactive in-memory fake: [documents]/[folders] live in [MutableStateFlow]s so
     *  `observeLibrary`/`observeFolders` genuinely react to writes the way Room would, rather
     *  than serving one fixed snapshot. */
    private class FakeDocumentRepository(
        documents: List<Document> = emptyList(),
        folders: List<Folder> = emptyList(),
        private val folderCount: Int = 0
    ) : DocumentRepository {
        private val documentsFlow = MutableStateFlow(documents)
        private val foldersFlow = MutableStateFlow(folders)

        val softDeletedIds = mutableListOf<Long>()
        val restoredIds = mutableListOf<Long>()
        val movedCalls = mutableListOf<Pair<List<Long>, Long?>>()
        val createdFolders = mutableListOf<Pair<String, Long?>>()
        private var nextFolderId = 100L

        override fun observeFolders(parentFolderId: Long?): Flow<List<Folder>> =
            foldersFlow.map { list -> list.filter { it.parentFolderId == parentFolderId } }

        override suspend fun getFolder(folderId: Long): Folder? = foldersFlow.value.find { it.id == folderId }

        override suspend fun createFolder(name: String, parentFolderId: Long?): Long {
            createdFolders += name to parentFolderId
            return nextFolderId++
        }

        override suspend fun folderCount(): Int = folderCount

        override fun observeFolderDocumentCounts(): Flow<Map<Long, Int>> =
            documentsFlow.map { docs -> docs.filter { it.folderId != null && it.deletedAt == null }
                .groupingBy { it.folderId!! }.eachCount() }

        override fun observeLibrary(folderId: Long?, typeFilter: DocumentTypeFilter, sortBy: DocumentSort): Flow<List<Document>> =
            documentsFlow.map { docs ->
                docs.filter { it.folderId == folderId && it.deletedAt == null }
                    .filter { doc ->
                        when (typeFilter) {
                            DocumentTypeFilter.ALL -> true
                            DocumentTypeFilter.PLAIN -> !doc.hasReceipt && !doc.hasWarranty
                            DocumentTypeFilter.RECEIPT -> doc.hasReceipt
                            DocumentTypeFilter.WARRANTY -> doc.hasWarranty
                        }
                    }
            }

        override fun observeDocument(documentId: Long): Flow<Document?> =
            documentsFlow.map { docs -> docs.find { it.id == documentId } }

        override suspend fun getDocument(documentId: Long): Document? = documentsFlow.value.find { it.id == documentId }

        override suspend fun getTitlesCreatedBetween(dayStart: Long, dayEnd: Long): List<String> = emptyList()

        override fun observeDeletedDocuments(): Flow<List<Document>> =
            documentsFlow.map { docs -> docs.filter { it.deletedAt != null } }

        override suspend fun softDeleteDocuments(documentIds: List<Long>) {
            softDeletedIds += documentIds
            documentsFlow.value = documentsFlow.value.map {
                if (it.id in documentIds) it.copy(deletedAt = 1L) else it
            }
        }

        override suspend fun restoreDocument(documentId: Long) {
            restoredIds += documentId
            documentsFlow.value = documentsFlow.value.map {
                if (it.id == documentId) it.copy(deletedAt = null) else it
            }
        }

        override suspend fun moveDocuments(documentIds: List<Long>, folderId: Long?) {
            movedCalls += documentIds to folderId
            documentsFlow.value = documentsFlow.value.map {
                if (it.id in documentIds) it.copy(folderId = folderId) else it
            }
        }

        override suspend fun renameDocument(documentId: Long, title: String) {
            documentsFlow.value = documentsFlow.value.map {
                if (it.id == documentId) it.copy(title = title) else it
            }
        }

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
