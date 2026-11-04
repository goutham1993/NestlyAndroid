package com.lifestyle.nestly.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.model.ActivityEntity
import com.lifestyle.nestly.ui.AppViewModel
import com.lifestyle.nestly.ui.Routes
import com.lifestyle.nestly.ui.activity.EntryListRow
import com.lifestyle.nestly.ui.activity.FiltersSheet
import com.lifestyle.nestly.ui.activity.card
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.HeaderIconButton
import com.lifestyle.nestly.ui.components.ProfileHeader
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.entry.EntryRequest
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import com.lifestyle.nestly.ui.theme.blobColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** Hour height of the timeline grid; 24h = 720.dp, scrollable under the week strip. */
private val HourHeight = 30.dp
private val GridHeight = HourHeight * 24
private val TimeGutter = 44.dp
private val BarHeight = 12.dp

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
        val history = data
        val zone = remember { ZoneId.systemDefault() }
        ProfileHeader(
            name = history?.profile?.name ?: "Nestly",
            // Match the reference: the date under the name is the selected day, not today.
            dateText = history?.selected?.let {
                Format.headerDate(it.atStartOfDay(zone).toInstant().toEpochMilli(), zone)
            } ?: Format.headerDate(System.currentTimeMillis()),
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

        if (history != null) {
            WeekStrip(
                days = history.days,
                selected = history.selected,
                onSelect = viewModel::selectDay,
                onPrevWeek = { viewModel.shiftWeek(-1) },
                onNextWeek = { viewModel.shiftWeek(1) },
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                TimelineGrid(
                    days = history.days,
                    entries = history.weekEntries,
                    selected = history.selected,
                    onSelectDay = viewModel::selectDay,
                    onOpenEntry = onOpenEntry,
                )
                SelectedDayList(history, onOpenEntry)
                Spacer(Modifier.height(24.dp))
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

/**
 * Reference week strip: month tabs (Oct / Nov) over the days they cover, a "<" to go to the
 * previous week, then 2-letter weekday + day-of-month columns. The selected day is a solid
 * blue column flush to the edge. Swipe left/right anywhere on the strip to change weeks.
 */
@Composable
private fun WeekStrip(
    days: List<HistoryDay>,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
) {
    var dragTotal by remember { mutableFloatStateOf(0f) }
    Column(
        Modifier
            .fillMaxWidth()
            .background(NestlyColors.Surface)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onDragEnd = {
                        when {
                            dragTotal > 80f -> onPrevWeek()
                            dragTotal < -80f -> onNextWeek()
                        }
                        dragTotal = 0f
                    },
                    onDragCancel = { dragTotal = 0f },
                ) { _, dragAmount -> dragTotal += dragAmount }
            },
    ) {
        // Month tabs, aligned over the columns belonging to each month.
        Row(Modifier.fillMaxWidth().padding(start = 32.dp)) {
            monthSegments(days).forEach { (month, count) ->
                Box(Modifier.weight(count.toFloat()), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(NestlyColors.SurfaceRaised)
                            .padding(horizontal = 16.dp, vertical = 3.dp),
                    ) {
                        Text(month, style = NestlyType.Small, color = NestlyColors.TextPrimary)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(32.dp)
                    .clickable(onClick = onPrevWeek)
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    "Previous week",
                    tint = NestlyColors.TextPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            days.forEach { day ->
                val isSelected = day.date == selected
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isSelected) NestlyColors.Accent else NestlyColors.Surface)
                        .clickable { onSelect(day.date) }
                        .padding(top = 8.dp, bottom = 12.dp),
                ) {
                    Text(
                        day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2),
                        style = NestlyType.Small.copy(fontWeight = FontWeight.Medium),
                        color = NestlyColors.TextPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        day.date.dayOfMonth.toString(),
                        style = NestlyType.BodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
                        color = NestlyColors.TextPrimary,
                    )
                }
            }
        }
    }
}

private fun monthSegments(days: List<HistoryDay>): List<Pair<String, Int>> {
    val segments = mutableListOf<Pair<String, Int>>()
    days.forEach { day ->
        val label = day.date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val last = segments.lastOrNull()
        if (last != null && last.first == label) segments[segments.lastIndex] = label to last.second + 1
        else segments += label to 1
    }
    return segments
}

/**
 * 7-day x 24-hour grid. Each filtered entry is a short coloured bar (colour = its activity
 * category) placed in its day column at its time of day — orange-yellow for Feed, salmon for
 * Pump, blue for Sleep, etc. Tap a bar to edit it; tap a column to select that day.
 * Dashed lines every 3 hours, night (12–6am / 6pm–12am) bands lighter than the day band,
 * and a white "now" line across today's column.
 */
@Composable
private fun TimelineGrid(
    days: List<HistoryDay>,
    entries: List<ActivityEntity>,
    selected: LocalDate,
    onSelectDay: (LocalDate) -> Unit,
    onOpenEntry: (EntryRequest) -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    Row(Modifier.fillMaxWidth().background(NestlyColors.Background)) {
        // Time labels: 12 AM, 3, 6 AM, 9, 12 PM, 3, 6 PM, 9, 12 AM.
        Box(Modifier.width(TimeGutter).height(GridHeight)) {
            (0..24 step 3).forEach { hour ->
                Text(
                    text = hourLabel(hour),
                    style = NestlyType.Small.copy(fontSize = 12.sp, lineHeight = 14.sp),
                    color = NestlyColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = if (hour == 24) GridHeight - 30.dp else (HourHeight * hour - 8.dp).coerceAtLeast(0.dp))
                        .padding(horizontal = 2.dp),
                )
            }
        }
        BoxWithConstraints(Modifier.weight(1f).height(GridHeight)) {
            val columnWidth = maxWidth / 7
            // Bands: night / day / night.
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(HourHeight * 6).background(NestlyColors.Surface))
                Box(Modifier.fillMaxWidth().height(HourHeight * 12).background(NestlyColors.Background))
                Box(Modifier.fillMaxWidth().height(HourHeight * 6).background(NestlyColors.Surface))
            }
            // Day columns: tap to select, faint wash on the selected day.
            Row(Modifier.fillMaxSize()) {
                days.forEach { day ->
                    Box(
                        Modifier
                            .width(columnWidth)
                            .fillMaxHeight()
                            .background(
                                if (day.date == selected) NestlyColors.Accent.copy(alpha = 0.10f)
                                else androidx.compose.ui.graphics.Color.Transparent,
                            )
                            .clickable { onSelectDay(day.date) },
                    )
                }
            }
            // Dashed 3-hour lines (+ stronger lines at the 6am / 6pm band edges).
            val divider = NestlyColors.Divider
            val bandEdge = NestlyColors.TextMuted
            Canvas(Modifier.fillMaxSize()) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                (0..24 step 3).forEach { hour ->
                    val y = HourHeight.toPx() * hour
                    if (y > size.height) return@forEach
                    drawLine(
                        color = if (hour == 6 || hour == 18) bandEdge else divider,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (hour == 6 || hour == 18) null else dash,
                    )
                }
            }
            // Entry bars.
            entries.forEach { entry ->
                val zoned = Instant.ofEpochMilli(entry.startTime).atZone(zone)
                val dayIndex = days.indexOfFirst { it.date == zoned.toLocalDate() }
                if (dayIndex < 0) return@forEach
                val minutes = zoned.hour * 60 + zoned.minute
                val y = (GridHeight * (minutes / (24f * 60f)) - BarHeight / 2).coerceIn(0.dp, GridHeight - BarHeight)
                Box(
                    Modifier
                        .offset(x = columnWidth * dayIndex + 4.dp, y = y)
                        .width(columnWidth - 8.dp)
                        .height(BarHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(entry.type.card.blobColor)
                        .clickable { onOpenEntry(EntryRequest.Edit(entry.id)) },
                )
            }
            // "Now" line across today's column.
            val todayIndex = days.indexOfFirst { it.date == today }
            if (todayIndex >= 0) {
                val now = Instant.now().atZone(zone)
                val minutes = now.hour * 60 + now.minute
                val y = GridHeight * (minutes / (24f * 60f))
                Box(
                    Modifier
                        .offset(x = columnWidth * todayIndex, y = y)
                        .width(columnWidth)
                        .height(2.dp)
                        .background(androidx.compose.ui.graphics.Color.White),
                )
                Box(
                    Modifier
                        .offset(x = columnWidth * (todayIndex + 1) - 5.dp, y = y - 4.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color.White),
                )
            }
        }
    }
}

@Composable
private fun SelectedDayList(history: HistoryData, onOpenEntry: (EntryRequest) -> Unit) {
    Column(Modifier.fillMaxWidth().background(NestlyColors.Surface)) {
        RowDivider()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                history.selected.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
                    ", " + history.selected.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()) +
                    " " + history.selected.dayOfMonth,
                style = NestlyType.RowLabel,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (history.entries.size == 1) "1 entry" else "${history.entries.size} entries",
                style = NestlyType.Small,
                color = NestlyColors.TextSecondary,
            )
        }
        if (history.entries.isEmpty()) {
            Text(
                "Nothing logged.",
                style = NestlyType.Body,
                color = NestlyColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            )
        } else {
            history.entries.forEach { entry ->
                EntryListRow(
                    entry = entry,
                    maxBarValue = history.maxBarValue,
                    showIcon = true,
                    onClick = { onOpenEntry(EntryRequest.Edit(entry.id)) },
                )
            }
        }
    }
}

private fun hourLabel(hour24: Int): String = when (hour24) {
    0, 24 -> "12\nAM"
    6 -> "6\nAM"
    12 -> "12\nPM"
    18 -> "6\nPM"
    else -> (if (hour24 > 12) hour24 - 12 else hour24).toString()
}
