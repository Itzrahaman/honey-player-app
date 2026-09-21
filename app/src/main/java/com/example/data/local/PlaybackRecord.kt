package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_records")
data class PlaybackRecord(
    @PrimaryKey
    val videoUri: String,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastPlayedTimestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val playbackSpeed: Float = 1.0f
) {
    val progressPercent: Float
        get() {
            if (durationMs <= 0L) return 0f
            return (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        }

    val isPartiallyWatched: Boolean
        get() = positionMs > 3000L && (durationMs <= 0L || positionMs < durationMs - 5000L)
}
