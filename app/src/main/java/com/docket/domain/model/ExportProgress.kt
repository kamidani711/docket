package com.docket.domain.model

/** Emitted by [com.docket.domain.usecase.ExportScanSessionUseCase] while it works through pages. */
data class ExportProgress(
    val currentPage: Int,
    val totalPages: Int,
    val stage: Stage
) {
    enum class Stage { PROCESSING_PAGE, WRITING_FILE, SAVING_TO_LIBRARY }

    companion object {
        val Idle = ExportProgress(0, 0, Stage.PROCESSING_PAGE)
    }
}
