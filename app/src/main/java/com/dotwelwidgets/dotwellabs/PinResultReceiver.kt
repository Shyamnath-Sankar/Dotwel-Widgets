package com.dotwelwidgets.dotwellabs

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class PinResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PIN_RESULT) return
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val kind = intent.getStringExtra(EXTRA_PIN_KIND)
        if (kind == PIN_KIND_WORLD) {
            val token = intent.getStringExtra(EXTRA_PIN_TOKEN)
            if (token != null) {
                WorldPrefs.takePending(context, token)?.let { zones ->
                    WorldPrefs.setZones(context, appWidgetId, zones)
                }
            }
        }
        WidgetUpdater.updateAll(context)
        val message = when (kind) {
            PIN_KIND_CLOCK -> R.string.pin_clock_confirmed
            PIN_KIND_DATE -> R.string.pin_date_confirmed
            PIN_KIND_ANALOG_BOLD -> R.string.pin_analog_bold_confirmed
            PIN_KIND_ANALOG_SCALE -> R.string.pin_analog_scale_confirmed
            PIN_KIND_WORLD -> R.string.pin_world_confirmed
            PIN_KIND_QUOTES -> R.string.pin_quotes_confirmed
            PIN_KIND_BATTERY -> R.string.pin_battery_confirmed
            PIN_KIND_BATTERY_MINI -> R.string.pin_battery_mini_confirmed
            PIN_KIND_KWGT_BATTERY_2 -> R.string.pin_kwgt_battery2_confirmed
            PIN_KIND_KWGT_DATE_TIME -> R.string.pin_kwgt_datetime_confirmed
            PIN_KIND_KWGT_DATE_TIME_2 -> R.string.pin_kwgt_datetime2_confirmed
            PIN_KIND_KWGT_DIGITAL_4 -> R.string.pin_kwgt_digital4_confirmed
            PIN_KIND_KWGT_DIGITAL_3 -> R.string.pin_kwgt_digital3_confirmed
            PIN_KIND_KWGT_GRADIENT_CLOCK -> R.string.pin_kwgt_gradient_confirmed
            PIN_KIND_KWGT_ANALOG_3 -> R.string.pin_kwgt_analog3_confirmed
            PIN_KIND_KWGT_ANALOG_2 -> R.string.pin_kwgt_analog2_confirmed
            PIN_KIND_KWGT_FUTURE -> R.string.pin_kwgt_future_confirmed
            PIN_KIND_KWGT_ANALOG_SECONDS -> R.string.pin_kwgt_analog_seconds_confirmed
            PIN_KIND_KWGT_ANALOG_5 -> R.string.pin_kwgt_analog5_confirmed
            PIN_KIND_KWGT_ANALOG_5_CLEAR -> R.string.pin_kwgt_analog5clear_confirmed
            PIN_KIND_KWGT_ORBIT -> R.string.pin_kwgt_orbit_confirmed
            PIN_KIND_KWGT_MINI_CALENDAR -> R.string.pin_kwgt_mini_calendar_confirmed
            PIN_KIND_KWGT_CLEAN_INFO -> R.string.pin_kwgt_clean_info_confirmed
            PIN_KIND_KWGT_IOS_BATTERY -> R.string.pin_kwgt_ios_battery_confirmed
            PIN_KIND_KWGT_DIGITAL_2 -> R.string.pin_kwgt_digital2_confirmed
            PIN_KIND_KWGT_DATE_TIME_3 -> R.string.pin_kwgt_date_time3_confirmed
            PIN_KIND_KWGT_INTERSTELLAR_ENDURANCE ->
                R.string.pin_kwgt_interstellar_endurance_confirmed
            PIN_KIND_KWGT_INTERSTELLAR_ANALOG ->
                R.string.pin_kwgt_interstellar_analog_confirmed
            PIN_KIND_KWGT_INTERSTELLAR_ANALOG_2 ->
                R.string.pin_kwgt_interstellar_analog2_confirmed
            PIN_KIND_WEATHER_TEMP -> R.string.pin_weather_temp_confirmed
            PIN_KIND_WEATHER_SUN -> R.string.pin_weather_sun_confirmed
            PIN_KIND_BT_STATUS -> R.string.pin_bt_status_confirmed
            PIN_KIND_WIFI_STATUS -> R.string.pin_wifi_status_confirmed
            PIN_KIND_CELLULAR_STATUS -> R.string.pin_cellular_status_confirmed
            PIN_KIND_BATTERY_STATUS -> R.string.pin_battery_status_confirmed
            PIN_KIND_BT_HEADPHONES_WIDE -> R.string.pin_bt_headphones_wide_confirmed
            PIN_KIND_BT_HEADPHONES_SQUARE -> R.string.pin_bt_headphones_square_confirmed
            PIN_KIND_MEDIA_EXPANDED -> R.string.pin_media_expanded_confirmed
            PIN_KIND_MEDIA_OUTPUT -> R.string.pin_media_output_confirmed
            PIN_KIND_MEDIA_MINI -> R.string.pin_media_mini_confirmed
            PIN_KIND_MEDIA_MINIMAL -> R.string.pin_media_minimal_confirmed
            PIN_KIND_MEDIA_MINIMAL2 -> R.string.pin_media_minimal2_confirmed
            PIN_KIND_MEDIA_MINIMAL3 -> R.string.pin_media_minimal3_confirmed
            PIN_KIND_MEDIA_MASTER -> R.string.pin_media_master_confirmed
            PIN_KIND_MEDIA_PLAYER -> R.string.pin_media_player_confirmed
            PIN_KIND_MEDIA_PLAYER2 -> R.string.pin_media_player2_confirmed
            PIN_KIND_MEDIA_PLAYER3 -> R.string.pin_media_player3_confirmed
            PIN_KIND_MEDIA_UNIVERSAL -> R.string.pin_media_universal_confirmed
            PIN_KIND_MEDIA_NOTIFICATION -> R.string.pin_media_notification_confirmed
            else -> R.string.pin_widget_confirmed
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val ACTION_PIN_RESULT =
            "com.dotwelwidgets.dotwellabs.action.PIN_RESULT"
        const val EXTRA_PIN_KIND =
            "com.dotwelwidgets.dotwellabs.extra.PIN_KIND"
        const val EXTRA_PIN_TOKEN =
            "com.dotwelwidgets.dotwellabs.extra.PIN_TOKEN"
        const val PIN_KIND_CLOCK = "clock"
        const val PIN_KIND_DATE = "date"
        const val PIN_KIND_ANALOG_BOLD = "analog_bold"
        const val PIN_KIND_ANALOG_SCALE = "analog_scale"
        const val PIN_KIND_WORLD = "world"
        const val PIN_KIND_QUOTES = "quotes"
        const val PIN_KIND_BATTERY = "battery"
        const val PIN_KIND_BATTERY_MINI = "battery_mini"
        const val PIN_KIND_KWGT_BATTERY_2 = "kwgt_battery_2"
        const val PIN_KIND_KWGT_DATE_TIME = "kwgt_date_time"
        const val PIN_KIND_KWGT_DATE_TIME_2 = "kwgt_date_time_2"
        const val PIN_KIND_KWGT_DIGITAL_4 = "kwgt_digital_4"
        const val PIN_KIND_KWGT_DIGITAL_3 = "kwgt_digital_3"
        const val PIN_KIND_KWGT_GRADIENT_CLOCK = "kwgt_gradient_clock"
        const val PIN_KIND_KWGT_ANALOG_3 = "kwgt_analog_3"
        const val PIN_KIND_KWGT_ANALOG_2 = "kwgt_analog_2"
        const val PIN_KIND_KWGT_FUTURE = "kwgt_future"
        const val PIN_KIND_KWGT_ANALOG_SECONDS = "kwgt_analog_seconds"
        const val PIN_KIND_KWGT_ANALOG_5 = "kwgt_analog_5"
        const val PIN_KIND_KWGT_ANALOG_5_CLEAR = "kwgt_analog_5_clear"
        const val PIN_KIND_KWGT_ORBIT = "kwgt_orbit_clock"
        const val PIN_KIND_KWGT_MINI_CALENDAR = "kwgt_mini_calendar"
        const val PIN_KIND_KWGT_CLEAN_INFO = "kwgt_clean_info"
        const val PIN_KIND_KWGT_IOS_BATTERY = "kwgt_ios_battery"
        const val PIN_KIND_KWGT_DIGITAL_2 = "kwgt_digital_2"
        const val PIN_KIND_KWGT_DATE_TIME_3 = "kwgt_date_time_3"
        const val PIN_KIND_KWGT_INTERSTELLAR_ENDURANCE = "kwgt_interstellar_endurance"
        const val PIN_KIND_KWGT_INTERSTELLAR_ANALOG = "kwgt_interstellar_analog"
        const val PIN_KIND_KWGT_INTERSTELLAR_ANALOG_2 = "kwgt_interstellar_analog_2"
        const val PIN_KIND_WEATHER_TEMP = "weather_temp"
        const val PIN_KIND_WEATHER_SUN = "weather_sun"
        const val PIN_KIND_BT_STATUS = "bt_status"
        const val PIN_KIND_WIFI_STATUS = "wifi_status"
        const val PIN_KIND_CELLULAR_STATUS = "cellular_status"
        const val PIN_KIND_BATTERY_STATUS = "battery_status"
        const val PIN_KIND_BT_HEADPHONES_WIDE = "bt_headphones_wide"
        const val PIN_KIND_BT_HEADPHONES_SQUARE = "bt_headphones_square"
        const val PIN_KIND_MEDIA_EXPANDED = "media_expanded"
        const val PIN_KIND_MEDIA_OUTPUT = "media_output"
        const val PIN_KIND_MEDIA_MINI = "media_mini"
        const val PIN_KIND_MEDIA_MINIMAL = "media_minimal"
        const val PIN_KIND_MEDIA_MINIMAL2 = "media_minimal2"
        const val PIN_KIND_MEDIA_MINIMAL3 = "media_minimal3"
        const val PIN_KIND_MEDIA_MASTER = "media_master"
        const val PIN_KIND_MEDIA_PLAYER = "media_player"
        const val PIN_KIND_MEDIA_PLAYER2 = "media_player2"
        const val PIN_KIND_MEDIA_PLAYER3 = "media_player3"
        const val PIN_KIND_MEDIA_UNIVERSAL = "media_universal"
        const val PIN_KIND_MEDIA_NOTIFICATION = "media_notification"
    }
}
