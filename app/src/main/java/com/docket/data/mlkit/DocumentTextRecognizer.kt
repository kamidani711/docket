package com.docket.data.mlkit

import android.graphics.Bitmap
import com.docket.domain.repository.TextRecognizer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Wraps ML Kit's bundled (on-device, no download) Latin-script Text Recognizer. No
 * `kotlinx-coroutines-play-services` dependency — the Task→suspend bridge is hand-rolled
 * below, since we only need it in this one spot.
 */
@Singleton
class DocumentTextRecognizer @Inject constructor() : TextRecognizer {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override suspend fun recognizeText(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result -> continuation.resume(result.text) }
                // This only feeds a name *suggestion* — never fail the save flow over it.
                .addOnFailureListener { continuation.resume("") }
        }
    }
}
