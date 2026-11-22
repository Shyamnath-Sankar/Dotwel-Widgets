package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

object DotMatrixRenderer {
    private const val GLYPH_COLUMNS = 5
    private const val GLYPH_ROWS = 7
    private const val DIGIT_GAP_COLUMNS = 1
    private const val ROW_GAP_ROWS = 3

    private val glyphs: Map<Char, Array<String>> = mapOf(
        '0' to arrayOf(
            "01110",
            "10001",
            "10001",
            "10001",
            "10001",
            "10001",
            "01110",
        ),
        '1' to arrayOf(
            "00100",
            "01100",
            "00100",
            "00100",
            "00100",
            "00100",
            "01110",
        ),
        '2' to arrayOf(
            "01110",
            "10001",
            "00001",
            "00010",
            "00100",
            "01000",
            "11111",
        ),
        '3' to arrayOf(
            "11110",
            "00001",
            "00001",
            "01110",
            "00001",
            "00001",
            "11110",
        ),
        '4' to arrayOf(
            "00010",
            "00110",
            "01010",
            "10010",
            "11111",
            "00010",
            "00010",
        ),
        '5' to arrayOf(
            "11111",
            "10000",
            "10000",
            "11110",
            "00001",
            "00001",
            "11110",
        ),
        '6' to arrayOf(
            "01110",
            "10000",
            "10000",
            "11110",
            "10001",
            "10001",
            "01110",
        ),
        '7' to arrayOf(
            "11111",
            "00001",
            "00010",
            "00100",
            "01000",
            "01000",
            "01000",
        ),
        '8' to arrayOf(
            "01110",
            "10001",
            "10001",
            "01110",
            "10001",
            "10001",
            "01110",
        ),
        '9' to arrayOf(
            "01110",
            "10001",
            "10001",
            "01111",
            "00001",
            "00001",
            "01110",
        ),
    )

    fun render(
        context: Context,
        width: Int,
        height: Int,
        style: WidgetStyle,
        time: Calendar = Calendar.getInstance(),
        includeBackground: Boolean = true,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (includeBackground) {
            WidgetSurface.drawBackground(canvas, width, height, context, style)
        }
        drawTime(canvas, width, height, context, time)
        return bitmap
    }

    private fun drawTime(
        canvas: Canvas,
        width: Int,
        height: Int,
        context: Context,
        time: Calendar,
    ) {
        val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
        val hour = if (is24Hour) {
            time.get(Calendar.HOUR_OF_DAY)
        } else {
            time.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        }
        val minute = time.get(Calendar.MINUTE)
        val hours = String.format(Locale.US, "%02d", hour)
        val minutes = String.format(Locale.US, "%02d", minute)

        val margin = min(width, height) * 0.08f
        val availableWidth = width - margin * 2f
        val availableHeight = height - margin * 2f
        val blockColumns = GLYPH_COLUMNS * 2 + DIGIT_GAP_COLUMNS
        val blockRows = GLYPH_ROWS * 2 + ROW_GAP_ROWS
        val pitch = min(
            availableWidth / blockColumns.toFloat(),
            availableHeight / blockRows.toFloat(),
        )
        if (pitch <= 0f) return

        val radius = pitch * 0.40f
        val blockWidth = blockColumns * pitch
        val blockHeight = blockRows * pitch
        val startX = (width - blockWidth) / 2f + pitch / 2f
        val startY = (height - blockHeight) / 2f + pitch / 2f

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = context.getColor(R.color.widget_ink)
        }
        drawPair(canvas, paint, hours, startX, startY, pitch, radius)
        drawPair(
            canvas,
            paint,
            minutes,
            startX,
            startY + (GLYPH_ROWS + ROW_GAP_ROWS).toFloat() * pitch,
            pitch,
            radius,
        )
    }

    private fun drawPair(
        canvas: Canvas,
        paint: Paint,
        text: String,
        originX: Float,
        originY: Float,
        pitch: Float,
        radius: Float,
    ) {
        text.forEachIndexed { index, digit ->
            val glyph = glyphs[digit] ?: return@forEachIndexed
            val digitX = originX + index * (GLYPH_COLUMNS + DIGIT_GAP_COLUMNS).toFloat() * pitch
            glyph.forEachIndexed { row, line ->
                line.forEachIndexed { column, dot ->
                    if (dot == '1') {
                        canvas.drawCircle(
                            digitX + column.toFloat() * pitch,
                            originY + row.toFloat() * pitch,
                            radius,
                            paint,
                        )
                    }
                }
            }
        }
    }
}
