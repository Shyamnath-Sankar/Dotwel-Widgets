package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import java.util.Locale
import kotlin.math.min

/**
 * Original code-drawn motif for categories that have no ready provider. It uses
 * Dotwel's own dot glyphs and a quiet dot field; it never shows a fake reading,
 * device or time.
 */
class CategoryMotifView(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {

    private var label: String = ""

    private val fieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.widget_muted)
        alpha = 40
    }
    private val glyphPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.widget_muted)
        alpha = 150
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.dot_accent)
        alpha = 190
    }

    fun setLabel(value: String) {
        label = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val step = min(width, height) / 11f
        if (step > 0f) {
            var y = step
            while (y < height) {
                var x = step
                while (x < width) {
                    canvas.drawCircle(x, y, step * 0.055f, fieldPaint)
                    x += step
                }
                y += step
            }
        }

        val initial = label.trim().take(1).uppercase(Locale.getDefault())
        if (initial.isEmpty()) return
        val columns = DotGlyphFont.columnsFor(initial).coerceAtLeast(1)
        val pitch = min(
            width / (columns + 3f),
            height / (DotGlyphFont.ROWS + 3f),
        )
        if (pitch <= 0f) return
        DotGlyphFont.drawCentered(
            canvas,
            initial,
            width / 2f,
            height / 2f,
            pitch,
            glyphPaint,
        )
        canvas.drawCircle(
            width / 2f + pitch * (columns / 2f + 1.1f),
            height / 2f - pitch * (DotGlyphFont.ROWS / 2f + 0.9f),
            pitch * 0.55f,
            accentPaint,
        )
    }
}
