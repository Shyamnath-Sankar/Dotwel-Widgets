package com.dotwelwidgets.dotwellabs

import android.content.Context
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class InterstellarLayerCacheRenderTest {
    @Test
    fun repeatedRenderReusesCompiledLayers() {
        val context: Context = RuntimeEnvironment.getApplication()
        val now = Calendar.getInstance()
        val variant = InterstellarShapeCompiler.Variant.ANALOG_2
        val style = WidgetStyle.TRANSPARENT
        val before = InterstellarShapeCompiler.cachedLayerCount()

        val first = InterstellarShapeCompiler.render(context, variant, 301, 257, style, now)
        val afterFirst = InterstellarShapeCompiler.cachedLayerCount()
        val second = InterstellarShapeCompiler.render(context, variant, 301, 257, style, now)
        val afterSecond = InterstellarShapeCompiler.cachedLayerCount()

        assertNotNull(first)
        assertNotNull(second)
        assertEquals(301, first.width)
        assertEquals(257, first.height)
        assertTrue(afterFirst >= before + 3)
        assertEquals(afterFirst, afterSecond)
    }
}
