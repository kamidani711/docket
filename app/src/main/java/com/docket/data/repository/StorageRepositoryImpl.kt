package com.docket.data.repository

import android.content.Context
import com.docket.domain.model.StorageBreakdown
import com.docket.domain.repository.StorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class StorageRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context
) : StorageRepository {

    override suspend fun getStorageBreakdown(): StorageBreakdown = withContext(Dispatchers.IO) {
        val documentsBytes = dirSize(File(appContext.filesDir, "documents"))
        val databaseFile = appContext.getDatabasePath("docket.db")
        val databaseBytes = databaseFile.length() +
            File(databaseFile.path + "-wal").length() +
            File(databaseFile.path + "-shm").length()
        val cacheBytes = dirSize(appContext.cacheDir) + dirSize(File(appContext.filesDir, "exports"))
        StorageBreakdown(documentsBytes, databaseBytes, cacheBytes)
    }

    override suspend fun clearCache() = withContext(Dispatchers.IO) {
        appContext.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        File(appContext.filesDir, "exports").deleteRecursively()
        Unit
    }

    private fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
