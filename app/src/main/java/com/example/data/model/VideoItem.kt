package com.example.data.model

import android.net.Uri
import java.util.Locale
import java.util.concurrent.TimeUnit

data class VideoItem(
    val id: Long,
    val contentUri: String,
    val title: String,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int = 0,
    val height: Int = 0,
    val dateAdded: Long = 0L,
    val dateModified: Long = 0L,
    val folderName: String = "Internal Storage",
    val filePath: String = "",
    val mimeType: String = "video/*"
) {
    val uri: Uri
        get() = Uri.parse(contentUri)

    val formattedDuration: String
        get() {
            if (durationMs <= 0) return "00:00"
            val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
            return if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "0 B"
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
                else -> "$sizeBytes B"
            }
        }

    val resolutionLabel: String
        get() {
            return when {
                height >= 2160 || width >= 3840 -> "4K"
                height >= 1440 || width >= 2560 -> "2K"
                height >= 1080 || width >= 1920 -> "1080p"
                height >= 720 || width >= 1280 -> "720p"
                height >= 480 || width >= 854 -> "480p"
                height > 0 && width > 0 -> "${width}x${height}"
                else -> "HD"
            }
        }
}

data class VideoFolder(
    val name: String,
    val path: String,
    val videoCount: Int,
    val firstVideoUri: String
)

enum class SortOption(val displayName: String) {
    DATE_DESC("Date (Newest first)"),
    DATE_ASC("Date (Oldest first)"),
    NAME_ASC("Name (A to Z)"),
    NAME_DESC("Name (Z to A)"),
    DURATION_DESC("Duration (Longest first)"),
    SIZE_DESC("File Size (Largest first)")
}
