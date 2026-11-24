package com.dotwelwidgets.dotwellabs

import com.dotwelwidgets.dotwellabs.weather.GeoLocation
import com.dotwelwidgets.dotwellabs.weather.TemperatureUnit
import com.dotwelwidgets.dotwellabs.weather.WeatherAirQuality
import com.dotwelwidgets.dotwellabs.weather.WeatherCurrent
import com.dotwelwidgets.dotwellabs.weather.WeatherRenderState
import com.dotwelwidgets.dotwellabs.weather.WeatherSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KwgtDateTime3RendererTest {
    private val location = GeoLocation(
        name = "Berlin",
        country = "Germany",
        admin1 = "Berlin",
        latitude = 52.52,
        longitude = 13.41,
        timezone = "Europe/Berlin",
    )

    @Test
    fun weatherLineIncludesCurrentTemperatureConditionAndUsAqi() {
        val snapshot = WeatherSnapshot(
            location = location,
            unit = TemperatureUnit.CELSIUS,
            current = WeatherCurrent(12.4, 11.0, 2, true, 1L),
            forecast = emptyList(),
            airQuality = WeatherAirQuality(42, 1L),
            fetchedAtEpochMillis = 1L,
        )
        val line = KwgtDateTime3Renderer.weatherLine(
            WeatherRenderState(snapshot, enabled = true, hasLocation = true, now = 1L),
        )
        assertEquals("12\u00B0C Partly cloudy, AQI 42", line)
    }

    @Test
    fun missingAqiIsStatedInsteadOfInvented() {
        val snapshot = WeatherSnapshot(
            location = location,
            unit = TemperatureUnit.CELSIUS,
            current = WeatherCurrent(12.4, 11.0, 0, true, 1L),
            forecast = emptyList(),
            airQuality = null,
            fetchedAtEpochMillis = 1L,
        )
        val line = KwgtDateTime3Renderer.weatherLine(
            WeatherRenderState(snapshot, enabled = true, hasLocation = true, now = 1L),
        )
        assertTrue(line.endsWith("AQI --"))
    }

    @Test
    fun layoutUsesSourceTextRatios() {
        val layout = KwgtDateTime3Renderer.layout(916, 542)
        assertEquals(542f, layout.side, 0.01f)
        assertEquals(542f / 2.8f, layout.timeSize, 0.01f)
        assertEquals(542f / 6.5f, layout.dateSize, 0.01f)
        assertEquals(542f / 8f, layout.weatherSize, 0.01f)
    }
}
