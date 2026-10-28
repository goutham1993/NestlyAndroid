package com.lifestyle.nestly.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

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
            runCatching {
                client.realtime.connect()
                val channel = client.channel("nestly-$familyId")
                val tables = listOf(
                    RefreshBus.ACTIVITY, RefreshBus.PROFILE, RefreshBus.CAREGIVER,
                    RefreshBus.REMINDER, RefreshBus.APPOINTMENT, RefreshBus.APPOINTMENT_QUESTION,
                )
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
                channel.subscribe()
                // Fresh state on (re)connect; never trust missed events.
                RefreshBus.invalidate(*tables.toTypedArray(), RefreshBus.FAMILY)
            }
        }
    }

    fun unsubscribe() {
        job?.cancel()
        job = null
        subscribedFamily = null
    }

    /** Refetch everything, e.g. on app foreground or network reconnect. */
    fun refreshAll() {
        RefreshBus.invalidate(
            RefreshBus.ACTIVITY, RefreshBus.PROFILE, RefreshBus.CAREGIVER,
            RefreshBus.FAMILY, RefreshBus.REMINDER,
            RefreshBus.APPOINTMENT, RefreshBus.APPOINTMENT_QUESTION,
        )
    }
}
