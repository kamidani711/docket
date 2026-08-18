package com.docket.data.repository

import com.docket.data.local.dao.MonthlySpendRow
import com.docket.data.local.dao.ReceiptDao
import com.docket.data.local.dao.ReceiptWithItems
import com.docket.data.local.entity.ReceiptEntity
import com.docket.data.local.entity.ReceiptLineItemEntity
import com.docket.domain.model.MonthlySpend
import com.docket.domain.model.ParsedReceipt
import com.docket.domain.model.Receipt
import com.docket.domain.model.ReceiptFilter
import com.docket.domain.model.ReceiptLineItem
import com.docket.domain.repository.ReceiptRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ReceiptRepositoryImpl @Inject constructor(
    private val receiptDao: ReceiptDao
) : ReceiptRepository {

    override fun observeReceiptForDocument(documentId: Long): Flow<Receipt?> =
        receiptDao.observeWithItems(documentId).map { it?.toDomain() }

    override suspend fun getReceiptForDocument(documentId: Long): Receipt? =
        receiptDao.getWithItems(documentId)?.toDomain()

    override suspend fun saveReceipt(documentId: Long, parsed: ParsedReceipt): Long {
        val entity = ReceiptEntity(
            documentId = documentId,
            merchant = parsed.merchant?.takeIf { it.isNotBlank() } ?: "Unknown merchant",
            totalAmountCents = parsed.totalAmountCents,
            currencyCode = parsed.currencyCode,
            purchaseDate = parsed.purchaseDate,
            paymentMethod = parsed.paymentMethod,
            createdAt = System.currentTimeMillis()
        )
        val lineItems = parsed.lineItems.mapIndexed { index, item ->
            ReceiptLineItemEntity(
                receiptId = 0, // replaceReceipt overwrites this once the new receipt id exists
                description = item.description,
                amountCents = item.amountCents,
                orderIndex = index
            )
        }
        return receiptDao.replaceReceipt(documentId, entity, lineItems)
    }

    override fun observeAllReceipts(filter: ReceiptFilter): Flow<List<Receipt>> =
        receiptDao.observeFiltered(
            merchantQuery = filter.merchantQuery?.takeIf { it.isNotBlank() },
            startDate = filter.startDate,
            endDate = filter.endDate,
            minAmountCents = filter.minAmountCents,
            maxAmountCents = filter.maxAmountCents
        ).map { entities -> entities.map { it.toDomain(lineItems = emptyList()) } }

    override fun observeMonthlySpend(): Flow<List<MonthlySpend>> =
        receiptDao.observeMonthlySpend().map { rows -> rows.map { it.toDomain() } }

    override fun observeMerchants(): Flow<List<String>> = receiptDao.observeMerchants()
}

private fun ReceiptWithItems.toDomain(): Receipt =
    receipt.toDomain(lineItems = lineItems.sortedBy { it.orderIndex }.map { it.toDomain() })

private fun ReceiptEntity.toDomain(lineItems: List<ReceiptLineItem>): Receipt = Receipt(
    id = id,
    documentId = documentId,
    merchant = merchant,
    totalAmountCents = totalAmountCents,
    currencyCode = currencyCode,
    purchaseDate = purchaseDate,
    paymentMethod = paymentMethod,
    lineItems = lineItems,
    createdAt = createdAt
)

private fun ReceiptLineItemEntity.toDomain(): ReceiptLineItem = ReceiptLineItem(
    id = id,
    description = description,
    amountCents = amountCents,
    orderIndex = orderIndex
)

private fun MonthlySpendRow.toDomain(): MonthlySpend = MonthlySpend(
    yearMonth = yearMonth,
    totalCents = totalCents,
    currencyCode = currencyCode
)
