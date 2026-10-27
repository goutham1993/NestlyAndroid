package com.lifestyle.nestly.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class FormatTest {
    private val minute = 60_000L

    @Test
    fun ago() {
        val now = 1_000 * minute
        assertEquals("Just now", Format.ago(now - 30_000, now))
        assertEquals("5m ago", Format.ago(now - 5 * minute, now))
        assertEquals("1h 53m ago", Format.ago(now - 113 * minute, now))
        assertEquals("2d ago", Format.ago(now - 2 * 24 * 60 * minute, now))
        assertEquals("Just now", Format.ago(now + minute, now))
    }

    @Test
    fun age() {
        val birth = LocalDate.of(2026, 1, 15)
        assertEquals("Born today", Format.age(birth, birth))
        assertEquals("1 day old", Format.age(birth, birth.plusDays(1)))
        assertEquals("5 days old", Format.age(birth, birth.plusDays(5)))
        assertEquals("1 week old", Format.age(birth, birth.plusWeeks(1)))
        assertEquals("2 weeks, 3 days old", Format.age(birth, birth.plusDays(17)))
        assertEquals("1 month old", Format.age(birth, birth.plusMonths(1)))
        assertEquals("4 months, 2 weeks old", Format.age(birth, birth.plusMonths(4).plusDays(14)))
        assertEquals("1 year old", Format.age(birth, birth.plusYears(1)))
        assertEquals("1 year, 3 months old", Format.age(birth, birth.plusYears(1).plusMonths(3)))
        assertEquals("2 years old", Format.age(birth, birth.plusYears(2)))
        assertNull(Format.age(birth, birth.minusDays(1)))
    }

    @Test
    fun duration() {
        assertEquals("0s", Format.duration(0))
        assertEquals("45s", Format.duration(45))
        assertEquals("12m", Format.duration(12 * 60))
        assertEquals("12m 5s", Format.duration(12 * 60 + 5))
        assertEquals("1h 5m", Format.duration(3600 + 5 * 60 + 30))
    }

    @Test
    fun durationUnderOneMinuteShowsSeconds() {
        assertEquals("0s", Format.duration(0))
        assertEquals("45s", Format.duration(45))
        assertEquals("59s", Format.duration(59))
    }

    @Test
    fun volumeRespectsUnitPrefs() {
        val mlPrefs = com.lifestyle.nestly.data.units.UnitPrefs()
        val ozPrefs = mlPrefs.copy(volume = com.lifestyle.nestly.data.units.VolumeUnit.OZ)
        assertEquals("150 mL", Format.volume(150.0, mlPrefs))
        assertEquals("5.1 oz", Format.volume(150.0, ozPrefs))
    }

    @Test
    fun number() {
        assertEquals("150", Format.number(150.0))
        assertEquals("7.34", Format.number(7.3412))
        assertEquals("7.5", Format.number(7.5))
        assertEquals("150 mL", Format.ml(149.6))
    }
}
