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
        const val DEFAULT_STALE_AFTER_MILLIS = 30 * 60 * 1000L
    }
}

sealed class WeatherResult {
    data class Success(val snapshot: WeatherSnapshot) : WeatherResult()
    data class Failure(
        val reason: Reason,
        val cached: WeatherSnapshot? = null,
        val detail: String? = null,
    ) : WeatherResult()

    enum class Reason {
        NETWORK,
        HTTP,
        PARSE,
        NO_LOCATION,
    }
}
