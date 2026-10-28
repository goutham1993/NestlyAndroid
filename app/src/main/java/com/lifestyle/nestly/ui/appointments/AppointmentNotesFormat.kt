package com.lifestyle.nestly.ui.appointments

/** Line formats available in the free-form notes & questions editor. */
enum class NoteLineFormat { BULLET, NUMBERED, CHECKBOX }

/**
 * Pure-text helpers for the free-form appointment notes. Checkbox state lives
 * in the text itself ("☐ ask about fever" / "☑ ask about fever") so notes stay
 * plain text in the database while still offering tappable checklist items.
 */
object AppointmentNotesFormat {
    const val BULLET_PREFIX = "• "
    const val UNCHECKED_PREFIX = "☐ "
    const val CHECKED_PREFIX = "☑ "

    private val numberedRegex = Regex("""^(\d+)\.\s+""")
    private val anyPrefixRegex = Regex("""^(•\s+|☐\s+|☑\s+|\d+\.\s+)""")

    data class CheckboxItem(val lineIndex: Int, val text: String, val checked: Boolean)

    data class FormatResult(val text: String, val cursor: Int)

    fun checkboxItems(text: String): List<CheckboxItem> =
        text.lines().mapIndexedNotNull { index, line ->
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith(UNCHECKED_PREFIX) ->
                    CheckboxItem(index, trimmed.removePrefix(UNCHECKED_PREFIX), checked = false)
                trimmed.startsWith(CHECKED_PREFIX) ->
                    CheckboxItem(index, trimmed.removePrefix(CHECKED_PREFIX), checked = true)
                else -> null
            }
        }

    /** Flips ☐ ↔ ☑ on the given line; returns the text unchanged if that line isn't a checkbox. */
    fun toggleCheckbox(text: String, lineIndex: Int): String {
        val lines = text.lines().toMutableList()
        if (lineIndex !in lines.indices) return text
        val line = lines[lineIndex]
        val indent = line.takeWhile { it == ' ' || it == '\t' }
        val trimmed = line.trimStart()
        lines[lineIndex] = when {
            trimmed.startsWith(UNCHECKED_PREFIX) ->
                indent + CHECKED_PREFIX + trimmed.removePrefix(UNCHECKED_PREFIX)
            trimmed.startsWith(CHECKED_PREFIX) ->
                indent + UNCHECKED_PREFIX + trimmed.removePrefix(CHECKED_PREFIX)
            else -> return text
        }
        return lines.joinToString("\n")
    }

    /**
     * Applies [format] to the line containing [cursor], replacing any existing
     * list prefix on that line. Numbered lines continue the sequence of the
     * numbered lines above them.
     */
    fun formatCurrentLine(text: String, cursor: Int, format: NoteLineFormat): FormatResult {
        val safeCursor = cursor.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', safeCursor - 1) + 1
        val lineEnd = text.indexOf('\n', safeCursor).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)
        val indent = line.takeWhile { it == ' ' || it == '\t' }
        val content = line.trimStart().replaceFirst(anyPrefixRegex, "")
        val prefix = when (format) {
            NoteLineFormat.BULLET -> BULLET_PREFIX
            NoteLineFormat.CHECKBOX -> UNCHECKED_PREFIX
            NoteLineFormat.NUMBERED -> "${nextNumber(text.substring(0, lineStart))}. "
        }
        val newLine = indent + prefix + content
        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        val cursorInLine = (safeCursor - lineStart).coerceIn(0, line.length)
        val newCursor = lineStart + (indent.length + prefix.length +
            (cursorInLine - (line.length - content.length)).coerceAtLeast(0))
            .coerceAtMost(newLine.length)
        return FormatResult(newText, newCursor)
    }

    private fun nextNumber(textBeforeLine: String): Int {
        val lastNumbered = textBeforeLine.lines()
            .mapNotNull { numberedRegex.find(it.trimStart())?.groupValues?.get(1)?.toIntOrNull() }
            .lastOrNull()
        return (lastNumbered ?: 0) + 1
    }
}
