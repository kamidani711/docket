package com.docket.domain.repository

import android.graphics.Bitmap
import com.docket.domain.model.OcrLanguage
import com.docket.domain.model.RecognizedText

/**
 * Distinct from [TextRecognizer] (Latin-only, used for the lightweight scan-name suggestion):
 * this runs every recognizer in [languages] against the same bitmap and keeps whichever result
 * has the most recognized characters — there's no on-device script *detection* in ML Kit, so
 * "run everything installed, keep the best-looking result" is the pragmatic stand-in. See
 * `data/mlkit/MultiScriptTextRecognizer.kt` for the caveats that come with that.
 */
interface MultiScriptTextRecognizer {
    suspend fun recognize(bitmap: Bitmap, languages: List<OcrLanguage>): RecognizedText
}
