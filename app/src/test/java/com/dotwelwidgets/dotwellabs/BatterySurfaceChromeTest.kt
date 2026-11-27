package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the reference battery surfaces: pure white cards, black dot-matrix
 * digits, a neutral track, and a red #D71921 accent only while the level is
 * low. The normal accent is the palette green #6A9C89.
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
    }

    @Test
    fun lowIsRedNormalIsGreenUnknownIsNeutral() {
        assertEquals(ReferenceBatterySurface.RED, ReferenceBatterySurface.accent(0))
        assertEquals(ReferenceBatterySurface.RED, ReferenceBatterySurface.accent(24))
        assertEquals(ReferenceBatterySurface.RED, ReferenceBatterySurface.accent(25))
        assertEquals(ReferenceBatterySurface.GREEN, ReferenceBatterySurface.accent(26))
        assertEquals(ReferenceBatterySurface.GREEN, ReferenceBatterySurface.accent(100))
        assertEquals(ReferenceBatterySurface.GRAY, ReferenceBatterySurface.accent(-1))
    }

    @Test
    fun digitsAreBlackExceptWhenLow() {
        assertEquals(ReferenceBatterySurface.INK, ReferenceBatterySurface.digitColor(75))
        assertEquals(ReferenceBatterySurface.INK, ReferenceBatterySurface.digitColor(-1))
        assertEquals(ReferenceBatterySurface.RED, ReferenceBatterySurface.digitColor(20))
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
    fun chargeLabelIsTruthful() {
        assertEquals("CHARGING", ReferenceBatterySurface.chargeLabel(true))
        assertEquals("DISCHARGING", ReferenceBatterySurface.chargeLabel(false))
    }
}
