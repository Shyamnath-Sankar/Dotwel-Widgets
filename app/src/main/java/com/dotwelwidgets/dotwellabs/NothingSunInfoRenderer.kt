package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.dotwelwidgets.dotwellabs.weather.WeatherCanvas
import com.dotwelwidgets.dotwellabs.weather.WeatherDisplay
import com.dotwelwidgets.dotwellabs.weather.WeatherRenderState
import kotlin.math.min

/**
 * Actual-source native port of KWGT Nothing_Sun_Info.
 *
 * Source facts retained from the 277-unit viewport: top label `SUNSET` when the
 * solar state is day and `SUNRISE` otherwise, the corresponding `hh:mma` event,
 * the outer accent half-circle width 260, the red half-circle width 200, the
 * horizontal red base side / 1.3, the red arrow shaft width 1 and height 36 with
 * its 26 x 8 clip and 14-unit rotated head / 1-unit stroke, the three accent
 * lines at side / 1.6, side / 2 and side / 3 with the 16-unit stack gap, the
 * side / 5 rounded background corners, the source 18-unit label and 26-unit
 * time sizes, and the red #D71921 primary.
 *
 * The source has no `h` global for the background height, so the background is
 * fitted to the host bounds. No source font or icon is copied; the platform
 * sans-serif family is used. The source's calendar tap is adapted to this app's
 * weather setup/refresh dialog because these widgets are weather-only.
 */
object NothingSunInfoRenderer {
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
        palette.background?.let { background ->
            val surface = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = background
                this.style = Paint.Style.FILL
            }
            val radius = side / 5f
            canvas.drawRoundRect(
                RectF(0f, 0f, width.toFloat(), height.toFloat()),
                radius,
                radius,
                surface,
            )
        }

        val snapshot = state.snapshot
        val solar = snapshot?.let { WeatherDisplay.solarState(it, state.now) }
        val label = when {
            solar == null -> "SUN"
            solar.isDay -> "SUNSET"
            else -> "SUNRISE"
        }
        val timeText = when {
            solar != null && snapshot != null -> WeatherDisplay.eventTime(
                solar.event,
                snapshot.location,
            )
            !state.enabled -> "Weather off"
            !state.hasLocation -> "Choose city"
            snapshot == null -> "No cache"
            else -> "Unavailable"
        }
        val footer = snapshot?.let {
            buildString {
                append(WeatherDisplay.locationLine(it.location))
                append(" \u00B7 ")
                append(WeatherDisplay.statusLine(it, state.now))
            }
        } ?: WeatherDisplay.setupMessage(state)

        val centerX = left + side / 2f
        val labelPaint = WeatherCanvas.textPaint(palette.ink)
        val timePaint = WeatherCanvas.textPaint(palette.ink)
        val footerPaint = WeatherCanvas.textPaint(
            WeatherCanvas.withAlpha(palette.ink, 0xB0),
        )
        val maxWidth = side * 0.72f

        val labelSize = WeatherCanvas.fitTextSize(
            labelPaint,
            label,
            maxWidth,
            side * 0.08f,
            side * 18f / 277f,
        )
        val timeSize = WeatherCanvas.fitTextSize(
            timePaint,
            timeText,
            maxWidth,
            side * 0.12f,
            side * 26f / 277f,
        )
        val footerSize = WeatherCanvas.fitTextSize(
            footerPaint,
            footer,
            maxWidth,
            side * 0.055f,
            side * 9f / 277f,
        )
        labelPaint.textSize = labelSize
        timePaint.textSize = timeSize
        footerPaint.textSize = footerSize

        val labelY = top + side * 0.085f
        WeatherCanvas.drawCenteredText(canvas, labelPaint, label, centerX, labelY)

        val baseY = top + side * 0.58f
        val outerRadius = side * 260f / 277f / 2f
        val innerRadius = side * 200f / 277f / 2f
        val arcPaint = WeatherCanvas.paint(
            palette.accent,
            (side / 277f).coerceAtLeast(1f),
        )
        canvas.drawArc(
            RectF(
                centerX - outerRadius,
                baseY - outerRadius,
                centerX + outerRadius,
                baseY + outerRadius,
            ),
            180f,
            180f,
            false,
            arcPaint,
        )
        val redArc = WeatherCanvas.paint(
            palette.primary,
            (side / 277f).coerceAtLeast(1f),
        )
        canvas.drawArc(
            RectF(
                centerX - innerRadius,
                baseY - innerRadius,
                centerX + innerRadius,
                baseY + innerRadius,
            ),
            180f,
            180f,
            false,
            redArc,
        )
        val red = WeatherCanvas.paint(palette.primary)
        canvas.drawRect(
            centerX - side / 1.3f / 2f,
            baseY - (side / 277f).coerceAtLeast(0.5f) / 2f,
            centerX + side / 1.3f / 2f,
            baseY + (side / 277f).coerceAtLeast(0.5f) / 2f,
            red,
        )

        drawArrow(
            canvas,
            centerX,
            top,
            baseY,
            side,
            solar?.isDay == true,
            palette.primary,
        )

        val linePaint = WeatherCanvas.paint(
            palette.accent,
            (side / 277f).coerceAtLeast(0.75f),
        )
        val lineGap = side * 16f / 277f
        val lineStart = baseY + lineGap * 0.75f
        val widths = floatArrayOf(side / 1.6f, side / 2f, side / 3f)
        widths.forEachIndexed { index, lineWidth ->
            val y = lineStart + index * lineGap
            canvas.drawLine(centerX - lineWidth / 2f, y, centerX + lineWidth / 2f, y, linePaint)
        }

        val timeY = top + side * 0.815f
        WeatherCanvas.drawCenteredText(canvas, timePaint, timeText, centerX, timeY)
        val footerY = top + side * 0.93f
        WeatherCanvas.drawCenteredText(canvas, footerPaint, footer, centerX, footerY)
        return bitmap
    }

    private fun drawArrow(
        canvas: Canvas,
        centerX: Float,
        top: Float,
        baseY: Float,
        side: Float,
        isDay: Boolean,
        color: Int,
    ) {
        val shaftWidth = (side / 277f).coerceAtLeast(1f)
        val shaftHeight = side * 36f / 277f
        val headRadius = side * 14f / 277f
        val paint = WeatherCanvas.paint(color)
        val stroke = WeatherCanvas.paint(color, (side / 277f).coerceAtLeast(0.75f))
        if (isDay) {
            val bottom = baseY
            val arrowTop = bottom - shaftHeight
            canvas.drawRect(
                centerX - shaftWidth / 2f,
                arrowTop,
                centerX + shaftWidth / 2f,
                bottom,
                paint,
            )
            val head = Path()
            head.moveTo(centerX, bottom + headRadius)
            head.lineTo(centerX + headRadius, bottom)
            head.lineTo(centerX, bottom - headRadius)
            head.lineTo(centerX - headRadius, bottom)
            head.close()
            canvas.drawPath(head, stroke)
        } else {
            val arrowTop = top + side * 0.16f
            val bottom = arrowTop + shaftHeight
            canvas.drawRect(
                centerX - shaftWidth / 2f,
                arrowTop,
                centerX + shaftWidth / 2f,
                bottom,
                paint,
            )
            val head = Path()
            head.moveTo(centerX, arrowTop - headRadius)
            head.lineTo(centerX + headRadius, arrowTop)
            head.lineTo(centerX, arrowTop + headRadius)
            head.lineTo(centerX - headRadius, arrowTop)
            head.close()
            canvas.drawPath(head, stroke)
        }
    }
}
