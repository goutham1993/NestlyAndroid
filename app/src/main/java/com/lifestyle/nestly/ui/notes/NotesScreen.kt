package com.lifestyle.nestly.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.model.NoteEntity
import com.lifestyle.nestly.ui.appointments.AppointmentNotesFormat
import com.lifestyle.nestly.ui.components.DetailScreen
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.components.FormattedNotesEditor
import com.lifestyle.nestly.ui.components.RowDivider
import com.lifestyle.nestly.ui.components.SectionHeading
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType

@Composable
fun NotesScreen(
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    onAddNote: () -> Unit,
) {
    val container = LocalContext.current.appContainer
    val viewModel: NotesViewModel = viewModel { NotesViewModel(container) }
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()

    DetailScreen("Notes", onBack, action = "Add", onAction = onAddNote) {
        val loaded = notes
        if (loaded == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NestlyColors.AccentText)
            }
            return@DetailScreen
        }
        LazyColumn(Modifier.weight(1f)) {
            if (loaded.isEmpty()) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "No notes yet",
                            style = NestlyType.SectionTitle,
                            color = NestlyColors.TextPrimary,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Jot down anything worth remembering — routines, to-dos, packing lists. " +
                                "Everyone in your family can see and edit notes.",
                            style = NestlyType.Body,
                            color = NestlyColors.TextSecondary,
                        )
                        Spacer(Modifier.height(24.dp))
                        TextButton(onClick = onAddNote) {
                            Text("Add note", color = NestlyColors.AccentText, style = NestlyType.Button)
                        }
                    }
                }
            }
            items(loaded, key = { it.id }) { note ->
                NoteRow(note, now) { onOpenNote(note.id) }
            }
        }
    }
}

@Composable
private fun NoteRow(note: NoteEntity, now: Long, onClick: () -> Unit) {
    Column {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .heightIn(min = 72.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                note.title.ifBlank { "Untitled" },
                style = NestlyType.RowLabel,
                color = if (note.title.isBlank()) NestlyColors.TextSecondary else NestlyColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            note.body.lines().firstOrNull { it.isNotBlank() }?.let { preview ->
                Text(
                    preview.trim(),
                    style = NestlyType.Small,
                    color = NestlyColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val checkboxItems = AppointmentNotesFormat.checkboxItems(note.body)
            val progress = checkboxItems.takeIf { it.isNotEmpty() }
                ?.let { "${it.count { item -> item.checked }}/${it.size} done" }
            Text(
                listOfNotNull("Edited ${Format.relativeDay(note.updatedAt, now)}", progress).joinToString(" · "),
                style = NestlyType.Small,
                color = NestlyColors.TextMuted,
            )
        }
        RowDivider()
    }
}

@Composable
fun NoteDetailScreen(noteId: String?, onBack: () -> Unit) {
    val container = LocalContext.current.appContainer
    val viewModel: NotesViewModel = viewModel { NotesViewModel(container) }

    val existingFlow = remember(noteId) { noteId?.let { viewModel.note(it) } }
    val existing by existingFlow?.collectAsState(initial = null) ?: remember { mutableStateOf(null) }

    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf(TextFieldValue("")) }
    // The version the editor opened; saves are checked against it so another
    // caregiver's edit is never silently overwritten.
    var loaded by remember { mutableStateOf<NoteEntity?>(null) }
    var initialized by remember { mutableStateOf(noteId == null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmingDelete by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    fun load(note: NoteEntity) {
        title = note.title
        body = TextFieldValue(note.body)
        loaded = note
        initialized = true
    }

    LaunchedEffect(existing?.id) {
        val note = existing ?: return@LaunchedEffect
        if (!initialized) load(note)
    }

    fun save() {
        if (title.isBlank() && body.text.isBlank()) {
            error = "Add a header or some text."
            return
        }
        error = null
        viewModel.save(
            loaded = loaded,
            title = title,
            body = body.text,
            onDone = onBack,
            onConflict = { latest, message ->
                latest?.let(::load)
                error = message
            },
            onError = { error = it },
        )
    }

    DetailScreen(
        title = if (noteId == null) "New Note" else "Note",
        onBack = onBack,
        action = "Save",
        onAction = ::save,
    ) {
        if (noteId != null && !initialized) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NestlyColors.AccentText)
            }
            return@DetailScreen
        }
        Column(Modifier.verticalScroll(scrollState)) {
            error?.let { message ->
                Text(
                    message,
                    style = NestlyType.Small,
                    color = NestlyColors.Danger,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Header") },
                    placeholder = { Text("e.g. Bedtime routine", color = NestlyColors.TextMuted) },
                    singleLine = true,
                    textStyle = NestlyType.RowLabel,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NestlyColors.AccentText,
                        unfocusedBorderColor = NestlyColors.Divider,
                        cursorColor = NestlyColors.AccentText,
                        focusedLabelColor = NestlyColors.AccentText,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionHeading("Note")
            FormattedNotesEditor(
                value = body,
                onValueChange = { body = it },
                hint = "Write freely. Use the buttons below to start a bullet, numbered or " +
                    "checkbox line; pressing Enter continues it onto the next line.",
                placeholder = "• Bath at 7pm\n1. Feed\n☐ Buy more diapers",
                minLines = 8,
                focusedMinLines = 12,
            )
            RowDivider()

            if (noteId != null) {
                Spacer(Modifier.height(24.dp))
                TextButton(
                    onClick = { confirmingDelete = true },
                    modifier = Modifier.padding(horizontal = 20.dp),
                ) {
                    Text("Delete note", color = NestlyColors.Danger, style = NestlyType.Button)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            containerColor = NestlyColors.Surface,
            title = { Text("Delete note?", style = NestlyType.RowLabel) },
            text = {
                Text(
                    "This removes the note for everyone in your family.",
                    style = NestlyType.Body,
                    color = NestlyColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    loaded?.let { note ->
                        viewModel.delete(note, onDone = onBack, onError = { error = it })
                    }
                }) {
                    Text("Delete", color = NestlyColors.Danger, style = NestlyType.Button)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text("Cancel", color = NestlyColors.AccentText, style = NestlyType.Button)
                }
            },
        )
    }
}
