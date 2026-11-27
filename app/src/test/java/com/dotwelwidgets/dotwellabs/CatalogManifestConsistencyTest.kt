package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class CatalogManifestConsistencyTest {
    @Test
    fun everyRegisteredCatalogProviderHasAManifestReceiver() {
        val receivers = manifestReceivers()
        WidgetCatalog.entries
            .filter { it.providerClass != null }
            .forEach { entry ->
                val name = ".${entry.providerClass!!.simpleName}"
                assertTrue(
                    "missing receiver for ${entry.id} ($name)",
                    name in receivers,
                )
            }
    }

    @Test
    fun catalogContainsTheBatchProviderCountWithoutStubs() {
        val registered = WidgetCatalog.entries.filter { it.providerClass != null }
        assertEquals(62, registered.size)
        assertEquals(registered.size, registered.map { it.id }.toSet().size)
        assertTrue(registered.all { it.descriptionRes != 0 })
        assertTrue(registered.all { it.pinKind != null })
    }

    private fun manifestReceivers(): Set<String> {
        val file = listOf(
            File("src/main/AndroidManifest.xml"),
            File("app/src/main/AndroidManifest.xml"),
        ).firstOrNull { it.isFile } ?: throw AssertionError("manifest not found")
        val root = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(file)
            .documentElement
        val result = mutableSetOf<String>()
        val nodes = root.getElementsByTagName("receiver")
        for (index in 0 until nodes.length) {
            val element = nodes.item(index) as Element
            val name = element.getAttribute("android:name")
            if (name.isNotBlank()) result += name
        }
        return result
    }
}
