package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.model.WidgetCategory
import com.example.model.WidgetGridSize
import com.example.model.WidgetInteractiveAction
import com.example.model.WidgetSpec
import com.example.model.WidgetThemeMode
import com.example.ui.theme.AppSurfaceMode
import com.example.ui.theme.DotMatrixFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalDotGridColors
import com.example.ui.viewmodel.ExploreUiState
import com.example.ui.widgets.DotGridWidgetCard
import java.util.Locale

@Composable
fun ExploreScreen(
    exploreState: ExploreUiState,
    snapshot: LiveSnapshot,
    pinnedCount: Int,
    currentSurfaceMode: AppSurfaceMode,
    onCycleSurfaceMode: () -> Unit,
    onOpenCategory: (WidgetCategory?) -> Unit,
    onCloseGallery: () -> Unit,
    onCategorySelected: (WidgetCategory?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSyncWidgetsClick: () -> Unit,
    onWidgetSelected: (WidgetSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    if (exploreState.showingAllGallery) {
        BackHandler(onBack = onCloseGallery)
        WidgetGalleryScreen(
            exploreState = exploreState,
            snapshot = snapshot,
            currentSurfaceMode = currentSurfaceMode,
            onBack = onCloseGallery,
            onCycleSurfaceMode = onCycleSurfaceMode,
            onCategorySelected = onCategorySelected,
            onSearchQueryChange = onSearchQueryChange,
            onWidgetSelected = onWidgetSelected,
            modifier = modifier
        )
    } else {
        HomeBentoHubScreen(
            snapshot = snapshot,
            pinnedCount = pinnedCount,
            currentSurfaceMode = currentSurfaceMode,
            onCycleSurfaceMode = onCycleSurfaceMode,
            onOpenCategory = onOpenCategory,
            onSyncWidgetsClick = onSyncWidgetsClick,
            modifier = modifier
        )
    }
}

@Composable
private fun HomeBentoHubScreen(
    snapshot: LiveSnapshot,
    pinnedCount: Int,
    currentSurfaceMode: AppSurfaceMode,
    onCycleSurfaceMode: () -> Unit,
    onOpenCategory: (WidgetCategory?) -> Unit,
    onSyncWidgetsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDotGridColors.current
    val categoryPairs = remember { WidgetCategory.entries.chunked(2) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ext.canvas)
            .cosmicDotBackdrop(ext.dotMatrix, ext.isLightMode),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 126.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Minimalist HOME Top Header + Single-Tap Theme Mode Pill (exact screenshot layout)
        item(key = "home_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HOME",
                    fontFamily = DotMatrixFontFamily,
                    fontSize = 32.sp,
                    letterSpacing = 2.sp,
                    color = ext.ink
                )

                val (modeLabel, modeIcon) = when (currentSurfaceMode) {
                    AppSurfaceMode.LIGHT -> "LIGHT" to Icons.Default.LightMode
                    AppSurfaceMode.DARK -> "DARK" to Icons.Default.DarkMode
                    AppSurfaceMode.OLED_NIGHT -> "OLED" to Icons.Default.NightsStay
                    AppSurfaceMode.SYSTEM -> "AUTO" to Icons.Default.DarkMode
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(ext.cardSurface)
                        .border(1.dp, ext.hairline, RoundedCornerShape(50))
                        .clickable(onClick = onCycleSurfaceMode)
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                        .testTag("home_theme_pill"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ext.redAccent)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = modeLabel,
                        fontFamily = DotMatrixFontFamily,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = ext.redAccent
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = modeIcon,
                        contentDescription = "Switch theme",
                        tint = ext.redAccent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 2. Hero SYSTEM_READOUT Card (056 ACTIVE_WIDGETS)
        item(key = "system_readout") {
            SystemReadoutHeroCard(
                totalWidgets = WidgetCatalog.allWidgets.size,
                pinnedCount = pinnedCount,
                ultraEcoMode = snapshot.settings.ultraEcoMode,
                onExploreAllClick = { onOpenCategory(null) }
            )
        }

        // 3. Signature Red Action Bar
        item(key = "eco_banner") {
            RedEcoActionBanner(onSyncClick = onSyncWidgetsClick)
        }

        // 4. Spotlight Row: TRY THE LATEST WIDGETS + NEW Starburst Card
        item(key = "spotlight_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LatestWidgetsLaserCard(
                    onClick = { onOpenCategory(WidgetCategory.NIGHTING) },
                    modifier = Modifier.weight(1f)
                )
                NewStarburstBadgeCard(
                    onClick = { onOpenCategory(WidgetCategory.CONTROLS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5. All 7 Tactile Category Squircle Cards (2-column grid with centered label below each card)
        items(categoryPairs, key = { pair -> pair.joinToString("_") { it.name } }) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                pair.forEach { cat ->
                    val count = remember(cat) { WidgetCatalog.allWidgets.count { it.category == cat } }
                    val isNewCat = cat == WidgetCategory.CONTROLS || cat == WidgetCategory.STUDIO || cat == WidgetCategory.NIGHTING
                    CategoryHubCard(
                        category = cat,
                        count = count,
                        showNewTag = isNewCat,
                        onClick = { onOpenCategory(cat) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Aspect-ratio-true Widget Gallery (4×2 wide widgets display full-width 2:1,
 * 2×2 widgets pair cleanly in 2-column 1:1 squares, 4×4 widgets display full-width 1:1)
 * rendered off-main-thread by the unified HomeScreenBitmapRenderer -> zero lag & 100% Home Screen parity!
 */
@Composable
private fun WidgetGalleryScreen(
    exploreState: ExploreUiState,
    snapshot: LiveSnapshot,
    currentSurfaceMode: AppSurfaceMode,
    onBack: () -> Unit,
    onCycleSurfaceMode: () -> Unit,
    onCategorySelected: (WidgetCategory?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onWidgetSelected: (WidgetSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalDotGridColors.current
    val previewTheme = when (currentSurfaceMode) {
        AppSurfaceMode.LIGHT -> WidgetThemeMode.LIGHT
        AppSurfaceMode.DARK -> WidgetThemeMode.DARK
        AppSurfaceMode.OLED_NIGHT -> WidgetThemeMode.OLED_NIGHT
        AppSurfaceMode.SYSTEM -> WidgetThemeMode.SYSTEM
    }

    val filteredWidgets = remember(exploreState.selectedCategory, exploreState.searchQuery) {
        val q = exploreState.searchQuery.trim().lowercase(Locale.US)
        WidgetCatalog.allWidgets.filter { spec ->
            val catMatch = exploreState.selectedCategory == null || spec.category == exploreState.selectedCategory
            val textMatch = q.isEmpty() ||
                spec.name.lowercase(Locale.US).contains(q) ||
                spec.subtitle.lowercase(Locale.US).contains(q) ||
                spec.category.displayName.lowercase(Locale.US).contains(q)
            catMatch && textMatch
        }
    }

    // Group widgets by shape so 4×2 wide cards NEVER get squashed into 1:1 squares!
    val layoutRows = remember(filteredWidgets) {
        val rows = mutableListOf<List<WidgetSpec>>()
        var pendingSquare: WidgetSpec? = null
        for (spec in filteredWidgets) {
            if (spec.defaultSize == WidgetGridSize.SMALL_2X2) {
                if (pendingSquare == null) {
                    pendingSquare = spec
                } else {
                    rows.add(listOf(pendingSquare, spec))
                    pendingSquare = null
                }
            } else {
                if (pendingSquare != null) {
                    rows.add(listOf(pendingSquare))
                    pendingSquare = null
                }
                rows.add(listOf(spec))
            }
        }
        if (pendingSquare != null) rows.add(listOf(pendingSquare))
        rows
    }

    val titleText = exploreState.selectedCategory?.displayName?.uppercase(Locale.US) ?: "ALL 56 WIDGETS"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ext.canvas)
            .cosmicDotBackdrop(ext.dotMatrix, ext.isLightMode),
        contentPadding = PaddingValues(top = 10.dp, bottom = 126.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "gallery_header") {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("gallery_back_btn")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = ext.ink
                            )
                        }
                        Column {
                            Text(
                                text = titleText,
                                fontFamily = DotMatrixFontFamily,
                                fontSize = 22.sp,
                                color = ext.ink,
                                maxLines = 1
                            )
                            Text(
                                text = "${filteredWidgets.size} WIDGETS · TRUE HOME SCREEN SHAPE",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                color = ext.mutedInk
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(ext.cardSurface)
                            .border(1.dp, ext.hairline, RoundedCornerShape(50))
                            .clickable(onClick = onCycleSurfaceMode)
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("gallery_theme_toggle")
                    ) {
                        Text(
                            text = previewTheme.label.uppercase(Locale.US),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            color = ext.redAccent
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = exploreState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "Search wi-fi, buds, photo, quote, clock...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ext.mutedInk
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.cd_search),
                            tint = ext.mutedInk
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = exploreState.searchQuery.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.testTag("clear_search_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.cd_clear_search),
                                    tint = ext.ink
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ext.ink,
                        unfocusedBorderColor = ext.hairline,
                        focusedContainerColor = ext.cardSurface,
                        unfocusedContainerColor = ext.cardSurface,
                        focusedTextColor = ext.ink,
                        unfocusedTextColor = ext.ink,
                        cursorColor = ext.redAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explore_search_input")
                )
            }
        }

        // Category filter pills
        item(key = "gallery_cats") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "cat_all") {
                    CategoryFilterChip(
                        label = "ALL ${WidgetCatalog.allWidgets.size}",
                        selected = exploreState.selectedCategory == null,
                        onClick = { onCategorySelected(null) },
                        tag = "cat_all"
                    )
                }
                items(WidgetCategory.entries, key = { it.name }) { cat ->
                    CategoryFilterChip(
                        label = cat.displayName.uppercase(Locale.US),
                        selected = exploreState.selectedCategory == cat,
                        onClick = { onCategorySelected(cat) },
                        tag = "cat_${cat.name.lowercase()}"
                    )
                }
            }
        }

        if (layoutRows.isEmpty()) {
            item(key = "empty_gallery") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("NO MATCHING WIDGETS", fontFamily = DotMatrixFontFamily, fontSize = 20.sp, color = ext.ink)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Try searching for wi-fi, buds, photo, quote, moon, or clock.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ext.mutedInk
                    )
                }
            }
        } else {
            items(layoutRows, key = { row -> row.joinToString("_") { it.typeId } }) { rowSpecs ->
                if (rowSpecs.size == 1 && rowSpecs.first().defaultSize != WidgetGridSize.SMALL_2X2) {
                    val spec = rowSpecs.first()
                    val cfg = remember(spec.typeId, previewTheme, snapshot.settings.use24Hour) {
                        WidgetCatalog.createDefaultConfig(spec, previewTheme, snapshot.settings.use24Hour)
                    }
                    val cardTheme = if (spec.category == WidgetCategory.NIGHTING && currentSurfaceMode == AppSurfaceMode.OLED_NIGHT) {
                        WidgetThemeMode.OLED_NIGHT
                    } else previewTheme
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DotGridWidgetCard(
                            config = cfg,
                            snapshot = snapshot,
                            overrideTheme = cardTheme,
                            compactPreview = true,
                            onClick = { onWidgetSelected(spec) },
                            modifier = Modifier
                                .fillMaxWidth(if (spec.defaultSize == WidgetGridSize.LARGE_4X4) 0.86f else 1f)
                                .testTag("widget_card_${spec.typeId}")
                        )
                        Spacer(Modifier.height(6.dp))
                        WidgetSpecCaptionRow(spec = spec, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp))
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowSpecs.forEach { spec ->
                            val cfg = remember(spec.typeId, previewTheme, snapshot.settings.use24Hour) {
                                WidgetCatalog.createDefaultConfig(spec, previewTheme, snapshot.settings.use24Hour)
                            }
                            val cardTheme = if (spec.category == WidgetCategory.NIGHTING && currentSurfaceMode == AppSurfaceMode.OLED_NIGHT) {
                                WidgetThemeMode.OLED_NIGHT
                            } else previewTheme
                            Column(modifier = Modifier.weight(1f)) {
                                DotGridWidgetCard(
                                    config = cfg,
                                    snapshot = snapshot,
                                    overrideTheme = cardTheme,
                                    compactPreview = true,
                                    onClick = { onWidgetSelected(spec) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("widget_card_${spec.typeId}")
                                )
                                Spacer(Modifier.height(6.dp))
                                WidgetSpecCaptionRow(spec = spec, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp))
                            }
                        }
                        if (rowSpecs.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetSpecCaptionRow(spec: WidgetSpec, modifier: Modifier = Modifier) {
    val ext = LocalDotGridColors.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = spec.name,
            style = MaterialTheme.typography.bodySmall,
            color = ext.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (spec.interactiveAction != WidgetInteractiveAction.NONE) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = "Interactive widget",
                tint = ext.redAccent,
                modifier = Modifier.size(12.dp).padding(end = 2.dp)
            )
        }
        Text(
            text = "${spec.defaultSize.label} · #${String.format(Locale.US, "%02d", spec.number)}",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 9.sp,
            color = ext.mutedInk
        )
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    val ext = LocalDotGridColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) ext.ink else ext.cardSurface)
            .border(1.dp, if (selected) ext.ink else ext.hairline, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ext.redAccent))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                letterSpacing = 0.7.sp,
                color = if (selected) ext.canvas else ext.ink
            )
        }
    }
}
