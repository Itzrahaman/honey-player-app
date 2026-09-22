package com.example.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.VideoItem
import com.example.ui.components.UnsupportedVideoDialog
import com.example.ui.theme.HoneyGold
import com.example.ui.viewmodel.VideoViewModel
import kotlinx.coroutines.Job
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

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.settings

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

    // Player state
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var bufferedPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(video.durationMs) }
    var playbackSpeed by remember { mutableFloatStateOf(settings.defaultSpeed) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var showUnsupportedDialog by remember { mutableStateOf(false) }
    var showResumePrompt by remember { mutableStateOf(false) }
    var savedPositionToResume by remember { mutableLongStateOf(0L) }
    var repeatMode by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }

    // Audio & Subtitle Sheets
    var showAudioSheet by remember { mutableStateOf(false) }
    var showSubtitleSheet by remember { mutableStateOf(false) }
    var availableAudioTracks by remember { mutableStateOf<List<AudioTrackInfo>>(emptyList()) }
    var availableSubtitleTracks by remember { mutableStateOf<List<SubtitleTrackInfo>>(emptyList()) }
    var currentCues by remember { mutableStateOf<List<Cue>>(emptyList()) }

    // Sleep Timer
    var sleepTimerRemainingMinutes by remember { mutableStateOf<Int?>(null) }
    var sleepTimerJob by remember { mutableStateOf<Job?>(null) }

    // Resize modes: 0=Fit, 1=Zoom/Crop, 2=Fill/Stretch
    var resizeModeIndex by remember { mutableIntStateOf(0) }
    val resizeModes = listOf(
        AspectRatioFrameLayout.RESIZE_MODE_FIT to "Fit",
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM to "Zoom / Crop",
        AspectRatioFrameLayout.RESIZE_MODE_FILL to "Stretch"
    )

    // Software fallback switch
    var useSoftwareDecoderOnly by remember { mutableStateOf(false) }

    // Build ExoPlayer with HoneyPlayerEngine
    var exoPlayer by remember {
        mutableStateOf(
            HoneyPlayerEngine.buildExoPlayer(
                context = context,
                settings = settings,
                forceSoftwareDecoders = useSoftwareDecoderOnly
            )
        )
    }

    // Handle Sleep Timer countdown
    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        sleepTimerRemainingMinutes = minutes
        if (minutes != null && minutes > 0) {
            Toast.makeText(context, "Sleep timer set for $minutes min", Toast.LENGTH_SHORT).show()
            sleepTimerJob = scope.launch {
                var remaining = minutes
                while (remaining > 0) {
                    delay(60000L)
                    remaining--
                    sleepTimerRemainingMinutes = remaining
                }
                exoPlayer.pause()
                sleepTimerRemainingMinutes = null
                Toast.makeText(context, "Sleep timer reached: Playback paused.", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(context, "Sleep timer turned off", Toast.LENGTH_SHORT).show()
        }
    }

    // Setup media item and prepare
    fun prepareMedia(player: ExoPlayer) {
        val mediaItem = HoneyPlayerEngine.buildMediaItem(video.contentUri, video.mimeType)
        player.setMediaItem(mediaItem)
        player.setPlaybackSpeed(playbackSpeed)
        player.prepare()
        player.play()
    }

    // Check saved position on video load
    LaunchedEffect(video.contentUri) {
        playbackError = null
        showUnsupportedDialog = false
        val savedRecord = viewModel.getSavedRecord(video.contentUri)
        if (savedRecord != null && savedRecord.isPartiallyWatched) {
            savedPositionToResume = savedRecord.positionMs
            showResumePrompt = true
            playbackSpeed = savedRecord.playbackSpeed
        } else {
            showResumePrompt = false
            savedPositionToResume = 0L
        }
        prepareMedia(exoPlayer)
    }

    // ExoPlayer listener attachment
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
                        // Refresh track information
                        availableAudioTracks = HoneyPlayerEngine.getAvailableAudioTracks(exoPlayer)
                        availableSubtitleTracks = HoneyPlayerEngine.getAvailableSubtitleTracks(exoPlayer)
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        viewModel.playNextVideo()
                    }
                    Player.STATE_IDLE -> isBuffering = false
                }
            }

            override fun onCues(cueGroup: CueGroup) {
                currentCues = cueGroup.cues
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playbackError = error.localizedMessage ?: "Playback failed"
                showUnsupportedDialog = true
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            sleepTimerJob?.cancel()
            exoPlayer.removeListener(listener)
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            viewModel.savePlaybackPosition(video.contentUri, pos, dur, playbackSpeed)
            exoPlayer.release()
        }
    }

    // Lifecycle observer
    DisposableEffect(lifecycleOwner, settings.backgroundAudioEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (!settings.backgroundAudioEnabled) {
                        exoPlayer.pause()
                    }
                    viewModel.savePlaybackPosition(
                        video.contentUri,
                        exoPlayer.currentPosition,
                        exoPlayer.duration,
                        playbackSpeed
                    )
                }
                Lifecycle.Event.ON_RESUME -> {}
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Position & Buffer update polling loop: only poll when playing to conserve CPU and avoid UI thrashing
    LaunchedEffect(exoPlayer, isPlaying) {
        if (!isPlaying) {
            currentPositionMs = exoPlayer.currentPosition
            bufferedPositionMs = exoPlayer.bufferedPosition
            if (exoPlayer.duration > 0) {
                durationMs = exoPlayer.duration
            }
            return@LaunchedEffect
        }
        while (isActive) {
            currentPositionMs = exoPlayer.currentPosition
            bufferedPositionMs = exoPlayer.bufferedPosition
            if (exoPlayer.duration > 0) {
                durationMs = exoPlayer.duration
            }
            delay(500)
        }
    }

    // Auto-hide controls after 3.5 seconds
    LaunchedEffect(controlsVisible, isPlaying, isLocked) {
        if (controlsVisible && isPlaying && !isLocked) {
            delay(3500)
            controlsVisible = false
        }
    }

    val currentPlaylistIndex = playlist.indexOfFirst { it.contentUri == video.contentUri }
    val hasPrev = currentPlaylistIndex > 0
    val hasNext = currentPlaylistIndex != -1 && currentPlaylistIndex + 1 < playlist.size

    BackHandler {
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
            gesturesEnabled = settings.gesturesEnabled,
            gestureSensitivity = settings.gestureSensitivity,
            doubleTapSeekDurationMs = settings.doubleTapSeekDurationMs,
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
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        resizeMode = resizeModes[resizeModeIndex].first
                    }
                },
                update = { playerView ->
                    playerView.player = exoPlayer
                    playerView.resizeMode = resizeModes[resizeModeIndex].first
                },
                modifier = Modifier.fillMaxSize()
            )

            // Compose-rendered Subtitles with full customization
            SubtitleOverlay(
                cues = currentCues,
                settings = settings
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

        // Unsupported / Error Dialog
        if (showUnsupportedDialog) {
            UnsupportedVideoDialog(
                video = video,
                errorMessage = playbackError,
                onRetrySoftwareFallback = {
                    showUnsupportedDialog = false
                    playbackError = null
                    useSoftwareDecoderOnly = true
                    exoPlayer.release()
                    val newPlayer = HoneyPlayerEngine.buildExoPlayer(
                        context = context,
                        settings = settings,
                        forceSoftwareDecoders = true
                    )
                    exoPlayer = newPlayer
                    prepareMedia(newPlayer)
                    Toast.makeText(context, "Retrying with software decoder fallback...", Toast.LENGTH_SHORT).show()
                },
                onDismiss = {
                    showUnsupportedDialog = false
                    onBack()
                }
            )
        }

        // Resume playback prompt
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
                bufferedPositionMs = bufferedPositionMs,
                durationMs = durationMs,
                playbackSpeed = playbackSpeed,
                hasPreviousVideo = hasPrev,
                hasNextVideo = hasNext,
                controlsVisible = controlsVisible,
                repeatMode = repeatMode,
                sleepTimerRemainingMinutes = sleepTimerRemainingMinutes,
                seekIntervalSec = (settings.defaultSeekIntervalMs / 1000L).toInt().coerceAtLeast(1),
                aspectRatioModeName = resizeModes[resizeModeIndex].second,
                isHdr = video.displayName.contains("HDR", ignoreCase = true) || video.mimeType.contains("hdr", ignoreCase = true),
                backgroundAudioEnabled = settings.backgroundAudioEnabled,
                subtitlesEnabled = settings.subtitlesEnabled,
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
                onRewind = {
                    val delta = settings.defaultSeekIntervalMs
                    val newPos = (exoPlayer.currentPosition - delta).coerceAtLeast(0L)
                    exoPlayer.seekTo(newPos)
                },
                onForward = {
                    val delta = settings.defaultSeekIntervalMs
                    val newPos = (exoPlayer.currentPosition + delta).coerceAtMost(durationMs)
                    exoPlayer.seekTo(newPos)
                },
                onFrameStepBackward = {
                    val newPos = (exoPlayer.currentPosition - 42L).coerceAtLeast(0L)
                    exoPlayer.seekTo(newPos)
                },
                onFrameStepForward = {
                    val newPos = (exoPlayer.currentPosition + 42L).coerceAtMost(durationMs)
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
                    Toast.makeText(context, "Aspect: ${resizeModes[resizeModeIndex].second}", Toast.LENGTH_SHORT).show()
                },
                onRepeatToggle = {
                    repeatMode = when (repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                        Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                        else -> Player.REPEAT_MODE_OFF
                    }
                    exoPlayer.repeatMode = repeatMode
                    val label = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> "Repeat One"
                        Player.REPEAT_MODE_ALL -> "Repeat All"
                        else -> "Repeat Off"
                    }
                    Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
                },
                onSleepTimerSet = { mins ->
                    setSleepTimer(mins)
                },
                onShowVideoInfo = {
                    viewModel.showVideoInfo(video)
                },
                onAddToPlaylist = {
                    viewModel.showAddToPlaylistDialog(video)
                },
                onOpenAudioTracks = {
                    availableAudioTracks = HoneyPlayerEngine.getAvailableAudioTracks(exoPlayer)
                    showAudioSheet = true
                },
                onOpenSubtitles = {
                    availableSubtitleTracks = HoneyPlayerEngine.getAvailableSubtitleTracks(exoPlayer)
                    showSubtitleSheet = true
                },
                onToggleBackgroundAudio = {
                    val newBg = !settings.backgroundAudioEnabled
                    viewModel.updateSettings(settings.copy(backgroundAudioEnabled = newBg))
                    Toast.makeText(context, if (newBg) "Background playback enabled" else "Background playback disabled", Toast.LENGTH_SHORT).show()
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

        // Audio Track Sheet
        if (showAudioSheet) {
            AudioTrackBottomSheet(
                tracks = availableAudioTracks,
                onSelectTrack = { track ->
                    HoneyPlayerEngine.selectAudioTrack(exoPlayer, track)
                    availableAudioTracks = HoneyPlayerEngine.getAvailableAudioTracks(exoPlayer)
                    Toast.makeText(context, "Audio: ${track.label}", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showAudioSheet = false }
            )
        }

        // Subtitle Sheet
        if (showSubtitleSheet) {
            SubtitleBottomSheet(
                tracks = availableSubtitleTracks,
                subtitlesEnabled = settings.subtitlesEnabled,
                settings = settings,
                onSelectTrack = { track ->
                    HoneyPlayerEngine.selectSubtitleTrack(exoPlayer, track)
                    availableSubtitleTracks = HoneyPlayerEngine.getAvailableSubtitleTracks(exoPlayer)
                    val enabled = track != null
                    viewModel.updateSettings(settings.copy(subtitlesEnabled = enabled))
                },
                onLoadExternalSubtitle = { uri ->
                    HoneyPlayerEngine.addExternalSubtitle(context, exoPlayer, uri)
                    availableSubtitleTracks = HoneyPlayerEngine.getAvailableSubtitleTracks(exoPlayer)
                    viewModel.updateSettings(settings.copy(subtitlesEnabled = true))
                    Toast.makeText(context, "Subtitle loaded", Toast.LENGTH_SHORT).show()
                },
                onUpdateSettings = { newSettings ->
                    viewModel.updateSettings(newSettings)
                },
                onDismiss = { showSubtitleSheet = false }
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val minutes = (ms / 1000) / 60
    val seconds = (ms / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}
