package com.example.data.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import coil.size.Precision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThumbnailHelper {
    @Volatile
    private var imageLoader: ImageLoader? = null

    fun getImageLoader(context: Context): ImageLoader {
        return imageLoader ?: synchronized(this) {
            imageLoader ?: ImageLoader.Builder(context.applicationContext)
                .components {
                    add(VideoFrameDecoder.Factory())
                }
                .memoryCache {
                    MemoryCache.Builder(context.applicationContext)
                        .maxSizePercent(0.20) // Cap memory cache at 20% of app heap
                        .build()
                }
                .diskCache {
                    DiskCache.Builder()
                        .directory(context.applicationContext.cacheDir.resolve("honey_thumbs"))
                        .maxSizeBytes(80L * 1024 * 1024) // 80 MB disk cache
                        .build()
                }
                .crossfade(true)
                .build().also { imageLoader = it }
        }
    }

    /**
     * Builds a downsampled, cached ImageRequest for video thumbnails.
     * Setting explicit width/height forces Coil to downsample during decode,
     * avoiding full-resolution 4K/1080p bitmap allocations.
     * allowRgb565 cuts memory footprint in half (2 bytes per pixel instead of 4).
     */
    fun buildThumbnailRequest(
        context: Context,
        uri: Uri,
        widthPx: Int = 360,
        heightPx: Int = 202,
        frameMillis: Long = 2000L
    ): ImageRequest {
        return ImageRequest.Builder(context.applicationContext)
            .data(uri)
            .videoFrameMillis(frameMillis)
            .size(widthPx, heightPx)
            .precision(Precision.INEXACT)
            .allowRgb565(true)
            .memoryCacheKey("${uri}_${widthPx}x${heightPx}_$frameMillis")
            .diskCacheKey("${uri}_${widthPx}x${heightPx}_$frameMillis")
            .crossfade(150)
            .build()
    }

    suspend fun loadThumbnailBitmap(context: Context, uriString: String, width: Int = 320, height: Int = 180): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(uri, Size(width, height), null)
                } else {
                    val id = uri.lastPathSegment?.toLongOrNull()
                    if (id != null) {
                        MediaStore.Video.Thumbnails.getThumbnail(
                            context.contentResolver,
                            id,
                            MediaStore.Video.Thumbnails.MINI_KIND,
                            null
                        )
                    } else null
                }
            } catch (_: Exception) {
                null
            }
        }
    }
}
