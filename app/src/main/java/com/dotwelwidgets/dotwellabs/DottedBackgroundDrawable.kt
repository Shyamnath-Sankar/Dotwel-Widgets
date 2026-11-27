package com.dotwelwidgets.dotwellabs

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * The app's quiet dotted backdrop. It is drawn in code so no decorative image
 * asset is bundled or copied.
 */
class DottedBackgroundDrawable(density: Float) : Drawable() {
    private val step = 22f * density
    private val radius = 1.15f * density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1C1F26.toInt()
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        if (bounds.isEmpty) return
        var y = bounds.top + step / 2f
        while (y < bounds.bottom) {
            var x = bounds.left + step / 2f
            while (x < bounds.right) {
                canvas.drawCircle(x, y, radius, paint)
                x += step
            }
            y += step
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
