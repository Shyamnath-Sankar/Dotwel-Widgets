package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.min

object WidgetSurface {
    fun drawBackground(
        canvas: Canvas,
        width: Int,
        height: Int,
        context: Context,
        style: WidgetStyle,
    ) {
        val color = when (style) {
            WidgetStyle.BLACK -> Color.BLACK
            WidgetStyle.TRANSLUCENT -> context.getColor(R.color.widget_translucent)
            WidgetStyle.TRANSPARENT -> Color.TRANSPARENT
        }
        val radius = min(width, height) * 0.22f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        canvas.drawRoundRect(
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            radius,
            radius,
            paint,
        )
    }
}
