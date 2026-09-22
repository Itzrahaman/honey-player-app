package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.SortOption
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerSettings(
    val defaultSpeed: Float = 1.0f,
    val defaultSeekIntervalMs: Long = 10000L,
    val autoResume: Boolean = true,
    val autoPlayNext: Boolean = true,
    val gesturesEnabled: Boolean = true,
    val gestureSensitivity: String = "normal", // "low", "normal", "high"
    val doubleTapSeekDurationMs: Long = 10000L,
    val defaultOrientation: String = "auto", // "auto", "landscape", "portrait"
    val defaultAspectRatio: Int = 0, // 0 = Fit, 1 = Zoom/Crop, 2 = Fill, 3 = 100% Original, 4 = 16:9, 5 = 4:3
    val subtitlesEnabled: Boolean = true,
    val subtitleSizeSp: Float = 16f,
    val subtitleColor: String = "white", // "white", "honey_gold", "yellow", "cyan", "green"
    val subtitleBackground: String = "semi_black", // "semi_black", "transparent", "solid_black"
    val subtitlePosition: String = "bottom", // "bottom", "raised", "center", "top"
    val subtitleDelayMs: Long = 0L,
    val performanceMode: String = "balanced", // "balanced", "battery_saver", "max_performance"
    val decoderMode: String = "hardware_preferred", // "hardware_preferred", "software_only"
    val backgroundAudioEnabled: Boolean = false,
    val controlsTimeoutSec: Int = 4,
    val pauseOnHeadphonesUnplugged: Boolean = true,
    val defaultSortOption: SortOption = SortOption.DATE_DESC,
    val defaultViewMode: String = "list", // "list" or "grid"
    val themeMode: ThemeMode = ThemeMode.AMOLED,
    val animationsEnabled: Boolean = true
)

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("honey_player_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<PlayerSettings> = _settings.asStateFlow()

    private fun loadSettings(): PlayerSettings {
        val themeStr = prefs.getString("theme_mode", ThemeMode.AMOLED.name) ?: ThemeMode.AMOLED.name
        val theme = try { ThemeMode.valueOf(themeStr) } catch (_: Exception) { ThemeMode.AMOLED }

        val sortStr = prefs.getString("sort_option", SortOption.DATE_DESC.name) ?: SortOption.DATE_DESC.name
        val sort = try { SortOption.valueOf(sortStr) } catch (_: Exception) { SortOption.DATE_DESC }

        return PlayerSettings(
            defaultSpeed = prefs.getFloat("default_speed", 1.0f),
            defaultSeekIntervalMs = prefs.getLong("seek_interval", 10000L),
            autoResume = prefs.getBoolean("auto_resume", true),
            autoPlayNext = prefs.getBoolean("auto_play_next", true),
            gesturesEnabled = prefs.getBoolean("gestures_enabled", true),
            gestureSensitivity = prefs.getString("gesture_sensitivity", "normal") ?: "normal",
            doubleTapSeekDurationMs = prefs.getLong("double_tap_seek", 10000L),
            defaultOrientation = prefs.getString("default_orientation", "auto") ?: "auto",
            defaultAspectRatio = prefs.getInt("aspect_ratio", 0),
            subtitlesEnabled = prefs.getBoolean("subtitles_enabled", true),
            subtitleSizeSp = prefs.getFloat("subtitle_size", 16f),
            subtitleColor = prefs.getString("subtitle_color", "white") ?: "white",
            subtitleBackground = prefs.getString("subtitle_bg", "semi_black") ?: "semi_black",
            subtitlePosition = prefs.getString("subtitle_pos", "bottom") ?: "bottom",
            subtitleDelayMs = prefs.getLong("subtitle_delay", 0L),
            performanceMode = prefs.getString("performance_mode", "balanced") ?: "balanced",
            decoderMode = prefs.getString("decoder_mode", "hardware_preferred") ?: "hardware_preferred",
            backgroundAudioEnabled = prefs.getBoolean("bg_audio", false),
            controlsTimeoutSec = prefs.getInt("controls_timeout", 4),
            pauseOnHeadphonesUnplugged = prefs.getBoolean("pause_on_unplugged", true),
            defaultSortOption = sort,
            defaultViewMode = prefs.getString("view_mode", "list") ?: "list",
            themeMode = theme,
            animationsEnabled = prefs.getBoolean("animations_enabled", true)
        )
    }

    fun updateSettings(newSettings: PlayerSettings) {
        prefs.edit().apply {
            putFloat("default_speed", newSettings.defaultSpeed)
            putLong("seek_interval", newSettings.defaultSeekIntervalMs)
            putBoolean("auto_resume", newSettings.autoResume)
            putBoolean("auto_play_next", newSettings.autoPlayNext)
            putBoolean("gestures_enabled", newSettings.gesturesEnabled)
            putString("gesture_sensitivity", newSettings.gestureSensitivity)
            putLong("double_tap_seek", newSettings.doubleTapSeekDurationMs)
            putString("default_orientation", newSettings.defaultOrientation)
            putInt("aspect_ratio", newSettings.defaultAspectRatio)
            putBoolean("subtitles_enabled", newSettings.subtitlesEnabled)
            putFloat("subtitle_size", newSettings.subtitleSizeSp)
            putString("subtitle_color", newSettings.subtitleColor)
            putString("subtitle_bg", newSettings.subtitleBackground)
            putString("subtitle_pos", newSettings.subtitlePosition)
            putLong("subtitle_delay", newSettings.subtitleDelayMs)
            putString("performance_mode", newSettings.performanceMode)
            putString("decoder_mode", newSettings.decoderMode)
            putBoolean("bg_audio", newSettings.backgroundAudioEnabled)
            putInt("controls_timeout", newSettings.controlsTimeoutSec)
            putBoolean("pause_on_unplugged", newSettings.pauseOnHeadphonesUnplugged)
            putString("sort_option", newSettings.defaultSortOption.name)
            putString("view_mode", newSettings.defaultViewMode)
            putString("theme_mode", newSettings.themeMode.name)
            putBoolean("animations_enabled", newSettings.animationsEnabled)
            apply()
        }
        _settings.value = newSettings
    }

    fun setThemeMode(mode: ThemeMode) {
        val updated = _settings.value.copy(themeMode = mode)
        updateSettings(updated)
    }

    fun setViewMode(viewMode: String) {
        val updated = _settings.value.copy(defaultViewMode = viewMode)
        updateSettings(updated)
    }

    fun setSortOption(sort: SortOption) {
        val updated = _settings.value.copy(defaultSortOption = sort)
        updateSettings(updated)
    }
}
