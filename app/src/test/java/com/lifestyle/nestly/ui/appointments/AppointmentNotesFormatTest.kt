package com.lifestyle.nestly.ui.appointments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentNotesFormatTest {

    @Test
    fun checkboxItemsParsesCheckedAndUnchecked() {
        val items = AppointmentNotesFormat.checkboxItems("☐ Ask about fever\nplain note\n☑ Bring records")
        assertEquals(2, items.size)
        assertEquals(AppointmentNotesFormat.CheckboxItem(0, "Ask about fever", false), items[0])
        assertEquals(AppointmentNotesFormat.CheckboxItem(2, "Bring records", true), items[1])
    }

    @Test
    fun toggleCheckboxFlipsBothWays() {
        val text = "☐ Ask about fever\n☑ Done thing"
        val toggled = AppointmentNotesFormat.toggleCheckbox(text, 0)
        assertEquals("☑ Ask about fever\n☑ Done thing", toggled)
        assertEquals(text, AppointmentNotesFormat.toggleCheckbox(toggled, 0))
        assertEquals(text, AppointmentNotesFormat.toggleCheckbox(text, 1).let {
            AppointmentNotesFormat.toggleCheckbox(it, 1)
        })
    }

    @Test
    fun toggleCheckboxIgnoresPlainLines() {
        val text = "just a note"
        assertEquals(text, AppointmentNotesFormat.toggleCheckbox(text, 0))
        assertEquals(text, AppointmentNotesFormat.toggleCheckbox(text, 5))
    }

    @Test
    fun bulletFormatsCurrentLine() {
        val result = AppointmentNotesFormat.formatCurrentLine("Ask about fever", 4, NoteLineFormat.BULLET)
        assertEquals("• Ask about fever", result.text)
        assertTrue(result.cursor in 0..result.text.length)
    }

    @Test
    fun checkboxReplacesBulletPrefixInsteadOfStacking() {
        val result = AppointmentNotesFormat.formatCurrentLine("• Ask about fever", 5, NoteLineFormat.CHECKBOX)
        assertEquals("☐ Ask about fever", result.text)
    }

    @Test
    fun numberedContinuesSequence() {
        val text = "1. First\n2. Second\nThird"
        val result = AppointmentNotesFormat.formatCurrentLine(text, text.length, NoteLineFormat.NUMBERED)
        assertEquals("1. First\n2. Second\n3. Third", result.text)
    }

    @Test
    fun numberedStartsAtOneWithoutPreviousNumbers() {
        val result = AppointmentNotesFormat.formatCurrentLine("Only line", 2, NoteLineFormat.NUMBERED)
        assertEquals("1. Only line", result.text)
    }

    @Test
    fun formatOnlyTouchesTheCursorLine() {
        val text = "first line\nsecond line"
        val result = AppointmentNotesFormat.formatCurrentLine(text, 2, NoteLineFormat.BULLET)
        assertEquals("• first line\nsecond line", result.text)
    }
}
