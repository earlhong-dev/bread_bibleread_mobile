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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.lazy.layout.LazyLayoutItemProvider
import androidx.compose.ui.util.lerp
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import kotlin.math.roundToInt
import com.bibleread.bread.R
import com.bibleread.bread.data.TranslationManager
import com.bibleread.bread.viewmodel.BibleUiState
import com.bibleread.bread.viewmodel.BibleViewModel
import androidx.compose.ui.text.font.Font
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.toColorInt
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
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
fun BibleScreen(
    vm: BibleViewModel = viewModel(),
    onOpenBookSelection: ((String, Int) -> Unit) -> Unit = {},
    onOpenAppearance: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    var selectedBook by rememberSaveable { mutableStateOf(vm.lastBook) }
    var targetChapter by rememberSaveable { mutableIntStateOf(vm.lastChapter) }

    val fontSize = vm.fontSize
    val fontStyle = vm.fontStyle
    var showTranslationPicker by rememberSaveable { mutableStateOf(false) }
    var showSelectedVersesWindow by rememberSaveable { mutableStateOf(false) }
    val selectedVerses = rememberSaveable(
        saver = listSaver(
            save = { it.toList() },
            restore = { 
                val set = mutableStateSetOf<String>()
                set.addAll(it)
                set
            }
        )
    ) { mutableStateSetOf<String>() }

    var showColorPickerRow by rememberSaveable { mutableStateOf(false) }
    var showCustomColorPicker by rememberSaveable { mutableStateOf(false) }

    val highlights = vm.highlights

    val uiState by vm.uiState.collectAsState()
    val activeTranslation by vm.activeTranslation.collectAsState()
    val listState = rememberLazyListState()
    val colorListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

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

    fun clearSelectedVerses(verseKeys: Set<String>) {
        selectedVerses.removeAll(verseKeys)
        if (selectedVerses.isEmpty()) {
            showColorPickerRow = false
            showSelectedVersesWindow = false
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is BibleUiState.Success) {
            val verses = (uiState as BibleUiState.Success).verses
            val versesByChapter = verses.groupBy { it.chapter }

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
                delay(90) // let the screen settle before animating in
                headerReady = true
                hasInitialized = true
            }

            // Every load: fade in content
            delay(35)
            contentVisible = true
        }
    }

    // ── Animated values ───────────────────────────────────────────────────────
    val headerAlpha by animateFloatAsState(
        targetValue = if (headerReady) 1f else 0f,
        animationSpec = tween(220),
        label = "headerAlpha"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = if (contentVisible) tween(220) else snap(), // instant hide, animated show
        label = "contentAlpha"
    )
    // ─────────────────────────────────────────────────────────────────────────

    val hasSelection = selectedVerses.isNotEmpty()

    val swipeDensity = LocalDensity.current
    val swipeThresholdPx = with(swipeDensity) { 60.dp.toPx() }
    var swipeAccumulator by remember { mutableFloatStateOf(0f) }

    val currentFontFamily = remember(fontStyle, vm.customFonts) {
        getFontFamily(fontStyle, vm.customFonts)
    }

    val showChapterLabel by remember(listState, contentVisible) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 && contentVisible
        }
    }

    var displayedChapterLabel by remember { mutableIntStateOf(targetChapter) }
    LaunchedEffect(showChapterLabel, targetChapter, contentVisible) {
        if (showChapterLabel) {
            displayedChapterLabel = targetChapter
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            // ── Header ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 18.dp)
                    .zIndex(1f)
                    .background(MaterialTheme.colorScheme.background)
                    .graphicsLayer { alpha = headerAlpha },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedBook,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontFamily = currentFontFamily,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        fontSize = 20.sp,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!hasSelection) onOpenBookSelection { book, chapter ->
                                requestChapter(book, chapter)
                            }
                        }
                    )

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showChapterLabel,
                        enter = slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(160)),
                        exit = slideOutVertically(targetOffsetY = { it / 4 }, animationSpec = tween(120)),
                        modifier = Modifier.align(Alignment.Center).offset(y = 20.dp)
                    ) {
                        Text(
                            text = "Chapter $displayedChapterLabel",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontFamily = currentFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Settings icon removed — moved to BookSelectionOverlay (Bible tab)

                // Back button — left
                IconButton(
                    onClick = { onBack() },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 14.dp)
                        .size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_chevron_left),
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }


            // ── Content area with sticky overlay ──────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {

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
                            showSelectedVersesWindow = false
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 36.dp)
                            .graphicsLayer { alpha = contentAlpha }
                            .pointerInput(hasSelection) {
                                if (!hasSelection) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { swipeAccumulator = 0f },
                                        onDragEnd = {
                                            if (swipeAccumulator < -swipeThresholdPx) {
                                                val maxChapter = BIBLE_BOOKS[selectedBook] ?: 1
                                                if (targetChapter < maxChapter)
                                                    requestChapter(selectedBook, targetChapter + 1)
                                            } else if (swipeAccumulator > swipeThresholdPx) {
                                                if (targetChapter > 1)
                                                    requestChapter(selectedBook, targetChapter - 1)
                                            }
                                            swipeAccumulator = 0f
                                        },
                                        onDragCancel = { swipeAccumulator = 0f },
                                        onHorizontalDrag = { _, dragAmount ->
                                            swipeAccumulator += dragAmount
                                        }
                                    )
                                }
                            },
                        contentPadding = PaddingValues(top = 0.dp, bottom = 80.dp)
                    ) {
                        versesByChapter.forEach { (chapter, verses) ->
                            item(key = "$selectedBook-$chapter-header") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 0.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chapter.toString(),
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                                        fontFamily = currentFontFamily,
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
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontFamily = currentFontFamily,
                                        fontSize = (fontSize * 1.1f).sp,
                                        fontWeight = FontWeight.Bold,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        lineHeight = (fontSize * 1.5f).sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 4.dp)
                                    )
                                    if (!verse.subheading.isNullOrBlank()) {
                                        Text(
                                            text = verse.subheading,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            fontFamily = currentFontFamily,
                                            fontSize = (fontSize * 0.7f).sp,
                                            fontWeight = FontWeight.Normal,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            lineHeight = (fontSize * 1.1f).sp,
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
                                            if (isSelected) {
                                                clearSelectedVerses(setOf(verseKey))
                                            } else {
                                                selectedVerses.add(verseKey)
                                                showColorPickerRow = true
                                            }
                                        }
                                        .padding(
                                            top = if (hasHeading) 16.dp else 0.dp,
                                            bottom = 22.dp,
                                            start = 2.dp,
                                            end = 2.dp
                                        )
                                ) {
                                    val normalTextColor = MaterialTheme.colorScheme.onBackground
                                    val annotatedText = remember(verse, isSelected, fontSize, currentFontFamily, normalTextColor) {
                                        buildAnnotatedString {
                                            withStyle(
                                                SpanStyle(
                                                    color = normalTextColor.copy(alpha = 0.5f),
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = currentFontFamily,
                                                    fontSize = (fontSize * 0.65f).sp,
                                                    textDecoration = if (isSelected)
                                                        androidx.compose.ui.text.style.TextDecoration.Underline
                                                    else null
                                                )
                                            ) { append("$verseLabel  ") }
                                            withStyle(
                                                SpanStyle(
                                                    color = normalTextColor,
                                                    fontFamily = currentFontFamily,
                                                    fontSize = fontSize.sp,
                                                    textDecoration = if (isSelected)
                                                        androidx.compose.ui.text.style.TextDecoration.Underline
                                                    else null
                                                )
                                            ) { append(verse.text.trim()) }
                                        }
                                    }

                                    Text(
                                        text = annotatedText,
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

        // ── Compact color picker row (appears when verses selected) ───────────
        AnimatedVisibility(
            visible = showColorPickerRow,
            enter = slideInVertically { it / 2 } + fadeIn(tween(180)),
            exit = slideOutVertically { it / 2 } + fadeOut(tween(140)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        state = colorListState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(presetColors) { c ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(c, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f), CircleShape)
                                    .clickable {
                                        val verseKeys = selectedVerses.toSet()
                                        vm.applyHighlight(verseKeys, c)
                                        clearSelectedVerses(verseKeys)
                                    }
                            )
                        }
                        items(vm.customColors) { c ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(c, CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f), CircleShape)
                                    .clickable {
                                        val verseKeys = selectedVerses.toSet()
                                        vm.applyHighlight(verseKeys, c)
                                        clearSelectedVerses(verseKeys)
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    TextButton(onClick = {
                        val verseKeys = selectedVerses.toSet()
                        verseKeys.forEach { vm.removeHighlight(it) }
                        clearSelectedVerses(verseKeys)
                    }) {
                        Text("Remove", color = MaterialTheme.colorScheme.onBackground)
                    }

                    TextButton(onClick = { showCustomColorPicker = true }) {
                        Text("Custom", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
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
                    val verseKeys = selectedVerses.toSet()
                    vm.applyHighlight(verseKeys, color)
                    vm.selectHighlightColor(color)
                    clearSelectedVerses(verseKeys)
                    showCustomColorPicker = false
                },
                onColorAdded = { color ->
                    val verseKeys = selectedVerses.toSet()
                    vm.addCustomColor(color)
                    vm.selectHighlightColor(color)
                    vm.applyHighlight(verseKeys, color)
                    clearSelectedVerses(verseKeys)
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

        // ── Selected verses overlay ───────────────────────────────────────────
        AnimatedVisibility(
            visible = showSelectedVersesWindow,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val selectedVerseEntities = remember(selectedVerses.size, uiState) {
                if (uiState is BibleUiState.Success) {
                    val currentVerses = (uiState as BibleUiState.Success).verses
                    currentVerses.filter { "${it.book}-${it.chapter}-${it.verse}" in selectedVerses }
                        .sortedBy { it.verse }
                } else {
                    emptyList()
                }
            }

            // Build full verse range label using full book name
            val fullVerseRangeLabel = remember(selectedVerses.size) {
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

            val context = LocalContext.current
            SelectedVersesOverlay(
                headerText = fullVerseRangeLabel,
                selectedVerses = selectedVerseEntities,
                fontStyle = fontStyle,
                fontSize = fontSize,
                customFonts = vm.customFonts,
                onClose = { showSelectedVersesWindow = false },
                onShare = {
                    val shareText = buildString {
                        append(fullVerseRangeLabel)
                        append("\n\n")
                        selectedVerseEntities.forEach { v ->
                            val lbl = v.display ?: v.verse.toString()
                            append("$lbl  ${v.text.trim()}\n")
                        }
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText.trim())
                    }
                    context.startActivity(Intent.createChooser(intent, null))
                },
                onCopy = {
                    val copyText = buildString {
                        append(fullVerseRangeLabel)
                        append("\n\n")
                        selectedVerseEntities.forEach { v ->
                            val lbl = v.display ?: v.verse.toString()
                            append("$lbl  ${v.text.trim()}\n")
                        }
                    }
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as android.content.ClipboardManager
                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText("verse", copyText.trim())
                    )
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

// Genre groupings
private val GENRE_LAW        = listOf("Genesis","Exodo","Levitico","Mga Bilang","Deuteronomio")
private val GENRE_HISTORY    = listOf("Josue","Mga Hukom","Ruth","1 Samuel","2 Samuel","1 Mga Hari","2 Mga Hari","1 Mga Cronica","2 Mga Cronica","Ezra","Nehemias","Ester")
private val GENRE_POETRY     = listOf("Job","Mga Awit","Mga Kawikaan","Ang Mangangaral","Ang Awit ni Solomon")
private val GENRE_PROPHETS   = listOf("Isaias","Jeremias","Mga Panaghoy","Ezekiel","Daniel","Hosea","Joel","Amos","Obadias","Jonas","Mikas","Nahum","Habakuk","Zefanias","Hagai","Zacarias","Malakias")
private val GENRE_GOSPELS    = listOf("Mateo","Marcos","Lucas","Juan")
private val GENRE_CHURCH     = listOf("Mga Gawa")
private val GENRE_LETTERS    = listOf("Mga Taga-Roma","1 Mga Taga-Corinto","2 Mga Taga-Corinto","Mga Taga-Galacia","Mga Taga-Efeso","Mga Taga-Filipos","Mga Taga-Colosas","1 Mga Taga-Tesalonica","2 Mga Taga-Tesalonica","1 Timoteo","2 Timoteo","Tito","Filemon","Mga Hebreo","Santiago","1 Pedro","2 Pedro","1 Juan","2 Juan","3 Juan","Judas")
private val GENRE_PROPHECY   = listOf("Pahayag")

private sealed class BookListItem {
    data class Label(val text: String) : BookListItem()
    data class Book(val name: String, val section: String) : BookListItem()
}

private data class BookSection(val label: String, val books: List<String>)

@Composable
fun BookSelectionOverlay(
    activeTranslationName: String = "",
    onTranslationClick: () -> Unit = {},
    onBookSelected: (String, Int) -> Unit,
    onClose: () -> Unit = {},
    onOpenAppearance: () -> Unit = {},
    fontStyle: String = "Default",
    customFonts: List<java.io.File> = emptyList()
) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedBook by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf("All Books") }

    val currentFontFamily = remember(fontStyle, customFonts) {
        getFontFamily(fontStyle, customFonts)
    }

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

    // Books to show based on selected filter (when not searching)
    val filteredBooks = when (selectedFilter) {
        "Old Testament" -> OLD_TESTAMENT
        "New Testament" -> NEW_TESTAMENT
        "Law"           -> GENRE_LAW
        "History"       -> GENRE_HISTORY
        "Poetry"        -> GENRE_POETRY
        "Prophets"      -> GENRE_PROPHETS
        "Gospels"       -> GENRE_GOSPELS
        "Church History"-> GENRE_CHURCH
        "Letters"       -> GENRE_LETTERS
        "Prophecy"      -> GENRE_PROPHECY
        else            -> OLD_TESTAMENT + NEW_TESTAMENT
    }

    val filteredOT = emptyList<String>() // no longer used separately
    val filteredNT = emptyList<String>() // no longer used separately

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        // Row with appearance button on the right
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, end = 6.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            IconButton(
                onClick = { onOpenAppearance() },
                modifier = Modifier
                    .size(32.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings2),
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        // Big title below the button row
        Text(
            text = "Bible",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = currentFontFamily,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        // ── Search ────────────────────────────────────────────────────────────
        CompositionLocalProvider(
            LocalTextSelectionColors provides TextSelectionColors(
                handleColor = MaterialTheme.colorScheme.onBackground,
                backgroundColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
            )
        ) {
        Surface(
            shape = RoundedCornerShape(50.dp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 16.dp)
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
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
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
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                            fontFamily = currentFontFamily,
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
                            color = MaterialTheme.colorScheme.onBackground,
                            fontFamily = currentFontFamily,
                            fontSize = 15.sp,
                            lineHeight = 15.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onBackground),
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
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        else
                            Color.Transparent,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        } // end CompositionLocalProvider
        Spacer(modifier = Modifier.height(10.dp))

        // Hoisted here so filter chips can scroll carousel to top
        val carouselListState = rememberLazyListState()
        val carouselScope = rememberCoroutineScope()
        LaunchedEffect(selectedFilter) {
            carouselListState.scrollToItem(0)
        }

        // ── Filter chips ──────────────────────────────────────────────────────
        val primaryFilters = listOf("All Books", "Old Testament", "New Testament")
        val genreFilters   = listOf("Law", "History", "Poetry", "Prophets", "Gospels", "Church History", "Letters", "Prophecy")

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(primaryFilters) { filter ->
                val isSelected = selectedFilter == filter
                Surface(
                    onClick = { selectedFilter = filter; expandedBook = null },
                    shape = RoundedCornerShape(50.dp),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onBackground
                    else
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    modifier = Modifier.height(34.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter,
                            fontFamily = currentFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.background
                            else
                                MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            // Separator line
            item {
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
                )
            }

            items(genreFilters) { filter ->
                val isSelected = selectedFilter == filter
                Surface(
                    onClick = { selectedFilter = filter; expandedBook = null },
                    shape = RoundedCornerShape(50.dp),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onBackground
                    else
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    modifier = Modifier.height(34.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter,
                            fontFamily = currentFontFamily,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected)
                                MaterialTheme.colorScheme.background
                            else
                                MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── Book list ─────────────────────────────────────────────────────────
        val allSections = listOf(
            BookSection("Law",           GENRE_LAW),
            BookSection("History",       GENRE_HISTORY),
            BookSection("Poetry",        GENRE_POETRY),
            BookSection("Prophets",      GENRE_PROPHETS),
            BookSection("Gospels",       GENRE_GOSPELS),
            BookSection("Church History",GENRE_CHURCH),
            BookSection("Letters",       GENRE_LETTERS),
            BookSection("Prophecy",      GENRE_PROPHECY)
        )

        val sectionsToShow = when (selectedFilter) {
            "Old Testament"  -> allSections.filter { it.label in listOf("Law","History","Poetry","Prophets") }
            "New Testament"  -> allSections.filter { it.label in listOf("Gospels","Church History","Letters","Prophecy") }
            "Law","History","Poetry","Prophets","Gospels","Church History","Letters","Prophecy"
                             -> allSections.filter { it.label == selectedFilter }
            else             -> allSections
        }

        // Flat list of books only (no label items — label shown above separately)
        val bookItems: List<BookListItem.Book> = if (isSearching) {
            rankedResults.map { BookListItem.Book(it, "") }
        } else {
            sectionsToShow.flatMap { section ->
                section.books.map { BookListItem.Book(it, section.label) }
            }
        }

        if (isSearching) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(rankedResults) { book ->
                    BookRow(
                        book = book,
                        isExpanded = expandedBook == book,
                        onToggle = { expandedBook = if (expandedBook == book) null else book },
                        onChapterSelected = { chapter -> onBookSelected(book, chapter) }
                    )
                }
            }
        } else {
            // ── Carousel ──────────────────────────────────────────────────────
            val listState = carouselListState
            val density   = LocalDensity.current

            // Card dimensions
            val cardWidth  = 160.dp
            val cardHeight = 220.dp
            val cardWidthPx  = with(density) { cardWidth.toPx() }
            val spacing      = 8.dp
            val spacingPx    = with(density) { spacing.toPx() }

            // Derive center index from scroll state
            val centerIndex by remember {
                derivedStateOf {
                    val layoutInfo   = listState.layoutInfo
                    val viewportMid  = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                    layoutInfo.visibleItemsInfo
                        .minByOrNull { kotlin.math.abs((it.offset + it.size / 2f) - viewportMid) }
                        ?.index ?: 0
                }
            }

            // Track label changes and scroll direction for slide animation
            var currentLabel by remember { mutableStateOf(bookItems.getOrNull(0)?.section ?: "") }
            var slideFromRight by remember { mutableStateOf(true) }
            var prevCenterIndex by remember { mutableIntStateOf(0) }

            // Reset label when filter changes (bookItems changes, center goes back to 0)
            LaunchedEffect(bookItems) {
                val newLabel = bookItems.getOrNull(0)?.section ?: ""
                slideFromRight = true
                currentLabel = newLabel
                prevCenterIndex = 0
            }

            LaunchedEffect(centerIndex) {
                val newLabel = bookItems.getOrNull(centerIndex)?.section ?: ""
                if (newLabel != currentLabel) {
                    slideFromRight = centerIndex > prevCenterIndex
                    currentLabel = newLabel
                }
                prevCenterIndex = centerIndex
                // Close chapter picker if center moved away from expanded book
                if (expandedBook != null && bookItems.getOrNull(centerIndex)?.name != expandedBook) {
                    expandedBook = null
                }
            }

            // Genre label with directional slide transition
            AnimatedContent(
                targetState = currentLabel,
                transitionSpec = {
                    if (slideFromRight) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "genreLabel"
            ) { label ->
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = currentFontFamily,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            // Carousel
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(
                    horizontal = (LocalConfiguration.current.screenWidthDp.dp - cardWidth) / 2,
                    vertical = 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight + 100.dp),
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
            ) {
                itemsIndexed(bookItems) { index, item ->
                    val distFromCenter by remember {
                        derivedStateOf {
                            val layoutInfo  = listState.layoutInfo
                            val viewportMid = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                            val info = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                            if (info != null) {
                                val itemMid = info.offset + info.size / 2f
                                kotlin.math.abs(itemMid - viewportMid) / (cardWidthPx + spacingPx)
                            } else 1f
                        }
                    }
                    val scale = lerp(1f, 0.78f, distFromCenter.coerceIn(0f, 1f))
                    val alpha = lerp(1f, 0.5f, distFromCenter.coerceIn(0f, 1f))

                    val cardColor = when (item.section) {
                        "Law"           -> Color(0xFF7EC8E3)
                        "History"       -> Color(0xFF8B5E3C)
                        "Poetry"        -> Color(0xFF4CAF50)
                        "Prophets"      -> Color(0xFFFFD54F)
                        "Gospels"       -> Color(0xFF9C27B0)
                        "Church History"-> Color(0xFF1976D2)
                        "Letters"       -> Color(0xFFF48FB1)
                        "Prophecy"      -> Color(0xFFFF7043)
                        else            -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.09f)
                    }

                    val cardImageRes = when (item.section) {
                        "Law"           -> R.drawable.books_law
                        "History"       -> R.drawable.books_history
                        "Poetry"        -> R.drawable.books_poetry
                        "Prophets"      -> R.drawable.books_prophets
                        "Gospels"       -> R.drawable.books_gospels
                        "Church History"-> R.drawable.books_church
                        "Letters"       -> R.drawable.books_letters
                        "Prophecy"      -> R.drawable.books_prophecy
                        else            -> null
                    }

                    Column(
                        modifier = Modifier
                            .width(cardWidth)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Card — image is the card
                        val clickModifier = Modifier
                            .width(cardWidth)
                            .height(cardHeight)
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                when {
                                    index == centerIndex -> {
                                        // Center book — open chapter picker
                                        expandedBook = if (expandedBook == item.name) null else item.name
                                    }
                                    index < centerIndex -> {
                                        // Left book — scroll to previous
                                        carouselScope.launch {
                                            listState.animateScrollToItem((centerIndex - 1).coerceAtLeast(0))
                                        }
                                    }
                                    else -> {
                                        // Right book — scroll to next
                                        carouselScope.launch {
                                            listState.animateScrollToItem((centerIndex + 1).coerceAtMost(bookItems.lastIndex))
                                        }
                                    }
                                }
                            }

                        if (cardImageRes != null) {
                            Image(
                                painter = painterResource(id = cardImageRes),
                                contentDescription = item.section,
                                contentScale = ContentScale.Crop,
                                modifier = clickModifier
                            )
                        } else {
                            Box(modifier = clickModifier.background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.09f)))
                        }
                        // Book name below card
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = item.name,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = currentFontFamily,
                            lineHeight = 17.sp,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.width(cardWidth)
                        )
                    }
                }
            }

            // Chapter picker for expanded book
            val expandedItem = bookItems.firstOrNull { it.name == expandedBook }
            AnimatedVisibility(
                visible = expandedItem != null,
                enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                exit  = fadeOut(tween(150)) + shrinkVertically(tween(200))
            ) {
                expandedItem?.let { item ->
                    val chapters = BIBLE_BOOKS[item.name] ?: 1
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        (1..chapters).forEach { chapter ->
                            Surface(
                                onClick = { onBookSelected(item.name, chapter) },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = chapter.toString(),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = currentFontFamily
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BookCard(
    book: String,
    isExpanded: Boolean,
    onTap: () -> Unit,
    onChapterSelected: (Int) -> Unit,
    fontFamily: FontFamily = FontFamily.Default
) {
    val chapters = BIBLE_BOOKS[book] ?: 1

    Column {
        // Book card
        Surface(
            onClick = onTap,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f),
            modifier = Modifier
                .width(110.dp)
                .height(80.dp)
        ) {
            Box(
                modifier = Modifier.padding(12.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = book,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = fontFamily,
                    lineHeight = 17.sp,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        // Chapter picker — expands below card
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(200)) + expandVertically(tween(250)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f),
                modifier = Modifier
                    .width(110.dp)
                    .padding(top = 6.dp)
            ) {
                FlowRow(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..chapters).forEach { chapter ->
                        Surface(
                            onClick = { onChapterSelected(chapter) },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = chapter.toString(),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = fontFamily
                                )
                            }
                        }
                    }
                }
            }
        }
    }
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
                color = MaterialTheme.colorScheme.onBackground,
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
            if (isExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
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
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = chapter.toString(),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
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
    selectedThemeIndex: Int,
    onThemeChange: (Int) -> Unit,
    onAddFont: () -> Unit = {},
    onRemoveFont: (String) -> Unit = {},
    onClose: () -> Unit
) {
    var isDeleteMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                color = MaterialTheme.colorScheme.onBackground,
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
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
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
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))

            val contentTextColor = MaterialTheme.colorScheme.onBackground
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = contentTextColor.copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.dp, contentTextColor.copy(alpha = 0.15f))
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = contentTextColor.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold,
                                fontFamily = getFontFamily(currentFontStyle, customFonts),
                                fontSize = (currentFontSize * 0.65f).sp
                            )
                        ) { append("1  ") }
                        withStyle(
                            SpanStyle(
                                color = contentTextColor,
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
                val lineTrackColor = MaterialTheme.colorScheme.onBackground
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    val stepsCount = 6
                    val trackWidth = size.width
                    val stepWidth = trackWidth / (stepsCount - 1)
                    val valuePerStep = (24f - 14f) / (stepsCount - 1)
                    
                    // Draw horizontal track
                    drawLine(
                        color = lineTrackColor.copy(alpha = 0.2f),
                        start = Offset(0f, size.height / 2),
                        end = Offset(trackWidth, size.height / 2),
                        strokeWidth = 2.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    
                    val activeTrackWidth = ((currentFontSize - 14f) / (24f - 14f)) * trackWidth
                    drawLine(
                        color = lineTrackColor,
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
                            color = if (isActive) lineTrackColor else lineTrackColor.copy(alpha = 0.4f),
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
                                .background(MaterialTheme.colorScheme.onBackground, CircleShape)
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
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
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
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    onClick = onAddFont,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_plus_lucide),
                            contentDescription = "Add Font",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
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
                        color = if (isSelected) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f) else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = fontName,
                            fontFamily = getFontFamily(fontName),
                            color = MaterialTheme.colorScheme.onBackground,
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
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
                        )
                    }
                    // Custom fonts
                    items(customFontNames) { fontName ->
                        val isSelected = fontName == currentFontStyle
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected && !isDeleteMode) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
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
                                color = MaterialTheme.colorScheme.onBackground,
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

            Spacer(modifier = Modifier.height(20.dp))

            // Theme section header
            Text(
                text = "Theme",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Defined themes for selection
            val themes = listOf(
                Triple("Paper", Color(0xFFFEF9F3), Color(0xFF5B4636)),
                Triple("Dark", Color(0xFF131313), Color(0xFFE0E0E0)),
                Triple("Light", Color(0xFFEEECED), Color.Black)
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(themes.size) { index ->
                    val (themeName, bgColor, textColor) = themes[index]
                    val isSelected = selectedThemeIndex == index
                    Surface(
                        onClick = { onThemeChange(index) },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.width(100.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Mini mockup box representing the theme layout
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(55.dp)
                                    .background(bgColor, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // 3 mockup text lines
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .height(3.dp)
                                            .background(textColor.copy(alpha = 0.8f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.75f)
                                            .height(3.dp)
                                            .background(textColor.copy(alpha = 0.8f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.5f)
                                            .height(3.dp)
                                            .background(textColor.copy(alpha = 0.5f))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = themeName,
                                color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
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
            .background(MaterialTheme.colorScheme.background)
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
                color = MaterialTheme.colorScheme.onBackground,
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
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
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
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
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
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 17.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = code,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                fontSize = 12.sp
                            )
                        }
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.onBackground,
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f),
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
            "#$initialHex".toColorInt()
        } catch (_: Exception) {
            android.graphics.Color.RED
        }
    }
    
    val initialHsv = remember(initialColor) {
        val hsvOut = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor, hsvOut)
        hsvOut
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
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
                            val parsed = "#$clean".toColorInt()
                            val hsvOut = FloatArray(3)
                            android.graphics.Color.colorToHSV(parsed, hsvOut)
                            if (hsvOut[1] > 0f) {
                                hue = hsvOut[0]
                            }
                            saturation = hsvOut[1]
                            value = hsvOut[2]
                            hexError = false
                        } catch (_: Exception) { hexError = true }
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
                        detectDragGestures { change: androidx.compose.ui.input.pointer.PointerInputChange, _: androidx.compose.ui.geometry.Offset ->
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
                        detectDragGestures { change: androidx.compose.ui.input.pointer.PointerInputChange, _: androidx.compose.ui.geometry.Offset ->
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

// ── Selected Verses Overlay ───────────────────────────────────────────────────

@Composable
fun SelectedVersesOverlay(
    headerText: String,
    selectedVerses: List<com.bibleread.bread.data.VerseEntity>,
    fontStyle: String,
    fontSize: Float,
    customFonts: List<java.io.File>,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit
) {
    var orientationMode by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("square") }
    var showShareOptions by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(start = 20.dp, end = 20.dp, top = 60.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main share window
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { /* consume touches */ },
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E1E),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // ── Header ────────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Orientation toggle button — left (cycles: square → portrait → landscape)
                        IconButton(
                            onClick = {
                                orientationMode = when (orientationMode) {
                                    "square" -> "portrait"
                                    "portrait" -> "landscape"
                                    else -> "square"
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 12.dp)
                                .size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    when (orientationMode) {
                                        "portrait" -> R.drawable.ic_rectangle_vertical
                                        "landscape" -> R.drawable.ic_rectangle_horizontal
                                        else -> R.drawable.ic_square
                                    }
                                ),
                                contentDescription = "Toggle Orientation",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        Text(
                            text = headerText,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 56.dp)
                        )
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.08f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // ── Verse list — centered with aspect ratio container ──────
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Aspect ratio container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(
                                    when (orientationMode) {
                                        "portrait" -> 9f / 16f
                                        "landscape" -> 16f / 9f
                                        else -> 1f // square
                                    }
                                )
                                .background(
                                    Color.White.copy(alpha = 0.05f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                )
                                .verticalScroll(rememberScrollState())
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                selectedVerses.forEach { verse ->
                                    val verseLabel = verse.display ?: verse.verse.toString()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 16.dp)
                                    ) {
                                        Text(
                                            text = buildAnnotatedString {
                                                withStyle(
                                                    SpanStyle(
                                                        color = Color(0xFFAAAAAA),
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = getFontFamily(fontStyle, customFonts),
                                                        fontSize = (fontSize * 0.65f).sp
                                                    )
                                                ) { append("$verseLabel  ") }
                                                withStyle(
                                                    SpanStyle(
                                                        color = Color.White,
                                                        fontFamily = getFontFamily(fontStyle, customFonts),
                                                        fontSize = fontSize.sp
                                                    )
                                                ) { append(verse.text.trim()) }
                                            },
                                            lineHeight = (fontSize * 1.9).sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Bottom action buttons ──────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Copy button
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_copy_lucide),
                                contentDescription = "Copy",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        // Change Background button
                        IconButton(
                            onClick = { /* TODO: Add change background logic */ },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_image_lucide),
                                contentDescription = "Change Image",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        // Share button
                        IconButton(
                            onClick = { showShareOptions = !showShareOptions },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_share2_lucide),
                                contentDescription = "Share",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // ── Share options popup ────────────────────────────────────────
            AnimatedVisibility(
                visible = showShareOptions,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(200))
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 0.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E1E1E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Share option
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                onShare()
                                showShareOptions = false
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_share2_lucide),
                                    contentDescription = "Share",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Share",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }

                        // Chat option
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                // TODO: Add chat logic
                                showShareOptions = false
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_chattab),
                                    contentDescription = "Chat",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Chat",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }

                        // Copy link option
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                onCopy()
                                showShareOptions = false
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_copy_lucide),
                                    contentDescription = "Copy Link",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Copy Link",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }

                        // Download option
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                // TODO: Add download logic
                                showShareOptions = false
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_download),
                                    contentDescription = "Download",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Download",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
