package com.example.widget

import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.example.data.local.WidgetPhotoStorage
import com.example.model.LiveSnapshot
import com.example.model.WidgetConfig
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

internal class WidgetDrawCtx(
    val canvas: Canvas,
    val w: Float,
    val h: Float,
    val dp: Float,
    val pad: Float,
    val config: WidgetConfig,
    val snapshot: LiveSnapshot,
    val cal: Calendar,
    val timeText: String,
    val dateUpper: String,
    val surfaceInt: Int,
    val variantInt: Int,
    val inkInt: Int,
    val mutedInt: Int,
    val dotInt: Int,
    val borderInt: Int,
    val accentInt: Int,
    val dotFont: Typeface,
    val monoFont: Typeface,
    val sansFont: Typeface
) {
    val minSide: Float = min(w, h)
    val isWide: Boolean = w > h * 1.35f

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = monoFont
        color = mutedInt
        textSize = 9.5f * dp
    }
    val accentLabelPaint = Paint(labelPaint).apply { color = accentInt }
    val inkLabelPaint = Paint(labelPaint).apply { color = inkInt }

    val heroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = dotFont
        color = inkInt
        textSize = 32f * dp
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = sansFont
        color = inkInt
        textSize = 12f * dp
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    fun fitHeroText(text: String, maxWidth: Float, idealDp: Float, minDp: Float = 14f): Float {
        var sz = idealDp * dp
        val floor = minDp * dp
        heroPaint.textSize = sz
        val measured = heroPaint.measureText(text)
        if (measured > maxWidth && measured > 0f) {
            sz = (sz * (maxWidth / measured)).coerceAtLeast(floor)
            heroPaint.textSize = sz
        }
        return sz
    }

    fun drawTopHeader(
        left: String,
        right: String,
        rightAccent: Boolean = false,
        leftDot: Boolean = false,
        leftDotActive: Boolean = true
    ) {
        val baseY = pad + 9f * dp
        var leftX = pad
        if (leftDot) {
            fillPaint.color = if (leftDotActive) accentInt else dotInt
            canvas.drawCircle(pad + 3.2f * dp, baseY - 3.2f * dp, 3.2f * dp, fillPaint)
            leftX += 10f * dp
        }
        labelPaint.color = mutedInt
        canvas.drawText(left, leftX, baseY, labelPaint)
        if (right.isNotEmpty()) {
            val rp = if (rightAccent) accentLabelPaint else labelPaint
            val rw = rp.measureText(right)
            canvas.drawText(right, w - pad - rw, baseY, rp)
        }
    }

    fun drawStatusPipRight(active: Boolean) {
        val cy = pad + 6f * dp
        fillPaint.color = if (active) accentInt else dotInt
        canvas.drawCircle(w - pad - 4f * dp, cy, 4f * dp, fillPaint)
    }

    fun drawDotBar(y: Float, progress: Float, segments: Int, leftX: Float = pad, rightX: Float = w - pad) {
        val count = segments.coerceAtLeast(1)
        val active = (progress.coerceIn(0f, 1f) * count).toInt().coerceIn(0, count)
        val usableW = (rightX - leftX).coerceAtLeast(20f)
        val step = usableW / count.toFloat()
        val r = min(step * 0.34f, 4.2f * dp).coerceAtLeast(1.8f * dp)
        for (i in 0 until count) {
            val cx = leftX + step * i + step / 2f
            val isTail = active > 0 && i == active - 1
            fillPaint.color = when {
                isTail -> accentInt
                i < active -> inkInt
                else -> dotInt
            }
            canvas.drawCircle(cx, y, if (isTail) r * 1.2f else r, fillPaint)
        }
    }

    fun drawDotRing(cx: Float, cy: Float, radius: Float, progress: Float, dots: Int = 24) {
        val active = (progress.coerceIn(0f, 1f) * dots).toInt().coerceIn(0, dots)
        val dotR = (radius * 0.088f).coerceIn(2.0f * dp, 4.4f * dp)
        for (i in 0 until dots) {
            val a = (-90.0 + (360.0 / dots) * i) * PI / 180.0
            val isHead = active > 0 && i == active - 1
            fillPaint.color = when {
                isHead -> accentInt
                i < active -> inkInt
                else -> dotInt
            }
            canvas.drawCircle(
                cx + (radius * cos(a)).toFloat(),
                cy + (radius * sin(a)).toFloat(),
                if (isHead) dotR * 1.32f else dotR,
                fillPaint
            )
        }
    }

    fun drawWifiGlyph(cx: Float, cy: Float, maxR: Float, isOn: Boolean) {
        val dotR = (maxR * 0.095f).coerceIn(1.8f * dp, 3.8f * dp)
        fillPaint.color = if (isOn) accentInt else dotInt
        canvas.drawCircle(cx, cy, dotR * 1.35f, fillPaint)
        val radii = floatArrayOf(maxR * 0.42f, maxR * 0.72f, maxR)
        val counts = intArrayOf(5, 7, 9)
        for (idx in radii.indices) {
            val rad = radii[idx]
            val n = counts[idx]
            for (i in 0 until n) {
                val deg = -135.0 + (90.0 * i / (n - 1).coerceAtLeast(1))
                val a = deg * PI / 180.0
                fillPaint.color = if (isOn) inkInt else dotInt
                canvas.drawCircle(cx + (rad * cos(a)).toFloat(), cy + (rad * sin(a)).toFloat(), dotR * 0.88f, fillPaint)
            }
        }
    }

    fun drawBluetoothGlyph(cx: Float, cy: Float, bh: Float, isOn: Boolean) {
        val bw = bh * 0.56f
        strokePaint.color = if (isOn) inkInt else dotInt
        strokePaint.strokeWidth = 2.4f * dp
        canvas.drawLine(cx, cy - bh / 2f, cx, cy + bh / 2f, strokePaint)
        canvas.drawLine(cx, cy - bh / 2f, cx + bw * 0.55f, cy - bh * 0.22f, strokePaint)
        canvas.drawLine(cx + bw * 0.55f, cy - bh * 0.22f, cx - bw * 0.55f, cy + bh * 0.24f, strokePaint)
        canvas.drawLine(cx - bw * 0.55f, cy - bh * 0.24f, cx + bw * 0.55f, cy + bh * 0.22f, strokePaint)
        canvas.drawLine(cx + bw * 0.55f, cy + bh * 0.22f, cx, cy + bh / 2f, strokePaint)
        fillPaint.color = if (isOn) accentInt else dotInt
        canvas.drawCircle(cx + bw * 0.88f, cy, 2.8f * dp, fillPaint)
        canvas.drawCircle(cx - bw * 0.88f, cy, 2.8f * dp, fillPaint)
    }

    fun drawEarbudsGlyph(box: RectF, connected: Boolean) {
        val bw = box.width()
        val bh = box.height()
        val mainCol = if (connected) inkInt else mutedInt
        val stemW = bw * 0.14f
        val stemH = bh * 0.48f
        val headR = min(bw, bh) * 0.165f

        val lx = box.left + bw * 0.30f
        val ly = box.top + bh * 0.34f
        fillPaint.color = mainCol
        canvas.drawCircle(lx, ly, headR, fillPaint)
        strokePaint.color = mainCol
        strokePaint.strokeWidth = 1.8f * dp
        canvas.drawRoundRect(RectF(lx - stemW / 2f, ly + headR * 0.25f, lx + stemW / 2f, ly + headR * 0.25f + stemH), stemW / 2f, stemW / 2f, strokePaint)
        fillPaint.color = mainCol
        canvas.drawCircle(lx, ly + headR * 0.85f, 2.1f * dp, fillPaint)

        val rx = box.left + bw * 0.70f
        val ry = box.top + bh * 0.34f
        fillPaint.color = mainCol
        canvas.drawCircle(rx, ry, headR, fillPaint)
        canvas.drawRoundRect(RectF(rx - stemW / 2f, ry + headR * 0.25f, rx + stemW / 2f, ry + headR * 0.25f + stemH), stemW / 2f, stemW / 2f, strokePaint)
        fillPaint.color = if (connected) accentInt else dotInt
        canvas.drawCircle(rx, ry + headR * 0.85f, 2.6f * dp, fillPaint)
    }

    fun drawDotEqualizer(box: RectF, isPlaying: Boolean, volumeFraction: Float) {
        val cols = 7
        val rows = 5
        val dx = box.width() / cols
        val dy = box.height() / rows
        val r = min(dx, dy) * 0.28f
        val heights = floatArrayOf(0.55f, 0.85f, 0.65f, 0.96f, 0.72f, 0.90f, 0.48f)
        for (c in 0 until cols) {
            val lvl = (if (isPlaying) heights[c] else heights[c] * 0.55f) * volumeFraction.coerceAtLeast(0.28f)
            val litRows = (lvl * rows).toInt().coerceIn(1, rows)
            for (row in 0 until rows) {
                val fromBot = rows - 1 - row
                val lit = fromBot < litRows
                val peak = lit && fromBot == litRows - 1 && isPlaying
                fillPaint.color = when {
                    peak -> accentInt
                    lit -> inkInt
                    else -> dotInt
                }
                canvas.drawCircle(
                    box.left + c * dx + dx / 2f,
                    box.top + row * dy + dy / 2f,
                    if (peak) r * 1.18f else r,
                    fillPaint
                )
            }
        }
    }

    fun drawMoonSphere(cx: Float, cy: Float, side: Float, phaseFraction: Float) {
        val grid = 9
        val sx = cx - side / 2f
        val sy = cy - side / 2f
        val cell = side / grid
        val rMax = grid / 2f
        for (row in 0 until grid) {
            for (col in 0 until grid) {
                val nx = (col + 0.5f - rMax) / rMax
                val ny = (row + 0.5f - rMax) / rMax
                if (nx * nx + ny * ny <= 0.96f) {
                    val halfChord = kotlin.math.sqrt((1f - ny * ny).coerceAtLeast(0.01f))
                    val normX = (nx / halfChord).coerceIn(-1f, 1f)
                    val term = cos(phaseFraction * 2f * PI.toFloat())
                    val lit = if (phaseFraction <= 0.5f) normX >= term else normX <= -term
                    fillPaint.color = when {
                        lit -> inkInt
                        row == 1 && col == grid - 3 && phaseFraction < 0.04f -> accentInt
                        else -> dotInt
                    }
                    canvas.drawCircle(
                        sx + (col + 0.5f) * cell,
                        sy + (row + 0.5f) * cell,
                        cell * (if (lit) 0.34f else 0.24f),
                        fillPaint
                    )
                }
            }
        }
    }

    fun drawWrappedText(text: String, x: Float, startY: Float, maxWidth: Float, maxLines: Int, paint: Paint) {
        val words = text.split(" ")
        var line = ""
        var y = startY
        var count = 0
        val lh = paint.textSize * 1.28f
        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) <= maxWidth) {
                line = candidate
            } else {
                if (count < maxLines - 1) {
                    canvas.drawText(line, x, y, paint)
                    count++
                    y += lh
                    line = word
                } else {
                    val trimmed = line.take((line.length - 1).coerceAtLeast(1)) + "…"
                    canvas.drawText(trimmed, x, y, paint)
                    return
                }
            }
        }
        if (line.isNotEmpty() && count < maxLines) {
            canvas.drawText(line, x, y, paint)
        }
    }

    fun drawCroppedPhoto(dest: RectF, circular: Boolean) {
        val srcBmp = WidgetPhotoStorage.loadBitmap(config.photoPath)
        canvas.save()
        val clip = Path().apply {
            if (circular) addOval(dest, Path.Direction.CW)
            else addRoundRect(dest, 13f * dp, 13f * dp, Path.Direction.CW)
        }
        canvas.clipPath(clip)
        if (srcBmp != null) {
            val srcAspect = srcBmp.width.toFloat() / srcBmp.height.coerceAtLeast(1)
            val dstAspect = dest.width() / dest.height().coerceAtLeast(1f)
            val srcRect = if (srcAspect > dstAspect) {
                val cropW = (srcBmp.height * dstAspect).toInt()
                val left = (srcBmp.width - cropW) / 2
                Rect(left, 0, left + cropW, srcBmp.height)
            } else {
                val cropH = (srcBmp.width / dstAspect).toInt()
                val top = (srcBmp.height - cropH) / 2
                Rect(0, top, srcBmp.width, top + cropH)
            }
            val photoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            if (config.monochromePhoto) {
                val cm = ColorMatrix().apply { setSaturation(0f) }
                photoPaint.colorFilter = ColorMatrixColorFilter(cm)
            }
            canvas.drawBitmap(srcBmp, srcRect, dest, photoPaint)
            srcBmp.recycle()
        } else {
            fillPaint.color = variantInt
            canvas.drawRect(dest, fillPaint)
            val step = (min(dest.width(), dest.height()) / 7f).coerceAtLeast(10f * dp)
            val cx = dest.centerX()
            val cy = dest.centerY()
            val maxD = min(dest.width(), dest.height()) * 0.28f
            var yy = dest.top + step * 0.6f
            while (yy < dest.bottom) {
                var xx = dest.left + step * 0.6f
                while (xx < dest.right) {
                    val dist = hypot((xx - cx).toDouble(), (yy - cy).toDouble()).toFloat()
                    val inside = dist < maxD
                    fillPaint.color = if (inside) inkInt else dotInt
                    canvas.drawCircle(xx, yy, (if (inside) 2.2f else 1.2f) * dp, fillPaint)
                    xx += step
                }
                yy += step
            }
            fillPaint.color = accentInt
            canvas.drawCircle(cx + dest.width() * 0.20f, cy - dest.height() * 0.16f, 6.2f * dp, fillPaint)
        }
        canvas.restore()
    }
}
