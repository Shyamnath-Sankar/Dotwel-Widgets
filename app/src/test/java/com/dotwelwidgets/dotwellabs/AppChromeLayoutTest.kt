package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards the reference-matched app chrome: pure black canvas, the
 * LIVE_SYSTEM_DISPLAY / DW-MOD-01 top bar, dot-matrix headline and subtitle,
 * the centred footer, and the light rounded category cards.
 */
class AppChromeLayoutTest {
    @Test
    fun canvasIsPureBlack() {
        val colors = parse("src/main/res/values/colors.xml").documentElement
        val black = colors.byTag("color", "name", "dotwel_black")
        assertEquals("#000000", black.textContent.trim().uppercase())

        val theme = parse("src/main/res/values/themes.xml").documentElement
        val window = theme.getElementsByTagName("item").asElements()
            .firstOrNull { it.getAttribute("name") == "android:windowBackground" }
        assertNotNull("Theme.Dotwel must set a window background", window)
        assertEquals("@color/dotwel_black", window!!.textContent.trim())
    }

    @Test
    fun topBarCarriesTheReferenceLabels() {
        val strings = parse("src/main/res/values/strings.xml").documentElement
        assertEquals(
            "LIVE_SYSTEM_DISPLAY",
            strings.byTag("string", "name", "chrome_live_label").textContent,
        )
        assertEquals(
            "ID: DW-MOD-01",
            strings.byTag("string", "name", "chrome_id_label").textContent,
        )
        assertEquals(
            "DOTWEL WIDGETS",
            strings.byTag("string", "name", "chrome_footer").textContent,
        )

        val activity = parse("src/main/res/layout/activity_main.xml").documentElement
        assertNotNull("top bar view is missing", activity.byId("top_bar"))
        assertNotNull("headline view is missing", activity.byId("chrome_headline"))
        assertNotNull("subtitle view is missing", activity.byId("chrome_subtitle"))
        assertNotNull("footer view is missing", activity.byId("chrome_footer"))
    }

    @Test
    fun headlineUsesDotoWhiteAndSubtitleUsesDotoRed() {
        val activity = parse("src/main/res/layout/activity_main.xml").documentElement
        val headline = activity.byId("chrome_headline")!!
        assertEquals("@font/doto", headline.getAttribute("android:fontFamily"))
        assertEquals("@color/dotwel_white", headline.getAttribute("android:textColor"))

        val subtitle = activity.byId("chrome_subtitle")!!
        assertEquals("@font/doto", subtitle.getAttribute("android:fontFamily"))
        assertEquals("@color/dot_accent", subtitle.getAttribute("android:textColor"))

        val footer = activity.byId("chrome_footer")!!
        assertEquals("center", footer.getAttribute("android:gravity"))
        assertEquals("monospace", footer.getAttribute("android:fontFamily"))
        assertEquals("@color/dotwel_gray", footer.getAttribute("android:textColor"))
    }

    @Test
    fun categoryCardsAreLightAndRoundedTwentyFourDp() {
        val tile = parse("src/main/res/drawable/bg_category_tile.xml").documentElement
        val solid = tile.byTag("solid", "android:color", null)
        assertEquals("#FEF7FF", solid.getAttribute("android:color").uppercase())
        val corners = tile.byTag("corners", "android:radius", null)
        assertEquals("24dp", corners.getAttribute("android:radius"))
    }

    @Test
    fun dotoFontAndItsOflLicenceAreBundled() {
        val font = repoFile("src/main/res/font/doto.ttf")
        assertTrue("Doto font is missing", font.isFile)
        assertTrue("Doto font looks empty", font.length() > 10_000)
        val header = font.inputStream().use { stream ->
            ByteArray(4).also { stream.read(it) }
        }
        assertTrue(
            "Doto font is not a TrueType font",
            header.contentEquals(byteArrayOf(0, 1, 0, 0)),
        )
        val licence = repoFile("src/main/res/raw/doto_ofl.txt")
        assertTrue("Doto OFL licence is missing", licence.isFile)
        assertTrue(
            "OFL licence text is wrong",
            licence.readText().contains("SIL OPEN FONT LICENSE"),
        )
    }

    private fun parse(relative: String): org.w3c.dom.Document =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(repoFile(relative))

    private fun repoFile(relative: String): File =
        listOf(File(relative), File("app/$relative"))
            .firstOrNull { it.isFile }
            ?: File(relative)

    private fun org.w3c.dom.Element.byId(id: String): Element? =
        getElementsByTagName("*").asElements()
            .firstOrNull { it.getAttribute("android:id") == "@+id/$id" }

    private fun org.w3c.dom.Document.byTag(
        tag: String,
        attribute: String?,
        value: String?,
    ): Element {
        val matches = getElementsByTagName(tag).asElements()
        val element = if (attribute == null || value == null) {
            matches.firstOrNull()
        } else {
            matches.firstOrNull { it.getAttribute(attribute) == value }
        }
        return element ?: throw AssertionError("missing <$tag $attribute=$value>")
    }

    private fun org.w3c.dom.Element.byTag(
        tag: String,
        attribute: String?,
        value: String?,
    ): Element {
        val matches = getElementsByTagName(tag).asElements()
        val element = if (attribute == null || value == null) {
            matches.firstOrNull()
        } else {
            matches.firstOrNull { it.getAttribute(attribute) == value }
        }
        return element ?: throw AssertionError("missing <$tag $attribute=$value>")
    }

    private fun org.w3c.dom.NodeList.asElements(): List<Element> =
        (0 until length).mapNotNull { item(it) as? Element }
}
