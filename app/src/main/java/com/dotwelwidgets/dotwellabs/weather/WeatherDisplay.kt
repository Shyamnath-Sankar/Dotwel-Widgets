package com.dotwelwidgets.dotwellabs.weather

import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

enum class SolarKind { SUNRISE, SUNSET }

data class SolarEvent(val kind: SolarKind, val epochMillis: Long)

data class SolarState(val isDay: Boolean, val event: SolarEvent)

/**
 * Snapshot plus the opt-in state the renderers need. It never performs network
 * I/O and never invents a fallback reading.
 */
data class WeatherRenderState(
    val snapshot: WeatherSnapshot?,
    val enabled: Boolean,
    val hasLocation: Boolean,
    val now: Long,
    val failure: WeatherFailure? = null,
) {
    companion object {
        fun from(context: Context, now: Long = System.currentTimeMillis()): WeatherRenderState {
            val enabled = WeatherSettings.isEnabled(context)
            val location = WeatherSettings.location(context)
            val snapshot = if (enabled && location != null) {
                WeatherCache.load(context)
            } else {
                null
            }
            return WeatherRenderState(
                snapshot = snapshot,
                enabled = enabled,
                hasLocation = location != null,
                now = now,
                failure = if (enabled && location != null) {
                    WeatherSettings.lastFailure(context)
                } else {
                    null
                },
            )
        }
    }
}

object WeatherDisplay {
    private val fetchedFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val dayFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
    private val eventFormatter = DateTimeFormatter.ofPattern("hh:mma", Locale.US)

    fun zone(location: GeoLocation): ZoneId = runCatching {
        ZoneId.of(location.timezone ?: "UTC")
    }.getOrDefault(ZoneOffset.UTC)

    fun localDate(epochMillis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    fun today(snapshot: WeatherSnapshot, now: Long): WeatherDay? {
        val date = localDate(now, zone(snapshot.location)).toString()
        return snapshot.forecast.firstOrNull { it.localDate == date }
    }

    fun isStale(snapshot: WeatherSnapshot, now: Long): Boolean {
        val zone = zone(snapshot.location)
        val fetchedDate = localDate(snapshot.fetchedAtEpochMillis, zone)
        val currentDate = localDate(now, zone)
        return now - snapshot.fetchedAtEpochMillis > snapshot.staleAfterMillis ||
            fetchedDate != currentDate
    }

    fun fetchedTime(snapshot: WeatherSnapshot): String =
        fetchedFormatter.format(
            Instant.ofEpochMilli(snapshot.fetchedAtEpochMillis)
                .atZone(zone(snapshot.location)),
        )

    fun forecastDayLabel(day: WeatherDay): String = runCatching {
        LocalDate.parse(day.localDate).format(dayFormatter)
    }.getOrDefault(day.localDate)

    fun temperature(value: Double, unit: TemperatureUnit): String? {
        if (!value.isFinite()) return null
        return "${value.roundToInt()}\u00B0${unitSymbol(unit)}"
    }

    fun unitSymbol(unit: TemperatureUnit): String =
        if (unit == TemperatureUnit.FAHRENHEIT) "F" else "C"


    fun currentTemperatureText(snapshot: WeatherSnapshot): String? {
        val value = snapshot.current.temperature
        if (!value.isFinite()) return null
        return "${value.roundToInt()}\u00B0${unitSymbol(snapshot.unit)}"
    }

    /**
     * Open-Meteo `weather_code` follows the WMO 4677 interpretation table. The
     * label is a short description, never a fabricated icon or measurement.
     */
    fun conditionLabel(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mainly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75 -> "Snow"
        77 -> "Snow grains"
        80, 81, 82 -> "Showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Unknown"
    }

    fun currentConditionText(snapshot: WeatherSnapshot): String =
        conditionLabel(snapshot.current.weatherCode)

    /**
     * The configured Open-Meteo air-quality endpoint reports United States AQI.
     * A missing reading is shown as unavailable, never as zero.
     */
    fun airQualityText(snapshot: WeatherSnapshot): String {
        val aqi = snapshot.airQuality?.usAqi ?: return "AQI --"
        return "AQI $aqi"
    }

    fun highLowText(day: WeatherDay, unit: TemperatureUnit): String? {
        val max = temperature(day.temperatureMax, unit) ?: return null
        val min = temperature(day.temperatureMin, unit) ?: return null
        return "H : $max   L : $min"
    }

    /**
     * Actual daylight now comes from the cached sunrise/sunset instants in the
     * selected city's zone. The API's stale current.isDay flag is not used.
     */
    fun solarState(snapshot: WeatherSnapshot, now: Long): SolarState? {
        val zone = zone(snapshot.location)
        val date = localDate(now, zone).toString()
        val today = snapshot.forecast.firstOrNull { it.localDate == date } ?: return null
        val sunrise = today.sunriseEpochMillis?.takeIf { it > 0L } ?: return null
        val sunset = today.sunsetEpochMillis?.takeIf { it > 0L } ?: return null
        val isDay = now >= sunrise && now < sunset
        val wanted = if (isDay) SolarKind.SUNSET else SolarKind.SUNRISE
        val next = snapshot.forecast.asSequence()
            .flatMap { day ->
                sequenceOf(
                    day.sunriseEpochMillis?.takeIf { it > 0L }?.let {
                        SolarEvent(SolarKind.SUNRISE, it)
                    },
                    day.sunsetEpochMillis?.takeIf { it > 0L }?.let {
                        SolarEvent(SolarKind.SUNSET, it)
                    },
                )
            }
            .filterNotNull()
            .filter { it.kind == wanted && it.epochMillis > now }
            .minByOrNull { it.epochMillis }
            ?: return null
        return SolarState(isDay, next)
    }

    fun eventTime(event: SolarEvent, location: GeoLocation): String =
        eventFormatter.format(
            Instant.ofEpochMilli(event.epochMillis).atZone(zone(location)),
        )

    fun statusLine(
        snapshot: WeatherSnapshot,
        now: Long,
        failure: WeatherFailure? = null,
    ): String {
        val stale = if (isStale(snapshot, now)) " \u00B7 STALE" else ""
        val failed = if (failure != null) " \u00B7 REFRESH FAILED" else ""
        return "Cached ${fetchedTime(snapshot)}$stale$failed"
    }

    fun locationLine(location: GeoLocation): String = buildString {
        append(location.name)
        location.admin1?.let { append(", ").append(it) }
        location.country?.let { append(", ").append(it) }
    }

    fun setupMessage(state: WeatherRenderState): String = when {
        !state.enabled -> "Weather is off \u00B7 Tap to set up"
        !state.hasLocation -> "Choose a city \u00B7 Tap to set up"
        state.snapshot == null && state.failure != null -> "Refresh failed \u00B7 Tap to retry"
        state.snapshot == null -> "No cached weather \u00B7 Tap to refresh"
        else -> "Weather unavailable"
    }
}
