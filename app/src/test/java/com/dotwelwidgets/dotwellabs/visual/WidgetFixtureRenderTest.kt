package com.dotwelwidgets.dotwellabs.visual

import android.content.Context
import android.graphics.Bitmap
import com.dotwelwidgets.dotwellabs.*
import com.dotwelwidgets.dotwellabs.weather.*
import java.io.File
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Isolated fixture render only. This is not launcher placement/native acceptance. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetFixtureRenderTest {
    @Test fun renderSourceWeatherAndPrivacyOffStates() {
        val context:Context=RuntimeEnvironment.getApplication()
        val now=1_795_536_000_000L
        val city=GeoLocation("Fixture City","IN",null,11.0,77.0,"Asia/Kolkata")
        val date=WeatherDisplay.localDate(now,WeatherDisplay.zone(city))
        val days=(0..5).map { i -> WeatherDay(date.plusDays(i.toLong()).toString(),if(i%2==0)0 else 61,
            29.0+i,20.0,now-3_600_000,now+7_200_000,35) }
        val snapshot=WeatherSnapshot(city,TemperatureUnit.CELSIUS,
            WeatherCurrent(27.0,28.0,2,true,now),days,WeatherAirQuality(65,now),now)
        val ready=WeatherRenderState(snapshot,true,true,now)
        val off=WeatherRenderState(null,false,false,now)
        val output=File("build/fixture-pixels").apply { mkdirs() }
        fun save(bitmap:Bitmap,name:String) {
            File(output,"$name.png").outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
            bitmap.recycle()
        }
        for(kind in NtWxRenderer.Kind.values()) {
            for(style in WidgetStyle.values()) {
                val wide=kind in listOf(NtWxRenderer.Kind.RECT,NtWxRenderer.Kind.SUNSET,NtWxRenderer.Kind.AIR)
                val width=if(wide)360 else 240
                val height=if(wide)180 else 240
                for(page in 0 until kind.pages) save(NtWxRenderer.render(context,kind,width,height,style,ready,page,DotwelWeatherIcons),"wx-${kind.name}-${style.name}-$page")
                save(NtWxRenderer.render(context,kind,width,height,style,off,0,DotwelWeatherIcons),"wx-off-${kind.name}-${style.name}")
            }
        }
        val photo=Bitmap.createBitmap(420,240,Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(photo).apply {
            drawColor(android.graphics.Color.BLUE)
            drawRect(0f,0f,140f,240f,android.graphics.Paint().apply { color=android.graphics.Color.RED })
            drawRect(280f,0f,420f,240f,android.graphics.Paint().apply { color=android.graphics.Color.GREEN })
        }
        NtPhotoRenderer.file(context,-2).outputStream().use { photo.compress(Bitmap.CompressFormat.JPEG,90,it) }
        photo.recycle()
        save(NtPhotoRenderer.render(context,-2,240,240,true,WidgetStyle.BLACK),"photo-fixture-circle")
        save(NtPhotoRenderer.render(context,-2,360,180,false,WidgetStyle.BLACK),"photo-fixture-rect")
        NtPhotoRenderer.file(context,-2).delete()
        val fixtureMedia=com.dotwelwidgets.dotwellabs.media.MediaSnapshot.noAccess().copy(
            accessGranted=true,hasSession=true,title="Fixture Track",artist="Fixture Artist",album="Fixture Album",
            isPlaying=true,positionMillis=65_000,durationMillis=240_000,canPlayPause=true,canSkipNext=true,
            canSkipPrevious=true,capturedAtMillis=now,volumeFraction=.6f,canAdjustVolume=true)
        for(kind in com.dotwelwidgets.dotwellabs.media.MediaWidgetKind.values()) {
            for(style in WidgetStyle.values()) {
                save(com.dotwelwidgets.dotwellabs.media.MediaRenderers.render(context,kind,300,240,style,
                    fixtureMedia,ready),"media-${kind.name}-${style.name}")
            }
        }
        save(NtPhotoRenderer.render(context,-1,240,240,true,WidgetStyle.BLACK),"photo-empty-circle")
        save(NtPhotoRenderer.render(context,-1,360,180,false,WidgetStyle.BLACK),"photo-empty-rect")
        for(kind in BatterySurfaceKind.values()) {
            val wide = kind in listOf(BatterySurfaceKind.TEXT_ARC, BatterySurfaceKind.SMALL_PROGRESSBAR)
            val width = if(wide) 360 else 240
            val height = if(wide) 180 else 240
            save(ReferenceBatterySurface.render(context,kind,width,height,24,false,WidgetStyle.BLACK),
                "battery-${kind.name}-low")
            save(ReferenceBatterySurface.render(context,kind,width,height,75,false,WidgetStyle.BLACK),
                "battery-${kind.name}-normal")
        }
    }
}
