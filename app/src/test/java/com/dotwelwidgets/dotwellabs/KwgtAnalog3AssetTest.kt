package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Locks the source-only asset contract: each style layout contains exactly one
 * platform AnalogClock with a filled dial and two hollow capsule hands. There
 * is no seconds hand, text clock, font payload, bitmap time or rounded card.
 */
class KwgtAnalog3AssetTest {
    @Test
    fun styleLayoutsMapToTheSourceDialAndTwoHands() {
        val expectations = mapOf(
            "widget_kwgt_analog3_black.xml" to LayoutRefs(
                dial = "@drawable/kwgt_analog3_dial_dark",
                hour = "@drawable/kwgt_analog3_hand_hour_light",
                minute = "@drawable/kwgt_analog3_hand_minute_light",
            ),
            "widget_kwgt_analog3_translucent.xml" to LayoutRefs(
                dial = "@drawable/kwgt_analog3_dial_translucent",
                hour = "@drawable/kwgt_analog3_hand_hour_dark",
                minute = "@drawable/kwgt_analog3_hand_minute_dark",
            ),
            "widget_kwgt_analog3_transparent.xml" to LayoutRefs(
                dial = "@drawable/kwgt_analog3_dial_none",
                hour = "@drawable/kwgt_analog3_hand_hour_light",
                minute = "@drawable/kwgt_analog3_hand_minute_light",
            ),
        )
        expectations.forEach { (file, refs) ->
            val root = parse("layout", file).documentElement
            assertEquals("$file root", "FrameLayout", root.tagName)
            assertEquals("$file id", "@+id/widget_root", root.getAttribute("android:id"))
            assertFalse("$file adds no rounded card", root.hasAttribute("android:background"))
            val children = root.childElements()
            assertEquals("$file only the clock", listOf("AnalogClock"), children.map { it.tagName })
            val clock = children.single()
            assertEquals("@+id/kwgt_analog3_clock", clock.getAttribute("android:id"))
            assertEquals("$file dial", refs.dial, clock.getAttribute("android:dial"))
            assertEquals("$file hour", refs.hour, clock.getAttribute("android:hand_hour"))
            assertEquals("$file minute", refs.minute, clock.getAttribute("android:hand_minute"))
            assertFalse("$file has no second hand", clock.hasAttribute("android:hand_second"))
            assertEquals("$file width", "match_parent", clock.getAttribute("android:layout_width"))
            assertEquals("$file height", "match_parent", clock.getAttribute("android:layout_height"))
        }
    }

    @Test
    fun providerSelectsADistinctLayoutForEachDotwelStyle() {
        assertEquals(
            R.layout.widget_kwgt_analog3_black,
            KwgtAnalog3WidgetProvider.layoutForStyle(WidgetStyle.BLACK),
        )
        assertEquals(
            R.layout.widget_kwgt_analog3_translucent,
            KwgtAnalog3WidgetProvider.layoutForStyle(WidgetStyle.TRANSLUCENT),
        )
        assertEquals(
            R.layout.widget_kwgt_analog3_transparent,
            KwgtAnalog3WidgetProvider.layoutForStyle(WidgetStyle.TRANSPARENT),
        )
        val layouts = WidgetStyle.entries.map(KwgtAnalog3WidgetProvider::layoutForStyle)
        assertEquals(layouts.size, layouts.toSet().size)
    }

    @Test
    fun handVectorsKeepHollowForegroundStrokes() {
        val expectations = mapOf(
            "kwgt_analog3_hand_minute_light" to "#FFFFFFFF",
            "kwgt_analog3_hand_minute_dark" to "#FF1B1B1D",
            "kwgt_analog3_hand_hour_light" to "#FFFFFFFF",
            "kwgt_analog3_hand_hour_dark" to "#FF1B1B1D",
        )
        expectations.forEach { (name, stroke) ->
            val path = parse("drawable", "$name.xml")
                .documentElement
                .childElements()
                .single { it.tagName == "path" }
            assertEquals("$name stroke", stroke, path.getAttribute("android:strokeColor"))
            assertEquals("$name hollow", "@android:color/transparent", path.getAttribute("android:fillColor"))
            assertEquals("$name source stroke", "2", path.getAttribute("android:strokeWidth"))
        }
    }

    @Test
    fun providerInfoIsResizableAndNeedsNoPeriodicAppAlarm() {
        val root = parse("xml", "kwgt_analog3_widget_info.xml").documentElement
        assertEquals("appwidget-provider", root.tagName)
        assertEquals("0", root.getAttribute("android:updatePeriodMillis"))
        assertEquals(
            "@layout/widget_kwgt_analog3_black",
            root.getAttribute("android:initialLayout"),
        )
        assertEquals(
            "@layout/widget_kwgt_analog3_black",
            root.getAttribute("android:previewLayout"),
        )
        assertTrue(root.getAttribute("android:resizeMode").contains("horizontal"))
        assertTrue(root.getAttribute("android:resizeMode").contains("vertical"))
    }

    private data class LayoutRefs(val dial: String, val hour: String, val minute: String)

    private fun parse(directory: String, name: String): org.w3c.dom.Document =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(resourceFile(directory, name))

    private fun resourceFile(directory: String, name: String): File {
        val candidates = listOf(
            File("src/main/res/$directory/$name"),
            File("app/src/main/res/$directory/$name"),
        )
        return candidates.firstOrNull { it.isFile }
            ?: throw AssertionError(
                "cannot find $directory/$name from ${File(".").absolutePath}",
            )
    }

    private fun Element.childElements(): List<Element> {
        val elements = mutableListOf<Element>()
        val nodes = childNodes
        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            if (node is Element) elements += node
        }
        return elements
    }
}
