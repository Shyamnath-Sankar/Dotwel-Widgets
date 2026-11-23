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

    @Test
    fun portraitAndSquareHostsLetterboxTheSquareSource() {
        val portrait = KwgtDigital3Renderer.layout(415f, 542f)
        assertEquals(415f, portrait.side, 0.01f)
        assertEquals(0f, portrait.contentLeft, 0.01f)
        assertEquals((542f - 415f) / 2f, portrait.contentTop, 0.01f)
        portrait.dots.forEach { dot ->
            assertTrue(dot.centerX - dot.radius >= portrait.contentLeft - 0.01f)
            assertTrue(
                dot.centerX + dot.radius <=
                    portrait.contentLeft + portrait.side + 0.01f,
            )
            assertTrue(dot.centerY - dot.radius >= portrait.contentTop - 0.01f)
            assertTrue(
                dot.centerY + dot.radius <=
                    portrait.contentTop + portrait.side + 0.01f,
            )
        }

        val landscape = KwgtDigital3Renderer.layout(542f, 415f)
        assertEquals(415f, landscape.side, 0.01f)
        assertEquals((542f - 415f) / 2f, landscape.contentLeft, 0.01f)
        assertEquals(0f, landscape.contentTop, 0.01f)

        val square = KwgtDigital3Renderer.layout(400f, 400f)
        assertEquals(0f, square.contentLeft, 0.01f)
        assertEquals(0f, square.contentTop, 0.01f)
    }

    @Test
    fun liveClockCellsShareTheFittedTransformAndNeverOverlap() {
        listOf(300f to 400f, 400f to 300f, 400f to 400f).forEach { (width, height) ->
            val layout = KwgtDigital3Renderer.layout(width, height)
            val cells = listOf(
                KwgtDigital3Renderer.liveCell(layout.hourRegion, layout),
                KwgtDigital3Renderer.liveCell(layout.minuteRegion, layout),
                KwgtDigital3Renderer.liveCell(layout.ampmRegion, layout),
            )
            cells.forEach { cell ->
                assertTrue(cell.width > 0f)
                assertTrue(cell.height > 0f)
                assertTrue(cell.textSizePx > 0f)
                assertTrue(cell.left >= -0.01f)
                assertTrue(cell.top >= -0.01f)
                assertTrue(cell.left + cell.width <= width + 0.01f)
                assertTrue(cell.top + cell.height <= height + 0.01f)
            }
            cells.indices.forEach { first ->
                (first + 1 until cells.size).forEach { second ->
                    val a = cells[first]
                    val b = cells[second]
                    val overlapX = a.left < b.left + b.width - 0.01f &&
                        b.left < a.left + a.width - 0.01f
                    val overlapY = a.top < b.top + b.height - 0.01f &&
                        b.top < a.top + a.height - 0.01f
                    assertTrue(
                        "cells $first/$second overlap at ${width}x$height",
                        !(overlapX && overlapY),
                    )
                }
            }
        }
    }

    @Test
    fun styledGlyphInkFillsAboutSixtyPercentOfTheCell() {
        val layout = KwgtDigital3Renderer.layout(415f, 542f)
        listOf("08", "59", "AM", "PM").forEach { text ->
            val ink = KwgtDigital3Renderer.glyphInk(text, layout.dotDiameter)
                ?: throw AssertionError("no ink for $text")
            assertTrue("$text width", ink.width <= layout.dotDiameter + 0.01f)
            assertTrue("$text height", ink.height <= layout.dotDiameter + 0.01f)
            assertTrue(
                "$text ink occupancy ${ink.height / layout.dotDiameter}",
                ink.height / layout.dotDiameter >= 0.55f,
            )
            assertTrue(
                "$text ink width ${ink.width / layout.dotDiameter}",
                ink.width / layout.dotDiameter >= 0.70f,
            )
        }
    }

    @Test
    fun bridgeVisibilityFollowsAuthorMinuteThresholds() {
        val layout = KwgtDigital3Renderer.layout(415f, 542f)
        fun has(
            minute: Int,
            fromColumn: Int,
            fromRow: Int,
            toColumn: Int,
            toRow: Int,
        ): KwgtDigital3Renderer.Bridge? {
            val from = layout.dot(fromColumn, fromRow)
                ?: throw AssertionError("missing dot $fromColumn,$fromRow")
            val to = layout.dot(toColumn, toRow)
                ?: throw AssertionError("missing dot $toColumn,$toRow")
            return KwgtDigital3Renderer.bridges(layout, minute).firstOrNull {
                it.startX == from.centerX &&
                    it.startY == from.centerY &&
                    it.endX == to.centerX &&
                    it.endY == to.centerY
            }
        }

        val topLeft = has(0, 0, 0, 1, 0)
            ?: throw AssertionError("missing top-left red bridge")
        assertEquals(
            KwgtDigital3Renderer.DotFill.PRIMARY,
            topLeft.fill,
        )
        assertTrue(has(0, 0, 1, 1, 1) != null)
        assertTrue(has(0, 0, 2, 1, 2) == null)
        assertTrue(has(9, 0, 2, 1, 2) == null)
        assertTrue(has(10, 0, 2, 1, 2) != null)
        assertTrue(has(29, 1, 2, 2, 2) == null)
        assertTrue(has(30, 1, 2, 2, 2) != null)
        assertTrue(has(39, 2, 1, 3, 1) == null)
        assertTrue(has(40, 2, 1, 3, 1) != null)
        assertTrue(has(49, 2, 2, 3, 2) == null)
        assertTrue(has(50, 2, 2, 3, 2) != null)

        assertTrue(has(0, 2, 0, 2, 1) != null)
        assertTrue(has(0, 3, 2, 3, 3) != null)
        assertTrue(has(0, 0, 0, 0, 1) == null)
        assertTrue(has(0, 1, 0, 1, 1) == null)

        assertEquals(8, KwgtDigital3Renderer.bridges(layout, 0).size)
        assertEquals(9, KwgtDigital3Renderer.bridges(layout, 10).size)
        assertEquals(10, KwgtDigital3Renderer.bridges(layout, 30).size)
        assertEquals(11, KwgtDigital3Renderer.bridges(layout, 40).size)
        assertEquals(12, KwgtDigital3Renderer.bridges(layout, 50).size)
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
