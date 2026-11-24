package com.dotwelwidgets.dotwellabs

import java.io.File
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure geometry, threshold, icon and provenance assertions for the
 * iOS_Battery_Widget port. These do not inflate Android views.
 */
class IosBatteryRendererTest {
    private val sizes = listOf(
        320f to 320f,
        320f to 480f,
        480f to 320f,
        420f to 420f,
        110f to 110f,
    )

    @Test
    fun ringColourFollowsTheActualSourceThresholds() {
        assertEquals(IosBatteryRenderer.LOW_RED, IosBatteryRenderer.ringColor(0))
        assertEquals(IosBatteryRenderer.LOW_RED, IosBatteryRenderer.ringColor(14))
        assertEquals(IosBatteryRenderer.MED_ORANGE, IosBatteryRenderer.ringColor(15))
        assertEquals(IosBatteryRenderer.MED_ORANGE, IosBatteryRenderer.ringColor(24))
        assertEquals(IosBatteryRenderer.HEALTHY_GREEN, IosBatteryRenderer.ringColor(25))
        assertEquals(IosBatteryRenderer.HEALTHY_GREEN, IosBatteryRenderer.ringColor(81))
        assertEquals(IosBatteryRenderer.HEALTHY_GREEN, IosBatteryRenderer.ringColor(100))
        assertEquals(
            "unknown level stays neutral and never invents a low reading",
            IosBatteryRenderer.TRACK,
            IosBatteryRenderer.ringColor(-1),
        )
        assertEquals(IosBatteryRenderer.TRACK, IosBatteryRenderer.ringColor(140))
    }

    @Test
    fun ringSweepTracksTheRealLevel() {
        assertEquals(0f, IosBatteryRenderer.sweepFraction(0), EPS)
        assertEquals(0.14f, IosBatteryRenderer.sweepFraction(14), EPS)
        assertEquals(0.81f, IosBatteryRenderer.sweepFraction(81), EPS)
        assertEquals(1f, IosBatteryRenderer.sweepFraction(100), EPS)
        assertEquals(0f, IosBatteryRenderer.sweepFraction(-1), EPS)
        assertEquals(0f, IosBatteryRenderer.sweepFraction(140), EPS)
    }

    @Test
    fun percentageReadoutUsesTheRealLevelAndHonestUnknown() {
        assertEquals("0%", IosBatteryRenderer.percentText(0))
        assertEquals("14%", IosBatteryRenderer.percentText(14))
        assertEquals("15%", IosBatteryRenderer.percentText(15))
        assertEquals("25%", IosBatteryRenderer.percentText(25))
        assertEquals("81%", IosBatteryRenderer.percentText(81))
        assertEquals("100%", IosBatteryRenderer.percentText(100))
        assertEquals("--", IosBatteryRenderer.percentText(-1))
        assertEquals("--", IosBatteryRenderer.percentText(140))
    }

    @Test
    fun iconFormulaMatchesChargingAndLowBatterySource() {
        assertEquals(
            R.drawable.dw_ios_battery_alert,
            IosBatteryRenderer.iconRes(charging = false, level = 14),
        )
        assertEquals(
            R.drawable.dw_ios_battery_std,
            IosBatteryRenderer.iconRes(charging = false, level = 15),
        )
        assertEquals(
            R.drawable.dw_ios_battery_std,
            IosBatteryRenderer.iconRes(charging = false, level = 100),
        )
        assertEquals(
            R.drawable.dw_ios_battery_std,
            IosBatteryRenderer.iconRes(charging = false, level = -1),
        )
        assertEquals(
            R.drawable.dw_ios_battery_charging,
            IosBatteryRenderer.iconRes(charging = true, level = 1),
        )
        assertEquals(
            R.drawable.dw_ios_battery_charging,
            IosBatteryRenderer.iconRes(charging = true, level = 100),
        )
    }

    @Test
    fun layoutRetainsSourceGeometryAndStaysContainedOnTargetSizes() {
        sizes.forEach { (width, height) ->
            val layout = IosBatteryRenderer.layout(width, height)
            val side = min(width, height)
            val scale = side / IosBatteryRenderer.SOURCE_SHORT_SIDE
            val label = "${width}x$height"

            assertEquals("$label side", side, layout.side, EPS)
            assertEquals("$label scale", scale, layout.scale, EPS)
            assertEquals(
                "$label background corner",
                IosBatteryRenderer.BACKGROUND_CORNER * scale,
                layout.backgroundCorner,
                EPS,
            )
            assertEquals(
                "$label ring padding",
                IosBatteryRenderer.RING_PADDING * scale,
                layout.ring.left,
                EPS,
            )
            assertEquals(
                "$label ring top padding",
                IosBatteryRenderer.RING_PADDING * scale,
                layout.ring.top,
                EPS,
            )
            assertEquals(
                "$label ring size",
                IosBatteryRenderer.RING_SIZE * scale,
                layout.ring.width,
                EPS,
            )
            assertEquals(
                "$label ring height",
                IosBatteryRenderer.RING_SIZE * scale,
                layout.ring.height,
                EPS,
            )
            assertEquals(
                "$label ring stroke",
                IosBatteryRenderer.RING_STROKE * scale,
                layout.ringStroke,
                EPS,
            )
            assertEquals(
                "$label text size",
                IosBatteryRenderer.TEXT_SIZE * scale,
                layout.textSize,
                EPS,
            )
            assertEquals(
                "$label text left",
                IosBatteryRenderer.TEXT_PADDING_LEFT * scale,
                layout.textLeft,
                EPS,
            )
            assertEquals(
                "$label text bottom",
                IosBatteryRenderer.TEXT_PADDING_BOTTOM * scale,
                layout.textBottom,
                EPS,
            )

            assertEquals("$label background fills width", 0f, layout.background.left, EPS)
            assertEquals("$label background fills height", 0f, layout.background.top, EPS)
            assertEquals("$label background right", width, layout.background.right, EPS)
            assertEquals("$label background bottom", height, layout.background.bottom, EPS)
            assertTrue("$label background inside", layout.background.inside(width, height))
            assertTrue("$label ring inside", layout.ring.inside(width, height))
            assertTrue("$label icon inside host", layout.icon.inside(width, height))
            assertTrue(
                "$label icon inside ring",
                layout.icon.left >= layout.ring.left - EPS &&
                    layout.icon.top >= layout.ring.top - EPS &&
                    layout.icon.right <= layout.ring.right + EPS &&
                    layout.icon.bottom <= layout.ring.bottom + EPS,
            )
            assertTrue("$label ring stays above text", layout.ring.bottom < height - layout.textBottom)
            assertTrue("$label text has positive width", layout.textMaxWidth > 0f)
            assertTrue(
                "$label uniform scale has no separate x/y stretch",
                layout.ring.width == layout.ring.height,
            )
        }
    }

    @Test
    fun stylesKeepTheSourceGreyWithClearInk() {
        assertEquals(
            IosBatteryRenderer.BLACK_SURFACE,
            IosBatteryRenderer.surfaceColor(WidgetStyle.BLACK),
        )
        assertEquals(
            IosBatteryRenderer.TRANSLUCENT_SURFACE,
            IosBatteryRenderer.surfaceColor(WidgetStyle.TRANSLUCENT),
        )
        assertEquals(0, IosBatteryRenderer.surfaceColor(WidgetStyle.TRANSPARENT))
        assertEquals(0xFF, (IosBatteryRenderer.INK ushr 24) and 0xFF)
    }

    @Test
    fun sanitizedSourceKeepsTogglesAndFormulasAndDropsPrivateData() {
        val text = listOf(
            File("src/main/assets/kwgt/ios_battery.sanitized.json"),
            File("app/src/main/assets/kwgt/ios_battery.sanitized.json"),
        ).first { it.isFile }.readText()
        assertTrue(text.contains("config_visible"))
        assertTrue(text.contains("\"config_visible\": 0"))
        assertTrue(text.contains("#B75B5B5B"))
        assertTrue(text.contains("shape_corners"))
        assertTrue(text.contains("69"))
        assertTrue(text.contains("b_low"))
        assertTrue(text.contains("b_med"))
        assertTrue(text.contains("#ED1B24"))
        assertTrue(text.contains("#FA9F1B"))
        assertTrue(text.contains("#3ABD54"))
        assertTrue(text.contains("battery_alert"))
        assertTrue(text.contains("Battery_std"))
        assertTrue(text.contains("Battery_charging_full"))
        assertTrue(text.contains("fx_gradient_bitmap"))
        assertTrue(text.contains("toggle 0"))
        assertTrue(text.contains("omitted from rendering"))
        assertFalse(text.contains("@gmail.com"))
        assertFalse(text.contains("LAUNCH_ACTIVITY"))
        assertFalse(text.contains("intent:"))
        assertFalse(text.contains(".ttf"))
        assertFalse(text.contains("text_family"))
        assertFalse(text.contains("internal_events"))
    }

    private companion object {
        const val EPS = 0.01f
    }
}
