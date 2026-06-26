package com.bibleread.bread.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary      = BreadTan,
    secondary    = FellowshipPurple,
    tertiary     = PersonalBibleGreen,
    background   = BackgroundDark,
    surface      = DarkGray,
    onPrimary    = Color.Black,
    onSecondary  = Color.White,
    onTertiary   = Color.Black,
    onBackground = Color.White,
    onSurface    = Color.White
)

@Composable
fun BreadTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography,
        content     = content
    )
}
