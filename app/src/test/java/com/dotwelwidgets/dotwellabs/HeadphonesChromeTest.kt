package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the reference headphone card: a #FEF7FF full-bleed card, CONNECTED in
 * black dot-matrix, a level-coloured percentage, and a gray plain-sans device
 * name. The old NA-headphones + progress-bar layout is gone.
 */
class HeadphonesChromeTest {
    @Test
    fun cardUsesTheReferenceLightSurface() {
        assertEquals(
            0xFFFEF7FF.toInt(),
            BluetoothHeadphonesRenderer.cardColor(WidgetStyle.BLACK),
        )
        assertEquals(
            0xFFFEF7FF.toInt(),
            BluetoothHeadphonesRenderer.cardColor(WidgetStyle.TRANSLUCENT),
        )
    }

    @Test
    fun earbudsAndStatusUseTheReferenceBlackInk() {
        assertEquals(0xFF1D1B20.toInt(), BluetoothHeadphonesRenderer.GLYPH_INK)
    }

    @Test
    fun earbudsGrowAndSitOnALightGrayHalo() {
        val wide = BluetoothHeadphonesRenderer.layout(110f, 40f, square = false)
        assertTrue(
            "wide earbuds are larger than the old 22px glyph",
            wide.icon.width >= 40f * 0.62f,
        )
        val square = BluetoothHeadphonesRenderer.layout(110f, 110f, square = true)
        assertTrue(
            "square earbuds are larger than the old 0.32 side glyph",
            square.icon.width >= 110f * 0.35f,
        )
        assertEquals(0xFFE9E7EC.toInt(), BluetoothHeadphonesRenderer.HALO)
    }

    @Test
    fun connectedStateReadsAsConnected() {
        val connected = BluetoothHeadphonesSnapshot(
            HeadphonesState.CONNECTED,
            "Nothing Ear (1)",
            100,
        )
        assertEquals("CONNECTED", BluetoothHeadphonesRenderer.statusWord(connected))
        assertEquals("100%", BluetoothHeadphonesRenderer.percentText(100))
        assertEquals("NA%", BluetoothHeadphonesRenderer.percentText(null))
    }

    @Test
    fun batteryLevelIsGreenWhenHighAndRedWhenLow() {
        assertEquals(
            0xFF6A9C89.toInt(),
            BluetoothHeadphonesRenderer.batteryColor(100),
        )
        assertEquals(
            0xFF6A9C89.toInt(),
            BluetoothHeadphonesRenderer.batteryColor(30),
        )
        assertEquals(
            0xFFD71921.toInt(),
            BluetoothHeadphonesRenderer.batteryColor(29),
        )
        assertEquals(
            0xFFD71921.toInt(),
            BluetoothHeadphonesRenderer.batteryColor(0),
        )
    }
}
