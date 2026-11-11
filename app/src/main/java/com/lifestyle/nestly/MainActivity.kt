package com.lifestyle.nestly

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lifestyle.nestly.data.reminder.ReminderNotifications
import com.lifestyle.nestly.data.supabase.refreshSessionIfExpiring
import com.lifestyle.nestly.ui.NestlyApp
import com.lifestyle.nestly.ui.entry.EntryKind
import com.lifestyle.nestly.ui.theme.NestlyTheme
import com.lifestyle.nestly.widget.WidgetActions
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var pendingEntryKind by mutableStateOf<EntryKind?>(null)
    private var pendingAppointmentId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingEntryKind = WidgetActions.entryKindFrom(intent)
        pendingAppointmentId = intent.getStringExtra(ReminderNotifications.EXTRA_OPEN_APPOINTMENT_ID)
        enableEdgeToEdge()
        setContent {
            NestlyTheme {
                NestlyApp(
                    pendingEntryKind = pendingEntryKind,
                    onPendingEntryConsumed = { pendingEntryKind = null },
                    pendingAppointmentId = pendingAppointmentId,
                    onPendingAppointmentConsumed = { pendingAppointmentId = null },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Strict online-only: always refetch fresh state when the app returns
        // to the foreground; Realtime events missed while backgrounded are
        // not replayed. The token may have expired while the process slept,
        // so renew it before the refetch goes out.
        val container = appContainer
        container.applicationScope.launch {
            container.supabase.refreshSessionIfExpiring()
            container.realtime.refreshAll()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingEntryKind = WidgetActions.entryKindFrom(intent)
        intent.getStringExtra(ReminderNotifications.EXTRA_OPEN_APPOINTMENT_ID)?.let { pendingAppointmentId = it }
    }
}
