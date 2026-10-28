package com.lifestyle.nestly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flare
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.ui.account.AccountTab
import com.lifestyle.nestly.ui.account.CommunicationPrefsScreen
import com.lifestyle.nestly.ui.account.EmailPasswordScreen
import com.lifestyle.nestly.ui.account.FamilyScreen
import com.lifestyle.nestly.ui.account.HelpScreen
import com.lifestyle.nestly.ui.account.SettingsScreen
import com.lifestyle.nestly.ui.account.SubscriptionScreen
import com.lifestyle.nestly.ui.account.UnitsScreen
import com.lifestyle.nestly.ui.activity.ActivityTab
import com.lifestyle.nestly.ui.activity.EditActivitiesScreen
import com.lifestyle.nestly.ui.components.DetailScreen
import com.lifestyle.nestly.ui.entry.EntryKind
import com.lifestyle.nestly.ui.entry.EntryRequest
import com.lifestyle.nestly.ui.entry.EntrySheetHost
import com.lifestyle.nestly.ui.importer.ImportScreen
import com.lifestyle.nestly.ui.importer.ImportViewModel
import com.lifestyle.nestly.ui.milestones.MilestonesScreen
import com.lifestyle.nestly.ui.history.HistoryTab
import com.lifestyle.nestly.ui.reminders.RemindersScreen
import com.lifestyle.nestly.ui.trends.TrendsTab
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.units.LocalUnitPrefs
import com.lifestyle.nestly.data.supabase.DataErrors
import com.lifestyle.nestly.data.units.UnitPrefs

object Routes {
    const val MAIN = "main"
    const val GROWTH = "growth/{metric}"
    fun growth(metric: com.lifestyle.nestly.data.growth.GrowthMetric) = "growth/${metric.name}"
    const val EDIT_ACTIVITIES = "editActivities"
    const val MILESTONES = "milestones"
    const val REMINDERS = "reminders"
    const val FAMILY = "family"
    const val SETTINGS = "settings"
    const val UNITS = "units"
    const val EMAIL = "email"
    const val COMMUNICATION = "communication"
    const val SUBSCRIPTION = "subscription"
    const val HELP = "help"
    const val IMPORT = "import"
}

enum class Tab(val label: String, val icon: ImageVector) {
    ACTIVITY("Activity", Icons.Outlined.Flare),
    HISTORY("History", Icons.Outlined.CalendarMonth),
    TRENDS("Trends", Icons.AutoMirrored.Outlined.ShowChart),
    ACCOUNT("Account", Icons.Outlined.VolunteerActivism),
}

@Composable
fun NestlyApp(
    pendingEntryKind: EntryKind? = null,
    onPendingEntryConsumed: () -> Unit = {},
) {
    val container = LocalContext.current.appContainer
    val appViewModel: AppViewModel = viewModel { AppViewModel(container) }
    val authViewModel: com.lifestyle.nestly.ui.auth.AuthViewModel = viewModel { com.lifestyle.nestly.ui.auth.AuthViewModel(container) }
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val authProfiles by authViewModel.profiles.collectAsStateWithLifecycle()
    val authHasFamily by authViewModel.hasFamily.collectAsStateWithLifecycle()
    val dataError by DataErrors.message.collectAsStateWithLifecycle()
    val units by container.preferences.units.collectAsStateWithLifecycle(initialValue = UnitPrefs.DEFAULT)

    when (authState) {
        com.lifestyle.nestly.ui.auth.AuthState.LOADING -> {
            com.lifestyle.nestly.ui.auth.LoadingScreen()
            return
        }
        com.lifestyle.nestly.ui.auth.AuthState.SIGNED_OUT -> {
            com.lifestyle.nestly.ui.auth.AuthScreen(authViewModel)
            return
        }
        com.lifestyle.nestly.ui.auth.AuthState.SIGNED_IN -> {
            val profiles = authProfiles
            if (profiles == null) {
                // No authenticated fetch has completed yet. A reported error
                // means it failed (e.g. offline on cold start) — offer a
                // retry rather than sitting on a spinner forever.
                val error = dataError
                if (error != null) {
                    com.lifestyle.nestly.ui.auth.ErrorScreen(error) { DataErrors.retry() }
                } else {
                    com.lifestyle.nestly.ui.auth.LoadingScreen("Loading your family…")
                }
                return
            }
            if (profiles.isEmpty()) {
                // An empty list is now trustworthy: it can only be emitted by
                // a fetch that succeeded as the signed-in user, never by a
                // failed or pre-session request. Still wait until hasFamily
                // is known so onboarding opens on the right step.
                if (authHasFamily == null) {
                    com.lifestyle.nestly.ui.auth.LoadingScreen("Loading your family…")
                    return
                }
                com.lifestyle.nestly.ui.auth.OnboardingScreen(authViewModel, hasFamily = authHasFamily ?: false)
                return
            }
        }
    }
    val nav = rememberNavController()
    var entry by remember { mutableStateOf<EntryRequest?>(null) }
    val openEntry: (EntryRequest) -> Unit = { entry = it }
    val back: () -> Unit = { nav.popBackStack() }

    LaunchedEffect(pendingEntryKind) {
        val kind = pendingEntryKind ?: return@LaunchedEffect
        entry = EntryRequest.New(kind)
        onPendingEntryConsumed()
    }

    CompositionLocalProvider(LocalUnitPrefs provides units) {
        Box(
            Modifier
                .fillMaxSize()
                .background(NestlyColors.Background),
        ) {
            Column(Modifier.fillMaxSize()) {
            com.lifestyle.nestly.ui.auth.DataErrorBanner()
            NavHost(nav, startDestination = Routes.MAIN) {
                composable(Routes.MAIN) {
                    MainTabs(appViewModel, openEntry, navigate = { nav.navigate(it) })
                }
                composable(Routes.GROWTH) { backStack ->
                    val metric = backStack.arguments?.getString("metric")
                        ?.let { runCatching { com.lifestyle.nestly.data.growth.GrowthMetric.valueOf(it) }.getOrNull() }
                        ?: com.lifestyle.nestly.data.growth.GrowthMetric.WEIGHT
                    com.lifestyle.nestly.ui.growth.GrowthScreen(metric, onBack = back, onOpenEntry = openEntry)
                }
                composable(Routes.EDIT_ACTIVITIES) { EditActivitiesScreen(onBack = back) }
                composable(Routes.MILESTONES) { MilestonesScreen(onBack = back, onOpenEntry = openEntry) }
                composable(Routes.REMINDERS) { RemindersScreen(onBack = back) }
                composable(Routes.FAMILY) { FamilyScreen(appViewModel, onBack = back) }
                composable(Routes.SETTINGS) { SettingsScreen(onBack = back, navigate = { nav.navigate(it) }) }
                composable(Routes.UNITS) { UnitsScreen(onBack = back) }
                composable(Routes.EMAIL) { EmailPasswordScreen(onBack = back) }
                composable(Routes.COMMUNICATION) { CommunicationPrefsScreen(onBack = back) }
                composable(Routes.SUBSCRIPTION) { SubscriptionScreen(onBack = back) }
                composable(Routes.HELP) { HelpScreen(onBack = back) }
                composable(Routes.IMPORT) {
                    val importViewModel = viewModel { ImportViewModel(container, container.appContext.contentResolver) }
                    DetailScreen("Import / Export", onBack = back) { ImportScreen(importViewModel) }
                }
            }
            }
            entry?.let { request ->
                EntrySheetHost(request, onDismiss = { entry = null })
            }
        }
    }
}

@Composable
private fun MainTabs(appViewModel: AppViewModel, openEntry: (EntryRequest) -> Unit, navigate: (String) -> Unit) {
    // Stored as a name so a tab selected before an update (e.g. the removed
    // Guides tab) falls back to Activity instead of failing to restore.
    var tabName by rememberSaveable { mutableStateOf(Tab.ACTIVITY.name) }
    val tab = Tab.entries.find { it.name == tabName } ?: Tab.ACTIVITY
    val selectTab: (Tab) -> Unit = { tabName = it.name }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.ACTIVITY -> ActivityTab(
                    appViewModel = appViewModel,
                    onOpenEntry = openEntry,
                    navigate = navigate,
                    onShowHistory = { selectTab(Tab.HISTORY) },
                )
                Tab.HISTORY -> HistoryTab(appViewModel, onOpenEntry = openEntry, navigate = navigate)
                Tab.TRENDS -> TrendsTab(appViewModel)
                Tab.ACCOUNT -> AccountTab(navigate)
            }
        }
        BottomBar(tab, onSelect = selectTab)
    }
}

@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(NestlyColors.Surface)
            .navigationBarsPadding(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NestlyColors.Divider),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { tab ->
                val color = if (tab == selected) NestlyColors.AccentText else NestlyColors.TextSecondary
                Column(
                    Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(tab) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(tab.icon, tab.label, tint = color, modifier = Modifier.size(28.dp))
                    Text(tab.label, color = color, fontSize = 12.sp)
                }
            }
        }
    }
}
