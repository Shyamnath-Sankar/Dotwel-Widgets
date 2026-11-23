package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

/**
 * Native renderer for Nothing_Mini_Calender from
 * AumGupta/KWGT-Widgets (commit 96ca27be4cbff35efec5a86c64dd39fd065c958f,
 * MIT, Copyright (c) 2024 Om Gupta).
 *
 * Retained author relationships:
 * - weekday `EEE` at `side / 6.291`, two-digit `dd` at `side / 3.282`
 * - five relative-day columns at offsets -2, -1, today, +1, +2
 * - outer columns use the source `accent` (ink at 50% alpha); inner columns and
 *   today use full ink
 * - today has a rounded highlight of `widgetWidth / 5` and a primary
 *   `#FFD71921` dot below the date
 * - the source horizontal margin is 58 of a 916-wide preset, scaled with the
 *   host width
 *
 * Deliberate host adaptation: the source is a wide 916x226 strip whose narrow
 * centre day was the practical result of the upstream preview host aspect ratio.
 * A placed 4x1 host is shorter, so this renderer fits the same five columns into
 * the available height and width with a small positive gap, keeping every glyph
 * on-screen. The source ndot-47.ttf is replaced with Dotwel's app-owned
 * [DotGlyphFont]; no font file is copied.
 *
 * The source highlight is `bg` (contrasting wallpaper tone). Dotwel replaces it
 * with a low-alpha ink wash so it remains visible over all three app-owned
 * widget backgrounds instead of disappearing into the black style.
 *
 * Sanitized source data: assets/kwgt/nothing_mini_calender.sanitized.json.
 */
object KwgtMiniCalendarRenderer {
    private const val RED = 0xFFD71921.toInt()
    private const val WEEKDAY_DIVISOR = 6.291f
    private const val DATE_DIVISOR = 3.282f
    private const val SOURCE_WIDTH = 916f
    private const val SOURCE_MARGIN = 58f
    private const val DOT_BOUND_ROWS = 6.8f
    private const val CONTENT_VERTICAL_RATIO = 0.92f
    private const val COLUMN_TEXT_RATIO = 0.92f
    private const val DOT_RADIUS_RATIO = 0.0397f
    private const val DOT_GAP_RATIO = 0.053f
    private const val ROW_GAP_RATIO = 0.02f
    private const val HIGHLIGHT_ALPHA = 0x24
    private const val ACCENT_ALPHA = 0x80

    fun render(
        context: Context,
        width: Int,
        height: Int,
        style: WidgetStyle,
        date: Calendar = Calendar.getInstance(),
        includeBackground: Boolean = false,
    ): Bitmap {
        val safeWidth = width.coerceAtLeast(1)
        val safeHeight = height.coerceAtLeast(1)
        val bitmap = if (includeBackground) {
            UpstreamNtDraw.surface(context, safeWidth, safeHeight, style)
        } else {
            Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(bitmap)
        val side = min(safeWidth, safeHeight).toFloat()
        val ink = UpstreamNtDraw.ink(context, style)
        val accent = withAlpha(ink, ACCENT_ALPHA)
        val todayHighlight = withAlpha(ink, HIGHLIGHT_ALPHA)
        val days = KwgtMiniCalendarDates.days(date)

        val margin = safeWidth * (SOURCE_MARGIN / SOURCE_WIDTH)
        val gap = max(1f, safeWidth * 0.005f)
        val columnWidth = ((safeWidth - 2f * margin - 4f * gap) / 5f).coerceAtLeast(1f)
        val todayCenter = safeWidth / 2f

        drawTodayHighlight(
            canvas = canvas,
            centerX = todayCenter,
            width = safeWidth.toFloat() / 5f,
            height = safeHeight.toFloat(),
            color = todayHighlight,
        )

        days.forEachIndexed { index, day ->
            val centerX = margin + gap / 2f + columnWidth / 2f +
                index * (columnWidth + gap)
            val color = if (day.offset == -2 || day.offset == 2) accent else ink
            drawColumn(
                canvas = canvas,
                context = context,
                style = style,
                day = day,
                centerX = centerX,
                columnWidth = columnWidth,
                width = safeWidth.toFloat(),
                height = safeHeight.toFloat(),
                side = side,
                color = color,
            )
        }
        return bitmap
    }

    private fun drawTodayHighlight(
        canvas: Canvas,
        centerX: Float,
        width: Float,
        height: Float,
        color: Int,
    ) {
        if (Color.alpha(color) == 0) return
        val halfWidth = width / 2f
        val halfHeight = height * 0.49f
        val corner = min(width, height) * 0.16f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawRoundRect(
            RectF(
                centerX - halfWidth,
                height / 2f - halfHeight,
                centerX + halfWidth,
                height / 2f + halfHeight,
            ),
            corner,
            corner,
            paint,
        )
    }

    private fun drawColumn(
        canvas: Canvas,
        context: Context,
        style: WidgetStyle,
        day: MiniCalendarDay,
        centerX: Float,
        columnWidth: Float,
        width: Float,
        height: Float,
        side: Float,
        color: Int,
    ) {
        val maxTextWidth = min(columnWidth * COLUMN_TEXT_RATIO, width * 0.9f)
        var weekdayPitch = DotGlyphFont.fitPitch(
            day.weekday,
            maxTextWidth,
            side / WEEKDAY_DIVISOR,
        )
        var datePitch = DotGlyphFont.fitPitch(
            day.day,
            maxTextWidth,
            side / DATE_DIVISOR,
        )
        var dotRadius = max(1f, side * DOT_RADIUS_RATIO)
        var rowGap = max(1f, side * ROW_GAP_RATIO)
        var dotGap = max(1f, side * DOT_GAP_RATIO)

        val maxHeight = height * CONTENT_VERTICAL_RATIO
        var total = totalHeight(weekdayPitch, datePitch, rowGap, dotGap, dotRadius)
        if (total > maxHeight) {
            val scale = maxHeight / total
            weekdayPitch *= scale
            datePitch *= scale
            dotRadius = max(0.6f, dotRadius * scale)
            rowGap *= scale
            dotGap *= scale
            total = totalHeight(weekdayPitch, datePitch, rowGap, dotGap, dotRadius)
        }

        val weekdayCenterY = (height - total) / 2f + weekdayPitch * DOT_BOUND_ROWS / 2f
        val dateCenterY = weekdayCenterY + weekdayPitch * DOT_BOUND_ROWS / 2f +
            rowGap + datePitch * DOT_BOUND_ROWS / 2f
        val dotCenterY = dateCenterY + datePitch * DOT_BOUND_ROWS / 2f +
            dotGap + dotRadius

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.color = color
            UpstreamNtDraw.shadow(style, this)
        }
        DotGlyphFont.drawCentered(
            canvas,
            day.weekday,
            centerX,
            weekdayCenterY,
            weekdayPitch,
            paint,
        )
        DotGlyphFont.drawCentered(
            canvas,
            day.day,
            centerX,
            dateCenterY,
            datePitch,
            paint,
        )
        if (day.isToday) {
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = RED
                UpstreamNtDraw.shadow(style, this)
            }
            canvas.drawCircle(centerX, dotCenterY, dotRadius, dotPaint)
        }
    }

    private fun totalHeight(
        weekdayPitch: Float,
        datePitch: Float,
        rowGap: Float,
        dotGap: Float,
        dotRadius: Float,
    ): Float = (weekdayPitch + datePitch) * DOT_BOUND_ROWS + rowGap + dotGap +
        dotRadius * 2f

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}
