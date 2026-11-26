package com.dotwelwidgets.dotwellabs

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import com.dotwelwidgets.dotwellabs.weather.GeoLocation
import com.dotwelwidgets.dotwellabs.weather.TemperatureUnit
import com.dotwelwidgets.dotwellabs.weather.WeatherCache
import com.dotwelwidgets.dotwellabs.weather.WeatherRenderState
import com.dotwelwidgets.dotwellabs.weather.WeatherSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Regression for the API 31+ combined RemoteViews crash: a multi-page NtWx
 * widget with a weather snapshot used to add its page-cycling click intent after
 * `buildBitmapViews` had already combined the per-size views, which throws
 * "RemoteViews specifying separate layouts for orientation or size cannot be
 * modified".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class NtWxCombinedRemoteViewsTest {
    private val location = GeoLocation("Fixture", "IN", null, 11.0, 77.0, "Asia/Kolkata")
    private val forecast = """
        {"timezone":"Asia/Kolkata",
         "current":{"temperature_2m":27.0,"apparent_temperature":28.0,"weather_code":1,"is_day":1,"time":"2026-06-01T10:00"},
         "daily":{"time":["2026-06-01"],"weather_code":[1],"temperature_2m_max":[30.0],"temperature_2m_min":[21.0],"sunrise":["2026-06-01T05:00"],"sunset":["2026-06-01T19:00"]}}
    """.trimIndent()

    private fun seedWeather(context: Context) {
        WeatherSettings.setEnabled(context, true)
        WeatherSettings.setUnit(context, TemperatureUnit.CELSIUS)
        WeatherSettings.setLocation(context, location)
        WeatherCache.save(
            context,
            forecast,
            location,
            TemperatureUnit.CELSIUS,
            System.currentTimeMillis(),
        )
    }

    private fun exactSizedWidget(
        context: Context,
        provider: Class<out android.appwidget.AppWidgetProvider>,
    ): Int {
        val manager = AppWidgetManager.getInstance(context)
        val id = Shadows.shadowOf(manager)
            .createWidget(provider, R.layout.widget_weather_temp)
        manager.updateAppWidgetOptions(id, Bundle().apply {
            putParcelableArrayList(
                AppWidgetManager.OPTION_APPWIDGET_SIZES,
                arrayListOf(SizeF(180f, 180f), SizeF(360f, 180f)),
            )
        })
        return id
    }

    private fun buildViews(provider: BaseWidgetProvider, context: Context, id: Int): RemoteViews {
        val method = BaseWidgetProvider::class.java.getDeclaredMethod(
            "buildViews",
            Context::class.java,
            Int::class.javaPrimitiveType,
        )
        method.isAccessible = true
        return method.invoke(provider, context, id) as RemoteViews
    }

    private fun combined(views: RemoteViews): Boolean = try {
        views.setTextViewText(R.id.weather_temp_fallback, "probe")
        false
    } catch (_: RuntimeException) {
        true
    }

    @Test
    fun multiPageWeatherWithSnapshotBuildsCombinedViewsWithoutMutation() {
        val context = RuntimeEnvironment.getApplication()
        seedWeather(context)
        assertNotNull(WeatherRenderState.from(context).snapshot)
        val id = exactSizedWidget(context, NtWxCircleProvider::class.java)
        val provider = NtWxCircleProvider()
        val views = buildViews(provider, context, id)
        assertNotNull(views)
        assertTrue(
            "exact-size build must return the combined RemoteViews that used to crash",
            combined(views),
        )
    }

    @Test
    fun decorationHookRunsForEveryExactSizeVariant() {
        val context = RuntimeEnvironment.getApplication()
        val id = exactSizedWidget(context, CountingWeatherProvider::class.java)
        val provider = CountingWeatherProvider()
        buildViews(provider, context, id)
        assertEquals(2, provider.decorated)
    }

    private class CountingWeatherProvider : BaseWeatherWidgetProvider() {
        override val layoutRes: Int = R.layout.widget_weather_temp
        override val bitmapViewId: Int = R.id.weather_temp_bitmap
        override val fallbackViewId: Int = R.id.weather_temp_fallback
        var decorated = 0

        override fun render(
            context: Context,
            widthPx: Int,
            heightPx: Int,
            style: WidgetStyle,
            state: WeatherRenderState,
        ): Bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)

        override fun decorateWeatherViews(
            views: RemoteViews,
            context: Context,
            appWidgetId: Int,
            usedBitmap: Boolean,
        ) {
            decorated += 1
        }
    }
}
