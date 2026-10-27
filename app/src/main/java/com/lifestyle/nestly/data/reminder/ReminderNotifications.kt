package com.lifestyle.nestly.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lifestyle.nestly.MainActivity
import com.lifestyle.nestly.R
import com.lifestyle.nestly.data.model.ReminderKind

object ReminderNotifications {
    const val CHANNEL_ID = "reminders"
    private const val CHANNEL_NAME = "Reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Alerts after the interval since the last activity"
        }
        manager.createNotificationChannel(channel)
    }

    fun show(context: Context, reminderId: String, kind: ReminderKind) {
        ensureChannel(context)
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_KIND, kind.name)
        }
        val pending = PendingIntent.getActivity(
            context,
            reminderId.hashCode(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = "${ReminderLogic.label(kind)} reminder"
        val body = "Time to log the next ${ReminderLogic.label(kind).lowercase()}."
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(reminderId.hashCode(), notification)
    }

    const val EXTRA_OPEN_KIND = "open_reminder_kind"
}
