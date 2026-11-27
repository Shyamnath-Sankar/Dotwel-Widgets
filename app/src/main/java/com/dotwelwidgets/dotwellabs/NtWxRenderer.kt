package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.dotwelwidgets.dotwellabs.weather.WeatherDisplay
import com.dotwelwidgets.dotwellabs.weather.WeatherRenderState
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Source translation candidate, native sweep and visual acceptance pending.
 * AwesomeAWidgets 0fae607, NtWxDraw.render circle/square branches and temp helper.
 * MIT Copyright (c) 2026 Sameer Al Sahab.
 * Fonts use platform families. Icons are supplied by the caller as original art,
 * not the upstream drawable payloads. Kinds2-8 are in NtWxExtraRenderer.
 */
object NtWxRenderer {
    enum class Kind(val sourceKind: Int, val pages: Int) {
        CIRCLE(0, 3), SQUARE(1, 2), RECT(2, 1), COMBO(3, 1),
        SUNSET(4, 1), AIR(5, 1), W1_TEMP(6, 1), W1_STATUS(7, 1), W1_RAIN(8, 1),
    }

    fun interface IconDrawer {
        fun draw(canvas: Canvas, code: Int, isDay: Boolean, cx: Float, cy: Float, side: Float, color: Int)
    }

    fun page(kind: Kind, requested: Int): Int = Math.floorMod(requested, kind.pages)

    /** All source branches are present; native verification remains pending. */
    fun supports(kind: Kind): Boolean = true

    fun render(
        context: Context, kind: Kind, width: Int, height: Int, style: WidgetStyle,
        state: WeatherRenderState, requestedPage: Int, icons: IconDrawer,
    ): Bitmap {
        require(supports(kind)) { "NtWx source branch not implemented: $kind" }
        val w = width.coerceAtLeast(1).toFloat()
        val h = height.coerceAtLeast(1).toFloat()
        val side = min(w, h)
        val bitmap = Bitmap.createBitmap(w.toInt(), h.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bg = UpstreamNtDraw.background(context, style)
        val ink = UpstreamNtDraw.ink(context, style)
        val muted = UpstreamNtDraw.muted(context, style)
        val pad = side * .10f
        if (Color.alpha(bg) > 0) {
            val p = paint(bg, 1f)
            if (kind == Kind.CIRCLE || kind.sourceKind >= 6) {
                canvas.drawOval(RectF((w-side)/2, (h-side)/2, (w+side)/2, (h+side)/2), p)
            } else if (kind != Kind.COMBO) {
                val radius = if (kind == Kind.SUNSET || kind == Kind.AIR) h/2 else side*.22f
                canvas.drawRoundRect(RectF(0f, 0f, w, h), radius, radius, p)
            }
        }
        val snapshot = state.snapshot
        if (snapshot == null) {
            val text = when {
                !state.enabled -> "Weather is off"
                !state.hasLocation -> "Set your city"
                else -> "No cached weather"
            }
            text(canvas, text, w/2, h/2-side*.02f, side*.11f, muted, Paint.Align.CENTER, side*.85f)
            return bitmap
        }
        val today = WeatherDisplay.today(snapshot, state.now)
        val selectedPage = page(kind, requestedPage)
        if (kind == Kind.CIRCLE) {
            when (selectedPage) {
                0 -> icons.draw(canvas, snapshot.current.weatherCode, snapshot.current.isDay, w/2, h/2, side*.46f, ink)
                1 -> {
                    val value = number(snapshot.current.temperature)
                    val size = fitTemperature(value, side*.62f, side*.40f)
                    temperature(canvas, value, w/2, h/2+size*.35f, size, Paint.Align.CENTER, ink)
                }
                else -> {
                    arrowTemperature(canvas, true, today?.temperatureMax, w/2, h/2-side*.13f, side*.19f, ink)
                    arrowTemperature(canvas, false, today?.temperatureMin, w/2, h/2+side*.13f, side*.19f, ink)
                }
            }
        } else if (kind == Kind.SQUARE && selectedPage == 0) {
            icons.draw(canvas, snapshot.current.weatherCode, snapshot.current.isDay,
                pad+side*.11f, pad+side*.11f, side*.22f, ink)
            text(canvas, snapshot.location.name.substringBefore(','), w-pad, pad+side*.07f,
                side*.075f, muted, Paint.Align.RIGHT, w*.45f)
            temperature(canvas, number(snapshot.current.temperature), pad, h-pad, side*.34f, Paint.Align.LEFT, ink)
        } else if (kind == Kind.SQUARE) {
            arrowTemperature(canvas, true, today?.temperatureMax, pad+side*.25f, pad+side*.14f, side*.19f, ink)
            arrowTemperature(canvas, false, today?.temperatureMin, pad+side*.25f, pad+side*.40f, side*.19f, ink)
            text(canvas, condition(snapshot.current.weatherCode), pad, h-pad, side*.09f, muted,
                Paint.Align.LEFT, w-pad*2)
        } else {
            NtWxExtraRenderer.draw(context, canvas, kind, w, h, style, state, icons)
        }
        return bitmap
    }

    internal fun number(value: Double?): String =
        if (value != null && value.isFinite()) value.roundToInt().toString() else "--"

    private fun paint(color: Int, size: Float, align: Paint.Align = Paint.Align.LEFT): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = align
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        }

    internal fun text(canvas: Canvas, value: String, x: Float, baseline: Float, size: Float,
                     color: Int, align: Paint.Align, maxWidth: Float) {
        val p = paint(color, size, align)
        val fitted = android.text.TextUtils.ellipsize(value, android.text.TextPaint(p),
            maxWidth.coerceAtLeast(1f), android.text.TextUtils.TruncateAt.END).toString()
        canvas.drawText(fitted, x, baseline, p)
    }

    internal fun fitTemperature(value: String, maxWidth: Float, requested: Float): Float {
        val p = paint(Color.WHITE, requested)
        return min(requested, requested * maxWidth / (p.measureText(value)+requested*.36f).coerceAtLeast(1f))
    }

    /** Source temp() draws eight degree dots, preserving its 0.36-size advance. */
    internal fun temperature(canvas: Canvas, value: String, anchorX: Float, baseline: Float,
                            size: Float, align: Paint.Align, color: Int) {
        val p = paint(color, size)
        val digitsWidth = p.measureText(value)
        val total = digitsWidth+size*.36f
        val x = when (align) {
            Paint.Align.CENTER -> anchorX-total/2
            Paint.Align.RIGHT -> anchorX-total
            else -> anchorX
        }
        canvas.drawText(value, x, baseline, p)
        val cx = x+digitsWidth+size*.18f
        val cy = baseline-size*.58f
        for (i in 0..7) {
            val angle = i*Math.PI/4
            canvas.drawCircle(cx+cos(angle).toFloat()*size*.12f,
                cy+sin(angle).toFloat()*size*.12f, size*.04f, p)
        }
    }

    internal fun arrowTemperature(canvas: Canvas, high: Boolean, value: Double?, x: Float,
                                 y: Float, size: Float, color: Int) {
        val valueText=number(value)
        val digits=paint(color,size).measureText(valueText)+size*.36f
        val left=x-(digits+size)/2
        // Original arrow geometry substitutes the upstream drawable, same source bounds.
        val centerX=left+size*.375f
        val r=size*.25f
        val p=paint(color,1f).apply { style=Paint.Style.STROKE;strokeWidth=size*.07f;strokeCap=Paint.Cap.ROUND }
        val direction=if(high) -1f else 1f
        canvas.drawLine(centerX,y-r,centerX,y+r,p)
        canvas.drawLine(centerX,y+direction*r,centerX-r*.6f,y,p)
        canvas.drawLine(centerX,y+direction*r,centerX+r*.6f,y,p)
        temperature(canvas,valueText,left+size,y+size*.36f,size,Paint.Align.LEFT,color)
    }

    internal fun condition(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Showers"
        in 85..86 -> "Snow showers"
        in 95..99 -> "Thunderstorm"
        else -> "Weather"
    }
}
