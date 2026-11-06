package com.lifestyle.nestly.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * supabase-kt can report Authenticated while holding an access token that is
 * already expired. Its refresh timer is a coroutine `delay`, which does not
 * advance while the device sleeps or the cached process is frozen, and when
 * the process was started by the widget, a reminder or the nightstand dream
 * (no Activity, so no lifecycle onStop) that timer is still "running" when
 * the user opens the app, so the library skips its own foreground reload.
 * Every PostgREST read then fails with PGRST303 "JWT expired".
 */
private val refreshMutex = Mutex()

internal fun Throwable.isExpiredJwt(): Boolean =
    (this is PostgrestRestException && code == "PGRST303") ||
        (this is RestException && statusCode == 401 && error.contains("JWT expired", ignoreCase = true))

/**
 * Refreshes the session unless someone else already replaced [staleToken].
 * Returns true when the current token differs from [staleToken] afterwards.
 */
suspend fun SupabaseClient.refreshSessionReplacing(staleToken: String?): Boolean = refreshMutex.withLock {
    val current = auth.currentSessionOrNull() ?: return@withLock false
    if (current.accessToken != staleToken) return@withLock true
    try {
        auth.refreshCurrentSession()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        return@withLock false
    }
    auth.currentAccessTokenOrNull().let { it != null && it != staleToken }
}

/** Foreground hook: refresh now if the token is expired or about to be. */
@OptIn(ExperimentalTime::class)
suspend fun SupabaseClient.refreshSessionIfExpiring() {
    val session = auth.currentSessionOrNull() ?: return
    if (session.expiresAt - EXPIRY_MARGIN <= Clock.System.now()) {
        refreshSessionReplacing(session.accessToken)
    }
}

/** Runs [fetch]; on "JWT expired", refreshes the session and tries once more. */
suspend fun <T> SupabaseClient.withFreshSession(fetch: suspend () -> T): T {
    val tokenUsed = auth.currentAccessTokenOrNull()
    return try {
        fetch()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (!e.isExpiredJwt() || !refreshSessionReplacing(tokenUsed)) throw e
        fetch()
    }
}

private val EXPIRY_MARGIN = 60.seconds
