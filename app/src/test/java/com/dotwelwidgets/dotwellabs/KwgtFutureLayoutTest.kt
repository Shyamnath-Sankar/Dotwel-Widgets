package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Source-contract guard for the KWGT Nothing_Future_Clock port. These checks
 * run without an Android host: the three native discs, the equal-intrinsic
 * transparent layers, the API 31 seconds gate, source gradients, labels and the
 * sanitized source transcription.
 */
class KwgtFutureLayoutTest {
    private data class StyleCase(
        val style: String,
        val bg: String,
        val fill: String,
        val labelInk: String,
    )

    private val styles = listOf(
        StyleCase("black", "dw_future_bg_black", "#FF1B1B1D", "#FFFFFFFF"),
        StyleCase("translucent", "dw_future_bg_translucent", "#A6FFFFFF", "#FF1B1B1D"),
        StyleCase("transparent", "dw_future_bg_none", "#00000000", "#FFFFFFFF"),
    )

    @Test
    fun eachStyleUsesThreeNativeClocksWithOnlyTheirOwnHand() {
        styles.forEach { case ->
            listOf("layout", "layout-v31").forEach { folder ->
                val clocks = parse(folder, "widget_kwgt_future_${case.style}.xml")
                    .documentElement
                    .childElementsRecursively()
                    .filter { it.tagName == "AnalogClock" }
                assertEquals("$folder/${case.style} clock count", 3, clocks.size)
                assertEquals("kwgt_future_hour_clock", localId(clocks[0]))
                assertEquals("kwgt_future_minute_clock", localId(clocks[1]))
                assertEquals("kwgt_future_second_clock", localId(clocks[2]))

                assertEquals("@drawable/dw_future_clear", clocks[0].getAttribute("android:dial"))
                assertEquals("@drawable/dw_future_hour", clocks[0].getAttribute("android:hand_hour"))
                assertEquals("@drawable/dw_future_clear", clocks[0].getAttribute("android:hand_minute"))
                assertFalse(clocks[0].hasAttribute("android:hand_second"))

                assertEquals("@drawable/dw_future_clear", clocks[1].getAttribute("android:dial"))
                assertEquals("@drawable/dw_future_clear", clocks[1].getAttribute("android:hand_hour"))
                assertEquals("@drawable/dw_future_minute", clocks[1].getAttribute("android:hand_minute"))
                assertFalse(clocks[1].hasAttribute("android:hand_second"))

                assertEquals("@drawable/dw_future_clear", clocks[2].getAttribute("android:dial"))
                assertEquals("@drawable/dw_future_clear", clocks[2].getAttribute("android:hand_hour"))
                assertEquals("@drawable/dw_future_clear", clocks[2].getAttribute("android:hand_minute"))
                if (folder == "layout-v31") {
                    assertEquals("@drawable/dw_future_second", clocks[2].getAttribute("android:hand_second"))
                } else {
                    assertFalse(clocks[2].hasAttribute("android:hand_second"))
                }
            }
        }
    }

    @Test
    fun everyFutureDrawableHasTheSameSquareIntrinsicSize() {
        listOf(
            "dw_future_hour",
            "dw_future_minute",
            "dw_future_second",
            "dw_future_clear",
        ).forEach { name ->
            val vector = parse("drawable", "$name.xml").documentElement
            assertEquals(name, "277dp", vector.getAttribute("android:width"))
            assertEquals(name, "277dp", vector.getAttribute("android:height"))
            assertEquals(name, "277", vector.getAttribute("android:viewportWidth"))
            assertEquals(name, "277", vector.getAttribute("android:viewportHeight"))
        }
    }

    @Test
    fun discVectorsKeepTheSourceSweepColoursAndOneHundredEightyRotation() {
        mapOf(
            "dw_future_hour" to "#FFD71921",
            "dw_future_minute" to "#FFCBCBCB",
            "dw_future_second" to "#FF6C696E",
        ).forEach { (name, end) ->
            val vector = parse("drawable", "$name.xml").documentElement
            val group = vector.getElementsByTagName("group").item(0) as Element
            assertEquals("$name source offset", "180", group.getAttribute("android:rotation"))
            assertEquals("$name pivot x", "138.5", group.getAttribute("android:pivotX"))
            assertEquals("$name pivot y", "138.5", group.getAttribute("android:pivotY"))

            val gradient = vector.getElementsByTagName("item")
            assertEquals("$name gradient stop count", 2, gradient.length)
            assertEquals("$name gradient start", "#FF000000", (gradient.item(0) as Element).getAttribute("android:color"))
            assertEquals("$name gradient end", end, (gradient.item(1) as Element).getAttribute("android:color"))
            val path = vector.getElementsByTagName("path").item(0) as Element
            assertTrue("$name full circle", path.getAttribute("android:pathData").contains("A138.5,138.5"))
        }
    }

    @Test
    fun clearLayerIsTransparentAndLabelsAreLiveTwelveHourTextClocks() {
        assertEquals(
            "#00000000",
            (parse("drawable", "dw_future_clear.xml").documentElement
                .getElementsByTagName("path").item(0) as Element)
                .getAttribute("android:fillColor"),
        )

        styles.forEach { case ->
            listOf("layout", "layout-v31").forEach { folder ->
                val labels = parse(folder, "widget_kwgt_future_${case.style}.xml")
                    .documentElement
                    .childElementsRecursively()
                    .filter { it.tagName == "TextClock" }
                assertEquals("$folder/${case.style} label count", 3, labels.size)
                assertEquals("kwgt_future_hour_label", localId(labels[0]))
                assertEquals("hh' hr'", labels[0].getAttribute("android:format12Hour"))
                assertEquals("hh' hr'", labels[0].getAttribute("android:format24Hour"))
                assertEquals("kwgt_future_minute_label", localId(labels[1]))
                assertEquals("mm' min'", labels[1].getAttribute("android:format12Hour"))
                assertEquals("mm' min'", labels[1].getAttribute("android:format24Hour"))
                assertEquals("kwgt_future_second_label", localId(labels[2]))
                assertEquals("ss' sec'", labels[2].getAttribute("android:format12Hour"))
                assertEquals("ss' sec'", labels[2].getAttribute("android:format24Hour"))
                labels.forEach { label ->
                    assertEquals(case.labelInk, label.getAttribute("android:textColor"))
                }
            }
        }
    }

    @Test
    fun backgroundBandKeepsTheSourceHostWidthAndHostHeightOverOnePointFive() {
        styles.forEach { case ->
            val vector = parse("drawable", "${case.bg}.xml").documentElement
            assertEquals("916", vector.getAttribute("android:viewportWidth"))
            assertEquals("361", vector.getAttribute("android:viewportHeight"))
            val path = vector.getElementsByTagName("path").item(0) as Element
            assertEquals("${case.bg} fill", case.fill, path.getAttribute("android:fillColor"))
            assertTrue(
                "${case.bg} rounded 58 corner band",
                path.getAttribute("android:pathData").contains("M58,0") &&
                    path.getAttribute("android:pathData").contains("A58,58"),
            )
        }
    }

    @Test
    fun providerIsDisabledBelowApi31AndUsesTheSafeBaseLayout() {
        val receiver = manifestReceivers()
            .first { it.getAttribute("android:name") == ".KwgtFutureWidgetProvider" }
        assertEquals(
            "@bool/kwgt_analog_seconds_available",
            receiver.getAttribute("android:enabled"),
        )
        val meta = receiver.getElementsByTagName("meta-data").item(0) as Element
        assertEquals("@xml/kwgt_future_widget_info", meta.getAttribute("android:resource"))

        val info = parse("xml", "kwgt_future_widget_info.xml").documentElement
        assertEquals("@layout/widget_kwgt_future_black", info.getAttribute("android:initialLayout"))
        assertEquals("@layout/widget_kwgt_future_black", info.getAttribute("android:previewLayout"))
        assertEquals("0", info.getAttribute("android:updatePeriodMillis"))
        assertEquals("horizontal|vertical", info.getAttribute("android:resizeMode"))
    }

    @Test
    fun sanitizedSourceKeepsFormulasAndRemovesPrivateOrBundledAssets() {
        val text = listOf(
            File("src/main/assets/kwgt/nothing_future_clock.sanitized.json"),
            File("app/src/main/assets/kwgt/nothing_future_clock.sanitized.json"),
        ).first { it.isFile }.readText()
        assertTrue(text.contains("CLOCK_SECOND"))
        assertTrue(text.contains("CLOCK_HOUR_SMOOTH"))
        assertTrue(text.contains("CLOCK_MINUTE"))
        assertTrue(text.contains("config_rotate_offset"))
        assertTrue(text.contains("180.0"))
        assertTrue(text.contains("\$gv(h)/2\$"))
        assertTrue(text.contains("\$si(rheight)/1.5\$"))
        assertTrue(text.contains("#FFD71921"))
        assertTrue(text.contains("#FFCBCBCB"))
        assertTrue(text.contains("#FF6C696E"))
        assertFalse(text.contains("@gmail.com"))
        assertFalse(text.contains("LAUNCH_APP"))
        assertFalse(text.contains("intent:"))
        assertFalse(text.contains(".ttf"))
    }

    private fun manifestReceivers(): List<Element> =
        parseFile("../AndroidManifest.xml").documentElement
            .getElementsByTagName("receiver")
            .let { nodes -> (0 until nodes.length).map { nodes.item(it) as Element } }

    private fun localId(element: Element): String =
        element.getAttribute("android:id").removePrefix("@+id/")

    private fun parse(folder: String, name: String) =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(resFile("$folder/$name"))

    private fun parseFile(relative: String) =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(resFile(relative))

    private fun resFile(relative: String): File {
        val candidates = listOf(
            File("src/main/res/$relative"),
            File("app/src/main/res/$relative"),
        )
        return candidates.firstOrNull { it.isFile }
            ?: throw AssertionError("cannot find $relative from ${File(".").absolutePath}")
    }

    private fun Element.elements(): List<Element> =
        (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }

    private fun Element.childElementsRecursively(): List<Element> {
        val elements = mutableListOf<Element>()
        val nodes = childNodes
        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            if (node is Element) {
                elements += node
                elements += node.childElementsRecursively()
            }
        }
        return elements
    }
}
