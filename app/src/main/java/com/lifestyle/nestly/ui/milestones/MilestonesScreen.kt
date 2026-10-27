package com.lifestyle.nestly.ui.milestones

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
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
import com.lifestyle.nestly.data.catalog.Milestone
import com.lifestyle.nestly.data.catalog.MilestoneCatalog
import com.lifestyle.nestly.data.db.entity.ActivityEntity
import com.lifestyle.nestly.data.db.entity.ActivityType
import com.lifestyle.nestly.ui.components.DetailScreen
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.entry.EntryKind
import com.lifestyle.nestly.ui.entry.EntryRequest
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun MilestonesScreen(onBack: () -> Unit, onOpenEntry: (EntryRequest) -> Unit) {
    val container = LocalContext.current.appContainer
    val profile by container.family.selectedProfile.collectAsState(initial = null)
    val logged by remember(container) {
        container.family.selectedProfile.flatMapLatest { p ->
            if (p == null) flowOf(emptyList())
            else container.activities.allOf(p.familyId, p.id, listOf(ActivityType.MILESTONE))
        }
    }.collectAsState(initial = emptyList())

    val byKey: Map<String, ActivityEntity> = remember(logged) {
        logged.mapNotNull { row ->
            val key = row.milestone?.milestoneKey ?: row.milestone?.title?.let { MilestoneCatalog.findByTitle(it)?.key }
            key?.let { it to row }
        }.toMap()
    }
    val ageMonths = profile?.birthDate?.let { ChronoUnit.MONTHS.between(it, LocalDate.now()).toInt() }
    val defaultGroup = MilestoneCatalog.groups.lastOrNull { ageMonths != null && it.ageMonths <= ageMonths.coerceAtLeast(2) }
        ?: MilestoneCatalog.groups.first()
    var expanded by rememberSaveable { mutableStateOf(setOf(defaultGroup.label)) }

    DetailScreen(title = "Milestones", onBack = onBack) {
        LazyColumn(Modifier.weight(1f)) {
            MilestoneCatalog.groups.forEach { group ->
                val isOpen = group.label in expanded
                val done = group.items.count { it.key in byKey }
                item(key = group.label) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(NestlyColors.Surface)
                            .clickable { expanded = if (isOpen) expanded - group.label else expanded + group.label }
                            .heightIn(min = 76.dp)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(group.label.replaceFirstChar { it.uppercase() }, style = NestlyType.SectionTitle, modifier = Modifier.weight(1f))
                        Text("$done/${group.items.size}", style = NestlyType.Small, color = NestlyColors.TextSecondary)
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
                    items(group.items, key = { it.key }) { milestone ->
                        val row = byKey[milestone.key]
                        MilestoneRow(milestone, row) {
                            onOpenEntry(
                                if (row != null) EntryRequest.Edit(row.id)
                                else EntryRequest.New(EntryKind.MILESTONE, milestone),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneRow(milestone: Milestone, logged: ActivityEntity?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(milestone.title, style = NestlyType.Body, color = NestlyColors.TextPrimary)
            if (logged != null) {
                Text(
                    Format.relativeDay(logged.startTime, System.currentTimeMillis()),
                    style = NestlyType.Small,
                    color = NestlyColors.TextSecondary,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (logged != null) NestlyColors.Accent else Color.Transparent)
                .border(1.5.dp, NestlyColors.Accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (logged != null) Icons.Outlined.Check else Icons.Outlined.Add,
                contentDescription = if (logged != null) "Logged" else "Log milestone",
                tint = if (logged != null) Color.White else NestlyColors.AccentText,
                modifier = Modifier.size(20.dp),
            )
        }
    }
    RowDivider()
}
