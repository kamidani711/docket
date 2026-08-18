package com.docket.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One warranty per document (unique index on [documentId]) — the linked receipt document
 *  the brief calls for. [expiryDate] is precomputed at save time (purchaseDate + duration)
 *  rather than derived on read, so reminder scheduling has a fixed target that doesn't shift
 *  if "now" changes. */
@Entity(
    tableName = "warranties",
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
data class WarrantyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val itemName: String,
    val purchaseDate: Long,
    val expiryDate: Long,
    val durationMonths: Int,
    val createdAt: Long
)
