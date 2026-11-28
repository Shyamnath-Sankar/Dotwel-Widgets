package com.example.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.LiveSnapshot
import com.example.model.WidgetCatalog
import com.example.model.WidgetConfig
import com.example.model.WidgetGridSize
import com.example.model.WidgetInteractiveAction
import com.example.ui.theme.DotMatrixFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalDotGridColors
import com.example.ui.widgets.DotGridWidgetCard
import java.util.Locale

@Composable
fun MyWidgetsScreen(
    savedWidgets: List<WidgetConfig>,
    snapshot: LiveSnapshot,
    onInteract: (WidgetConfig) -> Unit,
    onEdit: (WidgetConfig) -> Unit,
    onPin: (WidgetConfig) -> Unit,
    onDuplicate: (WidgetConfig) -> Unit,
    onDelete: (WidgetConfig) -> Unit,
    onBrowseCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDotGridColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ext.canvas)
            .cosmicDotBackdrop(ext.dotMatrix, ext.isLightMode)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MY WIDGETS",
                        fontFamily = DotMatrixFontFamily,
                        fontSize = 24.sp,
                        color = ext.ink
                    )
                    Spacer(Modifier.width(7.dp))
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(ext.redAccent))
                }
                Text(
                    text = "${savedWidgets.size} SAVED · ${savedWidgets.count { it.appWidgetId > 0 }} PINNED TO HOME",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = ext.mutedInk
                )
            }
        }

        if (savedWidgets.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NO SAVED WIDGETS YET",
                    fontFamily = DotMatrixFontFamily,
                    fontSize = 20.sp,
                    color = ext.ink
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Customize any of our 56 Nothing-inspired widgets and save or pin them here for quick access.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ext.mutedInk
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onBrowseCatalog,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ext.ink, contentColor = ext.canvas),
                    modifier = Modifier.testTag("empty_browse_btn")
                ) {
                    Text("EXPLORE 56 WIDGETS", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 126.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(savedWidgets, key = { it.id }) { config ->
                    val spec = WidgetCatalog.getSpec(config.widgetTypeId)
                    val isPinned = config.appWidgetId > 0
                    val isInteractive = spec.interactiveAction != WidgetInteractiveAction.NONE

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(ext.cardSurface)
                            .border(1.dp, ext.hairline, RoundedCornerShape(24.dp))
                            .padding(14.dp)
                            .testTag("saved_widget_${config.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = config.customName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ext.ink,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${config.themeMode.label.uppercase(Locale.US)} · ${config.gridSize.label}" +
                                        if (isInteractive) " · TAP WIDGET TO INTERACT" else "",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    color = ext.mutedInk
                                )
                            }
                            if (isPinned) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(ext.redAccent.copy(alpha = 0.14f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ext.redAccent))
                                    Spacer(Modifier.width(5.dp))
                                    Text("PINNED", fontFamily = JetBrainsMonoFontFamily, fontSize = 9.sp, color = ext.redAccent)
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            val wFrac = when (config.gridSize) {
                                WidgetGridSize.SMALL_2X2 -> 0.54f
                                WidgetGridSize.WIDE_4X2 -> 1f
                                WidgetGridSize.LARGE_4X4 -> 0.80f
                            }
                            DotGridWidgetCard(
                                config = config,
                                snapshot = snapshot,
                                compactPreview = false,
                                onClick = {
                                    if (isInteractive) onInteract(config) else onEdit(config)
                                },
                                modifier = Modifier.fillMaxWidth(wFrac)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(ext.pillSurface)
                                        .clickable { onEdit(config) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("edit_saved_${config.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.cd_edit_widget), tint = ext.ink, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("CUSTOMIZE", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = ext.ink)
                                }
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(ext.ink)
                                        .clickable { onPin(config) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("pin_saved_${config.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PushPin, contentDescription = stringResource(R.string.cd_pin_widget), tint = ext.redAccent, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("PIN", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp, color = ext.canvas)
                                }
                            }
                            Row {
                                IconButton(onClick = { onDuplicate(config) }, modifier = Modifier.testTag("dup_saved_${config.id}")) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.cd_duplicate_widget), tint = ext.mutedInk, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { onDelete(config) }, modifier = Modifier.testTag("del_saved_${config.id}")) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.cd_delete_widget), tint = ext.redAccent, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
