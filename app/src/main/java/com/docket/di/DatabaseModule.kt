package com.docket.di

import android.content.Context
import androidx.room.Room
import com.docket.data.local.DocketDatabase
import com.docket.data.local.dao.AnalyticsDao
import com.docket.data.local.dao.DocumentDao
import com.docket.data.local.dao.FolderDao
import com.docket.data.local.dao.OcrDao
import com.docket.data.local.dao.ReceiptDao
import com.docket.data.local.dao.ScanPageDao
import com.docket.data.local.dao.ScanSessionDao
import com.docket.data.local.dao.WarrantyDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDocketDatabase(@ApplicationContext context: Context): DocketDatabase =
        Room.databaseBuilder(context, DocketDatabase::class.java, "docket.db")
            // Pre-release, no real data to preserve yet — see DocketDatabase.kt.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDocumentDao(database: DocketDatabase): DocumentDao = database.documentDao()

    @Provides
    fun provideFolderDao(database: DocketDatabase): FolderDao = database.folderDao()

    @Provides
    fun provideScanSessionDao(database: DocketDatabase): ScanSessionDao = database.scanSessionDao()

    @Provides
    fun provideScanPageDao(database: DocketDatabase): ScanPageDao = database.scanPageDao()

    @Provides
    fun provideOcrDao(database: DocketDatabase): OcrDao = database.ocrDao()

    @Provides
    fun provideReceiptDao(database: DocketDatabase): ReceiptDao = database.receiptDao()

    @Provides
    fun provideWarrantyDao(database: DocketDatabase): WarrantyDao = database.warrantyDao()

    @Provides
    fun provideAnalyticsDao(database: DocketDatabase): AnalyticsDao = database.analyticsDao()
}
