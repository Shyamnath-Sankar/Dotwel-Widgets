package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Native renderer for `Nothing_Digital3` from AumGupta/KWGT-Widgets
 * (commit 96ca27be4cbff35efec5a86c64dd39fd065c958f, MIT, Copyright (c) 2024
 * Om Gupta). Sanitized source data:
 * assets/kwgt/nothing_digital3.sanitized.json.
 *
 * Retained author geometry and formulas:
 * - `side = min(rwidth, rheight)`, rounded surface corners `side / 5`
 * - four columns by four rows of `side * 48 / 277` circles with a
 *   `side * 12 / 277` stack margin, shifted right by `side / 8.5` and up by
 *   `side / 21.875` like the source Pattern stack
 * - the left dot columns are the accent groups, the first two cells are the
 *   primary text cells, and the right column stays at `accent2`
 * - minute thresholds `mm > 9`, `mm > 29`, `mm > 39` and `mm > 49` select
 *   `accent` or `accent2`; `primary` cells never change
 * - `hh`, `mm` and uppercase `aa` text modules inside their author cells
 *
 * Substitutions and adaptations:
 * - The upstream `ndot-47.ttf` is not redistributed. Dotwel's original
 *   circular-dot glyphs are fitted inside each author text cell.
 * - The source coordinate offsets are clamped only when a host is too narrow
 *   to hold the unmodified grid, so the grid never spills outside the surface.
 * - The source helper "joint" layers stay in the sanitized data but are not
 *   separately rasterized; the visible dot matrix and text cells are drawn.
 */
object KwgtDigital3Renderer {
    const val RED = 0xFFD71921.toInt()

    private const val CORNER_RATIO = 1f / 5f
    private const val DOT_DIAMETER_RATIO = 48f / 277f
    private const val DOT_STEP_RATIO = 60f / 277f
    private const val GRID_OFFSET_X_RATIO = 1f / 8.5f
    private const val GRID_OFFSET_Y_RATIO = 1f / 21.875f
    private const val GRID_EDGE_RATIO = 0.015f
    private const val TEXT_INK_FRACTION = 0.62f
    private const val TEXT_DOT_FRACTION = 0.72f
    private const val TEXT_X_MARGIN = 0.4f
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val LIGHT_INK = 0xFF1B1B1D.toInt()

    internal enum class DotFill {
        PRIMARY,
        ACCENT,
        ACCENT2,
        MINUTE_9,
        MINUTE_29,
        MINUTE_39,
        MINUTE_49,
    }

    private val DOT_FILLS: List<List<DotFill>> = listOf(
        listOf(DotFill.PRIMARY, DotFill.ACCENT, DotFill.ACCENT, DotFill.MINUTE_39),
        listOf(DotFill.PRIMARY, DotFill.ACCENT, DotFill.MINUTE_9, DotFill.ACCENT2),
        listOf(DotFill.ACCENT2, DotFill.MINUTE_49, DotFill.MINUTE_29, DotFill.ACCENT2),
        listOf(DotFill.ACCENT2, DotFill.ACCENT2, DotFill.ACCENT2, DotFill.ACCENT2),
    )

    internal data class Palette(
        val background: Int,
        val foreground: Int,
        val primary: Int,
        val accent: Int,
        val accent2: Int,
        val contrastInk: Int,
    )

    internal data class DotLayout(
        val column: Int,
        val row: Int,
        val fill: DotFill,
        val centerX: Float,
        val centerY: Float,
        val radius: Float,
    ) {
        val diameter: Float get() = radius * 2f
    }

    internal data class TextRegion(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    ) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top
    }

    internal data class Digital3Layout(
        val width: Float,
        val height: Float,
        val side: Float,
        val cornerRadius: Float,
        val dotDiameter: Float,
        val dotStep: Float,
        val gridCenterX: Float,
        val gridCenterY: Float,
        val dots: List<DotLayout>,
        val hourRegion: TextRegion,
        val minuteRegion: TextRegion,
        val ampmRegion: TextRegion,
    ) {
        fun dot(column: Int, row: Int): DotLayout? =
            dots.firstOrNull { it.column == column && it.row == row }
    }

    internal fun palette(style: WidgetStyle): Palette {
        val background = when (style) {
            WidgetStyle.BLACK -> LIGHT_INK
            WidgetStyle.TRANSLUCENT -> 0xA6FFFFFF.toInt()
            WidgetStyle.TRANSPARENT -> 0x00000000
        }
        val foreground = when (style) {
            WidgetStyle.BLACK -> WHITE
            WidgetStyle.TRANSLUCENT -> LIGHT_INK
            WidgetStyle.TRANSPARENT -> WHITE
        }
        val contrastInk = if (Color.alpha(background) >= 0x80) background else WHITE
        return Palette(
            background = background,
            foreground = foreground,
            primary = RED,
            accent = withAlpha(foreground, 35),
            accent2 = withAlpha(foreground, 15),
            contrastInk = contrastInk,
        )
    }

    internal fun dotColor(
        fill: DotFill,
        minute: Int,
        style: WidgetStyle,
    ): Int {
        val palette = palette(style)
        return when (fill) {
            DotFill.PRIMARY -> palette.primary
            DotFill.ACCENT -> palette.accent
            DotFill.ACCENT2 -> palette.accent2
            DotFill.MINUTE_9 -> if (minute > 9) palette.accent else palette.accent2
            DotFill.MINUTE_29 -> if (minute > 29) palette.accent else palette.accent2
            DotFill.MINUTE_39 -> if (minute > 39) palette.accent else palette.accent2
            DotFill.MINUTE_49 -> if (minute > 49) palette.accent else palette.accent2
        }
    }

    internal fun layout(width: Float, height: Float): Digital3Layout {
        val safeWidth = width.coerceAtLeast(1f)
        val safeHeight = height.coerceAtLeast(1f)
        val side = min(safeWidth, safeHeight)
        val cornerRadius = side * CORNER_RATIO
        val dotDiameter = side * DOT_DIAMETER_RATIO
        val dotRadius = dotDiameter / 2f
        val dotStep = side * DOT_STEP_RATIO
        val halfSpan = (dotStep * 3f + dotDiameter) / 2f
        val edge = side * GRID_EDGE_RATIO

        val desiredX = safeWidth / 2f + side * GRID_OFFSET_X_RATIO
        val desiredY = safeHeight / 2f - side * GRID_OFFSET_Y_RATIO
        val minX = halfSpan + edge
        val maxX = (safeWidth - halfSpan - edge).coerceAtLeast(minX)
        val minY = halfSpan + edge
        val maxY = (safeHeight - halfSpan - edge).coerceAtLeast(minY)
        val gridCenterX = desiredX.coerceIn(minX, maxX)
        val gridCenterY = desiredY.coerceIn(minY, maxY)

        val dots = buildList {
            DOT_FILLS.forEachIndexed { column, rows ->
                rows.forEachIndexed { row, fill ->
                    add(
                        DotLayout(
                            column = column,
                            row = row,
                            fill = fill,
                            centerX = gridCenterX + (column - 1.5f) * dotStep,
                            centerY = gridCenterY + (row - 1.5f) * dotStep,
                            radius = dotRadius,
                        ),
                    )
                }
            }
        }
        val hourDot = dots.first { it.column == 0 && it.row == 0 }
        val minuteDot = dots.first { it.column == 1 && it.row == 0 }
        val ampmDot = dots.first { it.column == 1 && it.row == 1 }

        return Digital3Layout(
            width = safeWidth,
            height = safeHeight,
            side = side,
            cornerRadius = cornerRadius,
            dotDiameter = dotDiameter,
            dotStep = dotStep,
            gridCenterX = gridCenterX,
            gridCenterY = gridCenterY,
            dots = dots,
            hourRegion = textRegion(hourDot),
            minuteRegion = textRegion(minuteDot),
            ampmRegion = textRegion(ampmDot),
        )
    }

    fun render(
        context: Context,
        width: Int,
        height: Int,
        style: WidgetStyle,
        date: Calendar = Calendar.getInstance(),
        includeBackground: Boolean = true,
    ): Bitmap {
        val boundary = layout(width.coerceAtLeast(1).toFloat(), height.coerceAtLeast(1).toFloat())
        val bitmap = Bitmap.createBitmap(
            boundary.width.toInt(),
            boundary.height.toInt(),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        if (includeBackground) {
            drawSurface(canvas, style, boundary)
        }
        drawGrid(canvas, boundary, style, date)
        drawText(canvas, boundary, style, date)
        return bitmap
    }

    /**
     * Dot matrix and surface without any time text. Used by the live TextClock
     * fallback so the bitmap can never freeze an hour or minute label.
     */
    fun renderDots(
        context: Context,
        width: Int,
        height: Int,
        style: WidgetStyle,
        date: Calendar = Calendar.getInstance(),
        includeBackground: Boolean = true,
    ): Bitmap {
        val boundary = layout(width.coerceAtLeast(1).toFloat(), height.coerceAtLeast(1).toFloat())
        val bitmap = Bitmap.createBitmap(
            boundary.width.toInt(),
            boundary.height.toInt(),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        if (includeBackground) {
            drawSurface(canvas, style, boundary)
        }
        drawGrid(canvas, boundary, style, date)
        return bitmap
    }

    fun hourText(date: Calendar): String =
        SimpleDateFormat("hh", Locale.getDefault()).format(date.time)

    fun minuteText(date: Calendar): String =
        SimpleDateFormat("mm", Locale.getDefault()).format(date.time)

    fun ampmText(date: Calendar): String =
        SimpleDateFormat("a", Locale.getDefault())
            .format(date.time)
            .uppercase(Locale.getDefault())

    private fun textRegion(dot: DotLayout): TextRegion {
        val inset = dot.radius * TEXT_DOT_FRACTION
        return TextRegion(
            left = dot.centerX - inset,
            top = dot.centerY - inset,
            right = dot.centerX + inset,
            bottom = dot.centerY + inset,
        )
    }

    private fun drawSurface(
        canvas: Canvas,
        style: WidgetStyle,
        boundary: Digital3Layout,
    ) {
        val background = palette(style).background
        if (Color.alpha(background) == 0) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = background }
        canvas.drawRoundRect(
            RectF(0f, 0f, boundary.width, boundary.height),
            boundary.cornerRadius,
            boundary.cornerRadius,
            paint,
        )
    }

    private fun surfacePath(boundary: Digital3Layout): Path = Path().apply {
        addRoundRect(
            RectF(0f, 0f, boundary.width, boundary.height),
            boundary.cornerRadius,
            boundary.cornerRadius,
            Path.Direction.CW,
        )
    }

    private fun drawGrid(
        canvas: Canvas,
        boundary: Digital3Layout,
        style: WidgetStyle,
        date: Calendar,
    ) {
        val minute = date.get(Calendar.MINUTE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.save()
        canvas.clipPath(surfacePath(boundary))
        boundary.dots.forEach { dot ->
            paint.color = dotColor(dot.fill, minute, style)
            canvas.drawCircle(dot.centerX, dot.centerY, dot.radius, paint)
        }
        canvas.restore()
    }

    private fun drawText(
        canvas: Canvas,
        boundary: Digital3Layout,
        style: WidgetStyle,
        date: Calendar,
    ) {
        val palette = palette(style)
        canvas.save()
        canvas.clipPath(surfacePath(boundary))
        drawCellText(
            canvas,
            boundary.hourRegion,
            hourText(date),
            palette.foreground,
            style,
        )
        drawCellText(
            canvas,
            boundary.minuteRegion,
            minuteText(date),
            palette.foreground,
            style,
        )
        drawCellText(
            canvas,
            boundary.ampmRegion,
            ampmText(date),
            palette.contrastInk,
            style,
        )
        canvas.restore()
    }

    private fun drawCellText(
        canvas: Canvas,
        region: TextRegion,
        text: String,
        color: Int,
        style: WidgetStyle,
    ) {
        if (text.isEmpty()) return
        val columns = DotGlyphFont.columnsFor(text).coerceAtLeast(1)
        val pitch = min(
            region.width * TEXT_INK_FRACTION / (columns - TEXT_X_MARGIN),
            region.height * TEXT_INK_FRACTION / DotGlyphFont.ROWS,
        ).coerceAtLeast(0f)
        if (pitch <= 0f) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.color = color
            UpstreamNtDraw.shadow(style, this)
        }
        DotGlyphFont.drawCentered(
            canvas,
            text,
            (region.left + region.right) / 2f,
            (region.top + region.bottom) / 2f,
            pitch,
            paint,
        )
    }

    private fun withAlpha(color: Int, percent: Int): Int = Color.argb(
        (255f * percent / 100f).roundToInt(),
        Color.red(color),
        Color.green(color),
        Color.blue(color),
    )
}
