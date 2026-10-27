package com.lifestyle.nestly.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifestyle.nestly.AppContainer
import com.lifestyle.nestly.data.model.ActivityEntity
import com.lifestyle.nestly.data.model.ActivityType
import com.lifestyle.nestly.data.model.ProfileEntity
import com.lifestyle.nestly.data.prefs.ActivityLayout
import com.lifestyle.nestly.data.prefs.CardKind
import com.lifestyle.nestly.data.timer.TimerKey
import com.lifestyle.nestly.data.timer.TimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

data class DashboardData(
    val profile: ProfileEntity,
    /** Everything since the start of yesterday, newest first. */
    val recent: List<ActivityEntity>,
    val latest: Map<CardKind, ActivityEntity?>,
    val growth: List<ActivityEntity>,
    val milestones: List<ActivityEntity>,
    val latestMedical: ActivityEntity?,
    val latestVaccine: ActivityEntity?,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModel(private val container: AppContainer) : ViewModel() {
    private val activities = container.activities
    private val zone: ZoneId get() = ZoneId.systemDefault()

    val layout: StateFlow<ActivityLayout> = container.preferences.layout
        .stateIn(viewModelScope, SharingStarted.Eagerly, ActivityLayout.DEFAULT)

    val timers: StateFlow<Map<TimerKey, TimerState>> = container.timers.timers
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    /** Ticks every second while a timer runs, otherwise every 30 seconds. */
    val now: StateFlow<Long> = timers
        .map { all -> all.values.any { it.isRunning } }
        .distinctUntilChanged()
        .flatMapLatest { running -> ticker(if (running) 1_000 else 30_000) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, System.currentTimeMillis())

    private val startOfYesterday: Flow<Long> = now
        .map { startOfDay(it) - DAY_MS }
        .distinctUntilChanged()

    val data: StateFlow<DashboardData?> = container.family.selectedProfile
        .flatMapLatest { profile ->
            if (profile == null) flowOf(null)
            else startOfYesterday.flatMapLatest { since -> dashboardFlow(profile, since) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun startOfDay(epochMs: Long): Long =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    fun saveLayout(layout: ActivityLayout) = viewModelScope.launch { container.preferences.setLayout(layout) }

    private fun dashboardFlow(profile: ProfileEntity, since: Long): Flow<DashboardData> {
        val f = profile.familyId
        val p = profile.id
        val cards = listOf(CardKind.FEED, CardKind.PUMP, CardKind.DIAPER, CardKind.SLEEP, CardKind.ROUTINE, CardKind.BABY_FIRSTS)
        val flows: List<Flow<Any?>> = listOf(
            activities.range(f, p, since, Long.MAX_VALUE),
            activities.allOf(f, p, listOf(ActivityType.GROWTH)),
            activities.allOf(f, p, listOf(ActivityType.MILESTONE)),
            activities.latestOf(f, p, listOf(ActivityType.MEDICAL)),
            activities.latestOf(f, p, listOf(ActivityType.VACCINE)),
        ) + cards.map { activities.latestOf(f, p, it.types) }

        return combine(flows) { values ->
            @Suppress("UNCHECKED_CAST")
            DashboardData(
                profile = profile,
                recent = values[0] as List<ActivityEntity>,
                growth = values[1] as List<ActivityEntity>,
                milestones = values[2] as List<ActivityEntity>,
                latestMedical = values[3] as ActivityEntity?,
                latestVaccine = values[4] as ActivityEntity?,
                latest = cards.mapIndexed { i, card -> card to values[5 + i] as ActivityEntity? }.toMap(),
            )
        }
    }

    private fun ticker(periodMs: Long) = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(periodMs - System.currentTimeMillis() % periodMs)
        }
    }

    companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
