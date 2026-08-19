package com.docket.domain.usecase

import com.docket.domain.common.PremiumRequiredException
import com.docket.domain.common.Result
import com.docket.domain.model.Document
import com.docket.domain.model.DocumentSort
import com.docket.domain.model.DocumentTypeFilter
import com.docket.domain.model.ExportFormat
import com.docket.domain.model.Folder
import com.docket.domain.model.PremiumFeature
import com.docket.domain.repository.DocumentRepository
import com.docket.domain.repository.PremiumRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-Kotlin unit test using hand-written fakes for both repository interfaces — no Room, no
 * DataStore, no Android framework, so this runs as a plain JVM test.
 */
class CreateFolderUseCaseTest {

    @Test
    fun `creates the folder when under the free limit`() = runBlocking {
        val documentRepository = FakeDocumentRepository(folderCount = 2)
        val useCase = CreateFolderUseCase(documentRepository, FakePremiumRepository(isPremium = false))

        val result = useCase("Taxes", parentFolderId = null)

        assertTrue(result is Result.Success)
        assertEquals(1, documentRepository.createdFolders.size)
        assertEquals("Taxes", documentRepository.createdFolders.first().first)
    }

    @Test
    fun `blocks creation at the free limit for a non-premium user`() = runBlocking {
        val documentRepository = FakeDocumentRepository(folderCount = 3) // FREE_FOLDER_LIMIT
        val useCase = CreateFolderUseCase(documentRepository, FakePremiumRepository(isPremium = false))

        val result = useCase("One Too Many", parentFolderId = null)

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).throwable is PremiumRequiredException)
        assertEquals(
            PremiumFeature.UNLIMITED_FOLDERS,
            (result.throwable as PremiumRequiredException).feature
        )
        assertEquals(0, documentRepository.createdFolders.size) // never actually created
    }

    @Test
    fun `premium users bypass the folder limit entirely`() = runBlocking {
        val documentRepository = FakeDocumentRepository(folderCount = 50)
        val useCase = CreateFolderUseCase(documentRepository, FakePremiumRepository(isPremium = true))

        val result = useCase("Folder 51", parentFolderId = null)

        assertTrue(result is Result.Success)
        assertEquals(1, documentRepository.createdFolders.size)
    }

    private class FakePremiumRepository(isPremium: Boolean) : PremiumRepository {
        override val isPremium: Flow<Boolean> = flowOf(isPremium)
        override fun isFeatureUnlocked(feature: PremiumFeature): Flow<Boolean> = this.isPremium
        override suspend fun setPremiumUnlocked(unlocked: Boolean) = Unit
    }

    private class FakeDocumentRepository(private val folderCount: Int) : DocumentRepository {
        val createdFolders = mutableListOf<Pair<String, Long?>>()
        private var nextFolderId = 1L

        override suspend fun createFolder(name: String, parentFolderId: Long?): Long {
            createdFolders += name to parentFolderId
            return nextFolderId++
        }

        override suspend fun folderCount(): Int = folderCount

        // Everything below is unused by CreateFolderUseCase — stubbed to keep this fake
        // implementing the full interface without pulling in Room/DataStore.
        override fun observeFolders(parentFolderId: Long?): Flow<List<Folder>> = flowOf(emptyList())
        override suspend fun getFolder(folderId: Long): Folder? = null
        override fun observeFolderDocumentCounts(): Flow<Map<Long, Int>> = flowOf(emptyMap())
        override suspend fun getTitlesCreatedBetween(dayStart: Long, dayEnd: Long): List<String> = emptyList()
        override suspend fun renameDocument(documentId: Long, title: String) = Unit
        override fun observeLibrary(folderId: Long?, typeFilter: DocumentTypeFilter, sortBy: DocumentSort): Flow<List<Document>> =
            flowOf(emptyList())
        override fun observeRecentDocuments(limit: Int): Flow<List<Document>> = flowOf(emptyList())
        override fun observeLibrarySize(): Flow<Int> = flowOf(0)
        override fun observeDocument(documentId: Long): Flow<Document?> = MutableStateFlow(null)
        override suspend fun getDocument(documentId: Long): Document? = null
        override fun observeDeletedDocuments(): Flow<List<Document>> = flowOf(emptyList())
        override suspend fun softDeleteDocuments(documentIds: List<Long>) = Unit
        override suspend fun restoreDocument(documentId: Long) = Unit
        override suspend fun moveDocuments(documentIds: List<Long>, folderId: Long?) = Unit
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
