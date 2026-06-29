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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.font.Font
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

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

fun getFontFamily(styleName: String, customFontFiles: List<File> = emptyList()): FontFamily {
    // Check custom fonts first
    val matchingFile = customFontFiles.firstOrNull {
        it.nameWithoutExtension.equals(styleName, ignoreCase = true)
    }
    if (matchingFile != null && matchingFile.exists()) {
        return FontFamily(Font(matchingFile))
    }
    return when (styleName.lowercase()) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }
}

@Composable
fun BibleScreen(vm: BibleViewModel = viewModel()) {
    val books = BIBLE_BOOKS.keys.toList()

    var selectedBook by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(vm.lastBook) }
    var showBookSelection by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var targetChapter by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(vm.lastChapter) }

    var fontSize by androidx.compose.runtime.saveable.rememberSaveable { mutableFloatStateOf(vm.fontSize) }
    var fontStyle by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(vm.fontStyle) }
    var showSettings by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showTranslationPicker by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    // SAF launcher for importing custom fonts
    val fontFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.importCustomFont(uri)
    }

    // Highlights panel — shown when any verse is selected
    val selectedVerses = androidx.compose.runtime.saveable.rememberSaveable(
        saver = androidx.compose.runtime.saveable.listSaver(
            save = { it.toList() },
            restore = { 
                val set = mutableStateSetOf<String>()
                set.addAll(it)
                set
            }
        )
    ) { mutableStateSetOf<String>() }

    val selectedHighlightColor by vm.selectedHighlightColor
    var showColorPickerRow by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showCustomColorPicker by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var isDeleteMode by remember { mutableStateOf(false) }

    val highlights = vm.highlights

    val uiState by vm.uiState.collectAsState()
    val activeTranslation by vm.activeTranslation.collectAsState()
    val listState = rememberLazyListState()
    val colorListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val presetColors = remember {
        listOf(
            Color(0xFF54D47A), // green
            Color(0xFFFFDD59), // yellow
            Color(0xFF48BEFF), // blue
            Color(0xFFFF6EB4), // pink
        )
    }

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
        vm.loadChapter(book, chapter, resetScroll = true)
    }

    LaunchedEffect(uiState) {
        if (uiState is BibleUiState.Success) {
            val verses = (uiState as BibleUiState.Success).verses
            val versesByChapter = verses.groupBy { it.chapter }

            // First-ever load: restore the persisted scroll index directly.
            // Subsequent chapter navigations: scroll to the chapter header.
            if (!hasInitialized) {
                val savedIndex = vm.lastScrollIndex
                if (savedIndex > 0) {
                    listState.scrollToItem(savedIndex)
                } else {
                    var targetIndex = 0
                    for ((chapter, chapterVerses) in versesByChapter) {
                        if (chapter == targetChapter) break
                        targetIndex += 1
                        targetIndex += chapterVerses.size
                        targetIndex += 1
                    }
                    listState.scrollToItem(targetIndex)
                }
            } else {
                // Navigated to a new chapter — scroll to its header
                var targetIndex = 0
                for ((chapter, chapterVerses) in versesByChapter) {
                    if (chapter == targetChapter) break
                    targetIndex += 1
                    targetIndex += chapterVerses.size
                    targetIndex += 1
                }
                listState.scrollToItem(targetIndex)
            }

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

    // Persist scroll position continuously while the user scrolls
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (hasInitialized) {
            vm.saveScrollIndex(listState.firstVisibleItemIndex)
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

                    // Reset color picker row when selection is cleared
                    LaunchedEffect(selectedVerses.size) {
                        if (selectedVerses.isEmpty()) {
                            showColorPickerRow = false
                            isDeleteMode = false
                        }
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
                                        fontFamily = getFontFamily(fontStyle, vm.customFonts),
                                        fontSize = (fontSize * 3.55f).sp,
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
                                        fontFamily = getFontFamily(fontStyle, vm.customFonts),
                                        fontSize = (fontSize * 1.1f).sp,
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
                                            fontFamily = getFontFamily(fontStyle, vm.customFonts),
                                            fontSize = (fontSize * 0.7f).sp,
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

                                val textLayout = remember { mutableStateOf<TextLayoutResult?>(null) }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            if (isSelected) selectedVerses.remove(verseKey)
                                            else selectedVerses.add(verseKey)
                                        }
                                        .padding(
                                            top = if (hasHeading) 16.dp else 0.dp,
                                            bottom = 22.dp,
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
                                                    fontFamily = getFontFamily(fontStyle, vm.customFonts),
                                                    fontSize = (fontSize * 0.65f).sp,
                                                    textDecoration = if (isSelected)
                                                        androidx.compose.ui.text.style.TextDecoration.Underline
                                                    else null
                                                )
                                            ) { append("$verseLabel  ") }
                                            withStyle(
                                                SpanStyle(
                                                    color = Color.White,
                                                    fontFamily = getFontFamily(fontStyle, vm.customFonts),
                                                    fontSize = fontSize.sp,
                                                    textDecoration = if (isSelected)
                                                        androidx.compose.ui.text.style.TextDecoration.Underline
                                                    else null
                                                )
                                            ) { append(verse.text.trim()) }
                                        },
                                        onTextLayout = { textLayout.value = it },
                                        lineHeight = (fontSize * 1.9).sp,
                                        modifier = Modifier.drawBehind {
                                            if (highlightColor != null) {
                                                textLayout.value?.let { layout ->
                                                    val hPad      = 5.dp.toPx()
                                                    val vPad      = 3.dp.toPx()
                                                    val textHeight = fontSize.sp.toPx()
                                                    val radius    = 4.dp.toPx()
                                                    for (i in 0 until layout.lineCount) {
                                                        val left    = layout.getLineLeft(i) - hPad
                                                        val right   = layout.getLineRight(i) + hPad
                                                        val lineMid = (layout.getLineTop(i) + layout.getLineBottom(i)) / 2f
                                                        val top     = lineMid - textHeight / 2f - vPad
                                                        val bottom  = lineMid + textHeight / 2f + vPad
                                                        drawRoundRect(
                                                            color = highlightColor.copy(alpha = 0.35f),
                                                            topLeft = Offset(left, top),
                                                            size = Size(right - left, bottom - top),
                                                            cornerRadius = CornerRadius(radius)
                                                        )
                                                    }
                                                }
                                            }
                                        }
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
                .height(160.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.4f to BackgroundDark.copy(alpha = 0.6f),
                            0.7f to BackgroundDark.copy(alpha = 0.92f),
                            1.0f to BackgroundDark
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
                if (verseNums.isEmpty()) {
                    "$selectedBook $targetChapter"
                } else {
                    val isAllConsecutive = verseNums.last() - verseNums.first() == verseNums.size - 1
                    val suffix = when {
                        verseNums.size == 1 -> ":${verseNums.first()}"
                        isAllConsecutive -> ":${verseNums.first()}-${verseNums.last()}"
                        verseNums.size == 2 -> ":${verseNums[0]}, ${verseNums[1]}"
                        else -> ":${verseNums.first()}..${verseNums.last()}"
                    }
                    "$selectedBook $targetChapter$suffix"
                }
            }

            // True when every selected verse already has a highlight
            val allSelectedAreHighlighted = selectedVerses.isNotEmpty() &&
                selectedVerses.all { highlights.containsKey(it) }

            // Use Box so Row 2 slides over Row 1 from the same position
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // ── Row 1: prev / book+chapter / next ─────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Color(0xFF1A1A1A), shape = RoundedCornerShape(32.dp))
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    // Prev
                    Surface(
                        onClick = {
                            if (targetChapter > 1) requestChapter(selectedBook, targetChapter - 1)
                        },
                        enabled = !hasSelection,
                        shape = CircleShape,
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.size(44.dp)
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
                        enabled = !hasSelection,
                        shape = androidx.compose.ui.graphics.RectangleShape,
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
                        enabled = !hasSelection,
                        shape = CircleShape,
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.size(44.dp)
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
                } // end Row 1
                } // end Row 1 container Box

                // ── Row 2: selection actions — slides up over Row 1 ───────────
                AnimatedVisibility(
                    visible = hasSelection,
                    enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                    exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(200))
                ) {
                    // Pastel preset colors + custom (defined at top level)

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        // Main Row 2 buttons
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .background(Color(0xFF111111), shape = RoundedCornerShape(32.dp))
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showColorPickerRow) {
                                    androidx.compose.foundation.lazy.LazyRow(
                                        state = colorListState,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(22.dp)),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        // Custom color circle (color wheel + plus)
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(
                                                        brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                                                            colors = listOf(
                                                                Color(0xFFFF6B6B), Color(0xFFFF9F43),
                                                                Color(0xFFFFDD59), Color(0xFF54D47A),
                                                                Color(0xFF48BEFF), Color(0xFFA55EEA),
                                                                Color(0xFFFF6EB4), Color(0xFFFF6B6B)
                                                            )
                                                        ),
                                                        shape = CircleShape
                                                    )
                                                    .clickable(
                                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                        indication = null
                                                    ) { showCustomColorPicker = true },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("+", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        // Preset color circles
                                        items(presetColors) { color ->
                                            val isChosen = selectedHighlightColor == color
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(color, CircleShape)
                                                    .then(
                                                        if (isChosen) Modifier.border(2.dp, Color.White, CircleShape)
                                                        else Modifier
                                                    )
                                                    .clickable(
                                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                        indication = null
                                                    ) { vm.selectHighlightColor(color) }
                                            )
                                        }

                                        // Separator line if there are custom colors
                                        if (vm.customColors.isNotEmpty()) {
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .width(2.dp)
                                                        .height(24.dp)
                                                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(1.dp))
                                                )
                                            }
                                        }

                                        // Custom color circles
                                        items(vm.customColors.toList()) { color ->
                                            val isChosen = selectedHighlightColor == color
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .background(color, CircleShape)
                                                    .then(
                                                        if (isChosen && !isDeleteMode) Modifier.border(2.dp, Color.White, CircleShape)
                                                        else Modifier
                                                    )
                                                    .pointerInput(color) {
                                                        detectTapGestures(
                                                            onLongPress = { isDeleteMode = true },
                                                            onTap = {
                                                                if (isDeleteMode) {
                                                                    vm.removeCustomColor(color)
                                                                    isDeleteMode = false
                                                                } else {
                                                                    vm.selectHighlightColor(color)
                                                                }
                                                            }
                                                        )
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isDeleteMode) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.ic_trash_lucide),
                                                            contentDescription = "Delete",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Highlight apply / eraser button (Pencil for color picker)
                                    val isEraserMode = allSelectedAreHighlighted && selectedVerses.all { highlights[it] == selectedHighlightColor }
                                    Surface(
                                        onClick = {
                                            if (isEraserMode) {
                                                selectedVerses.forEach { vm.removeHighlight(it) }
                                                showColorPickerRow = false
                                                isDeleteMode = false
                                            } else {
                                                selectedHighlightColor?.let {
                                                    vm.applyHighlight(selectedVerses.toSet(), it)
                                                    showColorPickerRow = false
                                                    isDeleteMode = false
                                                }
                                            }
                                        },
                                        enabled = isEraserMode || selectedHighlightColor != null,
                                        shape = CircleShape,
                                        color = Color(0xFF1A1A1A),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            val iconRes = if (isEraserMode) R.drawable.ic_eraser else R.drawable.ic_pencil_line
                                            Icon(
                                                painter = painterResource(iconRes),
                                                contentDescription = if (isEraserMode) "Remove Highlight" else "Apply Highlight",
                                                tint = if (isEraserMode || selectedHighlightColor != null) Color.White else Color.White.copy(alpha = 0.3f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                } else {
                                    // Verse range label
                                    Surface(
                                        onClick = { },
                                        shape = RoundedCornerShape(22.dp),
                                        color = Color(0xFF1A1A1A),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

                                    // Bookmark button (in normal mode) - Ready for new bookmark logic!
                                    Surface(
                                        onClick = {
                                            // TODO: Add bookmark logic here
                                        },
                                        shape = CircleShape,
                                        color = Color(0xFF1A1A1A),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_highlight),
                                                contentDescription = "Bookmark",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                if (!showColorPickerRow) {
                                    // Color picker toggle button
                                    Surface(
                                        onClick = { showColorPickerRow = true },
                                        shape = CircleShape,
                                        color = Color(0xFF1A1A1A),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            if (selectedHighlightColor == null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .background(
                                                            brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                                                                colors = listOf(
                                                                    Color(0xFFFF6B6B), Color(0xFFFF9F43),
                                                                    Color(0xFFFFDD59), Color(0xFF54D47A),
                                                                    Color(0xFF48BEFF), Color(0xFFA55EEA),
                                                                    Color(0xFFFF6EB4), Color(0xFFFF6B6B)
                                                                )
                                                            ),
                                                            shape = CircleShape
                                                        )
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(22.dp)
                                                        .background(selectedHighlightColor!!, CircleShape)
                                                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                                )
                                            }
                                        }
                                    }
                                }

                                // X — white icon (close / paintbrush)
                                Surface(
                                    onClick = {
                                        if (!showColorPickerRow) {
                                            selectedVerses.clear()
                                            showColorPickerRow = false
                                            isDeleteMode = false
                                        }
                                    },
                                    enabled = if (showColorPickerRow) selectedHighlightColor != null else true,
                                    shape = CircleShape,
                                    color = if (showColorPickerRow) Color(0xFF1A1A1A) else Color.White,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(if (showColorPickerRow) R.drawable.ic_paintbrush else R.drawable.ic_close),
                                            contentDescription = "Clear selection",
                                            tint = if (showColorPickerRow) {
                                                if (selectedHighlightColor != null) Color.White else Color.White.copy(alpha = 0.3f)
                                            } else Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
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
                onFontSizeChange = { 
                    fontSize = it
                    vm.saveFontSize(it)
                },
                currentFontStyle = fontStyle,
                onFontStyleChange = {
                    fontStyle = it
                    vm.saveFontStyle(it)
                },
                customFonts = vm.customFonts,
                onAddFont = { fontFileLauncher.launch(arrayOf("font/ttf", "font/otf", "*/*")) },
                onRemoveFont = { vm.removeCustomFont(it) },
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

        // ── Custom color picker popup ─────────────────────────────────────────
        AnimatedVisibility(
            visible = showCustomColorPicker,
            enter = slideInVertically { it } + fadeIn(tween(200)),
            exit = slideOutVertically { it } + fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            CustomColorPickerPanel(
                initialHex = vm.lastCustomHex,
                onHexChanged = { vm.saveLastCustomHex(it) },
                onColorSelected = { color ->
                    vm.selectHighlightColor(color)
                    showCustomColorPicker = false
                },
                onColorAdded = { color ->
                    vm.addCustomColor(color)
                    vm.selectHighlightColor(color)
                    showCustomColorPicker = false
                    coroutineScope.launch {
                        // wait a bit for the row to recompose
                        kotlinx.coroutines.delay(50)
                        colorListState.animateScrollToItem(presetColors.size + vm.customColors.size - 1)
                    }
                },
                onDismiss = { showCustomColorPicker = false }
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
    currentFontStyle: String,
    onFontStyleChange: (String) -> Unit,
    customFonts: List<File> = emptyList(),
    onAddFont: () -> Unit = {},
    onRemoveFont: (String) -> Unit = {},
    onClose: () -> Unit
) {
    var isDeleteMode by remember { mutableStateOf(false) }

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
                text = "Appearance",
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Preview",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = Color(0xFFAAAAAA),
                                fontWeight = FontWeight.Bold,
                                fontFamily = getFontFamily(currentFontStyle, customFonts),
                                fontSize = (currentFontSize * 0.65f).sp
                            )
                        ) { append("1  ") }
                        withStyle(
                            SpanStyle(
                                color = Color.White,
                                fontFamily = getFontFamily(currentFontStyle, customFonts),
                                fontSize = currentFontSize.sp
                            )
                        ) { append("Nang pasimula ay naroon na ang Salita, at ang Salita ay kasama ng Diyos, at ang Salita ay Diyos.") }
                    },
                    lineHeight = (currentFontSize * 1.9).sp,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val fontSizeLabel = when (currentFontSize.toInt()) {
                14 -> "Extra Small"
                16 -> "Small"
                18 -> "Normal"
                20 -> "Medium"
                22 -> "Large"
                24 -> "Extra Large"
                else -> "Custom"
            }

            @OptIn(ExperimentalMaterial3Api::class)
            Box(
                modifier = Modifier.fillMaxWidth().height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                // If thumb is 24.dp, its center rests exactly 12.dp from the edges
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    val stepsCount = 6
                    val trackWidth = size.width
                    val stepWidth = trackWidth / (stepsCount - 1)
                    val valuePerStep = (24f - 14f) / (stepsCount - 1)
                    
                    // Draw horizontal track
                    drawLine(
                        color = Color.White.copy(alpha = 0.2f),
                        start = Offset(0f, size.height / 2),
                        end = Offset(trackWidth, size.height / 2),
                        strokeWidth = 2.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    
                    val activeTrackWidth = ((currentFontSize - 14f) / (24f - 14f)) * trackWidth
                    drawLine(
                        color = Color.White,
                        start = Offset(0f, size.height / 2),
                        end = Offset(activeTrackWidth, size.height / 2),
                        strokeWidth = 2.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    
                    // Draw vertical ticks
                    for (i in 0 until stepsCount) {
                        val x = i * stepWidth
                        val tickValue = 14f + (i * valuePerStep)
                        val isActive = tickValue <= currentFontSize
                        
                        drawLine(
                            color = if (isActive) Color.White else Color.White.copy(alpha = 0.4f),
                            start = Offset(x, size.height / 2 - 7.dp.toPx()),
                            end = Offset(x, size.height / 2 + 7.dp.toPx()),
                            strokeWidth = 2.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
                Slider(
                    value = currentFontSize,
                    onValueChange = onFontSizeChange,
                    valueRange = 14f..24f,
                    steps = 4,
                    thumb = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.White, CircleShape)
                        )
                    },
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = fontSizeLabel,
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(20.dp))


            // Font Style header with "+ Add Fonts" button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Font Style",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    onClick = onAddFont,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp, Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_plus_lucide),
                            contentDescription = "Add Font",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            val builtInFontOptions = listOf("Sans-Serif", "Serif", "Monospace", "Cursive")
            val customFontNames = customFonts.map { it.nameWithoutExtension }
            val hasCustomFonts = customFontNames.isNotEmpty()

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Built-in fonts
                items(builtInFontOptions) { fontName ->
                    val isSelected = fontName == currentFontStyle
                    Surface(
                        onClick = { onFontStyleChange(fontName) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color.White else Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = fontName,
                            fontFamily = getFontFamily(fontName),
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
                // Divider between built-in and custom
                if (hasCustomFonts) {
                    item {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(48.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )
                    }
                    // Custom fonts
                    items(customFontNames) { fontName ->
                        val isSelected = fontName == currentFontStyle
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected && !isDeleteMode) Color.White else Color.White.copy(alpha = 0.2f),
                                    RoundedCornerShape(8.dp)
                                )
                                .pointerInput(fontName) {
                                    detectTapGestures(
                                        onLongPress = { isDeleteMode = true },
                                        onTap = {
                                            if (isDeleteMode) {
                                                onRemoveFont(fontName)
                                                isDeleteMode = false
                                            } else {
                                                onFontStyleChange(fontName)
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fontName,
                                fontFamily = getFontFamily(fontName, customFonts),
                                color = Color.White,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                            if (isDeleteMode) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_trash_lucide),
                                        contentDescription = "Delete Font",
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

// ── Custom Color Picker Panel ─────────────────────────────────────────────────

@Composable
fun CustomColorPickerPanel(
    initialHex: String,
    onHexChanged: (String) -> Unit,
    onColorSelected: (Color) -> Unit,
    onColorAdded: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val initialColor = remember(initialHex) { 
        try {
            android.graphics.Color.parseColor("#$initialHex")
        } catch (e: Exception) {
            android.graphics.Color.RED
        }
    }
    
    val initialHsv = remember(initialColor) {
        val hsvOut = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor, hsvOut)
        hsvOut
    }

    var hue by remember { mutableStateOf(initialHsv[0]) }
    var saturation by remember { mutableStateOf(initialHsv[1]) }
    var value by remember { mutableStateOf(initialHsv[2]) }
    var hexInput by remember { mutableStateOf(initialHex) }
    var hexError by remember { mutableStateOf(false) }

    fun hsvToColor(h: Float, s: Float, v: Float): Color {
        val hsv = floatArrayOf(h, s, v)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }

    val currentColor = hsvToColor(hue, saturation, value)

    fun syncHex() {
        val c = hsvToColor(hue, saturation, value)
        val newHex = String.format(
            "%02X%02X%02X",
            kotlin.math.round(c.red * 255f).toInt(),
            kotlin.math.round(c.green * 255f).toInt(),
            kotlin.math.round(c.blue * 255f).toInt()
        )
        hexInput = newHex
        onHexChanged(newHex)
        hexError = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // ── Top row: color circle | hex input (with # inside) | ✕ | ✓ ──────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Color preview circle beside hex input
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(currentColor, CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    .clickable { onColorAdded(currentColor) },
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            // Hex input with # prefix inside the box
            androidx.compose.foundation.text.BasicTextField(
                value = hexInput,
                onValueChange = { raw ->
                    val clean = raw.uppercase().filter { it.isLetterOrDigit() }.take(6)
                    hexInput = clean
                    onHexChanged(clean)
                    if (clean.length == 6) {
                        try {
                            val parsed = android.graphics.Color.parseColor("#$clean")
                            val hsvOut = FloatArray(3)
                            android.graphics.Color.colorToHSV(parsed, hsvOut)
                            if (hsvOut[1] > 0f) {
                                hue = hsvOut[0]
                            }
                            saturation = hsvOut[1]
                            value = hsvOut[2]
                            hexError = false
                        } catch (e: Exception) { hexError = true }
                    }
                },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = if (hexError) Color.Red else Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier
                    .weight(1f)
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                decorationBox = { inner ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        if (hexInput.isEmpty()) {
                            Text("RRGGBB", color = Color.White.copy(alpha = 0.3f), fontSize = 14.sp)
                        }
                        inner()
                    }
                }
            )

            // ✕ — cancel
            Surface(
                onClick = onDismiss,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_x_lucide),
                        contentDescription = "Cancel",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ✓ — select
            Surface(
                onClick = { onColorSelected(currentColor) },
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_lucide),
                        contentDescription = "Select",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Picker row: hue slider (left) | 2D picker (center) ──────────────
        val pickerHeight = 110.dp
        val pickerHeightPx = with(LocalDensity.current) { pickerHeight.toPx() }
        val sliderWidth = 36.dp // matches color preview circle width

        Row(
            modifier = Modifier.fillMaxWidth().height(pickerHeight),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: vertical hue slider aligned with preview circle
            Box(
                modifier = Modifier
                    .width(sliderWidth)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            hue = (offset.y / size.height * 360f).coerceIn(0f, 360f)
                            syncHex()
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            hue = (change.position.y / size.height * 360f).coerceIn(0f, 360f)
                            syncHex()
                        }
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .fillMaxHeight()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                (0..12).map { hsvToColor(it * 30f, 1f, 1f) }
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                )
                val thumbY = with(LocalDensity.current) {
                    (hue / 360f * pickerHeightPx).toDp()
                }
                Box(
                    modifier = Modifier
                        .offset(y = thumbY - 2.dp)
                        .size(width = 24.dp, height = 4.dp)
                        .background(Color.White, RoundedCornerShape(2.dp))
                        .border(0.5.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                )
            }

            // Center: 2D saturation/lightness picker
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(Color.White, hsvToColor(hue, 1f, 1f))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .then(
                        Modifier.background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            saturation = (offset.x / size.width).coerceIn(0f, 1f)
                            value  = (1f - offset.y / size.height).coerceIn(0f, 1f)
                            syncHex()
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                            value  = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                            syncHex()
                        }
                    }
            ) {
                val pinX = maxWidth * saturation
                val pinY = maxHeight * (1f - value)

                Box(
                    modifier = Modifier
                        .offset(x = pinX - 8.dp, y = pinY - 8.dp)
                        .size(16.dp)
                        .border(2.dp, Color.White, CircleShape)
                        .background(currentColor, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
