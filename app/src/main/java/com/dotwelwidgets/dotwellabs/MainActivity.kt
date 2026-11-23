package com.dotwelwidgets.dotwellabs

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar
import java.util.UUID

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val tag = "MainActivity"
    private val cards = mutableListOf<CardHolder>()
    private val previewBitmaps = mutableMapOf<ImageView, Bitmap>()
    private val availableEntries = WidgetCatalog.entries.filter {
        it.state == ImportState.READY && it.providerClass != null
    }
    private val upcomingEntries = WidgetCatalog.entries.filterNot {
        it.state == ImportState.READY && it.providerClass != null
    }
    private var exactAlarmReceiverRegistered = false
    private var pendingEntryAfterAccess: CatalogEntry? = null
    private var upcomingBuilt = false
    private val initialRender = Runnable { renderVisiblePreviews() }

    private val exactAlarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action ==
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
            ) {
                updateClockStateUi()
                renderVisiblePreviews()
                WidgetUpdater.updateAll(this@MainActivity)
            }
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            renderVisiblePreviews()
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

        buildGallery()
        applyStyleToFrames(current)
        group.setOnCheckedChangeListener { _, checkedId ->
            val style = styleFor(checkedId) ?: return@setOnCheckedChangeListener
            WidgetPrefs.setStyle(this, style)
            applyStyleToFrames(style)
            renderVisiblePreviews()
            WidgetUpdater.updateAll(this)
        }

        findViewById<Button>(R.id.refresh_widgets).setOnClickListener {
            WidgetUpdater.updateAll(this)
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
        findViewById<ScrollView>(R.id.settings_root).setOnScrollChangeListener { _, _, _, _, _ ->
            renderVisiblePreviews()
        }
        updateClockStateUi()
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
        renderVisiblePreviews()
        handler.post(tick)
        WidgetUpdater.updateAll(this)
        val waiting = pendingEntryAfterAccess
        if (waiting != null && MinuteTickScheduler.hasExactAccess(this)) {
            pendingEntryAfterAccess = null
            requestPin(waiting)
        }
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        findViewById<View>(R.id.settings_root)?.removeCallbacks(initialRender)
        previewBitmaps.clear()
        cards.clear()
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

    private fun buildGallery() {
        val container = findViewById<LinearLayout>(R.id.gallery_container)
        val inflater = LayoutInflater.from(this)
        availableEntries.forEach { entry ->
            val root = inflater.inflate(R.layout.item_widget_card, container, false)
            container.addView(root)
            cards += CardHolder(entry, root)
        }
        cards.forEach { bindCard(it) }
        findViewById<TextView>(R.id.gallery_label).text = getString(
            R.string.gallery_available_count,
            availableEntries.size,
            WidgetCatalog.entries.size,
        )
        val toggle = findViewById<Button>(R.id.upcoming_toggle)
        toggle.visibility = if (upcomingEntries.isEmpty()) View.GONE else View.VISIBLE
        toggle.text = getString(R.string.upcoming_toggle_format, upcomingEntries.size)
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

    private fun bindCard(holder: CardHolder) {
        val entry = holder.entry
        val root = holder.root
        root.findViewById<TextView>(R.id.card_title).text = entry.name
        val chip = root.findViewById<TextView>(R.id.card_status_chip)
        chip.text = getString(entry.state.labelRes)
        chip.backgroundTintList = ColorStateList.valueOf(stateColor(entry.state))

        val add = root.findViewById<Button>(R.id.card_add)
        val ready = entry.state == ImportState.READY && entry.providerClass != null
        add.isEnabled = ready
        add.text = if (ready) {
            getString(R.string.gallery_add_format, entry.name)
        } else {
            getString(R.string.gallery_not_ready)
        }
        add.setOnClickListener { onAdd(entry) }

        val secondary = root.findViewById<Button>(R.id.card_secondary)
        when (entry.action) {
            CardAction.CLOCK_ACCESS -> {
                secondary.setOnClickListener { requestExactAlarmAccess() }
            }
            CardAction.WORLD_ZONES -> {
                secondary.setOnClickListener { showWorldDialog(entry) }
            }
            CardAction.NONE -> secondary.visibility = View.GONE
        }
        if (entry.action == CardAction.WORLD_ZONES) {
            secondary.visibility = View.VISIBLE
            secondary.setText(R.string.gallery_configure_world)
        }
        updateCardState(holder)
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
            CardAction.NONE -> requestPin(entry)
        }
    }

    private fun showMinuteAccessDialog(entry: CatalogEntry) {
        val clock = entry.id == "digital"
        val title = if (clock) {
            R.string.clock_access_dialog_title
        } else {
            R.string.minute_access_dialog_title
        }
        val message = if (clock) {
            R.string.clock_access_dialog_message
        } else {
            R.string.minute_access_dialog_message
        }
        val enable = if (clock) {
            R.string.clock_access_dialog_enable
        } else {
            R.string.minute_access_dialog_enable
        }
        val plain = if (clock) {
            R.string.clock_access_dialog_plain
        } else {
            R.string.minute_access_dialog_plain
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
                cards.filter { it.entry.action == CardAction.WORLD_ZONES }
                    .forEach { it.lastRenderKey = null }
                renderVisiblePreviews()
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
        cards.forEach { updateCardState(it) }
    }

    private fun updateCardState(holder: CardHolder) {
        val entry = holder.entry
        val secondary = holder.root.findViewById<Button>(R.id.card_secondary)
        val detail = holder.root.findViewById<TextView>(R.id.card_status_detail)
        val badge = holder.root.findViewById<TextView>(R.id.card_preview_badge)
        if (holder.lastRenderFailed) {
            detail.setText(R.string.preview_render_failed_detail)
            badge.setText(
                if (WidgetPreviewRenderer.usesMinuteBitmap(entry.previewKind)) {
                    R.string.preview_badge_live
                } else {
                    R.string.preview_badge_unavailable
                },
            )
        }
        when (entry.action) {
            CardAction.CLOCK_ACCESS -> {
                val access = MinuteTickScheduler.hasExactAccess(this)
                secondary.visibility = if (access) View.GONE else View.VISIBLE
                secondary.setText(R.string.exact_alarm_enable)
                val clock = entry.id == "digital"
                if (!holder.lastRenderFailed) {
                    detail.setText(
                        when {
                            access && clock -> R.string.clock_dotted_detail
                            access -> R.string.analog_dotted_detail
                            clock -> R.string.clock_plain_detail
                            else -> R.string.analog_fallback_detail
                        },
                    )
                    badge.setText(
                        when {
                            access && clock -> R.string.preview_badge_dotted
                            access -> R.string.preview_badge_styled
                            else -> R.string.preview_badge_live
                        },
                    )
                }
            }
            CardAction.WORLD_ZONES -> {
                secondary.visibility = View.VISIBLE
                if (!holder.lastRenderFailed) {
                    detail.setText(entry.detailRes)
                    badge.setText(R.string.preview_badge)
                }
            }
            CardAction.NONE -> {
                secondary.visibility = View.GONE
                if (!holder.lastRenderFailed) {
                    detail.setText(entry.detailRes)
                    badge.setText(R.string.preview_badge)
                }
            }
        }
    }

    private fun applyStyleToFrames(style: WidgetStyle) {
        cards.forEach { holder ->
            holder.root.findViewById<View>(R.id.card_preview_frame)
                .setBackgroundResource(style.backgroundRes)
        }
    }

    private fun renderVisiblePreviews() {
        if (isFinishing || isDestroyed) return
        val style = WidgetPrefs.style(this)
        cards.forEach { holder ->
            if (isCardNearVisible(holder)) renderCard(holder, style)
        }
    }

    private fun isCardNearVisible(holder: CardHolder): Boolean {
        val scroll = findViewById<ScrollView>(R.id.settings_root)
        if (scroll.height <= 0) return true
        if (!holder.root.isShown) return false
        val scrollLocation = IntArray(2)
        val cardLocation = IntArray(2)
        scroll.getLocationInWindow(scrollLocation)
        holder.root.getLocationInWindow(cardLocation)
        val buffer = dp(180)
        val cardTop = cardLocation[1]
        val cardHeight = holder.root.height.takeIf { it > 0 } ?: dp(240)
        val cardBottom = cardTop + cardHeight
        return cardBottom >= scrollLocation[1] - buffer &&
            cardTop <= scrollLocation[1] + scroll.height + buffer
    }

    private fun renderCard(holder: CardHolder, style: WidgetStyle) {
        if (isFinishing || isDestroyed) return
        val frame = holder.root.findViewById<View>(R.id.card_preview_frame)
        if (frame.width <= 0 || frame.height <= 0) {
            attachLayoutListener(holder)
            return
        }
        val padding = dp(10)
        val width = (frame.width - padding * 2).coerceAtLeast(1)
        val height = (frame.height - padding * 2).coerceAtLeast(1)
        val access = MinuteTickScheduler.hasExactAccess(this)
        val key = previewKey(holder, style, width, height, access)
        if (holder.lastRenderKey == key) return

        val entry = holder.entry
        when (entry.previewKind) {
            PreviewKind.CLOCK -> {
                if (!access) {
                    holder.lastRenderFailed = false
                    showClockFallback(holder, style, failed = false)
                } else if (!renderBitmap(holder, style, width, height)) {
                    holder.lastRenderFailed = true
                    showClockFallback(holder, style, failed = true)
                } else {
                    holder.lastRenderFailed = false
                    holder.root.findViewById<TextView>(R.id.card_status_detail)
                        .setText(R.string.clock_dotted_detail)
                    holder.root.findViewById<TextView>(R.id.card_preview_badge)
                        .setText(R.string.preview_badge_dotted)
                }
            }
            PreviewKind.ANALOG_BOLD,
            PreviewKind.ANALOG_SCALE,
            -> {
                if (!access) {
                    holder.lastRenderFailed = false
                    showAnalogFallback(holder, failed = false)
                } else if (!renderBitmap(holder, style, width, height)) {
                    holder.lastRenderFailed = true
                    showAnalogFallback(holder, failed = true)
                } else {
                    holder.lastRenderFailed = false
                    holder.root.findViewById<TextView>(R.id.card_status_detail)
                        .setText(R.string.analog_dotted_detail)
                    holder.root.findViewById<TextView>(R.id.card_preview_badge)
                        .setText(R.string.preview_badge_styled)
                }
            }
            else -> {
                if (renderBitmap(holder, style, width, height)) {
                    holder.lastRenderFailed = false
                    holder.root.findViewById<TextView>(R.id.card_status_detail)
                        .setText(entry.detailRes)
                    holder.root.findViewById<TextView>(R.id.card_preview_badge)
                        .setText(R.string.preview_badge)
                } else {
                    holder.lastRenderFailed = true
                    val image = holder.root.findViewById<ImageView>(R.id.card_preview)
                    image.setImageDrawable(null)
                    image.visibility = View.GONE
                    holder.root.findViewById<TextView>(R.id.card_status_detail)
                        .setText(R.string.preview_render_failed_detail)
                    holder.root.findViewById<TextView>(R.id.card_preview_badge)
                        .setText(R.string.preview_badge_unavailable)
                }
            }
        }
        holder.lastRenderKey = key
    }

    private fun attachLayoutListener(holder: CardHolder) {
        if (holder.layoutListenerAttached) return
        holder.layoutListenerAttached = true
        val frame = holder.root.findViewById<View>(R.id.card_preview_frame)
        frame.addOnLayoutChangeListener(
            object : View.OnLayoutChangeListener {
                override fun onLayoutChange(
                    view: View,
                    left: Int,
                    top: Int,
                    right: Int,
                    bottom: Int,
                    oldLeft: Int,
                    oldTop: Int,
                    oldRight: Int,
                    oldBottom: Int,
                ) {
                    if (right - left <= 0 || bottom - top <= 0) return
                    view.removeOnLayoutChangeListener(this)
                    holder.layoutListenerAttached = false
                    renderVisiblePreviews()
                }
            },
        )
    }

    private fun previewKey(
        holder: CardHolder,
        style: WidgetStyle,
        width: Int,
        height: Int,
        access: Boolean,
    ): String {
        val entry = holder.entry
        val now = System.currentTimeMillis()
        val bucket = when {
            WidgetPreviewRenderer.usesMinuteBitmap(entry.previewKind) && !access -> 0L
            entry.previewKind == PreviewKind.CLOCK ||
                entry.previewKind == PreviewKind.ANALOG_BOLD ||
                entry.previewKind == PreviewKind.ANALOG_SCALE ||
                entry.previewKind == PreviewKind.WORLD -> now / 60_000L
            entry.previewKind == PreviewKind.DATE ||
                entry.previewKind == PreviewKind.QUOTES -> now / 86_400_000L
            entry.previewKind == PreviewKind.KWGT_BATTERY ||
                entry.previewKind == PreviewKind.KWGT_BATTERY_MINI -> now / 300_000L
            else -> 0L
        }
        val zones = if (entry.previewKind == PreviewKind.WORLD) {
            WorldPrefs.previewZones(this).joinToString(",") { it.zoneId }
        } else {
            ""
        }
        return "${entry.id}|${style.id}|${width}x${height}|$access|$bucket|$zones"
    }

    private fun renderBitmap(
        holder: CardHolder,
        style: WidgetStyle,
        width: Int,
        height: Int,
    ): Boolean {
        val bitmap = try {
            WidgetPreviewRenderer.render(this, holder.entry, width, height, style)
        } catch (_: OutOfMemoryError) {
            null
        } catch (_: RuntimeException) {
            null
        } ?: return false
        val image = holder.root.findViewById<ImageView>(R.id.card_preview)
        previewBitmaps[image] = bitmap
        image.setImageBitmap(bitmap)
        image.visibility = View.VISIBLE
        holder.root.findViewById<TextClock>(R.id.card_preview_clock).visibility = View.GONE
        holder.root.findViewById<View>(R.id.card_preview_analog).visibility = View.GONE
        return true
    }

    private fun showClockFallback(holder: CardHolder, style: WidgetStyle, failed: Boolean) {
        val image = holder.root.findViewById<ImageView>(R.id.card_preview)
        previewBitmaps.remove(image)
        image.setImageDrawable(null)
        image.visibility = View.GONE
        holder.root.findViewById<View>(R.id.card_preview_analog).visibility = View.GONE
        val textClock = holder.root.findViewById<TextClock>(R.id.card_preview_clock)
        textClock.visibility = View.VISIBLE
        textClock.setTextColor(UpstreamNtDraw.ink(this, style))
        holder.root.findViewById<TextView>(R.id.card_status_detail).setText(
            if (failed) {
                R.string.preview_render_failed_detail
            } else {
                R.string.clock_plain_detail
            },
        )
        holder.root.findViewById<TextView>(R.id.card_preview_badge)
            .setText(R.string.preview_badge_live)
    }

    private fun showAnalogFallback(holder: CardHolder, failed: Boolean) {
        val image = holder.root.findViewById<ImageView>(R.id.card_preview)
        previewBitmaps.remove(image)
        image.setImageDrawable(null)
        image.visibility = View.GONE
        holder.root.findViewById<View>(R.id.card_preview_clock).visibility = View.GONE
        holder.root.findViewById<View>(R.id.card_preview_analog).visibility = View.VISIBLE
        holder.root.findViewById<TextView>(R.id.card_status_detail).setText(
            if (failed) {
                R.string.preview_render_failed_detail
            } else {
                R.string.analog_fallback_detail
            },
        )
        holder.root.findViewById<TextView>(R.id.card_preview_badge)
            .setText(R.string.preview_badge_live)
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

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private class CardHolder(val entry: CatalogEntry, val root: View) {
        var lastRenderKey: String? = null
        var layoutListenerAttached: Boolean = false
        var lastRenderFailed: Boolean = false
    }

    private companion object {
        const val EXTRA_DEBUG_FORCE_PIN_UNSUPPORTED =
            "com.dotwelwidgets.dotwellabs.extra.DEBUG_FORCE_PIN_UNSUPPORTED"
    }
}
