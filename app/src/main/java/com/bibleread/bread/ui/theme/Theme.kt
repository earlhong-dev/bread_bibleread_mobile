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
    background        = Color(0xFFECECEC),
    surface           = Color(0xFFECECEC),
    surfaceVariant    = Color(0xFFDCDCDC),  // Row container background (slightly darker than base bg)
    secondaryContainer = Color(0xFFCECECE), // Buttons inside container (slightly darker than container)
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
    background        = Color(0xFF18191A),
    surface           = Color(0xFF18191A),
    surfaceVariant    = Color(0xFF242526),  // Row container background (slightly lighter than base bg)
    secondaryContainer = Color(0xFF303134), // Buttons inside container (slightly lighter than container)
    onPrimary         = Color.Black,
    onSecondary       = Color.White,
    onTertiary        = Color.Black,
    onBackground      = Color.White,
    onSurface         = Color.White
)

// Sepia Color Scheme
private val SepiaColorScheme = lightColorScheme(
    primary           = BreadTan,
    secondary         = FellowshipPurple,
    tertiary          = PersonalBibleGreen,
    background        = Color(0xFFF4ECD8),
    surface           = Color(0xFFF4ECD8),
    surfaceVariant    = Color(0xFFE6DCBE),  // Row container background
    secondaryContainer = Color(0xFFD9CDB0), // Buttons inside container (slightly darker)
    onPrimary         = Color.White,
    onSecondary       = Color(0xFF5B4636),
    onTertiary        = Color.White,
    onBackground      = Color(0xFF5B4636),
    onSurface         = Color(0xFF5B4636)
)


val LocalThemeIndex = staticCompositionLocalOf { 1 } // Default to 1 (Dark)

@Composable
fun BreadTheme(themeIndex: Int = 1, content: @Composable () -> Unit) {
    val colorScheme = when (themeIndex) {
        0 -> LightColorScheme
        2 -> SepiaColorScheme
        else -> DarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar background color matching theme background
            window.statusBarColor = colorScheme.background.toArgb()
            
            // Set status bar icons: dark icons for Light (0) and Sepia (2) themes, light icons for Dark (1) theme
            val darkStatusIcons = themeIndex == 0 || themeIndex == 2
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
