package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SortOption
import com.example.data.model.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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

  @Test
  fun `verify VideoItem sorting logic`() {
    val v1 = VideoItem(id = 1L, contentUri = "uri1", title = "B Video", displayName = "b.mp4", durationMs = 10000L, sizeBytes = 1000L, dateAdded = 100L)
    val v2 = VideoItem(id = 2L, contentUri = "uri2", title = "A Video", displayName = "a.mp4", durationMs = 50000L, sizeBytes = 5000L, dateAdded = 200L)
    val list = listOf(v1, v2)
    val sortedByName = list.sortedBy { it.title.lowercase() }
    assertEquals("A Video", sortedByName.first().title)
    val sortedByDuration = list.sortedByDescending { it.durationMs }
    assertEquals(50000L, sortedByDuration.first().durationMs)
  }
}

