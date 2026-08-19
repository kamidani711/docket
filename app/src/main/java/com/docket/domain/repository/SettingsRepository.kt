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

    /** Whether the first-launch onboarding carousel has already been shown once. */
    val hasSeenOnboarding: Flow<Boolean>
    suspend fun setOnboardingSeen()

    /** `null` = not yet set, caller falls back to the system dark-theme setting for that one
     *  read (same "null = loading, don't guess" convention as [appLockEnabled]'s consumers) —
     *  once the user touches the Dark Mode toggle this becomes an explicit override. */
    val darkModeOverride: Flow<Boolean?>
    suspend fun setDarkModeOverride(enabled: Boolean)
}
