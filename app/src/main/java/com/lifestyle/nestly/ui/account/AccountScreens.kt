package com.lifestyle.nestly.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.lifestyle.nestly.data.model.ProfileEntity
import kotlinx.coroutines.launch
import com.lifestyle.nestly.data.model.Sex
import com.lifestyle.nestly.ui.AppViewModel
import com.lifestyle.nestly.ui.Routes
import com.lifestyle.nestly.ui.activity.AddChildDialog
import com.lifestyle.nestly.ui.components.DateTimePickerDialog
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
import java.time.Period
import java.time.ZoneId

@Composable
fun AccountTab(navigate: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Account", style = NestlyType.Display, modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 20.dp))
        RowDivider()
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

/** "4m 3w 6d" */
fun shortAge(birthDate: LocalDate, today: LocalDate = LocalDate.now()): String {
    val period = Period.between(birthDate, today)
    val months = period.toTotalMonths()
    val weeks = period.days / 7
    val days = period.days % 7
    return "${months}m ${weeks}w ${days}d"
}

@Composable
fun FamilyScreen(appViewModel: AppViewModel, onBack: () -> Unit) {
    val profiles by appViewModel.profiles.collectAsStateWithLifecycle()
    val caregivers by appViewModel.caregivers.collectAsStateWithLifecycle()
    val currentCaregiverId by appViewModel.currentCaregiverId.collectAsStateWithLifecycle()
    var addingChild by remember { mutableStateOf(false) }
    var editingChild by remember { mutableStateOf<ProfileEntity?>(null) }
    var addingCaregiver by remember { mutableStateOf(false) }

    DetailScreen("Family", onBack) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            SectionHeading("Children")
            profiles.orEmpty().forEach { child ->
                MenuRow(
                    child.name,
                    subtitle = listOfNotNull(
                        child.birthDate?.let { "Age ${shortAge(it)}" },
                        "Tap to edit",
                    ).joinToString(" · "),
                    onClick = { editingChild = child },
                )
            }
            MenuRow("Add child", serif = false, chevron = false, onClick = { addingChild = true })
            SectionHeading("Caregivers")
            caregivers.forEach { caregiver ->
                MenuRow(
                    caregiver.name,
                    subtitle = if (caregiver.id == currentCaregiverId) "Your Profile" else "Tap if this is you",
                    onClick = { appViewModel.selectCaregiver(caregiver.id) },
                )
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
    if (addingChild) {
        AddChildDialog(
            askCaregiverName = caregivers.isEmpty(),
            onDismiss = { addingChild = false },
            onConfirm = { name, sex, birthDate, caregiverName ->
                appViewModel.addChild(name, sex, birthDate, caregiverName)
                addingChild = false
            },
        )
    }
    editingChild?.let { child ->
        EditChildDialog(
            initial = child,
            onDismiss = { editingChild = null },
            onConfirm = { name, sex, birthDate ->
                appViewModel.updateChild(child.id, name, sex, birthDate)
                appViewModel.selectProfile(child.id)
                editingChild = null
            },
        )
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
private fun EditChildDialog(
    initial: ProfileEntity,
    onDismiss: () -> Unit,
    onConfirm: (name: String, sex: Sex?, birthDate: LocalDate?) -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var sex by remember { mutableStateOf(initial.sex) }
    var birthDate by remember { mutableStateOf(initial.birthDate) }
    var pickingDate by remember { mutableStateOf(false) }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NestlyColors.AccentText,
        unfocusedBorderColor = NestlyColors.Divider,
        cursorColor = NestlyColors.AccentText,
        focusedLabelColor = NestlyColors.AccentText,
    )
    val valid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NestlyColors.Surface,
        title = { Text("Edit child", style = NestlyType.RowLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text("First Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = fieldColors,
                )
                SegmentedToggle(
                    options = listOf("Boy", "Girl"),
                    selected = when (sex) {
                        Sex.MALE -> 0
                        Sex.FEMALE -> 1
                        else -> -1
                    },
                    onSelect = { sex = if (it == 0) Sex.MALE else Sex.FEMALE },
                )
                FormRow(
                    "Birthdate",
                    value = birthDate?.let(Format::date),
                    action = if (birthDate == null) "Add" else null,
                    onClick = { pickingDate = true },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), sex, birthDate) }, enabled = valid) {
                Text("Save", color = if (valid) NestlyColors.AccentText else NestlyColors.TextMuted)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = NestlyColors.AccentText) } },
    )

    if (pickingDate) {
        val zone = ZoneId.systemDefault()
        DateTimePickerDialog(
            initial = (birthDate ?: LocalDate.now()).atStartOfDay(zone).toInstant().toEpochMilli(),
            dateOnly = true,
            onDismiss = { pickingDate = false },
            onConfirm = {
                birthDate = Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                pickingDate = false
            },
        )
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit, navigate: (String) -> Unit) {
    val container = LocalContext.current.appContainer
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val defaultBottleMl by container.preferences.defaultBottleMl.collectAsStateWithLifecycle(initialValue = null)
    val unitPrefs by container.preferences.units.collectAsStateWithLifecycle(initialValue = UnitPrefs.DEFAULT)
    var editingDefaultAmount by remember { mutableStateOf(false) }
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

        SectionHeading("Feeding")
        MenuRow(
            "Default bottle amount",
            subtitle = if (defaultBottleMl != null) "Pre-fills new bottle & combo feeds" else "Not set — new bottle feeds start empty",
            value = defaultBottleMl?.let { Format.volume(it, unitPrefs) },
            serif = false,
            onClick = { editingDefaultAmount = true },
        )
        Text(
            "Tap to change it. Leave the amount empty and tap OK to turn the default off.",
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
