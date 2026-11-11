package com.lifestyle.nestly.data.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PhotoPromptTest {
    private val birth: LocalDate = LocalDate.of(2026, 1, 15)

    @Test
    fun noPromptTheDayBeforeOneMonth() {
        assertNull(PhotoPrompt.current(birth, birth.plusMonths(1).minusDays(1)))
    }

    @Test
    fun everyMonthOfTheFirstYear() {
        for (month in 1..12) {
            val today = birth.plusMonths(month.toLong())
            val milestone = PhotoPrompt.current(birth, today)
            assertEquals("m$month", milestone?.id)
            assertEquals(today, milestone?.date)
            val dayBefore = today.minusDays(1)
            if (month == 1) {
                assertNull(PhotoPrompt.current(birth, dayBefore))
            } else {
                assertEquals("m${month - 1}", PhotoPrompt.current(birth, dayBefore)?.id)
            }
        }
        assertEquals("8 months", PhotoPrompt.current(birth, birth.plusMonths(8))?.phrase)
    }

    @Test
    fun twelveMonthsIsTheFirstBirthdayAndTheSecondBirthdayIsNext() {
        val first = PhotoPrompt.current(birth, birth.plusMonths(12))
        assertEquals("m12", first?.id)
        assertEquals(birth.plusYears(1), first?.date)

        val stillFirst = PhotoPrompt.current(birth, birth.plusMonths(12).plusDays(1))
        assertEquals("m12", stillFirst?.id)

        val second = PhotoPrompt.current(birth, birth.plusYears(2))
        assertEquals("y2", second?.id)
        assertEquals("2 years", second?.phrase)
    }

    @Test
    fun dismissHidesThisAgeUntilTheNextOne() {
        val during = birth.plusMonths(8)
        assertNull(PhotoPrompt.due(birth, photoUpdatedOn = null, dismissedMilestoneId = "m8", today = during))

        val next = birth.plusMonths(9)
        assertEquals("m9", PhotoPrompt.due(birth, null, "m8", next)?.id)
    }

    @Test
    fun photoOnTheMilestoneDayIsCurrent() {
        val milestone = birth.plusMonths(8)
        val today = birth.plusMonths(8).plusDays(10)
        assertNull(PhotoPrompt.due(birth, milestone, null, today))
        assertEquals("m8", PhotoPrompt.due(birth, milestone.minusDays(1), null, today)?.id)
        assertEquals("m8", PhotoPrompt.due(birth, null, null, today)?.id)
    }

    @Test
    fun selectedChildWinsAndOtherwiseTheLatestMilestone() {
        val older = PhotoPrompt.Child(id = "older", name = "Ada", birthDate = birth)
        val younger = PhotoPrompt.Child(
            id = "younger",
            name = "Bea",
            birthDate = birth.plusMonths(4).plusDays(10),
        )
        val today = birth.plusMonths(6).plusDays(5)
        val selectedYounger = PhotoPrompt.choose(listOf(older, younger), "younger", today)
        assertEquals("younger", selectedYounger?.profileId)
        assertEquals("m1", selectedYounger?.milestone?.id)

        val latest = PhotoPrompt.choose(listOf(younger, older), "nobody", today)
        assertEquals("older", latest?.profileId)
        assertEquals("m6", latest?.milestone?.id)
    }

    @Test
    fun childWithoutABirthDateIsSkipped() {
        val child = PhotoPrompt.Child(id = "a", name = "No Date", birthDate = null)
        assertNull(PhotoPrompt.choose(listOf(child), "a", birth.plusYears(2)))
    }
}
