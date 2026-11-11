package com.lifestyle.nestly.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lifestyle.nestly.R

/** Rounded sans, the Android stand-in for the iOS system rounded face. */
@OptIn(ExperimentalTextApi::class)
val NestlyRounded = FontFamily(
    Font(R.font.nunito, FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(R.font.nunito, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.nunito, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.nunito, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.nunito, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

/** Kept for the nightstand clock, which is sized around this face. */
val SerifDisplay = FontFamily(
    Font(R.font.sourceserif4display_light, FontWeight.Light),
    Font(R.font.sourceserif4display_regular, FontWeight.Normal),
)

val SerifText = FontFamily(Font(R.font.sourceserif4_regular, FontWeight.Normal))

object NestlyType {
    /** Child name, screen titles. Matches iOS large title. */
    val Display = TextStyle(fontFamily = NestlyRounded, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp)
    val SheetTitle = TextStyle(fontFamily = NestlyRounded, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp)
    val CardTitle = TextStyle(fontFamily = NestlyRounded, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp)
    val BigValue = TextStyle(fontFamily = NestlyRounded, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp)
    val SectionTitle = TextStyle(fontFamily = NestlyRounded, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp)
    val RowLabel = TextStyle(fontFamily = NestlyRounded, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp)
    val CardLead = TextStyle(fontFamily = NestlyRounded, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp)
    val Body = TextStyle(fontFamily = NestlyRounded, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 22.sp)
    val BodyMedium = TextStyle(fontFamily = NestlyRounded, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp)
    val Small = TextStyle(fontFamily = NestlyRounded, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
    val Button = TextStyle(fontFamily = NestlyRounded, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp)
    val TimerDigits = TextStyle(fontFamily = NestlyRounded, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp)
    val TimerUnit = TextStyle(fontFamily = NestlyRounded, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 14.sp)
}

val NestlyTypography = Typography(
    headlineMedium = NestlyType.Display,
    titleLarge = NestlyType.CardTitle,
    titleMedium = NestlyType.RowLabel,
    bodyLarge = NestlyType.Body,
    bodyMedium = NestlyType.Small,
    labelLarge = NestlyType.Button,
)
