package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the reference multi-device Bluetooth battery card: a pure white
 * #FFFFFF surface, light #F4F3F3 rounded tracks, a red #D71921 proportional
 * fill, and black dot-matrix device name / percentage text.
 */
class BluetoothBatteryLevelsChromeTest {
    @Test
    fun cardUsesTheReferenceWhiteSurface() {
        assertEquals(
            0xFFFFFFFF.toInt(),
            BluetoothBatteryLevelsRenderer.cardColor(WidgetStyle.BLACK),
        )
        assertEquals(
            0xFFFFFFFF.toInt(),
            BluetoothBatteryLevelsRenderer.cardColor(WidgetStyle.TRANSLUCENT),
        )
        assertEquals(
            0xFFFFFFFF.toInt(),
            BluetoothBatteryLevelsRenderer.cardColor(WidgetStyle.TRANSPARENT),
        )
    }

    @Test
    fun trackAndFillUseTheReferenceColors() {
        assertEquals(0xFFF4F3F3.toInt(), BluetoothBatteryLevelsRenderer.trackColor())
        assertEquals(0xFFD71921.toInt(), BluetoothBatteryLevelsRenderer.fillColor())
    }

    @Test
    fun percentAndDeviceNameAreUppercaseReferenceText() {
        assertEquals("85%", BluetoothBatteryLevelsRenderer.percentText(85))
        assertEquals("0%", BluetoothBatteryLevelsRenderer.percentText(0))
        assertEquals("--", BluetoothBatteryLevelsRenderer.percentText(null))
        assertEquals(
            "NOTHING EAR (R)",
            BluetoothBatteryLevelsRenderer.displayName("  Nothing Ear (r) "),
        )
        assertEquals("DEVICE", BluetoothBatteryLevelsRenderer.displayName("   "))
        assertEquals("DEVICE", BluetoothBatteryLevelsRenderer.displayName(null))
    }

    @Test
    fun fillFractionIsProportionalAndClamped() {
        assertEquals(0.85f, BluetoothBatteryLevelsRenderer.fillFraction(85), 0.0001f)
        assertEquals(1f, BluetoothBatteryLevelsRenderer.fillFraction(140), 0.0001f)
        assertEquals(0f, BluetoothBatteryLevelsRenderer.fillFraction(null), 0.0001f)
        assertEquals(0f, BluetoothBatteryLevelsRenderer.fillFraction(-5), 0.0001f)
    }

    @Test
    fun snapshotKeepsPhoneRowFirst() {
        val snapshot = BluetoothBatteryLevelsSnapshot(
            BluetoothBatteryLevelsStatus.ROWS,
            listOf(
                BluetoothBatteryRow("PHONE", 85, true, BluetoothBatteryDeviceKind.PHONE),
                BluetoothBatteryRow(
                    "PIXEL BUDS",
                    60,
                    false,
                    BluetoothBatteryDeviceKind.EARBUDS,
                ),
            ),
        )
        assertEquals(BluetoothBatteryDeviceKind.PHONE, snapshot.rows.first().kind)
        assertEquals("85%", BluetoothBatteryLevelsRenderer.percentText(snapshot.rows.first().percent))
    }
}
