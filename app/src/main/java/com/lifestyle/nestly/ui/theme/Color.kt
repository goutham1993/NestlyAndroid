package com.lifestyle.nestly.ui.theme

import androidx.compose.ui.graphics.Color
import com.lifestyle.nestly.data.prefs.CardKind

object NestlyColors {
    val Background = Color(0xFF222536)
    val Surface = Color(0xFF2D3142)
    val SurfaceRaised = Color(0xFF363A4D)
    val SegmentSelected = Color(0xFF4F5569)
    val Divider = Color(0xFF41455A)
    val Accent = Color(0xFF5579AE)
    val AccentText = Color(0xFF6E91C8)
    val TextPrimary = Color(0xFFF1F1F4)
    val TextSecondary = Color(0xFFB3B6C4)
    val TextMuted = Color(0xFF8A8EA0)
    val HeaderText = Color(0xFF23263A)
    val Danger = Color(0xFFE8703A)
    val Badge = Color(0xFFE5633A)
    val Outline = Color(0xFFB8BBC8)

    val Feed = Color(0xFFFFC94D)
    val Pump = Color(0xFFF4A99A)
    val Diaper = Color(0xFFEDE6D6)
    val Sleep = Color(0xFFB7E0EE)
    val Routine = Color(0xFFD9C5F2)
    val BabyFirsts = Color(0xFFA8DDE3)
    val Growth = Color(0xFFA9DB8E)
    val Health = Color(0xFFC9CBF2)

    val FeedBlob = Color(0xFFF2B937)
    val PumpBlob = Color(0xFFEC8C7C)
    val DiaperBlob = Color(0xFFCFC6B2)
    val SleepBlob = Color(0xFF8FB8CF)
    val RoutineBlob = Color(0xFF9C86CF)
    val BabyFirstsBlob = Color(0xFF7FC4CC)
    val GrowthBlob = Color(0xFF8CC46F)
    val HealthBlob = Color(0xFFA5A8E0)
}

val CardKind.headerColor: Color
    get() = when (this) {
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
    get() = when (this) {
        CardKind.FEED -> NestlyColors.FeedBlob
        CardKind.PUMP -> NestlyColors.PumpBlob
        CardKind.DIAPER -> NestlyColors.DiaperBlob
        CardKind.SLEEP -> NestlyColors.SleepBlob
        CardKind.ROUTINE -> NestlyColors.RoutineBlob
        CardKind.BABY_FIRSTS -> NestlyColors.BabyFirstsBlob
        CardKind.GROWTH -> NestlyColors.GrowthBlob
        CardKind.HEALTH -> NestlyColors.HealthBlob
    }
