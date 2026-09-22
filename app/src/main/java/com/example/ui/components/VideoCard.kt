package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.data.local.PlaybackRecord
import com.example.data.model.VideoItem
import com.example.data.thumbnail.ThumbnailHelper
import com.example.ui.theme.HoneyGold

@Composable
fun VideoCard(
    video: VideoItem,
    isFavorite: Boolean,
    playbackRecord: PlaybackRecord? = null,
    onVideoClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    onShowInfo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = ThumbnailHelper.getImageLoader(context)
    var showMenu by remember { mutableStateOf(false) }
    val thumbnailRequest = remember(video.contentUri) {
        ThumbnailHelper.buildThumbnailRequest(context, video.uri, widthPx = 480, heightPx = 270)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onVideoClick)
            .testTag("video_card_${video.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Thumbnail Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = thumbnailRequest,
                    imageLoader = imageLoader,
                    contentDescription = "Thumbnail for ${video.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x44000000),
                                    Color.Transparent,
                                    Color(0x99000000)
                                )
                            )
                        )
                )

                // Resolution Badge
                if (video.resolutionLabel.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .background(Color(0xCC000000), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = video.resolutionLabel,
                            color = HoneyGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Play Button Center
                Surface(
                    shape = CircleShape,
                    color = Color(0x99000000),
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.Center)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Duration Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .background(Color(0xDD000000), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = video.formattedDuration,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Progress Bar for partially watched videos
                if (playbackRecord != null && playbackRecord.isPartiallyWatched) {
                    LinearProgressIndicator(
                        progress = { playbackRecord.progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                        color = HoneyGold,
                        trackColor = Color(0x66000000)
                    )
                }
            }

            // Info details & More menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = video.folderName,
                            color = HoneyGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(text = "•", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        Text(
                            text = video.formattedSize,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) HoneyGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        VideoActionDropdownMenu(
                            expanded = showMenu,
                            onDismiss = { showMenu = false },
                            isFavorite = isFavorite,
                            onPlay = { showMenu = false; onVideoClick() },
                            onToggleFavorite = { showMenu = false; onFavoriteClick() },
                            onAddToPlaylist = { showMenu = false; onAddToPlaylist() },
                            onShowInfo = { showMenu = false; onShowInfo() },
                            onShare = {
                                showMenu = false
                                shareVideo(context, video)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VideoGridCard(
    video: VideoItem,
    isFavorite: Boolean,
    playbackRecord: PlaybackRecord? = null,
    onVideoClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    onShowInfo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = ThumbnailHelper.getImageLoader(context)
    var showMenu by remember { mutableStateOf(false) }
    val thumbnailRequest = remember(video.contentUri) {
        ThumbnailHelper.buildThumbnailRequest(context, video.uri, widthPx = 360, heightPx = 225)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onVideoClick)
            .testTag("video_grid_${video.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = thumbnailRequest,
                    imageLoader = imageLoader,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Resolution
                if (video.resolutionLabel.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(text = video.resolutionLabel, color = HoneyGold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Duration
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color(0xDD000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = video.formattedDuration, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                if (playbackRecord != null && playbackRecord.isPartiallyWatched) {
                    LinearProgressIndicator(
                        progress = { playbackRecord.progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = HoneyGold,
                        trackColor = Color(0x66000000)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = video.formattedSize,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    VideoActionDropdownMenu(
                        expanded = showMenu,
                        onDismiss = { showMenu = false },
                        isFavorite = isFavorite,
                        onPlay = { showMenu = false; onVideoClick() },
                        onToggleFavorite = { showMenu = false; onFavoriteClick() },
                        onAddToPlaylist = { showMenu = false; onAddToPlaylist() },
                        onShowInfo = { showMenu = false; onShowInfo() },
                        onShare = {
                            showMenu = false
                            shareVideo(context, video)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun VideoListRow(
    video: VideoItem,
    isFavorite: Boolean,
    playbackRecord: PlaybackRecord? = null,
    onVideoClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    onShowInfo: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = ThumbnailHelper.getImageLoader(context)
    var showMenu by remember { mutableStateOf(false) }
    val thumbnailRequest = remember(video.contentUri) {
        ThumbnailHelper.buildThumbnailRequest(context, video.uri, widthPx = 240, heightPx = 135)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onVideoClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .width(110.dp)
                .aspectRatio(16f / 9f)
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

            // Duration badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color(0xDD000000), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(text = video.formattedDuration, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            if (playbackRecord != null && playbackRecord.isPartiallyWatched) {
                LinearProgressIndicator(
                    progress = { playbackRecord.progressPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = HoneyGold,
                    trackColor = Color(0x66000000)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = video.folderName,
                    fontSize = 12.sp,
                    color = HoneyGold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = " • ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Text(
                    text = video.formattedSize,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (video.resolutionLabel.isNotBlank()) {
                    Text(text = " • ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    Text(
                        text = video.resolutionLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HoneyGold
                    )
                }
            }
        }

        IconButton(onClick = onFavoriteClick, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) HoneyGold else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            VideoActionDropdownMenu(
                expanded = showMenu,
                onDismiss = { showMenu = false },
                isFavorite = isFavorite,
                onPlay = { showMenu = false; onVideoClick() },
                onToggleFavorite = { showMenu = false; onFavoriteClick() },
                onAddToPlaylist = { showMenu = false; onAddToPlaylist() },
                onShowInfo = { showMenu = false; onShowInfo() },
                onShare = {
                    showMenu = false
                    shareVideo(context, video)
                }
            )
        }
    }
}

@Composable
private fun VideoActionDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShowInfo: () -> Unit,
    onShare: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        DropdownMenuItem(
            text = { Text("Play", color = MaterialTheme.colorScheme.onSurface) },
            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = HoneyGold) },
            onClick = onPlay
        )
        DropdownMenuItem(
            text = { Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites", color = MaterialTheme.colorScheme.onSurface) },
            leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = HoneyGold) },
            onClick = onToggleFavorite
        )
        DropdownMenuItem(
            text = { Text("Add to Playlist", color = MaterialTheme.colorScheme.onSurface) },
            leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = HoneyGold) },
            onClick = onAddToPlaylist
        )
        DropdownMenuItem(
            text = { Text("Video Details", color = MaterialTheme.colorScheme.onSurface) },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = HoneyGold) },
            onClick = onShowInfo
        )
        DropdownMenuItem(
            text = { Text("Share", color = MaterialTheme.colorScheme.onSurface) },
            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = HoneyGold) },
            onClick = onShare
        )
    }
}

private fun shareVideo(context: Context, video: VideoItem) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = video.mimeType
            putExtra(Intent.EXTRA_STREAM, video.uri)
            putExtra(Intent.EXTRA_SUBJECT, video.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share video via"))
    } catch (_: Exception) {}
}
