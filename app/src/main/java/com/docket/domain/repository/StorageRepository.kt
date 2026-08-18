package com.docket.domain.repository

import com.docket.domain.model.StorageBreakdown

/** Reads/clears on-device storage — no DataStore involved, these are live filesystem stats. */
interface StorageRepository {
    suspend fun getStorageBreakdown(): StorageBreakdown

    /** Deletes the system cache dir and any stale export-staging files. Never touches
     *  `documents/`, the database, or a pending backup restore. */
    suspend fun clearCache()
}
