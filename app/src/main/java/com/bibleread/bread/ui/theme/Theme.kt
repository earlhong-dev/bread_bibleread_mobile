package com.bibleread.bread.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

// Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary           = BreadTan,
    secondary         = FellowshipPurple,
    tertiary          = PersonalBibleGreen,
    background        = Color.White,
    surface           = Color.White,
    surfaceVariant    = Color(0xFFE8E8E8),  // Row container background
    secondaryContainer = Color(0xFFDCDCDC), // Buttons inside container (slightly darker)
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
    background        = BackgroundDark,
    surface           = BackgroundDark,
    surfaceVariant    = Color(0xFF1E1E1E),  // Row container background
    secondaryContainer = Color(0xFF2A2A2A), // Buttons inside container (slightly lighter)
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

    CompositionLocalProvider(LocalThemeIndex provides themeIndex) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}
