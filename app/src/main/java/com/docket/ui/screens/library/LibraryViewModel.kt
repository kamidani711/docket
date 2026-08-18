package com.docket.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.common.PremiumRequiredException
import com.docket.domain.common.Result
import com.docket.domain.model.Document
import com.docket.domain.model.DocumentSort
import com.docket.domain.model.DocumentTypeFilter
import com.docket.domain.model.Folder
import com.docket.domain.repository.DocumentRepository
import com.docket.domain.repository.SettingsRepository
import com.docket.domain.usecase.CreateFolderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryViewMode { LIST, GRID }

sealed interface LibraryEvent {
    data class UndoDelete(val documentIds: List<Long>) : LibraryEvent
    data class ShareFiles(val files: List<File>, val mimeType: String) : LibraryEvent
    data object PremiumRequired : LibraryEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val settingsRepository: SettingsRepository,
    private val createFolderUseCase: CreateFolderUseCase
) : ViewModel() {

    // Empty = root. At most 2 entries (one level of nesting) — see FolderEntity.
    private val _folderStack = MutableStateFlow<List<Long>>(emptyList())
    val folderStack: StateFlow<List<Long>> = _folderStack.asStateFlow()
    val canCreateSubfolder: StateFlow<Boolean> = folderStack
        .map { it.size < 2 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    // Seeded from Settings' "Library sort" default, then fully user-controlled for the rest of
    // this session — changing sort here doesn't overwrite the stored default, that's what the
    // explicit Settings control is for.
    private val _sortBy = MutableStateFlow(DocumentSort.DATE)
    val sortBy: StateFlow<DocumentSort> = _sortBy.asStateFlow()

    init {
        viewModelScope.launch { _sortBy.value = settingsRepository.defaultLibrarySort.first() }
    }

    private val _typeFilter = MutableStateFlow(DocumentTypeFilter.ALL)
    val typeFilter: StateFlow<DocumentTypeFilter> = _typeFilter.asStateFlow()

    // Grid is the default per the design brief — documents should read as documents (paper-feel
    // cards), not as a dense text list. List stays one tap away in the overflow menu.
    private val _viewMode = MutableStateFlow(LibraryViewMode.GRID)
    val viewMode: StateFlow<LibraryViewMode> = _viewMode.asStateFlow()

    private val _selection = MutableStateFlow<Set<Long>>(emptySet())
    val selection: StateFlow<Set<Long>> = _selection.asStateFlow()

    private val _events = MutableSharedFlow<LibraryEvent>()
    val events: SharedFlow<LibraryEvent> = _events.asSharedFlow()

    private val currentFolderId: StateFlow<Long?> = folderStack
        .map { it.lastOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val folders: StateFlow<List<Folder>> = currentFolderId
        .flatMapLatest { folderId -> documentRepository.observeFolders(folderId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folderDocumentCounts: StateFlow<Map<Long, Int>> = documentRepository.observeFolderDocumentCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val documents: StateFlow<List<Document>> = combine(currentFolderId, _typeFilter, _sortBy) { folderId, type, sort ->
        Triple(folderId, type, sort)
    }.flatMapLatest { (folderId, type, sort) ->
        documentRepository.observeLibrary(folderId, type, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun openFolder(folderId: Long) {
        _folderStack.value = _folderStack.value + folderId
        clearSelection()
    }

    fun navigateUp(): Boolean {
        if (_folderStack.value.isEmpty()) return false
        _folderStack.value = _folderStack.value.dropLast(1)
        clearSelection()
        return true
    }

    fun setSortBy(sort: DocumentSort) {
        _sortBy.value = sort
    }

    fun setTypeFilter(filter: DocumentTypeFilter) {
        _typeFilter.value = filter
    }

    fun setViewMode(mode: LibraryViewMode) {
        _viewMode.value = mode
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val result = createFolderUseCase(name, currentFolderId.value)
            if (result is Result.Error && result.throwable is PremiumRequiredException) {
                _events.emit(LibraryEvent.PremiumRequired)
            }
        }
    }

    fun toggleSelection(documentId: Long) {
        _selection.value = _selection.value.let { current ->
            if (documentId in current) current - documentId else current + documentId
        }
    }

    fun clearSelection() {
        _selection.value = emptySet()
    }

    /** Single-document swipe-to-delete and multi-select "Delete" both go through this. */
    fun deleteDocuments(documentIds: List<Long>) {
        if (documentIds.isEmpty()) return
        viewModelScope.launch {
            documentRepository.softDeleteDocuments(documentIds)
            clearSelection()
            _events.emit(LibraryEvent.UndoDelete(documentIds))
        }
    }

    fun undoDelete(documentIds: List<Long>) {
        viewModelScope.launch { documentIds.forEach { documentRepository.restoreDocument(it) } }
    }

    fun moveSelectedTo(folderId: Long?) {
        val ids = _selection.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            documentRepository.moveDocuments(ids, folderId)
            clearSelection()
        }
    }

    /** Shares the already-saved files for the current selection as-is — covers both "share"
     *  and "export" from multi-select, since these documents are already in their final saved
     *  form and re-processing them offers nothing a share of the existing file doesn't. */
    fun shareSelected() {
        val selected = documents.value.filter { it.id in _selection.value }
        shareDocuments(selected)
    }

    /** Same as [shareSelected] but for one document's row overflow menu — sharing shouldn't
     *  require entering multi-select first for the common "just share this one thing" case. */
    fun shareDocument(documentId: Long) {
        val document = documents.value.firstOrNull { it.id == documentId } ?: return
        shareDocuments(listOf(document))
    }

    private fun shareDocuments(selected: List<Document>) {
        if (selected.isEmpty()) return
        val allPdf = selected.all { it.pdfFilePath != null }
        val files = if (allPdf) {
            selected.mapNotNull { it.pdfFilePath?.let(::File) }
        } else {
            selected.flatMap { it.pageImagePaths.map(::File) }
        }
        val mimeType = if (allPdf) "application/pdf" else "image/jpeg"
        viewModelScope.launch { _events.emit(LibraryEvent.ShareFiles(files, mimeType)) }
    }
}
