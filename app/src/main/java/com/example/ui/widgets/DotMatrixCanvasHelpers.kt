package com.example.ui.widgets

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.model.DotDensity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun DotGridBackdropCanvas(
    dotColor: Color,
    density: DotDensity,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val stepPx = density.stepDp.dp.toPx().coerceAtLeast(12f)
        val radius = 1.dp.toPx()
        var y = stepPx * 0.7f
        while (y < size.height - stepPx * 0.3f) {
            var x = stepPx * 0.7f
            while (x < size.width - stepPx * 0.3f) {
                drawCircle(color = dotColor.copy(alpha = 0.42f), radius = radius, center = Offset(x, y))
                x += stepPx
            }
            y += stepPx
        }
    }
}

@Composable
fun DotProgressRing(
    progress: Float,
    dotCount: Int = 24,
    activeColor: Color,
    inactiveColor: Color,
    accentHeadColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    Canvas(modifier = modifier) {
        val radius = min(size.width, size.height) * 0.42f
        val center = Offset(size.width / 2f, size.height / 2f)
        val dotRadius = (min(size.width, size.height) * 0.036f).coerceIn(2.2f, 5.5f)
        val activeDots = (clamped * dotCount).toInt()
        for (i in 0 until dotCount) {
            val angleRad = (-90.0 + (360.0 / dotCount) * i) * PI / 180.0
            val pos = Offset(
                x = center.x + (radius * cos(angleRad)).toFloat(),
                y = center.y + (radius * sin(angleRad)).toFloat()
            )
            val isHead = accentHeadColor != null && activeDots > 0 && i == activeDots - 1
            val color = when {
                isHead -> accentHeadColor!!
                i < activeDots -> activeColor
                else -> inactiveColor
            }
            drawCircle(
                color = color,
                radius = if (isHead) dotRadius * 1.3f else dotRadius,
                center = pos
            )
        }
    }
}

@Composable
fun DotProgressBar(
    progress: Float,
    segments: Int = 16,
    activeColor: Color,
    inactiveColor: Color,
    accentTail: Color? = null,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    Canvas(modifier = modifier) {
        if (segments <= 0) return@Canvas
        val activeCount = (clamped * segments).toInt().coerceIn(0, segments)
        val spacing = size.width / segments.toFloat()
        val dotRadius = min(spacing * 0.34f, size.height * 0.42f).coerceIn(2.2f, 7f)
        val cy = size.height / 2f
        for (i in 0 until segments) {
            val cx = spacing * i + spacing / 2f
            val isTail = accentTail != null && activeCount > 0 && i == activeCount - 1
            val color = when {
                isTail -> accentTail!!
                i < activeCount -> activeColor
                else -> inactiveColor
            }
            drawCircle(
                color = color,
                radius = if (isTail) dotRadius * 1.18f else dotRadius,
                center = Offset(cx, cy)
            )
        }
    }
}

@Composable
fun DotWifiGlyph(
    isOn: Boolean,
    ink: Color,
    inactive: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height * 0.78f
        val maxR = min(size.width, size.height) * 0.62f
        val dotR = (min(size.width, size.height) * 0.055f).coerceIn(2f, 4.5f)
        // Center root dot
        drawCircle(color = if (isOn) accent else inactive, radius = dotR * 1.25f, center = Offset(cx, cy))
        // 3 dotted Wi-Fi arcs
        val radii = listOf(maxR * 0.42f, maxR * 0.72f, maxR)
        val counts = listOf(5, 7, 9)
        radii.forEachIndexed { idx, r ->
            val n = counts[idx]
            for (i in 0 until n) {
                val deg = -135.0 + (90.0 * i / (n - 1).coerceAtLeast(1))
                val rad = deg * PI / 180.0
                drawCircle(
                    color = if (isOn) ink else inactive,
                    radius = dotR * 0.85f,
                    center = Offset(cx + (r * cos(rad)).toFloat(), cy + (r * sin(rad)).toFloat())
                )
            }
        }
    }
}

@Composable
fun DotBluetoothGlyph(
    isOn: Boolean,
    ink: Color,
    inactive: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val h = size.height * 0.78f
        val w = size.width * 0.44f
        val top = Offset(cx, cy - h / 2f)
        val bot = Offset(cx, cy + h / 2f)
        val topRight = Offset(cx + w * 0.55f, cy - h * 0.22f)
        val botRight = Offset(cx + w * 0.55f, cy + h * 0.22f)
        val topLeft = Offset(cx - w * 0.55f, cy - h * 0.24f)
        val botLeft = Offset(cx - w * 0.55f, cy + h * 0.24f)

        val col = if (isOn) ink else inactive
        val stroke = 2.4.dp.toPx()
        drawLine(col, top, bot, stroke, StrokeCap.Round)
        drawLine(col, top, topRight, stroke, StrokeCap.Round)
        drawLine(col, topRight, botLeft, stroke, StrokeCap.Round)
        drawLine(col, topLeft, botRight, stroke, StrokeCap.Round)
        drawLine(col, botRight, bot, stroke, StrokeCap.Round)

        drawCircle(color = if (isOn) accent else inactive, radius = 3.2.dp.toPx(), center = Offset(cx + w * 0.85f, cy))
        drawCircle(color = if (isOn) accent else inactive, radius = 3.2.dp.toPx(), center = Offset(cx - w * 0.85f, cy))
    }
}

@Composable
fun DotEarbudsGlyph(
    connected: Boolean,
    ink: Color,
    muted: Color,
    inactive: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val mainColor = if (connected) ink else muted
        val stemW = w * 0.14f
        val stemH = h * 0.48f
        val headR = min(w, h) * 0.17f

        // Left Earbud (Nothing transparent stem style)
        val lx = w * 0.30f
        val ly = h * 0.34f
        drawCircle(color = mainColor, radius = headR, center = Offset(lx, ly))
        drawCircle(color = if (connected) ink else inactive, radius = headR * 0.42f, center = Offset(lx - headR * 0.35f, ly))
        drawRoundRect(
            color = mainColor,
            topLeft = Offset(lx - stemW / 2f, ly + headR * 0.3f),
            size = Size(stemW, stemH),
            cornerRadius = CornerRadius(stemW / 2f, stemW / 2f),
            style = Stroke(width = 2.dp.toPx())
        )
        // Subtle white dot inside left stem
        drawCircle(color = mainColor, radius = 2.2.dp.toPx(), center = Offset(lx, ly + headR * 0.8f))

        // Right Earbud (Signature Red Dot on Right Bud!)
        val rx = w * 0.70f
        val ry = h * 0.34f
        drawCircle(color = mainColor, radius = headR, center = Offset(rx, ry))
        drawRoundRect(
            color = mainColor,
            topLeft = Offset(rx - stemW / 2f, ry + headR * 0.3f),
            size = Size(stemW, stemH),
            cornerRadius = CornerRadius(stemW / 2f, stemW / 2f),
            style = Stroke(width = 2.dp.toPx())
        )
        // Signature Nothing red dot marking Right earbud
        drawCircle(color = if (connected) accent else inactive, radius = 2.8.dp.toPx(), center = Offset(rx, ry + headR * 0.8f))
    }
}

@Composable
fun DotEqualizerCanvas(
    isPlaying: Boolean,
    volumeFraction: Float,
    ink: Color,
    inactive: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "eq")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI.toFloat()),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eq_phase"
    )
    Canvas(modifier = modifier) {
        val cols = 7
        val rows = 5
        val dx = size.width / cols
        val dy = size.height / rows
        val r = min(dx, dy) * 0.28f
        for (c in 0 until cols) {
            val dynamicLevel = if (isPlaying) {
                (0.35f + 0.65f * ((sin(phase + c * 0.9f) + 1f) / 2f)) * volumeFraction.coerceAtLeast(0.3f)
            } else {
                (0.25f + 0.55f * ((c % 3) / 2f)) * volumeFraction.coerceAtLeast(0.2f)
            }
            val litRows = (dynamicLevel * rows).toInt().coerceIn(1, rows)
            for (row in 0 until rows) {
                val fromBottom = rows - 1 - row
                val isLit = fromBottom < litRows
                val isTopPeak = isLit && fromBottom == litRows - 1 && isPlaying
                val col = when {
                    isTopPeak -> accent
                    isLit -> ink
                    else -> inactive
                }
                drawCircle(
                    color = col,
                    radius = if (isTopPeak) r * 1.15f else r,
                    center = Offset(c * dx + dx / 2f, row * dy + dy / 2f)
                )
            }
        }
    }
}

@Composable
fun DotMoonSphere(
    phaseFraction: Float,
    litColor: Color,
    darkColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val grid = 9
        val side = min(size.width, size.height)
        val startX = (size.width - side) / 2f
        val startY = (size.height - side) / 2f
        val cell = side / grid.toFloat()
        val rMax = grid / 2f
        val dotR = (cell * 0.34f).coerceAtLeast(1.8f)

        for (row in 0 until grid) {
            for (col in 0 until grid) {
                val nx = (col + 0.5f - rMax) / rMax
                val ny = (row + 0.5f - rMax) / rMax
                val distSq = nx * nx + ny * ny
                if (distSq <= 0.96f) {
                    val halfChord = kotlin.math.sqrt((1f - ny * ny).coerceAtLeast(0.01f))
                    val normX = (nx / halfChord).coerceIn(-1f, 1f)
                    val terminator = cos(phaseFraction * 2f * PI.toFloat())
                    val isWaxing = phaseFraction <= 0.5f
                    val isLit = if (isWaxing) normX >= terminator else normX <= -terminator
                    val color = when {
                        isLit -> litColor
                        row == 1 && col == grid - 3 && phaseFraction < 0.04f -> accentColor
                        else -> darkColor
                    }
                    drawCircle(
                        color = color,
                        radius = if (isLit) dotR else dotR * 0.72f,
                        center = Offset(startX + (col + 0.5f) * cell, startY + (row + 0.5f) * cell)
                    )
                }
            }
        }
    }
}

@Composable
fun DotAnalogClockFace(
    hour: Int,
    minute: Int,
    second: Int,
    starlightMinimal: Boolean,
    colors: ResolvedWidgetColors,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val side = min(size.width, size.height)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = side * 0.44f

        val tickCount = if (starlightMinimal) 12 else 60
        for (i in 0 until tickCount) {
            val angle = (-90.0 + (360.0 / tickCount) * i) * PI / 180.0
            val isCardinal = if (starlightMinimal) i % 3 == 0 else i % 15 == 0
            val isHourTick = if (starlightMinimal) true else i % 5 == 0
            val dotR = when {
                isCardinal -> side * 0.022f
                isHourTick -> side * 0.015f
                else -> side * 0.008f
            }.coerceAtLeast(1.2f)
            val color = when {
                isCardinal -> colors.ink
                isHourTick -> colors.mutedInk
                else -> colors.dotInactive
            }
            drawCircle(
                color = color,
                radius = dotR,
                center = Offset(cx + (r * cos(angle)).toFloat(), cy + (r * sin(angle)).toFloat())
            )
        }

        val hourAngle = (-90.0 + ((hour % 12) + minute / 60.0) * 30.0) * PI / 180.0
        val minAngle = (-90.0 + (minute + second / 60.0) * 6.0) * PI / 180.0
        val secAngle = (-90.0 + second * 6.0) * PI / 180.0

        if (starlightMinimal) {
            drawLine(
                color = colors.mutedInk,
                start = Offset(cx, cy),
                end = Offset(cx + (r * 0.68f * cos(minAngle)).toFloat(), cy + (r * 0.68f * sin(minAngle)).toFloat()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = colors.accent,
                radius = (side * 0.048f).coerceAtLeast(4f),
                center = Offset(cx + (r * 0.72f * cos(hourAngle)).toFloat(), cy + (r * 0.72f * sin(hourAngle)).toFloat())
            )
            drawCircle(color = colors.ink, radius = 2.5.dp.toPx(), center = Offset(cx, cy))
        } else {
            drawLine(
                color = colors.ink,
                start = Offset(cx, cy),
                end = Offset(cx + (r * 0.50f * cos(hourAngle)).toFloat(), cy + (r * 0.50f * sin(hourAngle)).toFloat()),
                strokeWidth = 3.2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = colors.ink,
                start = Offset(cx, cy),
                end = Offset(cx + (r * 0.76f * cos(minAngle)).toFloat(), cy + (r * 0.76f * sin(minAngle)).toFloat()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = colors.accent,
                radius = (side * 0.032f).coerceAtLeast(3f),
                center = Offset(cx + (r * 0.86f * cos(secAngle)).toFloat(), cy + (r * 0.86f * sin(secAngle)).toFloat())
            )
            drawCircle(color = colors.accent, radius = 3.dp.toPx(), center = Offset(cx, cy))
        }
    }
}

@Composable
fun DotBinaryClockCanvas(
    hour: Int,
    minute: Int,
    second: Int,
    colors: ResolvedWidgetColors,
    modifier: Modifier = Modifier
) {
    val digits = intArrayOf(hour / 10, hour % 10, minute / 10, minute % 10, second / 10, second % 10)
    Canvas(modifier = modifier) {
        val cols = 6
        val rows = 4
        val colStep = size.width / cols
        val rowStep = size.height / rows
        val dotR = min(colStep, rowStep) * 0.28f
        for (c in 0 until cols) {
            val value = digits[c]
            for (r in 0 until rows) {
                val bit = 3 - r
                val isOn = ((value shr bit) and 1) == 1
                val color = when {
                    isOn && c >= 4 -> colors.accent
                    isOn -> colors.ink
                    else -> colors.dotInactive
                }
                drawCircle(
                    color = color,
                    radius = if (isOn) dotR * 1.05f else dotR * 0.75f,
                    center = Offset(colStep * c + colStep / 2f, rowStep * r + rowStep / 2f)
                )
            }
        }
    }
}

@Composable
fun DotYearMatrixCanvas(
    dayOfYear: Int,
    totalDays: Int,
    colors: ResolvedWidgetColors,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cols = 20
        val rows = 6
        val totalDots = cols * rows
        val progressDots = ((dayOfYear.toFloat() / totalDays.coerceAtLeast(365)) * totalDots).toInt().coerceIn(1, totalDots)
        val dx = size.width / cols
        val dy = size.height / rows
        val r = min(dx, dy) * 0.28f
        var idx = 0
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                idx++
                val color = when {
                    idx == progressDots -> colors.accent
                    idx < progressDots -> colors.ink
                    else -> colors.dotInactive
                }
                drawCircle(
                    color = color,
                    radius = if (idx == progressDots) r * 1.35f else r,
                    center = Offset(col * dx + dx / 2f, row * dy + dy / 2f)
                )
            }
        }
    }
}

@Composable
fun DotBreathingRingCanvas(
    isRunning: Boolean,
    colors: ResolvedWidgetColors,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "breathing")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isRunning) 4200 else 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_pulse"
    )
    Canvas(modifier = modifier) {
        val side = min(size.width, size.height)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = side * 0.44f
        val innerR = outerR * (if (isRunning) pulse else 0.62f)

        for (i in 0 until 16) {
            val a = i * (2 * PI / 16)
            drawCircle(
                color = colors.dotInactive,
                radius = 1.6.dp.toPx(),
                center = Offset(cx + (outerR * cos(a)).toFloat(), cy + (outerR * sin(a)).toFloat())
            )
        }
        for (i in 0 until 12) {
            val a = i * (2 * PI / 12)
            drawCircle(
                color = if (isRunning && i % 3 == 0) colors.accent else colors.ink,
                radius = 2.6.dp.toPx(),
                center = Offset(cx + (innerR * cos(a)).toFloat(), cy + (innerR * sin(a)).toFloat())
            )
        }
        drawCircle(
            color = if (isRunning) colors.accent else colors.mutedInk,
            radius = 4.dp.toPx(),
            style = Stroke(width = 1.6.dp.toPx()),
            center = Offset(cx, cy)
        )
    }
}
