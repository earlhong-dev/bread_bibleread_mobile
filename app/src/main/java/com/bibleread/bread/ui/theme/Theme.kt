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
    background        = Color(0xFFEEECED),
    surface           = Color(0xFFEEECED),
    surfaceVariant    = Color(0xFFDCDCDC),
    secondaryContainer = Color(0xFFCECECE),
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
    background        = Color(0xFF131313),
    surface           = Color(0xFF131313),
    surfaceVariant    = Color(0xFF242526),
    secondaryContainer = Color(0xFF303134),
    onPrimary         = Color.Black,
    onSecondary       = Color.White,
    onTertiary        = Color.Black,
    onBackground      = Color.White,
    onSurface         = Color.White
)

// Paper Color Scheme (formerly Sepia)
private val PaperColorScheme = lightColorScheme(
    primary           = BreadTan,
    secondary         = FellowshipPurple,
    tertiary          = PersonalBibleGreen,
    background        = Color(0xFFFEF9F3),
    surface           = Color(0xFFFEF9F3),
    surfaceVariant    = Color(0xFFEDE5D5),
    secondaryContainer = Color(0xFFDFD4C0),
    onPrimary         = Color.White,
    onSecondary       = Color(0xFF5B4636),
    onTertiary        = Color.White,
    onBackground      = Color(0xFF5B4636),
    onSurface         = Color(0xFF5B4636)
)


val LocalThemeIndex = staticCompositionLocalOf { 0 } // Default to 0 (Dark)

@Composable
fun BreadTheme(themeIndex: Int = 0, content: @Composable () -> Unit) {
    val colorScheme = when (themeIndex) {
        1 -> LightColorScheme
        2 -> PaperColorScheme
        else -> DarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT

            // Dark icons for Light (1) and Paper (2), light icons for Dark (0)
            val darkIcons = themeIndex == 1 || themeIndex == 2
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = darkIcons
            insetsController.isAppearanceLightNavigationBars = darkIcons
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
