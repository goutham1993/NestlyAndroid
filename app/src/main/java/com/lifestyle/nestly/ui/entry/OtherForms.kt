package com.lifestyle.nestly.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lifestyle.nestly.data.catalog.MedicationCatalog
import com.lifestyle.nestly.data.catalog.RoutineCatalog
import com.lifestyle.nestly.data.catalog.VaccineCatalog
import com.lifestyle.nestly.data.model.StoolColor
import com.lifestyle.nestly.data.model.StoolTexture
import com.lifestyle.nestly.data.timer.TimerKey
import com.lifestyle.nestly.data.units.TempUnit
import com.lifestyle.nestly.data.units.Units
import com.lifestyle.nestly.ui.components.ActivityIcon
import com.lifestyle.nestly.ui.components.ChoiceRow
import com.lifestyle.nestly.ui.components.CircleToggle
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.FormRow
import com.lifestyle.nestly.ui.components.NumberInputDialog
import com.lifestyle.nestly.ui.units.currentUnitPrefs
import com.lifestyle.nestly.ui.components.OptionPickerDialog
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.components.SwitchRow
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import com.lifestyle.nestly.ui.theme.headerColor

@Composable
fun DiaperForm(form: FormScope) {
    val d = form.draft
    TimeRow(form, "Time")
    ChoiceRow {
        CircleToggle("Wet", d.wet, onClick = { form.update { it.copy(wet = !it.wet, dry = false) } })
        CircleToggle("Dirty", d.dirty, onClick = { form.update { it.copy(dirty = !it.dirty, dry = false) } })
        CircleToggle("Dry", d.dry, onClick = { form.update { it.copy(dry = !it.dry, wet = false, dirty = false) } })
    }
    RowDivider()
    if (d.dirty) {
        ChipSection("Color") {
            StoolColor.entries.forEach { color ->
                Chip(
                    label = color.label,
                    selected = d.stoolColor == color,
                    swatch = color.swatch,
                    onClick = { form.update { it.copy(stoolColor = if (it.stoolColor == color) null else color) } },
                )
            }
        }
        ChipSection("Texture") {
            StoolTexture.entries.forEach { texture ->
                Chip(
                    label = texture.label,
                    selected = d.stoolTexture == texture,
                    onClick = { form.update { it.copy(stoolTexture = if (it.stoolTexture == texture) null else texture) } },
                )
            }
        }
    }
    SwitchRow("Diaper Rash", d.rash, onCheckedChange = { checked -> form.update { it.copy(rash = checked) } })
}

private val StoolColor.label: String get() = name.lowercase().replaceFirstChar { it.uppercase() }

private val StoolTexture.label: String
    get() = when (this) {
        StoolTexture.RUN -> "Runny"
        StoolTexture.MUSH -> "Mushy"
        StoolTexture.MUCOUS -> "Mucousy"
        else -> name.lowercase().replaceFirstChar { it.uppercase() }
    }

private val StoolColor.swatch: Color
    get() = when (this) {
        StoolColor.YELLOW -> Color(0xFFE8C547)
        StoolColor.GREEN -> Color(0xFF7A9A3A)
        StoolColor.BROWN -> Color(0xFF8B5A2B)
        StoolColor.BLACK -> Color(0xFF1C1C1C)
        StoolColor.RED -> Color(0xFFC0392B)
        StoolColor.WHITE -> Color(0xFFF2F2F2)
        StoolColor.ORANGE -> Color(0xFFE67E22)
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
        Text(title, style = NestlyType.RowLabel, color = NestlyColors.TextPrimary)
        Spacer(Modifier.height(14.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) { content() }
    }
    RowDivider()
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit, swatch: Color? = null) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(if (selected) NestlyColors.Accent else Color.Transparent)
            .border(1.5.dp, NestlyColors.Accent, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (swatch != null) {
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(swatch),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            label,
            style = NestlyType.Small.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) Color.White else NestlyColors.AccentText,
        )
    }
}

@Composable
fun SleepForm(form: FormScope) {
    val e = form.effective
    val seconds = e.endTime?.let { (it - e.startTime).coerceAtLeast(0) / 1000 } ?: 0
    StopwatchSection(form, TimerKey.SLEEP, seconds, title = "Total Time")
    TimeRow(form, "Start Time")
    TimeRow(form, "End Time", endTime = true)
}

@Composable
fun RoutineForm(form: FormScope) {
    val d = form.draft
    var picking by remember { mutableStateOf(false) }
    TimeRow(form, "Time")
    FormRow(
        "Routine",
        value = d.routineName,
        action = if (d.routineName == null) "Add" else null,
        chevron = true,
        onClick = { picking = true },
    )
    if (picking) {
        OptionPickerDialog(
            title = "Routine",
            headerColor = EntryKind.ROUTINE.card.headerColor,
            options = RoutineCatalog.routines,
            initial = listOfNotNull(d.routineName),
            multiSelect = false,
            onDismiss = { picking = false },
            onConfirm = { picked ->
                picking = false
                form.update { it.copy(routineName = picked.firstOrNull()) }
            },
        )
    }
}

@Composable
fun BabyFirstForm(form: FormScope) {
    TimeRow(form, "Date", dateOnly = true)
    InlineTextRow("Baby First", form.draft.babyFirstTitle) { text -> form.update { it.copy(babyFirstTitle = text) } }
}

@Composable
fun GrowthForm(form: FormScope) {
    val d = form.draft
    val units = currentUnitPrefs()
    TimeRow(form, "Date", dateOnly = true)
    MeasureRow(
        "Weight",
        Units.weightLabel(units.weight),
        d.weightKg?.let { Units.kgToDisplay(it, units.weight) },
    ) { v -> form.update { it.copy(weightKg = v?.let { Units.displayToKg(it, units.weight) }) } }
    MeasureRow(
        "Height",
        Units.lengthLabel(units.length),
        d.heightCm?.let { Units.cmToDisplay(it, units.length) },
    ) { v -> form.update { it.copy(heightCm = v?.let { Units.displayToCm(it, units.length) }) } }
    MeasureRow(
        "Head Size",
        Units.lengthLabel(units.length),
        d.headCm?.let { Units.cmToDisplay(it, units.length) },
    ) { v -> form.update { it.copy(headCm = v?.let { Units.displayToCm(it, units.length) }) } }
}

@Composable
private fun MeasureRow(label: String, unit: String, value: Double?, onChange: (Double?) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    FormRow(
        label,
        value = value?.let { "${Format.number(it)} $unit" },
        action = if (value == null) "Add" else null,
        onClick = { editing = true },
    )
    if (editing) {
        NumberInputDialog(label, unit, value, onDismiss = { editing = false }) { v ->
            editing = false
            onChange(v)
        }
    }
}

@Composable
fun MilestoneForm(form: FormScope) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ActivityIcon(EntryKind.MILESTONE.icon, size = 72.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            form.draft.milestoneTitle,
            style = NestlyType.CardTitle,
            color = NestlyColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
    }
    RowDivider()
    TimeRow(form, "Date", dateOnly = true)
}

@Composable
fun MedicalForm(form: FormScope) {
    val d = form.draft
    val units = currentUnitPrefs()
    var editingTemp by remember { mutableStateOf(false) }
    var editingMed by remember { mutableStateOf(false) }
    val tempC = d.temperatureF?.let { Units.displayToC(it, TempUnit.F) }
    TimeRow(form, "Time")
    FormRow(
        "Temperature",
        value = tempC?.let { Format.temp(it, units) },
        action = if (d.temperatureF == null) "Add" else null,
        onClick = { editingTemp = true },
    )
    FormRow(
        "Medication",
        value = d.medicationName?.let { name ->
            listOfNotNull(name, d.dose?.let { "${Format.number(it)} ${d.doseUnit.orEmpty()}".trim() }).joinToString(" · ")
        },
        action = if (d.medicationName == null) "Add" else null,
        chevron = true,
        onClick = { editingMed = true },
    )
    if (editingTemp) {
        val display = tempC?.let { Units.cToDisplay(it, units.temp) }
        NumberInputDialog("Temperature", Units.tempLabel(units.temp), display, onDismiss = { editingTemp = false }) { v ->
            editingTemp = false
            form.update {
                it.copy(
                    temperatureF = v?.let { value ->
                        Units.cToDisplay(Units.displayToC(value, units.temp), TempUnit.F)
                    },
                )
            }
        }
    }
    if (editingMed) {
        MedicationDialog(
            name = d.medicationName.orEmpty(),
            dose = d.dose,
            unit = d.doseUnit ?: MedicationCatalog.units.first(),
            onDismiss = { editingMed = false },
            onConfirm = { name, dose, unit ->
                editingMed = false
                form.update {
                    if (name.isBlank()) it.copy(medicationName = null, dose = null, doseUnit = null)
                    else it.copy(medicationName = name, dose = dose, doseUnit = unit.takeIf { dose != null })
                }
            },
        )
    }
}

@Composable
private fun MedicationDialog(
    name: String,
    dose: Double?,
    unit: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double?, String) -> Unit,
) {
    var nameText by remember { mutableStateOf(name) }
    var doseText by remember { mutableStateOf(dose?.let { Format.number(it) } ?: "") }
    var unitChoice by remember { mutableStateOf(unit) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NestlyColors.AccentText,
        unfocusedBorderColor = NestlyColors.Divider,
        cursorColor = NestlyColors.AccentText,
        focusedLabelColor = NestlyColors.AccentText,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NestlyColors.Surface,
        title = { Text("Medication", style = NestlyType.RowLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    nameText,
                    { nameText = it },
                    label = { Text("Name") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    doseText,
                    { new -> if (new.count { it == '.' } <= 1 && new.all { it.isDigit() || it == '.' }) doseText = new },
                    label = { Text("Dose") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MedicationCatalog.units.forEach { option ->
                        Chip(option, selected = unitChoice == option, onClick = { unitChoice = option })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(nameText.trim(), doseText.toDoubleOrNull(), unitChoice) }) {
                Text("OK", color = NestlyColors.AccentText, style = NestlyType.Button)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = NestlyColors.AccentText, style = NestlyType.Button) }
        },
    )
}

@Composable
fun VaccineForm(form: FormScope) {
    val d = form.draft
    var picking by remember { mutableStateOf(false) }
    TimeRow(form, "Date", dateOnly = true)
    FormRow(
        "Vaccines",
        value = d.vaccines.joinToString(", ").ifEmpty { null },
        action = if (d.vaccines.isEmpty()) "Add" else null,
        chevron = true,
        onClick = { picking = true },
    )
    if (picking) {
        OptionPickerDialog(
            title = "Vaccines",
            headerColor = EntryKind.VACCINE.card.headerColor,
            options = VaccineCatalog.vaccines,
            initial = d.vaccines,
            multiSelect = true,
            onDismiss = { picking = false },
            onConfirm = { picked ->
                picking = false
                form.update { it.copy(vaccines = picked) }
            },
        )
    }
}
