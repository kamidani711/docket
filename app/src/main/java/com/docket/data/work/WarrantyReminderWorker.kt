package com.docket.data.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.docket.MainActivity
import com.docket.R
import com.docket.data.notification.NotificationChannels
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Fires once per scheduled reminder (30 days before / 7 days before / day of expiry — see
 *  [WarrantyReminderScheduler]) and posts a local notification that deep-links back to the
 *  warranty's linked receipt document. Nothing here calls out to a server. */
@HiltWorker
class WarrantyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val warrantyId = inputData.getLong(KEY_WARRANTY_ID, -1L)
        val documentId = inputData.getLong(KEY_DOCUMENT_ID, -1L)
        val itemName = inputData.getString(KEY_ITEM_NAME) ?: return Result.failure()
        val reminderType = ReminderType.fromKey(inputData.getString(KEY_REMINDER_TYPE))
        if (warrantyId <= 0L || documentId <= 0L || reminderType == null) return Result.failure()

        postNotification(warrantyId, documentId, itemName, reminderType)
        return Result.success()
    }

    private fun postNotification(warrantyId: Long, documentId: Long, itemName: String, reminderType: ReminderType) {
        val context = applicationContext

        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return // nothing else to do — can't post, and shouldn't crash over it

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(MainActivity.EXTRA_DOCUMENT_ID, documentId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId(warrantyId, reminderType),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.WARRANTY_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminderType.title(itemName))
            .setContentText(reminderType.body(itemName))
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminderType.body(itemName)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId(warrantyId, reminderType), notification)
    }

    private fun notificationId(warrantyId: Long, reminderType: ReminderType): Int =
        (warrantyId.toString() + reminderType.key).hashCode()

    enum class ReminderType(val key: String, private val daysBefore: Int) {
        THIRTY_DAYS("30_DAYS", 30),
        SEVEN_DAYS("7_DAYS", 7),
        DAY_OF("DAY_OF", 0);

        fun title(itemName: String): String = when (this) {
            DAY_OF -> "$itemName warranty expires today"
            else -> "$itemName warranty expiring soon"
        }

        fun body(itemName: String): String = when (this) {
            DAY_OF -> "$itemName's warranty expires today."
            else -> "$itemName's warranty expires in $daysBefore days."
        }

        companion object {
            fun fromKey(key: String?): ReminderType? = entries.firstOrNull { it.key == key }
        }
    }

    companion object {
        const val KEY_WARRANTY_ID = "warrantyId"
        const val KEY_DOCUMENT_ID = "documentId"
        const val KEY_ITEM_NAME = "itemName"
        const val KEY_REMINDER_TYPE = "reminderType"

        fun inputData(warrantyId: Long, documentId: Long, itemName: String, reminderType: ReminderType): Data =
            workDataOf(
                KEY_WARRANTY_ID to warrantyId,
                KEY_DOCUMENT_ID to documentId,
                KEY_ITEM_NAME to itemName,
                KEY_REMINDER_TYPE to reminderType.key
            )
    }
}
