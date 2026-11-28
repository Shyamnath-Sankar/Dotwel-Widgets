package com.example.data.telemetry

import android.app.ActivityManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import com.example.model.DeviceTelemetry

class DeviceTelemetryProvider(private val context: Context) {

    fun readSnapshot(): DeviceTelemetry {
        // 1. Battery telemetry (zero permissions)
        val batteryIntent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPct = if (level >= 0 && scale > 0) ((level * 100) / scale).coerceIn(0, 100) else 82

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val rawTemp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 305) ?: 305
        val tempC = rawTemp / 10f

        val voltageMv = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4100) ?: 4100
        val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)
            ?: BatteryManager.BATTERY_HEALTH_GOOD
        val healthLabel = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "WARM"
            BatteryManager.BATTERY_HEALTH_COLD -> "COLD"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "HIGH V"
            else -> "NOMINAL"
        }

        // 2. Storage via StatFs (zero permissions)
        val (usedGb, totalGb) = runCatching {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.totalBytes.toDouble()
            val freeBytes = stat.availableBytes.toDouble()
            val gb = 1024.0 * 1024.0 * 1024.0
            val total = (totalBytes / gb).toFloat().coerceAtLeast(1f)
            val used = ((totalBytes - freeBytes) / gb).toFloat().coerceIn(0f, total)
            used to total
        }.getOrDefault(42f to 128f)

        // 3. RAM via ActivityManager (zero permissions)
        val (ramUsedGb, ramTotalGb) = runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(mi)
            val gb = 1024.0 * 1024.0 * 1024.0
            val total = (mi.totalMem / gb).toFloat().coerceAtLeast(1f)
            val avail = (mi.availMem / gb).toFloat().coerceIn(0f, total)
            (total - avail).coerceAtLeast(0.1f) to total
        }.getOrDefault(3.6f to 8.0f)

        // 4. Wi-Fi & Internet state (normal install-time ACCESS_WIFI_STATE & ACCESS_NETWORK_STATE only)
        val wifiEnabled = runCatching {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wm?.isWifiEnabled == true
        }.getOrDefault(false)

        var wifiConnected = false
        var cellularConnected = false
        var networkType = "OFFLINE"
        var isConnected = false
        var isMetered = false

        runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork
            val caps = if (network != null) cm.getNetworkCapabilities(network) else null
            if (caps != null) {
                wifiConnected = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                cellularConnected = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                isConnected = true
                isMetered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                networkType = when {
                    wifiConnected -> "WI-FI"
                    cellularConnected -> "5G / LTE"
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                    else -> "ONLINE"
                }
            }
        }

        // 5. Bluetooth radio state (zero dangerous runtime permissions)
        val bluetoothEnabled = runCatching {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bm?.adapter?.isEnabled == true
        }.getOrDefault(false)

        // 6. Earphones / Wireless Buds & Music Audio state via AudioManager (zero permissions!)
        var budsConnected = false
        var budsName = "EAR (BUDS)"
        var audioOutputLabel = "SPEAKER"
        var mediaVolumePercent = 50
        var isMusicPlaying = false

        runCatching {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager != null) {
                isMusicPlaying = audioManager.isMusicActive
                val curVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                mediaVolumePercent = ((curVol * 100) / maxVol).coerceIn(0, 100)

                val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                val wirelessBuds = outputs.firstOrNull { dev ->
                    dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            (dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET || dev.type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
                }
                val wiredHeadset = outputs.firstOrNull { dev ->
                    dev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        dev.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        dev.type == AudioDeviceInfo.TYPE_USB_HEADSET
                }

                when {
                    wirelessBuds != null -> {
                        budsConnected = true
                        audioOutputLabel = "BUDS"
                        val pname = wirelessBuds.productName?.toString()?.trim().orEmpty()
                        if (pname.isNotEmpty()) budsName = pname.uppercase()
                    }
                    wiredHeadset != null -> {
                        budsConnected = true
                        audioOutputLabel = "WIRED"
                        val pname = wiredHeadset.productName?.toString()?.trim().orEmpty()
                        budsName = if (pname.isNotEmpty()) pname.uppercase() else "WIRED EARPHONES"
                    }
                    else -> {
                        budsConnected = false
                        audioOutputLabel = "SPEAKER"
                    }
                }
            }
        }

        // 7. Power Save Mode
        val powerSave = runCatching {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isPowerSaveMode == true
        }.getOrDefault(false)

        return DeviceTelemetry(
            batteryPercent = batteryPct,
            isCharging = isCharging,
            batteryTempC = tempC,
            batteryVoltageMv = voltageMv,
            batteryHealthLabel = healthLabel,
            storageUsedGb = usedGb,
            storageTotalGb = totalGb,
            ramUsedGb = ramUsedGb,
            ramTotalGb = ramTotalGb,
            wifiEnabled = wifiEnabled,
            wifiConnected = wifiConnected,
            cellularConnected = cellularConnected,
            bluetoothEnabled = bluetoothEnabled,
            budsConnected = budsConnected,
            budsDeviceName = budsName,
            audioOutputLabel = audioOutputLabel,
            isMusicPlaying = isMusicPlaying,
            mediaVolumePercent = mediaVolumePercent,
            networkType = networkType,
            isConnected = isConnected,
            isMetered = isMetered,
            uptimeMillis = SystemClock.elapsedRealtime(),
            isPowerSaveMode = powerSave
        )
    }
}
