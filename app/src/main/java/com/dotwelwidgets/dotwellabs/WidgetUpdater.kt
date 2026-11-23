package com.dotwelwidgets.dotwellabs

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object WidgetUpdater {
    val providers: List<Class<out BaseWidgetProvider>> = listOf(
        ClockWidgetProvider::class.java,
        DateWidgetProvider::class.java,
        AnalogBoldWidgetProvider::class.java,
        AnalogScaleWidgetProvider::class.java,
        WorldWidgetProvider::class.java,
        QuotesWidgetProvider::class.java,
        BatteryWidgetProvider::class.java,
        BatteryMiniWidgetProvider::class.java,
        KwgtDateTimeWidgetProvider::class.java,
        KwgtDateTime2WidgetProvider::class.java,
        KwgtMiniCalendarWidgetProvider::class.java,
    )

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        providers.forEach { provider ->
            updateProvider(context, manager, provider)
        }
        MinuteTickScheduler.schedule(context)
    }

    private fun updateProvider(
        context: Context,
        manager: AppWidgetManager,
        provider: Class<out BaseWidgetProvider>,
    ) {
        val ids = manager.getAppWidgetIds(ComponentName(context, provider))
        if (ids.isEmpty()) return
        val intent = Intent(context, provider).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }
}

class MinuteTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != MinuteTickScheduler.ACTION_TICK) return
        WidgetUpdater.updateAll(context)
    }
}
