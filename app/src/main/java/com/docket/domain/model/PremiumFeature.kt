package com.docket.domain.model

/**
 * The power-user layer — everything NOT listed here (unlimited scans/pages, all filters, PDF
 * export, OCR, search) stays free forever. See the project brief: we charge for this layer,
 * not for the core.
 */
enum class PremiumFeature {
    BATCH_EXPORT,
    SEARCHABLE_PDF_TEXT_LAYER,
    ADDITIONAL_OCR_LANGUAGES,
    UNLIMITED_FOLDERS
}

/** Free tier cap — see [PremiumFeature.UNLIMITED_FOLDERS]. */
const val FREE_FOLDER_LIMIT = 3
