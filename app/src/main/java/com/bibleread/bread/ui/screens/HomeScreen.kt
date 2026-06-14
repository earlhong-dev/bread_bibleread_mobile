package com.bibleread.bread.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibleread.bread.ui.theme.*

@Composable
fun HomeScreen() {
    var selectedSpace by remember { mutableStateOf("Friends") }
    val spaces = listOf("Friends", "Community", "Church")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Space choices at the top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            spaces.forEach { space ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedSpace = space }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = space,
                        color = if (selectedSpace == space) Color.White else Color.Gray,
                        fontWeight = if (selectedSpace == space) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                    if (selectedSpace == space) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(4.dp)
                                .background(BreadTan, RoundedCornerShape(50))
                        )
                    }
                }
            }
        }
        
        // Content area - currently empty as requested
        Box(modifier = Modifier.fillMaxSize())
    }
}