package com.example.model

import com.example.ui.theme.AppSurfaceMode

enum class WidgetCategory(val displayName: String, val shortCode: String) {
    NIGHTING("Nighting", "NGT"),
    CONTROLS("Controls & Buds", "CTL"),
    STUDIO("Photos & Quotes", "STD"),
    TIME("Time & Clocks", "CLK"),
    SYSTEM("Telemetry", "SYS"),
    FOCUS("Focus & Cal", "FCS"),
    WELLNESS("Habits & Life", "WLL")
}

enum class WidgetThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
    OLED_NIGHT("OLED Night"),
    AUTO_NIGHT("Auto-Night")
}

enum class WidgetGridSize(
    val label: String,
    val cols: Int,
    val rows: Int,
    val aspectRatio: Float,
    val canonicalWidthDp: Float,
    val canonicalHeightDp: Float
) {
    SMALL_2X2("2×2", 2, 2, 1.0f, 172f, 172f),
    WIDE_4X2("4×2", 4, 2, 2.0f, 344f, 172f),
    LARGE_4X4("4×4", 4, 4, 1.0f, 344f, 344f)
}

enum class DotDensity(val label: String, val stepDp: Float) {
    COMPACT("Compact", 8f),
    BALANCED("Balanced", 11f),
    EXPRESSIVE("Expressive", 15f)
}

enum class WidgetBorderStyle(val label: String) {
    HAIRLINE("Hairline"),
    DOT_FRAME("Dot Frame"),
    BORDERLESS("None")
}

enum class WidgetInteractiveAction {
    NONE,
    OPEN_WIFI_PANEL,
    OPEN_BLUETOOTH_SETTINGS,
    OPEN_INTERNET_PANEL,
    OPEN_AUDIO_PANEL,
    TOGGLE_MEDIA_PLAY_PAUSE,
    TOGGLE_POMODORO,
    INCREMENT_HYDRATION,
    TOGGLE_HABIT_TODAY,
    CYCLE_CHECKLIST,
    INCREMENT_TALLY,
    CYCLE_MOOD,
    NEXT_QUOTE,
    TOGGLE_STOPWATCH,
    INCREMENT_PAGE,
    TOGGLE_BREATHING
}

enum class WidgetCustomFieldType {
    NONE,
    PHOTO_PICKER,
    PHOTO_AND_QUOTE,
    CUSTOM_QUOTE,
    BUDS_LABEL,
    TIMEZONE,
    COUNTDOWN_EVENT,
    NOTE_TEXT,
    CHECKLIST_ITEMS,
    HABIT_GOAL,
    POMODORO_MINUTES,
    TALLY_LABEL,
    FOCUS_WORD,
    READING_BOOK,
    SLEEP_TARGET
}

data class WidgetSpec(
    val typeId: String,
    val number: Int,
    val name: String,
    val subtitle: String,
    val category: WidgetCategory,
    val defaultSize: WidgetGridSize,
    val isNightingFeatured: Boolean = false,
    val interactiveAction: WidgetInteractiveAction = WidgetInteractiveAction.NONE,
    val customFieldType: WidgetCustomFieldType = WidgetCustomFieldType.NONE,
    val defaultTitle: String = "",
    val defaultSecondaryText: String = "",
    val defaultGoal: Int = 8
)

data class WidgetConfig(
    val id: Long = 0L,
    val appWidgetId: Int = -1,
    val widgetTypeId: String,
    val customName: String,
    val themeMode: WidgetThemeMode = WidgetThemeMode.OLED_NIGHT,
    val gridSize: WidgetGridSize = WidgetGridSize.SMALL_2X2,
    val cornerRadiusDp: Int = 24,
    val backgroundOpacity: Float = 1.0f,
    val dotDensity: DotDensity = DotDensity.BALANCED,
    val borderStyle: WidgetBorderStyle = WidgetBorderStyle.HAIRLINE,
    val showRedAccent: Boolean = true,
    val showDotBackdrop: Boolean = true,
    val use24Hour: Boolean = true,
    val monochromePhoto: Boolean = true,
    val photoPath: String = "",
    val timezoneId: String = "Asia/Tokyo",
    val customTitle: String = "",
    val customNote: String = "",
    val targetEpochMillis: Long = 0L,
    val goalValue: Int = 8,
    val currentValue: Int = 0,
    val bitmaskState: Int = 0,
    val isTimerRunning: Boolean = false,
    val timerStartedAtMillis: Long = 0L,
    val timerDurationSec: Int = 1500,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class DeviceTelemetry(
    val batteryPercent: Int = 84,
    val isCharging: Boolean = false,
    val batteryTempC: Float = 31.4f,
    val batteryVoltageMv: Int = 4120,
    val batteryHealthLabel: String = "GOOD",
    val storageUsedGb: Float = 42.6f,
    val storageTotalGb: Float = 128f,
    val ramUsedGb: Float = 3.8f,
    val ramTotalGb: Float = 8.0f,
    val wifiEnabled: Boolean = true,
    val wifiConnected: Boolean = true,
    val cellularConnected: Boolean = false,
    val bluetoothEnabled: Boolean = true,
    val budsConnected: Boolean = false,
    val budsDeviceName: String = "EAR (BUDS)",
    val audioOutputLabel: String = "SPEAKER",
    val isMusicPlaying: Boolean = false,
    val mediaVolumePercent: Int = 60,
    val networkType: String = "WI-FI",
    val isConnected: Boolean = true,
    val isMetered: Boolean = false,
    val uptimeMillis: Long = 0L,
    val isPowerSaveMode: Boolean = false
) {
    val storagePercent: Float get() = if (storageTotalGb > 0f) (storageUsedGb / storageTotalGb).coerceIn(0f, 1f) else 0f
    val ramPercent: Float get() = if (ramTotalGb > 0f) (ramUsedGb / ramTotalGb).coerceIn(0f, 1f) else 0f
}

data class LunarInfo(
    val phaseFraction: Float,
    val illuminationPercent: Int,
    val phaseName: String,
    val lunarDay: Float
)

data class AppSettings(
    val appSurfaceMode: AppSurfaceMode = AppSurfaceMode.OLED_NIGHT,
    val autoNightStartHour: Int = 21,
    val autoNightEndHour: Int = 6,
    val use24Hour: Boolean = true,
    val refreshIntervalMinutes: Int = 60,
    val ultraEcoMode: Boolean = true,
    val hasCompletedOnboarding: Boolean = false
)

data class LiveSnapshot(
    val nowMillis: Long = System.currentTimeMillis(),
    val telemetry: DeviceTelemetry = DeviceTelemetry(),
    val lunar: LunarInfo = LunarInfo(0.38f, 76, "WAXING GIBBOUS", 11.2f),
    val settings: AppSettings = AppSettings()
)
