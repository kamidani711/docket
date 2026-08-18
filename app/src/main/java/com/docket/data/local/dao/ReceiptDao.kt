package com.docket.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.docket.data.local.entity.ReceiptEntity
import com.docket.data.local.entity.ReceiptLineItemEntity
import kotlinx.coroutines.flow.Flow

data class ReceiptWithItems(
    @Embedded val receipt: ReceiptEntity,
    @Relation(parentColumn = "id", entityColumn = "receiptId")
    val lineItems: List<ReceiptLineItemEntity>
)

data class MonthlySpendRow(
    val yearMonth: String,
    val totalCents: Long,
    val currencyCode: String?
)

@Dao
interface ReceiptDao {
    @Insert
    suspend fun insert(receipt: ReceiptEntity): Long

    @Insert
    suspend fun insertLineItems(items: List<ReceiptLineItemEntity>)

    @Query("SELECT id FROM receipts WHERE documentId = :documentId LIMIT 1")
    suspend fun getReceiptId(documentId: Long): Long?

    @Query("DELETE FROM receipts WHERE id = :receiptId")
    suspend fun delete(receiptId: Long)

    /** Replace-on-save: simpler and safer than a field-by-field update given line items can
     *  change count/order entirely between edits — cascade delete removes the old line items. */
    @Transaction
    suspend fun replaceReceipt(documentId: Long, receipt: ReceiptEntity, lineItems: List<ReceiptLineItemEntity>): Long {
        getReceiptId(documentId)?.let { existingId -> delete(existingId) }
        val receiptId = insert(receipt)
        if (lineItems.isNotEmpty()) {
            insertLineItems(lineItems.map { it.copy(receiptId = receiptId) })
        }
        return receiptId
    }

    @Transaction
    @Query("SELECT * FROM receipts WHERE documentId = :documentId")
    fun observeWithItems(documentId: Long): Flow<ReceiptWithItems?>

    @Transaction
    @Query("SELECT * FROM receipts WHERE documentId = :documentId")
    suspend fun getWithItems(documentId: Long): ReceiptWithItems?

    @Query(
        """
        SELECT * FROM receipts
        WHERE (:merchantQuery IS NULL OR merchant LIKE '%' || :merchantQuery || '%')
          AND (:startDate IS NULL OR purchaseDate >= :startDate)
          AND (:endDate IS NULL OR purchaseDate <= :endDate)
          AND (:minAmountCents IS NULL OR totalAmountCents >= :minAmountCents)
          AND (:maxAmountCents IS NULL OR totalAmountCents <= :maxAmountCents)
        ORDER BY purchaseDate DESC
        """
    )
    fun observeFiltered(
        merchantQuery: String?,
        startDate: Long?,
        endDate: Long?,
        minAmountCents: Long?,
        maxAmountCents: Long?
    ): Flow<List<ReceiptEntity>>

    @Query(
        """
        SELECT strftime('%Y-%m', purchaseDate / 1000, 'unixepoch') AS yearMonth,
               SUM(totalAmountCents) AS totalCents,
               currencyCode
        FROM receipts
        WHERE purchaseDate IS NOT NULL AND totalAmountCents IS NOT NULL
        GROUP BY yearMonth, currencyCode
        ORDER BY yearMonth DESC
        """
    )
    fun observeMonthlySpend(): Flow<List<MonthlySpendRow>>

    @Query("SELECT DISTINCT merchant FROM receipts ORDER BY merchant COLLATE NOCASE ASC")
    fun observeMerchants(): Flow<List<String>>
}
