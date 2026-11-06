package com.lifestyle.nestly.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

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
 *
 * Live UI reads should use [SupabaseClient.refetchWhenAuthenticated]. A bare
 * [refetchFlow] still runs as whatever role PostgREST has right now, which
 * during session restore is `anon` — and `anon` has no table grants.
 *
 * A new tick does not cancel a read already on the wire. Opening the app
 * invalidates every table more than once (foreground refresh, the network
 * callback, realtime connect), and cancelling the first wave is what
 * supabase-kt logs as "GET request to endpoint … was cancelled". The tick
 * that arrived during the read is picked up as soon as that read finishes.
 */
fun <T> refetchFlow(table: String, fetch: suspend () -> T): Flow<T> = flow {
    // Per-collection last-good value (declared inside the flow builder so
    // independent collectors don't share it).
    var lastGood: T? = null
    var hasLastGood = false
    var seenTick = Int.MIN_VALUE
    val ticks = RefreshBus.tick(table)
    while (true) {
        val tick = ticks.first { it != seenTick }
        seenTick = tick
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
}

/**
 * [refetchFlow] that does not touch the database until the stored session is
 * authenticated. supabase-kt reports [SessionStatus.Initializing] on every
 * cold start and every return from the background, and PostgREST answers
 * those moments as `anon`. `anon` has no grants, so the read fails with
 * "permission denied" and the shared [DataErrors] banner flashes until a
 * later signed-in read clears it. This emits nothing during that gap, so
 * collectors keep the last value they already have.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> SupabaseClient.refetchWhenAuthenticated(table: String, fetch: suspend () -> T): Flow<T> =
    auth.sessionStatus
        .map { it is SessionStatus.Authenticated }
        .distinctUntilChanged()
        .flatMapLatest { authenticated ->
            if (authenticated) refetchFlow(table) { withFreshSession(fetch) } else emptyFlow()
        }
