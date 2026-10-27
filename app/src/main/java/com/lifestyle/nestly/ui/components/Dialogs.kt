package com.lifestyle.nestly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@Composable
private fun DialogButton(text: String, onClick: () -> Unit) =
    TextButton(onClick = onClick) { Text(text, color = NestlyColors.AccentText, style = NestlyType.Button) }

/**
 * Date picker followed by a time picker (unless [dateOnly]). Returns the chosen epoch millis in the
 * device zone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerDialog(
    initial: Long,
    dateOnly: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val initialZoned = Instant.ofEpochMilli(initial).atZone(zone)
    var pickedDate by remember { mutableStateOf<LocalDate?>(null) }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = initialZoned.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    val date = pickedDate
    if (date == null) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                DialogButton("OK") {
                    val chosen = dateState.selectedDateMillis
                        ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                        ?: initialZoned.toLocalDate()
                    if (dateOnly) onConfirm(chosen.atStartOfDay(zone).toInstant().toEpochMilli()) else pickedDate = chosen
                }
            },
            dismissButton = { DialogButton("Cancel", onDismiss) },
            colors = DatePickerDefaults.colors(containerColor = NestlyColors.Surface),
        ) {
            DatePicker(
                dateState,
                colors = DatePickerDefaults.colors(
                    containerColor = NestlyColors.Surface,
                    selectedDayContainerColor = NestlyColors.Accent,
                    todayDateBorderColor = NestlyColors.AccentText,
                    todayContentColor = NestlyColors.AccentText,
                ),
            )
        }
    } else {
        val timeState = rememberTimePickerState(initialZoned.hour, initialZoned.minute, is24Hour = false)
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = NestlyColors.Surface,
            confirmButton = {
                DialogButton("OK") {
                    onConfirm(date.atTime(LocalTime.of(timeState.hour, timeState.minute)).atZone(zone).toInstant().toEpochMilli())
                }
            },
            dismissButton = { DialogButton("Cancel", onDismiss) },
            text = { TimePicker(timeState) },
        )
    }
}

@Composable
private fun nestlyFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NestlyColors.AccentText,
    unfocusedBorderColor = NestlyColors.Divider,
    cursorColor = NestlyColors.AccentText,
    focusedLabelColor = NestlyColors.AccentText,
)

/** Numeric entry with a unit suffix, e.g. amount in mL or weight in kg. */
@Composable
fun NumberInputDialog(
    title: String,
    unit: String,
    initial: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double?) -> Unit,
) {
    var text by remember { mutableStateOf(initial?.let { Format.number(it) } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NestlyColors.Surface,
        title = { Text(title, style = NestlyType.RowLabel) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> if (new.count { it == '.' } <= 1 && new.all { it.isDigit() || it == '.' }) text = new },
                suffix = { Text(unit) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = nestlyFieldColors(),
            )
        },
        confirmButton = { DialogButton("OK") { onConfirm(text.toDoubleOrNull()) } },
        dismissButton = { DialogButton("Cancel", onDismiss) },
    )
}

/** Hours/minutes (and optionally seconds) entry for manual timer edits. */
@Composable
fun DurationInputDialog(
    title: String,
    initialSec: Long,
    showHours: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var hours by remember { mutableStateOf(if (showHours) (initialSec / 3600).toString() else "") }
    var minutes by remember {
        mutableStateOf((if (showHours) (initialSec % 3600) / 60 else initialSec / 60).toString())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NestlyColors.Surface,
        title = { Text(title, style = NestlyType.RowLabel) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (showHours) {
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { if (it.all(Char::isDigit) && it.length <= 2) hours = it },
                        suffix = { Text("h") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = nestlyFieldColors(),
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { if (it.all(Char::isDigit) && it.length <= 3) minutes = it },
                    suffix = { Text("m") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = nestlyFieldColors(),
                    modifier = Modifier.weight(1f),
                )
            }
        },
        confirmButton = {
            DialogButton("OK") {
                onConfirm((hours.toLongOrNull() ?: 0) * 3600 + (minutes.toLongOrNull() ?: 0) * 60)
            }
        },
        dismissButton = { DialogButton("Cancel", onDismiss) },
    )
}

@Composable
fun TextInputDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NestlyColors.Surface,
        title = { Text(title, style = NestlyType.RowLabel) },
        text = { OutlinedTextField(text, { text = it }, colors = nestlyFieldColors(), modifier = Modifier.fillMaxWidth()) },
        confirmButton = { DialogButton("OK") { onConfirm(text.trim()) } },
        dismissButton = { DialogButton("Cancel", onDismiss) },
    )
}

/**
 * Full-screen list picker used for routines, foods and vaccines. Supports multi-select and adding
 * custom entries.
 */
@Composable
fun OptionPickerDialog(
    title: String,
    headerColor: androidx.compose.ui.graphics.Color,
    options: List<String>,
    initial: List<String>,
    multiSelect: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    var selected by remember { mutableStateOf(initial) }
    var custom by remember { mutableStateOf(initial.filter { it !in options }) }
    var adding by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(NestlyColors.Surface),
        ) {
            SheetHeader(title, headerColor, onDismiss, onSave = { onConfirm(selected) }, saveLabel = "Done")
            LazyColumn(Modifier.weight(1f)) {
                items(options + custom) { option ->
                    val isSelected = option in selected
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .clickable {
                                selected = when {
                                    isSelected -> selected - option
                                    multiSelect -> selected + option
                                    else -> listOf(option)
                                }
                                if (!multiSelect && !isSelected) onConfirm(listOf(option))
                            }
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(option, style = NestlyType.RowLabel, modifier = Modifier.weight(1f))
                        if (isSelected) Icon(Icons.Outlined.Check, null, tint = NestlyColors.AccentText, modifier = Modifier.size(24.dp))
                    }
                    RowDivider()
                }
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { adding = true }
                            .padding(20.dp),
                    ) {
                        Text("+ Add your own", style = NestlyType.BodyMedium, color = NestlyColors.AccentText)
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }
        }
    }
    if (adding) {
        TextInputDialog("Add", "", onDismiss = { adding = false }) { value ->
            adding = false
            if (value.isNotEmpty()) {
                custom = custom + value
                selected = if (multiSelect) selected + value else listOf(value)
                if (!multiSelect) onConfirm(listOf(value))
            }
        }
    }
}
