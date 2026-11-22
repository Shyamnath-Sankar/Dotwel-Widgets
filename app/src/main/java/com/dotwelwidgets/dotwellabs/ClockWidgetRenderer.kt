package com.dotwelwidgets.dotwellabs

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import java.util.Calendar
import kotlin.math.roundToInt
import kotlin.math.sqrt

object ClockWidgetRenderer {
    // RemoteViews parcels bitmaps through Binder. Keep the total far below the
    // 1.5 MB RemoteViews bitmap budget so launchers accept every update.
    private const val MAX_BITMAP_PIXELS = 512 * 512
    private const val MIN_BITMAP_SIDE = 48
    private const val FALLBACK_SIDE_DP = 110

    fun render(
        context: Context,
        options: Bundle,
        style: WidgetStyle,
        now: Calendar = Calendar.getInstance(),
    ): Bitmap? {
        val (width, height) = targetPixels(context, options)
        return try {
            DotMatrixRenderer.render(
                context,
                width,
                height,
                style,
                now,
                includeBackground = false,
            )
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    fun targetPixels(context: Context, options: Bundle): Pair<Int, Int> {
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

        val widthDp = (if (minWidth > 0) minWidth else maxWidth).takeIf { it > 0 }
            ?: FALLBACK_SIDE_DP
        val heightDp = (if (minHeight > 0) minHeight else maxHeight).takeIf { it > 0 }
            ?: FALLBACK_SIDE_DP

        val density = context.resources.displayMetrics.density
        var width = (widthDp * density).roundToInt().coerceAtLeast(MIN_BITMAP_SIDE)
        var height = (heightDp * density).roundToInt().coerceAtLeast(MIN_BITMAP_SIDE)

        val pixels = width.toLong() * height.toLong()
        if (pixels > MAX_BITMAP_PIXELS) {
            val scale = sqrt(MAX_BITMAP_PIXELS.toDouble() / pixels.toDouble())
            width = (width * scale).roundToInt().coerceAtLeast(MIN_BITMAP_SIDE)
            height = (height * scale).roundToInt().coerceAtLeast(MIN_BITMAP_SIDE)
        }
        return width to height
    }
}
