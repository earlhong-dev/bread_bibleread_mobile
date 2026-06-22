package com.bibleread.bread.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bibleread.bread.ui.theme.BackgroundDark

@Composable
fun JournalScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    )
}
