package com.docket.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "receipt_line_items",
    foreignKeys = [
        ForeignKey(
            entity = ReceiptEntity::class,
            parentColumns = ["id"],
            childColumns = ["receiptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("receiptId")]
)
data class ReceiptLineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptId: Long,
    val description: String,
    val amountCents: Long?,
    val orderIndex: Int
)
