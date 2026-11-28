package com.example.widget

import android.graphics.Paint
import android.graphics.RectF
import com.example.data.telemetry.AstronomyCalculator
import com.example.model.WidgetCatalog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal fun WidgetDrawCtx.drawGroupB() {
    val tel = snapshot.telemetry
    val hour24 = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)

    when (config.widgetTypeId) {
        // =========================================================================
        // 4. TIME & WORLD CLOCKS (25..32)
        // =========================================================================
        "time_matrix_digital" -> {
            drawTopHeader(dateUpper, if (config.use24Hour) "24H" else AstronomyCalculator.formatAmPm(snapshot.nowMillis), rightAccent = true)
            fitHeroText(timeText, w * 0.72f, if (isWide) 48f else 36f)
            canvas.drawText(timeText, pad, h * 0.62f, heroPaint)
            val secStr = String.format(Locale.US, ":%02d", second)
            val sp = Paint(accentLabelPaint).apply { textSize = 13f * dp }
            canvas.drawText(secStr, pad + heroPaint.measureText(timeText) + 4f * dp, h * 0.61f, sp)
            drawDotBar(h - pad - 4f * dp, second / 60f, 15)
        }

        "time_analog_60dot" -> {
            val cx = w / 2f
            val cy = h / 2f
            val r = minSide * 0.42f
            for (i in 0 until 60) {
                val a = (-90.0 + i * 6.0) * PI / 180.0
                val card = i % 15 == 0
                val hr = i % 5 == 0
                fillPaint.color = when {
                    card -> inkInt
                    hr -> mutedInt
                    else -> dotInt
                }
                val dr = when {
                    card -> 2.8f
                    hr -> 1.8f
                    else -> 1.0f
                } * dp
                canvas.drawCircle(cx + (r * cos(a)).toFloat(), cy + (r * sin(a)).toFloat(), dr, fillPaint)
            }
            val hAng = (-90.0 + ((hour24 % 12) + minute / 60.0) * 30.0) * PI / 180.0
            val mAng = (-90.0 + (minute + second / 60.0) * 6.0) * PI / 180.0
            val sAng = (-90.0 + second * 6.0) * PI / 180.0
            strokePaint.color = inkInt
            strokePaint.strokeWidth = 3.2f * dp
            canvas.drawLine(cx, cy, cx + (r * 0.50f * cos(hAng)).toFloat(), cy + (r * 0.50f * sin(hAng)).toFloat(), strokePaint)
            strokePaint.strokeWidth = 2.0f * dp
            canvas.drawLine(cx, cy, cx + (r * 0.76f * cos(mAng)).toFloat(), cy + (r * 0.76f * sin(mAng)).toFloat(), strokePaint)
            fillPaint.color = accentInt
            canvas.drawCircle(cx + (r * 0.86f * cos(sAng)).toFloat(), cy + (r * 0.86f * sin(sAng)).toFloat(), 3.2f * dp, fillPaint)
            canvas.drawCircle(cx, cy, 3.0f * dp, fillPaint)
        }

        "time_split_flap" -> {
            val parts = timeText.split(":")
            val hh = parts.getOrElse(0) { "12" }.padStart(2, '0')
            val mm = parts.getOrElse(1) { "00" }.padStart(2, '0')
            val gap = 10f * dp
            val tileW = (w - pad * 2 - gap) / 2f
            val topY = pad
            val botY = h - pad
            listOf(hh, mm).forEachIndexed { idx, tile ->
                val left = pad + idx * (tileW + gap)
                val rect = RectF(left, topY, left + tileW, botY)
                fillPaint.color = variantInt
                canvas.drawRoundRect(rect, 14f * dp, 14f * dp, fillPaint)
                fitHeroText(tile, tileW * 0.75f, if (isWide) 52f else 36f)
                val fm = heroPaint.fontMetrics
                val ty = rect.centerY() - (fm.ascent + fm.descent) / 2f
                canvas.drawText(tile, rect.centerX() - heroPaint.measureText(tile) / 2f, ty, heroPaint)
                strokePaint.color = surfaceInt
                strokePaint.strokeWidth = 1.6f * dp
                canvas.drawLine(rect.left, rect.centerY(), rect.right, rect.centerY(), strokePaint)
                if (idx == 1) {
                    fillPaint.color = accentInt
                    canvas.drawCircle(rect.right - 10f * dp, rect.top + 10f * dp, 3.2f * dp, fillPaint)
                }
            }
        }

        "time_dual_world" -> {
            val tzId = config.timezoneId.ifBlank { "Asia/Tokyo" }
            val city = (WidgetCatalog.curatedTimezones.firstOrNull { it.first == tzId }?.second
                ?: tzId.substringAfterLast("/").replace("_", " ")).uppercase(Locale.US)
            val remote = AstronomyCalculator.formatTime(snapshot.nowMillis, config.use24Hour, tzId)
            val midX = w / 2f
            canvas.drawText("LOCAL", pad, pad + 18f * dp, accentLabelPaint)
            fitHeroText(timeText, midX - pad * 1.5f, 34f)
            canvas.drawText(timeText, pad, h * 0.64f, heroPaint)
            strokePaint.color = borderInt
            strokePaint.strokeWidth = 1.2f * dp
            canvas.drawLine(midX, pad + 14f * dp, midX, h - pad - 14f * dp, strokePaint)
            val rx = midX + 14f * dp
            canvas.drawText(city.take(11), rx, pad + 18f * dp, labelPaint)
            canvas.drawText(remote, rx, h * 0.64f, heroPaint)
        }

        "time_quad_global" -> {
            val zones = listOf("NYC" to "America/New_York", "LON" to "Europe/London", "TYO" to "Asia/Tokyo", "SYD" to "Australia/Sydney")
            val colW = (w - pad * 2) / 2f
            val rowH = (h - pad * 2) / 2f
            zones.forEachIndexed { idx, (code, tz) ->
                val col = idx % 2
                val row = idx / 2
                val x = pad + col * colW
                val y = pad + row * rowH
                val t = AstronomyCalculator.formatTime(snapshot.nowMillis, config.use24Hour, tz)
                canvas.drawText(code, x, y + 14f * dp, if (idx == 0) accentLabelPaint else labelPaint)
                fitHeroText(t, colW - 10f * dp, 28f)
                canvas.drawText(t, x, y + 46f * dp, heroPaint)
            }
        }

        "time_binary_bcd" -> {
            drawTopHeader("BCD", timeText, rightAccent = true)
            val digits = intArrayOf(hour24 / 10, hour24 % 10, minute / 10, minute % 10, second / 10, second % 10)
            val topY = pad + 20f * dp
            val boxH = h - topY - pad
            val boxW = w - pad * 2
            val colStep = boxW / 6f
            val rowStep = boxH / 4f
            val dotR = min(colStep, rowStep) * 0.28f
            for (c in 0 until 6) {
                val value = digits[c]
                for (r in 0 until 4) {
                    val isOn = ((value shr (3 - r)) and 1) == 1
                    fillPaint.color = when {
                        isOn && c >= 4 -> accentInt
                        isOn -> inkInt
                        else -> dotInt
                    }
                    canvas.drawCircle(pad + colStep * c + colStep / 2f, topY + rowStep * r + rowStep / 2f, if (isOn) dotR * 1.05f else dotR * 0.72f, fillPaint)
                }
            }
        }

        "time_word_clock" -> {
            val (l1, l2) = AstronomyCalculator.timeInWords(snapshot.nowMillis)
            drawTopHeader("IT IS", "", leftDot = true)
            bodyPaint.color = mutedInt
            bodyPaint.textSize = 14f * dp
            canvas.drawText(l1, pad, h * 0.45f, bodyPaint)
            fitHeroText(l2, w - pad * 2, 24f)
            canvas.drawText(l2, pad, h * 0.67f, heroPaint)
            canvas.drawText(timeText, pad, h - pad, labelPaint)
        }

        "time_epoch_telemetry" -> {
            val utc = AstronomyCalculator.formatTime(snapshot.nowMillis, true, "UTC")
            drawTopHeader("UNIX EPOCH", "W${cal.get(Calendar.WEEK_OF_YEAR)} · UTC $utc", rightAccent = true)
            val ep = (snapshot.nowMillis / 1000L).toString()
            fitHeroText(ep, w - pad * 2, 30f)
            canvas.drawText(ep, pad, h * 0.60f, heroPaint)
            canvas.drawText(TimeZone.getDefault().id.uppercase(Locale.US).take(26), pad, h - pad, labelPaint)
        }

        // =========================================================================
        // 5. TELEMETRY & SYSTEM (33..40)
        // =========================================================================
        "sys_battery_ring" -> {
            val cx = w / 2f
            val cy = h / 2f
            drawDotRing(cx, cy, minSide * 0.40f, tel.batteryPercent / 100f, 24)
            val pct = "${tel.batteryPercent}%"
            fitHeroText(pct, minSide * 0.56f, 26f)
            canvas.drawText(pct, cx - heroPaint.measureText(pct) / 2f, cy + 6f * dp, heroPaint)
            val sub = if (tel.isCharging) "CHARGING" else "BATTERY"
            val sp = if (tel.isCharging) accentLabelPaint else labelPaint
            canvas.drawText(sub, cx - sp.measureText(sub) / 2f, cy + 20f * dp, sp)
        }

        "sys_battery_thermal" -> {
            drawTopHeader("THERMAL", tel.batteryHealthLabel, rightAccent = true)
            val temp = String.format(Locale.US, "%.1f°C", tel.batteryTempC)
            fitHeroText(temp, w - pad * 2, 28f)
            canvas.drawText(temp, pad, h * 0.58f, heroPaint)
            canvas.drawText("${tel.batteryVoltageMv} mV · ${tel.batteryPercent}%", pad, h - pad, labelPaint)
        }

        "sys_storage_matrix" -> {
            drawTopHeader("STORAGE", "${(tel.storagePercent * 100).roundToInt()}%", rightAccent = true)
            val valStr = String.format(Locale.US, "%.0f / %.0f GB", tel.storageUsedGb, tel.storageTotalGb)
            fitHeroText(valStr, w - pad * 2, 28f)
            canvas.drawText(valStr, pad, h * 0.58f, heroPaint)
            drawDotBar(h - pad - 5f * dp, tel.storagePercent, 16)
        }

        "sys_ram_gauge" -> {
            drawTopHeader("RAM", "${(tel.ramPercent * 100).roundToInt()}%", rightAccent = true)
            val valStr = String.format(Locale.US, "%.1fG", tel.ramUsedGb)
            fitHeroText(valStr, w - pad * 2, 28f)
            canvas.drawText(valStr, pad, h * 0.56f, heroPaint)
            drawDotBar(h - pad - 5f * dp, tel.ramPercent, 10)
        }

        "sys_network_signal" -> {
            drawTopHeader("LINK", "")
            drawStatusPipRight(tel.isConnected)
            val main = if (tel.isConnected) tel.networkType else "OFFLINE"
            fitHeroText(main, w - pad * 2, 24f)
            canvas.drawText(main, pad, h * 0.58f, heroPaint)
            canvas.drawText(if (tel.isMetered) "METERED" else "UNMETERED", pad, h - pad, labelPaint)
        }

        "sys_uptime_counter" -> {
            val mins = (tel.uptimeMillis / 60_000L).coerceAtLeast(1L)
            drawTopHeader("UPTIME", "BOOT", rightAccent = true)
            val up = "${mins / 60}H ${mins % 60}M"
            fitHeroText(up, w - pad * 2, 26f)
            canvas.drawText(up, pad, h * 0.58f, heroPaint)
            canvas.drawText("ACTIVE SESSION", pad, h - pad, labelPaint)
        }

        "sys_trio_dashboard" -> {
            drawTopHeader("SYSTEM TRIO", if (tel.isPowerSaveMode) "ECO" else "LIVE", rightAccent = true)
            val cols = listOf(
                Triple("BAT", "${tel.batteryPercent}%", tel.batteryPercent / 100f),
                Triple("STO", "${(tel.storagePercent * 100).roundToInt()}%", tel.storagePercent),
                Triple("RAM", "${(tel.ramPercent * 100).roundToInt()}%", tel.ramPercent)
            )
            val gap = 12f * dp
            val cw = (w - pad * 2 - gap * 2) / 3f
            cols.forEachIndexed { idx, (lbl, txt, prog) ->
                val lx = pad + idx * (cw + gap)
                canvas.drawText(lbl, lx, h * 0.42f, labelPaint)
                fitHeroText(txt, cw, 22f)
                canvas.drawText(txt, lx, h * 0.68f, heroPaint)
                drawDotBar(h - pad - 5f * dp, prog, 6, lx, lx + cw)
            }
        }

        "sys_power_footprint" -> {
            drawTopHeader("ECO ENGINE", if (snapshot.settings.ultraEcoMode) "ULTRA" else "ECO", rightAccent = true, leftDot = true)
            fitHeroText("0 NET", w - pad * 2, 26f)
            canvas.drawText("0 NET", pad, h * 0.58f, heroPaint)
            canvas.drawText("OFFLINE · LOW DRAIN", pad, h - pad, labelPaint)
        }

        // =========================================================================
        // 6. FOCUS, TASKS & CALENDAR (41..48)
        // =========================================================================
        "focus_pomodoro_ring" -> {
            val total = config.goalValue.coerceAtLeast(5) * 60
            val rem = if (config.isTimerRunning) {
                val el = ((snapshot.nowMillis - config.timerStartedAtMillis) / 1000L).toInt().coerceAtLeast(0)
                (config.timerDurationSec - el).coerceAtLeast(0)
            } else config.timerDurationSec
            val prog = 1f - (rem.toFloat() / total.toFloat())
            val cx = w / 2f
            val cy = h / 2f
            drawDotRing(cx, cy, minSide * 0.40f, prog, 24)
            val mmss = String.format(Locale.US, "%02d:%02d", rem / 60, rem % 60)
            fitHeroText(mmss, minSide * 0.56f, 24f)
            canvas.drawText(mmss, cx - heroPaint.measureText(mmss) / 2f, cy + 5f * dp, heroPaint)
            val sub = if (config.isTimerRunning) "RUNNING" else "TAP START"
            canvas.drawText(sub, cx - accentLabelPaint.measureText(sub) / 2f, cy + 19f * dp, accentLabelPaint)
        }

        "focus_event_countdown" -> {
            val days = (abs(config.targetEpochMillis - snapshot.nowMillis) / 86_400_000L).toInt()
            drawTopHeader("COUNTDOWN", "DAYS", rightAccent = true, leftDot = true)
            val dStr = String.format(Locale.US, "%02d", days)
            fitHeroText(dStr, w - pad * 2, 40f)
            canvas.drawText(dStr, pad, h * 0.66f, heroPaint)
            canvas.drawText(config.customTitle.ifBlank { "LAUNCH DAY" }.uppercase(Locale.US).take(16), pad, h - pad, inkLabelPaint)
        }

        "focus_year_365" -> {
            val doy = cal.get(Calendar.DAY_OF_YEAR)
            val totalDays = cal.getActualMaximum(Calendar.DAY_OF_YEAR).coerceAtLeast(365)
            drawTopHeader("YEAR ${cal.get(Calendar.YEAR)}", "${(doy * 100 / totalDays)}%", rightAccent = true)
            val cols = 20
            val rows = 6
            val totalDots = cols * rows
            val litCount = ((doy.toFloat() / totalDays) * totalDots).toInt().coerceIn(1, totalDots)
            val topY = pad + 20f * dp
            val boxH = h - topY - pad - 14f * dp
            val dx = (w - pad * 2) / cols
            val dy = boxH / rows
            val r = min(dx, dy) * 0.28f
            var idx = 0
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    idx++
                    fillPaint.color = when {
                        idx == litCount -> accentInt
                        idx < litCount -> inkInt
                        else -> dotInt
                    }
                    canvas.drawCircle(pad + col * dx + dx / 2f, topY + row * dy + dy / 2f, if (idx == litCount) r * 1.35f else r, fillPaint)
                }
            }
            canvas.drawText("DAY $doy OF $totalDays", pad, h - pad, labelPaint)
        }

        "focus_quick_checklist" -> {
            val items = config.customNote.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                .ifEmpty { listOf("Ship release build", "Review design tokens", "Evening run") }
                .take(3)
            val doneCount = items.indices.count { ((config.bitmaskState shr it) and 1) == 1 }
            drawTopHeader(config.customTitle.ifBlank { "TODAY" }.uppercase(Locale.US), "$doneCount/${items.size}", rightAccent = true)
            val startY = pad + 32f * dp
            val rowGap = ((h - pad - startY) / items.size.coerceAtLeast(1)).coerceAtLeast(18f * dp)
            items.forEachIndexed { idx, task ->
                val checked = ((config.bitmaskState shr idx) and 1) == 1
                val cy = startY + idx * rowGap
                fillPaint.color = if (checked) accentInt else dotInt
                canvas.drawCircle(pad + 4f * dp, cy - 3.5f * dp, 4f * dp, fillPaint)
                bodyPaint.color = if (checked) mutedInt else inkInt
                bodyPaint.textSize = 12f * dp
                canvas.drawText(task.take(28), pad + 15f * dp, cy, bodyPaint)
            }
        }

        "focus_sticky_note" -> {
            drawTopHeader(config.customTitle.ifBlank { "MEMO" }.uppercase(Locale.US).take(14), "", leftDot = true)
            bodyPaint.color = inkInt
            bodyPaint.textSize = 13f * dp
            drawWrappedText(config.customNote.ifBlank { "Less noise. More signal." }, pad, pad + 34f * dp, w - pad * 2, 4, bodyPaint)
        }

        "cal_bold_date" -> {
            val monthShort = SimpleDateFormat("MMM", Locale.US).format(Date(snapshot.nowMillis)).uppercase(Locale.US)
            val wk = SimpleDateFormat("EEE", Locale.US).format(Date(snapshot.nowMillis)).uppercase(Locale.US)
            drawTopHeader(wk, monthShort, rightAccent = true, leftDot = true)
            val dayStr = String.format(Locale.US, "%02d", cal.get(Calendar.DAY_OF_MONTH))
            fitHeroText(dayStr, w - pad * 2, 58f)
            canvas.drawText(dayStr, pad, h - pad - 4f * dp, heroPaint)
        }

        "cal_month_grid" -> {
            val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date(snapshot.nowMillis)).uppercase(Locale.US)
            val today = cal.get(Calendar.DAY_OF_MONTH)
            drawTopHeader(monthTitle, "D$today", rightAccent = true)
            val firstCal = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
            val offset = (firstCal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY).coerceIn(0, 6)
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val colW = (w - pad * 2) / 7f
            val topY = pad + 26f * dp
            val rowH = (h - topY - pad) / 7f
            val daysHeader = listOf("S", "M", "T", "W", "T", "F", "S")
            daysHeader.forEachIndexed { i, d ->
                val cx = pad + i * colW + colW / 2f
                canvas.drawText(d, cx - labelPaint.measureText(d) / 2f, topY, labelPaint)
            }
            val cellPaint = Paint(inkLabelPaint).apply { textSize = 10f * dp }
            for (day in 1..maxDay) {
                val cellIdx = offset + (day - 1)
                val col = cellIdx % 7
                val row = cellIdx / 7 + 1
                val cx = pad + col * colW + colW / 2f
                val cy = topY + row * rowH
                if (day == today) {
                    fillPaint.color = accentInt
                    canvas.drawCircle(cx, cy - 3.5f * dp, min(colW, rowH) * 0.42f, fillPaint)
                    cellPaint.color = surfaceInt
                } else {
                    cellPaint.color = inkInt
                }
                val s = day.toString()
                canvas.drawText(s, cx - cellPaint.measureText(s) / 2f, cy, cellPaint)
            }
        }

        "cal_week_strip" -> {
            val monthShort = SimpleDateFormat("MMM yyyy", Locale.US).format(Date(snapshot.nowMillis)).uppercase(Locale.US)
            drawTopHeader(monthShort, "W${cal.get(Calendar.WEEK_OF_YEAR)}", rightAccent = true)
            val dow = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
            val colW = (w - pad * 2) / 7f
            val labels = listOf("S", "M", "T", "W", "T", "F", "S")
            for (i in 0 until 7) {
                val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i - dow) }
                val isToday = i == dow
                val left = pad + i * colW
                val cx = left + colW / 2f
                if (isToday) {
                    fillPaint.color = accentInt
                    canvas.drawRoundRect(RectF(left + 3f * dp, h * 0.36f, left + colW - 3f * dp, h - pad), 12f * dp, 12f * dp, fillPaint)
                }
                val lp = Paint(labelPaint).apply { color = if (isToday) surfaceInt else mutedInt }
                canvas.drawText(labels[i], cx - lp.measureText(labels[i]) / 2f, h * 0.54f, lp)
                val num = c.get(Calendar.DAY_OF_MONTH).toString()
                val np = Paint(heroPaint).apply { textSize = 15f * dp; color = if (isToday) surfaceInt else inkInt }
                canvas.drawText(num, cx - np.measureText(num) / 2f, h * 0.80f, np)
            }
        }

        // =========================================================================
        // 7. HABITS, WELLNESS & LIFESTYLE (49..56)
        // =========================================================================
        "well_habit_streak" -> {
            val done = (0 until 7).count { ((config.bitmaskState shr it) and 1) == 1 }
            drawTopHeader(config.customTitle.ifBlank { "HABIT" }.uppercase(Locale.US), "$done/7 DAYS", rightAccent = true)
            val labels = listOf("M", "T", "W", "T", "F", "S", "S")
            val step = (w - pad * 2) / 7f
            val cy = h * 0.56f
            for (i in 0 until 7) {
                val active = ((config.bitmaskState shr i) and 1) == 1
                val cx = pad + i * step + step / 2f
                fillPaint.color = if (active && i == 6) accentInt else if (active) inkInt else dotInt
                canvas.drawCircle(cx, cy, (if (active) 7.5f else 5.5f) * dp, fillPaint)
                canvas.drawText(labels[i], cx - labelPaint.measureText(labels[i]) / 2f, h - pad, labelPaint)
            }
        }

        "well_hydration_dots" -> {
            val g = config.goalValue.coerceIn(4, 12)
            drawTopHeader(config.customTitle.ifBlank { "WATER" }.uppercase(Locale.US), "${config.currentValue}/$g", rightAccent = true)
            val v = "${config.currentValue}"
            fitHeroText(v, w - pad * 2, 34f)
            canvas.drawText(v, pad, h * 0.58f, heroPaint)
            drawDotBar(h - pad - 5f * dp, config.currentValue.toFloat() / g, g)
        }

        "well_breathing_478" -> {
            val cx = w / 2f
            val cy = h / 2f
            val outerR = minSide * 0.40f
            val innerR = outerR * if (config.isTimerRunning) 0.72f else 0.58f
            for (i in 0 until 16) {
                val a = i * (2 * PI / 16)
                fillPaint.color = dotInt
                canvas.drawCircle(cx + (outerR * cos(a)).toFloat(), cy + (outerR * sin(a)).toFloat(), 1.8f * dp, fillPaint)
            }
            for (i in 0 until 12) {
                val a = i * (2 * PI / 12)
                fillPaint.color = if (config.isTimerRunning && i % 3 == 0) accentInt else inkInt
                canvas.drawCircle(cx + (innerR * cos(a)).toFloat(), cy + (innerR * sin(a)).toFloat(), 2.8f * dp, fillPaint)
            }
            val lbl = "4·7·8"
            heroPaint.textSize = 16f * dp
            canvas.drawText(lbl, cx - heroPaint.measureText(lbl) / 2f, cy + 5f * dp, heroPaint)
        }

        "well_tally_counter" -> {
            drawTopHeader(config.customTitle.ifBlank { "TALLY" }.uppercase(Locale.US).take(12), "+1", rightAccent = true)
            val v = String.format(Locale.US, "%02d", config.currentValue)
            fitHeroText(v, w - pad * 2, 44f)
            canvas.drawText(v, pad, h * 0.66f, heroPaint)
            canvas.drawText("TAP TO COUNT", pad, h - pad, labelPaint)
        }

        "well_focus_word" -> {
            drawTopHeader("DAILY INTENTION", "", leftDot = true)
            val word = config.customTitle.ifBlank { "CLARITY" }.uppercase(Locale.US)
            fitHeroText(word, w - pad * 2, if (isWide) 42f else 28f)
            canvas.drawText(word, pad, h * 0.62f, heroPaint)
            drawDotBar(h - pad - 4f * dp, 0.5f, 12)
        }

        "well_mood_dots" -> {
            val labels = listOf("LOW", "CALM", "STEADY", "FOCUSED", "PEAK")
            val level = config.currentValue.coerceIn(1, 5)
            drawTopHeader("STATE", "$level/5", rightAccent = true)
            val step = (w - pad * 2) / 5f
            val cy = h * 0.52f
            for (i in 1..5) {
                val cx = pad + (i - 1) * step + step / 2f
                fillPaint.color = when {
                    i == level -> accentInt
                    i < level -> inkInt
                    else -> dotInt
                }
                canvas.drawCircle(cx, cy, (if (i == level) 8.5f else 6f) * dp, fillPaint)
            }
            canvas.drawText(labels[level - 1], pad, h - pad, inkLabelPaint)
        }

        "well_reading_tracker" -> {
            val g = config.goalValue.coerceAtLeast(50)
            val pct = (config.currentValue.toFloat() / g).coerceIn(0f, 1f)
            drawTopHeader(config.customTitle.ifBlank { "BOOK" }.uppercase(Locale.US).take(16), "${(pct * 100).roundToInt()}%", rightAccent = true)
            val pTxt = "P.${config.currentValue} / $g"
            fitHeroText(pTxt, w - pad * 2, 28f)
            canvas.drawText(pTxt, pad, h * 0.58f, heroPaint)
            drawDotBar(h - pad - 5f * dp, pct, 14)
        }

        else -> {
            // focus_stopwatch (56)
            val s = config.currentValue + if (config.isTimerRunning) ((snapshot.nowMillis - config.timerStartedAtMillis) / 1000L).toInt().coerceAtLeast(0) else 0
            drawTopHeader("STOPWATCH", if (config.isTimerRunning) "RUN" else "IDLE", rightAccent = config.isTimerRunning, leftDot = true, leftDotActive = config.isTimerRunning)
            val mmss = String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
            fitHeroText(mmss, w - pad * 2, 32f)
            canvas.drawText(mmss, pad, h * 0.62f, heroPaint)
            canvas.drawText(if (config.isTimerRunning) "TAP PAUSE" else "TAP START", pad, h - pad, labelPaint)
        }
    }
}
