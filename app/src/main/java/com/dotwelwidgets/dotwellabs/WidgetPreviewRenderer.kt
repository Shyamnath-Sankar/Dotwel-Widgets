package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import java.util.Calendar
import kotlin.math.roundToInt
import kotlin.math.sqrt

object WidgetPreviewRenderer {
    private const val MAX_PREVIEW_PIXELS = 480 * 320
    private const val MIN_PREVIEW_SIDE = 32

    fun render(
        context: Context,
        entry: CatalogEntry,
        width: Int,
        height: Int,
        style: WidgetStyle,
    ): Bitmap? {
        if (usesMinuteBitmap(entry.previewKind) &&
            !MinuteTickScheduler.hasExactAccess(context)
        ) {
            return null
        }
        val size = boundedSize(width, height)
        val now = Calendar.getInstance()
        return when (entry.previewKind) {
            PreviewKind.CLOCK -> NtDigitalRenderer.render(
                context,
                size.width,
                size.height,
                style,
                now,
                includeBackground = false,
            )
            PreviewKind.DATE -> NtDateRenderer.render(
                context,
                size.width,
                size.height,
                style,
                now,
                includeBackground = false,
            )
            PreviewKind.ANALOG_BOLD -> NtAnalogRenderer.render(
                context,
                size.width,
                size.height,
                style,
                now,
                bold = true,
                includeBackground = false,
            )
            PreviewKind.ANALOG_SCALE -> NtAnalogRenderer.render(
                context,
                size.width,
                size.height,
                style,
                now,
                bold = false,
                includeBackground = false,
            )
            PreviewKind.WORLD -> NtWorldRenderer.render(
                context,
                size.width,
                size.height,
                style,
                WorldPrefs.previewZones(context),
                includeBackground = false,
            )
            PreviewKind.QUOTES -> NtQuotesRenderer.render(
                context,
                size.width,
                size.height,
                style,
                QuotePrefs.quoteForToday(context),
                includeBackground = false,
            )
            PreviewKind.KWGT_BATTERY -> KwgtBatteryRenderer.render(
                context,
                size.width,
                size.height,
                style,
                BatteryState.level(context),
                includeBackground = false,
            )
            PreviewKind.KWGT_BATTERY_MINI -> KwgtBatteryMiniRenderer.render(
                context,
                size.width,
                size.height,
                style,
                BatteryState.level(context),
                includeBackground = false,
            )
            PreviewKind.KWGT_BATTERY_2 -> {
                val snapshot = Battery2State.snapshot(context)
                KwgtBattery2Renderer.render(
                    context,
                    size.width,
                    size.height,
                    style,
                    snapshot.level,
                    KwgtBattery2Renderer.estimate(
                        snapshot.level,
                        snapshot.charging,
                        snapshot.remainingMillis,
                    ),
                    includeBackground = true,
                )
            }
            PreviewKind.KWGT_DATE_TIME -> KwgtDateTimeRenderer.render(
                context,
                size.width,
                size.height,
                style,
                now,
                includeBackground = false,
            )
            PreviewKind.KWGT_DATE_TIME_2 -> KwgtDateTime2Renderer.render(
                context,
                size.width,
                size.height,
                style,
                now,
                includeBackground = false,
            )
            PreviewKind.KWGT_DIGITAL_4 -> KwgtDigital4Renderer.render(
                context,
                size.width,
                size.height,
                style,
                now,
                includeBackground = false,
            )
            PreviewKind.KWGT_DIGITAL_3 -> KwgtDigital3Renderer.render(
                context,
                size.width,
                size.height,
                style,
                now,
                includeBackground = false,
            )
            PreviewKind.KWGT_GRADIENT_CLOCK -> KwgtGradientClockRenderer.render(
                context,
                size.width,
                size.height,
                style,
                now,
                includeBackground = false,
            )
            PreviewKind.KWGT_ANALOG_3 -> null
            PreviewKind.KWGT_ANALOG_5 -> null
            PreviewKind.KWGT_ANALOG_5_CLEAR -> null
            PreviewKind.KWGT_MINI_CALENDAR -> KwgtMiniCalendarRenderer.render(
                context,
                size.width,
                size.height,
                style,
                now,
                includeBackground = false,
            )
            PreviewKind.KWGT_CLEAN_INFO -> KwgtCleanInfoRenderer.render(
                context,
                size.width,
                size.height,
                style,
                now,
                BatteryState.level(context),
                includeBackground = true,
            )
            PreviewKind.PLACEHOLDER -> placeholder(context, entry, size.width, size.height, style)
        }
    }

    fun usesMinuteBitmap(kind: PreviewKind): Boolean = when (kind) {
        PreviewKind.CLOCK,
        PreviewKind.ANALOG_BOLD,
        PreviewKind.ANALOG_SCALE,
        PreviewKind.WORLD,
        PreviewKind.KWGT_DATE_TIME,
        PreviewKind.KWGT_DATE_TIME_2,
        PreviewKind.KWGT_DIGITAL_4,
        PreviewKind.KWGT_DIGITAL_3,
        PreviewKind.KWGT_GRADIENT_CLOCK,
        -> true
        else -> false
    }

    private fun boundedSize(width: Int, height: Int): Size {
        val safeWidth = width.coerceAtLeast(MIN_PREVIEW_SIDE)
        val safeHeight = height.coerceAtLeast(MIN_PREVIEW_SIDE)
        val pixels = safeWidth.toLong() * safeHeight.toLong()
        if (pixels <= MAX_PREVIEW_PIXELS) return Size(safeWidth, safeHeight)
        val scale = sqrt(MAX_PREVIEW_PIXELS.toDouble() / pixels.toDouble())
        return Size(
            (safeWidth * scale).roundToInt().coerceAtLeast(MIN_PREVIEW_SIDE),
            (safeHeight * scale).roundToInt().coerceAtLeast(MIN_PREVIEW_SIDE),
        )
    }

    private fun placeholder(
        context: Context,
        entry: CatalogEntry,
        width: Int,
        height: Int,
        style: WidgetStyle,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val ink = UpstreamNtDraw.muted(context, style)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            this.style = Paint.Style.STROKE
            strokeWidth = height * 0.006f
            pathEffect = DashPathEffect(floatArrayOf(height * 0.03f, height * 0.02f), 0f)
        }
        val inset = minOf(width, height) * 0.04f
        canvas.drawRoundRect(
            RectF(inset, inset, width - inset, height - inset),
            inset,
            inset,
            stroke,
        )
        val status = context.getString(entry.state.labelRes)
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textAlign = Paint.Align.CENTER
            textSize = minOf(width, height) * 0.10f
            typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
        }
        canvas.drawText(status, width / 2f, height / 2f, title)
        val caption = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = UpstreamNtDraw.muted(context, style)
            textAlign = Paint.Align.CENTER
            textSize = minOf(width, height) * 0.06f
        }
        canvas.drawText(entry.name, width / 2f, height / 2f + title.textSize * 1.35f, caption)
        return bitmap
    }

    private data class Size(val width: Int, val height: Int)
}
