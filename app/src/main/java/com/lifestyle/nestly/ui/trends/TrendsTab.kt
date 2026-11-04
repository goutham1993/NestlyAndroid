package com.lifestyle.nestly.ui.trends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.ui.AppViewModel
import com.lifestyle.nestly.ui.Routes
import com.lifestyle.nestly.ui.activity.title
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.ProfileHeader
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import com.lifestyle.nestly.ui.units.currentUnitPrefs

@Composable
fun TrendsTab(appViewModel: AppViewModel, navigate: (String) -> Unit) {
    val container = LocalContext.current.appContainer
    val viewModel: TrendsViewModel = viewModel { TrendsViewModel(container) }
    val data by viewModel.data.collectAsStateWithLifecycle()
    val profile by appViewModel.profile.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        ProfileHeader(
            name = data?.profile?.name ?: profile?.name ?: "Nestly",
            dateText = Format.headerDate(System.currentTimeMillis()),
            ageText = (data?.profile ?: profile)?.birthDate?.let { Format.age(it) },
        ) {
            WindowChips(data?.window ?: TrendsWindow.SEVEN, onSelect = viewModel::setWindow)
        }
        val trends = data ?: return@Column

        LazyColumn(Modifier.weight(1f)) {
            trends.rows.groupBy { it.metric.section }.forEach { (section, rows) ->
                item(key = section.name) {
                    Text(
                        section.title,
                        style = NestlyType.SectionTitle,
                        color = NestlyColors.TextPrimary,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 10.dp),
                    )
                    RowDivider()
                }
                items(rows, key = { it.metric.name }) { row ->
                    MetricListRow(row) { navigate(Routes.trendDetail(row.metric, trends.window)) }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MetricListRow(row: TrendRow, onClick: () -> Unit) {
    val prefs = currentUnitPrefs()
    val metric = row.metric
    Column(Modifier.background(NestlyColors.Surface)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .clickable(onClick = onClick)
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetricIcon(metric)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(metric.title, style = NestlyType.RowLabel.copy(fontSize = 18.sp, lineHeight = 22.sp), color = NestlyColors.TextPrimary)
                Text(metric.summary(row.current.value, prefs), style = NestlyType.Small, color = NestlyColors.TextSecondary)
                if (metric.showBreakdown) {
                    row.current.breakdown.filter { it.value > 0 }.forEach { segment ->
                        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            SeriesDot(segment.series)
                            Spacer(Modifier.width(8.dp))
                            Text(segment.series.label, style = NestlyType.Small.copy(fontSize = 12.sp), color = NestlyColors.TextPrimary)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                metric.formatValue(segment.value, prefs),
                                style = NestlyType.Small.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                                color = NestlyColors.TextPrimary,
                            )
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    null,
                    tint = NestlyColors.TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
                val delta = row.delta
                if (delta != null && metric.isMeaningful(delta)) DeltaBadge(metric, delta, prefs)
            }
        }
        RowDivider()
    }
}
