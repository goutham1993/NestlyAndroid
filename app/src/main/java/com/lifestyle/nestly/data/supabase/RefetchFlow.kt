package com.lifestyle.nestly.data.supabase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow

/**
 * A read flow for strict online-only mode: fetches now, refetches whenever the
 * table's RefreshBus tick changes (Realtime event, local write, reconnect,
 * manual retry). Failures are reported to [DataErrors] and fall back to
 * [fallback] so the UI shows its empty/error state instead of crashing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> refetchFlow(table: String, fallback: T, fetch: suspend () -> T): Flow<T> =
    RefreshBus.tick(table).flatMapLatest {
        flow {
            try {
                emit(fetch())
                DataErrors.clear()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                DataErrors.report(e)
                emit(fallback)
            }
        }
    }
