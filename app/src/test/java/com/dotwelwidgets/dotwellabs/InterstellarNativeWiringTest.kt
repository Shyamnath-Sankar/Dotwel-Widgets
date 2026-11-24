package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class InterstellarNativeWiringTest {
    private fun parse(path: String): Element =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(path)).documentElement

    private fun asset(name: String): String =
        File("src/main/assets/kwgt/$name").readText()

    @Test
    fun sanitizedAssetsKeepTheFullSourceModuleTrees() {
        val expected = mapOf(
            "interstellar_endurance.sanitized.json" to Triple(206, 6, 49),
            "interstellar_endurance_analog.sanitized.json" to Triple(208, 6, 53),
            "interstellar_endurance_analog_2.sanitized.json" to Triple(208, 6, 51),
        )
        expected.forEach { (name, counts) ->
            val text = asset(name)
            assertEquals(name, counts.first, Regex("\"internal_type\":\"ShapeModule\"").findAll(text).count())
            assertEquals(name, counts.second, Regex("\"internal_type\":\"ProgressModule\"").findAll(text).count())
            assertEquals(name, counts.third, Regex("\"internal_type\":\"OverlapLayerModule\"").findAll(text).count())
            assertTrue("$name must keep toggle/raw source data", text.contains("\"internal_toggles\""))
            assertTrue("$name must keep raw rotation values", text.contains("\"config_rotate_mode\""))
        }
    }

    @Test
    fun secondsVariantsUseApi31HandSecondAndPreserveZOrder() {
        val endurance = parse("src/main/res/layout-v31/widget_interstellar_endurance_black.xml")
        assertEquals(1, endurance.getElementsByTagName("AnalogClock").length)
        assertTrue((endurance.getElementsByTagName("AnalogClock").item(0) as Element)
            .getAttribute("android:hand_second")
            .contains("interstellar_endurance_seconds_black"))

        val analog = parse("src/main/res/layout-v31/widget_interstellar_analog_black.xml")
        val clocks = analog.getElementsByTagName("AnalogClock")
        assertEquals(2, clocks.length)
        val seconds = clocks.item(0) as Element
        val hands = clocks.item(1) as Element
        assertTrue(seconds.getAttribute("android:hand_second").contains("interstellar_analog_seconds_black"))
        assertTrue(hands.getAttribute("android:hand_hour").contains("interstellar_analog_hour_black"))
        assertTrue(hands.getAttribute("android:hand_minute").contains("interstellar_analog_minute_black"))
        assertTrue(seconds.getAttribute("android:id") != hands.getAttribute("android:id"))
    }

    @Test
    fun staticThirdVariantWorksWithoutSecondsGate() {
        val dial = parse("src/main/res/layout/widget_interstellar_analog2_black.xml")
        val clock = dial.getElementsByTagName("AnalogClock").item(0) as Element
        assertTrue(clock.getAttribute("android:dial").contains("interstellar_analog2_dial_black"))
        assertTrue(clock.getAttribute("android:hand_hour").contains("interstellar_analog2_hour_black"))
        assertTrue(clock.getAttribute("android:hand_minute").contains("interstellar_analog2_minute_black"))
        assertEquals("", clock.getAttribute("android:hand_second"))
        assertFalse(File("src/main/res/layout-v31/widget_interstellar_analog2_black.xml").exists())
    }

    @Test
    fun providerUsesNativeLayoutsInsteadOfBitmapFallback() {
        val source = File("src/main/java/com/dotwelwidgets/dotwellabs/InterstellarWidgetProviders.kt").readText()
        assertTrue(source.contains("InterstellarShapeCompiler.layoutFor"))
        assertTrue(source.contains("R.id.widget_root"))
        assertFalse(source.contains("buildBitmapViews"))
        assertFalse(source.contains("widget_interstellar_bitmap"))
        val compiler = File("src/main/java/com/dotwelwidgets/dotwellabs/InterstellarShapeCompiler.kt").readText()
        assertTrue(compiler.contains("interstellar_analog2_dial_black"))
        assertTrue(compiler.contains("interstellar_analog_seconds_black"))
    }
}
