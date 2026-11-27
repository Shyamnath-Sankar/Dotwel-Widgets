package com.dotwelwidgets.dotwellabs

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.TextPaint
import android.text.TextUtils
import android.widget.RemoteViews
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min

enum class HeadphonesState { PERMISSION_NEEDED, UNAVAILABLE, OFF, DISCONNECTED, CONNECTED }

data class BluetoothHeadphonesSnapshot(
    val state: HeadphonesState,
    val name: String? = null,
    val percent: Int? = null,
) {
    fun stateText(context: Context): String = when (state) {
        HeadphonesState.PERMISSION_NEEDED ->
            context.getString(R.string.headphones_state_permission)
        HeadphonesState.UNAVAILABLE ->
            context.getString(R.string.headphones_state_unavailable)
        HeadphonesState.OFF -> context.getString(R.string.headphones_state_off)
        HeadphonesState.DISCONNECTED ->
            context.getString(R.string.headphones_state_disconnected)
        HeadphonesState.CONNECTED -> percent?.let {
            context.getString(R.string.headphones_percent_format, it)
        } ?: context.getString(R.string.headphones_state_no_level)
    }
}

/**
 * Reads the connected headphone battery without adding a service or polling.
 *
 * `BluetoothDevice.getBatteryLevel()` and the battery-level broadcast are hidden
 * platform APIs, so they are accessed by reflection/string action and every call
 * fails closed. The hidden headset usage-hint fallback runs only on a short-lived
 * background thread and is throttled; it never blocks widget rendering.
 */
object BluetoothHeadphonesState {
    const val ACTION_BATTERY_LEVEL_CHANGED =
        "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
    private const val EXTRA_BATTERY_LEVEL =
        "android.bluetooth.device.extra.BATTERY_LEVEL"
    private const val EXTRA_DEVICE =
        "android.bluetooth.device.extra.DEVICE"
    private const val HINT_INTERVAL_MILLIS = 2 * 60 * 1000L

    private val cache = ConcurrentHashMap<String, Int>()
    private val hintLock = Any()
    private var lastHintAttempt = 0L

    @SuppressLint("MissingPermission")
    fun read(context: Context): BluetoothHeadphonesSnapshot {
        if (!BluetoothAccess.isGranted(context)) {
            return BluetoothHeadphonesSnapshot(HeadphonesState.PERMISSION_NEEDED)
        }
        val manager = context.getSystemService(BluetoothManager::class.java)
            ?: return BluetoothHeadphonesSnapshot(HeadphonesState.UNAVAILABLE)
        val adapter = manager.adapter
            ?: return BluetoothHeadphonesSnapshot(HeadphonesState.UNAVAILABLE)
        val enabled = try {
            adapter.isEnabled
        } catch (_: SecurityException) {
            return BluetoothHeadphonesSnapshot(HeadphonesState.PERMISSION_NEEDED)
        }
        if (!enabled) return BluetoothHeadphonesSnapshot(HeadphonesState.OFF)
        val device = connectedHeadphones(manager)
            ?: return BluetoothHeadphonesSnapshot(HeadphonesState.DISCONNECTED)
        val name = safeName(device)
        val address = safeAddress(device)
        val reflected = readDeviceBattery(device)
        if (reflected != null && address != null) cache[address] = reflected
        val percent = reflected ?: address?.let { cache[it] }
        return BluetoothHeadphonesSnapshot(HeadphonesState.CONNECTED, name, percent)
    }

    @SuppressLint("MissingPermission")
    fun updateFromBroadcast(context: Context, intent: Intent): Boolean {
        if (intent.action != ACTION_BATTERY_LEVEL_CHANGED) return false
        if (!BluetoothAccess.isGranted(context)) return false
        val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
        if (level !in 0..100) return false
        val device = parcelableDevice(intent) ?: return false
        val address = safeAddress(device) ?: return false
        return cache.put(address, level) != level
    }

    @SuppressLint("MissingPermission")
    fun refreshAsync(context: Context) {
        if (!BluetoothAccess.isGranted(context)) return
        val now = SystemClock.elapsedRealtime()
        synchronized(hintLock) {
            if (now - lastHintAttempt < HINT_INTERVAL_MILLIS) return
            lastHintAttempt = now
        }
        val app = context.applicationContext
        Thread({
            try {
                val manager = app.getSystemService(BluetoothManager::class.java)
                    ?: return@Thread
                val adapter = manager.adapter ?: return@Thread
                val device = connectedHeadphones(manager) ?: return@Thread
                if (readDeviceBattery(device) != null) return@Thread
                val proxyRef = AtomicReference<BluetoothProfile?>(null)
                val latch = CountDownLatch(1)
                val listener = object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(
                        profile: Int,
                        proxy: BluetoothProfile?,
                    ) {
                        proxyRef.set(proxy)
                        latch.countDown()
                    }

                    override fun onServiceDisconnected(profile: Int) {
                        latch.countDown()
                    }
                }
                if (!adapter.getProfileProxy(app, listener, BluetoothProfile.HEADSET)) {
                    return@Thread
                }
                latch.await(2, TimeUnit.SECONDS)
                val service = proxyRef.get() ?: return@Thread
                try {
                    val method = service.javaClass.getMethod(
                        "getBatteryUsageHint",
                        BluetoothDevice::class.java,
                    )
                    val level = (method.invoke(service, device) as? Int)
                        ?.takeIf { it in 0..100 }
                    if (level != null) {
                        safeAddress(device)?.let { cache[it] = level }
                        Handler(Looper.getMainLooper()).post {
                            WidgetUpdater.updateHeadphonesProviders(app)
                        }
                    }
                } finally {
                    adapter.closeProfileProxy(BluetoothProfile.HEADSET, service)
                }
            } catch (_: Throwable) {
            }
        }, "dotwel-bt-headphones-battery").start()
    }

    @SuppressLint("MissingPermission")
    private fun connectedHeadphones(manager: BluetoothManager): BluetoothDevice? {
        val profiles = intArrayOf(
            BluetoothProfile.A2DP,
            BluetoothProfile.HEADSET,
            BluetoothProfile.GATT,
        )
        for (profile in profiles) {
            val devices = try {
                manager.getConnectedDevices(profile)
            } catch (_: RuntimeException) {
                null
            }
            if (devices.isNullOrEmpty()) continue
            return devices.firstOrNull { safeName(it) != null } ?: devices.first()
        }
        return null
    }

    @SuppressLint("MissingPermission")
    private fun safeName(device: BluetoothDevice): String? = try {
        device.name?.takeIf { it.isNotBlank() }
    } catch (_: SecurityException) {
        null
    }

    @SuppressLint("MissingPermission")
    private fun safeAddress(device: BluetoothDevice): String? = try {
        device.address?.takeIf { it.isNotBlank() }
    } catch (_: SecurityException) {
        null
    }

    private fun readDeviceBattery(device: BluetoothDevice): Int? = try {
        val method = device.javaClass.getMethod("getBatteryLevel")
        (method.invoke(device) as? Int)?.takeIf { it in 0..100 }
    } catch (_: Throwable) {
        null
    }

    @Suppress("DEPRECATION")
    private fun parcelableDevice(intent: Intent): BluetoothDevice? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_DEVICE)
        }
    } catch (_: RuntimeException) {
        null
    }
}

internal object BluetoothHeadphonesRenderer {
    private val WHITE = 0xFFFFFFFF.toInt()
    private val CARD = 0xFFFEF7FF.toInt()
    private val INK = 0xFF000000.toInt()
    private val NAME_GRAY = 0xFF777379.toInt()
    private val GREEN = 0xFF6A9C89.toInt()
    private val RED = 0xFFD71921.toInt()
    private val TRANSPARENT_NAME = 0xCCFFFFFF.toInt()

    fun cardColor(style: WidgetStyle): Int = CARD

    fun batteryColor(percent: Int?): Int = when {
        percent == null -> NAME_GRAY
        percent >= 30 -> GREEN
        else -> RED
    }

    fun percentText(percent: Int?): String = percent?.let { "$it%" } ?: "NA%"

    fun statusWord(snapshot: BluetoothHeadphonesSnapshot): String = when (snapshot.state) {
        HeadphonesState.CONNECTED -> "CONNECTED"
        HeadphonesState.OFF -> "OFF"
        HeadphonesState.DISCONNECTED -> "DISCONNECTED"
        HeadphonesState.UNAVAILABLE -> "UNAVAILABLE"
        HeadphonesState.PERMISSION_NEEDED -> "NO ACCESS"
    }

    fun render(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        style: WidgetStyle,
        snapshot: BluetoothHeadphonesSnapshot,
        square: Boolean,
    ): Bitmap {
        val width = widthPx.coerceAtLeast(1)
        val height = heightPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val inset = min(width, height) * 0.03f
        val radius = min(width, height) * 0.16f
        if (style != WidgetStyle.TRANSPARENT) {
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = cardColor(style)
                if (style == WidgetStyle.TRANSLUCENT) {
                    setShadowLayer(6f, 0f, 3f, 0x33000000)
                }
            }
            canvas.drawRoundRect(
                RectF(inset, inset, width - inset, height - inset),
                radius,
                radius,
                cardPaint,
            )
        }
        val ink = if (style == WidgetStyle.TRANSPARENT) WHITE else INK
        val nameColor = if (style == WidgetStyle.TRANSPARENT) TRANSPARENT_NAME else NAME_GRAY
        if (square) {
            drawSquare(canvas, width, height, style, snapshot, ink, nameColor)
        } else {
            drawWide(canvas, width, height, style, snapshot, ink, nameColor)
        }
        return bitmap
    }

    private fun drawWide(
        canvas: Canvas,
        width: Int,
        height: Int,
        style: WidgetStyle,
        snapshot: BluetoothHeadphonesSnapshot,
        ink: Int,
        nameColor: Int,
    ) {
        val iconSize = min(height * 0.62f, width * 0.20f)
        drawEarbuds(canvas, width * 0.17f, height * 0.50f, iconSize, ink, style)
        val textLeft = width * 0.33f
        val textRight = width * 0.95f
        val textCenter = (textLeft + textRight) / 2f
        val textWidth = textRight - textLeft
        drawDotText(
            canvas,
            statusWord(snapshot),
            textCenter,
            height * 0.32f,
            textWidth,
            height * 0.30f,
            ink,
            style,
        )
        drawDotText(
            canvas,
            percentText(snapshot.percent),
            textCenter,
            height * 0.58f,
            textWidth,
            height * 0.24f,
            batteryColor(snapshot.percent),
            style,
        )
        drawName(
            canvas,
            snapshot.name ?: "HEADPHONES",
            textCenter,
            height * 0.84f,
            textWidth,
            height * 0.16f,
            nameColor,
            style,
        )
    }

    private fun drawSquare(
        canvas: Canvas,
        width: Int,
        height: Int,
        style: WidgetStyle,
        snapshot: BluetoothHeadphonesSnapshot,
        ink: Int,
        nameColor: Int,
    ) {
        val iconSize = min(width, height) * 0.32f
        drawEarbuds(canvas, width / 2f, height * 0.26f, iconSize, ink, style)
        drawDotText(
            canvas,
            statusWord(snapshot),
            width / 2f,
            height * 0.52f,
            width * 0.86f,
            height * 0.12f,
            ink,
            style,
        )
        drawDotText(
            canvas,
            percentText(snapshot.percent),
            width / 2f,
            height * 0.66f,
            width * 0.60f,
            height * 0.14f,
            batteryColor(snapshot.percent),
            style,
        )
        drawName(
            canvas,
            snapshot.name ?: "HEADPHONES",
            width / 2f,
            height * 0.82f,
            width * 0.86f,
            height * 0.075f,
            nameColor,
            style,
        )
    }

    private fun drawEarbuds(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        color: Int,
        style: WidgetStyle,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            if (style == WidgetStyle.TRANSPARENT) {
                setShadowLayer(4f, 0f, 2f, 0x99000000.toInt())
            }
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = RED
            if (style == WidgetStyle.TRANSPARENT) {
                setShadowLayer(4f, 0f, 2f, 0x99000000.toInt())
            }
        }
        val bulbRadius = size * 0.19f
        val stemWidth = size * 0.13f
        val stemHeight = size * 0.38f
        val gap = size * 0.36f
        val bulbCy = cy - size * 0.10f
        for (sign in intArrayOf(-1, 1)) {
            val bulbCx = cx + sign * gap / 2f
            canvas.drawCircle(bulbCx, bulbCy, bulbRadius, paint)
            canvas.drawCircle(bulbCx, bulbCy, bulbRadius * 0.30f, accentPaint)
            val stemTop = bulbCy + bulbRadius * 0.55f
            canvas.drawRoundRect(
                RectF(
                    bulbCx - stemWidth / 2f,
                    stemTop,
                    bulbCx + stemWidth / 2f,
                    stemTop + stemHeight,
                ),
                stemWidth / 2f,
                stemWidth / 2f,
                paint,
            )
        }
    }

    private fun drawDotText(
        canvas: Canvas,
        text: String,
        cx: Float,
        cy: Float,
        maxWidth: Float,
        maxHeight: Float,
        color: Int,
        style: WidgetStyle,
    ) {
        val pitch = DotGlyphFont.fitPitch(text, maxWidth, maxHeight)
        if (pitch <= 0f) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            if (style == WidgetStyle.TRANSPARENT) {
                setShadowLayer(4f, 0f, 2f, 0x99000000.toInt())
            }
        }
        DotGlyphFont.drawCenteredInk(canvas, text, cx, cy, pitch, paint)
    }

    private fun drawName(
        canvas: Canvas,
        text: String,
        cx: Float,
        cy: Float,
        maxWidth: Float,
        textSize: Float,
        color: Int,
        style: WidgetStyle,
    ) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.textSize = textSize.coerceAtLeast(6f)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            if (style == WidgetStyle.TRANSPARENT) {
                setShadowLayer(4f, 0f, 2f, 0x99000000.toInt())
            }
        }
        val fitted = TextUtils.ellipsize(
            text,
            paint,
            maxWidth.coerceAtLeast(1f),
            TextUtils.TruncateAt.END,
        ).toString()
        canvas.drawText(fitted, cx, cy, paint)
    }
}

abstract class BaseBluetoothHeadphonesWidgetProvider : BaseWidgetProvider() {
    override val layoutRes: Int = R.layout.widget_bt_headphones
    override val tapAction: String = Settings.ACTION_BLUETOOTH_SETTINGS
    protected abstract val square: Boolean

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val style = WidgetPrefs.style(context)
        val snapshot = BluetoothHeadphonesState.read(context)
        return buildBitmapViews(
            context,
            appWidgetId,
            R.id.headphones_bitmap,
            R.id.headphones_fallback,
            render = { widthPx, heightPx ->
                BluetoothHeadphonesRenderer.render(
                    context,
                    widthPx,
                    heightPx,
                    style,
                    snapshot,
                    square,
                )
            },
        ) decorate@{ views, usedBitmap ->
            views.setInt(
                R.id.widget_root,
                "setBackgroundResource",
                R.drawable.bg_widget_none,
            )
            views.setTextColor(
                R.id.headphones_fallback,
                if (style == WidgetStyle.TRANSPARENT) 0xFFFFFFFF.toInt() else 0xFF1B1B1D.toInt(),
            )
            views.setTextViewText(
                R.id.headphones_fallback,
                fallbackText(context, snapshot),
            )
            if (usedBitmap) {
                views.setContentDescription(
                    R.id.headphones_bitmap,
                    context.getString(
                        R.string.bt_headphones_content_description,
                        snapshot.name ?: context.getString(R.string.headphones_label_text),
                        snapshot.stateText(context),
                    ),
                )
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        BluetoothHeadphonesState.refreshAsync(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothHeadphonesState.ACTION_BATTERY_LEVEL_CHANGED,
            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothDevice.ACTION_ACL_DISCONNECTED,
            android.bluetooth.BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED,
            android.bluetooth.BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
            BluetoothAdapter.ACTION_STATE_CHANGED,
            -> {
                BluetoothHeadphonesState.updateFromBroadcast(context, intent)
                refreshWidgets(context)
                BluetoothHeadphonesState.refreshAsync(context)
                return
            }
        }
        super.onReceive(context, intent)
    }

    private fun fallbackText(
        context: Context,
        snapshot: BluetoothHeadphonesSnapshot,
    ): String = context.getString(
        R.string.headphones_fallback_format,
        snapshot.name ?: context.getString(R.string.headphones_label_text),
        snapshot.stateText(context),
    )
}

class BluetoothHeadphonesWideWidgetProvider : BaseBluetoothHeadphonesWidgetProvider() {
    override val square: Boolean = false
}

class BluetoothHeadphonesSquareWidgetProvider : BaseBluetoothHeadphonesWidgetProvider() {
    override val square: Boolean = true
}
