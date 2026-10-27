package com.lifestyle.nestly.ui.trends

import com.lifestyle.nestly.data.model.ActivityEntity
import com.lifestyle.nestly.data.model.ActivityType
import com.lifestyle.nestly.data.model.BottleDetails
import com.lifestyle.nestly.data.model.DiaperDetails
import com.lifestyle.nestly.data.model.DiaperType
import com.lifestyle.nestly.data.model.MilkType
import com.lifestyle.nestly.data.model.PumpDetails
import com.lifestyle.nestly.data.prefs.CardKind
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class TrendsAggregatorTest {
    private val zone = ZoneOffset.UTC
    private val day0 = LocalDate.of(2026, 10, 1)
    private fun ms(day: LocalDate, hour: Int = 12) =
        day.atTime(hour, 0).toInstant(zone).toEpochMilli()

    private fun bottle(day: LocalDate, ml: Double) = ActivityEntity(
        id = "b$ml$day",
        familyId = "f",
        profileId = "p",
        type = ActivityType.BOTTLE_FEED,
        startTime = ms(day),
        timeZone = "UTC",
        bottle = BottleDetails(MilkType.BREAST_MILK, breastMilkMl = ml, totalMl = ml),
        createdAt = 0,
        updatedAt = 0,
    )

    private fun pump(day: LocalDate, ml: Double) = ActivityEntity(
        id = "p$ml$day",
        familyId = "f",
        profileId = null,
        type = ActivityType.PUMP,
        startTime = ms(day),
        timeZone = "UTC",
        pump = PumpDetails(durationSec = 600, totalMl = ml),
        createdAt = 0,
        updatedAt = 0,
    )

    private fun diaper(day: LocalDate) = ActivityEntity(
        id = "d$day",
        familyId = "f",
        profileId = "p",
        type = ActivityType.DIAPER,
        startTime = ms(day),
        timeZone = "UTC",
        diaper = DiaperDetails(DiaperType.WET),
        createdAt = 0,
        updatedAt = 0,
    )

    @Test
    fun sevenDayAveragesAndBarCounts() {
        val entries = listOf(
            bottle(day0, 150.0),
            bottle(day0.plusDays(1), 100.0),
            pump(day0, 80.0),
            diaper(day0),
            diaper(day0),
            diaper(day0.plusDays(2)),
        )
        val cards = TrendsAggregator.aggregate(entries, day0, 7, zone)
        val feed = cards.first { it.kind == CardKind.FEED }
        assertEquals(2, feed.count)
        assertEquals(250.0 / 7, feed.average, 1e-9)
        assertEquals(7, feed.bars.size)
        assertEquals(150.0, feed.bars[0].value, 0.0)
        assertEquals(100.0, feed.bars[1].value, 0.0)
        assertEquals(0.0, feed.bars[2].value, 0.0)

        val pumpCard = cards.first { it.kind == CardKind.PUMP }
        assertEquals(1, pumpCard.count)
        assertEquals(80.0 / 7, pumpCard.average, 1e-9)

        val diaperCard = cards.first { it.kind == CardKind.DIAPER }
        assertEquals(3, diaperCard.count)
        assertEquals(3.0 / 7, diaperCard.average, 1e-9)
        assertEquals(2.0, diaperCard.bars[0].value, 0.0)
    }
}
