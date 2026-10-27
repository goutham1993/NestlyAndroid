package com.lifestyle.nestly.ui.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.prefs.CardKind
import com.lifestyle.nestly.data.units.Units
import com.lifestyle.nestly.data.units.VolumeUnit
import com.lifestyle.nestly.ui.AppViewModel
import com.lifestyle.nestly.ui.activity.title
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.ProfileHeader
import com.lifestyle.nestly.ui.components.SegmentedToggle
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import com.lifestyle.nestly.ui.units.currentUnitPrefs
import com.lifestyle.nestly.ui.theme.headerColor
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TrendsTab(appViewModel: AppViewModel) {
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
        )
        val trends = data
        if (trends == null) return@Column

        Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            SegmentedToggle(
                options = TrendsWindow.entries.map { it.label },
                selected = TrendsWindow.entries.indexOf(trends.window),
                onSelect = { viewModel.setWindow(TrendsWindow.entries[it]) },
            )
        }

        if (!trends.hasAnyData) {
            Text(
                "Not enough data yet.",
                style = NestlyType.Body,
                color = NestlyColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
            )
            return@Column
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            trends.cards.forEach { card -> TrendCardView(card) }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TrendCardView(card: TrendCard) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(NestlyColors.Surface),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(card.kind.headerColor)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(card.kind.trendTitle, style = NestlyType.CardTitle, color = NestlyColors.HeaderText)
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(averageLabel(card), style = NestlyType.CardLead)
            Spacer(Modifier.height(4.dp))
            Text(
                "${card.count} ${if (card.count == 1) "entry" else "entries"}",
                style = NestlyType.Small,
                color = NestlyColors.TextSecondary,
            )
            Spacer(Modifier.height(20.dp))
            TrendBars(card)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                card.bars.forEach { bar ->
                    Text(
                        bar.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = NestlyType.Small,
                        color = NestlyColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendBars(card: TrendCard) {
    val max = card.bars.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1.0
    val color = card.kind.headerColor
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(120.dp),
    ) {
        val n = card.bars.size.coerceAtLeast(1)
        val gap = 6.dp.toPx()
        val barWidth = ((size.width - gap * (n - 1)) / n).coerceAtLeast(4.dp.toPx())
        card.bars.forEachIndexed { index, bar ->
            val fraction = (bar.value / max).toFloat().coerceIn(0f, 1f)
            val h = (size.height * fraction).coerceAtLeast(if (bar.value > 0) 4.dp.toPx() else 0f)
            val x = index * (barWidth + gap)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, size.height - h),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
        }
    }
}

private val CardKind.trendTitle: String
    get() = when (this) {
        CardKind.FEED -> "Feed"
        CardKind.PUMP -> "Pump"
        CardKind.DIAPER -> "Diaper"
        CardKind.SLEEP -> "Sleep"
        else -> title
    }

@Composable
private fun averageLabel(card: TrendCard): String {
    val units = currentUnitPrefs()
    return when (card.kind) {
        CardKind.FEED, CardKind.PUMP -> {
            val avg = Units.mlToDisplay(card.average, units.volume)
            val decimals = if (units.volume == VolumeUnit.OZ) 1 else 0
            "Avg ${Format.number(avg, decimals)} ${Units.volumeLabel(units.volume)} / day"
        }
        CardKind.DIAPER -> "Avg ${Format.number(card.average, 1)} / day"
        CardKind.SLEEP -> "Avg ${Format.duration(card.average.toLong())} / day"
        else -> Format.number(card.average)
    }
}
