package com.example.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LiveSnapshot
import com.example.model.WidgetConfig
import com.example.ui.theme.DotMatrixFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun RenderSystemTelemetryWidget(
    config: WidgetConfig,
    snapshot: LiveSnapshot,
    colors: ResolvedWidgetColors,
    compactPreview: Boolean
) {
    val tel = snapshot.telemetry

    when (config.widgetTypeId) {
        // 33. Battery Dot Ring
        "sys_battery_ring" -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DotProgressRing(
                    progress = tel.batteryPercent / 100f,
                    dotCount = 24,
                    activeColor = if (tel.batteryPercent <= 20) colors.accent else colors.ink,
                    inactiveColor = colors.dotInactive,
                    accentHeadColor = colors.accent,
                    modifier = Modifier.fillMaxSize().padding(2.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${tel.batteryPercent}%",
                        fontFamily = DotMatrixFontFamily,
                        fontSize = if (compactPreview) 22.sp else 28.sp,
                        color = colors.ink
                    )
                    Text(
                        text = if (tel.isCharging) "CHARGING" else "BATTERY",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 8.sp,
                        color = if (tel.isCharging) colors.accent else colors.mutedInk
                    )
                }
            }
        }

        // 34. Battery Thermal & Volt
        "sys_battery_thermal" -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("CELL", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Text(tel.batteryHealthLabel, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.accent)
                }
                Text(
                    text = String.format(Locale.US, "%.1f°C", tel.batteryTempC),
                    fontFamily = DotMatrixFontFamily,
                    fontSize = if (compactPreview) 24.sp else 30.sp,
                    color = colors.ink
                )
                Text(
                    text = "${tel.batteryVoltageMv} mV",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = colors.mutedInk
                )
            }
        }

        // 35. Storage Matrix Bar
        "sys_storage_matrix" -> {
            val pct = (tel.storagePercent * 100).roundToInt()
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("STORAGE", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Text("$pct% USED", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = colors.accent)
                }
                Text(
                    text = String.format(Locale.US, "%.1f / %.0f GB", tel.storageUsedGb, tel.storageTotalGb),
                    fontFamily = DotMatrixFontFamily,
                    fontSize = if (compactPreview) 18.sp else 24.sp,
                    color = colors.ink
                )
                DotProgressBar(
                    progress = tel.storagePercent,
                    segments = 16,
                    activeColor = colors.ink,
                    inactiveColor = colors.dotInactive,
                    accentTail = colors.accent,
                    modifier = Modifier.fillMaxWidth().height(12.dp)
                )
            }
        }

        // 36. RAM Memory Gauge
        "sys_ram_gauge" -> {
            val pct = (tel.ramPercent * 100).roundToInt()
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("RAM", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Text("$pct%", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = colors.accent)
                }
                Text(
                    text = String.format(Locale.US, "%.1fG", tel.ramUsedGb),
                    fontFamily = DotMatrixFontFamily,
                    fontSize = if (compactPreview) 26.sp else 34.sp,
                    color = colors.ink
                )
                DotProgressBar(
                    progress = tel.ramPercent,
                    segments = 10,
                    activeColor = colors.ink,
                    inactiveColor = colors.dotInactive,
                    accentTail = colors.accent,
                    modifier = Modifier.fillMaxWidth().height(10.dp)
                )
            }
        }

        // 37. Network & Link Card
        "sys_network_signal" -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (tel.isConnected) colors.accent else colors.dotInactive)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("LINK", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                }
                Text(
                    text = tel.networkType,
                    fontFamily = DotMatrixFontFamily,
                    fontSize = if (compactPreview) 20.sp else 26.sp,
                    color = colors.ink,
                    maxLines = 1
                )
                Text(
                    text = if (!tel.isConnected) "DISCONNECTED" else if (tel.isMetered) "METERED" else "UNMETERED",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    color = colors.mutedInk
                )
            }
        }

        // 38. System Uptime Matrix
        "sys_uptime_counter" -> {
            val totalMin = (tel.uptimeMillis / 60_000L).coerceAtLeast(1L)
            val hrs = totalMin / 60L
            val mins = totalMin % 60L
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text("UPTIME", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                Text(
                    text = "${hrs}H ${mins}M",
                    fontFamily = DotMatrixFontFamily,
                    fontSize = if (compactPreview) 22.sp else 28.sp,
                    color = colors.ink,
                    maxLines = 1
                )
                Text("SINCE BOOT", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.accent)
            }
        }

        // 39. Trio System Dashboard
        "sys_trio_dashboard" -> {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val items = listOf(
                    Triple("BAT", "${tel.batteryPercent}%", tel.batteryPercent / 100f),
                    Triple("RAM", "${(tel.ramPercent * 100).roundToInt()}%", tel.ramPercent),
                    Triple("DSK", "${(tel.storagePercent * 100).roundToInt()}%", tel.storagePercent)
                )
                items.forEachIndexed { idx, (label, valText, prog) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Box(contentAlignment = Alignment.Center) {
                            DotProgressRing(
                                progress = prog,
                                dotCount = 14,
                                activeColor = colors.ink,
                                inactiveColor = colors.dotInactive,
                                accentHeadColor = if (idx == 0) colors.accent else null,
                                modifier = Modifier.size(if (compactPreview) 44.dp else 56.dp)
                            )
                            Text(valText, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.ink)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(label, fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = colors.mutedInk)
                    }
                }
            }
        }

        // 40. Power Footprint Badge
        else -> {
            val cadence = if (snapshot.settings.refreshIntervalMinutes == 0) "MANUAL" else "${snapshot.settings.refreshIntervalMinutes}M ECO"
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ZERO-NET ECO", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = colors.mutedInk)
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (snapshot.settings.ultraEcoMode) colors.accent else colors.dotInactive))
                }
                Text(
                    text = if (snapshot.settings.ultraEcoMode) "ULTRA" else "NORMAL",
                    fontFamily = DotMatrixFontFamily,
                    fontSize = if (compactPreview) 22.sp else 28.sp,
                    color = colors.ink
                )
                Text(
                    text = if (tel.isPowerSaveMode) "SAVER ON · $cadence" else "OFFLINE · $cadence",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    color = colors.mutedInk,
                    maxLines = 1
                )
            }
        }
    }
}
