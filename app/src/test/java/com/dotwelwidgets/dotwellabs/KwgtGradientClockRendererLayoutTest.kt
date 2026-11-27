package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import kotlin.math.min

class KwgtGradientClockRendererLayoutTest {
    private val sizes = listOf(
        200 to 200,
        300 to 400,
        400 to 200,
        415 to 542,
        542 to 415,
    )

    @Test
    fun gradientLayoutFitsHourChipAndHostBounds() {
        sizes.forEach { (width, height) ->
            assertFits(width, height, "12", "59")
        }
    }

    @Test
    fun longTwoDigitReadoutFitsWithoutCollision() {
        sizes.forEach { (width, height) ->
            assertFits(width, height, "88", "88")
        }
    }

    @Test
    fun sourceRatiosSetCornerChipAndNominalHour() {
        val width = 415f
        val height = 542f
        val layout = KwgtGradientClockRenderer.layout(width, height, "12", "59")
        val side = min(width, height)

        assertEquals(width / 2f, layout.centerX, 0.01f)
        assertEquals(height / 2f, layout.centerY, 0.01f)
        assertEquals(side / 5f, layout.cornerRadius, 0.01f)
        assertEquals(side / 6f, layout.minuteChipDiameter, 0.01f)
        assertEquals(
            width - layout.minuteChipRadius,
            layout.minuteChipCenterX,
            0.01f,
        )
        assertEquals(
            width,
            layout.minuteChipCenterX + layout.minuteChipRadius,
            0.01f,
        )
        assertEquals(
            side / 3f / 7f,
            layout.hourPitch,
            0.01f,
        )
    }

    @Test
    fun threeCirclesAreRightEdgeAnchoredWithSourceDiameters() {
        val width = 415f
        val height = 542f
        val layout = KwgtGradientClockRenderer.layout(width, height, "12", "59")
        val side = min(width, height)
        val expectedDiameters = listOf(side / 1.1f, side / 1.4f, side / 2f)

        assertEquals(3, layout.circles.size)
        layout.circles.forEachIndexed { index, circle ->
            val diameter = expectedDiameters[index]
            assertEquals(diameter / 2f, circle.radius, 0.01f)
            assertEquals(width, circle.centerX + circle.radius, 0.01f)
            assertEquals(height / 2f, circle.centerY, 0.01f)
        }
    }

    @Test
    fun twelveHourSourceReadoutHasNoAmPmSuffix() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 7)
        }
        assertEquals("09", KwgtGradientClockRenderer.hourText(calendar))
        assertEquals("07", KwgtGradientClockRenderer.minuteText(calendar))
    }

    private fun assertFits(width: Int, height: Int, hour: String, minute: String) {
        val layout = KwgtGradientClockRenderer.layout(
            width.toFloat(),
            height.toFloat(),
            hour,
            minute,
        )
        val label = "${width}x$height $hour:$minute"
        val side = min(width, height).toFloat()
        val chipLeft = layout.minuteChipCenterX - layout.minuteChipRadius

        assertTrue("$label positive corner", layout.cornerRadius > 0f)
        assertTrue("$label positive chip", layout.minuteChipDiameter > 0f)
        assertTrue("$label positive hour pitch", layout.hourPitch > 0f)
        assertTrue("$label positive minute pitch", layout.minutePitch > 0f)
        assertTrue("$label hour left", layout.hourLeft >= 0f)
        assertTrue("$label hour right", layout.hourRight <= width.toFloat() + 0.01f)
        assertTrue(
            "$label hour clears minute chip",
            layout.hourRight <= chipLeft + 0.01f,
        )
        assertTrue(
            "$label minute inside chip horizontally",
            layout.minuteLeft >= chipLeft + 0.01f &&
                layout.minuteRight <= layout.minuteChipCenterX + layout.minuteChipRadius,
        )
        assertTrue(
            "$label minute inside chip vertically",
            layout.minuteInkHeight <= layout.minuteChipDiameter,
        )
        assertTrue(
            "$label hour fitted to host height",
            layout.centerY - layout.hourInkHeight / 2f >= 0f &&
                layout.centerY + layout.hourInkHeight / 2f <= height.toFloat() + 0.01f,
        )
        assertTrue(
            "$label hour nominal size bound",
            layout.hourPitch <= side / 3f / 7f + 0.01f,
        )
        assertTrue(
            "$label minute nominal size bound",
            layout.minutePitch <= side / 12f / 7f + 0.01f,
        )
    }
}
