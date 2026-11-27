package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaUpdateThrottleTest {
    private var now = 1_000L
    private val throttle = MediaUpdateThrottle(500L) { now }

    @Test
    fun firstRequestRunsImmediately() {
        assertEquals(0L, throttle.nextDelayMillis())
    }

    @Test
    fun requestsInsideWindowWaitForTheRemainder() {
        throttle.markRun()
        now += 120L
        assertEquals(380L, throttle.nextDelayMillis())
        now += 379L
        assertEquals(1L, throttle.nextDelayMillis())
        now += 1L
        assertEquals(0L, throttle.nextDelayMillis())
    }

    @Test
    fun aNewRunRestartsTheWindow() {
        throttle.markRun()
        now += 500L
        throttle.markRun()
        now += 250L
        assertEquals(250L, throttle.nextDelayMillis())
    }
}
