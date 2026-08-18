package com.docket.domain.repository

import com.docket.domain.model.ImageExportRequest
import com.docket.domain.model.ImageExportResult

interface ImageExporter {
    suspend fun exportImages(request: ImageExportRequest): ImageExportResult
}
