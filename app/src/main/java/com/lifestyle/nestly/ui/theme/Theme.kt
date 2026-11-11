package com.lifestyle.nestly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun NestlyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkNestlyPalette else LightNestlyPalette
    val scheme = if (darkTheme) darkScheme(palette) else lightScheme(palette)
    CompositionLocalProvider(LocalNestlyColors provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = NestlyTypography,
        ) {
            CompositionLocalProvider(LocalContentColor provides palette.textPrimary, content = content)
        }
    }
}

private fun lightScheme(p: NestlyPalette) = lightColorScheme(
    primary = p.accent,
    onPrimary = Color.White,
    secondary = p.accent,
    background = p.background,
    onBackground = p.textPrimary,
    surface = p.surface,
    onSurface = p.textPrimary,
    surfaceVariant = p.surfaceRaised,
    onSurfaceVariant = p.textSecondary,
    surfaceContainer = p.surface,
    surfaceContainerHigh = p.surfaceRaised,
    surfaceContainerHighest = p.surfaceRaised,
    surfaceContainerLow = p.background,
    outline = p.outline,
    outlineVariant = p.divider,
    error = p.danger,
)

private fun darkScheme(p: NestlyPalette) = darkColorScheme(
    primary = p.accent,
    onPrimary = Color.Black,
    secondary = p.accent,
    background = p.background,
    onBackground = p.textPrimary,
    surface = p.surface,
    onSurface = p.textPrimary,
    surfaceVariant = p.surfaceRaised,
    onSurfaceVariant = p.textSecondary,
    surfaceContainer = p.surface,
    surfaceContainerHigh = p.surfaceRaised,
    surfaceContainerHighest = p.surfaceRaised,
    surfaceContainerLow = p.background,
    outline = p.outline,
    outlineVariant = p.divider,
    error = p.danger,
)
