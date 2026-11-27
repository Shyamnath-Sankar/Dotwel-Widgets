package com.dotwelwidgets.dotwellabs

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.RemoteViews
import com.dotwelwidgets.dotwellabs.weather.WeatherCache
import com.dotwelwidgets.dotwellabs.weather.WeatherRenderState
import com.dotwelwidgets.dotwellabs.weather.WeatherSettings

/**
 * Shared weather-widget provider contract.
 *
 * Widget updates only read the private cache. No receiver path performs network
 * I/O; all fetching happens off the main thread from an explicit user action or
 * the bounded app-resume auto refresh in MainActivity.
 */
abstract class BaseWeatherWidgetProvider : BaseWidgetProvider() {
    protected abstract val bitmapViewId: Int
    protected abstract val fallbackViewId: Int

    protected abstract fun render(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        style: WidgetStyle,
        state: WeatherRenderState,
    ): Bitmap

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val style = WidgetPrefs.style(context)
        val state = WeatherRenderState.from(context)
        return buildBitmapViews(
            context = context,
            appWidgetId = appWidgetId,
            bitmapViewId = bitmapViewId,
            fallbackViewId = fallbackViewId,
            render = { widthPx, heightPx ->
                render(context, widthPx, heightPx, style, state)
            },
        ) { views, usedBitmap ->
            bindWeatherTap(views, context, appWidgetId)
            views.setTextColor(fallbackViewId, UpstreamNtDraw.ink(context, style))
            if (!usedBitmap) {
                views.setTextViewText(fallbackViewId, fallbackText(context, state))
            }
            views.setContentDescription(
                if (usedBitmap) bitmapViewId else fallbackViewId,
                contentDescription(context, state),
            )
            decorateWeatherViews(views, context, appWidgetId, usedBitmap)
        }
    }

    /**
     * Per-variant hook for subclasses that need extra RemoteViews calls.
     *
     * [buildBitmapViews] can return a combined `RemoteViews(Map<SizeF, RemoteViews>)`
     * on API 31+ when the host supplies exact sizes. That combined object cannot be
     * modified afterwards, so this hook is invoked for every per-size view before
     * it enters the size map.
     */
    protected open fun decorateWeatherViews(
        views: RemoteViews,
        context: Context,
        appWidgetId: Int,
        usedBitmap: Boolean,
    ) = Unit

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            -> {
                refreshWidgets(context)
                return
            }
        }
        super.onReceive(context, intent)
    }

    private fun bindWeatherTap(views: RemoteViews, context: Context, appWidgetId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_OPEN_WEATHER
            putExtra(MainActivity.EXTRA_OPEN_WEATHER, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pending = PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, pending)
    }

    private fun fallbackText(context: Context, state: WeatherRenderState): String = when {
        !state.enabled -> context.getString(R.string.weather_widget_off)
        !state.hasLocation -> context.getString(R.string.weather_widget_setup)
        state.snapshot == null && state.failure != null ->
            context.getString(R.string.weather_widget_refresh_failed)
        state.snapshot == null -> context.getString(R.string.weather_widget_no_cache)
        else -> context.getString(R.string.weather_widget_unavailable)
    }

    private fun contentDescription(context: Context, state: WeatherRenderState): String {
        val snapshot = state.snapshot
        if (snapshot == null) return fallbackText(context, state)
        val day = com.dotwelwidgets.dotwellabs.weather.WeatherDisplay.today(
            snapshot,
            state.now,
        )
        return buildString {
            append(snapshot.location.name)
            day?.let {
                append(", ")
                append(
                    com.dotwelwidgets.dotwellabs.weather.WeatherDisplay
                        .forecastDayLabel(it),
                )
            }
            append(", ")
            append(
                com.dotwelwidgets.dotwellabs.weather.WeatherDisplay
                    .statusLine(snapshot, state.now, state.failure),
            )
        }
    }

    protected fun currentFallbackVisibility(visible: Boolean): Int =
        if (visible) View.VISIBLE else View.GONE
}

class NothingTempWidgetProvider : BaseWeatherWidgetProvider() {
    override val layoutRes: Int = R.layout.widget_weather_temp
    override val bitmapViewId: Int = R.id.weather_temp_bitmap
    override val fallbackViewId: Int = R.id.weather_temp_fallback

    override fun render(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        style: WidgetStyle,
        state: WeatherRenderState,
    ): Bitmap = NothingTempRenderer.render(
        context,
        widthPx,
        heightPx,
        style,
        state,
    )
}

class NothingSunInfoWidgetProvider : BaseWeatherWidgetProvider() {
    override val layoutRes: Int = R.layout.widget_weather_sun
    override val bitmapViewId: Int = R.id.weather_sun_bitmap
    override val fallbackViewId: Int = R.id.weather_sun_fallback

    override fun render(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        style: WidgetStyle,
        state: WeatherRenderState,
    ): Bitmap = NothingSunInfoRenderer.render(
        context,
        widthPx,
        heightPx,
        style,
        state,
    )
}
