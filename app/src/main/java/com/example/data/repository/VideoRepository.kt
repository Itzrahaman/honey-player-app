package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.PlaybackDao
import com.example.data.local.PlaybackRecord
import com.example.data.local.PlaylistDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistItemEntity
import com.example.data.local.SettingsManager
import com.example.data.mediastore.VideoScanner
import com.example.data.model.PlaylistWithVideos
import com.example.data.model.SortOption
import com.example.data.model.VideoFolder
import com.example.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class VideoRepository(
    private val context: Context,
    private val playbackDao: PlaybackDao = AppDatabase.getDatabase(context).playbackDao(),
    private val playlistDao: PlaylistDao = AppDatabase.getDatabase(context).playlistDao(),
    val settingsManager: SettingsManager = SettingsManager(context),
    private val scanner: VideoScanner = VideoScanner(context)
) {
    private val _scannedVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    val scannedVideos: StateFlow<List<VideoItem>> = _scannedVideos.asStateFlow()

    val continueWatchingRecords: Flow<List<PlaybackRecord>> = playbackDao.getContinueWatchingRecords()
    val allPlaybackRecords: Flow<List<PlaybackRecord>> = playbackDao.getAllRecords()
    val favoriteUris: Flow<List<String>> = playbackDao.getFavoriteUris()
    val playlists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun scanLocalVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val list = scanner.scanVideos()
        _scannedVideos.value = list
        list
    }

    suspend fun resolveVideoFromUri(uri: android.net.Uri): VideoItem? {
        return scanner.resolveVideoFromUri(uri)
    }

    fun getFolders(videos: List<VideoItem>): List<VideoFolder> {
        return scanner.groupIntoFolders(videos)
    }

    fun sortVideos(videos: List<VideoItem>, option: SortOption): List<VideoItem> {
        return when (option) {
            SortOption.DATE_DESC -> videos.sortedByDescending { maxOf(it.dateAdded, it.dateModified) }
            SortOption.DATE_ASC -> videos.sortedBy { if (it.dateAdded > 0) it.dateAdded else it.dateModified }
            SortOption.NAME_ASC -> videos.sortedBy { it.title.lowercase() }
            SortOption.NAME_DESC -> videos.sortedByDescending { it.title.lowercase() }
            SortOption.DURATION_DESC -> videos.sortedByDescending { it.durationMs }
            SortOption.SIZE_DESC -> videos.sortedByDescending { it.sizeBytes }
        }
    }

    fun filterVideos(videos: List<VideoItem>, query: String, folderName: String?): List<VideoItem> {
        return videos.filter { video ->
            val matchesFolder = folderName == null || video.folderName.equals(folderName, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    video.title.contains(query, ignoreCase = true) ||
                    video.displayName.contains(query, ignoreCase = true) ||
                    video.folderName.contains(query, ignoreCase = true)
            matchesFolder && matchesQuery
        }
    }

    fun getRecord(videoUri: String): Flow<PlaybackRecord?> {
        return playbackDao.getRecord(videoUri)
    }

    suspend fun getRecordSync(videoUri: String): PlaybackRecord? = withContext(Dispatchers.IO) {
        playbackDao.getRecordSync(videoUri)
    }

    suspend fun savePlaybackPosition(
        videoUri: String,
        positionMs: Long,
        durationMs: Long,
        playbackSpeed: Float = 1.0f
    ) = withContext(Dispatchers.IO) {
        val existing = playbackDao.getRecordSync(videoUri)
        val isFav = existing?.isFavorite ?: false
        val record = PlaybackRecord(
            videoUri = videoUri,
            positionMs = positionMs,
            durationMs = durationMs,
            lastPlayedTimestamp = System.currentTimeMillis(),
            isFavorite = isFav,
            playbackSpeed = playbackSpeed
        )
        playbackDao.upsertRecord(record)
    }

    suspend fun toggleFavorite(videoUri: String) = withContext(Dispatchers.IO) {
        val existing = playbackDao.getRecordSync(videoUri)
        if (existing != null) {
            playbackDao.updateFavorite(videoUri, !existing.isFavorite)
        } else {
            playbackDao.upsertRecord(
                PlaybackRecord(
                    videoUri = videoUri,
                    isFavorite = true
                )
            )
        }
    }

    suspend fun isFavorite(videoUri: String): Boolean = withContext(Dispatchers.IO) {
        playbackDao.getRecordSync(videoUri)?.isFavorite == true
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        playbackDao.clearAll()
    }

    suspend fun createPlaylist(name: String): Long = withContext(Dispatchers.IO) {
        playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun renamePlaylist(id: Long, newName: String) = withContext(Dispatchers.IO) {
        playlistDao.renamePlaylist(id, newName)
    }

    suspend fun deletePlaylist(id: Long) = withContext(Dispatchers.IO) {
        playlistDao.clearPlaylistItems(id)
        playlistDao.deletePlaylist(id)
    }

    suspend fun addVideoToPlaylist(playlistId: Long, videoUri: String) = withContext(Dispatchers.IO) {
        val existingItems = playlistDao.getItemsForPlaylistSync(playlistId)
        val order = existingItems.size
        playlistDao.insertPlaylistItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                videoUri = videoUri,
                orderIndex = order
            )
        )
    }

    suspend fun removeVideoFromPlaylist(playlistId: Long, videoUri: String) = withContext(Dispatchers.IO) {
        playlistDao.removePlaylistItem(playlistId, videoUri)
    }

    fun getPlaylistVideos(playlistId: Long, allVideos: List<VideoItem>): Flow<List<VideoItem>> {
        return playlistDao.getItemsForPlaylist(playlistId).map { items ->
            items.mapNotNull { item ->
                allVideos.find { it.contentUri == item.videoUri }
            }
        }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        try {
            context.cacheDir.deleteRecursively()
        } catch (_: Exception) {}
    }
}
