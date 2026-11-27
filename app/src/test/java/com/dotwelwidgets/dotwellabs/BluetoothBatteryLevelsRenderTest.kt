package com.dotwelwidgets.dotwellabs

import android.graphics.Bitmap
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Pixel-level fixture for the repaired rows. It is a Robolectric render, not
 * launcher placement or device runtime evidence.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BluetoothBatteryLevelsRenderTest {
    private val card = 0xFFFEF7FF.toInt()
    private val track = 0xFFF4F3F3.toInt()
    private val fill = 0xFFD71921.toInt()

    @Test
    fun rowsPaintTheCardTrackAndProportionalFill() {
        val context = RuntimeEnvironment.getApplication()
        val snapshot = BluetoothBatteryLevelsSnapshot(
            BluetoothBatteryLevelsStatus.ROWS,
            listOf(
                BluetoothBatteryRow(
                    "PHONE",
                    100,
                    false,
                    BluetoothBatteryDeviceKind.PHONE,
                ),
                BluetoothBatteryRow(
                    "PIXEL BUDS",
                    50,
                    false,
                    BluetoothBatteryDeviceKind.EARBUDS,
                ),
            ),
        )
        val bitmap = BluetoothBatteryLevelsRenderer.render(
            context,
            400,
            400,
            WidgetStyle.BLACK,
            snapshot,
        )
        try {
            val output = File("build/fixture-pixels").apply { mkdirs() }
            File(output, "bt-battery-levels-rows.png").outputStream().use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            val frames = BluetoothBatteryLevelsRenderer.rowFrames(400, 400, 2)
            val topRow = frames[0]
            val bottomRow = frames[1]

            assertEquals(card, bitmap.getPixel(200, (topRow.top - 10f).toInt()))
            assertEquals(
                fill,
                bitmap.getPixel(
                    (topRow.left + 8f).toInt(),
                    ((topRow.top + topRow.bottom) / 2f).toInt(),
                ),
            )
            assertEquals(
                track,
                bitmap.getPixel(
                    (bottomRow.left + bottomRow.width * 0.70f).toInt(),
                    ((bottomRow.top + bottomRow.bottom) / 2f).toInt(),
                ),
            )
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun aSingleSourceDoesNotPaintTheWholeCardRed() {
        val context = RuntimeEnvironment.getApplication()
        val snapshot = BluetoothBatteryLevelsSnapshot(
            BluetoothBatteryLevelsStatus.ROWS,
            listOf(
                BluetoothBatteryRow(
                    "PHONE",
                    100,
                    false,
                    BluetoothBatteryDeviceKind.PHONE,
                ),
            ),
        )
        val bitmap: Bitmap = BluetoothBatteryLevelsRenderer.render(
            context,
            400,
            400,
            WidgetStyle.BLACK,
            snapshot,
        )
        try {
            val row = BluetoothBatteryLevelsRenderer.rowFrames(400, 400, 1).single()
            assertEquals(card, bitmap.getPixel(200, (row.top - 10f).toInt()))
            assertEquals(card, bitmap.getPixel(200, (row.bottom + 10f).toInt()))
        } finally {
            bitmap.recycle()
        }
    }
}
