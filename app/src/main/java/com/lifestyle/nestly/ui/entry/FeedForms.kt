package com.lifestyle.nestly.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.EggAlt
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.catalog.FoodCatalog
import com.lifestyle.nestly.data.model.BreastSide
import com.lifestyle.nestly.data.model.MealType
import com.lifestyle.nestly.data.model.MilkType
import com.lifestyle.nestly.data.prefs.ActivityLayout
import com.lifestyle.nestly.data.prefs.ActivityToggle
import com.lifestyle.nestly.data.timer.TimerKey
import com.lifestyle.nestly.data.units.Units
import com.lifestyle.nestly.ui.components.ChoiceRow
import com.lifestyle.nestly.ui.components.CircleToggle
import com.lifestyle.nestly.ui.components.DurationInputDialog
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.FormRow
import com.lifestyle.nestly.ui.components.NumberInputDialog
import com.lifestyle.nestly.ui.components.OptionPickerDialog
import com.lifestyle.nestly.ui.components.PillButton
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.components.SegmentedToggle
import com.lifestyle.nestly.ui.components.TextInputDialog
import com.lifestyle.nestly.ui.components.TimerReadout
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import com.lifestyle.nestly.ui.theme.headerColor
import com.lifestyle.nestly.ui.units.currentUnitPrefs

/** "12M 5S" while under an hour, "1H 3M" after. */
fun timerParts(seconds: Long): List<Pair<Long, String>> =
    if (seconds >= 3600) listOf(seconds / 3600 to "H", (seconds % 3600) / 60 to "M")
    else listOf(seconds / 60 to "M", seconds % 60 to "S")

fun hourMinuteParts(seconds: Long): List<Pair<Long, String>> =
    listOf(seconds / 3600 to "H", (seconds % 3600) / 60 to "M")

@Composable
fun BreastfeedForm(form: FormScope) {
    BreastTimers(form)
    TimeRow(form, "Start Time")
    TotalTimeRow(form.effective.leftSec + form.effective.rightSec)
}

@Composable
private fun BreastTimers(form: FormScope) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        SideTimer(form, BreastSide.LEFT, TimerKey.BREAST_LEFT, form.effective.leftSec)
        SideTimer(form, BreastSide.RIGHT, TimerKey.BREAST_RIGHT, form.effective.rightSec)
    }
    RowDivider()
}

@Composable
private fun SideTimer(form: FormScope, side: BreastSide, key: TimerKey, seconds: Long) {
    var editing by remember { mutableStateOf(false) }
    val label = if (side == BreastSide.LEFT) "Left" else "Right"
    val running = form.controller.usesTimers && form.timer(key).isRunning
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.height(22.dp), contentAlignment = Alignment.Center) {
            if (!form.controller.isEditing && form.controller.lastSide == side) {
                Text("last side", style = NestlyType.Small, color = NestlyColors.TextSecondary)
            }
        }
        TimerReadout(timerParts(seconds), onEdit = { editing = true })
        Spacer(Modifier.height(8.dp))
        if (form.controller.usesTimers) {
            CircleToggle(
                label = if (running) "Pause" else label,
                selected = running,
                onClick = { form.controller.toggleTimer(key) },
                size = 110.dp,
            )
        } else {
            Text(label, style = NestlyType.BodyMedium, color = NestlyColors.TextSecondary)
        }
    }
    if (editing) {
        DurationInputDialog(
            title = "$label Side",
            initialSec = seconds,
            showHours = false,
            onDismiss = { editing = false },
            onConfirm = { sec ->
                editing = false
                form.controller.setTimerDuration(key, sec)
            },
        )
    }
}

@Composable
private fun TotalTimeRow(seconds: Long) {
    FormRow("Total Time", value = Format.duration(seconds), valueColor = NestlyColors.TextSecondary)
}

@Composable
fun BottleForm(form: FormScope) {
    val prefs = LocalContext.current.appContainer.preferences
    val layout by prefs.layout.collectAsState(initial = ActivityLayout.DEFAULT)
    BottleFields(form, layout)
}

@Composable
private fun BottleFields(form: FormScope, layout: ActivityLayout) {
    val d = form.draft
    val types = buildList {
        if (layout.isEnabled(ActivityToggle.BOTTLE_BREAST_MILK) || d.milkType == MilkType.BREAST_MILK) add(MilkType.BREAST_MILK)
        if (layout.isEnabled(ActivityToggle.BOTTLE_FORMULA) || d.milkType == MilkType.FORMULA) add(MilkType.FORMULA)
    }
    if (types.isNotEmpty()) {
        ChoiceRow {
            types.forEach { type ->
                CircleToggle(
                    label = if (type == MilkType.BREAST_MILK) "Breast\nMilk" else "Formula",
                    selected = d.milkType == type,
                    onClick = { form.update { it.copy(milkType = if (it.milkType == type) null else type) } },
                )
            }
        }
        RowDivider()
    }
    TimeRow(form, "Start Time")

    var editingAmount by remember { mutableStateOf(false) }
    val units = currentUnitPrefs()
    FormRow(
        "Amount",
        value = d.bottleMl?.let { Format.volume(it, units) },
        action = if (d.bottleMl == null) "Add" else null,
        onClick = { editingAmount = true },
    )
    val last = form.controller.lastBottleMl
    if (d.bottleMl == null && last != null && !form.controller.isEditing) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 82.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Use last amount: ${Format.volume(last, units)}?",
                style = NestlyType.Body,
                color = NestlyColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            PillButton("Yes", onClick = { form.update { it.copy(bottleMl = last) } }, compact = true)
        }
        RowDivider()
    }
    if (d.milkType == MilkType.FORMULA) {
        var editingName by remember { mutableStateOf(false) }
        FormRow(
            "Formula",
            value = d.formulaName,
            action = if (d.formulaName.isNullOrBlank()) "Add" else null,
            onClick = { editingName = true },
        )
        if (editingName) {
            TextInputDialog("Formula", d.formulaName.orEmpty(), onDismiss = { editingName = false }) { name ->
                editingName = false
                form.update { it.copy(formulaName = name.ifEmpty { null }) }
            }
        }
    }
    // Set when the user manually enters an amount that differs from the stored default;
    // drives the "update your default?" prompt below.
    var pendingDefaultMl by remember { mutableStateOf<Double?>(null) }
    if (editingAmount) {
        val display = d.bottleMl?.let { Units.mlToDisplay(it, units.volume) }
        NumberInputDialog("Amount", Units.volumeLabel(units.volume), display, onDismiss = { editingAmount = false }) { value ->
            editingAmount = false
            val newMl = value?.let { v -> Units.displayToMl(v, units.volume) }
            form.update { it.copy(bottleMl = newMl) }
            if (shouldOfferDefaultBottleUpdate(form.controller.isEditing, newMl, form.controller.defaultBottleMl)) {
                pendingDefaultMl = newMl
            }
        }
    }
    pendingDefaultMl?.let { pendingMl ->
        val currentDefault = form.controller.defaultBottleMl
        AlertDialog(
            onDismissRequest = { pendingDefaultMl = null },
            containerColor = NestlyColors.Surface,
            title = {
                Text(
                    if (currentDefault == null) "Set as default amount?" else "Update default amount?",
                    style = NestlyType.RowLabel,
                )
            },
            text = {
                Text(
                    if (currentDefault == null) {
                        "Use ${Format.volume(pendingMl, units)} as your default bottle amount? " +
                            "It will pre-fill future bottle feeds. This feed will use ${Format.volume(pendingMl, units)} either way."
                    } else {
                        "Your default bottle amount is ${Format.volume(currentDefault, units)}. " +
                            "Change it to ${Format.volume(pendingMl, units)} for future feeds? " +
                            "This feed will use ${Format.volume(pendingMl, units)} either way."
                    },
                    style = NestlyType.Small,
                    color = NestlyColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    form.controller.updateDefaultBottleMl(pendingMl)
                    pendingDefaultMl = null
                }) {
                    Text(if (currentDefault == null) "Yes, set default" else "Yes, update default", color = NestlyColors.AccentText)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDefaultMl = null }) {
                    Text(if (currentDefault == null) "Not now" else "No, keep default", color = NestlyColors.AccentText)
                }
            },
        )
    }
}

@Composable
fun SolidsForm(form: FormScope) {
    val d = form.draft
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 28.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        MealType.entries.forEach { meal ->
            MealButton(meal, selected = d.mealType == meal) {
                form.update { it.copy(mealType = if (it.mealType == meal) null else meal) }
            }
        }
    }
    RowDivider()
    TimeRow(form, "Start Time")

    var picking by remember { mutableStateOf(false) }
    FormRow(
        "Food",
        value = d.foods.joinToString(", ").ifEmpty { null },
        action = if (d.foods.isEmpty()) "Add" else null,
        chevron = true,
        onClick = { picking = true },
    )
    if (picking) {
        OptionPickerDialog(
            title = "Food",
            headerColor = EntryKind.SOLIDS.card.headerColor,
            options = FoodCatalog.foods,
            initial = d.foods,
            multiSelect = true,
            onDismiss = { picking = false },
            onConfirm = { foods ->
                picking = false
                form.update { it.copy(foods = foods) }
            },
        )
    }
}

private val MealType.label: String
    get() = name.lowercase().replaceFirstChar { it.uppercase() }

private val MealType.icon: ImageVector
    get() = when (this) {
        MealType.BREAKFAST -> Icons.Outlined.EggAlt
        MealType.LUNCH -> Icons.Outlined.LunchDining
        MealType.DINNER -> Icons.Outlined.DinnerDining
        MealType.SNACK -> Icons.Outlined.Cookie
    }

@Composable
private fun MealButton(meal: MealType, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(if (selected) NestlyColors.Accent else Color.Transparent)
                .border(1.5.dp, NestlyColors.Accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                meal.icon,
                contentDescription = null,
                tint = if (selected) Color.White else NestlyColors.AccentText,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(meal.label, style = NestlyType.Small.copy(fontWeight = FontWeight.SemiBold), color = NestlyColors.TextPrimary)
    }
}

@Composable
fun ComboForm(form: FormScope) {
    val prefs = LocalContext.current.appContainer.preferences
    val layout by prefs.layout.collectAsState(initial = ActivityLayout.DEFAULT)
    Box(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        SegmentedToggle(
            options = listOf("Breastfeed", "Bottle Feed"),
            selected = form.draft.comboTab,
            onSelect = { tab -> form.update { it.copy(comboTab = tab) } },
        )
    }
    if (form.draft.comboTab == 0) {
        BreastTimers(form)
        TimeRow(form, "Start Time")
        TotalTimeRow(form.effective.leftSec + form.effective.rightSec)
    } else {
        RowDivider()
        BottleFields(form, layout)
    }
}

@Composable
fun PumpForm(form: FormScope) {
    val d = form.draft
    val e = form.effective
    Box(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        SegmentedToggle(
            options = listOf("Left / Right", "Total"),
            selected = if (d.pumpMode == PumpMode.LEFT_RIGHT) 0 else 1,
            onSelect = { index ->
                form.update { it.copy(pumpMode = if (index == 0) PumpMode.LEFT_RIGHT else PumpMode.TOTAL) }
            },
        )
    }
    StopwatchSection(form, TimerKey.PUMP, e.pumpSec)
    TimeRow(form, "Start Time")

    var editingDuration by remember { mutableStateOf(false) }
    FormRow(
        "Total Time",
        value = if (e.pumpSec > 0) Format.duration(e.pumpSec) else null,
        action = if (e.pumpSec > 0) null else "Add",
        onClick = { editingDuration = true },
    )
    if (editingDuration) {
        DurationInputDialog("Total Time", e.pumpSec, showHours = true, onDismiss = { editingDuration = false }) { sec ->
            editingDuration = false
            form.controller.setTimerDuration(TimerKey.PUMP, sec)
        }
    }

    if (d.pumpMode == PumpMode.LEFT_RIGHT) {
        AmountRow("Left Side Amount", d.pumpLeftMl) { v -> form.update { it.copy(pumpLeftMl = v) } }
        AmountRow("Right Side Amount", d.pumpRightMl) { v -> form.update { it.copy(pumpRightMl = v) } }
        FormRow(
            "Total",
            value = d.pumpTotalComputed?.let { Format.volume(it, currentUnitPrefs()) } ?: "0 ${Units.volumeLabel(currentUnitPrefs().volume)}",
            valueColor = NestlyColors.TextSecondary,
        )
    } else {
        AmountRow("Total Amount", d.pumpTotalMl) { v -> form.update { it.copy(pumpTotalMl = v) } }
    }
}

@Composable
private fun AmountRow(label: String, value: Double?, onChange: (Double?) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    val units = currentUnitPrefs()
    FormRow(
        label,
        value = value?.let { Format.volume(it, units) },
        action = if (value == null) "Add" else null,
        onClick = { editing = true },
    )
    if (editing) {
        val display = value?.let { Units.mlToDisplay(it, units.volume) }
        NumberInputDialog(label, Units.volumeLabel(units.volume), display, onDismiss = { editing = false }) { v ->
            editing = false
            onChange(v?.let { Units.displayToMl(it, units.volume) })
        }
    }
}

/** Big "0H 0M" readout with a pencil, plus a Start/Pause Timer pill for new entries. */
@Composable
fun StopwatchSection(form: FormScope, key: TimerKey, seconds: Long, title: String? = null) {
    var editing by remember { mutableStateOf(false) }
    val running = form.controller.usesTimers && form.timer(key).isRunning
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (title != null) {
            Text(title, style = NestlyType.BodyMedium, color = NestlyColors.TextSecondary)
            Spacer(Modifier.height(4.dp))
        }
        TimerReadout(hourMinuteParts(seconds), onEdit = { editing = true })
        if (form.controller.usesTimers) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton(
                    text = when {
                        running -> "Pause Timer"
                        form.timer(key).isIdle -> "Start Timer"
                        else -> "Resume Timer"
                    },
                    onClick = { form.controller.toggleTimer(key) },
                )
                if (!form.timer(key).isIdle) {
                    PillButton("Reset", onClick = { form.controller.discardTimers() })
                }
            }
        }
    }
    RowDivider()
    if (editing) {
        DurationInputDialog("Duration", seconds, showHours = true, onDismiss = { editing = false }) { sec ->
            editing = false
            form.controller.setTimerDuration(key, sec)
        }
    }
}