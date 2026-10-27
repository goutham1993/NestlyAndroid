package com.lifestyle.nestly.widget

import android.content.Context
import android.widget.Toast
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.lifestyle.nestly.AppContainer
import com.lifestyle.nestly.appContainer
import com.lifestyle.nestly.data.model.ActivityType
import com.lifestyle.nestly.data.units.UnitPrefs
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.entry.EntryBuilder
import com.lifestyle.nestly.ui.entry.EntryContext
import com.lifestyle.nestly.ui.entry.EntryDraft
import com.lifestyle.nestly.ui.entry.EntryKind
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Amount for a one-tap bottle feed: the stored default wins, otherwise the most
 * recent bottle amount (mirroring the sheet's "use last amount"). Null when there
 * is nothing sensible to save.
 */
fun resolveQuickBottleMl(defaultMl: Double?, lastMl: Double?): Double? =
    listOf(defaultMl, lastMl).firstOrNull { it != null && it > 0 }

sealed interface QuickBottleResult {
    data class Saved(val ml: Double, val atMs: Long) : QuickBottleResult
    data object NotSignedIn : QuickBottleResult
    data object NoChild : QuickBottleResult
    data object NoAmount : QuickBottleResult
    data object Failed : QuickBottleResult
}

/**
 * Saves a bottle feed with no UI: default (or last) amount, current time, for the
 * currently selected child. Used by the widget's Bottle button so tapping it never
 * has to open the app.
 */
class QuickBottleLog(private val container: AppContainer) {

    suspend fun save(now: Long = System.currentTimeMillis()): QuickBottleResult {
        // A widget tap can start a fresh process, in which the stored Supabase
        // session is still being restored; querying before it lands returns no
        // profiles under RLS and would look like "no child selected".
        val session = withTimeoutOrNull(SESSION_WAIT_MS) {
            container.auth.sessionStatus.first {
                it is SessionStatus.Authenticated || it is SessionStatus.NotAuthenticated
            }
        }
        if (session !is SessionStatus.Authenticated) return QuickBottleResult.NotSignedIn

        return try {
            val profile = container.family.selectedProfile.first()
                ?: return QuickBottleResult.NoChild
            val defaultMl = runCatching { container.preferences.defaultBottleMl.first() }.getOrNull()
            val lastMl = if (defaultMl != null && defaultMl > 0) {
                null
            } else {
                runCatching {
                    container.activities.getLatest(profile.familyId, profile.id, ActivityType.BOTTLE_FEED)
                        ?.bottle?.totalMl
                }.getOrNull()
            }
            val ml = resolveQuickBottleMl(defaultMl, lastMl)
                ?: return QuickBottleResult.NoAmount
            val draft = EntryDraft(startTime = now, bottleMl = ml)
            if (!EntryBuilder.canSave(EntryKind.BOTTLE, draft)) return QuickBottleResult.NoAmount
            val ctx = EntryContext(
                familyId = profile.familyId,
                profileId = profile.id,
                caregiverId = container.family.currentCaregiverId(),
                timeZone = ZoneId.systemDefault().id,
                now = now,
                newId = { UUID.randomUUID().toString() },
            )
            container.activities.saveAll(EntryBuilder.build(EntryKind.BOTTLE, draft, ctx))
            runCatching { container.scheduler.rescheduleAll(container) }
            QuickBottleResult.Saved(ml, now)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            QuickBottleResult.Failed
        }
    }

    private companion object {
        const val SESSION_WAIT_MS = 5_000L
    }
}

/**
 * Glance callback for the widget's Bottle button. Saves in the background and
 * confirms with a toast — it never starts [com.lifestyle.nestly.MainActivity].
 */
class SaveBottleFeedCallback : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        if (!inProgress.compareAndSet(false, true)) {
            toast(context, "Saving bottle feed…")
            return
        }
        try {
            val container = context.appContainer
            val result = QuickBottleLog(container).save()
            val message = when (result) {
                is QuickBottleResult.Saved -> {
                    val units = runCatching { container.preferences.units.first() }
                        .getOrDefault(UnitPrefs.DEFAULT)
                    "Bottle feed saved: ${Format.volume(result.ml, units)} at ${Format.time(result.atMs)}"
                }
                QuickBottleResult.NotSignedIn ->
                    "Couldn't save bottle feed. Open Nestly and sign in first."
                QuickBottleResult.NoChild ->
                    "Couldn't save bottle feed. Open Nestly and select a child first."
                QuickBottleResult.NoAmount ->
                    "No default bottle amount set. Open Nestly to set one first."
                QuickBottleResult.Failed ->
                    "Couldn't save bottle feed. Check your connection and try again."
            }
            toast(context, message)
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            toast(context, "Couldn't save bottle feed. Check your connection and try again.")
        } finally {
            inProgress.set(false)
        }
    }

    private suspend fun toast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        /** Guards against double-taps creating duplicate feeds while a save is in flight. */
        private val inProgress = AtomicBoolean(false)
    }
}
