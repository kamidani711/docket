package com.docket.domain.repository

import com.docket.domain.model.Warranty
import kotlinx.coroutines.flow.Flow

interface WarrantyRepository {
    fun observeWarrantiesSortedByExpiry(): Flow<List<Warranty>>
    fun observeWarrantyForDocument(documentId: Long): Flow<Warranty?>
    suspend fun getWarranty(warrantyId: Long): Warranty?
    suspend fun getWarrantyForDocument(documentId: Long): Warranty?

    suspend fun saveWarranty(
        documentId: Long,
        itemName: String,
        purchaseDate: Long,
        expiryDate: Long,
        durationMonths: Int
    ): Long

    suspend fun deleteWarranty(warrantyId: Long)
}
