package com.dotwelwidgets.dotwellabs

import com.dotwelwidgets.dotwellabs.media.ReferenceMediaCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the reference media-controls card: a pure white #FFFFFF surface, a red
 * #D71921 dot-matrix artist line and a black dot-matrix title line.
 */
class MediaControlsChromeTest {
    @Test
    fun cardIsReferenceWhiteForEveryStyle() {
        WidgetStyle.values().forEach { style ->
            assertEquals(0xFFFFFFFF.toInt(), ReferenceMediaCard.cardColor(style))
        }
    }

    @Test
    fun artistIsReferenceRedAndTitleIsBlack() {
        assertEquals(0xFFD71921.toInt(), ReferenceMediaCard.artistColor())
        assertEquals(0xFF000000.toInt(), ReferenceMediaCard.titleColor())
    }

    @Test
    fun displayTextIsUppercaseDotMatrixWithHonestFallback() {
        assertEquals("EMINEM", ReferenceMediaCard.displayText("  Eminem ", "ARTIST"))
        assertEquals("HOUDINI", ReferenceMediaCard.displayText("Houdini", "TITLE"))
        assertEquals("NOTHING PLAYING", ReferenceMediaCard.displayText(null, "Nothing playing"))
        assertEquals("NOTHING PLAYING", ReferenceMediaCard.displayText("   ", "Nothing playing"))
    }

    @Test
    fun dotTextTruncatesToTheAvailableColumns() {
        assertEquals("ABC", ReferenceMediaCard.truncateDot("ABCDEF", 17))
        assertEquals("AB", ReferenceMediaCard.truncateDot("AB", 11))
        assertEquals("", ReferenceMediaCard.truncateDot("ABC", 0))
        assertTrue(
            ReferenceMediaCard.truncateDot("A LONG TITLE", 4).length <= 4,
        )
    }
}
