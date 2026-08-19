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
    val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
    /** `null` (absent) = follow the system setting; set once the user touches the toggle. */
    val DARK_MODE_OVERRIDE = booleanPreferencesKey("dark_mode_override")
    // Per-app language is NOT stored here — AppCompatDelegate.setApplicationLocales() already
    // persists it (AndroidX writes its own backing store), so LanguageScreen reads it back via
    // AppCompatDelegate.getApplicationLocales() directly rather than duplicating the value.
}

/** Keys for [com.docket.domain.repository.ProfileRepository] — the optional local-only profile.
 *  Kept in their own object (same DataStore instance) since it's a distinct, larger concern
 *  than the single-value settings above. */
internal object ProfileKeys {
    val DISPLAY_NAME = stringPreferencesKey("profile_display_name")
    val EMAIL = stringPreferencesKey("profile_email")
    val PASSPHRASE_HASH = stringPreferencesKey("profile_passphrase_hash")
    val PASSPHRASE_SALT = stringPreferencesKey("profile_passphrase_salt")
    val IS_SIGNED_IN = booleanPreferencesKey("profile_is_signed_in")
}
