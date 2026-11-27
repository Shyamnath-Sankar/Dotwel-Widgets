package com.dotwelwidgets.dotwellabs

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class KwgtCleanInfoRendererLayoutTest {
    private val sizes = listOf(
        553f to 248f,
        468f to 188f,
        320f to 160f,
        542f to 415f,
        250f to 110f,
    )

    private lateinit var previousLocale: Locale

    @Before
    fun useStableLocale() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(previousLocale)
    }

    @Test
    fun textFitsInsideSourceAndHostBounds() {
        sizes.forEach { (width, height) ->
            val layout = KwgtCleanInfoRenderer.layout(width, height)
            val label = "${width}x$height"
            assertTrue("$label time size", layout.timeTextSize > 0f)
            assertTrue("$label weekday size", layout.weekdayTextSize > 0f)
            assertTrue("$label date size", layout.dateTextSize > 0f)
            assertTrue("$label battery size", layout.batteryTextSize > 0f)

            assertTrue(
                "$label time width",
                layout.timeTextSize * KwgtCleanInfoRenderer.TIME_WIDTH_EM <=
                    width - layout.rightPad + 0.01f,
            )
            assertTrue(
                "$label time height",
                layout.timeTextSize <= height * 0.83f,
            )
            assertTrue(
                "$label weekday width",
                layout.weekdayTextSize * KwgtCleanInfoRenderer.WEEKDAY_WIDTH_EM <=
                    width - layout.leftPad + 0.01f,
            )
            assertTrue(
                "$label date width",
                layout.dateTextSize * KwgtCleanInfoRenderer.DATE_WIDTH_EM <=
                    width - layout.leftPad + 0.01f,
            )
        }
    }

    @Test
    fun sourceNumbersAreRetainedOnTheUntouchedCanvas() {
        val layout = KwgtCleanInfoRenderer.layout(
            KwgtCleanInfoRenderer.SOURCE_WIDTH,
            KwgtCleanInfoRenderer.SOURCE_HEIGHT,
        )
        assertEquals(1f, layout.scale, 0.0001f)
        assertEquals(KwgtCleanInfoRenderer.CORNER, layout.cornerRadius, 0.01f)
        assertEquals(KwgtCleanInfoRenderer.PAD, layout.leftPad, 0.01f)
        assertEquals(KwgtCleanInfoRenderer.PAD, layout.bottomPad, 0.01f)
        assertEquals(KwgtCleanInfoRenderer.TIME_SIZE, layout.timeTextSize, 0.01f)
        assertEquals(KwgtCleanInfoRenderer.WEEKDAY_SIZE, layout.weekdayTextSize, 0.01f)
        assertEquals(KwgtCleanInfoRenderer.DATE_SIZE, layout.dateTextSize, 0.01f)
    }

    @Test
    fun batteryBodyAndNubStayInsideTheCard() {
        sizes.forEach { (width, height) ->
            val layout = KwgtCleanInfoRenderer.layout(width, height)
            val label = "${width}x$height"
            val body = layout.batteryBody
            val nub = layout.batteryNub

            assertTrue("$label body inside", body.inside(width, height))
            assertTrue("$label nub inside", nub.inside(width, height))
            assertTrue("$label body width", body.width > 0f)
            assertTrue("$label body height", body.height > 0f)
            assertTrue("$label nub overlaps body", nub.left < body.right)
            assertTrue("$label nub extends right", nub.right > body.right)
            assertEquals("$label nub centre", body.centerY, nub.centerY, 0.01f)
            assertTrue("$label body starts at left pad", body.left >= layout.leftPad - 0.01f)
            assertTrue("$label body starts at top pad", body.top >= layout.topPad - 0.01f)
            assertTrue(
                "$label source body ratio",
                body.width / body.height >=
                    KwgtCleanInfoRenderer.BATTERY_BODY_WIDTH /
                    KwgtCleanInfoRenderer.BATTERY_BODY_HEIGHT - 0.01f,
            )
        }
    }

    @Test
    fun hostMappingUsesContentPixelsNotTheRasterBudget() {
        val density = 2f
        val host = KwgtCleanInfoRenderer.layoutForHost(300f, 160f, density)
        val inset = WidgetSizing.CONTENT_INSET_DP * density
        val expectedWidth = 300f * density - inset * 2f
        val expectedHeight = 160f * density - inset * 2f
        assertEquals(expectedWidth, host.width, 0.01f)
        assertEquals(expectedHeight, host.height, 0.01f)
        assertTrue(host.timeTextSize > 0f)
        assertTrue(host.batteryBody.inside(host.width, host.height))
    }

    @Test
    fun dateAndWeekdayMatchTheSourceExpressions() {
        val date = Calendar.getInstance().apply {
            set(2024, Calendar.NOVEMBER, 5, 9, 7, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals("Tuesday", KwgtCleanInfoRenderer.weekdayText(date))
        assertEquals("NOVEMBER 5", KwgtCleanInfoRenderer.dateText(date))
        assertEquals("9:07", KwgtCleanInfoRenderer.timeText(date))
    }

    @Test
    fun batteryTextIsTrueLevelAndClamped() {
        assertEquals("0%", KwgtCleanInfoRenderer.batteryText(0))
        assertEquals("57%", KwgtCleanInfoRenderer.batteryText(57))
        assertEquals("100%", KwgtCleanInfoRenderer.batteryText(100))
        assertEquals("100%", KwgtCleanInfoRenderer.batteryText(143))
        assertEquals("0%", KwgtCleanInfoRenderer.batteryText(-4))
    }

    @Test
    fun repairedForegroundConditionalUsesDarkWhiteAndLightBlack() {
        val dark = KwgtCleanInfoRenderer.palette(dark = true)
        val light = KwgtCleanInfoRenderer.palette(dark = false)
        assertEquals(KwgtCleanInfoRenderer.DARK_BACKGROUND, dark.background)
        assertEquals(0xFFFFFFFF.toInt(), dark.foreground)
        assertEquals(KwgtCleanInfoRenderer.LIGHT_BACKGROUND, light.background)
        assertEquals(0xFF000000.toInt(), light.foreground)
        assertEquals(48, (dark.timeInk ushr 24) and 0xFF)
        assertEquals(KwgtCleanInfoRenderer.DARK_BACKGROUND, dark.batteryInk)
        assertEquals(KwgtCleanInfoRenderer.LIGHT_BACKGROUND, light.batteryInk)
    }
}
