package com.example.data.worker

import android.content.Context
import android.os.PowerManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.DotGridApplication
import com.example.widget.WidgetUpdater
import java.util.concurrent.TimeUnit

class WidgetSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? DotGridApplication ?: return Result.success()
        val settings = app.repository.getSettingsOnce()
        val pm = applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isBatterySaver = pm?.isPowerSaveMode == true

        // Zero-network offline refresh; skips even lightweight redraws if Ultra-Eco + Battery Saver active
        if (!(settings.ultraEcoMode && isBatterySaver)) {
            WidgetUpdater.updateAllHomeScreenWidgets(applicationContext)
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "anywidgets_low_battery_refresh"

        fun schedule(context: Context, intervalMinutes: Int) {
            val wm = WorkManager.getInstance(context)
            if (intervalMinutes <= 0) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val safeInterval = intervalMinutes.coerceAtLeast(30).toLong()
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<WidgetSyncWorker>(safeInterval, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            wm.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
