package com.dotwelwidgets.dotwellabs

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.dotwelwidgets.dotwellabs.weather.WeatherCache

/**
 * Process-lifetime battery listener and startup re-arm.
 *
 * Registering here (rather than in the manifest) means `ACTION_BATTERY_CHANGED`
 * is observed while the app process is alive, so the battery providers can
 * refresh on level/status changes without a foreground service. When the process
 * is killed the registration disappears with it; recovery then comes from the
 * inexact [BatteryRefreshScheduler] alarm, not from an instant push.
 *
 * The receiver only forwards battery updates to [WidgetUpdater.updateBatteryProviders]
 * when the percent or charge status actually changes, and never re-registers
 * itself, so neither the sticky registration nor frequent property-only
 * `BATTERY_CHANGED` deliveries can cause a refresh loop or short-poll drain.
 */
class DotwelApp : Application() {
    private var lastLevel: Int? = null
    private var lastStatus: Int? = null

    private val batteryChangedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != Intent.ACTION_BATTERY_CHANGED) return
            val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val level = if (rawLevel >= 0 && scale > 0) {
                rawLevel * 100 / scale
            } else {
                -1
            }
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            if (level == lastLevel && status == lastStatus) return
            lastLevel = level
            lastStatus = status
            WidgetUpdater.updateBatteryProviders(context)
        }
    }

    override fun onCreate() {
        super.onCreate()
        WeatherCache.attach(this)
        BatteryRefreshScheduler.schedule(this)
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                batteryChangedReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED,
            )
        } else {
            registerReceiver(batteryChangedReceiver, filter)
        }
    }
}
