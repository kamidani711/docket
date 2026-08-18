package com.docket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.docket.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<FolderEntity>>

    /** [parentFolderId] null = top-level folders. */
    @Query("SELECT * FROM folders WHERE (:parentFolderId IS NULL AND parentFolderId IS NULL) OR parentFolderId = :parentFolderId ORDER BY name COLLATE NOCASE ASC")
    fun observeByParent(parentFolderId: Long?): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :folderId")
    suspend fun get(folderId: Long): FolderEntity?

    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Query("SELECT COUNT(*) FROM folders")
    suspend fun count(): Int
}
