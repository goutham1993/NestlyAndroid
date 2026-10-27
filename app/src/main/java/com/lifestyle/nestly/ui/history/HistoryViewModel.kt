package com.lifestyle.nestly.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifestyle.nestly.AppContainer
import com.lifestyle.nestly.data.db.entity.ActivityEntity
import com.lifestyle.nestly.data.db.entity.ProfileEntity
import com.lifestyle.nestly.data.prefs.FilterOption
import com.lifestyle.nestly.ui.activity.ActivityFilters
import com.lifestyle.nestly.ui.activity.barValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

data class HistoryDay(
    val date: LocalDate,
    val hasEntries: Boolean,
)

data class HistoryData(
    val profile: ProfileEntity,
    val weekStart: LocalDate,
    val days: List<HistoryDay>,
    val selected: LocalDate,
    /** Filtered entries for [selected], newest first. */
    val entries: List<ActivityEntity>,
    val maxBarValue: Double,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(private val container: AppContainer) : ViewModel() {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val firstDayOfWeek: DayOfWeek =
        WeekFields.of(Locale.getDefault()).firstDayOfWeek

    private val selectedDate = MutableStateFlow(LocalDate.now(zone))

    val data: StateFlow<HistoryData?> = combine(
        container.family.selectedProfile,
        selectedDate,
        container.preferences.filters,
    ) { profile, selected, filters -> Triple(profile, selected, filters) }
        .flatMapLatest { (profile, selected, filters) ->
            if (profile == null) flowOf(null)
            else {
                val weekStart = selected.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
                val weekEnd = weekStart.plusDays(7)
                val from = weekStart.atStartOfDay(zone).toInstant().toEpochMilli()
                val to = weekEnd.atStartOfDay(zone).toInstant().toEpochMilli()
                container.activities.range(profile.familyId, profile.id, from, to).map { weekEntries ->
                    buildData(profile, weekStart, selected, weekEntries, filters)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectDay(date: LocalDate) {
        selectedDate.value = date
    }

    fun shiftWeek(deltaWeeks: Long) {
        selectedDate.value = selectedDate.value.plusWeeks(deltaWeeks)
    }

    private fun buildData(
        profile: ProfileEntity,
        weekStart: LocalDate,
        selected: LocalDate,
        weekEntries: List<ActivityEntity>,
        filters: Set<FilterOption>,
    ): HistoryData {
        val filtered = ActivityFilters.filter(weekEntries, filters)
        val byDay = filtered.groupBy { Instant.ofEpochMilli(it.startTime).atZone(zone).toLocalDate() }
        val days = (0L until 7L).map { offset ->
            val date = weekStart.plusDays(offset)
            HistoryDay(date, date in byDay)
        }
        val dayEntries = byDay[selected].orEmpty()
        val maxBar = dayEntries.mapNotNull { it.barValue }.maxOrNull() ?: 0.0
        return HistoryData(
            profile = profile,
            weekStart = weekStart,
            days = days,
            selected = selected,
            entries = dayEntries,
            maxBarValue = maxBar,
        )
    }
}
