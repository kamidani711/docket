package com.docket.domain.repository

import com.docket.domain.model.DocumentSort
import com.docket.domain.model.ExportFormat
import kotlinx.coroutines.flow.Flow

/**
 * App-wide defaults and the optional app-lock toggle — small, low-churn preferences on the same
 * DataStore-backed pattern as [PremiumRepository], just for settings that aren't premium-gated.
 * App lock's actual authentication (BiometricPrompt) is a UI-layer concern handled in
 * `ui/lock` — this only stores whether it's turned on.
 */
interface SettingsRepository {
    val defaultExportFormat: Flow<ExportFormat>
    suspend fun setDefaultExportFormat(format: ExportFormat)

    val defaultLibrarySort: Flow<DocumentSort>
    suspend fun setDefaultLibrarySort(sort: DocumentSort)

    val appLockEnabled: Flow<Boolean>
    suspend fun setAppLockEnabled(enabled: Boolean)
}
