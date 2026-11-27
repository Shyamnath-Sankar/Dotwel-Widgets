package com.dotwelwidgets.dotwellabs

import android.graphics.Bitmap
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pixel fixture for the red battery fills and the pink iOS device disc. This is
 * a Robolectric render, not launcher placement or device runtime evidence.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BatterySurfaceRenderTest {
    private val red = 0xFFD71921.toInt()
    private val pink = 0xFFF8D7DA.toInt()

    @Test
    fun circleMiniAndBattery2FillsAreReferenceRedAtNormalLevel() {
        listOf(
            BatterySurfaceKind.CIRCLE_PROGRESS to "battery-circle-red",
            BatterySurfaceKind.SMALL_PROGRESSBAR to "battery-mini-red",
            BatterySurfaceKind.GLOW_PROGRESS to "battery-battery2-red",
        ).forEach { (kind, name) ->
            val bitmap = ReferenceBatterySurface.render(
                RuntimeEnvironment.getApplication(),
                kind,
                400,
                260,
                75,
                false,
                WidgetStyle.BLACK,
            )
            try {
                save(bitmap, name)
                assertTrue("$kind paints the reference red", bitmap.contains(red))
            } finally {
                bitmap.recycle()
            }
        }
    }

    @Test
    fun miniProgressBarShowsRedFillOnALightGrayRemainderTrack() {
        val bitmap = ReferenceBatterySurface.render(
            RuntimeEnvironment.getApplication(),
            BatterySurfaceKind.SMALL_PROGRESSBAR,
            400,
            260,
            75,
            false,
            WidgetStyle.BLACK,
        )
        try {
            save(bitmap, "battery-mini-light-track")
            assertTrue("red fill", bitmap.contains(red))
            assertEquals(
                "light-gray remainder track",
                ReferenceBatterySurface.TRACK_SOFT,
                bitmap.getPixel(360, 130),
            )
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun iosDeviceIconPaintsPinkDiscPhoneGlyphAndGrayStatus() {
        val bitmap = ReferenceBatterySurface.render(
            RuntimeEnvironment.getApplication(),
            BatterySurfaceKind.WITH_DEVICE_ICON,
            290,
            290,
            75,
            false,
            WidgetStyle.BLACK,
        )
        try {
            save(bitmap, "battery-ios-device-icon")
            assertTrue("pink disc", bitmap.contains(pink))
            assertTrue("black phone glyph", bitmap.contains(0xFF000000.toInt()))
            assertTrue("gray status word", bitmap.contains(ReferenceBatterySurface.GRAY))
            assertTrue("black dot percentage", bitmap.contains(0xFF000000.toInt()))
        } finally {
            bitmap.recycle()
        }
    }

    private fun save(bitmap: Bitmap, name: String) {
        val output = File("build/fixture-pixels").apply { mkdirs() }
        File(output, "$name.png").outputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }

    private fun Bitmap.contains(color: Int): Boolean {
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (getPixel(x, y) == color) return true
            }
        }
        return false
    }
}
