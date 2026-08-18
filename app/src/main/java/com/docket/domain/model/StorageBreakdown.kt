package com.docket.domain.model

/**
 * On-device storage usage, in bytes, for the Settings storage screen. [cacheBytes] covers
 * everything safe to delete without losing user data — the system cache directory plus this
 * app's own transient export staging files (`filesDir/exports/<uuid>`, recreated fresh on every
 * share/export and only left behind if a share sheet was cancelled).
 */
data class StorageBreakdown(
    val documentsBytes: Long,
    val databaseBytes: Long,
    val cacheBytes: Long
) {
    val totalBytes: Long get() = documentsBytes + databaseBytes + cacheBytes
}
