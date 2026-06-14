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
    "Genesis" to 50, "Exodus" to 40, "Leviticus" to 27, "Numbers" to 36,
    "Deuteronomy" to 34, "Joshua" to 24, "Judges" to 21, "Ruth" to 4,
    "1 Samuel" to 31, "2 Samuel" to 24, "1 Kings" to 22, "2 Kings" to 25,
    "1 Chronicles" to 29, "2 Chronicles" to 36, "Ezra" to 10, "Nehemiah" to 13,
    "Esther" to 10, "Job" to 42, "Psalms" to 150, "Proverbs" to 31,
    "Ecclesiastes" to 12, "Song of Solomon" to 8, "Isaiah" to 66, "Jeremiah" to 52,
    "Lamentations" to 5, "Ezekiel" to 48, "Daniel" to 12, "Hosea" to 14,
    "Joel" to 3, "Amos" to 9, "Obadiah" to 1, "Jonah" to 4, "Micah" to 7,
    "Nahum" to 3, "Habakkuk" to 3, "Zephaniah" to 3, "Haggai" to 2,
    "Zechariah" to 14, "Malachi" to 4, "Matthew" to 28, "Mark" to 16,
    "Luke" to 24, "John" to 21, "Acts" to 28, "Romans" to 16,
    "1 Corinthians" to 16, "2 Corinthians" to 13, "Galatians" to 6,
    "Ephesians" to 6, "Philippians" to 4, "Colossians" to 4,
    "1 Thessalonians" to 5, "2 Thessalonians" to 3, "1 Timothy" to 6,
    "2 Timothy" to 4, "Titus" to 3, "Philemon" to 1, "Hebrews" to 13,
    "James" to 5, "1 Peter" to 5, "2 Peter" to 3, "1 John" to 5,
    "2 John" to 1, "3 John" to 1, "Jude" to 1, "Revelation" to 22,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(vm: BibleViewModel = viewModel()) {
    val books = BIBLE_BOOKS.keys.toList()

    var selectedBook by remember { mutableStateOf(books[0]) }
    var showBookSelection by remember { mutableStateOf(false) }
    var showVersionSelection by remember { mutableStateOf(false) }
    var selectedVersion by remember { mutableStateOf("KJV") }
    var targetChapter by remember { mutableIntStateOf(1) }
    
    // Appearance settings
    var fontSize by remember { mutableFloatStateOf(17f) }
    var showSettings by remember { mutableStateOf(false) }

    val uiState by vm.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(selectedBook) {
        vm.loadBook(selectedBook, BIBLE_BOOKS[selectedBook] ?: 1)
        // We will scroll to targetChapter once the data is loaded in another LaunchedEffect
    }

    LaunchedEffect(uiState) {
        if (uiState is BibleUiState.Success) {
            val verses = (uiState as BibleUiState.Success).verses
            val versesByChapter = verses.groupBy { it.chapter }
            
            var targetIndex = 0
            for ((chapter, chapterVerses) in versesByChapter) {
                if (chapter == targetChapter) break
                targetIndex += 1 // header
                targetIndex += chapterVerses.size // items
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
                // Version Button (Left)
                Surface(
                    onClick = { showVersionSelection = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.height(48.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(selectedVersion, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

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
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = Color(0xFFAAAAAA), fontWeight = FontWeight.Bold, fontSize = 12.sp)) {
                                            append("${verse.verse}  ")
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
                },
                onClose = { showBookSelection = false }
            )
        }

        // Full Screen Version Selection Overlay
        AnimatedVisibility(
            visible = showVersionSelection,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            VersionSelectionOverlay(
                onVersionSelected = { version ->
                    selectedVersion = version
                    showVersionSelection = false
                },
                onClose = { showVersionSelection = false }
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
fun VersionSelectionOverlay(
    onVersionSelected: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Versions",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(listOf("KJV", "ASV", "WEB")) { version ->
                Text(
                    text = version,
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVersionSelected(version) }
                        .padding(16.dp)
                )
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



