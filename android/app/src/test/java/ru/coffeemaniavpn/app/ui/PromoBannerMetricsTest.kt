package ru.coffeemaniavpn.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

class PromoBannerMetricsTest {

    @Test
    fun phonePortrait_fillsWidth_andKeepsPhoneHeightCap() {
        val spec = PromoBannerMetrics.compute(widthDp = 360f, heightDp = 780f)

        assertNull(spec.maxWidthDp)
        assertEquals(160f, spec.maxHeightDp, 0.01f)
        assertTrue(spec.maxHeightDp <= heightFraction(780f))
    }

    @Test
    fun phoneLandscape_shortScreen_usesCompactHeight() {
        val spec = PromoBannerMetrics.compute(widthDp = 780f, heightDp = 360f)

        assertEquals(420f, spec.maxWidthDp!!, 0.01f)
        assertEquals(79.2f, spec.maxHeightDp, 0.1f) // 360 * 0.22
        assertTrue(spec.maxHeightDp >= PromoBannerMetrics.MIN_HEIGHT_DP)
    }

    @Test
    fun compactPhoneLandscape_belowExpanded_usesWidthFraction() {
        val spec = PromoBannerMetrics.compute(widthDp = 640f, heightDp = 360f)

        assertEquals(384f, spec.maxWidthDp!!, 0.01f) // 640 * 0.6
        assertEquals(79.2f, spec.maxHeightDp, 0.1f)
    }

    @Test
    fun smallPhonePortrait_neverExceedsFractionOrPhoneCap() {
        val spec = PromoBannerMetrics.compute(widthDp = 320f, heightDp = 568f)

        assertNull(spec.maxWidthDp)
        // min(160, 568*0.22=124.96) = 124.96
        assertEquals(124.96f, spec.maxHeightDp, 0.1f)
    }

    @Test
    fun tabletPortrait_capsWidthAndHeight() {
        val spec = PromoBannerMetrics.compute(widthDp = 800f, heightDp = 1280f)

        assertEquals(420f, spec.maxWidthDp!!, 0.01f)
        assertEquals(140f, spec.maxHeightDp, 0.01f)
    }

    @Test
    fun tabletLandscape_doesNotDominateHeight() {
        val spec = PromoBannerMetrics.compute(widthDp = 1280f, heightDp = 800f)

        assertEquals(420f, spec.maxWidthDp!!, 0.01f)
        assertEquals(128f, spec.maxHeightDp, 0.01f) // expanded absolute cap
        assertTrue(spec.maxHeightDp <= 800f * PromoBannerMetrics.MAX_HEIGHT_FRACTION)
    }

    @Test
    fun expandedDesktop_likeWidth_usesExpandedCap() {
        val spec = PromoBannerMetrics.compute(widthDp = 1024f, heightDp = 600f)

        assertEquals(420f, spec.maxWidthDp!!, 0.01f)
        assertEquals(128f, spec.maxHeightDp, 0.01f)
    }

    @Test
    fun veryShortAvailableHeight_floorsAtMinimum() {
        val spec = PromoBannerMetrics.compute(widthDp = 400f, heightDp = 200f)

        assertNull(spec.maxWidthDp)
        assertEquals(PromoBannerMetrics.MIN_HEIGHT_DP, spec.maxHeightDp, 0.01f)
    }

    @Test
    fun mediumTabletWidth_usesWidthFractionWhenSmallerThanAbsolute() {
        val spec = PromoBannerMetrics.compute(widthDp = 600f, heightDp = 960f)

        assertEquals(360f, spec.maxWidthDp!!, 0.01f) // 600 * 0.6 < 420
        assertEquals(140f, spec.maxHeightDp, 0.01f)
    }

    @Test
    fun zeroHeight_fallsBackToAbsoluteCap() {
        val phone = PromoBannerMetrics.compute(360f, 0f)
        val tablet = PromoBannerMetrics.compute(700f, 0f)
        val expanded = PromoBannerMetrics.compute(900f, 0f)

        assertEquals(160f, phone.maxHeightDp, 0.01f)
        assertEquals(140f, tablet.maxHeightDp, 0.01f)
        assertEquals(128f, expanded.maxHeightDp, 0.01f)
    }

    @Test
    fun bannerNeverConsumesMoreThanFractionOfAvailableHeight() {
        val sizes = listOf(
            360f to 640f,
            412f to 892f,
            600f to 960f,
            800f to 1280f,
            1280f to 800f,
            1920f to 1080f,
            320f to 480f,
        )
        sizes.forEach { (w, h) ->
            val spec = PromoBannerMetrics.compute(w, h)
            assertTrue(
                "height $h width $w -> ${spec.maxHeightDp}",
                spec.maxHeightDp <= h * PromoBannerMetrics.MAX_HEIGHT_FRACTION + 0.01f ||
                    spec.maxHeightDp == PromoBannerMetrics.MIN_HEIGHT_DP,
            )
            assertTrue(spec.maxHeightDp >= PromoBannerMetrics.MIN_HEIGHT_DP)
            assertTrue(spec.maxHeightDp <= PromoBannerMetrics.PHONE_MAX_HEIGHT_DP)
        }
    }

    @Test
    fun fitInside_fullWidthWhenAspectFitsHeight() {
        // banner_got_tg 770x205 ≈ 3.756
        val aspect = 770f / 205f
        val fitted = PromoBannerMetrics.fitInside(360f, 160f, aspect)

        assertEquals(360f, fitted.widthDp, 0.01f)
        assertEquals(360f / aspect, fitted.heightDp, 0.1f)
        assertTrue(fitted.heightDp <= 160f)
    }

    @Test
    fun fitInside_shrinksWidthWhenHeightCapped_noSideGaps() {
        val aspect = 770f / 205f
        val fitted = PromoBannerMetrics.fitInside(420f, 79.2f, aspect)

        assertEquals(79.2f, fitted.heightDp, 0.01f)
        assertEquals(79.2f * aspect, fitted.widthDp, 0.1f)
        assertTrue(fitted.widthDp < 420f)
        assertEquals(aspect, fitted.widthDp / fitted.heightDp, 0.01f)
    }

    @Test
    fun fitInside_preservesAspectAcrossDevices() {
        val aspect = 770f / 205f
        listOf(
            360f to 160f,
            420f to 140f,
            420f to 79.2f,
            320f to 124f,
        ).forEach { (w, h) ->
            val fitted = PromoBannerMetrics.fitInside(w, h, aspect)
            assertTrue(fitted.widthDp <= w + 0.01f)
            assertTrue(fitted.heightDp <= h + 0.01f)
            assertEquals(aspect, fitted.widthDp / fitted.heightDp, 0.01f)
        }
    }

    private fun heightFraction(heightDp: Float): Float =
        heightDp * PromoBannerMetrics.MAX_HEIGHT_FRACTION
}

@RunWith(Parameterized::class)
class PromoBannerMetricsParameterizedTest(
    private val widthDp: Float,
    private val heightDp: Float,
) {
    @Test
    fun invariantsHoldForCommonDevices() {
        val spec = PromoBannerMetrics.compute(widthDp, heightDp)

        assertTrue(spec.maxHeightDp >= PromoBannerMetrics.MIN_HEIGHT_DP)
        assertTrue(spec.maxHeightDp <= PromoBannerMetrics.PHONE_MAX_HEIGHT_DP)

        if (widthDp >= PromoBannerMetrics.MEDIUM_WIDTH_DP) {
            val expectedMax = minOf(
                PromoBannerMetrics.TABLET_MAX_WIDTH_DP,
                widthDp * PromoBannerMetrics.TABLET_WIDTH_FRACTION,
            )
            assertEquals(expectedMax, spec.maxWidthDp!!, 0.01f)
        } else {
            assertNull(spec.maxWidthDp)
        }

        // Список/контент должен оставлять не меньше ~78% высоты (кроме floor по MIN).
        if (heightDp * PromoBannerMetrics.MAX_HEIGHT_FRACTION >= PromoBannerMetrics.MIN_HEIGHT_DP) {
            assertTrue(spec.maxHeightDp <= heightDp * PromoBannerMetrics.MAX_HEIGHT_FRACTION + 0.01f)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "w={0}, h={1}")
        fun data(): Collection<Array<Float>> = listOf(
            arrayOf(320f, 568f),   // iPhone SE-ish
            arrayOf(360f, 640f),   // small android
            arrayOf(360f, 780f),   // common phone
            arrayOf(411f, 891f),   // Pixel-ish
            arrayOf(412f, 915f),   // large phone
            arrayOf(800f, 360f),   // phone landscape
            arrayOf(600f, 960f),   // 7" tablet portrait
            arrayOf(960f, 600f),   // 7" tablet landscape
            arrayOf(800f, 1280f),  // 10" tablet portrait
            arrayOf(1280f, 800f),  // 10" tablet landscape
            arrayOf(1024f, 768f),  // expanded
            arrayOf(1920f, 1200f), // desktop-like
            arrayOf(280f, 280f),   // tiny / fold outer
            arrayOf(673f, 841f),   // fold unfolded
        )
    }
}
