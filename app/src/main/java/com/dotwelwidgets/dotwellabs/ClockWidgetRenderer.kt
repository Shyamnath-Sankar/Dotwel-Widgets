package com.dotwelwidgets.dotwellabs

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import java.util.Calendar
import kotlin.math.roundToInt
import kotlin.math.sqrt

object ClockWidgetRenderer {
    // RemoteViews parcels every bitmap through Binder. Keep the combined budget
    // for all responsive size variants clearly under the 1 MB transaction limit.
    private const val MAX_TOTAL_BITMAP_PIXELS = 480 * 480
    private const val MIN_BITMAP_SIDE = 48
    private const val FALLBACK_SIDE_DP = 110f
    private const val MAX_SIZE_VARIANTS = 4

    // widget_clock.xml applies this padding to the root frame, so the ImageView
    // content area is this much smaller on each side than the host widget size.
    const val CONTENT_INSET_DP = 8f

    data class Target(
        val hostSize: SizeF,
        val widthPx: Int,
        val heightPx: Int,
    )

    fun render(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        style: WidgetStyle,
        now: Calendar = Calendar.getInstance(),
    ): Bitmap? = try {
        DotMatrixRenderer.render(
            context,
            widthPx,
            heightPx,
            style,
            now,
            includeBackground = false,
        )
    } catch (_: OutOfMemoryError) {
        null
    }

    fun exactSizesFromOptions(options: Bundle): List<SizeF> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return emptyList()
        val sizes: List<SizeF>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            options.getParcelableArrayList(
                AppWidgetManager.OPTION_APPWIDGET_SIZES,
                SizeF::class.java,
            )
        } else {
            @Suppress("DEPRECATION")
            options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES)
        }
        return sizes.orEmpty().filter { it.width > 0f && it.height > 0f }
    }

    fun targets(
        context: Context,
        options: Bundle,
        exactSizes: List<SizeF> = exactSizesFromOptions(options),
    ): List<Target> {
        val hostSizes = if (exactSizes.isNotEmpty()) {
            exactSizes
        } else {
            listOf(legacyHostSize(context, options))
        }
        val distinct = hostSizes
            .distinctBy { it.width to it.height }
            .take(MAX_SIZE_VARIANTS)
        val density = context.resources.displayMetrics.density
        val raw = distinct.map { host ->
            val contentWidthDp = (host.width - CONTENT_INSET_DP * 2f).coerceAtLeast(1f)
            val contentHeightDp = (host.height - CONTENT_INSET_DP * 2f).coerceAtLeast(1f)
            host to (
                (contentWidthDp * density).roundToInt().coerceAtLeast(MIN_BITMAP_SIDE) to
                    (contentHeightDp * density).roundToInt().coerceAtLeast(MIN_BITMAP_SIDE)
                )
        }
        val totalPixels = raw.sumOf { (_, pixels) ->
            pixels.first.toLong() * pixels.second.toLong()
        }
        val scale = if (totalPixels > MAX_TOTAL_BITMAP_PIXELS) {
            sqrt(MAX_TOTAL_BITMAP_PIXELS.toDouble() / totalPixels.toDouble())
        } else {
            1.0
        }
        return raw.map { (host, pixels) ->
            Target(
                hostSize = host,
                widthPx = (pixels.first * scale).roundToInt().coerceAtLeast(MIN_BITMAP_SIDE),
                heightPx = (pixels.second * scale).roundToInt().coerceAtLeast(MIN_BITMAP_SIDE),
            )
        }
    }

    private fun legacyHostSize(context: Context, options: Bundle): SizeF {
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
        val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        val portrait =
            context.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
        // Portrait is the narrow/tall pair; landscape is the wide/short pair.
        val width = if (portrait) {
            firstPositive(minWidth, maxWidth)
        } else {
            firstPositive(maxWidth, minWidth)
        }
        val height = if (portrait) {
            firstPositive(maxHeight, minHeight)
        } else {
            firstPositive(minHeight, maxHeight)
        }
        return SizeF(width ?: FALLBACK_SIDE_DP, height ?: FALLBACK_SIDE_DP)
    }

    private fun firstPositive(first: Int, second: Int): Float? = when {
        first > 0 -> first.toFloat()
        second > 0 -> second.toFloat()
        else -> null
    }
}
