package com.example.widget

import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.data.telemetry.AstronomyCalculator
import com.example.model.WidgetCatalog
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

internal fun WidgetDrawCtx.drawGroupA(): Boolean {
    val tel = snapshot.telemetry
    val hour24 = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)

    when (config.widgetTypeId) {
        // =========================================================================
        // 1. NIGHTING & BEDSIDE (1..8)
        // =========================================================================
        "night_bedside_clock" -> {
            drawTopHeader(
                left = "NIGHTING",
                right = if (config.use24Hour) "24H" else AstronomyCalculator.formatAmPm(snapshot.nowMillis),
                rightAccent = true,
                leftDot = true
            )
            fitHeroText(timeText, w - pad * 2, if (isWide) 54f else 42f)
            canvas.drawText(timeText, pad, h * 0.63f, heroPaint)
            canvas.drawText(dateUpper, pad, h - pad, labelPaint)
            val bat = "BAT ${tel.batteryPercent}%"
            canvas.drawText(bat, w - pad - labelPaint.measureText(bat), h - pad, labelPaint)
            return true
        }

        "night_lunar_matrix" -> {
            drawTopHeader("LUNAR", "${snapshot.lunar.illuminationPercent}%", rightAccent = true)
            drawMoonSphere(w / 2f, h * 0.50f, minSide * 0.48f, snapshot.lunar.phaseFraction)
            canvas.drawText(snapshot.lunar.phaseName, pad, h - pad, inkLabelPaint)
            return true
        }

        "night_solar_horizon" -> {
            val minOfDay = hour24 * 60 + minute
            val sr = snapshot.settings.autoNightEndHour * 60
            val ss = snapshot.settings.autoNightStartHour * 60
            val frac = ((minOfDay - sr).toFloat() / (ss - sr).coerceAtLeast(1)).coerceIn(0f, 1f)
            drawTopHeader("HORIZON", if (minOfDay in sr..ss) "DAYLIGHT" else "NOCTURNAL", rightAccent = true)

            val yBase = h * 0.69f
            val arcH = h * 0.34f
            strokePaint.color = borderInt
            strokePaint.strokeWidth = 1.2f * dp
            canvas.drawLine(pad, yBase, w - pad, yBase, strokePaint)
            val pts = 18
            for (i in 0..pts) {
                val t = i.toFloat() / pts
                val x = pad + (w - pad * 2) * t
                val y = yBase - sin(t * PI).toFloat() * arcH
                fillPaint.color = if (t <= frac) inkInt else dotInt
                canvas.drawCircle(x, y, 2.1f * dp, fillPaint)
            }
            val sx = pad + (w - pad * 2) * frac
            val sy = yBase - sin(frac * PI).toFloat() * arcH
            fillPaint.color = accentInt
            canvas.drawCircle(sx, sy, 4.8f * dp, fillPaint)

            val dawn = String.format(Locale.US, "DAWN %02d:00", snapshot.settings.autoNightEndHour)
            val dusk = String.format(Locale.US, "NIGHT %02d:00", snapshot.settings.autoNightStartHour)
            canvas.drawText(dawn, pad, h - pad, inkLabelPaint)
            canvas.drawText(dusk, w - pad - labelPaint.measureText(dusk), h - pad, labelPaint)
            return true
        }

        "night_sleep_cycles" -> {
            val cycles = config.goalValue.coerceIn(3, 6)
            val bed = config.customTitle.ifBlank { "23:00" }
            drawTopHeader("SLEEP WINDOW", "${cycles}×90M", rightAccent = true)
            fitHeroText(bed, w * 0.56f, 34f)
            canvas.drawText(bed, pad, h * 0.59f, heroPaint)
            canvas.drawText("BEDTIME", pad + heroPaint.measureText(bed) + 8f * dp, h * 0.58f, labelPaint)
            drawDotBar(h - pad - 5f * dp, cycles / 6f, 6)
            return true
        }

        "night_dimmer_mantra" -> {
            val mantra = WidgetCatalog.nightMantras[config.currentValue.mod(WidgetCatalog.nightMantras.size)]
            drawTopHeader("NIGHT MANTRA", "TAP", leftDot = true)
            heroPaint.textSize = (if (isWide) 18f else 15f) * dp
            drawWrappedText(mantra, pad, pad + 34f * dp, w - pad * 2, 3, heroPaint)
            canvas.drawText("TAP TO CYCLE", pad, h - pad, labelPaint)
            return true
        }

        "night_circadian_ribbon" -> {
            val pct = (hour24 * 60 + minute) / 1440f
            drawTopHeader("CIRCADIAN 24H", timeText, rightAccent = false)
            drawDotBar(h * 0.53f, pct, 24)
            val labels = listOf("00", "06", "12", "18", "24")
            val span = w - pad * 2
            labels.forEachIndexed { idx, s ->
                val x = pad + (span * idx / 4f) - (if (idx == 4) labelPaint.measureText(s) else if (idx > 0) labelPaint.measureText(s) / 2f else 0f)
                canvas.drawText(s, x, h - pad, if (idx == 2) inkLabelPaint else labelPaint)
            }
            return true
        }

        "night_starlight_analog" -> {
            val cx = w / 2f
            val cy = h / 2f
            val r = minSide * 0.41f
            for (i in 0 until 12) {
                val a = (-90.0 + i * 30.0) * PI / 180.0
                val card = i % 3 == 0
                fillPaint.color = if (card) inkInt else dotInt
                canvas.drawCircle(cx + (r * cos(a)).toFloat(), cy + (r * sin(a)).toFloat(), (if (card) 2.8f else 1.8f) * dp, fillPaint)
            }
            val hAng = (-90.0 + ((hour24 % 12) + minute / 60.0) * 30.0) * PI / 180.0
            val mAng = (-90.0 + (minute + second / 60.0) * 6.0) * PI / 180.0
            strokePaint.color = mutedInt
            strokePaint.strokeWidth = 2.0f * dp
            canvas.drawLine(cx, cy, cx + (r * 0.68f * cos(mAng)).toFloat(), cy + (r * 0.68f * sin(mAng)).toFloat(), strokePaint)
            fillPaint.color = accentInt
            canvas.drawCircle(cx + (r * 0.72f * cos(hAng)).toFloat(), cy + (r * 0.72f * sin(hAng)).toFloat(), 5.2f * dp, fillPaint)
            fillPaint.color = inkInt
            canvas.drawCircle(cx, cy, 2.4f * dp, fillPaint)
            return true
        }

        "night_quiet_hours" -> {
            val active = AstronomyCalculator.isNightTimeNow(snapshot.nowMillis, snapshot.settings)
            drawTopHeader("QUIET", "")
            drawStatusPipRight(active)
            val txt = if (active) "ACTIVE" else "STANDBY"
            fitHeroText(txt, w - pad * 2, 24f)
            canvas.drawText(txt, pad, h * 0.58f, heroPaint)
            canvas.drawText("${snapshot.settings.autoNightStartHour}:00–0${snapshot.settings.autoNightEndHour}:00", pad, h - pad, labelPaint)
            return true
        }

        // =========================================================================
        // 2. CONTROLS, CONNECTIVITY & EAR BUDS (9..16)
        // =========================================================================
        "ctl_wifi_toggle" -> {
            val on = tel.wifiEnabled || tel.wifiConnected
            drawTopHeader("WI-FI", "")
            drawStatusPipRight(on)
            drawWifiGlyph(w / 2f, h * 0.59f, minSide * 0.27f, on)
            fitHeroText(if (on) "ON" else "OFF", w * 0.5f, 22f)
            canvas.drawText(if (on) "ON" else "OFF", pad, h - pad, heroPaint)
            val tap = "TAP"
            canvas.drawText(tap, w - pad - labelPaint.measureText(tap), h - pad, labelPaint)
            return true
        }

        "ctl_bluetooth_toggle" -> {
            val on = tel.bluetoothEnabled
            drawTopHeader("BLUETOOTH", "")
            drawStatusPipRight(on)
            drawBluetoothGlyph(w / 2f, h * 0.49f, minSide * 0.30f, on)
            fitHeroText(if (on) "ON" else "OFF", w * 0.5f, 22f)
            canvas.drawText(if (on) "ON" else "OFF", pad, h - pad, heroPaint)
            val right = if (tel.budsConnected) "LINKED" else "PANEL"
            val rp = if (tel.budsConnected) accentLabelPaint else labelPaint
            canvas.drawText(right, w - pad - rp.measureText(right), h - pad, rp)
            return true
        }

        "ctl_internet_switch" -> {
            val online = tel.isConnected
            drawTopHeader("INTERNET", if (online) "CONNECTED" else "OFFLINE", rightAccent = online, leftDot = true, leftDotActive = online)
            val main = if (online) tel.networkType else "OFFLINE"
            fitHeroText(main, w * 0.62f, 28f)
            canvas.drawText(main, pad, h * 0.60f, heroPaint)
            canvas.drawText("TAP FOR WI-FI / DATA PANEL", pad, h - pad, labelPaint)

            // Monochrome pill toggle graphic on right
            val pillW = 48f * dp
            val pillH = 26f * dp
            val pillRect = RectF(w - pad - pillW, h * 0.48f - pillH / 2f, w - pad, h * 0.48f + pillH / 2f)
            fillPaint.color = if (online) inkInt else variantInt
            canvas.drawRoundRect(pillRect, pillH / 2f, pillH / 2f, fillPaint)
            val knobR = pillH * 0.36f
            val knobCx = if (online) pillRect.right - pillH / 2f else pillRect.left + pillH / 2f
            fillPaint.color = if (online) accentInt else mutedInt
            canvas.drawCircle(knobCx, pillRect.centerY(), knobR, fillPaint)
            return true
        }

        "ctl_buds_card" -> {
            val title = (if (tel.budsConnected) tel.budsDeviceName else config.customTitle.ifBlank { "EAR (2)" }).uppercase(Locale.US)
            drawTopHeader(if (tel.budsConnected) "CONNECTED" else "STANDBY", "", leftDot = true, leftDotActive = tel.budsConnected)
            fitHeroText(title.take(12), w * 0.58f, 24f)
            canvas.drawText(title.take(12), pad, h * 0.57f, heroPaint)
            canvas.drawText("${tel.audioOutputLabel} · VOL ${tel.mediaVolumePercent}%", pad, h - pad, labelPaint)

            val artSide = minSide * 0.54f
            drawEarbudsGlyph(RectF(w - pad - artSide, h * 0.50f - artSide / 2f, w - pad, h * 0.50f + artSide / 2f), tel.budsConnected)
            return true
        }

        "ctl_buds_volume" -> {
            drawTopHeader(tel.audioOutputLabel, if (tel.isMusicPlaying) "PLAYING" else "PAUSED", rightAccent = tel.isMusicPlaying)
            drawDotEqualizer(RectF(pad, pad + 24f * dp, w - pad, h - pad - 22f * dp), tel.isMusicPlaying, tel.mediaVolumePercent / 100f)
            canvas.drawText("VOL ${tel.mediaVolumePercent}%", pad, h - pad, inkLabelPaint)
            val playIcon = "▶/❚❚"
            canvas.drawText(playIcon, w - pad - accentLabelPaint.measureText(playIcon), h - pad, accentLabelPaint)
            return true
        }

        "ctl_quick_trio" -> {
            drawTopHeader("QUICK CONTROLS", "TAP PANEL", rightAccent = true)
            val pills = listOf(
                Triple("WI-FI", tel.wifiEnabled || tel.wifiConnected, "WLAN"),
                Triple("BT", tel.bluetoothEnabled, if (tel.budsConnected) "BUDS" else "RADIO"),
                Triple("NET", tel.isConnected, if (tel.isConnected) tel.networkType else "OFF")
            )
            val gap = 8f * dp
            val pillW = (w - pad * 2 - gap * 2) / 3f
            val topY = pad + 24f * dp
            val botY = h - pad
            pills.forEachIndexed { idx, (lbl, active, sub) ->
                val left = pad + idx * (pillW + gap)
                val rect = RectF(left, topY, left + pillW, botY)
                fillPaint.color = if (active) inkInt else variantInt
                canvas.drawRoundRect(rect, 14f * dp, 14f * dp, fillPaint)

                fillPaint.color = if (active) accentInt else mutedInt
                canvas.drawCircle(rect.centerX(), rect.top + 13f * dp, 3.2f * dp, fillPaint)

                heroPaint.textSize = 14f * dp
                heroPaint.color = if (active) surfaceInt else inkInt
                canvas.drawText(lbl, rect.centerX() - heroPaint.measureText(lbl) / 2f, rect.centerY() + 6f * dp, heroPaint)

                val sp = Paint(labelPaint).apply {
                    textSize = 7.5f * dp
                    color = if (active) surfaceInt else mutedInt
                }
                canvas.drawText(sub.take(6), rect.centerX() - sp.measureText(sub.take(6)) / 2f, rect.bottom - 10f * dp, sp)
            }
            heroPaint.color = inkInt
            return true
        }

        "ctl_link_guard" -> {
            drawTopHeader("DATA GUARD", "")
            drawStatusPipRight(tel.isConnected)
            val main = if (tel.isConnected) tel.networkType else "OFFLINE"
            fitHeroText(main, w - pad * 2, 24f)
            canvas.drawText(main, pad, h * 0.58f, heroPaint)
            val sub = if (!tel.isConnected) "AIRPLANE / OFF" else if (tel.isMetered) "METERED DATA" else "UNMETERED"
            canvas.drawText(sub, pad, h - pad, labelPaint)
            return true
        }

        "ctl_buds_night" -> {
            val cx = w / 2f
            val cy = h / 2f
            drawDotRing(cx, cy, minSide * 0.40f, tel.mediaVolumePercent / 100f, 20)
            val artSide = minSide * 0.30f
            drawEarbudsGlyph(RectF(cx - artSide / 2f, cy - artSide * 0.68f, cx + artSide / 2f, cy + artSide * 0.32f), tel.budsConnected)
            val volTxt = "${tel.mediaVolumePercent}%"
            canvas.drawText(volTxt, cx - inkLabelPaint.measureText(volTxt) / 2f, cy + artSide * 0.55f, inkLabelPaint)
            return true
        }

        // =========================================================================
        // 3. PHOTO STUDIO & QUOTES (17..24)
        // =========================================================================
        "studio_photo_mono", "studio_photo_wide" -> {
            val pRect = RectF(pad * 0.65f, pad * 0.65f, w - pad * 0.65f, h - pad * 0.65f)
            drawCroppedPhoto(pRect, circular = false)
            val cap = config.customTitle.ifBlank { if (config.widgetTypeId == "studio_photo_wide") "STUDIO STILL" else "FRAME 01" }.uppercase(Locale.US)
            val barH = 22f * dp
            val bar = RectF(pRect.left, pRect.bottom - barH, pRect.right, pRect.bottom)
            fillPaint.color = Color.argb(155, 0, 0, 0)
            canvas.drawRoundRect(bar, 10f * dp, 10f * dp, fillPaint)
            val cp = Paint(labelPaint).apply { color = Color.WHITE; textSize = 8.5f * dp }
            canvas.drawText(cap.take(20), bar.left + 9f * dp, bar.bottom - 7f * dp, cp)
            fillPaint.color = accentInt
            canvas.drawCircle(bar.right - 10f * dp, bar.centerY(), 2.8f * dp, fillPaint)
            return true
        }

        "studio_photo_polaroid" -> {
            val outer = RectF(pad * 0.7f, pad * 0.7f, w - pad * 0.7f, h - pad * 0.7f)
            fillPaint.color = variantInt
            canvas.drawRoundRect(outer, 14f * dp, 14f * dp, fillPaint)
            val pRect = RectF(outer.left + 8f * dp, outer.top + 8f * dp, outer.right - 8f * dp, outer.bottom - 30f * dp)
            drawCroppedPhoto(pRect, circular = false)
            val cap = config.customTitle.ifBlank { "ARCHIVE · 2026" }.uppercase(Locale.US)
            heroPaint.textSize = 12f * dp
            canvas.drawText(cap.take(20), outer.left + 10f * dp, outer.bottom - 10f * dp, heroPaint)
            fillPaint.color = accentInt
            canvas.drawCircle(outer.right - 12f * dp, outer.bottom - 14f * dp, 3.2f * dp, fillPaint)
            return true
        }

        "studio_photo_circle" -> {
            val side = minSide * 0.60f
            val pRect = RectF((w - side) / 2f, pad * 0.75f, (w + side) / 2f, pad * 0.75f + side)
            drawCroppedPhoto(pRect, circular = true)
            strokePaint.color = inkInt
            strokePaint.strokeWidth = 1.8f * dp
            canvas.drawOval(pRect, strokePaint)
            val cap = config.customTitle.ifBlank { "PORTRAIT" }.uppercase(Locale.US)
            canvas.drawText(cap, (w - labelPaint.measureText(cap)) / 2f, h - pad * 0.8f, labelPaint)
            return true
        }

        "studio_photo_quote" -> {
            val splitX = w * 0.42f
            drawCroppedPhoto(RectF(pad * 0.75f, pad * 0.75f, splitX, h - pad * 0.75f), circular = false)
            val tx = splitX + 12f * dp
            fillPaint.color = accentInt
            canvas.drawCircle(tx + 3f * dp, pad + 6f * dp, 2.8f * dp, fillPaint)
            canvas.drawText(config.customTitle.ifBlank { "STILLNESS" }.uppercase(Locale.US).take(14), tx + 10f * dp, pad + 9f * dp, labelPaint)
            val quote = config.customNote.ifBlank { WidgetCatalog.designQuotes[config.currentValue.mod(WidgetCatalog.designQuotes.size)].first }
            bodyPaint.color = inkInt
            bodyPaint.textSize = 11.5f * dp
            drawWrappedText("\"$quote\"", tx, pad + 28f * dp, w - tx - pad, 4, bodyPaint)
            canvas.drawText("STUDIO", tx, h - pad, accentLabelPaint)
            return true
        }

        "studio_quote_stoic", "studio_quote_design", "studio_quote_custom" -> {
            val (badge, quote, author, tapHint) = when (config.widgetTypeId) {
                "studio_quote_stoic" -> {
                    val (q, a) = WidgetCatalog.stoicQuotes[config.currentValue.mod(WidgetCatalog.stoicQuotes.size)]
                    Quad("STOIC", q, a, "TAP")
                }
                "studio_quote_design" -> {
                    val (q, a) = WidgetCatalog.designQuotes[config.currentValue.mod(WidgetCatalog.designQuotes.size)]
                    Quad("DESIGN ETHOS", q, a, "TAP")
                }
                else -> Quad(
                    "QUOTE",
                    config.customNote.ifBlank { "Build things that respect people's attention." },
                    config.customTitle.ifBlank { "PERSONAL ETHOS" },
                    ""
                )
            }
            drawTopHeader(badge, tapHint, leftDot = true)
            bodyPaint.color = inkInt
            bodyPaint.textSize = (if (isWide) 13.5f else 11.5f) * dp
            drawWrappedText("\"$quote\"", pad, pad + 32f * dp, w - pad * 2, 3, bodyPaint)
            canvas.drawText("— ${author.uppercase(Locale.US)}", pad, h - pad, accentLabelPaint)
            return true
        }

        else -> return false
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
