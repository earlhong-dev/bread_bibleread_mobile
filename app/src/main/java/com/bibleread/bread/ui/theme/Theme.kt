package com.bibleread.bread.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.SideEffect
import android.app.Activity
import androidx.compose.ui.graphics.toArgb

// Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary           = BreadTan,
    secondary         = FellowshipPurple,
    tertiary          = PersonalBibleGreen,
    background        = Color(0xFFFEF9F3),
    surface           = Color(0xFFFEF9F3),
    surfaceVariant    = Color(0xFFEEE9E3),
    secondaryContainer = Color(0xFFDED9D3),
    onPrimary         = Color.White,
    onSecondary       = Color.Black,
    onTertiary        = Color.White,
    onBackground      = Color.Black,
    onSurface         = Color.Black
)

// Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary           = BreadTan,
    secondary         = FellowshipPurple,
    tertiary          = PersonalBibleGreen,
    background        = Color(0xFF1A1A1A),
    surface           = Color(0xFF1A1A1A),
    surfaceVariant    = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF2A2A2A),
    onPrimary         = Color.Black,
    onSecondary       = Color.White,
    onTertiary        = Color.Black,
    onBackground      = Color.White,
    onSurface         = Color.White
)

val LocalThemeIndex = staticCompositionLocalOf { 1 } // Default to 1 (Dark)

@Composable
fun BreadTheme(themeIndex: Int = 1, content: @Composable () -> Unit) {
    val colorScheme = when (themeIndex) {
        0 -> LightColorScheme
        else -> DarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar and navigation bar to transparent for edge-to-edge display
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            
            // Set status bar icons: dark icons for Light (0) and Sepia (2) themes, light icons for Dark (1) theme
            val darkStatusIcons = themeIndex == 0
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkStatusIcons
        }
    }

    CompositionLocalProvider(LocalThemeIndex provides themeIndex) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}
