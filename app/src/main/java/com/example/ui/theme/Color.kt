package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Nothing / Something OS Signature Accent
val NothingRed = Color(0xFFD71921)
val NothingRedNight = Color(0xFFC0171E)
val StatusAmber = Color(0xFFD9B434)

// Light Mode Tokens (Crisp Cool Paper & Pure Cards — matches reference light screenshot)
val LightCanvas = Color(0xFFF4F4F6)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEEEF0)
val LightInsetArt = Color(0xFFF3F3F5)
val LightInk = Color(0xFF121214)
val LightMutedInk = Color(0xFF6B6B70)
val LightDot = Color(0xFFD4D4D8)
val LightHairline = Color(0xFFE5E5E8)

// Dark Mode Tokens (Matte Obsidian & Elevated Charcoal — matches reference dark screenshot)
val DarkCanvas = Color(0xFF0A0A0C)
val DarkSurface = Color(0xFF232325)
val DarkSurfaceVariant = Color(0xFF2E2E31)
val DarkInsetArt = Color(0xFF101012)
val DarkInk = Color(0xFFF5F5F7)
val DarkMutedInk = Color(0xFF949499)
val DarkDot = Color(0xFF343438)
val DarkHairline = Color(0xFF333337)

// OLED Night Mode Tokens ("Nighting" — True Black Zero-Glare)
val OledCanvas = Color(0xFF000000)
val OledSurface = Color(0xFF141416)
val OledSurfaceVariant = Color(0xFF1E1E21)
val OledInsetArt = Color(0xFF050506)
val OledInk = Color(0xFFC2C2BC)
val OledMutedInk = Color(0xFF66666A)
val OledDot = Color(0xFF222225)
val OledHairline = Color(0xFF222226)

@Immutable
data class DotGridExtendedColors(
    val canvas: Color,
    val cardSurface: Color,
    val pillSurface: Color,
    val insetSurface: Color,
    val ink: Color,
    val mutedInk: Color,
    val dotMatrix: Color,
    val hairline: Color,
    val redAccent: Color,
    val isNightMode: Boolean,
    val isLightMode: Boolean
)

val LightExtendedColors = DotGridExtendedColors(
    canvas = LightCanvas,
    cardSurface = LightSurface,
    pillSurface = LightSurfaceVariant,
    insetSurface = LightInsetArt,
    ink = LightInk,
    mutedInk = LightMutedInk,
    dotMatrix = LightDot,
    hairline = LightHairline,
    redAccent = NothingRed,
    isNightMode = false,
    isLightMode = true
)

val DarkExtendedColors = DotGridExtendedColors(
    canvas = DarkCanvas,
    cardSurface = DarkSurface,
    pillSurface = DarkSurfaceVariant,
    insetSurface = DarkInsetArt,
    ink = DarkInk,
    mutedInk = DarkMutedInk,
    dotMatrix = DarkDot,
    hairline = DarkHairline,
    redAccent = NothingRed,
    isNightMode = false,
    isLightMode = false
)

val OledNightExtendedColors = DotGridExtendedColors(
    canvas = OledCanvas,
    cardSurface = OledSurface,
    pillSurface = OledSurfaceVariant,
    insetSurface = OledInsetArt,
    ink = OledInk,
    mutedInk = OledMutedInk,
    dotMatrix = OledDot,
    hairline = OledHairline,
    redAccent = NothingRedNight,
    isNightMode = true,
    isLightMode = false
)

val LocalDotGridColors = compositionLocalOf { DarkExtendedColors }
