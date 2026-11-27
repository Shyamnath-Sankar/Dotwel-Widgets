package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards the KWGT Nothing_Analog_3 port contract that can be checked without an
 * Android host: three RemoteViews layouts, one native AnalogClock per layout,
 * the source vector viewport and the exact dial/hand pair per style.
 */
class KwgtAnalog3LayoutTest {
    private val cases = listOf(
        Triple("widget_kwgt_analog3_black.xml", "dw_analog3_dial_black", false),
        Triple("widget_kwgt_analog3_translucent.xml", "dw_analog3_dial_translucent", true),
        Triple("widget_kwgt_analog3_transparent.xml", "dw_analog3_dial_none", false),
    )

    @Test
    fun eachStyleUsesOneNativeAnalogClock() {
        cases.forEach { (layout, _, _) ->
            val root = parse("layout", layout).documentElement
            val clocks = root.childElementsRecursively()
                .filter { it.tagName == "AnalogClock" }
            assertEquals("$layout clock count", 1, clocks.size)
            val clock = clocks.single()
            assertEquals("kwgt_analog3_clock", localId(clock))
            assertEquals("match_parent", clock.getAttribute("android:layout_width"))
            assertEquals("match_parent", clock.getAttribute("android:layout_height"))
        }
    }

    @Test
    fun styleMapsToTheSourceDialAndForegroundHands() {
        cases.forEach { (layout, dial, darkHands) ->
            val clock = parse("layout", layout).documentElement
                .childElementsRecursively()
                .first { it.tagName == "AnalogClock" }
            assertEquals("@drawable/$dial", clock.getAttribute("android:dial"))
            val hand = if (darkHands) "dark" else "white"
            assertEquals(
                "@drawable/dw_analog3_hour_$hand",
                clock.getAttribute("android:hand_hour"),
            )
            assertEquals(
                "@drawable/dw_analog3_minute_$hand",
                clock.getAttribute("android:hand_minute"),
            )
        }
    }

    @Test
    fun handVectorsKeepThe277ViewportAndTwoUnitStroke() {
        listOf(
            "dw_analog3_hour_white",
            "dw_analog3_hour_dark",
            "dw_analog3_minute_white",
            "dw_analog3_minute_dark",
        ).forEach { name ->
            val vector = parse("drawable", "$name.xml").documentElement
            assertEquals("277", vector.getAttribute("android:viewportWidth"))
            assertEquals("277", vector.getAttribute("android:viewportHeight"))
            val path = vector.getElementsByTagName("path").item(0) as Element
            assertEquals("2", path.getAttribute("android:strokeWidth"))
            assertTrue(
                "$name must use a hollow hand",
                path.getAttribute("android:fillColor").contains("#00000000"),
            )
            assertTrue(
                "$name must pivot at 138.5",
                path.getAttribute("android:pathData").contains("138."),
            )
        }
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
