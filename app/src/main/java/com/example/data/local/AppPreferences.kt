package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppSettings
import com.example.ui.theme.AppSurfaceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.anyWidgetsDataStore: DataStore<Preferences> by preferencesDataStore(name = "anywidgets_prefs")

class AppPreferences(private val context: Context) {

    private object Keys {
        val SURFACE_MODE = stringPreferencesKey("surface_mode")
        val AUTO_NIGHT_START = intPreferencesKey("auto_night_start")
        val AUTO_NIGHT_END = intPreferencesKey("auto_night_end")
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        val REFRESH_INTERVAL_MIN = intPreferencesKey("refresh_interval_min")
        val ULTRA_ECO_MODE = booleanPreferencesKey("ultra_eco_mode")
        val PENDING_PIN_PRESET_ID = longPreferencesKey("pending_pin_preset_id")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val settingsFlow: Flow<AppSettings> = context.anyWidgetsDataStore.data.map { prefs ->
        AppSettings(
            appSurfaceMode = prefs[Keys.SURFACE_MODE]
                ?.let { runCatching { AppSurfaceMode.valueOf(it) }.getOrNull() }
                ?: AppSurfaceMode.OLED_NIGHT,
            autoNightStartHour = prefs[Keys.AUTO_NIGHT_START] ?: 21,
            autoNightEndHour = prefs[Keys.AUTO_NIGHT_END] ?: 6,
            use24Hour = prefs[Keys.USE_24_HOUR] ?: true,
            refreshIntervalMinutes = prefs[Keys.REFRESH_INTERVAL_MIN] ?: 60,
            ultraEcoMode = prefs[Keys.ULTRA_ECO_MODE] ?: true,
            hasCompletedOnboarding = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun getSettingsOnce(): AppSettings = settingsFlow.first()

    suspend fun setSurfaceMode(mode: AppSurfaceMode) {
        context.anyWidgetsDataStore.edit { it[Keys.SURFACE_MODE] = mode.name }
    }

    suspend fun setAutoNightHours(startHour: Int, endHour: Int) {
        context.anyWidgetsDataStore.edit {
            it[Keys.AUTO_NIGHT_START] = startHour.coerceIn(17, 23)
            it[Keys.AUTO_NIGHT_END] = endHour.coerceIn(4, 10)
        }
    }

    suspend fun setUse24Hour(value: Boolean) {
        context.anyWidgetsDataStore.edit { it[Keys.USE_24_HOUR] = value }
    }

    suspend fun setRefreshIntervalMinutes(minutes: Int) {
        context.anyWidgetsDataStore.edit { it[Keys.REFRESH_INTERVAL_MIN] = minutes }
    }

    suspend fun setUltraEcoMode(enabled: Boolean) {
        context.anyWidgetsDataStore.edit { it[Keys.ULTRA_ECO_MODE] = enabled }
    }

    suspend fun setPendingPinPresetId(id: Long) {
        context.anyWidgetsDataStore.edit { it[Keys.PENDING_PIN_PRESET_ID] = id }
    }

    suspend fun consumePendingPinPresetId(): Long {
        var result = -1L
        context.anyWidgetsDataStore.edit { prefs ->
            result = prefs[Keys.PENDING_PIN_PRESET_ID] ?: -1L
            prefs.remove(Keys.PENDING_PIN_PRESET_ID)
        }
        return result
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.anyWidgetsDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }
}
