package com.lifestyle.nestly.ui.entry

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.timer.TimerKey
import com.lifestyle.nestly.data.timer.TimerState
import com.lifestyle.nestly.ui.components.AddPhotoButton
import com.lifestyle.nestly.ui.components.DateTimePickerDialog
import com.lifestyle.nestly.ui.components.EntrySheet
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.FormRow
import com.lifestyle.nestly.ui.components.NotesField
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import com.lifestyle.nestly.ui.theme.headerColor
import kotlinx.coroutines.delay

/** What every form receives: the controller plus live timers and clock. */
class FormScope(
    val controller: EntryController,
    val timers: Map<TimerKey, TimerState>,
    val now: Long,
) {
    val draft: EntryDraft get() = controller.draft
    val effective: EntryDraft by lazy { controller.effectiveDraft(timers, now) }
    fun update(transform: (EntryDraft) -> EntryDraft) = controller.update(transform)
    fun timer(key: TimerKey): TimerState = timers[key] ?: TimerState()
}

@Composable
fun EntrySheetHost(request: EntryRequest, onDismiss: () -> Unit) {
    val container = LocalContext.current.appContainer
    val scope = rememberCoroutineScope()
    val controller = remember(request) { EntryController(container, request, scope) }
    val timers by container.timers.timers.collectAsState(initial = emptyMap())
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }

    val kind = controller.kind
    val form = FormScope(controller, timers, now)
    EntrySheet(
        title = kind.title,
        headerColor = kind.card.headerColor,
        onDismiss = onDismiss,
        onSave = { controller.save(timers, onDone = onDismiss) },
        saveEnabled = controller.loaded && !controller.saving && EntryBuilder.canSave(kind, form.effective),
        onDelete = if (controller.isEditing) ({ controller.delete(onDone = onDismiss) }) else null,
    ) {
        controller.saveError?.let { message ->
            Text(
                message,
                style = NestlyType.Small,
                color = NestlyColors.Danger,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
        if (!controller.loaded) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = NestlyColors.AccentText) }
            return@EntrySheet
        }
        when (kind) {
            EntryKind.BREASTFEED -> BreastfeedForm(form)
            EntryKind.BOTTLE -> BottleForm(form)
            EntryKind.SOLIDS -> SolidsForm(form)
            EntryKind.COMBO -> ComboForm(form)
            EntryKind.PUMP -> PumpForm(form)
            EntryKind.DIAPER -> DiaperForm(form)
            EntryKind.SLEEP -> SleepForm(form)
            EntryKind.ROUTINE -> RoutineForm(form)
            EntryKind.BABY_FIRST -> BabyFirstForm(form)
            EntryKind.GROWTH -> GrowthForm(form)
            EntryKind.MILESTONE -> MilestoneForm(form)
            EntryKind.MEDICAL -> MedicalForm(form)
            EntryKind.VACCINE -> VaccineForm(form)
        }
        NotesAndPhoto(form)
    }
}

@Composable
fun ColumnScope.NotesAndPhoto(form: FormScope) {
    NotesField(form.draft.note, onValueChange = { text -> form.update { it.copy(note = text) } })
    AddPhotoButton(form.draft.photoPath, onChange = { path -> form.update { it.copy(photoPath = path) } })
}

/**
 * "Start Time  Today 8:07pm". Shows "Today  Add" until a time is picked or a timer sets it.
 * [endTime] switches the row to edit the end time instead.
 */
@Composable
fun TimeRow(
    form: FormScope,
    label: String,
    dateOnly: Boolean = false,
    endTime: Boolean = false,
) {
    var picking by remember { mutableStateOf(false) }
    val effective = form.effective
    val value: Long? = if (endTime) effective.endTime else effective.startTime.takeIf {
        effective.startTimeSet || it != form.draft.startTime
    }
    val shown = when {
        value == null -> "Today" to "Add"
        dateOnly -> Format.relativeDay(value, form.now) to null
        else -> Format.dayAndTime(value, form.now) to null
    }
    FormRow(label, value = shown.first, action = shown.second, onClick = { picking = true })
    if (picking) {
        DateTimePickerDialog(
            initial = value ?: form.now,
            dateOnly = dateOnly,
            onDismiss = { picking = false },
            onConfirm = { picked ->
                picking = false
                form.update {
                    if (endTime) it.copy(endTime = picked)
                    else it.copy(
                        startTime = picked,
                        startTimeSet = true,
                        endTime = it.endTime?.takeIf { end -> end >= picked },
                    )
                }
            },
        )
    }
}

/** Free-text row shaped like a form row, e.g. "Baby First". */
@Composable
fun InlineTextRow(placeholder: String, value: String, onValueChange: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 82.dp)
            .padding(horizontal = 20.dp, vertical = 26.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = NestlyType.RowLabel.copy(color = NestlyColors.TextPrimary),
            cursorBrush = SolidColor(NestlyColors.AccentText),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, style = NestlyType.RowLabel, color = NestlyColors.TextPrimary)
                inner()
            },
        )
    }
    RowDivider()
}
