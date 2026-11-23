package com.dotwelwidgets.dotwellabs

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.AnalogClock
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextClock
import android.widget.TextView
import java.util.Calendar
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Shared preview host for the HOME category tiles and the category sheet.
 *
 * It deliberately mirrors the placed-widget contract: minute bitmap kinds use
 * the real Dotwel renderer only when optional Alarms &amp; reminders access is
 * present. Without that access they show the same live platform fallback the
 * provider uses, so a frozen bitmap time is never presented as live.
 */
object WidgetPreviewBinder {
    fun bind(
        context: Context,
        container: FrameLayout,
        entry: CatalogEntry,
        style: WidgetStyle,
    ) {
        container.removeAllViews()
        container.contentDescription = context.getString(
            R.string.sheet_preview_description_format,
            entry.name,
        )
        if (entry.previewKind == PreviewKind.KWGT_ANALOG_3) {
            bindNativeAnalog3(context, container, style)
            return
        }
        if (entry.previewKind == PreviewKind.KWGT_ANALOG_5) {
            bindNativeAnalog5(context, container, style)
            return
        }
        if (WidgetPreviewRenderer.usesMinuteBitmap(entry.previewKind) &&
            !MinuteTickScheduler.hasExactAccess(context)
        ) {
            bindNativeFallback(context, container, entry, style)
            return
        }
        val width = container.width.takeIf { it > 0 } ?: dp(context, 220)
        val height = container.height.takeIf { it > 0 } ?: dp(context, 180)
        val bitmap = try {
            WidgetPreviewRenderer.render(context, entry, width, height, style)
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: RuntimeException) {
            null
        }
        if (bitmap != null) {
            container.addView(
                ImageView(context).apply {
                    setImageBitmap(bitmap)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    contentDescription = context.getString(
                        R.string.sheet_preview_description_format,
                        entry.name,
                    )
                },
                matchParams(),
            )
        } else {
            bindNativeFallback(context, container, entry, style, failed = true)
        }
    }

    private fun bindNativeAnalog3(
        context: Context,
        container: FrameLayout,
        style: WidgetStyle,
    ) {
        val layout = KwgtAnalog3WidgetProvider.layoutFor(style)
        container.addView(
            LayoutInflater.from(context).inflate(layout, container, false),
            matchParams(),
        )
    }

    private fun bindNativeAnalog5(
        context: Context,
        container: FrameLayout,
        style: WidgetStyle,
    ) {
        val layout = KwgtAnalog5WidgetProvider.layoutFor(style)
        container.addView(
            LayoutInflater.from(context).inflate(layout, container, false),
            matchParams(),
        )
    }

    private fun bindNativeFallback(
        context: Context,
        container: FrameLayout,
        entry: CatalogEntry,
        style: WidgetStyle,
        failed: Boolean = false,
    ) {
        val width = container.width.takeIf { it > 0 } ?: dp(context, 220)
        val height = container.height.takeIf { it > 0 } ?: dp(context, 180)
        val ink = UpstreamNtDraw.ink(context, style)
        val accent = UpstreamNtDraw.accent(context, style)
        val muted = UpstreamNtDraw.muted(context, style)
        when (entry.previewKind) {
            PreviewKind.CLOCK -> {
                container.addView(
                    liveClock(
                        context,
                        R.string.clock_format_12,
                        R.string.clock_format_24,
                        ink,
                    ),
                    matchParams(),
                )
            }
            PreviewKind.ANALOG_BOLD,
            PreviewKind.ANALOG_SCALE,
            -> {
                container.addView(
                    AnalogClock(context).apply {
                        contentDescription = context.getString(
                            R.string.analog_preview_description,
                        )
                    },
                    matchParams(),
                )
            }
            PreviewKind.WORLD -> {
                container.addView(
                    worldFallback(context, style, height),
                    matchParams(),
                )
            }
            PreviewKind.KWGT_DATE_TIME -> {
                container.addView(
                    dateTimeFallback(context, style, height, ink, accent),
                    matchParams(),
                )
            }
            PreviewKind.KWGT_DATE_TIME_2 -> {
                container.addView(
                    dateTime2Fallback(context, style, height, ink, accent),
                    matchParams(),
                )
            }
            PreviewKind.KWGT_DIGITAL_4 -> {
                container.addView(
                    splitClockFallback(context, ink),
                    matchParams(),
                )
            }
            PreviewKind.KWGT_DIGITAL_3 -> {
                container.addView(
                    digital3Fallback(context, style, width, height),
                    matchParams(),
                )
            }
            PreviewKind.KWGT_GRADIENT_CLOCK -> {
                container.addView(
                    gradientClockFallback(context, style, width, height),
                    matchParams(),
                )
            }
            PreviewKind.KWGT_CLEAN_INFO -> {
                container.addView(
                    cleanInfoFallback(context, style, width, height),
                    matchParams(),
                )
            }
            PreviewKind.KWGT_BATTERY_2 -> {
                val snapshot = Battery2State.snapshot(context)
                val estimate = KwgtBattery2Renderer.estimate(
                    snapshot.level,
                    snapshot.charging,
                    snapshot.remainingMillis,
                )
                val fallbackText = when (estimate.kind) {
                    KwgtBattery2Renderer.EstimateKind.FULL -> context.getString(
                        R.string.kwgt_battery2_fallback_full,
                        snapshot.level,
                    )
                    KwgtBattery2Renderer.EstimateKind.TO_FULL -> context.getString(
                        R.string.kwgt_battery2_fallback_to_full,
                        snapshot.level,
                        estimate.valueText,
                    )
                    KwgtBattery2Renderer.EstimateKind.UNAVAILABLE -> context.getString(
                        R.string.kwgt_battery2_fallback_unknown,
                        snapshot.level,
                    )
                }
                container.addView(
                    TextView(context).apply {
                        text = fallbackText
                        setTextColor(ink)
                        gravity = Gravity.CENTER
                        textSize = 12f
                        setPadding(
                            dp(context, 10),
                            dp(context, 10),
                            dp(context, 10),
                            dp(context, 10),
                        )
                    },
                    matchParams(),
                )
            }
            else -> {
                container.addView(
                    TextView(context).apply {
                        text = if (failed) {
                            context.getString(R.string.preview_render_failed_detail)
                        } else {
                            context.getString(entry.state.labelRes)
                        }
                        setTextColor(muted)
                        gravity = Gravity.CENTER
                        textSize = 12f
                        setPadding(dp(context, 10), dp(context, 10), dp(context, 10), dp(context, 10))
                    },
                    matchParams(),
                )
            }
        }
    }

    private fun liveClock(
        context: Context,
        format12Res: Int,
        format24Res: Int,
        color: Int,
    ): TextClock = TextClock(context).apply {
        setFormat12Hour(context.getString(format12Res))
        setFormat24Hour(context.getString(format24Res))
        setTextColor(color)
        gravity = Gravity.CENTER
        includeFontPadding = false
        typeface = Typeface.MONOSPACE
        setTypeface(typeface, Typeface.BOLD)
        maxLines = 2
        setLineSpacing(0f, 0.9f)
        setAutoSizeTextTypeUniformWithConfiguration(
            10,
            96,
            2,
            TypedValue.COMPLEX_UNIT_SP,
        )
        contentDescription = context.getString(R.string.clock_preview_description)
    }

    private fun worldFallback(
        context: Context,
        style: WidgetStyle,
        heightPx: Int,
    ): LinearLayout {
        val density = context.resources.displayMetrics.density
        val base = ((heightPx / density) / 4f * 0.55f).coerceIn(9f, 24f)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(context, 10),
                dp(context, 6),
                dp(context, 10),
                dp(context, 6),
            )
        }
        WorldPrefs.previewZones(context).take(2).forEach { zone ->
            layout.addView(
                TextView(context).apply {
                    text = zone.label.uppercase(Locale.getDefault())
                    setTextColor(UpstreamNtDraw.muted(context, style))
                    textSize = base * 0.85f
                    includeFontPadding = false
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                },
            )
            layout.addView(
                TextClock(context).apply {
                    setFormat12Hour(context.getString(R.string.world_time_format_12))
                    setFormat24Hour(context.getString(R.string.world_time_format_24))
                    setTimeZone(zone.zoneId)
                    setTextColor(UpstreamNtDraw.ink(context, style))
                    textSize = base
                    includeFontPadding = false
                    maxLines = 1
                    typeface = Typeface.create(
                        "sans-serif-condensed",
                        Typeface.BOLD,
                    )
                },
            )
        }
        return layout
    }

    private fun dateTimeFallback(
        context: Context,
        style: WidgetStyle,
        heightPx: Int,
        ink: Int,
        accent: Int,
    ): LinearLayout {
        val density = context.resources.displayMetrics.density
        val base = ((heightPx / density) / 5f * 0.6f).coerceIn(9f, 30f)
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(context, 12),
                dp(context, 6),
                dp(context, 12),
                dp(context, 6),
            )
        }
        layout.addView(
            dateTimeClock(context, "EEE", ink, base),
        )
        layout.addView(
            dateTimeClock(context, "hh:mma", accent, base * 1.15f),
        )
        layout.addView(
            dateTimeClock(context, "MMMM", ink, base),
        )
        val dayRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        dayRow.addView(dateTimeClock(context, "d", ink, base))
        dayRow.addView(
            TextView(context).apply {
                text = KwgtDateTimeRenderer.ordinalSuffix(
                    Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                )
                setTextColor(ink)
                textSize = base
                includeFontPadding = false
                maxLines = 1
                typeface = Typeface.MONOSPACE
                setTypeface(typeface, Typeface.BOLD)
            },
        )
        layout.addView(dayRow)
        return layout
    }

    private fun dateTimeClock(
        context: Context,
        format: String,
        color: Int,
        sizeSp: Float,
    ): TextClock = TextClock(context).apply {
        setFormat12Hour(format)
        setFormat24Hour(format)
        setTextColor(color)
        textSize = sizeSp
        includeFontPadding = false
        maxLines = 1
        typeface = Typeface.MONOSPACE
        setTypeface(typeface, Typeface.BOLD)
    }


    private fun dateTime2Fallback(
        context: Context,
        style: WidgetStyle,
        heightPx: Int,
        ink: Int,
        accent: Int,
    ): RelativeLayout {
        val density = context.resources.displayMetrics.density
        val base = ((heightPx / density) / 5f * 0.55f).coerceIn(8f, 28f)
        val root = RelativeLayout(context)
        val dateStack = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 12), dp(context, 6), 0, 0)
        }
        dateStack.addView(sourceClock(context, "EEE", ink, base * 0.9f))
        dateStack.addView(sourceClock(context, "d", accent, base * 2.4f, bold = true))
        val dateParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_START)
            addRule(RelativeLayout.ALIGN_PARENT_TOP)
        }
        root.addView(dateStack, dateParams)

        val timeStack = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            setPadding(0, 0, dp(context, 12), dp(context, 6))
        }
        timeStack.addView(sourceClock(context, "hh", ink, base * 1.1f))
        timeStack.addView(
            sourceClock(context, "mm", ink, base * 1.1f).apply {
                gravity = Gravity.END
            },
        )
        val timeParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            addRule(RelativeLayout.ALIGN_PARENT_END)
            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        }
        root.addView(timeStack, timeParams)
        return root
    }

    private fun gradientClockFallback(
        context: Context,
        style: WidgetStyle,
        widthPx: Int,
        heightPx: Int,
    ): FrameLayout {
        val root = FrameLayout(context)
        val background = try {
            KwgtGradientClockRenderer.renderStaticBackground(
                context,
                widthPx,
                heightPx,
                style,
            )
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: RuntimeException) {
            null
        }
        if (background != null) {
            root.addView(
                ImageView(context).apply {
                    setImageBitmap(background)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    contentDescription = null
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
                matchParams(),
            )
        }
        root.addView(
            TextClock(context).apply {
                setFormat12Hour("hh")
                setFormat24Hour("hh")
                setTextColor(UpstreamNtDraw.ink(context, style))
                gravity = Gravity.CENTER
                includeFontPadding = false
                maxLines = 1
                typeface = Typeface.MONOSPACE
                setTypeface(typeface, Typeface.BOLD)
                setPadding(0, 0, dp(context, 42), 0)
                setAutoSizeTextTypeUniformWithConfiguration(
                    8,
                    96,
                    2,
                    TypedValue.COMPLEX_UNIT_SP,
                )
            },
            matchParams(),
        )
        val chipSide = (min(widthPx, heightPx) / 6f)
            .toInt()
            .coerceAtLeast(dp(context, 18))
        root.addView(
            TextClock(context).apply {
                setFormat12Hour("mm")
                setFormat24Hour("mm")
                setTextColor(KwgtGradientClockRenderer.minuteInk(context, style))
                gravity = Gravity.CENTER
                includeFontPadding = false
                maxLines = 1
                typeface = Typeface.MONOSPACE
                setTypeface(typeface, Typeface.BOLD)
                setBackgroundResource(R.drawable.bg_kwgt_gradient_minute_chip)
                setAutoSizeTextTypeUniformWithConfiguration(
                    7,
                    20,
                    1,
                    TypedValue.COMPLEX_UNIT_SP,
                )
            },
            FrameLayout.LayoutParams(
                chipSide,
                chipSide,
                Gravity.END or Gravity.CENTER_VERTICAL,
            ),
        )
        return root
    }

    private fun digital3Fallback(
        context: Context,
        style: WidgetStyle,
        widthPx: Int,
        heightPx: Int,
    ): FrameLayout {
        val root = FrameLayout(context)
        val background = try {
            KwgtDigital3Renderer.renderDots(
                context,
                widthPx,
                heightPx,
                style,
                Calendar.getInstance(),
                includeBackground = true,
            )
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: RuntimeException) {
            null
        }
        if (background != null) {
            root.addView(
                ImageView(context).apply {
                    setImageBitmap(background)
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    contentDescription = null
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
                matchParams(),
            )
        }
        val boundary = KwgtDigital3Renderer.layout(
            widthPx.toFloat(),
            heightPx.toFloat(),
        )
        val palette = KwgtDigital3Renderer.palette(style)
        root.addView(
            digital3Clock(
                context,
                "hh",
                palette.foreground,
                boundary.hourRegion,
                widthPx,
                heightPx,
            ),
            matchParams(),
        )
        root.addView(
            digital3Clock(
                context,
                "mm",
                palette.foreground,
                boundary.minuteRegion,
                widthPx,
                heightPx,
            ),
            matchParams(),
        )
        root.addView(
            digital3Clock(
                context,
                "a",
                palette.contrastInk,
                boundary.ampmRegion,
                widthPx,
                heightPx,
            ),
            matchParams(),
        )
        return root
    }

    private fun digital3Clock(
        context: Context,
        format: String,
        color: Int,
        region: KwgtDigital3Renderer.TextRegion,
        widthPx: Int,
        heightPx: Int,
    ): TextClock = TextClock(context).apply {
        setFormat12Hour(format)
        setFormat24Hour(format)
        setTextColor(color)
        gravity = Gravity.CENTER
        includeFontPadding = false
        maxLines = 1
        typeface = Typeface.MONOSPACE
        setTypeface(typeface, Typeface.BOLD)
        setPadding(
            region.left.roundToInt().coerceIn(0, widthPx),
            region.top.roundToInt().coerceIn(0, heightPx),
            (widthPx - region.right).roundToInt().coerceIn(0, widthPx),
            (heightPx - region.bottom).roundToInt().coerceIn(0, heightPx),
        )
        setAutoSizeTextTypeUniformWithConfiguration(
            7,
            72,
            1,
            TypedValue.COMPLEX_UNIT_SP,
        )
    }

    private fun cleanInfoFallback(
        context: Context,
        style: WidgetStyle,
        widthPx: Int,
        heightPx: Int,
    ): FrameLayout {
        val layout = KwgtCleanInfoRenderer.layout(
            widthPx.toFloat(),
            heightPx.toFloat(),
        )
        val palette = KwgtCleanInfoRenderer.palette(context, style)
        val root = FrameLayout(context)
        val background = try {
            KwgtCleanInfoRenderer.renderBackground(context, widthPx, heightPx, style)
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: RuntimeException) {
            null
        }
        if (background != null) {
            root.addView(
                ImageView(context).apply {
                    setImageBitmap(background)
                    scaleType = ImageView.ScaleType.FIT_XY
                    contentDescription = null
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
                matchParams(),
            )
        }
        root.addView(
            TextClock(context).apply {
                setFormat12Hour("h:mm")
                setFormat24Hour("H:mm")
                setTextColor(palette.timeInk)
                gravity = Gravity.CENTER_VERTICAL or Gravity.END
                includeFontPadding = false
                maxLines = 1
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, layout.timeTextSize)
                setPadding(0, 0, layout.rightPad.roundToInt().coerceIn(0, widthPx), 0)
            },
            matchParams(),
        )
        val stack = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM or Gravity.START
            setPadding(
                layout.leftPad.roundToInt().coerceIn(0, widthPx),
                0,
                0,
                layout.bottomPad.roundToInt().coerceIn(0, heightPx),
            )
        }
        stack.addView(
            TextClock(context).apply {
                setFormat12Hour("EEEE")
                setFormat24Hour("EEEE")
                setTextColor(palette.foreground)
                includeFontPadding = false
                maxLines = 1
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, layout.weekdayTextSize)
            },
        )
        stack.addView(
            TextClock(context).apply {
                setFormat12Hour("MMMM d")
                setFormat24Hour("MMMM d")
                setTextColor(palette.foreground)
                includeFontPadding = false
                maxLines = 1
                setAllCaps(true)
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, layout.dateTextSize)
            },
        )
        root.addView(stack, matchParams())
        val body = layout.batteryBody
        root.addView(
            TextView(context).apply {
                text = KwgtCleanInfoRenderer.batteryText(BatteryState.level(context))
                setTextColor(palette.batteryInk)
                gravity = Gravity.CENTER
                includeFontPadding = false
                maxLines = 1
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, layout.batteryTextSize)
                setPadding(
                    body.left.roundToInt().coerceIn(0, widthPx),
                    body.top.roundToInt().coerceIn(0, heightPx),
                    (widthPx - body.right).roundToInt().coerceIn(0, widthPx),
                    (heightPx - body.bottom).roundToInt().coerceIn(0, heightPx),
                )
            },
            matchParams(),
        )
        return root
    }

    private fun splitClockFallback(
        context: Context,
        ink: Int,
    ): LinearLayout {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        layout.addView(
            splitClock(context, "hh", KwgtDigital4Renderer.RED, Gravity.END),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f),
        )
        layout.addView(
            View(context).apply {
                setBackgroundColor(KwgtDigital4Renderer.RED)
            },
            LinearLayout.LayoutParams(
                dp(context, 1).coerceAtLeast(1),
                LinearLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        layout.addView(
            splitClock(context, "mm", ink, Gravity.START),
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f),
        )
        return layout
    }

    private fun splitClock(
        context: Context,
        format: String,
        color: Int,
        gravityValue: Int,
    ): TextClock = TextClock(context).apply {
        setFormat12Hour(format)
        setFormat24Hour(format)
        setTextColor(color)
        gravity = gravityValue or Gravity.CENTER_VERTICAL
        includeFontPadding = false
        maxLines = 1
        typeface = Typeface.MONOSPACE
        setTypeface(typeface, Typeface.BOLD)
        setAutoSizeTextTypeUniformWithConfiguration(
            8,
            96,
            2,
            TypedValue.COMPLEX_UNIT_SP,
        )
    }

    private fun sourceClock(
        context: Context,
        format: String,
        color: Int,
        sizeSp: Float,
        bold: Boolean = false,
    ): TextClock = TextClock(context).apply {
        setFormat12Hour(format)
        setFormat24Hour(
            when (format) {
                "hh" -> "HH"
                else -> format
            },
        )
        setTextColor(color)
        textSize = sizeSp
        includeFontPadding = false
        maxLines = 1
        typeface = Typeface.create(Typeface.SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun matchParams(): FrameLayout.LayoutParams =
        FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
        )

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
