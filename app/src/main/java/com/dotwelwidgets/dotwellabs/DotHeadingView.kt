package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/**
 * A small all-caps heading drawn with Dotwel's original circular-dot glyphs.
 * The XML contentDescription supplies the spoken label; the dots are decorative.
 */
class DotHeadingView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.widget_ink)
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.dot_accent)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val text = "DOTWEL"
        val columns = DotGlyphFont.columnsFor(text)
        val availableWidth = (width - paddingLeft - paddingRight).toFloat()
        val availableHeight = (height - paddingTop - paddingBottom).toFloat()
        val pitch = min(
            availableWidth / (columns + 2).toFloat(),
            availableHeight / DotGlyphFont.ROWS.toFloat(),
        )
        if (pitch <= 0f) return
        val left = paddingLeft.toFloat()
        val centerY = paddingTop + availableHeight / 2f
        DotGlyphFont.drawLeft(canvas, text, left, centerY, pitch, paint)
        val accentX = min(
            width - paddingRight - pitch * 0.6f,
            left + (columns + 1) * pitch,
        )
        canvas.drawCircle(accentX, centerY, pitch * 0.46f, accentPaint)
    }
}
