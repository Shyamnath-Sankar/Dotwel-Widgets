package com.dotwelwidgets.dotwellabs

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.Dialog
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.dotwelwidgets.dotwellabs.media.MediaAccess
import com.dotwelwidgets.dotwellabs.media.MediaPrefs
import com.dotwelwidgets.dotwellabs.weather.WeatherCache
import com.dotwelwidgets.dotwellabs.weather.WeatherController
import com.dotwelwidgets.dotwellabs.weather.WeatherDisplay
import com.dotwelwidgets.dotwellabs.weather.WeatherSettings
import java.util.UUID

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val tag = "MainActivity"
    private val tiles = mutableListOf<TileHolder>()
    private val upcomingEntries = WidgetCatalog.entries.filterNot {
        it.state == ImportState.READY && it.providerClass != null
    }
    private var exactAlarmReceiverRegistered = false
    private var pendingEntryAfterAccess: CatalogEntry? = null
    private var pendingBluetoothEntry: CatalogEntry? = null
    private var upcomingBuilt = false
    private var sheetDialog: Dialog? = null
    private val initialRender = Runnable { refreshTilePreviews() }

    private val exactAlarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action ==
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
            ) {
                updateClockStateUi()
                refreshTilePreviews()
                WidgetUpdater.updateAll(this@MainActivity)
            }
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            refreshTilePreviews()
            val now = System.currentTimeMillis()
            handler.postDelayed(this, 60_000L - (now % 60_000L) + 100L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(
            DottedBackgroundDrawable(resources.displayMetrics.density),
        )
        setContentView(R.layout.activity_main)
        applySystemBarInsets()

        buildCategoryGrid()

        findViewById<Button>(R.id.refresh_widgets).setOnClickListener {
            WidgetUpdater.updateAll(this)
            refreshTilePreviews()
            Toast.makeText(this, R.string.widgets_refreshed, Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.exact_alarm_button).setOnClickListener {
            requestExactAlarmAccess()
        }
        findViewById<Button>(R.id.upcoming_toggle).setOnClickListener {
            toggleUpcoming()
        }
        findViewById<Button>(R.id.about_button).setOnClickListener {
            showAboutDialog()
        }
        findViewById<Button>(R.id.weather_button).setOnClickListener {
            showWeatherSettingsDialog()
        }
        findViewById<TextView>(R.id.nav_widgets).setOnClickListener {
            findViewById<ScrollView>(R.id.settings_root).smoothScrollTo(0, 0)
        }
        findViewById<TextView>(R.id.nav_style).setOnClickListener {
            showStyleDialog()
        }
        findViewById<TextView>(R.id.nav_about).setOnClickListener {
            showAboutDialog()
        }
        updateClockStateUi()
        updateWeatherUi()
        if (intent.getBooleanExtra(EXTRA_OPEN_WEATHER, false)) {
            findViewById<View>(R.id.settings_root).post {
                showWeatherSettingsDialog()
            }
        }
        if (intent.getBooleanExtra(EXTRA_SHOW_MEDIA_ACCESS, false)) {
            intent.removeExtra(EXTRA_SHOW_MEDIA_ACCESS)
            findViewById<View>(R.id.settings_root).post {
                showMediaAccessDialog()
            }
        }
        findViewById<View>(R.id.settings_root).post(initialRender)
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val filter = IntentFilter(
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(
                    exactAlarmReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED,
                )
            } else {
                registerReceiver(exactAlarmReceiver, filter)
            }
            exactAlarmReceiverRegistered = true
        }
    }

    override fun onStop() {
        if (exactAlarmReceiverRegistered) {
            unregisterReceiver(exactAlarmReceiver)
            exactAlarmReceiverRegistered = false
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(tick)
        updateClockStateUi()
        refreshTilePreviews()
        handler.post(tick)
        WidgetUpdater.updateAll(this)
        updateWeatherUi()
        maybeAutoRefreshWeather()
        val waiting = pendingEntryAfterAccess
        if (waiting != null && MinuteTickScheduler.hasExactAccess(this)) {
            pendingEntryAfterAccess = null
            requestPin(waiting)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_WEATHER, false)) {
            showWeatherSettingsDialog()
        }
        if (intent.getBooleanExtra(EXTRA_SHOW_MEDIA_ACCESS, false)) {
            intent.removeExtra(EXTRA_SHOW_MEDIA_ACCESS)
            showMediaAccessDialog()
        }
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        findViewById<View>(R.id.settings_root)?.removeCallbacks(initialRender)
        sheetDialog?.dismiss()
        sheetDialog = null
        tiles.clear()
        super.onDestroy()
    }

    private fun applySystemBarInsets() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        window.setDecorFitsSystemWindows(false)
        val root = findViewById<View>(R.id.settings_root)
        val nav = findViewById<View>(R.id.bottom_nav)
        val navParams = nav.layoutParams as ViewGroup.MarginLayoutParams
        val baseLeft = root.paddingLeft
        val baseTop = root.paddingTop
        val baseRight = root.paddingRight
        val baseBottom = root.paddingBottom
        val navLeft = navParams.leftMargin
        val navRight = navParams.rightMargin
        val navBottom = navParams.bottomMargin
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
            navParams.setMargins(
                navLeft + bars.left,
                navParams.topMargin,
                navRight + bars.right,
                navBottom + bars.bottom,
            )
            nav.layoutParams = navParams
            insets
        }
    }

    private fun buildCategoryGrid() {
        val container = findViewById<LinearLayout>(R.id.gallery_container)
        container.removeAllViews()
        tiles.clear()

        findViewById<TextView>(R.id.gallery_label).apply {
            text = getString(R.string.home_categories_label)
            contentDescription = getString(
                R.string.home_ready_summary,
                readyEntryCount(),
                WidgetCategory.entries.size,
            )
        }

        val columns = if (useTwoColumns()) 2 else 1
        WidgetCategory.entries.chunked(columns).forEach { rowCategories ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply { bottomMargin = dp(16) }
            }
            rowCategories.forEachIndexed { index, category ->
                val params = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f,
                )
                if (index > 0) params.marginStart = dp(12)
                row.addView(buildCategoryColumn(category), params)
            }
            if (rowCategories.size < columns) {
                row.addView(View(this), LinearLayout.LayoutParams(0, 0, 1f))
            }
            container.addView(row)
        }
        container.post { refreshTilePreviews() }
    }

    private fun buildCategoryColumn(category: WidgetCategory): LinearLayout {
        val ready = WidgetCatalog.readyEntries(category)
        val entry = ready.firstOrNull()
        val unavailable = ready.isEmpty()
        val label = getString(category.labelRes)
        val count = if (unavailable) {
            getString(R.string.category_unavailable_count)
        } else {
            getString(R.string.category_ready_count_format, ready.size)
        }

        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val tile = SquareFrameLayout(this).apply {
            setBackgroundResource(R.drawable.bg_category_tile)
            isClickable = true
            isFocusable = true
            contentDescription = if (unavailable) {
                getString(R.string.category_unavailable_format, label)
            } else {
                getString(R.string.category_open_format, label, ready.size)
            }
            setOnClickListener { openCategory(category) }
        }
        val host = FrameLayout(this).apply {
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        tile.addView(
            host,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
        column.addView(
            tile,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        column.addView(
            TextView(this).apply {
                text = label
                setTextColor(getColor(R.color.widget_ink))
                textSize = 13f
                setPadding(0, dp(8), 0, 0)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        column.addView(
            TextView(this).apply {
                text = count
                setTextColor(getColor(R.color.widget_muted))
                textSize = 11f
                setPadding(0, dp(2), 0, 0)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        tiles += TileHolder(category, host, entry)
        return column
    }

    private fun refreshTilePreviews() {
        if (isFinishing || isDestroyed) return
        val style = WidgetPrefs.style(this)
        tiles.forEach { tile ->
            val entry = tile.entry
            if (entry != null) {
                WidgetPreviewBinder.bind(this, tile.host, entry, style)
            } else {
                tile.host.removeAllViews()
                tile.host.addView(
                    CategoryMotifView(this).apply {
                        setLabel(getString(tile.category.labelRes))
                    },
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        }
    }

    private fun openCategory(category: WidgetCategory) {
        val entries = WidgetCatalog.readyEntries(category)
        if (entries.isEmpty()) {
            Toast.makeText(
                this,
                getString(
                    R.string.category_unavailable_format,
                    getString(category.labelRes),
                ),
                Toast.LENGTH_LONG,
            ).show()
            return
        }
        showCategorySheet(category, entries)
    }

    private fun showCategorySheet(category: WidgetCategory, entries: List<CatalogEntry>) {
        var selected = 0
        val dialog = Dialog(this)
        sheetDialog = dialog
        val style = WidgetPrefs.style(this)

        val root = FrameLayout(this).apply {
            setPadding(dp(14), dp(14), dp(14), dp(14))
            clipToPadding = false
        }
        val scroll = ScrollView(this).apply {
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        val sheet = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_sheet_surface)
            setPadding(dp(18), dp(16), dp(18), dp(18))
        }
        scroll.addView(
            sheet,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )
        root.addView(
            scroll,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM,
            ),
        )

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(
            TextView(this).apply {
                text = getString(category.labelRes)
                textSize = 19f
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(getColor(R.color.widget_ink))
            },
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ),
        )
        header.addView(
            ImageButton(this).apply {
                setImageResource(R.drawable.ic_close)
                contentDescription = getString(R.string.sheet_close_description)
                background = null
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setOnClickListener { dialog.dismiss() }
            },
            LinearLayout.LayoutParams(dp(48), dp(48)),
        )
        sheet.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val previewFrame = SwipeFrameLayout(this).apply {
            setBackgroundResource(style.backgroundRes)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        sheet.addView(
            previewFrame,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(240),
            ).apply { topMargin = dp(14) },
        )

        val nameView = TextView(this).apply {
            textSize = 18f
            setTypeface(Typeface.DEFAULT_BOLD)
            setTextColor(getColor(R.color.widget_ink))
            setPadding(0, dp(14), 0, 0)
        }
        sheet.addView(nameView, matchWrap())

        val sizeView = TextView(this).apply {
            textSize = 12f
            setTextColor(getColor(R.color.widget_muted))
            setPadding(0, dp(4), 0, 0)
        }
        sheet.addView(sizeView, matchWrap())

        val modeView = TextView(this).apply {
            textSize = 13f
            setTextColor(getColor(R.color.dot_accent))
            setPadding(0, dp(10), 0, 0)
        }
        sheet.addView(modeView, matchWrap())

        val descView = TextView(this).apply {
            textSize = 12f
            setTextColor(getColor(R.color.widget_muted))
            setLineSpacing(dp(2).toFloat(), 1f)
            setPadding(0, dp(4), 0, 0)
        }
        sheet.addView(descView, matchWrap())

        val add = Button(this).apply {
            background = getDrawable(R.drawable.bg_add_pill)
            backgroundTintList = null
            stateListAnimator = null
            isAllCaps = false
            gravity = Gravity.CENTER
            minHeight = dp(52)
            maxLines = 1
            setPadding(0, 0, 0, 0)
            setTextColor(Color.WHITE)
            textSize = 15f
        }
        sheet.addView(
            add,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52),
            ).apply { topMargin = dp(16) },
        )

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val prev = sheetStepView(
            getString(R.string.sheet_previous),
            getString(R.string.sheet_previous_description),
        )
        val next = sheetStepView(
            getString(R.string.sheet_next),
            getString(R.string.sheet_next_description),
        )
        val dots = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        controls.addView(
            prev,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(48),
            ),
        )
        controls.addView(
            dots,
            LinearLayout.LayoutParams(0, dp(48), 1f),
        )
        controls.addView(
            next,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(48),
            ),
        )
        sheet.addView(
            controls,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(8) },
        )

        fun rebind() {
            val entry = entries[selected]
            nameView.text = entry.name
            sizeView.text = providerSizeText(entry)
            modeView.text = WidgetModeLabels.label(this, entry)
            val weatherNote = if (entry.group == WidgetGroup.WEATHER) {
                "\n\n" + weatherStatusSummary()
            } else {
                ""
            }
            descView.text = getString(entry.descriptionRes) + weatherNote +
                "\n\n" + getString(R.string.sheet_hint)
            add.text = getString(R.string.sheet_add_widget)
            add.contentDescription = getString(
                R.string.sheet_add_widget_format,
                entry.name,
            )
            add.isEnabled = entry.state == ImportState.READY && entry.providerClass != null
            add.setOnClickListener {
                dialog.dismiss()
                onAdd(entries[selected])
            }
            dots.removeAllViews()
            entries.indices.forEach { index ->
                dots.addView(
                    View(this).apply {
                        setBackgroundResource(
                            if (index == selected) {
                                R.drawable.bg_dot_selected
                            } else {
                                R.drawable.bg_dot_unselected
                            },
                        )
                    },
                    LinearLayout.LayoutParams(dp(8), dp(8)).apply {
                        marginStart = dp(3)
                        marginEnd = dp(3)
                    },
                )
            }
            dots.contentDescription = getString(
                R.string.sheet_position_format,
                selected + 1,
                entries.size,
            )
            val canCycle = entries.size > 1
            prev.isEnabled = canCycle
            next.isEnabled = canCycle
            prev.alpha = if (canCycle) 1f else 0.4f
            next.alpha = if (canCycle) 1f else 0.4f
            previewFrame.contentDescription = getString(
                R.string.sheet_preview_description_format,
                entry.name,
            )
            previewFrame.setOnClickListener {
                if (entry.group == WidgetGroup.WEATHER) {
                    showWeatherSettingsDialog()
                }
            }
            previewFrame.post {
                WidgetPreviewBinder.bind(this, previewFrame, entry, WidgetPrefs.style(this))
            }
        }

        fun step(delta: Int) {
            if (entries.size <= 1) return
            selected = ((selected + delta) % entries.size + entries.size) % entries.size
            rebind()
        }

        prev.setOnClickListener { step(-1) }
        next.setOnClickListener { step(1) }
        previewFrame.onSwipe = { direction -> step(direction) }

        dialog.setContentView(root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            dialog.window?.setDecorFitsSystemWindows(false)
        }
        dialog.setOnShowListener {
            val window = dialog.window
            if (window != null) {
                window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val baseLeft = root.paddingLeft
                    val baseTop = root.paddingTop
                    val baseRight = root.paddingRight
                    val baseBottom = root.paddingBottom
                    root.setOnApplyWindowInsetsListener { view, insets ->
                        val bars = insets.getInsets(
                            WindowInsets.Type.systemBars() or
                                WindowInsets.Type.displayCutout(),
                        )
                        view.setPadding(
                            baseLeft + bars.left,
                            baseTop + bars.top,
                            baseRight + bars.right,
                            baseBottom + bars.bottom,
                        )
                        insets
                    }
                    root.requestApplyInsets()
                }
            }
        }
        dialog.setOnDismissListener {
            if (sheetDialog === dialog) sheetDialog = null
        }
        dialog.show()
        rebind()
    }

    private fun sheetStepView(textValue: String, description: String): TextView =
        TextView(this).apply {
            text = textValue
            contentDescription = description
            gravity = Gravity.CENTER
            minWidth = dp(64)
            setTextColor(getColor(R.color.widget_ink))
            textSize = 13f
            isClickable = true
            isFocusable = true
            setPadding(dp(10), 0, dp(10), 0)
        }

    private fun matchWrap(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )

    private fun providerSizeText(entry: CatalogEntry): String {
        val info = WidgetCatalog.providerInfo(this, entry)
            ?: return getString(R.string.sheet_size_unknown)
        val minWidth = info.minWidth
        val minHeight = info.minHeight
        if (minWidth <= 0 || minHeight <= 0) {
            return getString(R.string.sheet_size_unknown)
        }
        return getString(R.string.sheet_size_format, minWidth, minHeight)
    }

    private fun readyEntryCount(): Int = WidgetCatalog.entries.count {
        it.state == ImportState.READY && it.providerClass != null
    }

    private fun useTwoColumns(): Boolean {
        val config = resources.configuration
        return config.fontScale < 1.3f && config.screenWidthDp >= 360
    }

    private fun toggleUpcoming() {
        val container = findViewById<LinearLayout>(R.id.upcoming_container)
        val toggle = findViewById<Button>(R.id.upcoming_toggle)
        if (container.visibility == View.VISIBLE) {
            container.visibility = View.GONE
            toggle.text = getString(R.string.upcoming_toggle_format, upcomingEntries.size)
            return
        }
        if (!upcomingBuilt) {
            buildUpcomingRows(container)
            upcomingBuilt = true
        }
        container.visibility = View.VISIBLE
        toggle.text = getString(R.string.upcoming_hide)
    }

    private fun buildUpcomingRows(container: LinearLayout) {
        val inflater = LayoutInflater.from(this)
        upcomingEntries.forEach { entry ->
            val row = inflater.inflate(R.layout.item_upcoming_row, container, false)
            row.findViewById<TextView>(R.id.upcoming_name).text = entry.name
            row.findViewById<TextView>(R.id.upcoming_reason).text = getString(
                R.string.upcoming_row_reason_format,
                getString(entry.group.labelRes),
                getString(entry.detailRes),
            )
            val status = row.findViewById<TextView>(R.id.upcoming_status)
            status.text = getString(entry.state.labelRes)
            status.backgroundTintList = ColorStateList.valueOf(stateColor(entry.state))
            container.addView(row)
        }
    }

    private fun onAdd(entry: CatalogEntry) {
        if (entry.state != ImportState.READY || entry.providerClass == null) return
        when (entry.action) {
            CardAction.CLOCK_ACCESS -> {
                if (MinuteTickScheduler.hasExactAccess(this)) {
                    requestPin(entry)
                } else {
                    showMinuteAccessDialog(entry)
                }
            }
            CardAction.WORLD_ZONES -> showWorldDialog(entry)
            CardAction.WEATHER -> {
                if (WeatherSettings.isEnabled(this) &&
                    WeatherSettings.location(this) != null
                ) {
                    requestPin(entry)
                } else {
                    showWeatherSettingsDialog { requestPin(entry) }
                }
            }
            CardAction.MEDIA_ACCESS -> {
                if (MediaAccess.isGranted(this)) {
                    requestPin(entry)
                } else {
                    showMediaAccessDialog()
                }
            }
            CardAction.BLUETOOTH_ACCESS -> {
                if (BluetoothAccess.isGranted(this)) {
                    requestPin(entry)
                } else {
                    showBluetoothAccessDialog(entry)
                }
            }
            CardAction.NONE -> requestPin(entry)
        }
    }


    private fun showMediaAccessDialog() {
        val granted = MediaAccess.isGranted(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (resources.displayMetrics.density * 20).toInt()
            setPadding(pad, pad, pad, pad)
        }
        content.addView(
            TextView(this).apply {
                text = getString(
                    R.string.media_access_settings_status,
                    getString(
                        if (granted) R.string.media_access_granted
                        else R.string.media_access_denied,
                    ),
                )
                setTextColor(Color.WHITE)
                textSize = 14f
            },
        )
        content.addView(
            TextView(this).apply {
                text = getString(R.string.media_access_explain)
                setTextColor(Color.LTGRAY)
                textSize = 13f
                setPadding(0, (resources.displayMetrics.density * 12).toInt(), 0, 0)
            },
        )
        val toggle = android.widget.CheckBox(this).apply {
            text = getString(R.string.media_text_toggle)
            isChecked = MediaPrefs.showNotificationText(this@MainActivity)
            setTextColor(Color.WHITE)
            setPadding(0, (resources.displayMetrics.density * 16).toInt(), 0, 0)
            setOnCheckedChangeListener { _, checked ->
                MediaPrefs.setShowNotificationText(this@MainActivity, checked)
                WidgetUpdater.updateMediaProviders(this@MainActivity)
            }
        }
        content.addView(toggle)
        content.addView(
            TextView(this).apply {
                text = getString(R.string.media_text_note)
                setTextColor(Color.GRAY)
                textSize = 12f
                setPadding(0, (resources.displayMetrics.density * 6).toInt(), 0, 0)
            },
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.media_access_title)
            .setView(content)
            .setPositiveButton(R.string.media_access_open) { _, _ ->
                val opened = runCatching {
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }.isSuccess
                if (!opened) {
                    Toast.makeText(
                        this,
                        R.string.media_access_open,
                        Toast.LENGTH_LONG,
                    ).show()
                }
            }
            .setNeutralButton(R.string.media_refresh) { _, _ ->
                WidgetUpdater.updateMediaProviders(this)
            }
            .setNegativeButton(R.string.media_close, null)
            .show()
    }

    private fun showBluetoothAccessDialog(entry: CatalogEntry) {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (resources.displayMetrics.density * 20).toInt()
            setPadding(pad, pad, pad, pad)
        }
        content.addView(
            TextView(this).apply {
                text = getString(
                    R.string.bt_access_status,
                    getString(R.string.bt_access_denied),
                )
                setTextColor(Color.WHITE)
                textSize = 14f
            },
        )
        content.addView(
            TextView(this).apply {
                text = getString(R.string.bt_access_explain)
                setTextColor(Color.LTGRAY)
                textSize = 13f
                setPadding(0, dp(12), 0, 0)
            },
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.bt_access_title)
            .setView(content)
            .setPositiveButton(R.string.bt_access_allow) { _, _ ->
                pendingBluetoothEntry = entry
                requestPermissions(
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    BLUETOOTH_PERMISSION_REQUEST,
                )
            }
            .setNegativeButton(R.string.bt_access_add_without) { _, _ ->
                requestPin(entry)
            }
            .setNeutralButton(R.string.bt_access_close, null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != BLUETOOTH_PERMISSION_REQUEST) return
        val entry = pendingBluetoothEntry ?: return
        pendingBluetoothEntry = null
        val granted = grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            Toast.makeText(this, R.string.bt_access_denied, Toast.LENGTH_LONG).show()
        }
        refreshTilePreviews()
        requestPin(entry)
    }

    private fun showMinuteAccessDialog(entry: CatalogEntry) {
        val clock = entry.id == "digital"
        val splitClock = entry.previewKind == PreviewKind.KWGT_DIGITAL_4
        val digital3 = entry.previewKind == PreviewKind.KWGT_DIGITAL_3
        val gradientClock = entry.previewKind == PreviewKind.KWGT_GRADIENT_CLOCK
        val dateTime = entry.previewKind == PreviewKind.KWGT_DATE_TIME ||
            entry.previewKind == PreviewKind.KWGT_DATE_TIME_2
        val title = when {
            clock -> R.string.clock_access_dialog_title
            splitClock -> R.string.kwgt_digital4_access_dialog_title
            digital3 -> R.string.kwgt_digital3_access_dialog_title
            gradientClock -> R.string.kwgt_gradient_access_dialog_title
            dateTime -> R.string.kwgt_datetime_access_dialog_title
            else -> R.string.minute_access_dialog_title
        }
        val message = when {
            clock -> R.string.clock_access_dialog_message
            splitClock -> R.string.kwgt_digital4_access_dialog_message
            digital3 -> R.string.kwgt_digital3_access_dialog_message
            gradientClock -> R.string.kwgt_gradient_access_dialog_message
            dateTime -> R.string.kwgt_datetime_access_dialog_message
            else -> R.string.minute_access_dialog_message
        }
        val enable = when {
            clock -> R.string.clock_access_dialog_enable
            splitClock -> R.string.kwgt_digital4_access_dialog_enable
            digital3 -> R.string.kwgt_digital3_access_dialog_enable
            gradientClock -> R.string.kwgt_gradient_access_dialog_enable
            dateTime -> R.string.kwgt_datetime_access_dialog_enable
            else -> R.string.minute_access_dialog_enable
        }
        val plain = when {
            clock -> R.string.clock_access_dialog_plain
            splitClock -> R.string.kwgt_digital4_access_dialog_plain
            digital3 -> R.string.kwgt_digital3_access_dialog_plain
            gradientClock -> R.string.kwgt_gradient_access_dialog_plain
            dateTime -> R.string.kwgt_datetime_access_dialog_plain
            else -> R.string.minute_access_dialog_plain
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(plain) { _, _ -> requestPin(entry) }
            .setNeutralButton(enable) { _, _ ->
                pendingEntryAfterAccess = entry
                requestExactAlarmAccess()
            }
            .setNegativeButton(R.string.clock_access_dialog_cancel, null)
            .show()
    }

    private fun showWorldDialog(entry: CatalogEntry) {
        val zones = (listOf(WorldPrefs.localChoice()) + WorldPrefs.choices)
            .distinctBy { it.zoneId }
        val labels = zones.map { "${it.label} · ${it.zoneId}" }
        val current = WorldPrefs.previewZones(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        content.addView(TextView(this).apply {
            setText(R.string.world_zone_first)
            setTextColor(getColor(R.color.widget_muted))
        })
        val first = Spinner(this)
        first.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        content.addView(first)
        content.addView(TextView(this).apply {
            setText(R.string.world_zone_second)
            setTextColor(getColor(R.color.widget_muted))
            setPadding(0, dp(12), 0, 0)
        })
        val second = Spinner(this)
        second.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, labels)
        content.addView(second)
        first.setSelection(zones.indexOfFirst { it.zoneId == current.getOrNull(0)?.zoneId }.coerceAtLeast(0))
        second.setSelection(zones.indexOfFirst { it.zoneId == current.getOrNull(1)?.zoneId }.coerceAtLeast(0))

        AlertDialog.Builder(this)
            .setTitle(R.string.world_add_title)
            .setView(content)
            .setPositiveButton(R.string.world_save_and_add) { _, _ ->
                val chosen = listOf(
                    zones[first.selectedItemPosition],
                    zones[second.selectedItemPosition],
                )
                val token = UUID.randomUUID().toString()
                WorldPrefs.setPending(this, token, chosen)
                refreshTilePreviews()
                requestPin(entry, token)
            }
            .setNegativeButton(R.string.world_cancel, null)
            .show()
    }

    private fun requestPin(entry: CatalogEntry, pendingToken: String? = null) {
        val provider = entry.providerClass ?: return
        val kind = entry.pinKind ?: return
        fun abandonPending() {
            if (pendingToken != null) WorldPrefs.clearPending(this, pendingToken)
        }
        if (isDebugForcePinUnsupported()) {
            abandonPending()
            showPinFallbackDialog()
            return
        }
        val manager = AppWidgetManager.getInstance(this)
        if (!manager.isRequestPinAppWidgetSupported) {
            abandonPending()
            showPinFallbackDialog()
            return
        }
        val requestCode = (kind + (pendingToken ?: "")).hashCode() and 0x7FFF
        val callback = PendingIntent.getBroadcast(
            this,
            requestCode,
            Intent(this, PinResultReceiver::class.java).apply {
                action = PinResultReceiver.ACTION_PIN_RESULT
                putExtra(PinResultReceiver.EXTRA_PIN_KIND, kind)
                if (pendingToken != null) {
                    putExtra(PinResultReceiver.EXTRA_PIN_TOKEN, pendingToken)
                }
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val requested = try {
            manager.requestPinAppWidget(ComponentName(this, provider), null, callback)
        } catch (error: SecurityException) {
            Log.w(tag, "Pin request was rejected", error)
            false
        } catch (error: IllegalStateException) {
            Log.w(tag, "Pin request needs a foreground activity", error)
            false
        }
        if (requested) {
            Toast.makeText(this, R.string.pin_request_sent, Toast.LENGTH_LONG).show()
        } else {
            abandonPending()
            showPinFallbackDialog()
        }
    }

    private fun isDebugForcePinUnsupported(): Boolean {
        val debuggable =
            (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return debuggable &&
            intent.getBooleanExtra(EXTRA_DEBUG_FORCE_PIN_UNSUPPORTED, false)
    }

    private fun showPinFallbackDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.pin_unavailable_title)
            .setMessage(R.string.pin_unavailable_message)
            .setPositiveButton(R.string.pin_unavailable_close, null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showWeatherSettingsDialog(afterConfigured: (() -> Unit)? = null) {
        WeatherSettingsDialog(this, afterConfigured).show()
    }

    private fun weatherStatusSummary(): String {
        if (!WeatherSettings.isEnabled(this)) {
            return getString(R.string.weather_preview_hint)
        }
        val location = WeatherSettings.location(this)
            ?: return getString(R.string.weather_status_no_city)
        val snapshot = WeatherCache.load(this)
            ?: return getString(R.string.weather_status_no_cache, location.name)
        val stale = WeatherDisplay.isStale(snapshot, System.currentTimeMillis())
        val failed = WeatherSettings.lastFailure(this) != null
        return getString(
            R.string.weather_status_updated,
            WeatherDisplay.locationLine(location),
            WeatherDisplay.fetchedTime(snapshot),
            buildString {
                if (stale) append(getString(R.string.weather_status_stale_suffix))
                if (failed) {
                    append(getString(R.string.weather_status_refresh_failed_suffix))
                }
            },
        )
    }

    private fun updateWeatherUi() {
        val button = findViewById<Button>(R.id.weather_button) ?: return
        button.text = getString(
            if (WeatherSettings.isEnabled(this)) {
                R.string.weather_header_on
            } else {
                R.string.weather_header_off
            },
        )
    }

    private fun maybeAutoRefreshWeather() {
        if (!WeatherSettings.isEnabled(this)) return
        if (WeatherSettings.location(this) == null) return
        val now = System.currentTimeMillis()
        val cached = WeatherCache.load(this)
        val stale = cached == null || WeatherDisplay.isStale(cached, now)
        if (!stale) return
        val lastAttempt = WeatherSettings.lastAutoAttempt(this)
        if (lastAttempt > 0L && now - lastAttempt < 60L * 60L * 1000L) return
        WeatherSettings.markAutoAttempt(this, now)
        WeatherController.fetch(this, auto = true) {
            if (isFinishing || isDestroyed) return@fetch
            WidgetUpdater.updateWeatherProviders(this)
            refreshTilePreviews()
            updateWeatherUi()
        }
    }

    private fun showStyleDialog() {
        val styles = WidgetStyle.entries
        val labels = arrayOf(
            getString(R.string.style_black),
            getString(R.string.style_translucent),
            getString(R.string.style_transparent),
        )
        val checked = styles.indexOf(WidgetPrefs.style(this)).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(R.string.style_label)
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                val style = styles.getOrNull(which) ?: return@setSingleChoiceItems
                WidgetPrefs.setStyle(this, style)
                refreshTilePreviews()
                WidgetUpdater.updateAll(this)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.about_close, null)
            .show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.about_title)
            .setMessage(R.string.about_body)
            .setPositiveButton(R.string.about_close, null)
            .setNeutralButton(R.string.about_licenses) { _, _ -> showLicenseDialog() }
            .show()
    }

    private fun showLicenseDialog() {
        val awesome = readRawText(R.raw.awesomeawidgets_mit)
        val kwgt = readRawText(R.raw.kwgt_widgets_mit)
        val body = "AwesomeAWidgets\n================\n\n$awesome\n\n" +
            "KWGT-Widgets\n================\n\n$kwgt"
        AlertDialog.Builder(this)
            .setTitle(R.string.about_license_title)
            .setMessage(body)
            .setPositiveButton(R.string.about_close, null)
            .show()
    }

    private fun readRawText(resourceId: Int): String =
        resources.openRawResource(resourceId).bufferedReader().use { it.readText() }

    private fun updateClockStateUi() {
        val status = findViewById<TextView>(R.id.clock_setup_status)
        val button = findViewById<Button>(R.id.exact_alarm_button)
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                status.setText(R.string.clock_setup_always)
                button.visibility = View.GONE
            }
            MinuteTickScheduler.hasExactAccess(this) -> {
                status.setText(R.string.clock_setup_enabled)
                button.visibility = View.GONE
            }
            else -> {
                status.setText(R.string.clock_setup_disabled)
                button.visibility = View.VISIBLE
            }
        }
    }

    private fun stateColor(state: ImportState): Int = when (state) {
        ImportState.READY -> 0xFF1E3A2A.toInt()
        ImportState.PENDING -> 0xFF3A3320.toInt()
        ImportState.BLOCKED -> 0xFF3A2020.toInt()
        ImportState.UNAVAILABLE -> 0xFF2A2A2E.toInt()
    }

    private fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val packageUri = Uri.parse("package:$packageName")
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri))
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(
                    this,
                    R.string.exact_alarm_unavailable,
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private class TileHolder(
        val category: WidgetCategory,
        val host: FrameLayout,
        val entry: CatalogEntry?,
    )

    companion object {
        const val EXTRA_DEBUG_FORCE_PIN_UNSUPPORTED =
            "com.dotwelwidgets.dotwellabs.extra.DEBUG_FORCE_PIN_UNSUPPORTED"
        const val ACTION_OPEN_WEATHER =
            "com.dotwelwidgets.dotwellabs.action.OPEN_WEATHER"
        const val EXTRA_OPEN_WEATHER =
            "com.dotwelwidgets.dotwellabs.extra.OPEN_WEATHER"
        const val EXTRA_SHOW_MEDIA_ACCESS =
            "com.dotwelwidgets.dotwellabs.extra.SHOW_MEDIA_ACCESS"
        const val BLUETOOTH_PERMISSION_REQUEST = 4101
    }
}
