package com.lifestyle.nestly.data.supabase

import com.lifestyle.nestly.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json

/** Last data-layer error, shown as a banner with Retry in strict online-only mode. */
object DataErrors {
    val message = MutableStateFlow<String?>(null)

    fun report(e: Throwable) {
        message.value = e.message?.takeIf { it.isNotBlank() } ?: "Couldn't reach the server. Check your connection."
    }

    fun clear() {
        message.value = null
    }

    fun retry() {
        clear()
        RefreshBus.invalidate(
            RefreshBus.ACTIVITY, RefreshBus.PROFILE, RefreshBus.CAREGIVER,
            RefreshBus.FAMILY, RefreshBus.REMINDER,
            RefreshBus.APPOINTMENT, RefreshBus.APPOINTMENT_QUESTION,
        )
    }
}

/** Thrown when a version-checked update matched zero rows: someone else edited first. */
class ConflictException(message: String = "This entry was changed by someone else. Showing the latest version.") :
    Exception(message)

fun createNestlyClient(): SupabaseClient = createSupabaseClient(
    supabaseUrl = BuildConfig.SUPABASE_URL,
    supabaseKey = BuildConfig.SUPABASE_KEY,
) {
    defaultSerializer = KotlinXSerializer(
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        },
    )
    install(Auth)
    install(Postgrest)
    install(Realtime)
    install(Storage)
}

/**
 * Shared invalidation bus. Realtime events, local writes, reconnects and manual
 * retries all bump a table's tick; repository flows refetch when their tick changes.
 */
object RefreshBus {
    private val ticks = mutableMapOf<String, MutableStateFlow<Int>>()

    fun tick(table: String): MutableStateFlow<Int> = synchronized(ticks) {
        ticks.getOrPut(table) { MutableStateFlow(0) }
    }

    fun invalidate(vararg tables: String) {
        tables.forEach { tick(it).value += 1 }
    }

    const val ACTIVITY = "activity"
    const val PROFILE = "profile"
    const val CAREGIVER = "caregiver"
    const val FAMILY = "family"
    const val REMINDER = "reminder"
    const val APPOINTMENT = "appointment"
    const val APPOINTMENT_QUESTION = "appointment_question"
}
