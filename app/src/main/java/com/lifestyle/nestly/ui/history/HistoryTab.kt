package com.lifestyle.nestly.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.ui.AppViewModel
import com.lifestyle.nestly.ui.Routes
import com.lifestyle.nestly.ui.activity.EntryListRow
import com.lifestyle.nestly.ui.activity.FiltersSheet
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.HeaderIconButton
import com.lifestyle.nestly.ui.components.ProfileHeader
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.entry.EntryRequest
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryTab(
    appViewModel: AppViewModel,
    onOpenEntry: (EntryRequest) -> Unit,
    navigate: (String) -> Unit,
) {
    val container = LocalContext.current.appContainer
    val viewModel: HistoryViewModel = viewModel { HistoryViewModel(container) }
    val data by viewModel.data.collectAsStateWithLifecycle()
    val filters by appViewModel.filters.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        ProfileHeader(
            name = data?.profile?.name ?: "Nestly",
            dateText = Format.headerDate(System.currentTimeMillis()),
            ageText = data?.profile?.birthDate?.let { Format.age(it) },
        ) {
            HeaderIconButton(Icons.Outlined.Tune, "Filters", onClick = { showFilters = true }, badge = filters.size)
            Box {
                HeaderIconButton(Icons.Outlined.MoreHoriz, "More", onClick = { showMenu = true })
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = NestlyColors.Surface,
                ) {
                    DropdownMenuItem(
                        text = { Text("Set Reminders", style = NestlyType.RowLabel) },
                        leadingIcon = { Icon(Icons.Outlined.Alarm, null, tint = NestlyColors.TextPrimary) },
                        onClick = {
                            showMenu = false
                            navigate(Routes.REMINDERS)
                        },
                    )
                }
            }
        }

        val history = data
        if (history != null) {
            WeekStrip(
                days = history.days,
                selected = history.selected,
                onSelect = viewModel::selectDay,
                onPrevWeek = { viewModel.shiftWeek(-1) },
                onNextWeek = { viewModel.shiftWeek(1) },
            )
            RowDivider()
            if (history.entries.isEmpty()) {
                Text(
                    "Nothing logged.",
                    style = NestlyType.Body,
                    color = NestlyColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                )
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(history.entries, key = { it.id }) { entry ->
                        EntryListRow(
                            entry = entry,
                            maxBarValue = history.maxBarValue,
                            showIcon = true,
                            onClick = { onOpenEntry(EntryRequest.Edit(entry.id)) },
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }

    if (showFilters) {
        FiltersSheet(
            initial = filters,
            onDismiss = { showFilters = false },
            onApply = {
                appViewModel.setFilters(it)
                showFilters = false
            },
        )
    }
}

@Composable
private fun WeekStrip(
    days: List<HistoryDay>,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(NestlyColors.Surface)
            .padding(bottom = 12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPrevWeek) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous week", tint = NestlyColors.TextPrimary)
            }
            Text(
                weekLabel(days.first().date, days.last().date),
                style = NestlyType.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = NestlyColors.TextPrimary,
            )
            IconButton(onClick = onNextWeek) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Next week", tint = NestlyColors.TextPrimary)
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            days.forEach { day ->
                DayCell(day, selected = day.date == selected, onClick = { onSelect(day.date) })
            }
        }
    }
}

@Composable
private fun DayCell(day: HistoryDay, selected: Boolean, onClick: () -> Unit) {
    val today = LocalDate.now()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Text(
            day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
            style = NestlyType.Small,
            color = NestlyColors.TextSecondary,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (selected) NestlyColors.Accent else NestlyColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.date.dayOfMonth.toString(),
                style = NestlyType.BodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                color = when {
                    selected -> NestlyColors.TextPrimary
                    day.date == today -> NestlyColors.AccentText
                    else -> NestlyColors.TextPrimary
                },
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (day.hasEntries) NestlyColors.AccentText else NestlyColors.Surface),
        )
    }
}

private fun weekLabel(start: LocalDate, end: LocalDate): String {
    val sameMonth = start.month == end.month && start.year == end.year
    return if (sameMonth) {
        "${start.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${start.dayOfMonth}–${end.dayOfMonth}"
    } else {
        val a = "${start.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${start.dayOfMonth}"
        val b = "${end.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${end.dayOfMonth}"
        "$a – $b"
    }
}
