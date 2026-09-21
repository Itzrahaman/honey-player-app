package com.example.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HoneyGold
import java.util.Locale
import java.util.concurrent.TimeUnit

val PLAYBACK_SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerControlsOverlay(
    modifier: Modifier = Modifier,
    title: String,
    resolutionTag: String,
    isPlaying: Boolean,
    isLocked: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    playbackSpeed: Float,
    hasPreviousVideo: Boolean,
    hasNextVideo: Boolean,
    controlsVisible: Boolean,
    onPlayPauseToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onRewind10s: () -> Unit,
    onForward10s: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onLockToggle: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onOrientationToggle: () -> Unit,
    onAspectRatioToggle: () -> Unit,
    onBackClick: () -> Unit
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableFloatStateOf(currentPositionMs.toFloat()) }
    var showSpeedDialog by remember { mutableStateOf(false) }

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
                            Color(0xB3000000),
                            Color(0x33000000),
                            Color(0x33000000),
                            Color(0xCC000000)
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
                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (resolutionTag.isNotBlank()) {
                        Text(
                            text = resolutionTag,
                            color = HoneyGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Aspect Ratio Toggle
                IconButton(onClick = onAspectRatioToggle) {
                    Icon(
                        imageVector = Icons.Default.AspectRatio,
                        contentDescription = "Aspect Ratio",
                        tint = Color.White
                    )
                }

                // Playback Speed Button
                IconButton(onClick = { showSpeedDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Playback Speed",
                        tint = Color.White
                    )
                }

                // Screen Lock Button
                IconButton(
                    onClick = onLockToggle,
                    modifier = Modifier.testTag("lock_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LockOpen,
                        contentDescription = "Lock Controls",
                        tint = HoneyGold
                    )
                }
            }

            // Center Playback Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous
                IconButton(
                    onClick = onPreviousClick,
                    enabled = hasPreviousVideo,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Video",
                        tint = if (hasPreviousVideo) Color.White else Color(0x66FFFFFF),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Rewind 10s
                IconButton(
                    onClick = onRewind10s,
                    modifier = Modifier.size(52.dp).testTag("rewind_10s_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "Rewind 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
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

                // Fast Forward 10s
                IconButton(
                    onClick = onForward10s,
                    modifier = Modifier.size(52.dp).testTag("forward_10s_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "Forward 10 seconds",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = onNextClick,
                    enabled = hasNextVideo,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Video",
                        tint = if (hasNextVideo) Color.White else Color(0x66FFFFFF),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Bottom Bar: Scrubber + Timing + Speed + Orientation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val displayPosition = if (isScrubbing) scrubPositionMs.toLong() else currentPositionMs

                // Progress Bar / Slider
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
                        inactiveTrackColor = Color(0x44FFFFFF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .testTag("player_seek_bar")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time Label
                    Text(
                        text = "${formatDuration(displayPosition)} / ${formatDuration(durationMs)}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Playback Speed Indicator Tag
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

                        Spacer(modifier = Modifier.width(12.dp))

                        // Orientation / Fullscreen Toggle
                        IconButton(
                            onClick = onOrientationToggle,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScreenRotation,
                                contentDescription = "Rotate Screen",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
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
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${speed}x",
                                fontSize = 16.sp,
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
            }
        )
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "00:00"
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
