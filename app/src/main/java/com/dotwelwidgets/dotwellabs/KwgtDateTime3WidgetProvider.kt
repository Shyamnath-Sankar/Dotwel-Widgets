package com.dotwelwidgets.dotwellabs

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import com.dotwelwidgets.dotwellabs.weather.WeatherDisplay
import com.dotwelwidgets.dotwellabs.weather.WeatherRenderState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Native provider for KWGT Nothing_Date_Time3.
 *
 * The source stack has a central time module with no expression, an AM/PM
 * sibling, a date line and a current-weather line. The native port repairs the
 * missing time expression with a live TextClock, repairs the source global
 * `accent ` trailing-space typo, and reads only the optional manual-city cache
 * for weather. It never requests location or network from the widget.
 */
class KwgtDateTime3WidgetProvider : BaseWidgetProvider() {
    override val layoutRes: Int = R.layout.widget_kwgt_date_time3

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        MinuteTickScheduler.schedule(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        MinuteTickScheduler.schedule(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        MinuteTickScheduler.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        MinuteTickScheduler.schedule(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        MinuteTickScheduler.schedule(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            MinuteTickScheduler.ACTION_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            -> {
                refreshWidgets(context)
                MinuteTickScheduler.schedule(context)
                return
            }
        }
        super.onReceive(context, intent)
    }

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val style = WidgetPrefs.style(context)
        val palette = KwgtDateTime3Renderer.palette(style)
        val views = baseViews(context, appWidgetId)
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        val options = manager.getAppWidgetOptions(appWidgetId)
        val target = WidgetSizing.targets(
            context,
            options,
            WidgetSizing.exactSizesFromOptions(options),
        ).firstOrNull()
        val layout = target?.let {
            KwgtDateTime3Renderer.layout(it.widthPx, it.heightPx)
        }
        views.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            style.backgroundRes,
        )
        val openWeather = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_WEATHER, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        views.setOnClickPendingIntent(
            R.id.widget_root,
            PendingIntent.getActivity(
                context,
                appWidgetId,
                openWeather,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        val state = WeatherRenderState.from(context)
        val weatherText = state.snapshot?.let {
            KwgtDateTime3Renderer.weatherLine(state)
        } ?: WeatherDisplay.setupMessage(state)
        val is24 = DateFormat.is24HourFormat(context)
        val ampm = SimpleDateFormat("a", Locale.getDefault())
            .format(Date())
            .uppercase(Locale.getDefault())

        views.setTextColor(R.id.kwgt_date_time3_time, palette.foreground)
        views.setTextColor(R.id.kwgt_date_time3_ampm, palette.accent)
        views.setTextColor(R.id.kwgt_date_time3_date, palette.foreground)
        views.setTextColor(R.id.kwgt_date_time3_weather, palette.accent)
        views.setTextViewText(R.id.kwgt_date_time3_ampm, ampm)
        views.setTextViewText(R.id.kwgt_date_time3_weather, weatherText)
        if (layout != null) {
            views.setTextViewTextSize(
                R.id.kwgt_date_time3_time,
                android.util.TypedValue.COMPLEX_UNIT_PX,
                layout.timeSize,
            )
            views.setTextViewTextSize(
                R.id.kwgt_date_time3_ampm,
                android.util.TypedValue.COMPLEX_UNIT_PX,
                layout.ampmSize,
            )
            views.setTextViewTextSize(
                R.id.kwgt_date_time3_date,
                android.util.TypedValue.COMPLEX_UNIT_PX,
                layout.dateSize,
            )
            views.setTextViewTextSize(
                R.id.kwgt_date_time3_weather,
                android.util.TypedValue.COMPLEX_UNIT_PX,
                layout.weatherSize,
            )
            views.setViewPadding(
                R.id.kwgt_date_time3_content,
                0,
                (layout.side * 0.02f).toInt(),
                layout.rightInset.toInt(),
                0,
            )
        }
        views.setViewVisibility(
            R.id.kwgt_date_time3_ampm,
            if (is24) View.GONE else View.VISIBLE,
        )
        views.setCharSequence(
            R.id.kwgt_date_time3_time,
            "setFormat12Hour",
            if (is24) "HH:mm" else "hh:mm",
        )
        views.setCharSequence(
            R.id.kwgt_date_time3_time,
            "setFormat24Hour",
            if (is24) "HH:mm" else "hh:mm",
        )
        return views
    }
}
