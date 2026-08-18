package com.docket.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.docket.data.settings.SettingsKeys
import com.docket.domain.model.PremiumFeature
import com.docket.domain.repository.PremiumRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Every gated feature just tracks the one premium flag — except [PremiumFeature.UNLIMITED_FOLDERS],
 * whose "unlocked" status also depends on the current folder count, not premium status alone.
 * That check lives with the caller (see `CreateFolderUseCase`) rather than here, since this
 * repository has no reason to know about folders.
 */
@Singleton
class PremiumRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PremiumRepository {

    override val isPremium: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[SettingsKeys.IS_PREMIUM] ?: false }

    override fun isFeatureUnlocked(feature: PremiumFeature): Flow<Boolean> = isPremium

    override suspend fun setPremiumUnlocked(unlocked: Boolean) {
        dataStore.edit { prefs -> prefs[SettingsKeys.IS_PREMIUM] = unlocked }
    }
}
