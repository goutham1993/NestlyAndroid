package com.lifestyle.nestly.ui.growth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.growth.GrowthMetric
import com.lifestyle.nestly.data.growth.GrowthPercentiles
import com.lifestyle.nestly.data.model.ActivityEntity
import com.lifestyle.nestly.data.model.ActivityType
import com.lifestyle.nestly.data.model.ProfileEntity
import com.lifestyle.nestly.data.model.Sex
import com.lifestyle.nestly.data.units.Units
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.PillButton
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.components.SegmentedToggle
import com.lifestyle.nestly.ui.entry.EntryKind
import com.lifestyle.nestly.ui.entry.EntryRequest
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import com.lifestyle.nestly.ui.units.currentUnitPrefs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private val CurveColor = Color(0xFFD9B45B)

/** A single measurement plotted / listed for the selected metric. */
private data class GrowthPoint(
    val entry: ActivityEntity,
    val date: LocalDate,
    /** Age in fractional months at the measurement, null without a birth date. */
    val ageMonths: Double?,
    /** Raw stored value: kg for weight, cm for height/head. */
    val value: Double,
    val percentile: Double?,
)

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun GrowthScreen(
    initialMetric: GrowthMetric,
    onBack: () -> Unit,
    onOpenEntry: (EntryRequest) -> Unit,
) {
    val container = LocalContext.current.appContainer
    val profile by container.family.selectedProfile.collectAsState(initial = null)
    val entries by remember(container) {
        container.family.selectedProfile.flatMapLatest { p ->
            if (p == null) flowOf(emptyList())
            else container.activities.allOf(p.familyId, p.id, listOf(ActivityType.GROWTH))
        }
    }.collectAsState(initial = emptyList())

    var metric by rememberSaveable(initialMetric) { mutableStateOf(initialMetric) }
    var showChart by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(NestlyColors.Background)
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(NestlyColors.Surface)
                .heightIn(min = 64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = NestlyColors.TextPrimary)
            }
            Text(
                "Growth",
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                color = NestlyColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onOpenEntry(EntryRequest.New(EntryKind.GROWTH)) }) {
                Icon(Icons.Outlined.Add, "Log growth", tint = NestlyColors.TextPrimary)
            }
            IconButton(onClick = { showChart = !showChart }) {
                Icon(
                    if (showChart) Icons.AutoMirrored.Outlined.List else Icons.AutoMirrored.Outlined.ShowChart,
                    if (showChart) "Show list" else "Show chart",
                    tint = NestlyColors.TextPrimary,
                )
            }
        }

        val currentProfile = profile
        if (currentProfile == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading…", style = NestlyType.Small, color = NestlyColors.TextSecondary)
            }
            return@Column
        }

        Box(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            SegmentedToggle(
                options = GrowthMetric.entries.map { it.label },
                selected = GrowthMetric.entries.indexOf(metric),
                onSelect = { metric = GrowthMetric.entries[it] },
            )
        }

        val points = remember(entries, metric, currentProfile) {
            buildPoints(entries, metric, currentProfile)
        }
        if (points.isEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "No ${metric.label.lowercase()} measurements yet.",
                    style = NestlyType.Body,
                    color = NestlyColors.TextSecondary,
                )
                Spacer(Modifier.height(20.dp))
                PillButton("Log growth", onClick = { onOpenEntry(EntryRequest.New(EntryKind.GROWTH)) }, filled = true)
            }
            return@Column
        }

        if (showChart) {
            GrowthChart(points, metric, currentProfile, onOpenEntry, Modifier.weight(1f))
        } else {
            GrowthList(points, metric, currentProfile, onOpenEntry, Modifier.weight(1f))
        }
    }
}

private fun buildPoints(
    entries: List<ActivityEntity>,
    metric: GrowthMetric,
    profile: ProfileEntity,
): List<GrowthPoint> {
    val birth = profile.adjustedBirthDate ?: profile.birthDate
    val zone = ZoneId.systemDefault()
    return entries.mapNotNull { entry ->
        val value = metric.valueOf(entry.growth) ?: return@mapNotNull null
        val date = Instant.ofEpochMilli(entry.startTime).atZone(zone).toLocalDate()
        val age = birth?.let { GrowthPercentiles.ageMonths(it, date) }
        GrowthPoint(
            entry = entry,
            date = date,
            ageMonths = age,
            value = value,
            percentile = age?.let { GrowthPercentiles.percentile(metric, profile.sex, it, value) },
        )
    }.sortedByDescending { it.entry.startTime }
}

@Composable
private fun percentileText(point: GrowthPoint, profile: ProfileEntity): String? {
    val percentile = point.percentile ?: return null
    val label = when (profile.sex) {
        Sex.MALE -> "Boys"
        Sex.FEMALE -> "Girls"
        else -> return null
    }
    return "$label Percentile ${percentile.roundToInt()}%"
}

@Composable
private fun valueText(point: GrowthPoint, metric: GrowthMetric): String {
    val prefs = currentUnitPrefs()
    return when (metric) {
        GrowthMetric.WEIGHT -> Format.weight(point.value, prefs)
        GrowthMetric.HEIGHT, GrowthMetric.HEAD -> Format.length(point.value, prefs)
    }
}

/** Screenshot 1: measurements newest-first, grouped under date headers. */
@Composable
private fun GrowthList(
    points: List<GrowthPoint>,
    metric: GrowthMetric,
    profile: ProfileEntity,
    onOpenEntry: (EntryRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    val birth = profile.adjustedBirthDate ?: profile.birthDate
    val grouped = remember(points) { points.groupBy { it.date } }
    LazyColumn(modifier) {
        grouped.forEach { (date, dayPoints) ->
            item(key = "date-$date") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(NestlyColors.Surface)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    Text(Format.date(date), style = NestlyType.BodyMedium, color = NestlyColors.TextPrimary)
                }
            }
            items(dayPoints, key = { it.entry.id }) { point ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenEntry(EntryRequest.Edit(point.entry.id)) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text("${metric.label} ", style = NestlyType.Body)
                            Text(valueText(point, metric), style = NestlyType.BodyMedium)
                        }
                        percentileText(point, profile)?.let {
                            Text(it, style = NestlyType.Body, color = NestlyColors.TextPrimary)
                        }
                        if (birth != null) {
                            Text(
                                "Age ${GrowthPercentiles.ageLabel(birth, point.date)}",
                                style = NestlyType.Body,
                                color = NestlyColors.TextPrimary,
                            )
                        }
                        point.entry.note?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = NestlyType.Body, color = NestlyColors.TextSecondary)
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        null,
                        tint = NestlyColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                RowDivider()
            }
        }
    }
}

/** Screenshot 2: WHO percentile curves with the child's measurements on top. */
@Composable
private fun GrowthChart(
    points: List<GrowthPoint>,
    metric: GrowthMetric,
    profile: ProfileEntity,
    onOpenEntry: (EntryRequest) -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefs = currentUnitPrefs()
    val birth = profile.adjustedBirthDate ?: profile.birthDate
    if (birth == null) {
        Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                "Add ${profile.name}'s birth date to see the growth chart.",
                style = NestlyType.Body,
                color = NestlyColors.TextSecondary,
            )
        }
        return
    }

    val plotted = remember(points) {
        points.filter { it.ageMonths != null && it.ageMonths >= 0.0 }
            .sortedBy { it.ageMonths }
    }
    if (plotted.isEmpty()) {
        Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No measurements to chart yet.", style = NestlyType.Body, color = NestlyColors.TextSecondary)
        }
        return
    }

    var selectedId by rememberSaveable(metric) { mutableStateOf<String?>(null) }
    var selectionDismissed by rememberSaveable(metric) { mutableStateOf(false) }
    val selected = plotted.find { it.entry.id == selectedId } ?: plotted.lastOrNull()
    val shownSelected = if (selectionDismissed) null else selected

    val toDisplay: (Double) -> Double = { raw ->
        when (metric) {
            GrowthMetric.WEIGHT -> Units.kgToDisplay(raw, prefs.weight)
            GrowthMetric.HEIGHT, GrowthMetric.HEAD -> Units.cmToDisplay(raw, prefs.length)
        }
    }
    val unitLabel = when (metric) {
        GrowthMetric.WEIGHT -> Units.weightLabel(prefs.weight)
        GrowthMetric.HEIGHT, GrowthMetric.HEAD -> Units.lengthLabel(prefs.length)
    }

    val maxAge = plotted.maxOf { it.ageMonths!! }
    val maxX = ceil(maxAge + 1.0).coerceAtLeast(6.0).coerceAtMost(GrowthPercentiles.MAX_AGE_MONTHS)
    val curveValues = remember(metric, profile.sex, maxX) {
        (0..60).mapNotNull { i ->
            val age = maxX * i / 60.0
            GrowthPercentiles.CHART_PERCENTILES.mapNotNull { p ->
                GrowthPercentiles.valueAtPercentile(metric, profile.sex, age, p)
            }.takeIf { it.size == GrowthPercentiles.CHART_PERCENTILES.size }?.let { age to it }
        }
    }
    val allDisplay = curveValues.flatMap { (_, values) -> values.map(toDisplay) } +
        plotted.map { toDisplay(it.value) }
    val rawMin = allDisplay.minOrNull() ?: 0.0
    val rawMax = allDisplay.maxOrNull() ?: 1.0
    val pad = ((rawMax - rawMin) * 0.12).takeIf { it > 0 } ?: 1.0
    val yMin = floor((rawMin - pad).coerceAtLeast(0.0))
    val yMax = ceil(rawMax + pad)
    val yTicks = (0..4).map { yMin + (yMax - yMin) * it / 4.0 }
    val xStep = when {
        maxX <= 12 -> 1.0
        maxX <= 24 -> 2.0
        maxX <= 36 -> 3.0
        else -> 6.0
    }

    Column(modifier.padding(horizontal = 12.dp)) {
        val textMeasurer = rememberTextMeasurer()
        val axisStyle = TextStyle(color = NestlyColors.TextSecondary, fontSize = 11.sp)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(430.dp)
                .pointerInput(plotted, maxX, yMin, yMax) {
                    detectTapGestures { tap ->
                        val left = 40.dp.toPx()
                        val right = size.width - 40.dp.toPx()
                        val top = 10.dp.toPx()
                        val bottom = size.height - 28.dp.toPx()
                        val nearest = plotted.minByOrNull { point ->
                            val x = left + ((point.ageMonths!! / maxX) * (right - left)).toFloat()
                            val y = bottom - (((toDisplay(point.value) - yMin) / (yMax - yMin)) * (bottom - top)).toFloat()
                            val dx = tap.x - x
                            val dy = tap.y - y
                            dx * dx + dy * dy
                        }
                        if (nearest != null) {
                            selectedId = nearest.entry.id
                            selectionDismissed = false
                        }
                    }
                },
        ) {
            val left = 40.dp.toPx()
            val right = size.width - 40.dp.toPx()
            val top = 10.dp.toPx()
            val bottom = size.height - 28.dp.toPx()
            fun x(age: Double) = left + ((age / maxX) * (right - left)).toFloat()
            fun y(displayValue: Double) =
                bottom - (((displayValue - yMin) / (yMax - yMin)) * (bottom - top)).toFloat()

            // Gridlines + y axis labels.
            yTicks.forEach { tick ->
                drawLine(NestlyColors.Divider.copy(alpha = 0.5f), Offset(left, y(tick)), Offset(right, y(tick)), 1f)
                val label = Format.number(tick, if (tick % 1.0 == 0.0) 0 else 1)
                val measured = textMeasurer.measure(label, axisStyle)
                drawText(
                    textMeasurer, label,
                    topLeft = Offset(left - measured.size.width - 6.dp.toPx(), y(tick) - measured.size.height / 2f),
                    style = axisStyle,
                )
            }
            // X axis labels.
            var ageTick = 0.0
            while (ageTick <= maxX) {
                val label = ageTick.toInt().toString()
                val measured = textMeasurer.measure(label, axisStyle)
                drawText(
                    textMeasurer, label,
                    topLeft = Offset(x(ageTick) - measured.size.width / 2f, bottom + 6.dp.toPx()),
                    style = axisStyle,
                )
                ageTick += xStep
            }
            val unitMeasured = textMeasurer.measure(unitLabel, axisStyle)
            drawText(
                textMeasurer, unitLabel,
                topLeft = Offset(left - unitMeasured.size.width - 6.dp.toPx(), top),
                style = axisStyle,
            )

            // WHO percentile curves.
            if (curveValues.isNotEmpty()) {
                GrowthPercentiles.CHART_PERCENTILES.forEachIndexed { index, percentile ->
                    val path = Path()
                    curveValues.forEachIndexed { i, (age, values) ->
                        val px = x(age)
                        val py = y(toDisplay(values[index]))
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    drawPath(
                        path, CurveColor,
                        style = Stroke(width = if (percentile == 50.0) 2.5.dp.toPx() else 1.2.dp.toPx()),
                    )
                    val lastValue = curveValues.last().second[index]
                    val label = "${percentile.toInt()}%"
                    val measured = textMeasurer.measure(label, axisStyle.copy(color = NestlyColors.TextPrimary))
                    drawText(
                        textMeasurer, label,
                        topLeft = Offset(right + 5.dp.toPx(), y(toDisplay(lastValue)) - measured.size.height / 2f),
                        style = axisStyle.copy(color = NestlyColors.TextPrimary),
                    )
                }
            }

            // The child's own measurements, joined up.
            val line = Path()
            plotted.forEachIndexed { i, point ->
                val px = x(point.ageMonths!!)
                val py = y(toDisplay(point.value))
                if (i == 0) line.moveTo(px, py) else line.lineTo(px, py)
            }
            if (plotted.size > 1) drawPath(line, NestlyColors.Accent, style = Stroke(width = 2.dp.toPx()))
            plotted.forEach { point ->
                val center = Offset(x(point.ageMonths!!), y(toDisplay(point.value)))
                if (point.entry.id == shownSelected?.entry?.id) {
                    drawCircle(NestlyColors.Surface, radius = 9.dp.toPx(), center = center)
                    drawCircle(NestlyColors.AccentText, radius = 9.dp.toPx(), center = center, style = Stroke(width = 2.5.dp.toPx()))
                } else {
                    drawCircle(NestlyColors.Accent, radius = 5.dp.toPx(), center = center)
                }
            }
        }
        Text(
            "months",
            style = NestlyType.Small,
            color = NestlyColors.TextSecondary,
            modifier = Modifier.align(Alignment.End).padding(end = 40.dp),
        )

        shownSelected?.let { point ->
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NestlyColors.Surface)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Format.date(point.date), style = NestlyType.BodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        "Edit",
                        style = NestlyType.BodyMedium,
                        color = NestlyColors.AccentText,
                        modifier = Modifier
                            .clickable { onOpenEntry(EntryRequest.Edit(point.entry.id)) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                    IconButton(onClick = { selectionDismissed = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Close, "Dismiss", tint = NestlyColors.TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row {
                    Text("${metric.label} ", style = NestlyType.Body)
                    Text(valueText(point, metric), style = NestlyType.BodyMedium)
                }
                percentileText(point, profile)?.let {
                    Text(it, style = NestlyType.Body)
                }
                Text("Age ${GrowthPercentiles.ageLabel(birth, point.date)}", style = NestlyType.Body)
                if (point.percentile != null) {
                    Spacer(Modifier.height(6.dp))
                    Text("Source: WHO", style = NestlyType.Small, color = NestlyColors.TextSecondary)
                }
            }
        }
    }
}
