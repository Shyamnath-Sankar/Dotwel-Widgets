package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Source-contract guard for the KWGT Nothing_Analog_ seconds port. These checks
 * run without an Android host: vector geometry, the API 31 second-hand gate and
 * the RemoteViews tag surface.
 */
class KwgtAnalogSecondsLayoutTest {
    private val styles = listOf(
        Triple("black", "dw_analogseconds_dial_black", "dw_analogseconds_hour_white"),
        Triple("translucent", "dw_analogseconds_dial_translucent", "dw_analogseconds_hour_dark"),
        Triple("transparent", "dw_analogseconds_dial_none", "dw_analogseconds_hour_white"),
    )

    @Test
    fun secondCircleUsesFullViewportAndSourceRadiusAtTwelve() {
        val vector = parse("drawable", "dw_analogseconds_second.xml").documentElement
        assertEquals("277", vector.getAttribute("android:viewportWidth"))
        assertEquals("277", vector.getAttribute("android:viewportHeight"))
        val path = vector.getElementsByTagName("path").item(0) as Element
        assertEquals("#FFD71921", path.getAttribute("android:fillColor"))
        val data = path.getAttribute("android:pathData")
        assertTrue("source diameter 277/23.08", data.contains("6.000866551126517"))
        assertTrue("centre 277/1.145/2 above dial centre", data.contains("17.53930131004367"))
        assertTrue("symmetric about dial centre x", data.contains("132.4991334488735") && data.contains("144.5008665511265"))
    }

    @Test
    fun hourCapsuleKeepsSourceWidthHeightAndCentredY() {
        listOf(
            "dw_analogseconds_hour_white" to "#FFFFFFFF",
            "dw_analogseconds_hour_dark" to "#FF1B1B1D",
        ).forEach { (name, color) ->
            val path = parse("drawable", "$name.xml").documentElement
                .getElementsByTagName("path").item(0) as Element
            assertEquals(name, color, path.getAttribute("android:fillColor"))
            val data = path.getAttribute("android:pathData")
            assertTrue("source capsule radius 277/11.54/2", data.contains("12.001733102253034"))
            assertTrue("top from centred Y", data.contains("79.49680828030901"))
            assertTrue("bottom from centred Y", data.contains("137.4945262084258"))
            assertTrue("centred X", data.contains("126.49826689774696"))
        }
    }

    @Test
    fun minuteCapsuleUsesConstantGrayAndSourceCentredY() {
        val path = parse("drawable", "dw_analogseconds_minute.xml").documentElement
            .getElementsByTagName("path").item(0) as Element
        assertEquals("#FF6C696E", path.getAttribute("android:fillColor"))
        val data = path.getAttribute("android:pathData")
        assertEquals("constant 10-unit width", true, data.contains("133.5,36.99562159678645"))
        assertTrue("source top from centred Y", data.contains("36.99562159678645"))
        assertTrue("source bottom from centred Y", data.contains("139.0053264087557"))
    }

    @Test
    fun dialsUseTheThreeStyleFills() {
        assertEquals(
            "#FF1B1B1D",
            firstPath("dw_analogseconds_dial_black").getAttribute("android:fillColor"),
        )
        assertEquals(
            "#A6FFFFFF",
            firstPath("dw_analogseconds_dial_translucent").getAttribute("android:fillColor"),
        )
        assertEquals(
            "#00000000",
            firstPath("dw_analogseconds_dial_none").getAttribute("android:fillColor"),
        )
    }

    @Test
    fun onlyV31LayoutsCarryTheNativeSecondHand() {
        styles.forEach { (style, _, _) ->
            val base = clock(parse("layout", "widget_kwgt_analog_seconds_$style.xml").documentElement)
            val v31 = clock(parse("layout-v31", "widget_kwgt_analog_seconds_$style.xml").documentElement)
            assertEquals("kwgt_analog_seconds_clock", localId(base))
            assertEquals("kwgt_analog_seconds_clock", localId(v31))
            assertEquals("match_parent", base.getAttribute("android:layout_width"))
            assertEquals("match_parent", v31.getAttribute("android:layout_width"))
            assertFalse(
                "$style base layout must stay safe below API 31",
                base.hasAttribute("android:hand_second"),
            )
            assertEquals(
                "@drawable/dw_analogseconds_second",
                v31.getAttribute("android:hand_second"),
            )
        }
    }

    @Test
    fun stylesMapToSourceDialHourAndMinute() {
        styles.forEach { (style, dial, hour) ->
            val v31 = clock(parse("layout-v31", "widget_kwgt_analog_seconds_$style.xml").documentElement)
            assertEquals("@drawable/$dial", v31.getAttribute("android:dial"))
            assertEquals("@drawable/$hour", v31.getAttribute("android:hand_hour"))
            assertEquals(
                "@drawable/dw_analogseconds_minute",
                v31.getAttribute("android:hand_minute"),
            )
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
            .first { it.getAttribute("android:name") == ".KwgtAnalogSecondsWidgetProvider" }
        assertEquals(
            "@bool/kwgt_analog_seconds_available",
            receiver.getAttribute("android:enabled"),
        )
        val meta = receiver.getElementsByTagName("meta-data").item(0) as Element
        assertEquals("@xml/kwgt_analog_seconds_widget_info", meta.getAttribute("android:resource"))

        val info = parse("xml", "kwgt_analog_seconds_widget_info.xml").documentElement
        assertEquals(
            "@layout/widget_kwgt_analog_seconds_black",
            info.getAttribute("android:initialLayout"),
        )
        assertEquals(
            "@layout/widget_kwgt_analog_seconds_black",
            info.getAttribute("android:previewLayout"),
        )
        assertEquals("0", info.getAttribute("android:updatePeriodMillis"))
    }

    @Test
    fun secondLayoutsOnlyUseRemoteViewsTags() {
        val safe = setOf("FrameLayout", "AnalogClock")
        styles.forEach { (style, _, _) ->
            listOf("layout", "layout-v31").forEach { folder ->
                parse(folder, "widget_kwgt_analog_seconds_$style.xml")
                    .documentElement
                    .childElementsRecursively()
                    .forEach { element ->
                        assertTrue(
                            "$folder/$style uses <${element.tagName}>",
                            element.tagName in safe,
                        )
                    }
            }
        }
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
