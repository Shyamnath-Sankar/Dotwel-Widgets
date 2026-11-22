package com.dotwelwidgets.dotwellabs

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object WidgetUpdater {
    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        updateProvider(context, manager, ClockWidgetProvider::class.java)
        updateProvider(context, manager, DateWidgetProvider::class.java)
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
