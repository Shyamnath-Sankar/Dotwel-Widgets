package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Locale

class Digital2MinuteFormatTest {
    @Test fun shiftedMinuteFormatsDigitsAcrossHourAndDay() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
            val parse = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
            val now = parse.parse("2026-11-24 00:00:30")!!.time
            assertEquals("59", KwgtDigital2Renderer.minuteAtOffset(now, -60000))
            assertEquals("01", KwgtDigital2Renderer.minuteAtOffset(now, 60000))
        } finally { TimeZone.setDefault(original) }
    }
}
