package com.lifestyle.nestly.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
private fun DialogButton(text: String, onClick: () -> Unit) =
    TextButton(onClick = onClick) { Text(text, color = NestlyColors.AccentText, style = NestlyType.Button) }

// Flat grey spinner picker matching the legacy Android TimePicker in the reference screenshot:
// three wheels (hour / minute / AM-PM), selected row in white between two rules, neighbours dimmed.
private val SpinnerPickerBackground = Color(0xFF4D4D4D)
private val SpinnerPickerRule = Color(0xFFCFCFCF)
private val SpinnerPickerDimmed = Color(0xFFA6A6A6)
private val SpinnerPickerItemHeight = 56.dp

@Composable
private fun WheelPickerColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // One blank row above and below the values lets the first/last value sit in the middle row.
    // With three rows visible, firstVisibleItemIndex therefore equals the selected index.
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val scope = rememberCoroutineScope()
    val centeredIndex by remember(listState) {
        derivedStateOf { listState.firstVisibleItemIndex.coerceIn(0, items.lastIndex) }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { !it }
            .collect {
                onSelectedIndexChange(listState.firstVisibleItemIndex.coerceIn(0, items.lastIndex))
            }
    }

    Box(modifier.height(SpinnerPickerItemHeight * 3)) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items.size + 2) { position ->
                if (position == 0 || position == items.size + 1) {
                    Spacer(Modifier.fillMaxWidth().height(SpinnerPickerItemHeight))
                } else {
                    val itemIndex = position - 1
                    val isCentered = itemIndex == centeredIndex
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(SpinnerPickerItemHeight)
                            .clickable(enabled = !isCentered) {
                                scope.launch { listState.animateScrollToItem(itemIndex) }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            items[itemIndex],
                            color = if (isCentered) Color.White else SpinnerPickerDimmed,
                            fontSize = 18.sp,
                            fontWeight = if (isCentered) FontWeight.Medium else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
        // Rules above and below the selected (middle) row, with a gap between columns.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .offset(y = SpinnerPickerItemHeight)
                .height(1.dp)
                .background(SpinnerPickerRule),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .offset(y = SpinnerPickerItemHeight * 2)
                .height(1.dp)
                .background(SpinnerPickerRule),
        )
    }
}

@Composable
private fun SpinnerTimePickerDialog(
    initialHour24: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour24: Int, minute: Int) -> Unit,
) {
    var hourIndex by remember { mutableIntStateOf((initialHour24 + 11) % 12) } // 0 -> "1" ... 11 -> "12"
    var minute by remember { mutableIntStateOf(initialMinute) }
    var amPmIndex by remember { mutableIntStateOf(if (initialHour24 >= 12) 1 else 0) } // 0 AM, 1 PM

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth(0.82f)
                .widthIn(max = 340.dp)
                .background(SpinnerPickerBackground)
                .padding(horizontal = 28.dp, vertical = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                WheelPickerColumn(
                    items = (1..12).map { it.toString() },
                    selectedIndex = hourIndex,
                    onSelectedIndexChange = { hourIndex = it },
                    modifier = Modifier.weight(1f),
                )
                Text(
                    ":",
                    color = SpinnerPickerDimmed,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(20.dp),
                )
                WheelPickerColumn(
                    items = (0..59).map { it.toString().padStart(2, '0') },
                    selectedIndex = minute,
                    onSelectedIndexChange = { minute = it },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(12.dp))
                WheelPickerColumn(
                    items = listOf("AM", "PM"),
                    selectedIndex = amPmIndex,
                    onSelectedIndexChange = { amPmIndex = it },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("CANCEL", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        val hour12 = hourIndex + 1
                        onConfirm((hour12 % 12) + if (amPmIndex == 1) 12 else 0, minute)
                    },
                ) {
                    Text("OK", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

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
        SpinnerTimePickerDialog(
            initialHour24 = initialZoned.hour,
            initialMinute = initialZoned.minute,
            onDismiss = onDismiss,
        ) { hour24, minute ->
            onConfirm(date.atTime(LocalTime.of(hour24, minute)).atZone(zone).toInstant().toEpochMilli())
        }
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
