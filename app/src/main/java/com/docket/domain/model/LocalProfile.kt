package com.docket.domain.model

/**
 * A local-only "profile" — just a display name and email the user optionally sets so
 * Account/Personal Info has something to show. Never transmitted anywhere; see
 * [com.docket.domain.repository.ProfileRepository]'s class doc and PrivacyScreen.
 */
data class LocalProfile(val displayName: String, val email: String)
