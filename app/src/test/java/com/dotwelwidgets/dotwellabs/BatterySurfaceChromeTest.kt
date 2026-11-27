package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the reference battery surfaces: pure white cards, black dot-matrix
 * digits, a neutral track, and the reference red #D71921 fill at every known
 * level (circle, pill and bar alike). The iOS device card keeps a black phone
 * glyph on a light pink disc with a small gray status word.
 */
class BatterySurfaceChromeTest {
    @Test
    fun cardIsReferenceWhiteForEveryStyle() {
        WidgetStyle.values().forEach { style ->
            assertEquals(0xFFFFFFFF.toInt(), ReferenceBatterySurface.cardColor(style))
        }
    }

    @Test
    fun paletteMatchesTheReferenceLiterals() {
        assertEquals(0xFFD71921.toInt(), ReferenceBatterySurface.RED)
        assertEquals(0xFF6A9C89.toInt(), ReferenceBatterySurface.GREEN)
        assertEquals(0xFF777379.toInt(), ReferenceBatterySurface.GRAY)
        assertEquals(0xFF000000.toInt(), ReferenceBatterySurface.INK)
        assertEquals(0xFFFFFFFF.toInt(), ReferenceBatterySurface.WHITE)
        assertEquals(0xFFEEEEEE.toInt(), ReferenceBatterySurface.TRACK)
        assertEquals(0xFFE1E1E1.toInt(), ReferenceBatterySurface.TRACK_SOFT)
        assertEquals(0xFFB4B4B8.toInt(), ReferenceBatterySurface.EMPTY_DOT)
    }

    @Test
    fun everyKnownFillUsesTheReferenceRed() {
        listOf(0, 1, 24, 25, 26, 75, 99, 100).forEach { level ->
            assertEquals(
                "level $level fill",
                ReferenceBatterySurface.RED,
                ReferenceBatterySurface.accent(level),
            )
        }
        assertEquals(ReferenceBatterySurface.GRAY, ReferenceBatterySurface.accent(-1))
        assertEquals(ReferenceBatterySurface.GRAY, ReferenceBatterySurface.accent(140))
    }

    @Test
    fun iosDeviceIconUsesALightPinkDiscAndGrayStatusWord() {
        assertEquals(0xFFF8D7DA.toInt(), ReferenceBatterySurface.DEVICE_PINK)
        assertEquals(
            ReferenceBatterySurface.DEVICE_PINK,
            ReferenceBatterySurface.deviceCircleColor(),
        )
        assertEquals(
            ReferenceBatterySurface.GRAY,
            ReferenceBatterySurface.deviceStatusColor(),
        )
    }

    @Test
    fun digitsStayBlackAtEveryLevel() {
        listOf(-1, 0, 20, 75, 100, 140).forEach { level ->
            assertEquals(
                ReferenceBatterySurface.INK,
                ReferenceBatterySurface.digitColor(level),
            )
        }
    }

    @Test
    fun emptyDotMatchesReferenceGray() {
        assertEquals(0xFFB4B4B8.toInt(), ReferenceBatterySurface.EMPTY_DOT)
    }

    @Test
    fun percentAndFillAreClampedAndHonest() {
        assertEquals("75%", ReferenceBatterySurface.percentText(75))
        assertEquals("0%", ReferenceBatterySurface.percentText(0))
        assertEquals("100%", ReferenceBatterySurface.percentText(100))
        assertEquals("--", ReferenceBatterySurface.percentText(-1))
        assertEquals("--", ReferenceBatterySurface.percentText(140))
        assertEquals(0.75f, ReferenceBatterySurface.fillFraction(75), 0.0001f)
        assertEquals(1f, ReferenceBatterySurface.fillFraction(140), 0.0001f)
        assertEquals(0f, ReferenceBatterySurface.fillFraction(-1), 0.0001f)
    }

    @Test
    fun verticalDotsFollowTheLevelAndClamp() {
        assertEquals(75, ReferenceBatterySurface.verticalFilledDots(75, 100))
        assertEquals(0, ReferenceBatterySurface.verticalFilledDots(-5, 100))
        assertEquals(100, ReferenceBatterySurface.verticalFilledDots(140, 100))
        assertEquals(15, ReferenceBatterySurface.verticalFilledDots(75, 20))
    }

    @Test
    fun verticalDotsFillEveryColumnEvenly() {
        assertEquals(15, ReferenceBatterySurface.verticalFilledPerColumn(75, 20))
        assertEquals(5, ReferenceBatterySurface.verticalFilledPerColumn(24, 20))
        assertEquals(0, ReferenceBatterySurface.verticalFilledPerColumn(0, 20))
        assertEquals(20, ReferenceBatterySurface.verticalFilledPerColumn(100, 20))
        assertEquals(0, ReferenceBatterySurface.verticalFilledPerColumn(-5, 20))
        assertEquals(20, ReferenceBatterySurface.verticalFilledPerColumn(140, 20))
    }

    @Test
    fun chargeLabelIsTruthful() {
        assertEquals("CHARGING", ReferenceBatterySurface.chargeLabel(true))
        assertEquals("DISCHARGING", ReferenceBatterySurface.chargeLabel(false))
    }
}
