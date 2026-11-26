package com.dotwelwidgets.dotwellabs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InterstellarLayerCacheTest {
    private fun cache(maxBytes: Long) =
        BoundedLayerCache<String, String>(maxBytes) { it.length.toLong() }

    @Test
    fun evictsLeastRecentlyUsedWhenOverBudget() {
        val cache = cache(6)
        assertTrue(cache.put("a", "aaa"))
        assertTrue(cache.put("b", "bbb"))
        assertEquals("aaa", cache.get("a"))
        assertTrue(cache.put("c", "ccc"))
        assertNull(cache.get("b"))
        assertEquals("aaa", cache.get("a"))
        assertEquals("ccc", cache.get("c"))
        assertEquals(2, cache.size())
    }

    @Test
    fun doesNotCacheEntryLargerThanBudget() {
        val cache = cache(2)
        assertFalse(cache.put("big", "xxxx"))
        assertNull(cache.get("big"))
        assertEquals(0, cache.size())
        assertEquals(0L, cache.bytes())
    }

    @Test
    fun replacingAKeyDoesNotDoubleCountBytes() {
        val cache = cache(6)
        assertTrue(cache.put("a", "aaa"))
        assertTrue(cache.put("a", "aaaa"))
        assertEquals(1, cache.size())
        assertEquals(4L, cache.bytes())
        assertEquals("aaaa", cache.get("a"))
    }
}
