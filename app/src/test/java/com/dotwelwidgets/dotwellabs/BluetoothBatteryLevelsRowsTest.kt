package com.dotwelwidgets.dotwellabs

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks the repaired reference row layout for the Bluetooth battery card.
 * A single source must be a short pill, not one card-sized red squircle, and
 * multiple sources must be equal-height full-width rows with even spacing.
 */
class BluetoothBatteryLevelsRowsTest {
    @Test
    fun cardUsesTheReferenceLightSurfaceForEveryStyle() {
        WidgetStyle.values().forEach { style ->
            assertEquals(0xFFFEF7FF.toInt(), BluetoothBatteryLevelsRenderer.cardColor(style))
        }
    }

    @Test
    fun cardRadiusMatchesTheOtherDesignMatchSurfaces() {
        assertEquals(
            400f * 0.16f,
            BluetoothBatteryLevelsRenderer.cardRadius(400, 400),
            0.001f,
        )
        assertEquals(
            240f * 0.16f,
            BluetoothBatteryLevelsRenderer.cardRadius(360, 240),
            0.001f,
        )
    }

    @Test
    fun aSingleRowIsAShortFullWidthPillNotTheWholeCard() {
        val frames = BluetoothBatteryLevelsRenderer.rowFrames(400, 400, 1)
        assertEquals(1, frames.size)
        val row = frames.single()
        assertTrue("single row height ${row.height} must stay pill-sized", row.height <= 400f * 0.20f)
        assertTrue("single row must span most of the card", row.width >= 400f * 0.80f)
        assertTrue("single row must stay inside the card", row.top > 0f && row.bottom < 400f)
    }

    @Test
    fun fourRowsHaveEqualHeightEvenGapsAndFullWidth() {
        val frames = BluetoothBatteryLevelsRenderer.rowFrames(400, 400, 4)
        assertEquals(4, frames.size)
        val heights = frames.map { it.height }
        heights.forEach { height ->
            assertEquals(heights.first(), height, 0.001f)
        }
        val gaps = (1 until frames.size).map { index ->
            frames[index].top - frames[index - 1].bottom
        }
        gaps.forEach { gap ->
            assertEquals(gaps.first(), gap, 0.001f)
        }
        frames.forEach { row ->
            assertEquals(frames.first().left, row.left, 0.001f)
            assertEquals(frames.first().right, row.right, 0.001f)
        }
    }

    @Test
    fun rowStackIsVerticallyCentredInsideTheCard() {
        val frames = BluetoothBatteryLevelsRenderer.rowFrames(400, 400, 2)
        val topSpace = frames.first().top
        val bottomSpace = 400f - frames.last().bottom
        assertEquals(topSpace, bottomSpace, 0.001f)
    }

    @Test
    fun rowCountIsCappedAtFour() {
        assertEquals(4, BluetoothBatteryLevelsRenderer.rowFrames(400, 400, 7).size)
        assertEquals(0, BluetoothBatteryLevelsRenderer.rowFrames(400, 400, 0).size)
    }

    @Test
    fun rowsStayInsideTheCardForWideAndTallHosts() {
        listOf(400 to 240, 240 to 400).forEach { (width, height) ->
            val frames = BluetoothBatteryLevelsRenderer.rowFrames(width, height, 4)
            assertEquals(4, frames.size)
            frames.forEach { row ->
                assertTrue(row.left > 0f && row.right < width)
                assertTrue(row.top > 0f && row.bottom < height)
            }
            val heights = frames.map { it.height }
            heights.forEach { heightPx ->
                assertEquals(heights.first(), heightPx, 0.001f)
            }
        }
    }

    @Test
    fun rowGeometryIsIndependentOfTheNumberOfSources() {
        val one = BluetoothBatteryLevelsRenderer.rowFrames(400, 400, 1).single()
        val four = BluetoothBatteryLevelsRenderer.rowFrames(400, 400, 4)
        assertEquals(four.first().height, one.height, 0.001f)
        assertEquals(four.first().left, one.left, 0.001f)
        assertEquals(four.first().right, one.right, 0.001f)
        assertTrue(abs(four.first().top - one.top) > 0.001f)
    }
}
