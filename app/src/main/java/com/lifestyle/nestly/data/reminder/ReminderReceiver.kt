package com.lifestyle.nestly.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.model.ReminderKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                        ReminderNotifications.ensureChannel(context)
                        context.appContainer.scheduler.rescheduleAll()
                    }
                    ReminderScheduler.ACTION_APPOINTMENT_FIRE -> {
                        val id = intent.getStringExtra(ReminderScheduler.EXTRA_APPOINTMENT_ID) ?: return@launch
                        val appointment = context.appContainer.appointments.getById(id) ?: return@launch
                        if (appointment.reminderLead ==
                            com.lifestyle.nestly.data.model.AppointmentReminderLead.NONE
                        ) return@launch
                        // At-time alarms fire a hair after startsAt; allow a short grace window.
                        if (appointment.startsAt < System.currentTimeMillis() - 5 * 60_000L) return@launch
                        ReminderNotifications.showAppointment(context, appointment)
                    }
                    ReminderScheduler.ACTION_FIRE -> {
                        val id = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_ID) ?: return@launch
                        val kindName = intent.getStringExtra(ReminderScheduler.EXTRA_KIND) ?: return@launch
                        val kind = ReminderKind.entries.find { it.name == kindName } ?: return@launch
                        val reminder = context.appContainer.reminders.getById(id)
                        if (reminder == null || !reminder.enabled) return@launch
                        ReminderNotifications.show(context, id, kind)
                        // Wait for the next matching activity (or reminder edit) to schedule again.
                        // Catch-up fires once; rescheduling from the same last activity would spam every minute.
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
