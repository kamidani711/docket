package com.docket.domain.repository

import android.net.Uri
import com.docket.domain.common.Result

/**
 * Whole-library encrypted export/import — the user picks the destination/source file via the
 * Storage Access Framework (their own Drive, SD card, wherever); this never touches the
 * network or knows where the file ends up. [passphrase] both encrypts the archive and is the
 * only thing that can decrypt it — there's no recovery if it's lost, by design (a
 * server-recoverable passphrase would mean a server involved at all).
 */
interface BackupManager {
    suspend fun createBackup(destinationUri: Uri, passphrase: CharArray, onProgress: (String) -> Unit): Result<Unit>

    /** Stages the decrypted archive; the actual swap-in happens at next app launch, before
     *  Room (or anything else) opens the database — see PendingRestoreApplier. */
    suspend fun restoreBackup(sourceUri: Uri, passphrase: CharArray, onProgress: (String) -> Unit): Result<Unit>
}
