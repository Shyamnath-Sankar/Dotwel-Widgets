package com.dotwelwidgets.dotwellabs.visual

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import com.dotwelwidgets.dotwellabs.*
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Local Android view inflation smoke test, NOT Pixel launcher/native placement proof. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProviderInflationTest {
    @Test fun registeredProvidersInflateAtSmallAndWideHostSizes() {
        val context:Context=RuntimeEnvironment.getApplication()
        val manager=AppWidgetManager.getInstance(context)
        val shadow=Shadows.shadowOf(manager)
        val output=File("build/provider-inflation").apply { mkdirs() }
        for(entry in WidgetCatalog.entries.filter { it.providerClass!=null }) {
            val clazz=entry.providerClass!!
            for(style in WidgetStyle.values()) {
                WidgetPrefs.setStyle(context,style)
                for((w,h) in listOf(180 to 180,360 to 180)) {
                    val id=shadow.createWidget(clazz,R.layout.widget_weather_temp)
                    manager.updateAppWidgetOptions(id,Bundle().apply {
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,w)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,w)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,h)
                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,h)
                    })
                    val provider=shadow.getAppWidgetProviderFor(id)
                    provider.onUpdate(context,manager,intArrayOf(id))
                    val view=shadow.getViewFor(id)
                    assertNotNull("${entry.id} must inflate",view)
                    view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY))
                    view.layout(0,0,w,h)
                    val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
                    view.draw(Canvas(bitmap))
                    File(output,"${entry.id}-${style.name}-$w.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
                    bitmap.recycle()
                    provider.onDeleted(context,intArrayOf(id))
                }
            }
        }
    }
}
