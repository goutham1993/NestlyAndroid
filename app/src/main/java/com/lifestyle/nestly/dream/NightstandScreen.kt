package com.lifestyle.nestly.dream

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lifestyle.nestly.data.reminder.NextFeedState
import com.lifestyle.nestly.ui.activity.detailText
import com.lifestyle.nestly.ui.components.Format
import com.lifestyle.nestly.ui.theme.NestlyColors
import com.lifestyle.nestly.ui.theme.NestlyType
import com.lifestyle.nestly.ui.theme.SerifDisplay
import com.lifestyle.nestly.ui.components.DoseStatusDot
import com.lifestyle.nestly.widget.QuickBottleResult
import com.lifestyle.nestly.widget.QuickVitaminDResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Near-black so an OLED nightstand stays dark; [NestlyColors.Background] is too bright at 3am. */
private val NightBackground = Color(0xFF0B0C12)
private val NightDim = Color(0xFF98989F)
private val NightFaint = Color(0xFF636366)

/** True red for overdue — must read as "late" from across the room, even half-asleep. */
private val NightOverdue = Color(0xFFFF5147)

private fun heroStyle(size: TextUnit) = TextStyle(
    fontFamily = SerifDisplay,
    fontWeight = FontWeight.Normal,
    fontSize = size,
    lineHeight = size * 1.05f,
)

private fun labelStyle(size: TextUnit) = TextStyle(
    fontWeight = FontWeight.SemiBold,
    fontSize = size,
    lineHeight = size * 1.22f,
    letterSpacing = 4.sp,
)

@Composable
fun NightstandScreen(
    viewModel: NightstandViewModel,
    onLogFeed: suspend () -> QuickBottleResult,
    onLogVitaminD: suspend () -> QuickVitaminDResult,
    onExit: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var logging by remember { mutableStateOf(false) }
    var loggingVitaminD by remember { mutableStateOf(false) }

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
                    QuickBottleResult.SessionNotReady ->
                        "Couldn't add feed yet — sign-in is still loading. Check your connection and tap again."
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
    fun logVitaminD() {
        if (loggingVitaminD) return
        loggingVitaminD = true
        scope.launch {
            try {
                val message = when (val result = onLogVitaminD()) {
                    is QuickVitaminDResult.Saved -> "Vitamin D added at ${Format.time(result.atMs)}"
                    is QuickVitaminDResult.AlreadyGiven -> "Vitamin D was already given today at ${Format.time(result.atMs)}"
                    QuickVitaminDResult.NotSignedIn -> "Couldn't add Vitamin D. Open Nestly and sign in first."
                    QuickVitaminDResult.SessionNotReady ->
                        "Couldn't add Vitamin D yet — sign-in is still loading. Check your connection and tap again."
                    QuickVitaminDResult.NoChild -> "Couldn't add Vitamin D. Open Nestly and select a child first."
                    QuickVitaminDResult.Failed -> "Couldn't add Vitamin D. Check your connection and try again."
                }
                toast(message)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast("Couldn't add Vitamin D. Check your connection and try again.")
            } finally {
                loggingVitaminD = false
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
        val landscape = maxWidth > maxHeight

        // --- Responsive type: EVERY font below is derived from the actual
        // window size reported by BoxWithConstraints, so it recomposes and
        // rescales live on rotation / resize / a different tablet. No fixed
        // sp anywhere in landscape.
        //
        // Landscape: 3 heroes share one row. Each hero column is ~1/3 of the
        // usable width; a 6-char hero ("5:36pm", "1h 43m") is ~3.2em wide in
        // this serif, so fontSize = columnWidth * 0.32 keeps it on one line
        // with margin at any tablet size. Height caps it on short screens.
        val heroSize = if (landscape) {
            val columnWidth = (maxWidth.value - 64f) / 3f
            minOf(columnWidth * 0.32f, maxHeight.value * 0.30f).coerceIn(48f, 180f).sp
        } else {
            minOf(maxWidth.value * 0.23f, maxHeight.value * 0.15f).coerceIn(64f, 150f).sp
        }
        val countdownSize = heroSize

        // All other type scales in proportion to the hero, clamped so it
        // stays sane on very small / very large tablets.
        // e.g. landscape hero 78sp (small tab) -> label 14sp, buttons 24/20sp
        //      landscape hero 130sp (10" tab) -> label 21sp, buttons 36/30sp
        //      landscape hero 180sp (large tab) -> label 26sp, buttons 42/36sp
        val labelSize = (heroSize.value * 0.16f).coerceIn(14f, 26f).sp
        val headerSize = (heroSize.value * 0.21f).coerceIn(18f, 36f).sp
        val supportSize = (heroSize.value * 0.155f).coerceIn(15f, 27f).sp
        val supportSmallSize = (heroSize.value * 0.14f).coerceIn(14f, 24f).sp
        val logTextSize = (heroSize.value * 0.26f).coerceIn(22f, 42f).sp
        val closeTextSize = (heroSize.value * 0.22f).coerceIn(19f, 36f).sp
        // Vitamin D status: larger than the hero labels so red/green reads from across the room.
        // Scales with the window: by hero size, and capped by width so the longest line
        // ("VITAMIN D GIVEN", ~17em with dot and tracking) never clips on a narrow phone.
        val horizontalPadding = if (landscape) 64f else 48f
        val vitaminDSize = minOf(heroSize.value * 0.26f, (maxWidth.value - horizontalPadding) / 17f)
            .coerceAtLeast(16f).sp
        // Button height also grows with the tablet's height in landscape.
        val landscapeButtonHeight = (maxHeight.value * 0.115f).coerceIn(64f, 96f).dp

        val showLog = state?.profileName != null

        if (landscape) {
            // Tablet on a TV console, read from the couch: a single row of
            // NOW | NEXT FEED | IN, then supporting line, then actions
            // side-by-side. Nothing stacks vertically except those 3 bands,
            // so nothing can be pushed off the bottom.
            Column(
                Modifier
                    .offset(x = shiftX, y = shiftY)
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HeaderLine(state, now, headerSize)
                VitaminDStatus(state, loggingVitaminD, vitaminDSize, onLog = { logVitaminD() })
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        HeroNumber(
                            label = "NOW",
                            value = Format.time(now),
                            valueColor = NestlyColors.TextPrimary,
                            fontSize = heroSize,
                            labelSize = labelSize,
                        )
                    }
                    VerticalDivider(
                        color = NestlyColors.Divider.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxHeight(0.55f),
                    )
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        NextFeedHero(state, fontSize = heroSize, labelSize = labelSize)
                    }
                    VerticalDivider(
                        color = NestlyColors.Divider.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxHeight(0.55f),
                    )
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CountdownHero(state, now, fontSize = countdownSize, labelSize = labelSize)
                    }
                }
                SupportingLine(state, now, supportSize, supportSmallSize)
                Spacer(Modifier.height(18.dp))
                BigActions(
                    logging = logging,
                    showLog = showLog,
                    onLog = { logFeed() },
                    onClose = onExit,
                    horizontal = true,
                    logTextSize = logTextSize,
                    closeTextSize = closeTextSize,
                    horizontalButtonHeight = landscapeButtonHeight,
                )
            }
        } else {
            // Phone on a nightstand: one stacked column, heroes top to bottom.
            Column(
                Modifier
                    .offset(x = shiftX, y = shiftY)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                HeaderLine(state, now, headerSize)
                VitaminDStatus(state, loggingVitaminD, vitaminDSize, onLog = { logVitaminD() })
                Spacer(Modifier.height(4.dp))
                HeroNumber(
                    label = "NOW",
                    value = Format.time(now),
                    valueColor = NestlyColors.TextPrimary,
                    fontSize = heroSize,
                    labelSize = labelSize,
                )
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = NestlyColors.Divider.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth(0.86f))
                Spacer(Modifier.height(14.dp))
                NextFeedHero(state, fontSize = heroSize, labelSize = labelSize)
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = NestlyColors.Divider.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth(0.86f))
                Spacer(Modifier.height(14.dp))
                CountdownHero(state, now, fontSize = countdownSize, labelSize = labelSize)
                SupportingLine(state, now, supportSize, supportSmallSize)
                Spacer(Modifier.height(26.dp))
                BigActions(
                    logging = logging,
                    showLog = showLog,
                    onLog = { logFeed() },
                    onClose = onExit,
                    logTextSize = logTextSize,
                    closeTextSize = closeTextSize,
                )
            }
        }
    }
}

@Composable
private fun HeaderLine(state: NightstandState?, now: Long, fontSize: TextUnit = 24.sp) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            state?.profileName ?: "Nestly",
            style = TextStyle(fontFamily = SerifDisplay, fontWeight = FontWeight.Normal, fontSize = fontSize, lineHeight = fontSize * 1.17f),
            color = NightDim,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            "  ·  ${Format.headerDate(now)}",
            style = TextStyle(fontSize = fontSize * 0.83f, lineHeight = fontSize),
            color = NightFaint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/**
 * Daily Vitamin D: pulsing red "tap when given" until today's dose is logged, then a
 * solid green "given" line. Tapping is only possible while it's still missing.
 */
@Composable
private fun VitaminDStatus(
    state: NightstandState?,
    logging: Boolean,
    fontSize: TextUnit,
    onLog: () -> Unit,
) {
    if (state?.profileName == null) return
    val givenAt = state.vitaminDGivenAt
    val color = if (givenAt != null) NestlyColors.DoseGiven else NestlyColors.DoseMissing
    // Short headline at full size; the detail sits under it smaller so neither has to clip.
    val headline = if (givenAt != null) "VITAMIN D GIVEN" else "VITAMIN D DUE"
    val detail = when {
        givenAt != null -> "at ${Format.time(givenAt)}"
        logging -> "saving…"
        else -> "tap when given"
    }
    val hintSize = fontSize * 0.62f
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .then(if (givenAt == null && !logging) Modifier.clickable(onClick = onLog) else Modifier)
            .padding(horizontal = (fontSize.value * 0.6f).dp, vertical = (fontSize.value * 0.25f).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DoseStatusDot(given = givenAt != null, size = (fontSize.value * 0.7f).dp)
        Spacer(Modifier.width((fontSize.value * 0.3f).dp))
        Column {
            Text(
                headline,
                style = labelStyle(fontSize).copy(letterSpacing = fontSize * 0.08f),
                color = color,
                maxLines = 1,
                softWrap = false,
            )
            Text(
                detail,
                style = TextStyle(fontSize = hintSize, lineHeight = hintSize * 1.2f),
                color = if (givenAt != null) NightDim else color.copy(alpha = 0.85f),
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/** One hero number: small tracked-out label above a very large value. */
@Composable
private fun HeroNumber(
    label: String,
    value: String,
    valueColor: Color,
    fontSize: TextUnit,
    labelColor: Color = NightFaint,
    labelSize: TextUnit = 18.sp,
) {
    // Never wrap a hero ("1h 43m" must stay on one line): shrink longer
    // values instead — "11:36pm" / "1h 43m" (6-7 chars) vs "NOW" (3).
    val shrink = when {
        value.length <= 4 -> 1f
        value.length == 5 -> 0.94f
        value.length == 6 -> 0.84f
        value.length == 7 -> 0.74f
        else -> 0.64f
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = labelStyle(labelSize), color = labelColor, textAlign = TextAlign.Center, maxLines = 1, softWrap = false)
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = heroStyle(fontSize * shrink),
            color = valueColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** #2 — next feed time. */
@Composable
private fun NextFeedHero(state: NightstandState?, fontSize: TextUnit, labelSize: TextUnit = 18.sp) {
    val dueMs = when (val next = state?.nextFeed) {
        is NextFeedState.Upcoming -> next.dueMs
        is NextFeedState.DueNow -> next.dueMs
        is NextFeedState.Overdue -> next.dueMs
        else -> null
    }
    when {
        state == null -> HeroNumber("NEXT FEED", "—", NightFaint, fontSize, labelSize = labelSize)
        state.profileName == null -> HeroNumber("NEXT FEED", "—", NightFaint, fontSize, labelSize = labelSize)
        state.lastFeed == null -> HeroNumber("NEXT FEED", "—", NightFaint, fontSize, labelSize = labelSize)
        dueMs != null -> HeroNumber("NEXT FEED", Format.time(dueMs), NestlyColors.TextPrimary, fontSize, labelSize = labelSize)
        else -> HeroNumber("NEXT FEED", "—", NightFaint, fontSize, labelSize = labelSize)
    }
}

/**
 * #3 / #4 — time until the next feed, or (in red) how long overdue it is.
 * Same hero slot so it never jumps around: the label and colour switch
 * from "IN" (amber) to "OVERDUE BY" / "DUE NOW" (red) at the due time.
 */
@Composable
private fun CountdownHero(state: NightstandState?, now: Long, fontSize: TextUnit, labelSize: TextUnit = 18.sp) {
    when {
        state == null ->
            HeroNumber("IN", "…", NightFaint, fontSize, labelSize = labelSize)
        state.profileName == null ->
            Text(
                "Open Nestly to set up your family",
                style = NestlyType.RowLabel,
                color = NightDim,
                textAlign = TextAlign.Center,
            )
        state.lastFeed == null -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HeroNumber("IN", "—", NightFaint, fontSize, labelSize = labelSize)
                Spacer(Modifier.height(8.dp))
                Text("No feeds logged yet — tap LOG FEED", style = NestlyType.RowLabel, color = NightDim, textAlign = TextAlign.Center)
            }
        }
        else -> when (val next = state.nextFeed) {
            is NextFeedState.Upcoming ->
                HeroNumber(
                    label = "IN",
                    value = minutesText(next.remainingMs, roundUp = true),
                    valueColor = NestlyColors.Feed,
                    fontSize = fontSize,
                    labelSize = labelSize,
                )
            is NextFeedState.DueNow ->
                HeroNumber(
                    label = "IN",
                    value = "NOW",
                    valueColor = NightOverdue,
                    fontSize = fontSize,
                    labelColor = NightOverdue,
                    labelSize = labelSize,
                )
            is NextFeedState.Overdue ->
                HeroNumber(
                    label = "OVERDUE BY",
                    value = minutesText(next.overdueMs, roundUp = false),
                    valueColor = NightOverdue,
                    fontSize = fontSize,
                    labelColor = NightOverdue,
                    labelSize = labelSize,
                )
            NextFeedState.NoFeed ->
                HeroNumber("IN", "—", NightFaint, fontSize, labelSize = labelSize)
        }
    }
}

/** Demoted supporting info — deliberately small next to the heroes. */
@Composable
private fun SupportingLine(
    state: NightstandState?,
    now: Long,
    fontSize: TextUnit = 19.sp,
    smallFontSize: TextUnit = 17.sp,
) {
    if (state?.lastFeed == null || state.profileName == null) return
    val lastFeed = state.lastFeed
    Spacer(Modifier.height(16.dp))
    val lastParts = listOfNotNull(
        "Last feed ${Format.time(lastFeed.startTime)}",
        Format.ago(lastFeed.startTime, now),
        lastFeed.detailText(state.units),
    )
    Text(
        lastParts.joinToString("  ·  "),
        style = TextStyle(fontSize = fontSize, lineHeight = fontSize * 1.26f),
        color = NightDim,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(6.dp))
    val todayText = when {
        state.todayFeedCount == 0 -> "No feeds yet today"
        state.todayBottleMl > 0 -> "Today: ${state.todayFeedCount} ${if (state.todayFeedCount == 1) "feed" else "feeds"}  ·  ${Format.volume(state.todayBottleMl, state.units)}"
        else -> "Today: ${state.todayFeedCount} ${if (state.todayFeedCount == 1) "feed" else "feeds"}"
    }
    Text(
        "$todayText  ·  every ~${minutesText(state.intervalMinutes * 60_000L, roundUp = false)}",
        style = TextStyle(fontSize = smallFontSize, lineHeight = smallFontSize * 1.29f),
        color = NightFaint,
        textAlign = TextAlign.Center,
    )
}

/**
 * Big, couch-distance actions: LOG FEED on top, Close directly under it.
 * Replaces the old small corner "Done" — no hunting for a tiny target.
 */
@Composable
private fun BigActions(
    logging: Boolean,
    showLog: Boolean,
    onLog: () -> Unit,
    onClose: () -> Unit,
    horizontal: Boolean = false,
    logTextSize: TextUnit = 30.sp,
    closeTextSize: TextUnit = 25.sp,
    horizontalButtonHeight: androidx.compose.ui.unit.Dp = 76.dp,
) {
    if (horizontal) {
        // Landscape: side-by-side so the buttons take one short band of
        // height instead of stacking and pushing content off-screen.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (showLog) {
                LogFeedButton(logging, onLog, Modifier.weight(1f).height(horizontalButtonHeight), logTextSize)
            }
            CloseButton(onClose, Modifier.weight(1f).height(horizontalButtonHeight), closeTextSize)
        }
    } else {
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (showLog) {
                LogFeedButton(logging, onLog, Modifier.fillMaxWidth().height(84.dp), logTextSize)
            }
            CloseButton(onClose, Modifier.fillMaxWidth().height(68.dp), closeTextSize)
        }
    }
}

@Composable
private fun LogFeedButton(
    logging: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textSize: TextUnit = 30.sp,
) {
    Button(
        onClick = onClick,
        enabled = !logging,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NestlyColors.Feed,
            contentColor = NestlyColors.HeaderText,
            disabledContainerColor = NestlyColors.Feed.copy(alpha = 0.5f),
            disabledContentColor = NestlyColors.HeaderText.copy(alpha = 0.6f),
        ),
    ) {
        Text(
            if (logging) "LOGGING…" else "LOG FEED",
            style = TextStyle(fontWeight = FontWeight.Bold, fontSize = textSize, lineHeight = textSize * 1.13f, letterSpacing = 2.sp),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textSize: TextUnit = 25.sp,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NestlyColors.SurfaceRaised,
            contentColor = NestlyColors.TextPrimary,
        ),
    ) {
        Text(
            "CLOSE",
            style = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = textSize, lineHeight = textSize * 1.2f, letterSpacing = 2.sp),
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** Whole-minutes duration ("11m", "1h 23m"); the dream ticks every 30s, so seconds would jump. */
private fun minutesText(ms: Long, roundUp: Boolean): String {
    val minutes = if (roundUp) (ms + 59_999L) / 60_000L else ms / 60_000L
    // Format.duration renders whole hours as "3h 0m"; trim the dangling zero.
    return Format.duration(minutes.coerceAtLeast(1) * 60).removeSuffix(" 0m")
}
