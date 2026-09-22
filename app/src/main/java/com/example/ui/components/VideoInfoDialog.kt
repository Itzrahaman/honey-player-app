package com.example.ui.components

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Hd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.device.DeviceCapabilityDetector
import com.example.data.model.VideoItem
import com.example.ui.theme.HoneyGold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DetailedMediaMetadata(
    val frameRate: String = "Unknown",
    val bitrate: String = "Unknown",
    val videoCodec: String = "Unknown",
    val audioCodec: String = "Unknown",
    val audioDetails: String = "Unknown",
    val hdrType: String = "SDR (Standard)",
    val containerFormat: String = "Unknown",
    val decoderStatus: String = "Standard Decoder"
)

@Composable
fun VideoInfoDialog(
    video: VideoItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var metadata by remember { mutableStateOf(DetailedMediaMetadata()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(video.contentUri) {
        withContext(Dispatchers.IO) {
            val extracted = extractFullMetadata(context, video)
            metadata = extracted
            isLoading = false
        }
    }

    val dateFormatted = remember(video.dateAdded, video.dateModified) {
        val timestamp = if (video.dateAdded > 0) video.dateAdded * 1000L else video.dateModified * 1000L
        if (timestamp > 0) {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        } else "Unknown"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 520.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = HoneyGold.copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = HoneyGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Video Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Technical Media Specifications",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Section: File & Storage
                InfoCategoryTitle("FILE & STORAGE")
                InfoItem(label = "File Name", value = video.displayName)
                InfoItem(label = "Container", value = metadata.containerFormat)
                InfoItem(label = "File Size", value = video.formattedSize)
                InfoItem(label = "Duration", value = video.formattedDuration)
                InfoItem(label = "Folder", value = video.folderName)
                if (video.filePath.isNotBlank()) {
                    InfoItem(label = "File Path", value = video.filePath)
                }
                InfoItem(label = "Date Added", value = dateFormatted)

                Spacer(modifier = Modifier.height(6.dp))

                // Section: Video Stream
                InfoCategoryTitle("VIDEO STREAM")
                val resString = if (video.width > 0 && video.height > 0) {
                    "${video.width} × ${video.height} (${video.resolutionLabel})"
                } else video.resolutionLabel
                InfoItem(label = "Resolution", value = resString)
                InfoItem(label = "Video Codec", value = metadata.videoCodec)
                InfoItem(label = "Frame Rate", value = metadata.frameRate)
                InfoItem(label = "Bitrate", value = metadata.bitrate)
                InfoItem(label = "Color & Dynamic Range", value = metadata.hdrType)
                InfoItem(label = "Hardware Decoder", value = metadata.decoderStatus)

                Spacer(modifier = Modifier.height(6.dp))

                // Section: Audio Stream
                InfoCategoryTitle("AUDIO STREAM")
                InfoItem(label = "Audio Codec", value = metadata.audioCodec)
                InfoItem(label = "Audio Channels & Rate", value = metadata.audioDetails)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = HoneyGold, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun InfoCategoryTitle(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = HoneyGold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(0.42f)
            )
            Text(
                text = value,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 16.sp,
                modifier = Modifier.weight(0.58f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    }
}

private fun extractFullMetadata(context: Context, video: VideoItem): DetailedMediaMetadata {
    var fpsStr = "Unknown"
    var bitrateStr = "Unknown"
    var vCodecStr = "Unknown"
    var aCodecStr = "None detected"
    var aDetailsStr = "None"
    var hdrStr = "SDR (Standard)"
    var containerStr = getContainerFromExtension(video.displayName)
    var decoderStr = "Hardware Accelerated"

    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, Uri.parse(video.contentUri))

        // Bitrate
        val rawBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
        if (rawBitrate != null && rawBitrate > 0) {
            bitrateStr = if (rawBitrate >= 1_000_000) {
                String.format(Locale.US, "%.2f Mbps", rawBitrate / 1_000_000.0)
            } else {
                "${rawBitrate / 1000} kbps"
            }
        }

        // Frame rate
        val rawFps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
        if (!rawFps.isNullOrBlank()) {
            fpsStr = "$rawFps fps"
        }

        // Mime / Codec
        val rawMime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        if (!rawMime.isNullOrBlank()) {
            vCodecStr = cleanCodecName(rawMime)
            if (rawMime.contains("matroska", ignoreCase = true)) containerStr = "Matroska (.mkv)"
            if (rawMime.contains("mp4", ignoreCase = true)) containerStr = "MPEG-4 (.mp4)"
            if (rawMime.contains("webm", ignoreCase = true)) containerStr = "WebM (.webm)"
        }

        // HDR detection from MediaMetadataRetriever (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val colorStandard = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_STANDARD)?.toIntOrNull()
            val colorTransfer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_TRANSFER)?.toIntOrNull()
            if (colorTransfer == MediaFormat.COLOR_TRANSFER_ST2084) {
                hdrStr = "HDR10 / PQ (ST 2084)"
            } else if (colorTransfer == MediaFormat.COLOR_TRANSFER_HLG) {
                hdrStr = "HLG (Hybrid Log-Gamma)"
            }
        }
    } catch (_: Exception) {
    } finally {
        try { retriever.release() } catch (_: Exception) {}
    }

    // Try MediaExtractor for precise track inspection
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(context, Uri.parse(video.contentUri), null)
        val trackCount = extractor.trackCount
        for (i in 0 until trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue

            if (mime.startsWith("video/")) {
                vCodecStr = cleanCodecName(mime)
                if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                    val rate = format.getInteger(MediaFormat.KEY_FRAME_RATE)
                    fpsStr = "$rate fps"
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    if (format.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) {
                        val transfer = format.getInteger(MediaFormat.KEY_COLOR_TRANSFER)
                        if (transfer == MediaFormat.COLOR_TRANSFER_ST2084) {
                            hdrStr = "HDR10 / PQ"
                        } else if (transfer == MediaFormat.COLOR_TRANSFER_HLG) {
                            hdrStr = "HLG"
                        }
                    }
                }
            } else if (mime.startsWith("audio/")) {
                aCodecStr = cleanAudioCodecName(mime)
                val channels = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                } else 2
                val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                } else 48000
                val channelLabel = when (channels) {
                    1 -> "Mono (1.0)"
                    2 -> "Stereo (2.0)"
                    6 -> "5.1 Surround"
                    8 -> "7.1 Surround"
                    else -> "$channels channels"
                }
                aDetailsStr = "$channelLabel @ ${sampleRate / 1000} kHz"
            }
        }
    } catch (_: Exception) {
    } finally {
        try { extractor.release() } catch (_: Exception) {}
    }

    if (vCodecStr.contains("HEVC", ignoreCase = true) || vCodecStr.contains("H.265", ignoreCase = true)) {
        decoderStr = "Hardware HEVC (ExoPlayer)"
    } else if (vCodecStr.contains("AVC", ignoreCase = true) || vCodecStr.contains("H.264", ignoreCase = true)) {
        decoderStr = "Hardware AVC/H.264"
    } else if (vCodecStr.contains("VP9", ignoreCase = true)) {
        decoderStr = "Hardware VP9"
    } else if (vCodecStr.contains("AV1", ignoreCase = true)) {
        decoderStr = "Hardware / Optimized AV1"
    }

    return DetailedMediaMetadata(
        frameRate = fpsStr,
        bitrate = bitrateStr,
        videoCodec = vCodecStr,
        audioCodec = aCodecStr,
        audioDetails = aDetailsStr,
        hdrType = hdrStr,
        containerFormat = containerStr,
        decoderStatus = decoderStr
    )
}

private fun cleanCodecName(mime: String): String {
    return when {
        mime.equals("video/hevc", ignoreCase = true) -> "HEVC / H.265"
        mime.equals("video/avc", ignoreCase = true) -> "AVC / H.264"
        mime.equals("video/x-vnd.on2.vp9", ignoreCase = true) -> "VP9"
        mime.equals("video/av01", ignoreCase = true) -> "AV1 (AOMedia)"
        mime.equals("video/x-vnd.on2.vp8", ignoreCase = true) -> "VP8"
        mime.equals("video/mp4v-es", ignoreCase = true) -> "MPEG-4 Part 2"
        mime.equals("video/3gpp", ignoreCase = true) -> "H.263 / 3GPP"
        else -> mime.removePrefix("video/").uppercase()
    }
}

private fun cleanAudioCodecName(mime: String): String {
    return when {
        mime.contains("mp4a", ignoreCase = true) || mime.contains("aac", ignoreCase = true) -> "AAC (Advanced Audio Coding)"
        mime.contains("opus", ignoreCase = true) -> "Opus"
        mime.contains("vorbis", ignoreCase = true) -> "Ogg Vorbis"
        mime.contains("flac", ignoreCase = true) -> "FLAC Lossless"
        mime.contains("mpeg", ignoreCase = true) || mime.contains("mp3", ignoreCase = true) -> "MP3 (MPEG Audio Layer III)"
        mime.contains("ac3", ignoreCase = true) -> "Dolby Digital (AC-3)"
        mime.contains("eac3", ignoreCase = true) -> "Dolby Digital Plus (E-AC-3)"
        else -> mime.removePrefix("audio/").uppercase()
    }
}

private fun getContainerFromExtension(fileName: String): String {
    val ext = fileName.substringAfterLast(".", "").lowercase()
    return when (ext) {
        "mp4", "m4v" -> "MPEG-4 Part 14 (.mp4)"
        "mkv" -> "Matroska (.mkv)"
        "webm" -> "WebM (.webm)"
        "ts" -> "MPEG Transport Stream (.ts)"
        "3gp" -> "3GPP (.3gp)"
        "avi" -> "Audio Video Interleave (.avi)"
        "mov" -> "QuickTime (.mov)"
        "flv" -> "Flash Video (.flv)"
        else -> if (ext.isNotBlank()) ".$ext container" else "Video Container"
    }
}
