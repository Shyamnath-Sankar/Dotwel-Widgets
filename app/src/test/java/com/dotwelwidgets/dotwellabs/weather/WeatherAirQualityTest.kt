package com.dotwelwidgets.dotwellabs.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherAirQualityTest {
    @Test
    fun parsesUsAqiAndObservationTime() {
        val body = """
            {"timezone":"Europe/Berlin","current":{"time":"2026-11-24T13:00","us_aqi":28}}
        """.trimIndent()
        val aqi = WeatherRepository.parseAirQuality(body)
        assertEquals(28, aqi?.usAqi)
        assertEquals(true, aqi?.observedAtEpochMillis != null)
    }

    @Test
    fun missingOrNegativeAqiStaysUnavailable() {
        assertNull(WeatherRepository.parseAirQuality("""{"current":{"time":"x"}}"""))
        assertNull(
            WeatherRepository.parseAirQuality(
                """{"timezone":"UTC","current":{"time":"2026-11-24T13:00","us_aqi":-1}}""",
            ),
        )
    }
}
