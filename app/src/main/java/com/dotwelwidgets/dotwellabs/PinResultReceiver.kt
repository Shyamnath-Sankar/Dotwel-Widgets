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
            PIN_KIND_KWGT_ANALOG_5 -> R.string.pin_kwgt_analog5_confirmed
            PIN_KIND_KWGT_MINI_CALENDAR -> R.string.pin_kwgt_mini_calendar_confirmed
            PIN_KIND_KWGT_CLEAN_INFO -> R.string.pin_kwgt_clean_info_confirmed
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
        const val PIN_KIND_KWGT_ANALOG_5 = "kwgt_analog_5"
        const val PIN_KIND_KWGT_MINI_CALENDAR = "kwgt_mini_calendar"
        const val PIN_KIND_KWGT_CLEAN_INFO = "kwgt_clean_info"
    }
}
