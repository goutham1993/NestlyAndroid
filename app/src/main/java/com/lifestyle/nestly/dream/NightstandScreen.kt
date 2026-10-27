package com.lifestyle.nestly.dream

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifestyle.nestly.data.reminder.NextFeedState
import com.lifestyle.nestly.ui.activity.detailText
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import com.lifestyle.nestly.ui.theme.SerifDisplay
import com.lifestyle.nestly.widget.QuickBottleResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Near-black so an OLED nightstand stays dark; [NestlyColors.Background] is too bright at 3am. */
private val NightBackground = Color(0xFF0B0C12)
private val NightDim = NestlyColors.TextSecondary.copy(alpha = 0.75f)
private val NightFaint = NestlyColors.TextMuted.copy(alpha = 0.8f)

private val ClockStyle = TextStyle(fontFamily = SerifDisplay, fontWeight = FontWeight.Light, fontSize = 88.sp, lineHeight = 96.sp)
private val CountdownStyle = TextStyle(fontFamily = SerifDisplay, fontWeight = FontWeight.Light, fontSize = 64.sp, lineHeight = 72.sp)

@Composable
fun NightstandScreen(
    viewModel: NightstandViewModel,
    onLogFeed: suspend () -> QuickBottleResult,
    onExit: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var logging by remember { mutableStateOf(false) }

    fun toast(message: String) {
        Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
    }

    fun logFeed() {
        if (logging) return
        logging = true
        scope.launch {
            try {
                val message = when (val result = onLogFeed()) {
                    is QuickBottleResult.Saved ->
                        "Feed added successfully: ${Format.volume(result.ml, state?.units ?: com.lifestyle.nestly.data.units.UnitPrefs.DEFAULT)} at ${Format.time(result.atMs)}"
                    QuickBottleResult.NotSignedIn ->
                        "Couldn't add feed. Open Nestly and sign in first."
                    QuickBottleResult.NoChild ->
                        "Couldn't add feed. Open Nestly and select a child first."
                    QuickBottleResult.NoAmount ->
                        "No default bottle amount set. Open Nestly to set one first."
                    QuickBottleResult.Failed ->
                        "Couldn't add feed. Check your connection and try again."
                }
                toast(message)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast("Couldn't add feed. Check your connection and try again.")
            } finally {
                logging = false
            }
        }
    }
    // The state flow may not have emitted yet on a cold process start.
    val localNow by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000L)
            value = System.currentTimeMillis()
        }
    }
    val now = state?.now ?: localNow

    // Burn-in protection: slowly drift the whole composition a few dp.
    val minute = now / 60_000L
    val shiftX = ((minute % 9).toInt() - 4).dp
    val shiftY = (((minute / 9) % 9).toInt() - 4).dp

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(NightBackground)
            // Consume every touch that misses a button; otherwise the
            // system ends even an interactive dream on stray taps.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {},
        contentAlignment = Alignment.Center,
    ) {
        // Landscape (nightstand on its side) gets two side-by-side panes;
        // portrait keeps the stacked column.
        val landscape = maxWidth > maxHeight
        if (landscape) {
            Row(
                Modifier
                    .offset(x = shiftX, y = shiftY)
                    .fillMaxSize()
                    .padding(horizontal = 48.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(state?.profileName ?: "Nestly", style = NestlyType.CardTitle, color = NightDim)
                    Spacer(Modifier.height(8.dp))
                    Text(Format.time(now), style = ClockStyle, color = NestlyColors.TextPrimary)
                    Text(Format.headerDate(now), style = NestlyType.RowLabel, color = NightFaint)
                }
                Spacer(Modifier.width(32.dp))
                Column(
                    Modifier.weight(1.15f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    NightstandBody(state, now, compact = true)
                    if (state?.profileName != null) {
                        Spacer(Modifier.height(20.dp))
                        LogFeedButton(logging, onClick = { logFeed() })
                    }
                }
            }
        } else {
            Column(
                Modifier
                    .offset(x = shiftX, y = shiftY)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    state?.profileName ?: "Nestly",
                    style = NestlyType.CardTitle,
                    color = NightDim,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(Format.time(now), style = ClockStyle, color = NestlyColors.TextPrimary, textAlign = TextAlign.Center)
                Text(Format.headerDate(now), style = NestlyType.RowLabel, color = NightFaint, textAlign = TextAlign.Center)
                Spacer(Modifier.height(40.dp))
                NightstandBody(state, now)
                if (state?.profileName != null) {
                    Spacer(Modifier.height(36.dp))
                    LogFeedButton(logging, onClick = { logFeed() })
                }
            }
        }
        // An interactive dream no longer exits on any touch, so give an
        // explicit way out (the power button also ends it).
        TextButton(
            onClick = onExit,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
        ) {
            Text("Done", style = NestlyType.Body, color = NightFaint)
        }
    }
}

@Composable
private fun LogFeedButton(logging: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !logging,
        colors = ButtonDefaults.buttonColors(
            containerColor = NestlyColors.Feed,
            contentColor = NestlyColors.HeaderText,
            disabledContainerColor = NestlyColors.Feed.copy(alpha = 0.5f),
            disabledContentColor = NestlyColors.HeaderText.copy(alpha = 0.6f),
        ),
    ) {
        Text(
            if (logging) "Logging…" else "Log Feed",
            style = NestlyType.Button,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
        )
    }
}

/** Whole-minutes duration ("11m", "1h 23m"); the dream ticks every 30s, so seconds would jump. */
private fun minutesText(ms: Long, roundUp: Boolean): String {
    val minutes = if (roundUp) (ms + 59_999L) / 60_000L else ms / 60_000L
    // Format.duration renders whole hours as "3h 0m"; trim the dangling zero.
    return Format.duration(minutes.coerceAtLeast(1) * 60).removeSuffix(" 0m")
}

@Composable
private fun NightstandBody(state: NightstandState?, now: Long, compact: Boolean = false) {
    when {
        state == null -> Text("Loading…", style = NestlyType.Body, color = NightFaint)
        state.profileName == null ->
            Text(
                "Open Nestly to set up your family",
                style = NestlyType.Body,
                color = NightDim,
                textAlign = TextAlign.Center,
            )
        state.lastFeed == null -> {
            Text("No feeds logged yet", style = CountdownStyle, color = NestlyColors.TextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text("Tap Log Feed to record one now", style = NestlyType.Body, color = NightDim, textAlign = TextAlign.Center)
        }
        else -> FeedBody(state, now, compact)
    }
}

@Composable
private fun FeedBody(state: NightstandState, now: Long, compact: Boolean = false) {
    val lastFeed = state.lastFeed ?: return
    val next = state.nextFeed

    Text("NEXT FEED", style = NestlyType.Small, color = NightFaint, textAlign = TextAlign.Center)
    Spacer(Modifier.height(4.dp))
    // The due time is the hero, as large as the clock; the countdown supports it.
    val (dueMs, countdown, countdownColor) = when (next) {
        is NextFeedState.Upcoming ->
            Triple(next.dueMs, "in ${minutesText(next.remainingMs, roundUp = true)}", NestlyColors.TextPrimary)
        is NextFeedState.DueNow ->
            Triple(next.dueMs, "Due now", NestlyColors.Danger)
        is NextFeedState.Overdue ->
            Triple(next.dueMs, "${minutesText(next.overdueMs, roundUp = false)} overdue", NestlyColors.Danger)
        NextFeedState.NoFeed -> null // unreachable: lastFeed != null implies a due time
    } ?: return
    Text(Format.time(dueMs), style = ClockStyle, color = NestlyColors.TextPrimary, textAlign = TextAlign.Center)
    Text(countdown, style = NestlyType.CardTitle, color = countdownColor, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    Text(
        "Estimated every ${minutesText(state.intervalMinutes * 60_000L, roundUp = false)} after the last feed",
        style = NestlyType.Small,
        color = NightFaint,
        textAlign = TextAlign.Center,
    )

    Spacer(Modifier.height(if (compact) 16.dp else 32.dp))
    val lastParts = listOfNotNull(
        "Last feed ${Format.time(lastFeed.startTime)}",
        Format.ago(lastFeed.startTime, now),
        lastFeed.detailText(state.units),
    )
    Text(lastParts.joinToString("  ·  "), style = NestlyType.RowLabel, color = NightDim, textAlign = TextAlign.Center)
    Spacer(Modifier.height(8.dp))
    val todayText = when {
        state.todayFeedCount == 0 -> "No feeds yet today"
        state.todayBottleMl > 0 -> "Today: ${state.todayFeedCount} ${if (state.todayFeedCount == 1) "feed" else "feeds"}  ·  ${Format.volume(state.todayBottleMl, state.units)}"
        else -> "Today: ${state.todayFeedCount} ${if (state.todayFeedCount == 1) "feed" else "feeds"}"
    }
    Text(todayText, style = NestlyType.Body, color = NightFaint, textAlign = TextAlign.Center)
}
