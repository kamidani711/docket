package com.docket.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

object NotificationChannels {
    const val WARRANTY_REMINDERS = "warranty_reminders"

    /** Call once, early — DocketApplication.onCreate(). Channel creation is idempotent
     *  (re-creating with the same id is a no-op), so calling this more than once is harmless. */
    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            WARRANTY_REMINDERS,
            "Warranty reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Alerts before a tracked warranty expires"
        }
        context.getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }
}
