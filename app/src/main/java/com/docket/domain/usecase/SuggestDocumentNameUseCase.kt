package com.docket.domain.usecase

import com.docket.domain.repository.DocumentRepository
import com.docket.domain.repository.ImageProcessor
import com.docket.domain.repository.TextRecognizer
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Suggests a document name from the first page's OCR text. Deliberately a loose heuristic, not
 * structured receipt parsing — this is a name *suggestion* the user can always edit, not the
 * dedicated receipt-parsing feature described in the project brief (which gets its own
 * `ReceiptEntity` fields and its own pass later).
 *
 * Naming priority:
 * 1. A validated OCR candidate line — a merchant name or document title actually printed on the
 *    page ("Carrefour Receipt", "Tenancy Contract"). [looksLikeName] exists specifically to
 *    reject OCR noise that isn't a name — a bare clock reading, a bare date, a barcode digit
 *    run — rather than blindly trusting whatever the first non-empty line happened to be.
 * 2. A human date fallback: "Scan – 17 Aug 2026".
 * 3. If another document was already created today with that exact fallback name, a counter is
 *    appended: "Scan 2 – 17 Aug 2026".
 *
 * This never returns a bare timestamp or a truncated OCR fragment — the failure mode that used
 * to surface as a document named "7:51 A" (a status-bar clock the naive old heuristic took
 * verbatim as line one).
 */
class SuggestDocumentNameUseCase @Inject constructor(
    private val imageProcessor: ImageProcessor,
    private val textRecognizer: TextRecognizer,
    private val documentRepository: DocumentRepository
) {
    suspend operator fun invoke(firstPageImagePath: String): String {
        val preview = imageProcessor.decodeDownsampled(firstPageImagePath, maxDimension = OCR_MAX_DIMENSION)
        val text = try {
            textRecognizer.recognizeText(preview)
        } finally {
            preview.recycle()
        }
        return suggestNameFrom(text)
    }

    /** The same fallback [invoke] uses when OCR finds nothing usable — exposed separately for
     *  sessions with no OCR source at all (a PDF import has nothing to run text recognition on
     *  without rendering a page first). Still never a bare date: "Scan – 17 Aug 2026", with a
     *  same-day counter if that name is already taken. */
    suspend fun suggestDefaultName(): String = defaultName()

    private suspend fun suggestNameFrom(text: String): String {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val looksLikeReceipt = CURRENCY_REGEX.containsMatchIn(text) ||
            text.contains("total", ignoreCase = true) ||
            text.contains("subtotal", ignoreCase = true) ||
            text.contains("receipt", ignoreCase = true)

        val candidate = lines.take(CANDIDATE_SCAN_LINES).firstOrNull { looksLikeName(it) }
            ?: return defaultName()

        val cleaned = cleanUp(candidate)
        val name = if (looksLikeReceipt && !cleaned.contains("receipt", ignoreCase = true)) {
            "$cleaned Receipt"
        } else {
            cleaned
        }
        return truncate(name)
    }

    /** Rejects the shape of OCR noise that produced the original "7:51 A" bug: a bare clock
     *  reading, a bare date, a barcode/phone-number digit run, or anything too short or too
     *  digit-heavy to plausibly be a name the user would recognise as theirs. Scans the first
     *  few OCR lines rather than assuming line one is always the name — logos and letterhead
     *  decoration often OCR as noise before the real title line. */
    private fun looksLikeName(line: String): Boolean {
        if (line.length < MIN_NAME_LENGTH) return false
        if (BARE_TIME_REGEX.matches(line)) return false
        if (DATE_REGEX.matches(line)) return false
        val letterCount = line.count { it.isLetter() }
        if (letterCount < MIN_LETTER_COUNT) return false
        if (letterCount.toFloat() / line.length < MIN_LETTER_RATIO) return false
        return true
    }

    /** Receipts are often printed in ALL CAPS ("CARREFOUR") — title-case a shouty line so the
     *  suggested name reads like a name rather than a label. Mixed-case lines are left exactly
     *  as printed since we can't improve on real title casing without risking it. */
    private fun cleanUp(line: String): String {
        val collapsed = line.replace(Regex("\\s+"), " ").trim()
        val hasLowercase = collapsed.any { it.isLowerCase() }
        return if (!hasLowercase && collapsed.any { it.isUpperCase() }) {
            collapsed.lowercase(Locale.getDefault())
                .split(" ")
                .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.getDefault()) } }
        } else {
            collapsed
        }
    }

    private fun truncate(name: String): String {
        if (name.length <= MAX_NAME_LENGTH) return name
        val cut = name.substring(0, MAX_NAME_LENGTH)
        val lastSpace = cut.lastIndexOf(' ')
        return if (lastSpace > MAX_NAME_LENGTH / 2) cut.substring(0, lastSpace) else cut
    }

    private suspend fun defaultName(): String {
        // Same locale-aware "medium" date style as ui/util/DateFormatting.kt's
        // formatDisplayDate — duplicated rather than shared because domain code can't depend
        // on a ui-layer utility, and this is genuinely the smallest correct fix.
        val today = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date())
        val base = "Scan – $today"

        val (dayStart, dayEnd) = todayRange()
        val existingTitles = documentRepository.getTitlesCreatedBetween(dayStart, dayEnd)
        val fallbackPattern = Regex("^Scan(?: (\\d+))? – ${Regex.escape(today)}$")
        val usedCounters = existingTitles.mapNotNull { title ->
            val match = fallbackPattern.find(title) ?: return@mapNotNull null
            match.groupValues[1].toIntOrNull() ?: 1
        }
        if (usedCounters.isEmpty()) return base
        return "Scan ${usedCounters.max() + 1} – $today"
    }

    private fun todayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val end = calendar.timeInMillis
        return start to end
    }

    private companion object {
        const val OCR_MAX_DIMENSION = 1024
        const val MAX_NAME_LENGTH = 60
        const val CANDIDATE_SCAN_LINES = 6
        const val MIN_NAME_LENGTH = 3
        const val MIN_LETTER_COUNT = 3
        const val MIN_LETTER_RATIO = 0.5f

        // Loose on purpose: catches "12/08/2026", "12-08-26", "Aug 12, 2026".
        val DATE_REGEX = Regex(
            "\\b(\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}|[A-Z][a-z]{2,8}\\s+\\d{1,2},?\\s+\\d{4})\\b"
        )
        val CURRENCY_REGEX = Regex("[$€£₹]\\s?\\d")

        // Catches "7:51", "7:51 A", "7:51 AM", "07:51:23 PM" — status-bar clocks and similar
        // OCR noise, exactly the shape of the original "7:51 A" bug.
        val BARE_TIME_REGEX = Regex("^\\d{1,2}:\\d{2}(:\\d{2})?\\s?[APap]?\\.?[Mm]?\\.?$")
    }
}
