package com.lifestyle.nestly.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NestlyColorScheme = darkColorScheme(
    primary = NestlyColors.Accent,
    onPrimary = NestlyColors.TextPrimary,
    secondary = NestlyColors.AccentText,
    background = NestlyColors.Background,
    onBackground = NestlyColors.TextPrimary,
    surface = NestlyColors.Surface,
    onSurface = NestlyColors.TextPrimary,
    surfaceVariant = NestlyColors.SurfaceRaised,
    onSurfaceVariant = NestlyColors.TextSecondary,
    surfaceContainer = NestlyColors.Surface,
    surfaceContainerHigh = NestlyColors.SurfaceRaised,
    surfaceContainerHighest = NestlyColors.SurfaceRaised,
    surfaceContainerLow = NestlyColors.Surface,
    outline = NestlyColors.Outline,
    outlineVariant = NestlyColors.Divider,
    error = NestlyColors.Danger,
)

@Composable
fun NestlyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NestlyColorScheme,
        typography = NestlyTypography,
    ) {
        CompositionLocalProvider(LocalContentColor provides NestlyColors.TextPrimary, content = content)
    }
}
