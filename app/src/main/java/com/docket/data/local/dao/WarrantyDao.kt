package com.docket.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.docket.data.local.entity.WarrantyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WarrantyDao {
    @Insert
    suspend fun insert(warranty: WarrantyEntity): Long

    @Query("DELETE FROM warranties WHERE id = :warrantyId")
    suspend fun delete(warrantyId: Long)

    @Query("SELECT * FROM warranties ORDER BY expiryDate ASC")
    fun observeSortedByExpiry(): Flow<List<WarrantyEntity>>

    @Query("SELECT * FROM warranties WHERE documentId = :documentId LIMIT 1")
    fun observeForDocument(documentId: Long): Flow<WarrantyEntity?>

    @Query("SELECT * FROM warranties WHERE id = :warrantyId LIMIT 1")
    suspend fun get(warrantyId: Long): WarrantyEntity?

    @Query("SELECT id FROM warranties WHERE documentId = :documentId LIMIT 1")
    suspend fun getIdForDocument(documentId: Long): Long?

    @Query("SELECT * FROM warranties WHERE documentId = :documentId LIMIT 1")
    suspend fun getForDocument(documentId: Long): WarrantyEntity?
}
