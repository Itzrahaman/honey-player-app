package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PlaybackRecord
import com.example.data.model.SortOption
import com.example.data.model.VideoFolder
import com.example.data.model.VideoItem
import com.example.data.repository.VideoRepository
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VideoUiState(
    val isLoading: Boolean = true,
    val permissionGranted: Boolean = false,
    val permissionDenied: Boolean = false,
    val allVideos: List<VideoItem> = emptyList(),
    val filteredVideos: List<VideoItem> = emptyList(),
    val folders: List<VideoFolder> = emptyList(),
    val continueWatching: List<Pair<VideoItem, PlaybackRecord>> = emptyList(),
    val favoriteVideos: List<VideoItem> = emptyList(),
    val searchQuery: String = "",
    val selectedFolder: String? = null,
    val sortOption: SortOption = SortOption.DATE_DESC,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val activeVideo: VideoItem? = null,
    val currentPlaylist: List<VideoItem> = emptyList()
)

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository(application.applicationContext)

    private val _isLoading = MutableStateFlow(true)
    private val _permissionGranted = MutableStateFlow(false)
    private val _permissionDenied = MutableStateFlow(false)
    private val _scannedVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFolder = MutableStateFlow<String?>(null)
    private val _sortOption = MutableStateFlow(SortOption.DATE_DESC)
    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    private val _activeVideo = MutableStateFlow<VideoItem?>(null)
    private val _currentPlaylist = MutableStateFlow<List<VideoItem>>(emptyList())

    val uiState: StateFlow<VideoUiState> = combine(
        combine(
            _isLoading,
            _permissionGranted,
            _permissionDenied,
            _scannedVideos,
            _searchQuery
        ) { loading, permission, denied, videos, query ->
            FilterTuple(loading, permission, denied, videos, query)
        },
        combine(
            _selectedFolder,
            _sortOption,
            _themeMode,
            _activeVideo,
            _currentPlaylist
        ) { folder, sort, theme, active, playlist ->
            MetaTuple(folder, sort, theme, active, playlist)
        },
        repository.continueWatchingRecords,
        repository.favoriteUris
    ) { filter, meta, cwRecords, favUris ->
        val filtered = repository.filterVideos(
            videos = filter.videos,
            query = filter.query,
            folderName = meta.folder
        )
        val sorted = repository.sortVideos(filtered, meta.sort)
        val folders = repository.getFolders(filter.videos)

        // Continue watching pairing
        val cwPairs = cwRecords.mapNotNull { record ->
            val video = filter.videos.find { it.contentUri == record.videoUri }
            if (video != null) video to record else null
        }

        // Favorite items
        val favorites = filter.videos.filter { favUris.contains(it.contentUri) }

        VideoUiState(
            isLoading = filter.loading,
            permissionGranted = filter.permission,
            permissionDenied = filter.denied,
            allVideos = filter.videos,
            filteredVideos = sorted,
            folders = folders,
            continueWatching = cwPairs,
            favoriteVideos = favorites,
            searchQuery = filter.query,
            selectedFolder = meta.folder,
            sortOption = meta.sort,
            themeMode = meta.theme,
            activeVideo = meta.active,
            currentPlaylist = meta.playlist
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VideoUiState()
    )

    fun onPermissionResult(isGranted: Boolean) {
        _permissionGranted.value = isGranted
        _permissionDenied.value = !isGranted
        if (isGranted) {
            refreshVideos()
        } else {
            _isLoading.value = false
        }
    }

    fun refreshVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            val list = repository.scanLocalVideos()
            // Preserve any manually picked videos
            val currentList = _scannedVideos.value
            val manualVideos = currentList.filter { current ->
                list.none { it.contentUri == current.contentUri } && current.folderName == "Selected Videos"
            }
            _scannedVideos.value = manualVideos + list
            _isLoading.value = false
        }
    }

    fun pickAndPlayVideo(uri: android.net.Uri) {
        viewModelScope.launch {
            val videoItem = repository.resolveVideoFromUri(uri)
            if (videoItem != null) {
                val current = _scannedVideos.value.toMutableList()
                if (current.none { it.contentUri == videoItem.contentUri }) {
                    current.add(0, videoItem)
                    _scannedVideos.value = current
                }
                playVideo(videoItem, _scannedVideos.value)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun selectFolder(folderName: String?) {
        _selectedFolder.value = folderName
    }

    fun toggleThemeMode() {
        _themeMode.value = when (_themeMode.value) {
            ThemeMode.DARK -> ThemeMode.AMOLED
            ThemeMode.AMOLED -> ThemeMode.DARK
            ThemeMode.SYSTEM -> ThemeMode.DARK
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    fun toggleFavorite(video: VideoItem) {
        viewModelScope.launch {
            repository.toggleFavorite(video.contentUri)
        }
    }

    fun playVideo(video: VideoItem, playlist: List<VideoItem> = emptyList()) {
        val finalPlaylist = if (playlist.isNotEmpty()) playlist else listOf(video)
        _currentPlaylist.value = finalPlaylist
        _activeVideo.value = video
    }

    fun playNextVideo() {
        val current = _activeVideo.value ?: return
        val playlist = _currentPlaylist.value
        val currentIndex = playlist.indexOfFirst { it.contentUri == current.contentUri }
        if (currentIndex != -1 && currentIndex + 1 < playlist.size) {
            _activeVideo.value = playlist[currentIndex + 1]
        }
    }

    fun playPreviousVideo() {
        val current = _activeVideo.value ?: return
        val playlist = _currentPlaylist.value
        val currentIndex = playlist.indexOfFirst { it.contentUri == current.contentUri }
        if (currentIndex > 0) {
            _activeVideo.value = playlist[currentIndex - 1]
        }
    }

    fun closePlayer() {
        _activeVideo.value = null
    }

    suspend fun getSavedRecord(videoUri: String): PlaybackRecord? {
        return repository.getRecordSync(videoUri)
    }

    fun savePlaybackPosition(videoUri: String, positionMs: Long, durationMs: Long, speed: Float) {
        viewModelScope.launch {
            repository.savePlaybackPosition(videoUri, positionMs, durationMs, speed)
        }
    }
}

private data class FilterTuple(
    val loading: Boolean,
    val permission: Boolean,
    val denied: Boolean,
    val videos: List<VideoItem>,
    val query: String
)

private data class MetaTuple(
    val folder: String?,
    val sort: SortOption,
    val theme: ThemeMode,
    val active: VideoItem?,
    val playlist: List<VideoItem>
)
