package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min

class KwgtDigital3RendererLayoutTest {
    private val sizes = listOf(
        200 to 200,
        300 to 400,
        400 to 300,
        415 to 542,
        542 to 415,
    )

    @Test
    fun sourceDotGeometryFitsEveryHost() {
        sizes.forEach { (width, height) ->
            val layout = KwgtDigital3Renderer.layout(
                width.toFloat(),
                height.toFloat(),
            )
            val side = min(width, height).toFloat()
            val expectedDiameter = side * 48f / 277f
            val expectedStep = side * 60f / 277f
            val label = "${width}x$height"

            assertEquals("$label dot count", 16, layout.dots.size)
            assertEquals("$label corners", side / 5f, layout.cornerRadius, 0.01f)
            assertEquals("$label diameter", expectedDiameter, layout.dotDiameter, 0.01f)
            assertEquals("$label step", expectedStep, layout.dotStep, 0.01f)
            layout.dots.forEach { dot ->
                assertEquals(expectedDiameter / 2f, dot.radius, 0.01f)
                assertTrue(
                    "$label ${dot.column},${dot.row} left",
                    dot.centerX - dot.radius >= 0f,
                )
                assertTrue(
                    "$label ${dot.column},${dot.row} top",
                    dot.centerY - dot.radius >= 0f,
                )
                assertTrue(
                    "$label ${dot.column},${dot.row} right",
                    dot.centerX + dot.radius <= width.toFloat() + 0.01f,
                )
                assertTrue(
                    "$label ${dot.column},${dot.row} bottom",
                    dot.centerY + dot.radius <= height.toFloat() + 0.01f,
                )
            }
        }
    }

    @Test
    fun dotsKeepSourceRowAndColumnSpacing() {
        val layout = KwgtDigital3Renderer.layout(415f, 542f)
        layout.dots.filter { it.row == 0 }.zipWithNext().forEach { (first, second) ->
            assertEquals(layout.dotStep, second.centerX - first.centerX, 0.01f)
        }
        layout.dots.filter { it.column == 0 }.zipWithNext().forEach { (first, second) ->
            assertEquals(layout.dotStep, second.centerY - first.centerY, 0.01f)
        }
    }

    @Test
    fun textModulesStayInsideTheirAuthorDots() {
        val layout = KwgtDigital3Renderer.layout(415f, 542f)
        assertRegionInsideDot(layout, layout.hourRegion, 0, 0)
        assertRegionInsideDot(layout, layout.minuteRegion, 1, 0)
        assertRegionInsideDot(layout, layout.ampmRegion, 1, 1)
    }

    @Test
    fun minuteThresholdsSelectAccentOrAccent2() {
        val style = WidgetStyle.BLACK
        val accent = KwgtDigital3Renderer.palette(style).accent
        val accent2 = KwgtDigital3Renderer.palette(style).accent2
        val primary = KwgtDigital3Renderer.palette(style).primary

        assertEquals(
            primary,
            KwgtDigital3Renderer.dotColor(KwgtDigital3Renderer.DotFill.PRIMARY, 0, style),
        )
        assertEquals(
            accent2,
            KwgtDigital3Renderer.dotColor(KwgtDigital3Renderer.DotFill.MINUTE_9, 9, style),
        )
        assertEquals(
            accent,
            KwgtDigital3Renderer.dotColor(KwgtDigital3Renderer.DotFill.MINUTE_9, 10, style),
        )
        assertEquals(
            accent2,
            KwgtDigital3Renderer.dotColor(KwgtDigital3Renderer.DotFill.MINUTE_29, 29, style),
        )
        assertEquals(
            accent,
            KwgtDigital3Renderer.dotColor(KwgtDigital3Renderer.DotFill.MINUTE_29, 30, style),
        )
        assertEquals(
            accent2,
            KwgtDigital3Renderer.dotColor(KwgtDigital3Renderer.DotFill.MINUTE_39, 39, style),
        )
        assertEquals(
            accent,
            KwgtDigital3Renderer.dotColor(KwgtDigital3Renderer.DotFill.MINUTE_39, 40, style),
        )
        assertEquals(
            accent2,
            KwgtDigital3Renderer.dotColor(KwgtDigital3Renderer.DotFill.MINUTE_49, 49, style),
        )
        assertEquals(
            accent,
            KwgtDigital3Renderer.dotColor(KwgtDigital3Renderer.DotFill.MINUTE_49, 50, style),
        )
    }

    @Test
    fun accentAlphasFollowAuthorPercentages() {
        val palette = KwgtDigital3Renderer.palette(WidgetStyle.BLACK)
        assertEquals(89, (palette.accent ushr 24) and 0xFF)
        assertEquals(38, (palette.accent2 ushr 24) and 0xFF)
        assertEquals(KwgtDigital3Renderer.RED, palette.primary)
    }

    private fun assertRegionInsideDot(
        layout: KwgtDigital3Renderer.Digital3Layout,
        region: KwgtDigital3Renderer.TextRegion,
        column: Int,
        row: Int,
    ) {
        val dot = layout.dot(column, row)
            ?: throw AssertionError("missing author dot $column,$row")
        assertTrue(region.left >= dot.centerX - dot.radius)
        assertTrue(region.right <= dot.centerX + dot.radius)
        assertTrue(region.top >= dot.centerY - dot.radius)
        assertTrue(region.bottom <= dot.centerY + dot.radius)
        assertTrue(region.width > 0f)
        assertTrue(region.height > 0f)
    }
}
