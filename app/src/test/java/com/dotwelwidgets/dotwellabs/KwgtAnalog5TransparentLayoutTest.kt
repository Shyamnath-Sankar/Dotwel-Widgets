package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.hypot

/**
 * Guards the actual-source Nothing_Analog_5_Transparent port without an Android
 * host: the distinct provider, the three native AnalogClock layouts, the 277
 * vector viewport, the real even-odd CLEAR cut-outs, the source 1.05 minute
 * ring, the /5 outer clip corner, the /6.5 and /7 CLEAR corners, and the reused
 * source-identical hand vectors.
 */
class KwgtAnalog5TransparentLayoutTest {
    private val cases = listOf(
        Triple("widget_kwgt_analog5clear_black.xml", "dw_analog5clear_dial_white", false),
        Triple("widget_kwgt_analog5clear_translucent.xml", "dw_analog5clear_dial_dark", true),
        Triple("widget_kwgt_analog5clear_transparent.xml", "dw_analog5clear_dial_white", false),
    )

    @Test
    fun eachStyleUsesOneNativeAnalogClockAndTheSourceDials() {
        cases.forEach { (layout, dial, darkMinute) ->
            val root = parse("layout", layout).documentElement
            val clocks = root.childElementsRecursively()
                .filter { it.tagName == "AnalogClock" }
            assertEquals("$layout clock count", 1, clocks.size)
            val clock = clocks.single()
            assertEquals("kwgt_analog5clear_clock", localId(clock))
            assertEquals("match_parent", clock.getAttribute("android:layout_width"))
            assertEquals("match_parent", clock.getAttribute("android:layout_height"))
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
    fun dialIsOnlyTicksWithNoFilledCard() {
        cases.forEach { (_, dial, _) ->
            val vector = parse("drawable", "$dial.xml").documentElement
            assertEquals("277", vector.getAttribute("android:viewportWidth"))
            assertEquals("277", vector.getAttribute("android:viewportHeight"))
            val paths = vector.getElementsByTagName("path").elements()
            assertEquals("$dial must not add a filled dial card", 2, paths.size)
            paths.forEach { path ->
                assertEquals("$dial CLEAR hole fill type", "evenOdd", path.getAttribute("android:fillType"))
                assertFalse(
                    "$dial must not paint a transparent background card",
                    path.getAttribute("android:fillColor") == "#00000000",
                )
            }
        }
    }

    @Test
    fun minuteTicksUseTheSourceOnePointZeroFiveRing() {
        cases.forEach { (_, dial, _) ->
            val minute = tickSubpaths(dialPath(dial, 0))
            assertEquals("$dial minute tick count", 60, minute.size)
            val first = minute.first()
            assertEquals("minute tick width", 4.0, first.maxOf { it.first } - first.minOf { it.first }, 0.0001)
            assertEquals("minute tick height", 55.4, first.maxOf { it.second } - first.minOf { it.second }, 0.0001)
            val radii = first.map { (x, y) -> hypot(x - 138.5, y - 138.5) }
            assertEquals("minute 1.05 ring inner radius", 117.7420, radii.min(), 0.01)
            assertEquals("minute 1.05 ring outer radius", 173.1366, radii.max(), 0.01)
            assertTrue(
                "$dial must keep the source 1.05 ring, not Analog5's 138.5 centre radius",
                minute.first().none { (_, y) ->
                    kotlin.math.abs(y - 27.7) < 0.001 ||
                        kotlin.math.abs(y + 27.7) < 0.001
                },
            )
        }
    }

    @Test
    fun hourTicksUseTheSourceBaseRing() {
        cases.forEach { (_, dial, _) ->
            val hour = tickSubpaths(dialPath(dial, 1))
            assertEquals("$dial hour tick count", 12, hour.size)
            val first = hour.first()
            assertEquals("hour tick width", 6.0, first.maxOf { it.first } - first.minOf { it.first }, 0.0001)
            assertEquals("hour tick height", 69.25, first.maxOf { it.second } - first.minOf { it.second }, 0.0001)
            assertEquals("hour tick left", 135.5, first.minOf { it.first }, 0.0001)
            assertEquals("hour tick bottom", -34.625, first.minOf { it.second }, 0.0001)
        }
    }

    @Test
    fun clearOverlaysAreRealEvenOddHolesNotBackgroundFills() {
        cases.forEach { (_, dial, _) ->
            val minutePath = dialPath(dial, 0)
            val hourPath = dialPath(dial, 1)
            val minuteSubpaths = minutePath.split("M").drop(1)
            val hourSubpaths = hourPath.split("M").drop(1)
            assertEquals("minute ticks plus first CLEAR", 61, minuteSubpaths.size)
            assertEquals("hour ticks plus last CLEAR", 13, hourSubpaths.size)

            assertTrue(
                "$dial minute path must cut the first CLEAR square",
                minutePath.contains("18.0652") &&
                    minutePath.contains("258.9348") &&
                    minutePath.contains("42.6154"),
            )
            assertTrue(
                "$dial hour path must cut the last CLEAR square",
                hourPath.contains("31.9615") &&
                    hourPath.contains("245.0385") &&
                    hourPath.contains("39.5714"),
            )
            assertTrue(
                "$dial must express CLEAR as a hole, not an opaque overlay",
                minutePath.contains("M60.6806,18.0652") &&
                    hourPath.contains("M71.5330,31.9615"),
            )
        }
    }

    @Test
    fun clipAndClearCornersMatchTheActualTransparentSource() {
        cases.forEach { (_, dial, _) ->
            val vector = parse("drawable", "$dial.xml").documentElement
            val clips = vector.getElementsByTagName("clip-path").elements()
            assertEquals("$dial clip paths", 2, clips.size)
            clips.forEach { clip ->
                val data = clip.getAttribute("android:pathData")
                assertTrue(
                    "$dial outer clip width 277/1.08 and corner 277/5",
                    data.contains("10.2593") &&
                        data.contains("266.7407") &&
                        data.contains("55.4000") &&
                        !data.contains("46.1667"),
                )
            }
            val minute = dialPath(dial, 0)
            val hour = dialPath(dial, 1)
            assertTrue(
                "$dial first CLEAR corner 277/6.5",
                minute.contains("42.6154") && !minute.contains("40.7353"),
            )
            assertTrue(
                "$dial last CLEAR corner 277/7",
                hour.contains("39.5714"),
            )
        }
    }

    @Test
    fun sanitizedAssetKeepsTheActualFormulasAndDropsPrivateData() {
        val text = repoFile("src/main/assets", "kwgt/nothing_analog5_transparent.sanitized.json").readText()
        assertTrue(text.contains("157.74908"))
        assertTrue(text.contains("\"paint_mode\": \"CLEAR\""))
        assertTrue(text.contains("\$gv(side)*1.05\$"))
        assertTrue(text.contains("\$gv(side)/5\$"))
        assertTrue(text.contains("\$gv(side)/6.5\$"))
        assertTrue(text.contains("\$gv(side)/7\$"))
        assertTrue(text.contains("missing_global_adaptation"))
        assertTrue(text.contains("manual wallpaper"))
        assertTrue(text.contains("no filled dial card"))
        assertFalse(text.contains("aaumgupta", ignoreCase = true))
        assertFalse(text.contains("@"))
        assertFalse(text.contains("SINGLE_TAP"))
        assertFalse(text.contains(".ttf", ignoreCase = true))
        assertFalse(text.contains("ndot", ignoreCase = true))
        assertFalse(text.contains("font-family", ignoreCase = true))
    }

    @Test
    fun providerIsDistinctAndWiredThroughCatalogPinManifestAndUpdater() {
        val provider = repoFile(
            "src/main/java/com/dotwelwidgets/dotwellabs",
            "KwgtAnalog5TransparentWidgetProvider.kt",
        ).readText()
        assertTrue(provider.contains("class KwgtAnalog5TransparentWidgetProvider : BaseWidgetProvider()"))
        assertFalse(provider.contains("KwgtAnalog5WidgetProvider()"))
        assertTrue(provider.contains("widget_kwgt_analog5clear_black"))
        assertTrue(provider.contains("widget_kwgt_analog5clear_translucent"))
        assertTrue(provider.contains("widget_kwgt_analog5clear_transparent"))

        val catalog = repoFile(
            "src/main/java/com/dotwelwidgets/dotwellabs",
            "WidgetCatalog.kt",
        ).readText()
        assertTrue(catalog.contains("\"kwgt_nothing_analog5_transparent\""))
        assertTrue(catalog.contains("KwgtAnalog5TransparentWidgetProvider::class.java"))
        assertTrue(catalog.contains("PIN_KIND_KWGT_ANALOG_5_CLEAR"))

        val pin = repoFile(
            "src/main/java/com/dotwelwidgets/dotwellabs",
            "PinResultReceiver.kt",
        ).readText()
        assertTrue(pin.contains("PIN_KIND_KWGT_ANALOG_5_CLEAR = \"kwgt_analog_5_clear\""))

        val updater = repoFile(
            "src/main/java/com/dotwelwidgets/dotwellabs",
            "WidgetUpdater.kt",
        ).readText()
        assertTrue(updater.contains("KwgtAnalog5TransparentWidgetProvider::class.java"))

        val manifest = repoFile("src/main", "AndroidManifest.xml").readText()
        assertTrue(manifest.contains(".KwgtAnalog5TransparentWidgetProvider"))
        assertTrue(manifest.contains("@xml/kwgt_analog5clear_widget_info"))

        val info = parse("xml", "kwgt_analog5clear_widget_info.xml").documentElement
        assertEquals("@layout/widget_kwgt_analog5clear_black", info.getAttribute("android:initialLayout"))
        assertEquals("@layout/widget_kwgt_analog5clear_black", info.getAttribute("android:previewLayout"))
    }

    @Test
    fun handsReuseTheSourceIdenticalAnalog5Vectors() {
        val hour = parse("drawable", "dw_analog5_hour.xml").documentElement
        val hourPath = hour.getElementsByTagName("path").item(0) as Element
        assertEquals("#FFD71921", hourPath.getAttribute("android:fillColor"))
        listOf("white" to "#FFFFFFFF", "dark" to "#FF1B1B1D").forEach { (name, ink) ->
            val minute = parse("drawable", "dw_analog5_minute_$name.xml").documentElement
            val path = minute.getElementsByTagName("path").item(0) as Element
            assertEquals(ink, path.getAttribute("android:fillColor"))
        }
    }

    private fun dialPath(dial: String, groupIndex: Int): String {
        val vector = parse("drawable", "$dial.xml").documentElement
        val group = vector.getElementsByTagName("group").item(groupIndex) as Element
        return (group.getElementsByTagName("path").item(0) as Element)
            .getAttribute("android:pathData")
    }

    private fun tickSubpaths(data: String): List<List<Pair<Double, Double>>> =
        data.split("M").drop(1).mapNotNull { subpath ->
            if (subpath.contains("A")) {
                null
            } else {
                Regex("(-?\\d+(?:\\.\\d+)?),(-?\\d+(?:\\.\\d+)?)")
                    .findAll(subpath)
                    .map { it.groupValues[1].toDouble() to it.groupValues[2].toDouble() }
                    .toList()
                    .takeIf { it.size == 4 }
            }
        }

    private fun localId(element: Element): String =
        element.getAttribute("android:id").removePrefix("@+id/")

    private fun parse(folder: String, name: String) =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(repoFile("src/main/res/$folder", name))

    private fun repoFile(relativeDir: String, name: String): File {
        val candidates = listOf(
            File("$relativeDir/$name"),
            File("app/$relativeDir/$name"),
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
