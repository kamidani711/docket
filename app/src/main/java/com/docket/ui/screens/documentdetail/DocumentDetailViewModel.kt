package com.docket.ui.screens.documentdetail

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.common.PremiumRequiredException
import com.docket.domain.common.Result
import com.docket.domain.model.AnalyticsEventType
import com.docket.domain.model.Document
import com.docket.domain.model.ExportFormat
import com.docket.domain.model.ImageExportFormat
import com.docket.domain.model.ParsedLineItem
import com.docket.domain.model.ParsedReceipt
import com.docket.domain.model.PdfPageSize
import com.docket.domain.model.Receipt
import com.docket.domain.model.Warranty
import com.docket.domain.model.WarrantyDuration
import com.docket.domain.repository.AnalyticsRepository
import com.docket.domain.repository.DocumentRepository
import com.docket.domain.repository.ImageProcessor
import com.docket.domain.repository.OcrRepository
import com.docket.domain.repository.PdfWriter
import com.docket.domain.repository.PremiumRepository
import com.docket.domain.repository.ReceiptRepository
import com.docket.domain.repository.WarrantyRepository
import com.docket.domain.usecase.DeleteWarrantyUseCase
import com.docket.domain.usecase.ExportDocumentUseCase
import com.docket.domain.usecase.ParseReceiptUseCase
import com.docket.domain.usecase.SetWarrantyUseCase
import com.docket.ui.navigation.Destination
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data object Exporting : ExportUiState
    data class Done(val files: List<File>, val mimeType: String) : ExportUiState
    data class Failed(val message: String, val isPremiumRequired: Boolean) : ExportUiState
}

sealed interface DocumentDetailEvent {
    data class ShareFiles(val files: List<File>, val mimeType: String) : DocumentDetailEvent
    data object Deleted : DocumentDetailEvent
}

@HiltViewModel
class DocumentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val documentRepository: DocumentRepository,
    ocrRepository: OcrRepository,
    premiumRepository: PremiumRepository,
    private val receiptRepository: ReceiptRepository,
    private val warrantyRepository: WarrantyRepository,
    private val exportDocumentUseCase: ExportDocumentUseCase,
    private val parseReceiptUseCase: ParseReceiptUseCase,
    private val setWarrantyUseCase: SetWarrantyUseCase,
    private val deleteWarrantyUseCase: DeleteWarrantyUseCase,
    private val analyticsRepository: AnalyticsRepository,
    private val pdfWriter: PdfWriter,
    private val imageProcessor: ImageProcessor
) : ViewModel() {

    private val documentId: Long = checkNotNull(savedStateHandle[Destination.ARG_DOCUMENT_ID])

    val document: StateFlow<Document?> = documentRepository.observeDocument(documentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val hasOcrText: StateFlow<Boolean> = ocrRepository.observeHasOcrText(documentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isPremium: StateFlow<Boolean> = premiumRepository.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val receipt: StateFlow<Receipt?> = receiptRepository.observeReceiptForDocument(documentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val warranty: StateFlow<Warranty?> = warrantyRepository.observeWarrantyForDocument(documentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    private val _receiptDraft = MutableStateFlow<ParsedReceipt?>(null)
    val receiptDraft: StateFlow<ParsedReceipt?> = _receiptDraft.asStateFlow()

    private val _isParsingReceipt = MutableStateFlow(false)
    val isParsingReceipt: StateFlow<Boolean> = _isParsingReceipt.asStateFlow()

    private val _uiEvents = MutableSharedFlow<DocumentDetailEvent>()
    val uiEvents: SharedFlow<DocumentDetailEvent> = _uiEvents.asSharedFlow()

    // Page thumbnails/full-screen-viewer bitmaps, decoded on demand as pages scroll into view
    // (see ensurePageBitmap) rather than all at once — same bounded-concurrency discipline as
    // ScanSessionViewModel's preview cache, for the same reason: an N-page document shouldn't
    // try to hold N full decodes in memory at once.
    private val _pageBitmaps = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
    val pageBitmaps: StateFlow<Map<Int, Bitmap>> = _pageBitmaps.asStateFlow()
    private val pageDecodeSemaphore = Semaphore(PAGE_DECODE_CONCURRENCY)
    private val pagesInFlight = mutableSetOf<Int>()

    fun exportPdf(pageSize: PdfPageSize, jpegQuality: Int, searchable: Boolean) {
        viewModelScope.launch {
            _exportState.value = ExportUiState.Exporting
            when (val result = exportDocumentUseCase.exportPdf(documentId, pageSize, jpegQuality, searchable)) {
                is Result.Success -> _exportState.value = ExportUiState.Done(
                    files = listOf(result.data.file),
                    mimeType = "application/pdf"
                )
                is Result.Error -> _exportState.value = result.toFailedState()
                Result.Loading -> Unit
            }
        }
    }

    fun exportImages(pageIndexes: List<Int>, format: ImageExportFormat, quality: Int) {
        viewModelScope.launch {
            _exportState.value = ExportUiState.Exporting
            when (val result = exportDocumentUseCase.exportImages(documentId, pageIndexes, format, quality)) {
                is Result.Success -> _exportState.value = ExportUiState.Done(
                    files = result.data.files,
                    mimeType = if (format == ImageExportFormat.PNG) "image/png" else "image/jpeg"
                )
                is Result.Error -> _exportState.value = result.toFailedState()
                Result.Loading -> Unit
            }
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            _exportState.value = ExportUiState.Exporting
            when (val result = exportDocumentUseCase.exportCsv(documentId)) {
                is Result.Success -> _exportState.value = ExportUiState.Done(
                    files = listOf(result.data),
                    mimeType = "text/csv"
                )
                is Result.Error -> _exportState.value = result.toFailedState()
                Result.Loading -> Unit
            }
        }
    }

    fun clearExportState() {
        _exportState.value = ExportUiState.Idle
    }

    /** Kicks off the "tag as receipt" flow: parses OCR text into a [ParsedReceipt] suggestion
     *  for the edit form — nothing is saved until [saveReceipt] is called. */
    fun startTaggingAsReceipt() {
        viewModelScope.launch {
            _isParsingReceipt.value = true
            _receiptDraft.value = parseReceiptUseCase(documentId)
            _isParsingReceipt.value = false
        }
    }

    fun editExistingReceipt() {
        val current = receipt.value ?: return
        _receiptDraft.value = ParsedReceipt(
            merchant = current.merchant,
            totalAmountCents = current.totalAmountCents,
            currencyCode = current.currencyCode,
            purchaseDate = current.purchaseDate,
            lineItems = current.lineItems.map { ParsedLineItem(it.description, it.amountCents) },
            paymentMethod = current.paymentMethod
        )
    }

    fun saveReceipt(parsed: ParsedReceipt) {
        viewModelScope.launch {
            receiptRepository.saveReceipt(documentId, parsed)
            _receiptDraft.value = null
            analyticsRepository.logEvent(AnalyticsEventType.FEATURE_USED, "receipt_tagged")
        }
    }

    fun cancelReceiptEdit() {
        _receiptDraft.value = null
    }

    fun setWarranty(itemName: String, purchaseDate: Long, duration: WarrantyDuration) {
        viewModelScope.launch {
            setWarrantyUseCase(documentId, itemName, purchaseDate, duration)
            analyticsRepository.logEvent(AnalyticsEventType.FEATURE_USED, "warranty_set")
        }
    }

    fun deleteWarranty() {
        val warrantyId = warranty.value?.id ?: return
        viewModelScope.launch { deleteWarrantyUseCase(warrantyId) }
    }

    /** Blank/unchanged input is silently ignored rather than saved — the inline name field
     *  always has *something* valid in it already (the document's current title), so there's
     *  no legitimate "clear the name" action to support here, only "typed nothing, tapped
     *  away." Trimmed, same as every other name-entry point in the app. */
    fun renameDocument(newTitle: String) {
        val trimmed = newTitle.trim()
        val current = document.value ?: return
        if (trimmed.isEmpty() || trimmed == current.title) return
        viewModelScope.launch { documentRepository.renameDocument(documentId, trimmed) }
    }

    /** Shares the document's already-saved file(s) as-is — same reasoning as Library's
     *  per-row share action (see LibraryViewModel.shareDocument): this is already the file's
     *  final saved form, so a fresh export has nothing to add over sharing it directly. */
    fun shareDocument() {
        val current = document.value ?: return
        val files = if (current.format == ExportFormat.PDF) {
            listOfNotNull(current.pdfFilePath?.let(::File))
        } else {
            current.pageImagePaths.map(::File)
        }
        if (files.isEmpty()) return
        val mimeType = if (current.format == ExportFormat.PDF) "application/pdf" else "image/jpeg"
        viewModelScope.launch { _uiEvents.emit(DocumentDetailEvent.ShareFiles(files, mimeType)) }
    }

    /** Soft-delete only — recoverable from Recently Deleted for 30 days like every other
     *  delete path in the app. The screen navigates back on [DocumentDetailEvent.Deleted]
     *  since staying on a just-deleted document's detail screen has nothing left to show. */
    fun deleteDocument() {
        viewModelScope.launch {
            documentRepository.softDeleteDocuments(listOf(documentId))
            analyticsRepository.logEvent(AnalyticsEventType.FEATURE_USED, "document_deleted_from_detail")
            _uiEvents.emit(DocumentDetailEvent.Deleted)
        }
    }

    /** No-op if [index] is already decoded or has a decode in flight — cheap to call from
     *  every page-strip item on every recomposition, same pattern as ScanSessionViewModel
     *  .ensurePreview. [maxDimension] is shared by the page strip and the full-screen viewer
     *  (one cache, one resolution) rather than keeping a separate low-res thumbnail cache — a
     *  deliberate memory-vs-sharpness tradeoff, not an oversight; see the class doc. */
    fun ensurePageBitmap(index: Int, maxDimension: Int) {
        if (index in pagesInFlight || _pageBitmaps.value.containsKey(index)) return
        val current = document.value ?: return
        if (index !in current.pageIndices()) return
        pagesInFlight += index
        viewModelScope.launch(Dispatchers.Default) {
            pageDecodeSemaphore.withPermit {
                val bitmap = when (current.format) {
                    ExportFormat.PDF -> pdfWriter.renderPage(
                        File(requireNotNull(current.pdfFilePath) { "PDF document has no file path" }),
                        index,
                        maxDimension
                    )
                    ExportFormat.IMAGE_SET -> imageProcessor.decodeDownsampled(current.pageImagePaths[index], maxDimension)
                }
                _pageBitmaps.update { it + (index to bitmap) }
                pagesInFlight -= index
            }
        }
    }

    private fun Document.pageIndices(): IntRange = 0 until pageCount

    private fun Result.Error.toFailedState(): ExportUiState.Failed {
        val isPremiumRequired = throwable is PremiumRequiredException
        val message = if (isPremiumRequired) {
            "This needs Premium to unlock."
        } else {
            message ?: "Export failed."
        }
        return ExportUiState.Failed(message, isPremiumRequired)
    }

    private companion object {
        const val PAGE_DECODE_CONCURRENCY = 2
    }
}
