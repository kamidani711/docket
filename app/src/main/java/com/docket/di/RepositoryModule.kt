package com.docket.di

import com.docket.data.backup.BackupManagerImpl
import com.docket.data.billing.BillingRepositoryImpl
import com.docket.data.csv.CsvReceiptExporter
import com.docket.data.imaging.BitmapImageExporter
import com.docket.data.imaging.BitmapTransformer
import com.docket.data.imaging.PreviewBitmapCache
import com.docket.data.mlkit.DocumentTextRecognizer
import com.docket.data.mlkit.MultiScriptTextRecognizerImpl
import com.docket.data.pdf.PdfExporter
import com.docket.data.receipt.RuleBasedReceiptParser
import com.docket.data.repository.AnalyticsRepositoryImpl
import com.docket.data.repository.DocumentRepositoryImpl
import com.docket.data.repository.OcrRepositoryImpl
import com.docket.data.repository.PremiumRepositoryImpl
import com.docket.data.repository.ReceiptRepositoryImpl
import com.docket.data.repository.ScanSessionRepositoryImpl
import com.docket.data.repository.SearchRepositoryImpl
import com.docket.data.repository.SettingsRepositoryImpl
import com.docket.data.repository.StorageRepositoryImpl
import com.docket.data.repository.WarrantyRepositoryImpl
import com.docket.data.work.OcrScheduler
import com.docket.data.work.WarrantyReminderScheduler
import com.docket.domain.repository.AnalyticsRepository
import com.docket.domain.repository.BackgroundOcrScheduler
import com.docket.domain.repository.BackupManager
import com.docket.domain.repository.BillingRepository
import com.docket.domain.repository.CsvExporter
import com.docket.domain.repository.DocumentRepository
import com.docket.domain.repository.ImageExporter
import com.docket.domain.repository.ImageProcessor
import com.docket.domain.repository.MultiScriptTextRecognizer
import com.docket.domain.repository.OcrRepository
import com.docket.domain.repository.PdfWriter
import com.docket.domain.repository.PremiumRepository
import com.docket.domain.repository.PreviewCache
import com.docket.domain.repository.ReceiptParser
import com.docket.domain.repository.ReceiptRepository
import com.docket.domain.repository.ReminderScheduler
import com.docket.domain.repository.ScanSessionRepository
import com.docket.domain.repository.SearchRepository
import com.docket.domain.repository.SettingsRepository
import com.docket.domain.repository.StorageRepository
import com.docket.domain.repository.TextRecognizer
import com.docket.domain.repository.WarrantyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds every domain-owned interface to its one `data` implementation. Singleton-ness comes
 * from the `@Singleton` annotation on each impl class (see those files) — a `@Binds` alias to
 * an already-scoped constructor binding is itself effectively scoped, so it isn't repeated here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindScanSessionRepository(impl: ScanSessionRepositoryImpl): ScanSessionRepository

    @Binds
    abstract fun bindDocumentRepository(impl: DocumentRepositoryImpl): DocumentRepository

    @Binds
    abstract fun bindImageProcessor(impl: BitmapTransformer): ImageProcessor

    @Binds
    abstract fun bindTextRecognizer(impl: DocumentTextRecognizer): TextRecognizer

    @Binds
    abstract fun bindPdfWriter(impl: PdfExporter): PdfWriter

    @Binds
    abstract fun bindPreviewCache(impl: PreviewBitmapCache): PreviewCache

    @Binds
    abstract fun bindMultiScriptTextRecognizer(impl: MultiScriptTextRecognizerImpl): MultiScriptTextRecognizer

    @Binds
    abstract fun bindOcrRepository(impl: OcrRepositoryImpl): OcrRepository

    @Binds
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds
    abstract fun bindPremiumRepository(impl: PremiumRepositoryImpl): PremiumRepository

    @Binds
    abstract fun bindImageExporter(impl: BitmapImageExporter): ImageExporter

    @Binds
    abstract fun bindBackgroundOcrScheduler(impl: OcrScheduler): BackgroundOcrScheduler

    @Binds
    abstract fun bindReceiptParser(impl: RuleBasedReceiptParser): ReceiptParser

    @Binds
    abstract fun bindReceiptRepository(impl: ReceiptRepositoryImpl): ReceiptRepository

    @Binds
    abstract fun bindWarrantyRepository(impl: WarrantyRepositoryImpl): WarrantyRepository

    @Binds
    abstract fun bindReminderScheduler(impl: WarrantyReminderScheduler): ReminderScheduler

    @Binds
    abstract fun bindCsvExporter(impl: CsvReceiptExporter): CsvExporter

    @Binds
    abstract fun bindBackupManager(impl: BackupManagerImpl): BackupManager

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository

    @Binds
    abstract fun bindBillingRepository(impl: BillingRepositoryImpl): BillingRepository

    @Binds
    abstract fun bindAnalyticsRepository(impl: AnalyticsRepositoryImpl): AnalyticsRepository
}
