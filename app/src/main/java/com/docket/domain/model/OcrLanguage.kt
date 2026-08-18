package com.docket.domain.model

/**
 * Script models ML Kit Text Recognition v2 can run. [LATIN] is always available — it's the
 * bundled model already shipped in the APK (see the project build notes). The rest are
 * unbundled, Play-services-delivered packs the user installs on demand from Settings.
 */
enum class OcrLanguage(val displayName: String, val alwaysAvailable: Boolean, val approxDownloadMb: Int) {
    LATIN("Latin", alwaysAvailable = true, approxDownloadMb = 0),
    DEVANAGARI("Devanagari (Hindi, Marathi, ...)", alwaysAvailable = false, approxDownloadMb = 25),
    CHINESE("Chinese", alwaysAvailable = false, approxDownloadMb = 25),
    JAPANESE("Japanese", alwaysAvailable = false, approxDownloadMb = 25),
    KOREAN("Korean", alwaysAvailable = false, approxDownloadMb = 25)
}
