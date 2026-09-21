package com.example.ui.player

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HoneyGold
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

enum class GestureHudType {
    NONE,
    BRIGHTNESS,
    VOLUME,
    SEEK,
    DOUBLE_TAP_REWIND,
    DOUBLE_TAP_FORWARD
}

@Composable
fun PlayerGestureOverlay(
    modifier: Modifier = Modifier,
    isLocked: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onSingleTap: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onDoubleTapSeek: (seekDeltaMs: Long) -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }

    var hudType by remember { mutableStateOf(GestureHudType.NONE) }
    var brightnessLevel by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f }
                ?: try {
                    Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) / 255f
                } catch (_: Exception) {
                    0.5f
                }
        )
    }
    var currentVolumeLevel by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume.coerceAtLeast(1))
    }
    var seekDeltaMs by remember { mutableLongStateOf(0L) }
    var targetSeekPositionMs by remember { mutableLongStateOf(currentPositionMs) }

    // Auto-dismiss HUD
    LaunchedEffect(hudType) {
        if (hudType != GestureHudType.NONE && hudType != GestureHudType.SEEK) {
            delay(1200)
            hudType = GestureHudType.NONE
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isLocked) {
                if (isLocked) {
                    detectTapGestures(
                        onTap = { onSingleTap() }
                    )
                } else {
                    detectTapGestures(
                        onTap = { onSingleTap() },
                        onDoubleTap = { offset ->
                            val isRightSide = offset.x > size.width / 2
                            if (isRightSide) {
                                hudType = GestureHudType.DOUBLE_TAP_FORWARD
                                onDoubleTapSeek(10000L)
                            } else {
                                hudType = GestureHudType.DOUBLE_TAP_REWIND
                                onDoubleTapSeek(-10000L)
                            }
                        }
                    )
                }
            }
            .pointerInput(isLocked) {
                if (!isLocked) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            val isLeftSide = offset.x < size.width / 2
                            if (isLeftSide) {
                                hudType = GestureHudType.BRIGHTNESS
                            } else {
                                hudType = GestureHudType.VOLUME
                            }
                        },
                        onDragEnd = {
                            // Leave it to LaunchedEffect to dismiss
                        },
                        onDragCancel = {
                            hudType = GestureHudType.NONE
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val isLeftSide = change.position.x < size.width / 2
                            val delta = -dragAmount / 600f // Swipe up = increase

                            if (isLeftSide) {
                                hudType = GestureHudType.BRIGHTNESS
                                val newBrightness = (brightnessLevel + delta).coerceIn(0.01f, 1.0f)
                                brightnessLevel = newBrightness
                                activity?.let { act ->
                                    val lp = act.window.attributes
                                    lp.screenBrightness = newBrightness
                                    act.window.attributes = lp
                                }
                            } else {
                                hudType = GestureHudType.VOLUME
                                val newVol = (currentVolumeLevel + delta).coerceIn(0f, 1f)
                                currentVolumeLevel = newVol
                                val streamVol = (newVol * maxVolume).toInt()
                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, streamVol, 0)
                            }
                        }
                    )
                }
            }
            .pointerInput(isLocked, currentPositionMs, durationMs) {
                if (!isLocked && durationMs > 0) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            hudType = GestureHudType.SEEK
                            seekDeltaMs = 0L
                            targetSeekPositionMs = currentPositionMs
                        },
                        onDragEnd = {
                            onSeekTo(targetSeekPositionMs)
                            hudType = GestureHudType.NONE
                        },
                        onDragCancel = {
                            hudType = GestureHudType.NONE
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            hudType = GestureHudType.SEEK
                            // Sensitivity: 1 pixel ~ 100ms
                            val deltaMs = (dragAmount * 120f).toLong()
                            seekDeltaMs += deltaMs
                            targetSeekPositionMs = (currentPositionMs + seekDeltaMs).coerceIn(0L, durationMs)
                        }
                    )
                }
            }
    ) {
        // Child video player / controls content
        content()

        // Visual HUD Feedback Overlays
        AnimatedVisibility(
            visible = hudType != GestureHudType.NONE,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            when (hudType) {
                GestureHudType.BRIGHTNESS -> {
                    HudIndicator(
                        icon = {
                            Icon(
                                imageVector = if (brightnessLevel > 0.5f) Icons.Default.BrightnessMedium else Icons.Default.BrightnessLow,
                                contentDescription = "Brightness",
                                tint = HoneyGold,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        title = "Brightness",
                        progress = brightnessLevel,
                        valueText = "${(brightnessLevel * 100).toInt()}%"
                    )
                }
                GestureHudType.VOLUME -> {
                    HudIndicator(
                        icon = {
                            Icon(
                                imageVector = if (currentVolumeLevel <= 0.05f) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = "Volume",
                                tint = HoneyGold,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        title = "Volume",
                        progress = currentVolumeLevel,
                        valueText = "${(currentVolumeLevel * 100).toInt()}%"
                    )
                }
                GestureHudType.SEEK -> {
                    SeekHudIndicator(
                        seekDeltaMs = seekDeltaMs,
                        targetMs = targetSeekPositionMs,
                        durationMs = durationMs
                    )
                }
                GestureHudType.DOUBLE_TAP_REWIND -> {
                    DoubleTapSeekIndicator(
                        icon = Icons.Default.FastRewind,
                        text = "-10 sec"
                    )
                }
                GestureHudType.DOUBLE_TAP_FORWARD -> {
                    DoubleTapSeekIndicator(
                        icon = Icons.Default.FastForward,
                        text = "+10 sec"
                    )
                }
                GestureHudType.NONE -> {}
            }
        }
    }
}

@Composable
private fun HudIndicator(
    icon: @Composable () -> Unit,
    title: String,
    progress: Float,
    valueText: String
) {
    Box(
        modifier = Modifier
            .background(Color(0xCC121316), RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$title: $valueText",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(140.dp)
                    .height(6.dp),
                color = HoneyGold,
                trackColor = Color(0x33FFFFFF),
            )
        }
    }
}

@Composable
private fun SeekHudIndicator(
    seekDeltaMs: Long,
    targetMs: Long,
    durationMs: Long
) {
    val sign = if (seekDeltaMs >= 0) "+" else "-"
    val absDeltaSec = abs(seekDeltaMs) / 1000

    Box(
        modifier = Modifier
            .background(Color(0xCC121316), RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$sign${absDeltaSec}s",
                color = HoneyGold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${formatTime(targetMs)} / ${formatTime(durationMs)}",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DoubleTapSeekIndicator(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(Color(0xCC181A20), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HoneyGold,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun formatTime(ms: Long): String {
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
