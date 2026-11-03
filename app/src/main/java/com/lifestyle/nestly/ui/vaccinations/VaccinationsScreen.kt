package com.lifestyle.nestly.ui.vaccinations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.catalog.DoseState
import com.lifestyle.nestly.data.catalog.DoseStatus
import com.lifestyle.nestly.data.catalog.VaccineSchedule
import com.lifestyle.nestly.data.model.ActivityEntity
import com.lifestyle.nestly.data.model.ActivityType
import com.lifestyle.nestly.data.model.ProfileEntity
import com.lifestyle.nestly.ui.components.DetailScreen
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.PillButton
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.components.SectionHeading
import com.lifestyle.nestly.ui.entry.EntryKind
import com.lifestyle.nestly.ui.entry.EntryRequest
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun VaccinationsScreen(onBack: () -> Unit, onOpenEntry: (EntryRequest) -> Unit) {
    val container = LocalContext.current.appContainer
    val profile by container.family.selectedProfile.collectAsState(initial = null)
    val entries by remember(container) {
        container.family.selectedProfile.flatMapLatest { p ->
            if (p == null) flowOf(emptyList())
            else container.activities.allOf(p.familyId, p.id, listOf(ActivityType.VACCINE))
        }
    }.collectAsState(initial = emptyList())

    val addVaccine: () -> Unit = { onOpenEntry(EntryRequest.New(EntryKind.VACCINE)) }

    DetailScreen(title = "Vaccinations", onBack = onBack, action = "Add", onAction = addVaccine) {
        val currentProfile = profile
        if (currentProfile == null) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Loading…", style = NestlyType.Body, color = NestlyColors.TextSecondary)
            }
            return@DetailScreen
        }
        VaccinationContent(currentProfile, entries, onOpenEntry, addVaccine)
    }
}

@Composable
private fun VaccinationContent(
    profile: ProfileEntity,
    entries: List<ActivityEntity>,
    onOpenEntry: (EntryRequest) -> Unit,
    onAddVaccine: () -> Unit,
) {
    val today = LocalDate.now()
    val birthDate = profile.birthDate
    val statuses = remember(entries, birthDate, today) {
        VaccineSchedule.doseStatuses(birthDate, entries, today)
    }
    val byGroup = remember(statuses) { statuses.groupBy { it.groupLabel } }
    val recommendations = remember(statuses) { VaccineSchedule.recommendations(statuses) }
    val counted = statuses.filter { it.countsTowardCompletion }
    val givenCount = counted.count { it.state == DoseState.GIVEN }

    val onDoseClick: (DoseStatus) -> Unit = { status ->
        val entryId = status.givenEntryId
        if (status.state == DoseState.GIVEN && entryId != null) {
            onOpenEntry(EntryRequest.Edit(entryId))
        } else {
            onOpenEntry(EntryRequest.New(EntryKind.VACCINE, vaccines = listOf(status.dose.vaccineName)))
        }
    }

    // Open the age group the child is currently in, like the Milestones screen.
    val ageMonths = birthDate?.let { ChronoUnit.MONTHS.between(it, today).toInt() }
    val defaultGroup = VaccineSchedule.groups.lastOrNull { group ->
        ageMonths != null && group.doses.minOf { it.dueStartMonths } <= ageMonths
    } ?: VaccineSchedule.groups.first()
    var expanded by rememberSaveable { mutableStateOf(setOf(defaultGroup.label)) }

    LazyColumn(Modifier.fillMaxWidth()) {
        if (birthDate == null) {
            item {
                Text(
                    "Add ${profile.name}'s birth date (Account → Family → tap their name) to see due dates, " +
                        "recommendations and overdue doses. Vaccines you've logged still show below.",
                    style = NestlyType.Body,
                    color = NestlyColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
                RowDivider()
            }
        } else {
            item {
                Text(
                    "$givenCount of ${counted.size} scheduled doses given",
                    style = NestlyType.Body,
                    color = NestlyColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
                RowDivider()
            }
        }

        if (entries.isEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "No vaccines logged yet",
                        style = NestlyType.SectionTitle,
                        color = NestlyColors.TextPrimary,
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        "Log the vaccines ${profile.name} has already had and Nestly will track what's due next.",
                        style = NestlyType.Body,
                        color = NestlyColors.TextSecondary,
                    )
                    Spacer(Modifier.size(20.dp))
                    PillButton("Add vaccination", onClick = onAddVaccine, filled = true)
                }
                RowDivider()
            }
        }

        if (birthDate != null && recommendations.isNotEmpty()) {
            item { SectionHeading("Up next for ${profile.name}") }
            items(recommendations, key = { "rec-${it.dose.key}" }) { status ->
                DoseRow(status, onClick = { onDoseClick(status) })
            }
        }
        if (birthDate != null && recommendations.isEmpty() && entries.isNotEmpty()) {
            item {
                Text(
                    "All scheduled doses are up to date. Nice work!",
                    style = NestlyType.Body,
                    color = NestlyColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
                RowDivider()
            }
        }

        item { SectionHeading("Vaccination chart") }
        VaccineSchedule.groups.forEach { group ->
            val groupStatuses = byGroup[group.label].orEmpty()
            val groupCounted = groupStatuses.filter { it.countsTowardCompletion }
            val groupGiven = groupCounted.count { it.state == DoseState.GIVEN }
            val isOpen = group.label in expanded
            item(key = "group-${group.label}") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(NestlyColors.Surface)
                        .clickable { expanded = if (isOpen) expanded - group.label else expanded + group.label }
                        .heightIn(min = 76.dp)
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(group.label, style = NestlyType.SectionTitle, modifier = Modifier.weight(1f))
                    Text(
                        "$groupGiven/${groupCounted.size}",
                        style = NestlyType.Small,
                        color = NestlyColors.TextSecondary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (isOpen) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = NestlyColors.TextPrimary,
                    )
                }
                RowDivider()
            }
            if (isOpen) {
                items(groupStatuses, key = { it.dose.key }) { status ->
                    DoseRow(status, onClick = { onDoseClick(status) })
                }
            }
        }

        item { SectionHeading("Also recommended") }
        items(VaccineSchedule.alsoRecommended, key = { it.title }) { info ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                Text(info.title, style = NestlyType.Body, color = NestlyColors.TextPrimary)
                Text(info.detail, style = NestlyType.Small, color = NestlyColors.TextSecondary)
            }
            RowDivider()
        }

        if (entries.isNotEmpty()) {
            item { SectionHeading("History") }
            items(entries, key = { it.id }) { entry ->
                HistoryRow(entry) { onOpenEntry(EntryRequest.Edit(entry.id)) }
            }
        }

        item {
            Text(
                "Based on the CDC (US) immunization schedule for birth–6 years. Dose numbers are worked out " +
                    "from the order vaccines were logged. If a dose was late, catch-up timing can differ — " +
                    "this isn't medical advice, so confirm what's due with your pediatrician.",
                style = NestlyType.Small,
                color = NestlyColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            )
        }
    }
}

@Composable
private fun DoseRow(status: DoseStatus, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusIcon(status.state)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${status.dose.vaccineName} — dose ${status.dose.doseNumber}",
                style = NestlyType.Body,
                color = NestlyColors.TextPrimary,
            )
            Text(
                doseSubtitle(status),
                style = NestlyType.Small,
                color = if (status.state == DoseState.OVERDUE) NestlyColors.Danger else NestlyColors.TextSecondary,
            )
            status.dose.note?.let {
                Text(it, style = NestlyType.Small, color = NestlyColors.TextMuted)
            }
        }
        StatusChip(status.state)
    }
    RowDivider()
}

private fun doseSubtitle(status: DoseStatus): String = when (status.state) {
    DoseState.GIVEN -> status.givenDate?.let { "Given ${Format.date(it)}" } ?: "Given"
    DoseState.OVERDUE -> status.dueDate?.let { "Was due ${Format.date(it)} · ${status.groupLabel}" }
        ?: "Overdue · ${status.groupLabel}"
    DoseState.DUE -> "Due now · recommended at ${status.groupLabel}"
    DoseState.UPCOMING -> status.dueDate?.let { "Due ${Format.date(it)} · ${status.groupLabel}" }
        ?: "Recommended at ${status.groupLabel}"
}

@Composable
private fun StatusIcon(state: DoseState) {
    when (state) {
        DoseState.GIVEN -> Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(NestlyColors.Accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Check, "Given", tint = Color.White, modifier = Modifier.size(20.dp))
        }
        DoseState.OVERDUE -> Icon(
            Icons.Outlined.ErrorOutline,
            "Overdue",
            tint = NestlyColors.Danger,
            modifier = Modifier.size(30.dp),
        )
        DoseState.DUE -> Icon(
            Icons.Outlined.Schedule,
            "Due now",
            tint = NestlyColors.AccentText,
            modifier = Modifier.size(30.dp),
        )
        DoseState.UPCOMING -> Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .border(1.5.dp, NestlyColors.TextMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Add, "Log dose", tint = NestlyColors.AccentText, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun StatusChip(state: DoseState) {
    val (label, color) = when (state) {
        DoseState.GIVEN -> "Given" to NestlyColors.AccentText
        DoseState.OVERDUE -> "Overdue" to NestlyColors.Danger
        DoseState.DUE -> "Due now" to NestlyColors.AccentText
        DoseState.UPCOMING -> "Upcoming" to NestlyColors.TextSecondary
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, style = NestlyType.Small, color = color)
    }
}

@Composable
private fun HistoryRow(entry: ActivityEntity, onClick: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(entry.startTime).atZone(zone).toLocalDate()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                entry.vaccine?.vaccines?.joinToString(", ")?.ifBlank { null } ?: "Vaccine",
                style = NestlyType.Body,
                color = NestlyColors.TextPrimary,
            )
            Text(
                Format.date(date),
                style = NestlyType.Small,
                color = NestlyColors.TextSecondary,
            )
            entry.note?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = NestlyType.Small, color = NestlyColors.TextSecondary)
            }
        }
    }
    RowDivider()
}
