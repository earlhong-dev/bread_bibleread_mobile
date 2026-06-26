package com.bibleread.bread.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bibleread.bread.R
import com.bibleread.bread.data.TranslationManager
import com.bibleread.bread.ui.theme.BackgroundDark
import com.bibleread.bread.viewmodel.BibleUiState
import com.bibleread.bread.viewmodel.BibleViewModel
import kotlinx.coroutines.delay

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

@Composable
fun BibleScreen(vm: BibleViewModel = viewModel()) {
    val books = BIBLE_BOOKS.keys.toList()

    var selectedBook by remember { mutableStateOf("Genesis") }
    var showBookSelection by remember { mutableStateOf(false) }
    var targetChapter by remember { mutableIntStateOf(1) }

    var fontSize by remember { mutableFloatStateOf(17f) }
    var showSettings by remember { mutableStateOf(false) }
    var showTranslationPicker by remember { mutableStateOf(false) }

    // Highlights panel — shown when any verse is selected
    var showHighlightsPanel by remember { mutableStateOf(false) }
    val selectedVerses = remember { mutableStateSetOf<String>() }

    val highlights = vm.highlights

    val uiState by vm.uiState.collectAsState()
    val activeTranslation by vm.activeTranslation.collectAsState()
    val listState = rememberLazyListState()

    // ── Animation state ──────────────────────────────────────────────────────
    // headerReady: flips true once on first successful load, never resets
    var headerReady by remember { mutableStateOf(false) }
    var hasInitialized by remember { mutableStateOf(false) }

    // bottomBarReady: same — slides up once on first load
    var bottomBarReady by remember { mutableStateOf(false) }

    // contentVisible: resets to false every time a chapter change is requested,
    // flips back to true when the new data arrives and is ready to render
    var contentVisible by remember { mutableStateOf(false) }
    // ─────────────────────────────────────────────────────────────────────────

    // When chapter/book buttons are tapped, hide content immediately
    fun requestChapter(book: String, chapter: Int) {
        contentVisible = false
        selectedBook = book
        targetChapter = chapter
        vm.loadChapter(book, chapter)
    }

    LaunchedEffect(uiState) {
        if (uiState is BibleUiState.Success) {
            val verses = (uiState as BibleUiState.Success).verses
            val versesByChapter = verses.groupBy { it.chapter }

            // Scroll to the right chapter
            var targetIndex = 0
            for ((chapter, chapterVerses) in versesByChapter) {
                if (chapter == targetChapter) break
                targetIndex += 1
                targetIndex += chapterVerses.size
                targetIndex += 1
            }
            listState.scrollToItem(targetIndex)

            // First-ever load: bring in header and bottom bar
            if (!hasInitialized) {
                delay(200) // let the screen settle before animating in
                headerReady = true
                bottomBarReady = true
                hasInitialized = true
            }

            // Every load: fade in content
            delay(80)
            contentVisible = true
        }
    }

    // ── Animated values ───────────────────────────────────────────────────────
    val headerAlpha by animateFloatAsState(
        targetValue = if (headerReady) 1f else 0f,
        animationSpec = tween(300),
        label = "headerAlpha"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = if (contentVisible) tween(250) else snap(), // instant hide, animated show
        label = "contentAlpha"
    )
    // ─────────────────────────────────────────────────────────────────────────

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
        ) {

            // ── Header ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp)
                    .graphicsLayer { alpha = headerAlpha },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedBook,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 56.dp),
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                // Version button — left
                Surface(
                    onClick = { showTranslationPicker = true },
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 14.dp)
                ) {
                    Text(
                        text = TranslationManager.displayName(activeTranslation),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    )
                }

                // Settings icon — right
                IconButton(
                    onClick = { showSettings = true },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_settings2),
                        contentDescription = "Settings",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            when (val state = uiState) {
                is BibleUiState.Idle, is BibleUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize())
                }
                is BibleUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            (state as BibleUiState.Error).message,
                            color = Color.Red,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                is BibleUiState.Success -> {
                    val versesByChapter = state.verses.groupBy { it.chapter }

                    // Auto-show/hide highlights panel with selection
                    LaunchedEffect(selectedVerses.size) {
                        if (selectedVerses.isEmpty()) showHighlightsPanel = false
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 36.dp)
                            .graphicsLayer { alpha = contentAlpha },
                        contentPadding = PaddingValues(top = 0.dp, bottom = 80.dp)
                    ) {
                        versesByChapter.forEach { (chapter, verses) ->
                            item(key = "$selectedBook-$chapter-header") {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chapter.toString(),
                                        color = Color.White.copy(alpha = 0.45f),
                                        fontSize = 64.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 2.sp
                                    )
                                }
                            }
                            items(
                                verses,
                                key = { "${it.book}-${it.chapter}-${it.verse}" }
                            ) { verse ->
                                if (!verse.heading.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = verse.heading,
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 4.dp)
                                    )
                                    if (!verse.subheading.isNullOrBlank()) {
                                        Text(
                                            text = verse.subheading,
                                            color = Color.Gray,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Normal,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            lineHeight = 18.sp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                val verseKey      = "${verse.book}-${verse.chapter}-${verse.verse}"
                                val isSelected    = verseKey in selectedVerses
                                val highlightColor = highlights[verseKey]
                                val verseLabel    = verse.display ?: verse.verse.toString()
                                val hasHeading    = !verse.heading.isNullOrBlank()

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (highlightColor != null)
                                                Modifier.background(
                                                    highlightColor.copy(alpha = 0.25f),
                                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                                                )
                                            else Modifier
                                        )
                                        .clickable(
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            if (isSelected) selectedVerses.remove(verseKey)
                                            else selectedVerses.add(verseKey)
                                        }
                                        .padding(
                                            top = if (hasHeading) 16.dp else 0.dp,
                                            bottom = 16.dp,
                                            start = 2.dp,
                                            end = 2.dp
                                        )
                                ) {
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(
                                                SpanStyle(
                                                    color = Color(0xFFAAAAAA),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    textDecoration = if (isSelected)
                                                        androidx.compose.ui.text.style.TextDecoration.Underline
                                                    else null
                                                )
                                            ) { append("$verseLabel  ") }
                                            withStyle(
                                                SpanStyle(
                                                    color = Color.White,
                                                    fontSize = fontSize.sp,
                                                    textDecoration = if (isSelected)
                                                        androidx.compose.ui.text.style.TextDecoration.Underline
                                                    else null
                                                )
                                            ) { append(verse.text.trim()) }
                                        },
                                        lineHeight = (fontSize * 1.5).sp
                                    )
                                }
                            }
                            item { Spacer(modifier = Modifier.height(24.dp)) }
                        }
                    }
                }
            }
        }

        // ── Bottom fade gradient ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            BackgroundDark.copy(alpha = 0.7f),
                            BackgroundDark
                        )
                    )
                )
        )

        // ── Bottom bar ────────────────────────────────────────────────────────
        // Row 1 (prev/book/next) + Row 2 (selection actions) stacked in a Column
        AnimatedVisibility(
            visible = bottomBarReady,
            enter = slideInVertically(animationSpec = tween(400)) { it } + fadeIn(tween(400)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val hasSelection = selectedVerses.isNotEmpty()

            // Build verse range label: "Genesis 1:1" or "Genesis 1:1-4"
            val verseRangeLabel = run {
                val verseNums = selectedVerses
                    .filter { it.startsWith("$selectedBook-$targetChapter-") }
                    .mapNotNull { it.substringAfterLast("-").toIntOrNull() }
                    .sorted()
                if (verseNums.isEmpty()) "$selectedBook $targetChapter"
                else if (verseNums.size == 1) "$selectedBook $targetChapter:${verseNums.first()}"
                else "$selectedBook $targetChapter:${verseNums.first()}-${verseNums.last()}"
            }

            // Use Box so Row 2 slides over Row 1 from the same position
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // ── Row 1: prev / book+chapter / next ─────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp), // keeps row 1 at original 36dp margin
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prev
                    Surface(
                        onClick = {
                            if (targetChapter > 1) requestChapter(selectedBook, targetChapter - 1)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.size(width = 44.dp, height = 44.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_left),
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Book + chapter selector
                    Surface(
                        onClick = { showBookSelection = true },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "$selectedBook $targetChapter",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }

                    // Next
                    Surface(
                        onClick = {
                            val maxChapter = BIBLE_BOOKS[selectedBook] ?: 1
                            if (targetChapter < maxChapter) requestChapter(selectedBook, targetChapter + 1)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.size(width = 44.dp, height = 44.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_chevron_right),
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // ── Row 2: selection actions — slides up over Row 1 ───────────
                AnimatedVisibility(
                    visible = hasSelection,
                    enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                    exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(200))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFF111111),
                                shape = RoundedCornerShape(36.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Verse range label — same radius as circles (22dp)
                            Surface(
                                onClick = { },
                                shape = RoundedCornerShape(22.dp),
                                color = Color(0xFF1A1A1A),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = verseRangeLabel,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 10.dp)
                                    )
                                }
                            }

                            // Highlight button — circle
                            Surface(
                                onClick = { showHighlightsPanel = true },
                                shape = CircleShape,
                                color = Color(0xFF1A1A1A),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_highlight),
                                        contentDescription = "Highlight",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Draw button — circle
                            Surface(
                                onClick = { },
                                shape = CircleShape,
                                color = Color(0xFF1A1A1A),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_draw),
                                        contentDescription = "Draw",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // X — circle
                            Surface(
                                onClick = {
                                    selectedVerses.clear()
                                    showHighlightsPanel = false
                                },
                                shape = CircleShape,
                                color = Color(0xFF1A1A1A),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_close),
                                        contentDescription = "Clear selection",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Settings overlay ──────────────────────────────────────────────────
        if (showSettings) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { /* consume all touches behind overlay */ }
            )
        }
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

        // ── Translation picker overlay ────────────────────────────────────────
        if (showTranslationPicker) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { }
            )
        }
        AnimatedVisibility(
            visible = showTranslationPicker,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            TranslationPickerOverlay(
                translations = vm.availableTranslations,
                activeTranslation = activeTranslation,
                onSelected = { code ->
                    showTranslationPicker = false
                    vm.switchTranslation(code)
                },
                onClose = { showTranslationPicker = false }
            )
        }

        // ── Book selection overlay ────────────────────────────────────────────
        if (showBookSelection) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { }
            )
        }
        AnimatedVisibility(
            visible = showBookSelection,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            BookSelectionOverlay(
                books = books,
                onBookSelected = { book, chapter ->
                    showBookSelection = false
                    requestChapter(book, chapter)
                },
                onClose = { showBookSelection = false }
            )
        }

        // ── Highlights panel ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showHighlightsPanel,
            enter = slideInVertically { it } + fadeIn(tween(200)),
            exit = slideOutVertically { it } + fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            HighlightsPanel(
                onColorSelected = { color ->
                    vm.applyHighlight(selectedVerses.toSet(), color)
                    selectedVerses.clear()
                    showHighlightsPanel = false
                },
                onDismiss = {
                    selectedVerses.clear()
                    showHighlightsPanel = false
                }
            )
        }
    }
}

// ── Book Selection Overlay ────────────────────────────────────────────────────

private val OLD_TESTAMENT = listOf(
    "Genesis","Exodo","Levitico","Mga Bilang","Deuteronomio","Josue","Mga Hukom","Ruth",
    "1 Samuel","2 Samuel","1 Mga Hari","2 Mga Hari","1 Mga Cronica","2 Mga Cronica",
    "Ezra","Nehemias","Ester","Job","Mga Awit","Mga Kawikaan","Ang Mangangaral",
    "Ang Awit ni Solomon","Isaias","Jeremias","Mga Panaghoy","Ezekiel","Daniel",
    "Hosea","Joel","Amos","Obadias","Jonas","Mikas","Nahum","Habakuk","Zefanias",
    "Hagai","Zacarias","Malakias"
)

private val NEW_TESTAMENT = listOf(
    "Mateo","Marcos","Lucas","Juan","Mga Gawa","Mga Taga-Roma",
    "1 Mga Taga-Corinto","2 Mga Taga-Corinto","Mga Taga-Galacia","Mga Taga-Efeso",
    "Mga Taga-Filipos","Mga Taga-Colosas","1 Mga Taga-Tesalonica","2 Mga Taga-Tesalonica",
    "1 Timoteo","2 Timoteo","Tito","Filemon","Mga Hebreo","Santiago",
    "1 Pedro","2 Pedro","1 Juan","2 Juan","3 Juan","Judas","Pahayag"
)

@Composable
fun BookSelectionOverlay(
    books: List<String>,
    onBookSelected: (String, Int) -> Unit,
    onClose: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedBook by remember { mutableStateOf<String?>(null) }

    val query = searchQuery.trim().lowercase()

    // When searching: flat ranked list (starts-with first, then word-starts-with, then contains)
    // When empty: keep OT/NT split
    data class RankedBook(val name: String, val rank: Int) // rank 0=best

    fun rankBook(name: String, q: String): Int? {
        val n = name.lowercase()
        return when {
            n.startsWith(q) -> 0
            n.split(" ").any { it.startsWith(q) } -> 1
            n.contains(q) -> 2
            else -> null
        }
    }

    val isSearching = query.isNotEmpty()
    val rankedResults = if (isSearching) {
        (OLD_TESTAMENT + NEW_TESTAMENT)
            .mapNotNull { book -> rankBook(book, query)?.let { RankedBook(book, it) } }
            .sortedWith(compareBy({ it.rank }, { it.name }))
            .map { it.name }
    } else emptyList()

    val filteredOT = if (!isSearching) OLD_TESTAMENT
                     else emptyList()
    val filteredNT = if (!isSearching) NEW_TESTAMENT
                     else emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Scripture",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .size(32.dp)
            ) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    contentDescription = "Close",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ── Search ────────────────────────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            "Search book...",
                            color = Color.Black.copy(alpha = 0.35f),
                            fontSize = 15.sp,
                            lineHeight = 15.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            expandedBook = null
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = Color.Black,
                            fontSize = 15.sp,
                            lineHeight = 15.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // Always reserve space; invisible when empty
                IconButton(
                    onClick = { searchQuery = "" },
                    modifier = Modifier.size(20.dp),
                    enabled = searchQuery.isNotEmpty()
                ) {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        contentDescription = "Clear",
                        tint = if (searchQuery.isNotEmpty())
                            Color.Black.copy(alpha = 0.5f)
                        else
                            Color.Transparent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // ── Book list ─────────────────────────────────────────────────────────
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (isSearching) {
                // Flat ranked results — no testament headers
                items(rankedResults) { book ->
                    BookRow(
                        book = book,
                        isExpanded = expandedBook == book,
                        onToggle = { expandedBook = if (expandedBook == book) null else book },
                        onChapterSelected = { chapter -> onBookSelected(book, chapter) }
                    )
                }
            } else {
                if (filteredOT.isNotEmpty()) {
                    item { TestamentLabel("Old Testament") }
                    items(filteredOT) { book ->
                        BookRow(
                            book = book,
                            isExpanded = expandedBook == book,
                            onToggle = { expandedBook = if (expandedBook == book) null else book },
                            onChapterSelected = { chapter -> onBookSelected(book, chapter) }
                        )
                    }
                }
                if (filteredNT.isNotEmpty()) {
                    item { TestamentLabel("New Testament") }
                    items(filteredNT) { book ->
                        BookRow(
                            book = book,
                            isExpanded = expandedBook == book,
                            onToggle = { expandedBook = if (expandedBook == book) null else book },
                            onChapterSelected = { chapter -> onBookSelected(book, chapter) }
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun TestamentLabel(label: String) {
    Text(
        text = label,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookRow(
    book: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onChapterSelected: (Int) -> Unit
) {
    val chapters = BIBLE_BOOKS[book] ?: 1

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = book,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
        }

        // Chapter grid — smooth expand/collapse
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(200)) + expandVertically(tween(250)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
        ) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (1..chapters).forEach { chapter ->
                    Surface(
                        onClick = { onChapterSelected(chapter) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = chapter.toString(),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.06f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

// ── Appearance Settings Overlay ───────────────────────────────────────────────

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
                Icon(painterResource(R.drawable.ic_close), contentDescription = "Close", tint = Color.White)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Font Size",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
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

            Text(
                text = "Font Style",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))

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

// ── Translation Picker Overlay ────────────────────────────────────────────────

@Composable
fun TranslationPickerOverlay(
    translations: List<String>,
    activeTranslation: String,
    onSelected: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Bible Translation",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .size(32.dp)
            ) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    contentDescription = "Close",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (translations.isEmpty()) {
            // No pre-built translations yet — only one is available (currently parsing)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No additional translations available.\nAdd .db files to assets/translations/.",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(translations) { code ->
                    val isActive = code == activeTranslation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(code) }
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = TranslationManager.displayName(code),
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = code,
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 12.sp
                            )
                        }
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = Color.White,
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                        }
                    }
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.07f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

// ── Highlights Panel ─────────────────────────────────────────────────────────

private val HIGHLIGHT_COLORS = listOf(
    Color(0xFFFFEB3B), // Yellow
    Color(0xFF4CAF50), // Green
    Color(0xFF2196F3), // Blue
    Color(0xFFFF9800), // Orange
    Color(0xFFE91E63), // Pink
    Color(0xFF9C27B0), // Purple
)

@Composable
fun HighlightsPanel(
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf<Color?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFF1A1A1A),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        // Handle bar
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Highlight",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Color circles row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HIGHLIGHT_COLORS.forEach { color ->
                val isChosen = selectedColor == color
                Box(
                    modifier = Modifier
                        .size(if (isChosen) 40.dp else 34.dp)
                        .background(color, CircleShape)
                        .then(
                            if (isChosen) Modifier.border(
                                width = 2.5.dp,
                                color = Color.White,
                                shape = CircleShape
                            ) else Modifier
                        )
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { selectedColor = color }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancel
            Surface(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                }
            }

            // Apply checkmark
            Surface(
                onClick = {
                    selectedColor?.let { onColorSelected(it) }
                },
                shape = RoundedCornerShape(12.dp),
                color = if (selectedColor != null) Color.White else Color.White.copy(alpha = 0.15f),
                modifier = Modifier.weight(1f).height(44.dp),
                enabled = selectedColor != null
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Apply",
                        color = if (selectedColor != null) Color.Black else Color.White.copy(alpha = 0.3f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
