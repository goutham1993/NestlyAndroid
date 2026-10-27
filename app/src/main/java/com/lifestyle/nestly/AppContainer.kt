package com.lifestyle.nestly

import android.app.Application
import android.content.Context
import com.lifestyle.nestly.data.db.NestlyDatabase
import com.lifestyle.nestly.data.importer.NaraCsvImporter
import com.lifestyle.nestly.data.importer.NestlyCsvExporter
import com.lifestyle.nestly.data.importer.NestlyCsvImporter
import com.lifestyle.nestly.data.prefs.UserPreferences
import com.lifestyle.nestly.data.reminder.ReminderNotifications
import com.lifestyle.nestly.data.reminder.ReminderScheduler
import com.lifestyle.nestly.data.repository.ActivityRepository
import com.lifestyle.nestly.data.repository.FamilyRepository
import com.lifestyle.nestly.data.repository.ReminderRepository
import com.lifestyle.nestly.data.timer.ActiveTimerStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val database: NestlyDatabase = NestlyDatabase.get(appContext)
    val preferences = UserPreferences(appContext)
    val timers = ActiveTimerStore(appContext)
    val activities = ActivityRepository(database)
    val family = FamilyRepository(database, preferences)
    val reminders = ReminderRepository(database)
    val scheduler = ReminderScheduler(appContext)
    val importer = NaraCsvImporter(database)
    val nestlyImporter = NestlyCsvImporter(database)
    val exporter = NestlyCsvExporter(database)
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

class NestlyApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderNotifications.ensureChannel(this)
        container.applicationScope.launch {
            container.scheduler.rescheduleAll(container)
        }
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as NestlyApplication).container
