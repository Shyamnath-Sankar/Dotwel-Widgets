package com.example

import android.app.Application
import com.example.data.repository.DotGridRepository
import com.example.data.worker.WidgetSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DotGridApplication : Application() {

    lateinit var repository: DotGridRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        repository = DotGridRepository(this)
        appScope.launch {
            repository.seedStarterWidgetsIfEmpty()
            val settings = repository.getSettingsOnce()
            WidgetSyncWorker.schedule(this@DotGridApplication, settings.refreshIntervalMinutes)
        }
    }
}
