package com.chatapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Strict Black & White Theme (Manga Style)
private val MangaColorScheme = lightColorScheme(
    primary = MangaPrimary,
    onPrimary = MangaOnPrimary,
    primaryContainer = Black,
    onPrimaryContainer = White,
    secondary = MangaSecondary,
    onSecondary = MangaOnSecondary,
    tertiary = Black,
    onTertiary = White,
    background = MangaBackground,
    onBackground = Black,
    surface = MangaSurface,
    onSurface = MangaOnSurface,
    surfaceVariant = LightGrey,
    onSurfaceVariant = Black,
    error = Black, // Even errors are black (maybe with texture later)
    onError = White
)

// Inverted for Dark Mode (White ink on Black paper)
private val DarkMangaColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = White,
    onPrimaryContainer = Black,
    secondary = LightGrey,
    onSecondary = Black,
    tertiary = White,
    onTertiary = Black,
    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,
    surfaceVariant = DarkGrey,
    onSurfaceVariant = White,
    error = White,
    onError = Black
)

@Composable
fun MangaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic color to enforce B&W
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkMangaColorScheme
        else -> MangaColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
