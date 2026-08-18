package com.docket.domain.model

/** [parentFolderId] non-null means this is a subfolder — exactly one level deep is enforced
 *  where folders are created, not here. */
data class Folder(
    val id: Long,
    val name: String,
    val parentFolderId: Long?,
    val createdAt: Long
)
