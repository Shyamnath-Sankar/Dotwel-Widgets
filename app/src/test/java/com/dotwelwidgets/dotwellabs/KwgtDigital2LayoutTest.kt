package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KwgtDigital2LayoutTest {
    @Test
    fun sourceRatiosDriveTheLayout() {
        val layout = KwgtDigital2Renderer.layout(415, 542)
        assertEquals(415f, layout.side, 0.01f)
        assertEquals(415f / 5f, layout.cornerRadius, 0.01f)
        assertEquals(415f / 8.5f, layout.rightInset, 0.01f)
        assertEquals(415f / 3.5f, layout.centerTextSize, 0.01f)
        assertEquals(415f / 5f, layout.smallTextSize, 0.01f)
    }

    @Test
    fun landscapeUsesTheShortSideAndKeepsTextInsideTheSurface() {
        val layout = KwgtDigital2Renderer.layout(640, 300)
        assertEquals(300f, layout.side, 0.01f)
        assertTrue(layout.textRight <= layout.width)
        assertTrue(layout.textRight > layout.backgroundLeft)
    }

    @Test
    fun stylesKeepContrastForAllThreeSurfaces() {
        val black = KwgtDigital2Renderer.palette(WidgetStyle.BLACK)
        val translucent = KwgtDigital2Renderer.palette(WidgetStyle.TRANSLUCENT)
        val transparent = KwgtDigital2Renderer.palette(WidgetStyle.TRANSPARENT)
        assertEquals(0xFF1B1B1D.toInt(), black.background)
        assertEquals(0xFFFFFFFF.toInt(), black.foreground)
        assertEquals(0xA6FFFFFF.toInt(), translucent.background)
        assertEquals(0xFF1B1B1D.toInt(), translucent.foreground)
        assertEquals(0, transparent.background)
        assertEquals(0xFFFFFFFF.toInt(), transparent.foreground)
    }
}
