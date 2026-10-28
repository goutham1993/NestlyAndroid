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
 * A failed fetch must never masquerade as "the table is empty": callers
 * (notably the signed-in gate in NestlyApp) treat an empty profile list as
 * "no child yet" and route to onboarding. So a failed refetch re-emits the
 * last good value, and a failed *first* fetch emits nothing at all — the
 * collector simply stays in its loading state until a fetch succeeds or the
 * user retries from the error screen. Emitting a fallback empty list here
 * was the cause of the onboarding flash on cold start: the first fetch races
 * session restoration, and an unauthenticated PostgREST request either fails
 * or (via RLS) succeeds with zero rows, neither of which means the account
 * has no children.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> refetchFlow(table: String, fetch: suspend () -> T): Flow<T> = flow {
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
                if (hasLastGood) {
                    @Suppress("UNCHECKED_CAST")
                    emit(lastGood as T)
                }
                // No last good value: emit nothing. The caller remains in
                // its loading state and DataErrors drives the error UI.
            }
        }
    }.collect { emit(it) }
}
