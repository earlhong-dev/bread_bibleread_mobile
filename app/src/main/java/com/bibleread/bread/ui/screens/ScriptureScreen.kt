package com.bibleread.bread.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
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

    val selectedVerses = remember { mutableStateSetOf<String>() }

    var showColorPickerRow by rememberSaveable { mutableStateOf(false) }
    var showNoColorNotif by remember { mutableStateOf(false) }
    var isChapterPillExpanded by rememberSaveable { mutableStateOf(true) }

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
                            onOpenBookSelection { book, chapter ->
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

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 36.dp)
                            .graphicsLayer { alpha = contentAlpha }
                            .pointerInput(Unit) {
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
                            item(key = "$selectedBook-$chapter-markread") {
                                val isRead = vm.isChapterRead(selectedBook, chapter)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 36.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        onClick = {
                                            if (isRead) vm.unmarkChapterRead(selectedBook, chapter)
                                            else vm.markChapterRead(selectedBook, chapter)
                                        },
                                        shape = RoundedCornerShape(50.dp),
                                        color = if (isRead)
                                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
                                        else
                                            MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (isRead) "✓ Read" else "Mark as Read",
                                                color = if (isRead)
                                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                                else
                                                    MaterialTheme.colorScheme.background,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = currentFontFamily
                                            )
                                        }
                                    }
                                }
                            }
                            item { Spacer(modifier = Modifier.height(24.dp)) }
                        }
                    }
                }
            }

        }
    }


        // ── Gradient fade behind scripture toolbar ────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // ── Scripture toolbar (bottom-center) ─────────────────────────────────
        AnimatedVisibility(
            visible = headerReady,
            enter = fadeIn(tween(220)) + slideInVertically { it / 2 },
            exit = fadeOut(tween(160)) + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            val context = LocalContext.current
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedVisibility(
                    visible = showNoColorNotif,
                    enter = fadeIn(tween(200)) + slideInVertically { it / 2 },
                    exit = fadeOut(tween(200)) + slideOutVertically { it / 2 }
                ) {
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = Color(0xCC1A1A1A),
                        tonalElevation = 0.dp
                    ) {
                        Text(
                            text = "No Highlighter Color Selected",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
                // ── Smart Ease Animated Widths ──
                val smartEaseSpec = tween<androidx.compose.ui.unit.Dp>(
                    durationMillis = 350,
                    easing = FastOutSlowInEasing
                )
                val leftWidth by animateDpAsState(
                    targetValue = if (!isChapterPillExpanded) 232.dp else 52.dp,
                    animationSpec = smartEaseSpec,
                    label = "leftWidth"
                )
                val rightWidth by animateDpAsState(
                    targetValue = if (!isChapterPillExpanded) 52.dp else 232.dp,
                    animationSpec = smartEaseSpec,
                    label = "rightWidth"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val maxChapter = BIBLE_BOOKS[selectedBook] ?: 1

                    // ── LEFT ELEMENT (Pill <-> Circle with Smart Ease Morph) ──
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 4.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .width(leftWidth)
                            .height(52.dp)
                            .drawBehind {
                                drawIntoCanvas { canvas ->
                                    val paint = Paint().apply {
                                        asFrameworkPaint().apply {
                                            isAntiAlias = true
                                            color = android.graphics.Color.TRANSPARENT
                                            setShadowLayer(
                                                8f, 0f, 2f,
                                                android.graphics.Color.argb(120, 0, 0, 0)
                                            )
                                        }
                                    }
                                    val cornerRadius = size.height / 2f
                                    canvas.drawRoundRect(
                                        left = 0f,
                                        top = 0f,
                                        right = size.width,
                                        bottom = size.height,
                                        radiusX = cornerRadius,
                                        radiusY = cornerRadius,
                                        paint = paint
                                    )
                                }
                            }
                    ) {
                        AnimatedContent(
                            targetState = isChapterPillExpanded,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220, delayMillis = 60, easing = FastOutSlowInEasing)) togetherWith
                                    fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing))
                            },
                            label = "leftContent"
                        ) { expanded ->
                            if (!expanded) {
                                // Full Toolbar Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                ) {
                                    // Share
                                    IconButton(
                                        onClick = {
                                            val shareText = "$selectedBook $targetChapter"
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(Intent.createChooser(intent, null))
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_share2_lucide),
                                            contentDescription = "Share",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Appearance settings
                                    IconButton(
                                        onClick = { onOpenAppearance() },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_settings2),
                                            contentDescription = "Appearance",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Draw / annotate
                                    IconButton(
                                        onClick = { /* TODO: draw mode */ },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_pencil),
                                            contentDescription = "Draw",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Bookmark / Highlight selected verses
                                    val allHighlighted = selectedVerses.isNotEmpty() &&
                                        selectedVerses.all { vm.highlights[it] != null }
                                    val hasSelection = selectedVerses.isNotEmpty()
                                    IconButton(
                                        enabled = hasSelection,
                                        onClick = {
                                            if (allHighlighted) {
                                                selectedVerses.toSet().forEach { vm.removeHighlight(it) }
                                                selectedVerses.clear()
                                            } else {
                                                val color = vm.selectedHighlightColor.value
                                                if (color != null) {
                                                    vm.applyHighlight(selectedVerses.toSet(), color)
                                                    selectedVerses.clear()
                                                } else {
                                                    showNoColorNotif = true
                                                    coroutineScope.launch {
                                                        delay(2000)
                                                        showNoColorNotif = false
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(
                                                if (allHighlighted) R.drawable.ic_bookmark_filled
                                                else R.drawable.ic_bookmark
                                            ),
                                            contentDescription = "Bookmark",
                                            tint = when {
                                                !hasSelection -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                                allHighlighted -> Color.White
                                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Highlight color circle — shows active color, opens color picker
                                    val activeHighlightColor = vm.selectedHighlightColor.value
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clickable(
                                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                showColorPickerRow = !showColorPickerRow
                                            }
                                    ) {
                                        if (activeHighlightColor != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .background(color = activeHighlightColor, shape = CircleShape)
                                                    .border(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .drawBehind {
                                                        val sweep = Brush.sweepGradient(
                                                            colors = listOf(
                                                                Color(0xFFFF6B6B), // vivid coral
                                                                Color(0xFFFF9F43), // vivid orange
                                                                Color(0xFFFECA57), // vivid yellow
                                                                Color(0xFF1DD1A1), // vivid teal
                                                                Color(0xFF54A0FF), // vivid blue
                                                                Color(0xFFA55EEA), // vivid purple
                                                                Color(0xFFFD79A8), // vivid pink
                                                                Color(0xFFFF6B6B), // back to vivid coral
                                                            ),
                                                            center = Offset(size.width / 2f, size.height / 2f)
                                                        )
                                                        drawCircle(
                                                            brush = sweep,
                                                            radius = size.minDimension / 2f
                                                        )
                                                    }
                                                    .border(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), CircleShape)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Collapsed Circle Icon — tapping it expands back to toolbar pill
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clickable(
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            isChapterPillExpanded = false
                                        }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_settings2),
                                        contentDescription = "Show Toolbar",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // ── RIGHT ELEMENT (Circle <-> Pill with Smart Ease Morph) ──
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = Color.White,
                        tonalElevation = 4.dp,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .width(rightWidth)
                            .height(52.dp)
                            .drawBehind {
                                drawIntoCanvas { canvas ->
                                    val paint = Paint().apply {
                                        asFrameworkPaint().apply {
                                            isAntiAlias = true
                                            color = android.graphics.Color.TRANSPARENT
                                            setShadowLayer(
                                                8f, 0f, 2f,
                                                android.graphics.Color.argb(120, 0, 0, 0)
                                            )
                                        }
                                    }
                                    val cornerRadius = size.height / 2f
                                    canvas.drawRoundRect(
                                        left = 0f,
                                        top = 0f,
                                        right = size.width,
                                        bottom = size.height,
                                        radiusX = cornerRadius,
                                        radiusY = cornerRadius,
                                        paint = paint
                                    )
                                }
                            }
                    ) {
                        AnimatedContent(
                            targetState = isChapterPillExpanded,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220, delayMillis = 60, easing = FastOutSlowInEasing)) togetherWith
                                    fadeOut(animationSpec = tween(140, easing = FastOutSlowInEasing))
                            },
                            label = "rightContent"
                        ) { expanded ->
                            if (!expanded) {
                                // Chapter Number Circle (clickable to expand)
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clickable(
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            isChapterPillExpanded = true
                                        }
                                ) {
                                    Text(
                                        text = targetChapter.toString(),
                                        color = Color(0xFF1A1A1A),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = currentFontFamily
                                    )
                                }
                            } else {
                                // Chapter Navigation Pill
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                ) {
                                    // Previous Chapter Button
                                    val hasPrev = targetChapter > 1
                                    IconButton(
                                        enabled = hasPrev,
                                        onClick = {
                                            if (hasPrev) {
                                                requestChapter(selectedBook, targetChapter - 1)
                                            }
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_chevron_left),
                                            contentDescription = "Previous Chapter",
                                            tint = if (hasPrev) Color(0xFF1A1A1A) else Color(0xFF1A1A1A).copy(alpha = 0.25f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    // Center: Chapter Name / Book Selection
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(
                                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                onOpenBookSelection { book, chapter ->
                                                    requestChapter(book, chapter)
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = "Chapter $targetChapter",
                                            color = Color(0xFF1A1A1A),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = currentFontFamily
                                        )
                                    }

                                    // Next Chapter Button
                                    val hasNext = targetChapter < maxChapter
                                    IconButton(
                                        enabled = hasNext,
                                        onClick = {
                                            if (hasNext) {
                                                requestChapter(selectedBook, targetChapter + 1)
                                            }
                                        },
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_chevron_right),
                                            contentDescription = "Next Chapter",
                                            tint = if (hasNext) Color(0xFF1A1A1A) else Color(0xFF1A1A1A).copy(alpha = 0.25f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
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

        // ── Color picker bottom sheet ─────────────────────────────────────────
        // Scrim
        AnimatedVisibility(
            visible = showColorPickerRow,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(260)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) { showColorPickerRow = false }
            )
        }
        // Sheet with slide-up/down + drag-to-close
        var dragOffsetY by remember { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }
        var colorPendingDelete by remember { mutableStateOf<Color?>(null) }
        val animatedOffset by animateFloatAsState(
            targetValue = if (isDragging) dragOffsetY else 0f,
            animationSpec = if (isDragging) snap() else tween(durationMillis = 220),
            label = "sheetDrag"
        )
        val dismissThreshold = with(LocalDensity.current) { 120.dp.toPx() }

        AnimatedVisibility(
            visible = showColorPickerRow,
            enter = slideInVertically(animationSpec = tween(300)) { it },
            exit = slideOutVertically(animationSpec = tween(260)) { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationY = if (isDragging) dragOffsetY else animatedOffset }
            ) {
                Surface(
                    color = Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { isDragging = true },
                                onDragEnd = {
                                    isDragging = false
                                    if (dragOffsetY > dismissThreshold) {
                                        showColorPickerRow = false
                                    }
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    isDragging = false
                                    dragOffsetY = 0f
                                },
                                onDrag = { _, dragAmount ->
                                    dragOffsetY = (dragOffsetY + dragAmount.y).coerceAtLeast(0f)
                                }
                            )
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Drag handle
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 36.dp, height = 4.dp)
                                    .background(
                                        Color.White,
                                        RoundedCornerShape(50.dp)
                                    )
                            )
                        }

                        Text(
                            text = "Highlight Color",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White,
                            fontFamily = currentFontFamily,
                            modifier = Modifier.padding(bottom = 20.dp)
                        )

                        // Color swatches with edge fades
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                        ) {
                            val canScrollLeft by remember {
                                derivedStateOf {
                                    colorListState.firstVisibleItemIndex > 0 ||
                                        colorListState.firstVisibleItemScrollOffset > 0
                                }
                            }
                            val canScrollRight by remember {
                                derivedStateOf {
                                    val info = colorListState.layoutInfo
                                    val last = info.visibleItemsInfo.lastOrNull()
                                    last != null && (last.index < info.totalItemsCount - 1 ||
                                        last.offset + last.size > info.viewportEndOffset)
                                }
                            }

                            LazyRow(
                                state = colorListState,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                            items(presetColors) { c ->
                                val isSelected = vm.selectedHighlightColor.value == c
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(c, CircleShape)
                                        .clickable {
                                            if (vm.selectedHighlightColor.value == c) {
                                                vm.selectHighlightColor(null)
                                            } else {
                                                vm.selectHighlightColor(c)
                                            }
                                        }
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_check_lucide),
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            // Vertical separator — always shown
                            item {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .width(12.dp)
                                        .height(32.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(20.dp)
                                            .background(Color.White.copy(alpha = 0.15f))
                                    )
                                }
                            }
                            if (vm.customColors.isEmpty()) {
                                item {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(
                                            text = "Add Custom Color?",
                                            color = Color.White.copy(alpha = 0.35f),
                                            fontSize = 12.sp,
                                            fontFamily = currentFontFamily,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            items(vm.customColors) { c ->
                                val isSelected = vm.selectedHighlightColor.value == c
                                val isPendingDelete = colorPendingDelete == c
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(c, CircleShape)
                                        .pointerInput(colorPendingDelete) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    colorPendingDelete = if (isPendingDelete) null else c
                                                },
                                                onTap = {
                                                    if (isPendingDelete) {
                                                        vm.removeCustomColor(c)
                                                        colorPendingDelete = null
                                                    } else {
                                                        colorPendingDelete = null
                                                        if (vm.selectedHighlightColor.value == c) {
                                                            vm.selectHighlightColor(null)
                                                        } else {
                                                            vm.selectHighlightColor(c)
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                ) {
                                    when {
                                        isPendingDelete -> Icon(
                                            painter = painterResource(R.drawable.ic_trash_lucide),
                                            contentDescription = "Delete",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        isSelected -> Icon(
                                            painter = painterResource(R.drawable.ic_check_lucide),
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        } // end LazyRow

                            // Left fade — only when not at start
                            if (canScrollLeft) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .width(20.dp)
                                        .height(32.dp)
                                        .background(
                                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                colors = listOf(Color(0xFF1A1A1A), Color.Transparent)
                                            )
                                        )
                                )
                            }
                            // Right fade — only when not at end
                            if (canScrollRight) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .width(20.dp)
                                        .height(32.dp)
                                        .background(
                                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                colors = listOf(Color.Transparent, Color(0xFF1A1A1A))
                                            )
                                        )
                                )
                            }
                        } // end Box (swatches + fades)

                        Spacer(modifier = Modifier.height(0.dp))

                        // Inline custom color picker
                        val selectedHex = remember(vm.selectedHighlightColor.value) {
                            vm.selectedHighlightColor.value?.let { c ->
                                String.format(
                                    "%02X%02X%02X",
                                    (c.red * 255).toInt(),
                                    (c.green * 255).toInt(),
                                    (c.blue * 255).toInt()
                                )
                            } ?: vm.lastCustomHex
                        }
                        CustomColorPickerPanel(
                            initialHex = selectedHex,
                            onHexChanged = { vm.saveLastCustomHex(it) },
                            onColorSelected = { color ->
                                vm.selectHighlightColor(color)
                                showColorPickerRow = false
                            },
                            onColorAdded = { color ->
                                vm.addCustomColor(color)
                                vm.selectHighlightColor(color)
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(50)
                                    colorListState.animateScrollToItem(presetColors.size + vm.customColors.size - 1)
                                }
                            },
                            onDismiss = { showColorPickerRow = false }
                        )
                    }
                }
            }
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
    customFonts: List<java.io.File> = emptyList(),
    getReadCount: (String) -> Int = { 0 },
    getTotalCount: (String) -> Int = { 1 },
    initialViewMode: String = "carousel",
    onViewModeChange: (String) -> Unit = {},
    initialFilter: String = "All Books",
    onFilterChange: (String) -> Unit = {},
    initialCarouselIndex: Int = 0,
    onCarouselIndexChange: (Int) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    var searchQuery by remember { mutableStateOf("") }
    var expandedBook by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf(initialFilter) }

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
                // Clear button — circle with X icon, white background in dark mode
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (searchQuery.isNotEmpty())
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
                            else Color.Transparent
                        )
                        .clickable(
                            enabled = searchQuery.isNotEmpty(),
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = { searchQuery = "" }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_close),
                        contentDescription = "Clear",
                        tint = if (searchQuery.isNotEmpty())
                            MaterialTheme.colorScheme.onBackground
                        else
                            Color.Transparent,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
        } // end CompositionLocalProvider
        Spacer(modifier = Modifier.height(10.dp))

        // Hoisted here so filte
        // r chips can scroll carousel to top
        val carouselListState = remember(selectedFilter) {
            androidx.compose.foundation.lazy.LazyListState(0, 0)
        }
        val carouselScope = rememberCoroutineScope()

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
                    onClick = { selectedFilter = filter; expandedBook = null; onFilterChange(filter) },
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

            // Separator
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
                    onClick = { selectedFilter = filter; expandedBook = null; onFilterChange(filter) },
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

        run {
            // ── Carousel / List toggle ─────────────────────────────────────────
            var viewMode by remember { mutableStateOf(initialViewMode) }
            // When searching, always use list view
            val effectiveViewMode = if (isSearching) "list" else viewMode

            val listState = carouselListState
            val density   = LocalDensity.current

            // Card dimensions
            val cardWidth  = 160.dp
            val cardHeight = 220.dp
            val cardWidthPx  = with(density) { cardWidth.toPx() }
            val spacing      = 8.dp
            val spacingPx    = with(density) { spacing.toPx() }

            // Derive center index from scroll state
            val centerIndex by remember(carouselListState) {
                derivedStateOf {
                    val layoutInfo   = carouselListState.layoutInfo
                    val viewportMid  = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                    layoutInfo.visibleItemsInfo
                        .minByOrNull { kotlin.math.abs((it.offset + it.size / 2f) - viewportMid) }
                        ?.index ?: 0
                }
            }

            // Track label changes for slide animation
            var currentLabel by remember { mutableStateOf(bookItems.getOrNull(0)?.section ?: "") }

            // In list view (or searching), override label for primary filters
            val displayLabel = if (effectiveViewMode == "list") {
                when (selectedFilter) {
                    "All Books"    -> "All"
                    "Old Testament"-> "Old"
                    "New Testament"-> "New"
                    else           -> selectedFilter
                }
            } else currentLabel

            // Reset label when filter changes (bookItems changes, center goes back to 0)
            LaunchedEffect(bookItems) {
                val newLabel = bookItems.getOrNull(0)?.section ?: ""
                currentLabel = newLabel
            }

            LaunchedEffect(centerIndex, viewMode, bookItems) {
                val newLabel = bookItems.getOrNull(centerIndex)?.section ?: ""
                if (newLabel.isNotEmpty()) {
                    currentLabel = newLabel
                }
                onCarouselIndexChange(centerIndex)
                // Close chapter picker if center moved away from expanded book
                if (expandedBook != null && bookItems.getOrNull(centerIndex)?.name != expandedBook) {
                    expandedBook = null
                }
            }

            // Genre label row — hidden when searching
            if (!isSearching) Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = displayLabel,
                    transitionSpec = {
                        (slideInHorizontally { it } + fadeIn()).togetherWith(
                            slideOutHorizontally { -it } + fadeOut()
                        )
                    },
                    label = "genreLabel",
                    modifier = Modifier.weight(1f)
                ) { label ->
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = currentFontFamily,
                        letterSpacing = 0.5.sp
                    )
                }
                // View toggle — outside AnimatedContent so it doesn't slide, hidden when searching
                if (!isSearching) Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { viewMode = if (viewMode == "carousel") "list" else "carousel"
                               onViewModeChange(viewMode) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (viewMode == "carousel") R.drawable.ic_list_view
                            else R.drawable.ic_book_view
                        ),
                        contentDescription = "Toggle view",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (effectiveViewMode == "carousel") {
                // Fade in on first load
                var carouselVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { carouselVisible = true }

                AnimatedVisibility(
                    visible = carouselVisible,
                    enter = fadeIn(animationSpec = tween(500))
                ) {
                    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                    val isSmallScreen = screenHeight < 760.dp

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Carousel
                        LazyRow(
                            state = listState,
                            contentPadding = PaddingValues(
                                horizontal = (LocalConfiguration.current.screenWidthDp.dp - cardWidth) / 2,
                                vertical = if (isSmallScreen) 8.dp else 16.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(spacing),
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight(),
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
                                    // Card — image only, scroll left/right on side taps, no open
                                    val cardModifier = Modifier
                                        .width(cardWidth)
                                        .height(cardHeight)
                                        .clickable(
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            when {
                                                index < centerIndex -> carouselScope.launch {
                                                     listState.animateScrollToItem((centerIndex - 1).coerceAtLeast(0))
                                                }
                                                index > centerIndex -> carouselScope.launch {
                                                     listState.animateScrollToItem((centerIndex + 1).coerceAtMost(bookItems.lastIndex))
                                                }
                                                // center — no action, Read button handles opening
                                            }
                                        }

                                    if (cardImageRes != null) {
                                        Image(
                                            painter = painterResource(id = cardImageRes),
                                            contentDescription = item.section,
                                            contentScale = ContentScale.Crop,
                                            modifier = cardModifier
                                        )
                                    } else {
                                        Box(modifier = cardModifier.background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.09f)))
                                    }
                                    // Book name below card
                                    Spacer(modifier = Modifier.height(if (isSmallScreen) 6.dp else 10.dp))
                                    BoxWithConstraints(
                                        modifier = Modifier.width(cardWidth),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val availableWidthSp = with(LocalDensity.current) { maxWidth.toSp() }
                                        val nameLength = item.name.length.coerceAtLeast(1)
                                        // Estimate: ~0.6sp per char at given size; shrink if needed
                                        val fontSize = (availableWidthSp.value / (nameLength * 0.62f))
                                            .coerceIn(8f, 16f).sp
                                        Text(
                                            text = item.name,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = currentFontFamily,
                                            maxLines = 1,
                                            fontSize = fontSize,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                                            softWrap = false,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        // Read button + Reading Progress
                        val centerBook = bookItems.getOrNull(centerIndex)
                        if (centerBook != null) {
                            Spacer(modifier = Modifier.height(0.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    onClick = { focusManager.clearFocus(); onBookSelected(centerBook.name, 1) },
                                    shape = RoundedCornerShape(50.dp),
                                    color = Color.White,
                                    modifier = Modifier.height(44.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Read",
                                            color = Color.Black,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = currentFontFamily
                                        )
                                    }
                                }
                            }

                            // Reading Progress
                            Spacer(modifier = Modifier.height(if (isSmallScreen) 8.dp else 14.dp))
                            val readCount   = getReadCount(centerBook.name)
                            val totalCount  = getTotalCount(centerBook.name).coerceAtLeast(1)
                            val progress    = readCount / totalCount.toFloat()

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Reading Progress",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = currentFontFamily
                                    )
                                    Text(
                                        text = "$readCount / $totalCount",
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = currentFontFamily
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                // Custom tall progress bar with percentage label inside
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(22.dp)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))
                                ) {
                                    // Fill
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                                            .background(MaterialTheme.colorScheme.onBackground)
                                    )
                                    // Percentage label centered inside
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        color = if (progress > 0.5f)
                                            MaterialTheme.colorScheme.background
                                        else
                                            MaterialTheme.colorScheme.onBackground,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = currentFontFamily,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    } // end Column inside AnimatedVisibility
                } // end AnimatedVisibility — carousel + read button + progress all fade in together
            } else {
                // ── List view (and search results) ────────────────────────────
                val density = LocalDensity.current
                val filterKey = "$selectedFilter-$searchQuery"
                val listViewState = remember(filterKey) { androidx.compose.foundation.lazy.LazyListState(0, 0) }
                var initialStaggerDone by remember(filterKey) { mutableStateOf(false) }
                LaunchedEffect(filterKey) {
                    delay(450)
                    initialStaggerDone = true
                }

                LazyColumn(
                    state = listViewState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp, bottom = 120.dp, top = 4.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(bookItems, key = { _, item -> "$filterKey-${item.name}" }) { index, item ->
                        val totalChapters = BIBLE_BOOKS[item.name] ?: 1
                        val readCount = getReadCount(item.name)
                        val progress = readCount / totalChapters.toFloat()

                        // Staggered entrance plays every time filter changes for top 8 cards; offscreen/scrolled cards show immediately
                        val shouldShowInstantly = initialStaggerDone || index >= 8
                        var visible by remember(filterKey, item.name, shouldShowInstantly) { mutableStateOf(shouldShowInstantly) }
                        LaunchedEffect(filterKey, item.name, shouldShowInstantly) {
                            if (!visible) {
                                delay((index.coerceAtMost(8) * 50).toLong())
                                visible = true
                            }
                        }
                        val animAlpha by animateFloatAsState(
                            targetValue = if (visible) 1f else 0f,
                            animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            label = "cardFade"
                        )
                        val animOffsetY by animateFloatAsState(
                            targetValue = if (visible) 0f else 24f,
                            animationSpec = tween(durationMillis = 300, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                            label = "cardSlide"
                        )

                        Surface(
                            onClick = { focusManager.clearFocus(); onBookSelected(item.name, 1) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.07f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    alpha = animAlpha
                                    translationY = with(density) { animOffsetY.dp.toPx() }
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.name,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = currentFontFamily,
                                            maxLines = 2,
                                            lineHeight = 20.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = item.section,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                            fontSize = 12.sp,
                                            fontFamily = currentFontFamily
                                        )
                                    }
                                    Text(
                                        text = "$totalChapters Chapters",
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = currentFontFamily
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Reading Progress",
                                            color = MaterialTheme.colorScheme.onBackground,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = currentFontFamily
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "$readCount/$totalChapters",
                                                color = MaterialTheme.colorScheme.onBackground,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = currentFontFamily
                                            )
                                            Text(
                                                text = "|",
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                                                fontSize = 11.sp,
                                                fontFamily = currentFontFamily
                                            )
                                            Text(
                                                text = "${(progress * 100).toInt()}%",
                                                color = MaterialTheme.colorScheme.onBackground,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFamily = currentFontFamily
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(9.dp)
                                            .clip(RoundedCornerShape(50.dp))
                                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                                .background(MaterialTheme.colorScheme.onBackground)
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
                .weight(1f)
                .verticalScroll(rememberScrollState())
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
            
            val builtInFontOptions = listOf("Serif", "Sans-Serif", "Monospace", "Cursive")
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

            // Defined themes for selection: 0 = Dark, 1 = Light, 2 = Paper
            val themes = listOf(
                Triple("Dark", Color(0xFF131313), Color(0xFFE0E0E0)),
                Triple("Light", Color(0xFFEEECED), Color.Black),
                Triple("Paper", Color(0xFFFEF9F3), Color(0xFF5B4636))
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

    // Sync all state whenever initialHex changes (e.g. swatch selected externally)
    LaunchedEffect(initialHex) {
        try {
            val parsed = "#$initialHex".toColorInt()
            val hsvOut = FloatArray(3)
            android.graphics.Color.colorToHSV(parsed, hsvOut)
            if (hsvOut[1] > 0f) hue = hsvOut[0]
            saturation = hsvOut[1]
            value = hsvOut[2]
            hexInput = initialHex
            hexError = false
        } catch (_: Exception) { }
    }

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
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {

        // ── Top row: hex input | color circle | add (was ✕) | ✓ ──────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Hex input — fixed width for exactly 6 hex chars + # prefix
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
                    .width(90.dp)
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

            // Color preview circle — no button, just preview
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(currentColor, CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )

            Spacer(modifier = Modifier.weight(1f))

            // + Add to custom colors (replaces ✕)
            Surface(
                onClick = { onColorAdded(currentColor) },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_plus_lucide),
                        contentDescription = "Add color",
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

        // ── 2D saturation/value picker ────────────────────────────────────────
        val pickerHeight = 110.dp

        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(pickerHeight)
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
                        value = (1f - offset.y / size.height).coerceIn(0f, 1f)
                        syncHex()
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change: androidx.compose.ui.input.pointer.PointerInputChange, _: androidx.compose.ui.geometry.Offset ->
                        saturation = (change.position.x / size.width).coerceIn(0f, 1f)
                        value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
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

        Spacer(modifier = Modifier.height(10.dp))

        // ── Horizontal hue slider below the 2D box ────────────────────────────
        val sliderHeight = 16.dp

        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(sliderHeight)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        hue = (offset.x / size.width * 360f).coerceIn(0f, 360f)
                        syncHex()
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change: androidx.compose.ui.input.pointer.PointerInputChange, _: androidx.compose.ui.geometry.Offset ->
                        hue = (change.position.x / size.width * 360f).coerceIn(0f, 360f)
                        syncHex()
                    }
                }
        ) {
            // Rainbow track
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            (0..12).map { hsvToColor(it * 30f, 1f, 1f) }
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
            )
            // Thumb — circle matching the color pin style, centered on track
            val thumbX = maxWidth * (hue / 360f)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = thumbX - 8.dp)
                    .size(16.dp)
                    .border(2.dp, Color.White, CircleShape)
                    .background(hsvToColor(hue, 1f, 1f), CircleShape)
            )
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
