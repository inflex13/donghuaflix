package com.donghuaflix.ui.player

import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import com.donghuaflix.domain.model.StreamType
import com.donghuaflix.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun PlayerScreen(
    showId: Int,
    episodeNumber: Int,
    website: String?,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // ExoPlayer instance
    val player = remember { ExoPlayer.Builder(context).build() }

    // Auto-hide controls
    LaunchedEffect(uiState.showControls) {
        if (uiState.showControls) {
            delay(5000)
            viewModel.hideControls()
        }
    }

    // Set media source when stream URL changes
    LaunchedEffect(uiState.streamUrl) {
        val url = uiState.streamUrl ?: return@LaunchedEffect
        val dataSourceFactory = DefaultHttpDataSource.Factory()

        // Build MediaItem with subtitle tracks
        val mediaItemBuilder = MediaItem.Builder().setUri(url)

        // Add subtitle tracks (prefer English)
        val subtitleConfigs = uiState.subtitles.map { sub ->
            MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(sub.url))
                .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_SUBRIP)
                .setLanguage(sub.language)
                .setLabel(sub.label)
                .setSelectionFlags(
                    if (sub.language == "en") androidx.media3.common.C.SELECTION_FLAG_DEFAULT
                    else 0
                )
                .build()
        }
        if (subtitleConfigs.isNotEmpty()) {
            mediaItemBuilder.setSubtitleConfigurations(subtitleConfigs)
        }

        val mediaItem = mediaItemBuilder.build()

        val mediaSource = when (uiState.streamType) {
            StreamType.HLS -> HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
            StreamType.DASH -> DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
            StreamType.MP4, StreamType.UNKNOWN -> ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
        }

        player.setMediaSource(mediaSource)
        if (uiState.resumePositionMs > 0) {
            player.seekTo(uiState.resumePositionMs)
        }
        player.prepare()
        player.playWhenReady = true
    }

    // Listen for playback end
    LaunchedEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_ENDED) {
                    viewModel.onPlaybackEnded()
                }
            }
        }
        player.addListener(listener)
    }

    // If playback ended and no auto-play, go back to detail
    LaunchedEffect(uiState.playbackEnded) {
        if (uiState.playbackEnded) {
            onBack()
        }
    }

    // Save progress periodically — only when we have valid data
    LaunchedEffect(player) {
        while (true) {
            delay(10_000)
            if (player.isPlaying && player.duration > 0 && player.duration < 86_400_000) {
                viewModel.saveProgress(player.currentPosition, player.duration)
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            if (player.duration > 0 && player.duration < 86_400_000 && player.currentPosition > 0) {
                viewModel.saveProgress(player.currentPosition, player.duration)
            }
            player.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { event ->
                if (event.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) return@onKeyEvent false
                when (event.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        if (!uiState.showControls) {
                            viewModel.showControls()
                        } else {
                            val handled = viewModel.activateFocusedControl(player.isPlaying)
                            if (!handled) {
                                // PLAY_PAUSE: toggle playback
                                if (player.isPlaying) player.pause() else player.play()
                            }
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (!uiState.showControls || viewModel.isFocusedOnSeekBar()) {
                            viewModel.showControls()
                            player.seekTo(maxOf(0, player.currentPosition - 10_000))
                        } else {
                            viewModel.focusPrevControl()
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (!uiState.showControls || viewModel.isFocusedOnSeekBar()) {
                            viewModel.showControls()
                            player.seekTo(player.currentPosition + 10_000)
                        } else {
                            viewModel.focusNextControl()
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (uiState.showControls) {
                            viewModel.focusPrevControl()
                        } else {
                            viewModel.showControls()
                        }
                        true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (uiState.showControls) {
                            viewModel.focusNextControl()
                        } else {
                            viewModel.showControls()
                        }
                        true
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        if (uiState.showControls) {
                            viewModel.hideControls()
                        } else {
                            if (player.duration > 0 && player.duration < 86_400_000 && player.currentPosition > 0) {
                                viewModel.saveProgress(player.currentPosition, player.duration)
                            }
                            onBack()
                        }
                        true
                    }
                    else -> false
                }
            }
            .focusable(),
    ) {
        // Video player
        if (uiState.streamUrl != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = false
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Loading state
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Loading stream...", color = Color.White, fontSize = 18.sp)
            }
        }

        // Error state
        if (uiState.error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(uiState.error!!, color = SecondaryColor, fontSize = 16.sp)
            }
        }

        // Controls overlay
        AnimatedVisibility(
            visible = uiState.showControls,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
            ) {
                // Top-left: show title + episode
                Text(
                    text = "${uiState.showTitle} - EP ${uiState.episodeNumber}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(24.dp),
                )

                // Top-right: help text
                Text(
                    text = "\u25B2\u25BC Navigate  OK Select  \u25C0\u25B6 Seek",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp),
                )

                // Bottom: progress bar, time, control pills
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Seek bar area — highlighted when focused
                    val seekFocused = uiState.focusedControl == PlayerControl.SEEK_BAR
                    val progress = if (player.duration > 0) {
                        player.currentPosition.toFloat() / player.duration
                    } else 0f

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .then(
                                if (seekFocused) Modifier.border(2.dp, AccentFuchsia, RoundedCornerShape(6.dp))
                                else Modifier
                            )
                            .background(if (seekFocused) AccentFuchsia.copy(alpha = 0.1f) else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        // Progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (seekFocused) 6.dp else 4.dp)
                                .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(3.dp)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .background(AccentFuchsia, RoundedCornerShape(3.dp)),
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Time display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(formatTime(player.currentPosition), color = if (seekFocused) AccentFuchsia else Color.White, fontSize = 12.sp)
                            if (seekFocused) Text("◀▶ Seek", color = AccentFuchsia, fontSize = 10.sp)
                            Text(formatTime(player.duration), color = TextMuted, fontSize = 12.sp)
                        }
                    }

                    // Control pills row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Play/Pause pill
                        ControlPill(
                            text = if (player.isPlaying) "❚❚  Pause" else "▶  Play",
                            isFocused = uiState.focusedControl == PlayerControl.PLAY_PAUSE,
                            isActive = true,
                        )

                        // Prev EP pill (conditional)
                        if (uiState.hasPrevEpisode) {
                            ControlPill(
                                text = "◀  Prev EP",
                                isFocused = uiState.focusedControl == PlayerControl.PREV_EP,
                                isActive = true,
                            )
                        }

                        // Next EP pill (conditional)
                        if (uiState.hasNextEpisode) {
                            ControlPill(
                                text = "Next EP  ▶",
                                isFocused = uiState.focusedControl == PlayerControl.NEXT_EP,
                                isActive = true,
                            )
                        }

                        // Autoplay pill
                        ControlPill(
                            text = if (uiState.autoPlayNext) "Autoplay ON" else "Autoplay OFF",
                            isFocused = uiState.focusedControl == PlayerControl.AUTOPLAY,
                            isActive = uiState.autoPlayNext,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlPill(
    text: String,
    isFocused: Boolean,
    isActive: Boolean,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isFocused) {
                    Modifier.border(2.dp, AccentFuchsia, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            )
            .background(
                when {
                    isFocused -> AccentFuchsia.copy(alpha = 0.25f)
                    isActive -> AccentPurple.copy(alpha = 0.2f)
                    else -> SurfaceCard
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isFocused -> AccentFuchsia
                isActive -> AccentPurple
                else -> TextSecondary
            },
        )
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
