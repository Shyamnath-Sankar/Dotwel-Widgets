package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DotGridApplication
import com.example.R
import com.example.model.LiveSnapshot
import com.example.model.WidgetCatalog
import com.example.model.WidgetCategory
import com.example.model.WidgetConfig
import com.example.model.WidgetCustomFieldType
import com.example.model.WidgetGridSize
import com.example.model.WidgetThemeMode
import com.example.ui.theme.AppSurfaceMode
import com.example.ui.theme.DotGridTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalDotGridColors
import com.example.ui.widgets.DotGridWidgetCard
import kotlinx.coroutines.launch

class WidgetConfigureActivity : ComponentActivity() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val repo = (applicationContext as DotGridApplication).repository
        val detectedGridSize = WidgetUpdater.resolveGridSizeForWidgetId(this, appWidgetId)

        setContent {
            var surfaceMode by remember { mutableStateOf(AppSurfaceMode.OLED_NIGHT) }
            var snapshot by remember { mutableStateOf(LiveSnapshot()) }
            var draft by remember {
                mutableStateOf(
                    WidgetCatalog.createDefaultConfig(WidgetCatalog.allWidgets.first())
                        .copy(appWidgetId = appWidgetId, gridSize = detectedGridSize)
                )
            }
            var selectedCategory by remember { mutableStateOf<WidgetCategory?>(null) }
            val scope = rememberCoroutineScope()

            val photoPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) {
                    scope.launch {
                        repo.importStudioPhoto(uri).onSuccess { path ->
                            draft = draft.copy(photoPath = path)
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                val settings = repo.getSettingsOnce()
                surfaceMode = settings.appSurfaceMode
                snapshot = repo.buildCurrentLiveSnapshot()
                val existing = repo.getWidgetByAppWidgetId(appWidgetId)
                if (existing != null) {
                    draft = existing.copy(gridSize = detectedGridSize)
                }
            }

            DotGridTheme(surfaceMode = surfaceMode) {
                ConfigureScreen(
                    draft = draft,
                    snapshot = snapshot,
                    selectedCategory = selectedCategory,
                    onCategoryChange = { selectedCategory = it },
                    onDraftChange = { draft = it },
                    onPickPhoto = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onCancel = { finish() },
                    onApply = {
                        scope.launch {
                            val toSave = draft.copy(appWidgetId = appWidgetId, gridSize = detectedGridSize)
                            repo.saveWidgetConfig(toSave)
                            val manager = AppWidgetManager.getInstance(this@WidgetConfigureActivity)
                            WidgetUpdater.updateSingleWidget(
                                this@WidgetConfigureActivity,
                                manager,
                                appWidgetId,
                                toSave.gridSize
                            )
                            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            setResult(RESULT_OK, resultValue)
                            finish()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ConfigureScreen(
    draft: WidgetConfig,
    snapshot: LiveSnapshot,
    selectedCategory: WidgetCategory?,
    onCategoryChange: (WidgetCategory?) -> Unit,
    onDraftChange: (WidgetConfig) -> Unit,
    onPickPhoto: () -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    val ext = LocalDotGridColors.current
    val spec = remember(draft.widgetTypeId) { WidgetCatalog.getSpec(draft.widgetTypeId) }
    val needsPhoto = spec.customFieldType == WidgetCustomFieldType.PHOTO_PICKER ||
        spec.customFieldType == WidgetCustomFieldType.PHOTO_AND_QUOTE
    val filtered = remember(selectedCategory) {
        if (selectedCategory == null) WidgetCatalog.allWidgets
        else WidgetCatalog.allWidgets.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ext.canvas)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel, modifier = Modifier.testTag("config_cancel_btn")) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_back), tint = ext.ink)
            }
            Text("ADD WIDGET · ${draft.gridSize.label}", style = MaterialTheme.typography.titleSmall, color = ext.ink)
            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(containerColor = ext.ink, contentColor = ext.canvas),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("config_apply_btn")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("Add", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(ext.cardSurface)
                .border(1.dp, ext.hairline, RoundedCornerShape(24.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            DotGridWidgetCard(
                config = draft,
                snapshot = snapshot,
                compactPreview = false,
                modifier = Modifier.fillMaxWidth(
                    when (draft.gridSize) {
                        WidgetGridSize.WIDE_4X2 -> 0.94f
                        WidgetGridSize.LARGE_4X4 -> 0.56f
                        WidgetGridSize.SMALL_2X2 -> 0.48f
                    }
                )
            )
        }

        if (needsPhoto) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onPickPhoto,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("PICK STUDIO PHOTO (PHOTO PICKER)", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp)
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(WidgetThemeMode.entries) { mode ->
                val active = draft.themeMode == mode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (active) ext.ink else ext.pillSurface)
                        .clickable { onDraftChange(draft.copy(themeMode = mode)) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = mode.label.uppercase(),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = if (active) ext.canvas else ext.ink
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                val active = selectedCategory == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (active) ext.redAccent else ext.pillSurface)
                        .clickable { onCategoryChange(null) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("ALL 56", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = if (active) ext.canvas else ext.ink)
                }
            }
            items(WidgetCategory.entries) { cat ->
                val active = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (active) ext.redAccent else ext.pillSurface)
                        .clickable { onCategoryChange(cat) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(cat.displayName.uppercase(), fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = if (active) ext.canvas else ext.ink)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.typeId }) { itemSpec ->
                val selected = draft.widgetTypeId == itemSpec.typeId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) ext.pillSurface else ext.cardSurface)
                        .border(1.dp, if (selected) ext.redAccent else ext.hairline, RoundedCornerShape(16.dp))
                        .clickable {
                            onDraftChange(
                                WidgetCatalog.createDefaultConfig(itemSpec, draft.themeMode)
                                    .copy(
                                        id = draft.id,
                                        appWidgetId = draft.appWidgetId,
                                        gridSize = draft.gridSize,
                                        photoPath = draft.photoPath
                                    )
                            )
                        }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(itemSpec.name, style = MaterialTheme.typography.titleMedium, color = ext.ink)
                        Text(itemSpec.subtitle, style = MaterialTheme.typography.bodySmall, color = ext.mutedInk, maxLines = 1)
                    }
                    Text("#${itemSpec.number}", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, color = if (selected) ext.redAccent else ext.mutedInk)
                }
            }
        }
    }
}
