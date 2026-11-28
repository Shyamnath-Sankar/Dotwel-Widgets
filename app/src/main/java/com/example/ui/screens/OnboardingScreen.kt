package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppSurfaceMode
import com.example.ui.theme.DotMatrixFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalDotGridColors
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

private data class OnboardingPageSpec(
    val indexLabel: String,
    val badge: String,
    val title: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPageSpec(
        indexLabel = "01 / 04",
        badge = "56 MONOCHROME WIDGETS",
        title = "DESIGNED FOR\nPURE CLARITY.",
        description = "Crafted in crisp dot-matrix geometry across Light, Dark, and zero-glare OLED Night modes."
    ),
    OnboardingPageSpec(
        indexLabel = "02 / 04",
        badge = "NIGHTING ESSENTIALS",
        title = "ZERO-GLARE\nOLED NIGHTS.",
        description = "True-black bedside clocks, real-time lunar matrices, and scheduled Auto-Night comfort."
    ),
    OnboardingPageSpec(
        indexLabel = "03 / 04",
        badge = "CONTROLS · BUDS · STUDIO",
        title = "LIVE HARDWARE\n& PHOTO STUDIO.",
        description = "Wi-Fi, Bluetooth, Internet panels, Ear Buds audio, monochrome Photo Studio, and rotating Quotes."
    ),
    OnboardingPageSpec(
        indexLabel = "04 / 04",
        badge = "1:1 EXACT PLACEMENT",
        title = "PIN TO HOME\nPIXEL-PERFECT.",
        description = "Every 2×2, 4×2, and 4×4 widget snaps cleanly onto your Home Screen with zero stretch and ultra-low drain."
    )
)

@Composable
fun OnboardingScreen(
    currentSurfaceMode: AppSurfaceMode,
    onSurfaceModeChange: (AppSurfaceMode) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDotGridColors.current
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingPages.lastIndex

    // Single shared infinite timeline drives all kinetic canvas illustrations at locked 60-120fps
    val infinite = rememberInfiniteTransition(label = "onboarding_timeline")
    val masterPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "master_phase"
    )
    val breathe by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ext.canvas)
            .cosmicDotBackdrop(ext.dotMatrix, ext.isLightMode)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header: Brand wordmark + SKIP pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "ANYWIDGETS",
                    fontFamily = DotMatrixFontFamily,
                    fontSize = 20.sp,
                    letterSpacing = 1.6.sp,
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

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(ext.cardSurface)
                    .border(1.dp, ext.hairline, RoundedCornerShape(50))
                    .clickable(onClick = onFinish)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("onboarding_skip_btn")
            ) {
                Text(
                    text = "SKIP",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = ext.mutedInk
                )
            }
        }

        // Interactive Parallax Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 560.dp)
        ) { page ->
            val spec = onboardingPages[page]
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val absOffset = abs(pageOffset).coerceIn(0f, 1f)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Kinetic Hero Artwork Card with GPU parallax + scale
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 360.dp)
                        .aspectRatio(1.06f)
                        .graphicsLayer {
                            val s = 1f - absOffset * 0.08f
                            scaleX = s
                            scaleY = s
                            alpha = 1f - absOffset * 0.45f
                            translationX = -pageOffset * 36f
                        }
                        .clip(RoundedCornerShape(34.dp))
                        .background(ext.cardSurface)
                        .border(1.dp, ext.hairline, RoundedCornerShape(34.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when (page) {
                        0 -> Page1KineticDotMatrixCanvas(
                            phase = masterPhase,
                            breathe = breathe,
                            ink = ext.ink,
                            muted = ext.mutedInk,
                            dot = ext.dotMatrix,
                            accent = ext.redAccent,
                            insetBg = ext.insetSurface
                        )
                        1 -> Page2KineticLunarNightCanvas(
                            phase = masterPhase,
                            breathe = breathe,
                            ink = ext.ink,
                            muted = ext.mutedInk,
                            dot = ext.dotMatrix,
                            accent = ext.redAccent,
                            insetBg = ext.insetSurface
                        )
                        2 -> Page3KineticControlsStudioCanvas(
                            phase = masterPhase,
                            breathe = breathe,
                            ink = ext.ink,
                            muted = ext.mutedInk,
                            dot = ext.dotMatrix,
                            accent = ext.redAccent,
                            insetBg = ext.insetSurface,
                            cardBg = ext.cardSurface
                        )
                        else -> Page4KineticHomeDropCanvas(
                            phase = masterPhase,
                            ink = ext.ink,
                            muted = ext.mutedInk,
                            dot = ext.dotMatrix,
                            accent = ext.redAccent,
                            insetBg = ext.insetSurface
                        )
                    }

                    // Top-left step badge inside hero stage
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp)
                            .clip(RoundedCornerShape(50))
                            .background(ext.pillSurface.copy(alpha = 0.92f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ext.redAccent))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = spec.indexLabel,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            color = ext.ink
                        )
                    }
                }

                // Editorial Copy Block with Parallax Slide
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .graphicsLayer {
                            translationX = -pageOffset * 85f
                            alpha = (1f - absOffset * 1.1f).coerceIn(0f, 1f)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = spec.badge,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        letterSpacing = 1.4.sp,
                        color = ext.redAccent
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = spec.title,
                        fontFamily = DotMatrixFontFamily,
                        fontSize = 30.sp,
                        lineHeight = 35.sp,
                        letterSpacing = 1.2.sp,
                        color = ext.ink,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = spec.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.mutedInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 320.dp)
                    )

                    // On Page 1, let users feel the Light / Dark / OLED Night theme live right away!
                    if (page == 0) {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(ext.cardSurface)
                                .border(1.dp, ext.hairline, RoundedCornerShape(50))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                Triple(AppSurfaceMode.OLED_NIGHT, "OLED", Icons.Default.NightsStay),
                                Triple(AppSurfaceMode.DARK, "DARK", Icons.Default.DarkMode),
                                Triple(AppSurfaceMode.LIGHT, "LIGHT", Icons.Default.LightMode)
                            ).forEach { (mode, label, icon) ->
                                val selected = currentSurfaceMode == mode
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (selected) ext.ink else Color.Transparent)
                                        .clickable { onSurfaceModeChange(mode) }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                        .testTag("onboard_theme_${mode.name.lowercase()}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (selected) ext.redAccent else ext.mutedInk,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                    Text(
                                        text = label,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.sp,
                                        color = if (selected) ext.canvas else ext.mutedInk
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Controls: Morphing Dot Indicators + Tactile Spring CTA Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(top = 8.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Morphing dot-matrix pager indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(onboardingPages.size) { index ->
                    val selected = pagerState.currentPage == index
                    val pillWidth by animateDpAsState(
                        targetValue = if (selected) 28.dp else 8.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "dot_w_$index"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (selected) ext.redAccent else ext.dotMatrix,
                        animationSpec = tween(220),
                        label = "dot_c_$index"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(pillWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }

            // Spring-bounce primary CTA pill
            val btnInteraction = remember { MutableInteractionSource() }
            val pressed by btnInteraction.collectIsPressedAsState()
            val btnScale by animateFloatAsState(
                targetValue = if (pressed) 0.94f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "cta_scale"
            )
            val btnBg by animateColorAsState(
                targetValue = if (isLastPage) ext.redAccent else ext.ink,
                animationSpec = tween(240),
                label = "cta_bg"
            )
            val btnContentColor = if (isLastPage) Color.White else ext.canvas

            Row(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = btnScale
                        scaleY = btnScale
                    }
                    .clip(RoundedCornerShape(50))
                    .background(btnBg)
                    .clickable(
                        interactionSource = btnInteraction,
                        indication = androidx.compose.material3.ripple(color = btnContentColor)
                    ) {
                        if (isLastPage) {
                            onFinish()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    page = pagerState.currentPage + 1,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    }
                    .padding(horizontal = 22.dp, vertical = 14.dp)
                    .testTag("onboarding_next_btn"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isLastPage) "GET STARTED" else "NEXT",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = btnContentColor
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (isLastPage) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (isLastPage) Color.White else ext.redAccent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Page 1: Hypnotic 9×9 Sonar-Wave Dot Matrix + Orbiting 24-Dot Ring & Glowing Red Laser Pip.
 */
@Composable
private fun Page1KineticDotMatrixCanvas(
    phase: Float,
    breathe: Float,
    ink: Color,
    muted: Color,
    dot: Color,
    accent: Color,
    insetBg: Color
) {
    Canvas(modifier = Modifier.fillMaxSize().padding(18.dp)) {
        val side = min(size.width, size.height)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val bgR = side * 0.46f

        drawCircle(color = insetBg, radius = bgR, center = Offset(cx, cy))

        // Outer rotating 24-dot clock/precision ring
        val outerR = side * 0.40f
        val orbitAngle = phase * 2f * PI.toFloat()
        for (i in 0 until 24) {
            val a = (-90f + i * 15f) * PI.toFloat() / 180f + orbitAngle * 0.25f
            val isLead = i == 0 || i == 12
            drawCircle(
                color = if (i == 0) accent else if (isLead) ink else dot,
                radius = if (i == 0) 4.5.dp.toPx() else if (isLead) 2.6.dp.toPx() else 1.7.dp.toPx(),
                center = Offset(cx + outerR * cos(a), cy + outerR * sin(a))
            )
        }

        // Inner 9×9 circular dot constellation pulsing with a radial wave
        val grid = 9
        val boxSide = side * 0.56f
        val step = boxSide / grid
        val startX = cx - boxSide / 2f
        val startY = cy - boxSide / 2f
        val waveRadius = (phase * 1.6f) % 1.6f

        for (r in 0 until grid) {
            for (c in 0 until grid) {
                val nx = (c + 0.5f - grid / 2f) / (grid / 2f)
                val ny = (r + 0.5f - grid / 2f) / (grid / 2f)
                val dist = hypot(nx, ny)
                if (dist <= 0.96f) {
                    val waveHit = (1f - abs(dist - waveRadius) * 3.2f).coerceIn(0f, 1f)
                    val isAccentDot = (r == 1 && c == grid - 3) || (waveHit > 0.72f && c > grid / 2)
                    val baseR = step * (0.20f + 0.17f * waveHit + 0.04f * breathe)
                    drawCircle(
                        color = when {
                            isAccentDot -> accent
                            waveHit > 0.30f -> ink
                            else -> muted.copy(alpha = 0.55f)
                        },
                        radius = baseR,
                        center = Offset(startX + (c + 0.5f) * step, startY + (r + 0.5f) * step)
                    )
                }
            }
        }
    }
}

/**
 * Page 2: Kinetic Lunar Eclipse Dot Sphere + Nocturnal Radar Ripples & Orbiting Red Pip.
 */
@Composable
private fun Page2KineticLunarNightCanvas(
    phase: Float,
    breathe: Float,
    ink: Color,
    muted: Color,
    dot: Color,
    accent: Color,
    insetBg: Color
) {
    Canvas(modifier = Modifier.fillMaxSize().padding(18.dp)) {
        val side = min(size.width, size.height)
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Expanding soft radar rings
        for (ringIdx in 0..2) {
            val p = (phase + ringIdx / 3f) % 1f
            val r = side * (0.22f + p * 0.25f)
            drawCircle(
                color = accent.copy(alpha = (1f - p) * 0.35f),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 1.4.dp.toPx())
            )
        }

        drawCircle(color = insetBg, radius = side * 0.34f, center = Offset(cx, cy))

        // Morphing Lunar Phase 9×9 Dot Sphere (smoothly cycles crescent -> gibbous -> full!)
        val grid = 9
        val moonSide = side * 0.54f
        val cell = moonSide / grid
        val sx = cx - moonSide / 2f
        val sy = cy - moonSide / 2f
        val rMax = grid / 2f
        val lunarCycle = (phase * 0.9f + 0.05f) % 1f

        for (row in 0 until grid) {
            for (col in 0 until grid) {
                val nx = (col + 0.5f - rMax) / rMax
                val ny = (row + 0.5f - rMax) / rMax
                if (nx * nx + ny * ny <= 0.96f) {
                    val halfChord = kotlin.math.sqrt((1f - ny * ny).coerceAtLeast(0.01f))
                    val normX = (nx / halfChord).coerceIn(-1f, 1f)
                    val term = cos(lunarCycle * 2f * PI.toFloat())
                    val isWaxing = lunarCycle <= 0.5f
                    val isLit = if (isWaxing) normX >= term else normX <= -term
                    val isTerminator = abs(normX - (if (isWaxing) term else -term)) < 0.22f
                    drawCircle(
                        color = when {
                            isTerminator -> accent
                            isLit -> ink
                            else -> dot
                        },
                        radius = cell * (if (isTerminator) 0.36f else if (isLit) 0.32f else 0.20f),
                        center = Offset(sx + (col + 0.5f) * cell, sy + (row + 0.5f) * cell)
                    )
                }
            }
        }

        // Orbiting nocturnal satellite pip
        val satAngle = phase * 2f * PI.toFloat() - PI.toFloat() / 2f
        val satR = side * 0.42f
        drawCircle(
            color = accent,
            radius = (4.5f + breathe * 1.5f).dp.toPx(),
            center = Offset(cx + satR * cos(satAngle), cy + satR * sin(satAngle))
        )
    }
}

/**
 * Page 3: Levitating Tactile Bento Trio (Nothing Earbuds + Equalizer, Wi-Fi/BT Pill, Photo Aperture).
 */
@Composable
private fun Page3KineticControlsStudioCanvas(
    phase: Float,
    breathe: Float,
    ink: Color,
    muted: Color,
    dot: Color,
    accent: Color,
    insetBg: Color,
    cardBg: Color
) {
    Canvas(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        val w = size.width
        val h = size.height
        val floatA = sin(phase * 2f * PI.toFloat()) * 6.dp.toPx()
        val floatB = sin((phase + 0.33f) * 2f * PI.toFloat()) * 6.dp.toPx()
        val floatC = sin((phase + 0.66f) * 2f * PI.toFloat()) * 5.dp.toPx()

        // 1. Top-Wide Live Earbuds & Animated 7×5 Equalizer Card
        val topRect = Offset(w * 0.06f, h * 0.14f + floatA)
        val topSize = Size(w * 0.88f, h * 0.35f)
        drawRoundRect(color = insetBg, topLeft = topRect, size = topSize, cornerRadius = CornerRadius(22.dp.toPx()))
        drawRoundRect(color = dot, topLeft = topRect, size = topSize, cornerRadius = CornerRadius(22.dp.toPx()), style = Stroke(1.2.dp.toPx()))

        // Nothing Earbud stem on left of top card
        val ex = topRect.x + topSize.width * 0.20f
        val ey = topRect.y + topSize.height * 0.36f
        val eHead = topSize.height * 0.18f
        drawCircle(color = ink, radius = eHead, center = Offset(ex, ey))
        drawRoundRect(
            color = ink,
            topLeft = Offset(ex - eHead * 0.42f, ey + eHead * 0.3f),
            size = Size(eHead * 0.84f, topSize.height * 0.42f),
            cornerRadius = CornerRadius(eHead * 0.42f),
            style = Stroke(2.dp.toPx())
        )
        drawCircle(color = accent, radius = 3.2.dp.toPx(), center = Offset(ex, ey + eHead * 0.85f))

        // Live bouncing 7-column dot equalizer on right of top card
        val eqLeft = topRect.x + topSize.width * 0.38f
        val eqW = topSize.width * 0.54f
        val eqTop = topRect.y + topSize.height * 0.22f
        val eqH = topSize.height * 0.56f
        val cols = 7
        val rows = 5
        val dx = eqW / cols
        val dy = eqH / rows
        val er = min(dx, dy) * 0.28f
        for (c in 0 until cols) {
            val lvl = 0.25f + 0.75f * ((sin(phase * 4f * PI.toFloat() + c * 0.85f) + 1f) / 2f)
            val lit = (lvl * rows).toInt().coerceIn(1, rows)
            for (r in 0 until rows) {
                val fromBot = rows - 1 - r
                val isLit = fromBot < lit
                val isPeak = isLit && fromBot == lit - 1
                drawCircle(
                    color = if (isPeak) accent else if (isLit) ink else dot,
                    radius = if (isPeak) er * 1.15f else er,
                    center = Offset(eqLeft + c * dx + dx / 2f, eqTop + r * dy + dy / 2f)
                )
            }
        }

        // 2. Bottom-Left Animated Wi-Fi / Internet Toggle Tile
        val blPos = Offset(w * 0.06f, h * 0.56f + floatB)
        val sqSize = Size(w * 0.41f, h * 0.34f)
        drawRoundRect(color = insetBg, topLeft = blPos, size = sqSize, cornerRadius = CornerRadius(20.dp.toPx()))
        drawRoundRect(color = dot, topLeft = blPos, size = sqSize, cornerRadius = CornerRadius(20.dp.toPx()), style = Stroke(1.2.dp.toPx()))

        // Animated Wi-Fi 3-arc wave
        val wcx = blPos.x + sqSize.width / 2f
        val wcy = blPos.y + sqSize.height * 0.68f
        val maxR = min(sqSize.width, sqSize.height) * 0.46f
        drawCircle(color = accent, radius = 3.5.dp.toPx(), center = Offset(wcx, wcy))
        val activeArc = ((phase * 6f).toInt()) % 4
        listOf(0.42f, 0.72f, 1.0f).forEachIndexed { idx, frac ->
            val n = 5 + idx * 2
            val arcLit = idx < activeArc
            for (i in 0 until n) {
                val deg = -135.0 + (90.0 * i / (n - 1))
                val a = deg * PI / 180.0
                drawCircle(
                    color = if (arcLit) ink else dot,
                    radius = 2.1.dp.toPx(),
                    center = Offset(wcx + (maxR * frac * cos(a)).toFloat(), wcy + (maxR * frac * sin(a)).toFloat())
                )
            }
        }

        // 3. Bottom-Right Photo Studio Aperture Tile
        val brPos = Offset(w * 0.53f, h * 0.56f + floatC)
        drawRoundRect(color = insetBg, topLeft = brPos, size = sqSize, cornerRadius = CornerRadius(20.dp.toPx()))
        drawRoundRect(color = dot, topLeft = brPos, size = sqSize, cornerRadius = CornerRadius(20.dp.toPx()), style = Stroke(1.2.dp.toPx()))
        val pcx = brPos.x + sqSize.width / 2f
        val pcy = brPos.y + sqSize.height / 2f
        val lensR = min(sqSize.width, sqSize.height) * (0.26f + 0.04f * breathe)
        drawCircle(color = ink, radius = lensR, center = Offset(pcx, pcy), style = Stroke(2.4.dp.toPx()))
        drawCircle(color = dot, radius = lensR * 0.55f, center = Offset(pcx, pcy), style = Stroke(1.4.dp.toPx()))
        drawCircle(
            color = accent,
            radius = (3.5f + breathe * 1.4f).dp.toPx(),
            center = Offset(brPos.x + sqSize.width * 0.78f, brPos.y + sqSize.height * 0.24f)
        )
        // Unused cardBg suppression
        if (cardBg.alpha < 0f) drawCircle(cardBg, 0f, Offset.Zero)
    }
}

/**
 * Page 4: Kinetic Home Screen Widget Drop & Snap-In Lock Animation.
 */
@Composable
private fun Page4KineticHomeDropCanvas(
    phase: Float,
    ink: Color,
    muted: Color,
    dot: Color,
    accent: Color,
    insetBg: Color
) {
    Canvas(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        val w = size.width
        val h = size.height
        val phoneW = w * 0.62f
        val phoneH = h * 0.84f
        val phoneLeft = (w - phoneW) / 2f
        val phoneTop = (h - phoneH) / 2f

        // Phone Home Screen Shell
        drawRoundRect(
            color = insetBg,
            topLeft = Offset(phoneLeft, phoneTop),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(26.dp.toPx())
        )
        drawRoundRect(
            color = dot,
            topLeft = Offset(phoneLeft, phoneTop),
            size = Size(phoneW, phoneH),
            cornerRadius = CornerRadius(26.dp.toPx()),
            style = Stroke(1.8.dp.toPx())
        )

        // 4×5 Launcher Grid Icons at bottom
        val gridPad = phoneW * 0.11f
        val cols = 4
        val cellW = (phoneW - gridPad * 2) / cols
        val iconR = cellW * 0.28f
        for (row in 2..4) {
            for (col in 0 until cols) {
                val icx = phoneLeft + gridPad + col * cellW + cellW / 2f
                val icy = phoneTop + phoneH * (0.50f + (row - 2) * 0.16f)
                drawCircle(color = dot.copy(alpha = 0.85f), radius = iconR, center = Offset(icx, icy))
            }
        }

        // Animated 4×2 Widget gliding into place & snapping cleanly with red lock ripple!
        val dropProgress = when {
            phase < 0.35f -> FastOutSlowInEasing.transform(phase / 0.35f)
            phase < 0.80f -> 1f
            else -> 1f - FastOutSlowInEasing.transform((phase - 0.80f) / 0.20f)
        }
        val widgetW = phoneW - gridPad * 2
        val widgetH = widgetW * 0.48f
        val targetY = phoneTop + phoneH * 0.14f
        val startY = targetY - 26.dp.toPx()
        val curY = startY + (targetY - startY) * dropProgress
        val scaleLift = 1.08f - 0.08f * dropProgress
        val drawW = widgetW * scaleLift
        val drawH = widgetH * scaleLift
        val drawX = (w - drawW) / 2f

        // Red snap-in confirmation pulse when seated
        if (phase in 0.35f..0.65f) {
            val pulseP = (phase - 0.35f) / 0.30f
            val expand = pulseP * 14.dp.toPx()
            drawRoundRect(
                color = accent.copy(alpha = (1f - pulseP) * 0.75f),
                topLeft = Offset(drawX - expand, curY - expand),
                size = Size(drawW + expand * 2, drawH + expand * 2),
                cornerRadius = CornerRadius(18.dp.toPx() + expand),
                style = Stroke(2.dp.toPx())
            )
        }

        // Crisp 4×2 Monochrome Widget Card
        drawRoundRect(
            color = ink,
            topLeft = Offset(drawX, curY),
            size = Size(drawW, drawH),
            cornerRadius = CornerRadius(16.dp.toPx())
        )
        // Signature red indicator dot + minimalist bars inside the snapped widget
        drawCircle(
            color = accent,
            radius = 3.5.dp.toPx(),
            center = Offset(drawX + 14.dp.toPx(), curY + 14.dp.toPx())
        )
        drawLine(
            color = insetBg,
            start = Offset(drawX + 14.dp.toPx(), curY + drawH * 0.58f),
            end = Offset(drawX + drawW * 0.62f, curY + drawH * 0.58f),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = muted,
            start = Offset(drawX + 14.dp.toPx(), curY + drawH * 0.78f),
            end = Offset(drawX + drawW * 0.40f, curY + drawH * 0.78f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
