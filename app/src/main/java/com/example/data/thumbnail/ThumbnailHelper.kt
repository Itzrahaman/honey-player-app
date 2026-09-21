package com.example.data.thumbnail

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ThumbnailHelper {
    private var imageLoader: ImageLoader? = null

    fun getImageLoader(context: Context): ImageLoader {
        return imageLoader ?: synchronized(this) {
            val loader = ImageLoader.Builder(context.applicationContext)
                .components {
                    add(VideoFrameDecoder.Factory())
                }
                .crossfade(true)
                .build()
            imageLoader = loader
            loader
        }
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
