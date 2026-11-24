package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Source-contract guard for the KWGT Nothing_Analog_2 port. These checks run
 * without an Android host: vector geometry, the API 31 seconds gate, the source
 * z-order (ticks and red centre above the native clock) and the RemoteViews tag
 * surface.
 */
class KwgtAnalog2LayoutTest {
    private data class StyleCase(
        val style: String,
        val dial: String,
        val hour: String,
        val minute: String,
        val overlay: String,
    )

    private val styles = listOf(
        StyleCase(
            "black",
            "dw_analog2_dial_black",
            "dw_analog2_hour_white",
            "dw_analog2_minute_white",
            "dw_analog2_overlay_white",
        ),
        StyleCase(
            "translucent",
            "dw_analog2_dial_translucent",
            "dw_analog2_hour_dark",
            "dw_analog2_minute_dark",
            "dw_analog2_overlay_dark",
        ),
        StyleCase(
            "transparent",
            "dw_analog2_dial_none",
            "dw_analog2_hour_white",
            "dw_analog2_minute_white",
            "dw_analog2_overlay_white",
        ),
    )

    @Test
    fun eachStyleUsesOneNativeClockWithTheOverlayAboveIt() {
        styles.forEach { case ->
            listOf("layout", "layout-v31").forEach { folder ->
                val root = parse(folder, "widget_kwgt_analog2_${case.style}.xml").documentElement
                val children = root.directChildren()
                assertEquals("$folder/${case.style} child count", 2, children.size)
                assertEquals("$folder/${case.style} clock first", "AnalogClock", children[0].tagName)
                assertEquals("$folder/${case.style} overlay last", "ImageView", children[1].tagName)
                assertEquals("kwgt_analog2_clock", localId(children[0]))
                assertEquals("kwgt_analog2_overlay", localId(children[1]))
            }
        }
    }

    @Test
    fun overlayIsFullViewportFitCenterForResponsiveAlignment() {
        styles.forEach { case ->
            listOf("layout", "layout-v31").forEach { folder ->
                val root = parse(folder, "widget_kwgt_analog2_${case.style}.xml").documentElement
                val clock = root.directChildren().first { it.tagName == "AnalogClock" }
                val overlay = root.directChildren().first { it.tagName == "ImageView" }
                assertEquals("$folder/${case.style} clock width", "match_parent", clock.getAttribute("android:layout_width"))
                assertEquals("$folder/${case.style} clock height", "match_parent", clock.getAttribute("android:layout_height"))
                assertEquals("$folder/${case.style} clock gravity", "center", clock.getAttribute("android:layout_gravity"))
                assertEquals("$folder/${case.style} overlay width", "match_parent", overlay.getAttribute("android:layout_width"))
                assertEquals("$folder/${case.style} overlay height", "match_parent", overlay.getAttribute("android:layout_height"))
                assertEquals("$folder/${case.style} overlay gravity", "center", overlay.getAttribute("android:layout_gravity"))
                assertEquals("$folder/${case.style} overlay scale", "fitCenter", overlay.getAttribute("android:scaleType"))
                assertEquals("@drawable/${case.overlay}", overlay.getAttribute("android:src"))
            }
        }
    }

    @Test
    fun onlyV31LayoutsCarryTheNativeSecondHand() {
        styles.forEach { case ->
            val base = clock(parse("layout", "widget_kwgt_analog2_${case.style}.xml").documentElement)
            val v31 = clock(parse("layout-v31", "widget_kwgt_analog2_${case.style}.xml").documentElement)
            assertFalse(
                "${case.style} base layout must stay safe below API 31",
                base.hasAttribute("android:hand_second"),
            )
            assertEquals(
                "@drawable/dw_analog2_second",
                v31.getAttribute("android:hand_second"),
            )
        }
    }

    @Test
    fun stylesMapToTheSourceDialHandsAndOverlayInk() {
        styles.forEach { case ->
            val v31 = clock(parse("layout-v31", "widget_kwgt_analog2_${case.style}.xml").documentElement)
            assertEquals("@drawable/${case.dial}", v31.getAttribute("android:dial"))
            assertEquals("@drawable/${case.hour}", v31.getAttribute("android:hand_hour"))
            assertEquals("@drawable/${case.minute}", v31.getAttribute("android:hand_minute"))
            assertEquals("@drawable/dw_analog2_second", v31.getAttribute("android:hand_second"))
        }
    }

    @Test
    fun providerIsDisabledBelowApi31AndPointsAtTheSafeBaseLayout() {
        val defaults = parseFile("values/bools.xml").documentElement
            .getElementsByTagName("bool").item(0) as Element
        assertEquals("kwgt_analog_seconds_available", defaults.getAttribute("name"))
        assertEquals("false", defaults.textContent)
        val v31 = parseFile("values-v31/bools.xml").documentElement
            .getElementsByTagName("bool").item(0) as Element
        assertEquals("kwgt_analog_seconds_available", v31.getAttribute("name"))
        assertEquals("true", v31.textContent)

        val receiver = manifestReceivers()
            .first { it.getAttribute("android:name") == ".KwgtAnalog2WidgetProvider" }
        assertEquals(
            "@bool/kwgt_analog_seconds_available",
            receiver.getAttribute("android:enabled"),
        )
        val meta = receiver.getElementsByTagName("meta-data").item(0) as Element
        assertEquals("@xml/kwgt_analog2_widget_info", meta.getAttribute("android:resource"))

        val info = parse("xml", "kwgt_analog2_widget_info.xml").documentElement
        assertEquals(
            "@layout/widget_kwgt_analog2_black",
            info.getAttribute("android:initialLayout"),
        )
        assertEquals(
            "@layout/widget_kwgt_analog2_black",
            info.getAttribute("android:previewLayout"),
        )
        assertEquals("0", info.getAttribute("android:updatePeriodMillis"))
    }

    @Test
    fun overlayKeepsExactlySeventyTwoTicksAndTheRedCentreOnTop() {
        listOf(
            "dw_analog2_overlay_white" to "#FFFFFFFF",
            "dw_analog2_overlay_dark" to "#FF1B1B1D",
        ).forEach { (name, ink) ->
            val vector = parse("drawable", "$name.xml").documentElement
            assertEquals("$name viewport width", "277", vector.getAttribute("android:viewportWidth"))
            assertEquals("$name viewport height", "277", vector.getAttribute("android:viewportHeight"))

            val groups = vector.getElementsByTagName("group").elements()
            val hour = groups.first { it.getAttribute("android:name") == "hour_ticks" }
            val minute = groups.first { it.getAttribute("android:name") == "minute_ticks" }
            val hourPaths = hour.getElementsByTagName("path").elements()
            val minutePaths = minute.getElementsByTagName("path").elements()
            assertEquals("$name hour tick count", 12, hourPaths.size)
            assertEquals("$name minute tick count", 60, minutePaths.size)
            assertEquals("$name total tick count", 72, hourPaths.size + minutePaths.size)

            hourPaths.forEach { assertEquals("$name hour ink", ink, it.getAttribute("android:fillColor")) }
            minutePaths.forEach { assertEquals("$name minute ink", ink, it.getAttribute("android:fillColor")) }

            assertEquals(
                "$name 12 o'clock hour tick radius 125 and 8 x 16.5",
                "M134.5,5.25 L142.5,5.25 L142.5,21.75 L134.5,21.75 Z",
                hourPaths[0].getAttribute("android:pathData"),
            )
            assertEquals(
                "$name 12 o'clock minute tick radius 129 and 11 x 8",
                "M133,5.5 L144,5.5 L144,13.5 L133,13.5 Z",
                minutePaths[0].getAttribute("android:pathData"),
            )

            val allPaths = vector.getElementsByTagName("path").elements()
            assertEquals("$name tick plus centre path count", 73, allPaths.size)
            val centre = allPaths.last()
            assertEquals("$name centre ink", "#FFD71921", centre.getAttribute("android:fillColor"))
            val centreData = centre.getAttribute("android:pathData")
            assertTrue("$name centre diameter 16", centreData.contains("M130.5,138.5 A8,8"))
            assertTrue("$name centre x extent", centreData.contains("146.5,138.5"))
        }
    }

    @Test
    fun handAndDialVectorsKeepExactSourceGeometry() {
        listOf(
            "dw_analog2_hour_white" to "#FFFFFFFF",
            "dw_analog2_hour_dark" to "#FF1B1B1D",
        ).forEach { (name, ink) ->
            val path = firstPath(name)
            assertEquals(name, ink, path.getAttribute("android:fillColor"))
            assertEquals(
                "$name source 6 x 74 at centre Y 106.5",
                "M135.5,69.5 L141.5,69.5 L141.5,143.5 L135.5,143.5 Z",
                path.getAttribute("android:pathData"),
            )
        }

        listOf(
            "dw_analog2_minute_white" to "#FFFFFFFF",
            "dw_analog2_minute_dark" to "#FF1B1B1D",
        ).forEach { (name, ink) ->
            val path = firstPath(name)
            assertEquals(name, ink, path.getAttribute("android:fillColor"))
            assertEquals(
                "$name source 6 x 100 at centre Y 82.5",
                "M135.5,32.5 L141.5,32.5 L141.5,132.5 L135.5,132.5 Z",
                path.getAttribute("android:pathData"),
            )
        }

        val second = firstPath("dw_analog2_second")
        assertEquals("#FFD71921", second.getAttribute("android:fillColor"))
        assertEquals(
            "source seconds rect 3 x 130, vertical at zero, extent 23.5..153.5",
            "M137,23.5 L140,23.5 L140,153.5 L137,153.5 Z",
            second.getAttribute("android:pathData"),
        )

        assertEquals(
            "#FF1B1B1D",
            firstPath("dw_analog2_dial_black").getAttribute("android:fillColor"),
        )
        assertEquals(
            "#A6FFFFFF",
            firstPath("dw_analog2_dial_translucent").getAttribute("android:fillColor"),
        )
        assertEquals(
            "#00000000",
            firstPath("dw_analog2_dial_none").getAttribute("android:fillColor"),
        )
        firstPath("dw_analog2_dial_black").let { dial ->
            assertTrue(dial.getAttribute("android:pathData").contains("A138.5,138.5"))
        }
    }

    @Test
    fun allAnalog2LayoutsOnlyUseRemoteViewsTags() {
        val safe = setOf("FrameLayout", "AnalogClock", "ImageView")
        styles.forEach { case ->
            listOf("layout", "layout-v31").forEach { folder ->
                parse(folder, "widget_kwgt_analog2_${case.style}.xml")
                    .documentElement
                    .childElementsRecursively()
                    .forEach { element ->
                        assertTrue(
                            "$folder/${case.style} uses <${element.tagName}>",
                            element.tagName in safe,
                        )
                    }
            }
        }
    }

    @Test
    fun sanitizedSourceKeepsFormulasTogglesAndDocumentsUncertainty() {
        val text = listOf(
            File("src/main/assets/kwgt/nothing_analog2.sanitized.json"),
            File("app/src/main/assets/kwgt/nothing_analog2.sanitized.json"),
        ).first { it.isFile }.readText()
        assertTrue(text.contains("CLOCK_SECOND"))
        assertTrue(text.contains("CLOCK_HOUR_SMOOTH"))
        assertTrue(text.contains("CLOCK_MINUTE"))
        assertTrue(text.contains("style_size"))
        assertTrue(text.contains("250"))
        assertTrue(text.contains("258"))
        assertTrue(text.contains("config_rotate_mode"))
        assertTrue(text.contains("raw source width 6"))
        assertTrue(text.contains("default shape type is CIRCLE"))
        assertTrue(text.contains("277, not 279"))
        assertFalse(text.contains("@gmail.com"))
        assertFalse(text.contains("LAUNCH_APP"))
        assertFalse(text.contains("intent:"))
        assertFalse(text.contains(".ttf"))
    }

    private fun firstPath(name: String): Element =
        parse("drawable", "$name.xml").documentElement
            .getElementsByTagName("path").item(0) as Element

    private fun clock(root: Element): Element =
        root.childElementsRecursively().first { it.tagName == "AnalogClock" }

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
            ?: throw AssertionError(
                "cannot find $relative from ${File(".").absolutePath}",
            )
    }

    private fun org.w3c.dom.NodeList.elements(): List<Element> =
        (0 until length).mapNotNull { item(it) as? Element }

    private fun Element.directChildren(): List<Element> =
        childNodes.let { nodes ->
            (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }
        }

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
