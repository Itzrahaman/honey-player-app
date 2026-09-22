package com.example.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.example.data.model.VideoFolder
import com.example.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VideoScanner(private val context: Context) {

    suspend fun scanVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val videoList = mutableListOf<VideoItem>()

        // Use standard external content URI compatible across all Android versions
        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projectionList = mutableListOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projectionList.add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
            projectionList.add(MediaStore.Video.Media.RELATIVE_PATH)
        } else {
            @Suppress("DEPRECATION")
            projectionList.add(MediaStore.Video.Media.DATA)
        }

        val projection = projectionList.toTypedArray()
        // Filter out zero-byte or corrupt files directly in MediaStore SQL query
        val selection = "${MediaStore.Video.Media.SIZE} > 0 AND ${MediaStore.Video.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val displayNameCol = c.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val titleCol = c.getColumnIndex(MediaStore.Video.Media.TITLE)
                val durationCol = c.getColumnIndex(MediaStore.Video.Media.DURATION)
                val sizeCol = c.getColumnIndex(MediaStore.Video.Media.SIZE)
                val dateAddedCol = c.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val dateModifiedCol = c.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED)
                val mimeCol = c.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val widthCol = c.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = c.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val bucketCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    c.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                } else -1
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    c.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else -1
                val dataCol = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    c.getColumnIndex(MediaStore.Video.Media.DATA)
                } else -1

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val duration = if (durationCol != -1) c.getLong(durationCol) else 0L
                    if (duration <= 0L) continue

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    val rawDisplayName = if (displayNameCol != -1) c.getString(displayNameCol) else null
                    val displayName = rawDisplayName?.takeIf { it.isNotBlank() } ?: "Video_$id"
                    val title = if (titleCol != -1) c.getString(titleCol)?.takeIf { it.isNotBlank() } ?: displayName else displayName

                    val size = if (sizeCol != -1) c.getLong(sizeCol) else 0L
                    val dateAdded = if (dateAddedCol != -1) c.getLong(dateAddedCol) else 0L
                    val dateModified = if (dateModifiedCol != -1) c.getLong(dateModifiedCol) else dateAdded
                    val mime = if (mimeCol != -1) c.getString(mimeCol) ?: "video/*" else "video/*"
                    val width = if (widthCol != -1) c.getInt(widthCol) else 0
                    val height = if (heightCol != -1) c.getInt(heightCol) else 0

                    val filePath = if (dataCol != -1) c.getString(dataCol) ?: "" else ""

                    // Determine folder name cleanly
                    val folderName = when {
                        bucketCol != -1 && !c.getString(bucketCol).isNullOrBlank() -> {
                            c.getString(bucketCol)
                        }
                        relPathCol != -1 && !c.getString(relPathCol).isNullOrBlank() -> {
                            c.getString(relPathCol).trim('/').split('/').lastOrNull()?.takeIf { it.isNotBlank() } ?: "Internal Storage"
                        }
                        filePath.isNotBlank() -> {
                            try {
                                File(filePath).parentFile?.name ?: "Internal Storage"
                            } catch (_: Exception) {
                                "Internal Storage"
                            }
                        }
                        else -> "Internal Storage"
                    }

                    videoList.add(
                        VideoItem(
                            id = id,
                            contentUri = contentUri,
                            title = title,
                            displayName = displayName,
                            durationMs = duration,
                            sizeBytes = size,
                            width = width,
                            height = height,
                            dateAdded = dateAdded,
                            dateModified = dateModified,
                            folderName = folderName,
                            filePath = filePath,
                            mimeType = mime
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        videoList
    }

    suspend fun resolveVideoFromUri(uri: Uri): VideoItem? = withContext(Dispatchers.IO) {
        var displayName = "Selected_Video"
        var size = 0L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        displayName = cursor.getString(nameIndex) ?: displayName
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {}

        var duration = 0L
        var width = 0
        var height = 0
        var mimeType = "video/*"

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "video/*"
            retriever.release()
        } catch (_: Exception) {}

        val title = displayName.substringBeforeLast(".")

        VideoItem(
            id = System.currentTimeMillis(),
            contentUri = uri.toString(),
            title = title,
            displayName = displayName,
            durationMs = duration,
            sizeBytes = size,
            width = width,
            height = height,
            dateAdded = System.currentTimeMillis() / 1000,
            dateModified = System.currentTimeMillis() / 1000,
            folderName = "Selected Videos",
            filePath = uri.path ?: "",
            mimeType = mimeType
        )
    }

    fun groupIntoFolders(videos: List<VideoItem>): List<VideoFolder> {
        val folderMap = linkedMapOf<String, MutableList<VideoItem>>()

        for (video in videos) {
            folderMap.getOrPut(video.folderName) { mutableListOf() }.add(video)
        }

        return folderMap.map { (name, items) ->
            val firstItem = items.firstOrNull()
            val folderPath = firstItem?.filePath?.let {
                try {
                    File(it).parent ?: ""
                } catch (_: Exception) {
                    ""
                }
            } ?: ""

            VideoFolder(
                name = name,
                path = folderPath,
                videoCount = items.size,
                firstVideoUri = firstItem?.contentUri ?: ""
            )
        }.sortedByDescending { it.videoCount }
    }
}
