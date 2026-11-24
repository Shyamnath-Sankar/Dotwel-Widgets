package com.dotwelwidgets.dotwellabs.weather

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Private weather preference state. Weather is opt-in and starts disabled.
 *
 * The selected city only ever comes from a manual Open-Meteo geocoding search
 * triggered by the user. There is no default city, no startup lookup and no
 * device-location or GPS permission. The backing SharedPreferences file is
 * excluded from Android cloud backup and device transfer.
 */
object WeatherSettings {
    internal const val PREFS = "dotwel_weather"
    private const val KEY_ENABLED = "weather_enabled"
    private const val KEY_LOCATION = "weather_location"
    private const val KEY_UNIT = "weather_unit"
    private const val KEY_LAST_AUTO = "weather_last_auto_attempt"
    private const val KEY_FAILURE_LOCATION = "weather_failure_location"
    private const val KEY_FAILURE_REASON = "weather_failure_reason"
    private const val KEY_FAILURE_AT = "weather_failure_at"

    /**
     * Shared transaction lock for consent, selected city, unit, generation and
     * cache commit/clear. No network I/O may run while this lock is held.
     */
    internal val STATE_LOCK = Any()

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun location(context: Context): GeoLocation? =
        decodeLocation(prefs(context).getString(KEY_LOCATION, null))

    fun setLocation(context: Context, location: GeoLocation) {
        prefs(context).edit().putString(KEY_LOCATION, encodeLocation(location)).apply()
    }

    fun clearLocation(context: Context) {
        prefs(context).edit().remove(KEY_LOCATION).apply()
    }

    fun unit(context: Context): TemperatureUnit = runCatching {
        TemperatureUnit.valueOf(prefs(context).getString(KEY_UNIT, null) ?: "")
    }.getOrDefault(TemperatureUnit.CELSIUS)

    fun setUnit(context: Context, unit: TemperatureUnit) {
        prefs(context).edit().putString(KEY_UNIT, unit.name).apply()
    }

    fun clearUnit(context: Context) {
        prefs(context).edit().remove(KEY_UNIT).apply()
    }

    fun recordFailure(
        context: Context,
        location: GeoLocation,
        reason: WeatherResult.Reason,
        atMillis: Long,
    ) {
        prefs(context).edit()
            .putString(KEY_FAILURE_LOCATION, encodeLocation(location))
            .putString(KEY_FAILURE_REASON, reason.name)
            .putLong(KEY_FAILURE_AT, atMillis)
            .apply()
    }

    fun clearFailure(context: Context) {
        prefs(context).edit()
            .remove(KEY_FAILURE_LOCATION)
            .remove(KEY_FAILURE_REASON)
            .remove(KEY_FAILURE_AT)
            .apply()
    }

    fun lastFailure(context: Context): WeatherFailure? {
        val selected = location(context) ?: return null
        val prefs = prefs(context)
        val failedLocation = decodeLocation(prefs.getString(KEY_FAILURE_LOCATION, null))
        if (failedLocation != selected) return null
        val reason = runCatching {
            WeatherResult.Reason.valueOf(prefs.getString(KEY_FAILURE_REASON, null) ?: "")
        }.getOrNull() ?: return null
        val at = prefs.getLong(KEY_FAILURE_AT, 0L)
        if (at <= 0L) return null
        return WeatherFailure(failedLocation, reason, at)
    }

    fun lastAutoAttempt(context: Context): Long =
        prefs(context).getLong(KEY_LAST_AUTO, 0L)

    fun markAutoAttempt(context: Context, now: Long) {
        prefs(context).edit().putLong(KEY_LAST_AUTO, now).apply()
    }

    internal fun encodeLocation(location: GeoLocation): String = JSONObject().apply {
        put("name", location.name)
        put("country", location.country)
        put("admin1", location.admin1)
        put("latitude", location.latitude)
        put("longitude", location.longitude)
        put("timezone", location.timezone)
    }.toString()

    internal fun decodeLocation(value: String?): GeoLocation? {
        if (value == null) return null
        return runCatching {
            val json = JSONObject(value)
            val latitude = json.optDouble("latitude", Double.NaN)
            val longitude = json.optDouble("longitude", Double.NaN)
            if (!latitude.isFinite() || !longitude.isFinite()) return null
            GeoLocation(
                name = json.optString("name", "Unknown"),
                country = json.optString("country").ifBlank { null },
                admin1 = json.optString("admin1").ifBlank { null },
                latitude = latitude,
                longitude = longitude,
                timezone = json.optString("timezone").ifBlank { null },
            )
        }.getOrNull()
    }
}
