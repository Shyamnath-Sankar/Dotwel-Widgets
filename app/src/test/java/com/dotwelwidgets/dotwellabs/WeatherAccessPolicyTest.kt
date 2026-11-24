package com.dotwelwidgets.dotwellabs

import com.dotwelwidgets.dotwellabs.weather.GeoLocation
import com.dotwelwidgets.dotwellabs.weather.TemperatureUnit
import com.dotwelwidgets.dotwellabs.weather.WeatherAccessPolicy
import com.dotwelwidgets.dotwellabs.weather.WeatherClearPlan
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherAccessPolicyTest {
    private val london = GeoLocation(
        name = "London",
        country = "United Kingdom",
        admin1 = "England",
        latitude = 51.5,
        longitude = -0.12,
        timezone = "Europe/London",
    )

    private fun policy(
        enabled: Boolean = true,
        location: GeoLocation? = london,
        unit: TemperatureUnit = TemperatureUnit.CELSIUS,
        generation: Int = 7,
    ) = WeatherAccessPolicy(enabled, location, unit, generation)

    @Test
    fun disabledConsentBlocksSearchFetchSelectionAndCommit() {
        val disabled = policy(enabled = false)
        assertFalse(disabled.maySearch())
        assertFalse(disabled.mayFetch())
        assertFalse(disabled.maySelectCity())
        assertFalse(disabled.mayCommit(7, london, TemperatureUnit.CELSIUS))
    }

    @Test
    fun enabledWithoutCityCanSearchButCannotFetch() {
        val noCity = policy(location = null)
        assertTrue(noCity.maySearch())
        assertFalse(noCity.mayFetch())
        assertTrue(noCity.maySelectCity())
    }

    @Test
    fun staleGenerationCannotCommitAfterDisableOrLocationChange() {
        val current = policy(generation = 8)
        assertTrue(current.mayCommit(8, london, TemperatureUnit.CELSIUS))
        assertFalse(current.mayCommit(7, london, TemperatureUnit.CELSIUS))
    }

    @Test
    fun oldCityOrUnitCannotCommitIntoNewSelection() {
        val current = policy(unit = TemperatureUnit.CELSIUS, generation = 9)
        val otherCity = london.copy(name = "Paris", latitude = 48.85, longitude = 2.35)
        assertFalse(current.mayCommit(9, otherCity, TemperatureUnit.CELSIUS))
        assertFalse(current.mayCommit(9, london, TemperatureUnit.FAHRENHEIT))
    }

    @Test
    fun disableClearsCityCacheAndFailureAndFullDeleteAlsoClearsUnit() {
        val off = WeatherClearPlan.disable()
        assertTrue(off.clearLocation)
        assertTrue(off.clearCache)
        assertTrue(off.clearFailure)
        assertFalse(off.clearUnit)

        val deleted = WeatherClearPlan.disable(clearUnit = true)
        assertTrue(deleted.clearLocation)
        assertTrue(deleted.clearCache)
        assertTrue(deleted.clearFailure)
        assertTrue(deleted.clearUnit)
    }

    @Test
    fun citySelectionAndUnitChangeInvalidateCacheWithoutDroppingCityOrUnit() {
        val city = WeatherClearPlan.selectCity()
        assertFalse(city.clearLocation)
        assertTrue(city.clearCache)
        assertTrue(city.clearFailure)
        assertFalse(city.clearUnit)

        val unit = WeatherClearPlan.changeUnit()
        assertFalse(unit.clearLocation)
        assertTrue(unit.clearCache)
        assertTrue(unit.clearFailure)
        assertFalse(unit.clearUnit)
    }

    @Test
    fun inFlightResultCannotReEnableWeather() {
        val revoked = policy(enabled = false)
        assertFalse(revoked.mayCommit(7, london, TemperatureUnit.CELSIUS))
        assertFalse(revoked.maySelectCity())
        assertFalse(revoked.mayFetch())
    }
}
