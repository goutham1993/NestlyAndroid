package com.lifestyle.nestly.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.model.CaregiverEntity
import com.lifestyle.nestly.data.model.ProfileEntity
import com.lifestyle.nestly.data.model.canRemoveCaregiver
import kotlinx.coroutines.launch
import com.lifestyle.nestly.data.model.ReminderKind
import com.lifestyle.nestly.data.model.Sex
import com.lifestyle.nestly.ui.AppViewModel
import com.lifestyle.nestly.ui.Routes
import com.lifestyle.nestly.ui.components.DateTimePickerDialog
import com.lifestyle.nestly.ui.components.DurationInputDialog
import com.lifestyle.nestly.ui.components.DetailScreen
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.FormRow
import com.lifestyle.nestly.data.units.UnitPrefs
import com.lifestyle.nestly.data.units.Units
import com.lifestyle.nestly.ui.components.MenuRow
import com.lifestyle.nestly.ui.components.NestlySwitch
import com.lifestyle.nestly.ui.components.NumberInputDialog
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.components.SectionHeading
import com.lifestyle.nestly.ui.components.SegmentedToggle
import com.lifestyle.nestly.ui.components.TextInputDialog
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun AccountTab(navigate: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Account", style = NestlyType.Display, modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 8.dp))
        MenuRow("Family", onClick = { navigate(Routes.FAMILY) })
        MenuRow("Settings", onClick = { navigate(Routes.SETTINGS) })
        MenuRow("Subscription", onClick = { navigate(Routes.SUBSCRIPTION) })
        MenuRow("Help", onClick = { navigate(Routes.HELP) })
        SectionHeading("Data")
        MenuRow("Import / Export CSV", serif = false, onClick = { navigate(Routes.IMPORT) })
        SectionHeading("Share & Social")
        MenuRow("Share Nestly With Friends", serif = false, chevron = false, onClick = {})
        MenuRow("Rate Nestly on the Play Store", serif = false, chevron = false, onClick = {})
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun FamilyScreen(appViewModel: AppViewModel, onBack: () -> Unit, navigate: (String) -> Unit) {
    val profiles by appViewModel.profiles.collectAsStateWithLifecycle()
    val caregivers by appViewModel.caregivers.collectAsStateWithLifecycle()
    val currentCaregiverId by appViewModel.currentCaregiverId.collectAsStateWithLifecycle()
    val signedIn by appViewModel.signedInCaregiver.collectAsStateWithLifecycle()
    var addingCaregiver by remember { mutableStateOf(false) }
    var pendingRemoval by remember { mutableStateOf<CaregiverEntity?>(null) }
    var removing by remember { mutableStateOf(false) }
    var removeError by remember { mutableStateOf<String?>(null) }

    DetailScreen("Family", onBack) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            SectionHeading("Children")
            profiles.orEmpty().forEach { child ->
                MenuRow(
                    child.name,
                    subtitle = listOfNotNull(
                        child.birthDate?.let { Format.age(it) },
                        "Tap to edit",
                    ).joinToString(" · "),
                    onClick = { navigate(Routes.childEdit(child.id)) },
                )
            }
            MenuRow(
                "Add child",
                serif = false,
                chevron = false,
                onClick = { navigate(Routes.childNew(caregivers.isEmpty())) },
            )
            SectionHeading("Caregivers")
            caregivers.forEach { caregiver ->
                val subtitle = if (caregiver.id == currentCaregiverId) "Your Profile" else "Tap if this is you"
                val canRemove = canRemoveCaregiver(signedIn, caregiver, caregivers)
                if (canRemove) {
                    CaregiverRow(
                        name = caregiver.name,
                        subtitle = subtitle,
                        action = if (caregiver.id == signedIn?.id) "Leave" else "Remove",
                        onSelect = { appViewModel.selectCaregiver(caregiver.id) },
                        onRemove = { removeError = null; pendingRemoval = caregiver },
                    )
                } else {
                    MenuRow(
                        caregiver.name,
                        subtitle = subtitle,
                        onClick = { appViewModel.selectCaregiver(caregiver.id) },
                    )
                }
            }
            removeError?.let {
                Text(it, style = NestlyType.Small, color = NestlyColors.Danger, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            }
            MenuRow("Add caregiver", serif = false, chevron = false, onClick = { addingCaregiver = true })
            SectionHeading("Invite to this family")
            InviteSection(appViewModel)
            Text(
                "Entries you log are recorded as added by your profile. Tap a child to edit their details.",
                style = NestlyType.Small,
                color = NestlyColors.TextSecondary,
                modifier = Modifier.padding(20.dp),
            )
        }
    }
    if (addingCaregiver) {
        TextInputDialog(
            title = "Add caregiver",
            initial = "",
            onDismiss = { addingCaregiver = false },
            onConfirm = { name ->
                addingCaregiver = false
                if (name.isNotBlank()) appViewModel.addCaregiver(name)
            },
        )
    }
    pendingRemoval?.let { caregiver ->
        val leaving = caregiver.id == signedIn?.id
        AlertDialog(
            onDismissRequest = { if (!removing) pendingRemoval = null },
            containerColor = NestlyColors.Surface,
            title = {
                Text(
                    if (leaving) "Leave this family?" else "Remove ${caregiver.name}?",
                    style = NestlyType.RowLabel,
                )
            },
            text = {
                Text(
                    when {
                        leaving -> "You'll lose access to this family's children and entries until someone invites you back."
                        caregiver.userId == null -> "Past entries stay attributed to ${caregiver.name}. You can add them again later."
                        else -> "${caregiver.name} will lose access to this family. Past entries stay attributed to them. Adding this name again, or a new invite, brings them back."
                    },
                    style = NestlyType.Body,
                    color = NestlyColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !removing,
                    onClick = {
                        removing = true
                        appViewModel.removeCaregiver(
                            caregiver.id,
                            onDone = {
                                removing = false
                                pendingRemoval = null
                            },
                            onError = { message ->
                                removing = false
                                pendingRemoval = null
                                removeError = message
                            },
                        )
                    },
                ) {
                    Text(
                        when {
                            removing && leaving -> "Leaving…"
                            removing -> "Removing…"
                            leaving -> "Leave"
                            else -> "Remove"
                        },
                        color = NestlyColors.Danger,
                        style = NestlyType.Button,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }, enabled = !removing) {
                    Text("Cancel", color = NestlyColors.AccentText, style = NestlyType.Button)
                }
            },
        )
    }
}

@Composable
private fun CaregiverRow(
    name: String,
    subtitle: String,
    action: String,
    onSelect: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(Modifier.background(NestlyColors.Surface)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 82.dp)
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onSelect)
                    .padding(vertical = 12.dp),
            ) {
                Text(name, style = NestlyType.RowLabel)
                Text(subtitle, style = NestlyType.Small, color = NestlyColors.TextSecondary)
            }
            Text(
                action,
                style = NestlyType.Button,
                color = NestlyColors.Danger,
                modifier = Modifier
                    .clickable(onClick = onRemove)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
        RowDivider()
    }
}

@Composable
private fun InviteSection(appViewModel: AppViewModel) {
    val profiles by appViewModel.profiles.collectAsStateWithLifecycle()
    val familyId = profiles?.firstOrNull()?.familyId
    var inviteCode by remember { mutableStateOf<String?>(null) }
    var inviteError by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val container = LocalContext.current.appContainer
    MenuRow(
        "Create invite code",
        subtitle = inviteCode?.let { "Code: $it — share it with another caregiver" } ?: "They enter it when joining",
        serif = false,
        chevron = false,
        onClick = {
            val fid = familyId ?: return@MenuRow
            scope.launch {
                runCatching { container.family.createInvite(fid) }
                    .onSuccess { inviteCode = it.code; inviteError = null }
                    .onFailure { inviteError = it.message }
            }
        },
    )
    inviteError?.let {
        Text(it, style = NestlyType.Small, color = NestlyColors.Danger, modifier = Modifier.padding(horizontal = 20.dp))
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, navigate: (String) -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val defaultBottleMl by container.preferences.defaultBottleMl.collectAsStateWithLifecycle(initialValue = null)
    val defaultFeedInterval by container.preferences.defaultFeedIntervalMinutes.collectAsStateWithLifecycle(initialValue = 180)
    val unitPrefs by container.preferences.units.collectAsStateWithLifecycle(initialValue = UnitPrefs.DEFAULT)
    var editingDefaultAmount by remember { mutableStateOf(false) }
    var editingFeedInterval by remember { mutableStateOf(false) }
    var confirmingClear by remember { mutableStateOf(false) }
    var clearing by remember { mutableStateOf(false) }
    var clearError by remember { mutableStateOf<String?>(null) }
    DetailScreen("Settings", onBack, action = "Log Out", onAction = {
        scope.launch {
            runCatching { container.auth.signOut() }
            container.realtime.unsubscribe()
        }
    }) {
        MenuRow("Email & password", onClick = { navigate(Routes.EMAIL) })
        MenuRow("Units", onClick = { navigate(Routes.UNITS) })
        MenuRow("Communication preferences", onClick = { navigate(Routes.COMMUNICATION) })

        SectionHeading("Nightstand")
        MenuRow(
            "Nightstand Mode",
            subtitle = "Show the clock and next feed while this device is charging",
            serif = false,
            onClick = {
                // Android only lets the user pick the active screen saver;
                // deep-link there, falling back to Display settings.
                val dreamSettings = android.content.Intent(android.provider.Settings.ACTION_DREAM_SETTINGS)
                val fallback = android.content.Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)
                runCatching { context.startActivity(dreamSettings) }
                    .onFailure { runCatching { context.startActivity(fallback) } }
            },
        )
        Text(
            "Select Nestly Nightstand as your screen saver, then set it to start while charging. " +
                "From it you can log a feed with your default bottle amount in one tap, without unlocking.",
            style = NestlyType.Small,
            color = NestlyColors.TextSecondary,
            modifier = Modifier.padding(20.dp),
        )

        SectionHeading("Feeding")
        MenuRow(
            "Default bottle amount",
            subtitle = if (defaultBottleMl != null) "Pre-fills new bottle & combo feeds" else "Not set — new bottle feeds start empty",
            value = defaultBottleMl?.let { Format.volume(it, unitPrefs) },
            serif = false,
            onClick = { editingDefaultAmount = true },
        )
        MenuRow(
            "Default feed interval",
            subtitle = "When the next feed is due",
            value = feedIntervalLabel(defaultFeedInterval),
            serif = false,
            onClick = { editingFeedInterval = true },
        )
        Text(
            "Bottle amount is used by Nightstand's Log Feed button. Feed interval sets when the next feed is due, and updates your feed reminders.",
            style = NestlyType.Small,
            color = NestlyColors.TextSecondary,
            modifier = Modifier.padding(20.dp),
        )

        // TEST ONLY: wipe this family's Supabase data to restart testing
        // (e.g. re-import from Nara). Debug builds only; remove before release.
        if (com.lifestyle.nestly.BuildConfig.DEBUG) {
            SectionHeading("Testing")
            MenuRow(
                "Clear all data",
                subtitle = "Deletes this family's children, activities, reminders and photos from Supabase",
                serif = false,
                chevron = false,
                color = NestlyColors.Danger,
                onClick = { clearError = null; confirmingClear = true },
            )
            clearError?.let {
                Text(it, style = NestlyType.Small, color = NestlyColors.Danger, modifier = Modifier.padding(20.dp))
            }
        }
    }

    if (editingDefaultAmount) {
        NumberInputDialog(
            title = "Default bottle amount",
            unit = Units.volumeLabel(unitPrefs.volume),
            initial = defaultBottleMl?.let { Units.mlToDisplay(it, unitPrefs.volume) },
            onDismiss = { editingDefaultAmount = false },
        ) { value ->
            editingDefaultAmount = false
            scope.launch {
                container.preferences.setDefaultBottleMl(value?.let { Units.displayToMl(it, unitPrefs.volume) })
            }
        }
    }

    if (editingFeedInterval) {
        DurationInputDialog(
            title = "Default feed interval",
            initialSec = defaultFeedInterval * 60L,
            showHours = true,
            onDismiss = { editingFeedInterval = false },
            onConfirm = { seconds ->
                editingFeedInterval = false
                val minutes = (seconds / 60).toInt().coerceAtLeast(1)
                scope.launch { applyDefaultFeedInterval(container, context, minutes) }
            },
        )
    }

    if (confirmingClear) {
        AlertDialog(
            onDismissRequest = { if (!clearing) confirmingClear = false },
            containerColor = NestlyColors.Surface,
            title = { Text("Clear all data?", style = NestlyType.RowLabel) },
            text = {
                Text(
                    "This permanently deletes this family's children, activities, reminders, invites and photos " +
                        "from Supabase for every caregiver. This cannot be undone. You'll return to setup and can re-import from Nara.",
                    style = NestlyType.Small,
                    color = NestlyColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !clearing,
                    onClick = {
                        clearing = true
                        scope.launch {
                            val outcome = runCatching {
                                val familyId = container.family.currentFamilyId()
                                if (familyId != null) {
                                    runCatching { container.photos.clearFamilyPhotos(familyId) }
                                    runCatching { container.photos.clearFamilyProfilePhotos(familyId) }
                                }
                                container.family.clearCurrentFamilyData()
                            }
                            clearing = false
                            confirmingClear = false
                            outcome.onFailure {
                                clearError = "Couldn't clear data: ${it.message ?: "check your connection and try again."}"
                            }
                            // On success the profiles flow empties and the app
                            // returns to onboarding automatically.
                        }
                    },
                ) {
                    Text(if (clearing) "Clearing…" else "Clear everything", color = NestlyColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingClear = false }, enabled = !clearing) {
                    Text("Cancel", color = NestlyColors.AccentText)
                }
            },
        )
    }
}

@Composable
fun EmailPasswordScreen(onBack: () -> Unit) {
    val container = LocalContext.current.appContainer
    val email = container.auth.currentEmail
    DetailScreen("Email & password", onBack) {
        MenuRow("Email", subtitle = email ?: "Signed in", chevron = false)
        Text(
            "Your account is used to share this family's data with other caregivers through Supabase. " +
                "Data is stored online only; there is no local copy on this device.",
            style = NestlyType.Small,
            color = NestlyColors.TextSecondary,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Composable
fun CommunicationPrefsScreen(onBack: () -> Unit) {
    var reminders by remember { mutableStateOf(true) }
    var updates by remember { mutableStateOf(true) }
    DetailScreen("Communication preferences", onBack) {
        PrefRow("Reminders", "Get alerts for timers you set.", reminders) { reminders = it }
        PrefRow("Product updates", "Stay up to date on new features and more.", updates) { updates = it }
    }
}

@Composable
private fun PrefRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(NestlyColors.Surface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = NestlyType.RowLabel)
            Text(subtitle, style = NestlyType.Small, color = NestlyColors.TextSecondary)
        }
        NestlySwitch(checked, onChange)
    }
    RowDivider()
}

@Composable
fun SubscriptionScreen(onBack: () -> Unit) {
    DetailScreen("Subscription", onBack) {
        MenuRow("Status", value = "Free", chevron = false)
        SectionHeading("Manage")
        MenuRow("Subscribe", serif = false, chevron = false, onClick = {})
        Text(
            "Nestly is free while in development.",
            style = NestlyType.Small,
            color = NestlyColors.TextSecondary,
            modifier = Modifier.padding(20.dp),
        )
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        "v${info.versionName} (${info.longVersionCode})"
    }
    DetailScreen("Help", onBack) {
        MenuRow("Contact Us", value = "help@nestly.app", chevron = false)
        MenuRow("Baby tracker FAQs", onClick = {})
        MenuRow("Version", value = version, chevron = false)
        MenuRow("Legal", onClick = {})
        Spacer(Modifier.height(20.dp))
        MenuRow("Advanced Settings", onClick = {})
    }
}

private fun feedIntervalLabel(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours > 0 && rest > 0 -> "${hours}h ${rest}m"
        hours > 0 -> "${hours}h"
        else -> "${rest}m"
    }
}

/** Saves the countdown interval and copies it onto existing feed reminders. */
private suspend fun applyDefaultFeedInterval(
    container: com.lifestyle.nestly.AppContainer,
    context: android.content.Context,
    minutes: Int,
) {
    container.preferences.setDefaultFeedIntervalMinutes(minutes)
    val reminders = runCatching { container.reminders.fetchAll() }.getOrElse {
        android.widget.Toast.makeText(
            context,
            "Couldn't update every feed reminder. Check your connection and try again.",
            android.widget.Toast.LENGTH_LONG,
        ).show()
        return
    }
    var failed = false
    for (reminder in reminders) {
        if (reminder.kind != ReminderKind.FEED || reminder.intervalMinutes == minutes) continue
        val saved = runCatching {
            container.reminders.setInterval(ReminderKind.FEED, reminder.familyId, reminder.profileId, minutes)
        }.getOrNull()
        if (saved == null) {
            failed = true
        } else if (saved.enabled) {
            runCatching { container.scheduler.schedule(saved) }
        }
    }
    if (failed) {
        android.widget.Toast.makeText(
            context,
            "Couldn't update every feed reminder. Check your connection and try again.",
            android.widget.Toast.LENGTH_LONG,
        ).show()
    }
}
