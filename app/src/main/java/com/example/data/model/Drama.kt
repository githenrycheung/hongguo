package com.example.data.model

data class Episode(
    val index: Int,
    val title: String,
    val videoUrl: String,
    val durationSec: Int = 120
)

data class Drama(
    val id: String,
    val title: String,
    val category: String,
    val coverUrl: String,
    val description: String,
    val tag: String,
    val rating: Float = 9.4f,
    val totalEpisodes: Int = 80,
    val episodes: List<Episode> = emptyList()
)

data class ParsedDramaInfo(
    val rawInput: String,
    val title: String,
    val episodeIndex: Int = 1,
    val totalEpisodes: Int = 80,
    val videoUrl: String,
    val coverUrl: String = "",
    val sourceType: String = "HONGGUO",
    val parsedAt: Long = System.currentTimeMillis()
)

enum class TvNavTab(val title: String, val iconName: String) {
    HOME("精选热播", "home"),
    CLIPBOARD("剪贴板解析", "content_paste"),
    HISTORY("播放历史", "history"),
    CATEGORIES("剧集分类", "category")
}
