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
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.util.Calendar

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val tag = "MainActivity"
    private val cards = mutableListOf<CardHolder>()
    private val previewBitmaps = mutableMapOf<ImageView, Bitmap>()
    private var exactAlarmReceiverRegistered = false
    private var pendingEntryAfterAccess: CatalogEntry? = null

    private val exactAlarmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action ==
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
            ) {
                updateClockStateUi()
                WidgetUpdater.updateAll(this@MainActivity)
            }
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            renderAllPreviews()
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
            renderAllPreviews()
            WidgetUpdater.updateAll(this)
        }

        findViewById<Button>(R.id.refresh_widgets).setOnClickListener {
            WidgetUpdater.updateAll(this)
            Toast.makeText(this, R.string.widgets_refreshed, Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.exact_alarm_button).setOnClickListener {
            requestExactAlarmAccess()
        }
        updateClockStateUi()
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
        handler.post(tick)
        updateClockStateUi()
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
        handler.removeCallbacks(tick)
        previewBitmaps.values.forEach { it.recycle() }
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
        WidgetGroup.values().forEach { group ->
            val entries = WidgetCatalog.entries.filter { it.group == group }
            if (entries.isEmpty()) return@forEach
            val header = TextView(this).apply {
                text = getString(group.labelRes)
                setTextColor(getColor(R.color.widget_muted))
                textSize = 11f
                letterSpacing = 0.18f
                setPadding(4, 14, 4, 4)
            }
            container.addView(header)
            entries.forEach { entry ->
                val root = inflater.inflate(R.layout.item_widget_card, container, false)
                container.addView(root)
                cards += CardHolder(entry, root)
            }
        }
        cards.forEach { bindCard(it) }
    }

    private fun bindCard(holder: CardHolder) {
        val entry = holder.entry
        val root = holder.root
        root.findViewById<TextView>(R.id.card_title).text = entry.name
        val chip = root.findViewById<TextView>(R.id.card_status_chip)
        chip.text = getString(entry.state.labelRes)
        chip.backgroundTintList = ColorStateList.valueOf(stateColor(entry.state))
        root.findViewById<TextView>(R.id.card_upstream).text = entry.upstream
        root.findViewById<TextView>(R.id.card_description).setText(entry.descriptionRes)
        root.findViewById<TextView>(R.id.card_status_detail).setText(entry.detailRes)

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
        val inflater = LayoutInflater.from(this)
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
                WorldPrefs.setPending(this, chosen)
                renderAllPreviews()
                requestPin(entry)
            }
            .setNegativeButton(R.string.world_cancel, null)
            .show()
    }

    private fun requestPin(entry: CatalogEntry) {
        val provider = entry.providerClass ?: return
        val kind = entry.pinKind ?: return
        if (isDebugForcePinUnsupported()) {
            showPinFallbackDialog()
            return
        }
        val manager = AppWidgetManager.getInstance(this)
        if (!manager.isRequestPinAppWidgetSupported) {
            showPinFallbackDialog()
            return
        }
        val callback = PendingIntent.getBroadcast(
            this,
            kind.hashCode() and 0x7FFF,
            Intent(this, PinResultReceiver::class.java).apply {
                action = PinResultReceiver.ACTION_PIN_RESULT
                putExtra(PinResultReceiver.EXTRA_PIN_KIND, kind)
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
        if (entry.action == CardAction.CLOCK_ACCESS) {
            val access = MinuteTickScheduler.hasExactAccess(this)
            secondary.visibility = if (access) View.GONE else View.VISIBLE
            secondary.setText(R.string.exact_alarm_enable)
            val clock = entry.id == "digital"
            detail.text = when {
                access && clock -> getString(R.string.clock_dotted_detail)
                access -> getString(R.string.analog_dotted_detail)
                clock -> getString(R.string.clock_plain_detail)
                else -> getString(R.string.analog_fallback_detail)
            }
        } else if (entry.action == CardAction.WORLD_ZONES) {
            secondary.visibility = View.VISIBLE
        }
    }

    private fun applyStyleToFrames(style: WidgetStyle) {
        cards.forEach { holder ->
            holder.root.findViewById<View>(R.id.card_preview_frame)
                .setBackgroundResource(style.backgroundRes)
        }
    }

    private fun renderAllPreviews() {
        val style = WidgetPrefs.style(this)
        cards.forEach { renderCard(it, style) }
    }

    private fun renderCard(holder: CardHolder, style: WidgetStyle) {
        val frame = holder.root.findViewById<View>(R.id.card_preview_frame)
        val image = holder.root.findViewById<ImageView>(R.id.card_preview)
        if (frame.width <= 0 || frame.height <= 0) {
            frame.post { renderCard(holder, style) }
            return
        }
        val padding = dp(10)
        val width = (frame.width - padding * 2).coerceAtLeast(1)
        val height = (frame.height - padding * 2).coerceAtLeast(1)
        val bitmap = WidgetPreviewRenderer.render(this, holder.entry, width, height, style)
        val previous = previewBitmaps.put(image, bitmap)
        image.setImageBitmap(bitmap)
        previous?.recycle()
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

    private class CardHolder(val entry: CatalogEntry, val root: View)

    private companion object {
        const val EXTRA_DEBUG_FORCE_PIN_UNSUPPORTED =
            "com.dotwelwidgets.dotwellabs.extra.DEBUG_FORCE_PIN_UNSUPPORTED"
    }
}
