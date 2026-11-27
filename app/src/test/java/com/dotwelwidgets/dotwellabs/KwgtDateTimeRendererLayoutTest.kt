package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min

class KwgtDateTimeRendererLayoutTest {
    private val texts = listOf("WED", "10:30PM", "SEPTEMBER", "23RD")

    @Test
    fun rowsDoNotOverlapAndStayInsideHeight() {
        listOf(200 to 200, 300 to 400, 400 to 200).forEach { (width, height) ->
            val side = min(width, height).toFloat()
            val rows = layout(width, height, side)
            val label = "${width}x$height"

            assertEquals(label, texts.size, rows.size)
            rows.forEachIndexed { index, row ->
                assertTrue("$label positive pitch", row.pitch > 0f)
                if (index < rows.lastIndex) {
                    assertTrue("$label positive gap", row.gapAfter > 0f)
                }
                assertTrue("$label inside top", row.top >= 0f)
                assertTrue("$label inside bottom", row.bottom <= height.toFloat() + 0.01f)
            }
            rows.zipWithNext().forEach { (upper, lower) ->
                assertTrue(
                    "$label overlap ${upper.bottom} > ${lower.top}",
                    upper.bottom <= lower.top + 0.01f,
                )
            }
        }
    }

    @Test
    fun longMonthNamesFitTheWidth() {
        listOf("NOVEMBER", "SEPTEMBER").forEach { month ->
            listOf(200 to 200, 300 to 400, 400 to 200).forEach { (width, height) ->
                val side = min(width, height).toFloat()
                val rows = layout(width, height, side, month)
                val label = "$month ${width}x$height"
                val left = side / 8.5f
                rows.forEachIndexed { index, row ->
                    val columns = DotGlyphFont.columnsFor(
                        if (index == 2) month else texts[index],
                    )
                    val right = left + (0.5f + (columns - 1) + 0.4f) * row.pitch
                    assertTrue("$label right $right > $width", right <= width.toFloat() + 0.01f)
                }
            }
        }
    }

    private fun layout(
        width: Int,
        height: Int,
        side: Float,
        month: String = "SEPTEMBER",
    ): List<KwgtDateTimeRenderer.FittedRow> {
        val rowTexts = texts.toMutableList().also { it[2] = month }
        return KwgtDateTimeRenderer.fitRows(
            texts = rowTexts,
            baseTextSizes = listOf(side / 7f, side / 6f, side / 7f, side / 7f),
            side = side,
            width = width.toFloat(),
            height = height.toFloat(),
        )
    }
}
