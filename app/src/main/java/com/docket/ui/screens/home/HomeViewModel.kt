package com.docket.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.model.Document
import com.docket.domain.repository.DocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val RECENT_FILES_LIMIT = 4

sealed interface HomeEvent {
    data class ShareFiles(val files: List<File>, val mimeType: String) : HomeEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val documentRepository: DocumentRepository
) : ViewModel() {

    val recentDocuments: StateFlow<List<Document>> = documentRepository.observeRecentDocuments(RECENT_FILES_LIMIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    fun shareDocument(documentId: Long) {
        val document = recentDocuments.value.firstOrNull { it.id == documentId } ?: return
        val allPdf = document.pdfFilePath != null
        val files = if (allPdf) {
            listOfNotNull(document.pdfFilePath?.let(::File))
        } else {
            document.pageImagePaths.map(::File)
        }
        if (files.isEmpty()) return
        val mimeType = if (allPdf) "application/pdf" else "image/jpeg"
        viewModelScope.launch { _events.emit(HomeEvent.ShareFiles(files, mimeType)) }
    }
}
