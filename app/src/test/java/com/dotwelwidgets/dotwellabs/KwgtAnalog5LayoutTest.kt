package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards the KWGT Nothing_Analog_5 port contract that can be checked without an
 * Android host: three RemoteViews layouts, one native AnalogClock per layout,
 * the source 277 vector viewport, the clipped 60/12 tick groups, the rounded
 * overlay geometry, and the filled pill hand drawables.
 */
class KwgtAnalog5LayoutTest {
    private val cases = listOf(
        Triple("widget_kwgt_analog5_black.xml", "dw_analog5_dial_black", false),
        Triple("widget_kwgt_analog5_translucent.xml", "dw_analog5_dial_translucent", true),
        Triple("widget_kwgt_analog5_transparent.xml", "dw_analog5_dial_none", false),
    )

    @Test
    fun eachStyleUsesOneNativeAnalogClock() {
        cases.forEach { (layout, _, _) ->
            val root = parse("layout", layout).documentElement
            val clocks = root.childElementsRecursively()
                .filter { it.tagName == "AnalogClock" }
            assertEquals("$layout clock count", 1, clocks.size)
            val clock = clocks.single()
            assertEquals("kwgt_analog5_clock", localId(clock))
            assertEquals("match_parent", clock.getAttribute("android:layout_width"))
            assertEquals("match_parent", clock.getAttribute("android:layout_height"))
        }
    }

    @Test
    fun styleMapsToTheSourceDialAndHands() {
        cases.forEach { (layout, dial, darkMinute) ->
            val clock = parse("layout", layout).documentElement
                .childElementsRecursively()
                .first { it.tagName == "AnalogClock" }
            assertEquals("@drawable/$dial", clock.getAttribute("android:dial"))
            assertEquals(
                "@drawable/dw_analog5_hour",
                clock.getAttribute("android:hand_hour"),
            )
            val minute = if (darkMinute) "dark" else "white"
            assertEquals(
                "@drawable/dw_analog5_minute_$minute",
                clock.getAttribute("android:hand_minute"),
            )
        }
    }

    @Test
    fun dialKeepsThe277ViewportAndClippedSixtyAndTwelveTickGroups() {
        cases.forEach { (_, dial, _) ->
            val vector = parse("drawable", "$dial.xml").documentElement
            assertEquals("277", vector.getAttribute("android:viewportWidth"))
            assertEquals("277", vector.getAttribute("android:viewportHeight"))
            val groups = vector.getElementsByTagName("group").elements()
            assertEquals("$dial clip groups", 2, groups.size)
            val counts = groups.map { group ->
                group.getElementsByTagName("clip-path").elements().size to
                    group.getElementsByTagName("path").elements().size
            }
            assertEquals("$dial minute group clip+ticks", 1 to 60, counts[0])
            assertEquals("$dial hour group clip+ticks", 1 to 12, counts[1])
            groups.forEach { group ->
                val clip = group.getElementsByTagName("clip-path").item(0) as Element
                assertTrue(
                    "$dial clip must be the 277/1.08 by 277 rounded rect",
                    clip.getAttribute("android:pathData").contains("266.7407") &&
                        clip.getAttribute("android:pathData").contains("46.1667"),
                )
            }
        }
    }

    @Test
    fun dialUsesSquareRoundedOverlaysAndSourceCorners() {
        cases.forEach { (_, dial, _) ->
            val vector = parse("drawable", "$dial.xml").documentElement
            val paths = vector.getElementsByTagName("path").elements()
            val data = paths.map { it.getAttribute("android:pathData") }
            val firstOverlay = data.first { it.contains("18.0652") }
            assertTrue(
                "$dial first overlay must be square 277/1.15",
                firstOverlay.contains("258.9348") &&
                    firstOverlay.contains("40.7353"),
            )
            val innerOverlay = data.first { it.contains("31.9615") }
            assertTrue(
                "$dial inner overlay must be square 277/1.3",
                innerOverlay.contains("245.0385") &&
                    innerOverlay.contains("39.5714"),
            )
        }
    }

    @Test
    fun styleColorsMatchTheSourceDarkLightAndTransparentMapping() {
        val expected = mapOf(
            "dw_analog5_dial_black" to ("#FF1B1B1D" to "#FFFFFFFF"),
            "dw_analog5_dial_translucent" to ("#A6FFFFFF" to "#FF1B1B1D"),
            "dw_analog5_dial_none" to ("#00000000" to "#FFFFFFFF"),
        )
        expected.forEach { (dial, colors) ->
            val vector = parse("drawable", "$dial.xml").documentElement
            val paths = vector.getElementsByTagName("path").elements()
            assertEquals(
                "$dial outer background",
                colors.first,
                paths[0].getAttribute("android:fillColor"),
            )
            val tick = (vector.getElementsByTagName("group").item(0) as Element)
                .getElementsByTagName("path").item(0) as Element
            assertEquals(
                "$dial tick ink",
                colors.second,
                tick.getAttribute("android:fillColor"),
            )
        }
    }

    @Test
    fun handVectorsAreFilledPillsOnThe277Canvas() {
        val hour = parse("drawable", "dw_analog5_hour.xml").documentElement
        val hourPath = hour.getElementsByTagName("path").item(0) as Element
        assertEquals("277", hour.getAttribute("android:viewportWidth"))
        assertEquals("#FFD71921", hourPath.getAttribute("android:fillColor"))
        assertTrue(hourPath.getAttribute("android:pathData").contains("92.9957"))

        listOf("white" to "#FFFFFFFF", "dark" to "#FF1B1B1D").forEach { (name, ink) ->
            val minute = parse("drawable", "dw_analog5_minute_$name.xml").documentElement
            val path = minute.getElementsByTagName("path").item(0) as Element
            assertEquals("277", minute.getAttribute("android:viewportWidth"))
            assertEquals(ink, path.getAttribute("android:fillColor"))
            assertTrue(path.getAttribute("android:pathData").contains("93.9949") ||
                path.getAttribute("android:pathData").contains("138.4945"))
        }
    }

    @Test
    fun sanitizedSourceRecordsTheMissingHeightAdaptation() {
        val file = listOf(
            File("src/main/assets/kwgt/nothing_analog5.sanitized.json"),
            File("app/src/main/assets/kwgt/nothing_analog5.sanitized.json"),
        ).first { it.isFile }
        val text = file.readText()
        assertTrue(text.contains("missing_global_adaptation"))
        assertTrue(text.contains("277/13.19"))
        assertTrue(text.contains("277/25.18"))
        assertTrue(text.contains("CLOCK_HOUR_SMOOTH"))
        assertTrue(text.contains("CLOCK_MINUTE"))
    }

    private fun localId(element: Element): String =
        element.getAttribute("android:id").removePrefix("@+id/")

    private fun parse(folder: String, name: String) =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file(folder, name))

    private fun file(folder: String, name: String): File {
        val candidates = listOf(
            File("src/main/res/$folder/$name"),
            File("app/src/main/res/$folder/$name"),
        )
        return candidates.firstOrNull { it.isFile }
            ?: throw AssertionError(
                "cannot find $name from ${File(".").absolutePath}",
            )
    }

    private fun org.w3c.dom.NodeList.elements(): List<Element> =
        (0 until length).mapNotNull { item(it) as? Element }

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
