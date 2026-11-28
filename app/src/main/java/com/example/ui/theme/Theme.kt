package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

enum class AppSurfaceMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    OLED_NIGHT("OLED Night")
}

@Composable
fun DotGridTheme(
    surfaceMode: AppSurfaceMode = AppSurfaceMode.DARK,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val extended = remember(surfaceMode, systemDark) {
        when (surfaceMode) {
            AppSurfaceMode.LIGHT -> LightExtendedColors
            AppSurfaceMode.DARK -> DarkExtendedColors
            AppSurfaceMode.OLED_NIGHT -> OledNightExtendedColors
            AppSurfaceMode.SYSTEM -> if (systemDark) DarkExtendedColors else LightExtendedColors
        }
    }

    val colorScheme = remember(extended) {
        if (extended.isLightMode) {
            lightColorScheme(
                primary = extended.ink,
                onPrimary = extended.cardSurface,
                primaryContainer = extended.pillSurface,
                onPrimaryContainer = extended.ink,
                secondary = extended.mutedInk,
                onSecondary = extended.cardSurface,
                secondaryContainer = extended.pillSurface,
                onSecondaryContainer = extended.ink,
                tertiary = extended.redAccent,
                onTertiary = LightSurface,
                background = extended.canvas,
                onBackground = extended.ink,
                surface = extended.cardSurface,
                onSurface = extended.ink,
                surfaceVariant = extended.pillSurface,
                onSurfaceVariant = extended.mutedInk,
                outline = extended.hairline,
                outlineVariant = extended.dotMatrix,
                error = extended.redAccent,
                onError = LightSurface
            )
        } else {
            darkColorScheme(
                primary = extended.ink,
                onPrimary = extended.canvas,
                primaryContainer = extended.pillSurface,
                onPrimaryContainer = extended.ink,
                secondary = extended.mutedInk,
                onSecondary = extended.canvas,
                secondaryContainer = extended.pillSurface,
                onSecondaryContainer = extended.ink,
                tertiary = extended.redAccent,
                onTertiary = DarkInk,
                background = extended.canvas,
                onBackground = extended.ink,
                surface = extended.cardSurface,
                onSurface = extended.ink,
                surfaceVariant = extended.pillSurface,
                onSurfaceVariant = extended.mutedInk,
                outline = extended.hairline,
                outlineVariant = extended.dotMatrix,
                error = extended.redAccent,
                onError = DarkInk
            )
        }
    }

    CompositionLocalProvider(LocalDotGridColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DotGridTypography,
            content = content
        )
    }
}
