package com.example.ui.player

import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.HongguoGold
import com.example.ui.theme.HongguoRed
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvFocusBorder
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceElevated
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.util.tvFocusable
import com.example.viewmodel.ActivePlayerState
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun TvVideoPlayer(
    playerState: ActivePlayerState,
    onClose: () -> Unit,
    onEpisodeSelected: (Int) -> Unit,
    onNextEpisode: () -> Unit,
    onPrevEpisode: () -> Unit,
    onSaveProgress: (positionMs: Long, durationMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(1L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showControls by remember { mutableStateOf(true) }
    var showEpisodeDrawer by remember { mutableStateOf(false) }
    var seekNoticeText by remember { mutableStateOf<String?>(null) }

    val focusRequester = remember { FocusRequester() }

    val videoHeaders = remember {
        mapOf(
            "User-Agent" to "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        )
    }

    // Auto-hide controls after 4 seconds
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4500)
            showControls = false
        }
    }

    // Auto-hide seek notice text
    LaunchedEffect(seekNoticeText) {
        if (seekNoticeText != null) {
            delay(1500)
            seekNoticeText = null
        }
    }

    // Dynamic stream URL reload & periodic ticker to sync progress
    LaunchedEffect(playerState.currentStreamUrl) {
        currentPositionMs = 0L
        durationMs = 1L
        isBuffering = true
        errorMessage = null

        videoViewRef?.let { vv ->
            try {
                vv.stopPlayback()
                vv.setVideoURI(Uri.parse(playerState.currentStreamUrl), videoHeaders)
                vv.start()
            } catch (e: Exception) {
                errorMessage = "视频加载失败: ${e.message}"
            }
        }

        while (true) {
            delay(500)
            videoViewRef?.let { vv ->
                try {
                    if (vv.isPlaying) {
                        currentPositionMs = vv.currentPosition.toLong()
                        durationMs = maxOf(1L, vv.duration.toLong())
                        isBuffering = false
                        errorMessage = null
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    // Save progress every 5 seconds
    LaunchedEffect(currentPositionMs) {
        if (currentPositionMs > 1000L) {
            onSaveProgress(currentPositionMs, durationMs)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.let { vv ->
                try {
                    onSaveProgress(vv.currentPosition.toLong(), maxOf(1L, vv.duration.toLong()))
                    vv.stopPlayback()
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }

    fun applySpeed(speed: Float) {
        playbackSpeed = speed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayerRef?.let { mp ->
                    mp.playbackParams = mp.playbackParams.setSpeed(speed)
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun seekBy(deltaSeconds: Int) {
        videoViewRef?.let { vv ->
            try {
                val newPos = (vv.currentPosition + deltaSeconds * 1000).coerceIn(0, maxOf(0, vv.duration))
                vv.seekTo(newPos)
                currentPositionMs = newPos.toLong()
                seekNoticeText = if (deltaSeconds > 0) "快进 +${deltaSeconds}秒" else "快退 ${deltaSeconds}秒"
                showControls = true
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp) {
                    when (keyEvent.key) {
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.MediaPlayPause -> {
                            videoViewRef?.let { vv ->
                                if (vv.isPlaying) {
                                    vv.pause()
                                    isPlaying = false
                                    showControls = true
                                } else {
                                    vv.start()
                                    isPlaying = true
                                }
                            }
                            true
                        }
                        Key.DirectionLeft -> {
                            seekBy(-10)
                            true
                        }
                        Key.DirectionRight -> {
                            seekBy(10)
                            true
                        }
                        Key.DirectionUp -> {
                            showEpisodeDrawer = !showEpisodeDrawer
                            showControls = true
                            true
                        }
                        Key.DirectionDown -> {
                            showControls = true
                            true
                        }
                        Key.MediaNext -> {
                            onNextEpisode()
                            true
                        }
                        Key.MediaPrevious -> {
                            onPrevEpisode()
                            true
                        }
                        Key.Back -> {
                            if (showEpisodeDrawer) {
                                showEpisodeDrawer = false
                                true
                            } else if (showControls) {
                                showControls = false
                                true
                            } else {
                                onClose()
                                true
                            }
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
    ) {
        // Core VideoView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    setOnPreparedListener { mp ->
                        mediaPlayerRef = mp
                        mp.isLooping = false
                        isBuffering = false
                        durationMs = maxOf(1L, duration.toLong())
                        if (playerState.initialPositionMs > 0L) {
                            seekTo(playerState.initialPositionMs.toInt())
                        }
                        start()
                        isPlaying = true
                        applySpeed(playbackSpeed)
                    }
                    setOnCompletionListener {
                        onNextEpisode()
                    }
                    setOnErrorListener { _, what, extra ->
                        isBuffering = false
                        errorMessage = "视频源加载异常 (错误码: $what, 附带码: $extra)。若为防盗链或时效性链接，请重新解析。"
                        true // handled
                    }
                    setVideoURI(Uri.parse(playerState.currentStreamUrl), videoHeaders)
                    videoViewRef = this
                }
            },
            update = { vv ->
                videoViewRef = vv
            }
        )

        // Error Notice overlay if stream fails
        errorMessage?.let { errText ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.92f))
                    .border(1.5.dp, HongguoRed, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(420.dp)
                ) {
                    Text(
                        text = "⚠️ 播放提示",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = HongguoGold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errText,
                        fontSize = 13.sp,
                        color = Color.White,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "当前地址: ${playerState.currentStreamUrl}",
                        fontSize = 11.sp,
                        color = TvTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(HongguoRed)
                                .tvFocusable(
                                    shape = RoundedCornerShape(8.dp),
                                    focusedBorderColor = Color.White,
                                    scaleOnFocus = 1.08f,
                                    onClick = {
                                        errorMessage = null
                                        isBuffering = true
                                        videoViewRef?.let { vv ->
                                            vv.stopPlayback()
                                            vv.setVideoURI(Uri.parse(playerState.currentStreamUrl), videoHeaders)
                                            vv.start()
                                        }
                                    }
                                )
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "重试播放",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(TvSurfaceElevated)
                                .tvFocusable(
                                    shape = RoundedCornerShape(8.dp),
                                    focusedBorderColor = TvTextSecondary,
                                    scaleOnFocus = 1.08f,
                                    onClick = onClose
                                )
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "返回列表",
                                fontSize = 14.sp,
                                color = TvTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Loading / Buffering Indicator
        if (isBuffering && errorMessage == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = HongguoRed,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "红果高速缓冲中...",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = TvTextPrimary
                    )
                }
            }
        }

        // On-screen Seek Notice Badge
        seekNoticeText?.let { text ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .border(1.dp, HongguoRed, RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Overlay Controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 32.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(TvSurface)
                            .tvFocusable(
                                shape = CircleShape,
                                focusedBorderColor = HongguoRed,
                                scaleOnFocus = 1.15f,
                                onClick = onClose
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = playerState.dramaTitle,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "正在播放 第 ${playerState.currentEpisode} 集 / 共 ${playerState.totalEpisodes} 集",
                                fontSize = 13.sp,
                                color = HongguoGold
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            val isDemo = playerState.currentStreamUrl.contains("storage.googleapis.com")
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isDemo) TvSurfaceElevated else HongguoRed)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isDemo) "示例演练源" else "真实视频源",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "视频流: ${playerState.currentStreamUrl}",
                            fontSize = 11.sp,
                            color = TvTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(600.dp)
                        )
                    }
                }

                // Bottom Controls Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.9f),
                                    Color.Black
                                )
                            )
                        )
                        .padding(horizontal = 32.dp, vertical = 20.dp)
                ) {
                    // Progress Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPositionMs),
                            fontSize = 13.sp,
                            color = TvTextSecondary,
                            fontWeight = FontWeight.Medium
                        )

                        Slider(
                            value = (currentPositionMs.toFloat() / maxOf(1L, durationMs).toFloat()).coerceIn(0f, 1f),
                            onValueChange = { frac ->
                                val targetPos = (frac * durationMs).toLong()
                                videoViewRef?.seekTo(targetPos.toInt())
                                currentPositionMs = targetPos
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = HongguoRed,
                                activeTrackColor = HongguoRed,
                                inactiveTrackColor = TvSurfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp)
                        )

                        Text(
                            text = formatTime(durationMs),
                            fontSize = 13.sp,
                            color = TvTextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Episode
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(TvSurface)
                                .tvFocusable(
                                    shape = CircleShape,
                                    focusedBorderColor = TvFocusBorder,
                                    scaleOnFocus = 1.15f,
                                    onClick = onPrevEpisode
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "上一集",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Fast Rewind 10s
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(TvSurface)
                                .tvFocusable(
                                    shape = CircleShape,
                                    focusedBorderColor = TvFocusBorder,
                                    scaleOnFocus = 1.15f,
                                    onClick = { seekBy(-10) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "快退10秒",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // Play/Pause Main Button
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(HongguoRed)
                                .tvFocusable(
                                    shape = CircleShape,
                                    focusedBorderColor = Color.White,
                                    scaleOnFocus = 1.15f,
                                    onClick = {
                                        videoViewRef?.let { vv ->
                                            if (vv.isPlaying) {
                                                vv.pause()
                                                isPlaying = false
                                            } else {
                                                vv.start()
                                                isPlaying = true
                                            }
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "暂停" else "播放",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // Fast Forward 10s
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(TvSurface)
                                .tvFocusable(
                                    shape = CircleShape,
                                    focusedBorderColor = TvFocusBorder,
                                    scaleOnFocus = 1.15f,
                                    onClick = { seekBy(10) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "快进10秒",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Next Episode
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(TvSurface)
                                .tvFocusable(
                                    shape = CircleShape,
                                    focusedBorderColor = TvFocusBorder,
                                    scaleOnFocus = 1.15f,
                                    onClick = onNextEpisode
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "下一集",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(28.dp))

                        // Speed Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(TvSurface)
                                .tvFocusable(
                                    shape = RoundedCornerShape(20.dp),
                                    focusedBorderColor = TvFocusBorder,
                                    scaleOnFocus = 1.08f,
                                    onClick = {
                                        val nextSpeed = when (playbackSpeed) {
                                            1.0f -> 1.25f
                                            1.25f -> 1.5f
                                            1.5f -> 2.0f
                                            else -> 1.0f
                                        }
                                        applySpeed(nextSpeed)
                                    }
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "倍速",
                                    tint = HongguoGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${playbackSpeed}x",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Episode List Drawer Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (showEpisodeDrawer) HongguoRed else TvSurface)
                                .tvFocusable(
                                    shape = RoundedCornerShape(20.dp),
                                    focusedBorderColor = TvFocusBorder,
                                    scaleOnFocus = 1.08f,
                                    onClick = { showEpisodeDrawer = !showEpisodeDrawer }
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.FormatListNumbered,
                                    contentDescription = "选集",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "选集",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Episode Selection Drawer (Triggered by Up or 选集 button)
        AnimatedVisibility(
            visible = showEpisodeDrawer,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .border(1.dp, TvSurfaceElevated, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(vertical = 16.dp, horizontal = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "剧集列表 (遥控器左右键切换选集)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvTextPrimary
                        )
                        Text(
                            text = "当前: 第 ${playerState.currentEpisode} 集",
                            fontSize = 13.sp,
                            color = HongguoGold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(playerState.episodes) { ep ->
                            val isCurrent = ep.index == playerState.currentEpisode
                            Box(
                                modifier = Modifier
                                    .size(width = 72.dp, height = 48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCurrent) HongguoRed else TvSurfaceElevated)
                                    .tvFocusable(
                                        shape = RoundedCornerShape(8.dp),
                                        focusedBorderColor = TvFocusBorder,
                                        scaleOnFocus = 1.12f,
                                        onClick = {
                                            onEpisodeSelected(ep.index)
                                            showEpisodeDrawer = false
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${ep.index}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
