package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the reference control-tile geometry: one filled circle that fills 93%
 * of the cell, a single centred glyph and no status text, with a red/white
 * on/off pair and a white battery disc carrying black dot-matrix digits.
 */
class StatusTileChromeTest {
    @Test
    fun circleFillsNinetyThreePercentOfTheCell() {
        val tile = StatusTileRenderer.layout(200, 200)
        assertEquals(100f, tile.centerX, 0.5f)
        assertEquals(100f, tile.centerY, 0.5f)
        assertEquals(93f, tile.radius, 0.6f)
        assertEquals(tile.centerY, tile.glyphCenterY, 0.01f)
    }

    @Test
    fun activeTileIsReferenceRedWithWhiteInk() {
        val on = StatusSnapshot(StatusKind.WIFI, ConnectivityState.CONNECTED)
        assertEquals(0xFFD71921.toInt(), StatusTileRenderer.circleColor(on))
        assertEquals(0xFFFFFFFF.toInt(), StatusTileRenderer.glyphColor(on))
        assertEquals("", StatusTileRenderer.tileText(on))
    }

    @Test
    fun inactiveTileIsWhiteWithBlackInk() {
        val off = StatusSnapshot(StatusKind.BLUETOOTH, ConnectivityState.OFF)
        assertEquals(0xFFFFFFFF.toInt(), StatusTileRenderer.circleColor(off))
        assertEquals(0xFF000000.toInt(), StatusTileRenderer.glyphColor(off))
        assertEquals("", StatusTileRenderer.tileText(off))
    }

    @Test
    fun batteryTileIsWhiteWithBlackPercent() {
        val battery = StatusSnapshot(
            StatusKind.BATTERY,
            ConnectivityState.ON,
            percent = 64,
        )
        assertEquals(0xFFFFFFFF.toInt(), StatusTileRenderer.circleColor(battery))
        assertEquals(0xFF000000.toInt(), StatusTileRenderer.glyphColor(battery))
        assertEquals("64%", StatusTileRenderer.tileText(battery))
    }

    @Test
    fun unavailableBatteryStillUsesWhiteDiscAndBlackInk() {
        val battery = StatusSnapshot(StatusKind.BATTERY, ConnectivityState.UNAVAILABLE)
        assertEquals(0xFFFFFFFF.toInt(), StatusTileRenderer.circleColor(battery))
        assertEquals(0xFF000000.toInt(), StatusTileRenderer.glyphColor(battery))
        assertEquals("NA%", StatusTileRenderer.tileText(battery))
    }
}
