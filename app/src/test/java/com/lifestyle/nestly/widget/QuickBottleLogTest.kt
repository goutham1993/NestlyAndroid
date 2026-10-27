package com.lifestyle.nestly.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickBottleLogTest {

    @Test
    fun prefersStoredDefaultOverLastAmount() {
        assertEquals(150.0, resolveQuickBottleMl(defaultMl = 150.0, lastMl = 120.0)!!, 0.0)
    }

    @Test
    fun fallsBackToLastAmountWhenNoDefaultSet() {
        assertEquals(120.0, resolveQuickBottleMl(defaultMl = null, lastMl = 120.0)!!, 0.0)
    }

    @Test
    fun noAmountWhenNeitherDefaultNorLastExists() {
        assertNull(resolveQuickBottleMl(defaultMl = null, lastMl = null))
    }

    @Test
    fun ignoresNonPositiveAmounts() {
        assertEquals(120.0, resolveQuickBottleMl(defaultMl = 0.0, lastMl = 120.0)!!, 0.0)
        assertNull(resolveQuickBottleMl(defaultMl = null, lastMl = 0.0))
        assertNull(resolveQuickBottleMl(defaultMl = -5.0, lastMl = null))
    }
}
