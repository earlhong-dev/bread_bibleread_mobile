package com.bibleread.bread.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bibleread.bread.ui.theme.BackgroundDark

@Composable
fun ChatsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(top = 16.dp)
    ) {
        // Header
        Text(
            text = "CHATS",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)
        )

        // Search Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFE0E0E0)
        ) {
            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Search", color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stories / Notes
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(4) { index ->
                NoteItem(index)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Messages Header
        Text(
            text = "Messages",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Messages List
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(10) {
                ChatItem()
            }
        }
    }
}

@Composable
fun NoteItem(index: Int) {
    val notes = listOf("Share a note", "grabe ito sa kanta na praise habang drive", "Kinig ako hehe", "ano nanaman yung ginagawa nyo sa ano ko")
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Box(
            modifier = Modifier
                .background(Color.Gray, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Text(
                text = notes.getOrElse(index) { "" },
                fontSize = 10.sp,
                color = Color.White,
                maxLines = 2,
                lineHeight = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
        )
    }
}

@Composable
fun ChatItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "toaderrr", color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(text = "4 new messages", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = "3m", color = Color.Gray, fontSize = 12.sp)
    }
}