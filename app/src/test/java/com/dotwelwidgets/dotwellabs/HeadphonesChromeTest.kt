package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
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
