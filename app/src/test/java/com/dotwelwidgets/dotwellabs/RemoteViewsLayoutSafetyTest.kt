package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Regression guard for the Android 15 launcher "Couldn't add widget" failure:
 * RemoteViews only inflates allowlisted widget classes, so a raw <View> divider
 * failed preview and placement inflation. These assertions read the shipped
 * provider layouts directly and keep every tag on the supported widget list.
 */
class RemoteViewsLayoutSafetyTest {
    private val providerLayouts = listOf(
        "widget_kwgt_digital3.xml",
        "widget_kwgt_digital4.xml",
        "widget_kwgt_datetime.xml",
        "widget_kwgt_datetime2.xml",
        "widget_kwgt_gradient_clock.xml",
        "widget_kwgt_mini_calendar.xml",
        "widget_kwgt_battery2.xml",
        "widget_kwgt_analog3_black.xml",
        "widget_kwgt_analog3_translucent.xml",
        "widget_kwgt_analog3_transparent.xml",
    )

    private val remoteViewsSafeTags = setOf(
        "FrameLayout",
        "LinearLayout",
        "RelativeLayout",
        "GridLayout",
        "TextClock",
        "TextView",
        "ImageView",
        "ImageButton",
        "Button",
        "Chronometer",
        "AnalogClock",
        "ProgressBar",
        "ViewFlipper",
        "AdapterViewFlipper",
        "ListView",
        "GridView",
        "StackView",
        "ViewStub",
    )

    @Test
    fun providerLayoutsOnlyUseRemoteViewsSupportedTags() {
        providerLayouts.forEach { name ->
            val root = parse(name).documentElement
            root.childElementsRecursively().map { it.tagName }.toSet().forEach { tag ->
                assertTrue(
                    "$name uses <$tag>, which RemoteViews cannot inflate",
                    tag in remoteViewsSafeTags,
                )
            }
        }
    }

    @Test
    fun splitClockDividerStaysAStaticOneDpImageView() {
        val root = parse("widget_kwgt_digital4.xml").documentElement
        val divider = root.childElementsRecursively()
            .firstOrNull { it.getAttribute("android:id") == "@+id/kwgt_digital4_divider" }
            ?: throw AssertionError("split clock divider is missing")

        assertEquals("ImageView", divider.tagName)
        assertEquals("1dp", divider.getAttribute("android:layout_width"))
        assertEquals("match_parent", divider.getAttribute("android:layout_height"))
        assertEquals("@color/dot_accent", divider.getAttribute("android:background"))
    }

    private fun parse(name: String) =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(layoutFile(name))

    private fun layoutFile(name: String): File {
        val candidates = listOf(
            File("src/main/res/layout/$name"),
            File("app/src/main/res/layout/$name"),
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
