package com.dotwelwidgets.dotwellabs

import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure geometry and honesty assertions for the Nothing_Battery2 port. These do
 * not inflate Android views; they lock the source proportions, the sixteen
 * clipped segments, the level clamp and the unknown-estimate copy.
 */
class KwgtBattery2RendererLayoutTest {
    private val sizes = listOf(
        277f to 277f,
        415f to 542f,
        320f to 320f,
        392f to 500f,
        540f to 320f,
    )

    @Test
    fun geometryRetainsSourceProportionsAndFitsTheHost() {
        sizes.forEach { (width, height) ->
            val layout = KwgtBattery2Renderer.layout(width, height)
            val side = min(width, height)
            val scale = side / KwgtBattery2Renderer.REFERENCE_SIDE
            val label = "${width}x$height"

            assertEquals("$label scale", scale, layout.scale, EPS)
            assertEquals(
                "$label background corner",
                side / KwgtBattery2Renderer.BACKGROUND_CORNER_DIVISOR,
                layout.backgroundCorner,
                EPS,
            )
            assertEquals(
                "$label info offset",
                side / KwgtBattery2Renderer.INFO_OFFSET_DIVISOR,
                layout.infoCenterX - width / 2f,
                EPS,
            )
            assertEquals(
                "$label label size",
                side / KwgtBattery2Renderer.LABEL_SIZE_DIVISOR,
                layout.labelTextSize,
                EPS,
            )
            assertEquals(
                "$label body width",
                KwgtBattery2Renderer.BODY_WIDTH * scale,
                layout.body.width,
                EPS,
            )
            assertEquals(
                "$label body height",
                KwgtBattery2Renderer.BODY_HEIGHT * scale,
                layout.body.height,
                EPS,
            )
            assertEquals(
                "$label clip width",
                KwgtBattery2Renderer.CLIP_WIDTH * scale,
                layout.clip.width,
                EPS,
            )
            assertEquals(
                "$label clip height",
                KwgtBattery2Renderer.CLIP_HEIGHT * scale,
                layout.clip.height,
                EPS,
            )
            assertEquals(
                "$label progress width",
                KwgtBattery2Renderer.PROGRESS_WIDTH * scale,
                layout.progress.width,
                EPS,
            )
            assertEquals(
                "$label progress height",
                KwgtBattery2Renderer.PROGRESS_HEIGHT * scale,
                layout.progress.height,
                EPS,
            )
            assertEquals(
                "$label nub width",
                KwgtBattery2Renderer.NUB_WIDTH * scale,
                layout.nub.width,
                EPS,
            )
            assertEquals(
                "$label nub height",
                KwgtBattery2Renderer.NUB_HEIGHT * scale,
                layout.nub.height,
                EPS,
            )
            assertEquals(
                "$label nub margin",
                KwgtBattery2Renderer.NUB_MARGIN * scale,
                layout.nub.left - layout.body.right,
                EPS,
            )
            assertEquals(
                "$label level size",
                side / KwgtBattery2Renderer.LEVEL_SIZE_DIVISOR,
                layout.levelTextSize,
                EPS,
            )
            assertEquals(
                "$label percent size",
                side / KwgtBattery2Renderer.PERCENT_SIZE_DIVISOR,
                layout.percentTextSize,
                EPS,
            )
            assertEquals(
                "$label bottom size",
                side / KwgtBattery2Renderer.BOTTOM_SIZE_DIVISOR,
                layout.bottomTextSize,
                EPS,
            )

            assertTrue("$label background inside", layout.background.inside(width, height))
            assertTrue("$label body inside", layout.body.inside(width, height))
            assertTrue("$label clip inside body", layout.clip.inside(width, height))
            assertTrue("$label progress inside", layout.progress.inside(width, height))
            assertTrue("$label nub inside", layout.nub.inside(width, height))
            assertTrue(
                "$label clip is narrower than body",
                layout.clip.width < layout.body.width,
            )
            assertTrue(
                "$label progress is clipped",
                layout.progress.width > layout.clip.width,
            )
        }
    }

    @Test
    fun sixteenSegmentsClampAndTrackTheRealLevel() {
        val layout = KwgtBattery2Renderer.layout(320f, 320f)
        val segments = KwgtBattery2Renderer.segments(layout, 42)
        assertEquals(KwgtBattery2Renderer.SEGMENT_COUNT, segments.size)
        assertEquals(16, segments.size)
        segments.forEachIndexed { index, segment ->
            assertTrue("segment $index width", segment.width > 0f)
            assertTrue("segment $index height", segment.height > 0f)
            assertTrue("segment $index top", segment.top >= layout.progress.top - EPS)
            assertTrue("segment $index bottom", segment.bottom <= layout.progress.bottom + EPS)
            assertTrue("segment $index left", segment.left >= layout.progress.left - EPS)
            assertTrue("segment $index right", segment.right <= layout.progress.right + EPS)
            assertTrue(
                "segment $index fraction",
                segment.filledFraction in 0f..1f,
            )
        }
        val fills = KwgtBattery2Renderer.segmentFills(42)
        fills.take(6).forEach { assertEquals(1f, it, EPS) }
        assertEquals(0.72f, fills[6], 0.001f)
        fills.drop(7).forEach { assertEquals(0f, it, EPS) }

        KwgtBattery2Renderer.segmentFills(-20).forEach {
            assertEquals("negative level clamps to empty", 0f, it, EPS)
        }
        KwgtBattery2Renderer.segmentFills(140).forEach {
            assertEquals("over-100 level clamps to full", 1f, it, EPS)
        }
        KwgtBattery2Renderer.segmentFills(0).forEach {
            assertEquals("zero level", 0f, it, EPS)
        }
        KwgtBattery2Renderer.segmentFills(100).forEach {
            assertEquals("full level", 1f, it, EPS)
        }
    }

    @Test
    fun unknownEstimateNeverFabricatesHoursOrMinutes() {
        val discharging = KwgtBattery2Renderer.estimate(73, false, null)
        assertEquals(KwgtBattery2Renderer.EstimateKind.UNAVAILABLE, discharging.kind)
        assertEquals("--", discharging.valueText)
        assertEquals("ESTIMATE UNAVAILABLE", discharging.labelText)

        val chargingWithoutValue = KwgtBattery2Renderer.estimate(42, true, null)
        assertEquals(KwgtBattery2Renderer.EstimateKind.UNAVAILABLE, chargingWithoutValue.kind)
        assertEquals("--", chargingWithoutValue.valueText)

        val dischargeWithSystemValue = KwgtBattery2Renderer.estimate(42, false, 3_900_000L)
        assertEquals(
            KwgtBattery2Renderer.EstimateKind.UNAVAILABLE,
            dischargeWithSystemValue.kind,
        )
        assertEquals("--", dischargeWithSystemValue.valueText)

        val zeroValue = KwgtBattery2Renderer.estimate(42, true, 0L)
        assertEquals(KwgtBattery2Renderer.EstimateKind.UNAVAILABLE, zeroValue.kind)
        assertEquals("--", zeroValue.valueText)
    }

    @Test
    fun realChargingEstimateIsLabelledToFull() {
        val estimate = KwgtBattery2Renderer.estimate(42, true, 3_900_000L)
        assertEquals(KwgtBattery2Renderer.EstimateKind.TO_FULL, estimate.kind)
        assertEquals(1, estimate.hours)
        assertEquals(5, estimate.minutes)
        assertEquals("1H 5M", estimate.valueText)
        assertEquals("TO FULL", estimate.labelText)

        val underOneMinute = KwgtBattery2Renderer.estimate(0, true, 30_000L)
        assertEquals(KwgtBattery2Renderer.EstimateKind.TO_FULL, underOneMinute.kind)
        assertEquals("0H 1M", underOneMinute.valueText)
    }

    @Test
    fun fullBatteryShowsFullOnlyAndNoInventedTime() {
        val full = KwgtBattery2Renderer.estimate(100, true, 3_900_000L)
        assertEquals(KwgtBattery2Renderer.EstimateKind.FULL, full.kind)
        assertEquals("FULL", full.valueText)
        assertEquals("", full.labelText)

        val clampedFull = KwgtBattery2Renderer.estimate(140, false, null)
        assertEquals(KwgtBattery2Renderer.EstimateKind.FULL, clampedFull.kind)
        assertEquals("FULL", clampedFull.valueText)
    }

    private companion object {
        const val EPS = 0.01f
    }
}
