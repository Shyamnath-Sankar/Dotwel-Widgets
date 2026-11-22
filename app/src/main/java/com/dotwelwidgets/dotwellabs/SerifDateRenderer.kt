package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextUtils
import android.view.View
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

object SerifDateRenderer {
    fun render(
        context: Context,
        width: Int,
        height: Int,
        style: WidgetStyle,
        date: Calendar = Calendar.getInstance(),
        includeBackground: Boolean = true,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        if (includeBackground) {
            WidgetSurface.drawBackground(canvas, width, height, context, style)
        }
        drawDate(canvas, width, height, context, date)
        return bitmap
    }

    private fun drawDate(
        canvas: Canvas,
        width: Int,
        height: Int,
        context: Context,
        date: Calendar,
    ) {
        val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        val day = SimpleDateFormat("d", locale).format(date.time)
        val weekday = SimpleDateFormat("EEE", locale).format(date.time)
        val serif = Typeface.create("serif", Typeface.NORMAL)
        val minimumSide = min(width, height).toFloat()
        val margin = minimumSide * 0.10f

        val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = serif
            color = context.getColor(R.color.widget_ink)
            textAlign = Paint.Align.CENTER
        }
        dayPaint.textSize = fitTextSize(
            dayPaint,
            day,
            width - margin * 2f,
            height * 0.66f,
        )
        val metrics = dayPaint.fontMetrics
        val dayBaseline = height * 0.58f - (metrics.ascent + metrics.descent) / 2f
        canvas.drawText(day, width / 2f, dayBaseline, dayPaint)

        val rtl = TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL
        val weekdayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = serif
            color = context.getColor(R.color.dot_accent)
            textAlign = if (rtl) Paint.Align.LEFT else Paint.Align.RIGHT
        }
        weekdayPaint.textSize = minimumSide * 0.15f
        val weekdayWidth = weekdayPaint.measureText(weekday)
        val maxWeekdayWidth = width * 0.42f
        if (weekdayWidth > maxWeekdayWidth && weekdayWidth > 0f) {
            weekdayPaint.textSize *= maxWeekdayWidth / weekdayWidth
        }
        val weekdayX = if (rtl) margin else width - margin
        val weekdayBaseline = margin - weekdayPaint.fontMetrics.ascent
        canvas.drawText(weekday, weekdayX, weekdayBaseline, weekdayPaint)
    }

    private fun fitTextSize(
        paint: Paint,
        text: String,
        maxWidth: Float,
        maxHeight: Float,
    ): Float {
        paint.textSize = 100f
        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        if (bounds.width() <= 0 || bounds.height() <= 0) return 20f
        return 100f * min(
            maxWidth / bounds.width().toFloat(),
            maxHeight / bounds.height().toFloat(),
        )
    }
}
