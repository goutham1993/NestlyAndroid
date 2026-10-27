package com.lifestyle.nestly.ui.trends

import com.lifestyle.nestly.data.model.ActivityEntity
import com.lifestyle.nestly.data.model.ActivityType
import com.lifestyle.nestly.data.prefs.CardKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayBar(val date: LocalDate, val value: Double)

data class TrendCard(
    val kind: CardKind,
    /** Total sessions / changes in the window. */
    val count: Int,
    /** Average per day (mL, count, or seconds). */
    val average: Double,
    val unit: String,
    val bars: List<DayBar>,
)

object TrendsAggregator {
    fun aggregate(
        entries: List<ActivityEntity>,
        from: LocalDate,
        days: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<TrendCard> {
        val dates = (0 until days).map { from.plusDays(it.toLong()) }
        return listOf(
            feedCard(entries, dates, zone),
            pumpCard(entries, dates, zone),
            diaperCard(entries, dates, zone),
            sleepCard(entries, dates, zone),
        )
    }

    private fun feedCard(entries: List<ActivityEntity>, dates: List<LocalDate>, zone: ZoneId): TrendCard {
        val feeds = entries.filter {
            it.type == ActivityType.BOTTLE_FEED || it.type == ActivityType.BREASTFEED || it.type == ActivityType.SOLIDS
        }
        val bars = dates.map { date ->
            val dayEntries = feeds.filter { it.on(date, zone) }
            DayBar(date, dayEntries.sumOf { it.bottle?.totalMl ?: 0.0 })
        }
        val totalMl = bars.sumOf { it.value }
        return TrendCard(
            kind = CardKind.FEED,
            count = feeds.size,
            average = if (dates.isEmpty()) 0.0 else totalMl / dates.size,
            unit = "mL",
            bars = bars,
        )
    }

    private fun pumpCard(entries: List<ActivityEntity>, dates: List<LocalDate>, zone: ZoneId): TrendCard {
        val pumps = entries.filter { it.type == ActivityType.PUMP }
        val bars = dates.map { date ->
            DayBar(date, pumps.filter { it.on(date, zone) }.sumOf { it.pump?.totalMl ?: 0.0 })
        }
        val total = bars.sumOf { it.value }
        return TrendCard(
            kind = CardKind.PUMP,
            count = pumps.size,
            average = if (dates.isEmpty()) 0.0 else total / dates.size,
            unit = "mL",
            bars = bars,
        )
    }

    private fun diaperCard(entries: List<ActivityEntity>, dates: List<LocalDate>, zone: ZoneId): TrendCard {
        val diapers = entries.filter { it.type == ActivityType.DIAPER }
        val bars = dates.map { date ->
            DayBar(date, diapers.count { it.on(date, zone) }.toDouble())
        }
        val total = bars.sumOf { it.value }
        return TrendCard(
            kind = CardKind.DIAPER,
            count = diapers.size,
            average = if (dates.isEmpty()) 0.0 else total / dates.size,
            unit = "",
            bars = bars,
        )
    }

    private fun sleepCard(entries: List<ActivityEntity>, dates: List<LocalDate>, zone: ZoneId): TrendCard {
        val sleeps = entries.filter { it.type == ActivityType.SLEEP }
        val bars = dates.map { date ->
            DayBar(
                date,
                sleeps.filter { it.on(date, zone) }.sumOf { sleep ->
                    sleep.endTime?.let { ((it - sleep.startTime) / 1000.0).coerceAtLeast(0.0) } ?: 0.0
                },
            )
        }
        val total = bars.sumOf { it.value }
        return TrendCard(
            kind = CardKind.SLEEP,
            count = sleeps.size,
            average = if (dates.isEmpty()) 0.0 else total / dates.size,
            unit = "s",
            bars = bars,
        )
    }

    private fun ActivityEntity.on(date: LocalDate, zone: ZoneId): Boolean =
        Instant.ofEpochMilli(startTime).atZone(zone).toLocalDate() == date
}
