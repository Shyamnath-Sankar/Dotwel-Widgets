package com.dotwelwidgets.dotwellabs.weather

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Reusable, UI-independent Open-Meteo transport adapted from the author-written
 * NtWxData/NtWxFetch/NtWxModel data flow in AwesomeAWidgets commit
 * 0fae60722764bda790f89bd94119c6dab854d90e.
 *
 * The OEM net.oneplus OpSettings bridge is deliberately replaced with a small
 * location/unit contract. No API key, paid service, GPS permission or telemetry
 * is used. Callers must run these blocking methods off the main thread.
 */
object WeatherRepository {
    private const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
    private const val AIR_URL = "https://air-quality-api.open-meteo.com/v1/air-quality"
    private const val GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 15_000

    fun searchCities(query: String, count: Int = 8): List<GeoLocation> {
        if (query.isBlank()) return emptyList()
        val url = buildString {
            append(GEOCODING_URL)
            append("?name=").append(URLEncoder.encode(query, "UTF-8"))
            append("&count=").append(count.coerceIn(1, 20))
            append("&language=en&format=json")
        }
        val body = get(url)
        val results = JSONObject(body).optJSONArray("results") ?: JSONArray()
        return buildList {
            for (index in 0 until results.length()) {
                val item = results.optJSONObject(index) ?: continue
                val latitude = item.optDouble("latitude", Double.NaN)
                val longitude = item.optDouble("longitude", Double.NaN)
                if (latitude.isNaN() || longitude.isNaN()) continue
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

    fun fetch(location: GeoLocation, unit: TemperatureUnit): WeatherResult {
        val cached = WeatherCache.load()
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
            val forecast = get(forecastUrl)
            val air = try {
                get(airUrl)
            } catch (_: Exception) {
                null
            }
            val fetchedAt = System.currentTimeMillis()
            val snapshot = parse(location, unit, forecast, air, fetchedAt)
            WeatherCache.save(forecast, air, location, unit, fetchedAt)
            WeatherResult.Success(snapshot)
        } catch (error: Exception) {
            val reason = when (error) {
                is java.io.IOException -> WeatherResult.Reason.NETWORK
                is org.json.JSONException -> WeatherResult.Reason.PARSE
                else -> WeatherResult.Reason.HTTP
            }
            WeatherResult.Failure(reason, cached, error.message)
        }
    }

    fun loadCached(): WeatherSnapshot? = WeatherCache.load()

    private fun parse(
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
        val hourly = root.optJSONObject("hourly")
        val dates = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val highs = daily.getJSONArray("temperature_2m_max")
        val lows = daily.getJSONArray("temperature_2m_min")
        val sunrises = daily.optJSONArray("sunrise")
        val sunsets = daily.optJSONArray("sunset")
        val rainByDate = precipitationByDate(hourly, zone)

        val days = buildList {
            for (index in 0 until dates.length()) {
                val date = dates.optString(index)
                add(
                    WeatherDay(
                        localDate = date,
                        weatherCode = codes.optInt(index),
                        temperatureMax = highs.optDouble(index, Double.NaN),
                        temperatureMin = lows.optDouble(index, Double.NaN),
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
            temperature = current.getDouble("temperature_2m"),
            apparentTemperature = current.getDouble("apparent_temperature"),
            weatherCode = current.getInt("weather_code"),
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

    private fun precipitationByDate(
        hourly: JSONObject?,
        zone: ZoneId,
    ): Map<String, Int> {
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

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw java.io.IOException("Open-Meteo HTTP $code")
            }
            BufferedReader(connection.inputStream.reader(Charsets.UTF_8)).use { reader ->
                return reader.readText()
            }
        } finally {
            connection.disconnect()
        }
    }
}

internal object WeatherCache {
    private const val PREFS = "dotwel_weather"
    private const val KEY_FORECAST = "forecast_json"
    private const val KEY_AIR = "air_json"
    private const val KEY_LOCATION = "location_json"
    private const val KEY_UNIT = "unit"
    private const val KEY_FETCHED_AT = "fetched_at"

    private var context: Context? = null

    fun attach(context: Context) {
        this.context = context.applicationContext
    }

    fun save(
        forecast: String,
        air: String?,
        location: GeoLocation,
        unit: TemperatureUnit,
        fetchedAt: Long,
    ) {
        val prefs = context?.getSharedPreferences(PREFS, Context.MODE_PRIVATE) ?: return
        prefs.edit()
            .putString(KEY_FORECAST, forecast)
            .putString(KEY_AIR, air)
            .putString(KEY_LOCATION, encodeLocation(location))
            .putString(KEY_UNIT, unit.name)
            .putLong(KEY_FETCHED_AT, fetchedAt)
            .apply()
    }

    fun load(): WeatherSnapshot? = null

    fun load(context: Context): WeatherSnapshot? {
        this.context = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val forecast = prefs.getString(KEY_FORECAST, null) ?: return null
        val location = decodeLocation(prefs.getString(KEY_LOCATION, null)) ?: return null
        val unit = runCatching {
            TemperatureUnit.valueOf(prefs.getString(KEY_UNIT, null) ?: return null)
        }.getOrNull() ?: return null
        val fetchedAt = prefs.getLong(KEY_FETCHED_AT, 0L)
        if (fetchedAt <= 0L) return null
        return runCatching {
            parseCached(forecast, prefs.getString(KEY_AIR, null), location, unit, fetchedAt)
        }.getOrNull()
    }

    private fun parseCached(
        forecastBody: String,
        airBody: String?,
        location: GeoLocation,
        unit: TemperatureUnit,
        fetchedAt: Long,
    ): WeatherSnapshot {
        val root = JSONObject(forecastBody)
        val zone = runCatching {
            ZoneId.of(root.optString("timezone", location.timezone ?: "UTC"))
        }.getOrElse { ZoneOffset.UTC }
        val current = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        val dates = daily.getJSONArray("time")
        val days = buildList {
            for (index in 0 until dates.length()) {
                add(
                    WeatherDay(
                        localDate = dates.optString(index),
                        weatherCode = daily.getJSONArray("weather_code").optInt(index),
                        temperatureMax = daily.getJSONArray("temperature_2m_max").optDouble(index),
                        temperatureMin = daily.getJSONArray("temperature_2m_min").optDouble(index),
                        sunriseEpochMillis = daily.optJSONArray("sunrise")
                            ?.optString(index)?.let { LocalDateTime.parse(it).atZone(zone).toInstant().toEpochMilli() },
                        sunsetEpochMillis = daily.optJSONArray("sunset")
                            ?.optString(index)?.let { LocalDateTime.parse(it).atZone(zone).toInstant().toEpochMilli() },
                        precipitationProbabilityMaxPercent = null,
                    ),
                )
            }
        }
        return WeatherSnapshot(
            location = location,
            unit = unit,
            current = WeatherCurrent(
                temperature = current.getDouble("temperature_2m"),
                apparentTemperature = current.getDouble("apparent_temperature"),
                weatherCode = current.getInt("weather_code"),
                isDay = current.optInt("is_day", 1) == 1,
                observedAtEpochMillis = parseIso(current.optString("time"), zone) ?: fetchedAt,
            ),
            forecast = days,
            airQuality = airBody?.let {
                val air = JSONObject(it).optJSONObject("current")
                WeatherAirQuality(
                    usAqi = air?.optInt("us_aqi"),
                    observedAtEpochMillis = air?.optString("time")?.let { time ->
                        parseIso(time, zone)
                    },
                )
            },
            fetchedAtEpochMillis = fetchedAt,
        )
    }

    private fun parseIso(value: String, zone: ZoneId): Long? =
        runCatching { LocalDateTime.parse(value).atZone(zone).toInstant().toEpochMilli() }
            .getOrNull()

    private fun encodeLocation(location: GeoLocation): String = JSONObject().apply {
        put("name", location.name)
        put("country", location.country)
        put("admin1", location.admin1)
        put("latitude", location.latitude)
        put("longitude", location.longitude)
        put("timezone", location.timezone)
    }.toString()

    private fun decodeLocation(value: String?): GeoLocation? {
        if (value == null) return null
        return runCatching {
            val json = JSONObject(value)
            GeoLocation(
                name = json.optString("name", "Unknown"),
                country = json.optString("country").ifBlank { null },
                admin1 = json.optString("admin1").ifBlank { null },
                latitude = json.getDouble("latitude"),
                longitude = json.getDouble("longitude"),
                timezone = json.optString("timezone").ifBlank { null },
            )
        }.getOrNull()
    }
}
