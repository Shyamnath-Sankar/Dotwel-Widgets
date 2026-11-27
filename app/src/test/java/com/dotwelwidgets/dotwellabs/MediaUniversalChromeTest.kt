package com.dotwelwidgets.dotwellabs

import com.dotwelwidgets.dotwellabs.media.MediaWidgetKind
import com.dotwelwidgets.dotwellabs.media.ReferenceMediaCard
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the Universal media card to the media_controls reference: a #FEF7FF
 * card, a large rounded-square album-art block at the top-left, a black music
 * note above a black circular play button at the top-right, and a red artist
 * line above a black title at the bottom-left.
 */
class MediaUniversalChromeTest {
    private val kind = MediaWidgetKind.UNIVERSAL

    @Test
    fun universalCardUsesTheReferenceFef7ffSurfaceOnly() {
        WidgetStyle.values().forEach { style ->
            assertEquals(
                0xFFFEF7FF.toInt(),
                ReferenceMediaCard.cardColor(kind, style),
            )
        }
        assertEquals(
            0xFFFFFFFF.toInt(),
            ReferenceMediaCard.cardColor(MediaWidgetKind.MINI, WidgetStyle.BLACK),
        )
    }

    @Test
    fun artIsAboutFortyPercentOfTheCardWidthAndAnchoredTopLeft() {
        val layout = ReferenceMediaCard.universalLayout(kind)
        val side = min(kind.designWidth, kind.designHeight)
        assertTrue(layout.art.width() >= kind.designWidth * 0.38f)
        assertTrue(layout.art.width() <= kind.designWidth * 0.42f)
        assertTrue(layout.art.height() >= kind.designWidth * 0.38f)
        assertTrue(layout.art.left <= side * 0.04f)
        assertTrue(layout.art.top <= side * 0.04f)
        assertTrue(layout.art.right < kind.designWidth * 0.60f)
        assertTrue(layout.art.bottom < layout.artistCenterY)
        assertTrue(layout.artCorner > 0f)
    }

    @Test
    fun noteSitsAboveTheBlackPlayButtonAtTheTopRight() {
        val layout = ReferenceMediaCard.universalLayout(kind)
        assertTrue(layout.noteCenterY < layout.playCenterY)
        assertTrue(layout.noteCenterX > kind.designWidth * 0.75f)
        assertTrue(layout.playCenterX > kind.designWidth * 0.75f)
        assertTrue(layout.playCenterX + layout.playRadius <= kind.designWidth)
        assertTrue(layout.playCenterY + layout.playRadius <= kind.designHeight * 0.55f)
        assertEquals(0xFF000000.toInt(), ReferenceMediaCard.noteColor())
        assertEquals(0xFF000000.toInt(), ReferenceMediaCard.playButtonColor())
    }

    @Test
    fun artistAndTitleAreBottomLeftWithArtistAbove() {
        val layout = ReferenceMediaCard.universalLayout(kind)
        assertEquals(layout.artistLeft, layout.titleLeft, EPS)
        assertEquals(layout.artistRight, layout.titleRight, EPS)
        assertTrue(layout.artistLeft <= kind.designWidth * 0.10f)
        assertTrue(layout.artistCenterY < layout.titleCenterY)
        assertTrue(layout.titleBottom <= kind.designHeight)
        assertTrue(layout.titleBottom >= kind.designHeight * 0.82f)
        assertEquals(0xFFD71921.toInt(), ReferenceMediaCard.artistColor())
        assertEquals(0xFF000000.toInt(), ReferenceMediaCard.titleColor())
    }

    private companion object {
        const val EPS = 0.5f
    }
}
