package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class KwgtMiniCalendarDatesTest {
    @Test
    fun offsetsCrossDecemberYearBoundary() {
        val base = calendar("UTC", 2024, Calendar.DECEMBER, 31)
        val expected = listOf(
            intArrayOf(2024, Calendar.DECEMBER, 29),
            intArrayOf(2024, Calendar.DECEMBER, 30),
            intArrayOf(2024, Calendar.DECEMBER, 31),
            intArrayOf(2025, Calendar.JANUARY, 1),
            intArrayOf(2025, Calendar.JANUARY, 2),
        )
        KwgtMiniCalendarDates.OFFSETS.forEachIndexed { index, offset ->
            val date = KwgtMiniCalendarDates.anchored(base, offset)
            assertEquals("year at offset $offset", expected[index][0], date.get(Calendar.YEAR))
            assertEquals("month at offset $offset", expected[index][1], date.get(Calendar.MONTH))
            assertEquals("day at offset $offset", expected[index][2], date.get(Calendar.DAY_OF_MONTH))
        }
    }

    @Test
    fun offsetsCrossLeapFebruary() {
        val base = calendar("UTC", 2024, Calendar.FEBRUARY, 28)
        val expected = listOf(
            intArrayOf(2024, Calendar.FEBRUARY, 26),
            intArrayOf(2024, Calendar.FEBRUARY, 27),
            intArrayOf(2024, Calendar.FEBRUARY, 28),
            intArrayOf(2024, Calendar.FEBRUARY, 29),
            intArrayOf(2024, Calendar.MARCH, 1),
        )
        KwgtMiniCalendarDates.OFFSETS.forEachIndexed { index, offset ->
            val date = KwgtMiniCalendarDates.anchored(base, offset)
            assertEquals("year at offset $offset", expected[index][0], date.get(Calendar.YEAR))
            assertEquals("month at offset $offset", expected[index][1], date.get(Calendar.MONTH))
            assertEquals("day at offset $offset", expected[index][2], date.get(Calendar.DAY_OF_MONTH))
        }
    }

    @Test
    fun leapDayRollsIntoMarch() {
        val base = calendar("UTC", 2024, Calendar.FEBRUARY, 29)
        val next = KwgtMiniCalendarDates.anchored(base, 1)
        assertEquals(2024, next.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, next.get(Calendar.MONTH))
        assertEquals(1, next.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun calendarAddPreservesWallClockAcrossDst() {
        val base = calendar("America/New_York", 2024, Calendar.MARCH, 9, hour = 12)
        val next = KwgtMiniCalendarDates.anchored(base, 1)
        assertEquals(Calendar.MARCH, next.get(Calendar.MONTH))
        assertEquals(10, next.get(Calendar.DAY_OF_MONTH))
        assertEquals(12, next.get(Calendar.HOUR_OF_DAY))
        assertNotEquals(
            "A 24-hour millisecond add would cross the DST day length",
            86_400_000L,
            next.timeInMillis - base.timeInMillis,
        )
    }

    @Test
    fun fiveDayLabelsKeepSourceOffsetsAndToday() {
        val base = calendar("UTC", 2024, Calendar.DECEMBER, 31)
        val days = KwgtMiniCalendarDates.days(base)
        assertEquals(listOf(-2, -1, 0, 1, 2), days.map { it.offset })
        assertEquals(listOf(false, false, true, false, false), days.map { it.isToday })
        assertTrue(days.all { it.weekday.isNotEmpty() })
        assertTrue(days.all { it.day.length == 2 })
        assertEquals("31", days[2].day)
    }

    private fun calendar(
        zone: String,
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 12,
    ): Calendar = GregorianCalendar(TimeZone.getTimeZone(zone)).apply {
        set(year, month, day, hour, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
