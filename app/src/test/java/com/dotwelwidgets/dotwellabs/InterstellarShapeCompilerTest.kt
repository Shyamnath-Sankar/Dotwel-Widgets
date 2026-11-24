package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InterstellarShapeCompilerTest {
    @Test
    fun evaluatesSourceSideDivisorFormulas() {
        assertEquals(18f, InterstellarShapeCompiler.eval("\$gv(side)/(277/18)\$", 277f)!!, 0.001f)
        assertEquals(1.5f, InterstellarShapeCompiler.eval("\$gv(side)/(277/1.5)\$", 277f)!!, 0.001f)
        assertEquals(138.5f, InterstellarShapeCompiler.eval("\$gv(side)/2\$", 277f)!!, 0.001f)
    }

    @Test
    fun evaluatesNegativeMarginsAndConstants() {
        assertEquals(-18f, InterstellarShapeCompiler.eval("-\$gv(side)/(277/18)\$", 277f)!!, 0.001f)
        assertEquals(15f, InterstellarShapeCompiler.eval("15.0", 277f)!!, 0.001f)
    }

    @Test
    fun unsupportedExpressionsAreSkippedInsteadOfGuessed() {
        assertNull(InterstellarShapeCompiler.eval("CLOCK_SECOND", 277f))
        assertNull(InterstellarShapeCompiler.eval("", 277f))
        assertNull(InterstellarShapeCompiler.eval(null, 277f))
    }
}
