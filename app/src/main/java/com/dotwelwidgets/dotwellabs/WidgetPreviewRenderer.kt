package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import java.util.Calendar

object WidgetPreviewRenderer {
    fun render(
        context: Context,
        entry: CatalogEntry,
        width: Int,
        height: Int,
        style: WidgetStyle,
    ): Bitmap {
        val now = Calendar.getInstance()
        return when (entry.previewKind) {
            PreviewKind.CLOCK -> NtDigitalRenderer.render(
                context,
                width,
                height,
                style,
                now,
                includeBackground = false,
            )
            PreviewKind.DATE -> NtDateRenderer.render(
                context,
                width,
                height,
                style,
                now,
                includeBackground = false,
            )
            PreviewKind.ANALOG_BOLD -> NtAnalogRenderer.render(
                context,
                width,
                height,
                style,
                now,
                bold = true,
                includeBackground = false,
            )
            PreviewKind.ANALOG_SCALE -> NtAnalogRenderer.render(
                context,
                width,
                height,
                style,
                now,
                bold = false,
                includeBackground = false,
            )
            PreviewKind.WORLD -> NtWorldRenderer.render(
                context,
                width,
                height,
                style,
                WorldPrefs.previewZones(context),
                includeBackground = false,
            )
            PreviewKind.QUOTES -> NtQuotesRenderer.render(
                context,
                width,
                height,
                style,
                QuotePrefs.quoteForToday(context),
                includeBackground = false,
            )
            PreviewKind.PLACEHOLDER -> placeholder(context, entry, width, height, style)
        }
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
}
