package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.graphics.Bitmap

/**
 * Battery Mini surface. The source Nothing_Battery_Mini ring geometry stays in
 * `assets/kwgt/nothing_battery_mini.sanitized.json` for provenance, but the
 * visible surface now follows the reference battery family: a white #FFFFFF
 * card, the real level in black dot-matrix and a small rounded progress bar
 * whose accent is red #D71921 only while low, otherwise the palette green
 * #6A9C89.
 */
object KwgtBatteryMiniRenderer {
    fun render(
        context: Context,
        width: Int,
        height: Int,
        style: WidgetStyle,
        level: Int,
        includeBackground: Boolean = false,
    ): Bitmap = ReferenceBatterySurface.render(
        context,
        BatterySurfaceKind.SMALL_PROGRESSBAR,
        width,
        height,
        level,
        BatteryState.isCharging(context),
        style,
    )
}
