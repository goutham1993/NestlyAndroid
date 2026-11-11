package com.lifestyle.nestly.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.catalog.MilestoneCatalog
import com.lifestyle.nestly.data.growth.GrowthMetric
import com.lifestyle.nestly.data.model.ActivityEntity
import com.lifestyle.nestly.data.model.ActivityType
import com.lifestyle.nestly.data.model.ProfileEntity
import com.lifestyle.nestly.data.prefs.ActivityLayout
import com.lifestyle.nestly.data.prefs.ActivityToggle
import com.lifestyle.nestly.data.prefs.CardKind
import com.lifestyle.nestly.data.prefs.FilterOption
import com.lifestyle.nestly.data.reminder.DailyDose
import com.lifestyle.nestly.data.reminder.NextFeedState
import com.lifestyle.nestly.data.reminder.ReminderLogic
import com.lifestyle.nestly.data.timer.TimerKey
import com.lifestyle.nestly.data.timer.TimerState
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.IconKind
import com.lifestyle.nestly.ui.components.IconTile
import com.lifestyle.nestly.ui.components.MonogramAvatar
import com.lifestyle.nestly.ui.entry.EntryKind
import com.lifestyle.nestly.ui.entry.EntryRequest
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import com.lifestyle.nestly.ui.theme.headerColor
import com.lifestyle.nestly.ui.units.currentUnitPrefs
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

private val CardRadius = RoundedCornerShape(20.dp)
private val Glanceable = setOf(
    CardKind.FEED, CardKind.PUMP, CardKind.DIAPER, CardKind.SLEEP, CardKind.ROUTINE, CardKind.BABY_FIRSTS,
)

@Composable
fun ActivityHome(
    profiles: List<ProfileEntity>,
    dashboard: DashboardData?,
    now: Long,
    layout: ActivityLayout,
    timers: Map<TimerKey, TimerState>,
    feedIntervalMinutes: Int,
    filters: Set<FilterOption>,
    startOfToday: Long,
    onOpenEntry: (EntryRequest) -> Unit,
    onShowHistory: () -> Unit,
    onMilestones: () -> Unit,
    onGrowth: (GrowthMetric) -> Unit,
    onVaccinations: () -> Unit,
    onReminders: () -> Unit,
    onAppointments: () -> Unit,
    onNotes: () -> Unit,
    onEditActivities: () -> Unit,
    onSelectProfile: (String) -> Unit,
    onShowSummary: () -> Unit,
) {
    val profile = dashboard?.profile ?: profiles.firstOrNull()
    val cards = layout.order.filter(layout::isCardVisible)
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        if (profile != null) {
            HomeHeader(
                profile = profile,
                profiles = profiles,
                now = now,
                onSelectProfile = onSelectProfile,
                onShowSummary = onShowSummary,
                onReminders = onReminders,
                onAppointments = onAppointments,
                onNotes = onNotes,
                onEditActivities = onEditActivities,
            )
        }
        val timerActive = RunningTimer.entries.any { group ->
            group.keys.any { key -> timers[key]?.isIdle == false }
        }
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (timerActive) item { TimerBanners(timers, now, onOpenEntry) }
            item { QuickLogRow(cards, layout, onOpenEntry) }
            if (dashboard == null) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NestlyColors.Accent)
                    }
                }
            } else {
                item { AtAGlance(cards, dashboard, timers, now, feedIntervalMinutes, layout, onOpenEntry) }
                item { TodayCard(cards, dashboard, now, startOfToday, filters, onOpenEntry, onShowHistory) }
                if (cards.contains(CardKind.GROWTH)) {
                    item { GrowthSection(dashboard, onGrowth, onMilestones, onOpenEntry) }
                }
                if (cards.contains(CardKind.HEALTH)) {
                    item { HealthSection(dashboard, now, startOfToday, onOpenEntry, onVaccinations) }
                }
            }
            item {
                TextButton(onClick = onEditActivities, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Icon(Icons.Outlined.Tune, null, tint = NestlyColors.Accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Activities", color = NestlyColors.Accent, style = NestlyType.BodyMedium)
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(
    profile: ProfileEntity,
    profiles: List<ProfileEntity>,
    now: Long,
    onSelectProfile: (String) -> Unit,
    onShowSummary: () -> Unit,
    onReminders: () -> Unit,
    onAppointments: () -> Unit,
    onNotes: () -> Unit,
    onEditActivities: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var switcher by remember { mutableStateOf(false) }
    val age = profile.birthDate?.let { birth ->
        Format.age(birth, Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate())
    }
    val subtitle = listOfNotNull(age, Format.headerDate(now)).joinToString(" · ")
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonogramAvatar(profile.name, size = 56.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            if (profiles.size > 1) {
                Box {
                    Row(
                        Modifier.clickable { switcher = true },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            profile.name,
                            style = NestlyType.Display,
                            color = NestlyColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Icon(Icons.Outlined.KeyboardArrowDown, "Switch child", tint = NestlyColors.TextSecondary)
                    }
                    DropdownMenu(switcher, { switcher = false }, containerColor = NestlyColors.Surface) {
                        profiles.forEach { child ->
                            DropdownMenuItem(
                                text = { Text(child.name, style = NestlyType.RowLabel) },
                                onClick = {
                                    switcher = false
                                    onSelectProfile(child.id)
                                },
                            )
                        }
                    }
                }
            } else {
                Text(profile.name, style = NestlyType.Display, color = NestlyColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(subtitle, style = NestlyType.Small, color = NestlyColors.TextSecondary, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
            ActionDisc(Icons.AutoMirrored.Outlined.Subject, "Summary", onShowSummary)
        Spacer(Modifier.width(8.dp))
        Box {
            ActionDisc(Icons.Outlined.MoreHoriz, "More") { menu = true }
            DropdownMenu(menu, { menu = false }, containerColor = NestlyColors.Surface) {
                MenuItem("Set Reminders", Icons.Outlined.Alarm) { menu = false; onReminders() }
                MenuItem("Doctor Appointments", Icons.Outlined.CalendarMonth) { menu = false; onAppointments() }
                MenuItem("Notes", Icons.Outlined.EditNote) { menu = false; onNotes() }
                MenuItem("Edit Activities", Icons.Outlined.Edit) { menu = false; onEditActivities() }
            }
        }
    }
}

@Composable
private fun MenuItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, style = NestlyType.RowLabel) },
        leadingIcon = { Icon(icon, null, tint = NestlyColors.TextPrimary) },
        onClick = onClick,
    )
}

@Composable
private fun ActionDisc(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(NestlyColors.Accent.copy(alpha = 0.15f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = NestlyColors.Accent, modifier = Modifier.size(22.dp))
    }
}

private enum class RunningTimer(val keys: List<TimerKey>, val kind: EntryKind, val title: String, val icon: IconKind) {
    BREAST(listOf(TimerKey.BREAST_LEFT, TimerKey.BREAST_RIGHT), EntryKind.BREASTFEED, "Breastfeeding", IconKind.BREAST),
    PUMP(listOf(TimerKey.PUMP), EntryKind.PUMP, "Pumping", IconKind.PUMP),
    SLEEP(listOf(TimerKey.SLEEP), EntryKind.SLEEP, "Sleeping", IconKind.SLEEP),
}

@Composable
private fun TimerBanners(
    timers: Map<TimerKey, TimerState>,
    now: Long,
    onOpenEntry: (EntryRequest) -> Unit,
) {
    val active = RunningTimer.entries.mapNotNull { group ->
        val states = group.keys.mapNotNull { timers[it] }.filter { !it.isIdle }
        if (states.isEmpty()) null else group to states
    }
    val scope = rememberCoroutineScope()
    val timersStore = LocalContext.current.appContainer.timers
    if (active.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        active.forEach { (group, states) ->
            val running = states.any { it.isRunning }
            val elapsed = states.sumOf { it.elapsedMs(now) } / 1000
            val side = if (group == RunningTimer.BREAST) {
                val left = timers[TimerKey.BREAST_LEFT]?.isRunning == true
                val right = timers[TimerKey.BREAST_RIGHT]?.isRunning == true
                when {
                    left && !right -> " · Left"
                    right && !left -> " · Right"
                    else -> ""
                }
            } else ""
            val tint = group.kind.card.headerColor
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(CardRadius)
                    .background(tint.copy(alpha = 0.10f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.weight(1f).clickable { onOpenEntry(EntryRequest.New(group.kind)) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconTile(group.icon, tint, size = 44.dp, filled = true)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            (if (running) group.title else "${group.title} · Paused") + side,
                            style = NestlyType.BodyMedium,
                            color = NestlyColors.TextPrimary,
                        )
                        Text(Format.duration(elapsed), style = NestlyType.CardTitle, color = NestlyColors.TextPrimary)
                        if (group == RunningTimer.SLEEP) {
                            states.mapNotNull { it.sessionStart }.minOrNull()?.let { start ->
                                Text("Since ${Format.time(start)}", style = NestlyType.Small, color = NestlyColors.TextSecondary)
                            }
                        }
                    }
                }
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(tint.copy(alpha = 0.16f))
                        .clickable {
                            scope.launch {
                                val at = System.currentTimeMillis()
                                group.keys.forEach { key ->
                                    val state = timers[key] ?: return@forEach
                                    if (state.isIdle) return@forEach
                                    timersStore.update(key) { current ->
                                        if (running) {
                                            if (current.isRunning) current.pause(at) else current
                                        } else {
                                            current.start(at)
                                        }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (running) "Pause" else "Resume", tint = tint)
                }
            }
        }
    }
}

@Composable
private fun QuickLogRow(cards: List<CardKind>, layout: ActivityLayout, onOpenEntry: (EntryRequest) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Log", style = NestlyType.SectionTitle, color = NestlyColors.TextPrimary)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            cards.filter { it != CardKind.HEALTH }.forEach { card ->
                QuickLogTile(card, layout, onOpenEntry)
            }
        }
    }
}

@Composable
private fun QuickLogTile(card: CardKind, layout: ActivityLayout, onOpenEntry: (EntryRequest) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val options = when (card) {
        CardKind.FEED -> feedOptions(layout)
        CardKind.GROWTH -> listOf(EntryKind.GROWTH, EntryKind.MILESTONE)
        else -> emptyList()
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            Column(
                Modifier
                    .width(76.dp)
                    .clip(CardRadius)
                    .background(NestlyColors.Surface)
                    .clickable {
                        if (options.isEmpty()) onOpenEntry(EntryRequest.New(defaultKind(card, layout)))
                        else menu = true
                    }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconTile(card.iconKind, card.headerColor, size = 56.dp)
                Text(
                    card.title,
                    style = NestlyType.Small.copy(fontWeight = FontWeight.SemiBold),
                    color = NestlyColors.TextPrimary,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
            DropdownMenu(menu, { menu = false }, containerColor = NestlyColors.Surface) {
                options.forEach { kind ->
                    DropdownMenuItem(
                        text = { Text(kind.title, style = NestlyType.RowLabel) },
                        onClick = {
                            menu = false
                            onOpenEntry(EntryRequest.New(kind))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AtAGlance(
    cards: List<CardKind>,
    data: DashboardData,
    timers: Map<TimerKey, TimerState>,
    now: Long,
    feedIntervalMinutes: Int,
    layout: ActivityLayout,
    onOpenEntry: (EntryRequest) -> Unit,
) {
    val shown = cards.filter { it in Glanceable }
    if (shown.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("At a glance", style = NestlyType.SectionTitle, color = NestlyColors.TextPrimary)
        shown.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { card ->
                    StatusCard(
                        card = card,
                        status = statusFor(card, data, timers, now, feedIntervalMinutes),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val latest = data.latest[card]
                            if (latest != null) onOpenEntry(EntryRequest.Edit(latest.id))
                            else onOpenEntry(EntryRequest.New(defaultKind(card, layout)))
                        },
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private sealed interface GlanceStatus {
    data class Value(
        val primary: String,
        val detail: String? = null,
        val footnote: String? = null,
        val footnoteWarning: Boolean = false,
        val live: Boolean = false,
    ) : GlanceStatus
    data class Empty(val text: String) : GlanceStatus
}

@Composable
private fun statusFor(
    card: CardKind,
    data: DashboardData,
    timers: Map<TimerKey, TimerState>,
    now: Long,
    feedIntervalMinutes: Int,
): GlanceStatus {
    val latest = data.latest[card]
    val units = currentUnitPrefs()
    return when (card) {
        CardKind.FEED -> {
            if (latest == null) GlanceStatus.Empty("Track a feeding")
            else {
                val next = ReminderLogic.nextFeedState(latest.startTime, feedIntervalMinutes, now)
                val footnote = Format.nextFeeding(next)?.replace("Next feeding", "Next")
                val warning = next is NextFeedState.DueNow || next is NextFeedState.Overdue
                GlanceStatus.Value(Format.ago(latest.startTime, now), feedDetail(latest, units), footnote, warning)
            }
        }
        CardKind.PUMP -> {
            if (latest == null) GlanceStatus.Empty("Track a pumping session")
            else if (latest.endTime == null && (latest.pump?.durationSec ?: 0L) == 0L) {
                GlanceStatus.Value(Format.ago(latest.startTime, now), "Pumping since ${Format.time(latest.startTime)}")
            } else {
                GlanceStatus.Value(Format.ago(latest.startTime, now), latest.pump?.totalMl?.let { Format.volume(it, units) })
            }
        }
        CardKind.DIAPER -> {
            if (latest == null) GlanceStatus.Empty("Track a diaper change")
            else GlanceStatus.Value(Format.ago(latest.startTime, now), latest.label)
        }
        CardKind.SLEEP -> {
            val sleep = timers[TimerKey.SLEEP]
            if (sleep != null && !sleep.isIdle) {
                GlanceStatus.Value(
                    Format.duration(sleep.elapsedMs(now) / 1000),
                    "Sleeping since ${Format.time(sleep.sessionStart ?: now)}",
                    live = true,
                )
            } else if (latest == null) {
                GlanceStatus.Empty("Track a sleep session")
            } else if (latest.endTime == null) {
                GlanceStatus.Value(Format.ago(latest.startTime, now), "Sleeping since ${Format.time(latest.startTime)}")
            } else {
                GlanceStatus.Value(
                    Format.ago(latest.endTime ?: latest.startTime, now),
                    latest.durationSec?.let { "Slept ${Format.duration(it)}" },
                )
            }
        }
        CardKind.ROUTINE -> {
            if (latest == null) GlanceStatus.Empty("Track a routine or create your own")
            else GlanceStatus.Value(latest.label, Format.ago(latest.startTime, now))
        }
        CardKind.BABY_FIRSTS -> {
            if (latest == null) GlanceStatus.Empty("Track memorable moments")
            else GlanceStatus.Value(latest.label, Format.relativeDay(latest.startTime, now))
        }
        else -> GlanceStatus.Empty("")
    }
}

@Composable
private fun StatusCard(card: CardKind, status: GlanceStatus, modifier: Modifier, onClick: () -> Unit) {
    val tint = card.headerColor
    Column(
        modifier
            .height(148.dp)
            .clip(CardRadius)
            .background(NestlyColors.Surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(card.iconKind, tint, size = 28.dp)
            Spacer(Modifier.width(8.dp))
            Text(card.title, style = NestlyType.BodyMedium, color = tint, modifier = Modifier.weight(1f))
            if (status is GlanceStatus.Value && status.live) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(tint))
            }
        }
        Spacer(Modifier.weight(1f))
        when (status) {
            is GlanceStatus.Value -> {
                Text(status.primary, style = NestlyType.CardTitle, color = NestlyColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!status.detail.isNullOrBlank()) {
                    Text(status.detail, style = NestlyType.Small, color = NestlyColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (!status.footnote.isNullOrBlank()) {
                    Text(
                        status.footnote,
                        style = NestlyType.Small.copy(fontWeight = FontWeight.Medium),
                        color = if (status.footnoteWarning) NestlyColors.Warning else NestlyColors.TextSecondary,
                        maxLines = 2,
                    )
                }
            }
            is GlanceStatus.Empty -> {
                Text(status.text, style = NestlyType.Small, color = NestlyColors.TextSecondary, maxLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Add, null, tint = tint, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add", style = NestlyType.Small.copy(fontWeight = FontWeight.SemiBold), color = tint)
                }
            }
        }
    }
}

@Composable
private fun TodayCard(
    cards: List<CardKind>,
    data: DashboardData,
    now: Long,
    startOfToday: Long,
    filters: Set<FilterOption>,
    onOpenEntry: (EntryRequest) -> Unit,
    onShowHistory: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val listed = cards.filter { it != CardKind.HEALTH && it != CardKind.GROWTH }.flatMap { it.types }.toSet()
    val rows = ActivityFilters.filter(
        data.recent.filter { it.startTime >= startOfToday && it.type in listed },
        filters,
    ).sortedByDescending { it.startTime }
    val shown = if (expanded) rows else rows.take(4)
    val maxBar = rows.mapNotNull { it.barValue }.maxOrNull() ?: 0.0
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Today", style = NestlyType.SectionTitle, color = NestlyColors.TextPrimary, modifier = Modifier.weight(1f))
            if (rows.isNotEmpty()) {
                Text(if (rows.size == 1) "1 entry" else "${rows.size} entries", style = NestlyType.Small, color = NestlyColors.TextSecondary)
            }
        }
        Column(
            Modifier.fillMaxWidth().clip(CardRadius).background(NestlyColors.Surface).padding(12.dp),
        ) {
            if (rows.isEmpty()) {
                Text(
                    "Nothing logged yet today. Tap a tile above to start.",
                    style = NestlyType.Small,
                    color = NestlyColors.TextSecondary,
                )
            } else {
                shown.forEachIndexed { index, entry ->
                    if (index > 0) Box(Modifier.padding(start = 46.dp).fillMaxWidth().height(1.dp).background(NestlyColors.Divider))
                    TodayRow(entry, maxBar) { onOpenEntry(EntryRequest.Edit(entry.id)) }
                }
                Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(1.dp).background(NestlyColors.Divider))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (rows.size > 4) {
                        TextButton(onClick = { expanded = !expanded }) {
                            Icon(
                                if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                null,
                                tint = NestlyColors.Accent,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (expanded) "Show Less" else "Show All", color = NestlyColors.Accent, style = NestlyType.BodyMedium)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onShowHistory) {
                        Text("History", color = NestlyColors.Accent, style = NestlyType.BodyMedium)
                        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = NestlyColors.Accent, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayRow(entry: ActivityEntity, maxBar: Double, onClick: () -> Unit) {
    val detail = entry.detailText(currentUnitPrefs())
    val bar = entry.barValue
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(entry.iconKind, entry.type.card.headerColor, size = 34.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                listOf(Format.time(entry.startTime), entry.label).filter { it.isNotBlank() }.joinToString("  "),
                style = NestlyType.Body,
                color = NestlyColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (bar != null || !detail.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (bar != null && maxBar > 0) {
                        val fraction = (bar / maxBar).toFloat().coerceIn(0.08f, 1f)
                        Box(
                            Modifier
                                .width((96 * fraction).dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(entry.type.card.headerColor),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    if (!detail.isNullOrBlank()) {
                        Text(detail, style = NestlyType.Small, color = NestlyColors.TextSecondary, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun GrowthSection(
    data: DashboardData,
    onGrowth: (GrowthMetric) -> Unit,
    onMilestones: () -> Unit,
    onOpenEntry: (EntryRequest) -> Unit,
) {
    val units = currentUnitPrefs()
    val growth = data.growth.filter { it.type == ActivityType.GROWTH }.sortedByDescending { it.startTime }
    val logged = data.milestones.mapNotNull { it.milestone?.milestoneKey ?: it.milestone?.title }.toSet().size
    val total = MilestoneCatalog.groups.sumOf { it.items.size }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Growth", style = NestlyType.SectionTitle, color = NestlyColors.TextPrimary)
        Column(Modifier.fillMaxWidth().clip(CardRadius).background(NestlyColors.Surface).padding(horizontal = 12.dp, vertical = 4.dp)) {
            GrowthMetric.entries.forEachIndexed { index, metric ->
                if (index > 0) InsetDivider()
                val raw = growth.firstNotNullOfOrNull { metric.valueOf(it.growth) }
                val value = raw?.let {
                    when (metric) {
                        GrowthMetric.WEIGHT -> Format.weight(it, units)
                        GrowthMetric.HEIGHT, GrowthMetric.HEAD -> Format.length(it, units)
                    }
                } ?: "—"
                MetricLine(metric.iconKind, metric.label, value, CardKind.GROWTH.headerColor) { onGrowth(metric) }
            }
            InsetDivider()
            MetricLine(IconKind.MILESTONE, "Milestones", "$logged of $total", CardKind.GROWTH.headerColor, onClick = onMilestones)
        }
    }
}

@Composable
private fun HealthSection(
    data: DashboardData,
    now: Long,
    startOfToday: Long,
    onOpenEntry: (EntryRequest) -> Unit,
    onVaccinations: () -> Unit,
) {
    val units = currentUnitPrefs()
    val zone = ZoneId.systemDefault()
    val startOfTomorrow = Instant.ofEpochMilli(startOfToday).atZone(zone).toLocalDate().plusDays(1)
        .atStartOfDay(zone).toInstant().toEpochMilli()
    val given = DailyDose.givenAt(data.recent, startOfToday, startOfTomorrow)
    val givenEntry = given?.let { at -> data.recent.firstOrNull { it.type == ActivityType.VITAMIN_D && it.startTime == at } }
    val tint = CardKind.HEALTH.headerColor
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Health", style = NestlyType.SectionTitle, color = NestlyColors.TextPrimary)
        Column(Modifier.fillMaxWidth().clip(CardRadius).background(NestlyColors.Surface).padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (givenEntry != null) onOpenEntry(EntryRequest.Edit(givenEntry.id))
                        else onOpenEntry(EntryRequest.New(EntryKind.VITAMIN_D))
                    }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile(IconKind.MEDICAL, NestlyColors.BabyFirsts, size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Vitamin D", style = NestlyType.Body, color = NestlyColors.TextPrimary)
                    Text(
                        if (given != null) "Given today at ${Format.time(given)}" else "Not given today",
                        style = NestlyType.Small,
                        color = NestlyColors.TextSecondary,
                    )
                }
                if (given != null) {
                    Icon(Icons.Outlined.CheckCircle, "Given", tint = NestlyColors.Routine, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        "Log",
                        style = NestlyType.Small.copy(fontWeight = FontWeight.SemiBold),
                        color = NestlyColors.BabyFirsts,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(NestlyColors.BabyFirsts.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            InsetDivider()
            MetricLine(
                IconKind.MEDICAL,
                "Medical",
                data.latestMedical?.let { medicalText(it, now, units) } ?: "Temperature and medication",
                tint,
                valueIsSubtitle = true,
            ) { onOpenEntry(EntryRequest.New(EntryKind.MEDICAL)) }
            InsetDivider()
            MetricLine(
                IconKind.VACCINE,
                "Vaccines",
                data.latestVaccine?.let { vaccineText(it, now) } ?: "Schedule and records",
                tint,
                valueIsSubtitle = true,
                onClick = onVaccinations,
            )
        }
    }
}

@Composable
private fun MetricLine(
    icon: IconKind,
    title: String,
    value: String,
    tint: Color,
    valueIsSubtitle: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, tint, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = NestlyType.Body, color = NestlyColors.TextPrimary)
            if (valueIsSubtitle) Text(value, style = NestlyType.Small, color = NestlyColors.TextSecondary, maxLines = 2)
        }
        if (!valueIsSubtitle) {
            Text(value, style = NestlyType.Body, color = NestlyColors.TextSecondary, maxLines = 1)
        }
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = NestlyColors.TextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun InsetDivider() {
    Box(Modifier.padding(start = 48.dp).fillMaxWidth().height(1.dp).background(NestlyColors.Divider))
}

private fun feedOptions(layout: ActivityLayout): List<EntryKind> = buildList {
    if (layout.isEnabled(ActivityToggle.BREASTFEEDING)) add(EntryKind.BREASTFEED)
    if (layout.isEnabled(ActivityToggle.BOTTLE_FEEDING)) add(EntryKind.BOTTLE)
    if (layout.isEnabled(ActivityToggle.SOLID_FEEDING)) add(EntryKind.SOLIDS)
    if (layout.isEnabled(ActivityToggle.COMBO_FEEDING)) add(EntryKind.COMBO)
    add(EntryKind.VITAMIN_D)
}

private fun defaultKind(card: CardKind, layout: ActivityLayout): EntryKind = when (card) {
    CardKind.FEED -> when {
        layout.isEnabled(ActivityToggle.BOTTLE_FEEDING) -> EntryKind.BOTTLE
        layout.isEnabled(ActivityToggle.BREASTFEEDING) -> EntryKind.BREASTFEED
        else -> EntryKind.SOLIDS
    }
    CardKind.PUMP -> EntryKind.PUMP
    CardKind.DIAPER -> EntryKind.DIAPER
    CardKind.SLEEP -> EntryKind.SLEEP
    CardKind.ROUTINE -> EntryKind.ROUTINE
    CardKind.BABY_FIRSTS -> EntryKind.BABY_FIRST
    CardKind.GROWTH -> EntryKind.GROWTH
    CardKind.HEALTH -> EntryKind.MEDICAL
}

private val CardKind.iconKind: IconKind
    get() = when (this) {
        CardKind.FEED -> IconKind.BOTTLE
        CardKind.PUMP -> IconKind.PUMP
        CardKind.DIAPER -> IconKind.DIAPER
        CardKind.SLEEP -> IconKind.SLEEP
        CardKind.ROUTINE -> IconKind.ROUTINE
        CardKind.BABY_FIRSTS -> IconKind.BABY_FIRST
        CardKind.GROWTH -> IconKind.HEIGHT
        CardKind.HEALTH -> IconKind.MEDICAL
    }

private val GrowthMetric.iconKind: IconKind
    get() = when (this) {
        GrowthMetric.WEIGHT -> IconKind.WEIGHT
        GrowthMetric.HEIGHT -> IconKind.HEIGHT
        GrowthMetric.HEAD -> IconKind.HEAD
    }

@Composable
private fun feedDetail(entry: ActivityEntity, units: com.lifestyle.nestly.data.units.UnitPrefs): String {
    val value = when (entry.type) {
        ActivityType.BOTTLE_FEED -> entry.bottle?.totalMl?.let { Format.volume(it, units) }.orEmpty()
        ActivityType.BREASTFEED -> {
            val seconds = entry.breastfeed?.let { it.leftDurationSec + it.rightDurationSec } ?: entry.durationSec
            seconds?.takeIf { it > 0 }?.let(Format::duration).orEmpty()
        }
        else -> entry.label
    }
    return listOf(entry.type.shortName, value).filter { it.isNotBlank() }.joinToString(" · ")
}

private val ActivityType.shortName: String
    get() = when (this) {
        ActivityType.BOTTLE_FEED -> "Bottle"
        ActivityType.BREASTFEED -> "Breastfeed"
        ActivityType.SOLIDS -> "Solids"
        ActivityType.PUMP -> "Pump"
        ActivityType.DIAPER -> "Diaper"
        ActivityType.SLEEP -> "Sleep"
        ActivityType.ROUTINE -> "Routine"
        ActivityType.BABY_FIRST -> "Baby First"
        ActivityType.GROWTH -> "Growth"
        ActivityType.MILESTONE -> "Milestone"
        ActivityType.MEDICAL -> "Medical"
        ActivityType.VACCINE -> "Vaccine"
        ActivityType.VITAMIN_D -> "Vitamin D"
    }

private fun medicalText(entry: ActivityEntity, now: Long, units: com.lifestyle.nestly.data.units.UnitPrefs): String {
    val detail = entry.medical?.temperatureC?.let { Format.temp(it, units) } ?: entry.medical?.medicationName.orEmpty()
    return listOf(Format.dayAndTime(entry.startTime, now), detail).filter { it.isNotBlank() }.joinToString(" · ")
}

private fun vaccineText(entry: ActivityEntity, now: Long): String {
    val n = entry.vaccine?.vaccines?.size ?: 0
    val count = if (n == 1) "1 vaccine" else "$n vaccines"
    return "${Format.relativeDay(entry.startTime, now)} · $count"
}
