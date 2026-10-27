package com.lifestyle.nestly.data.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderLogicTest {
    @Test
    fun nextFireFromLastActivity() {
        val last = 1_000_000L
        val interval = 180
        assertEquals(last + 180 * 60_000L, ReminderLogic.nextFireMs(last, interval, now = last + 60_000)!!)
    }

    @Test
    fun pastDueFiresOneMinuteFromNow() {
        val last = 1_000_000L
        val now = last + 5 * 60 * 60_000L
        assertEquals(now + 60_000L, ReminderLogic.nextFireMs(last, 180, now = now)!!)
    }

    @Test
    fun noActivityMeansNoFire() {
        assertNull(ReminderLogic.nextFireMs(null, 180, now = 1_000L))
    }
}
