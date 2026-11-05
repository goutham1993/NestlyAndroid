package com.lifestyle.nestly.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Subscribes to Postgres Changes for the current family and treats every event
 * as a refetch trigger via [RefreshBus]. Payloads are never applied directly;
 * on (re)connect all tables are invalidated so missed events don't matter.
 */
class RealtimeManager(
    private val client: SupabaseClient,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null
    private var subscribedFamily: String? = null

    fun subscribe(familyId: String) {
        if (familyId.isBlank() || familyId == subscribedFamily) return
        unsubscribe()
        subscribedFamily = familyId
        job = scope.launch {
            val tables = listOf(
                RefreshBus.ACTIVITY, RefreshBus.PROFILE, RefreshBus.CAREGIVER,
                RefreshBus.REMINDER, RefreshBus.APPOINTMENT, RefreshBus.APPOINTMENT_QUESTION,
                RefreshBus.NOTE,
            )
            // Register the change flows once per subscribe() call: client.channel
            // returns the same channel on retry, so re-registering would stack
            // duplicate callbacks.
            var flowsRegistered = false
            var retryDelayMs = INITIAL_RETRY_MS
            // Retry until actually subscribed. The old one-shot runCatching
            // swallowed a cold-start connect/subscribe failure (session still
            // restoring, radio still waking) and, with subscribedFamily already
            // set, no later call ever retried — Realtime stayed dead for the
            // life of the process, which is why a cold-started screensaver
            // never saw feeds added from another device.
            while (isActive) {
                try {
                    client.realtime.connect()
                    val channel = client.channel("nestly-$familyId")
                    if (!flowsRegistered) {
                        tables.forEach { table ->
                            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                                this.table = table
                                filter("family_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, familyId)
                            }.onEach { RefreshBus.invalidate(table) }
                                .launchIn(this)
                        }
                        // family rows have no family_id column; watch by id instead.
                        channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                            table = "family"
                            filter("id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, familyId)
                        }.onEach { RefreshBus.invalidate(RefreshBus.FAMILY) }
                            .launchIn(this)
                        flowsRegistered = true
                    }
                    // Block until the server confirms, so a join that never
                    // lands is a retryable failure rather than silent success.
                    withTimeout(SUBSCRIBE_TIMEOUT_MS) {
                        channel.subscribe(blockUntilSubscribed = true)
                    }
                    // Fresh state on (re)connect; never trust missed events.
                    RefreshBus.invalidate(*tables.toTypedArray(), RefreshBus.FAMILY)
                    break
                } catch (e: TimeoutCancellationException) {
                    // Join not confirmed in time; retry below. Must come
                    // before CancellationException (its superclass check):
                    // an outer cancellation has isActive == false.
                    if (!isActive) throw e
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Fall through to backoff/retry.
                }
                delay(retryDelayMs)
                retryDelayMs = (retryDelayMs * 2).coerceAtMost(MAX_RETRY_MS)
            }
        }
    }

    fun unsubscribe() {
        job?.cancel()
        job = null
        subscribedFamily = null
    }

    private companion object {
        const val SUBSCRIBE_TIMEOUT_MS = 15_000L
        const val INITIAL_RETRY_MS = 2_000L
        const val MAX_RETRY_MS = 60_000L
    }

    /** Refetch everything, e.g. on app foreground or network reconnect. */
    fun refreshAll() {
        RefreshBus.invalidate(
            RefreshBus.ACTIVITY, RefreshBus.PROFILE, RefreshBus.CAREGIVER,
            RefreshBus.FAMILY, RefreshBus.REMINDER,
            RefreshBus.APPOINTMENT, RefreshBus.APPOINTMENT_QUESTION, RefreshBus.NOTE,
        )
    }
}
