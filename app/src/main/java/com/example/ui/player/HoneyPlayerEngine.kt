package com.example.ui.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import com.example.data.local.PlayerSettings

data class AudioTrackInfo(
    val id: String,
    val trackGroup: TrackGroup,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val channelCount: Int,
    val sampleRate: Int,
    val mimeType: String,
    val isSelected: Boolean
)

data class SubtitleTrackInfo(
    val id: String,
    val trackGroup: TrackGroup?,
    val trackIndex: Int,
    val label: String,
    val language: String,
    val mimeType: String,
    val isSelected: Boolean,
    val isExternal: Boolean = false
)

object HoneyPlayerEngine {

    @OptIn(UnstableApi::class)
    fun buildExoPlayer(
        context: Context,
        settings: PlayerSettings,
        forceSoftwareDecoders: Boolean = false
    ): ExoPlayer {
        // Configure renderers with decoder fallback enabled
        val renderersFactory = DefaultRenderersFactory(context.applicationContext).apply {
            setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            setEnableDecoderFallback(true) // Crucial: fall back to software decoder if HW decoder fails
            if (forceSoftwareDecoders || settings.decoderMode == "software_only") {
                // If user forced software only, prefer non-hardware codecs
                setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                    val defaultList = MediaCodecSelector.DEFAULT.getDecoderInfos(
                        mimeType,
                        requiresSecureDecoder,
                        requiresTunnelingDecoder
                    )
                    defaultList.sortedBy { it.hardwareAccelerated }
                }
            }
        }

        // Configure LoadControl based on performance mode
        val loadControlBuilder = DefaultLoadControl.Builder()
        when (settings.performanceMode) {
            "battery_saver" -> {
                loadControlBuilder.setBufferDurationsMs(
                    8_000,   // minBufferMs
                    16_000,  // maxBufferMs
                    1_000,   // bufferForPlaybackMs
                    2_000    // bufferForPlaybackAfterRebufferMs
                )
            }
            "max_performance" -> {
                loadControlBuilder.setBufferDurationsMs(
                    30_000,  // minBufferMs
                    60_000,  // maxBufferMs
                    1_500,   // bufferForPlaybackMs
                    3_000    // bufferForPlaybackAfterRebufferMs
                )
            }
            else -> { // "balanced"
                loadControlBuilder.setBufferDurationsMs(
                    15_000,  // minBufferMs
                    30_000,  // maxBufferMs
                    1_200,   // bufferForPlaybackMs
                    2_500    // bufferForPlaybackAfterRebufferMs
                )
            }
        }
        loadControlBuilder.setPrioritizeTimeOverSizeThresholds(true)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(context.applicationContext, renderersFactory)
            .setLoadControl(loadControlBuilder.build())
            .setAudioAttributes(audioAttributes, settings.pauseOnHeadphonesUnplugged)
            .setHandleAudioBecomingNoisy(settings.pauseOnHeadphonesUnplugged)
            .setSeekBackIncrementMs(settings.defaultSeekIntervalMs)
            .setSeekForwardIncrementMs(settings.defaultSeekIntervalMs)
            .build()
    }

    @OptIn(UnstableApi::class)
    fun getAvailableAudioTracks(player: ExoPlayer): List<AudioTrackInfo> {
        val tracks = player.currentTracks
        val result = mutableListOf<AudioTrackInfo>()

        for (group in tracks.groups) {
            if (group.type != C.TRACK_TYPE_AUDIO) continue

            val trackGroup = group.mediaTrackGroup
            for (i in 0 until trackGroup.length) {
                val format = trackGroup.getFormat(i)
                val lang = format.language ?: "und"
                val label = format.label ?: formatLanguageName(lang)
                val channels = format.channelCount
                val mime = format.sampleMimeType ?: "audio"
                val isSelected = group.isTrackSelected(i)

                result.add(
                    AudioTrackInfo(
                        id = "${trackGroup.id}_$i",
                        trackGroup = trackGroup,
                        trackIndex = i,
                        label = label,
                        language = lang,
                        channelCount = channels,
                        sampleRate = format.sampleRate,
                        mimeType = mime,
                        isSelected = isSelected
                    )
                )
            }
        }
        return result
    }

    @OptIn(UnstableApi::class)
    fun selectAudioTrack(player: ExoPlayer, track: AudioTrackInfo) {
        val override = TrackSelectionOverride(track.trackGroup, track.trackIndex)
        val params = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            .addOverride(override)
            .build()
        player.trackSelectionParameters = params
    }

    @OptIn(UnstableApi::class)
    fun getAvailableSubtitleTracks(player: ExoPlayer): List<SubtitleTrackInfo> {
        val tracks = player.currentTracks
        val result = mutableListOf<SubtitleTrackInfo>()

        for (group in tracks.groups) {
            if (group.type != C.TRACK_TYPE_TEXT) continue

            val trackGroup = group.mediaTrackGroup
            for (i in 0 until trackGroup.length) {
                val format = trackGroup.getFormat(i)
                val lang = format.language ?: "und"
                val label = format.label ?: "Track ${result.size + 1} (${formatLanguageName(lang)})"
                val isSelected = group.isTrackSelected(i)

                result.add(
                    SubtitleTrackInfo(
                        id = "${trackGroup.id}_$i",
                        trackGroup = trackGroup,
                        trackIndex = i,
                        label = label,
                        language = lang,
                        mimeType = format.sampleMimeType ?: "text/unknown",
                        isSelected = isSelected,
                        isExternal = false
                    )
                )
            }
        }
        return result
    }

    @OptIn(UnstableApi::class)
    fun selectSubtitleTrack(player: ExoPlayer, track: SubtitleTrackInfo?) {
        val paramsBuilder = player.trackSelectionParameters.buildUpon()
        if (track == null || track.trackGroup == null) {
            // Disable subtitles
            paramsBuilder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
            paramsBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            paramsBuilder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            val override = TrackSelectionOverride(track.trackGroup, track.trackIndex)
            paramsBuilder.clearOverridesOfType(C.TRACK_TYPE_TEXT)
            paramsBuilder.addOverride(override)
        }
        player.trackSelectionParameters = paramsBuilder.build()
    }

    fun buildMediaItem(uriString: String, mimeType: String? = null): MediaItem {
        val builder = MediaItem.Builder().setUri(Uri.parse(uriString))
        if (!mimeType.isNullOrBlank() && mimeType != "video/*") {
            builder.setMimeType(mimeType)
        }
        return builder.build()
    }

    @OptIn(UnstableApi::class)
    fun addExternalSubtitle(context: Context, player: ExoPlayer, subtitleUri: Uri) {
        val subConfig = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
            .setLanguage("ext")
            .setLabel("External Subtitle")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()
        val currentItem = player.currentMediaItem ?: return
        val newItem = currentItem.buildUpon()
            .setSubtitleConfigurations(listOf(subConfig))
            .build()
        val currentPos = player.currentPosition
        val wasPlaying = player.isPlaying
        player.setMediaItem(newItem, currentPos)
        if (wasPlaying) player.play()
    }

    fun buildMediaItemWithSubtitles(
        videoUri: Uri,
        externalSubtitleUri: Uri? = null,
        subtitleMimeType: String = MimeTypes.APPLICATION_SUBRIP
    ): MediaItem {
        val builder = MediaItem.Builder().setUri(videoUri)
        if (externalSubtitleUri != null) {
            val subConfig = MediaItem.SubtitleConfiguration.Builder(externalSubtitleUri)
                .setMimeType(subtitleMimeType)
                .setLanguage("ext")
                .setLabel("External Subtitle")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
            builder.setSubtitleConfigurations(listOf(subConfig))
        }
        return builder.build()
    }

    private fun formatLanguageName(code: String): String {
        return when (code.lowercase()) {
            "en", "eng" -> "English"
            "es", "spa" -> "Spanish"
            "fr", "fra", "fre" -> "French"
            "de", "deu", "ger" -> "German"
            "it", "ita" -> "Italian"
            "pt", "por" -> "Portuguese"
            "ru", "rus" -> "Russian"
            "zh", "zho", "chi" -> "Chinese"
            "ja", "jpn" -> "Japanese"
            "ko", "kor" -> "Korean"
            "ar", "ara" -> "Arabic"
            "hi", "hin" -> "Hindi"
            "tr", "tur" -> "Turkish"
            "und" -> "Default Track"
            else -> code.uppercase()
        }
    }
}
