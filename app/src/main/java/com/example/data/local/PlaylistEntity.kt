package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_items",
    primaryKeys = ["playlistId", "videoUri"]
)
data class PlaylistItemEntity(
    val playlistId: Long,
    val videoUri: String,
    val orderIndex: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)
