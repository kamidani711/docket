package com.docket.domain.repository

import com.docket.domain.model.LocalProfile
import kotlinx.coroutines.flow.Flow

/**
 * An entirely optional, entirely local "account." Docket has no server and no real
 * authentication — see PrivacyScreen — so this is not a security boundary: the passphrase is
 * hashed and checked locally only so "Sign in" has something real to verify against, the same
 * way a note-taking app might gate a local passcode. [com.docket.ui.lock.AppLockGate]'s
 * biometric/device-credential lock remains the actual device-security feature.
 *
 * Nothing here is ever required — Home/Files/Premium are fully usable with [profile] == null.
 * Reachable only from the Account tab's guest-state prompt (Sign in / Create account).
 */
interface ProfileRepository {
    val profile: Flow<LocalProfile?>
    val isSignedIn: Flow<Boolean>

    /**
     * Creates (or overwrites) the local profile and signs in immediately. Used by both Sign up
     * and "reset your local profile" — a fresh save *is* the honest local equivalent of a
     * password reset, since there's no email to actually send a reset link through.
     */
    suspend fun saveProfile(displayName: String, email: String, passphrase: String)

    /** Returns true and marks signed-in if [email]+[passphrase] match the stored profile. */
    suspend fun signIn(email: String, passphrase: String): Boolean

    /** Clears the signed-in flag only — the stored profile is kept so signing back in doesn't
     *  lose it, matching ordinary "log out" semantics rather than deleting the account. */
    suspend fun signOut()

    suspend fun updatePersonalInfo(displayName: String, email: String)

    /** Deletes the local profile entirely (name, email, and passphrase). */
    suspend fun deleteProfile()
}
