package com.lifestyle.nestly.ui.photo

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.photo.PhotoPrompt
import com.lifestyle.nestly.ui.AppViewModel
import com.lifestyle.nestly.ui.Routes
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Asks for a new shared photo at each month of the first year, then each birthday. */
@Composable
fun PhotoPromptHost(appViewModel: AppViewModel, navigate: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = context.appContainer.preferences
    val scope = rememberCoroutineScope()
    val profiles by appViewModel.profiles.collectAsStateWithLifecycle()
    val selected by appViewModel.profile.collectAsStateWithLifecycle()
    val dismissed by prefs.dismissedPhotoPrompts.collectAsStateWithLifecycle(initialValue = emptyMap())
    val suppressed by appViewModel.photoPromptSuppressed.collectAsStateWithLifecycle()
    val children = profiles ?: return
    if (suppressed || children.isEmpty()) return

    val zone = ZoneId.systemDefault()
    val today = LocalDate.now()
    val due = PhotoPrompt.choose(
        children.map { child ->
            PhotoPrompt.Child(
                id = child.id,
                name = child.name,
                birthDate = child.birthDate,
                photoPath = child.photoPath,
                photoUpdatedOn = child.photoUpdatedAt?.let {
                    Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                },
                dismissedMilestoneId = dismissed[child.id],
            )
        },
        selectedId = selected?.id,
        today = today,
    ) ?: return

    AlertDialog(
        onDismissRequest = {
            scope.launch { prefs.dismissPhotoPrompt(due.profileId, due.milestone.id) }
        },
        containerColor = NestlyColors.Surface,
        title = { Text(due.title, style = NestlyType.RowLabel) },
        text = { Text(due.message, style = NestlyType.Body, color = NestlyColors.TextSecondary) },
        confirmButton = {
            TextButton(onClick = {
                appViewModel.hidePhotoPromptForSession()
                navigate(Routes.childEdit(due.profileId))
            }) {
                Text("Add Photo", color = NestlyColors.AccentText)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                scope.launch { prefs.dismissPhotoPrompt(due.profileId, due.milestone.id) }
            }) {
                Text("Not Now", color = NestlyColors.AccentText)
            }
        },
    )
}
