package com.example.ui.widgets

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.data.telemetry.AstronomyCalculator
import com.example.model.LiveSnapshot
import com.example.model.WidgetConfig
import com.example.model.WidgetThemeMode
import com.example.ui.theme.DarkDot
import com.example.ui.theme.DarkHairline
import com.example.ui.theme.DarkInk
import com.example.ui.theme.DarkMutedInk
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LightDot
import com.example.ui.theme.LightHairline
import com.example.ui.theme.LightInk
import com.example.ui.theme.LightMutedInk
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LightSurfaceVariant
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingRedNight
import com.example.ui.theme.OledCanvas
import com.example.ui.theme.OledDot
import com.example.ui.theme.OledHairline
import com.example.ui.theme.OledInk
import com.example.ui.theme.OledMutedInk
import com.example.ui.theme.OledSurfaceVariant

@Immutable
data class ResolvedWidgetColors(
    val surface: Color,
    val surfaceVariant: Color,
    val ink: Color,
    val mutedInk: Color,
    val dotInactive: Color,
    val border: Color,
    val accent: Color,
    val isOledNight: Boolean,
    val isLight: Boolean
)

object WidgetPaletteResolver {

    fun resolve(
        config: WidgetConfig,
        snapshot: LiveSnapshot,
        isSystemDark: Boolean,
        overrideTheme: WidgetThemeMode? = null
    ): ResolvedWidgetColors {
        val effectiveMode = overrideTheme ?: config.themeMode
        val resolvedMode = when (effectiveMode) {
            WidgetThemeMode.SYSTEM -> if (isSystemDark) WidgetThemeMode.DARK else WidgetThemeMode.LIGHT
            WidgetThemeMode.AUTO_NIGHT -> {
                if (AstronomyCalculator.isNightTimeNow(snapshot.nowMillis, snapshot.settings)) {
                    WidgetThemeMode.OLED_NIGHT
                } else if (isSystemDark) {
                    WidgetThemeMode.DARK
                } else {
                    WidgetThemeMode.LIGHT
                }
            }
            else -> effectiveMode
        }

        val alpha = config.backgroundOpacity.coerceIn(0.4f, 1.0f)

        return when (resolvedMode) {
            WidgetThemeMode.LIGHT -> ResolvedWidgetColors(
                surface = LightSurface.copy(alpha = alpha),
                surfaceVariant = LightSurfaceVariant.copy(alpha = (alpha * 0.9f).coerceIn(0.35f, 1f)),
                ink = LightInk,
                mutedInk = LightMutedInk,
                dotInactive = LightDot,
                border = LightHairline,
                accent = if (config.showRedAccent) NothingRed else LightInk,
                isOledNight = false,
                isLight = true
            )
            WidgetThemeMode.DARK -> ResolvedWidgetColors(
                surface = DarkSurface.copy(alpha = alpha),
                surfaceVariant = DarkSurfaceVariant.copy(alpha = (alpha * 0.9f).coerceIn(0.35f, 1f)),
                ink = DarkInk,
                mutedInk = DarkMutedInk,
                dotInactive = DarkDot,
                border = DarkHairline,
                accent = if (config.showRedAccent) NothingRed else DarkInk,
                isOledNight = false,
                isLight = false
            )
            else -> ResolvedWidgetColors(
                surface = OledCanvas.copy(alpha = alpha),
                surfaceVariant = OledSurfaceVariant.copy(alpha = (alpha * 0.85f).coerceIn(0.3f, 1f)),
                ink = OledInk,
                mutedInk = OledMutedInk,
                dotInactive = OledDot,
                border = OledHairline,
                accent = if (config.showRedAccent) NothingRedNight else OledInk,
                isOledNight = true,
                isLight = false
            )
        }
    }
}
