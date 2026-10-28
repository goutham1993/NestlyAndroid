package com.lifestyle.nestly.ui.appointments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.model.AppointmentReminderLead
import com.lifestyle.nestly.data.model.AppointmentWithQuestions
import com.lifestyle.nestly.ui.components.DateTimePickerDialog
import com.lifestyle.nestly.ui.components.DetailScreen
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.FormRow
import com.lifestyle.nestly.ui.components.OptionPickerDialog
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.components.SectionHeading
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import java.time.LocalDate
import java.time.ZoneId

/** Common visit reasons offered as one-tap title hints, ahead of the user's own past titles. */
private val TITLE_HINTS = listOf(
    "Well-child checkup",
    "Vaccination visit",
    "Sick visit",
    "Follow-up visit",
    "Developmental check",
    "Weight check",
)

@Composable
fun AppointmentsScreen(
    onBack: () -> Unit,
    onOpenAppointment: (String) -> Unit,
    onAddAppointment: () -> Unit,
) {
    val container = LocalContext.current.appContainer
    val viewModel: AppointmentsViewModel = viewModel { AppointmentsViewModel(container) }
    val all by viewModel.appointments.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()
    val upcoming = all.filter { it.appointment.startsAt >= now }
    val past = all.filter { it.appointment.startsAt < now }.reversed()

    DetailScreen("Appointments", onBack, action = "Add", onAction = onAddAppointment) {
        LazyColumn(Modifier.weight(1f)) {
            if (all.isEmpty()) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "No appointments yet",
                            style = NestlyType.SectionTitle,
                            color = NestlyColors.TextPrimary,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Add a doctor appointment to keep its date, your questions and notes in one place.",
                            style = NestlyType.Body,
                            color = NestlyColors.TextSecondary,
                        )
                        Spacer(Modifier.height(24.dp))
                        TextButton(onClick = onAddAppointment) {
                            Text("Add appointment", color = NestlyColors.AccentText, style = NestlyType.Button)
                        }
                    }
                }
            }
            if (upcoming.isNotEmpty()) {
                item { SectionHeading("Upcoming") }
                items(upcoming, key = { it.appointment.id }) { item ->
                    AppointmentRow(item, isPast = false, now = now) { onOpenAppointment(item.appointment.id) }
                }
            }
            if (past.isNotEmpty()) {
                item { SectionHeading("Past") }
                items(past, key = { it.appointment.id }) { item ->
                    AppointmentRow(item, isPast = true, now = now) { onOpenAppointment(item.appointment.id) }
                }
            }
        }
    }
}

@Composable
private fun AppointmentRow(item: AppointmentWithQuestions, isPast: Boolean, now: Long, onClick: () -> Unit) {
    val appt = item.appointment
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 82.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(appt.title, style = NestlyType.RowLabel, color = NestlyColors.TextPrimary)
                val details = listOfNotNull(
                    Format.dayAndTime(appt.startsAt, now),
                    appt.doctorName?.takeIf { it.isNotBlank() },
                    appt.location?.takeIf { it.isNotBlank() },
                ).joinToString(" · ")
                Text(details, style = NestlyType.Small, color = NestlyColors.TextSecondary)
                val checkboxItems = AppointmentNotesFormat.checkboxItems(appt.notes.orEmpty())
                val questions = when {
                    checkboxItems.isEmpty() -> null
                    isPast -> "${checkboxItems.count { it.checked }}/${checkboxItems.size} questions asked"
                    checkboxItems.all { !it.checked } ->
                        if (checkboxItems.size == 1) "1 question to ask" else "${checkboxItems.size} questions to ask"
                    else -> "${checkboxItems.count { it.checked }}/${checkboxItems.size} questions asked"
                }
                val reminder = appt.reminderLead
                    .takeIf { !isPast && it != AppointmentReminderLead.NONE }
                    ?.let { "Reminder: ${it.label.lowercase()}" }
                listOfNotNull(questions, reminder).joinToString(" · ").takeIf { it.isNotEmpty() }?.let {
                    Text(it, style = NestlyType.Small, color = NestlyColors.TextSecondary)
                }
            }
        }
        RowDivider()
    }
}

/**
 * Text field with tap-to-fill suggestions drawn from [suggestions] (most
 * relevant first). Suggestions appear while the field is focused; typing
 * filters them. Used for title hints and for doctor/location values reused
 * from previous appointments.
 */
@Composable
private fun SuggestingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    suggestions: List<String>,
    placeholder: String? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val matches = remember(value, suggestions, focused) {
        if (!focused) emptyList()
        else suggestions
            .filter { it.isNotBlank() && !it.equals(value.trim(), ignoreCase = true) }
            .filter { value.isBlank() || it.contains(value.trim(), ignoreCase = true) }
            .distinctBy { it.lowercase() }
            .take(5)
    }
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it, color = NestlyColors.TextMuted) } },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NestlyColors.AccentText,
                unfocusedBorderColor = NestlyColors.Divider,
                cursorColor = NestlyColors.AccentText,
                focusedLabelColor = NestlyColors.AccentText,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
        )
        matches.forEach { suggestion ->
            Text(
                suggestion,
                style = NestlyType.Body,
                color = NestlyColors.AccentText,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onValueChange(suggestion) }
                    .padding(vertical = 10.dp),
            )
        }
    }
    RowDivider()
}

@Composable
fun AppointmentDetailScreen(appointmentId: String?, onBack: () -> Unit) {
    val container = LocalContext.current.appContainer
    val viewModel: AppointmentsViewModel = viewModel { AppointmentsViewModel(container) }
    val context = LocalContext.current
    val all by viewModel.appointments.collectAsStateWithLifecycle()

    // Most recent first (list state is soonest-first), so reused values surface early.
    val previousTitles = remember(all) { all.reversed().map { it.appointment.title } }
    val previousDoctors = remember(all) {
        all.reversed().mapNotNull { it.appointment.doctorName?.takeIf { name -> name.isNotBlank() } }
    }
    val previousLocations = remember(all) {
        all.reversed().mapNotNull { it.appointment.location?.takeIf { loc -> loc.isNotBlank() } }
    }

    val existingFlow = remember(appointmentId) {
        appointmentId?.let { viewModel.appointment(it) }
    }
    val existing by existingFlow?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }

    var title by remember { mutableStateOf("") }
    var doctor by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var startsAt by remember {
        mutableStateOf(
            LocalDate.now().plusDays(1).atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
    }
    var lead by remember { mutableStateOf(AppointmentReminderLead.ONE_DAY) }
    var notes by remember { mutableStateOf(TextFieldValue("")) }
    var initialized by remember { mutableStateOf(appointmentId == null) }
    var error by remember { mutableStateOf<String?>(null) }

    var pickingTime by remember { mutableStateOf(false) }
    var pickingLead by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    LaunchedEffect(existing?.appointment?.id) {
        val appt = existing?.appointment ?: return@LaunchedEffect
        if (!initialized) {
            title = appt.title
            doctor = appt.doctorName.orEmpty()
            location = appt.location.orEmpty()
            startsAt = appt.startsAt
            lead = appt.reminderLead
            notes = TextFieldValue(appt.notes.orEmpty())
            initialized = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* reminder is saved either way; notifications may be blocked until granted */ }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun applyFormat(format: NoteLineFormat) {
        val result = AppointmentNotesFormat.formatCurrentLine(notes.text, notes.selection.start, format)
        notes = TextFieldValue(result.text, TextRange(result.cursor))
    }

    fun save() {
        if (title.isBlank()) {
            error = "Give the appointment a title — pick a hint or type your own."
            return
        }
        error = null
        viewModel.save(
            id = appointmentId,
            title = title,
            doctorName = doctor.ifBlank { null },
            location = location.ifBlank { null },
            startsAt = startsAt,
            reminderLead = lead,
            notes = notes.text.ifBlank { null },
            onDone = onBack,
            onError = { error = it },
        )
    }

    DetailScreen(
        title = if (appointmentId == null) "New Appointment" else "Appointment",
        onBack = onBack,
        action = "Save",
        onAction = ::save,
    ) {
        if (appointmentId != null && existing == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NestlyColors.AccentText)
            }
            return@DetailScreen
        }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            error?.let { message ->
                Text(
                    message,
                    style = NestlyType.Small,
                    color = NestlyColors.Danger,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
            SuggestingTextField(
                label = "Title",
                value = title,
                onValueChange = { title = it },
                suggestions = previousTitles + TITLE_HINTS,
                placeholder = "e.g. 4-month checkup",
            )
            SuggestingTextField(
                label = "Doctor",
                value = doctor,
                onValueChange = { doctor = it },
                suggestions = previousDoctors,
                placeholder = "e.g. Dr. Patel",
            )
            SuggestingTextField(
                label = "Location",
                value = location,
                onValueChange = { location = it },
                suggestions = previousLocations,
                placeholder = "e.g. Sunny Pediatrics",
            )
            FormRow(
                "Date & Time",
                value = Format.longDateTime(startsAt),
                onClick = { pickingTime = true },
            )
            FormRow(
                "Reminder",
                value = lead.label,
                chevron = true,
                onClick = { pickingLead = true },
            )

            SectionHeading("Notes & questions")
            Text(
                "Write freely — questions to ask, what the doctor said, follow-ups. " +
                    "Use the buttons below to start a bullet, numbered or checkbox line.",
                style = NestlyType.Small,
                color = NestlyColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Row(Modifier.padding(horizontal = 12.dp)) {
                TextButton(onClick = { applyFormat(NoteLineFormat.BULLET) }) {
                    Text("• Bullet", color = NestlyColors.AccentText, style = NestlyType.Button)
                }
                TextButton(onClick = { applyFormat(NoteLineFormat.NUMBERED) }) {
                    Text("1. Numbered", color = NestlyColors.AccentText, style = NestlyType.Button)
                }
                TextButton(onClick = { applyFormat(NoteLineFormat.CHECKBOX) }) {
                    Text("☐ Checkbox", color = NestlyColors.AccentText, style = NestlyType.Button)
                }
            }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = {
                        Text(
                            "☐ Ask about feeding\n• Bring vaccine record\n1. First question",
                            color = NestlyColors.TextMuted,
                        )
                    },
                    minLines = 6,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NestlyColors.AccentText,
                        unfocusedBorderColor = NestlyColors.Divider,
                        cursorColor = NestlyColors.AccentText,
                        focusedLabelColor = NestlyColors.AccentText,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val checkboxItems = AppointmentNotesFormat.checkboxItems(notes.text)
            if (checkboxItems.isNotEmpty()) {
                Text(
                    "Tap to check off (${checkboxItems.count { it.checked }}/${checkboxItems.size} done)",
                    style = NestlyType.Small,
                    color = NestlyColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                checkboxItems.forEach { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(start = 8.dp, end = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = item.checked,
                            onCheckedChange = {
                                notes = TextFieldValue(
                                    AppointmentNotesFormat.toggleCheckbox(notes.text, item.lineIndex),
                                    notes.selection,
                                )
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = NestlyColors.Accent,
                                uncheckedColor = NestlyColors.TextMuted,
                                checkmarkColor = NestlyColors.Surface,
                            ),
                        )
                        Text(
                            item.text,
                            style = NestlyType.Body,
                            color = if (item.checked) NestlyColors.TextSecondary else NestlyColors.TextPrimary,
                            textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            RowDivider()

            if (appointmentId != null) {
                Spacer(Modifier.height(24.dp))
                TextButton(
                    onClick = { confirmingDelete = true },
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Text("Delete appointment", color = NestlyColors.Danger, style = NestlyType.Button)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (pickingTime) {
        DateTimePickerDialog(
            initial = startsAt,
            dateOnly = false,
            onDismiss = { pickingTime = false },
            onConfirm = {
                startsAt = it
                pickingTime = false
            },
        )
    }
    if (pickingLead) {
        OptionPickerDialog(
            title = "Reminder",
            headerColor = NestlyColors.Accent,
            options = AppointmentReminderLead.entries.map { it.label },
            initial = listOf(lead.label),
            multiSelect = false,
            onDismiss = { pickingLead = false },
            onConfirm = { picked ->
                pickingLead = false
                lead = AppointmentReminderLead.entries.find { it.label == picked.firstOrNull() }
                    ?: AppointmentReminderLead.NONE
                if (lead != AppointmentReminderLead.NONE) ensureNotificationPermission()
            },
        )
    }
    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            containerColor = NestlyColors.Surface,
            title = { Text("Delete appointment?", style = NestlyType.RowLabel) },
            text = {
                Text(
                    "This removes the appointment, its notes and its reminder.",
                    style = NestlyType.Body,
                    color = NestlyColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    existing?.appointment?.let { appt ->
                        viewModel.delete(appt, onDone = onBack, onError = { error = it })
                    }
                }) {
                    Text("Delete", color = NestlyColors.Danger, style = NestlyType.Button)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text("Cancel", color = NestlyColors.AccentText, style = NestlyType.Button)
                }
            },
        )
    }
}
