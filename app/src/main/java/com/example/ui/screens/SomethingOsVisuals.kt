package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WidgetCategory
import com.example.ui.theme.DotMatrixFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalDotGridColors
import com.example.ui.theme.StatusAmber
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

// Precomputed deterministic starfield micro-dots (zero allocation during draw)
private val CosmicDotCoords: List<Triple<Float, Float, Float>> = List(64) { i ->
    val x = ((i * 73 + 19) % 100) / 100f
    val y = ((i * 41 + 11) % 100) / 100f
    val r = if (i % 7 == 0) 2.2f else if (i % 3 == 0) 1.5f else 1.0f
    Triple(x, y, r)
}

/**
 * Lightweight, allocation-free scattered cosmic micro-dot backdrop matching the reference screenshots.
 */
fun Modifier.cosmicDotBackdrop(dotColor: Color, isLightMode: Boolean): Modifier = this.drawBehind {
    val alphaBase = if (isLightMode) 0.45f else 0.34f
    CosmicDotCoords.forEachIndexed { idx, (fx, fy, r) ->
        drawCircle(
            color = dotColor.copy(alpha = if (idx % 5 == 0) alphaBase * 1.4f else alphaBase),
            radius = r * density,
            center = Offset(size.width * fx, size.height * fy)
        )
    }
}

@Composable
fun SystemReadoutHeroCard(
    totalWidgets: Int,
    pinnedCount: Int,
    ultraEcoMode: Boolean,
    onExploreAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDotGridColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(ext.cardSurface)
            .border(1.dp, ext.hairline, RoundedCornerShape(26.dp))
            .padding(18.dp)
    ) {
        // Top Status Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(ext.redAccent)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "SYSTEM_READOUT",
                    fontFamily = DotMatrixFontFamily,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp,
                    color = ext.redAccent
                )
            }
            Text(
                text = if (ultraEcoMode) "STATUS: ECO_READY" else "STATUS: ACTIVE",
                fontFamily = DotMatrixFontFamily,
                fontSize = 11.sp,
                letterSpacing = 0.8.sp,
                color = StatusAmber
            )
        }

        Spacer(Modifier.height(14.dp))

        // Giant 056 ACTIVE_WIDGETS Readout with vertical 4-dot brackets
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            VerticalFourDotBracket(color = ext.redAccent.copy(alpha = 0.45f))
            Spacer(Modifier.width(10.dp))
            Text(
                text = String.format(Locale.US, "%03d", totalWidgets),
                fontFamily = DotMatrixFontFamily,
                fontSize = 40.sp,
                letterSpacing = 2.sp,
                color = ext.ink
            )
            Spacer(Modifier.width(10.dp))
            VerticalFourDotBracket(color = ext.redAccent)
            Spacer(Modifier.width(14.dp))
            Text(
                text = "ACTIVE_WIDGETS",
                fontFamily = DotMatrixFontFamily,
                fontSize = 15.sp,
                letterSpacing = 1.sp,
                color = ext.ink
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = if (pinnedCount > 0) {
                "ASSET_SYNC_COMPLETE · ${pinnedCount}_PINNED_TO_HOME"
            } else {
                "ASSET_SYNCHRONIZATION_COMPLETE"
            },
            fontFamily = DotMatrixFontFamily,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp,
            color = ext.redAccent.copy(alpha = 0.85f)
        )

        Spacer(Modifier.height(14.dp))

        // Full signature red pill CTA
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(ext.redAccent)
                .clickable(onClick = onExploreAllClick)
                .padding(horizontal = 18.dp, vertical = 10.dp)
                .testTag("hero_explore_all_btn"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.GridView,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Explore & customize all $totalWidgets widgets",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 12.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun VerticalFourDotBracket(color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .size(4.5.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
fun RedEcoActionBanner(
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDotGridColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ext.redAccent)
            .clickable(onClick = onSyncClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("eco_sync_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.BatteryChargingFull,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "SYNC HOME SCREEN WIDGETS",
                fontFamily = DotMatrixFontFamily,
                fontSize = 14.sp,
                letterSpacing = 0.8.sp,
                color = Color.White,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .border(1.2.dp, Color.White, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 5.dp)
        ) {
            Text(
                text = "SYNC",
                fontFamily = DotMatrixFontFamily,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun LatestWidgetsLaserCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDotGridColors.current
    val infinite = rememberInfiniteTransition(label = "latest_laser_card")
    // State objects read strictly inside Canvas / drawWithContent / graphicsLayer -> zero recomposition!
    val scanProgress = infinite.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_scan_x"
    )
    val arrowNudge = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 5.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 820, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_nudge"
    )
    val beamBreath = infinite.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beam_breath"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(28.dp))
            .background(ext.insetSurface)
            .border(1.dp, ext.hairline, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .testTag("card_try_latest")
    ) {
        // 1. Live reactive dot-matrix grid illuminated by the sweeping red scanner
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 11.dp.toPx()
            val baseDotR = 1.15.dp.toPx()
            val beamX = size.width * scanProgress.value
            val glowRadius = 38.dp.toPx()
            val baseDotColor = ext.dotMatrix.copy(alpha = 0.48f)
            val hotDotColor = ext.redAccent.copy(alpha = 0.92f)

            var y = step * 0.7f
            while (y < size.height) {
                var x = step * 0.7f
                while (x < size.width) {
                    val dist = abs(x - beamX)
                    val heat = (1f - (dist / glowRadius)).coerceIn(0f, 1f)
                    val smoothHeat = heat * heat
                    val dotColor = if (smoothHeat > 0.01f) {
                        lerp(baseDotColor, hotDotColor, smoothHeat)
                    } else {
                        baseDotColor
                    }
                    val dotR = baseDotR * (1f + 0.65f * smoothHeat)
                    drawCircle(
                        color = dotColor,
                        radius = dotR,
                        center = Offset(x, y)
                    )
                    x += step
                }
                y += step
            }

            // Soft trailing aura behind text
            val auraHalf = 34.dp.toPx()
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        ext.redAccent.copy(alpha = 0.28f * beamBreath.value),
                        Color.Transparent
                    ),
                    startX = beamX - auraHalf,
                    endX = beamX + auraHalf
                ),
                topLeft = Offset(beamX - auraHalf, 0f),
                size = Size(auraHalf * 2f, size.height)
            )
        }

        // 2. Stacked dot-matrix headline: base muted ink + laser-clipped glowing red reveal!
        Box(modifier = Modifier.fillMaxSize()) {
            // Base muted layer
            LatestWidgetsTypographyContent(
                textColor = ext.mutedInk.copy(alpha = 0.75f),
                arrowColor = ext.redAccent.copy(alpha = 0.88f),
                arrowNudgeProvider = { arrowNudge.value },
                modifier = Modifier.fillMaxSize()
            )
            // Dynamic glowing red scan reveal clipped strictly around/behind the sweeping laser
            LatestWidgetsTypographyContent(
                textColor = ext.redAccent,
                arrowColor = ext.redAccent,
                arrowNudgeProvider = { arrowNudge.value },
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        val beamX = size.width * scanProgress.value
                        val trailLeft = (beamX - 66.dp.toPx()).coerceAtLeast(0f)
                        val trailRight = (beamX + 18.dp.toPx()).coerceAtMost(size.width)
                        clipRect(left = trailLeft, top = 0f, right = trailRight, bottom = size.height) {
                            this@drawWithContent.drawContent()
                        }
                    }
            )
        }

        // 3. Foreground razor-sharp red laser beam + emitter pips sweeping right over the text
        Canvas(modifier = Modifier.fillMaxSize()) {
            val beamX = size.width * scanProgress.value
            val glowHalf = 14.dp.toPx()
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        ext.redAccent.copy(alpha = 0.24f * beamBreath.value),
                        Color.Transparent
                    ),
                    startX = beamX - glowHalf,
                    endX = beamX + glowHalf
                ),
                topLeft = Offset(beamX - glowHalf, 0f),
                size = Size(glowHalf * 2f, size.height)
            )
            // Outer red laser line
            drawLine(
                color = ext.redAccent,
                start = Offset(beamX, 0f),
                end = Offset(beamX, size.height),
                strokeWidth = 2.dp.toPx()
            )
            // Bright hot inner filament
            drawLine(
                color = Color.White.copy(alpha = 0.45f * beamBreath.value),
                start = Offset(beamX, 0f),
                end = Offset(beamX, size.height),
                strokeWidth = 0.7.dp.toPx()
            )
            // Top & bottom laser emitter sparks
            drawCircle(
                color = ext.redAccent,
                radius = 3.2.dp.toPx(),
                center = Offset(beamX, 3.dp.toPx())
            )
            drawCircle(
                color = ext.redAccent,
                radius = 3.2.dp.toPx(),
                center = Offset(beamX, size.height - 3.dp.toPx())
            )
        }
    }
}

@Composable
private fun LatestWidgetsTypographyContent(
    textColor: Color,
    arrowColor: Color,
    arrowNudgeProvider: () -> Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "TRY THE",
                fontFamily = DotMatrixFontFamily,
                fontSize = 15.sp,
                letterSpacing = 0.6.sp,
                color = textColor,
                maxLines = 1
            )
            Text(
                text = "LATEST",
                fontFamily = DotMatrixFontFamily,
                fontSize = 15.sp,
                letterSpacing = 0.6.sp,
                color = textColor,
                maxLines = 1
            )
            Text(
                text = "WIDGETS",
                fontFamily = DotMatrixFontFamily,
                fontSize = 15.sp,
                letterSpacing = 0.6.sp,
                color = textColor,
                maxLines = 1
            )
        }
        Spacer(Modifier.width(12.dp))
        // Crisp Nothing-style dotted arrow animated via GPU translationX
        Canvas(
            modifier = Modifier
                .size(width = 22.dp, height = 18.dp)
                .graphicsLayer {
                    translationX = arrowNudgeProvider().dp.toPx()
                }
        ) {
            val dotR = 1.4.dp.toPx()
            val step = 3.6.dp.toPx()
            val cy = size.height / 2f
            // Horizontal shaft (5 dots)
            for (i in 0..4) {
                drawCircle(
                    color = arrowColor,
                    radius = dotR,
                    center = Offset(dotR + i * step, cy)
                )
            }
            // Arrowhead chevron dots
            drawCircle(arrowColor, dotR, Offset(dotR + 3 * step, cy - step))
            drawCircle(arrowColor, dotR, Offset(dotR + 3 * step, cy + step))
            drawCircle(arrowColor, dotR, Offset(dotR + 2 * step, cy - step * 2f))
            drawCircle(arrowColor, dotR, Offset(dotR + 2 * step, cy + step * 2f))
        }
    }
}

@Composable
fun NewStarburstBadgeCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDotGridColors.current
    val infinite = rememberInfiniteTransition(label = "new_starburst_card")
    val sealRotation = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starburst_rotation"
    )
    val dotPulse = infinite.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "new_dot_pulse"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(28.dp))
            .background(ext.cardSurface)
            .border(1.dp, ext.hairline, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .testTag("card_new_featured")
    ) {
        // Top-left NEW pill with gently breathing red dot
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .background(ext.pillSurface)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer {
                        val s = dotPulse.value
                        scaleX = s
                        scaleY = s
                    }
                    .clip(CircleShape)
                    .background(ext.redAccent)
            )
            Spacer(Modifier.width(5.dp))
            Text("NEW", fontFamily = DotMatrixFontFamily, fontSize = 10.sp, color = ext.ink)
        }

        // Inset rounded panel with smoothly rotating scalloped red NEW seal
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxSize(0.74f)
                .clip(RoundedCornerShape(topStart = 24.dp, bottomEnd = 26.dp))
                .background(ext.insetSurface),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize(0.84f)
                    .graphicsLayer {
                        rotationZ = sealRotation.value
                    }
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val baseR = min(size.width, size.height) * 0.42f
                val lobes = 10
                val path = Path()
                val steps = 120
                for (i in 0..steps) {
                    val a = (2.0 * PI * i / steps)
                    val r = baseR + baseR * 0.11f * sin(a * lobes).toFloat()
                    val px = cx + (r * cos(a)).toFloat()
                    val py = cy + (r * sin(a)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(path, color = ext.redAccent, style = Stroke(width = 4.dp.toPx()))
            }
            Text(
                text = "NEW",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 1.sp,
                color = ext.redAccent
            )
        }
    }
}

@Composable
fun CategoryHubCard(
    category: WidgetCategory,
    count: Int,
    showNewTag: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDotGridColors.current
    val icon: ImageVector = remember(category) {
        when (category) {
            WidgetCategory.NIGHTING -> Icons.Default.Explore
            WidgetCategory.CONTROLS -> Icons.Default.Headphones
            WidgetCategory.STUDIO -> Icons.Default.CameraAlt
            WidgetCategory.TIME -> Icons.Default.Schedule
            WidgetCategory.SYSTEM -> Icons.Default.BatteryChargingFull
            WidgetCategory.FOCUS -> Icons.Default.CalendarMonth
            WidgetCategory.WELLNESS -> Icons.Default.CheckCircle
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(ext.cardSurface)
                .border(1.dp, ext.hairline, RoundedCornerShape(28.dp))
                .clickable(onClick = onClick)
                .testTag("hub_cat_${category.name.lowercase()}")
        ) {
            // Top-left crisp monochrome icon
            Icon(
                imageVector = icon,
                contentDescription = category.displayName,
                tint = ext.ink,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
                    .size(20.dp)
            )

            // Top-right badge pill
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(ext.pillSurface)
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(5.5.dp).clip(CircleShape).background(ext.redAccent))
                Spacer(Modifier.width(5.dp))
                Text(
                    text = if (showNewTag) "NEW" else String.format(Locale.US, "%02d", count),
                    fontFamily = DotMatrixFontFamily,
                    fontSize = 10.sp,
                    color = ext.ink
                )
            }

            // Bottom-right inset art panel (exact Something OS signature detail!)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxSize(0.74f)
                    .clip(RoundedCornerShape(topStart = 24.dp, bottomEnd = 26.dp))
                    .background(ext.insetSurface)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                CategoryInsetArt(
                    category = category,
                    ink = ext.ink,
                    muted = ext.mutedInk,
                    dot = ext.dotMatrix,
                    accent = ext.redAccent,
                    surface = ext.insetSurface
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = category.displayName,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 13.sp,
            color = ext.ink,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun CategoryInsetArt(
    category: WidgetCategory,
    ink: Color,
    muted: Color,
    dot: Color,
    accent: Color,
    surface: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val side = min(w, h)
        val cx = w / 2f
        val cy = h / 2f

        when (category) {
            WidgetCategory.NIGHTING -> {
                // Iconic Red & White Dotted Lunar Sphere (inspired directly by reference Astronomy card!)
                val grid = 8
                val cell = side / grid
                val rMax = grid / 2f
                val dr = cell * 0.37f
                for (row in 0 until grid) {
                    for (col in 0 until grid) {
                        val nx = (col + 0.5f - rMax) / rMax
                        val ny = (row + 0.5f - rMax) / rMax
                        if (nx * nx + ny * ny <= 0.96f) {
                            val isRedSide = nx < -0.18f + (ny * ny * 0.25f)
                            drawCircle(
                                color = if (isRedSide) accent else ink.copy(alpha = 0.88f),
                                radius = dr,
                                center = Offset((col + 0.5f) * cell, (row + 0.5f) * cell)
                            )
                        }
                    }
                }
            }

            WidgetCategory.CONTROLS -> {
                // Nothing Ear Stem + Red Dot + Signal Arc
                val headR = side * 0.19f
                val stemW = side * 0.16f
                val stemH = side * 0.44f
                drawCircle(color = ink, radius = headR, center = Offset(cx - side * 0.14f, cy - side * 0.14f))
                drawRoundRect(
                    color = ink,
                    topLeft = Offset(cx - side * 0.14f - stemW / 2f, cy - side * 0.10f),
                    size = Size(stemW, stemH),
                    cornerRadius = CornerRadius(stemW / 2f, stemW / 2f),
                    style = Stroke(width = 2.5.dp.toPx())
                )
                drawCircle(color = accent, radius = 3.6.dp.toPx(), center = Offset(cx - side * 0.14f, cy + side * 0.04f))
                // Wireless arcs on right
                listOf(side * 0.18f, side * 0.30f).forEachIndexed { idx, rad ->
                    val n = 5 + idx * 2
                    for (i in 0 until n) {
                        val deg = -55.0 + (110.0 * i / (n - 1))
                        val a = deg * PI / 180.0
                        drawCircle(
                            color = if (idx == 0) accent else muted,
                            radius = 2.4.dp.toPx(),
                            center = Offset(cx + side * 0.04f + (rad * cos(a)).toFloat(), cy - side * 0.08f + (rad * sin(a)).toFloat())
                        )
                    }
                }
            }

            WidgetCategory.STUDIO -> {
                // Polaroid Studio Aperture + Red Shutter Dot
                val frameSide = side * 0.78f
                val left = cx - frameSide / 2f
                val top = cy - frameSide / 2f
                drawRoundRect(
                    color = dot.copy(alpha = 0.65f),
                    topLeft = Offset(left, top),
                    size = Size(frameSide, frameSide),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )
                drawCircle(
                    color = ink,
                    radius = frameSide * 0.25f,
                    center = Offset(cx, cy - frameSide * 0.05f),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(color = accent, radius = 5.dp.toPx(), center = Offset(left + frameSide * 0.78f, top + frameSide * 0.22f))
            }

            WidgetCategory.TIME -> {
                // Crisp 12-Dot Analog Face with Red Hour Pip
                val r = side * 0.40f
                for (i in 0 until 12) {
                    val a = (-90.0 + i * 30.0) * PI / 180.0
                    drawCircle(
                        color = if (i % 3 == 0) ink else dot,
                        radius = if (i % 3 == 0) 3.dp.toPx() else 2.dp.toPx(),
                        center = Offset(cx + (r * cos(a)).toFloat(), cy + (r * sin(a)).toFloat())
                    )
                }
                drawLine(ink, Offset(cx, cy), Offset(cx + r * 0.55f, cy - r * 0.35f), 2.5.dp.toPx(), StrokeCap.Round)
                drawCircle(color = accent, radius = 5.5.dp.toPx(), center = Offset(cx - r * 0.35f, cy - r * 0.55f))
            }

            WidgetCategory.SYSTEM -> {
                // Circular Battery Ring + Minimal Phone Silhouette (inspired directly by reference Battery card!)
                val ringR = side * 0.40f
                drawArc(
                    color = dot.copy(alpha = 0.75f),
                    startAngle = -90f,
                    sweepAngle = 270f,
                    useCenter = true,
                    topLeft = Offset(cx - ringR, cy - ringR),
                    size = Size(ringR * 2, ringR * 2)
                )
                val pw = side * 0.26f
                val ph = side * 0.42f
                drawRoundRect(
                    color = ink,
                    topLeft = Offset(cx - pw / 2f, cy - ph / 2f),
                    size = Size(pw, ph),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                // Tiny detail dots on phone back
                drawCircle(color = surface, radius = 2.dp.toPx(), center = Offset(cx - pw * 0.2f, cy - ph * 0.24f))
                drawCircle(color = accent, radius = 2.2.dp.toPx(), center = Offset(cx + pw * 0.2f, cy - ph * 0.24f))
                for (i in -1..1) {
                    drawCircle(color = surface, radius = 1.6.dp.toPx(), center = Offset(cx + i * pw * 0.22f, cy + ph * 0.28f))
                }
            }

            WidgetCategory.FOCUS -> {
                // 4×4 Dot Calendar Matrix with Red Active Day
                val cols = 4
                val step = (side * 0.76f) / cols
                val sx = cx - (step * cols) / 2f
                val sy = cy - (step * cols) / 2f
                for (r in 0 until cols) {
                    for (c in 0 until cols) {
                        val isToday = r == 1 && c == 2
                        drawCircle(
                            color = if (isToday) accent else if (r == 0) ink else dot,
                            radius = if (isToday) step * 0.32f else step * 0.22f,
                            center = Offset(sx + (c + 0.5f) * step, sy + (r + 0.5f) * step)
                        )
                    }
                }
            }

            WidgetCategory.WELLNESS -> {
                // 7-Dot Habit Streak Arc with Red Leading Dot
                val r = side * 0.36f
                for (i in 0 until 7) {
                    val deg = 140.0 + i * (260.0 / 6.0)
                    val a = deg * PI / 180.0
                    drawCircle(
                        color = when {
                            i == 5 -> accent
                            i < 5 -> ink
                            else -> dot
                        },
                        radius = if (i == 5) 5.5.dp.toPx() else 4.dp.toPx(),
                        center = Offset(cx + (r * cos(a)).toFloat(), cy + (r * sin(a)).toFloat())
                    )
                }
            }
        }
    }
}
