package com.dotwelwidgets.dotwellabs

import android.graphics.Bitmap
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pixel fixture for the gallery-only headphone state and the real no-access
 * state. This is a Robolectric render, not launcher placement or device runtime
 * evidence.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HeadphonesFixtureRenderTest {
    private val card = 0xFFFEF7FF.toInt()
    private val ink = 0xFF1D1B20.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val green = 0xFF6A9C89.toInt()
    private val red = 0xFFD71921.toInt()
    private val gray = 0xFF777379.toInt()
    private val halo = 0xFFE9E7EC.toInt()

    @Test
    fun wideAndSquareFixtureCardsPaintEveryReferenceLine() {
        renderAndCheck(
            name = "headphones-wide-fixture",
            width = 110,
            height = 40,
            square = false,
            snapshot = BluetoothHeadphonesRenderer.previewSnapshot(),
            percentColor = green,
        )
        renderAndCheck(
            name = "headphones-square-fixture",
            width = 110,
            height = 110,
            square = true,
            snapshot = BluetoothHeadphonesRenderer.previewSnapshot(),
            percentColor = green,
        )
    }

    @Test
    fun earbudsAndStatusStayReferenceBlackInEveryStyle() {
        assertEquals(ink, BluetoothHeadphonesRenderer.GLYPH_INK)
        for (style in WidgetStyle.values()) {
            for (square in listOf(false, true)) {
                val width = 110
                val height = if (square) 110 else 40
                val name = "headphones-${if (square) "square" else "wide"}" +
                    "-${style.name.lowercase()}-black-ink"
                val bitmap = BluetoothHeadphonesRenderer.render(
                    RuntimeEnvironment.getApplication(),
                    width,
                    height,
                    style,
                    BluetoothHeadphonesRenderer.previewSnapshot(),
                    square,
                )
                try {
                    save(bitmap, name)
                    val layout = BluetoothHeadphonesRenderer.layout(
                        width.toFloat(),
                        height.toFloat(),
                        square,
                    )
                    assertTrue(
                        "$name earbuds solid #1D1B20",
                        bitmap.containsOpaqueExactIn(layout.icon, ink),
                    )
                    assertFalse(
                        "$name earbuds are not white",
                        bitmap.containsOpaqueExactIn(layout.icon, white),
                    )
                    assertTrue(
                        "$name status black dot ink",
                        bitmap.containsIn(layout.status, ink),
                    )
                    assertFalse(
                        "$name status is not white",
                        bitmap.containsOpaqueExactIn(layout.status, white),
                    )
                    assertTrue(
                        "$name red earbud dot",
                        bitmap.containsIn(layout.icon, red),
                    )
                    assertTrue(
                        "$name light-gray halo behind the earbuds",
                        bitmap.containsOpaqueExactIn(layout.icon, halo),
                    )
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }

    @Test
    fun noAccessCardsKeepTheSameLayoutWithoutClipping() {
        val noAccess = BluetoothHeadphonesSnapshot(HeadphonesState.PERMISSION_NEEDED)
        renderAndCheck(
            name = "headphones-wide-no-access",
            width = 110,
            height = 40,
            square = false,
            snapshot = noAccess,
            percentColor = gray,
        )
        renderAndCheck(
            name = "headphones-square-no-access",
            width = 110,
            height = 110,
            square = true,
            snapshot = noAccess,
            percentColor = gray,
        )
    }

    private fun renderAndCheck(
        name: String,
        width: Int,
        height: Int,
        square: Boolean,
        snapshot: BluetoothHeadphonesSnapshot,
        percentColor: Int,
    ) {
        val bitmap = BluetoothHeadphonesRenderer.render(
            RuntimeEnvironment.getApplication(),
            width,
            height,
            WidgetStyle.BLACK,
            snapshot,
            square,
        )
        try {
            save(bitmap, name)
            val layout = BluetoothHeadphonesRenderer.layout(
                width.toFloat(),
                height.toFloat(),
                square,
            )
            assertTrue("$name card surface", bitmap.contains(card))
            assertTrue(
                "$name status ink in status box",
                bitmap.containsIn(layout.status, ink),
            )
            assertTrue(
                "$name percent fill",
                bitmap.containsIn(layout.percent, percentColor),
            )
            assertTrue("$name red earbud dot", bitmap.containsIn(layout.icon, red))
            assertTrue(
                "$name light-gray halo behind the earbuds",
                bitmap.containsOpaqueExactIn(layout.icon, halo),
            )
            assertTrue("$name gray device name", bitmap.containsIn(layout.name, gray))
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

    private fun Bitmap.containsIn(
        box: BluetoothHeadphonesRenderer.TextBox,
        color: Int,
    ): Boolean {
        val left = box.left.toInt().coerceIn(0, width - 1)
        val top = box.top.toInt().coerceIn(0, height - 1)
        val right = box.right.toInt().coerceIn(0, width - 1)
        val bottom = box.bottom.toInt().coerceIn(0, height - 1)
        for (y in top..bottom) {
            for (x in left..right) {
                if (near(getPixel(x, y), color)) return true
            }
        }
        return false
    }

    private fun Bitmap.containsOpaqueExactIn(
        box: BluetoothHeadphonesRenderer.TextBox,
        color: Int,
    ): Boolean {
        val expected = color and 0x00FFFFFF
        return scan(box) { pixel ->
            (pixel ushr 24) == 0xFF && (pixel and 0x00FFFFFF) == expected
        }
    }

    private fun Bitmap.scan(
        box: BluetoothHeadphonesRenderer.TextBox,
        match: (Int) -> Boolean,
    ): Boolean {
        val left = box.left.toInt().coerceIn(0, width - 1)
        val top = box.top.toInt().coerceIn(0, height - 1)
        val right = box.right.toInt().coerceIn(0, width - 1)
        val bottom = box.bottom.toInt().coerceIn(0, height - 1)
        for (y in top..bottom) {
            for (x in left..right) {
                if (match(getPixel(x, y))) return true
            }
        }
        return false
    }

    private fun near(actual: Int, expected: Int): Boolean {
        val a = actual and 0x00FFFFFF
        val b = expected and 0x00FFFFFF
        val dr = kotlin.math.abs((a ushr 16 and 0xFF) - (b ushr 16 and 0xFF))
        val dg = kotlin.math.abs((a ushr 8 and 0xFF) - (b ushr 8 and 0xFF))
        val db = kotlin.math.abs((a and 0xFF) - (b and 0xFF))
        return dr <= 80 && dg <= 80 && db <= 80
    }
}
