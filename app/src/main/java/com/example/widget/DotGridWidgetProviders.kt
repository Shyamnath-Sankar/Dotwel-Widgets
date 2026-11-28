package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import com.example.DotGridApplication
import com.example.MainActivity
import com.example.R
import com.example.model.LiveSnapshot
import com.example.model.WidgetCatalog
import com.example.model.WidgetConfig
import com.example.model.WidgetGridSize
import com.example.model.WidgetInteractiveAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val ACTION_WIDGET_TAP = "com.dotwellabs.anywidgets.ACTION_WIDGET_TAP"
const val EXTRA_PRESET_ID = "extra_preset_id"

abstract class BaseDotGridWidgetProvider(
    private val defaultGridSize: WidgetGridSize
) : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    WidgetUpdater.updateSingleWidget(context, appWidgetManager, appWidgetId, defaultGridSize)
                }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WidgetUpdater.updateSingleWidget(context, appWidgetManager, appWidgetId, defaultGridSize)
            } finally {
                pending.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = (context.applicationContext as? DotGridApplication)?.repository ?: return@launch
                for (id in appWidgetIds) {
                    repo.unbindAppWidgetId(id)
                }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_TAP) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val repo = (context.applicationContext as? DotGridApplication)?.repository ?: return@launch
                    val existing = repo.getWidgetByAppWidgetId(appWidgetId) ?: return@launch
                    val spec = WidgetCatalog.getSpec(existing.widgetTypeId)
                    when (spec.interactiveAction) {
                        WidgetInteractiveAction.OPEN_WIFI_PANEL -> SystemControlsLauncher.openWifiPanel(context)
                        WidgetInteractiveAction.OPEN_INTERNET_PANEL -> SystemControlsLauncher.openInternetPanel(context)
                        WidgetInteractiveAction.OPEN_BLUETOOTH_SETTINGS -> SystemControlsLauncher.openBluetoothSettings(context)
                        WidgetInteractiveAction.OPEN_AUDIO_PANEL -> SystemControlsLauncher.openVolumeAudioPanel(context)
                        WidgetInteractiveAction.NONE -> {
                            val openIntent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            context.startActivity(openIntent)
                        }
                        else -> {
                            val mutated = repo.applyInteractiveTap(existing)
                            repo.saveWidgetConfig(mutated)
                        }
                    }
                    val manager = AppWidgetManager.getInstance(context)
                    WidgetUpdater.updateSingleWidget(context, manager, appWidgetId, existing.gridSize)
                } finally {
                    pending.finish()
                }
            }
        }
    }
}

class DotGridSmallWidgetProvider : BaseDotGridWidgetProvider(WidgetGridSize.SMALL_2X2)
class DotGridWideWidgetProvider : BaseDotGridWidgetProvider(WidgetGridSize.WIDE_4X2)
class DotGridLargeWidgetProvider : BaseDotGridWidgetProvider(WidgetGridSize.LARGE_4X4)

class WidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val presetId = intent.getLongExtra(EXTRA_PRESET_ID, -1L)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = (context.applicationContext as? DotGridApplication)?.repository ?: return@launch
                if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return@launch

                val alreadyBound = repo.getWidgetByAppWidgetId(appWidgetId)
                val consumedPending = repo.preferences.consumePendingPinPresetId()
                val targetPresetId = if (presetId > 0L) presetId else consumedPending
                if (alreadyBound == null && targetPresetId > 0L) {
                    val preset = repo.getWidgetById(targetPresetId)
                    if (preset != null) {
                        if (preset.appWidgetId > 0 && preset.appWidgetId != appWidgetId) {
                            repo.saveWidgetConfig(preset.copy(id = 0L, appWidgetId = appWidgetId))
                        } else {
                            repo.bindPresetToAppWidgetId(targetPresetId, appWidgetId)
                        }
                    }
                }
                val bound = repo.getWidgetByAppWidgetId(appWidgetId)
                val manager = AppWidgetManager.getInstance(context)
                WidgetUpdater.updateSingleWidget(
                    context = context,
                    manager = manager,
                    appWidgetId = appWidgetId,
                    fallbackSize = bound?.gridSize ?: WidgetUpdater.resolveGridSizeForWidgetId(context, appWidgetId)
                )
            } finally {
                pending.finish()
            }
        }
    }
}

object WidgetUpdater {

    suspend fun updateAllHomeScreenWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val providers = listOf(
            DotGridSmallWidgetProvider::class.java to WidgetGridSize.SMALL_2X2,
            DotGridWideWidgetProvider::class.java to WidgetGridSize.WIDE_4X2,
            DotGridLargeWidgetProvider::class.java to WidgetGridSize.LARGE_4X4
        )
        for ((clazz, size) in providers) {
            val ids = manager.getAppWidgetIds(ComponentName(context, clazz))
            for (id in ids) {
                updateSingleWidget(context, manager, id, size)
            }
        }
    }

    suspend fun updateSingleWidget(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        fallbackSize: WidgetGridSize
    ) {
        val repo = (context.applicationContext as? DotGridApplication)?.repository ?: return
        val snapshot = repo.buildCurrentLiveSnapshot()
        var config = repo.getWidgetByAppWidgetId(appWidgetId)
        if (config == null) {
            val pendingId = repo.preferences.consumePendingPinPresetId()
            if (pendingId > 0L) {
                val preset = repo.getWidgetById(pendingId)
                if (preset != null) {
                    if (preset.appWidgetId > 0 && preset.appWidgetId != appWidgetId) {
                        val clonedId = repo.saveWidgetConfig(preset.copy(id = 0L, appWidgetId = appWidgetId))
                        config = repo.getWidgetById(clonedId)
                    } else {
                        repo.bindPresetToAppWidgetId(pendingId, appWidgetId)
                        config = repo.getWidgetById(pendingId)
                    }
                }
            }
        }
        if (config == null) {
            val defaultSpec = WidgetCatalog.getSpec("night_bedside_clock")
            config = WidgetCatalog.createDefaultConfig(defaultSpec).copy(
                appWidgetId = appWidgetId,
                gridSize = fallbackSize
            )
            repo.saveWidgetConfig(config)
        }

        val options = manager.getAppWidgetOptions(appWidgetId)
        val (slotWdp, slotHdp) = resolveAccurateSlotSizeDp(context, options, config.gridSize)

        val bitmap = HomeScreenBitmapRenderer.render(
            context = context,
            config = config,
            snapshot = snapshot,
            widthDp = slotWdp,
            heightDp = slotHdp,
            maxSidePx = 740
        )
        val views = RemoteViews(context.packageName, R.layout.widget_dotgrid_layout)
        views.setImageViewBitmap(R.id.widget_bitmap, bitmap)

        // Tap body -> interactive widget action or Play-compliant Settings Panel
        val providerClass = providerForSize(resolveGridSizeForWidgetId(context, appWidgetId, config.gridSize))
        val tapIntent = Intent(context, providerClass).apply {
            action = ACTION_WIDGET_TAP
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val tapPi = PendingIntent.getBroadcast(
            context,
            appWidgetId * 10 + 1,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_bitmap, tapPi)

        // Tap top-right corner -> open individual widget settings
        val configIntent = Intent(context, WidgetConfigureActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val configPi = PendingIntent.getActivity(
            context,
            appWidgetId * 10 + 2,
            configIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_settings_hit, configPi)

        manager.updateAppWidget(appWidgetId, views)
    }

    /**
     * Resolves the EXACT portrait or landscape launcher bounding box in dp.
     * Fixes the classic Android AppWidget bug where using (MIN_WIDTH, MIN_HEIGHT) mixes
     * portrait width with landscape height and distorts shape & orientation.
     */
    private fun resolveAccurateSlotSizeDp(
        context: Context,
        options: Bundle,
        gridSize: WidgetGridSize
    ): Pair<Float, Float> {
        val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        // 1. Android 12+ exact launcher sizes list
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            @Suppress("DEPRECATION")
            val sizes: ArrayList<SizeF>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
            } else {
                options.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES)
            }
            if (!sizes.isNullOrEmpty()) {
                val chosen = if (sizes.size == 1) {
                    sizes.first()
                } else if (isLandscape) {
                    sizes.maxByOrNull { it.width } ?: sizes.first()
                } else {
                    sizes.maxByOrNull { it.height } ?: sizes.first()
                }
                if (chosen.width > 80f && chosen.height > 70f) {
                    return chosen.width to chosen.height
                }
            }
        }

        // 2. Orientation-correct MIN/MAX dp options
        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

        val rawW = (if (isLandscape) maxW else minW).toFloat()
        val rawH = (if (isLandscape) minH else maxH).toFloat()

        return if (rawW > 80f && rawH > 70f) {
            rawW to rawH
        } else {
            gridSize.canonicalWidthDp to gridSize.canonicalHeightDp
        }
    }

    fun requestPinToHomeScreen(
        context: Context,
        config: WidgetConfig,
        snapshot: LiveSnapshot
    ): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !manager.isRequestPinAppWidgetSupported) {
            return false
        }
        val provider = ComponentName(context, providerForSize(config.gridSize))

        val previewBundle = runCatching {
            val previewBmp = HomeScreenBitmapRenderer.render(
                context = context,
                config = config,
                snapshot = snapshot,
                widthDp = config.gridSize.canonicalWidthDp,
                heightDp = config.gridSize.canonicalHeightDp,
                maxSidePx = 640
            )
            val previewViews = RemoteViews(context.packageName, R.layout.widget_dotgrid_layout).apply {
                setImageViewBitmap(R.id.widget_bitmap, previewBmp)
            }
            Bundle().apply {
                putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, previewViews)
            }
        }.getOrNull()

        val callbackIntent = Intent(context, WidgetPinReceiver::class.java).apply {
            putExtra(EXTRA_PRESET_ID, config.id)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val successCallback = PendingIntent.getBroadcast(
            context,
            (config.id % 10000).toInt() + 500,
            callbackIntent,
            flags
        )
        return runCatching {
            manager.requestPinAppWidget(provider, previewBundle, successCallback)
        }.getOrDefault(false)
    }

    fun resolveGridSizeForWidgetId(
        context: Context,
        appWidgetId: Int,
        fallback: WidgetGridSize = WidgetGridSize.SMALL_2X2
    ): WidgetGridSize {
        val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId) ?: return fallback
        return when (info.provider.className) {
            DotGridWideWidgetProvider::class.java.name -> WidgetGridSize.WIDE_4X2
            DotGridLargeWidgetProvider::class.java.name -> WidgetGridSize.LARGE_4X4
            DotGridSmallWidgetProvider::class.java.name -> WidgetGridSize.SMALL_2X2
            else -> fallback
        }
    }

    private fun providerForSize(size: WidgetGridSize): Class<out BaseDotGridWidgetProvider> = when (size) {
        WidgetGridSize.SMALL_2X2 -> DotGridSmallWidgetProvider::class.java
        WidgetGridSize.WIDE_4X2 -> DotGridWideWidgetProvider::class.java
        WidgetGridSize.LARGE_4X4 -> DotGridLargeWidgetProvider::class.java
    }
}
