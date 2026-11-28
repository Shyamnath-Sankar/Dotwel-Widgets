package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppSettings
import com.example.model.LiveSnapshot
import com.example.ui.theme.AppSurfaceMode
import com.example.ui.theme.DotMatrixFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalDotGridColors
import com.example.widget.SystemControlsLauncher
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: AppSettings,
    snapshot: LiveSnapshot,
    onSurfaceModeChange: (AppSurfaceMode) -> Unit,
    onAutoNightHoursChange: (Int, Int) -> Unit,
    onUse24HourChange: (Boolean) -> Unit,
    onRefreshIntervalChange: (Int) -> Unit,
    onUltraEcoChange: (Boolean) -> Unit,
    onRefreshHomeWidgets: () -> Unit,
    onReplayOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDotGridColors.current
    val context = LocalContext.current
    val tel = snapshot.telemetry

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ext.canvas)
            .cosmicDotBackdrop(ext.dotMatrix, ext.isLightMode),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 126.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "settings_header") {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "POWER & ECO",
                        fontFamily = DotMatrixFontFamily,
                        fontSize = 24.sp,
                        color = ext.ink
                    )
                    Spacer(Modifier.width(7.dp))
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(ext.redAccent)
                    )
                }
                Text(
                    text = "ZERO-NETWORK OFFLINE · PLAY-COMPLIANT WIDGET ENGINE",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = ext.mutedInk
                )
            }
        }

        // 1. Low-Battery Footprint & Widget Refresh Engine
        item(key = "battery_footprint") {
            SettingsCard(title = "LOW-BATTERY FOOTPRINT & WORKMANAGER") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (settings.ultraEcoMode) "ULTRA-ECO ACTIVE" else "STANDARD ECO",
                            fontFamily = DotMatrixFontFamily,
                            fontSize = 20.sp,
                            color = ext.ink
                        )
                        Text(
                            text = "ZERO NETWORK CALLS · 100% ON-DEVICE",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            color = ext.mutedInk
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.BatterySaver,
                        contentDescription = null,
                        tint = ext.redAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ext.pillSurface)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("BATTERY", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = ext.mutedInk)
                        Text(
                            text = "${tel.batteryPercent}% ${if (tel.isCharging) "CHG" else tel.batteryHealthLabel}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = ext.ink
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("NETWORK", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = ext.mutedInk)
                        Text(
                            text = if (tel.isConnected) tel.networkType else "OFFLINE",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = ext.redAccent
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("HOME WIDGET REFRESH CADENCE", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = ext.mutedInk)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30 to "30M", 60 to "1H", 180 to "3H", 0 to "MANUAL").forEach { (mins, lbl) ->
                        val active = settings.refreshIntervalMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (active) ext.ink else ext.pillSurface)
                                .clickable { onRefreshIntervalChange(mins) }
                                .padding(vertical = 10.dp)
                                .testTag("refresh_interval_$mins"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = lbl,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = if (active) ext.canvas else ext.ink
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Ultra-Eco Mode", style = MaterialTheme.typography.bodyLarge, color = ext.ink)
                        Text(
                            text = "Pauses background widget refreshes whenever Android Battery Saver is active or battery is low.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ext.mutedInk
                        )
                    }
                    Switch(
                        checked = settings.ultraEcoMode,
                        onCheckedChange = onUltraEcoChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = ext.canvas, checkedTrackColor = ext.ink),
                        modifier = Modifier.testTag("switch_ultra_eco")
                    )
                }

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onRefreshHomeWidgets,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ext.ink, contentColor = ext.canvas),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("refresh_widgets_now_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.cd_refresh_widgets),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("REFRESH HOME WIDGETS NOW", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                }
            }
        }

        // 2. Live Connectivity & Earphones/Buds Quick Panels
        item(key = "connectivity_hub") {
            SettingsCard(title = "CONNECTIVITY & EARPHONES (BUDS) PANELS") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickHardwareTile(
                        icon = Icons.Default.Wifi,
                        label = "WI-FI",
                        status = if (tel.wifiEnabled) "ON" else "OFF",
                        active = tel.wifiEnabled,
                        onClick = { SystemControlsLauncher.openWifiPanel(context) },
                        tag = "quick_wifi_panel",
                        modifier = Modifier.weight(1f)
                    )
                    QuickHardwareTile(
                        icon = Icons.Default.Bluetooth,
                        label = "BT",
                        status = if (tel.bluetoothEnabled) "ON" else "OFF",
                        active = tel.bluetoothEnabled,
                        onClick = { SystemControlsLauncher.openBluetoothSettings(context) },
                        tag = "quick_bt_panel",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickHardwareTile(
                        icon = Icons.Default.Public,
                        label = "INTERNET",
                        status = if (tel.isConnected) tel.networkType else "OFFLINE",
                        active = tel.isConnected,
                        onClick = { SystemControlsLauncher.openInternetPanel(context) },
                        tag = "quick_internet_panel",
                        modifier = Modifier.weight(1f)
                    )
                    QuickHardwareTile(
                        icon = Icons.Default.Headphones,
                        label = "BUDS / AUDIO",
                        status = if (tel.budsConnected) tel.budsDeviceName.take(10) else tel.audioOutputLabel,
                        active = tel.budsConnected,
                        onClick = { SystemControlsLauncher.openVolumeAudioPanel(context) },
                        tag = "quick_audio_panel",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Uses official Android Settings Panels & AudioManager to monitor Wi-Fi, Internet, Bluetooth, and Earphones/Buds with zero invasive permissions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.mutedInk
                )
            }
        }

        // 3. Appearance & Auto-Night Schedule
        item(key = "appearance_settings") {
            SettingsCard(title = "THEME, NIGHTING SCHEDULE & CLOCK") {
                Text("APP SURFACE MODE", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = ext.mutedInk)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppSurfaceMode.entries.forEach { mode ->
                        val active = settings.appSurfaceMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (active) ext.ink else ext.pillSurface)
                                .clickable { onSurfaceModeChange(mode) }
                                .padding(vertical = 10.dp)
                                .testTag("settings_theme_${mode.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (mode) {
                                    AppSurfaceMode.OLED_NIGHT -> "OLED"
                                    else -> mode.label.uppercase(Locale.US)
                                },
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                color = if (active) ext.canvas else ext.ink
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("AUTO-NIGHT SCHEDULE", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = ext.mutedInk)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(20 to 6, 21 to 6, 22 to 7, 23 to 7).forEach { (startH, endH) ->
                        val active = settings.autoNightStartHour == startH && settings.autoNightEndHour == endH
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(if (active) ext.ink else ext.pillSurface)
                                .clickable { onAutoNightHoursChange(startH, endH) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${startH}–0${endH}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = if (active) ext.canvas else ext.ink
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("24-Hour Clock Default", style = MaterialTheme.typography.bodyLarge, color = ext.ink)
                    Switch(
                        checked = settings.use24Hour,
                        onCheckedChange = onUse24HourChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = ext.canvas, checkedTrackColor = ext.ink),
                        modifier = Modifier.testTag("settings_24h_switch")
                    )
                }
            }
        }

        // 4. Home Screen Placement & Play Policy Privacy Card
        item(key = "guide_card") {
            SettingsCard(title = "HOME SCREEN PLACEMENT & PRIVACY") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = ext.redAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "ZERO DANGEROUS PERMISSIONS",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = ext.ink
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "1. Tap any of the 56 widgets to customize theme, radius, opacity, dot density, quotes, or pick a photo via Android Photo Picker.\n" +
                        "2. Press PIN TO HOME to add it straight to your Android Home Screen with a live preview.\n" +
                        "3. Or long-press your Home Screen → Widgets → AnyWidgets (2×2, 4×2, or 4×4).\n" +
                        "4. Tap the top-right corner of any placed widget to reconfigure it anytime.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ext.mutedInk
                )
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onReplayOnboarding,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("replay_onboarding_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = ext.redAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "REPLAY ONBOARDING ANIMATION",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = ext.ink
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickHardwareTile(
    icon: ImageVector,
    label: String,
    status: String,
    active: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    val ext = LocalDotGridColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) ext.ink else ext.pillSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) ext.redAccent else ext.mutedInk,
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 8.sp,
                color = if (active) ext.canvas.copy(alpha = 0.72f) else ext.mutedInk
            )
            Text(
                text = status.uppercase(Locale.US),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = if (active) ext.canvas else ext.ink,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    val ext = LocalDotGridColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(ext.cardSurface)
            .border(1.dp, ext.hairline, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(ext.redAccent)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = title,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = ext.mutedInk
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}
