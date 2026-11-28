package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DotGridApplication
import com.example.data.telemetry.AstronomyCalculator
import com.example.model.AppSettings
import com.example.model.DeviceTelemetry
import com.example.model.LiveSnapshot
import com.example.model.WidgetCatalog
import com.example.model.WidgetCategory
import com.example.model.WidgetConfig
import com.example.model.WidgetInteractiveAction
import com.example.model.WidgetThemeMode
import com.example.ui.theme.AppSurfaceMode
import com.example.widget.SystemControlsLauncher
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExploreUiState(
    val selectedCategory: WidgetCategory? = null,
    val showingAllGallery: Boolean = false,
    val searchQuery: String = ""
)

class DotGridViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = (application as DotGridApplication).repository

    // Off-main-thread hardware telemetry flow (polls every 6s on Dispatchers.IO -> ZERO UI thread jank!)
    private val telemetryFlow: StateFlow<DeviceTelemetry> = flow {
        while (true) {
            emit(repo.readDeviceTelemetry())
            delay(6_000L)
        }
    }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DeviceTelemetry()
        )

    // Calm 15s clock bucket for Home / Gallery / My Widgets (eliminates 1s full-tree recomposition lag!)
    private val calmClockFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(15_000L)
        }
    }

    val settings: StateFlow<AppSettings> = repo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    val onboardingCompleted: StateFlow<Boolean?> = repo.settingsFlow
        .map<AppSettings, Boolean?> { it.hasCompletedOnboarding }
        .distinctUntilChanged()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val savedWidgets: StateFlow<List<WidgetConfig>> = repo.savedWidgetsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val liveSnapshot: StateFlow<LiveSnapshot> = combine(
        calmClockFlow,
        telemetryFlow,
        repo.settingsFlow
    ) { nowMillis, telemetry, appSettings ->
        LiveSnapshot(
            nowMillis = nowMillis,
            telemetry = telemetry,
            lunar = AstronomyCalculator.calculateLunarInfo(nowMillis),
            settings = appSettings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LiveSnapshot()
    )

    private val _exploreState = MutableStateFlow(ExploreUiState())
    val exploreState: StateFlow<ExploreUiState> = _exploreState.asStateFlow()

    private val _studioDraft = MutableStateFlow<WidgetConfig?>(null)
    val studioDraft: StateFlow<WidgetConfig?> = _studioDraft.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun openCategoryGallery(category: WidgetCategory?) {
        _exploreState.update {
            it.copy(selectedCategory = category, showingAllGallery = true)
        }
    }

    fun closeCategoryGallery() {
        _exploreState.update {
            it.copy(selectedCategory = null, showingAllGallery = false, searchQuery = "")
        }
    }

    fun selectCategory(category: WidgetCategory?) {
        _exploreState.update { it.copy(selectedCategory = category) }
    }

    fun updateSearchQuery(query: String) {
        _exploreState.update {
            it.copy(
                searchQuery = query,
                showingAllGallery = if (query.isNotBlank()) true else it.showingAllGallery
            )
        }
    }

    fun openStudioForCatalogWidget(typeId: String) {
        val spec = WidgetCatalog.getSpec(typeId)
        val curSettings = settings.value
        val defaultTheme = when (curSettings.appSurfaceMode) {
            AppSurfaceMode.LIGHT -> WidgetThemeMode.LIGHT
            AppSurfaceMode.DARK -> WidgetThemeMode.DARK
            AppSurfaceMode.OLED_NIGHT -> WidgetThemeMode.OLED_NIGHT
            AppSurfaceMode.SYSTEM -> WidgetThemeMode.SYSTEM
        }
        _studioDraft.value = WidgetCatalog.createDefaultConfig(
            spec = spec,
            defaultTheme = defaultTheme,
            use24Hour = curSettings.use24Hour
        )
    }

    fun openStudioForSavedWidget(config: WidgetConfig) {
        _studioDraft.value = config
    }

    fun updateStudioDraft(transform: (WidgetConfig) -> WidgetConfig) {
        _studioDraft.update { current -> current?.let(transform) }
    }

    fun importPhotoForStudioDraft(uri: Uri) {
        viewModelScope.launch {
            repo.importStudioPhoto(uri).onSuccess { path ->
                _studioDraft.update { it?.copy(photoPath = path) }
                _snackbarMessage.value = "Photo added to Studio widget"
            }.onFailure {
                _snackbarMessage.value = "Could not load selected photo"
            }
        }
    }

    fun clearStudioPhoto() {
        _studioDraft.update { it?.copy(photoPath = "") }
    }

    fun resetStudioDraftToDefault() {
        val current = _studioDraft.value ?: return
        val spec = WidgetCatalog.getSpec(current.widgetTypeId)
        val curSettings = settings.value
        _studioDraft.value = WidgetCatalog.createDefaultConfig(
            spec = spec,
            use24Hour = curSettings.use24Hour
        ).copy(id = current.id, appWidgetId = current.appWidgetId)
        _snackbarMessage.value = "Reset ${spec.name} to defaults"
    }

    fun interactWithStudioDraft() {
        val current = _studioDraft.value ?: return
        val spec = WidgetCatalog.getSpec(current.widgetTypeId)
        when (spec.interactiveAction) {
            WidgetInteractiveAction.OPEN_WIFI_PANEL -> SystemControlsLauncher.openWifiPanel(getApplication())
            WidgetInteractiveAction.OPEN_INTERNET_PANEL -> SystemControlsLauncher.openInternetPanel(getApplication())
            WidgetInteractiveAction.OPEN_BLUETOOTH_SETTINGS -> SystemControlsLauncher.openBluetoothSettings(getApplication())
            WidgetInteractiveAction.OPEN_AUDIO_PANEL -> SystemControlsLauncher.openVolumeAudioPanel(getApplication())
            WidgetInteractiveAction.NONE -> Unit
            else -> _studioDraft.value = repo.applyInteractiveTap(current)
        }
    }

    fun saveStudioDraft() {
        val draft = _studioDraft.value ?: return
        viewModelScope.launch {
            val savedId = repo.saveWidgetConfig(draft)
            _studioDraft.value = draft.copy(id = savedId)
            if (draft.appWidgetId > 0) {
                WidgetUpdater.updateAllHomeScreenWidgets(getApplication())
            }
            _snackbarMessage.value = "Saved \"${draft.customName}\" to My Widgets"
        }
    }

    fun pinStudioDraftToHomeScreen() {
        val draft = _studioDraft.value ?: return
        viewModelScope.launch {
            val savedId = repo.saveWidgetConfig(draft)
            val persisted = draft.copy(id = savedId)
            _studioDraft.value = persisted
            repo.preferences.setPendingPinPresetId(savedId)
            val requested = WidgetUpdater.requestPinToHomeScreen(
                context = getApplication(),
                config = persisted,
                snapshot = liveSnapshot.value
            )
            _snackbarMessage.value = if (requested) {
                "Pin dialog opened — tap Add to place on your Home Screen"
            } else {
                "Saved! Long-press your Home Screen → Widgets → AnyWidgets (${persisted.gridSize.label}) to place it."
            }
        }
    }

    fun pinSavedWidgetToHomeScreen(config: WidgetConfig) {
        viewModelScope.launch {
            val savedId = if (config.id > 0L) config.id else repo.saveWidgetConfig(config)
            val persisted = config.copy(id = savedId)
            repo.preferences.setPendingPinPresetId(savedId)
            val requested = WidgetUpdater.requestPinToHomeScreen(
                context = getApplication(),
                config = persisted,
                snapshot = liveSnapshot.value
            )
            _snackbarMessage.value = if (requested) {
                "Pinning \"${persisted.customName}\" to Home Screen"
            } else {
                "Long-press Home Screen → Widgets → AnyWidgets (${persisted.gridSize.label}) to place"
            }
        }
    }

    fun interactWithSavedWidget(config: WidgetConfig) {
        val spec = WidgetCatalog.getSpec(config.widgetTypeId)
        viewModelScope.launch {
            when (spec.interactiveAction) {
                WidgetInteractiveAction.OPEN_WIFI_PANEL -> SystemControlsLauncher.openWifiPanel(getApplication())
                WidgetInteractiveAction.OPEN_INTERNET_PANEL -> SystemControlsLauncher.openInternetPanel(getApplication())
                WidgetInteractiveAction.OPEN_BLUETOOTH_SETTINGS -> SystemControlsLauncher.openBluetoothSettings(getApplication())
                WidgetInteractiveAction.OPEN_AUDIO_PANEL -> SystemControlsLauncher.openVolumeAudioPanel(getApplication())
                WidgetInteractiveAction.NONE -> Unit
                else -> {
                    val mutated = repo.applyInteractiveTap(config)
                    repo.saveWidgetConfig(mutated)
                    if (mutated.appWidgetId > 0) {
                        WidgetUpdater.updateAllHomeScreenWidgets(getApplication())
                    }
                }
            }
        }
    }

    fun duplicateSavedWidget(config: WidgetConfig) {
        viewModelScope.launch {
            repo.duplicateWidget(config)
            _snackbarMessage.value = "Duplicated \"${config.customName}\""
        }
    }

    fun deleteSavedWidget(config: WidgetConfig) {
        viewModelScope.launch {
            repo.deleteWidget(config.id)
            _snackbarMessage.value = "Removed \"${config.customName}\""
        }
    }

    fun setAppSurfaceMode(mode: AppSurfaceMode) {
        viewModelScope.launch {
            repo.setAppSurfaceMode(mode)
        }
    }

    fun cycleAppSurfaceMode() {
        val next = when (settings.value.appSurfaceMode) {
            AppSurfaceMode.OLED_NIGHT -> AppSurfaceMode.DARK
            AppSurfaceMode.DARK -> AppSurfaceMode.LIGHT
            AppSurfaceMode.LIGHT -> AppSurfaceMode.OLED_NIGHT
            AppSurfaceMode.SYSTEM -> AppSurfaceMode.LIGHT
        }
        setAppSurfaceMode(next)
    }

    fun setAutoNightHours(start: Int, end: Int) {
        viewModelScope.launch {
            repo.setAutoNightHours(start, end)
            WidgetUpdater.updateAllHomeScreenWidgets(getApplication())
        }
    }

    fun setUse24Hour(use24: Boolean) {
        viewModelScope.launch {
            repo.setUse24Hour(use24)
            WidgetUpdater.updateAllHomeScreenWidgets(getApplication())
        }
    }

    fun setRefreshIntervalMinutes(minutes: Int) {
        viewModelScope.launch {
            repo.setRefreshIntervalMinutes(minutes)
            _snackbarMessage.value = if (minutes == 0) {
                "Background refresh paused (Manual & tap-only)"
            } else {
                "Eco refresh cadence set to every ${minutes}m"
            }
        }
    }

    fun setUltraEcoMode(enabled: Boolean) {
        viewModelScope.launch {
            repo.setUltraEcoMode(enabled)
            WidgetUpdater.updateAllHomeScreenWidgets(getApplication())
        }
    }

    fun refreshAllHomeWidgetsNow() {
        viewModelScope.launch {
            WidgetUpdater.updateAllHomeScreenWidgets(getApplication())
            _snackbarMessage.value = "All pinned Home Screen widgets synced"
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repo.setOnboardingCompleted(true)
        }
    }

    fun replayOnboarding() {
        viewModelScope.launch {
            repo.setOnboardingCompleted(false)
        }
    }
}
