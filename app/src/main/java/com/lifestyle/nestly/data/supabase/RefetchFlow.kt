package com.lifestyle.nestly.data.supabase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

/**
 * A read flow for strict online-only mode: fetches now, refetches whenever the
 * table's RefreshBus tick changes (Realtime event, local write, reconnect,
 * manual retry). Failures are reported to [DataErrors].
 *
 * A failed refetch must never masquerade as "the table is empty": callers
 * (notably the signed-in gate in NestlyApp) treat an empty profile list as
 * "no child yet" and route to onboarding. So once a fetch has succeeded, a
 * later failure re-emits the last good value instead of [fallback]. This
 * happens on ordinary warm resumes — [com.lifestyle.nestly.MainActivity]
 * calls refreshAll() from onStart, and that refetch can race an expired
 * access token / still-restoring network and fail even though nothing was
 * deleted. [fallback] is only emitted when no fetch has succeeded yet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> refetchFlow(table: String, fallback: T, fetch: suspend () -> T): Flow<T> = flow {
    // Per-collection last-good value (declared inside the flow builder so
    // independent collectors don't share it).
    var lastGood: T? = null
    var hasLastGood = false
    RefreshBus.tick(table).flatMapLatest {
        flow {
            try {
                val value = fetch()
                lastGood = value
                hasLastGood = true
                emit(value)
                DataErrors.clear()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DataErrors.report(e)
                emit(if (hasLastGood) lastGood ?: fallback else fallback)
            }
        }
    }.collect { emit(it) }
}
