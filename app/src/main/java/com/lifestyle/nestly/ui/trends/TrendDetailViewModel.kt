package com.lifestyle.nestly.ui.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifestyle.nestly.AppContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class TrendDetailData(
    val profileName: String,
    val window: TrendsWindow,
    /** 0 for the window ending today, 1 for the one before, … */
    val periodsBack: Int,
    val dates: List<LocalDate>,
    val row: TrendRow,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TrendDetailViewModel(
    private val container: AppContainer,
    private val metric: TrendMetric,
    initialWindow: TrendsWindow,
) : ViewModel() {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val window = MutableStateFlow(initialWindow)
    private val periodsBack = MutableStateFlow(0)

    val data: StateFlow<TrendDetailData?> = combine(
        container.family.selectedProfile,
        window,
        periodsBack,
    ) { profile, w, back -> Triple(profile, w, back) }
        .flatMapLatest { (profile, w, back) ->
            if (profile == null) flowOf(null)
            else container.trendRows(profile, w, back, listOf(metric), zone).map { (from, rows) ->
                TrendDetailData(
                    profileName = profile.name,
                    window = w,
                    periodsBack = back,
                    dates = (0 until w.days).map { from.plusDays(it.toLong()) },
                    row = rows.single(),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setWindow(window: TrendsWindow) {
        this.window.value = window
        periodsBack.value = 0
    }

    fun previousPeriod() {
        periodsBack.value += 1
    }

    fun nextPeriod() {
        periodsBack.value = (periodsBack.value - 1).coerceAtLeast(0)
    }
}
