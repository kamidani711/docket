package com.docket.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/** Single DataStore for the app's settings/unlock state — premium flag, installed OCR
 *  language packs. Small, low-churn key/value data; Room is for actual content. */
val Context.docketDataStore by preferencesDataStore(name = "docket_settings")

internal object SettingsKeys {
    val IS_PREMIUM = booleanPreferencesKey("is_premium")
    val INSTALLED_OCR_LANGUAGES = stringSetPreferencesKey("installed_ocr_languages")
    val DEFAULT_EXPORT_FORMAT = stringPreferencesKey("default_export_format")
    val DEFAULT_LIBRARY_SORT = stringPreferencesKey("default_library_sort")
    val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
}
