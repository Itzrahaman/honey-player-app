package com.example.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.VideoItem
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.HoneyGold
import com.example.ui.viewmodel.VideoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    video: VideoItem,
    playlist: List<VideoItem>,
    viewModel: VideoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // Keep screen awake during playback
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }

    // Orientation handling
    var isLandscape by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    fun toggleOrientation() {
        activity?.let { act ->
            if (isLandscape) {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                isLandscape = false
            } else {
                act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                isLandscape = true
            }
        }
    }

    // ExoPlayer state
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(video.durationMs) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var showResumePrompt by remember { mutableStateOf(false) }
    var savedPositionToResume by remember { mutableLongStateOf(0L) }

    // Resize modes: RESIZE_MODE_FIT (0), RESIZE_MODE_ZOOM (3), RESIZE_MODE_FILL (4)
    var resizeModeIndex by remember { mutableIntStateOf(0) }
    val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT,
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        AspectRatioFrameLayout.RESIZE_MODE_FILL
    )

    // Build ExoPlayer
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    // Check saved position on video load
    LaunchedEffect(video.contentUri) {
        playbackError = null
        val savedRecord = viewModel.getSavedRecord(video.contentUri)
        if (savedRecord != null && savedRecord.isPartiallyWatched) {
            savedPositionToResume = savedRecord.positionMs
            showResumePrompt = true
            playbackSpeed = savedRecord.playbackSpeed
        } else {
            showResumePrompt = false
            savedPositionToResume = 0L
        }

        val mediaItem = MediaItem.fromUri(Uri.parse(video.contentUri))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.setPlaybackSpeed(playbackSpeed)
        exoPlayer.prepare()
    }

    // ExoPlayer listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isBuffering = false
                        durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        // Automatically play next in playlist if available
                        viewModel.playNextVideo()
                    }
                    Player.STATE_IDLE -> isBuffering = false
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playbackError = "Unable to play video: ${error.localizedMessage ?: "Unsupported format or file not found."}"
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            // Save position on dispose
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            viewModel.savePlaybackPosition(video.contentUri, pos, dur, playbackSpeed)
            exoPlayer.release()
        }
    }

    // Lifecycle observer to pause/resume playback
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                    viewModel.savePlaybackPosition(
                        video.contentUri,
                        exoPlayer.currentPosition,
                        exoPlayer.duration,
                        playbackSpeed
                    )
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (!isLocked) {
                        // Keep paused if user paused
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Progress update loop
    LaunchedEffect(exoPlayer, isPlaying) {
        while (isActive) {
            currentPositionMs = exoPlayer.currentPosition
            if (exoPlayer.duration > 0) {
                durationMs = exoPlayer.duration
            }
            delay(500)
        }
    }

    // Auto-hide controls after 3.5 seconds
    LaunchedEffect(controlsVisible, isPlaying) {
        if (controlsVisible && isPlaying && !isLocked) {
            delay(3500)
            controlsVisible = false
        }
    }

    val currentPlaylistIndex = playlist.indexOfFirst { it.contentUri == video.contentUri }
    val hasPrev = currentPlaylistIndex > 0
    val hasNext = currentPlaylistIndex != -1 && currentPlaylistIndex + 1 < playlist.size

    BackHandler {
        // Save progress before closing
        viewModel.savePlaybackPosition(
            video.contentUri,
            exoPlayer.currentPosition,
            exoPlayer.duration,
            playbackSpeed
        )
        onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Gestures & Video Surface
        PlayerGestureOverlay(
            modifier = Modifier.fillMaxSize(),
            isLocked = isLocked,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            onSingleTap = {
                controlsVisible = !controlsVisible
            },
            onSeekTo = { seekTargetMs ->
                exoPlayer.seekTo(seekTargetMs)
            },
            onDoubleTapSeek = { deltaMs ->
                val newPos = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, durationMs)
                exoPlayer.seekTo(newPos)
            }
        ) {
            // AndroidView embedding Media3 PlayerView
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false // We provide custom Compose controls overlay
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        resizeMode = resizeModes[resizeModeIndex]
                    }
                },
                update = { playerView ->
                    playerView.resizeMode = resizeModes[resizeModeIndex]
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Buffering Indicator
        if (isBuffering && playbackError == null) {
            CircularProgressIndicator(
                color = HoneyGold,
                modifier = Modifier
                    .size(56.dp)
                    .align(Alignment.Center)
            )
        }

        // Error message banner
        if (playbackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xEE000000))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Playback Error",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = playbackError ?: "Unknown error",
                        color = Color(0xFFB0B0B0),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row {
                        OutlinedButton(onClick = onBack) {
                            Text("Go Back", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(
                            onClick = {
                                playbackError = null
                                exoPlayer.prepare()
                                exoPlayer.play()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HoneyGold)
                        ) {
                            Text("Retry", color = Color.Black)
                        }
                    }
                }
            }
        }

        // Resume playback snackbar/prompt
        AnimatedVisibility(
            visible = showResumePrompt && playbackError == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp, start = 16.dp, end = 16.dp)
        ) {
            Snackbar(
                modifier = Modifier.padding(horizontal = 8.dp),
                containerColor = Color(0xEE1E2129),
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
                action = {
                    Row {
                        TextButton(
                            onClick = {
                                showResumePrompt = false
                                exoPlayer.seekTo(0L)
                                exoPlayer.play()
                            }
                        ) {
                            Text("Start Over", color = Color(0xFFB0B0B0))
                        }
                        TextButton(
                            onClick = {
                                showResumePrompt = false
                                exoPlayer.seekTo(savedPositionToResume)
                                exoPlayer.play()
                            }
                        ) {
                            Text("Resume", color = HoneyGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = HoneyGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Resume from ${formatTime(savedPositionToResume)}?",
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Controls overlay
        if (playbackError == null) {
            PlayerControlsOverlay(
                title = video.title,
                resolutionTag = video.resolutionLabel,
                isPlaying = isPlaying,
                isLocked = isLocked,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                playbackSpeed = playbackSpeed,
                hasPreviousVideo = hasPrev,
                hasNextVideo = hasNext,
                controlsVisible = controlsVisible,
                onPlayPauseToggle = {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                },
                onSeekTo = { seekMs ->
                    exoPlayer.seekTo(seekMs)
                },
                onRewind10s = {
                    val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                    exoPlayer.seekTo(newPos)
                },
                onForward10s = {
                    val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationMs)
                    exoPlayer.seekTo(newPos)
                },
                onPreviousClick = {
                    viewModel.playPreviousVideo()
                },
                onNextClick = {
                    viewModel.playNextVideo()
                },
                onLockToggle = {
                    isLocked = !isLocked
                    controlsVisible = true
                },
                onSpeedChange = { newSpeed ->
                    playbackSpeed = newSpeed
                    exoPlayer.setPlaybackSpeed(newSpeed)
                },
                onOrientationToggle = {
                    toggleOrientation()
                },
                onAspectRatioToggle = {
                    resizeModeIndex = (resizeModeIndex + 1) % resizeModes.size
                },
                onBackClick = {
                    viewModel.savePlaybackPosition(
                        video.contentUri,
                        exoPlayer.currentPosition,
                        exoPlayer.duration,
                        playbackSpeed
                    )
                    onBack()
                }
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val minutes = (ms / 1000) / 60
    val seconds = (ms / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
