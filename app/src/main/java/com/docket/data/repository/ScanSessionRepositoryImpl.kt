package com.docket.data.repository

import com.docket.data.local.dao.ScanPageDao
import com.docket.data.local.dao.ScanSessionDao
import com.docket.data.local.entity.ScanPageEntity
import com.docket.data.local.entity.ScanSessionEntity
import com.docket.domain.model.NormalizedPoint
import com.docket.domain.model.Quad
import com.docket.domain.model.ScanFilter
import com.docket.domain.model.ScanPage
import com.docket.domain.model.ScanSession
import com.docket.domain.repository.ScanSessionRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Room-backed — there's no separate in-memory session state anywhere in the app.
 * [observeSession] just reflects the DB, so every mutation here IS the process-death recovery
 * story: nothing extra to persist, nothing to fall out of sync.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ScanSessionRepositoryImpl @Inject constructor(
    private val scanSessionDao: ScanSessionDao,
    private val scanPageDao: ScanPageDao
) : ScanSessionRepository {

    override fun observeSession(): Flow<ScanSession?> =
        scanSessionDao.observeSession().flatMapLatest { session ->
            if (session == null) {
                flowOf(null)
            } else {
                scanPageDao.observePages(session.id).map { pages ->
                    session.toDomain(pages.map { it.toDomain() })
                }
            }
        }

    override suspend fun startImageSession(originalImagePaths: List<String>): String {
        scanSessionDao.deleteAll() // only one draft session at a time
        val sessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        scanSessionDao.insert(ScanSessionEntity(id = sessionId, createdAt = now, updatedAt = now))
        scanPageDao.insertAll(originalImagePaths.mapIndexed { index, path -> newPageEntity(sessionId, index, path) })
        return sessionId
    }

    override suspend fun startPdfImportSession(pdfPath: String, pageCount: Int): String {
        scanSessionDao.deleteAll()
        val sessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        scanSessionDao.insert(
            ScanSessionEntity(
                id = sessionId,
                createdAt = now,
                updatedAt = now,
                importedPdfPath = pdfPath,
                importedPdfPageCount = pageCount
            )
        )
        return sessionId
    }

    override suspend fun appendPages(sessionId: String, originalImagePaths: List<String>) {
        val startIndex = scanPageDao.maxOrderIndex(sessionId) + 1
        scanPageDao.insertAll(
            originalImagePaths.mapIndexed { offset, path -> newPageEntity(sessionId, startIndex + offset, path) }
        )
    }

    override suspend fun updateCorners(pageId: String, corners: Quad) {
        scanPageDao.updateCorners(
            pageId = pageId,
            tlX = corners.topLeft.x, tlY = corners.topLeft.y,
            trX = corners.topRight.x, trY = corners.topRight.y,
            brX = corners.bottomRight.x, brY = corners.bottomRight.y,
            blX = corners.bottomLeft.x, blY = corners.bottomLeft.y
        )
    }

    override suspend fun rotatePage(pageId: String, deltaDegrees: Int) {
        scanPageDao.rotateBy(pageId, deltaDegrees)
    }

    override suspend fun updateFilter(pageId: String, filter: ScanFilter) {
        scanPageDao.updateFilter(pageId, filter.name)
    }

    override suspend fun reorderPages(sessionId: String, orderedPageIds: List<String>) {
        scanPageDao.reorder(orderedPageIds)
    }

    override suspend fun deletePage(pageId: String) {
        scanPageDao.delete(pageId)
    }

    override suspend fun replacePageImage(pageId: String, newOriginalImagePath: String) {
        scanPageDao.replaceImageAndResetEdits(pageId, newOriginalImagePath)
    }

    override suspend fun updateSuggestedName(sessionId: String, name: String) {
        scanSessionDao.updateSuggestedName(sessionId, name, System.currentTimeMillis())
    }

    override suspend fun clearSession(sessionId: String) {
        scanSessionDao.delete(sessionId) // ScanPageEntity FK is CASCADE — pages go with it
    }

    private fun newPageEntity(sessionId: String, orderIndex: Int, path: String) = ScanPageEntity(
        id = UUID.randomUUID().toString(),
        sessionId = sessionId,
        orderIndex = orderIndex,
        originalImagePath = path
    )
}

private fun ScanSessionEntity.toDomain(pages: List<ScanPage>): ScanSession = ScanSession(
    id = id,
    pages = pages,
    importedPdfPath = importedPdfPath,
    importedPdfPageCount = importedPdfPageCount,
    suggestedName = suggestedName
)

private fun ScanPageEntity.toDomain(): ScanPage = ScanPage(
    id = id,
    originalImagePath = originalImagePath,
    corners = Quad(
        topLeft = NormalizedPoint(cropTlX, cropTlY),
        topRight = NormalizedPoint(cropTrX, cropTrY),
        bottomRight = NormalizedPoint(cropBrX, cropBrY),
        bottomLeft = NormalizedPoint(cropBlX, cropBlY)
    ),
    rotationDegrees = rotationDegrees,
    filter = ScanFilter.valueOf(filter),
    orderIndex = orderIndex
)
