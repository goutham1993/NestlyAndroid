package com.lifestyle.nestly.dream

import com.lifestyle.nestly.AppContainer
import com.lifestyle.nestly.data.model.ActivityEntity
import com.lifestyle.nestly.data.model.ProfileEntity
import com.lifestyle.nestly.data.model.ReminderKind
import com.lifestyle.nestly.data.reminder.NextFeedState
import com.lifestyle.nestly.data.reminder.ReminderLogic
import com.lifestyle.nestly.data.units.UnitPrefs
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneId

data class NightstandState(
    val profileName: String? = null,
    val lastFeed: ActivityEntity? = null,
    val nextFeed: NextFeedState = NextFeedState.NoFeed,
    val intervalMinutes: Int = ReminderLogic.defaultIntervalMinutes(ReminderKind.FEED),
    val todayFeedCount: Int = 0,
    /** Total bottle volume today in ml; breastfeed/solids contribute no volume. */
    val todayBottleMl: Double = 0.0,
    val units: UnitPrefs = UnitPrefs.DEFAULT,
    val now: Long = System.currentTimeMillis(),
)

/**
 * Plain state holder for the nightstand dream (a DreamService has no
 * Activity/ViewModelStore to host a real ViewModel). Owns no scope of its
 * own; the service passes its dreaming scope and cancels it on stop.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NightstandViewModel(
    private val container: AppContainer,
    scope: CoroutineScope,
) {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    init {
        scope.launch {
            // A dream can cold-start the process while the stored Supabase
            // session is still being restored; querying earlier returns no
            // profiles under RLS. Once the session lands, refetch everything.
            val session = withTimeoutOrNull(SESSION_WAIT_MS) {
                container.auth.sessionStatus.first {
                    it is SessionStatus.Authenticated || it is SessionStatus.NotAuthenticated
                }
            }
            if (session is SessionStatus.Authenticated) container.realtime.refreshAll()
        }
    }

    /** Minute-resolution clock; the countdown never needs second precision. */
    private val now: Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000L - System.currentTimeMillis() % 30_000L)
        }
    }

    val state: StateFlow<NightstandState?> = container.family.selectedProfile
        .flatMapLatest { profile -> profileState(profile) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    private fun profileState(profile: ProfileEntity?): Flow<NightstandState> {
        if (profile == null) {
            return combine(now, container.preferences.units) { nowMs, units ->
                NightstandState(units = units, now = nowMs)
            }
        }
        val familyId = profile.familyId
        val profileId = profile.id
        // Fetch since yesterday and filter client-side so the "today"
        // window survives crossing midnight while dreaming.
        val sinceYesterday: Flow<Long> = now
            .map { startOfDay(it) - DAY_MS }
            .distinctUntilChanged()

        val feedTypes = ReminderLogic.typesFor(ReminderKind.FEED)
        val inner: Flow<RawFeeds> = sinceYesterday.flatMapLatest { since ->
            val flows: List<Flow<Any?>> = listOf(
                container.activities.latestOf(familyId, profileId, feedTypes),
                container.activities.range(familyId, profileId, since, Long.MAX_VALUE),
                container.reminders.observeForProfile(familyId, profileId),
                container.preferences.units,
            )
            combine(flows) { values ->
                @Suppress("UNCHECKED_CAST")
                RawFeeds(
                    lastFeed = values[0] as ActivityEntity?,
                    recent = values[1] as List<ActivityEntity>,
                    reminders = values[2] as List<com.lifestyle.nestly.data.model.ReminderEntity>,
                    units = values[3] as UnitPrefs,
                )
            }
        }
        return combine(inner, now) { raw, nowMs ->
            val startOfToday = startOfDay(nowMs)
            val todayFeeds = raw.recent.filter {
                it.deletedAt == null && it.type in feedTypes &&
                    it.startTime >= startOfToday && it.startTime <= nowMs + 60_000L
            }
            val interval = raw.reminders.firstOrNull { it.kind == ReminderKind.FEED }?.intervalMinutes
                ?: ReminderLogic.defaultIntervalMinutes(ReminderKind.FEED)
            NightstandState(
                profileName = profile.name,
                lastFeed = raw.lastFeed,
                nextFeed = ReminderLogic.nextFeedState(raw.lastFeed?.startTime, interval, nowMs),
                intervalMinutes = interval,
                todayFeedCount = todayFeeds.size,
                todayBottleMl = todayFeeds.sumOf { it.bottle?.totalMl ?: 0.0 },
                units = raw.units,
                now = nowMs,
            )
        }
    }

    private fun startOfDay(epochMs: Long): Long =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    private data class RawFeeds(
        val lastFeed: ActivityEntity?,
        val recent: List<ActivityEntity>,
        val reminders: List<com.lifestyle.nestly.data.model.ReminderEntity>,
        val units: UnitPrefs,
    )

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
        const val SESSION_WAIT_MS = 5_000L
    }
}
