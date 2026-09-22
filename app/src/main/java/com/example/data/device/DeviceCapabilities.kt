package com.example.data.device

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.view.Display
import android.view.WindowManager
import kotlin.math.max

data class DecoderCapability(
    val name: String,
    val mimeType: String,
    val isHardwareAccelerated: Boolean,
    val isSoftwareOnly: Boolean,
    val maxSupportedWidth: Int,
    val maxSupportedHeight: Int,
    val maxSupportedFps: Double
)

data class HdrDisplayInfo(
    val isHdrSupported: Boolean,
    val supportedHdrTypes: List<String>,
    val isWideColorGamut: Boolean
)

data class DeviceCapabilitiesReport(
    val deviceModel: String,
    val androidVersion: String,
    val isLowRamDevice: Boolean,
    val supportsPictureInPicture: Boolean,
    val hdrInfo: HdrDisplayInfo,
    val maxVideoResolution: String,
    val maxVideoFps: Int,
    val videoDecoders: List<DecoderCapability>,
    val audioDecoders: List<String>,
    val supports4KPlayback: Boolean,
    val supportsHevcHardware: Boolean,
    val supportsAv1Hardware: Boolean,
    val supportsVp9Hardware: Boolean
)

object DeviceCapabilityDetector {

    fun getDeviceCapabilities(context: Context): DeviceCapabilitiesReport {
        val packageManager = context.packageManager
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

        val isLowRam = activityManager?.isLowRamDevice ?: false
        val supportsPip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        } else false

        // Detect HDR
        val hdrInfo = detectHdrInfo(windowManager)

        // Query MediaCodecList
        val videoDecoders = mutableListOf<DecoderCapability>()
        val audioMimeSet = mutableSetOf<String>()

        var globalMaxWidth = 1920
        var globalMaxHeight = 1080
        var globalMaxFps = 60.0

        var hasHevcHw = false
        var hasAv1Hw = false
        var hasVp9Hw = false

        try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            for (codecInfo in codecList.codecInfos) {
                if (codecInfo.isEncoder) continue

                val isHw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    codecInfo.isHardwareAccelerated
                } else {
                    !codecInfo.name.startsWith("OMX.google.", ignoreCase = true) &&
                            !codecInfo.name.startsWith("c2.android.", ignoreCase = true)
                }

                val isSw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    codecInfo.isSoftwareOnly
                } else {
                    !isHw
                }

                for (mime in codecInfo.supportedTypes) {
                    if (mime.startsWith("video/", ignoreCase = true)) {
                        try {
                            val caps = codecInfo.getCapabilitiesForType(mime)
                            val videoCaps = caps.videoCapabilities

                            val maxWidth = (videoCaps?.supportedWidths?.upper as? Number)?.toInt() ?: 1920
                            val maxHeight = (videoCaps?.supportedHeights?.upper as? Number)?.toInt() ?: 1080
                            val maxFps = (videoCaps?.supportedFrameRates?.upper as? Number)?.toDouble() ?: 60.0

                            globalMaxWidth = kotlin.math.max(globalMaxWidth, maxWidth)
                            globalMaxHeight = kotlin.math.max(globalMaxHeight, maxHeight)
                            globalMaxFps = kotlin.math.max(globalMaxFps, maxFps)

                            if (isHw) {
                                if (mime.equals("video/hevc", ignoreCase = true)) hasHevcHw = true
                                if (mime.equals("video/av01", ignoreCase = true)) hasAv1Hw = true
                                if (mime.equals("video/x-vnd.on2.vp9", ignoreCase = true)) hasVp9Hw = true
                            }

                            // Keep primary format decoders
                            if (isCommonVideoMime(mime)) {
                                videoDecoders.add(
                                    DecoderCapability(
                                        name = codecInfo.name,
                                        mimeType = mime,
                                        isHardwareAccelerated = isHw,
                                        isSoftwareOnly = isSw,
                                        maxSupportedWidth = maxWidth,
                                        maxSupportedHeight = maxHeight,
                                        maxSupportedFps = maxFps
                                    )
                                )
                            }
                        } catch (_: Exception) {
                            // Capability not queryable for this codec
                        }
                    } else if (mime.startsWith("audio/", ignoreCase = true)) {
                        val cleanName = formatAudioMime(mime)
                        if (cleanName.isNotBlank()) {
                            audioMimeSet.add(cleanName)
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        val supports4K = globalMaxWidth >= 3840 || globalMaxHeight >= 2160

        return DeviceCapabilitiesReport(
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            isLowRamDevice = isLowRam,
            supportsPictureInPicture = supportsPip,
            hdrInfo = hdrInfo,
            maxVideoResolution = "${globalMaxWidth} × ${globalMaxHeight}",
            maxVideoFps = globalMaxFps.toInt(),
            videoDecoders = videoDecoders.distinctBy { "${it.mimeType}-${it.isHardwareAccelerated}" },
            audioDecoders = audioMimeSet.sorted(),
            supports4KPlayback = supports4K,
            supportsHevcHardware = hasHevcHw,
            supportsAv1Hardware = hasAv1Hw,
            supportsVp9Hardware = hasVp9Hw
        )
    }

    private fun detectHdrInfo(windowManager: WindowManager?): HdrDisplayInfo {
        var isHdr = false
        val supportedTypes = mutableListOf<String>()
        var isWideGamut = false

        if (windowManager != null) {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    windowManager.defaultDisplay
                } catch (_: Exception) { null }
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }

            if (display != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    isWideGamut = display.isWideColorGamut
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    isHdr = display.isHdr
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val hdrCaps = display.hdrCapabilities
                    if (hdrCaps != null) {
                        for (type in hdrCaps.supportedHdrTypes) {
                            when (type) {
                                Display.HdrCapabilities.HDR_TYPE_HDR10 -> supportedTypes.add("HDR10")
                                Display.HdrCapabilities.HDR_TYPE_HLG -> supportedTypes.add("HLG")
                                Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> supportedTypes.add("Dolby Vision")
                                Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> supportedTypes.add("HDR10+")
                            }
                        }
                        if (supportedTypes.isNotEmpty()) {
                            isHdr = true
                        }
                    }
                }
            }
        }

        return HdrDisplayInfo(
            isHdrSupported = isHdr,
            supportedHdrTypes = supportedTypes.distinct(),
            isWideColorGamut = isWideGamut
        )
    }

    private fun isCommonVideoMime(mime: String): Boolean {
        return mime.equals("video/avc", ignoreCase = true) ||
                mime.equals("video/hevc", ignoreCase = true) ||
                mime.equals("video/x-vnd.on2.vp9", ignoreCase = true) ||
                mime.equals("video/av01", ignoreCase = true) ||
                mime.equals("video/x-vnd.on2.vp8", ignoreCase = true) ||
                mime.equals("video/mp4v-es", ignoreCase = true)
    }

    private fun formatAudioMime(mime: String): String {
        return when {
            mime.contains("mp4a", ignoreCase = true) || mime.contains("aac", ignoreCase = true) -> "AAC"
            mime.contains("opus", ignoreCase = true) -> "Opus"
            mime.contains("vorbis", ignoreCase = true) -> "Vorbis"
            mime.contains("flac", ignoreCase = true) -> "FLAC"
            mime.contains("mpeg", ignoreCase = true) || mime.contains("mp3", ignoreCase = true) -> "MP3"
            mime.contains("ac3", ignoreCase = true) -> "Dolby Digital (AC-3)"
            mime.contains("eac3", ignoreCase = true) -> "Dolby Digital Plus (E-AC-3)"
            mime.contains("dts", ignoreCase = true) -> "DTS"
            mime.contains("raw", ignoreCase = true) -> "PCM Audio"
            else -> mime.substringAfter("audio/").uppercase()
        }
    }
}
