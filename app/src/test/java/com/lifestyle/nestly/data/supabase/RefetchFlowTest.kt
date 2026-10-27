package com.lifestyle.nestly.data.supabase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RefetchFlowTest {

    @Test
    fun failedRefetchKeepsLastGoodValueInsteadOfFallback() = runTest {
        val table = "profile_test_keep_last"
        var attempt = 0
        val values = async {
            refetchFlow(table, emptyList<String>()) {
                attempt++
                if (attempt == 2) throw RuntimeException("offline / expired token")
                listOf("child-$attempt")
            }.take(3).toList()
        }
        runCurrent() // initial fetch succeeds
        RefreshBus.invalidate(table) // MainActivity.onStart -> refreshAll()
        runCurrent() // refetch fails: must re-emit last good, not emptyList()
        RefreshBus.invalidate(table) // manual retry / next foreground
        runCurrent() // refetch succeeds again

        assertEquals(
            listOf(listOf("child-1"), listOf("child-1"), listOf("child-3")),
            values.await(),
        )
        DataErrors.clear()
    }

    @Test
    fun failedFirstFetchEmitsFallback() = runTest {
        val table = "profile_test_first_fails"
        val values = async {
            refetchFlow(table, listOf("fallback")) {
                throw RuntimeException("offline")
            }.take(1).toList()
        }
        runCurrent()

        // No successful fetch yet: fallback is all we have. The UI gate
        // distinguishes this from a real empty result via DataErrors.
        assertEquals(listOf(listOf("fallback")), values.await())
        DataErrors.clear()
    }
}
