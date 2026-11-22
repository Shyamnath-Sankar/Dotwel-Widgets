package com.dotwelwidgets.dotwellabs

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import java.util.Calendar

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

    protected fun refreshWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            android.content.ComponentName(context, this::class.java),
        )
        ids.forEach { appWidgetId ->
            manager.updateAppWidget(appWidgetId, buildViews(context, appWidgetId))
        }
    }

    protected open fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
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

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        ClockTickScheduler.schedule(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        ClockTickScheduler.schedule(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        ClockTickScheduler.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        ClockTickScheduler.cancel(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        ClockTickScheduler.schedule(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ClockTickScheduler.ACTION_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            -> {
                refreshWidgets(context)
                ClockTickScheduler.schedule(context)
                return
            }
        }
        super.onReceive(context, intent)
    }

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val views = super.buildViews(context, appWidgetId)
        val now = Calendar.getInstance()
        val bitmap = if (ClockTickScheduler.hasExactAccess(context)) {
            val manager = AppWidgetManager.getInstance(context)
            val options = manager.getAppWidgetOptions(appWidgetId)
            ClockWidgetRenderer.render(context, options, WidgetPrefs.style(context), now)
        } else {
            null
        }
        if (bitmap != null) {
            views.setViewVisibility(R.id.widget_clock_bitmap, View.VISIBLE)
            views.setImageViewBitmap(R.id.widget_clock_bitmap, bitmap)
            views.setViewVisibility(R.id.clock_time, View.GONE)
            val spokenTime = DateFormat.getTimeFormat(context).format(now.time)
            views.setContentDescription(
                R.id.widget_clock_bitmap,
                context.getString(R.string.clock_content_description, spokenTime),
            )
        } else {
            views.setViewVisibility(R.id.widget_clock_bitmap, View.GONE)
            views.setViewVisibility(R.id.clock_time, View.VISIBLE)
        }
        return views
    }
}

class DateWidgetProvider : BaseWidgetProvider() {
    override val layoutRes: Int = R.layout.widget_date
    override val tapCategory: String = Intent.CATEGORY_APP_CALENDAR
}
