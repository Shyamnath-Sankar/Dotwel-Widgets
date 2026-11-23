package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.RemoteViews

/**
 * Reads the real system battery level. BATTERY_PROPERTY_CAPACITY is preferred;
 * the sticky ACTION_BATTERY_CHANGED broadcast is the fallback. No permission,
 * fake charge state or separate service is involved.
 */
object BatteryState {
    fun level(context: Context): Int {
        val manager = context.getSystemService(BatteryManager::class.java)
        val capacity = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ?: -1
        if (capacity in 0..100) return capacity
        val sticky = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )
        val rawLevel = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (rawLevel >= 0 && scale > 0) {
            (rawLevel * 100 / scale).coerceIn(0, 100)
        } else {
            0
        }
    }
}

class BatteryWidgetProvider : BaseWidgetProvider() {
    override val layoutRes: Int = R.layout.widget_battery
    override val tapAction: String = Intent.ACTION_POWER_USAGE_SUMMARY
    override val refreshesBatteryAlarm: Boolean = true

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val style = WidgetPrefs.style(context)
        val level = BatteryState.level(context)
        return buildBitmapViews(
            context,
            appWidgetId,
            R.id.widget_battery_bitmap,
            R.id.battery_fallback,
            render = { widthPx, heightPx ->
                KwgtBatteryRenderer.render(context, widthPx, heightPx, style, level)
            },
        ) decorate@{ views, usedBitmap ->
            views.setTextColor(R.id.battery_fallback, UpstreamNtDraw.ink(context, style))
            views.setTextViewText(
                R.id.battery_fallback,
                context.getString(R.string.battery_fallback_format, level),
            )
            if (usedBitmap) {
                views.setContentDescription(
                    R.id.widget_battery_bitmap,
                    context.getString(R.string.battery_content_description, level),
                )
            }
        }
    }
}

class BatteryMiniWidgetProvider : BaseWidgetProvider() {
    override val layoutRes: Int = R.layout.widget_battery
    override val tapAction: String = Intent.ACTION_POWER_USAGE_SUMMARY
    override val refreshesBatteryAlarm: Boolean = true

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val style = WidgetPrefs.style(context)
        val level = BatteryState.level(context)
        return buildBitmapViews(
            context,
            appWidgetId,
            R.id.widget_battery_bitmap,
            R.id.battery_fallback,
            render = { widthPx, heightPx ->
                KwgtBatteryMiniRenderer.render(context, widthPx, heightPx, style, level)
            },
        ) decorate@{ views, usedBitmap ->
            views.setTextColor(R.id.battery_fallback, UpstreamNtDraw.ink(context, style))
            views.setTextViewText(
                R.id.battery_fallback,
                context.getString(R.string.battery_fallback_format, level),
            )
            if (usedBitmap) {
                views.setContentDescription(
                    R.id.widget_battery_bitmap,
                    context.getString(R.string.battery_mini_content_description, level),
                )
            }
        }
    }
}
