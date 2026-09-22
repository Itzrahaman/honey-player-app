package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.PermissionPrompt
import com.example.ui.components.VideoInfoDialog
import com.example.ui.player.PlayerScreen
import com.example.ui.theme.HoneyGold
import com.example.ui.viewmodel.VideoViewModel

enum class MainTab(val label: String) {
    HOME("Home"),
    VIDEOS("Videos"),
    FOLDERS("Folders"),
    PLAYLISTS("Playlists"),
    FAVORITES("Favorites")
}

@Composable
fun MainAppScreen(
    viewModel: VideoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var isSettingsOpen by remember { mutableStateOf(false) }

    // Use READ_MEDIA_VIDEO on Android 13+ (API 33+), READ_EXTERNAL_STORAGE on 12 and below
    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.onPermissionResult(isGranted)
    }

    // System file picker (ACTION_OPEN_DOCUMENT) to pick video manually
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            viewModel.pickAndPlayVideo(uri)
        }
    }

    val onPickVideo: () -> Unit = {
        videoPickerLauncher.launch(arrayOf("video/*"))
    }

    val onOpenAppSystemSettings: () -> Unit = {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    // Check permission on startup
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            requiredPermission
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionResult(granted)
    }

    // Re-check permission when app returns from background / Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    requiredPermission
                ) == PackageManager.PERMISSION_GRANTED
                if (granted != uiState.permissionGranted) {
                    viewModel.onPermissionResult(granted)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // If active video is selected, show Fullscreen Player!
    val activeVideo = uiState.activeVideo
    if (activeVideo != null) {
        PlayerScreen(
            video = activeVideo,
            playlist = uiState.currentPlaylist,
            viewModel = viewModel,
            onBack = { viewModel.closePlayer() }
        )
        return
    }

    // If Settings Screen is open
    if (isSettingsOpen) {
        BackHandler { isSettingsOpen = false }
        SettingsScreen(
            uiState = uiState,
            viewModel = viewModel,
            onBack = { isSettingsOpen = false }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (uiState.permissionGranted) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Sticky Mini Player
                    val miniVideo = uiState.miniPlayerVideo
                    if (miniVideo != null) {
                        MiniPlayerBar(
                            video = miniVideo,
                            isPlaying = uiState.isMiniPlayerPlaying,
                            onExpand = { viewModel.resumeFromMiniPlayer() },
                            onTogglePlayPause = { viewModel.toggleMiniPlayerPlayback() },
                            onClose = { viewModel.dismissMiniPlayer() }
                        )
                    }

                    // 5-Tab Navigation Bar
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        val tabs = MainTab.entries
                        tabs.forEachIndexed { index, tab ->
                            val isSelected = selectedTab == index
                            val (selectedIcon, unselectedIcon) = when (tab) {
                                MainTab.HOME -> Icons.Filled.Home to Icons.Outlined.Home
                                MainTab.VIDEOS -> Icons.Filled.VideoLibrary to Icons.Outlined.VideoLibrary
                                MainTab.FOLDERS -> Icons.Filled.Folder to Icons.Outlined.Folder
                                MainTab.PLAYLISTS -> Icons.Filled.PlaylistPlay to Icons.Outlined.PlaylistPlay
                                MainTab.FAVORITES -> Icons.Filled.Favorite to Icons.Outlined.FavoriteBorder
                            }

                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab = index },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) selectedIcon else unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 10.sp
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = HoneyGold,
                                    indicatorColor = HoneyGold,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!uiState.permissionGranted) {
                PermissionPrompt(
                    isDenied = uiState.permissionDenied,
                    onRequestPermission = {
                        permissionLauncher.launch(requiredPermission)
                    },
                    onOpenSettings = onOpenAppSystemSettings,
                    onPickVideo = onPickVideo
                )
            } else {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_switch"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> HomeScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onNavigateToVideos = { selectedTab = 1 },
                            onNavigateToFolders = { selectedTab = 2 },
                            onNavigateToPlaylists = { selectedTab = 3 },
                            onNavigateToFavorites = { selectedTab = 4 },
                            onOpenSettings = { isSettingsOpen = true },
                            onPickVideo = onPickVideo
                        )
                        1 -> VideosScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onPickVideo = onPickVideo
                        )
                        2 -> FoldersScreen(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                        3 -> PlaylistsScreen(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                        4 -> FavoritesScreen(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    // Video Info Modal Dialog
    val infoVideo = uiState.infoDialogVideo
    if (infoVideo != null) {
        VideoInfoDialog(
            video = infoVideo,
            onDismiss = { viewModel.dismissVideoInfo() }
        )
    }

    // Add to Playlist Modal Dialog
    val addToPlaylistVideo = uiState.addToPlaylistVideo
    if (addToPlaylistVideo != null) {
        AddToPlaylistDialog(
            video = addToPlaylistVideo,
            playlists = uiState.playlists,
            onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
            onAddToPlaylist = { playlistId -> viewModel.addVideoToPlaylist(playlistId, addToPlaylistVideo) },
            onDismiss = { viewModel.dismissAddToPlaylistDialog() }
        )
    }
}
