package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PlayerSettings
import com.example.data.model.SortOption
import com.example.ui.theme.HoneyGold
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.VideoUiState
import com.example.ui.viewmodel.VideoViewModel

@Composable
fun SettingsScreen(
    uiState: VideoUiState,
    viewModel: VideoViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentSettings = uiState.settings

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSeekDialog by remember { mutableStateOf(false) }
    var showOrientationDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showDeviceCapabilities by remember { mutableStateOf(false) }

    if (showDeviceCapabilities) {
        DeviceCapabilitiesScreen(onBack = { showDeviceCapabilities = false })
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Playback Section
            SettingsSectionHeader(title = "Playback")
            SettingsCard {
                SettingsClickableItem(
                    icon = Icons.Default.Speed,
                    title = "Default Playback Speed",
                    subtitle = "${currentSettings.defaultSpeed}x",
                    onClick = { showSpeedDialog = true }
                )
                SettingsDivider()
                SettingsClickableItem(
                    icon = Icons.Default.FastForward,
                    title = "Seek Interval",
                    subtitle = "${currentSettings.defaultSeekIntervalMs / 1000} seconds",
                    onClick = { showSeekDialog = true }
                )
                SettingsDivider()
                SettingsSwitchItem(
                    icon = Icons.Default.PlayCircle,
                    title = "Auto-Resume Playback",
                    subtitle = "Resume videos from where you left off",
                    checked = currentSettings.autoResume,
                    onCheckedChange = { viewModel.updateSettings(currentSettings.copy(autoResume = it)) }
                )
                SettingsDivider()
                SettingsSwitchItem(
                    icon = Icons.Default.PlayCircle,
                    title = "Auto-Play Next Video",
                    subtitle = "Automatically play the next video in sequence",
                    checked = currentSettings.autoPlayNext,
                    onCheckedChange = { viewModel.updateSettings(currentSettings.copy(autoPlayNext = it)) }
                )
            }

            // Player Section
            SettingsSectionHeader(title = "Player & Gestures")
            SettingsCard {
                SettingsSwitchItem(
                    icon = Icons.Default.Gesture,
                    title = "Gesture Controls",
                    subtitle = "Swipe for volume, brightness, and double-tap to seek",
                    checked = currentSettings.gesturesEnabled,
                    onCheckedChange = { viewModel.updateSettings(currentSettings.copy(gesturesEnabled = it)) }
                )
                SettingsDivider()
                SettingsClickableItem(
                    icon = Icons.Default.ScreenRotation,
                    title = "Screen Orientation",
                    subtitle = currentSettings.defaultOrientation.replaceFirstChar { it.uppercase() },
                    onClick = { showOrientationDialog = true }
                )
            }

            // Subtitles Section
            SettingsSectionHeader(title = "Subtitles")
            SettingsCard {
                SettingsSwitchItem(
                    icon = Icons.Default.Subtitles,
                    title = "Enable Subtitles",
                    subtitle = "Automatically load subtitles when available",
                    checked = currentSettings.subtitlesEnabled,
                    onCheckedChange = { viewModel.updateSettings(currentSettings.copy(subtitlesEnabled = it)) }
                )
                if (currentSettings.subtitlesEnabled) {
                    SettingsDivider()
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Subtitle Text Size",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${currentSettings.subtitleSizeSp.toInt()} sp",
                                fontSize = 14.sp,
                                color = HoneyGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = currentSettings.subtitleSizeSp,
                            onValueChange = { viewModel.updateSettings(currentSettings.copy(subtitleSizeSp = it)) },
                            valueRange = 12f..28f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = HoneyGold,
                                activeTrackColor = HoneyGold
                            )
                        )
                    }
                }
            }

            // Library Section
            SettingsSectionHeader(title = "Library & Appearance")
            SettingsCard {
                SettingsClickableItem(
                    icon = Icons.Default.GridView,
                    title = "Default View Mode",
                    subtitle = if (currentSettings.defaultViewMode == "grid") "Grid View" else "List View",
                    onClick = { viewModel.toggleViewMode() }
                )
                SettingsDivider()
                SettingsClickableItem(
                    icon = Icons.Default.Palette,
                    title = "Theme",
                    subtitle = when (uiState.themeMode) {
                        ThemeMode.AMOLED -> "AMOLED Black (True Black)"
                        ThemeMode.DARK -> "Dark Theme"
                        ThemeMode.SYSTEM -> "System Default"
                    },
                    onClick = { viewModel.toggleThemeMode() }
                )
                SettingsDivider()
                SettingsClickableItem(
                    icon = Icons.Default.Refresh,
                    title = "Rescan Media Library",
                    subtitle = "Scan device storage for new and updated videos",
                    onClick = {
                        viewModel.refreshVideos()
                        Toast.makeText(context, "Scanning library...", Toast.LENGTH_SHORT).show()
                    }
                )
                SettingsDivider()
                SettingsClickableItem(
                    icon = Icons.Default.Info,
                    title = "Device & Codec Capabilities",
                    subtitle = "Inspect hardware decoders, display refresh rate & HDR",
                    onClick = { showDeviceCapabilities = true }
                )
            }

            // Privacy & Data
            SettingsSectionHeader(title = "Privacy & Storage")
            SettingsCard {
                SettingsClickableItem(
                    icon = Icons.Default.History,
                    title = "Clear Playback History",
                    subtitle = "Reset progress and continue watching list",
                    onClick = { showClearHistoryDialog = true }
                )
                SettingsDivider()
                SettingsClickableItem(
                    icon = Icons.Default.Delete,
                    title = "Clear Thumbnail Cache",
                    subtitle = "Free up temporary cached data",
                    onClick = { showClearCacheDialog = true }
                )
                SettingsDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = HoneyGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "100% Offline & Private",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "HONEY Player works entirely offline. No user data, videos, or history are uploaded anywhere.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // App Version Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "HONEY Player v1.0.0",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HoneyGold
                    )
                    Text(
                        text = "Professional Android Video Player",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Playback Speed Dialog
    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Default Playback Speed", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    speeds.forEach { sp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings(currentSettings.copy(defaultSpeed = sp))
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSettings.defaultSpeed == sp,
                                onClick = {
                                    viewModel.updateSettings(currentSettings.copy(defaultSpeed = sp))
                                    showSpeedDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = HoneyGold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "${sp}x", fontSize = 16.sp)
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

    // Seek Interval Dialog
    if (showSeekDialog) {
        val intervals = listOf(5000L, 10000L, 15000L, 30000L)
        AlertDialog(
            onDismissRequest = { showSeekDialog = false },
            title = { Text("Seek Interval", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    intervals.forEach { ms ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings(currentSettings.copy(defaultSeekIntervalMs = ms))
                                    showSeekDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSettings.defaultSeekIntervalMs == ms,
                                onClick = {
                                    viewModel.updateSettings(currentSettings.copy(defaultSeekIntervalMs = ms))
                                    showSeekDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = HoneyGold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "${ms / 1000} seconds", fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSeekDialog = false }) {
                    Text("Close", color = HoneyGold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Orientation Dialog
    if (showOrientationDialog) {
        val options = listOf("auto" to "Auto-rotate with Sensor", "landscape" to "Force Landscape", "portrait" to "Force Portrait")
        AlertDialog(
            onDismissRequest = { showOrientationDialog = false },
            title = { Text("Default Orientation", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    options.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings(currentSettings.copy(defaultOrientation = key))
                                    showOrientationDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSettings.defaultOrientation == key,
                                onClick = {
                                    viewModel.updateSettings(currentSettings.copy(defaultOrientation = key))
                                    showOrientationDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = HoneyGold)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOrientationDialog = false }) {
                    Text("Close", color = HoneyGold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Clear History Dialog
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Playback History", fontWeight = FontWeight.Bold) },
            text = { Text("This will reset all playback progress for watched videos. Favorites will remain preserved.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearPlaybackHistory()
                        showClearHistoryDialog = false
                        Toast.makeText(context, "Playback history cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = HoneyGold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Clear Cache Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Thumbnail Cache", fontWeight = FontWeight.Bold) },
            text = { Text("This will clear cached video thumbnails. Thumbnails will be re-generated next time you browse your library.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCache()
                        showClearCacheDialog = false
                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HoneyGold)
                ) {
                    Text("Clear", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel", color = HoneyGold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = HoneyGold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HoneyGold,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HoneyGold,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = HoneyGold
            )
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
