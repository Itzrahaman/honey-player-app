package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    DARK,
    AMOLED,
    SYSTEM
}

val LocalThemeMode = staticCompositionLocalOf { ThemeMode.DARK }

private val HoneyDarkColorScheme = darkColorScheme(
    primary = HoneyGold,
    onPrimary = Color(0xFF1E1400),
    primaryContainer = HoneyGoldDark,
    onPrimaryContainer = Color(0xFFFFFAEE),
    secondary = HoneyAccent,
    onSecondary = Color(0xFF1E1400),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkCardBorder,
    error = ErrorRed,
    onError = Color.White
)

private val HoneyAmoledColorScheme = darkColorScheme(
    primary = HoneyGold,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF332300),
    onPrimaryContainer = HoneyGoldLight,
    secondary = HoneyAccent,
    onSecondary = Color.Black,
    background = AmoledBackground,
    onBackground = DarkTextPrimary,
    surface = AmoledSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = AmoledCardBorder,
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isAmoled = themeMode == ThemeMode.AMOLED
    val colorScheme = when {
        isAmoled -> HoneyAmoledColorScheme
        themeMode == ThemeMode.DARK -> HoneyDarkColorScheme
        isSystemDark -> HoneyDarkColorScheme
        else -> HoneyDarkColorScheme // Video players look best in dark
    }

    CompositionLocalProvider(LocalThemeMode provides themeMode) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
