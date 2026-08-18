package com.docket.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** [parentFolderId] allows exactly one level of nesting — enforced in the create-folder UI
 *  (refuses to create a folder inside a folder that already has a parent), not at the DB
 *  level, per "do not over-engineer this." */
@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentFolderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentFolderId")]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentFolderId: Long? = null,
    val createdAt: Long
)
