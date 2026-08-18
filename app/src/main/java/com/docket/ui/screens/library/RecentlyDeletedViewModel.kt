package com.docket.ui.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docket.domain.model.Document
import com.docket.domain.repository.DocumentRepository
import com.docket.domain.usecase.HardDeleteDocumentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RecentlyDeletedViewModel @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val hardDeleteDocumentUseCase: HardDeleteDocumentUseCase
) : ViewModel() {

    val documents: StateFlow<List<Document>> = documentRepository.observeDeletedDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(documentId: Long) {
        viewModelScope.launch { documentRepository.restoreDocument(documentId) }
    }

    /** Permanent, immediate — bypasses the 30-day wait for this one document. */
    fun deleteForever(documentId: Long) {
        viewModelScope.launch { hardDeleteDocumentUseCase(documentId) }
    }
}
