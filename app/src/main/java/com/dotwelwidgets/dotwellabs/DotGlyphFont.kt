package com.dotwelwidgets.dotwellabs

import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.min

/**
 * Dotwel's original 5x7 circular-dot digit geometry. This is a deliberate font
 * substitution for the upstream Ndot font files, which are not redistributed.
 * The fitted row proportions and centre positions come from the translated
 * NtDigital/NtWorld composition; the glyph matrix itself is original to Dotwel.
 */
object DotGlyphFont {
    const val COLUMNS = 5
    const val ROWS = 7
    private const val DIGIT_GAP = 1

    private val glyphs: Map<Char, Array<String>> = mapOf(
        '0' to arrayOf("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
        '1' to arrayOf("00100", "01100", "00100", "00100", "00100", "00100", "01110"),
        '2' to arrayOf("01110", "10001", "00001", "00010", "00100", "01000", "11111"),
        '3' to arrayOf("11110", "00001", "00001", "01110", "00001", "00001", "11110"),
        '4' to arrayOf("00010", "00110", "01010", "10010", "11111", "00010", "00010"),
        '5' to arrayOf("11111", "10000", "10000", "11110", "00001", "00001", "11110"),
        '6' to arrayOf("01110", "10000", "10000", "11110", "10001", "10001", "01110"),
        '7' to arrayOf("11111", "00001", "00010", "00100", "01000", "01000", "01000"),
        '8' to arrayOf("01110", "10001", "10001", "01110", "10001", "10001", "01110"),
        '9' to arrayOf("01110", "10001", "10001", "01111", "00001", "00001", "01110"),
        ':' to arrayOf("00000", "00100", "00100", "00000", "00100", "00100", "00000"),
        '%' to arrayOf("11001", "11011", "00110", "00100", "01100", "11011", "10011"),
        ' ' to arrayOf("00000", "00000", "00000", "00000", "00000", "00000", "00000"),
        'D' to arrayOf("11110", "10001", "10001", "10001", "10001", "10001", "11110"),
        'E' to arrayOf("11111", "10000", "10000", "11110", "10000", "10000", "11111"),
        'L' to arrayOf("10000", "10000", "10000", "10000", "10000", "10000", "11111"),
        'O' to arrayOf("01110", "10001", "10001", "10001", "10001", "10001", "01110"),
        'T' to arrayOf("11111", "00100", "00100", "00100", "00100", "00100", "00100"),
        'W' to arrayOf("10001", "10001", "10001", "10101", "10101", "10101", "01010"),
    )

    fun columnsFor(text: String): Int =
        if (text.isEmpty()) 0
        else COLUMNS * text.length + DIGIT_GAP * (text.length - 1)

    /** Equivalent of NtDraw.fit for the "00" reference at a chosen row height. */
    fun fitPitch(text: String, maxWidth: Float, maxHeight: Float): Float {
        val columns = columnsFor(text).coerceAtLeast(1)
        val byWidth = maxWidth / columns.toFloat()
        val byHeight = maxHeight / ROWS.toFloat()
        return min(byWidth, byHeight).coerceAtLeast(0f)
    }

    fun drawLeft(
        canvas: Canvas,
        text: String,
        left: Float,
        centerY: Float,
        pitch: Float,
        paint: Paint,
    ) {
        if (pitch <= 0f) return
        val columns = columnsFor(text)
        if (columns <= 0) return
        draw(canvas, text, left + pitch * 0.5f, centerY, pitch, paint)
    }

    fun drawCentered(
        canvas: Canvas,
        text: String,
        centerX: Float,
        centerY: Float,
        pitch: Float,
        paint: Paint,
    ) {
        if (pitch <= 0f) return
        val columns = columnsFor(text)
        if (columns <= 0) return
        draw(canvas, text, centerX - (columns - 1) * pitch / 2f, centerY, pitch, paint)
    }

    private fun draw(
        canvas: Canvas,
        text: String,
        startX: Float,
        centerY: Float,
        pitch: Float,
        paint: Paint,
    ) {
        val radius = pitch * 0.40f
        val startY = centerY - (ROWS - 1) * pitch / 2f
        var cursor = 0
        text.forEach { character ->
            val glyph = glyphs[character] ?: return@forEach
            glyph.forEachIndexed { row, line ->
                line.forEachIndexed { column, dot ->
                    if (dot == '1') {
                        canvas.drawCircle(
                            startX + (cursor + column) * pitch,
                            startY + row * pitch,
                            radius,
                            paint,
                        )
                    }
                }
            }
            cursor += COLUMNS + DIGIT_GAP
        }
    }
}
