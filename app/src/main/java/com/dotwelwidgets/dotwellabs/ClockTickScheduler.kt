package com.dotwelwidgets.dotwellabs

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

object ClockTickScheduler {
    const val ACTION_TICK = "com.dotwelwidgets.dotwellabs.action.CLOCK_TICK"
    private const val REQUEST_CODE = 0x0C10

    fun hasExactAccess(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun schedule(context: Context) {
        if (!hasPlacedClocks(context) || !hasExactAccess(context)) {
            cancel(context)
            return
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val now = System.currentTimeMillis()
        val triggerAt = now - (now % 60_000L) + 60_000L
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC,
                triggerAt,
                tickPendingIntent(context),
            )
        } catch (_: SecurityException) {
            cancel(context)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(tickPendingIntent(context))
    }

    private fun hasPlacedClocks(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, ClockWidgetProvider::class.java),
        )
        return ids.isNotEmpty()
    }

    private fun tickPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ClockWidgetProvider::class.java).apply {
            action = ACTION_TICK
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
