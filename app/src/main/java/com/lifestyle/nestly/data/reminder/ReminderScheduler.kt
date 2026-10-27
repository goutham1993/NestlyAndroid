package com.lifestyle.nestly.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lifestyle.nestly.AppContainer
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.db.entity.ReminderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReminderScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    suspend fun rescheduleAll(container: AppContainer = context.appContainer) = withContext(Dispatchers.IO) {
        container.reminders.getEnabled().forEach { schedule(it, container) }
    }

    suspend fun schedule(reminder: ReminderEntity, container: AppContainer = context.appContainer) =
        withContext(Dispatchers.IO) {
            if (!reminder.enabled) {
                cancel(reminder.id)
                return@withContext
            }
            val profileId = reminder.profileId.orEmpty()
            val last = container.activities.getLatestOf(
                reminder.familyId,
                profileId,
                ReminderLogic.typesFor(reminder.kind),
            )
            val fireAt = ReminderLogic.nextFireMs(last?.startTime, reminder.intervalMinutes) ?: run {
                cancel(reminder.id)
                return@withContext
            }
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = ACTION_FIRE
                putExtra(EXTRA_REMINDER_ID, reminder.id)
                putExtra(EXTRA_KIND, reminder.kind.name)
                putExtra(EXTRA_PROFILE_ID, reminder.profileId)
            }
            val pending = PendingIntent.getBroadcast(
                context,
                reminder.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pending)
            } else {
                alarms.set(AlarmManager.RTC_WAKEUP, fireAt, pending)
            }
        }

    fun cancel(reminderId: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply { action = ACTION_FIRE }
        val pending = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarms.cancel(pending)
    }

    companion object {
        const val ACTION_FIRE = "com.lifestyle.nestly.REMINDER_FIRE"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_KIND = "kind"
        const val EXTRA_PROFILE_ID = "profile_id"
    }
}
