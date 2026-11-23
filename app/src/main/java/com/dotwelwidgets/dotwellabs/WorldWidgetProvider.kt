package com.dotwelwidgets.dotwellabs

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.widget.RemoteViews
import java.util.TimeZone

object WorldPrefs {
    private const val PREFS = "dotwel_world"
    private const val PENDING = "pending_zones"
    private const val DEFAULT = "London|Europe/London;Tokyo|Asia/Tokyo"

    val choices: List<NtWorldRenderer.Zone> = listOf(
        NtWorldRenderer.Zone("London", "Europe/London"),
        NtWorldRenderer.Zone("New York", "America/New_York"),
        NtWorldRenderer.Zone("Los Angeles", "America/Los_Angeles"),
        NtWorldRenderer.Zone("Berlin", "Europe/Berlin"),
        NtWorldRenderer.Zone("Paris", "Europe/Paris"),
        NtWorldRenderer.Zone("Dubai", "Asia/Dubai"),
        NtWorldRenderer.Zone("Mumbai", "Asia/Kolkata"),
        NtWorldRenderer.Zone("Singapore", "Asia/Singapore"),
        NtWorldRenderer.Zone("Tokyo", "Asia/Tokyo"),
        NtWorldRenderer.Zone("Seoul", "Asia/Seoul"),
        NtWorldRenderer.Zone("Sydney", "Australia/Sydney"),
        NtWorldRenderer.Zone("Sao Paulo", "America/Sao_Paulo"),
    )

    fun localChoice(): NtWorldRenderer.Zone {
        val zone = TimeZone.getDefault()
        return NtWorldRenderer.Zone(zone.getDisplayName(false, TimeZone.SHORT), zone.id)
    }

    fun zones(context: Context, appWidgetId: Int): List<NtWorldRenderer.Zone> =
        parse(prefs(context).getString("zones_$appWidgetId", DEFAULT) ?: DEFAULT)

    fun setZones(context: Context, appWidgetId: Int, zones: List<NtWorldRenderer.Zone>) {
        prefs(context).edit().putString("zones_$appWidgetId", encode(zones)).apply()
    }

    fun remove(context: Context, appWidgetId: Int) {
        prefs(context).edit().remove("zones_$appWidgetId").apply()
    }

    fun setPending(context: Context, zones: List<NtWorldRenderer.Zone>) {
        prefs(context).edit().putString(PENDING, encode(zones)).apply()
    }

    fun clearPending(context: Context) {
        prefs(context).edit().remove(PENDING).apply()
    }

    fun previewZones(context: Context): List<NtWorldRenderer.Zone> =
        prefs(context).getString(PENDING, null)?.let { parse(it) } ?: parse(DEFAULT)

    fun takePending(context: Context): List<NtWorldRenderer.Zone>? {
        val stored = prefs(context).getString(PENDING, null) ?: return null
        clearPending(context)
        return parse(stored)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun encode(zones: List<NtWorldRenderer.Zone>): String =
        zones.take(2).joinToString(";") { "${it.label}|${it.zoneId}" }

    private fun parse(value: String): List<NtWorldRenderer.Zone> =
        value.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size != 2 || parts[1].isBlank()) null
            else NtWorldRenderer.Zone(parts[0], parts[1])
        }.ifEmpty { parse(DEFAULT) }
}

class WorldWidgetProvider : BaseWidgetProvider() {
    override val layoutRes: Int = R.layout.widget_world
    override val tapAction: String = AlarmClock.ACTION_SHOW_ALARMS

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        MinuteTickScheduler.schedule(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
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
        MinuteTickScheduler.cancel(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WorldPrefs.remove(context, it) }
        super.onDeleted(context, appWidgetIds)
        MinuteTickScheduler.schedule(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == MinuteTickScheduler.ACTION_TICK) {
            refreshWidgets(context)
            MinuteTickScheduler.schedule(context)
            return
        }
        super.onReceive(context, intent)
    }

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val style = WidgetPrefs.style(context)
        val zones = WorldPrefs.zones(context, appWidgetId)
        return buildBitmapViews(
            context,
            appWidgetId,
            R.id.widget_world_bitmap,
            R.id.world_fallback,
        ) { widthPx, heightPx ->
            NtWorldRenderer.render(context, widthPx, heightPx, style, zones, false)
        } decorate@{ views, _ ->
            views.setTextColor(R.id.world_fallback, UpstreamNtDraw.ink(context, style))
        }
    }
}
