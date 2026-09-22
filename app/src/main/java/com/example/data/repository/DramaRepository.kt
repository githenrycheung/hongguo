package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.ClipboardHistoryEntity
import com.example.data.local.PlayHistoryEntity
import com.example.data.model.Drama
import com.example.data.model.Episode
import com.example.data.model.ParsedDramaInfo
import com.example.data.parser.ShortplayLinkParser
import kotlinx.coroutines.flow.Flow

class DramaRepository(
    private val database: AppDatabase
) {
    private val dao = database.historyDao()

    val playHistory: Flow<List<PlayHistoryEntity>> = dao.getAllPlayHistory()
    val clipboardHistory: Flow<List<ClipboardHistoryEntity>> = dao.getAllClipboardHistory()

    suspend fun savePlayProgress(
        dramaId: String,
        dramaTitle: String,
        episodeIndex: Int,
        totalEpisodes: Int,
        videoUrl: String,
        coverUrl: String,
        positionMs: Long,
        durationMs: Long
    ) {
        val entity = PlayHistoryEntity(
            dramaId = dramaId,
            dramaTitle = dramaTitle,
            episodeIndex = episodeIndex,
            totalEpisodes = totalEpisodes,
            videoUrl = videoUrl,
            coverUrl = coverUrl,
            positionMs = positionMs,
            durationMs = durationMs,
            updatedTimestamp = System.currentTimeMillis()
        )
        dao.insertOrUpdatePlayHistory(entity)
    }

    suspend fun getPlayProgress(dramaId: String): PlayHistoryEntity? {
        return dao.getPlayHistory(dramaId)
    }

    suspend fun deletePlayHistory(dramaId: String) {
        dao.deletePlayHistory(dramaId)
    }

    suspend fun clearAllHistory() {
        dao.clearAllPlayHistory()
    }

    suspend fun parseAndSaveClipboard(rawText: String): ParsedDramaInfo {
        val parsed = ShortplayLinkParser.parseText(rawText)
        dao.insertClipboardHistory(
            ClipboardHistoryEntity(
                rawText = rawText,
                extractedUrl = parsed.videoUrl,
                parsedTitle = parsed.title,
                episodeIndex = parsed.episodeIndex,
                videoUrl = parsed.videoUrl,
                timestamp = System.currentTimeMillis()
            )
        )
        return parsed
    }

    suspend fun clearClipboardHistory() {
        dao.clearAllClipboardHistory()
    }

    fun getFeaturedDramas(): List<Drama> = curatedDramas

    fun getDramasByCategory(category: String): List<Drama> {
        if (category == "全部" || category.isBlank()) return curatedDramas
        return curatedDramas.filter { it.category == category }
    }

    fun getCategories(): List<String> = listOf("全部", "战神归来", "穿越逆袭", "豪门恩怨", "都市异能", "甜宠虐恋")

    companion object {
        private val sampleCovers = listOf(
            "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1518676590629-3dcbd9c5a5c9?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1478760329108-5c3ed9d495a0?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1574267432553-4b4628081c31?w=600&auto=format&fit=crop&q=80",
            "https://images.unsplash.com/photo-1509281373149-e957c6296406?w=600&auto=format&fit=crop&q=80"
        )

        private fun generateEpisodes(dramaId: String, total: Int): List<Episode> {
            return (1..total).map { epIndex ->
                val streamUrl = ShortplayLinkParser.SAMPLE_STREAMS[
                    (epIndex + Math.abs(dramaId.hashCode())) % ShortplayLinkParser.SAMPLE_STREAMS.size
                ]
                Episode(
                    index = epIndex,
                    title = "第 $epIndex 集",
                    videoUrl = streamUrl,
                    durationSec = 90 + (epIndex * 7) % 60
                )
            }
        }

        private val curatedDramas = listOf(
            Drama(
                id = "hg_001",
                title = "天龙战神归来",
                category = "战神归来",
                coverUrl = sampleCovers[0],
                description = "五年前他被迫隐姓埋名，五年后携十万天龙禁卫重返都市，谁敢伤我妻儿分毫，必叫天地变色！",
                tag = "热播榜 TOP 1",
                rating = 9.8f,
                totalEpisodes = 80,
                episodes = generateEpisodes("hg_001", 80)
            ),
            Drama(
                id = "hg_002",
                title = "九品芝麻官翻身记",
                category = "穿越逆袭",
                coverUrl = sampleCovers[1],
                description = "现代特种兵意外穿越到风雨飘摇的大燕王朝，从一个小小的九品巡检开始智斗权相、横扫漠北！",
                tag = "逆袭爽剧",
                rating = 9.6f,
                totalEpisodes = 60,
                episodes = generateEpisodes("hg_002", 60)
            ),
            Drama(
                id = "hg_003",
                title = "隐形巨富与冷艳总裁",
                category = "豪门恩怨",
                coverUrl = sampleCovers[2],
                description = "他甘当三年赘婿被岳母百般羞辱，今日继承万亿财团身份揭晓，各大商界巨擘纷纷登门叩拜！",
                tag = "全网爆款",
                rating = 9.7f,
                totalEpisodes = 72,
                episodes = generateEpisodes("hg_003", 72)
            ),
            Drama(
                id = "hg_004",
                title = "我有神级透视眼",
                category = "都市异能",
                coverUrl = sampleCovers[3],
                description = "偶获上古黄金瞳，鉴古玉、识奇宝、断生死！少年一跃成为都市传奇，傲视群雄！",
                tag = "高分推荐",
                rating = 9.5f,
                totalEpisodes = 68,
                episodes = generateEpisodes("hg_004", 68)
            ),
            Drama(
                id = "hg_005",
                title = "替嫁甜妻别想逃",
                category = "甜宠虐恋",
                coverUrl = sampleCovers[4],
                description = "妹妹逃婚，她被迫替嫁给传说中残暴嗜血的商界暴君，没想到新婚之夜竟被宠上了天！",
                tag = "甜宠必看",
                rating = 9.4f,
                totalEpisodes = 55,
                episodes = generateEpisodes("hg_005", 55)
            ),
            Drama(
                id = "hg_006",
                title = "修罗殿主：万神之王",
                category = "战神归来",
                coverUrl = sampleCovers[5],
                description = "战乱平息，修罗殿封刀归隐。当女儿的一封求救信寄来，三千修罗铁骑再次血战长空！",
                tag = "热血无敌",
                rating = 9.9f,
                totalEpisodes = 88,
                episodes = generateEpisodes("hg_006", 88)
            ),
            Drama(
                id = "hg_007",
                title = "大明第一权臣",
                category = "穿越逆袭",
                coverUrl = sampleCovers[1],
                description = "开局一介布衣，凭借超前六百年的科技与政治智慧，在内阁党争与外敌环伺中力挽狂澜！",
                tag = "烧脑大作",
                rating = 9.6f,
                totalEpisodes = 65,
                episodes = generateEpisodes("hg_007", 65)
            ),
            Drama(
                id = "hg_008",
                title = "千亿霸总独宠娇妻",
                category = "甜宠虐恋",
                coverUrl = sampleCovers[4],
                description = "一场乌龙契约婚姻，冷血腹黑总裁竟化身黏人忠犬，全球定制私人飞机只为博娇妻一笑！",
                tag = "浪漫爆笑",
                rating = 9.5f,
                totalEpisodes = 50,
                episodes = generateEpisodes("hg_008", 50)
            )
        )
    }
}
