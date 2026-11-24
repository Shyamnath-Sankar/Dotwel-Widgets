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
