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
        BluetoothStatusWidgetProvider::class.java,
        WifiStatusWidgetProvider::class.java,
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
        NtAwayWidgetProvider::class.java,
        NtPhotoCircleProvider::class.java,
        NtPhotoRectProvider::class.java,
        NtWxCircleProvider::class.java,
        NtWxSquareProvider::class.java,
        NtWxRectProvider::class.java,
        NtWxComboProvider::class.java,
        NtWxSunsetProvider::class.java,
        NtWxAirProvider::class.java,
        NtWxW1TempProvider::class.java,
        NtWxW1StatusProvider::class.java,
        NtWxW1RainProvider::class.java,
        NothingTempWidgetProvider::class.java,
        NothingSunInfoWidgetProvider::class.java,
        KwgtDigital2WidgetProvider::class.java,
        KwgtDateTime3WidgetProvider::class.java,
        KwgtInterstellarEnduranceProvider::class.java,
        KwgtInterstellarEnduranceAnalogProvider::class.java,
        KwgtInterstellarEnduranceAnalog2Provider::class.java,
        Material3ExpandedMediaProvider::class.java,
        Material3MediaOutputProvider::class.java,
        MiniMediaPlayerProvider::class.java,
        MinimalMediaPlayerProvider::class.java,
        MinimalMediaPlayer2Provider::class.java,
        MinimalMediaPlayer3Provider::class.java,
        NothingMasterWidgetProvider::class.java,
        NothingMediaPlayerProvider::class.java,
        NothingMediaPlayer2Provider::class.java,
        NothingMediaPlayer3Provider::class.java,
        UniversalMediaPlayerProvider::class.java,
        MaterialNotificationWidgetProvider::class.java,
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
        updateProvider(context, manager, NtWxCircleProvider::class.java)
        updateProvider(context, manager, NtWxSquareProvider::class.java)
        updateProvider(context, manager, NtWxRectProvider::class.java)
        updateProvider(context, manager, NtWxComboProvider::class.java)
        updateProvider(context, manager, NtWxSunsetProvider::class.java)
        updateProvider(context, manager, NtWxAirProvider::class.java)
        updateProvider(context, manager, NtWxW1TempProvider::class.java)
        updateProvider(context, manager, NtWxW1StatusProvider::class.java)
        updateProvider(context, manager, NtWxW1RainProvider::class.java)
        updateProvider(context, manager, NothingTempWidgetProvider::class.java)
        updateProvider(context, manager, NothingSunInfoWidgetProvider::class.java)
        updateProvider(context, manager, NothingMasterWidgetProvider::class.java)
        updateProvider(context, manager, KwgtDateTime3WidgetProvider::class.java)
    }

    fun updateMediaProviders(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        listOf(
            Material3ExpandedMediaProvider::class.java,
            Material3MediaOutputProvider::class.java,
            MiniMediaPlayerProvider::class.java,
            MinimalMediaPlayerProvider::class.java,
            MinimalMediaPlayer2Provider::class.java,
            MinimalMediaPlayer3Provider::class.java,
            NothingMasterWidgetProvider::class.java,
            NothingMediaPlayerProvider::class.java,
            NothingMediaPlayer2Provider::class.java,
            NothingMediaPlayer3Provider::class.java,
            UniversalMediaPlayerProvider::class.java,
            MaterialNotificationWidgetProvider::class.java,
        ).forEach { updateProvider(context, manager, it) }
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
