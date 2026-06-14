package com.bibleread.bread.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bibleread.bread.ui.theme.BackgroundDark
import com.bibleread.bread.viewmodel.DownloadViewModel

@Composable
fun DownloadsScreen(vm: DownloadViewModel = viewModel()) {
    val bookStates by vm.bookStates.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Text(
            text = "DOWNLOADS",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 8.dp)
        )

        val downloadedCount = bookStates.count { it.isDownloaded }
        Text(
            text = "$downloadedCount / ${bookStates.size} books downloaded",
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)
        )

        HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)

        if (bookStates.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            LazyColumn {
                items(bookStates) { state ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = state.book,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = when {
                                    state.isDownloading -> "Downloading..."
                                    state.isDownloaded -> "Downloaded"
                                    else -> "Not downloaded"
                                },
                                color = when {
                                    state.isDownloading -> Color(0xFFFFAA00)
                                    state.isDownloaded -> Color(0xFF4CAF50)
                                    else -> Color.Gray
                                },
                                fontSize = 12.sp
                            )
                        }

                        when {
                            state.isDownloading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFFFFAA00),
                                    strokeWidth = 2.dp
                                )
                            }
                            state.isDownloaded -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Downloaded",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            else -> {
                                IconButton(
                                    onClick = { vm.downloadBook(state.book) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(
                        color = Color.DarkGray,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
