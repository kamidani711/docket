package com.docket.data.repository

import com.docket.data.local.dao.WarrantyDao
import com.docket.data.local.entity.WarrantyEntity
import com.docket.domain.model.Warranty
import com.docket.domain.repository.WarrantyRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class WarrantyRepositoryImpl @Inject constructor(
    private val warrantyDao: WarrantyDao
) : WarrantyRepository {

    override fun observeWarrantiesSortedByExpiry(): Flow<List<Warranty>> =
        warrantyDao.observeSortedByExpiry().map { list -> list.map { it.toDomain() } }

    override fun observeWarrantyForDocument(documentId: Long): Flow<Warranty?> =
        warrantyDao.observeForDocument(documentId).map { it?.toDomain() }

    override suspend fun getWarranty(warrantyId: Long): Warranty? =
        warrantyDao.get(warrantyId)?.toDomain()

    override suspend fun getWarrantyForDocument(documentId: Long): Warranty? =
        warrantyDao.getForDocument(documentId)?.toDomain()

    override suspend fun saveWarranty(
        documentId: Long,
        itemName: String,
        purchaseDate: Long,
        expiryDate: Long,
        durationMonths: Int
    ): Long {
        warrantyDao.getIdForDocument(documentId)?.let { existingId -> warrantyDao.delete(existingId) }
        return warrantyDao.insert(
            WarrantyEntity(
                documentId = documentId,
                itemName = itemName,
                purchaseDate = purchaseDate,
                expiryDate = expiryDate,
                durationMonths = durationMonths,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun deleteWarranty(warrantyId: Long) {
        warrantyDao.delete(warrantyId)
    }
}

private fun WarrantyEntity.toDomain(): Warranty = Warranty(
    id = id,
    documentId = documentId,
    itemName = itemName,
    purchaseDate = purchaseDate,
    expiryDate = expiryDate,
    durationMonths = durationMonths,
    createdAt = createdAt
)
