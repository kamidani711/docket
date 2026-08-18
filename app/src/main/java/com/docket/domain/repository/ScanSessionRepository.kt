package com.docket.domain.repository

import com.docket.domain.model.Quad
import com.docket.domain.model.ScanFilter
import com.docket.domain.model.ScanSession
import kotlinx.coroutines.flow.Flow

/**
 * The single in-progress scan draft, backed by Room. Every mutation here is a direct write to
 * disk (SQLite) — that's the entire process-death recovery story: there's no separate "session
 * cache" to fall out of sync with, [observeSession] just reflects whatever's on disk.
 */
interface ScanSessionRepository {
    fun observeSession(): Flow<ScanSession?>

    /** Clears any existing session and starts a fresh one with [originalImagePaths] as pages. */
    suspend fun startImageSession(originalImagePaths: List<String>): String

    /** Clears any existing session and starts a fresh PDF-import session (no pages to edit). */
    suspend fun startPdfImportSession(pdfPath: String, pageCount: Int): String

    /** Appends newly captured/imported pages to the end of an existing session. */
    suspend fun appendPages(sessionId: String, originalImagePaths: List<String>)

    suspend fun updateCorners(pageId: String, corners: Quad)
    /** Relative, not absolute — [deltaDegrees] is added to whatever rotation is already stored,
     *  atomically, in SQL. Deliberately not "set rotation to X": that shape is what let rapid
     *  taps race against each other and lose an increment (see ScanPageDao.rotateBy). */
    suspend fun rotatePage(pageId: String, deltaDegrees: Int)
    suspend fun updateFilter(pageId: String, filter: ScanFilter)
    suspend fun reorderPages(sessionId: String, orderedPageIds: List<String>)
    suspend fun deletePage(pageId: String)

    /** Retake: swaps in a freshly captured image and resets edit params to defaults. */
    suspend fun replacePageImage(pageId: String, newOriginalImagePath: String)

    suspend fun updateSuggestedName(sessionId: String, name: String)

    /** Call after a successful save, or an explicit "discard scan" — deletes session + pages
     *  (page originals on disk are the caller's responsibility; see ExportScanSessionUseCase). */
    suspend fun clearSession(sessionId: String)
}
