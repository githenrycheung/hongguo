package com.example.data.parser

import android.util.Log
import com.example.data.model.ParsedDramaInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object ShortplayLinkParser {

    private const val TAG = "ShortplayLinkParser"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // Reliable public short drama & cinema demo video streams for fallback / sample playback
    val SAMPLE_STREAMS = listOf(
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4"
    )

    private val URL_REGEX = Pattern.compile("(https?://[a-zA-Z0-9.\\-_]+(/[^\\s\\u4e00-\\u9fa5]*)?)")
    private val TITLE_BOOK_REGEX = Pattern.compile("《([^》]+)》")
    private val TITLE_BRACKET_REGEX = Pattern.compile("【([^】]+)】")
    private val EPISODE_REGEX = Pattern.compile("(?:第\\s*(\\d+)\\s*[集话]|ep\\s*(\\d+))", Pattern.CASE_INSENSITIVE)

    /**
     * Checks if the text has any URL or short drama share characteristics.
     */
    fun isPotentialShortplayLink(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val trimmed = text.trim()
        if (trimmed.contains("http://") || trimmed.contains("https://")) return true
        if (trimmed.contains("红果") || trimmed.contains("短剧") || trimmed.contains("集")) return true
        return false
    }

    /**
     * Parse clipboard text or user input into structured drama info.
     */
    suspend fun parseText(input: String): ParsedDramaInfo = withContext(Dispatchers.IO) {
        val trimmed = input.trim()

        // 1. Extract URL
        val matcher = URL_REGEX.matcher(trimmed)
        val extractedUrl = if (matcher.find()) matcher.group(1) ?: "" else ""

        // 2. Extract Title
        var title = ""
        val bookMatcher = TITLE_BOOK_REGEX.matcher(trimmed)
        if (bookMatcher.find()) {
            title = bookMatcher.group(1) ?: ""
        } else {
            val bracketMatcher = TITLE_BRACKET_REGEX.matcher(trimmed)
            if (bracketMatcher.find()) {
                val candidate = bracketMatcher.group(1) ?: ""
                if (!candidate.contains("红果") && candidate.length > 1) {
                    title = candidate
                }
            }
        }
        if (title.isBlank()) {
            // Check if there is short text without url
            title = if (extractedUrl.isNotBlank()) {
                "红果解析短剧 · " + (if (extractedUrl.length > 28) extractedUrl.take(28) + "..." else extractedUrl)
            } else {
                if (trimmed.length > 20) trimmed.take(20) + "..." else trimmed
            }
        }

        // 3. Extract Episode Number
        var episodeIndex = 1
        val epMatcher = EPISODE_REGEX.matcher(trimmed)
        if (epMatcher.find()) {
            val numStr = epMatcher.group(1) ?: epMatcher.group(2)
            episodeIndex = numStr?.toIntOrNull() ?: 1
        }

        // 4. Resolve Direct Video Stream
        var finalVideoUrl = ""
        var sourceType = "HONGGUO"

        if (extractedUrl.isNotBlank()) {
            if (isDirectVideoUrl(extractedUrl)) {
                finalVideoUrl = extractedUrl
                sourceType = "DIRECT_STREAM"
            } else {
                // Try resolving redirect & fetching content
                try {
                    val resolved = resolveNetworkLink(extractedUrl)
                    if (resolved.isNotBlank()) {
                        finalVideoUrl = resolved
                        sourceType = "WEB_EXTRACT"
                    }
                } catch (e: Exception) {
                    safeLogW(TAG, "Network resolution failed for $extractedUrl", e)
                }

                // If resolution did not find explicit sub-stream, use the user's real URL directly
                if (finalVideoUrl.isBlank()) {
                    finalVideoUrl = extractedUrl
                    sourceType = "REAL_ORIGINAL_URL"
                }
            }
        }

        // Only fallback to demo stream if the input contained NO URL at all
        if (finalVideoUrl.isBlank()) {
            finalVideoUrl = SAMPLE_STREAMS[Math.abs(trimmed.hashCode()) % SAMPLE_STREAMS.size]
            sourceType = "HONGGUO_DEMO_STREAM"
        }

        safeLogD(TAG, "Resolved play url: $finalVideoUrl (sourceType: $sourceType)")

        ParsedDramaInfo(
            rawInput = trimmed,
            title = title,
            episodeIndex = episodeIndex,
            totalEpisodes = if (isDirectVideoUrl(extractedUrl)) 1 else maxOf(80, episodeIndex),
            videoUrl = finalVideoUrl,
            sourceType = sourceType,
            parsedAt = System.currentTimeMillis()
        )
    }

    private fun isDirectVideoUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".mp4") ||
               lower.endsWith(".m3u8") ||
               lower.endsWith(".flv") ||
               lower.endsWith(".mkv") ||
               lower.endsWith(".webm") ||
               lower.endsWith(".ts") ||
               lower.endsWith(".mov") ||
               lower.contains(".mp4?") ||
               lower.contains(".m3u8?") ||
               lower.contains(".flv?") ||
               lower.contains("douyinvod.com") ||
               lower.contains("bytevod.com") ||
               lower.contains("toutiaovod.com") ||
               lower.contains("volccdn.com") ||
               lower.contains("snssdk.com/video") ||
               lower.contains("video_url") ||
               lower.contains("mime_type=video")
    }

    private fun resolveNetworkLink(targetUrl: String): String {
        val request = Request.Builder()
            .url(targetUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .build()

        val response = okHttpClient.newCall(request).execute()
        val finalUrl = response.request.url.toString()
        if (isDirectVideoUrl(finalUrl)) {
            return finalUrl
        }

        val body = response.body?.string() ?: ""

        // 1. Check for douyin / hongguo / toutiao video url_list
        val urlListPattern = Pattern.compile("\"url_list\":\\s*\\[\\s*\"(https?:[^\"]+)\"")
        val urlListMatcher = urlListPattern.matcher(body)
        if (urlListMatcher.find()) {
            val candidate = urlListMatcher.group(1)
            if (!candidate.isNullOrBlank()) {
                return candidate.replace("\\u0026", "&").replace("\\/", "/")
            }
        }

        // 2. Check for play_addr / main_url / video_url / play_url
        val playAddrPattern = Pattern.compile("\"(?:play_addr|main_url|video_url|play_url)\":\\s*\"(https?:[^\"]+)\"")
        val playMatcher = playAddrPattern.matcher(body)
        if (playMatcher.find()) {
            val candidate = playMatcher.group(1)
            if (!candidate.isNullOrBlank()) {
                return candidate.replace("\\u0026", "&").replace("\\/", "/")
            }
        }

        // 3. Search HTML for video tags or source tags
        val htmlVideoPattern = Pattern.compile("<(?:video|source)[^>]+src=[\"'](https?:[^\"']+)[\"']", Pattern.CASE_INSENSITIVE)
        val htmlMatcher = htmlVideoPattern.matcher(body)
        if (htmlMatcher.find()) {
            val candidate = htmlMatcher.group(1)
            if (!candidate.isNullOrBlank()) {
                return candidate.replace("&amp;", "&")
            }
        }

        // 4. Search body for direct mp4 or m3u8 url
        val videoPattern = Pattern.compile("https?://[^\"'\\s]+\\.(?:mp4|m3u8)[^\"'\\s]*")
        val videoMatcher = videoPattern.matcher(body)
        if (videoMatcher.find()) {
            val url = videoMatcher.group(0)
            if (!url.isNullOrBlank()) {
                return url.replace("\\/", "/")
            }
        }

        // If redirect led to another URL different from targetUrl, return the redirected url
        if (finalUrl != targetUrl && (finalUrl.startsWith("http://") || finalUrl.startsWith("https://"))) {
            return finalUrl
        }

        return ""
    }

    private fun safeLogD(tag: String, msg: String) {
        try {
            Log.d(tag, msg)
        } catch (e: Throwable) {
            println("[$tag] $msg")
        }
    }

    private fun safeLogW(tag: String, msg: String, tr: Throwable?) {
        try {
            Log.w(tag, msg, tr)
        } catch (e: Throwable) {
            println("[$tag] $msg ${tr?.message}")
        }
    }
}
