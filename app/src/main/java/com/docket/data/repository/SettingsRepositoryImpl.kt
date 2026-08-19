package com.docket.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.docket.data.settings.SettingsKeys
import com.docket.domain.model.DocumentSort
import com.docket.domain.model.ExportFormat
import com.docket.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val defaultExportFormat: Flow<ExportFormat> = dataStore.data.map { prefs ->
        prefs[SettingsKeys.DEFAULT_EXPORT_FORMAT]?.let { name ->
            runCatching { ExportFormat.valueOf(name) }.getOrNull()
        } ?: ExportFormat.PDF
    }

    override suspend fun setDefaultExportFormat(format: ExportFormat) {
        dataStore.edit { prefs -> prefs[SettingsKeys.DEFAULT_EXPORT_FORMAT] = format.name }
    }

    override val defaultLibrarySort: Flow<DocumentSort> = dataStore.data.map { prefs ->
        prefs[SettingsKeys.DEFAULT_LIBRARY_SORT]?.let { name ->
            runCatching { DocumentSort.valueOf(name) }.getOrNull()
        } ?: DocumentSort.DATE
    }

    override suspend fun setDefaultLibrarySort(sort: DocumentSort) {
        dataStore.edit { prefs -> prefs[SettingsKeys.DEFAULT_LIBRARY_SORT] = sort.name }
    }

    override val appLockEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[SettingsKeys.APP_LOCK_ENABLED] ?: false }

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[SettingsKeys.APP_LOCK_ENABLED] = enabled }
    }

    override val hasSeenOnboarding: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[SettingsKeys.HAS_SEEN_ONBOARDING] ?: false }

    override suspend fun setOnboardingSeen() {
        dataStore.edit { prefs -> prefs[SettingsKeys.HAS_SEEN_ONBOARDING] = true }
    }

    override val darkModeOverride: Flow<Boolean?> =
        dataStore.data.map { prefs -> prefs[SettingsKeys.DARK_MODE_OVERRIDE] }

    override suspend fun setDarkModeOverride(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[SettingsKeys.DARK_MODE_OVERRIDE] = enabled }
    }
}
