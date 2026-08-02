package ru.coffeemaniavpn.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnounceDeviceLimitParserTest {
    @Test
    fun parsesLimitFromRealAnnounceBase64() {
        val announce =
            "base64:8J+GlDogNjI3MDcxMzcxIArQm9C40LzQuNGCINGD0YHRgtGA0L7QudGB0YLQsiDwn5OxIDAK0J7RgdGC0LDQu9C+0YHRjCDij7M6IDEyCtCf0YDQvtCy0LXRgNC40YLRjCDQutCw0LrQvtC5INGB0LXRgNCy0LXRgCDQv9C+0LTRhdC+0LTQuNGCINC90LDQttC80LjRgtC1INC90LAg8J+TryBQSU5HIArim5TvuI8g0J3QtSDRgNCw0LHQvtGC0LDQtdGCPyDQndCw0LbQvNC4IPCflIQg0L3QsCBXSUZJCvCfk7YgLSDQtNC70Y8g0LzQvtCx0LjQu9GM0L3QvtCz0L4g0LjQvdGC0LXRgNC90LXRgtCw"

        val decoded = AnnounceDeviceLimitParser.decodeAnnounce(announce)
        assertTrue(decoded.contains("устройств", ignoreCase = true))

        val limit = AnnounceDeviceLimitParser.parse(announce)
        assertEquals(Integer.valueOf(0), limit)
    }

    @Test
    fun parseFromResponseKeepsAnnounceText() {
        val announce =
            "base64:8J+GlDogNjI3MDcxMzcxIArQm9C40LzQuNGCINGD0YHRgtGA0L7QudGB0YLQsiDwn5OxIDAK0J7RgdGC0LDQu9C+0YHRjCDij7M6IDEyCtCf0YDQvtCy0LXRgNC40YLRjCDQutCw0LrQvtC5INGB0LXRgNCy0LXRgCDQv9C+0LTRhdC+0LTQuNGCINC90LDQttC80LjRgtC1INC90LAg8J+TryBQSU5HIArim5TvuI8g0J3QtSDRgNCw0LHQvtGC0LDQtdGCPyDQndCw0LbQvNC4IPCflIQg0L3QsCBXSUZJCvCfk7YgLSDQtNC70Y8g0LzQvtCx0LjQu9GM0L3QvtCz0L4g0LjQvdGC0LXRgNC90LXRgtCw"

        val info = SubscriptionInfoParser.parseFromResponse(
            userInfoHeader = "upload=0; download=1; total=0; expire=1786446393",
            profileTitleHeader = "base64:UE9ST1pPRkY=",
            announceHeader = announce,
            body = "",
        )

        assertTrue(info != null)
        assertTrue(info!!.hasAnnounce)
        assertTrue(info.announce.contains("Лимит устройств"))
        assertTrue(info.announce.contains("PING"))
        assertEquals(Integer.valueOf(0), info.deviceLimit)
        assertEquals("Устройств: ∞", info.devicesLabel())
    }

    @Test
    fun extractsLimitWithEmojiBetweenLabelAndNumber() {
        val text = "Лимит устройств 📱 5"
        assertEquals(Integer.valueOf(5), AnnounceDeviceLimitParser.extractDeviceLimit(text))
        assertEquals(
            "Устройств: 5",
            SubscriptionInfo(announce = text).devicesLabel(),
        )
    }
}
