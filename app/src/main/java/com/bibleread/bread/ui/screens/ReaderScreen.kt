package com.bibleread.bread.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bibleread.bread.ui.theme.BackgroundDark
import com.bibleread.bread.viewmodel.BibleUiState
import com.bibleread.bread.viewmodel.BibleViewModel

val BIBLE_BOOKS = mapOf(
    // Old Testament
    "Genesis" to 50, "Exodo" to 40, "Levitico" to 27, "Mga Bilang" to 36,
    "Deuteronomio" to 34, "Josue" to 24, "Mga Hukom" to 21, "Ruth" to 4,
    "1 Samuel" to 31, "2 Samuel" to 24, "1 Mga Hari" to 22, "2 Mga Hari" to 25,
    "1 Mga Cronica" to 29, "2 Mga Cronica" to 36, "Ezra" to 10, "Nehemias" to 13,
    "Ester" to 10, "Job" to 42, "Mga Awit" to 150, "Mga Kawikaan" to 31,
    "Ang Mangangaral" to 12, "Ang Awit ni Solomon" to 8, "Isaias" to 66, "Jeremias" to 52,
    "Mga Panaghoy" to 5, "Ezekiel" to 48, "Daniel" to 12, "Hosea" to 14,
    "Joel" to 3, "Amos" to 9, "Obadias" to 1, "Jonas" to 4, "Mikas" to 7,
    "Nahum" to 3, "Habakuk" to 3, "Zefanias" to 3, "Hagai" to 2,
    "Zacarias" to 14, "Malakias" to 4,
    // New Testament
    "Mateo" to 28, "Marcos" to 16, "Lucas" to 24, "Juan" to 21,
    "Mga Gawa" to 28, "Mga Taga-Roma" to 16,
    "1 Mga Taga-Corinto" to 16, "2 Mga Taga-Corinto" to 13, "Mga Taga-Galacia" to 6,
    "Mga Taga-Efeso" to 6, "Mga Taga-Filipos" to 4, "Mga Taga-Colosas" to 4,
    "1 Mga Taga-Tesalonica" to 5, "2 Mga Taga-Tesalonica" to 3, "1 Timoteo" to 6,
    "2 Timoteo" to 4, "Tito" to 3, "Filemon" to 1, "Mga Hebreo" to 13,
    "Santiago" to 5, "1 Pedro" to 5, "2 Pedro" to 3, "1 Juan" to 5,
    "2 Juan" to 1, "3 Juan" to 1, "Judas" to 1, "Pahayag" to 22,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(vm: BibleViewModel = viewModel()) {
    val books = BIBLE_BOOKS.keys.toList()

    var selectedBook by remember { mutableStateOf("Genesis") }
    var showBookSelection by remember { mutableStateOf(false) }
    var targetChapter by remember { mutableIntStateOf(1) }
    
    // Appearance settings
    var fontSize by remember { mutableFloatStateOf(17f) }
    var showSettings by remember { mutableStateOf(false) }

    val uiState by vm.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState) {
        if (uiState is BibleUiState.Success) {
            val verses = (uiState as BibleUiState.Success).verses
            val versesByChapter = verses.groupBy { it.chapter }

            var targetIndex = 0
            for ((chapter, chapterVerses) in versesByChapter) {
                if (chapter == targetChapter) break
                targetIndex += 1 // sticky header
                targetIndex += chapterVerses.size // verse items
                targetIndex += 1 // spacer
            }
            listState.scrollToItem(targetIndex)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
        ) {
            // Selection Buttons Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Books Button (Center, Taller, Big/Bold text)
                Surface(
                    onClick = { showBookSelection = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$selectedBook $targetChapter",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Settings Button (Right)
                Surface(
                    onClick = { showSettings = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }
            }

            // Content
            when (val state = uiState) {
                is BibleUiState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No Content", color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp)
                    }
                }
                is BibleUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
                is BibleUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = Color.Red, modifier = Modifier.padding(24.dp))
                    }
                }
                is BibleUiState.Success -> {
                    val versesByChapter = state.verses.groupBy { it.chapter }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                    ) {
                        versesByChapter.forEach { (chapter, verses) ->
                            stickyHeader(key = "$selectedBook-$chapter") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BackgroundDark)
                                        .padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = "$selectedBook $chapter",
                                        color = Color.White,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                }
                            }
                            items(verses, key = { "${it.book}-${it.chapter}-${it.verse}" }) { verse ->
                                // Heading
                                if (!verse.heading.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = verse.heading,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                    // Subheading
                                    if (!verse.subheading.isNullOrBlank()) {
                                        Text(
                                            text = verse.subheading,
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
                                }
                                // Verse text
                                val verseLabel = verse.display ?: verse.verse.toString()
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = Color(0xFFAAAAAA), fontWeight = FontWeight.Bold, fontSize = 12.sp)) {
                                            append("$verseLabel  ")
                                        }
                                        withStyle(SpanStyle(color = Color.White, fontSize = fontSize.sp)) {
                                            append(verse.text.trim())
                                        }
                                    },
                                    modifier = Modifier.padding(bottom = 12.dp),
                                    lineHeight = (fontSize * 1.5).sp
                                )
                            }
                            item { Spacer(modifier = Modifier.height(24.dp)) }
                        }
                    }
                }
            }
        }

        // Settings Overlay
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            AppearanceSettingsOverlay(
                currentFontSize = fontSize,
                onFontSizeChange = { fontSize = it },
                onClose = { showSettings = false }
            )
        }

        // Full Screen Book Selection Overlay
        AnimatedVisibility(
            visible = showBookSelection,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            BookSelectionOverlay(
                books = books,
                onBookSelected = { book, chapter ->
                    targetChapter = chapter
                    selectedBook = book
                    showBookSelection = false
                    vm.loadChapter(book, chapter)
                },
                onClose = { showBookSelection = false }
            )
        }
    }
}

@Composable
fun BookSelectionOverlay(
    books: List<String>,
    onBookSelected: (String, Int) -> Unit,
    onClose: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Book, 2: Chapter
    var tempSelectedBook by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step == 2) {
                IconButton(onClick = { step = 1 }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            }
            Text(
                text = if (step == 1) "Books" else tempSelectedBook,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        if (step == 1) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(books) { book ->
                    Text(
                        text = book,
                        color = Color.White,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tempSelectedBook = book
                                step = 2
                            }
                            .padding(16.dp)
                    )
                }
            }
        } else {
            val chapters = BIBLE_BOOKS[tempSelectedBook] ?: 1
            LazyVerticalGrid(
                columns = GridCells.Adaptive(60.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items((1..chapters).toList()) { chapter ->
                    Surface(
                        onClick = { onBookSelected(tempSelectedBook, chapter) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.1f)
                    ) {
                        Box(modifier = Modifier.size(50.dp), contentAlignment = Alignment.Center) {
                            Text(text = chapter.toString(), color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppearanceSettingsOverlay(
    currentFontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Appearance",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = "Font Size", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = currentFontSize,
                onValueChange = onFontSizeChange,
                valueRange = 12f..32f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
            Text(
                text = "${currentFontSize.toInt()} sp",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                modifier = Modifier.align(Alignment.End)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(text = "Font Style", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sample text preview
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.05f)
            ) {
                Text(
                    text = "In the beginning God created the heaven and the earth.",
                    color = Color.White,
                    fontSize = currentFontSize.sp,
                    modifier = Modifier.padding(16.dp),
                    lineHeight = (currentFontSize * 1.5).sp
                )
            }
        }
    }
}
