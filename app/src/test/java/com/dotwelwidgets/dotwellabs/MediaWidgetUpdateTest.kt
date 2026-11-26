package com.dotwelwidgets.dotwellabs

import android.content.Context
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MediaWidgetUpdateTest {
    @Test
    fun noPlacedMediaWidgetMeansNoMediaWork() {
        val context: Context = RuntimeEnvironment.getApplication()
        assertFalse(WidgetUpdater.hasPlacedMediaWidgets(context))
    }
}
