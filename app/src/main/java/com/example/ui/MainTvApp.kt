package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.TvNavTab
import com.example.data.repository.DramaRepository
import com.example.ui.components.ClipboardNoticeDialog
import com.example.ui.components.TvTopBar
import com.example.ui.player.TvVideoPlayer
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.ClipboardParserScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.TvBackground
import com.example.viewmodel.MainTvViewModel

@Composable
fun MainTvApp(
    viewModel: MainTvViewModel,
    repository: DramaRepository,
    currentClipboardText: String,
    onTriggerCheckClipboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val featuredDramas by viewModel.featuredDramas.collectAsState()
    val categoryDramas by viewModel.categoryDramas.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val playHistory by viewModel.playHistory.collectAsState()
    val clipboardHistory by viewModel.clipboardHistory.collectAsState()
    val clipboardNotice by viewModel.clipboardNotice.collectAsState()
    val activePlayerState by viewModel.activePlayerState.collectAsState()
    val isParsing by viewModel.isParsing.collectAsState()
    val parseStatusMessage by viewModel.parseStatusMessage.collectAsState()

    // If player is active, show the TV Video Player in full screen
    if (activePlayerState != null) {
        val player = activePlayerState!!
        BackHandler {
            viewModel.closePlayer()
        }
        TvVideoPlayer(
            playerState = player,
            onClose = { viewModel.closePlayer() },
            onEpisodeSelected = { epIndex -> viewModel.switchEpisode(epIndex) },
            onNextEpisode = { viewModel.playNextEpisode() },
            onPrevEpisode = { viewModel.playPrevEpisode() },
            onSaveProgress = { posMs, durMs -> viewModel.saveProgress(posMs, durMs) }
        )
        return
    }

    // Main TV Navigation and View
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TV Top Bar
            TvTopBar(
                currentTab = currentTab,
                onTabSelected = { tab -> viewModel.selectTab(tab) }
            )

            // Content Area based on Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_transition"
                ) { tab ->
                    when (tab) {
                        TvNavTab.HOME -> {
                            HomeScreen(
                                dramas = featuredDramas,
                                onDramaSelected = { drama -> viewModel.playDrama(drama) }
                            )
                        }
                        TvNavTab.CLIPBOARD -> {
                            ClipboardParserScreen(
                                clipboardHistory = clipboardHistory,
                                currentClipboardText = currentClipboardText,
                                isParsing = isParsing,
                                statusMessage = parseStatusMessage,
                                onReadClipboardAndParse = {
                                    onTriggerCheckClipboard()
                                    if (currentClipboardText.isNotBlank()) {
                                        viewModel.manualParseAndPlay(currentClipboardText)
                                    }
                                },
                                onParseText = { text -> viewModel.manualParseAndPlay(text) },
                                onPlayHistoryItem = { item ->
                                    viewModel.manualParseAndPlay(item.rawText)
                                },
                                onClearHistory = { viewModel.clearClipboardHistory() }
                            )
                        }
                        TvNavTab.HISTORY -> {
                            HistoryScreen(
                                playHistory = playHistory,
                                onPlayHistoryItem = { hist -> viewModel.playFromHistory(hist) },
                                onClearAll = { viewModel.clearAllHistory() }
                            )
                        }
                        TvNavTab.CATEGORIES -> {
                            CategoriesScreen(
                                categories = repository.getCategories(),
                                selectedCategory = selectedCategory,
                                dramas = categoryDramas,
                                onSelectCategory = { cat -> viewModel.selectCategory(cat) },
                                onDramaSelected = { drama -> viewModel.playDrama(drama) }
                            )
                        }
                    }
                }
            }
        }

        // Automatic Clipboard Detection Notice Dialog
        clipboardNotice?.let { notice ->
            ClipboardNoticeDialog(
                notice = notice,
                onConfirmPlay = { viewModel.playFromClipboardNotice() },
                onDismiss = { viewModel.dismissClipboardNotice() }
            )
        }
    }
}
