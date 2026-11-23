package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure lifecycle assertions for the bounded battery refresh. These lock the
 * inexact period/flex window, the exact provider scope and the internal alarm
 * action without needing an Android host.
 */
class BatteryRefreshSchedulerTest {
    @Test
    fun refreshWindowIsBoundedAndInexact() {
        assertEquals(15 * 60 * 1000L, BatteryRefreshScheduler.REFRESH_PERIOD_MILLIS)
        assertEquals(10 * 60 * 1000L, BatteryRefreshScheduler.REFRESH_FLEX_MILLIS)
        val now = 1_000_000L
        assertEquals(
            now + BatteryRefreshScheduler.REFRESH_PERIOD_MILLIS,
            BatteryRefreshScheduler.nextTriggerElapsed(now),
        )
        assertTrue(BatteryRefreshScheduler.REFRESH_FLEX_MILLIS > 0L)
    }

    @Test
    fun onlyBatteryProvidersAreScheduled() {
        assertEquals(
            listOf(
                BatteryWidgetProvider::class.java,
                BatteryMiniWidgetProvider::class.java,
                KwgtBattery2WidgetProvider::class.java,
                KwgtCleanInfoWidgetProvider::class.java,
            ),
            BatteryRefreshScheduler.providers,
        )
    }

    @Test
    fun alarmActionIsScopedToTheInternalReceiver() {
        assertEquals(
            "com.dotwelwidgets.dotwellabs.action.BATTERY_REFRESH",
            BatteryRefreshScheduler.ACTION_REFRESH,
        )
        assertTrue(
            BatteryRefreshScheduler.isRefreshAction(
                BatteryRefreshScheduler.ACTION_REFRESH,
            ),
        )
        assertFalse(
            BatteryRefreshScheduler.isRefreshAction(
                "android.intent.action.BATTERY_CHANGED",
            ),
        )
        assertFalse(BatteryRefreshScheduler.isRefreshAction(null))
    }
}
