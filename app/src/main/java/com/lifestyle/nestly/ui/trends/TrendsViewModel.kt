package com.lifestyle.nestly.ui.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifestyle.nestly.AppContainer
import com.lifestyle.nestly.data.model.ProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

enum class TrendsWindow(val days: Int, val label: String) {
    ONE(1, "1d"),
    SEVEN(7, "7d"),
    FOURTEEN(14, "14d"),
}

/** A metric for the selected window alongside the same metric for the window just before it. */
data class TrendRow(val current: MetricResult, val previous: MetricResult) {
    val metric: TrendMetric get() = current.metric

    /** Null when either window has nothing to compare. */
    val delta: Double?
        get() = if (current.sampleCount > 0 && previous.sampleCount > 0) current.value - previous.value else null
}

data class TrendsData(
    val profile: ProfileEntity,
    val window: TrendsWindow,
    val rows: List<TrendRow>,
)

/**
 * Loads [window] days ending [periodsBack] windows before today, plus the window before
 * that for the comparison, and maps each to [TrendRow]s for [metrics].
 */
internal fun AppContainer.trendRows(
    profile: ProfileEntity,
    window: TrendsWindow,
    periodsBack: Int,
    metrics: List<TrendMetric>,
    zone: ZoneId,
): Flow<Pair<LocalDate, List<TrendRow>>> {
    val days = window.days
    val from = LocalDate.now(zone).minusDays((days - 1L) + days.toLong() * periodsBack)
    val previousFrom = from.minusDays(days.toLong())
    val fromMs = previousFrom.atStartOfDay(zone).toInstant().toEpochMilli()
    val toMs = from.plusDays(days.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
    return activities.range(profile.familyId, profile.id, fromMs, toMs).map { entries ->
        from to metrics.map { metric ->
            TrendRow(
                current = TrendsAggregator.compute(metric, entries, from, days, zone),
                previous = TrendsAggregator.compute(metric, entries, previousFrom, days, zone),
            )
        }
    }.flowOn(Dispatchers.Default)
}

@OptIn(ExperimentalCoroutinesApi::class)
class TrendsViewModel(private val container: AppContainer) : ViewModel() {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val window = MutableStateFlow(TrendsWindow.SEVEN)

    val data: StateFlow<TrendsData?> = combine(window, container.family.selectedProfile) { w, p -> w to p }
        .flatMapLatest { (selected, profile) ->
            if (profile == null) flowOf(null)
            else container.trendRows(profile, selected, 0, TrendMetric.entries, zone).map { (_, rows) ->
                TrendsData(profile, selected, rows)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setWindow(window: TrendsWindow) {
        this.window.value = window
    }
}
