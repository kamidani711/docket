package com.docket

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.docket.data.local.DocketDatabase
import com.docket.data.local.entity.DocumentEntity
import com.docket.data.local.entity.PageOcrEntity
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * NOT a correctness test — a runnable benchmark for the brief's "search must feel instant on a
 * 500-document library — measure and tell me the numbers" requirement. I have no device or
 * emulator in this environment, so I cannot produce real numbers myself; this is what to run to
 * get them:
 *
 *   ./gradlew connectedAndroidTest --tests "com.docket.SearchBenchmarkTest"
 *
 * ...then read the timings from logcat/the test output (each query prints its own line). Seeds
 * an in-memory DB with 500 documents x 3 pages of synthetic OCR text, then times FTS4 MATCH
 * queries against it — same table, same indexes, same query shape the app actually uses.
 */
@RunWith(AndroidJUnit4::class)
class SearchBenchmarkTest {

    private lateinit var db: DocketDatabase

    @Before
    fun setUp() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DocketDatabase::class.java
        ).allowMainThreadQueries().build()
        seedDocuments(documentCount = 500, pagesPerDocument = 3)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun measureSearchLatency() = runBlocking {
        // A mix of common-word (many matches to rank/snippet) and prefix queries, since those
        // stress different parts of FTS4 (postings list size vs. prefix scan).
        val queries = listOf("invoice", "total", "warranty*", "the*", "customer")

        for (query in queries) {
            val ftsQuery = query.split(" ").joinToString(" ") { term -> "text:$term" }
            var resultCount = 0
            val elapsedMs = measureTimeMillis {
                resultCount = db.ocrDao().search(
                    query = ftsQuery,
                    matchStart = MATCH_START,
                    matchEnd = MATCH_END,
                    limit = 50
                ).size
            }
            println("SearchBenchmark: query=\"$query\" -> $resultCount results in ${elapsedMs}ms")
        }
    }

    private suspend fun seedDocuments(documentCount: Int, pagesPerDocument: Int) {
        val words = listOf(
            "invoice", "total", "receipt", "warranty", "purchase", "date", "amount", "store",
            "electronics", "grocery", "return", "policy", "expires", "serial", "number", "the",
            "and", "for", "with", "customer"
        )
        repeat(documentCount) { docIndex ->
            val documentId = db.documentDao().insert(
                DocumentEntity(
                    title = "Document $docIndex",
                    folderId = null,
                    createdAt = System.currentTimeMillis(),
                    pageCount = pagesPerDocument,
                    format = "PDF",
                    pdfFilePath = null,
                    thumbnailPath = null
                )
            )
            repeat(pagesPerDocument) { pageIndex ->
                val text = (1..40).joinToString(" ") { words.random() }
                db.ocrDao().savePageText(
                    PageOcrEntity(
                        documentId = documentId,
                        pageIndex = pageIndex,
                        text = text,
                        wordsBlob = "",
                        language = "LATIN",
                        recognizedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // Must match SearchRepositoryImpl's markers exactly in *value*, not literally in source —
    // see that file for why these are built from character codes rather than typed directly.
    private companion object {
        val MATCH_START: String = 1.toChar().toString()
        val MATCH_END: String = 2.toChar().toString()
    }
}
