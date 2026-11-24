package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.dotwelwidgets.dotwellabs.weather.WeatherDisplay
import com.dotwelwidgets.dotwellabs.weather.WeatherRenderState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

/**
 * Native renderer for `Nothing_Date_Time3` from AumGupta/KWGT-Widgets
 * (commit 96ca27be4cbff35efec5a86c64dd39fd065c958f, MIT, Copyright (c) 2024
 * Om Gupta). Sanitized source data:
 * assets/kwgt/nothing_date_time3.sanitized.json.
 *
 * Retained author geometry and formulas:
 * - `side = min(rwidth, rheight)`
 * - right-aligned stack shifted by `side / 8.5` horizontally and
 *   `side / 30.083` vertically
 * - time `side / 2.8`, AM/PM `side / 5`, date `side / 6.5`,
 *   weather `side / 8`
 * - date `df(EEE), df(d) df(MMM)` and weather
 *   `wi(temp)°wi(tempu) wi(cond), AQI aq(index)`
 *
 * Repairs and substitutions:
 * - The central time TextModule has no source expression, so the native port
 *   supplies the live `hh:mm` readout the preview and AM/PM sibling imply.
 * - The source global is literally named `accent ` with a trailing space. The
 *   native port reads the intended `accent` colour and records the typo repair
 *   instead of silently changing unrelated source data.
 * - Weather values are optional, manual-city, cache-only values. A missing AQI
 *   renders `AQI --`; the widget never invents a reading.
 */
object KwgtDateTime3Renderer {
    const val SOURCE_WIDTH = 916f
    const val SOURCE_HEIGHT = 542f

    private const val TIME_RATIO = 1f / 2.8f
    private const val AMPM_RATIO = 1f / 5f
    private const val DATE_RATIO = 1f / 6.5f
    private const val WEATHER_RATIO = 1f / 8f
    private const val RIGHT_INSET_RATIO = 1f / 8.5f
    private const val TOP_INSET_RATIO = 1f / 30.083f

    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val LIGHT_INK = 0xFF1B1B1D.toInt()

    data class Palette(
        val background: Int,
        val foreground: Int,
        val accent: Int,
    )

    data class Layout(
        val width: Float,
        val height: Float,
        val side: Float,
        val rightInset: Float,
        val timeSize: Float,
        val ampmSize: Float,
        val dateSize: Float,
        val weatherSize: Float,
        val contentCenterY: Float,
    ) {
        val textRight: Float get() = width - rightInset
    }

    fun palette(style: WidgetStyle): Palette {
        val background = when (style) {
            WidgetStyle.BLACK -> LIGHT_INK
            WidgetStyle.TRANSLUCENT -> 0xA6FFFFFF.toInt()
            WidgetStyle.TRANSPARENT -> 0x00000000
        }
        val foreground = when (style) {
            WidgetStyle.BLACK -> WHITE
            WidgetStyle.TRANSLUCENT -> LIGHT_INK
            WidgetStyle.TRANSPARENT -> WHITE
        }
        return Palette(
            background = background,
            foreground = foreground,
            accent = withAlpha(foreground, 50),
        )
    }

    fun layout(width: Int, height: Int): Layout {
        val safeWidth = width.coerceAtLeast(1).toFloat()
        val safeHeight = height.coerceAtLeast(1).toFloat()
        val side = min(safeWidth, safeHeight)
        return Layout(
            width = safeWidth,
            height = safeHeight,
            side = side,
            rightInset = side * RIGHT_INSET_RATIO,
            timeSize = side * TIME_RATIO,
            ampmSize = side * AMPM_RATIO,
            dateSize = side * DATE_RATIO,
            weatherSize = side * WEATHER_RATIO,
            contentCenterY = safeHeight / 2f + side * TOP_INSET_RATIO,
        )
    }

    fun render(
        context: Context,
        width: Int,
        height: Int,
        style: WidgetStyle,
        state: WeatherRenderState,
        now: Long = System.currentTimeMillis(),
    ): Bitmap {
        val safeWidth = width.coerceAtLeast(2)
        val safeHeight = height.coerceAtLeast(2)
        val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val layout = layout(safeWidth, safeHeight)
        val palette = palette(style)
        if (palette.background != 0) {
            canvas.drawRect(
                RectF(0f, 0f, layout.width, layout.height),
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.background },
            )
        }
        val time = SimpleDateFormat("hh:mm", Locale.getDefault()).format(Date(now))
        val ampm = SimpleDateFormat("a", Locale.getDefault())
            .format(Date(now))
            .uppercase(Locale.getDefault())
        val date = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(now))
        val weather = weatherLine(state)
        val timePaint = textPaint(palette.foreground, layout.timeSize, Paint.Align.RIGHT)
        val ampmPaint = textPaint(palette.accent, layout.ampmSize, Paint.Align.RIGHT)
        val datePaint = textPaint(palette.foreground, layout.dateSize, Paint.Align.RIGHT)
        val weatherPaint = textPaint(palette.accent, layout.weatherSize, Paint.Align.RIGHT)

        val timeBaseline = layout.contentCenterY
        canvas.drawText(time, layout.textRight, timeBaseline, timePaint)
        val ampmWidth = ampmPaint.measureText(ampm)
        canvas.drawText(
            ampm,
            layout.textRight - timePaint.measureText(time) - layout.side * 0.02f,
            timeBaseline,
            ampmPaint,
        )
        val dateBaseline = timeBaseline + layout.dateSize * 1.55f
        canvas.drawText(date, layout.textRight, dateBaseline, datePaint)
        val weatherBaseline = dateBaseline + layout.weatherSize * 1.85f
        canvas.drawText(weather, layout.textRight, weatherBaseline, weatherPaint)
        if (ampmWidth <= 0f) {
            canvas.drawText(ampm, layout.textRight, timeBaseline, ampmPaint)
        }
        return bitmap
    }

    fun weatherLine(state: WeatherRenderState): String {
        val snapshot = state.snapshot ?: return "Weather unavailable"
        val temp = WeatherDisplay.currentTemperatureText(snapshot) ?: "Weather unavailable"
        val condition = WeatherDisplay.currentConditionText(snapshot)
        return "$temp $condition, ${WeatherDisplay.airQualityText(snapshot)}"
    }

    private fun textPaint(color: Int, size: Float, align: Paint.Align): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size.coerceAtLeast(1f)
            textAlign = align
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            isSubpixelText = true
        }

    private fun withAlpha(color: Int, alphaPercent: Int): Int {
        val alpha = (((color ushr 24) and 0xFF) * alphaPercent / 100).coerceIn(0, 255)
        return (alpha shl 24) or (color and 0x00FFFFFF)
    }
}
