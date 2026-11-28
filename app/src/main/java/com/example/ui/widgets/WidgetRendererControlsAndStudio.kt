package com.example.ui.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.WidgetPhotoStorage
import com.example.model.LiveSnapshot
import com.example.model.WidgetCatalog
import com.example.model.WidgetConfig
import com.example.ui.theme.DotMatrixFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import java.util.Locale

@Composable
fun RenderControlsAndStudioWidget(
    config: WidgetConfig,
    snapshot: LiveSnapshot,
    colors: ResolvedWidgetColors,
    compactPreview: Boolean
) {
    val tel = snapshot.telemetry

    when (config.widgetTypeId) {
        // =========================================================================
        // CATEGORY 2: CONNECTIVITY, TOGGLES & EAR BUDS (9..16)
        // =========================================================================
        "ctl_wifi_toggle" -> {
            val isOn = tel.wifiEnabled || tel.wifiConnected
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("WI-FI", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isOn) colors.accent else colors.dotInactive))
                }
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    DotWifiGlyph(
                        isOn = isOn,
                        ink = colors.ink,
                        inactive = colors.dotInactive,
                        accent = colors.accent,
                        modifier = Modifier.size(if (compactPreview) 52.dp else 68.dp)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (isOn) "ON" else "OFF", fontFamily = DotMatrixFontFamily, fontSize = if (compactPreview) 18.sp else 22.sp, color = colors.ink)
                    Text("TAP", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk, modifier = Modifier.align(Alignment.Bottom))
                }
            }
        }

        "ctl_bluetooth_toggle" -> {
            val isOn = tel.bluetoothEnabled
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("BLUETOOTH", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isOn) colors.accent else colors.dotInactive))
                }
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    DotBluetoothGlyph(
                        isOn = isOn,
                        ink = colors.ink,
                        inactive = colors.dotInactive,
                        accent = colors.accent,
                        modifier = Modifier.size(if (compactPreview) 48.dp else 64.dp)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (isOn) "ON" else "OFF", fontFamily = DotMatrixFontFamily, fontSize = if (compactPreview) 18.sp else 22.sp, color = colors.ink)
                    Text(if (tel.budsConnected) "LINKED" else "PANEL", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = if (tel.budsConnected) colors.accent else colors.mutedInk, modifier = Modifier.align(Alignment.Bottom))
                }
            }
        }

        "ctl_internet_switch" -> {
            val online = tel.isConnected
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (online) colors.accent else colors.dotInactive))
                        Spacer(Modifier.width(6.dp))
                        Text("INTERNET", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    }
                    Text(if (online) "CONNECTED" else "OFFLINE", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = if (online) colors.accent else colors.mutedInk)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            text = if (online) tel.networkType else "OFFLINE",
                            fontFamily = DotMatrixFontFamily,
                            fontSize = if (compactPreview) 22.sp else 28.sp,
                            color = colors.ink
                        )
                        Text(
                            text = "TAP FOR WI-FI / DATA TOGGLE",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 8.sp,
                            color = colors.mutedInk
                        )
                    }
                    // Monochrome toggle pill graphic
                    Box(
                        modifier = Modifier
                            .width(46.dp)
                            .height(26.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (online) colors.ink else colors.surfaceVariant)
                            .padding(3.dp),
                        contentAlignment = if (online) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (online) colors.accent else colors.mutedInk)
                        )
                    }
                }
            }
        }

        "ctl_buds_card" -> {
            val budsTitle = if (tel.budsConnected) tel.budsDeviceName else config.customTitle.ifBlank { "EAR (2)" }
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.25f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (tel.budsConnected) colors.accent else colors.dotInactive))
                        Spacer(Modifier.width(6.dp))
                        Text(if (tel.budsConnected) "CONNECTED" else "STANDBY", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = if (tel.budsConnected) colors.accent else colors.mutedInk)
                    }
                    Text(
                        text = budsTitle.uppercase(Locale.US),
                        fontFamily = DotMatrixFontFamily,
                        fontSize = if (compactPreview) 18.sp else 24.sp,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${tel.audioOutputLabel} · VOL ${tel.mediaVolumePercent}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 9.sp,
                        color = colors.mutedInk
                    )
                }
                DotEarbudsGlyph(
                    connected = tel.budsConnected,
                    ink = colors.ink,
                    muted = colors.mutedInk,
                    inactive = colors.dotInactive,
                    accent = colors.accent,
                    modifier = Modifier.size(if (compactPreview) 62.dp else 84.dp)
                )
            }
        }

        "ctl_buds_volume" -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tel.audioOutputLabel, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Text(if (tel.isMusicPlaying) "PLAYING" else "PAUSED", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = if (tel.isMusicPlaying) colors.accent else colors.mutedInk)
                }
                DotEqualizerCanvas(
                    isPlaying = tel.isMusicPlaying,
                    volumeFraction = tel.mediaVolumePercent / 100f,
                    ink = colors.ink,
                    inactive = colors.dotInactive,
                    accent = colors.accent,
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 6.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("VOL ${tel.mediaVolumePercent}%", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.ink)
                    Text("▶/❚❚", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.accent)
                }
            }
        }

        "ctl_quick_trio" -> {
            val pills = listOf(
                Triple("WI-FI", tel.wifiEnabled || tel.wifiConnected, "WLAN"),
                Triple("BT", tel.bluetoothEnabled, if (tel.budsConnected) "BUDS" else "RADIO"),
                Triple("NET", tel.isConnected, if (tel.isConnected) tel.networkType else "OFF")
            )
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("QUICK CONTROLS", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Text("TAP PANEL", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.accent)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pills.forEach { (title, active, sub) ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active) colors.ink else colors.surfaceVariant)
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (active) colors.accent else colors.mutedInk)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(title, fontFamily = DotMatrixFontFamily, fontSize = 13.sp, color = if (active) colors.surface else colors.ink)
                            Text(sub, fontFamily = JetBrainsMonoFontFamily, fontSize = 7.sp, color = if (active) colors.surface.copy(alpha = 0.7f) else colors.mutedInk, maxLines = 1)
                        }
                    }
                }
            }
        }

        "ctl_link_guard" -> {
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("DATA GUARD", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (tel.isConnected) colors.accent else colors.dotInactive))
                }
                Text(
                    text = if (tel.isConnected) tel.networkType else "OFFLINE",
                    fontFamily = DotMatrixFontFamily,
                    fontSize = if (compactPreview) 20.sp else 26.sp,
                    color = colors.ink
                )
                Text(
                    text = if (!tel.isConnected) "AIRPLANE / OFF" else if (tel.isMetered) "METERED DATA" else "UNMETERED",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    color = colors.mutedInk
                )
            }
        }

        "ctl_buds_night" -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DotProgressRing(
                    progress = tel.mediaVolumePercent / 100f,
                    dotCount = 20,
                    activeColor = colors.ink,
                    inactiveColor = colors.dotInactive,
                    accentHeadColor = colors.accent,
                    modifier = Modifier.fillMaxSize().padding(2.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    DotEarbudsGlyph(
                        connected = tel.budsConnected,
                        ink = colors.ink,
                        muted = colors.mutedInk,
                        inactive = colors.dotInactive,
                        accent = colors.accent,
                        modifier = Modifier.size(if (compactPreview) 34.dp else 44.dp)
                    )
                    Text(
                        text = "${tel.mediaVolumePercent}%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = colors.ink
                    )
                }
            }
        }

        // =========================================================================
        // CATEGORY 3: PHOTO STUDIO & QUOTES (17..24)
        // =========================================================================
        "studio_photo_mono" -> {
            StudioPhotoBox(
                photoPath = config.photoPath,
                monochrome = config.monochromePhoto,
                colors = colors,
                caption = config.customTitle.ifBlank { "FRAME 01" },
                circular = false,
                polaroidFrame = false
            )
        }

        "studio_photo_polaroid" -> {
            StudioPhotoBox(
                photoPath = config.photoPath,
                monochrome = config.monochromePhoto,
                colors = colors,
                caption = config.customTitle.ifBlank { "ARCHIVE · 2026" },
                circular = false,
                polaroidFrame = true
            )
        }

        "studio_photo_circle" -> {
            StudioPhotoBox(
                photoPath = config.photoPath,
                monochrome = config.monochromePhoto,
                colors = colors,
                caption = config.customTitle.ifBlank { "PORTRAIT" },
                circular = true,
                polaroidFrame = false
            )
        }

        "studio_photo_wide" -> {
            StudioPhotoBox(
                photoPath = config.photoPath,
                monochrome = config.monochromePhoto,
                colors = colors,
                caption = config.customTitle.ifBlank { "STUDIO STILL" },
                circular = false,
                polaroidFrame = false
            )
        }

        "studio_quote_stoic" -> {
            val quotes = WidgetCatalog.stoicQuotes
            val (quote, author) = quotes[config.currentValue.mod(quotes.size)]
            QuoteCardBody(
                badge = "STOIC",
                quote = quote,
                author = author,
                colors = colors,
                compactPreview = compactPreview,
                showTapHint = true
            )
        }

        "studio_quote_design" -> {
            val quotes = WidgetCatalog.designQuotes
            val (quote, author) = quotes[config.currentValue.mod(quotes.size)]
            QuoteCardBody(
                badge = "DESIGN ETHOS",
                quote = quote,
                author = author,
                colors = colors,
                compactPreview = compactPreview,
                showTapHint = true
            )
        }

        "studio_quote_custom" -> {
            QuoteCardBody(
                badge = "QUOTE",
                quote = config.customNote.ifBlank { "Build things that respect people's attention." },
                author = config.customTitle.ifBlank { "PERSONAL ETHOS" },
                colors = colors,
                compactPreview = compactPreview,
                showTapHint = false
            )
        }

        // 24. Photo + Quote Studio Split
        else -> {
            val quotes = WidgetCatalog.designQuotes
            val quoteText = config.customNote.ifBlank { quotes[config.currentValue.mod(quotes.size)].first }
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    StudioPhotoBox(
                        photoPath = config.photoPath,
                        monochrome = config.monochromePhoto,
                        colors = colors,
                        caption = "",
                        circular = false,
                        polaroidFrame = false
                    )
                }
                Column(
                    modifier = Modifier.weight(1.15f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(colors.accent))
                        Spacer(Modifier.width(5.dp))
                        Text(config.customTitle.ifBlank { "STILLNESS" }.uppercase(Locale.US), fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = colors.mutedInk, maxLines = 1)
                    }
                    Text(
                        text = "\"$quoteText\"",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = if (compactPreview) 11.sp else 13.sp,
                        lineHeight = if (compactPreview) 15.sp else 18.sp,
                        color = colors.ink,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("STUDIO", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = colors.accent)
                }
            }
        }
    }
}

@Composable
private fun StudioPhotoBox(
    photoPath: String,
    monochrome: Boolean,
    colors: ResolvedWidgetColors,
    caption: String,
    circular: Boolean,
    polaroidFrame: Boolean
) {
    val imageBitmap = remember(photoPath) {
        WidgetPhotoStorage.loadBitmap(photoPath)?.asImageBitmap()
    }
    val monoFilter = remember(monochrome) {
        if (monochrome) {
            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        } else null
    }

    if (circular) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, colors.ink, CircleShape)
                        .background(colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageBitmap != null) {
                        Image(
                            bitmap = imageBitmap,
                            contentDescription = caption,
                            contentScale = ContentScale.Crop,
                            colorFilter = monoFilter,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        StudioMonochromeArtPlaceholder(colors = colors, modifier = Modifier.fillMaxSize())
                    }
                }
            }
            if (caption.isNotBlank()) {
                Text(caption.uppercase(Locale.US), fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk, maxLines = 1)
            }
        }
    } else if (polaroidFrame) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceVariant)
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = caption,
                        contentScale = ContentScale.Crop,
                        colorFilter = monoFilter,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    StudioMonochromeArtPlaceholder(colors = colors, modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(caption.uppercase(Locale.US), fontFamily = DotMatrixFontFamily, fontSize = 12.sp, color = colors.ink, maxLines = 1)
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.accent))
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceVariant)
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = caption,
                    contentScale = ContentScale.Crop,
                    colorFilter = monoFilter,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                StudioMonochromeArtPlaceholder(colors = colors, modifier = Modifier.fillMaxSize())
            }
            if (caption.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.58f))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(caption.uppercase(Locale.US), fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = Color.White, maxLines = 1)
                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(colors.accent))
                }
            }
        }
    }
}

@Composable
private fun StudioMonochromeArtPlaceholder(
    colors: ResolvedWidgetColors,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        // Minimalist Nothing camera aperture + dot halftone grid placeholder
        val step = (kotlin.math.min(w, h) / 7f).coerceAtLeast(10f)
        var y = step * 0.6f
        while (y < h) {
            var x = step * 0.6f
            while (x < w) {
                val dist = kotlin.math.hypot(x - cx, y - cy)
                val maxD = kotlin.math.min(w, h) * 0.45f
                val r = if (dist < maxD * 0.55f) 3.2f else 1.8f
                drawCircle(
                    color = if (dist < maxD * 0.55f) colors.ink.copy(alpha = 0.7f) else colors.dotInactive,
                    radius = r,
                    center = Offset(x, y)
                )
                x += step
            }
            y += step
        }
        drawCircle(
            color = colors.accent,
            radius = kotlin.math.min(w, h) * 0.08f,
            center = Offset(cx + kotlin.math.min(w, h) * 0.22f, cy - kotlin.math.min(w, h) * 0.18f)
        )
    }
}

@Composable
private fun QuoteCardBody(
    badge: String,
    quote: String,
    author: String,
    colors: ResolvedWidgetColors,
    compactPreview: Boolean,
    showTapHint: Boolean
) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.accent))
                Spacer(Modifier.width(6.dp))
                Text(badge, fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = colors.mutedInk)
            }
            if (showTapHint) {
                Text("TAP", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = colors.mutedInk)
            }
        }
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
        Text(
            text = "— ${author.uppercase(Locale.US)}",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.sp,
            color = colors.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
