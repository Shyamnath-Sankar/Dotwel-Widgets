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
        KwgtBattery2WidgetProvider::class.java,
        KwgtDateTimeWidgetProvider::class.java,
        KwgtDateTime2WidgetProvider::class.java,
        KwgtDigital4WidgetProvider::class.java,
        KwgtDigital3WidgetProvider::class.java,
        KwgtGradientClockWidgetProvider::class.java,
        KwgtAnalog2WidgetProvider::class.java,
        KwgtFutureWidgetProvider::class.java,
        KwgtAnalog3WidgetProvider::class.java,
        KwgtAnalogSecondsWidgetProvider::class.java,
        KwgtAnalog5WidgetProvider::class.java,
        KwgtAnalog5TransparentWidgetProvider::class.java,
        KwgtOrbitClockWidgetProvider::class.java,
        KwgtMiniCalendarWidgetProvider::class.java,
        KwgtCleanInfoWidgetProvider::class.java,
        IosBatteryWidgetProvider::class.java,
        NothingTempWidgetProvider::class.java,
        NothingSunInfoWidgetProvider::class.java,
    )

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        providers.forEach { provider ->
            updateProvider(context, manager, provider)
        }
        MinuteTickScheduler.schedule(context)
        BatteryRefreshScheduler.schedule(context)
    }

    fun updateWeatherProviders(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        updateProvider(context, manager, NothingTempWidgetProvider::class.java)
        updateProvider(context, manager, NothingSunInfoWidgetProvider::class.java)
    }

    fun updateBatteryProviders(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        BatteryRefreshScheduler.providers.forEach { provider ->
            updateProvider(context, manager, provider)
        }
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
