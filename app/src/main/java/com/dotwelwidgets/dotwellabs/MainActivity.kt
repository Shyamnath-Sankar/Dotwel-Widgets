package com.dotwelwidgets.dotwellabs

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextClock
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applySystemBarInsets()
        applyLocaleDateFormat()

        val preview = findViewById<View>(R.id.preview_card)
        val group = findViewById<RadioGroup>(R.id.style_group)
        val current = WidgetPrefs.style(this)

        group.check(buttonFor(current))
        applyPreview(preview, current)

        group.setOnCheckedChangeListener { _, checkedId ->
            val style = styleFor(checkedId) ?: return@setOnCheckedChangeListener
            WidgetPrefs.setStyle(this, style)
            applyPreview(preview, style)
            WidgetUpdater.updateAll(this)
        }

        findViewById<Button>(R.id.refresh_widgets).setOnClickListener {
            WidgetUpdater.updateAll(this)
            Toast.makeText(this, R.string.widgets_refreshed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyLocaleDateFormat() {
        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "yMMMMEEEEd")
        findViewById<TextClock>(R.id.preview_date).apply {
            format12Hour = pattern
            format24Hour = pattern
        }
    }

    private fun applySystemBarInsets() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        window.setDecorFitsSystemWindows(false)
        val root = findViewById<View>(R.id.settings_root)
        val baseLeft = root.paddingLeft
        val baseTop = root.paddingTop
        val baseRight = root.paddingRight
        val baseBottom = root.paddingBottom
        root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout(),
            )
            view.setPadding(
                baseLeft + bars.left,
                baseTop + bars.top,
                baseRight + bars.right,
                baseBottom + bars.bottom,
            )
            insets
        }
    }

    private fun applyPreview(preview: View, style: WidgetStyle) {
        preview.setBackgroundResource(style.backgroundRes)
    }

    private fun buttonFor(style: WidgetStyle): Int = when (style) {
        WidgetStyle.BLACK -> R.id.style_black
        WidgetStyle.TRANSLUCENT -> R.id.style_translucent
        WidgetStyle.TRANSPARENT -> R.id.style_transparent
    }

    private fun styleFor(buttonId: Int): WidgetStyle? = when (buttonId) {
        R.id.style_black -> WidgetStyle.BLACK
        R.id.style_translucent -> WidgetStyle.TRANSLUCENT
        R.id.style_transparent -> WidgetStyle.TRANSPARENT
        else -> null
    }
}
