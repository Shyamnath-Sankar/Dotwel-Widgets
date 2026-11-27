package com.dotwelwidgets.dotwellabs

import com.dotwelwidgets.dotwellabs.media.MediaTouchGeometry
import com.dotwelwidgets.dotwellabs.media.MediaWidgetKind
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class AwesomeBatchLogicTest {
    @Test fun sourceWeatherKindsAndPageCountsStayDistinct() {
        assertEquals((0..8).toList(),NtWxRenderer.Kind.values().map { it.sourceKind })
        assertEquals(listOf(3,2,1,1,1,1,1,1,1),NtWxRenderer.Kind.values().map { it.pages })
        assertEquals(2,NtWxRenderer.page(NtWxRenderer.Kind.CIRCLE,-1))
        assertEquals(0,NtWxRenderer.page(NtWxRenderer.Kind.SQUARE,2))
    }
    @Test fun everyReadyProviderCanBeReachedFromACategory() {
        val ids=WidgetCategory.values().flatMap { it.entryIds }.toSet()
        WidgetCatalog.entries.filter { it.state==ImportState.READY && it.providerClass!=null }.forEach {
            assertTrue("Gallery category missing ${it.id}",it.id in ids)
        }
    }
    @Test fun airQualityUnknownIsNotZero() {
        assertEquals(0,NtWxExtraRenderer.airBand(null))
        assertEquals(0,NtWxExtraRenderer.airBand(-1))
        assertEquals(1,NtWxExtraRenderer.airBand(0))
        assertEquals(1,NtWxExtraRenderer.airBand(50))
        assertEquals(2,NtWxExtraRenderer.airBand(51))
        assertEquals(6,NtWxExtraRenderer.airBand(301))
    }
    @Test fun awayOnlyAddsAnObservedOffToOnInterval() {
        val start=1_790_000_000_000L
        val initial=NtAwayState.Sample(lastOn=start)
        val off=NtAwayState.update(initial,false,start+60_000)
        assertEquals(0L,off.total)
        val on=NtAwayState.update(off,true,start+180_000)
        assertEquals(180_000L,on.total)
        assertEquals(180_000L,on.longest)
        assertEquals(180_000L,on.last)
        assertEquals(on.total,NtAwayState.update(on,true,start+240_000).total)
        assertEquals("1h 5m",NtAwayState.format(65*60_000L))
        assertEquals("0m",NtAwayState.format(-1))
    }
    @Test fun mediaHitAreasMapTheSameDesignSpaceAsTheBitmap() {
        for(kind in MediaWidgetKind.values()) {
            val control=MediaTouchGeometry.design(kind)
            for(box in listOfNotNull(control.previous,control.play,control.next,control.clear,control.volumeUp,control.volumeDown)) {
                assertTrue(box.right>box.left)
                assertTrue(box.bottom>box.top)
                val mapped=MediaTouchGeometry.map(box,kind,180f,360f)
                assertTrue(mapped.left>=-1)
                assertTrue(mapped.top>=-1)
                assertTrue(mapped.right<=181)
                assertTrue(mapped.bottom<=361)
            }
        }
        assertNull(MediaTouchGeometry.design(MediaWidgetKind.NOTIFICATION).play)
        assertNotNull(MediaTouchGeometry.design(MediaWidgetKind.MINI).next)
        assertNotNull(MediaTouchGeometry.design(MediaWidgetKind.MINI).previous)
        assertNotNull(MediaTouchGeometry.design(MediaWidgetKind.NOTHING_PLAYER_3).volumeUp)
        assertNotNull(MediaTouchGeometry.design(MediaWidgetKind.NOTHING_PLAYER_3).volumeDown)
    }
    @Test fun newProvidersDoNotFetchFromReceiverPathsOrAskForBroadPhotoAccess() {
        val weather=File("src/main/java/com/dotwelwidgets/dotwellabs/NtWxWidgetProviders.kt").readText()
        assertFalse(weather.contains("WeatherRepository"))
        val photo=File("src/main/java/com/dotwelwidgets/dotwellabs/NtPhotoWidgets.kt").readText()
        assertTrue(photo.contains("ACTION_GET_CONTENT"))
        assertFalse(photo.contains("READ_MEDIA_IMAGES"))
        assertFalse(photo.contains("READ_EXTERNAL_STORAGE"))
        assertTrue(photo.contains("override fun onDeleted"))
    }
}
