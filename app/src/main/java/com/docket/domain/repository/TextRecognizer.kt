package com.docket.domain.repository

import android.graphics.Bitmap

/** Wraps ML Kit's (bundled, on-device) Text Recognition — see `data/mlkit`. */
interface TextRecognizer {
    suspend fun recognizeText(bitmap: Bitmap): String
}
