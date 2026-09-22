package com.example.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HoneyGold
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.max

val PLAYBACK_SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f, 4.0f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerControlsOverlay(
    modifier: Modifier = Modifier,
    title: String,
    resolutionTag: String,
    isPlaying: Boolean,
    isLocked: Boolean,
    currentPositionMs: Long,
    bufferedPositionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    hasPreviousVideo: Boolean,
    hasNextVideo: Boolean,
    controlsVisible: Boolean,
    repeatMode: Int,
    sleepTimerRemainingMinutes: Int?,
    seekIntervalSec: Int = 10,
    aspectRatioModeName: String = "Fit",
    isHdr: Boolean = false,
    backgroundAudioEnabled: Boolean = false,
    subtitlesEnabled: Boolean = true,
    onPlayPauseToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onFrameStepBackward: () -> Unit,
    onFrameStepForward: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onLockToggle: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onOrientationToggle: () -> Unit,
    onAspectRatioToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onSleepTimerSet: (Int?) -> Unit,
    onShowVideoInfo: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenAudioTracks: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onToggleBackgroundAudio: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableFloatStateOf(currentPositionMs.toFloat()) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showMoreBottomSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showRemainingTime by remember { mutableStateOf(false) }

    // When locked, only show unlock button when controls are requested
    if (isLocked) {
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Surface(
                    onClick = onLockToggle,
                    shape = CircleShape,
                    color = Color(0xCC181A20),
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .statusBarsPadding()
                        .size(52.dp)
                        .testTag("unlock_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Unlock Screen",
                            tint = HoneyGold,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
        return
    }

    // Unlocked Controls
    AnimatedVisibility(
        visible = controlsVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xDD000000),
                            Color(0x33000000),
                            Color(0x22000000),
                            Color(0xEE000000)
                        )
                    )
                )
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("player_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (resolutionTag.isNotBlank()) {
                            Text(
                                text = resolutionTag,
                                color = HoneyGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (isHdr) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = HoneyGold,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "HDR",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // Audio Track Picker
                IconButton(onClick = onOpenAudioTracks) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = "Audio Tracks",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Subtitle Picker
                IconButton(onClick = onOpenSubtitles) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = "Subtitles",
                        tint = if (subtitlesEnabled) HoneyGold else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Aspect Ratio Quick Toggle
                IconButton(onClick = onAspectRatioToggle) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Aspect Ratio",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Screen Lock Button
                IconButton(
                    onClick = onLockToggle,
                    modifier = Modifier.testTag("lock_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock Screen",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // More Options Menu
                IconButton(onClick = { showMoreBottomSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Center Controls: Rewind, Previous, Play/Pause, Next, Forward (+ Frame stepping if paused)
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Video
                    IconButton(
                        onClick = onPreviousClick,
                        enabled = hasPreviousVideo,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Video",
                            tint = if (hasPreviousVideo) Color.White else Color(0x55FFFFFF),
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Rewind
                    IconButton(
                        onClick = onRewind,
                        modifier = Modifier.size(52.dp).testTag("rewind_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Rewind $seekIntervalSec seconds",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Main Play/Pause Button
                    Surface(
                        onClick = onPlayPauseToggle,
                        shape = CircleShape,
                        color = HoneyGold,
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("player_play_pause_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.Black,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }

                    // Fast Forward
                    IconButton(
                        onClick = onForward,
                        modifier = Modifier.size(52.dp).testTag("forward_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Forward $seekIntervalSec seconds",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Next Video
                    IconButton(
                        onClick = onNextClick,
                        enabled = hasNextVideo,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Video",
                            tint = if (hasNextVideo) Color.White else Color(0x55FFFFFF),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Frame-stepping controls when paused
                if (!isPlaying && durationMs > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0x33121316), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(onClick = onFrameStepBackward)
                        ) {
                            Icon(imageVector = Icons.Default.FirstPage, contentDescription = null, tint = HoneyGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Step -1 Frame", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        Box(modifier = Modifier.width(1.dp).height(12.dp).background(Color(0x33FFFFFF)))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(onClick = onFrameStepForward)
                        ) {
                            Text(text = "Step +1 Frame", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.Default.LastPage, contentDescription = null, tint = HoneyGold, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Bottom Bar: Scrubber + Timing + Speed + Orientation + Aspect Ratio
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                val displayPosition = if (isScrubbing) scrubPositionMs.toLong() else currentPositionMs

                // Progress Bar with Buffered Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Buffered Track Indicator
                    if (durationMs > 0 && bufferedPositionMs > 0) {
                        val bufferFraction = (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .padding(horizontal = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(bufferFraction)
                                    .height(4.dp)
                                    .background(Color(0x44FFFFFF), RoundedCornerShape(2.dp))
                            )
                        }
                    }

                    Slider(
                        value = if (durationMs > 0) {
                            (if (isScrubbing) scrubPositionMs else currentPositionMs.toFloat()).coerceIn(0f, durationMs.toFloat())
                        } else 0f,
                        onValueChange = { newPos ->
                            isScrubbing = true
                            scrubPositionMs = newPos
                        },
                        onValueChangeFinished = {
                            isScrubbing = false
                            onSeekTo(scrubPositionMs.toLong())
                        },
                        valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = HoneyGold,
                            activeTrackColor = HoneyGold,
                            inactiveTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_seek_bar")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time Label (Click to toggle total vs remaining)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showRemainingTime = !showRemainingTime }
                    ) {
                        val remainingMs = max(0L, durationMs - displayPosition)
                        val rightTimeText = if (showRemainingTime) "-${formatDuration(remainingMs)}" else formatDuration(durationMs)
                        Text(
                            text = "${formatDuration(displayPosition)} / $rightTimeText",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Repeat Button
                        IconButton(
                            onClick = onRepeatToggle,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (repeatMode == 1) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                contentDescription = "Repeat Mode",
                                tint = if (repeatMode != 0) HoneyGold else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Playback Speed Tag
                        Box(
                            modifier = Modifier
                                .background(Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                .clickable { showSpeedDialog = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${playbackSpeed}x",
                                color = HoneyGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Aspect Ratio Tag
                        Box(
                            modifier = Modifier
                                .background(Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                .clickable { onAspectRatioToggle() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = aspectRatioModeName,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // PiP Button (if supported)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            IconButton(
                                onClick = {
                                    (context as? Activity)?.let { act ->
                                        val params = PictureInPictureParams.Builder()
                                            .setAspectRatio(Rational(16, 9))
                                            .build()
                                        act.enterPictureInPictureMode(params)
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPicture,
                                    contentDescription = "Picture in Picture",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        // Orientation Toggle
                        IconButton(
                            onClick = onOrientationToggle,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "Rotate Screen",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Playback Speed Selection Dialog
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = {
                Text(
                    text = "Playback Speed",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    PLAYBACK_SPEEDS.forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSpeedChange(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${speed}x",
                                fontSize = 15.sp,
                                fontWeight = if (speed == playbackSpeed) FontWeight.Bold else FontWeight.Normal,
                                color = if (speed == playbackSpeed) HoneyGold else MaterialTheme.colorScheme.onSurface
                            )
                            if (speed == playbackSpeed) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = HoneyGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Close", color = HoneyGold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Sleep Timer Selection Dialog
    if (showSleepTimerDialog) {
        val timerOptions = listOf(
            null to "Turn Off Timer",
            15 to "15 minutes",
            30 to "30 minutes",
            45 to "45 minutes",
            60 to "60 minutes",
            90 to "90 minutes",
            120 to "120 minutes"
        )
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    timerOptions.forEach { (mins, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSleepTimerSet(mins)
                                    showSleepTimerDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 15.sp,
                                fontWeight = if (sleepTimerRemainingMinutes == mins) FontWeight.Bold else FontWeight.Normal,
                                color = if (sleepTimerRemainingMinutes == mins) HoneyGold else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Close", color = HoneyGold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // More Options Bottom Sheet
    if (showMoreBottomSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showMoreBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Player Options",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Video Info
                PlayerOptionItem(
                    icon = Icons.Default.Info,
                    title = "Video Information",
                    subtitle = "Detailed codecs, bitrate, frame rate, resolution",
                    onClick = {
                        showMoreBottomSheet = false
                        onShowVideoInfo()
                    }
                )

                // Background Audio Playback
                PlayerOptionItem(
                    icon = Icons.Default.Headphones,
                    title = "Background Audio Playback",
                    subtitle = if (backgroundAudioEnabled) "Active: Playing in background" else "Off",
                    onClick = {
                        onToggleBackgroundAudio()
                        showMoreBottomSheet = false
                    }
                )

                // Add to Playlist
                PlayerOptionItem(
                    icon = Icons.Default.PlaylistAdd,
                    title = "Add to Playlist",
                    subtitle = "Save this video into a custom playlist",
                    onClick = {
                        showMoreBottomSheet = false
                        onAddToPlaylist()
                    }
                )

                // Sleep Timer
                PlayerOptionItem(
                    icon = Icons.Default.Bedtime,
                    title = "Sleep Timer",
                    subtitle = if (sleepTimerRemainingMinutes != null) "Active: $sleepTimerRemainingMinutes min remaining" else "Off",
                    onClick = {
                        showMoreBottomSheet = false
                        showSleepTimerDialog = true
                    }
                )

                // Playback Speed
                PlayerOptionItem(
                    icon = Icons.Default.Speed,
                    title = "Playback Speed (${playbackSpeed}x)",
                    subtitle = "Adjust playback rate between 0.25x and 4.0x",
                    onClick = {
                        showMoreBottomSheet = false
                        showSpeedDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PlayerOptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HoneyGold,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSec = ms / 1000L
    val sec = totalSec % 60L
    val min = (totalSec / 60L) % 60L
    val hrs = totalSec / 3600L
    val secStr = if (sec < 10) "0$sec" else sec.toString()
    val minStr = if (min < 10) "0$min" else min.toString()
    return if (hrs > 0) {
        "$hrs:$minStr:$secStr"
    } else {
        "$minStr:$secStr"
    }
}
