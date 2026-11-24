package com.dotwelwidgets.dotwellabs

import com.dotwelwidgets.dotwellabs.weather.GeoLocation
import com.dotwelwidgets.dotwellabs.weather.SolarKind
import com.dotwelwidgets.dotwellabs.weather.TemperatureUnit
import com.dotwelwidgets.dotwellabs.weather.WeatherCurrent
import com.dotwelwidgets.dotwellabs.weather.WeatherDay
import com.dotwelwidgets.dotwellabs.weather.WeatherDisplay
import com.dotwelwidgets.dotwellabs.weather.WeatherFailure
import com.dotwelwidgets.dotwellabs.weather.WeatherRenderState
import com.dotwelwidgets.dotwellabs.weather.WeatherResult
import com.dotwelwidgets.dotwellabs.weather.WeatherSnapshot
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherDisplayTest {
    private val london = GeoLocation(
        name = "London",
        country = "United Kingdom",
        admin1 = "England",
        latitude = 51.5,
        longitude = -0.12,
        timezone = "Europe/London",
    )

    private fun millis(value: String): Long = Instant.parse(value).toEpochMilli()

    private fun day(
        date: String,
        sunrise: String? = null,
        sunset: String? = null,
        min: Double = 10.0,
        max: Double = 20.0,
    ) = WeatherDay(
        localDate = date,
        weatherCode = 1,
        temperatureMax = max,
        temperatureMin = min,
        sunriseEpochMillis = sunrise?.let(::millis),
        sunsetEpochMillis = sunset?.let(::millis),
        precipitationProbabilityMaxPercent = 20,
    )

    private fun snapshot(
        days: List<WeatherDay>,
        fetchedAt: String = "2026-06-01T10:00:00Z",
        unit: TemperatureUnit = TemperatureUnit.CELSIUS,
        location: GeoLocation = london,
    ) = WeatherSnapshot(
        location = location,
        unit = unit,
        current = WeatherCurrent(15.0, 14.0, 1, true, millis(fetchedAt)),
        forecast = days,
        airQuality = null,
        fetchedAtEpochMillis = millis(fetchedAt),
    )

    @Test
    fun nonFiniteTemperaturesAreNeverShownAsZero() {
        assertNull(WeatherDisplay.temperature(Double.NaN, TemperatureUnit.CELSIUS))
        assertNull(WeatherDisplay.temperature(Double.POSITIVE_INFINITY, TemperatureUnit.CELSIUS))
        assertNull(
            WeatherDisplay.highLowText(
                day("2026-06-01", min = Double.NaN, max = 20.0),
                TemperatureUnit.CELSIUS,
            ),
        )
    }

    @Test
    fun unitSymbolsAndHighLowLabelsStayCorrect() {
        assertEquals("C", WeatherDisplay.unitSymbol(TemperatureUnit.CELSIUS))
        assertEquals("F", WeatherDisplay.unitSymbol(TemperatureUnit.FAHRENHEIT))
        assertEquals(
            "H : 20\u00B0C   L : 10\u00B0C",
            WeatherDisplay.highLowText(day("2026-06-01"), TemperatureUnit.CELSIUS),
        )
    }

    @Test
    fun staleUsesAgeOrCityLocalDateRollover() {
        val data = snapshot(listOf(day("2026-06-01", "2026-06-01T04:00:00Z", "2026-06-01T20:00:00Z")))
        assertFalse(WeatherDisplay.isStale(data, millis("2026-06-01T10:30:00Z")))
        assertTrue(WeatherDisplay.isStale(data, millis("2026-06-01T12:00:00Z")))
        assertTrue(WeatherDisplay.isStale(data, millis("2026-06-02T00:01:00Z")))
    }

    @Test
    fun todayUsesTheSelectedCityZoneAndDate() {
        val data = snapshot(
            listOf(
                day("2026-06-01"),
                day("2026-06-02", min = 11.0, max = 21.0),
            ),
        )
        val today = WeatherDisplay.today(data, millis("2026-06-02T10:00:00Z"))
        assertEquals("2026-06-02", today?.localDate)
    }

    @Test
    fun solarStatePicksTheNextRealEvent() {
        val data = snapshot(listOf(day("2026-06-01", "2026-06-01T04:00:00Z", "2026-06-01T20:00:00Z")))
        val midday = WeatherDisplay.solarState(data, millis("2026-06-01T12:00:00Z"))
        assertTrue(midday?.isDay == true)
        assertEquals(SolarKind.SUNSET, midday?.event?.kind)

        val beforeDawn = WeatherDisplay.solarState(data, millis("2026-06-01T03:00:00Z"))
        assertFalse(beforeDawn?.isDay == true)
        assertEquals(SolarKind.SUNRISE, beforeDawn?.event?.kind)
    }

    @Test
    fun nextSunriseRollsIntoTheFollowingForecastDay() {
        val data = snapshot(
            listOf(
                day("2026-06-01", "2026-06-01T04:00:00Z", "2026-06-01T20:00:00Z"),
                day("2026-06-02", "2026-06-02T04:00:00Z", "2026-06-02T20:00:00Z"),
            ),
        )
        val night = WeatherDisplay.solarState(data, millis("2026-06-01T22:00:00Z"))
        assertFalse(night?.isDay == true)
        assertEquals(SolarKind.SUNRISE, night?.event?.kind)
        assertEquals(millis("2026-06-02T04:00:00Z"), night?.event?.epochMillis)
    }

    @Test
    fun polarDayWithoutSunriseOrSunsetIsUnavailable() {
        val polar = london.copy(name = "Longyearbyen", timezone = "Arctic/Longyearbyen")
        val data = snapshot(listOf(day("2026-06-01")), location = polar)
        assertNull(WeatherDisplay.solarState(data, millis("2026-06-01T12:00:00Z")))
    }

    @Test
    fun cachedStatusAgesClearlyAndSurfacesRefreshFailure() {
        val data = snapshot(listOf(day("2026-06-01")))
        val failure = WeatherFailure(london, WeatherResult.Reason.NETWORK, millis("2026-06-01T10:30:00Z"))
        val line = WeatherDisplay.statusLine(data, millis("2026-06-01T12:00:00Z"), failure)
        assertTrue(line.contains("Cached 11:00"))
        assertTrue(line.contains("STALE"))
        assertTrue(line.contains("REFRESH FAILED"))
    }

    @Test
    fun failureWithoutCacheIsNotShownAsFreshOrEmpty() {
        val state = WeatherRenderState(
            snapshot = null,
            enabled = true,
            hasLocation = true,
            now = millis("2026-06-01T12:00:00Z"),
            failure = WeatherFailure(london, WeatherResult.Reason.NETWORK, millis("2026-06-01T11:00:00Z")),
        )
        assertTrue(WeatherDisplay.setupMessage(state).contains("Refresh failed"))
    }
}
