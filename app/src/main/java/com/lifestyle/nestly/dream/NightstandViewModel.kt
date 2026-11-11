package com.lifestyle.nestly.dream

import com.lifestyle.nestly.AppContainer
import com.lifestyle.nestly.data.model.ActivityEntity
import com.lifestyle.nestly.data.model.ProfileEntity
import com.lifestyle.nestly.data.model.ReminderKind
import com.lifestyle.nestly.data.reminder.DailyDose
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    /** When today's Vitamin D was given; null until it is (resets at local midnight). */
    val vitaminDGivenAt: Long? = null,
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

    // Written by the session collector, read by the periodic refresher.
    @Volatile
    private var authenticated = false

    init {
        // A dream can cold-start the process while the stored Supabase session
        // is still being restored (Initializing / RefreshFailure can last well
        // past any short wait — token refresh retries are ~10s apart; see
        // QuickBottleLog). The old one-shot 5s wait gave up before the session
        // landed on exactly those cold starts, so nothing ever refetched again
        // and the dream sat on its initial (or empty) fetch forever. Instead:
        // refetch the moment the session becomes Authenticated, however long
        // that takes, for as long as the dream runs.
        scope.launch {
            container.auth.sessionStatus
                .map { it is SessionStatus.Authenticated }
                .distinctUntilChanged()
                .collect { isAuthenticated ->
                    authenticated = isAuthenticated
                    if (isAuthenticated) container.realtime.refreshAll()
                }
        }
        // Belt-and-suspenders while dreaming: Realtime is the live path, but
        // if its subscribe never landed (cold-start race, dead socket) there
        // is no other recovery. A plugged-in/docked dream can afford to
        // refetch outright every minute; refetchFlow keeps the last good
        // value if a fetch fails, so a transient miss costs nothing.
        scope.launch {
            while (true) {
                delay(PERIODIC_REFRESH_MS)
                if (authenticated) container.realtime.refreshAll()
            }
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
                container.activities.latestFeedForSchedule(familyId, profileId),
            )
            combine(flows) { values ->
                @Suppress("UNCHECKED_CAST")
                RawFeeds(
                    lastFeed = values[0] as ActivityEntity?,
                    recent = values[1] as List<ActivityEntity>,
                    reminders = values[2] as List<com.lifestyle.nestly.data.model.ReminderEntity>,
                    units = values[3] as UnitPrefs,
                    scheduleFeed = values[4] as ActivityEntity?,
                )
            }
        }
        return combine(inner, now, container.preferences.defaultFeedIntervalMinutes) { raw, nowMs, defaultInterval ->
            val startOfToday = startOfDay(nowMs)
            val todayFeeds = raw.recent.filter {
                it.deletedAt == null && it.type in feedTypes &&
                    it.startTime >= startOfToday && it.startTime <= nowMs + 60_000L
            }
            val interval = raw.reminders.firstOrNull { it.kind == ReminderKind.FEED }?.intervalMinutes
                ?: defaultInterval
            NightstandState(
                profileName = profile.name,
                lastFeed = raw.lastFeed,
                nextFeed = ReminderLogic.nextFeedState(raw.scheduleFeed?.startTime, interval, nowMs),
                intervalMinutes = interval,
                todayFeedCount = todayFeeds.size,
                todayBottleMl = todayFeeds.sumOf { it.bottle?.totalMl ?: 0.0 },
                units = raw.units,
                vitaminDGivenAt = DailyDose.givenAt(raw.recent, startOfToday, startOfNextDay(nowMs)),
                now = nowMs,
            )
        }
    }

    private fun startOfDay(epochMs: Long): Long =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()

    private fun startOfNextDay(epochMs: Long): Long =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    private data class RawFeeds(
        val lastFeed: ActivityEntity?,
        val recent: List<ActivityEntity>,
        val reminders: List<com.lifestyle.nestly.data.model.ReminderEntity>,
        val units: UnitPrefs,
        val scheduleFeed: ActivityEntity?,
    )

    private companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
        const val PERIODIC_REFRESH_MS = 60_000L
    }
}
