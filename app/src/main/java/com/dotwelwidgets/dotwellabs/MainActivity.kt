package com.dotwelwidgets.dotwellabs

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import java.util.Calendar

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var clockBitmap: Bitmap? = null
    private var dateBitmap: Bitmap? = null

    private val tick = object : Runnable {
        override fun run() {
            renderPreviews()
            val now = System.currentTimeMillis()
            handler.postDelayed(this, 60_000L - (now % 60_000L) + 100L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applySystemBarInsets()

        val group = findViewById<RadioGroup>(R.id.style_group)
        val current = WidgetPrefs.style(this)

        group.check(buttonFor(current))
        applyPreview(current)

        group.setOnCheckedChangeListener { _, checkedId ->
            val style = styleFor(checkedId) ?: return@setOnCheckedChangeListener
            WidgetPrefs.setStyle(this, style)
            applyPreview(style)
            renderPreviews()
            WidgetUpdater.updateAll(this)
        }

        findViewById<Button>(R.id.refresh_widgets).setOnClickListener {
            WidgetUpdater.updateAll(this)
            Toast.makeText(this, R.string.widgets_refreshed, Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.preview_card_clock).post { renderPreviews() }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        clockBitmap?.recycle()
        dateBitmap?.recycle()
        clockBitmap = null
        dateBitmap = null
        super.onDestroy()
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

    private fun applyPreview(style: WidgetStyle) {
        findViewById<View>(R.id.preview_card_clock)
            .setBackgroundResource(style.backgroundRes)
        findViewById<View>(R.id.preview_card_date)
            .setBackgroundResource(style.backgroundRes)
    }

    private fun renderPreviews() {
        renderPreview(R.id.preview_clock, clock = true)
        renderPreview(R.id.preview_date, clock = false)
    }

    private fun renderPreview(viewId: Int, clock: Boolean) {
        val view = findViewById<ImageView>(viewId)
        if (view.width <= 0 || view.height <= 0) return
        val style = WidgetPrefs.style(this)
        val now = Calendar.getInstance()
        val bitmap = if (clock) {
            DotMatrixRenderer.render(
                this,
                view.width,
                view.height,
                style,
                now,
                includeBackground = false,
            )
        } else {
            SerifDateRenderer.render(
                this,
                view.width,
                view.height,
                style,
                now,
                includeBackground = false,
            )
        }
        val previous = if (clock) clockBitmap else dateBitmap
        view.setImageBitmap(bitmap)
        if (clock) clockBitmap = bitmap else dateBitmap = bitmap
        previous?.recycle()
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
