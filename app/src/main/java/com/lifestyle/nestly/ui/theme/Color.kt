package com.lifestyle.nestly.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.lifestyle.nestly.data.prefs.CardKind

/**
 * Calm, low-saturation colors matching the iOS palette. One warm accent, one tint per activity,
 * with separate light and dark values.
 */
@Immutable
data class NestlyPalette(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val tertiary: Color,
    val segmentSelected: Color,
    val divider: Color,
    val accent: Color,
    val accentText: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val headerText: Color,
    val danger: Color,
    val badge: Color,
    val outline: Color,
    val warning: Color,
    val feed: Color,
    val pump: Color,
    val diaper: Color,
    val sleep: Color,
    val routine: Color,
    val babyFirsts: Color,
    val growth: Color,
    val health: Color,
    val doseMissing: Color,
    val doseGiven: Color,
)

val LightNestlyPalette = NestlyPalette(
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFFFFFFF),
    tertiary = Color(0x1F767680),
    segmentSelected = Color(0xFFFFFFFF),
    divider = Color(0xFFC6C6C8),
    accent = Color(0xFFC0664A),
    accentText = Color(0xFFC0664A),
    textPrimary = Color(0xFF1C1C1E),
    textSecondary = Color(0xFF8E8E93),
    textMuted = Color(0xFFAEAEB2),
    headerText = Color(0xFF1C1C1E),
    danger = Color(0xFFC2493F),
    badge = Color(0xFFC2493F),
    outline = Color(0xFFC7C7CC),
    warning = Color(0xFFC98A2E),
    feed = Color(0xFFD9884F),
    pump = Color(0xFF8E76C4),
    diaper = Color(0xFF4A93BB),
    sleep = Color(0xFF5A68AE),
    routine = Color(0xFF5B9877),
    babyFirsts = Color(0xFFC8962F),
    growth = Color(0xFF3D958C),
    health = Color(0xFFC75A6B),
    doseMissing = Color(0xFFC2493F),
    doseGiven = Color(0xFF5B9877),
)

val DarkNestlyPalette = NestlyPalette(
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surfaceRaised = Color(0xFF2C2C2E),
    tertiary = Color(0x3D767680),
    segmentSelected = Color(0xFF636366),
    divider = Color(0xFF38383A),
    accent = Color(0xFFE08B6E),
    accentText = Color(0xFFE08B6E),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF98989F),
    textMuted = Color(0xFF636366),
    headerText = Color(0xFFFFFFFF),
    danger = Color(0xFFE06A60),
    badge = Color(0xFFE06A60),
    outline = Color(0xFF48484A),
    warning = Color(0xFFE2A94F),
    feed = Color(0xFFEBA06C),
    pump = Color(0xFFAD98E0),
    diaper = Color(0xFF6EB1D6),
    sleep = Color(0xFF8B97D8),
    routine = Color(0xFF7FBA97),
    babyFirsts = Color(0xFFE0B356),
    growth = Color(0xFF5EB7AD),
    health = Color(0xFFE2808E),
    doseMissing = Color(0xFFFF5147),
    doseGiven = Color(0xFF7FBA97),
)

val LocalNestlyColors = staticCompositionLocalOf { LightNestlyPalette }

/** Theme-aware colors. Read from a composable so light and dark follow the iOS palette. */
object NestlyColors {
    private val palette: NestlyPalette
        @Composable @ReadOnlyComposable get() = LocalNestlyColors.current

    val Background: Color @Composable @ReadOnlyComposable get() = palette.background
    val Surface: Color @Composable @ReadOnlyComposable get() = palette.surface
    val SurfaceRaised: Color @Composable @ReadOnlyComposable get() = palette.surfaceRaised
    val Tertiary: Color @Composable @ReadOnlyComposable get() = palette.tertiary
    val SegmentSelected: Color @Composable @ReadOnlyComposable get() = palette.segmentSelected
    val Divider: Color @Composable @ReadOnlyComposable get() = palette.divider
    val Accent: Color @Composable @ReadOnlyComposable get() = palette.accent
    val AccentText: Color @Composable @ReadOnlyComposable get() = palette.accentText
    val TextPrimary: Color @Composable @ReadOnlyComposable get() = palette.textPrimary
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = palette.textSecondary
    val TextMuted: Color @Composable @ReadOnlyComposable get() = palette.textMuted
    val HeaderText: Color @Composable @ReadOnlyComposable get() = palette.headerText
    val Danger: Color @Composable @ReadOnlyComposable get() = palette.danger
    val Badge: Color @Composable @ReadOnlyComposable get() = palette.badge
    val Outline: Color @Composable @ReadOnlyComposable get() = palette.outline
    val Warning: Color @Composable @ReadOnlyComposable get() = palette.warning

    val Feed: Color @Composable @ReadOnlyComposable get() = palette.feed
    val Pump: Color @Composable @ReadOnlyComposable get() = palette.pump
    val Diaper: Color @Composable @ReadOnlyComposable get() = palette.diaper
    val Sleep: Color @Composable @ReadOnlyComposable get() = palette.sleep
    val Routine: Color @Composable @ReadOnlyComposable get() = palette.routine
    val BabyFirsts: Color @Composable @ReadOnlyComposable get() = palette.babyFirsts
    val Growth: Color @Composable @ReadOnlyComposable get() = palette.growth
    val Health: Color @Composable @ReadOnlyComposable get() = palette.health

    val FeedBlob: Color @Composable @ReadOnlyComposable get() = palette.feed
    val PumpBlob: Color @Composable @ReadOnlyComposable get() = palette.pump
    val DiaperBlob: Color @Composable @ReadOnlyComposable get() = palette.diaper
    val SleepBlob: Color @Composable @ReadOnlyComposable get() = palette.sleep
    val RoutineBlob: Color @Composable @ReadOnlyComposable get() = palette.routine
    val BabyFirstsBlob: Color @Composable @ReadOnlyComposable get() = palette.babyFirsts
    val GrowthBlob: Color @Composable @ReadOnlyComposable get() = palette.growth
    val HealthBlob: Color @Composable @ReadOnlyComposable get() = palette.health

    val DoseMissing: Color @Composable @ReadOnlyComposable get() = palette.doseMissing
    val DoseGiven: Color @Composable @ReadOnlyComposable get() = palette.doseGiven
}

val CardKind.headerColor: Color
    @Composable @ReadOnlyComposable get() = when (this) {
        CardKind.FEED -> NestlyColors.Feed
        CardKind.PUMP -> NestlyColors.Pump
        CardKind.DIAPER -> NestlyColors.Diaper
        CardKind.SLEEP -> NestlyColors.Sleep
        CardKind.ROUTINE -> NestlyColors.Routine
        CardKind.BABY_FIRSTS -> NestlyColors.BabyFirsts
        CardKind.GROWTH -> NestlyColors.Growth
        CardKind.HEALTH -> NestlyColors.Health
    }

val CardKind.blobColor: Color
    @Composable @ReadOnlyComposable get() = headerColor
