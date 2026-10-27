package com.lifestyle.nestly.data.reminder

import com.lifestyle.nestly.data.db.entity.ActivityType
import com.lifestyle.nestly.data.db.entity.ReminderKind

object ReminderLogic {
    fun typesFor(kind: ReminderKind): List<ActivityType> = when (kind) {
        ReminderKind.FEED -> listOf(ActivityType.BOTTLE_FEED, ActivityType.BREASTFEED, ActivityType.SOLIDS)
        ReminderKind.PUMP -> listOf(ActivityType.PUMP)
        ReminderKind.DIAPER -> listOf(ActivityType.DIAPER)
        ReminderKind.SLEEP -> listOf(ActivityType.SLEEP)
    }

    /**
     * When the next notification should fire. If the due time is already past, schedules a catch-up
     * one minute from [now] so something still alerts once.
     */
    fun nextFireMs(lastActivityStart: Long?, intervalMinutes: Int, now: Long = System.currentTimeMillis()): Long? {
        if (lastActivityStart == null) return null
        val due = lastActivityStart + intervalMinutes * 60_000L
        return if (due > now) due else now + 60_000L
    }

    fun defaultIntervalMinutes(kind: ReminderKind): Int = when (kind) {
        ReminderKind.FEED, ReminderKind.PUMP -> 180
        ReminderKind.DIAPER, ReminderKind.SLEEP -> 120
    }

    fun label(kind: ReminderKind): String = when (kind) {
        ReminderKind.FEED -> "Feed"
        ReminderKind.PUMP -> "Pump"
        ReminderKind.DIAPER -> "Diaper"
        ReminderKind.SLEEP -> "Sleep"
    }
}
