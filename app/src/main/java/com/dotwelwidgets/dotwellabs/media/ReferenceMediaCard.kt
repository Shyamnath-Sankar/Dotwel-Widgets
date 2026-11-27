package com.dotwelwidgets.dotwellabs.media

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.dotwelwidgets.dotwellabs.DotGlyphFont
import com.dotwelwidgets.dotwellabs.WidgetStyle
import java.util.Locale
import kotlin.math.min

/**
 * Reference media-controls card: pure white #FFFFFF surface, album art at the
 * top-left, the artist in red #D71921 dot-matrix, the title in black dot-matrix
 * and the play/pause control at the top-right. The card stays white for every
 * style so the reference contrast is preserved, matching the Bluetooth battery
 * card decision.
 *
 * The upstream dot font is not bundled; Dotwel's original uppercase 5x7 dot
 * glyphs are used, so text is upper-cased and any glyph the original set does
 * not contain is skipped rather than substituted with a different typeface.
 */
internal object ReferenceMediaCard {
    private const val WHITE = 0xFFFFFFFF.toInt()
    private const val RED = 0xFFD71921.toInt()
    private const val BLACK = 0xFF000000.toInt()
    private const val GRAY = 0xFF777379.toInt()
    private const val ART_FALLBACK = 0xFFEDEDED.toInt()

    fun cardColor(style: WidgetStyle): Int = WHITE

    fun artistColor(): Int = RED

    fun titleColor(): Int = BLACK

    fun displayText(value: String?, fallback: String): String {
        val cleaned = value?.trim().orEmpty()
        val source = if (cleaned.isEmpty()) fallback else cleaned
        return source.uppercase(Locale.US)
    }

    fun truncateDot(value: String, maxColumns: Int): String {
        if (maxColumns <= 0) return ""
        var end = value.length
        while (end > 0 && DotGlyphFont.columnsFor(value.substring(0, end)) > maxColumns) {
            end--
        }
        return value.substring(0, end)
    }

    data class ControlLayout(
        val previousCenterX: Float,
        val playCenterX: Float,
        val nextCenterX: Float,
        val centerY: Float,
        val playRadius: Float,
        val skipSize: Float,
        val volumeUpCenterX: Float,
        val volumeDownCenterX: Float,
        val volumeCenterY: Float,
    )

    fun controlLayout(kind: MediaWidgetKind): ControlLayout {
        val width = kind.designWidth
        val height = kind.designHeight
        val side = min(width, height)
        val pad = side * 0.055f
        val playRadius = side * 0.075f
        val skipSize = side * 0.050f
        val nextX = width - pad - skipSize * 0.9f
        val playX = nextX - playRadius - skipSize * 0.7f
        val previousX = playX - playRadius - skipSize * 0.7f
        val centerY = pad + playRadius
        return ControlLayout(
            previousCenterX = previousX,
            playCenterX = playX,
            nextCenterX = nextX,
            centerY = centerY,
            playRadius = playRadius,
            skipSize = skipSize,
            volumeUpCenterX = playX,
            volumeDownCenterX = previousX,
            volumeCenterY = centerY + playRadius * 2.4f,
        )
    }

    fun clearRect(kind: MediaWidgetKind): RectF {
        val box = MediaTouchGeometry.clearBox(kind)
        return RectF(box.left, box.top, box.right, box.bottom)
    }

    fun draw(canvas: Canvas, kind: MediaWidgetKind, snapshot: MediaSnapshot) {
        val width = kind.designWidth
        val height = kind.designHeight
        val side = min(width, height)
        val pad = side * 0.055f
        val radius = side * 0.10f

        canvas.drawRoundRect(
            RectF(0f, 0f, width, height),
            radius,
            radius,
            MediaDraw.fill(WHITE),
        )

        val artSize = side * 0.38f
        MediaDraw.cover(
            canvas,
            snapshot.albumArt,
            RectF(pad, pad, pad + artSize, pad + artSize),
            side * 0.04f,
            ART_FALLBACK,
        )

        drawTransport(canvas, kind, snapshot)
        drawTextBlock(canvas, kind, snapshot, pad, artSize, side)
        if (kind == MediaWidgetKind.NOTIFICATION) {
            drawClear(canvas, kind)
        }
    }

    private fun drawClear(canvas: Canvas, kind: MediaWidgetKind) {
        val rect = clearRect(kind)
        val radius = rect.height() / 2f
        canvas.drawRoundRect(rect, radius, radius, MediaDraw.fill(BLACK))
        val label = "CLEAR"
        val pitch = DotGlyphFont.fitPitch(label, rect.width() * 0.72f, rect.height() * 0.55f)
        if (pitch > 0f) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = WHITE }
            DotGlyphFont.drawCenteredInk(
                canvas,
                label,
                rect.centerX(),
                rect.centerY(),
                pitch,
                paint,
            )
        }
    }

    private fun drawTransport(canvas: Canvas, kind: MediaWidgetKind, snapshot: MediaSnapshot) {
        val controls = controlLayout(kind)
        val playColor = if (snapshot.canPlayPause) BLACK else GRAY
        canvas.drawCircle(
            controls.playCenterX,
            controls.centerY,
            controls.playRadius,
            MediaDraw.fill(playColor),
        )
        MediaDraw.drawPlayPause(
            canvas,
            controls.playCenterX,
            controls.centerY,
            controls.playRadius * 1.15f,
            snapshot.isPlaying,
            WHITE,
        )
        if (snapshot.canSkipPrevious) {
            MediaDraw.drawSkip(
                canvas,
                controls.previousCenterX,
                controls.centerY,
                controls.skipSize,
                previous = true,
                color = BLACK,
            )
        }
        if (snapshot.canSkipNext) {
            MediaDraw.drawSkip(
                canvas,
                controls.nextCenterX,
                controls.centerY,
                controls.skipSize,
                previous = false,
                color = BLACK,
            )
        }
        if (kind == MediaWidgetKind.NOTHING_PLAYER_3) {
            val volumeColor = if (snapshot.canAdjustVolume) BLACK else GRAY
            val volumePaint = MediaDraw.text(volumeColor, controls.skipSize * 1.4f, bold = true)
            MediaDraw.drawText(
                canvas,
                volumePaint,
                "+",
                controls.volumeUpCenterX,
                controls.volumeCenterY + controls.skipSize * 0.5f,
                controls.skipSize * 2f,
                Paint.Align.CENTER,
            )
            MediaDraw.drawText(
                canvas,
                volumePaint,
                "-",
                controls.volumeDownCenterX,
                controls.volumeCenterY + controls.skipSize * 0.5f,
                controls.skipSize * 2f,
                Paint.Align.CENTER,
            )
        }
    }

    private fun drawTextBlock(
        canvas: Canvas,
        kind: MediaWidgetKind,
        snapshot: MediaSnapshot,
        pad: Float,
        artSize: Float,
        side: Float,
    ) {
        val width = kind.designWidth
        val height = kind.designHeight
        val wide = width > height * 1.25f
        val titleValue = displayText(
            snapshot.notification?.title ?: snapshot.title,
            "Nothing playing",
        )
        val artistValue = displayText(
            snapshot.notification?.text ?: snapshot.artist,
            if (snapshot.accessGranted) "Unknown artist" else "Media access off",
        )

        val textLeft: Float
        val textRight: Float
        val titleCenterY: Float
        val artistCenterY: Float
        val titleHeight: Float
        val artistHeight: Float

        if (wide) {
            textLeft = pad + artSize + pad
            textRight = width - pad - side * 0.30f
            titleHeight = side * 0.16f
            artistHeight = side * 0.13f
            titleCenterY = height * 0.62f
            artistCenterY = height * 0.34f
        } else {
            textLeft = pad
            textRight = width - pad
            titleHeight = side * 0.14f
            artistHeight = side * 0.12f
            titleCenterY = height - pad - titleHeight / 2f
            artistCenterY = titleCenterY - titleHeight / 2f - side * 0.03f - artistHeight / 2f
        }

        val maxWidth = (textRight - textLeft).coerceAtLeast(1f)
        drawDotLine(
            canvas,
            artistValue,
            textLeft,
            artistCenterY,
            maxWidth,
            artistHeight,
            artistColor(),
        )
        drawDotLine(
            canvas,
            titleValue,
            textLeft,
            titleCenterY,
            maxWidth,
            titleHeight,
            titleColor(),
        )
    }

    private fun drawDotLine(
        canvas: Canvas,
        value: String,
        left: Float,
        centerY: Float,
        maxWidth: Float,
        maxHeight: Float,
        color: Int,
    ) {
        if (value.isEmpty()) return
        val fitted = DotGlyphFont.fitPitch(value, maxWidth, maxHeight)
        if (fitted <= 0f) return
        val maxColumns = (maxWidth / fitted).toInt()
        val text = truncateDot(value, maxColumns)
        if (text.isEmpty()) return
        val pitch = DotGlyphFont.fitPitch(text, maxWidth, maxHeight)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        DotGlyphFont.drawLeft(canvas, text, left, centerY, pitch, paint)
    }
}
