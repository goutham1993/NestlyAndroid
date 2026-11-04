package com.lifestyle.nestly.ui.trends

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.units.UnitPrefs
import com.lifestyle.nestly.data.units.Units
import com.lifestyle.nestly.data.units.VolumeUnit
import com.lifestyle.nestly.ui.activity.EntryListRow
import com.lifestyle.nestly.ui.activity.barValue
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.SegmentedToggle
import com.lifestyle.nestly.ui.entry.EntryRequest
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import com.lifestyle.nestly.ui.theme.headerColor
import com.lifestyle.nestly.ui.units.currentUnitPrefs
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/** Left column shared by the day strip, calendar and graph so their day columns line up. */
private val Gutter = 40.dp
private val CalendarHourHeight = 17.dp
private val CalendarHeight = CalendarHourHeight * 24
private val PointMarkHeight = 5.dp
private val GraphHeight = 380.dp

private enum class DetailTab(val label: String) { CALENDAR("Calendar"), GRAPH("Graph"), ENTRIES("Entries") }

@Composable
fun TrendDetailScreen(
    metric: TrendMetric,
    initialWindow: TrendsWindow,
    onBack: () -> Unit,
    onOpenEntry: (EntryRequest) -> Unit,
) {
    val context = LocalContext.current
    val container = context.appContainer
    val viewModel: TrendDetailViewModel = viewModel { TrendDetailViewModel(container, metric, initialWindow) }
    val data by viewModel.data.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(DetailTab.CALENDAR) }
    val prefs = currentUnitPrefs()
    val zone = remember { ZoneId.systemDefault() }

    Column(
        Modifier
            .fillMaxSize()
            .background(NestlyColors.Background)
            .statusBarsPadding(),
    ) {
        DetailTopBar(
            title = metric.title,
            window = data?.window ?: initialWindow,
            showShare = tab != DetailTab.ENTRIES && data != null,
            onBack = onBack,
            onShare = {
                data?.let { d ->
                    val text = "${d.profileName} · ${metric.title} (${rangeText(d.dates)}): ${metric.summary(d.row.current.value, prefs)}"
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                    context.startActivity(Intent.createChooser(send, null))
                }
            },
            onSelectWindow = viewModel::setWindow,
        )
        val detail = data ?: return@Column
        val current = detail.row.current

        LazyColumn(Modifier.weight(1f)) {
            item(key = "headline") {
                Text(
                    metric.summary(current.value, prefs),
                    style = NestlyType.SheetTitle,
                    color = NestlyColors.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                )
            }
            item(key = "tabs") {
                SegmentedToggle(
                    options = DetailTab.entries.map { it.label },
                    selected = tab.ordinal,
                    onSelect = { tab = DetailTab.entries[it] },
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                )
            }
            when (tab) {
                DetailTab.CALENDAR, DetailTab.GRAPH -> item(key = "chart") {
                    Column {
                        DayStrip(
                            dates = detail.dates,
                            today = LocalDate.now(zone),
                            onPrev = viewModel::previousPeriod,
                            onNext = viewModel::nextPeriod,
                        )
                        if (tab == DetailTab.CALENDAR) CalendarGrid(detail.dates, current.marks, onOpenEntry)
                        else TrendGraph(metric, current.days, current.value, prefs)
                        DetailSummary(detail, prefs)
                    }
                }
                DetailTab.ENTRIES -> {
                    if (current.entries.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                "No entries in this period.",
                                style = NestlyType.Body,
                                color = NestlyColors.TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                            )
                        }
                    }
                    val maxBar = current.entries.mapNotNull { it.barValue }.maxOrNull() ?: 0.0
                    current.entries
                        .groupBy { Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate() }
                        .forEach { (date, entries) ->
                            item(key = "date-$date") {
                                Text(
                                    Format.date(date),
                                    style = NestlyType.Body.copy(fontSize = 17.sp),
                                    color = NestlyColors.TextPrimary,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                )
                            }
                            items(entries, key = { it.id }) { entry ->
                                Column(Modifier.background(NestlyColors.Surface)) {
                                    EntryListRow(entry, maxBar, showIcon = true) { onOpenEntry(EntryRequest.Edit(entry.id)) }
                                }
                            }
                        }
                    item(key = "bottom") { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DetailTopBar(
    title: String,
    window: TrendsWindow,
    showShare: Boolean,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onSelectWindow: (TrendsWindow) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val outline = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = NestlyColors.TextPrimary)
        }
        Text(
            title,
            style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
            color = NestlyColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (showShare) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(outline)
                    .border(1.dp, NestlyColors.TextMuted, outline)
                    .clickable(onClick = onShare),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Share, "Share", tint = NestlyColors.TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
        }
        Box {
            Row(
                Modifier
                    .height(40.dp)
                    .clip(outline)
                    .border(1.dp, NestlyColors.TextMuted, outline)
                    .clickable { menuOpen = true }
                    .padding(start = 12.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(window.label, style = NestlyType.BodyMedium, color = NestlyColors.TextPrimary)
                Icon(Icons.Outlined.KeyboardArrowDown, null, tint = NestlyColors.TextPrimary, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = NestlyColors.Surface,
            ) {
                TrendsWindow.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label, style = NestlyType.Body, color = NestlyColors.TextPrimary) },
                        onClick = {
                            menuOpen = false
                            onSelectWindow(option)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Month tabs over the days they cover, a "<" for the previous period, then a column per day
 * with today highlighted. Swipe right for earlier periods, left to come back towards today.
 */
@Composable
private fun DayStrip(dates: List<LocalDate>, today: LocalDate, onPrev: () -> Unit, onNext: () -> Unit) {
    var dragTotal by remember { mutableFloatStateOf(0f) }
    val prev by rememberUpdatedState(onPrev)
    val next by rememberUpdatedState(onNext)
    Column(
        Modifier
            .fillMaxWidth()
            .background(NestlyColors.Surface)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onDragEnd = {
                        when {
                            dragTotal > 80f -> prev()
                            dragTotal < -80f -> next()
                        }
                        dragTotal = 0f
                    },
                    onDragCancel = { dragTotal = 0f },
                ) { _, amount -> dragTotal += amount }
            },
    ) {
        Row(Modifier.fillMaxWidth().padding(start = Gutter)) {
            monthSegments(dates).forEach { (month, count) ->
                Box(Modifier.weight(count.toFloat()), contentAlignment = Alignment.CenterStart) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(NestlyColors.SurfaceRaised)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    ) {
                        Text(month, style = NestlyType.Small.copy(fontSize = 13.sp), color = NestlyColors.TextPrimary)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(Gutter)
                    .clickable(onClick = onPrev)
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Previous period", tint = NestlyColors.TextPrimary, modifier = Modifier.size(20.dp))
            }
            dates.forEach { date ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .background(if (date == today) NestlyColors.Accent else NestlyColors.Surface)
                        .padding(top = 8.dp, bottom = 10.dp),
                ) {
                    Text(
                        date.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()).take(2),
                        style = NestlyType.Small.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        color = NestlyColors.TextPrimary,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        date.dayOfMonth.toString(),
                        style = NestlyType.BodyMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        color = NestlyColors.TextPrimary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** 24-hour grid with each contributing entry placed in its day column at its time of day. */
@Composable
private fun CalendarGrid(dates: List<LocalDate>, marks: List<TrendMark>, onOpenEntry: (EntryRequest) -> Unit) {
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    Row(Modifier.fillMaxWidth().background(NestlyColors.Background)) {
        Box(Modifier.width(Gutter).height(CalendarHeight)) {
            (0..24 step 3).forEach { hour ->
                Text(
                    hourLabel(hour),
                    style = NestlyType.Small.copy(fontSize = 11.sp, lineHeight = 12.sp),
                    color = NestlyColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = if (hour == 24) CalendarHeight - 26.dp else (CalendarHourHeight * hour - 7.dp).coerceAtLeast(0.dp)),
                )
            }
        }
        BoxWithConstraints(Modifier.weight(1f).height(CalendarHeight)) {
            val columnWidth = maxWidth / dates.size.coerceAtLeast(1)
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(CalendarHourHeight * 6).background(NestlyColors.Surface))
                Box(Modifier.fillMaxWidth().height(CalendarHourHeight * 12).background(NestlyColors.Background))
                Box(Modifier.fillMaxWidth().height(CalendarHourHeight * 6).background(NestlyColors.Surface))
            }
            val divider = NestlyColors.Divider
            Canvas(Modifier.fillMaxSize()) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                (3..21 step 3).forEach { hour ->
                    val y = CalendarHourHeight.toPx() * hour
                    drawLine(divider, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(), pathEffect = dash)
                }
            }
            val inset = (columnWidth * 0.12f).coerceAtMost(6.dp)
            dates.forEachIndexed { index, date ->
                val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val dayLength = (dayEnd - dayStart).toFloat()
                marks.forEach { mark ->
                    val isPoint = mark.endMs <= mark.startMs
                    val visible = if (isPoint) mark.startMs in dayStart until dayEnd else mark.endMs > dayStart && mark.startMs < dayEnd
                    if (!visible) return@forEach
                    val top = CalendarHeight * ((maxOf(mark.startMs, dayStart) - dayStart) / dayLength)
                    val bottom = CalendarHeight * ((minOf(mark.endMs, dayEnd) - dayStart) / dayLength)
                    val y = if (isPoint) top - PointMarkHeight / 2 else top
                    val height = if (isPoint) PointMarkHeight else (bottom - top).coerceAtLeast(PointMarkHeight)
                    Box(
                        Modifier
                            .offset(x = columnWidth * index + inset, y = y.coerceIn(0.dp, CalendarHeight - height))
                            .width(columnWidth - inset * 2)
                            .height(height)
                            .clip(RoundedCornerShape(2.dp))
                            .background(mark.series.color)
                            .clickable { onOpenEntry(EntryRequest.Edit(mark.entryId)) },
                    )
                }
            }
            val todayIndex = dates.indexOf(today)
            if (todayIndex >= 0) {
                val now = Instant.now().atZone(zone)
                val y = CalendarHeight * ((now.hour * 60 + now.minute) / (24f * 60f))
                Box(
                    Modifier
                        .offset(x = columnWidth * todayIndex, y = y)
                        .width(columnWidth)
                        .height(2.dp)
                        .background(Color.White),
                )
                Box(
                    Modifier
                        .offset(x = columnWidth * (todayIndex + 1) - 5.dp, y = y - 4.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
        }
    }
}

private class Axis(val max: Double, val step: Double, val label: (Double) -> String)

/**
 * One bar per day. Counts are drawn as stacked blocks (one per session / diaper / nap);
 * volumes and durations as stacked segments per series. A dashed line marks the average.
 */
@Composable
private fun TrendGraph(metric: TrendMetric, days: List<TrendDay>, average: Double, prefs: UnitPrefs) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = NestlyColors.TextSecondary)
    val avgStyle = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
    val peak = maxOf(days.maxOfOrNull { it.total } ?: 0.0, average)
    val axis = remember(metric, peak, prefs) { axisFor(metric, peak, prefs) }
    val blocks = metric.unit == MetricUnit.COUNT && metric.stat == MetricStat.PER_DAY
    val divider = NestlyColors.Divider
    val accent = NestlyColors.Accent

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(GraphHeight)
            .background(NestlyColors.Background),
    ) {
        val gutter = Gutter.toPx()
        val top = 16.dp.toPx()
        val bottom = size.height - 1.dp.toPx()
        val plotHeight = bottom - top
        fun y(value: Double) = bottom - (value / axis.max).toFloat() * plotHeight

        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
        val minorStep = axis.step / 5
        var minor = minorStep
        while (minor < axis.max - minorStep / 2) {
            val yy = y(minor)
            drawLine(divider, Offset(gutter - 5.dp.toPx(), yy), Offset(gutter - 1.dp.toPx(), yy), 1.dp.toPx())
            minor += minorStep
        }
        var tick = 0.0
        while (tick <= axis.max + axis.step / 2) {
            val yy = y(tick)
            if (tick > 0) drawLine(divider, Offset(gutter, yy), Offset(size.width, yy), 1.dp.toPx(), pathEffect = dash)
            val text = measurer.measure(axis.label(tick), labelStyle)
            drawText(
                text,
                topLeft = Offset(gutter - 8.dp.toPx() - text.size.width, (yy - text.size.height / 2f).coerceIn(0f, size.height - text.size.height)),
            )
            tick += axis.step
        }
        drawLine(divider, Offset(gutter, bottom), Offset(size.width, bottom), 1.dp.toPx())

        val count = days.size.coerceAtLeast(1)
        val columnWidth = (size.width - gutter) / count
        val barWidth = minOf(columnWidth * 0.86f, 56.dp.toPx())
        val gap = 2.dp.toPx()
        days.forEachIndexed { index, day ->
            val x = gutter + columnWidth * index + (columnWidth - barWidth) / 2
            var stacked = 0.0
            day.segments.forEach { segment ->
                if (blocks) {
                    repeat(segment.value.roundToInt()) {
                        val blockTop = y(stacked + 1) + gap / 2
                        val blockBottom = y(stacked) - gap / 2
                        drawRect(segment.series.color, Offset(x, blockTop), Size(barWidth, (blockBottom - blockTop).coerceAtLeast(1f)))
                        stacked += 1
                    }
                } else if (segment.value > 0) {
                    val segTop = y(stacked + segment.value)
                    val segBottom = y(stacked) - if (stacked > 0) 1.dp.toPx() else 0f
                    drawRect(segment.series.color, Offset(x, segTop), Size(barWidth, (segBottom - segTop).coerceAtLeast(1f)))
                    stacked += segment.value
                }
            }
        }

        if (average > 0) {
            val ya = y(average)
            drawLine(accent, Offset(gutter, ya), Offset(size.width, ya), 1.5.dp.toPx(), pathEffect = dash)
            val pillHeight = 16.dp.toPx()
            val pillLeft = 2.dp.toPx()
            drawRoundRect(
                accent,
                topLeft = Offset(pillLeft, ya - pillHeight / 2),
                size = Size(gutter - pillLeft * 2, pillHeight),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
            val text = measurer.measure("AVG", avgStyle)
            drawText(text, topLeft = Offset((gutter - text.size.width) / 2, ya - text.size.height / 2f))
        }
    }
}

private fun axisFor(metric: TrendMetric, peak: Double, prefs: UnitPrefs): Axis = when (metric.unit) {
    MetricUnit.COUNT -> {
        val target = maxOf(peak * 1.25, 5.0)
        val step = niceStep(target / 5).coerceAtLeast(1.0)
        Axis(ceil(target / step) * step, step) { Format.number(it, 0) }
    }
    MetricUnit.VOLUME -> {
        val unit = prefs.volume
        val target = maxOf(Units.mlToDisplay(peak, unit) * 1.25, if (unit == VolumeUnit.OZ) 4.0 else 100.0)
        val step = niceStep(target / 5)
        Axis(Units.displayToMl(ceil(target / step) * step, unit), Units.displayToMl(step, unit)) {
            Format.number(Units.mlToDisplay(it, unit), 1)
        }
    }
    MetricUnit.DURATION -> {
        val target = maxOf(peak * 1.25, 3600.0)
        val step = DurationSteps.firstOrNull { it >= target / 5 } ?: DurationSteps.last()
        Axis(ceil(target / step) * step, step, ::durationLabel)
    }
}

private val DurationSteps = listOf(60.0, 300.0, 600.0, 900.0, 1800.0, 3600.0, 7200.0, 10800.0, 14400.0, 21600.0, 43200.0)

private fun niceStep(raw: Double): Double {
    if (raw <= 0) return 1.0
    val magnitude = 10.0.pow(floor(log10(raw)))
    return listOf(1.0, 2.0, 5.0, 10.0).map { it * magnitude }.first { it >= raw }
}

/** "0", "30m", "2h", "1h 30m". */
private fun durationLabel(seconds: Double): String {
    val total = seconds.roundToInt()
    val h = total / 3600
    val m = (total % 3600) / 60
    return when {
        total == 0 -> "0"
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

/** Legend with each series' value, then the change against the previous period. */
@Composable
private fun DetailSummary(detail: TrendDetailData, prefs: UnitPrefs) {
    val row = detail.row
    val metric = row.metric
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val segments = row.current.breakdown.filter { it.value > 0 }
        if (segments.isEmpty()) {
            Text("Nothing logged in this period.", style = NestlyType.Body, color = NestlyColors.TextSecondary)
        }
        segments.forEach { segment ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                SeriesDot(segment.series, size = 18.dp, square = true)
                Spacer(Modifier.width(10.dp))
                Text(segment.series.label, style = NestlyType.Body, color = NestlyColors.TextPrimary)
                Spacer(Modifier.width(8.dp))
                Text(
                    metric.formatValue(segment.value, prefs),
                    style = NestlyType.BodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = NestlyColors.TextPrimary,
                )
            }
        }
        val delta = row.delta ?: return@Column
        Spacer(Modifier.height(10.dp))
        if (metric.isMeaningful(delta)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(metric.section.headerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (delta > 0) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                        null,
                        tint = NestlyColors.HeaderText,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                val amount = metric.formatValue(abs(delta), prefs)
                Text(
                    if (metric.unit == MetricUnit.COUNT) "$amount ${metric.noun}" else amount,
                    style = NestlyType.Body,
                    color = NestlyColors.TextPrimary,
                )
            }
            Text(
                metric.comparison(delta, detail.window),
                style = NestlyType.Body,
                color = NestlyColors.TextPrimary,
                textAlign = TextAlign.Center,
            )
        } else {
            val period = if (detail.window.days == 1) "the previous day" else "the previous ${detail.window.days} days"
            Text("Same as $period", style = NestlyType.Body, color = NestlyColors.TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

private fun monthSegments(dates: List<LocalDate>): List<Pair<String, Int>> {
    val segments = mutableListOf<Pair<String, Int>>()
    dates.forEach { date ->
        val label = date.month.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
        val last = segments.lastOrNull()
        if (last != null && last.first == label) segments[segments.lastIndex] = label to last.second + 1
        else segments += label to 1
    }
    return segments
}

private fun hourLabel(hour24: Int): String = when (hour24) {
    0, 24 -> "12\nAM"
    6 -> "6\nAM"
    12 -> "12\nPM"
    18 -> "6\nPM"
    else -> (if (hour24 > 12) hour24 - 12 else hour24).toString()
}

private val RangeFormat = DateTimeFormatter.ofPattern("MMM d", Locale.US)

private fun rangeText(dates: List<LocalDate>): String {
    val first = dates.firstOrNull() ?: return ""
    val last = dates.last()
    return if (first == last) RangeFormat.format(first) else "${RangeFormat.format(first)} – ${RangeFormat.format(last)}"
}
