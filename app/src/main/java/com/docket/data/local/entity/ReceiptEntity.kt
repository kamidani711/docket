package com.docket.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One receipt per document (unique index on [documentId]) — "tagging a scan as a receipt"
 *  creates this row; re-tagging replaces it (see ReceiptRepositoryImpl.saveReceipt). Amounts
 *  are integer cents — see the [com.docket.domain.model.Receipt] doc. */
@Entity(
    tableName = "receipts",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId", unique = true)]
)
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val merchant: String,
    val totalAmountCents: Long?,
    val currencyCode: String?,
    val purchaseDate: Long?,
    val paymentMethod: String?,
    val createdAt: Long
)
