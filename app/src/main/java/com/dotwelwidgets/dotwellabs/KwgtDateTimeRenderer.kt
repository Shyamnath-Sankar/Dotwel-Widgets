package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

/**
 * Readable Kotlin conversion of the author layer geometry in
 * AumGupta/KWGT-Widgets widgets/Nothing_Date_Time.kwgt (commit
 * 96ca27be4cbff35efec5a86c64dd39fd065c958f, MIT, Copyright (c) 2024 Om Gupta).
 *
 * Retained from preset.json:
 * - side = min(widget width, widget height)
 * - rounded background corners = side / 5 (preset literal 58)
 * - left stack offset x = side / 8.5
 * - stack margin = -4 preset units, interpreted as -4/415 of side
 * - four text rows: EEE, hh:mma, MMMM, d + ordinal suffix
 * - text size side / 7 for weekday, month and day; side / 6 for the time
 * - primary colour #FFD71921 for the time row
 *
 * Substitutions: the bundled ndot-47.ttf is replaced by Dotwel's original
 * circular-dot glyphs. Each row is width-fitted so long localized month names
 * do not clip. Sanitized source data:
 * assets/kwgt/nothing_date_time.sanitized.json.
 */
object KwgtDateTimeRenderer {
    private const val ACCENT = 0xFFD71921.toInt()
    private const val STACK_MARGIN_RATIO = 4f / 415f
    private const val CORNER_RATIO = 1f / 5f
    private const val LEFT_RATIO = 1f / 8.5f
    private const val RIGHT_PADDING_RATIO = 1f / 20f

    private data class Line(val text: String, val baseTextSize: Float, val color: Int)

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
        val bitmap = Bitmap.createBitmap(
            safeWidth,
            safeHeight,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        val side = min(safeWidth, safeHeight).toFloat()

        if (includeBackground) {
            val background = UpstreamNtDraw.background(context, style)
            if (Color.alpha(background) != 0) {
                val radius = side * CORNER_RATIO
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = background }
                canvas.drawRoundRect(
                    RectF(0f, 0f, safeWidth.toFloat(), safeHeight.toFloat()),
                    radius,
                    radius,
                    paint,
                )
            }
        }

        val locale = Locale.getDefault()
        val weekday = SimpleDateFormat("EEE", locale)
            .format(date.time)
            .uppercase(locale)
        val time = SimpleDateFormat("hh:mma", locale)
            .format(date.time)
            .uppercase(locale)
        val month = SimpleDateFormat("MMMM", locale)
            .format(date.time)
            .uppercase(locale)
        val dayNumber = date.get(Calendar.DAY_OF_MONTH)
        val day = "$dayNumber${ordinalSuffix(dayNumber)}".uppercase(locale)

        val ink = UpstreamNtDraw.ink(context, style)
        val lines = listOf(
            Line(weekday, side / 7f, ink),
            Line(time, side / 6f, ACCENT),
            Line(month, side / 7f, ink),
            Line(day, side / 7f, ink),
        )

        val left = side * LEFT_RATIO
        val maxTextWidth = (safeWidth - left - side * RIGHT_PADDING_RATIO)
            .coerceAtLeast(1f)
        val pitches = lines.map { line ->
            val columns = DotGlyphFont.columnsFor(line.text).coerceAtLeast(1)
            min(line.baseTextSize / DotGlyphFont.ROWS, maxTextWidth / columns)
        }
        val lineHeights = pitches.map { it * DotGlyphFont.ROWS }
        val gap = -side * STACK_MARGIN_RATIO
        val totalHeight = lineHeights.sum() + gap * (lines.size - 1)
        var top = safeHeight / 2f - totalHeight / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)
        lines.forEachIndexed { index, line ->
            val lineHeight = lineHeights[index]
            paint.color = line.color
            paint.setShadowLayer(0f, 0f, 0f, 0)
            UpstreamNtDraw.shadow(style, paint)
            DotGlyphFont.drawLeft(
                canvas,
                line.text,
                left,
                top + lineHeight / 2f,
                pitches[index],
                paint,
            )
            top += lineHeight + gap
        }
        return bitmap
    }

    fun ordinalSuffix(dayOfMonth: Int): String {
        if (dayOfMonth % 100 in 11..13) return "TH"
        return when (dayOfMonth % 10) {
            1 -> "ST"
            2 -> "ND"
            3 -> "RD"
            else -> "TH"
        }
    }
}
