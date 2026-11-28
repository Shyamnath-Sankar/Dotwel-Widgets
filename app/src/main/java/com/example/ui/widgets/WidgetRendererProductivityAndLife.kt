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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun RenderProductivityCalendarWellnessWidget(
    config: WidgetConfig,
    snapshot: LiveSnapshot,
    colors: ResolvedWidgetColors,
    compactPreview: Boolean
) {
    val cal = Calendar.getInstance().apply { timeInMillis = snapshot.nowMillis }
    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
    val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
    val totalDaysInYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
    val monthShort = SimpleDateFormat("MMM", Locale.US).format(Date(snapshot.nowMillis)).uppercase(Locale.US)
    val weekdayShort = SimpleDateFormat("EEE", Locale.US).format(Date(snapshot.nowMillis)).uppercase(Locale.US)

    when (config.widgetTypeId) {
        // 33. Pomodoro Focus Ring
        "focus_pomodoro_ring" -> {
            val totalSec = (config.goalValue.coerceAtLeast(5) * 60)
            val remainingSec = if (config.isTimerRunning) {
                val elapsed = ((snapshot.nowMillis - config.timerStartedAtMillis) / 1000L).toInt().coerceAtLeast(0)
                (config.timerDurationSec - elapsed).coerceAtLeast(0)
            } else {
                config.timerDurationSec.coerceAtLeast(0)
            }
            val mm = remainingSec / 60
            val ss = remainingSec % 60
            val prog = 1f - (remainingSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DotProgressRing(
                    progress = if (config.isTimerRunning) prog.coerceAtLeast(0.05f) else 1f - (remainingSec.toFloat() / totalSec.toFloat()),
                    dotCount = 24,
                    activeColor = colors.ink,
                    inactiveColor = colors.dotInactive,
                    accentHeadColor = colors.accent,
                    modifier = Modifier.fillMaxSize().padding(2.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", mm, ss),
                        fontFamily = DotMatrixFontFamily,
                        fontSize = if (compactPreview) 20.sp else 26.sp,
                        color = colors.ink
                    )
                    Text(
                        text = if (config.isTimerRunning) "RUNNING" else "TAP START",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 8.sp,
                        color = if (config.isTimerRunning) colors.accent else colors.mutedInk
                    )
                }
            }
        }

        // 34. Event Countdown
        "focus_event_countdown" -> {
            val diffMs = config.targetEpochMillis - snapshot.nowMillis
            val days = (abs(diffMs) / 86_400_000L).toInt()
            val hrs = ((abs(diffMs) % 86_400_000L) / 3_600_000L).toInt()
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = config.customTitle.ifBlank { "LAUNCH DAY" }.uppercase(Locale.US),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    color = colors.mutedInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${days}D",
                        fontFamily = DotMatrixFontFamily,
                        fontSize = if (compactPreview) 30.sp else 38.sp,
                        color = colors.ink
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${hrs}H",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        color = colors.accent,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Text(
                    text = if (diffMs >= 0) "REMAINING" else "DAYS AGO",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 8.sp,
                    color = colors.mutedInk
                )
            }
        }

        // 35. Year Progress Matrix
        "focus_year_365" -> {
            val pct = ((dayOfYear.toFloat() / totalDaysInYear) * 100f).roundToInt()
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${cal.get(Calendar.YEAR)} MATRIX", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Text("$pct%", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = colors.accent)
                }
                DotYearMatrixCanvas(
                    dayOfYear = dayOfYear,
                    totalDays = totalDaysInYear,
                    colors = colors,
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 4.dp)
                )
            }
        }

        // 36. Day Progress Bar
        "focus_day_progress" -> {
            val minOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val pct = minOfDay / 1440f
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("DAY PROGRESS", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Text("${(pct * 100).roundToInt()}%", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = colors.accent)
                }
                DotProgressBar(
                    progress = pct,
                    segments = 20,
                    activeColor = colors.ink,
                    inactiveColor = colors.dotInactive,
                    accentTail = colors.accent,
                    modifier = Modifier.fillMaxWidth().height(16.dp)
                )
                Text("${24 - cal.get(Calendar.HOUR_OF_DAY)}H LEFT TODAY", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
            }
        }

        // 37. Quick 3-Task Checklist
        "focus_quick_checklist" -> {
            val items = config.customNote
                .split("|")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .ifEmpty { listOf("Ship release build", "Review tokens", "Evening run") }
                .take(3)
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(config.customTitle.ifBlank { "TODAY" }.uppercase(Locale.US), fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    val doneCount = (0 until items.size).count { ((config.bitmaskState shr it) and 1) == 1 }
                    Text("$doneCount/${items.size}", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.accent)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items.forEachIndexed { idx, task ->
                        val done = ((config.bitmaskState shr idx) and 1) == 1
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (done) colors.accent else colors.dotInactive)
                            )
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = task,
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = if (compactPreview) 11.sp else 13.sp,
                                color = if (done) colors.mutedInk else colors.ink,
                                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 38. Sticky Dot Note
        "focus_sticky_note" -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.accent))
                    Spacer(Modifier.width(6.dp))
                    Text(config.customTitle.ifBlank { "MEMO" }.uppercase(Locale.US), fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                }
                Text(
                    text = config.customNote.ifBlank { "Less noise. More signal." },
                    fontFamily = SpaceGroteskFontFamily,
                    fontSize = if (compactPreview) 13.sp else 16.sp,
                    lineHeight = if (compactPreview) 17.sp else 21.sp,
                    color = colors.ink,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 39. Next Up Agenda Card
        "focus_next_up" -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("NEXT UP", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.accent)
                    Text(weekdayShort, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                }
                Text(
                    text = config.customTitle.ifBlank { "DESIGN SYNC" }.uppercase(Locale.US),
                    fontFamily = DotMatrixFontFamily,
                    fontSize = if (compactPreview) 18.sp else 23.sp,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = config.customNote.ifBlank { "15:30 • STUDIO B" },
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = colors.mutedInk,
                    maxLines = 1
                )
            }
        }

        // 40. Tap Stopwatch Matrix
        "focus_stopwatch" -> {
            val totalSec = config.currentValue + if (config.isTimerRunning) {
                ((snapshot.nowMillis - config.timerStartedAtMillis) / 1000L).toInt().coerceAtLeast(0)
            } else 0
            val mm = totalSec / 60
            val ss = totalSec % 60
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("STOPWATCH", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (config.isTimerRunning) colors.accent else colors.dotInactive))
                }
                Text(
                    text = String.format(Locale.US, "%02d:%02d", mm, ss),
                    fontFamily = DotMatrixFontFamily,
                    fontSize = if (compactPreview) 26.sp else 34.sp,
                    color = colors.ink
                )
                Text(
                    text = if (config.isTimerRunning) "TAP PAUSE" else "TAP START",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 8.sp,
                    color = colors.mutedInk
                )
            }
        }

        // 41. Bold Date Tile
        "cal_bold_date" -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(weekdayShort, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = colors.accent)
                    Text(monthShort, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = colors.mutedInk)
                }
                Text(
                    text = String.format(Locale.US, "%02d", dayOfMonth),
                    fontFamily = DotMatrixFontFamily,
                    fontSize = if (compactPreview) 40.sp else 54.sp,
                    color = colors.ink
                )
            }
        }

        // 42. Monthly Dot Calendar
        "cal_month_grid" -> {
            val tempCal = Calendar.getInstance().apply {
                timeInMillis = snapshot.nowMillis
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val firstDow = (tempCal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("$monthShort ${cal.get(Calendar.YEAR)}", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = colors.ink)
                    Text("D$dayOfMonth", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.accent)
                }
                val headers = listOf("M", "T", "W", "T", "F", "S", "S")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    headers.forEach { h ->
                        Text(h, fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = colors.mutedInk)
                    }
                }
                val totalCells = firstDow + maxDays
                val rows = ((totalCells + 6) / 7).coerceAtMost(6)
                for (r in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (c in 0 until 7) {
                            val d = r * 7 + c - firstDow + 1
                            if (d in 1..maxDays) {
                                val isToday = d == dayOfMonth
                                Box(
                                    modifier = Modifier
                                        .size(if (compactPreview) 15.dp else 20.dp)
                                        .clip(CircleShape)
                                        .background(if (isToday) colors.accent else androidx.compose.ui.graphics.Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = d.toString(),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = if (compactPreview) 7.sp else 9.sp,
                                        color = if (isToday) colors.surface else colors.ink
                                    )
                                }
                            } else {
                                Spacer(Modifier.size(if (compactPreview) 15.dp else 20.dp))
                            }
                        }
                    }
                }
            }
        }

        // 43. 7-Day Week Strip
        "cal_week_strip" -> {
            val labels = listOf("M", "T", "W", "T", "F", "S", "S")
            val mondayCal = Calendar.getInstance().apply {
                timeInMillis = snapshot.nowMillis
                val offset = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
                add(Calendar.DAY_OF_MONTH, -offset)
            }
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text("$monthShort · WEEK ${cal.get(Calendar.WEEK_OF_YEAR)}", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (i in 0 until 7) {
                        val dNum = mondayCal.get(Calendar.DAY_OF_MONTH)
                        val isToday = dNum == dayOfMonth
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isToday) colors.accent else colors.surfaceVariant)
                                .padding(horizontal = if (compactPreview) 4.dp else 7.dp, vertical = 6.dp)
                        ) {
                            Text(labels[i], fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = if (isToday) colors.surface else colors.mutedInk)
                            Spacer(Modifier.height(2.dp))
                            Text("$dNum", fontFamily = DotMatrixFontFamily, fontSize = if (compactPreview) 12.sp else 15.sp, color = if (isToday) colors.surface else colors.ink)
                        }
                        mondayCal.add(Calendar.DAY_OF_MONTH, 1)
                    }
                }
            }
        }

        // 44. Day-of-Year Counter
        "cal_day_of_year" -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text("JULIAN DAY", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                Text(
                    text = "$dayOfYear",
                    fontFamily = DotMatrixFontFamily,
                    fontSize = if (compactPreview) 32.sp else 42.sp,
                    color = colors.ink
                )
                Text(
                    text = "${totalDaysInYear - dayOfYear} DAYS LEFT",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    color = colors.accent
                )
            }
        }

        // 45. Quarter Progress Grid
        "cal_quarter_blocks" -> {
            val currentQ = (cal.get(Calendar.MONTH) / 3) + 1
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("QUARTER", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Text("Q$currentQ", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = colors.accent)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (q in 1..4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(if (compactPreview) 34.dp else 44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        q == currentQ -> colors.accent
                                        q < currentQ -> colors.ink
                                        else -> colors.dotInactive
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Q$q",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = if (q <= currentQ) colors.surface else colors.mutedInk
                            )
                        }
                    }
                }
            }
        }

        // 46. Monthly Milestone Ring
        "cal_milestone_ring" -> {
            val targetDay = config.goalValue.coerceIn(1, 28)
            val maxMonthDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val daysUntil = if (targetDay >= dayOfMonth) targetDay - dayOfMonth else (maxMonthDays - dayOfMonth) + targetDay
            val prog = 1f - (daysUntil.toFloat() / maxMonthDays.toFloat()).coerceIn(0f, 1f)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DotProgressRing(
                    progress = prog,
                    dotCount = 20,
                    activeColor = colors.ink,
                    inactiveColor = colors.dotInactive,
                    accentHeadColor = colors.accent,
                    modifier = Modifier.fillMaxSize().padding(2.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${daysUntil}D", fontFamily = DotMatrixFontFamily, fontSize = if (compactPreview) 22.sp else 28.sp, color = colors.ink)
                    Text(config.customTitle.ifBlank { "RESET" }.take(8).uppercase(Locale.US), fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = colors.mutedInk)
                }
            }
        }

        // 47. Workweek Dot Bar
        "cal_workweek_bar" -> {
            val dowMon0 = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            val progress = ((dowMon0 + 1).coerceAtMost(5)) / 5f
            val isWeekend = dowMon0 >= 5
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("WORKWEEK", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Text(if (isWeekend) "WEEKEND" else "DAY ${dowMon0 + 1}/5", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.accent)
                }
                DotProgressBar(
                    progress = progress,
                    segments = 5,
                    activeColor = colors.ink,
                    inactiveColor = colors.dotInactive,
                    accentTail = colors.accent,
                    modifier = Modifier.fillMaxWidth().height(18.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("MON", "TUE", "WED", "THU", "FRI").forEach {
                        Text(it, fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = colors.mutedInk)
                    }
                }
            }
        }

        // 48. ISO Week & Lunar Pair
        "cal_iso_lunar_pair" -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text("ISO WEEK", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                Text("W${cal.get(Calendar.WEEK_OF_YEAR)}", fontFamily = DotMatrixFontFamily, fontSize = if (compactPreview) 30.sp else 38.sp, color = colors.ink)
                Text("MOON ${snapshot.lunar.illuminationPercent}%", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.accent)
            }
        }

        // 49. 7-Day Habit Streak
        "well_habit_streak" -> {
            val doneCount = (0 until 7).count { ((config.bitmaskState shr it) and 1) == 1 }
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(config.customTitle.ifBlank { "MORNING WALK" }.uppercase(Locale.US), fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk, maxLines = 1)
                    Text("$doneCount/7", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = colors.accent)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (i in 0 until 7) {
                        val checked = ((config.bitmaskState shr i) and 1) == 1
                        val isToday = i == 6
                        Box(
                            modifier = Modifier
                                .size(if (compactPreview) 18.dp else 24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        checked && isToday -> colors.accent
                                        checked -> colors.ink
                                        else -> colors.dotInactive
                                    }
                                )
                        )
                    }
                }
                Text("TAP TO TOGGLE TODAY", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = colors.mutedInk)
            }
        }

        // 50. Hydration 8-Dot Counter
        "well_hydration_dots" -> {
            val goal = config.goalValue.coerceIn(4, 12)
            val count = config.currentValue.coerceIn(0, goal)
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(config.customTitle.ifBlank { "WATER" }.uppercase(Locale.US), fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Text("$count/$goal", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = colors.accent)
                }
                DotProgressBar(
                    progress = count.toFloat() / goal.toFloat(),
                    segments = goal,
                    activeColor = colors.ink,
                    inactiveColor = colors.dotInactive,
                    accentTail = colors.accent,
                    modifier = Modifier.fillMaxWidth().height(16.dp)
                )
                Text("TAP +1 GLASS", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = colors.mutedInk)
            }
        }

        // 51. 4-7-8 Breathing Pacer
        "well_breathing_478" -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("4·7·8", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Text(if (config.isTimerRunning) "PACE" else "CALM", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.accent)
                }
                DotBreathingRingCanvas(
                    isRunning = config.isTimerRunning,
                    colors = colors,
                    modifier = Modifier.size(if (compactPreview) 58.dp else 76.dp)
                )
                Text(
                    text = if (config.isTimerRunning) "INHALE · EXHALE" else "TAP TO BREATHE",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 8.sp,
                    color = colors.mutedInk
                )
            }
        }

        // 52. Minimal Tally Counter
        "well_tally_counter" -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text(config.customTitle.ifBlank { "TALLY" }.uppercase(Locale.US), fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk, maxLines = 1)
                Text(
                    text = String.format(Locale.US, "%02d", config.currentValue),
                    fontFamily = DotMatrixFontFamily,
                    fontSize = if (compactPreview) 36.sp else 46.sp,
                    color = colors.ink
                )
                Text("TAP +1", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.accent)
            }
        }

        // 53. Daily Focus Word
        "well_focus_word" -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.accent))
                    Spacer(Modifier.width(6.dp))
                    Text("INTENTION", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                }
                Text(
                    text = config.customTitle.ifBlank { "CLARITY" }.uppercase(Locale.US),
                    fontFamily = DotMatrixFontFamily,
                    fontSize = if (compactPreview) 24.sp else 34.sp,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("$weekdayShort · $dayOfMonth $monthShort", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
            }
        }

        // 54. 5-Dot Mood Logger
        "well_mood_dots" -> {
            val level = config.currentValue.coerceIn(1, 5)
            val labels = listOf("LOW", "CALM", "STEADY", "FOCUSED", "PEAK")
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text("STATE", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (i in 1..5) {
                        Box(
                            modifier = Modifier
                                .size(if (compactPreview) 14.dp else 18.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        i == level -> colors.accent
                                        i < level -> colors.ink
                                        else -> colors.dotInactive
                                    }
                                )
                        )
                    }
                }
                Text(labels[level - 1], fontFamily = DotMatrixFontFamily, fontSize = if (compactPreview) 16.sp else 20.sp, color = colors.ink)
            }
        }

        // 55. Reading Page Tracker
        "well_reading_tracker" -> {
            val total = config.goalValue.coerceAtLeast(50)
            val cur = config.currentValue.coerceIn(0, total)
            val pct = cur.toFloat() / total.toFloat()
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(config.customTitle.ifBlank { "READING" }.uppercase(Locale.US), fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk, maxLines = 1)
                    Text("${(pct * 100).roundToInt()}%", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.accent)
                }
                Text("P. $cur / $total", fontFamily = DotMatrixFontFamily, fontSize = if (compactPreview) 20.sp else 26.sp, color = colors.ink)
                DotProgressBar(
                    progress = pct,
                    segments = 14,
                    activeColor = colors.ink,
                    inactiveColor = colors.dotInactive,
                    accentTail = colors.accent,
                    modifier = Modifier.fillMaxWidth().height(12.dp)
                )
            }
        }

        // 56. Minimalist Quote Card
        else -> {
            val quotes = WidgetCatalog.designQuotes
            val (quote, author) = quotes[config.currentValue.mod(quotes.size)]
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "\"$quote\"",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = if (compactPreview) 11.sp else 14.sp,
                    lineHeight = if (compactPreview) 15.sp else 19.sp,
                    color = colors.ink,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("— $author", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.accent)
                    Text("TAP", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = colors.mutedInk)
                }
            }
        }
    }
}
