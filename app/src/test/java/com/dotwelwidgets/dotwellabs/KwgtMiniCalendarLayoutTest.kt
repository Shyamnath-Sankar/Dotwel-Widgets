package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import kotlin.math.abs

/**
 * Pure layout assertions for the Nothing_Mini_Calender strip. These lock the
 * measured-pixel defects fixed in the renderer: a weekday must stay inside its
 * own column and clear the adjacent today highlight, and a square host must
 * letterbox the natural wide strip instead of stretching the highlight.
 */
class KwgtMiniCalendarLayoutTest {
    private val aspects = listOf(
        32 to 32,
        220 to 220,
        392 to 392,
        480 to 320,
        750 to 120,
        1080 to 300,
    )

    @Test
    fun weekdayAndDateGlyphsStayInsideTheirColumn() {
        aspects.forEach { (width, height) ->
            val layout = layout(width, height)
            val label = "${width}x$height"
            layout.columns.forEach { column ->
                val weekday = KwgtMiniCalendarRenderer.glyphWidth(
                    column.day.weekday,
                    column.weekdayPitch,
                )
                val date = KwgtMiniCalendarRenderer.glyphWidth(
                    column.day.day,
                    column.datePitch,
                )
                assertTrue("$label weekday positive", weekday > 0f)
                assertTrue("$label date positive", date > 0f)
                assertTrue(
                    "$label weekday left ${column.centerX - weekday / 2f} < ${column.cellLeft}",
                    column.centerX - weekday / 2f >= column.cellLeft - EPS,
                )
                assertTrue(
                    "$label weekday right ${column.centerX + weekday / 2f} > ${column.cellRight}",
                    column.centerX + weekday / 2f <= column.cellRight + EPS,
                )
                assertTrue(
                    "$label date left ${column.centerX - date / 2f} < ${column.cellLeft}",
                    column.centerX - date / 2f >= column.cellLeft - EPS,
                )
                assertTrue(
                    "$label date right ${column.centerX + date / 2f} > ${column.cellRight}",
                    column.centerX + date / 2f <= column.cellRight + EPS,
                )
            }
        }
    }

    @Test
    fun adjacentWeekdaysAndDatesDoNotOverlap() {
        aspects.forEach { (width, height) ->
            val layout = layout(width, height)
            val label = "${width}x$height"
            layout.columns.zipWithNext().forEach { (left, right) ->
                val leftWeekday = KwgtMiniCalendarRenderer.glyphWidth(
                    left.day.weekday,
                    left.weekdayPitch,
                )
                val rightWeekday = KwgtMiniCalendarRenderer.glyphWidth(
                    right.day.weekday,
                    right.weekdayPitch,
                )
                assertTrue(
                    "$label weekday ${left.day.weekday}/${right.day.weekday} overlap",
                    left.centerX + leftWeekday / 2f <=
                        right.centerX - rightWeekday / 2f + EPS,
                )
                val leftDate = KwgtMiniCalendarRenderer.glyphWidth(
                    left.day.day,
                    left.datePitch,
                )
                val rightDate = KwgtMiniCalendarRenderer.glyphWidth(
                    right.day.day,
                    right.datePitch,
                )
                assertTrue(
                    "$label date ${left.day.day}/${right.day.day} overlap",
                    left.centerX + leftDate / 2f <=
                        right.centerX - rightDate / 2f + EPS,
                )
            }
        }
    }

    @Test
    fun weekdayGlyphsClearTheTodayHighlight() {
        aspects.forEach { (width, height) ->
            val layout = layout(width, height)
            val label = "${width}x$height"
            val highlightWidth = layout.highlightRight - layout.highlightLeft
            val highlightCenter = (layout.highlightLeft + layout.highlightRight) / 2f
            val today = layout.columns.first { it.day.isToday }
            val todayWidth =
                KwgtMiniCalendarRenderer.glyphWidth(today.day.weekday, today.weekdayPitch)
            assertTrue(
                "$label today weekday wider than highlight",
                todayWidth <= highlightWidth + EPS,
            )
            layout.columns.filterNot { it.day.isToday }.forEach { column ->
                val glyph =
                    KwgtMiniCalendarRenderer.glyphWidth(column.day.weekday, column.weekdayPitch)
                val distance = abs(column.centerX - highlightCenter)
                assertTrue(
                    "$label ${column.day.weekday} overlaps highlight: $distance < " +
                        "${glyph / 2f + highlightWidth / 2f}",
                    distance >= glyph / 2f + highlightWidth / 2f - EPS,
                )
            }
        }
    }

    @Test
    fun contentAndHighlightStayInsideTheStripBand() {
        aspects.forEach { (width, height) ->
            val layout = layout(width, height)
            val label = "${width}x$height"
            assertTrue("$label content top", layout.contentTop >= layout.stripTop - EPS)
            assertTrue("$label content bottom", layout.contentBottom <= layout.stripBottom + EPS)
            assertTrue("$label highlight top", layout.highlightTop >= layout.stripTop - EPS)
            assertTrue(
                "$label highlight bottom",
                layout.highlightBottom <= layout.stripBottom + EPS,
            )
            assertTrue(
                "$label highlight shorter than full host",
                layout.highlightBottom - layout.highlightTop <= height + EPS,
            )
        }
    }

    @Test
    fun squareHostLetterboxesTheWideStrip() {
        val square = layout(392, 392)
        assertTrue(
            "square strip should be shorter than the host",
            square.stripHeight < 392f - 1f,
        )
        assertTrue("square strip is vertically centred", square.stripTop > 1f)
        val highlightHeight = square.highlightBottom - square.highlightTop
        assertTrue(
            "square highlight must not be a full-height stripe",
            highlightHeight <= square.stripHeight + EPS,
        )
        assertTrue(
            "square highlight should leave clear letterboxing",
            highlightHeight < 392f * 0.6f,
        )
    }

    @Test
    fun wideFourByOneHostKeepsFullHeight() {
        val wide = layout(750, 120)
        assertEquals("wide host uses its full height", 120f, wide.stripHeight, EPS)
        assertEquals("wide host has no vertical letterbox", 0f, wide.stripTop, EPS)
        assertTrue(
            "wide host aspect must exceed the natural strip aspect",
            750f / 120f > 916f / 226f,
        )
    }

    private fun layout(width: Int, height: Int): KwgtMiniCalendarRenderer.StripLayout {
        val base = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            set(2024, Calendar.DECEMBER, 31, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return KwgtMiniCalendarRenderer.layoutStrip(
            width,
            height,
            KwgtMiniCalendarDates.days(base),
        )
    }

    private companion object {
        const val EPS = 0.05f
    }
}
