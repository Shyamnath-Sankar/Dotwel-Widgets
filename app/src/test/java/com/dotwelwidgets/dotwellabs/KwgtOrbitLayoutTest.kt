package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Source-contract guard for the KWGT Nothing_Digital_Clock orbit port. These
 * checks run without an Android host: the 277 dial/orbit/dot geometry, the
 * native AnalogClock references, the live TextClock formats, the inactive
 * CLOCK_SECOND toggle values and the three style layouts.
 */
class KwgtOrbitLayoutTest {
    private data class StyleCase(
        val style: String,
        val dial: String,
        val minute: String,
        val textColor: String,
        val outer: String,
        val inner: String,
        val orbit: String,
    )

    private val styles = listOf(
        StyleCase(
            "black",
            "dw_orbit_dial_black",
            "dw_orbit_minute_white",
            "#FF1B1B1D",
            "#FF1B1B1D",
            "#FFFFFFFF",
            "#80FFFFFF",
        ),
        StyleCase(
            "translucent",
            "dw_orbit_dial_translucent",
            "dw_orbit_minute_dark",
            "#FFFFFFFF",
            "#A6FFFFFF",
            "#FF1B1B1D",
            "#801B1B1D",
        ),
        StyleCase(
            "transparent",
            "dw_orbit_dial_none",
            "dw_orbit_minute_white",
            "#FF1B1B1D",
            "#00000000",
            "#FFFFFFFF",
            "#80FFFFFF",
        ),
    )

    @Test
    fun eachStyleUsesOneNativeClockAndOneLiveTextClock() {
        styles.forEach { case ->
            val root = parse("layout", "widget_kwgt_orbit_${case.style}.xml").documentElement
            val children = root.directChildren()
            assertEquals("${case.style} child count", 2, children.size)
            assertEquals("${case.style} clock first", "AnalogClock", children[0].tagName)
            assertEquals("${case.style} text last", "TextClock", children[1].tagName)

            val clock = children[0]
            assertEquals("kwgt_orbit_clock", localId(clock))
            assertEquals("match_parent", clock.getAttribute("android:layout_width"))
            assertEquals("match_parent", clock.getAttribute("android:layout_height"))
            assertEquals("center", clock.getAttribute("android:layout_gravity"))
            assertEquals("@drawable/${case.dial}", clock.getAttribute("android:dial"))
            assertEquals("@drawable/dw_orbit_hour", clock.getAttribute("android:hand_hour"))
            assertEquals("@drawable/${case.minute}", clock.getAttribute("android:hand_minute"))
            assertFalse("${case.style} must not add seconds", clock.hasAttribute("android:hand_second"))

            val text = children[1]
            assertEquals("kwgt_orbit_time", localId(text))
            assertEquals("hh:mm", text.getAttribute("android:format12Hour"))
            assertEquals("HH:mm", text.getAttribute("android:format24Hour"))
            assertEquals("sans-serif", text.getAttribute("android:fontFamily"))
            assertEquals(case.textColor, text.getAttribute("android:textColor"))
            assertFalse(
                "${case.style} must not pin a fixed preview text size",
                text.hasAttribute("android:textSize"),
            )
            assertFalse(
                "${case.style} must not pin fixed preview offsets",
                text.hasAttribute("android:translationX"),
            )
            assertFalse(
                "${case.style} must not pin fixed preview offsets",
                text.hasAttribute("android:translationY"),
            )
        }
    }

    @Test
    fun translucentTimeInkIsWhiteAndOtherStylesKeepTheDarkSourceInk() {
        assertEquals(
            0xFFFFFFFF.toInt(),
            KwgtOrbitClockWidgetProvider.timeInk(WidgetStyle.TRANSLUCENT),
        )
        assertEquals(
            0xFF1B1B1D.toInt(),
            KwgtOrbitClockWidgetProvider.timeInk(WidgetStyle.BLACK),
        )
        assertEquals(
            0xFF1B1B1D.toInt(),
            KwgtOrbitClockWidgetProvider.timeInk(WidgetStyle.TRANSPARENT),
        )
    }

    @Test
    fun timeMetricsUseTheSource46Over277SizeAnd3Minus4Offsets() {
        val metrics = KwgtOrbitClockWidgetProvider.timeMetrics(
            hostWidthDp = 110f,
            hostHeightDp = 110f,
            density = 3f,
        )
        val sidePx = (110f - WidgetSizing.CONTENT_INSET_DP * 2f) * 3f

        assertEquals(sidePx * 46f / 277f, metrics.textSizePx, 0.01f)
        assertEquals(sidePx * 3f / 277f, metrics.offsetXPx, 0.01f)
        assertEquals(-sidePx * 4f / 277f, metrics.offsetYPx, 0.01f)
        assertTrue(
            "live time must stay inside the 164/277 inner disc",
            metrics.textSizePx < sidePx * 164f / 277f,
        )
        assertEquals(
            metrics.offsetXPx,
            (metrics.padLeft - metrics.padRight) / 2f,
            0.51f,
        )
        assertEquals(
            metrics.offsetYPx,
            (metrics.padTop - metrics.padBottom) / 2f,
            0.51f,
        )
    }

    @Test
    fun nonSquareHostsScaleFromTheShortSideWithoutDistortingTheCircle() {
        val square = KwgtOrbitClockWidgetProvider.timeMetrics(110f, 110f, 2f)
        val wide = KwgtOrbitClockWidgetProvider.timeMetrics(200f, 110f, 2f)
        val tall = KwgtOrbitClockWidgetProvider.timeMetrics(110f, 200f, 2f)

        assertEquals(square.textSizePx, wide.textSizePx, 0.01f)
        assertEquals(square.textSizePx, tall.textSizePx, 0.01f)
        assertEquals(square.offsetXPx, wide.offsetXPx, 0.01f)
        assertEquals(square.offsetYPx, wide.offsetYPx, 0.01f)
        assertEquals(square.offsetXPx, tall.offsetXPx, 0.01f)
        assertEquals(square.offsetYPx, tall.offsetYPx, 0.01f)
    }

    @Test
    fun providerReappliesStyleInkSizeOffsetsAndFormatsOnEveryUpdate() {
        val source = listOf(
            File(
                "src/main/java/com/dotwelwidgets/dotwellabs/" +
                    "KwgtOrbitClockWidgetProvider.kt",
            ),
            File(
                "app/src/main/java/com/dotwelwidgets/dotwellabs/" +
                    "KwgtOrbitClockWidgetProvider.kt",
            ),
        ).first { it.isFile }.readText()

        assertTrue(source.contains("views.setTextColor(R.id.kwgt_orbit_time"))
        assertTrue(source.contains("views.setTextViewTextSize("))
        assertTrue(source.contains("views.setViewPadding("))
        assertTrue(source.contains("setFormat12Hour"))
        assertTrue(source.contains("setFormat24Hour"))
    }

    @Test
    fun dialVectorsKeepThe277CirclesAndBothThinOrbits() {
        styles.forEach { case ->
            val vector = parse("drawable", "${case.dial}.xml").documentElement
            assertEquals("${case.dial} viewport width", "277", vector.getAttribute("android:viewportWidth"))
            assertEquals("${case.dial} viewport height", "277", vector.getAttribute("android:viewportHeight"))
            val paths = vector.getElementsByTagName("path").elements()
            assertEquals("${case.dial} path count", 4, paths.size)

            assertEquals("${case.dial} outer fill", case.outer, paths[0].getAttribute("android:fillColor"))
            assertTrue(
                "${case.dial} outer diameter 277",
                paths[0].getAttribute("android:pathData").contains("A138.5,138.5"),
            )
            assertEquals("${case.dial} inner fill", case.inner, paths[1].getAttribute("android:fillColor"))
            assertTrue(
                "${case.dial} inner diameter 164",
                paths[1].getAttribute("android:pathData").contains("A82,82"),
            )
            assertEquals("${case.dial} hour orbit fill", "#00000000", paths[2].getAttribute("android:fillColor"))
            assertEquals("${case.dial} hour orbit stroke", case.orbit, paths[2].getAttribute("android:strokeColor"))
            assertEquals("${case.dial} hour orbit width", "0.5", paths[2].getAttribute("android:strokeWidth"))
            assertTrue(
                "${case.dial} hour orbit diameter 205",
                paths[2].getAttribute("android:pathData").contains("A102.5,102.5"),
            )
            assertEquals("${case.dial} minute orbit fill", "#00000000", paths[3].getAttribute("android:fillColor"))
            assertEquals("${case.dial} minute orbit stroke", case.orbit, paths[3].getAttribute("android:strokeColor"))
            assertEquals("${case.dial} minute orbit width", "0.5", paths[3].getAttribute("android:strokeWidth"))
            assertTrue(
                "${case.dial} minute orbit diameter 245",
                paths[3].getAttribute("android:pathData").contains("A122.5,122.5"),
            )
        }
    }

    @Test
    fun hourAndMinuteDotsSitAtTheSourceRadiiAtTwelveOClock() {
        val hour = firstPath("dw_orbit_hour")
        assertEquals("#FFD71921", hour.getAttribute("android:fillColor"))
        assertEquals(
            "hour dot diameter 12, centre radius 102, zero at 12 o'clock",
            "M138.5,30.5 A6,6 0 1,1 138.5,42.5 A6,6 0 1,1 138.5,30.5 Z",
            hour.getAttribute("android:pathData"),
        )

        listOf(
            "dw_orbit_minute_white" to "#FFFFFFFF",
            "dw_orbit_minute_dark" to "#FF1B1B1D",
        ).forEach { (name, ink) ->
            val minute = firstPath(name)
            assertEquals(name, ink, minute.getAttribute("android:fillColor"))
            assertEquals(
                "$name minute dot diameter 12, centre radius 122.5, zero at 12 o'clock",
                "M138.5,10 A6,6 0 1,1 138.5,22 A6,6 0 1,1 138.5,10 Z",
                minute.getAttribute("android:pathData"),
            )
        }
    }

    @Test
    fun dotCentresStayInsideTheirOrbitsAndInsideTheOuterCircle() {
        val hourCentre = 138.5 - 36.5
        val minuteCentre = 138.5 - 16.0
        val hourOrbit = 205.0 / 2
        val minuteOrbit = 245.0 / 2
        val outer = 277.0 / 2
        val inner = 164.0 / 2
        val dotRadius = 12.0 / 2

        assertEquals(102.0, hourCentre, 0.0001)
        assertEquals(122.5, minuteCentre, 0.0001)
        assertTrue("hour dot centre inside its orbit", hourCentre <= hourOrbit)
        assertTrue("minute dot centre on its orbit", minuteCentre <= minuteOrbit)
        assertTrue("hour orbit inside outer circle", hourOrbit < outer)
        assertTrue("minute orbit inside outer circle", minuteOrbit < outer)
        assertTrue("inner circle inside hour orbit", inner < hourCentre)
        assertTrue("dot diameter is 12", dotRadius * 2 == 12.0)
    }

    @Test
    fun widgetInfoIsTwoByTwoAndNeverRestrictsApi() {
        val info = parse("xml", "kwgt_orbit_widget_info.xml").documentElement
        assertEquals("@layout/widget_kwgt_orbit_black", info.getAttribute("android:initialLayout"))
        assertEquals("@layout/widget_kwgt_orbit_black", info.getAttribute("android:previewLayout"))
        assertEquals("110dp", info.getAttribute("android:minWidth"))
        assertEquals("110dp", info.getAttribute("android:minHeight"))
        assertEquals("2", info.getAttribute("android:targetCellWidth"))
        assertEquals("2", info.getAttribute("android:targetCellHeight"))
        assertEquals("0", info.getAttribute("android:updatePeriodMillis"))
        assertEquals("horizontal|vertical", info.getAttribute("android:resizeMode"))

        val receiver = manifestReceivers()
            .first { it.getAttribute("android:name") == ".KwgtOrbitClockWidgetProvider" }
        assertFalse("orbit clock must not be API gated", receiver.hasAttribute("android:enabled"))
        val meta = receiver.getElementsByTagName("meta-data").item(0) as Element
        assertEquals("@xml/kwgt_orbit_widget_info", meta.getAttribute("android:resource"))
    }

    @Test
    fun sanitizedSourceKeepsFormulasAndDocumentsThePreviewLedRepair() {
        val text = listOf(
            File("src/main/assets/kwgt/nothing_digital_clock.sanitized.json"),
            File("app/src/main/assets/kwgt/nothing_digital_clock.sanitized.json"),
        ).first { it.isFile }.readText()

        assertTrue(text.contains("CLOCK_HOUR_SMOOTH"))
        assertTrue(text.contains("CLOCK_MINUTE"))
        assertTrue(text.contains("CLOCK_SECOND"))
        assertTrue(text.contains("\"config_rotate_mode\": 0"))
        assertTrue(text.contains("inactive"))
        assertTrue(text.contains("no seconds"))
        assertTrue(text.contains("\"text_expression\": null"))
        assertTrue(text.contains("02:11"))
        assertTrue(text.contains("preview-led"))
        assertTrue(text.contains("205"))
        assertTrue(text.contains("245"))
        assertTrue(text.contains("164"))
        assertTrue(text.contains("46"))
        assertTrue(text.contains("204"))
        assertFalse(text.contains("@gmail.com"))
        assertFalse(text.contains("LAUNCH_APP"))
        assertFalse(text.contains("intent:"))
        assertFalse(text.contains(".ttf"))
    }

    @Test
    fun allOrbitLayoutsOnlyUseRemoteViewsTags() {
        val safe = setOf("FrameLayout", "AnalogClock", "TextClock")
        styles.forEach { case ->
            parse("layout", "widget_kwgt_orbit_${case.style}.xml")
                .documentElement
                .childElementsRecursively()
                .forEach { element ->
                    assertTrue(
                        "${case.style} uses <${element.tagName}>",
                        element.tagName in safe,
                    )
                }
        }
    }

    private fun firstPath(name: String): Element =
        parse("drawable", "$name.xml").documentElement
            .getElementsByTagName("path").item(0) as Element

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
