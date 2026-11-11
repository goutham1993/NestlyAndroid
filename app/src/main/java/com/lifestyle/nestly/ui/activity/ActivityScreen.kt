package com.lifestyle.nestly.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Subject
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.model.Sex
import com.lifestyle.nestly.data.prefs.ActivityToggle
import com.lifestyle.nestly.ui.AppViewModel
import com.lifestyle.nestly.ui.Routes
import com.lifestyle.nestly.ui.components.BabyAvatar
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.FormRow
import com.lifestyle.nestly.ui.components.HeaderIconButton
import com.lifestyle.nestly.ui.components.PillButton
import com.lifestyle.nestly.ui.components.ProfileHeader
import com.lifestyle.nestly.ui.components.SegmentedToggle
import com.lifestyle.nestly.ui.entry.EntryKind
import com.lifestyle.nestly.ui.entry.EntryRequest
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun ActivityTab(
    appViewModel: AppViewModel,
    onOpenEntry: (EntryRequest) -> Unit,
    navigate: (String) -> Unit,
    onShowHistory: () -> Unit,
) {
    val container = LocalContext.current.appContainer
    val viewModel: ActivityViewModel = viewModel { ActivityViewModel(container) }
    val profiles by appViewModel.profiles.collectAsStateWithLifecycle()
    val data by viewModel.data.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val layout by viewModel.layout.collectAsStateWithLifecycle()
    val timers by viewModel.timers.collectAsStateWithLifecycle()
    val feedIntervalMinutes by viewModel.feedIntervalMinutes.collectAsStateWithLifecycle()

    val filters by appViewModel.filters.collectAsStateWithLifecycle()

    var showSummary by remember { mutableStateOf(false) }

    when {
        profiles == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NestlyColors.Accent)
        }
        profiles.isNullOrEmpty() -> WelcomeScreen(
            onImport = { navigate(Routes.IMPORT) },
            onAddChild = { navigate(Routes.childNew(askCaregiver = true)) },
        )
        else -> ActivityHome(
            profiles = profiles.orEmpty(),
            dashboard = data,
            now = now,
            layout = layout,
            timers = timers,
            feedIntervalMinutes = feedIntervalMinutes,
            filters = filters,
            startOfToday = viewModel.startOfDay(now),
            onOpenEntry = onOpenEntry,
            onShowHistory = onShowHistory,
            onMilestones = { navigate(Routes.MILESTONES) },
            onGrowth = { navigate(Routes.growth(it)) },
            onVaccinations = { navigate(Routes.VACCINATIONS) },
            onReminders = { navigate(Routes.REMINDERS) },
            onAppointments = { navigate(Routes.APPOINTMENTS) },
            onNotes = { navigate(Routes.NOTES) },
            onEditActivities = { navigate(Routes.EDIT_ACTIVITIES) },
            onSelectProfile = appViewModel::selectProfile,
            onShowSummary = { showSummary = true },
        )
    }

    if (showSummary) {
        data?.let { SummarySheet(it.recent, now, viewModel.startOfDay(now), onDismiss = { showSummary = false }) }
    }
}

@Composable
private fun WelcomeScreen(onImport: () -> Unit, onAddChild: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BabyAvatar()
        Spacer(Modifier.height(24.dp))
        Text("Welcome to Nestly", style = NestlyType.Display, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "Track feeds, pumping, diapers, sleep and more. Add your child to get started, or bring your history over from Nara Baby.",
            style = NestlyType.Body,
            color = NestlyColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        PillButton("Add Child", onClick = onAddChild, filled = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        PillButton("Import from Nara", onClick = onImport, modifier = Modifier.fillMaxWidth())
    }
}
