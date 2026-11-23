package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
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

    /**
     * Source `joint_helpers.block.size` is `side / (277 / 20)`; a round-capped
     * connector of that thickness reproduces the authored bridge and its
     * `circle_size` mask ends.
     */
    private const val BRIDGE_THICKNESS_RATIO = 20f / 277f

    /** Styled dot glyphs occupy roughly 60% of the source cell diameter. */
    private const val TEXT_INK_HEIGHT_FRACTION = 0.60f
    private const val TEXT_INK_WIDTH_FRACTION = 0.90f

    /** Fixed live TextClock em relative to the fitted source circle diameter. */
    internal const val LIVE_CLOCK_EM_FRACTION = 0.85f

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
        val centerX: Float get() = (left + right) / 2f
        val centerY: Float get() = (top + bottom) / 2f
    }

    /** Fixed-size live TextClock allocation centred on a source cell. */
    internal data class LiveCell(
        val left: Float,
        val top: Float,
        val width: Float,
        val height: Float,
        val textSizePx: Float,
    )

    /** A source `joint_helpers` bridge between two adjacent source dots. */
    internal data class Bridge(
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val fill: DotFill,
    )

    internal data class Digital3Layout(
        val width: Float,
        val height: Float,
        val side: Float,
        val contentLeft: Float,
        val contentTop: Float,
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
        val contrastInk = if (((background ushr 24) and 0xFF) >= 0x80) background else WHITE
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
        val contentLeft = (safeWidth - side) / 2f
        val contentTop = (safeHeight - side) / 2f
        val cornerRadius = side * CORNER_RATIO
        val dotDiameter = side * DOT_DIAMETER_RATIO
        val dotRadius = dotDiameter / 2f
        val dotStep = side * DOT_STEP_RATIO
        val halfSpan = (dotStep * 3f + dotDiameter) / 2f
        val edge = side * GRID_EDGE_RATIO

        val desiredX = contentLeft + side / 2f + side * GRID_OFFSET_X_RATIO
        val desiredY = contentTop + side / 2f - side * GRID_OFFSET_Y_RATIO
        val minX = contentLeft + halfSpan + edge
        val maxX = (contentLeft + side - halfSpan - edge).coerceAtLeast(minX)
        val minY = contentTop + halfSpan + edge
        val maxY = (contentTop + side - halfSpan - edge).coerceAtLeast(minY)
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
            contentLeft = contentLeft,
            contentTop = contentTop,
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

    /**
     * Fixed text size and allocation for a live platform TextClock inside one
     * source cell. Width uses the author step so two glyphs fit; height uses the
     * author circle so hour/minute/AM-PM never overlap.
     */
    internal fun liveCell(region: TextRegion, boundary: Digital3Layout): LiveCell {
        val cellWidth = boundary.dotStep
        val cellHeight = boundary.dotDiameter
        return LiveCell(
            left = region.centerX - cellWidth / 2f,
            top = region.centerY - cellHeight / 2f,
            width = cellWidth,
            height = cellHeight,
            textSizePx = boundary.dotDiameter * LIVE_CLOCK_EM_FRACTION,
        )
    }

    /**
     * Source `joint_helpers` visibility. Horizontal entries are
     * `[gap][row]` for the three column gaps; vertical entries are the source
     * four-column visibility list. A bridge inherits the colour of the dot it
     * leaves (left/upper), matching the authored joint masks.
     */
    internal fun bridges(boundary: Digital3Layout, minute: Int): List<Bridge> {
        val result = ArrayList<Bridge>()
        val horizontal = listOf(
            listOf(true, true, minute > 9, false),
            listOf(false, false, minute > 29, false),
            listOf(false, minute > 39, minute > 49, false),
        )
        horizontal.forEachIndexed { gap, rows ->
            rows.forEachIndexed { row, visible ->
                if (!visible) return@forEachIndexed
                val from = boundary.dot(gap, row) ?: return@forEachIndexed
                val to = boundary.dot(gap + 1, row) ?: return@forEachIndexed
                result.add(
                    Bridge(
                        startX = from.centerX,
                        startY = from.centerY,
                        endX = to.centerX,
                        endY = to.centerY,
                        fill = from.fill,
                    ),
                )
            }
        }
        val verticalColumns = listOf(false, false, true, true)
        verticalColumns.forEachIndexed { column, visibleColumn ->
            if (!visibleColumn) return@forEachIndexed
            for (row in 0 until 3) {
                val from = boundary.dot(column, row) ?: continue
                val to = boundary.dot(column, row + 1) ?: continue
                result.add(
                    Bridge(
                        startX = from.centerX,
                        startY = from.centerY,
                        endX = to.centerX,
                        endY = to.centerY,
                        fill = from.fill,
                    ),
                )
            }
        }
        return result
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
            boundary.side.roundToInt().coerceAtLeast(1),
            boundary.side.roundToInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        canvas.translate(-boundary.contentLeft, -boundary.contentTop)
        if (includeBackground) {
            drawSurface(canvas, style, boundary)
        }
        drawGrid(canvas, boundary, style, date)
        drawText(canvas, boundary, style, date)
        return bitmap
    }

    /**
     * Dot matrix and surface without any time text. Used by the live TextClock
     * fallback so the bitmap can never freeze an hour or minute label. The
     * bitmap is a square source canvas; the host ImageView letterboxes it with
     * the same fitCenter placement used by the styled bitmap.
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
            boundary.side.roundToInt().coerceAtLeast(1),
            boundary.side.roundToInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        canvas.translate(-boundary.contentLeft, -boundary.contentTop)
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

    private fun textRegion(dot: DotLayout): TextRegion = TextRegion(
        left = dot.centerX - dot.radius,
        top = dot.centerY - dot.radius,
        right = dot.centerX + dot.radius,
        bottom = dot.centerY + dot.radius,
    )

    private fun drawSurface(
        canvas: Canvas,
        style: WidgetStyle,
        boundary: Digital3Layout,
    ) {
        val background = palette(style).background
        if (((background ushr 24) and 0xFF) == 0) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = background }
        canvas.drawRoundRect(
            contentRect(boundary),
            boundary.cornerRadius,
            boundary.cornerRadius,
            paint,
        )
    }

    private fun contentRect(boundary: Digital3Layout): RectF = RectF(
        boundary.contentLeft,
        boundary.contentTop,
        boundary.contentLeft + boundary.side,
        boundary.contentTop + boundary.side,
    )

    private fun surfacePath(boundary: Digital3Layout): Path = Path().apply {
        addRoundRect(
            contentRect(boundary),
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
        val bridgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = boundary.side * BRIDGE_THICKNESS_RATIO
        }
        bridges(boundary, minute).forEach { bridge ->
            bridgePaint.color = dotColor(bridge.fill, minute, style)
            canvas.drawLine(
                bridge.startX,
                bridge.startY,
                bridge.endX,
                bridge.endY,
                bridgePaint,
            )
        }
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
            boundary.dotDiameter,
        )
        drawCellText(
            canvas,
            boundary.minuteRegion,
            minuteText(date),
            palette.foreground,
            style,
            boundary.dotDiameter,
        )
        drawCellText(
            canvas,
            boundary.ampmRegion,
            ampmText(date),
            palette.contrastInk,
            style,
            boundary.dotDiameter,
        )
        canvas.restore()
    }

    internal data class GlyphInk(
        val pitch: Float,
        val width: Float,
        val height: Float,
    )

    /** Fits visible dot-glyph ink to the source cell diameter. */
    internal fun glyphInk(text: String, dotDiameter: Float): GlyphInk? {
        if (text.isEmpty() || dotDiameter <= 0f) return null
        val bounds = DotGlyphFont.metrics(text)
        if (bounds.inkWidthUnits <= 0f || bounds.inkHeightUnits <= 0f) return null
        var pitch = dotDiameter * TEXT_INK_HEIGHT_FRACTION / bounds.inkHeightUnits
        val width = bounds.inkWidthUnits * pitch
        val maxWidth = dotDiameter * TEXT_INK_WIDTH_FRACTION
        if (width > maxWidth) {
            pitch *= maxWidth / width
        }
        if (pitch <= 0f) return null
        return GlyphInk(
            pitch = pitch,
            width = bounds.inkWidthUnits * pitch,
            height = bounds.inkHeightUnits * pitch,
        )
    }

    private fun drawCellText(
        canvas: Canvas,
        region: TextRegion,
        text: String,
        color: Int,
        style: WidgetStyle,
        dotDiameter: Float,
    ) {
        val ink = glyphInk(text, dotDiameter) ?: return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.color = color
            UpstreamNtDraw.shadow(style, this)
        }
        DotGlyphFont.drawCenteredInk(
            canvas,
            text,
            region.centerX,
            region.centerY,
            ink.pitch,
            paint,
        )
    }

    private fun withAlpha(color: Int, percent: Int): Int =
        ((255f * percent / 100f).roundToInt() shl 24) or (color and 0x00FFFFFF)
}
