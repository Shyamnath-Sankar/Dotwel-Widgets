package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min

class KwgtDigital4RendererLayoutTest {
    private val sizes = listOf(200 to 200, 300 to 400, 400 to 200)

    @Test
    fun splitLayoutFitsBothHalvesDividerAndHeight() {
        sizes.forEach { (width, height) ->
            assertFits(width, height, "12", "59")
        }
    }

    @Test
    fun longTwoDigitReadoutFitsWithoutFlow() {
        sizes.forEach { (width, height) ->
            assertFits(width, height, "88", "88")
        }
    }

    @Test
    fun sourceRatiosSetTheNominalGeometry() {
        val width = 400f
        val height = 400f
        val layout = KwgtDigital4Renderer.layout(width, height, "12", "59")
        val side = min(width, height)
        val dividerHalf = side * 2f / 277f / 2f

        assertEquals(width / 2f, layout.centerX, 0.01f)
        assertEquals(height / 2f, layout.centerY, 0.01f)
        assertEquals(side * 2f / 277f, layout.lineWidth, 0.01f)
        assertEquals(side * 8f / 277f, layout.hourGap, 0.01f)
        assertEquals(side * 10f / 277f, layout.minuteGap, 0.01f)
        assertEquals(
            layout.centerX - dividerHalf - layout.hourGap,
            layout.hourRight,
            0.01f,
        )
        assertEquals(
            layout.centerX + dividerHalf + layout.minuteGap,
            layout.minuteLeft,
            0.01f,
        )
    }

    private fun assertFits(width: Int, height: Int, hour: String, minute: String) {
        val layout = KwgtDigital4Renderer.layout(
            width.toFloat(),
            height.toFloat(),
            hour,
            minute,
        )
        val label = "${width}x$height $hour:$minute"
        val side = min(width, height).toFloat()
        val dividerHalf = layout.lineWidth / 2f

        assertTrue("$label positive line", layout.lineWidth > 0f)
        assertTrue("$label positive pitch", layout.pitch > 0f)
        assertTrue("$label hour inside left", layout.hourLeft >= 0f)
        assertTrue(
            "$label hour clears divider",
            layout.hourRight <= layout.centerX - dividerHalf + 0.01f,
        )
        assertTrue(
            "$label minute clears divider",
            layout.minuteLeft >= layout.centerX + dividerHalf - 0.01f,
        )
        assertTrue(
            "$label minute inside right",
            layout.minuteRight <= width.toFloat() + 0.01f,
        )
        assertTrue(
            "$label hour fits its half",
            layout.hourInkWidth <= layout.centerX - layout.hourGap,
        )
        assertTrue(
            "$label minute fits its half",
            layout.minuteInkWidth <= width.toFloat() - layout.centerX - layout.minuteGap,
        )
        assertTrue("$label hour ink height", layout.hourInkHeight > 0f)
        assertTrue("$label minute ink height", layout.minuteInkHeight > 0f)
        assertTrue(
            "$label hour top",
            layout.centerY - layout.hourInkHeight / 2f >= 0f,
        )
        assertTrue(
            "$label hour bottom",
            layout.centerY + layout.hourInkHeight / 2f <= height.toFloat() + 0.01f,
        )
        assertTrue("$label source size bound", layout.pitch <= side * 28f / 277f / 7f + 0.01f)
    }
}
