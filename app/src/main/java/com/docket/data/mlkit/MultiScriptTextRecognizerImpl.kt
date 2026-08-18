package com.docket.data.mlkit

import android.graphics.Bitmap
import android.graphics.Color
import com.docket.domain.model.OcrLanguage
import com.docket.domain.model.OcrWord
import com.docket.domain.model.RecognizedText
import com.docket.domain.repository.MultiScriptTextRecognizer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * ML Kit has no on-device script *detection* — each recognizer only knows its own script, and
 * running the wrong one on an image just produces low/no output rather than an error. So:
 * run every *installed* language's recognizer against the page and keep whichever result has
 * the most recognized (non-whitespace) characters, as a stand-in for "which one actually read
 * this page." Good enough for search indexing; not a substitute for real script detection if
 * that ever matters more (e.g. mixed-script documents will only get whichever script "wins").
 */
@Singleton
class MultiScriptTextRecognizerImpl @Inject constructor() : MultiScriptTextRecognizer {

    private val clients = mutableMapOf<OcrLanguage, com.google.mlkit.vision.text.TextRecognizer>()

    override suspend fun recognize(bitmap: Bitmap, languages: List<OcrLanguage>): RecognizedText {
        val candidates = languages.ifEmpty { listOf(OcrLanguage.LATIN) }
        var best: RecognizedText? = null
        var bestScore = -1

        for (language in candidates) {
            val result = runCatching { runSingle(bitmap, language) }.getOrNull() ?: continue
            val score = result.text.count { !it.isWhitespace() }
            if (score > bestScore) {
                best = result
                bestScore = score
            }
        }
        return best ?: RecognizedText(text = "", words = emptyList(), language = OcrLanguage.LATIN)
    }

    /**
     * Forces the model download for an unbundled script pack to happen now, rather than
     * silently on the next real OCR call — see the chat notes on why this (running real OCR on
     * a throwaway blank bitmap) is the download trigger instead of a dedicated "download this
     * model" API: I couldn't confirm ML Kit exposes `RemoteModelManager`-style pre-download for
     * text-recognition script packs specifically, and this approach is one I'm confident
     * actually works mechanically, since it's exactly how "unbundled" ML Kit features have
     * always behaved (first call downloads, then processes).
     */
    suspend fun warmUp(language: OcrLanguage): Boolean {
        val placeholder = Bitmap.createBitmap(WARMUP_SIZE, WARMUP_SIZE, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
        return try {
            runSingle(placeholder, language)
            true
        } catch (t: Throwable) {
            false
        } finally {
            placeholder.recycle()
        }
    }

    private suspend fun runSingle(bitmap: Bitmap, language: OcrLanguage): RecognizedText {
        val image = InputImage.fromBitmap(bitmap, 0)
        val text = suspendCancellableCoroutine { continuation ->
            clientFor(language).process(image)
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }
        return RecognizedText(
            text = text.text,
            words = extractWords(text, bitmap.width, bitmap.height),
            language = language
        )
    }

    private fun extractWords(text: Text, bitmapWidth: Int, bitmapHeight: Int): List<OcrWord> {
        if (bitmapWidth <= 0 || bitmapHeight <= 0) return emptyList()
        return text.textBlocks
            .flatMap { it.lines }
            .flatMap { it.elements }
            .mapNotNull { element ->
                val box = element.boundingBox ?: return@mapNotNull null
                OcrWord(
                    text = element.text,
                    leftFrac = box.left.toFloat() / bitmapWidth,
                    topFrac = box.top.toFloat() / bitmapHeight,
                    rightFrac = box.right.toFloat() / bitmapWidth,
                    bottomFrac = box.bottom.toFloat() / bitmapHeight
                )
            }
    }

    private fun clientFor(language: OcrLanguage) = clients.getOrPut(language) {
        val options = when (language) {
            OcrLanguage.LATIN -> TextRecognizerOptions.DEFAULT_OPTIONS
            OcrLanguage.CHINESE -> ChineseTextRecognizerOptions.Builder().build()
            OcrLanguage.DEVANAGARI -> DevanagariTextRecognizerOptions.Builder().build()
            OcrLanguage.JAPANESE -> JapaneseTextRecognizerOptions.Builder().build()
            OcrLanguage.KOREAN -> KoreanTextRecognizerOptions.Builder().build()
        }
        TextRecognition.getClient(options)
    }

    private companion object {
        const val WARMUP_SIZE = 32
    }
}
