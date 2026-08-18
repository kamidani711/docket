package com.docket.data.backup

import android.content.Context
import java.io.File

/**
 * Applies a staged restore (see [com.docket.data.backup.BackupManagerImpl.restoreBackup])
 * by overwriting the real database file and documents directory. Called from
 * DocketApplication.attachBaseContext() — deliberately before Hilt injection or anything else
 * has a chance to open the database. Swapping the DB file while Room already has it open would
 * risk corrupting exactly the data a restore is supposed to protect; attachBaseContext is the
 * earliest point a Context is usable and runs before that can happen.
 */
object PendingRestoreApplier {
    private const val STAGING_DIR_NAME = "pending_restore"

    fun stagingDir(context: Context): File = File(context.filesDir, STAGING_DIR_NAME)

    fun applyIfPending(context: Context) {
        val staging = stagingDir(context)
        val stagedDb = File(staging, "docket.db")
        if (!stagedDb.exists()) return

        val realDbFile = context.getDatabasePath("docket.db")
        realDbFile.parentFile?.mkdirs()
        stagedDb.copyTo(realDbFile, overwrite = true)
        // Stale WAL/SHM files from the previous database would otherwise get replayed against
        // the freshly-restored main file.
        File(realDbFile.path + "-wal").delete()
        File(realDbFile.path + "-shm").delete()

        val stagedDocuments = File(staging, "documents")
        val realDocuments = File(context.filesDir, "documents")
        if (stagedDocuments.exists()) {
            realDocuments.deleteRecursively()
            stagedDocuments.copyRecursively(realDocuments, overwrite = true)
        }

        staging.deleteRecursively()
    }
}
