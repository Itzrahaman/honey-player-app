package com.example.ui.player

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PlayerSettings
import com.example.ui.theme.HoneyGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTrackBottomSheet(
    tracks: List<AudioTrackInfo>,
    onSelectTrack: (AudioTrackInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = null,
                    tint = HoneyGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Audio Tracks",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${tracks.size} audio stream${if (tracks.size != 1) "s" else ""} available",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            if (tracks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No audio tracks detected",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tracks.forEach { track ->
                        val channelStr = when (track.channelCount) {
                            1 -> "Mono 1.0"
                            2 -> "Stereo 2.0"
                            6 -> "5.1 Surround"
                            8 -> "7.1 Surround"
                            else -> "${track.channelCount} ch"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (track.isSelected) HoneyGold.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    onSelectTrack(track)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.label,
                                    fontSize = 15.sp,
                                    fontWeight = if (track.isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (track.isSelected) HoneyGold else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$channelStr • ${track.sampleRate / 1000} kHz • ${track.language.uppercase()}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (track.isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = HoneyGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleBottomSheet(
    tracks: List<SubtitleTrackInfo>,
    subtitlesEnabled: Boolean,
    settings: PlayerSettings,
    onSelectTrack: (SubtitleTrackInfo?) -> Unit,
    onLoadExternalSubtitle: (Uri) -> Unit,
    onUpdateSettings: (PlayerSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Tracks, 1: Styling & Sync

    val openSubtitleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onLoadExternalSubtitle(uri)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = null,
                        tint = HoneyGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Subtitles",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .background(Color(0x1EFFFFFF), RoundedCornerShape(20.dp))
                        .padding(3.dp)
                ) {
                    TabPill(title = "Tracks", selected = selectedTab == 0) { selectedTab = 0 }
                    TabPill(title = "Style & Sync", selected = selectedTab == 1) { selectedTab = 1 }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTab == 0) {
                // Tracks Tab
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Disable Subtitles Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!subtitlesEnabled) HoneyGold.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable {
                                onSelectTrack(null)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SubtitlesOff,
                                contentDescription = null,
                                tint = if (!subtitlesEnabled) HoneyGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Off (Disable Subtitles)",
                                fontSize = 15.sp,
                                fontWeight = if (!subtitlesEnabled) FontWeight.Bold else FontWeight.Medium,
                                color = if (!subtitlesEnabled) HoneyGold else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (!subtitlesEnabled) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = HoneyGold)
                        }
                    }

                    // Available Tracks
                    tracks.forEach { track ->
                        val isTrackSelected = subtitlesEnabled && track.isSelected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isTrackSelected) HoneyGold.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    onSelectTrack(track)
                                    onDismiss()
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = track.label,
                                    fontSize = 15.sp,
                                    fontWeight = if (isTrackSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isTrackSelected) HoneyGold else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${track.language.uppercase()} • ${track.mimeType.substringAfterLast("/")}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isTrackSelected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = HoneyGold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Open external subtitle file button
                    OutlinedButton(
                        onClick = {
                            openSubtitleLauncher.launch(
                                arrayOf(
                                    "text/*",
                                    "application/x-subrip",
                                    "application/vtt",
                                    "application/octet-stream"
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = HoneyGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open External Subtitle File (.srt, .vtt, .ass)", color = HoneyGold)
                    }
                }
            } else {
                // Style & Sync Tab
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Subtitle Sync / Delay
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = HoneyGold, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Subtitle Sync Delay", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "${if (settings.subtitleDelayMs > 0) "+" else ""}${settings.subtitleDelayMs} ms",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = HoneyGold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onUpdateSettings(settings.copy(subtitleDelayMs = settings.subtitleDelayMs - 250L)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("-250 ms", fontSize = 12.sp, color = HoneyGold)
                            }
                            OutlinedButton(
                                onClick = { onUpdateSettings(settings.copy(subtitleDelayMs = 0L)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reset (0)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            OutlinedButton(
                                onClick = { onUpdateSettings(settings.copy(subtitleDelayMs = settings.subtitleDelayMs + 250L)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+250 ms", fontSize = 12.sp, color = HoneyGold)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // Text Size
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Text Size", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(text = "${settings.subtitleSizeSp.toInt()} sp", fontSize = 13.sp, color = HoneyGold, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = settings.subtitleSizeSp,
                            onValueChange = { onUpdateSettings(settings.copy(subtitleSizeSp = it)) },
                            valueRange = 12f..30f,
                            steps = 8,
                            colors = SliderDefaults.colors(thumbColor = HoneyGold, activeTrackColor = HoneyGold)
                        )
                    }

                    // Text Color
                    Column {
                        Text(text = "Text Color", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val colors = listOf(
                                "white" to Color.White,
                                "honey_gold" to HoneyGold,
                                "yellow" to Color(0xFFFFEB3B),
                                "cyan" to Color(0xFF00E5FF),
                                "green" to Color(0xFF69F0AE)
                            )
                            colors.forEach { (key, clr) ->
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(clr)
                                        .clickable { onUpdateSettings(settings.copy(subtitleColor = key)) }
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (settings.subtitleColor == key) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (clr == Color.White || clr == Color(0xFFFFEB3B)) Color.Black else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Background Style
                    Column {
                        Text(text = "Background Style", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val bgOptions = listOf(
                                "semi_black" to "Semi-Black",
                                "transparent" to "Shadow Only",
                                "solid_black" to "Solid Black"
                            )
                            bgOptions.forEach { (key, label) ->
                                val isSel = settings.subtitleBackground == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) HoneyGold else Color(0x22FFFFFF))
                                        .clickable { onUpdateSettings(settings.copy(subtitleBackground = key)) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) Color.Black else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TabPill(title: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) HoneyGold else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.Black else Color.White
        )
    }
}
