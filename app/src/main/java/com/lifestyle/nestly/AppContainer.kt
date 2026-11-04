package com.lifestyle.nestly

import android.app.Application
import android.content.Context
import com.lifestyle.nestly.data.importer.NaraCsvImporter
import com.lifestyle.nestly.data.importer.NestlyCsvExporter
import com.lifestyle.nestly.data.importer.NestlyCsvImporter
import com.lifestyle.nestly.data.prefs.UserPreferences
import com.lifestyle.nestly.data.reminder.ReminderNotifications
import com.lifestyle.nestly.data.reminder.ReminderScheduler
import com.lifestyle.nestly.data.repository.ActivityRepository
import com.lifestyle.nestly.data.repository.AppointmentRepository
import com.lifestyle.nestly.data.repository.AuthRepository
import com.lifestyle.nestly.data.repository.FamilyRepository
import com.lifestyle.nestly.data.repository.ReminderRepository
import com.lifestyle.nestly.data.supabase.PhotoStore
import com.lifestyle.nestly.data.supabase.RealtimeManager
import com.lifestyle.nestly.data.supabase.createNestlyClient
import com.lifestyle.nestly.data.timer.ActiveTimerStore
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val supabase: SupabaseClient = createNestlyClient()
    val preferences = UserPreferences(appContext)
    val timers = ActiveTimerStore(appContext)
    val photos = PhotoStore(appContext, supabase)
    val auth = AuthRepository(supabase)
    val family = FamilyRepository(supabase, preferences)
    val activities = ActivityRepository(supabase, photos)
    val reminders = ReminderRepository(supabase, family)
    val appointments = AppointmentRepository(supabase)
    val realtime = RealtimeManager(supabase, CoroutineScope(SupervisorJob() + Dispatchers.IO))
    val scheduler = ReminderScheduler(appContext)
    val importer = NaraCsvImporter(activities, family)
    val nestlyImporter = NestlyCsvImporter(activities, family)
    val exporter = NestlyCsvExporter(activities, family)
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Refetch everything when connectivity returns. */
    fun startConnectivityRefresh() {
        val cm = appContext.getSystemService(android.net.ConnectivityManager::class.java)
        cm?.registerDefaultNetworkCallback(object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                realtime.refreshAll()
            }
        })
    }

    /** Keeps the Realtime subscription pointed at the signed-in user's family. */
    fun startRealtimeSync() {
        applicationScope.launch {
            auth.userId.distinctUntilChanged().collect { userId ->
                if (userId == null) {
                    realtime.unsubscribe()
                } else runCatching {
                    // selectedProfile.first() can suspend forever on a cold
                    // start: refetchFlow emits nothing when its first fetch
                    // fails (e.g. raced session restore), so waiting without a
                    // bound blocks this collector and subscribe() is never
                    // reached. Bound it and fall back to the direct fetches,
                    // which fail fast instead of hanging.
                    val profile = withTimeoutOrNull(SELECTED_PROFILE_WAIT_MS) {
                        runCatching { family.selectedProfile.first() }.getOrNull()
                    }
                    val familyId = profile?.familyId
                        ?: runCatching { family.fetchProfiles().firstOrNull()?.familyId }.getOrNull()
                        ?: runCatching { family.fetchFamilies().firstOrNull()?.id }.getOrNull()
                    if (familyId != null) realtime.subscribe(familyId)
                }
            }
        }
    }

    private companion object {
        const val SELECTED_PROFILE_WAIT_MS = 20_000L
    }
}

class NestlyApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        // Fresh start for the online-only app: drop any pre-migration Room database.
        deleteDatabase("nestly.db")
        container = AppContainer(this)
        ReminderNotifications.ensureChannel(this)
        container.startRealtimeSync()
        container.startConnectivityRefresh()
        container.applicationScope.launch {
            runCatching { container.scheduler.rescheduleAll(container) }
        }
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as NestlyApplication).container
