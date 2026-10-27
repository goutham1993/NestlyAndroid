package com.lifestyle.nestly.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun listRoundTripKeepsCommas() {
        val list = listOf("DTaP, dose 1", "Hib", "")
        assertEquals(list, converters.stringToList(converters.listToString(list)))
    }

    @Test
    fun emptyAndNullLists() {
        assertEquals(emptyList<String>(), converters.stringToList(converters.listToString(emptyList())))
        assertNull(converters.stringToList(converters.listToString(null)))
    }

    @Test
    fun localDateRoundTrip() {
        val date = LocalDate.of(2026, 5, 12)
        assertEquals(date, converters.stringToLocalDate(converters.localDateToString(date)))
    }
}
