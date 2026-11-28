package com.example.data.repository

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.KeyEvent
import com.example.data.local.AppPreferences
import com.example.data.local.DotGridDatabase
import com.example.data.local.WidgetInstanceEntity
import com.example.data.local.WidgetPhotoStorage
import com.example.data.telemetry.AstronomyCalculator
import com.example.data.telemetry.DeviceTelemetryProvider
import com.example.data.worker.WidgetSyncWorker
import com.example.model.AppSettings
import com.example.model.DeviceTelemetry
import com.example.model.LiveSnapshot
import com.example.model.WidgetCatalog
import com.example.model.WidgetConfig
import com.example.model.WidgetInteractiveAction
import com.example.ui.theme.AppSurfaceMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DotGridRepository(private val context: Context) {

    private val database = DotGridDatabase.getInstance(context)
    private val widgetDao = database.widgetInstanceDao()
    val preferences = AppPreferences(context)
    private val telemetryProvider = DeviceTelemetryProvider(context)

    val settingsFlow: Flow<AppSettings> = preferences.settingsFlow

    val savedWidgetsFlow: Flow<List<WidgetConfig>> = widgetDao.observeAll().map { list ->
        list.map { it.toModel() }
    }

    suspend fun getSettingsOnce(): AppSettings = preferences.getSettingsOnce()

    suspend fun getWidgetById(id: Long): WidgetConfig? = widgetDao.getById(id)?.toModel()

    suspend fun getWidgetByAppWidgetId(appWidgetId: Int): WidgetConfig? =
        widgetDao.getByAppWidgetId(appWidgetId)?.toModel()

    suspend fun saveWidgetConfig(config: WidgetConfig): Long {
        val updated = config.copy(updatedAtMillis = System.currentTimeMillis())
        return widgetDao.upsert(WidgetInstanceEntity.fromModel(updated))
    }

    suspend fun bindPresetToAppWidgetId(presetId: Long, appWidgetId: Int) {
        widgetDao.bindAppWidgetId(presetId, appWidgetId)
    }

    suspend fun unbindAppWidgetId(appWidgetId: Int) {
        widgetDao.unbindAppWidgetId(appWidgetId)
    }

    suspend fun deleteWidget(id: Long) {
        widgetDao.deleteById(id)
    }

    suspend fun duplicateWidget(config: WidgetConfig): Long {
        val copy = config.copy(
            id = 0L,
            appWidgetId = -1,
            customName = "${config.customName} Copy",
            updatedAtMillis = System.currentTimeMillis()
        )
        return widgetDao.upsert(WidgetInstanceEntity.fromModel(copy))
    }

    fun readDeviceTelemetry(): DeviceTelemetry = telemetryProvider.readSnapshot()

    suspend fun buildCurrentLiveSnapshot(): LiveSnapshot {
        val now = System.currentTimeMillis()
        val settings = preferences.getSettingsOnce()
        return LiveSnapshot(
            nowMillis = now,
            telemetry = readDeviceTelemetry(),
            lunar = AstronomyCalculator.calculateLunarInfo(now),
            settings = settings
        )
    }

    suspend fun importStudioPhoto(uri: Uri): Result<String> =
        WidgetPhotoStorage.importFromPhotoPickerUri(context, uri)

    fun dispatchMediaPlayPause() {
        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        }
    }

    suspend fun seedStarterWidgetsIfEmpty() {
        val existing = widgetDao.getAll()
        if (existing.isEmpty()) {
            val starters = listOf(
                "night_bedside_clock",
                "ctl_quick_trio",
                "ctl_buds_card",
                "studio_photo_mono",
                "studio_quote_stoic",
                "sys_trio_dashboard"
            )
            starters.forEach { typeId ->
                val spec = WidgetCatalog.getSpec(typeId)
                val cfg = WidgetCatalog.createDefaultConfig(spec)
                widgetDao.upsert(WidgetInstanceEntity.fromModel(cfg))
            }
        }
    }

    suspend fun setAppSurfaceMode(mode: AppSurfaceMode) {
        preferences.setSurfaceMode(mode)
    }

    suspend fun setAutoNightHours(start: Int, end: Int) {
        preferences.setAutoNightHours(start, end)
    }

    suspend fun setUse24Hour(use24: Boolean) {
        preferences.setUse24Hour(use24)
    }

    suspend fun setRefreshIntervalMinutes(minutes: Int) {
        preferences.setRefreshIntervalMinutes(minutes)
        WidgetSyncWorker.schedule(context, minutes)
    }

    suspend fun setUltraEcoMode(enabled: Boolean) {
        preferences.setUltraEcoMode(enabled)
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        preferences.setOnboardingCompleted(completed)
    }

    fun applyInteractiveTap(config: WidgetConfig): WidgetConfig {
        val spec = WidgetCatalog.getSpec(config.widgetTypeId)
        val now = System.currentTimeMillis()
        return when (spec.interactiveAction) {
            WidgetInteractiveAction.TOGGLE_POMODORO -> {
                if (config.isTimerRunning) {
                    val elapsedSec = ((now - config.timerStartedAtMillis) / 1000L).toInt().coerceAtLeast(0)
                    val remaining = (config.timerDurationSec - elapsedSec).coerceAtLeast(0)
                    config.copy(
                        isTimerRunning = false,
                        timerDurationSec = if (remaining <= 0) config.goalValue.coerceAtLeast(5) * 60 else remaining,
                        updatedAtMillis = now
                    )
                } else {
                    val dur = if (config.timerDurationSec <= 5) config.goalValue.coerceAtLeast(5) * 60 else config.timerDurationSec
                    config.copy(
                        isTimerRunning = true,
                        timerStartedAtMillis = now,
                        timerDurationSec = dur,
                        updatedAtMillis = now
                    )
                }
            }
            WidgetInteractiveAction.TOGGLE_STOPWATCH -> {
                if (config.isTimerRunning) {
                    val elapsedSec = ((now - config.timerStartedAtMillis) / 1000L).toInt().coerceAtLeast(0)
                    config.copy(
                        isTimerRunning = false,
                        currentValue = config.currentValue + elapsedSec,
                        updatedAtMillis = now
                    )
                } else {
                    config.copy(
                        isTimerRunning = true,
                        timerStartedAtMillis = now,
                        updatedAtMillis = now
                    )
                }
            }
            WidgetInteractiveAction.INCREMENT_HYDRATION -> {
                val maxGoal = config.goalValue.coerceIn(4, 16)
                val next = if (config.currentValue >= maxGoal) 0 else config.currentValue + 1
                config.copy(currentValue = next, updatedAtMillis = now)
            }
            WidgetInteractiveAction.TOGGLE_HABIT_TODAY -> {
                val toggled = config.bitmaskState xor (1 shl 6)
                config.copy(bitmaskState = toggled, updatedAtMillis = now)
            }
            WidgetInteractiveAction.CYCLE_CHECKLIST -> {
                val nextMask = when (config.bitmaskState and 0b111) {
                    0b000 -> 0b001
                    0b001 -> 0b011
                    0b011 -> 0b111
                    else -> 0b000
                }
                config.copy(bitmaskState = nextMask, updatedAtMillis = now)
            }
            WidgetInteractiveAction.INCREMENT_TALLY -> {
                config.copy(currentValue = config.currentValue + 1, updatedAtMillis = now)
            }
            WidgetInteractiveAction.CYCLE_MOOD -> {
                val next = (config.currentValue % 5) + 1
                config.copy(currentValue = next, updatedAtMillis = now)
            }
            WidgetInteractiveAction.NEXT_QUOTE -> {
                config.copy(currentValue = config.currentValue + 1, updatedAtMillis = now)
            }
            WidgetInteractiveAction.INCREMENT_PAGE -> {
                val total = config.goalValue.coerceAtLeast(50)
                val next = if (config.currentValue >= total) 0 else (config.currentValue + 10).coerceAtMost(total)
                config.copy(currentValue = next, updatedAtMillis = now)
            }
            WidgetInteractiveAction.TOGGLE_BREATHING -> {
                config.copy(isTimerRunning = !config.isTimerRunning, timerStartedAtMillis = now, updatedAtMillis = now)
            }
            WidgetInteractiveAction.TOGGLE_MEDIA_PLAY_PAUSE -> {
                dispatchMediaPlayPause()
                config.copy(updatedAtMillis = now)
            }
            else -> config
        }
    }
}
