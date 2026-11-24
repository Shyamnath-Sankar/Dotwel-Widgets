package com.dotwelwidgets.dotwellabs.weather

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Injectable Open-Meteo transport. Production uses [HttpWeatherTransport];
 * tests supply fixtures without touching the network.
 */
fun interface WeatherTransport {
    fun get(url: String): String
}

/**
 * Reusable, UI-independent Open-Meteo transport adapted from the author-written
 * NtWxData/NtWxFetch/NtWxModel data flow in AwesomeAWidgets commit
 * 0fae60722764bda790f89bd94119c6dab854d90e.
 *
 * The OEM net.oneplus OpSettings bridge is deliberately replaced with a small
 * manually-selected city/unit contract. No API key, paid service, GPS permission
 * or telemetry is used. Callers must run these blocking methods off the main
 * thread. Responses are bounded and use strict connect/read timeouts.
 */
object HttpWeatherTransport : WeatherTransport {
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 15_000
    private const val MAX_RESPONSE_BYTES = 512 * 1024

    override fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("Open-Meteo HTTP $code")
            }
            return connection.inputStream.use(::readLimited)
        } finally {
            connection.disconnect()
        }
    }

    private fun readLimited(input: InputStream): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_RESPONSE_BYTES) {
                throw IOException("Open-Meteo response too large")
            }
            output.write(buffer, 0, read)
        }
        return output.toString("UTF-8")
    }
}

object WeatherRepository {
    private const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
    private const val AIR_URL = "https://air-quality-api.open-meteo.com/v1/air-quality"
    private const val GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"
    private const val MAX_QUERY_LENGTH = 120

    fun searchCities(
        query: String,
        count: Int = 8,
        transport: WeatherTransport = HttpWeatherTransport,
    ): List<GeoLocation> {
        val clean = query.trim().take(MAX_QUERY_LENGTH)
        if (clean.isBlank()) return emptyList()
        val url = buildString {
            append(GEOCODING_URL)
            append("?name=").append(URLEncoder.encode(clean, "UTF-8"))
            append("&count=").append(count.coerceIn(1, 20))
            append("&language=en&format=json")
        }
        val body = transport.get(url)
        val results = JSONObject(body).optJSONArray("results") ?: JSONArray()
        return buildList {
            for (index in 0 until results.length()) {
                val item = results.optJSONObject(index) ?: continue
                val latitude = item.optDouble("latitude", Double.NaN)
                val longitude = item.optDouble("longitude", Double.NaN)
                if (!latitude.isFinite() || !longitude.isFinite()) continue
                add(
                    GeoLocation(
                        name = item.optString("name", "Unknown"),
                        country = item.optString("country").ifBlank { null },
                        admin1 = item.optString("admin1").ifBlank { null },
                        latitude = latitude,
                        longitude = longitude,
                        timezone = item.optString("timezone").ifBlank { null },
                    ),
                )
            }
        }
    }

    fun fetch(
        context: Context?,
        location: GeoLocation,
        unit: TemperatureUnit,
        transport: WeatherTransport = HttpWeatherTransport,
        now: Long = System.currentTimeMillis(),
        shouldCommit: () -> Boolean = { true },
    ): WeatherResult {
        if (!location.latitude.isFinite() || !location.longitude.isFinite()) {
            return WeatherResult.Failure(WeatherResult.Reason.NO_LOCATION)
        }
        val cached = context?.let { WeatherCache.load(it) }
        val unitParam = if (unit == TemperatureUnit.FAHRENHEIT) {
            "&temperature_unit=fahrenheit"
        } else {
            ""
        }
        val forecastUrl = buildString {
            append(FORECAST_URL)
            append("?latitude=").append(location.latitude)
            append("&longitude=").append(location.longitude)
            append("&current=temperature_2m,apparent_temperature,weather_code,is_day")
            append("&hourly=precipitation_probability")
            append("&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset")
            append("&timezone=auto&forecast_days=7")
            append(unitParam)
        }
        val airUrl = buildString {
            append(AIR_URL)
            append("?latitude=").append(location.latitude)
            append("&longitude=").append(location.longitude)
            append("&current=us_aqi")
        }
        return try {
            val forecast = transport.get(forecastUrl)
            val air = runCatching { transport.get(airUrl) }.getOrNull()
            val snapshot = parseSnapshot(location, unit, forecast, air, now)
            if (context != null &&
                shouldCommit() &&
                WeatherSettings.isEnabled(context) &&
                WeatherSettings.location(context) == location &&
                WeatherSettings.unit(context) == unit
            ) {
                WeatherCache.save(context, forecast, air, location, unit, now)
            }
            WeatherResult.Success(snapshot)
        } catch (error: Exception) {
            val reason = when (error) {
                is IOException -> WeatherResult.Reason.NETWORK
                is org.json.JSONException -> WeatherResult.Reason.PARSE
                else -> WeatherResult.Reason.HTTP
            }
            WeatherResult.Failure(reason, cached, error.message)
        }
    }

    fun loadCached(context: Context): WeatherSnapshot? = WeatherCache.load(context)

    internal fun parseSnapshot(
        location: GeoLocation,
        unit: TemperatureUnit,
        forecastBody: String,
        airBody: String?,
        fetchedAt: Long,
    ): WeatherSnapshot {
        val root = JSONObject(forecastBody)
        val timezone = root.optString("timezone").ifBlank {
            location.timezone ?: "UTC"
        }
        val zone = runCatching { ZoneId.of(timezone) }
            .getOrElse { ZoneOffset.ofTotalSeconds(root.optInt("utc_offset_seconds", 0)) }
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        val dates = daily.optJSONArray("time") ?: JSONArray()
        val codes = daily.optJSONArray("weather_code")
        val highs = daily.optJSONArray("temperature_2m_max")
        val lows = daily.optJSONArray("temperature_2m_min")
        val sunrises = daily.optJSONArray("sunrise")
        val sunsets = daily.optJSONArray("sunset")
        val rainByDate = precipitationByDate(root.optJSONObject("hourly"))

        val days = buildList {
            for (index in 0 until dates.length()) {
                val date = dates.optString(index)
                if (date.isBlank()) continue
                add(
                    WeatherDay(
                        localDate = date,
                        weatherCode = codes?.optInt(index, 0) ?: 0,
                        temperatureMax = highs?.optDouble(index, Double.NaN) ?: Double.NaN,
                        temperatureMin = lows?.optDouble(index, Double.NaN) ?: Double.NaN,
                        sunriseEpochMillis = sunrises?.optString(index)?.let {
                            parseLocal(it, zone)
                        },
                        sunsetEpochMillis = sunsets?.optString(index)?.let {
                            parseLocal(it, zone)
                        },
                        precipitationProbabilityMaxPercent = rainByDate[date],
                    ),
                )
            }
        }
        val currentSnapshot = WeatherCurrent(
            temperature = current.optDouble("temperature_2m", Double.NaN),
            apparentTemperature = current.optDouble("apparent_temperature", Double.NaN),
            weatherCode = current.optInt("weather_code", 0),
            isDay = current.optInt("is_day", 1) == 1,
            observedAtEpochMillis = parseLocal(current.optString("time"), zone) ?: fetchedAt,
        )
        val air = airBody?.let { parseAir(it, zone) }
        return WeatherSnapshot(
            location = location,
            unit = unit,
            current = currentSnapshot,
            forecast = days,
            airQuality = air,
            fetchedAtEpochMillis = fetchedAt,
        )
    }

    private fun parseAir(body: String, zone: ZoneId): WeatherAirQuality? = try {
        val current = JSONObject(body).optJSONObject("current") ?: return null
        WeatherAirQuality(
            usAqi = if (current.has("us_aqi")) current.optInt("us_aqi") else null,
            observedAtEpochMillis = parseLocal(current.optString("time"), zone),
        )
    } catch (_: Exception) {
        null
    }

    private fun precipitationByDate(hourly: JSONObject?): Map<String, Int> {
        val times = hourly?.optJSONArray("time") ?: return emptyMap()
        val values = hourly.optJSONArray("precipitation_probability") ?: return emptyMap()
        val result = HashMap<String, Int>()
        for (index in 0 until minOf(times.length(), values.length())) {
            val time = times.optString(index)
            val date = time.substringBefore('T')
            val value = values.optInt(index, -1)
            if (value < 0) continue
            result[date] = maxOf(result[date] ?: 0, value)
        }
        return result
    }

    private fun parseLocal(value: String, zone: ZoneId): Long? {
        if (value.isBlank()) return null
        return runCatching {
            LocalDateTime.parse(value).atZone(zone).toInstant().toEpochMilli()
        }.getOrNull()
    }
}

internal object WeatherCache {
    private const val KEY_FORECAST = "cache_forecast_json"
    private const val KEY_AIR = "cache_air_json"
    private const val KEY_LOCATION = "cache_location"
    private const val KEY_UNIT = "cache_unit"
    private const val KEY_FETCHED_AT = "cache_fetched_at"

    private var context: Context? = null

    fun attach(context: Context) {
        this.context = context.applicationContext
    }

    fun save(
        context: Context,
        forecast: String,
        air: String?,
        location: GeoLocation,
        unit: TemperatureUnit,
        fetchedAt: Long,
    ) {
        val prefs = context.getSharedPreferences(
            WeatherSettings.PREFS,
            Context.MODE_PRIVATE,
        )
        prefs.edit()
            .putString(KEY_FORECAST, forecast)
            .putString(KEY_AIR, air)
            .putString(KEY_LOCATION, WeatherSettings.encodeLocation(location))
            .putString(KEY_UNIT, unit.name)
            .putLong(KEY_FETCHED_AT, fetchedAt)
            .apply()
    }

    fun load(): WeatherSnapshot? = context?.let(::load)

    fun load(context: Context): WeatherSnapshot? {
        this.context = context.applicationContext
        val prefs = context.getSharedPreferences(
            WeatherSettings.PREFS,
            Context.MODE_PRIVATE,
        )
        val forecast = prefs.getString(KEY_FORECAST, null) ?: return null
        val fetchedAt = prefs.getLong(KEY_FETCHED_AT, 0L)
        if (fetchedAt <= 0L) return null
        val cachedLocation = WeatherSettings.decodeLocation(
            prefs.getString(KEY_LOCATION, null),
        ) ?: return null
        val selectedLocation = WeatherSettings.location(context) ?: return null
        if (cachedLocation != selectedLocation) return null
        val cachedUnit = runCatching {
            TemperatureUnit.valueOf(prefs.getString(KEY_UNIT, null) ?: "")
        }.getOrNull() ?: return null
        if (cachedUnit != WeatherSettings.unit(context)) return null
        return runCatching {
            WeatherRepository.parseSnapshot(
                cachedLocation,
                cachedUnit,
                forecast,
                prefs.getString(KEY_AIR, null),
                fetchedAt,
            )
        }.getOrNull()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(WeatherSettings.PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_FORECAST)
            .remove(KEY_AIR)
            .remove(KEY_LOCATION)
            .remove(KEY_UNIT)
            .remove(KEY_FETCHED_AT)
            .apply()
    }
}
