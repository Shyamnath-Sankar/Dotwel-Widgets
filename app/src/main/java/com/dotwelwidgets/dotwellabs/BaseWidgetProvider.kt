package com.dotwelwidgets.dotwellabs

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.RemoteViews

abstract class BaseWidgetProvider : AppWidgetProvider() {
    protected abstract val layoutRes: Int
    protected open val tapAction: String? = null
    protected open val tapCategory: String? = null

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, buildViews(context, appWidgetId))
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        appWidgetManager.updateAppWidget(appWidgetId, buildViews(context, appWidgetId))
    }

    private fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val views = RemoteViews(context.packageName, layoutRes)
        views.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            WidgetPrefs.style(context).backgroundRes,
        )
        tapIntent(context)?.let { intent ->
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        }
        return views
    }

    private fun tapIntent(context: Context): Intent? {
        tapCategory?.let { category ->
            return Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        tapAction?.let { action ->
            return Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return null
    }
}

class ClockWidgetProvider : BaseWidgetProvider() {
    override val layoutRes: Int = R.layout.widget_clock
    override val tapAction: String = AlarmClock.ACTION_SHOW_ALARMS
}

class DateWidgetProvider : BaseWidgetProvider() {
    override val layoutRes: Int = R.layout.widget_date
    override val tapCategory: String = Intent.CATEGORY_APP_CALENDAR
}
