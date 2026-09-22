package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.example.data.model.VideoItem
import com.example.data.thumbnail.ThumbnailHelper
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.ContinueWatchingHeroCard
import com.example.ui.components.HoneyLogo
import com.example.ui.components.SortBottomSheet
import com.example.ui.components.VideoCard
import com.example.ui.theme.HoneyGold
import com.example.ui.theme.ThemeMode
import com.example.ui.viewmodel.VideoUiState
import com.example.ui.viewmodel.VideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: VideoUiState,
    viewModel: VideoViewModel,
    onNavigateToVideos: () -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToPlaylists: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestPermission: () -> Unit = {},
    onOpenSystemSettings: () -> Unit = {},
    onPickVideo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    val recentlyAdded = remember(uiState.allVideos) {
        uiState.allVideos.sortedByDescending { it.dateAdded }.take(8)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar Header with HONEY Player logo, Search, Settings, Theme toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HoneyLogo(
                    size = 38.dp,
                    showGlow = true
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "HONEY",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "VIDEO PLAYER",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = HoneyGold,
                        letterSpacing = 2.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pick Video Test Button (ACTION_OPEN_DOCUMENT)
                IconButton(
                    onClick = onPickVideo,
                    modifier = Modifier.testTag("pick_video_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = "Pick Video (File Picker)",
                        tint = HoneyGold
                    )
                }

                // Search Toggle
                IconButton(
                    onClick = {
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded) viewModel.setSearchQuery("")
                    },
                    modifier = Modifier.testTag("search_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Theme Toggle
                IconButton(
                    onClick = { viewModel.toggleThemeMode() },
                    modifier = Modifier.testTag("theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (uiState.themeMode == ThemeMode.AMOLED) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle AMOLED/Dark Theme",
                        tint = HoneyGold
                    )
                }

                // Settings Button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Expandable Search Bar
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search videos or folders...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HoneyGold,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = HoneyGold)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_input_field")
                )
            }
        }

        if (!uiState.permissionGranted) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = HoneyGold.copy(alpha = 0.12f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("storage_permission_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = HoneyGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Storage Access Required",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.permissionDenied) "Permission denied. Open Settings to grant access." else "Allow storage access to scan videos.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Button(
                        onClick = {
                            if (uiState.permissionDenied) onOpenSystemSettings() else onRequestPermission()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = HoneyGold,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("banner_permission_action_button")
                    ) {
                        Text(
                            text = if (uiState.permissionDenied) "Settings" else "Allow",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = HoneyGold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scanning device videos...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else if (uiState.filteredVideos.isEmpty() && uiState.searchQuery.isNotEmpty()) {
            // Search Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No videos matching \"${uiState.searchQuery}\"",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else if (uiState.allVideos.isEmpty()) {
            // No videos on device or permission not granted
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = HoneyGold,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (!uiState.permissionGranted) "Storage Access Needed" else "No Videos Found",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (!uiState.permissionGranted) {
                            "Storage permission allows HONEY Player to automatically scan and list all your local video files. Please grant permission or pick a video directly."
                        } else {
                            "No local video files were detected in your media library. Pick a video manually to play or tap Refresh after adding videos."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!uiState.permissionGranted) {
                            androidx.compose.material3.Button(
                                onClick = {
                                    if (uiState.permissionDenied) onOpenSystemSettings() else onRequestPermission()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = HoneyGold,
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier.testTag("empty_state_grant_permission_button")
                            ) {
                                Text(
                                    text = if (uiState.permissionDenied) "Settings" else "Grant Access",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        androidx.compose.material3.Button(
                            onClick = onPickVideo,
                            shape = RoundedCornerShape(12.dp),
                            colors = if (!uiState.permissionGranted) {
                                androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = HoneyGold,
                                    contentColor = Color.Black
                                )
                            },
                            modifier = Modifier.testTag("pick_video_empty_state_button")
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Video", fontWeight = FontWeight.Bold)
                        }

                        if (uiState.permissionGranted) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = { viewModel.refreshVideos() },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("refresh_library_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Refresh")
                            }
                        }
                    }
                }
            }
        } else {
            val heroItem = uiState.continueWatching.firstOrNull()
            val remainingItems = remember(uiState.continueWatching) {
                if (uiState.continueWatching.size > 1) uiState.continueWatching.drop(1) else emptyList()
            }

            // Dashboard Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // Navigation Quick Stats Hub (All Videos, Folders, Playlists, Favorites)
                if (uiState.searchQuery.isEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DashboardPill(
                                title = "Videos",
                                count = "${uiState.allVideos.size}",
                                icon = Icons.Default.VideoLibrary,
                                onClick = onNavigateToVideos,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardPill(
                                title = "Folders",
                                count = "${uiState.folders.size}",
                                icon = Icons.Default.Folder,
                                onClick = onNavigateToFolders,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardPill(
                                title = "Playlists",
                                count = "${uiState.playlists.size}",
                                icon = Icons.Default.PlaylistPlay,
                                onClick = onNavigateToPlaylists,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardPill(
                                title = "Favorites",
                                count = "${uiState.favoriteVideos.size}",
                                icon = Icons.Default.Favorite,
                                onClick = onNavigateToFavorites,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Continue Watching Section (Hero card for most recent, row for others)
                if (heroItem != null && uiState.searchQuery.isEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)) {
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                ContinueWatchingHeroCard(
                                    video = heroItem.first,
                                    record = heroItem.second,
                                    onResumeClick = {
                                        viewModel.playVideo(heroItem.first, uiState.filteredVideos)
                                    }
                                )
                            }

                            if (remainingItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "More to Resume",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    items(remainingItems, key = { it.first.contentUri }) { (video, record) ->
                                        ContinueWatchingCard(
                                            video = video,
                                            record = record,
                                            onClick = {
                                                viewModel.playVideo(video, uiState.filteredVideos)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Recently Added Section
                if (uiState.allVideos.isNotEmpty() && uiState.searchQuery.isEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recently Added",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                TextButton(onClick = onNavigateToVideos) {
                                    Text("See All", color = HoneyGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(recentlyAdded, key = { it.contentUri }) { video ->
                                    RecentVideoMiniCard(
                                        video = video,
                                        onClick = { viewModel.playVideo(video, uiState.allVideos) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Favorites Section
                if (uiState.favoriteVideos.isNotEmpty() && uiState.searchQuery.isEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Favorites",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                TextButton(onClick = onNavigateToFavorites) {
                                    Text("See All", color = HoneyGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.favoriteVideos.take(8), key = { it.contentUri }) { video ->
                                    RecentVideoMiniCard(
                                        video = video,
                                        onClick = { viewModel.playVideo(video, uiState.favoriteVideos) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Playlists Section
                if (uiState.playlists.isNotEmpty() && uiState.searchQuery.isEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Playlists",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                TextButton(onClick = onNavigateToPlaylists) {
                                    Text("See All", color = HoneyGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(uiState.playlists.take(6), key = { it.id }) { playlist ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .clickable {
                                                onNavigateToPlaylists()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlaylistPlay,
                                                contentDescription = null,
                                                tint = HoneyGold,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = playlist.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Folders Row
                if (uiState.folders.isNotEmpty() && uiState.searchQuery.isEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Folders",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                TextButton(onClick = onNavigateToFolders) {
                                    Text("See All", color = HoneyGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(uiState.folders.take(6), key = { it.name }) { folder ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .clickable {
                                                viewModel.selectFolder(folder.name)
                                                onNavigateToFolders()
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = HoneyGold,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = folder.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(${folder.videoCount})",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // All Videos Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.searchQuery.isNotEmpty()) "Search Results (${uiState.filteredVideos.size})" else "All Videos (${uiState.filteredVideos.size})",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        IconButton(onClick = { showSortSheet = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort", tint = HoneyGold, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Video Cards List
                items(uiState.filteredVideos, key = { it.contentUri }) { video ->
                    val isFav = uiState.favoriteUris.contains(video.contentUri)
                    val record = uiState.playbackRecordsMap[video.contentUri]
                    VideoCard(
                        video = video,
                        isFavorite = isFav,
                        playbackRecord = record,
                        onVideoClick = {
                            viewModel.playVideo(video, uiState.filteredVideos)
                        },
                        onFavoriteClick = {
                            viewModel.toggleFavorite(video)
                        },
                        onAddToPlaylist = { viewModel.showAddToPlaylistDialog(video) },
                        onShowInfo = { viewModel.showVideoInfo(video) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            currentSort = uiState.sortOption,
            onSortSelected = { viewModel.setSortOption(it) },
            onDismiss = { showSortSheet = false }
        )
    }
}

@Composable
private fun DashboardPill(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.clickable(onClick = onClick),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = HoneyGold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RecentVideoMiniCard(
    video: VideoItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = ThumbnailHelper.getImageLoader(context)
    val thumbnailRequest = remember(video.contentUri) {
        ThumbnailHelper.buildThumbnailRequest(context, video.uri, widthPx = 260, heightPx = 160)
    }

    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = thumbnailRequest,
                imageLoader = imageLoader,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color(0xDD000000), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = video.formattedDuration,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = video.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
