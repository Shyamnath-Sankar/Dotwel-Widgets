package com.example.ui.widgets

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.telemetry.AstronomyCalculator
import com.example.model.LiveSnapshot
import com.example.model.WidgetCatalog
import com.example.model.WidgetConfig
import com.example.ui.theme.DotMatrixFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RenderNightAndTimeWidget(
    config: WidgetConfig,
    snapshot: LiveSnapshot,
    colors: ResolvedWidgetColors,
    compactPreview: Boolean
) {
    val cal = Calendar.getInstance().apply { timeInMillis = snapshot.nowMillis }
    val hour24 = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)
    val timeText = AstronomyCalculator.formatTime(snapshot.nowMillis, config.use24Hour)
    val amPm = AstronomyCalculator.formatAmPm(snapshot.nowMillis)
    val dateUpper = SimpleDateFormat("EEE · dd MMM", Locale.US).format(Date(snapshot.nowMillis)).uppercase(Locale.US)

    when (config.widgetTypeId) {
        // 1. Nightstand Dot Clock
        "night_bedside_clock" -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.accent)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (colors.isOledNight) "NIGHTING" else "BEDSIDE",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            letterSpacing = 1.2.sp,
                            color = colors.mutedInk
                        )
                    }
                    Text(
                        text = if (config.use24Hour) "24H" else amPm,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = colors.accent
                    )
                }
                Text(
                    text = timeText,
                    fontFamily = DotMatrixFontFamily,
                    fontSize = if (compactPreview) 34.sp else 46.sp,
                    color = colors.ink,
                    maxLines = 1
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateUpper,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = colors.mutedInk
                    )
                    Text(
                        text = "BAT ${snapshot.telemetry.batteryPercent}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = colors.mutedInk
                    )
                }
            }
        }

        // 2. Lunar Phase Matrix
        "night_lunar_matrix" -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "LUNAR",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = colors.mutedInk
                    )
                    Text(
                        text = "${snapshot.lunar.illuminationPercent}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = colors.accent
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    DotMoonSphere(
                        phaseFraction = snapshot.lunar.phaseFraction,
                        litColor = colors.ink,
                        darkColor = colors.dotInactive,
                        accentColor = colors.accent,
                        modifier = Modifier.size(if (compactPreview) 62.dp else 82.dp)
                    )
                }
                Text(
                    text = snapshot.lunar.phaseName,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 3. Nocturnal Horizon Arc (On-Device Auto-Night Schedule)
        "night_solar_horizon" -> {
            val minuteOfDay = hour24 * 60 + minute
            val sr = snapshot.settings.autoNightEndHour * 60
            val ss = snapshot.settings.autoNightStartHour * 60
            val daySpan = (ss - sr).coerceAtLeast(1)
            val solarFraction = ((minuteOfDay - sr).toFloat() / daySpan.toFloat()).coerceIn(0f, 1f)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("HORIZON", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Text(
                        text = if (minuteOfDay in sr..ss) "DAYLIGHT" else "NOCTURNAL",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = colors.accent
                    )
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (compactPreview) 44.dp else 56.dp)
                ) {
                    val yBase = size.height * 0.82f
                    val arcH = size.height * 0.65f
                    drawLine(colors.border, Offset(0f, yBase), Offset(size.width, yBase), 1.2.dp.toPx())
                    val pts = 18
                    for (i in 0..pts) {
                        val t = i.toFloat() / pts
                        val x = size.width * 0.06f + size.width * 0.88f * t
                        val y = yBase - sin(t * PI).toFloat() * arcH
                        val passed = t <= solarFraction
                        drawCircle(
                            color = if (passed) colors.ink else colors.dotInactive,
                            radius = 2.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                    val sx = size.width * 0.06f + size.width * 0.88f * solarFraction
                    val sy = yBase - sin(solarFraction * PI).toFloat() * arcH
                    drawCircle(color = colors.accent, radius = 4.5.dp.toPx(), center = Offset(sx, sy))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(String.format(Locale.US, "DAWN %02d:00", snapshot.settings.autoNightEndHour), fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = colors.ink)
                    Text(String.format(Locale.US, "NIGHT %02d:00", snapshot.settings.autoNightStartHour), fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = colors.mutedInk)
                }
            }
        }

        // 4. Sleep Window Cycles
        "night_sleep_cycles" -> {
            val bedStr = config.customTitle.ifBlank { "23:00" }
            val cycles = config.goalValue.coerceIn(3, 6)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("SLEEP WINDOW", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Text("${cycles}×90M", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.accent)
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = bedStr,
                        fontFamily = DotMatrixFontFamily,
                        fontSize = if (compactPreview) 26.sp else 32.sp,
                        color = colors.ink
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "BEDTIME",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = colors.mutedInk,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
                DotProgressBar(
                    progress = cycles / 6f,
                    segments = 6,
                    activeColor = colors.ink,
                    inactiveColor = colors.dotInactive,
                    accentTail = colors.accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                )
            }
        }

        // 5. Night Dimmer Mantra
        "night_dimmer_mantra" -> {
            val mantras = WidgetCatalog.nightMantras
            val mantra = mantras[config.currentValue.mod(mantras.size)]
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.accent))
                    Spacer(Modifier.width(6.dp))
                    Text("NIGHT MANTRA", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                }
                Text(
                    text = mantra,
                    fontFamily = DotMatrixFontFamily,
                    fontSize = if (compactPreview) 15.sp else 19.sp,
                    lineHeight = if (compactPreview) 20.sp else 24.sp,
                    color = colors.ink,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "TAP TO CYCLE",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 8.sp,
                    color = colors.mutedInk
                )
            }
        }

        // 6. Circadian 24h Ribbon
        "night_circadian_ribbon" -> {
            val pct = (hour24 * 60 + minute) / 1440f
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("CIRCADIAN 24H", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Text(timeText, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = colors.ink)
                }
                DotProgressBar(
                    progress = pct,
                    segments = 24,
                    activeColor = colors.ink,
                    inactiveColor = colors.dotInactive,
                    accentTail = colors.accent,
                    modifier = Modifier.fillMaxWidth().height(16.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("00", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = colors.mutedInk)
                    Text("06", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = colors.mutedInk)
                    Text("12", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = colors.ink)
                    Text("18", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = colors.mutedInk)
                    Text("24", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = colors.mutedInk)
                }
            }
        }

        // 7. Starlight Red-Pip Dial
        "night_starlight_analog" -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DotAnalogClockFace(
                    hour = hour24,
                    minute = minute,
                    second = second,
                    starlightMinimal = true,
                    colors = colors,
                    modifier = Modifier.fillMaxSize().padding(2.dp)
                )
            }
        }

        // 8. Quiet Hours Status
        "night_quiet_hours" -> {
            val isNight = AstronomyCalculator.isNightTimeNow(snapshot.nowMillis, snapshot.settings)
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("QUIET", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isNight) colors.accent else colors.dotInactive)
                    )
                }
                Text(
                    text = if (isNight) "ACTIVE" else "STANDBY",
                    fontFamily = DotMatrixFontFamily,
                    fontSize = if (compactPreview) 20.sp else 24.sp,
                    color = colors.ink
                )
                Text(
                    text = "${snapshot.settings.autoNightStartHour}:00–0${snapshot.settings.autoNightEndHour}:00",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = colors.mutedInk
                )
            }
        }

        // 9. Matrix Digital Clock
        "time_matrix_digital" -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text(dateUpper, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = colors.mutedInk)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeText,
                        fontFamily = DotMatrixFontFamily,
                        fontSize = if (compactPreview) 34.sp else 44.sp,
                        color = colors.ink,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = String.format(Locale.US, ":%02d", second),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 13.sp,
                        color = colors.accent,
                        modifier = Modifier.padding(bottom = 7.dp)
                    )
                }
                DotProgressBar(
                    progress = second / 60f,
                    segments = 15,
                    activeColor = colors.ink,
                    inactiveColor = colors.dotInactive,
                    accentTail = colors.accent,
                    modifier = Modifier.fillMaxWidth().height(10.dp)
                )
            }
        }

        // 10. 60-Dot Analog Dial
        "time_analog_60dot" -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DotAnalogClockFace(
                    hour = hour24,
                    minute = minute,
                    second = second,
                    starlightMinimal = false,
                    colors = colors,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 11. Split-Flap Dot Clock
        "time_split_flap" -> {
            val parts = timeText.split(":")
            val hh = parts.getOrElse(0) { "12" }.padStart(2, '0')
            val mm = parts.getOrElse(1) { "00" }.padStart(2, '0')
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(hh, mm).forEachIndexed { idx, tile ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tile,
                            fontFamily = DotMatrixFontFamily,
                            fontSize = if (compactPreview) 30.sp else 38.sp,
                            color = if (idx == 1 && !compactPreview) colors.ink else colors.ink
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.surface)
                        )
                        if (idx == 1) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                            )
                        }
                    }
                }
            }
        }

        // 12. Dual World Clock
        "time_dual_world" -> {
            val tzId = config.timezoneId.ifBlank { "Asia/Tokyo" }
            val secondCity = WidgetCatalog.curatedTimezones.firstOrNull { it.first == tzId }?.second
                ?: tzId.substringAfterLast("/").replace("_", " ")
            val remoteTime = AstronomyCalculator.formatTime(snapshot.nowMillis, config.use24Hour, tzId)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text("LOCAL", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.accent)
                    Spacer(Modifier.height(4.dp))
                    Text(timeText, fontFamily = DotMatrixFontFamily, fontSize = if (compactPreview) 24.sp else 30.sp, color = colors.ink)
                }
                Box(modifier = Modifier.width(1.dp).height(46.dp).background(colors.border))
                Column(
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(secondCity.uppercase(Locale.US), fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(remoteTime, fontFamily = DotMatrixFontFamily, fontSize = if (compactPreview) 24.sp else 30.sp, color = colors.ink)
                }
            }
        }

        // 13. Quad Global Matrix
        "time_quad_global" -> {
            val zones = listOf(
                "NYC" to "America/New_York",
                "LON" to "Europe/London",
                "TYO" to "Asia/Tokyo",
                "SYD" to "Australia/Sydney"
            )
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                zones.chunked(2).forEachIndexed { rowIdx, pair ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        pair.forEachIndexed { colIdx, (code, tz) ->
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = code,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    color = if (rowIdx == 0 && colIdx == 0) colors.accent else colors.mutedInk
                                )
                                Text(
                                    text = AstronomyCalculator.formatTime(snapshot.nowMillis, config.use24Hour, tz),
                                    fontFamily = DotMatrixFontFamily,
                                    fontSize = if (compactPreview) 16.sp else 22.sp,
                                    color = colors.ink
                                )
                            }
                        }
                    }
                }
            }
        }

        // 14. Binary BCD Dot Clock
        "time_binary_bcd" -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("BCD", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Text(timeText, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.accent)
                }
                DotBinaryClockCanvas(
                    hour = hour24,
                    minute = minute,
                    second = second,
                    colors = colors,
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 4.dp)
                )
            }
        }

        // 15. Typographic Word Clock
        "time_word_clock" -> {
            val (line1, line2) = AstronomyCalculator.timeInWords(snapshot.nowMillis)
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text("IT IS", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.accent)
                Column {
                    Text(
                        text = line1,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compactPreview) 14.sp else 18.sp,
                        color = colors.mutedInk,
                        maxLines = 1
                    )
                    Text(
                        text = line2,
                        fontFamily = DotMatrixFontFamily,
                        fontSize = if (compactPreview) 20.sp else 26.sp,
                        color = colors.ink,
                        maxLines = 1
                    )
                }
                Text(timeText, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
            }
        }

        // 16. Epoch & UTC Telemetry
        else -> {
            val epochSec = snapshot.nowMillis / 1000L
            val utcStr = AstronomyCalculator.formatTime(snapshot.nowMillis, true, "UTC")
            val week = cal.get(Calendar.WEEK_OF_YEAR)
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("UNIX EPOCH", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Text("W$week · UTC $utcStr", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.accent)
                }
                Text(
                    text = epochSec.toString(),
                    fontFamily = DotMatrixFontFamily,
                    fontSize = if (compactPreview) 20.sp else 26.sp,
                    color = colors.ink,
                    maxLines = 1
                )
                Text(
                    text = TimeZone.getDefault().id.uppercase(Locale.US),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    color = colors.mutedInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
