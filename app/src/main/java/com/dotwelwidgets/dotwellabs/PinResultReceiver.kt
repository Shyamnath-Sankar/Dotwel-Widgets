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
        val message = when (intent.getStringExtra(EXTRA_PIN_KIND)) {
            PIN_KIND_CLOCK -> R.string.pin_clock_confirmed
            PIN_KIND_DATE -> R.string.pin_date_confirmed
            else -> R.string.pin_widget_confirmed
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        const val ACTION_PIN_RESULT =
            "com.dotwelwidgets.dotwellabs.action.PIN_RESULT"
        const val EXTRA_PIN_KIND =
            "com.dotwelwidgets.dotwellabs.extra.PIN_KIND"
        const val PIN_KIND_CLOCK = "clock"
        const val PIN_KIND_DATE = "date"
    }
}
