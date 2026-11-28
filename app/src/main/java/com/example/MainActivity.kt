package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.MyWidgetsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WidgetStudioScreen
import com.example.ui.theme.AppSurfaceMode
import com.example.ui.theme.DotGridTheme
import com.example.ui.theme.DotMatrixFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalDotGridColors
import com.example.ui.viewmodel.DotGridViewModel

private object Routes {
    const val EXPLORE = "explore"
    const val MY_WIDGETS = "my_widgets"
    const val SETTINGS = "settings"
    const val STUDIO = "studio"
}

private data class NavDestinationItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: DotGridViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDarkChrome = when (settings.appSurfaceMode) {
                AppSurfaceMode.LIGHT -> false
                AppSurfaceMode.DARK, AppSurfaceMode.OLED_NIGHT -> true
                AppSurfaceMode.SYSTEM -> systemDark
            }

            LaunchedEffect(isDarkChrome) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkChrome) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDarkChrome) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    }
                )
            }

            DotGridTheme(surfaceMode = settings.appSurfaceMode) {
                Crossfade(
                    targetState = onboardingCompleted,
                    animationSpec = tween(durationMillis = 360),
                    label = "root_onboarding_gate"
                ) { completed ->
                    when (completed) {
                        null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(LocalDotGridColors.current.canvas)
                            )
                        }
                        false -> {
                            OnboardingScreen(
                                currentSurfaceMode = settings.appSurfaceMode,
                                onSurfaceModeChange = viewModel::setAppSurfaceMode,
                                onFinish = viewModel::completeOnboarding
                            )
                        }
                        true -> {
                            AnyWidgetsAppShell(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnyWidgetsAppShell(viewModel: DotGridViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.EXPLORE

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snapshot by viewModel.liveSnapshot.collectAsStateWithLifecycle()
    val exploreState by viewModel.exploreState.collectAsStateWithLifecycle()
    val savedWidgets by viewModel.savedWidgets.collectAsStateWithLifecycle()
    val studioDraft by viewModel.studioDraft.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    var showHelpSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val navItems = remember {
        listOf(
            NavDestinationItem(Routes.EXPLORE, R.string.nav_explore, Icons.Filled.Home, Icons.Outlined.Home, "nav_explore"),
            NavDestinationItem(Routes.MY_WIDGETS, R.string.nav_my_widgets, Icons.Filled.DashboardCustomize, Icons.Outlined.DashboardCustomize, "nav_my_widgets"),
            NavDestinationItem(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Bolt, Icons.Outlined.Bolt, "nav_settings")
        )
    }

    val showChromeNav = currentRoute != Routes.STUDIO
    val ext = LocalDotGridColors.current
    val pinnedCount = remember(savedWidgets) { savedWidgets.count { it.appWidgetId > 0 } }

    fun navigateTopLevel(route: String) {
        if (route == Routes.EXPLORE && currentRoute == Routes.EXPLORE && exploreState.showingAllGallery) {
            viewModel.closeCategoryGallery()
            return
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ext.canvas)) {
        val useNavRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = ext.canvas,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = if (showChromeNav && !useNavRail) 88.dp else 12.dp)
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = ext.ink,
                        contentColor = ext.canvas,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                    if (showChromeNav && useNavRail) {
                        NavigationRail(containerColor = ext.cardSurface) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = stringResource(item.labelRes),
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 9.sp
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = ext.redAccent,
                                        selectedTextColor = ext.redAccent,
                                        indicatorColor = ext.pillSurface,
                                        unselectedIconColor = ext.mutedInk,
                                        unselectedTextColor = ext.mutedInk
                                    ),
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Routes.EXPLORE,
                        enterTransition = { fadeIn(tween(180)) },
                        exitTransition = { fadeOut(tween(140)) },
                        modifier = Modifier.weight(1f).fillMaxSize()
                    ) {
                        composable(Routes.EXPLORE) {
                            ExploreScreen(
                                exploreState = exploreState,
                                snapshot = snapshot,
                                pinnedCount = pinnedCount,
                                currentSurfaceMode = settings.appSurfaceMode,
                                onCycleSurfaceMode = viewModel::cycleAppSurfaceMode,
                                onOpenCategory = viewModel::openCategoryGallery,
                                onCloseGallery = viewModel::closeCategoryGallery,
                                onCategorySelected = viewModel::selectCategory,
                                onSearchQueryChange = viewModel::updateSearchQuery,
                                onSyncWidgetsClick = viewModel::refreshAllHomeWidgetsNow,
                                onWidgetSelected = { spec ->
                                    viewModel.openStudioForCatalogWidget(spec.typeId)
                                    navController.navigate(Routes.STUDIO)
                                }
                            )
                        }

                        composable(Routes.MY_WIDGETS) {
                            MyWidgetsScreen(
                                savedWidgets = savedWidgets,
                                snapshot = snapshot,
                                onInteract = viewModel::interactWithSavedWidget,
                                onEdit = { cfg ->
                                    viewModel.openStudioForSavedWidget(cfg)
                                    navController.navigate(Routes.STUDIO)
                                },
                                onPin = viewModel::pinSavedWidgetToHomeScreen,
                                onDuplicate = viewModel::duplicateSavedWidget,
                                onDelete = viewModel::deleteSavedWidget,
                                onBrowseCatalog = {
                                    viewModel.openCategoryGallery(null)
                                    navigateTopLevel(Routes.EXPLORE)
                                }
                            )
                        }

                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                settings = settings,
                                snapshot = snapshot,
                                onSurfaceModeChange = viewModel::setAppSurfaceMode,
                                onAutoNightHoursChange = viewModel::setAutoNightHours,
                                onUse24HourChange = viewModel::setUse24Hour,
                                onRefreshIntervalChange = viewModel::setRefreshIntervalMinutes,
                                onUltraEcoChange = viewModel::setUltraEcoMode,
                                onRefreshHomeWidgets = viewModel::refreshAllHomeWidgetsNow,
                                onReplayOnboarding = viewModel::replayOnboarding
                            )
                        }

                        composable(Routes.STUDIO) {
                            val draft = studioDraft
                            if (draft != null) {
                                WidgetStudioScreen(
                                    draft = draft,
                                    snapshot = snapshot,
                                    onBack = { navController.popBackStack() },
                                    onDraftChange = viewModel::updateStudioDraft,
                                    onImportPhoto = viewModel::importPhotoForStudioDraft,
                                    onClearPhoto = viewModel::clearStudioPhoto,
                                    onReset = viewModel::resetStudioDraftToDefault,
                                    onInteractPreview = viewModel::interactWithStudioDraft,
                                    onSave = viewModel::saveStudioDraft,
                                    onPinToHome = viewModel::pinStudioDraftToHomeScreen
                                )
                            }
                        }
                    }
                }

                // Floating Pill Dock + Compact HELP Chip (matching reference screenshots!)
                if (showChromeNav && !useNavRail) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Compact floating ? HELP chip tucked right above the dock
                        Row(
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .shadow(6.dp, RoundedCornerShape(50))
                                .clip(RoundedCornerShape(50))
                                .background(ext.cardSurface.copy(alpha = 0.96f))
                                .border(1.dp, ext.hairline, RoundedCornerShape(50))
                                .clickable { showHelpSheet = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("floating_help_btn"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.HelpOutline,
                                contentDescription = "Widget placement help",
                                tint = ext.redAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "HELP",
                                fontFamily = DotMatrixFontFamily,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = ext.redAccent
                            )
                        }

                        // Translucent Floating Pill Navigation Dock
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(12.dp, RoundedCornerShape(50))
                                .clip(RoundedCornerShape(50))
                                .background(ext.cardSurface.copy(alpha = 0.94f))
                                .border(1.dp, ext.hairline, RoundedCornerShape(50))
                                .padding(5.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.route
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (selected) ext.pillSurface else androidx.compose.ui.graphics.Color.Transparent)
                                        .clickable { navigateTopLevel(item.route) }
                                        .padding(vertical = 8.dp)
                                        .testTag(item.testTag),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes),
                                        tint = if (selected) ext.redAccent else ext.ink,
                                        modifier = Modifier.size(21.dp)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(item.labelRes),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = if (selected) ext.redAccent else ext.mutedInk
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHelpSheet) {
        ModalBottomSheet(
            onDismissRequest = { showHelpSheet = false },
            containerColor = ext.cardSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(ext.redAccent))
                    Spacer(Modifier.width(8.dp))
                    Text("HOME SCREEN WIDGET GUIDE", fontFamily = DotMatrixFontFamily, fontSize = 18.sp, color = ext.ink)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "• 1-Tap Pinning: Open any widget → customize theme, shape & photo → tap PIN TO HOME.\n" +
                        "• Exact Shape & Orientation: Every 2×2, 4×2, and 4×4 widget renders with the exact same aspect ratio and look as seen in the app, and auto-adapts cleanly if you resize it on your Home Screen.\n" +
                        "• Quick Reconfigure: Tap the top-right corner of any placed widget to reopen its settings anytime.\n" +
                        "• Play-Safe Privacy: Uses Android Photo Picker and native Android Settings Panels with zero invasive permissions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ext.mutedInk
                )
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
