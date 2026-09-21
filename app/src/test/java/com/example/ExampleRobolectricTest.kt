package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SortOption
import com.example.data.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("HONEY Player", appName)
  }

  @Test
  fun `verify VideoItem duration and size formatting`() {
    val video = VideoItem(
        id = 1L,
        contentUri = "content://media/external/video/media/1",
        title = "Sample Video",
        displayName = "sample.mp4",
        durationMs = 125000L, // 2 mins 5 secs
        sizeBytes = 52428800L, // 50 MB
        width = 1920,
        height = 1080
    )
    assertEquals("02:05", video.formattedDuration)
    assertEquals("50.0 MB", video.formattedSize)
    assertEquals("1080p", video.resolutionLabel)
  }
}

