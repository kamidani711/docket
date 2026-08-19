package com.docket.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.docket.data.settings.ProfileKeys
import com.docket.domain.model.LocalProfile
import com.docket.domain.repository.ProfileRepository
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed, same pattern as [SettingsRepositoryImpl]. The passphrase is never stored in
 * the clear — PBKDF2WithHmacSHA256 (same algorithm/iteration count as [com.docket.data.backup
 * .BackupManagerImpl]'s real encryption key derivation, reused here even though this isn't a
 * security boundary, simply because it's already the project's proven local KDF).
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ProfileRepository {

    override val profile: Flow<LocalProfile?> = dataStore.data.map { prefs ->
        val name = prefs[ProfileKeys.DISPLAY_NAME]
        val email = prefs[ProfileKeys.EMAIL]
        if (name != null && email != null) LocalProfile(name, email) else null
    }

    override val isSignedIn: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[ProfileKeys.IS_SIGNED_IN] ?: false }

    override suspend fun saveProfile(displayName: String, email: String, passphrase: String) {
        val salt = randomBytes(SALT_LENGTH)
        val hash = deriveHash(passphrase, salt)
        dataStore.edit { prefs ->
            prefs[ProfileKeys.DISPLAY_NAME] = displayName
            prefs[ProfileKeys.EMAIL] = email
            prefs[ProfileKeys.PASSPHRASE_SALT] = salt.toHex()
            prefs[ProfileKeys.PASSPHRASE_HASH] = hash.toHex()
            prefs[ProfileKeys.IS_SIGNED_IN] = true
        }
    }

    override suspend fun signIn(email: String, passphrase: String): Boolean {
        val prefs = dataStore.data.first()
        val storedEmail = prefs[ProfileKeys.EMAIL] ?: return false
        val saltHex = prefs[ProfileKeys.PASSPHRASE_SALT] ?: return false
        val hashHex = prefs[ProfileKeys.PASSPHRASE_HASH] ?: return false
        if (!storedEmail.equals(email, ignoreCase = true)) return false

        val candidateHash = deriveHash(passphrase, saltHex.fromHex()).toHex()
        val matches = constantTimeEquals(candidateHash, hashHex)
        if (matches) dataStore.edit { it[ProfileKeys.IS_SIGNED_IN] = true }
        return matches
    }

    override suspend fun signOut() {
        dataStore.edit { prefs -> prefs[ProfileKeys.IS_SIGNED_IN] = false }
    }

    override suspend fun updatePersonalInfo(displayName: String, email: String) {
        dataStore.edit { prefs ->
            prefs[ProfileKeys.DISPLAY_NAME] = displayName
            prefs[ProfileKeys.EMAIL] = email
        }
    }

    override suspend fun deleteProfile() {
        dataStore.edit { prefs ->
            prefs.remove(ProfileKeys.DISPLAY_NAME)
            prefs.remove(ProfileKeys.EMAIL)
            prefs.remove(ProfileKeys.PASSPHRASE_SALT)
            prefs.remove(ProfileKeys.PASSPHRASE_HASH)
            prefs.remove(ProfileKeys.IS_SIGNED_IN)
        }
    }

    private fun deriveHash(passphrase: String, salt: ByteArray): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return factory.generateSecret(spec).encoded
    }

    private fun randomBytes(length: Int): ByteArray = ByteArray(length).also { SecureRandom().nextBytes(it) }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    /** Avoids a timing side-channel on the hash comparison — low-stakes here (no server, no
     *  attacker-controlled repeated attempts), but costs nothing to do properly. */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) result = result or (a[i].code xor b[i].code)
        return result == 0
    }

    private companion object {
        const val SALT_LENGTH = 16
        const val PBKDF2_ITERATIONS = 150_000
        const val KEY_LENGTH_BITS = 256
    }
}
