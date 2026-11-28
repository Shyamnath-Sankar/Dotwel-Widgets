package com.example.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Play-Policy compliant Android system panel launcher for Wi-Fi, Internet, Bluetooth & Audio/Buds.
 * Uses official Android Settings Panels (API 29+) with standard Settings fallbacks, requiring
 * zero dangerous permissions and respecting Android 10+ security restrictions.
 */
object SystemControlsLauncher {

    fun openWifiPanel(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        launchSafely(context, intent, Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    fun openInternetPanel(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            Intent(Settings.ACTION_WIRELESS_SETTINGS)
        }
        launchSafely(context, intent, Intent(Settings.ACTION_WIRELESS_SETTINGS))
    }

    fun openBluetoothSettings(context: Context) {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        launchSafely(context, intent, Intent(Settings.ACTION_SETTINGS))
    }

    fun openVolumeAudioPanel(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_VOLUME)
        } else {
            Intent(Settings.ACTION_SOUND_SETTINGS)
        }
        launchSafely(context, intent, Intent(Settings.ACTION_SOUND_SETTINGS))
    }

    private fun launchSafely(context: Context, primary: Intent, fallback: Intent) {
        primary.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val launched = runCatching {
            context.startActivity(primary)
            true
        }.getOrDefault(false)
        if (!launched) {
            runCatching { context.startActivity(fallback) }
        }
    }
}
