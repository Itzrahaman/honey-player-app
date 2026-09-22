package com.example.data.model

import com.example.data.local.PlaylistEntity

data class PlaylistWithVideos(
    val playlist: PlaylistEntity,
    val videos: List<VideoItem>
)
