package com.lifestyle.nestly.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.lifestyle.nestly.ui.appointments.AppointmentNotesFormat
import com.lifestyle.nestly.ui.appointments.NoteLineFormat
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Free-form notes with two states. Reading: the note is rendered once, with
 * ☐ / ☑ lines as tappable checkboxes; tapping any other line (or Edit) opens
 * the editor at that line. Editing: a plain text field with a Bullet /
 * Numbered / Checkbox toolbar and Enter continuing the current list.
 * Meant for a vertically scrolling screen that applies imePadding.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FormattedNotesEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    hint: String,
    placeholder: String,
    minLines: Int = 6,
    focusedMinLines: Int = 10,
) {
    var editing by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    var pendingFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val showEditor = editing || value.text.isBlank()

    fun startEditing(cursor: Int) {
        onValueChange(value.copy(selection = TextRange(cursor.coerceIn(0, value.text.length))))
        editing = true
        pendingFocus = true
    }

    fun finishEditing() {
        focusManager.clearFocus()
        editing = false
    }

    fun applyFormat(format: NoteLineFormat) {
        val result = AppointmentNotesFormat.formatCurrentLine(value.text, value.selection.start, format)
        onValueChange(TextFieldValue(result.text, TextRange(result.cursor)))
        // The toolbar buttons take focus on tap; hand it straight back so the
        // keyboard stays up and the user can keep typing on the formatted line.
        focusRequester.requestFocus()
    }

    fun onTextChange(newValue: TextFieldValue) {
        // A lone inserted newline continues a bullet / numbered / checkbox
        // line onto the new line; every other edit is accepted untouched.
        val continued = AppointmentNotesFormat.continueListOnEnter(
            oldText = value.text,
            oldSelectionStart = value.selection.start,
            oldSelectionEnd = value.selection.end,
            newText = newValue.text,
            newCursor = newValue.selection.start,
        )
        onValueChange(
            if (continued != null && newValue.selection.collapsed) {
                TextFieldValue(continued.text, TextRange(continued.cursor))
            } else {
                newValue
            },
        )
    }

    LaunchedEffect(showEditor, pendingFocus) {
        if (showEditor && pendingFocus) {
            pendingFocus = false
            focusRequester.requestFocus()
        }
    }

    if (showEditor) {
        Text(
            hint,
            style = NestlyType.Small,
            color = NestlyColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        Row(
            Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { applyFormat(NoteLineFormat.BULLET) }) {
                Text("• Bullet", color = NestlyColors.AccentText, style = NestlyType.Button)
            }
            TextButton(onClick = { applyFormat(NoteLineFormat.NUMBERED) }) {
                Text("1. Numbered", color = NestlyColors.AccentText, style = NestlyType.Button)
            }
            TextButton(onClick = { applyFormat(NoteLineFormat.CHECKBOX) }) {
                Text("☐ Checkbox", color = NestlyColors.AccentText, style = NestlyType.Button)
            }
            Spacer(Modifier.weight(1f))
            if (value.text.isNotBlank()) {
                TextButton(onClick = ::finishEditing) {
                    Text("Done", color = NestlyColors.AccentText, style = NestlyType.Button)
                }
            }
        }
        Column(
            Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .bringIntoViewRequester(bringIntoViewRequester),
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = ::onTextChange,
                placeholder = { Text(placeholder, color = NestlyColors.TextMuted) },
                // While typing the field grows and is scrolled fully above
                // the keyboard (see DetailScreen's imePadding), so the line
                // being written is never hidden behind it.
                minLines = if (focused) focusedMinLines else minLines,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NestlyColors.AccentText,
                    unfocusedBorderColor = NestlyColors.Divider,
                    cursorColor = NestlyColors.AccentText,
                    focusedLabelColor = NestlyColors.AccentText,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        val wasFocused = focused
                        focused = state.isFocused
                        if (state.isFocused && !wasFocused) {
                            scope.launch {
                                // Wait for the keyboard to start opening so the
                                // (now ime-shrunk) viewport is the one we scroll in.
                                delay(250)
                                bringIntoViewRequester.bringIntoView()
                            }
                        } else if (!state.isFocused && wasFocused) {
                            scope.launch {
                                // Toolbar taps drop focus only momentarily; leave
                                // editing only once focus has really moved away.
                                delay(200)
                                if (!focused) editing = false
                            }
                        }
                    },
            )
        }
    } else {
        NotesPreview(
            text = value.text,
            onToggle = { lineIndex ->
                onValueChange(
                    value.copy(text = AppointmentNotesFormat.toggleCheckbox(value.text, lineIndex)),
                )
            },
            onEditAt = ::startEditing,
        )
    }
}

@Composable
private fun NotesPreview(
    text: String,
    onToggle: (lineIndex: Int) -> Unit,
    onEditAt: (cursor: Int) -> Unit,
) {
    val lines = text.lines()
    val checkboxItems = AppointmentNotesFormat.checkboxItems(text).associateBy { it.lineIndex }
    val lineEnds = lines.runningFold(-1) { end, line -> end + line.length + 1 }.drop(1)

    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, NestlyColors.Divider, RoundedCornerShape(4.dp))
                .padding(vertical = 8.dp),
        ) {
            lines.forEachIndexed { index, line ->
                val item = checkboxItems[index]
                when {
                    item != null -> Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable { onToggle(index) }
                            .padding(start = 4.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = item.checked,
                            onCheckedChange = { onToggle(index) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = NestlyColors.Accent,
                                uncheckedColor = NestlyColors.TextMuted,
                                checkmarkColor = NestlyColors.Surface,
                            ),
                        )
                        Text(
                            item.text,
                            style = NestlyType.Body,
                            color = if (item.checked) NestlyColors.TextSecondary else NestlyColors.TextPrimary,
                            textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    line.isBlank() -> Spacer(Modifier.height(8.dp))
                    else -> Text(
                        line,
                        style = NestlyType.Body,
                        color = NestlyColors.TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditAt(lineEnds[index]) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (checkboxItems.isNotEmpty()) {
                Text(
                    "${checkboxItems.values.count { it.checked }} of ${checkboxItems.size} done",
                    style = NestlyType.Small,
                    color = NestlyColors.TextSecondary,
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onEditAt(text.length) }) {
                Text("Edit", color = NestlyColors.AccentText, style = NestlyType.Button)
            }
        }
    }
}
