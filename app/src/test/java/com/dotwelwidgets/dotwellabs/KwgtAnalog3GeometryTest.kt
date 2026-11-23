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
 * Pure geometry assertions for the Nothing_Analog3 port. The source is only a
 * filled side-diameter circle plus two hollow capsule hands, so these tests
 * lock the author side divisors and the actual vector path bounds.
 */
class KwgtAnalog3GeometryTest {
    private val eps = 0.01

    @Test
    fun sourceHandProportionsUseTheActualPillDivisors() {
        val minute = KwgtAnalog3Geometry.minute
        val hour = KwgtAnalog3Geometry.hour
        val side = KwgtAnalog3Geometry.REFERENCE_SIDE

        assertEquals(side / 13.85, minute.width, eps)
        assertEquals(side / 13.85, hour.width, eps)
        assertEquals(side / 2.475, minute.height, eps)
        assertEquals(side / 3.3765, hour.height, eps)
        assertEquals(side / 138.5, minute.strokeWidth, eps)
        assertEquals(minute.strokeWidth, hour.strokeWidth, eps)
        assertEquals(side / 2.0, minute.centerX, eps)
        assertEquals(side / 2.0, hour.centerX, eps)
    }

    @Test
    fun paddingShiftsEachPillCenterUpByHalfTheSourcePadding() {
        listOf(
            KwgtAnalog3Geometry.minute to KwgtAnalog3Geometry.REFERENCE_SIDE / 2.988,
            KwgtAnalog3Geometry.hour to KwgtAnalog3Geometry.REFERENCE_SIDE / 4.4675,
        ).forEach { (pill, padding) ->
            val pivot = KwgtAnalog3Geometry.REFERENCE_SIDE / 2.0
            assertTrue(pill.centerY < pivot)
            assertEquals(pivot - padding / 2.0, pill.centerY, eps)
            assertEquals(pill.centerY - pill.height / 2.0, pill.top, eps)
            assertEquals(pill.centerY + pill.height / 2.0, pill.bottom, eps)
        }
    }

    @Test
    fun minuteHandReachesFurtherAndBothCapsShareThePivotTail() {
        val minute = KwgtAnalog3Geometry.minute
        val hour = KwgtAnalog3Geometry.hour
        val pivot = KwgtAnalog3Geometry.REFERENCE_SIDE / 2.0

        assertTrue("minute reaches higher", minute.top < hour.top)
        assertTrue("hour is shorter", hour.height < minute.height)
        assertTrue("minute extends below the pivot", minute.bottom > pivot)
        assertTrue("hour extends below the pivot", hour.bottom > pivot)
        assertEquals("shared capsule tail", minute.bottom, hour.bottom, 0.5)
        assertEquals("same gauge width", minute.width, hour.width, eps)
    }

    @Test
    fun handVectorsMatchTheDerivedPillBounds() {
        val expectations = listOf(
            "kwgt_analog3_hand_minute_light" to KwgtAnalog3Geometry.minute,
            "kwgt_analog3_hand_minute_dark" to KwgtAnalog3Geometry.minute,
            "kwgt_analog3_hand_hour_light" to KwgtAnalog3Geometry.hour,
            "kwgt_analog3_hand_hour_dark" to KwgtAnalog3Geometry.hour,
        )
        val pivot = KwgtAnalog3Geometry.REFERENCE_SIDE / 2.0
        expectations.forEach { (name, pill) ->
            val vector = parse("drawable", "$name.xml").documentElement
            assertEquals("$name viewport width", "277", vector.getAttribute("android:viewportWidth"))
            assertEquals(
                "$name viewport height",
                "277",
                vector.getAttribute("android:viewportHeight"),
            )
            assertEquals(
                "$name square intrinsic",
                vector.getAttribute("android:width"),
                vector.getAttribute("android:height"),
            )
            val path = vector.childElements().first { it.tagName == "path" }
            assertEquals("$name source stroke", "2", path.getAttribute("android:strokeWidth"))
            assertEquals(
                "$name hollow fill",
                "@android:color/transparent",
                path.getAttribute("android:fillColor"),
            )
            val endpoints = endpointCoordinates(path.getAttribute("android:pathData"))
            assertEquals("$name left", pill.left, endpoints.minOf { it.first }, eps)
            assertEquals("$name right", pill.right, endpoints.maxOf { it.first }, eps)
            assertEquals("$name top", pill.top, endpoints.minOf { it.second }, eps)
            assertEquals("$name bottom", pill.bottom, endpoints.maxOf { it.second }, eps)
            assertFalse(
                "$name stays inside the source circle",
                endpoints.any {
                    hypot(it.first - pivot, it.second - pivot) > pivot + pill.strokeWidth
                },
            )
        }
    }

    @Test
    fun dialVectorsFillTheFullReferenceCanvas() {
        val expectations = mapOf(
            "kwgt_analog3_dial_dark" to "#FF1B1B1D",
            "kwgt_analog3_dial_light" to "#FFFFFFFF",
            "kwgt_analog3_dial_translucent" to "#A6FFFFFF",
            "kwgt_analog3_dial_none" to "#00000000",
        )
        expectations.forEach { (name, fill) ->
            val vector = parse("drawable", "$name.xml").documentElement
            assertEquals("$name viewport width", "277", vector.getAttribute("android:viewportWidth"))
            assertEquals(
                "$name viewport height",
                "277",
                vector.getAttribute("android:viewportHeight"),
            )
            val path = vector.childElements().first { it.tagName == "path" }
            assertEquals("$name fill", fill, path.getAttribute("android:fillColor"))
            assertTrue(
                "$name side-diameter circle",
                path.getAttribute("android:pathData").contains("138.5,138.5"),
            )
        }
    }

    private fun endpointCoordinates(pathData: String): List<Pair<Double, Double>> {
        val tokens = Regex("([MLAZmlaz])|(-?\\d+(?:\\.\\d+)?)")
            .findAll(pathData)
            .map { it.value }
            .toList()
        val points = mutableListOf<Pair<Double, Double>>()
        var index = 0
        while (index < tokens.size) {
            when (tokens[index].uppercase()) {
                "M", "L" -> {
                    points += tokens[index + 1].toDouble() to tokens[index + 2].toDouble()
                    index += 3
                }
                "A" -> {
                    points += tokens[index + 6].toDouble() to tokens[index + 7].toDouble()
                    index += 8
                }
                else -> index++
            }
        }
        return points
    }

    private fun parse(directory: String, name: String): org.w3c.dom.Document =
        DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(resourceFile(directory, name))

    private fun resourceFile(directory: String, name: String): File {
        val candidates = listOf(
            File("src/main/res/$directory/$name"),
            File("app/src/main/res/$directory/$name"),
        )
        return candidates.firstOrNull { it.isFile }
            ?: throw AssertionError(
                "cannot find $directory/$name from ${File(".").absolutePath}",
            )
    }

    private fun Element.childElements(): List<Element> {
        val elements = mutableListOf<Element>()
        val nodes = childNodes
        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            if (node is Element) elements += node
        }
        return elements
    }
}
