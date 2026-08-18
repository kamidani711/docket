package com.docket.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.docket.data.local.dao.AnalyticsDao
import com.docket.data.local.dao.DocumentDao
import com.docket.data.local.dao.FolderDao
import com.docket.data.local.dao.OcrDao
import com.docket.data.local.dao.ReceiptDao
import com.docket.data.local.dao.ScanPageDao
import com.docket.data.local.dao.ScanSessionDao
import com.docket.data.local.dao.WarrantyDao
import com.docket.data.local.entity.AnalyticsEventEntity
import com.docket.data.local.entity.DocumentEntity
import com.docket.data.local.entity.DocumentPageEntity
import com.docket.data.local.entity.FolderEntity
import com.docket.data.local.entity.PageOcrEntity
import com.docket.data.local.entity.PageOcrFtsEntity
import com.docket.data.local.entity.ReceiptEntity
import com.docket.data.local.entity.ReceiptLineItemEntity
import com.docket.data.local.entity.ScanPageEntity
import com.docket.data.local.entity.ScanSessionEntity
import com.docket.data.local.entity.WarrantyEntity

// v3 -> v4: Receipt/Warranty got real fields, added ReceiptLineItem for parsed line items.
// v4 -> v5: added the local-only analytics_events table.
// fallbackToDestructiveMigration is fine pre-release (no installed users yet) — swap it for a
// real Migration before shipping anything that ships with data worth keeping.
@Database(
    entities = [
        DocumentEntity::class,
        DocumentPageEntity::class,
        FolderEntity::class,
        ScanSessionEntity::class,
        ScanPageEntity::class,
        PageOcrEntity::class,
        PageOcrFtsEntity::class,
        ReceiptEntity::class,
        ReceiptLineItemEntity::class,
        WarrantyEntity::class,
        AnalyticsEventEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class DocketDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun folderDao(): FolderDao
    abstract fun scanSessionDao(): ScanSessionDao
    abstract fun scanPageDao(): ScanPageDao
    abstract fun ocrDao(): OcrDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun warrantyDao(): WarrantyDao
    abstract fun analyticsDao(): AnalyticsDao
}
