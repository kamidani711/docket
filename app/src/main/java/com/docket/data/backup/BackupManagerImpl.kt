package com.docket.data.backup

import android.content.Context
import android.net.Uri
import com.docket.data.local.DocketDatabase
import com.docket.domain.common.Result
import com.docket.domain.repository.BackupManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * No third-party archive/crypto library — `java.util.zip` and `javax.crypto` are both JDK
 * standard. Archive layout: `MAGIC | salt | iv | AES-256-GCM(zip(docket.db + documents/))`.
 * The key is derived from the user's passphrase via PBKDF2 — never stored anywhere, so losing
 * the passphrase means losing the backup. That's the correct tradeoff for "we never touch it."
 */
class BackupManagerImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val database: DocketDatabase
) : BackupManager {

    override suspend fun createBackup(
        destinationUri: Uri,
        passphrase: CharArray,
        onProgress: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val tempZip = File.createTempFile("docket_backup", ".zip", appContext.cacheDir)
        try {
            onProgress("Preparing…")
            // Flush WAL into the main file so the copy below is a consistent snapshot, not a
            // main-file-without-its-pending-writes.
            database.query("PRAGMA wal_checkpoint(FULL)", null).close()

            onProgress("Packaging files…")
            ZipOutputStream(tempZip.outputStream().buffered()).use { zip ->
                addFileToZip(zip, appContext.getDatabasePath("docket.db"), "docket.db")
                val documentsDir = File(appContext.filesDir, "documents")
                if (documentsDir.exists()) addDirectoryToZip(zip, documentsDir, "documents")
            }

            onProgress("Encrypting…")
            val salt = randomBytes(SALT_LENGTH)
            val iv = randomBytes(IV_LENGTH)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(GCM_TAG_BITS, iv))

            val output = appContext.contentResolver.openOutputStream(destinationUri)
                ?: error("Could not open the destination file")
            output.use { out ->
                out.write(MAGIC)
                out.write(salt)
                out.write(iv)
                CipherOutputStream(out, cipher).use { cipherOut ->
                    tempZip.inputStream().use { it.copyTo(cipherOut) }
                }
            }
            onProgress("Done")
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error(t)
        } finally {
            tempZip.delete()
        }
    }

    override suspend fun restoreBackup(
        sourceUri: Uri,
        passphrase: CharArray,
        onProgress: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val staging = PendingRestoreApplier.stagingDir(appContext)
        try {
            onProgress("Reading archive…")
            staging.deleteRecursively()
            staging.mkdirs()

            val input = appContext.contentResolver.openInputStream(sourceUri)
                ?: error("Could not open that file")

            input.use { rawIn ->
                val magic = ByteArray(MAGIC.size)
                if (rawIn.readFully(magic) < magic.size || !magic.contentEquals(MAGIC)) {
                    return@withContext Result.Error(IllegalArgumentException("That doesn't look like a Docket backup file."))
                }
                val salt = ByteArray(SALT_LENGTH).also { rawIn.readFully(it) }
                val iv = ByteArray(IV_LENGTH).also { rawIn.readFully(it) }
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(GCM_TAG_BITS, iv))

                onProgress("Decrypting…")
                CipherInputStream(rawIn, cipher).use { cipherIn ->
                    ZipInputStream(cipherIn).use { zip -> extractZip(zip, staging) }
                }
            }

            if (!File(staging, "docket.db").exists()) {
                staging.deleteRecursively()
                return@withContext Result.Error(IllegalArgumentException("That archive doesn't contain a Docket database — nothing was restored."))
            }

            onProgress("Ready — restart Docket to finish importing")
            Result.Success(Unit)
        } catch (t: AEADBadTagException) {
            staging.deleteRecursively()
            Result.Error(IllegalArgumentException("Incorrect passphrase, or the file is corrupted."))
        } catch (t: Throwable) {
            staging.deleteRecursively()
            Result.Error(t)
        }
    }

    private fun extractZip(zip: ZipInputStream, destinationDir: File) {
        var entry = zip.nextEntry
        while (entry != null) {
            val outFile = File(destinationDir, entry.name)
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                outFile.outputStream().use { zip.copyTo(it) }
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    private fun addFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists()) return
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun addDirectoryToZip(zip: ZipOutputStream, dir: File, entryPrefix: String) {
        dir.listFiles()?.forEach { child ->
            val name = "$entryPrefix/${child.name}"
            if (child.isDirectory) addDirectoryToZip(zip, child, name) else addFileToZip(zip, child, name)
        }
    }

    private fun randomBytes(length: Int): ByteArray = ByteArray(length).also { SecureRandom().nextBytes(it) }

    /** [InputStream.read] can return fewer bytes than requested even mid-stream; loops until
     *  [buffer] is full or the stream ends. Returns bytes actually read. */
    private fun InputStream.readFully(buffer: ByteArray): Int {
        var offset = 0
        while (offset < buffer.size) {
            val read = read(buffer, offset, buffer.size - offset)
            if (read == -1) break
            offset += read
        }
        return offset
    }

    private companion object {
        val MAGIC = "DOCKETBK1".toByteArray(Charsets.US_ASCII)
        const val SALT_LENGTH = 16
        const val IV_LENGTH = 12
        const val GCM_TAG_BITS = 128
        const val PBKDF2_ITERATIONS = 150_000
        const val KEY_LENGTH_BITS = 256
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
