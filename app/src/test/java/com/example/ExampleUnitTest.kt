package com.example

import com.example.data.parser.ShortplayLinkParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    @Test
    fun testIsPotentialShortplayLink() {
        assertTrue(ShortplayLinkParser.isPotentialShortplayLink("https://v.douyin.com/abc/"))
        assertTrue(ShortplayLinkParser.isPotentialShortplayLink("【红果短剧】《都市至尊龙王》第15集"))
        assertTrue(ShortplayLinkParser.isPotentialShortplayLink("长按复制此条消息，查看第3集"))
    }

    @Test
    fun testParseShortplayText() = runBlocking {
        val input = "【红果免费短剧】《都市至尊龙王》第15集 复制此链接打开：https://novel.snssdk.com/api/test"
        val parsed = ShortplayLinkParser.parseText(input)

        assertEquals("都市至尊龙王", parsed.title)
        assertEquals(15, parsed.episodeIndex)
        assertTrue(parsed.videoUrl.isNotBlank())
    }

    @Test
    fun testRealUrlPlaybackPreserved() = runBlocking {
        val realUrl = "https://test-cdn.hongguo.com/media/ep01.mp4"
        val input = "长按复制观看《绝世天医》第2集 $realUrl"
        val parsed = ShortplayLinkParser.parseText(input)

        assertEquals("绝世天医", parsed.title)
        assertEquals(2, parsed.episodeIndex)
        assertEquals(realUrl, parsed.videoUrl)
        assertEquals("DIRECT_STREAM", parsed.sourceType)
    }
}
