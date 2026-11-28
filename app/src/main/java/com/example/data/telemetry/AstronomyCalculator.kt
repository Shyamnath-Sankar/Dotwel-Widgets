package com.example.data.telemetry

import com.example.model.AppSettings
import com.example.model.LunarInfo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt

object AstronomyCalculator {

    private const val SYNODIC_MONTH_DAYS = 29.53058867
    private const val REF_NEW_MOON_MILLIS = 947182440000L

    fun calculateLunarInfo(nowMillis: Long = System.currentTimeMillis()): LunarInfo {
        val elapsedDays = (nowMillis - REF_NEW_MOON_MILLIS) / 86_400_000.0
        val cycles = elapsedDays / SYNODIC_MONTH_DAYS
        val fraction = ((cycles - kotlin.math.floor(cycles)) + 1.0) % 1.0
        val lunarDay = (fraction * SYNODIC_MONTH_DAYS).toFloat()
        val illumination = (((1.0 - cos(2.0 * PI * fraction)) / 2.0) * 100.0).roundToInt().coerceIn(0, 100)

        val phaseName = when {
            fraction < 0.03 || fraction >= 0.97 -> "NEW MOON"
            fraction < 0.22 -> "WAXING CRESCENT"
            fraction < 0.28 -> "FIRST QUARTER"
            fraction < 0.47 -> "WAXING GIBBOUS"
            fraction < 0.53 -> "FULL MOON"
            fraction < 0.72 -> "WANING GIBBOUS"
            fraction < 0.78 -> "LAST QUARTER"
            else -> "WANING CRESCENT"
        }

        return LunarInfo(
            phaseFraction = fraction.toFloat(),
            illuminationPercent = illumination,
            phaseName = phaseName,
            lunarDay = lunarDay
        )
    }

    fun isNightTimeNow(nowMillis: Long, settings: AppSettings): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMin = settings.autoNightStartHour * 60
        val endMin = settings.autoNightEndHour * 60
        return if (startMin > endMin) {
            minuteOfDay >= startMin || minuteOfDay < endMin
        } else {
            minuteOfDay in startMin until endMin
        }
    }

    fun formatTime(
        millis: Long,
        use24Hour: Boolean,
        timezoneId: String? = null
    ): String {
        val pattern = if (use24Hour) "HH:mm" else "h:mm"
        val sdf = SimpleDateFormat(pattern, Locale.US)
        if (!timezoneId.isNullOrBlank()) {
            sdf.timeZone = TimeZone.getTimeZone(timezoneId)
        }
        return sdf.format(Date(millis))
    }

    fun formatAmPm(millis: Long, timezoneId: String? = null): String {
        val sdf = SimpleDateFormat("a", Locale.US)
        if (!timezoneId.isNullOrBlank()) {
            sdf.timeZone = TimeZone.getTimeZone(timezoneId)
        }
        return sdf.format(Date(millis)).uppercase(Locale.US)
    }

    fun timeInWords(nowMillis: Long): Pair<String, String> {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val hours = arrayOf(
            "TWELVE", "ONE", "TWO", "THREE", "FOUR", "FIVE",
            "SIX", "SEVEN", "EIGHT", "NINE", "TEN", "ELEVEN"
        )
        val nextHour = hours[(hour24 + 1) % 12]
        val curHour = hours[hour24 % 12]
        return when {
            minute < 5 -> curHour to "O'CLOCK"
            minute < 15 -> "TEN PAST" to curHour
            minute < 25 -> "QUARTER PAST" to curHour
            minute < 35 -> "HALF PAST" to curHour
            minute < 45 -> "TWENTY TO" to nextHour
            minute < 55 -> "TEN TO" to nextHour
            else -> "ALMOST" to nextHour
        }
    }
}
