package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.dotwelwidgets.dotwellabs.weather.WeatherCanvas
import com.dotwelwidgets.dotwellabs.weather.WeatherDisplay
import com.dotwelwidgets.dotwellabs.weather.WeatherRenderState
import kotlin.math.min

/**
 * Actual-source native port of KWGT Nothing_Temp.
 *
 * Source facts retained: EXAGON six-sided surface with side = min(width,height)
 * and rounded corners side / 9, a centred vertical stack with a 14-unit margin,
 * a large side / 3.5 line using the forecast minimum and a side / 10 line.
 *
 * Repairs / substitutions:
 * - The source large line is `wf(min, 0)` (today's forecast low), not the current
 *   temperature. It is kept as the low and labelled with the unit.
 * - The source small line says `H : min L : max`, which reverses the labels. It
 *   is repaired to `H : max L : min`; the minimum is never presented as the high.
 * - The source has no `h` global for the background height, so the hexagon is
 *   fitted to a centred square of min(width,height). This is a documented
 *   native adaptation, not a new height meaning.
 * - NType82 / NType82Mono are not bundled; the platform sans-serif family is
 *   substituted.
 */
object NothingTempRenderer {
    fun render(
        context: Context,
        width: Int,
        height: Int,
        style: WidgetStyle,
        state: WeatherRenderState,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(
            width.coerceAtLeast(1),
            height.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        val palette = WeatherCanvas.palette(style)
        val side = min(width, height).toFloat()
        val left = (width - side) / 2f
        val top = (height - side) / 2f
        val hex = RectF(left, top, left + side, top + side)
        palette.background?.let { background ->
            WeatherCanvas.drawRoundedHexagon(
                canvas,
                hex,
                side / 9f,
                WeatherCanvas.paint(background),
            )
        }

        val snapshot = state.snapshot
        val today = snapshot?.let { WeatherDisplay.today(it, state.now) }
        val hasForecast = snapshot != null &&
            today != null &&
            today.temperatureMin.isFinite() &&
            today.temperatureMax.isFinite()
        val bigText = if (hasForecast && snapshot != null && today != null) {
            WeatherDisplay.temperature(today.temperatureMin, snapshot.unit)
                ?: "--\u00B0"
        } else {
            "--\u00B0"
        }
        val smallText = if (hasForecast && snapshot != null && today != null) {
            WeatherDisplay.highLowText(today, snapshot.unit) ?: "Forecast unavailable"
        } else {
            "Forecast unavailable"
        }

        val locationLine = when {
            !state.enabled -> "Weather is off \u00B7 Tap to configure"
            !state.hasLocation -> "Choose a city \u00B7 Tap to configure"
            snapshot == null -> "No cached weather \u00B7 Tap to refresh"
            hasForecast && today != null -> buildString {
                append(WeatherDisplay.locationLine(snapshot.location))
                append(" \u00B7 ")
                append(WeatherDisplay.forecastDayLabel(today))
            }
            else -> "Forecast unavailable"
        }
        val updatedLine = if (snapshot != null) {
            WeatherDisplay.statusLine(snapshot, state.now, state.failure)
        } else {
            WeatherDisplay.setupMessage(state)
        }

        val centerX = hex.centerX()
        val centerY = hex.centerY()
        val maxWidth = side * 0.74f
        val bigPaint = WeatherCanvas.textPaint(palette.ink)
        val smallPaint = WeatherCanvas.textPaint(palette.ink)
        val statusPaint = WeatherCanvas.textPaint(palette.ink)
        val gap = side * 14f / 415f

        var bigSize = WeatherCanvas.fitTextSize(
            bigPaint,
            bigText,
            maxWidth,
            side * 0.30f,
            side / 3.5f,
        )
        var smallSize = WeatherCanvas.fitTextSize(
            smallPaint,
            smallText,
            maxWidth,
            side * 0.16f,
            side / 10f,
        )
        var statusSize = WeatherCanvas.fitTextSize(
            statusPaint,
            locationLine,
            maxWidth,
            side * 0.08f,
            side / 22f,
        )
        val updatedPaint = WeatherCanvas.textPaint(palette.accent)
        var updatedSize = WeatherCanvas.fitTextSize(
            updatedPaint,
            updatedLine,
            maxWidth,
            side * 0.08f,
            side / 22f,
        )

        fun heights(): FloatArray {
            bigPaint.textSize = bigSize
            smallPaint.textSize = smallSize
            statusPaint.textSize = statusSize
            updatedPaint.textSize = updatedSize
            return floatArrayOf(
                bigPaint.fontMetrics.descent - bigPaint.fontMetrics.ascent,
                smallPaint.fontMetrics.descent - smallPaint.fontMetrics.ascent,
                statusPaint.fontMetrics.descent - statusPaint.fontMetrics.ascent,
                updatedPaint.fontMetrics.descent - updatedPaint.fontMetrics.ascent,
            )
        }

        var h = heights()
        var total = h.sum() + gap * 3f
        val limit = side * 0.78f
        if (total > limit) {
            val scale = limit / total
            bigSize *= scale
            smallSize *= scale
            statusSize *= scale
            updatedSize *= scale
            h = heights()
            total = h.sum() + gap * 3f
        }
        var y = centerY - total / 2f
        WeatherCanvas.drawCenteredText(canvas, bigPaint, bigText, centerX, y + h[0] / 2f)
        y += h[0] + gap
        WeatherCanvas.drawCenteredText(canvas, smallPaint, smallText, centerX, y + h[1] / 2f)
        y += h[1] + gap
        WeatherCanvas.drawCenteredText(
            canvas,
            statusPaint,
            locationLine,
            centerX,
            y + h[2] / 2f,
        )
        y += h[2] + gap * 0.5f
        WeatherCanvas.drawCenteredText(
            canvas,
            updatedPaint,
            updatedLine,
            centerX,
            y + h[3] / 2f,
        )
        return bitmap
    }
}
