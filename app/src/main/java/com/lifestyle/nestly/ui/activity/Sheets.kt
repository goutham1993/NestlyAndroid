package com.lifestyle.nestly.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifestyle.nestly.data.db.entity.ActivityEntity
import com.lifestyle.nestly.data.prefs.FilterOption
import com.lifestyle.nestly.ui.components.ActivityIcon
import com.lifestyle.nestly.ui.components.EntrySheet
import com.lifestyle.nestly.ui.components.IconKind
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.components.SegmentedToggle
import com.lifestyle.nestly.ui.entry.EntryKind
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import com.lifestyle.nestly.ui.theme.headerColor
import com.lifestyle.nestly.ui.units.currentUnitPrefs

/** Bottom sheet listing Breastfeed / Bottle Feed / Solids / Combo Feed with large icons. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedTypePicker(options: List<EntryKind>, onDismiss: () -> Unit, onPick: (EntryKind) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = NestlyColors.Surface,
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
        dragHandle = null,
    ) {
        Column(
            Modifier
                .padding(vertical = 24.dp)
                .navigationBarsPadding(),
        ) {
            options.forEach { kind ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(kind) }
                        .padding(horizontal = 28.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActivityIcon(kind.icon, size = 76.dp)
                    Spacer(Modifier.width(32.dp))
                    Text(kind.title, style = NestlyType.CardLead.copy(fontSize = 24.sp))
                }
            }
        }
    }
}

/** Today / Last 24 Hours totals per activity type. */
@Composable
fun SummarySheet(recent: List<ActivityEntity>, now: Long, startOfToday: Long, onDismiss: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val units = currentUnitPrefs()
    val lines = if (tab == 0) Summary.summarize(recent, startOfToday, now + 60_000, units)
    else Summary.summarize(recent, now - ActivityViewModel.DAY_MS, now + 60_000, units)

    EntrySheet(title = "Summary", headerColor = Color.Transparent, onDismiss = onDismiss, onSave = null) {
        RowDivider()
        Box(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            SegmentedToggle(listOf("Today", "Last 24 Hours"), tab, onSelect = { tab = it })
        }
        RowDivider()
        if (lines.isEmpty()) {
            Text(
                "Nothing logged yet.",
                style = NestlyType.Body,
                color = NestlyColors.TextSecondary,
                modifier = Modifier.padding(24.dp),
            )
        }
        lines.forEach { line ->
            val kind = EntryKind.of(line.type)
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ActivityIcon(kind.icon, size = 60.dp)
                Spacer(Modifier.width(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(line.type.displayName, style = NestlyType.CardLead)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            line.count.toString(),
                            style = NestlyType.Small.copy(fontWeight = FontWeight.SemiBold),
                            color = NestlyColors.HeaderText,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(kind.card.headerColor)
                                .padding(horizontal = 8.dp, vertical = 1.dp),
                        )
                    }
                    line.details.forEach { Text(it, style = NestlyType.Body) }
                }
            }
            RowDivider()
        }
    }
}

private data class FilterRow(val option: FilterOption, val label: String, val icon: IconKind?)

private val FILTER_ROWS = listOf(
    FilterRow(FilterOption.NOTES, "Notes", null),
    FilterRow(FilterOption.PHOTOS, "Photos", null),
    FilterRow(FilterOption.FEED, "Feed", IconKind.BOTTLE),
    FilterRow(FilterOption.PUMP, "Pump", IconKind.PUMP),
    FilterRow(FilterOption.DIAPER, "Diaper", IconKind.DIAPER),
    FilterRow(FilterOption.SLEEP, "Sleep", IconKind.SLEEP),
    FilterRow(FilterOption.ROUTINE, "Routine", IconKind.ROUTINE),
    FilterRow(FilterOption.HEALTH, "Health", IconKind.MEDICAL),
)

@Composable
fun FiltersSheet(initial: Set<FilterOption>, onDismiss: () -> Unit, onApply: (Set<FilterOption>) -> Unit) {
    var selected by remember { mutableStateOf(initial) }
    EntrySheet(
        title = "Filters",
        headerColor = NestlyColors.Diaper,
        onDismiss = onDismiss,
        onSave = { onApply(selected) },
    ) {
        FILTER_ROWS.forEachIndexed { index, row ->
            if (index == 2) {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 30.dp)
                        .background(NestlyColors.Background),
                )
                RowDivider()
            }
            val isOn = row.option in selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 82.dp)
                    .clickable { selected = if (isOn) selected - row.option else selected + row.option }
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (row.icon != null) {
                    ActivityIcon(row.icon, size = 44.dp)
                    Spacer(Modifier.width(16.dp))
                }
                Text(row.label, style = NestlyType.RowLabel, modifier = Modifier.weight(1f))
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isOn) NestlyColors.Accent else Color.Transparent)
                        .border(1.5.dp, NestlyColors.Accent, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isOn) Icon(Icons.Outlined.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            RowDivider()
        }
    }
}
