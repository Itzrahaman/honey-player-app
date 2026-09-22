package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PlaybackRecord
import com.example.data.local.PlayerSettings
import com.example.data.local.PlaylistEntity
import com.example.data.model.SortOption
import com.example.data.model.VideoFolder
import com.example.data.model.VideoItem
import com.example.data.repository.VideoRepository
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    val favoriteUris: Set<String> = emptySet(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val playbackRecordsMap: Map<String, PlaybackRecord> = emptyMap(),
    val searchQuery: String = "",
    val selectedFolder: String? = null,
    val sortOption: SortOption = SortOption.DATE_DESC,
    val themeMode: ThemeMode = ThemeMode.AMOLED,
    val viewMode: String = "list", // "list" or "grid"
    val activeVideo: VideoItem? = null,
    val currentPlaylist: List<VideoItem> = emptyList(),
    val miniPlayerVideo: VideoItem? = null,
    val isMiniPlayerPlaying: Boolean = false,
    val settings: PlayerSettings = PlayerSettings(),
    val infoDialogVideo: VideoItem? = null,
    val addToPlaylistVideo: VideoItem? = null
)

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VideoRepository(application.applicationContext)
    private val settingsManager = repository.settingsManager

    private val _isLoading = MutableStateFlow(true)
    private val _permissionGranted = MutableStateFlow(false)
    private val _permissionDenied = MutableStateFlow(false)
    private val _scannedVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedFolder = MutableStateFlow<String?>(null)
    private val _sortOption = MutableStateFlow(settingsManager.settings.value.defaultSortOption)
    private val _themeMode = MutableStateFlow(settingsManager.settings.value.themeMode)
    private val _viewMode = MutableStateFlow(settingsManager.settings.value.defaultViewMode)
    private val _activeVideo = MutableStateFlow<VideoItem?>(null)
    private val _currentPlaylist = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _miniPlayerVideo = MutableStateFlow<VideoItem?>(null)
    private val _isMiniPlayerPlaying = MutableStateFlow(false)
    private val _infoDialogVideo = MutableStateFlow<VideoItem?>(null)
    private val _addToPlaylistVideo = MutableStateFlow<VideoItem?>(null)

    // In-memory computation caches to prevent redundant quadratic work on DB changes
    private var lastVideosForFolders: List<VideoItem>? = null
    private var cachedFolders: List<VideoFolder> = emptyList()
    private var lastFilterVideos: List<VideoItem>? = null
    private var lastQuery: String? = null
    private var lastFolder: String? = null
    private var lastSort: SortOption? = null
    private var cachedSortedVideos: List<VideoItem> = emptyList()

    private var lastScanTimestamp: Long = 0L
    private var lastSavedUri: String? = null
    private var lastSavedPos: Long = -1L
    private var lastSavedTimestamp: Long = 0L

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
            _viewMode,
            _activeVideo
        ) { folder, sort, theme, viewMode, active ->
            MetaTuple(folder, sort, theme, viewMode, active)
        },
        combine(
            _currentPlaylist,
            _miniPlayerVideo,
            _isMiniPlayerPlaying,
            _infoDialogVideo,
            _addToPlaylistVideo
        ) { playlist, miniVideo, miniPlaying, infoVid, addPlayVid ->
            PlaybackTuple(playlist, miniVideo, miniPlaying, infoVid, addPlayVid)
        },
        combine(
            repository.continueWatchingRecords,
            repository.favoriteUris,
            repository.playlists,
            repository.allPlaybackRecords,
            settingsManager.settings
        ) { cwRecords, favUris, playlists, allRecords, settings ->
            DataTuple(cwRecords, favUris, playlists, allRecords, settings)
        }
    ) { filter, meta, playback, data ->
        // Only re-filter & re-sort when video list, search query, folder filter, or sort option changes
        val sorted = if (filter.videos === lastFilterVideos &&
            filter.query == lastQuery &&
            meta.folder == lastFolder &&
            meta.sort == lastSort
        ) {
            cachedSortedVideos
        } else {
            val filtered = repository.filterVideos(
                videos = filter.videos,
                query = filter.query,
                folderName = meta.folder
            )
            val res = repository.sortVideos(filtered, meta.sort)
            lastFilterVideos = filter.videos
            lastQuery = filter.query
            lastFolder = meta.folder
            lastSort = meta.sort
            cachedSortedVideos = res
            res
        }

        // Only regroup folders when the video list reference changes
        val folders = if (filter.videos === lastVideosForFolders) {
            cachedFolders
        } else {
            val f = repository.getFolders(filter.videos)
            lastVideosForFolders = filter.videos
            cachedFolders = f
            f
        }

        // Continue watching pairing
        val cwPairs = data.cwRecords.mapNotNull { record ->
            val video = filter.videos.find { it.contentUri == record.videoUri }
            if (video != null) video to record else null
        }

        // Favorite items
        val favSet = data.favUris.toSet()
        val favorites = filter.videos.filter { favSet.contains(it.contentUri) }

        // Records map
        val recordsMap = data.allRecords.associateBy { it.videoUri }

        VideoUiState(
            isLoading = filter.loading,
            permissionGranted = filter.permission,
            permissionDenied = filter.denied,
            allVideos = filter.videos,
            filteredVideos = sorted,
            folders = folders,
            continueWatching = cwPairs,
            favoriteVideos = favorites,
            favoriteUris = favSet,
            playlists = data.playlists,
            playbackRecordsMap = recordsMap,
            searchQuery = filter.query,
            selectedFolder = meta.folder,
            sortOption = meta.sort,
            themeMode = meta.theme,
            viewMode = meta.viewMode,
            activeVideo = meta.active,
            currentPlaylist = playback.playlist,
            miniPlayerVideo = playback.miniVideo,
            isMiniPlayerPlaying = playback.miniPlaying,
            settings = data.settings,
            infoDialogVideo = playback.infoVid,
            addToPlaylistVideo = playback.addPlayVid
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

    fun refreshVideos(force: Boolean = false) {
        if (!force && _scannedVideos.value.isNotEmpty() && (System.currentTimeMillis() - lastScanTimestamp < 45_000L)) {
            // Already scanned within 45s, skip redundant storage re-scan
            _isLoading.value = false
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val list = repository.scanLocalVideos()
            lastScanTimestamp = System.currentTimeMillis()
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
        settingsManager.setSortOption(option)
    }

    fun selectFolder(folderName: String?) {
        _selectedFolder.value = folderName
    }

    fun toggleViewMode() {
        val next = if (_viewMode.value == "grid") "list" else "grid"
        _viewMode.value = next
        settingsManager.setViewMode(next)
    }

    fun setViewMode(mode: String) {
        _viewMode.value = mode
        settingsManager.setViewMode(mode)
    }

    fun toggleThemeMode() {
        val next = when (_themeMode.value) {
            ThemeMode.AMOLED -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
            ThemeMode.SYSTEM -> ThemeMode.AMOLED
        }
        setThemeMode(next)
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        settingsManager.setThemeMode(mode)
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
        // dismiss mini player
        _miniPlayerVideo.value = null
        _isMiniPlayerPlaying.value = false
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

    fun closePlayer(wasPlaying: Boolean = false) {
        val current = _activeVideo.value
        if (wasPlaying && current != null) {
            _miniPlayerVideo.value = current
            _isMiniPlayerPlaying.value = true
        } else {
            _miniPlayerVideo.value = null
            _isMiniPlayerPlaying.value = false
        }
        _activeVideo.value = null
    }

    fun dismissMiniPlayer() {
        _miniPlayerVideo.value = null
        _isMiniPlayerPlaying.value = false
    }

    fun resumeFromMiniPlayer() {
        val mini = _miniPlayerVideo.value ?: return
        _activeVideo.value = mini
        _miniPlayerVideo.value = null
        _isMiniPlayerPlaying.value = false
    }

    fun toggleMiniPlayerPlayback() {
        _isMiniPlayerPlaying.value = !_isMiniPlayerPlaying.value
    }

    suspend fun getSavedRecord(videoUri: String): PlaybackRecord? {
        return repository.getRecordSync(videoUri)
    }

    fun savePlaybackPosition(
        videoUri: String,
        positionMs: Long,
        durationMs: Long,
        speed: Float,
        force: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        if (!force && lastSavedUri == videoUri && (now - lastSavedTimestamp < 4000L) && kotlin.math.abs(positionMs - lastSavedPos) < 4000L) {
            return // debounce unnecessary rapid writes to Room
        }
        lastSavedUri = videoUri
        lastSavedPos = positionMs
        lastSavedTimestamp = now
        viewModelScope.launch(Dispatchers.IO) {
            repository.savePlaybackPosition(videoUri, positionMs, durationMs, speed)
        }
    }

    // Playlist operations
    fun createPlaylist(name: String, initialVideo: VideoItem? = null) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name)
            if (initialVideo != null) {
                repository.addVideoToPlaylist(id, initialVideo.contentUri)
            }
        }
    }

    fun renamePlaylist(id: Long, newName: String) {
        viewModelScope.launch {
            repository.renamePlaylist(id, newName)
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(id)
        }
    }

    fun addVideoToPlaylist(playlistId: Long, video: VideoItem) {
        viewModelScope.launch {
            repository.addVideoToPlaylist(playlistId, video.contentUri)
            _addToPlaylistVideo.value = null
        }
    }

    fun removeVideoFromPlaylist(playlistId: Long, videoUri: String) {
        viewModelScope.launch {
            repository.removeVideoFromPlaylist(playlistId, videoUri)
        }
    }

    fun playPlaylist(playlistId: Long, shuffle: Boolean = false) {
        viewModelScope.launch {
            val videos = repository.getPlaylistVideos(playlistId, _scannedVideos.value).first()
            if (videos.isNotEmpty()) {
                val list = if (shuffle) videos.shuffled() else videos
                playVideo(list.first(), list)
            }
        }
    }

    fun getPlaylistVideosFlow(playlistId: Long) =
        repository.getPlaylistVideos(playlistId, _scannedVideos.value)

    fun showVideoInfo(video: VideoItem?) {
        _infoDialogVideo.value = video
    }

    fun dismissVideoInfo() {
        _infoDialogVideo.value = null
    }

    fun showAddToPlaylistDialog(video: VideoItem?) {
        _addToPlaylistVideo.value = video
    }

    fun dismissAddToPlaylistDialog() {
        _addToPlaylistVideo.value = null
    }

    fun updateSettings(newSettings: PlayerSettings) {
        settingsManager.updateSettings(newSettings)
        _sortOption.value = newSettings.defaultSortOption
        _themeMode.value = newSettings.themeMode
        _viewMode.value = newSettings.defaultViewMode
    }

    fun clearPlaybackHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
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
    val viewMode: String,
    val active: VideoItem?
)

private data class PlaybackTuple(
    val playlist: List<VideoItem>,
    val miniVideo: VideoItem?,
    val miniPlaying: Boolean,
    val infoVid: VideoItem?,
    val addPlayVid: VideoItem?
)

private data class DataTuple(
    val cwRecords: List<PlaybackRecord>,
    val favUris: List<String>,
    val playlists: List<PlaylistEntity>,
    val allRecords: List<PlaybackRecord>,
    val settings: PlayerSettings
)
