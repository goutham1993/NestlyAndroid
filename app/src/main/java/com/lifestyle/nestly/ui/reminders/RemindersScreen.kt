package com.lifestyle.nestly.ui.reminders

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.model.ReminderKind
import com.lifestyle.nestly.data.reminder.ReminderLogic
import com.lifestyle.nestly.ui.components.DetailScreen
import com.lifestyle.nestly.ui.components.DurationInputDialog
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.NestlySwitch
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType

@Composable
fun RemindersScreen(onBack: () -> Unit) {
    val container = LocalContext.current.appContainer
    val viewModel: RemindersViewModel = viewModel { RemindersViewModel(container) }
    val rows by viewModel.rows.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editing by remember { mutableStateOf<ReminderKind?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* switch already flipped; notifications may be blocked until granted */ }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    DetailScreen("Reminders", onBack) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(
                "Get an alert after a set time since the last activity of each type.",
                style = NestlyType.Small,
                color = NestlyColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            )
            RowDivider()
            ReminderKind.entries.forEach { kind ->
                val row = rows[kind]
                val enabled = row?.enabled == true
                val minutes = row?.intervalMinutes ?: ReminderLogic.defaultIntervalMinutes(kind)
                ReminderRow(
                    kind = kind,
                    enabled = enabled,
                    intervalMinutes = minutes,
                    onToggle = { on ->
                        if (on) ensureNotificationPermission()
                        viewModel.setEnabled(kind, on)
                    },
                    onEditInterval = { editing = kind },
                )
            }
        }
    }

    editing?.let { kind ->
        val minutes = rows[kind]?.intervalMinutes ?: ReminderLogic.defaultIntervalMinutes(kind)
        DurationInputDialog(
            title = "${ReminderLogic.label(kind)} interval",
            initialSec = minutes * 60L,
            showHours = true,
            onDismiss = { editing = null },
            onConfirm = { sec ->
                editing = null
                viewModel.setInterval(kind, (sec / 60).toInt().coerceAtLeast(1))
            },
        )
    }
}

@Composable
private fun ReminderRow(
    kind: ReminderKind,
    enabled: Boolean,
    intervalMinutes: Int,
    onToggle: (Boolean) -> Unit,
    onEditInterval: () -> Unit,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 82.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onEditInterval),
            ) {
                Text(ReminderLogic.label(kind), style = NestlyType.RowLabel)
                Text(
                    "Every ${Format.duration(intervalMinutes * 60L)} after last ${ReminderLogic.label(kind).lowercase()}",
                    style = NestlyType.Small,
                    color = NestlyColors.TextSecondary,
                )
            }
            NestlySwitch(enabled, onToggle)
        }
        RowDivider()
    }
}
