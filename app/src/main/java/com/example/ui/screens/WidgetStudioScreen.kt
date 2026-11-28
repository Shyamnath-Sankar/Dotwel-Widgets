package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.DotDensity
import com.example.model.LiveSnapshot
import com.example.model.WidgetBorderStyle
import com.example.model.WidgetCatalog
import com.example.model.WidgetConfig
import com.example.model.WidgetCustomFieldType
import com.example.model.WidgetGridSize
import com.example.model.WidgetInteractiveAction
import com.example.model.WidgetThemeMode
import com.example.ui.theme.DotMatrixFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalDotGridColors
import com.example.ui.widgets.DotGridBackdropCanvas
import com.example.ui.widgets.DotGridWidgetCard
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WidgetStudioScreen(
    draft: WidgetConfig,
    snapshot: LiveSnapshot,
    onBack: () -> Unit,
    onDraftChange: ((WidgetConfig) -> WidgetConfig) -> Unit,
    onImportPhoto: (Uri) -> Unit,
    onClearPhoto: () -> Unit,
    onReset: () -> Unit,
    onInteractPreview: () -> Unit,
    onSave: () -> Unit,
    onPinToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDotGridColors.current
    val spec = remember(draft.widgetTypeId) { WidgetCatalog.getSpec(draft.widgetTypeId) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onImportPhoto(uri)
    }
    var stageBackdrop by remember { mutableStateOf(0) } // 0 = OLED Black, 1 = Charcoal, 2 = Light Paper
    val stageBg by animateColorAsState(
        targetValue = when (stageBackdrop) {
            0 -> Color(0xFF000000)
            1 -> Color(0xFF1A1A1A)
            else -> Color(0xFFE6E6E1)
        },
        animationSpec = tween(220),
        label = "stage_bg"
    )
    val stageDot by animateColorAsState(
        targetValue = if (stageBackdrop == 2) Color(0xFFBFBFB8) else Color(0xFF2B2B28),
        animationSpec = tween(220),
        label = "stage_dot"
    )

    Column(modifier = modifier.fillMaxSize().background(ext.canvas)) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("studio_back_btn")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = ext.ink
                    )
                }
                Column {
                    Text(
                        text = spec.name.uppercase(Locale.US),
                        fontFamily = DotMatrixFontFamily,
                        fontSize = 18.sp,
                        color = ext.ink,
                        maxLines = 1
                    )
                    Text(
                        text = String.format(Locale.US, "#%02d · %s STUDIO", spec.number, spec.category.displayName.uppercase(Locale.US)),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = ext.mutedInk
                    )
                }
            }
            IconButton(onClick = onReset, modifier = Modifier.testTag("studio_reset_btn")) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset defaults", tint = ext.mutedInk)
            }
        }

        // Scrollable Customizer Content
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Live Interactive Preview Stage
            item(key = "hero_stage") {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (draft.gridSize == WidgetGridSize.LARGE_4X4) 308.dp else 242.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(stageBg)
                            .border(1.dp, ext.hairline, RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        DotGridBackdropCanvas(dotColor = stageDot, density = DotDensity.BALANCED)

                        // Backdrop switcher top-left
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("OLED", "DARK", "PAPER").forEachIndexed { idx, label ->
                                val active = stageBackdrop == idx
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (active) Color.White else Color.Transparent)
                                        .clickable { stageBackdrop = idx }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("stage_bg_$idx")
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 8.sp,
                                        color = if (active) Color.Black else Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        if (spec.interactiveAction != WidgetInteractiveAction.NONE) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.TouchApp, contentDescription = null, tint = ext.redAccent, modifier = Modifier.size(11.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("TAP TO TEST", fontFamily = JetBrainsMonoFontFamily, fontSize = 8.sp, color = Color.White)
                            }
                        }

                        val wFrac = when (draft.gridSize) {
                            WidgetGridSize.SMALL_2X2 -> 0.50f
                            WidgetGridSize.WIDE_4X2 -> 0.90f
                            WidgetGridSize.LARGE_4X4 -> 0.68f
                        }

                        DotGridWidgetCard(
                            config = draft,
                            snapshot = snapshot,
                            compactPreview = false,
                            onClick = if (spec.interactiveAction != WidgetInteractiveAction.NONE) onInteractPreview else null,
                            modifier = Modifier
                                .padding(top = 42.dp, bottom = 14.dp)
                                .fillMaxWidth(wFrac)
                                .testTag("studio_live_preview")
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = spec.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.mutedInk,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // 2. Theme Mode & Nighting Selector
            item(key = "theme_section") {
                StudioSectionCard(title = "SURFACE & NIGHTING MODE") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(WidgetThemeMode.entries) { mode ->
                            StudioPillOption(
                                label = mode.label,
                                selected = draft.themeMode == mode,
                                onClick = { onDraftChange { it.copy(themeMode = mode) } },
                                tag = "studio_theme_${mode.name.lowercase()}"
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Auto-Night switches this widget to true-black OLED Night automatically during your Auto-Night schedule.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.mutedInk
                    )
                }
            }

            // 3. Footprint & Geometry Controls
            item(key = "geometry_section") {
                StudioSectionCard(title = "GRID FOOTPRINT & GEOMETRY") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WidgetGridSize.entries.forEach { size ->
                            StudioPillOption(
                                label = size.label,
                                selected = draft.gridSize == size,
                                onClick = { onDraftChange { it.copy(gridSize = size) } },
                                tag = "studio_size_${size.name.lowercase()}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    StudioSliderRow(
                        label = "CORNER RADIUS",
                        valueText = "${draft.cornerRadiusDp} DP",
                        value = draft.cornerRadiusDp.toFloat(),
                        range = 10f..32f,
                        onValueChange = { v -> onDraftChange { it.copy(cornerRadiusDp = v.roundToInt()) } }
                    )

                    Spacer(Modifier.height(10.dp))
                    StudioSliderRow(
                        label = "SURFACE OPACITY",
                        valueText = "${(draft.backgroundOpacity * 100).roundToInt()}%",
                        value = draft.backgroundOpacity,
                        range = 0.40f..1.0f,
                        onValueChange = { v -> onDraftChange { it.copy(backgroundOpacity = v) } }
                    )
                }
            }

            // 4. Dot Matrix & Border Aesthetics
            item(key = "aesthetics_section") {
                StudioSectionCard(title = "DOT MATRIX & ACCENT STYLE") {
                    Text("DOT GRID DENSITY", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = ext.mutedInk)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DotDensity.entries.forEach { density ->
                            StudioPillOption(
                                label = density.label,
                                selected = draft.dotDensity == density,
                                onClick = { onDraftChange { it.copy(dotDensity = density) } },
                                tag = "studio_density_${density.name.lowercase()}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text("FRAME BORDER", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = ext.mutedInk)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WidgetBorderStyle.entries.forEach { style ->
                            StudioPillOption(
                                label = style.label,
                                selected = draft.borderStyle == style,
                                onClick = { onDraftChange { it.copy(borderStyle = style) } },
                                tag = "studio_border_${style.name.lowercase()}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    StudioSwitchRow(
                        title = "Signature Red Accent Dot",
                        subtitle = "Turn off for strict pure black & white monochrome",
                        checked = draft.showRedAccent,
                        onCheckedChange = { v -> onDraftChange { it.copy(showRedAccent = v) } },
                        tag = "switch_red_accent"
                    )
                    StudioSwitchRow(
                        title = "Dot-Matrix Surface Grid",
                        subtitle = "Subtle micro-dot background pattern",
                        checked = draft.showDotBackdrop,
                        onCheckedChange = { v -> onDraftChange { it.copy(showDotBackdrop = v) } },
                        tag = "switch_dot_backdrop"
                    )
                    StudioSwitchRow(
                        title = "24-Hour Clock Format",
                        subtitle = "Overrides global format for this widget",
                        checked = draft.use24Hour,
                        onCheckedChange = { v -> onDraftChange { it.copy(use24Hour = v) } },
                        tag = "switch_24h"
                    )
                }
            }

            // 5. Widget-Specific Content & Behavior Settings
            item(key = "widget_specific_section") {
                StudioSectionCard(title = "INDIVIDUAL WIDGET CONTENT") {
                    StudioTextField(
                        label = "Preset Name (My Widgets)",
                        value = draft.customName,
                        onValueChange = { v -> onDraftChange { it.copy(customName = v) } },
                        tag = "input_preset_name"
                    )

                    when (spec.customFieldType) {
                        WidgetCustomFieldType.PHOTO_PICKER,
                        WidgetCustomFieldType.PHOTO_AND_QUOTE -> {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "PHOTO STUDIO (ZERO-PERMISSION ANDROID PHOTO PICKER)",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                color = ext.mutedInk
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ext.ink, contentColor = ext.canvas),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("studio_pick_photo_btn")
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (draft.photoPath.isBlank()) "PICK PHOTO" else "CHANGE PHOTO",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp
                                    )
                                }
                                if (draft.photoPath.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = onClearPhoto,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("studio_clear_photo_btn")
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear photo", tint = ext.redAccent, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            StudioSwitchRow(
                                title = "Monochrome B&W Film Filter",
                                subtitle = "Turn off to display your original color photo",
                                checked = draft.monochromePhoto,
                                onCheckedChange = { v -> onDraftChange { it.copy(monochromePhoto = v) } },
                                tag = "switch_monochrome_photo"
                            )
                            Spacer(Modifier.height(8.dp))
                            StudioTextField(
                                label = "Studio Caption / Stamp",
                                value = draft.customTitle,
                                onValueChange = { v -> onDraftChange { it.copy(customTitle = v) } },
                                tag = "input_photo_caption"
                            )
                            if (spec.customFieldType == WidgetCustomFieldType.PHOTO_AND_QUOTE) {
                                Spacer(Modifier.height(10.dp))
                                StudioTextField(
                                    label = "Overlay Quote / Note",
                                    value = draft.customNote,
                                    onValueChange = { v -> onDraftChange { it.copy(customNote = v) } },
                                    tag = "input_photo_quote"
                                )
                            }
                        }

                        WidgetCustomFieldType.CUSTOM_QUOTE -> {
                            Spacer(Modifier.height(12.dp))
                            StudioTextField(
                                label = "Custom Quote (leave blank for rotating deck)",
                                value = draft.customNote,
                                onValueChange = { v -> onDraftChange { it.copy(customNote = v) } },
                                tag = "input_custom_quote"
                            )
                            Spacer(Modifier.height(10.dp))
                            StudioTextField(
                                label = "Author / Signature",
                                value = draft.customTitle,
                                onValueChange = { v -> onDraftChange { it.copy(customTitle = v) } },
                                tag = "input_quote_author"
                            )
                        }

                        WidgetCustomFieldType.BUDS_LABEL -> {
                            Spacer(Modifier.height(12.dp))
                            StudioTextField(
                                label = "Fallback Earphones / Buds Label",
                                value = draft.customTitle,
                                onValueChange = { v -> onDraftChange { it.copy(customTitle = v) } },
                                tag = "input_buds_label"
                            )
                        }

                        WidgetCustomFieldType.TIMEZONE -> {
                            Spacer(Modifier.height(12.dp))
                            Text("SECOND WORLD CITY", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = ext.mutedInk)
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(WidgetCatalog.curatedTimezones) { (tzId, cityLabel) ->
                                    StudioPillOption(
                                        label = cityLabel,
                                        selected = draft.timezoneId == tzId,
                                        onClick = { onDraftChange { it.copy(timezoneId = tzId, customTitle = cityLabel) } },
                                        tag = "tz_$cityLabel"
                                    )
                                }
                            }
                        }

                        WidgetCustomFieldType.COUNTDOWN_EVENT -> {
                            Spacer(Modifier.height(12.dp))
                            StudioTextField(
                                label = "Event Title",
                                value = draft.customTitle,
                                onValueChange = { v -> onDraftChange { it.copy(customTitle = v) } },
                                tag = "input_event_title"
                            )
                            Spacer(Modifier.height(10.dp))
                            val daysFromNow = ((draft.targetEpochMillis - System.currentTimeMillis()) / 86_400_000L).toInt().coerceAtLeast(1)
                            StudioStepperRow(
                                label = "DAYS FROM TODAY",
                                valueLabel = "$daysFromNow DAYS",
                                onDecrement = {
                                    val next = (daysFromNow - 1).coerceAtLeast(1)
                                    onDraftChange { it.copy(targetEpochMillis = System.currentTimeMillis() + next * 86_400_000L) }
                                },
                                onIncrement = {
                                    val next = (daysFromNow + 1).coerceAtMost(3650)
                                    onDraftChange { it.copy(targetEpochMillis = System.currentTimeMillis() + next * 86_400_000L) }
                                }
                            )
                        }

                        WidgetCustomFieldType.NOTE_TEXT -> {
                            Spacer(Modifier.height(12.dp))
                            StudioTextField(
                                label = "Header Label",
                                value = draft.customTitle,
                                onValueChange = { v -> onDraftChange { it.copy(customTitle = v) } },
                                tag = "input_note_title"
                            )
                            Spacer(Modifier.height(10.dp))
                            StudioTextField(
                                label = "Memo Text",
                                value = draft.customNote,
                                onValueChange = { v -> onDraftChange { it.copy(customNote = v) } },
                                tag = "input_note_body"
                            )
                        }

                        WidgetCustomFieldType.CHECKLIST_ITEMS -> {
                            Spacer(Modifier.height(12.dp))
                            StudioTextField(
                                label = "List Heading",
                                value = draft.customTitle,
                                onValueChange = { v -> onDraftChange { it.copy(customTitle = v) } },
                                tag = "input_checklist_title"
                            )
                            Spacer(Modifier.height(10.dp))
                            StudioTextField(
                                label = "3 Tasks (separate with |)",
                                value = draft.customNote,
                                onValueChange = { v -> onDraftChange { it.copy(customNote = v) } },
                                tag = "input_checklist_items"
                            )
                        }

                        WidgetCustomFieldType.POMODORO_MINUTES -> {
                            Spacer(Modifier.height(12.dp))
                            Text("FOCUS SESSION LENGTH", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = ext.mutedInk)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(15, 25, 45, 60).forEach { mins ->
                                    StudioPillOption(
                                        label = "${mins}M",
                                        selected = draft.goalValue == mins,
                                        onClick = {
                                            onDraftChange {
                                                it.copy(goalValue = mins, timerDurationSec = mins * 60, isTimerRunning = false)
                                            }
                                        },
                                        tag = "pomo_$mins",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        WidgetCustomFieldType.HABIT_GOAL,
                        WidgetCustomFieldType.TALLY_LABEL,
                        WidgetCustomFieldType.FOCUS_WORD,
                        WidgetCustomFieldType.READING_BOOK,
                        WidgetCustomFieldType.SLEEP_TARGET -> {
                            Spacer(Modifier.height(12.dp))
                            StudioTextField(
                                label = when (spec.customFieldType) {
                                    WidgetCustomFieldType.SLEEP_TARGET -> "Target Bedtime (e.g. 23:00)"
                                    WidgetCustomFieldType.FOCUS_WORD -> "Daily Focus Word"
                                    WidgetCustomFieldType.READING_BOOK -> "Book Title"
                                    else -> "Custom Label"
                                },
                                value = draft.customTitle,
                                onValueChange = { v -> onDraftChange { it.copy(customTitle = v) } },
                                tag = "input_custom_title"
                            )
                            if (spec.customFieldType != WidgetCustomFieldType.FOCUS_WORD) {
                                Spacer(Modifier.height(10.dp))
                                val step = if (spec.customFieldType == WidgetCustomFieldType.READING_BOOK) 20 else 1
                                val minV = if (spec.customFieldType == WidgetCustomFieldType.READING_BOOK) 50 else 1
                                val maxV = if (spec.customFieldType == WidgetCustomFieldType.READING_BOOK) 1200 else 28
                                StudioStepperRow(
                                    label = when (spec.customFieldType) {
                                        WidgetCustomFieldType.SLEEP_TARGET -> "SLEEP CYCLES"
                                        WidgetCustomFieldType.READING_BOOK -> "TOTAL PAGES"
                                        else -> "DAILY GOAL"
                                    },
                                    valueLabel = "${draft.goalValue}",
                                    onDecrement = { onDraftChange { it.copy(goalValue = (it.goalValue - step).coerceAtLeast(minV)) } },
                                    onIncrement = { onDraftChange { it.copy(goalValue = (it.goalValue + step).coerceAtMost(maxV)) } }
                                )
                            }
                        }

                        WidgetCustomFieldType.NONE -> Unit
                    }
                }
            }
        }

        // Bottom Sticky Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ext.cardSurface)
                .border(1.dp, ext.hairline)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onSave,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("studio_save_btn")
            ) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("SAVE", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
            }
            Button(
                onClick = onPinToHome,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ext.ink, contentColor = ext.canvas),
                modifier = Modifier
                    .weight(1.35f)
                    .height(50.dp)
                    .testTag("studio_pin_btn")
            ) {
                Icon(Icons.Default.PushPin, contentDescription = null, tint = ext.redAccent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("PIN TO HOME", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun StudioSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    val ext = LocalDotGridColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(ext.cardSurface)
            .border(1.dp, ext.hairline, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(ext.redAccent))
            Spacer(Modifier.width(7.dp))
            Text(
                text = title,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = ext.mutedInk
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun StudioPillOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    val ext = LocalDotGridColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) ext.ink else ext.pillSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(Locale.US),
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = if (selected) ext.canvas else ext.ink,
            maxLines = 1
        )
    }
}

@Composable
private fun StudioSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val ext = LocalDotGridColors.current
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = ext.mutedInk)
            Text(valueText, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = ext.redAccent)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = ext.redAccent,
                activeTrackColor = ext.ink,
                inactiveTrackColor = ext.dotMatrix
            )
        )
    }
}

@Composable
private fun StudioSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    val ext = LocalDotGridColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = ext.ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = ext.mutedInk)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ext.canvas,
                checkedTrackColor = ext.ink,
                uncheckedThumbColor = ext.mutedInk,
                uncheckedTrackColor = ext.pillSurface
            ),
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun StudioTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    tag: String
) {
    val ext = LocalDotGridColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ext.ink,
            unfocusedBorderColor = ext.hairline,
            focusedTextColor = ext.ink,
            unfocusedTextColor = ext.ink,
            cursorColor = ext.redAccent
        ),
        modifier = Modifier.fillMaxWidth().testTag(tag)
    )
}

@Composable
private fun StudioStepperRow(
    label: String,
    valueLabel: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    val ext = LocalDotGridColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = ext.mutedInk)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onDecrement,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ext.pillSurface)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = ext.ink, modifier = Modifier.size(18.dp))
            }
            Text(valueLabel, fontFamily = JetBrainsMonoFontFamily, fontSize = 13.sp, color = ext.ink)
            IconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(ext.pillSurface)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = ext.ink, modifier = Modifier.size(18.dp))
            }
        }
    }
}
