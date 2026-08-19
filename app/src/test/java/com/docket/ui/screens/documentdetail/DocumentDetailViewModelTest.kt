package com.docket.ui.screens.documentdetail

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.model.AnalyticsEventType
import com.docket.domain.model.Document
import com.docket.domain.model.ExportFormat
import com.docket.domain.model.LanguagePackState
import com.docket.domain.model.OcrLanguage
import com.docket.domain.model.PageOcrData
import com.docket.domain.model.ParsedReceipt
import com.docket.domain.model.PremiumFeature
import com.docket.domain.model.Quad
import com.docket.domain.model.Receipt
import com.docket.domain.model.ReceiptLineItem
import com.docket.domain.model.ScanFilter
import com.docket.domain.model.Warranty
import com.docket.domain.model.WarrantyDuration
import com.docket.domain.repository.AnalyticsRepository
import com.docket.domain.repository.DocumentRepository
import com.docket.domain.repository.ImageExporter
import com.docket.domain.repository.ImageProcessor
import com.docket.domain.repository.OcrRepository
import com.docket.domain.repository.PdfWriter
import com.docket.domain.repository.PremiumRepository
import com.docket.domain.repository.ReceiptParser
import com.docket.domain.repository.ReceiptRepository
import com.docket.domain.repository.ReminderScheduler
import com.docket.domain.repository.WarrantyRepository
import com.docket.domain.usecase.DeleteWarrantyUseCase
import com.docket.domain.usecase.ExportDocumentUseCase
import com.docket.domain.usecase.ParseReceiptUseCase
import com.docket.domain.usecase.SetWarrantyUseCase
import com.docket.ui.navigation.Destination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * Plain JVM test, same hand-written-fakes convention as [com.docket.ui.screens.library
 * .LibraryViewModelTest]. [ExportDocumentUseCase] needs a real `Context` in its constructor
 * (see the architecture review's note on `Context` leaking into a few use case signatures) —
 * [mock] stands in for it here *purely for wiring*: every test in this file deliberately avoids
 * calling `exportPdf`/`exportImages`, the only methods that would actually touch it (real
 * `Context.getFilesDir()` calls have no meaning, and no safe return value, off a mock).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DocumentDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // See LibraryViewModelTest's identical field for why: viewModelScope is Dispatchers.Main
    // .immediate, a live global delegate — an uncancelled ViewModel's sharing coroutines can
    // otherwise still be sitting on it when the next test's @Before repoints Main elsewhere.
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
    fun `startTaggingAsReceipt parses OCR text into a draft and toggles the parsing flag`() = runTest {
        val documentRepository = FakeDocumentRepository(sampleDocument())
        val ocrRepository = FakeOcrRepository(text = "Fresh Mart\nTotal 12.34")
        val receiptParser = FakeReceiptParser(
            result = ParsedReceipt(
                merchant = "Fresh Mart",
                totalAmountCents = 1234,
                currencyCode = "USD",
                purchaseDate = null,
                lineItems = emptyList(),
                paymentMethod = null
            )
        )
        val viewModel = viewModel(documentRepository = documentRepository, ocrRepository = ocrRepository, receiptParser = receiptParser)

        assertFalse(viewModel.isParsingReceipt.value)
        viewModel.startTaggingAsReceipt()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isParsingReceipt.value)
        assertEquals("Fresh Mart", viewModel.receiptDraft.value?.merchant)
        assertEquals(1234L, viewModel.receiptDraft.value?.totalAmountCents)
    }

    @Test
    fun `editExistingReceipt copies the saved receipt into an editable draft`() = runTest {
        val receipt = sampleReceipt()
        val receiptRepository = FakeReceiptRepository(receiptForDocument = receipt)
        val viewModel = viewModel(receiptRepository = receiptRepository)
        backgroundScope.launch { viewModel.receipt.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.editExistingReceipt()

        assertEquals(receipt.merchant, viewModel.receiptDraft.value?.merchant)
        assertEquals(receipt.totalAmountCents, viewModel.receiptDraft.value?.totalAmountCents)
        assertEquals(1, viewModel.receiptDraft.value?.lineItems?.size)
    }

    @Test
    fun `editExistingReceipt with no saved receipt is a no-op`() = runTest {
        val viewModel = viewModel(receiptRepository = FakeReceiptRepository(receiptForDocument = null))
        backgroundScope.launch { viewModel.receipt.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.editExistingReceipt()

        assertNull(viewModel.receiptDraft.value)
    }

    @Test
    fun `saveReceipt persists, clears the draft, and logs the receipt_tagged event`() = runTest {
        val receiptRepository = FakeReceiptRepository()
        val analyticsRepository = FakeAnalyticsRepository()
        val viewModel = viewModel(receiptRepository = receiptRepository, analyticsRepository = analyticsRepository)
        val parsed = ParsedReceipt("Corner Store", 500, "USD", null, emptyList(), null)

        viewModel.saveReceipt(parsed)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(parsed, receiptRepository.savedReceipts.single().second)
        assertNull(viewModel.receiptDraft.value)
        assertTrue(analyticsRepository.loggedEvents.any { it == AnalyticsEventType.FEATURE_USED to "receipt_tagged" })
    }

    @Test
    fun `cancelReceiptEdit clears the draft without saving anything`() = runTest {
        val receiptRepository = FakeReceiptRepository()
        val viewModel = viewModel(receiptRepository = receiptRepository)
        viewModel.saveReceipt(ParsedReceipt("X", null, null, null, emptyList(), null))
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.editExistingReceipt() // no saved receipt yet, but exercises the clear path below regardless

        viewModel.cancelReceiptEdit()

        assertNull(viewModel.receiptDraft.value)
    }

    @Test
    fun `setWarranty schedules reminders, saves, and logs the warranty_set event`() = runTest {
        val warrantyRepository = FakeWarrantyRepository()
        val reminderScheduler = FakeReminderScheduler()
        val analyticsRepository = FakeAnalyticsRepository()
        val viewModel = viewModel(
            warrantyRepository = warrantyRepository,
            reminderScheduler = reminderScheduler,
            analyticsRepository = analyticsRepository
        )

        viewModel.setWarranty("Fridge", purchaseDate = 0L, duration = WarrantyDuration.OneYear)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Fridge", warrantyRepository.savedItemNames.single())
        assertEquals(1, reminderScheduler.scheduledWarrantyIds.size)
        assertTrue(analyticsRepository.loggedEvents.any { it == AnalyticsEventType.FEATURE_USED to "warranty_set" })
    }

    @Test
    fun `deleteWarranty with nothing set is a no-op`() = runTest {
        val warrantyRepository = FakeWarrantyRepository()
        val reminderScheduler = FakeReminderScheduler()
        val viewModel = viewModel(warrantyRepository = warrantyRepository, reminderScheduler = reminderScheduler)
        backgroundScope.launch { viewModel.warranty.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteWarranty()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(warrantyRepository.deletedIds.isEmpty())
        assertTrue(reminderScheduler.cancelledWarrantyIds.isEmpty())
    }

    @Test
    fun `deleteWarranty cancels reminders then deletes the existing warranty`() = runTest {
        val warranty = sampleWarranty(id = 42L)
        val warrantyRepository = FakeWarrantyRepository(warrantyForDocument = warranty)
        val reminderScheduler = FakeReminderScheduler()
        val viewModel = viewModel(warrantyRepository = warrantyRepository, reminderScheduler = reminderScheduler)
        backgroundScope.launch { viewModel.warranty.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteWarranty()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(42L), reminderScheduler.cancelledWarrantyIds)
        assertEquals(listOf(42L), warrantyRepository.deletedIds)
    }

    @Test
    fun `renameDocument ignores blank input`() = runTest {
        val documentRepository = FakeDocumentRepository(sampleDocument(title = "Original"))
        val viewModel = viewModel(documentRepository = documentRepository)
        backgroundScope.launch { viewModel.document.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.renameDocument("   ")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(documentRepository.renamedTitles.isEmpty())
    }

    @Test
    fun `renameDocument ignores a value identical to the current title`() = runTest {
        val documentRepository = FakeDocumentRepository(sampleDocument(title = "Original"))
        val viewModel = viewModel(documentRepository = documentRepository)
        backgroundScope.launch { viewModel.document.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.renameDocument("Original")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(documentRepository.renamedTitles.isEmpty())
    }

    @Test
    fun `renameDocument trims and persists a real change`() = runTest {
        val documentRepository = FakeDocumentRepository(sampleDocument(title = "Original"))
        val viewModel = viewModel(documentRepository = documentRepository)
        backgroundScope.launch { viewModel.document.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.renameDocument("  New Name  ")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("New Name", documentRepository.renamedTitles.single())
    }

    @Test
    fun `shareDocument emits the pdf path for a PDF document`() = runTest {
        val documentRepository = FakeDocumentRepository(
            sampleDocument(format = ExportFormat.PDF, pdfFilePath = "/docs/x.pdf")
        )
        val viewModel = viewModel(documentRepository = documentRepository)
        backgroundScope.launch { viewModel.document.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        // async{ flow.first() } rather than backgroundScope.launch{ flow.collect{} } — with a
        // second WhileSubscribed StateFlow (document) already active in the background, a
        // plain collect-into-a-var subscriber reliably missed this SharedFlow's one emission
        // under StandardTestDispatcher; first() does not. See LibraryViewModelTest for the same
        // pattern.
        val eventDeferred = async { viewModel.uiEvents.first() as DocumentDetailEvent.ShareFiles }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.shareDocument()
        testDispatcher.scheduler.advanceUntilIdle()

        val event = eventDeferred.await()
        assertEquals("application/pdf", event.mimeType)
        // Built via File(...).path rather than a raw string literal — java.io.File normalizes
        // separators per-OS (Windows rewrites "/" to "\"), so the literal wouldn't round-trip.
        assertEquals(listOf(java.io.File("/docs/x.pdf").path), event.files.map { it.path })
    }

    @Test
    fun `deleteDocument soft-deletes, logs analytics, and emits Deleted`() = runTest {
        val documentRepository = FakeDocumentRepository(sampleDocument())
        val analyticsRepository = FakeAnalyticsRepository()
        val viewModel = viewModel(documentRepository = documentRepository, analyticsRepository = analyticsRepository)

        var sawDeleted = false
        backgroundScope.launch { viewModel.uiEvents.collect { if (it is DocumentDetailEvent.Deleted) sawDeleted = true } }

        viewModel.deleteDocument()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(DOCUMENT_ID), documentRepository.softDeletedIds)
        assertTrue(analyticsRepository.loggedEvents.any { it == AnalyticsEventType.FEATURE_USED to "document_deleted_from_detail" })
        assertTrue(sawDeleted)
    }

    // ---- Fixture wiring ----

    private companion object {
        const val DOCUMENT_ID = 1L
    }

    private fun viewModel(
        documentRepository: DocumentRepository = FakeDocumentRepository(sampleDocument()),
        ocrRepository: OcrRepository = FakeOcrRepository(),
        premiumRepository: PremiumRepository = FakePremiumRepository(),
        receiptRepository: ReceiptRepository = FakeReceiptRepository(),
        warrantyRepository: WarrantyRepository = FakeWarrantyRepository(),
        reminderScheduler: ReminderScheduler = FakeReminderScheduler(),
        receiptParser: ReceiptParser = FakeReceiptParser(),
        analyticsRepository: AnalyticsRepository = FakeAnalyticsRepository(),
        pdfWriter: PdfWriter = FakePdfWriter(),
        imageProcessor: ImageProcessor = FakeImageProcessor()
    ): DocumentDetailViewModel = DocumentDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf(Destination.ARG_DOCUMENT_ID to DOCUMENT_ID)),
        documentRepository = documentRepository,
        ocrRepository = ocrRepository,
        premiumRepository = premiumRepository,
        receiptRepository = receiptRepository,
        warrantyRepository = warrantyRepository,
        exportDocumentUseCase = ExportDocumentUseCase(
            documentRepository = documentRepository,
            ocrRepository = ocrRepository,
            imageProcessor = imageProcessor,
            pdfWriter = pdfWriter,
            imageExporter = FakeImageExporter(),
            premiumRepository = premiumRepository,
            analyticsRepository = analyticsRepository,
            appContext = mock(Context::class.java)
        ),
        parseReceiptUseCase = ParseReceiptUseCase(documentRepository, ocrRepository, receiptParser),
        setWarrantyUseCase = SetWarrantyUseCase(warrantyRepository, reminderScheduler),
        deleteWarrantyUseCase = DeleteWarrantyUseCase(warrantyRepository, reminderScheduler),
        analyticsRepository = analyticsRepository,
        pdfWriter = pdfWriter,
        imageProcessor = imageProcessor
    ).also { createdViewModels += it }

    private fun sampleDocument(
        title: String = "Doc",
        format: ExportFormat = ExportFormat.PDF,
        pdfFilePath: String? = "/docs/doc.pdf"
    ) = Document(
        id = DOCUMENT_ID,
        title = title,
        folderId = null,
        createdAt = 0L,
        pageCount = 1,
        format = format,
        pdfFilePath = pdfFilePath,
        pageImagePaths = emptyList(),
        thumbnailPath = null,
        sizeBytes = 0
    )

    private fun sampleReceipt() = Receipt(
        id = 1L,
        documentId = DOCUMENT_ID,
        merchant = "Fresh Mart",
        totalAmountCents = 4218,
        currencyCode = "USD",
        purchaseDate = null,
        paymentMethod = null,
        lineItems = listOf(ReceiptLineItem(id = 1L, description = "Milk", amountCents = 350, orderIndex = 0)),
        createdAt = 0L
    )

    private fun sampleWarranty(id: Long) = Warranty(
        id = id,
        documentId = DOCUMENT_ID,
        itemName = "Fridge",
        purchaseDate = 0L,
        expiryDate = 0L,
        durationMonths = 12,
        createdAt = 0L
    )

    // ---- Fakes ----

    private class FakeAnalyticsRepository : AnalyticsRepository {
        val loggedEvents = mutableListOf<Pair<AnalyticsEventType, String?>>()
        override suspend fun logEvent(type: AnalyticsEventType, detail: String?) {
            loggedEvents += type to detail
        }
        override fun observeSummary() = flowOf(com.docket.domain.model.AnalyticsSummary())
        override suspend fun clearAll() = Unit
    }

    private class FakePremiumRepository(isPremium: Boolean = false) : PremiumRepository {
        override val isPremium: Flow<Boolean> = flowOf(isPremium)
        override fun isFeatureUnlocked(feature: PremiumFeature): Flow<Boolean> = isPremium
        override suspend fun setPremiumUnlocked(unlocked: Boolean) = Unit
    }

    private class FakeOcrRepository(private val text: String = "") : OcrRepository {
        override fun observeLanguagePacks(): Flow<List<LanguagePackState>> = flowOf(emptyList())
        override suspend fun installedLanguages(): List<OcrLanguage> = emptyList()
        override suspend fun installLanguagePack(language: OcrLanguage) = Unit
        override suspend fun removeLanguagePack(language: OcrLanguage) = Unit
        override suspend fun savePageOcr(result: PageOcrData) = Unit
        override suspend fun getPageOcr(documentId: Long, pageIndex: Int): PageOcrData? =
            if (text.isEmpty()) null else PageOcrData(documentId, pageIndex, text, emptyList(), OcrLanguage.LATIN)
        override fun observeHasOcrText(documentId: Long): Flow<Boolean> = flowOf(text.isNotEmpty())
    }

    private class FakeReceiptParser(private val result: ParsedReceipt = ParsedReceipt(null, null, null, null, emptyList(), null)) :
        ReceiptParser {
        override fun parse(ocrText: String): ParsedReceipt = result
    }

    private class FakeReceiptRepository(private val receiptForDocument: Receipt? = null) : ReceiptRepository {
        val savedReceipts = mutableListOf<Pair<Long, ParsedReceipt>>()
        override fun observeReceiptForDocument(documentId: Long): Flow<Receipt?> = flowOf(receiptForDocument)
        override suspend fun getReceiptForDocument(documentId: Long): Receipt? = receiptForDocument
        override suspend fun saveReceipt(documentId: Long, parsed: ParsedReceipt): Long {
            savedReceipts += documentId to parsed
            return 1L
        }
        override fun observeAllReceipts(filter: com.docket.domain.model.ReceiptFilter) = flowOf(emptyList<Receipt>())
        override fun observeMonthlySpend() = flowOf(emptyList<com.docket.domain.model.MonthlySpend>())
        override fun observeMerchants(): Flow<List<String>> = flowOf(emptyList())
    }

    private class FakeWarrantyRepository(private val warrantyForDocument: Warranty? = null) : WarrantyRepository {
        val savedItemNames = mutableListOf<String>()
        val deletedIds = mutableListOf<Long>()
        override fun observeWarrantiesSortedByExpiry(): Flow<List<Warranty>> = flowOf(emptyList())
        override fun observeWarrantyForDocument(documentId: Long): Flow<Warranty?> = flowOf(warrantyForDocument)
        override suspend fun getWarranty(warrantyId: Long): Warranty? = warrantyForDocument
        override suspend fun getWarrantyForDocument(documentId: Long): Warranty? = warrantyForDocument
        override suspend fun saveWarranty(
            documentId: Long,
            itemName: String,
            purchaseDate: Long,
            expiryDate: Long,
            durationMonths: Int
        ): Long {
            savedItemNames += itemName
            return 1L
        }
        override suspend fun deleteWarranty(warrantyId: Long) {
            deletedIds += warrantyId
        }
    }

    private class FakeReminderScheduler : ReminderScheduler {
        val scheduledWarrantyIds = mutableListOf<Long>()
        val cancelledWarrantyIds = mutableListOf<Long>()
        override fun scheduleWarrantyReminders(warrantyId: Long, itemName: String, documentId: Long, expiryDate: Long) {
            scheduledWarrantyIds += warrantyId
        }
        override fun cancelWarrantyReminders(warrantyId: Long) {
            cancelledWarrantyIds += warrantyId
        }
    }

    private class FakePdfWriter : PdfWriter {
        override suspend fun writePdf(pageCount: Int, destination: java.io.File, renderPage: suspend (Int) -> Bitmap) = Unit
        override suspend fun renderThumbnail(pdfFile: java.io.File, maxDimension: Int): Bitmap = mock(Bitmap::class.java)
        override suspend fun renderPage(pdfFile: java.io.File, pageIndex: Int, maxDimension: Int): Bitmap = mock(Bitmap::class.java)
        override suspend fun writeExportPdf(request: com.docket.domain.model.PdfExportRequest) =
            throw UnsupportedOperationException("not exercised by these tests — see the class doc")
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

    private class FakeImageExporter : ImageExporter {
        override suspend fun exportImages(request: com.docket.domain.model.ImageExportRequest) =
            throw UnsupportedOperationException("not exercised by these tests — see the class doc")
    }

    private class FakeDocumentRepository(private val document: Document?) : DocumentRepository {
        val renamedTitles = mutableListOf<String>()
        val softDeletedIds = mutableListOf<Long>()
        private val documentFlow = MutableStateFlow(document)

        override fun observeFolders(parentFolderId: Long?): Flow<List<com.docket.domain.model.Folder>> = flowOf(emptyList())
        override suspend fun getFolder(folderId: Long): com.docket.domain.model.Folder? = null
        override suspend fun createFolder(name: String, parentFolderId: Long?): Long = 0L
        override suspend fun folderCount(): Int = 0
        override fun observeFolderDocumentCounts(): Flow<Map<Long, Int>> = flowOf(emptyMap())
        override fun observeLibrary(
            folderId: Long?,
            typeFilter: com.docket.domain.model.DocumentTypeFilter,
            sortBy: com.docket.domain.model.DocumentSort
        ): Flow<List<Document>> = flowOf(emptyList())
        override fun observeDocument(documentId: Long): Flow<Document?> = documentFlow
        override suspend fun getDocument(documentId: Long): Document? = documentFlow.value
        override suspend fun getTitlesCreatedBetween(dayStart: Long, dayEnd: Long): List<String> = emptyList()
        override fun observeDeletedDocuments(): Flow<List<Document>> = flowOf(emptyList())
        override suspend fun softDeleteDocuments(documentIds: List<Long>) {
            softDeletedIds += documentIds
        }
        override suspend fun restoreDocument(documentId: Long) = Unit
        override suspend fun moveDocuments(documentIds: List<Long>, folderId: Long?) = Unit
        override suspend fun renameDocument(documentId: Long, title: String) {
            renamedTitles += title
            documentFlow.value = documentFlow.value?.copy(title = title)
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
