package com.example.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.telemetry.AstronomyCalculator
import com.example.model.LiveSnapshot
import com.example.model.WidgetBorderStyle
import com.example.model.WidgetConfig
import com.example.model.WidgetThemeMode
import com.example.ui.widgets.WidgetPaletteResolver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Single source of truth renderer for BOTH the Android Home Screen RemoteViews
 * and in-app Compose previews. Uses proportional dp-exact scaling so aspect ratio,
 * orientation, corner radius, typography, and layout match 100% identically.
 */
object HomeScreenBitmapRenderer {

    @Volatile private var cachedDotFont: Typeface? = null
    @Volatile private var cachedMonoFont: Typeface? = null
    @Volatile private var cachedSansFont: Typeface? = null

    private val previewCache = object : LruCache<String, Bitmap>(16 * 1024) { // ~16 MB max
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }

    fun getOrRenderCachedPreview(
        context: Context,
        config: WidgetConfig,
        snapshot: LiveSnapshot,
        overrideTheme: WidgetThemeMode? = null,
        maxSidePx: Int = 520
    ): Bitmap {
        val minuteBucket = snapshot.nowMillis / 15_000L
        val tel = snapshot.telemetry
        val cacheKey = buildString {
            append(config.widgetTypeId).append('|')
            append(config.gridSize.name).append('|')
            append(overrideTheme?.name ?: config.themeMode.name).append('|')
            append(config.cornerRadiusDp).append('|')
            append((config.backgroundOpacity * 100).roundToInt()).append('|')
            append(config.dotDensity.name).append('|')
            append(config.borderStyle.name).append('|')
            append(config.showRedAccent).append('|')
            append(config.showDotBackdrop).append('|')
            append(config.use24Hour).append('|')
            append(config.monochromePhoto).append('|')
            append(config.photoPath.hashCode()).append('|')
            append(config.timezoneId).append('|')
            append(config.customTitle).append('|')
            append(config.customNote).append('|')
            append(config.goalValue).append('|')
            append(config.currentValue).append('|')
            append(config.bitmaskState).append('|')
            append(config.isTimerRunning).append('|')
            append(tel.batteryPercent).append('|')
            append(tel.wifiEnabled).append('|')
            append(tel.bluetoothEnabled).append('|')
            append(tel.budsConnected).append('|')
            append(tel.mediaVolumePercent).append('|')
            append(tel.isConnected).append('|')
            append(minuteBucket).append('|')
            append(maxSidePx)
        }
        previewCache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return cached
        }
        val rendered = render(
            context = context,
            config = config,
            snapshot = snapshot,
            widthDp = config.gridSize.canonicalWidthDp,
            heightDp = config.gridSize.canonicalHeightDp,
            maxSidePx = maxSidePx,
            overrideTheme = overrideTheme
        )
        previewCache.put(cacheKey, rendered)
        return rendered
    }

    fun render(
        context: Context,
        config: WidgetConfig,
        snapshot: LiveSnapshot,
        widthDp: Float = config.gridSize.canonicalWidthDp,
        heightDp: Float = config.gridSize.canonicalHeightDp,
        maxSidePx: Int = 740,
        overrideTheme: WidgetThemeMode? = null
    ): Bitmap {
        val safeWdp = widthDp.coerceIn(100f, 600f)
        val safeHdp = heightDp.coerceIn(90f, 600f)
        val density = context.resources.displayMetrics.density.coerceIn(1.5f, 4.0f)

        val rawW = safeWdp * density
        val rawH = safeHdp * density
        // STRICTLY PROPORTIONAL scaling so aspect ratio & orientation NEVER distort!
        val scaleDown = min(1f, maxSidePx.toFloat() / max(rawW, rawH))
        val w = (rawW * scaleDown).roundToInt().coerceAtLeast(160)
        val h = (rawH * scaleDown).roundToInt().coerceAtLeast(140)
        val dp = w.toFloat() / safeWdp

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val colors = WidgetPaletteResolver.resolve(config, snapshot, isSystemDark, overrideTheme)

        val surfaceInt = colors.surface.toArgb()
        val variantInt = colors.surfaceVariant.toArgb()
        val inkInt = colors.ink.toArgb()
        val mutedInt = colors.mutedInk.toArgb()
        val dotInt = colors.dotInactive.toArgb()
        val borderInt = colors.border.toArgb()
        val accentInt = colors.accent.toArgb()

        val dotFont = cachedDotFont ?: (runCatching { ResourcesCompat.getFont(context, R.font.dotgothic16) }.getOrNull()
            ?: Typeface.MONOSPACE).also { cachedDotFont = it }
        val monoFont = cachedMonoFont ?: (runCatching { ResourcesCompat.getFont(context, R.font.jetbrains_mono) }.getOrNull()
            ?: Typeface.MONOSPACE).also { cachedMonoFont = it }
        val sansFont = cachedSansFont ?: (runCatching { ResourcesCompat.getFont(context, R.font.space_grotesk) }.getOrNull()
            ?: Typeface.SANS_SERIF).also { cachedSansFont = it }

        val inset = 1.5f * dp
        val rect = RectF(inset, inset, w - inset, h - inset)
        val maxCorner = min(rect.width(), rect.height()) * 0.45f
        val cornerPx = (config.cornerRadiusDp.coerceIn(10, 32) * dp).coerceAtMost(maxCorner)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = surfaceInt
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(rect, cornerPx, cornerPx, bgPaint)

        canvas.save()
        val clipPath = Path().apply { addRoundRect(rect, cornerPx, cornerPx, Path.Direction.CW) }
        canvas.clipPath(clipPath)

        if (config.showDotBackdrop) {
            val step = (config.dotDensity.stepDp * dp).coerceAtLeast(8f * dp)
            val dPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = dotInt
                alpha = 110
            }
            var y = step * 0.7f
            while (y < h - step * 0.3f) {
                var x = step * 0.7f
                while (x < w - step * 0.3f) {
                    canvas.drawCircle(x, y, 0.95f * dp, dPaint)
                    x += step
                }
                y += step
            }
        }

        val cal = Calendar.getInstance().apply { timeInMillis = snapshot.nowMillis }
        val timeText = AstronomyCalculator.formatTime(snapshot.nowMillis, config.use24Hour)
        val dateUpper = SimpleDateFormat("EEE · dd MMM", Locale.US).format(Date(snapshot.nowMillis)).uppercase(Locale.US)

        val ctx = WidgetDrawCtx(
            canvas = canvas,
            w = w.toFloat(),
            h = h.toFloat(),
            dp = dp,
            pad = 15f * dp,
            config = config,
            snapshot = snapshot,
            cal = cal,
            timeText = timeText,
            dateUpper = dateUpper,
            surfaceInt = surfaceInt,
            variantInt = variantInt,
            inkInt = inkInt,
            mutedInt = mutedInt,
            dotInt = dotInt,
            borderInt = borderInt,
            accentInt = accentInt,
            dotFont = dotFont,
            monoFont = monoFont,
            sansFont = sansFont
        )

        if (!ctx.drawGroupA()) {
            ctx.drawGroupB()
        }

        canvas.restore()

        when (config.borderStyle) {
            WidgetBorderStyle.HAIRLINE -> {
                val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = borderInt
                    style = Paint.Style.STROKE
                    strokeWidth = 1.2f * dp
                }
                canvas.drawRoundRect(rect, cornerPx, cornerPx, bp)
            }
            WidgetBorderStyle.DOT_FRAME -> {
                val bp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = mutedInt
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f * dp
                    pathEffect = DashPathEffect(floatArrayOf(3.5f * dp, 6f * dp), 0f)
                }
                val dotInset = 2.5f * dp
                canvas.drawRoundRect(RectF(dotInset, dotInset, w - dotInset, h - dotInset), cornerPx, cornerPx, bp)
            }
            WidgetBorderStyle.BORDERLESS -> Unit
        }

        return bitmap
    }
}
