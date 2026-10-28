package com.lifestyle.nestly.ui.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lifestyle.nestly.data.catalog.MilestoneCatalog
import com.lifestyle.nestly.data.model.ActivityEntity
import com.lifestyle.nestly.data.model.ActivityType
import com.lifestyle.nestly.data.prefs.ActivityLayout
import com.lifestyle.nestly.data.prefs.ActivityToggle
import com.lifestyle.nestly.data.prefs.CardKind
import com.lifestyle.nestly.data.timer.TimerKey
import com.lifestyle.nestly.data.timer.TimerState
import com.lifestyle.nestly.data.units.Units
import com.lifestyle.nestly.data.units.VolumeUnit
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.IconKind
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.entry.EntryKind
import com.lifestyle.nestly.ui.entry.EntryRequest
import com.lifestyle.nestly.ui.units.currentUnitPrefs
import java.time.Instant
import java.time.ZoneId

/** What a card needs to render; built once per recomposition from [DashboardData]. */
class CardContext(
    val data: DashboardData,
    val now: Long,
    val startOfToday: Long,
    val layout: ActivityLayout,
    val timers: Map<TimerKey, TimerState>,
    val onOpenEntry: (EntryRequest) -> Unit,
    val onFeedPicker: () -> Unit,
    val onShowHistory: () -> Unit,
    val onMilestones: () -> Unit,
    val onGrowth: (com.lifestyle.nestly.data.growth.GrowthMetric) -> Unit,
) {
    fun today(card: CardKind): List<ActivityEntity> =
        data.recent.filter { it.type.card == card && it.startTime >= startOfToday && it.startTime <= now + 60_000 }

    fun edit(entry: ActivityEntity) = onOpenEntry(EntryRequest.Edit(entry.id))
    fun new(kind: EntryKind) = onOpenEntry(EntryRequest.New(kind))

    fun elapsed(vararg keys: TimerKey): Long? {
        val active = keys.mapNotNull { timers[it] }.filter { !it.isIdle }
        if (active.isEmpty()) return null
        return active.sumOf { it.elapsedMs(now) } / 1000
    }
}

@Composable
fun DashboardCardFor(kind: CardKind, ctx: CardContext) {
    when (kind) {
        CardKind.FEED -> FeedCard(ctx)
        CardKind.PUMP -> PumpCard(ctx)
        CardKind.DIAPER -> DiaperCard(ctx)
        CardKind.SLEEP -> SleepCard(ctx)
        CardKind.ROUTINE -> SimpleCard(ctx, CardKind.ROUTINE, EntryKind.ROUTINE, "Track a routine or create your own")
        CardKind.BABY_FIRSTS -> SimpleCard(ctx, CardKind.BABY_FIRSTS, EntryKind.BABY_FIRST, "Track memorable moments")
        CardKind.GROWTH -> GrowthCard(ctx)
        CardKind.HEALTH -> HealthCard(ctx)
    }
}

@Composable
private fun TodayList(
    ctx: CardContext,
    card: CardKind,
    showIcons: Boolean,
    defaultExpanded: Boolean,
    footer: String = "Entries before 12:00am",
) {
    val today = ctx.today(card)
    var expanded by rememberSaveable(card) { mutableStateOf(defaultExpanded) }
    if (today.isNotEmpty()) {
        ShowMoreRow(expanded) { expanded = !expanded }
        if (expanded) {
            val max = today.mapNotNull { it.barValue }.maxOrNull() ?: 0.0
            today.forEach { entry -> EntryListRow(entry, max, showIcons, onClick = { ctx.edit(entry) }) }
        }
    }
    LinkRow(footer, onClick = ctx.onShowHistory)
}

@Composable
private fun FeedCard(ctx: CardContext) {
    val latest = ctx.data.latest[CardKind.FEED]
    DashboardCard(CardKind.FEED, onAdd = ctx.onFeedPicker) {
        if (latest == null) {
            EmptyCardRow(IconKind.BOTTLE, "Track a feeding", onClick = ctx.onFeedPicker)
        } else {
            val units = currentUnitPrefs()
            val (value, unit) = when (latest.type) {
                ActivityType.BOTTLE_FEED -> latest.bottle?.totalMl?.let {
                    Format.number(Units.mlToDisplay(it, units.volume), if (units.volume == VolumeUnit.OZ) 1 else 0)
                } to Units.volumeLabel(units.volume)
                ActivityType.BREASTFEED ->
                    latest.breastfeed?.let { b ->
                        Format.duration(b.leftDurationSec + b.rightDurationSec)
                    } to null
                else -> latest.label.lowercase() to null
            }
            LastEntryRow(
                icon = latest.iconKind,
                title = "Last feeding",
                subtitle = Format.ago(latest.startTime, ctx.now),
                value = value,
                unit = unit.takeIf { value != null },
                onClick = { ctx.edit(latest) },
            )
        }
        if (ctx.layout.isEnabled(ActivityToggle.BREASTFEEDING)) {
            ctx.elapsed(TimerKey.BREAST_LEFT, TimerKey.BREAST_RIGHT)?.let { sec ->
                InProgressRow(IconKind.BREAST, "Breastfeeding", Format.duration(sec)) { ctx.new(EntryKind.BREASTFEED) }
            }
        }
        TodayList(ctx, CardKind.FEED, showIcons = true, defaultExpanded = true)
    }
}

@Composable
private fun PumpCard(ctx: CardContext) {
    val latest = ctx.data.latest[CardKind.PUMP]
    val add = { ctx.new(EntryKind.PUMP) }
    DashboardCard(CardKind.PUMP, onAdd = add) {
        if (latest == null) {
            EmptyCardRow(IconKind.PUMP, "Track a pumping session", onClick = add)
        } else {
            val units = currentUnitPrefs()
            val total = latest.pump?.totalMl
            LastEntryRow(
                icon = IconKind.PUMP,
                title = "Last session",
                subtitle = Format.ago(latest.startTime, ctx.now),
                value = total?.let {
                    Format.number(Units.mlToDisplay(it, units.volume), if (units.volume == VolumeUnit.OZ) 1 else 0)
                },
                unit = Units.volumeLabel(units.volume),
                onClick = { ctx.edit(latest) },
            )
        }
        ctx.elapsed(TimerKey.PUMP)?.let { sec ->
            InProgressRow(IconKind.PUMP, "Pumping", Format.duration(sec), onClick = add)
        }
        TodayList(ctx, CardKind.PUMP, showIcons = false, defaultExpanded = true)
    }
}

@Composable
private fun DiaperCard(ctx: CardContext) {
    val latest = ctx.data.latest[CardKind.DIAPER]
    val add = { ctx.new(EntryKind.DIAPER) }
    DashboardCard(CardKind.DIAPER, onAdd = add) {
        if (latest == null) {
            EmptyCardRow(IconKind.DIAPER, "Track a diaper change", onClick = add)
        } else {
            LastEntryRow(
                icon = IconKind.DIAPER,
                title = "Last change",
                subtitle = Format.ago(latest.startTime, ctx.now),
                value = latest.diaper?.diaperType?.let { diaperLabel(it).lowercase().replace(" + ", "+") },
                onClick = { ctx.edit(latest) },
            )
        }
        TodayList(ctx, CardKind.DIAPER, showIcons = false, defaultExpanded = false, footer = "View History")
    }
}

@Composable
private fun SleepCard(ctx: CardContext) {
    val latest = ctx.data.latest[CardKind.SLEEP]
    val add = { ctx.new(EntryKind.SLEEP) }
    val running = ctx.elapsed(TimerKey.SLEEP)
    DashboardCard(CardKind.SLEEP, onAdd = add) {
        when {
            running != null -> LastEntryRow(
                icon = IconKind.SLEEP,
                title = "Sleeping",
                subtitle = ctx.timers[TimerKey.SLEEP]?.sessionStart?.let { "since ${Format.time(it)}" },
                value = Format.duration(running),
                onClick = add,
            )
            latest == null -> EmptyCardRow(IconKind.SLEEP, "Track a sleep session", onClick = add)
            else -> LastEntryRow(
                icon = IconKind.SLEEP,
                title = "Last sleep",
                subtitle = Format.ago(latest.endTime ?: latest.startTime, ctx.now),
                value = latest.durationSec?.let(Format::duration),
                onClick = { ctx.edit(latest) },
            )
        }
        if (latest != null) TodayList(ctx, CardKind.SLEEP, showIcons = false, defaultExpanded = false)
    }
}

@Composable
private fun SimpleCard(ctx: CardContext, card: CardKind, kind: EntryKind, emptyText: String) {
    val latest = ctx.data.latest[card]
    val add = { ctx.new(kind) }
    DashboardCard(card, onAdd = add) {
        if (latest == null) {
            EmptyCardRow(kind.icon, emptyText, onClick = add)
        } else {
            LastEntryRow(
                icon = kind.icon,
                title = latest.label,
                subtitle = if (card == CardKind.BABY_FIRSTS) Format.relativeDay(latest.startTime, ctx.now)
                else Format.ago(latest.startTime, ctx.now),
                value = null,
                onClick = { ctx.edit(latest) },
            )
            TodayList(ctx, card, showIcons = false, defaultExpanded = false)
        }
    }
}

@Composable
private fun GrowthCard(ctx: CardContext) {
    val units = currentUnitPrefs()
    val growth = ctx.data.growth
    val weight = growth.firstOrNull { it.growth?.weightKg != null }
    val height = growth.firstOrNull { it.growth?.heightCm != null }
    val head = growth.firstOrNull { it.growth?.headCm != null }
    val add = { ctx.new(EntryKind.GROWTH) }
    val zone = ZoneId.systemDefault()
    fun dateOf(e: ActivityEntity?) = e?.let { Format.date(Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate()) }

    DashboardCard(CardKind.GROWTH, onAdd = add) {
        Column {
            Spacer(Modifier.height(8.dp))
            MetricRow(IconKind.WEIGHT, "Weight", dateOf(weight), weight?.growth?.weightKg?.let { Format.weight(it, units) }) {
                ctx.onGrowth(com.lifestyle.nestly.data.growth.GrowthMetric.WEIGHT)
            }
            MetricRow(IconKind.HEIGHT, "Height", dateOf(height), height?.growth?.heightCm?.let { Format.length(it, units) }) {
                ctx.onGrowth(com.lifestyle.nestly.data.growth.GrowthMetric.HEIGHT)
            }
            MetricRow(IconKind.HEAD, "Head Size", dateOf(head), head?.growth?.headCm?.let { Format.length(it, units) }) {
                ctx.onGrowth(com.lifestyle.nestly.data.growth.GrowthMetric.HEAD)
            }
            val logged = ctx.data.milestones.size
            val latestMilestone = ctx.data.milestones.firstOrNull()
            MetricRow(
                IconKind.MILESTONE,
                "Milestones",
                latestMilestone?.let { dateOf(it) },
                if (logged > 0) "$logged of ${MilestoneCatalog.groups.sumOf { it.items.size }}" else null,
                onClick = ctx.onMilestones,
            )
        }
    }
}

@Composable
private fun HealthCard(ctx: CardContext) {
    val units = currentUnitPrefs()
    val medical = ctx.data.latestMedical
    val vaccine = ctx.data.latestVaccine
    DashboardCard(CardKind.HEALTH, onAdd = null) {
        Spacer(Modifier.height(8.dp))
        MetricRow(
            IconKind.MEDICAL,
            "Medical",
            medical?.let { Format.dayAndTime(it.startTime, ctx.now) },
            medical?.medical?.temperatureC?.let { Format.temp(it, units) }
                ?: medical?.medical?.medicationName,
        ) { ctx.new(EntryKind.MEDICAL) }
        MetricRow(
            IconKind.VACCINE,
            "Vaccine",
            vaccine?.let { Format.relativeDay(it.startTime, ctx.now) },
            vaccine?.vaccine?.vaccines?.size?.let { if (it == 1) "1 vaccine" else "$it vaccines" },
        ) { ctx.new(EntryKind.VACCINE) }
    }
}
