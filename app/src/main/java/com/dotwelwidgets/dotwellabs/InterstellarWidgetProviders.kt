package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import android.view.View
import android.widget.RemoteViews

/**
 * Shared native provider for the three Interstellar Endurance source presets.
 * The shape art is compiled from sanitized source JSON and redrawn by the
 * shared minute scheduler. Seconds-driven source layers are sampled at each
 * update; the port never claims live seconds and gates the catalog entry to
 * Android 12+ where the supported RemoteViews path is available.
 */
abstract class BaseInterstellarWidgetProvider : BaseWidgetProvider() {
    override val layoutRes: Int = R.layout.widget_interstellar
    override val tapAction: String = AlarmClock.ACTION_SHOW_ALARMS
    protected abstract val variant: InterstellarShapeCompiler.Variant

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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val views = baseViews(context, appWidgetId)
            views.setViewVisibility(R.id.widget_interstellar_bitmap, View.GONE)
            views.setViewVisibility(R.id.widget_message, View.VISIBLE)
            views.setTextColor(
                R.id.widget_message,
                UpstreamNtDraw.muted(context, WidgetPrefs.style(context)),
            )
            views.setTextViewText(
                R.id.widget_message,
                context.getString(R.string.interstellar_pre31),
            )
            return views
        }
        val style = WidgetPrefs.style(context)
        return buildBitmapViews(
            context,
            appWidgetId,
            R.id.widget_interstellar_bitmap,
            R.id.widget_message,
            render = { widthPx, heightPx ->
                InterstellarShapeCompiler.render(
                    context,
                    variant,
                    widthPx,
                    heightPx,
                    style,
                )
            },
        ) { views, usedBitmap ->
            views.setInt(
                R.id.widget_root,
                "setBackgroundResource",
                if (usedBitmap) R.drawable.bg_widget_none else style.backgroundRes,
            )
            if (!usedBitmap) {
                views.setTextViewText(
                    R.id.widget_message,
                    context.getString(R.string.interstellar_unavailable),
                )
                views.setTextColor(
                    R.id.widget_message,
                    UpstreamNtDraw.muted(context, style),
                )
            }
        }
    }
}

class KwgtInterstellarEnduranceProvider : BaseInterstellarWidgetProvider() {
    override val variant: InterstellarShapeCompiler.Variant =
        InterstellarShapeCompiler.Variant.ENDURANCE
}

class KwgtInterstellarEnduranceAnalogProvider : BaseInterstellarWidgetProvider() {
    override val variant: InterstellarShapeCompiler.Variant =
        InterstellarShapeCompiler.Variant.ANALOG
}

class KwgtInterstellarEnduranceAnalog2Provider : BaseInterstellarWidgetProvider() {
    override val variant: InterstellarShapeCompiler.Variant =
        InterstellarShapeCompiler.Variant.ANALOG_2
}
