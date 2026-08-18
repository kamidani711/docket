package com.docket.domain.repository

import com.docket.domain.model.MonthlySpend
import com.docket.domain.model.ParsedReceipt
import com.docket.domain.model.Receipt
import com.docket.domain.model.ReceiptFilter
import kotlinx.coroutines.flow.Flow

interface ReceiptRepository {
    fun observeReceiptForDocument(documentId: Long): Flow<Receipt?>
    suspend fun getReceiptForDocument(documentId: Long): Receipt?

    /** Saves (or overwrites) the receipt for [documentId] from a user-confirmed [parsed] form. */
    suspend fun saveReceipt(documentId: Long, parsed: ParsedReceipt): Long

    fun observeAllReceipts(filter: ReceiptFilter): Flow<List<Receipt>>
    fun observeMonthlySpend(): Flow<List<MonthlySpend>>
    fun observeMerchants(): Flow<List<String>>
}
