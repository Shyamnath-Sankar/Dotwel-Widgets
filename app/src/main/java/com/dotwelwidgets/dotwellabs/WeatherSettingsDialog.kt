package com.dotwelwidgets.dotwellabs

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Typeface
import android.text.InputType
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import com.dotwelwidgets.dotwellabs.weather.GeoLocation
import com.dotwelwidgets.dotwellabs.weather.TemperatureUnit
import com.dotwelwidgets.dotwellabs.weather.WeatherCache
import com.dotwelwidgets.dotwellabs.weather.WeatherController
import com.dotwelwidgets.dotwellabs.weather.WeatherDisplay
import com.dotwelwidgets.dotwellabs.weather.WeatherResult
import com.dotwelwidgets.dotwellabs.weather.WeatherSettings

/**
 * Optional Open-Meteo setup. Weather starts off. The explanation is always shown
 * before any request can be made, and geocoding only runs from the explicit
 * Search button. The selected city comes from the returned city/region/country
 * list; there is no default city and no startup or background polling.
 */
class WeatherSettingsDialog(
    private val activity: Activity,
    private val onConfigured: (() -> Unit)? = null,
) {
    private var dialog: AlertDialog? = null
    private var destroyed = false

    private lateinit var enableSwitch: Switch
    private lateinit var cityInput: EditText
    private lateinit var searchButton: Button
    private lateinit var resultsContainer: LinearLayout
    private lateinit var statusView: TextView
    private lateinit var refreshButton: Button
    private lateinit var deleteButton: Button
    private lateinit var celsiusButton: RadioButton
    private lateinit var fahrenheitButton: RadioButton

    private var searchResults: List<GeoLocation> = emptyList()
    private var selectedResult = -1
    private var configuredCallbackFired = false
    private var suppressUnitCallback = false
    private var lastStatusMessage: String? = null

    fun show() {
        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(16))
        }
        val scroll = ScrollView(activity).apply {
            addView(
                content,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        content.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.weather_explain)
                textSize = 12f
                setTextColor(activity.getColor(R.color.widget_muted))
                setLineSpacing(dp(2).toFloat(), 1f)
                movementMethod = LinkMovementMethod.getInstance()
            }.also { Linkify.addLinks(it, Linkify.WEB_URLS) },
            matchWrap(),
        )

        statusView = TextView(activity).apply {
            textSize = 12f
            setTextColor(activity.getColor(R.color.widget_ink))
            setPadding(0, dp(10), 0, dp(4))
        }
        content.addView(statusView, matchWrap())

        enableSwitch = Switch(activity).apply {
            setText(R.string.weather_enable_label)
            setTextColor(activity.getColor(R.color.widget_ink))
            textSize = 13f
            setPadding(0, dp(8), 0, dp(8))
            setOnCheckedChangeListener { _, checked -> onEnabledChanged(checked) }
        }
        content.addView(enableSwitch, matchWrap())

        content.addView(
            sectionLabel(activity.getString(R.string.weather_city_label)),
            matchWrap(),
        )
        cityInput = EditText(activity).apply {
            hint = activity.getString(R.string.weather_city_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            maxLines = 1
        }
        content.addView(cityInput, matchWrap())
        searchButton = Button(activity).apply {
            setText(R.string.weather_search)
            setOnClickListener { search() }
        }
        content.addView(searchButton, matchWrap())
        resultsContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(resultsContainer, matchWrap())

        content.addView(
            sectionLabel(activity.getString(R.string.weather_unit_label)),
            matchWrap(),
        )
        val unitGroup = RadioGroup(activity).apply {
            orientation = RadioGroup.HORIZONTAL
        }
        celsiusButton = RadioButton(activity).apply {
            id = View.generateViewId()
            setText(R.string.weather_celsius)
            setTextColor(activity.getColor(R.color.widget_ink))
        }
        fahrenheitButton = RadioButton(activity).apply {
            id = View.generateViewId()
            setText(R.string.weather_fahrenheit)
            setTextColor(activity.getColor(R.color.widget_ink))
        }
        unitGroup.addView(celsiusButton, wrapWrap())
        unitGroup.addView(fahrenheitButton, wrapWrap())
        unitGroup.setOnCheckedChangeListener { _, checkedId ->
            if (suppressUnitCallback) return@setOnCheckedChangeListener
            val unit = if (checkedId == fahrenheitButton.id) {
                TemperatureUnit.FAHRENHEIT
            } else {
                TemperatureUnit.CELSIUS
            }
            onUnitChanged(unit)
        }
        content.addView(unitGroup, matchWrap())

        refreshButton = Button(activity).apply {
            setText(R.string.weather_refresh)
            setOnClickListener { fetchNow() }
        }
        content.addView(
            refreshButton,
            matchWrap().apply { topMargin = dp(10) },
        )
        deleteButton = Button(activity).apply {
            setText(R.string.weather_delete)
            setOnClickListener { confirmDelete() }
        }
        content.addView(deleteButton, matchWrap())

        content.addView(
            TextView(activity).apply {
                text = activity.getString(R.string.weather_attribution)
                textSize = 11f
                setTextColor(activity.getColor(R.color.widget_muted))
                setPadding(0, dp(14), 0, 0)
                movementMethod = LinkMovementMethod.getInstance()
            }.also { Linkify.addLinks(it, Linkify.WEB_URLS) },
            matchWrap(),
        )

        dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.weather_dialog_title)
            .setView(scroll)
            .setNegativeButton(R.string.weather_close, null)
            .create()
        dialog?.setOnDismissListener { destroyed = true }
        dialog?.show()
        refreshUi()
    }

    private fun onEnabledChanged(enabled: Boolean) {
        if (enabled == WeatherSettings.isEnabled(activity)) return
        WeatherSettings.setEnabled(activity, enabled)
        WeatherController.invalidate()
        if (!enabled) WeatherController.cancelPending()
        WidgetUpdater.updateWeatherProviders(activity)
        refreshUi()
        updateStatus(
            activity.getString(
                if (enabled) R.string.weather_status_enabled else R.string.weather_status_off,
            ),
        )
    }

    private fun search() {
        val query = cityInput.text?.toString()?.trim().orEmpty()
        if (query.isBlank()) {
            updateStatus(activity.getString(R.string.weather_city_hint))
            return
        }
        searchButton.isEnabled = false
        updateStatus(activity.getString(R.string.weather_searching))
        WeatherController.search(query) { locations, error ->
            if (destroyed) return@search
            searchButton.isEnabled = WeatherSettings.isEnabled(activity)
            if (locations == null || locations.isEmpty()) {
                searchResults = emptyList()
                selectedResult = -1
                buildResults()
                updateStatus(
                    if (error != null) {
                        activity.getString(R.string.weather_no_results) + " " + error
                    } else {
                        activity.getString(R.string.weather_no_results)
                    },
                )
            } else {
                searchResults = locations
                selectedResult = if (locations.size == 1) 0 else -1
                buildResults()
                updateStatus(activity.getString(R.string.weather_choose_result))
            }
        }
    }

    private fun buildResults() {
        resultsContainer.removeAllViews()
        if (searchResults.isEmpty()) return
        val group = RadioGroup(activity).apply {
            orientation = RadioGroup.VERTICAL
        }
        searchResults.forEachIndexed { index, location ->
            val button = RadioButton(activity).apply {
                id = View.generateViewId()
                text = resultLabel(location)
                textSize = 12f
                setTextColor(activity.getColor(R.color.widget_ink))
                isChecked = index == selectedResult
                setOnCheckedChangeListener { _, checked ->
                    if (checked) selectedResult = index
                }
            }
            group.addView(button, matchWrap())
        }
        resultsContainer.addView(group, matchWrap())
        resultsContainer.addView(
            Button(activity).apply {
                setText(R.string.weather_use_city)
                setOnClickListener { useSelectedCity() }
            },
            matchWrap(),
        )
    }

    private fun useSelectedCity() {
        val location = searchResults.getOrNull(selectedResult)
        if (location == null) {
            updateStatus(activity.getString(R.string.weather_select_city_first))
            return
        }
        WeatherSettings.setEnabled(activity, true)
        WeatherSettings.setLocation(activity, location)
        WeatherCache.clear(activity)
        WeatherController.invalidate()
        WeatherController.cancelPending()
        WidgetUpdater.updateWeatherProviders(activity)
        refreshUi()
        val callback = onConfigured
        if (callback != null && !configuredCallbackFired) {
            configuredCallbackFired = true
            dialog?.dismiss()
            callback()
            return
        }
        fetchNow()
    }

    private fun onUnitChanged(unit: TemperatureUnit) {
        if (unit == WeatherSettings.unit(activity)) return
        WeatherSettings.setUnit(activity, unit)
        WeatherCache.clear(activity)
        WeatherController.invalidate()
        WeatherController.cancelPending()
        WidgetUpdater.updateWeatherProviders(activity)
        refreshUi()
        if (WeatherSettings.isEnabled(activity) &&
            WeatherSettings.location(activity) != null
        ) {
            fetchNow()
        }
    }

    private fun fetchNow() {
        if (!WeatherSettings.isEnabled(activity)) {
            updateStatus(activity.getString(R.string.weather_status_off))
            return
        }
        if (WeatherSettings.location(activity) == null) {
            updateStatus(activity.getString(R.string.weather_status_no_city))
            return
        }
        refreshButton.isEnabled = false
        updateStatus(activity.getString(R.string.weather_refreshing))
        val started = WeatherController.fetch(activity, auto = false) { result ->
            if (destroyed) return@fetch
            refreshButton.isEnabled = WeatherSettings.isEnabled(activity)
            WidgetUpdater.updateWeatherProviders(activity)
            when (result) {
                is WeatherResult.Success -> updateStatus(computeStatus())
                is WeatherResult.Failure -> {
                    val cached = WeatherCache.load(activity)
                    if (cached != null) {
                        updateStatus(
                            activity.getString(
                                R.string.weather_status_failed_cached,
                                result.reason.name.lowercase(),
                                computeStatus(),
                            ),
                        )
                    } else {
                        updateStatus(
                            activity.getString(
                                R.string.weather_status_failed,
                                result.reason.name.lowercase(),
                            ),
                        )
                    }
                }
            }
        }
        if (!started) {
            refreshButton.isEnabled = true
            updateStatus(activity.getString(R.string.weather_status_no_city))
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(activity)
            .setTitle(R.string.weather_delete_confirm_title)
            .setMessage(R.string.weather_delete_confirm_message)
            .setPositiveButton(R.string.weather_delete_confirm) { _, _ -> deleteAll() }
            .setNegativeButton(R.string.weather_cancel, null)
            .show()
    }

    private fun deleteAll() {
        WeatherSettings.setEnabled(activity, false)
        WeatherSettings.clearLocation(activity)
        WeatherCache.clear(activity)
        WeatherController.invalidate()
        WeatherController.cancelPending()
        WidgetUpdater.updateWeatherProviders(activity)
        searchResults = emptyList()
        selectedResult = -1
        buildResults()
        refreshUi()
        updateStatus(activity.getString(R.string.weather_status_off))
    }

    private fun refreshUi() {
        val enabled = WeatherSettings.isEnabled(activity)
        enableSwitch.setOnCheckedChangeListener(null)
        enableSwitch.isChecked = enabled
        enableSwitch.setOnCheckedChangeListener { _, checked -> onEnabledChanged(checked) }
        cityInput.isEnabled = enabled
        searchButton.isEnabled = enabled
        refreshButton.isEnabled = enabled
        deleteButton.isEnabled = true
        suppressUnitCallback = true
        val unit = WeatherSettings.unit(activity)
        celsiusButton.isChecked = unit == TemperatureUnit.CELSIUS
        fahrenheitButton.isChecked = unit == TemperatureUnit.FAHRENHEIT
        suppressUnitCallback = false
        updateStatus(lastStatusMessage)
    }

    private fun updateStatus(message: String? = null) {
        lastStatusMessage = message
        val text = message ?: computeStatus()
        statusView.text = text
    }

    private fun computeStatus(): String {
        if (!WeatherSettings.isEnabled(activity)) {
            return activity.getString(R.string.weather_status_off)
        }
        val location = WeatherSettings.location(activity)
            ?: return activity.getString(R.string.weather_status_no_city)
        val snapshot = WeatherCache.load(activity)
            ?: return activity.getString(R.string.weather_status_no_cache, location.name)
        val stale = WeatherDisplay.isStale(snapshot, System.currentTimeMillis())
        return activity.getString(
            R.string.weather_status_updated,
            WeatherDisplay.locationLine(location),
            WeatherDisplay.fetchedTime(snapshot),
            if (stale) activity.getString(R.string.weather_status_stale_suffix) else "",
        )
    }

    private fun resultLabel(location: GeoLocation): String = buildList {
        add(location.name)
        location.admin1?.let(::add)
        location.country?.let(::add)
    }.joinToString(", ")

    private fun sectionLabel(text: String): TextView = TextView(activity).apply {
        this.text = text
        textSize = 12f
        setTypeface(Typeface.DEFAULT_BOLD)
        setTextColor(activity.getColor(R.color.widget_ink))
        setPadding(0, dp(12), 0, dp(4))
    }

    private fun matchWrap(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    )

    private fun wrapWrap(): LinearLayout.LayoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT,
    )

    private fun dp(value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()
}
