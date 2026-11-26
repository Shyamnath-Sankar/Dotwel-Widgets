package com.dotwelwidgets.dotwellabs

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.widget.RemoteViews
import kotlin.math.max
import kotlin.math.min

/**
 * Optional Bluetooth access. The dangerous permission only exists on Android 12+
 * (API 31); older releases use the normal install-time BLUETOOTH permission.
 * Nothing here prompts the user: the activity owns the explained opt-in.
 */
object BluetoothAccess {
    fun isGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
}

enum class StatusKind { BLUETOOTH, WIFI }

enum class ConnectivityState { OFF, ON, CONNECTED, DISCONNECTED, PERMISSION_NEEDED, UNAVAILABLE }

data class StatusSnapshot(
    val kind: StatusKind,
    val state: ConnectivityState,
    val detail: String? = null,
) {
    fun label(context: Context): String = detail?.takeIf { it.isNotBlank() } ?: when (kind) {
        StatusKind.BLUETOOTH -> context.getString(R.string.bt_status_label_text)
        StatusKind.WIFI -> context.getString(R.string.wifi_status_label_text)
    }

    fun stateText(context: Context): String = when (state) {
        ConnectivityState.OFF -> context.getString(R.string.status_off)
        ConnectivityState.ON -> context.getString(R.string.status_on)
        ConnectivityState.CONNECTED -> context.getString(R.string.status_connected)
        ConnectivityState.DISCONNECTED -> context.getString(R.string.status_disconnected)
        ConnectivityState.PERMISSION_NEEDED -> context.getString(R.string.bt_status_permission_needed)
        ConnectivityState.UNAVAILABLE -> context.getString(R.string.status_unavailable)
    }
}

object BluetoothStatusState {
    @SuppressLint("MissingPermission")
    fun read(context: Context): StatusSnapshot {
        if (!BluetoothAccess.isGranted(context)) {
            return StatusSnapshot(StatusKind.BLUETOOTH, ConnectivityState.PERMISSION_NEEDED)
        }
        val manager = context.getSystemService(BluetoothManager::class.java)
        val adapter = manager?.adapter
            ?: return StatusSnapshot(StatusKind.BLUETOOTH, ConnectivityState.UNAVAILABLE)
        val enabled = try {
            adapter.isEnabled
        } catch (_: SecurityException) {
            return StatusSnapshot(StatusKind.BLUETOOTH, ConnectivityState.PERMISSION_NEEDED)
        }
        if (!enabled) return StatusSnapshot(StatusKind.BLUETOOTH, ConnectivityState.OFF)
        val name = connectedDeviceName(manager)
        return if (name != null) {
            StatusSnapshot(StatusKind.BLUETOOTH, ConnectivityState.CONNECTED, name)
        } else {
            StatusSnapshot(StatusKind.BLUETOOTH, ConnectivityState.ON)
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectedDeviceName(manager: BluetoothManager): String? {
        val profiles = intArrayOf(
            BluetoothProfile.HEADSET,
            BluetoothProfile.A2DP,
            BluetoothProfile.GATT,
            BluetoothProfile.GATT_SERVER,
        )
        for (profile in profiles) {
            val devices = try {
                manager.getConnectedDevices(profile)
            } catch (_: RuntimeException) {
                null
            }
            devices?.firstOrNull { it.name?.isNotBlank() == true }?.let { return it.name }
        }
        return null
    }
}

object WifiStatusState {
    @Suppress("DEPRECATION")
    fun read(context: Context): StatusSnapshot {
        val manager = context.applicationContext.getSystemService(WifiManager::class.java)
            ?: return StatusSnapshot(StatusKind.WIFI, ConnectivityState.UNAVAILABLE)
        val state = try {
            manager.wifiState
        } catch (_: SecurityException) {
            return StatusSnapshot(StatusKind.WIFI, ConnectivityState.UNAVAILABLE)
        }
        when (state) {
            WifiManager.WIFI_STATE_DISABLED,
            WifiManager.WIFI_STATE_DISABLING,
            -> return StatusSnapshot(StatusKind.WIFI, ConnectivityState.OFF)
            WifiManager.WIFI_STATE_ENABLED,
            WifiManager.WIFI_STATE_ENABLING,
            -> Unit
            else -> return StatusSnapshot(StatusKind.WIFI, ConnectivityState.UNAVAILABLE)
        }
        val info = try {
            manager.connectionInfo
        } catch (_: RuntimeException) {
            null
        } ?: return StatusSnapshot(StatusKind.WIFI, ConnectivityState.ON)
        val connected = info.supplicantState == SupplicantState.COMPLETED ||
            info.networkId != -1
        return if (connected) {
            StatusSnapshot(StatusKind.WIFI, ConnectivityState.CONNECTED, readableSsid(info.ssid))
        } else {
            StatusSnapshot(StatusKind.WIFI, ConnectivityState.DISCONNECTED)
        }
    }

    private fun readableSsid(raw: String?): String? {
        val value = raw?.trim()?.trim('"') ?: return null
        if (value.isBlank()) return null
        if (value.equals("<unknown ssid>", ignoreCase = true)) return null
        if (value.equals("0x", ignoreCase = true)) return null
        return value
    }
}

/** Original programmatic connectivity glyphs; no font or bitmap asset is bundled. */
internal object StatusTileRenderer {
    fun render(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        style: WidgetStyle,
        snapshot: StatusSnapshot,
    ): Bitmap {
        val width = widthPx.coerceAtLeast(1)
        val height = heightPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val side = min(width, height).toFloat()
        val ink = UpstreamNtDraw.ink(context, style)
        val muted = UpstreamNtDraw.muted(context, style)
        val accent = UpstreamNtDraw.accent(context, style)
        val glyphColor = when (snapshot.state) {
            ConnectivityState.CONNECTED -> accent
            ConnectivityState.OFF,
            ConnectivityState.DISCONNECTED,
            ConnectivityState.UNAVAILABLE,
            -> muted
            else -> ink
        }
        drawGlyph(canvas, snapshot.kind, width / 2f, height * 0.34f, side * 0.30f, glyphColor, style)
        drawCentered(
            canvas,
            snapshot.label(context),
            width / 2f,
            height * 0.68f,
            side * 0.085f,
            muted,
            width * 0.9f,
            medium = false,
            style = style,
        )
        drawCentered(
            canvas,
            snapshot.stateText(context),
            width / 2f,
            height * 0.86f,
            side * 0.115f,
            ink,
            width * 0.9f,
            medium = true,
            style = style,
        )
        return bitmap
    }

    private fun drawGlyph(
        canvas: Canvas,
        kind: StatusKind,
        cx: Float,
        cy: Float,
        size: Float,
        color: Int,
        style: WidgetStyle,
    ) {
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            this.style = Paint.Style.STROKE
            strokeWidth = max(2f, size * 0.085f)
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            if (style == WidgetStyle.TRANSPARENT) {
                setShadowLayer(4f, 0f, 2f, 0x99000000.toInt())
            }
        }
        when (kind) {
            StatusKind.BLUETOOTH -> {
                val top = cy - size / 2f
                val bottom = cy + size / 2f
                val mid = cy
                val right = cx + size * 0.30f
                canvas.drawLine(cx, top, cx, bottom, stroke)
                canvas.drawLine(cx, top, right, top + size * 0.22f, stroke)
                canvas.drawLine(right, top + size * 0.22f, cx, mid, stroke)
                canvas.drawLine(cx, bottom, right, bottom - size * 0.22f, stroke)
                canvas.drawLine(right, bottom - size * 0.22f, cx, mid, stroke)
            }
            StatusKind.WIFI -> {
                val centerY = cy + size * 0.30f
                for (radius in floatArrayOf(size * 0.52f, size * 0.36f, size * 0.20f)) {
                    canvas.drawArc(
                        cx - radius,
                        centerY - radius,
                        cx + radius,
                        centerY + radius,
                        225f,
                        90f,
                        false,
                        stroke,
                    )
                }
                val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = color
                    if (style == WidgetStyle.TRANSPARENT) {
                        setShadowLayer(4f, 0f, 2f, 0x99000000.toInt())
                    }
                }
                canvas.drawCircle(cx, centerY, max(2f, size * 0.055f), dot)
            }
        }
    }

    private fun drawCentered(
        canvas: Canvas,
        text: String,
        cx: Float,
        baseline: Float,
        size: Float,
        color: Int,
        maxWidth: Float,
        medium: Boolean,
        style: WidgetStyle,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.color = color
            textSize = size
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(
                if (medium) "sans-serif-medium" else "sans-serif",
                Typeface.NORMAL,
            )
            if (style == WidgetStyle.TRANSPARENT) {
                setShadowLayer(5f, 0f, 2f, 0x99000000.toInt())
            }
        }
        canvas.drawText(ellipsize(paint, text, maxWidth), cx, baseline, paint)
    }

    private fun ellipsize(paint: Paint, text: String, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.substring(0, end) + "\u2026") > maxWidth) {
            end--
        }
        return text.substring(0, end) + "\u2026"
    }
}

abstract class BaseStatusWidgetProvider : BaseWidgetProvider() {
    override val layoutRes: Int = R.layout.widget_status_tile
    protected abstract fun readStatus(context: Context): StatusSnapshot
    protected abstract val contentDescriptionRes: Int

    override fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val style = WidgetPrefs.style(context)
        val snapshot = readStatus(context)
        return buildBitmapViews(
            context,
            appWidgetId,
            R.id.status_tile_bitmap,
            R.id.status_tile_fallback,
            render = { widthPx, heightPx ->
                StatusTileRenderer.render(context, widthPx, heightPx, style, snapshot)
            },
        ) decorate@{ views, usedBitmap ->
            views.setTextColor(R.id.status_tile_fallback, UpstreamNtDraw.ink(context, style))
            views.setTextViewText(
                R.id.status_tile_fallback,
                context.getString(
                    R.string.status_tile_fallback_format,
                    snapshot.label(context),
                    snapshot.stateText(context),
                ),
            )
            if (usedBitmap) {
                views.setContentDescription(
                    R.id.status_tile_bitmap,
                    context.getString(contentDescriptionRes, snapshot.stateText(context)),
                )
            }
        }
    }
}

class BluetoothStatusWidgetProvider : BaseStatusWidgetProvider() {
    override val tapAction: String = Settings.ACTION_BLUETOOTH_SETTINGS
    override val contentDescriptionRes: Int = R.string.bt_status_content_description

    override fun readStatus(context: Context): StatusSnapshot =
        BluetoothStatusState.read(context)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothDevice.ACTION_ACL_DISCONNECTED,
            android.bluetooth.BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED,
            android.bluetooth.BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
            -> {
                refreshWidgets(context)
                return
            }
        }
        super.onReceive(context, intent)
    }
}

class WifiStatusWidgetProvider : BaseStatusWidgetProvider() {
    override val contentDescriptionRes: Int = R.string.wifi_status_content_description

    override fun readStatus(context: Context): StatusSnapshot =
        WifiStatusState.read(context)

    override fun tapIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val panel = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (context.packageManager.resolveActivity(panel, 0) != null) return panel
        }
        return Intent(Settings.ACTION_WIFI_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
