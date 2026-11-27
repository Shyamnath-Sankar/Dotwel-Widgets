package com.dotwelwidgets.dotwellabs

import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fixture-state and fit checks for the 110x40 wide and 110x110 square headphone
 * cards. The gallery preview uses [BluetoothHeadphonesRenderer.previewSnapshot]
 * so the connected state is verifiable without Bluetooth hardware, while the
 * placed widget still uses the real no-access state with the same geometry.
 */
class HeadphonesFixtureLayoutTest {
    private val fixture = BluetoothHeadphonesRenderer.previewSnapshot()

    @Test
    fun galleryFixtureShowsTheConnectedReferenceState() {
        assertEquals(HeadphonesState.CONNECTED, fixture.state)
        assertEquals("Nothing Ear (1)", fixture.name)
        assertEquals(100, fixture.percent)
        assertEquals("CONNECTED", BluetoothHeadphonesRenderer.statusWord(fixture))
        assertEquals("100%", BluetoothHeadphonesRenderer.percentText(fixture.percent))
        assertEquals(
            0xFF6A9C89.toInt(),
            BluetoothHeadphonesRenderer.batteryColor(fixture.percent),
        )
    }

    @Test
    fun wideCardFitsEveryFixtureAndNoAccessLineWithoutClipping() {
        val layout = BluetoothHeadphonesRenderer.layout(110f, 40f, square = false)
        assertInside(layout, 110f, 40f)
        assertDotFits("CONNECTED", layout.status)
        assertDotFits("100%", layout.percent)
        assertNameFits("Nothing Ear (1)", layout.name)
        assertNoAccessFits(layout)
    }

    @Test
    fun squareCardUsesTheSameStackAndFitsWithoutClipping() {
        val layout = BluetoothHeadphonesRenderer.layout(110f, 110f, square = true)
        assertInside(layout, 110f, 110f)
        assertDotFits("CONNECTED", layout.status)
        assertDotFits("100%", layout.percent)
        assertNameFits("Nothing Ear (1)", layout.name)
        assertNoAccessFits(layout)
        assertTrue(layout.icon.centerY < layout.status.centerY)
        assertTrue(layout.status.centerY < layout.percent.centerY)
        assertTrue(layout.percent.centerY < layout.name.centerY)
    }

    @Test
    fun wideCardKeepsTheSameVerticalStack() {
        val layout = BluetoothHeadphonesRenderer.layout(110f, 40f, square = false)
        assertTrue(layout.icon.centerY > layout.status.top)
        assertTrue(layout.icon.centerY < layout.name.bottom)
        assertTrue(layout.status.centerY < layout.percent.centerY)
        assertTrue(layout.percent.centerY < layout.name.centerY)
        assertTrue(layout.icon.centerX < layout.status.centerX)
    }

    private fun assertNoAccessFits(layout: BluetoothHeadphonesRenderer.HeadphonesLayout) {
        val noAccess = BluetoothHeadphonesSnapshot(HeadphonesState.PERMISSION_NEEDED)
        assertDotFits(
            BluetoothHeadphonesRenderer.statusWord(noAccess),
            layout.status,
        )
        assertDotFits(
            BluetoothHeadphonesRenderer.percentText(noAccess.percent),
            layout.percent,
        )
        assertNameFits("HEADPHONES", layout.name)
    }

    private fun assertDotFits(text: String, box: BluetoothHeadphonesRenderer.TextBox) {
        val pitch = DotGlyphFont.fitPitch(text, box.width, box.height)
        assertTrue("$text has a positive pitch", pitch > 0f)
        assertTrue(
            "$text width fits ${box.width}",
            DotGlyphFont.columnsFor(text) * pitch <= box.width + EPS,
        )
        assertTrue(
            "$text height fits ${box.height}",
            DotGlyphFont.ROWS * pitch <= box.height + EPS,
        )
    }

    private fun assertNameFits(text: String, box: BluetoothHeadphonesRenderer.TextBox) {
        val size = BluetoothHeadphonesRenderer.fittedNameSize(text, box.width, box.height)
        assertTrue("$text name size is positive", size > 0f)
        assertTrue(
            "$text name width fits ${box.width}",
            text.length * size * BluetoothHeadphonesRenderer.NAME_WIDTH_ESTIMATE <=
                box.width + EPS,
        )
        assertTrue(
            "$text name height fits ${box.height}",
            size * BluetoothHeadphonesRenderer.NAME_HEIGHT_FACTOR <= box.height + EPS,
        )
    }

    private fun assertInside(
        layout: BluetoothHeadphonesRenderer.HeadphonesLayout,
        width: Float,
        height: Float,
    ) {
        val box = layout.icon
        assertTrue(box.left >= -EPS && box.top >= -EPS)
        assertTrue(box.right <= width + EPS && box.bottom <= height + EPS)
        listOf(layout.status, layout.percent, layout.name).forEach { text ->
            assertTrue(text.left >= -EPS && text.top >= -EPS)
            assertTrue(text.right <= width + EPS && text.bottom <= height + EPS)
            assertTrue(text.width > 0f && text.height > 0f)
        }
        assertEquals(min(width, height) * 0.16f, layout.cardRadius, EPS)
    }

    private companion object {
        const val EPS = 0.01f
    }
}
