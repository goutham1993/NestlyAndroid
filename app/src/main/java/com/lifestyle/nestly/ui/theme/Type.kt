package com.lifestyle.nestly.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lifestyle.nestly.R

val SerifDisplay = FontFamily(
    Font(R.font.sourceserif4display_light, FontWeight.Light),
    Font(R.font.sourceserif4display_regular, FontWeight.Normal),
)

val SerifText = FontFamily(Font(R.font.sourceserif4_regular, FontWeight.Normal))

object NestlyType {
    /** Child name in the header, sheet titles. */
    val Display = TextStyle(fontFamily = SerifDisplay, fontWeight = FontWeight.Light, fontSize = 34.sp, lineHeight = 40.sp)
    val SheetTitle = TextStyle(fontFamily = SerifDisplay, fontWeight = FontWeight.Light, fontSize = 32.sp, lineHeight = 38.sp)
    val CardTitle = TextStyle(fontFamily = SerifDisplay, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 34.sp)
    val BigValue = TextStyle(fontFamily = SerifDisplay, fontWeight = FontWeight.Light, fontSize = 56.sp, lineHeight = 60.sp)
    val SectionTitle = TextStyle(fontFamily = SerifDisplay, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 32.sp)
    /** Form labels such as "Start Time", list titles. */
    val RowLabel = TextStyle(fontFamily = SerifText, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 26.sp)
    val CardLead = TextStyle(fontFamily = SerifText, fontWeight = FontWeight.Normal, fontSize = 20.sp, lineHeight = 24.sp)
    val Body = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp)
    val BodyMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp)
    val Small = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 18.sp)
    val Button = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp)
    val TimerDigits = TextStyle(fontWeight = FontWeight.Light, fontSize = 56.sp, lineHeight = 60.sp)
    val TimerUnit = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 14.sp)
}

val NestlyTypography = Typography(
    headlineMedium = NestlyType.Display,
    titleLarge = NestlyType.CardTitle,
    titleMedium = NestlyType.RowLabel,
    bodyLarge = NestlyType.Body,
    bodyMedium = NestlyType.Small,
    labelLarge = NestlyType.Button,
)
