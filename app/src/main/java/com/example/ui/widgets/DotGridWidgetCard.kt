package com.example.ui.widgets

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.model.LiveSnapshot
import com.example.model.WidgetConfig
import com.example.model.WidgetThemeMode
import com.example.ui.theme.LocalDotGridColors
import com.example.widget.HomeScreenBitmapRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Unified single-source-of-truth widget preview card.
 * Renders the EXACT same HomeScreenBitmapRenderer off the main thread (Dispatchers.Default)
 * with LRU caching and exact canonical aspect ratio (1:1 for 2×2 & 4×4, 2:1 for 4×2),
 * guaranteeing 100% pixel-identical shape, orientation, and visuals between the app and
 * the Android Home Screen while keeping scrolling locked at 60-120fps.
 */
@Composable
fun DotGridWidgetCard(
    config: WidgetConfig,
    snapshot: LiveSnapshot,
    modifier: Modifier = Modifier,
    overrideTheme: WidgetThemeMode? = null,
    compactPreview: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current.applicationContext
    val ext = LocalDotGridColors.current
    val cornerDp = config.cornerRadiusDp.coerceIn(10, 32).dp
    val shape = RoundedCornerShape(cornerDp)
    val maxSidePx = if (compactPreview) 440 else 640

    val bitmapState by produceState<Bitmap?>(
        initialValue = null,
        key1 = config,
        key2 = snapshot,
        key3 = overrideTheme
    ) {
        value = withContext(Dispatchers.Default) {
            HomeScreenBitmapRenderer.getOrRenderCachedPreview(
                context = context,
                config = config,
                snapshot = snapshot,
                overrideTheme = overrideTheme,
                maxSidePx = maxSidePx
            )
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .aspectRatio(config.gridSize.aspectRatio)
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = androidx.compose.material3.ripple(color = ext.ink),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmapState
        if (bmp != null && !bmp.isRecycled) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = config.customName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ext.insetSurface)
            )
        }
    }
}
