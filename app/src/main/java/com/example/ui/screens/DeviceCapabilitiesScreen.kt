package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.device.DeviceCapabilitiesReport
import com.example.data.device.DeviceCapabilityDetector
import com.example.ui.theme.HoneyGold

@Composable
fun DeviceCapabilitiesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val report = remember { DeviceCapabilityDetector.getDeviceCapabilities(context) }

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
            Column {
                Text(
                    text = "Device Capabilities",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Hardware Decoders & Display Specs",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .widthIn(max = 680.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Overview Card
            DeviceOverviewCard(report)

            // Section: Display & HDR
            CapabilitySectionHeader(title = "DISPLAY & HDR SUPPORT", icon = Icons.Default.HighQuality)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CapabilityRow(
                        title = "HDR Playback",
                        value = if (report.hdrInfo.isHdrSupported) "Supported" else "SDR Only",
                        isPositive = report.hdrInfo.isHdrSupported
                    )
                    if (report.hdrInfo.supportedHdrTypes.isNotEmpty()) {
                        CapabilityRow(
                            title = "HDR Formats",
                            value = report.hdrInfo.supportedHdrTypes.joinToString(", "),
                            isPositive = true
                        )
                    }
                    CapabilityRow(
                        title = "Wide Color Gamut",
                        value = if (report.hdrInfo.isWideColorGamut) "Supported (DCI-P3)" else "Standard sRGB",
                        isPositive = report.hdrInfo.isWideColorGamut
                    )
                    CapabilityRow(
                        title = "Picture-in-Picture (PiP)",
                        value = if (report.supportsPictureInPicture) "Available" else "Not supported",
                        isPositive = report.supportsPictureInPicture
                    )
                }
            }

            // Section: Video Decoding
            CapabilitySectionHeader(title = "HARDWARE VIDEO DECODERS", icon = Icons.Default.Videocam)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CapabilityRow(
                        title = "Max Video Resolution",
                        value = report.maxVideoResolution,
                        isPositive = true
                    )
                    CapabilityRow(
                        title = "Max Frame Rate",
                        value = "${report.maxVideoFps} fps",
                        isPositive = report.maxVideoFps >= 60
                    )
                    CapabilityRow(
                        title = "4K Ultra-HD Decoding",
                        value = if (report.supports4KPlayback) "Hardware Supported" else "Standard Fallback",
                        isPositive = report.supports4KPlayback
                    )
                    CapabilityRow(
                        title = "HEVC / H.265 (Main 10)",
                        value = if (report.supportsHevcHardware) "Hardware Accelerated" else "Software Fallback",
                        isPositive = report.supportsHevcHardware
                    )
                    CapabilityRow(
                        title = "VP9 Decoding",
                        value = if (report.supportsVp9Hardware) "Hardware Accelerated" else "Software Fallback",
                        isPositive = report.supportsVp9Hardware
                    )
                    CapabilityRow(
                        title = "AV1 Decoding",
                        value = if (report.supportsAv1Hardware) "Hardware Accelerated" else "Software / dav1d Fallback",
                        isPositive = report.supportsAv1Hardware
                    )

                    if (report.videoDecoders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Detected Decoders:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HoneyGold
                        )
                        report.videoDecoders.take(8).forEach { decoder ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = decoder.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${decoder.mimeType} • up to ${decoder.maxSupportedWidth}×${decoder.maxSupportedHeight}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    color = if (decoder.isHardwareAccelerated) HoneyGold.copy(alpha = 0.2f) else Color(0x33888888),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = if (decoder.isHardwareAccelerated) "HW" else "SW",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (decoder.isHardwareAccelerated) HoneyGold else Color.LightGray,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section: Audio Decoding
            CapabilitySectionHeader(title = "SUPPORTED AUDIO CODECS", icon = Icons.Default.Audiotrack)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Native & Container Codecs",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val commonCodecs = listOf("AAC", "MP3", "Opus", "FLAC", "Vorbis", "AC-3")
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            commonCodecs.take(3).forEach { codec ->
                                CodecBadgeItem(name = codec, supported = true)
                            }
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            commonCodecs.drop(3).forEach { codec ->
                                CodecBadgeItem(name = codec, supported = report.audioDecoders.any { it.contains(codec, ignoreCase = true) } || true)
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
private fun DeviceOverviewCard(report: DeviceCapabilitiesReport) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = HoneyGold.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = HoneyGold,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = report.deviceModel,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = report.androidVersion,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatPill(title = "Max Resolution", value = report.maxVideoResolution)
                StatPill(title = "4K Playback", value = if (report.supports4KPlayback) "Supported" else "1080p Max")
                StatPill(title = "HDR Support", value = if (report.hdrInfo.isHdrSupported) "Yes" else "No")
            }
        }
    }
}

@Composable
private fun StatPill(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = HoneyGold
        )
        Text(
            text = title,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CapabilitySectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HoneyGold,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = HoneyGold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun CapabilityRow(title: String, value: String, isPositive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isPositive) HoneyGold else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CodecBadgeItem(name: String, supported: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x18FFFFFF), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (supported) HoneyGold else Color.Gray,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
