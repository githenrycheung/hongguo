package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ClipboardHistoryEntity
import com.example.data.local.PlayHistoryEntity
import com.example.data.model.Drama
import com.example.data.model.Episode
import com.example.data.model.ParsedDramaInfo
import com.example.data.model.TvNavTab
import com.example.data.parser.ShortplayLinkParser
import com.example.data.repository.DramaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ClipboardNotice(
    val rawText: String,
    val parsedInfo: ParsedDramaInfo
)

data class ActivePlayerState(
    val dramaId: String,
    val dramaTitle: String,
    val coverUrl: String,
    val currentEpisode: Int,
    val totalEpisodes: Int,
    val episodes: List<Episode>,
    val currentStreamUrl: String,
    val initialPositionMs: Long = 0L
)

class MainTvViewModel(
    private val repository: DramaRepository
) : ViewModel() {

    private val _currentTab = MutableStateFlow(TvNavTab.HOME)
    val currentTab: StateFlow<TvNavTab> = _currentTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow("全部")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _featuredDramas = MutableStateFlow(repository.getFeaturedDramas())
    val featuredDramas: StateFlow<List<Drama>> = _featuredDramas.asStateFlow()

    private val _categoryDramas = MutableStateFlow(repository.getDramasByCategory("全部"))
    val categoryDramas: StateFlow<List<Drama>> = _categoryDramas.asStateFlow()

    val playHistory: StateFlow<List<PlayHistoryEntity>> = repository.playHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clipboardHistory: StateFlow<List<ClipboardHistoryEntity>> = repository.clipboardHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _clipboardNotice = MutableStateFlow<ClipboardNotice?>(null)
    val clipboardNotice: StateFlow<ClipboardNotice?> = _clipboardNotice.asStateFlow()

    private val _activePlayerState = MutableStateFlow<ActivePlayerState?>(null)
    val activePlayerState: StateFlow<ActivePlayerState?> = _activePlayerState.asStateFlow()

    private val _isParsing = MutableStateFlow(false)
    val isParsing: StateFlow<Boolean> = _isParsing.asStateFlow()

    private val _parseStatusMessage = MutableStateFlow<String?>(null)
    val parseStatusMessage: StateFlow<String?> = _parseStatusMessage.asStateFlow()

    fun selectTab(tab: TvNavTab) {
        _currentTab.value = tab
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _categoryDramas.value = repository.getDramasByCategory(category)
    }

    fun onClipboardDetected(text: String) {
        if (!ShortplayLinkParser.isPotentialShortplayLink(text)) return

        viewModelScope.launch {
            try {
                val parsed = repository.parseAndSaveClipboard(text)
                _clipboardNotice.value = ClipboardNotice(text, parsed)
            } catch (e: Exception) {
                // Ignore parse errors silently
            }
        }
    }

    fun dismissClipboardNotice() {
        _clipboardNotice.value = null
    }

    fun playFromClipboardNotice() {
        val notice = _clipboardNotice.value ?: return
        _clipboardNotice.value = null
        playDirectParsed(notice.parsedInfo)
    }

    fun manualParseAndPlay(input: String) {
        if (input.isBlank()) return
        viewModelScope.launch {
            _isParsing.value = true
            _parseStatusMessage.value = "正在智能解析短剧链接..."
            try {
                val parsed = repository.parseAndSaveClipboard(input)
                _parseStatusMessage.value = "解析成功: ${parsed.title}"
                playDirectParsed(parsed)
            } catch (e: Exception) {
                _parseStatusMessage.value = "解析失败: ${e.message}"
            } finally {
                _isParsing.value = false
            }
        }
    }

    fun playDrama(drama: Drama, startEpisode: Int = 1, resumePositionMs: Long = 0L) {
        val ep = drama.episodes.find { it.index == startEpisode }
            ?: drama.episodes.firstOrNull()
            ?: Episode(1, "第 1 集", ShortplayLinkParser.SAMPLE_STREAMS[0])

        _activePlayerState.value = ActivePlayerState(
            dramaId = drama.id,
            dramaTitle = drama.title,
            coverUrl = drama.coverUrl,
            currentEpisode = ep.index,
            totalEpisodes = drama.totalEpisodes,
            episodes = drama.episodes,
            currentStreamUrl = ep.videoUrl,
            initialPositionMs = resumePositionMs
        )
    }

    fun playFromHistory(history: PlayHistoryEntity) {
        val drama = repository.getFeaturedDramas().find { it.id == history.dramaId }
        if (drama != null) {
            playDrama(drama, history.episodeIndex, history.positionMs)
        } else {
            // Standalone parsed drama
            val ep = Episode(
                index = history.episodeIndex,
                title = "第 ${history.episodeIndex} 集",
                videoUrl = history.videoUrl
            )
            val generatedEpisodes = (1..history.totalEpisodes).map { i ->
                Episode(
                    index = i,
                    title = "第 $i 集",
                    videoUrl = if (i == history.episodeIndex) history.videoUrl else ShortplayLinkParser.SAMPLE_STREAMS[i % ShortplayLinkParser.SAMPLE_STREAMS.size]
                )
            }
            _activePlayerState.value = ActivePlayerState(
                dramaId = history.dramaId,
                dramaTitle = history.dramaTitle,
                coverUrl = history.coverUrl,
                currentEpisode = history.episodeIndex,
                totalEpisodes = history.totalEpisodes,
                episodes = generatedEpisodes,
                currentStreamUrl = history.videoUrl,
                initialPositionMs = history.positionMs
            )
        }
    }

    fun playDirectParsed(parsed: ParsedDramaInfo) {
        val dramaId = "parsed_" + Math.abs(parsed.title.hashCode())
        val generatedEpisodes = (1..parsed.totalEpisodes).map { i ->
            Episode(
                index = i,
                title = "第 $i 集",
                videoUrl = if (i == parsed.episodeIndex) parsed.videoUrl else ShortplayLinkParser.SAMPLE_STREAMS[i % ShortplayLinkParser.SAMPLE_STREAMS.size]
            )
        }
        _activePlayerState.value = ActivePlayerState(
            dramaId = dramaId,
            dramaTitle = parsed.title,
            coverUrl = parsed.coverUrl,
            currentEpisode = parsed.episodeIndex,
            totalEpisodes = parsed.totalEpisodes,
            episodes = generatedEpisodes,
            currentStreamUrl = parsed.videoUrl,
            initialPositionMs = 0L
        )
    }

    fun switchEpisode(episodeIndex: Int) {
        val current = _activePlayerState.value ?: return
        val targetEp = current.episodes.find { it.index == episodeIndex } ?: return
        _activePlayerState.value = current.copy(
            currentEpisode = targetEp.index,
            currentStreamUrl = targetEp.videoUrl,
            initialPositionMs = 0L
        )
    }

    fun playNextEpisode() {
        val current = _activePlayerState.value ?: return
        if (current.currentEpisode < current.totalEpisodes) {
            switchEpisode(current.currentEpisode + 1)
        }
    }

    fun playPrevEpisode() {
        val current = _activePlayerState.value ?: return
        if (current.currentEpisode > 1) {
            switchEpisode(current.currentEpisode - 1)
        }
    }

    fun saveProgress(positionMs: Long, durationMs: Long) {
        val current = _activePlayerState.value ?: return
        viewModelScope.launch {
            repository.savePlayProgress(
                dramaId = current.dramaId,
                dramaTitle = current.dramaTitle,
                episodeIndex = current.currentEpisode,
                totalEpisodes = current.totalEpisodes,
                videoUrl = current.currentStreamUrl,
                coverUrl = current.coverUrl,
                positionMs = positionMs,
                durationMs = durationMs
            )
        }
    }

    fun closePlayer() {
        _activePlayerState.value = null
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    fun clearClipboardHistory() {
        viewModelScope.launch {
            repository.clearClipboardHistory()
        }
    }
}
