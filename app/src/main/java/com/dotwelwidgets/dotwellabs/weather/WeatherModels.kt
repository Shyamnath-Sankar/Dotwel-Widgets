package com.dotwelwidgets.dotwellabs.weather

enum class TemperatureUnit(val apiValue: String) {
    CELSIUS("celsius"),
    FAHRENHEIT("fahrenheit"),
}

data class GeoLocation(
    val name: String,
    val country: String?,
    val admin1: String?,
    val latitude: Double,
    val longitude: Double,
    val timezone: String?,
)

data class WeatherCurrent(
    val temperature: Double,
    val apparentTemperature: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val observedAtEpochMillis: Long,
)

data class WeatherDay(
    val localDate: String,
    val weatherCode: Int,
    val temperatureMax: Double,
    val temperatureMin: Double,
    val sunriseEpochMillis: Long?,
    val sunsetEpochMillis: Long?,
    val precipitationProbabilityMaxPercent: Int?,
)

data class WeatherAirQuality(
    val usAqi: Int?,
    val observedAtEpochMillis: Long?,
)

data class WeatherSnapshot(
    val location: GeoLocation,
    val unit: TemperatureUnit,
    val current: WeatherCurrent,
    val forecast: List<WeatherDay>,
    val airQuality: WeatherAirQuality?,
    val fetchedAtEpochMillis: Long,
    val staleAfterMillis: Long = DEFAULT_STALE_AFTER_MILLIS,
) {
    val isStale: Boolean
        get() = System.currentTimeMillis() - fetchedAtEpochMillis > staleAfterMillis

    companion object {
        const val DEFAULT_STALE_AFTER_MILLIS = 60 * 60 * 1000L
    }
}

sealed class WeatherResult {
    data class Success(val snapshot: WeatherSnapshot) : WeatherResult()
    data class Failure(
        val reason: Reason,
        val cached: WeatherSnapshot? = null,
    ) : WeatherResult()

    enum class Reason {
        NETWORK,
        HTTP,
        PARSE,
        NO_LOCATION,
        CANCELLED,
    }
}

/**
 * Last failed refresh for one manually selected city. It is deliberately kept
 * apart from the forecast cache so a cached widget can state that its last
 * refresh failed instead of presenting old data as freshly updated.
 */
data class WeatherFailure(
    val location: GeoLocation,
    val reason: WeatherResult.Reason,
    val atMillis: Long,
)

/**
 * Pure deletion plan so a disable and a full delete cannot silently drift apart:
 * disable always removes city, cache and failure; full delete also removes unit.
 */
data class WeatherClearPlan(
    val clearLocation: Boolean,
    val clearCache: Boolean,
    val clearFailure: Boolean,
    val clearUnit: Boolean,
) {
    companion object {
        fun disable(clearUnit: Boolean = false) = WeatherClearPlan(
            clearLocation = true,
            clearCache = true,
            clearFailure = true,
            clearUnit = clearUnit,
        )

        fun selectCity() = WeatherClearPlan(
            clearLocation = false,
            clearCache = true,
            clearFailure = true,
            clearUnit = false,
        )

        fun changeUnit() = WeatherClearPlan(
            clearLocation = false,
            clearCache = true,
            clearFailure = true,
            clearUnit = false,
        )
    }
}

/**
 * Pure consent/commit guard. A request token is valid only while weather is
 * enabled, the selected city and unit still match, and no disable/location/unit
 * transaction has bumped the generation.
 */
data class WeatherAccessPolicy(
    val enabled: Boolean,
    val location: GeoLocation?,
    val unit: TemperatureUnit,
    val generation: Int,
) {
    fun maySearch(): Boolean = enabled

    fun mayFetch(): Boolean = enabled && location != null

    fun maySelectCity(): Boolean = enabled

    fun mayCommit(
        token: Int,
        committedLocation: GeoLocation,
        committedUnit: TemperatureUnit,
    ): Boolean = enabled &&
        generation == token &&
        location == committedLocation &&
        unit == committedUnit
}
