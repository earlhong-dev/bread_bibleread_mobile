package com.bibleread.bread.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.TransformOrigin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import com.bibleread.bread.R
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import com.bibleread.bread.data.BibleDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation

@OptIn(ExperimentalTextApi::class)
fun getInterFont(weightValue: Int): FontFamily {
    val safeWeight = weightValue.coerceIn(100, 900)
    return FontFamily(
        Font(
            resId = R.font.inter,
            weight = FontWeight(safeWeight),
            variationSettings = FontVariation.Settings(
                FontVariation.weight(safeWeight)
            )
        )
    )
}

// No auto-dismiss — stays until user taps Enter (once DB is ready)

private val ALLOWED_BOOKS = listOf(
    "Mga Awit", "Awit", "Psalms",
    "Mga Kawikaan", "Kawikaan", "Proverbs",
    "Mga Taga Roma", "Mga Taga-Roma", "Roma", "Romans",
    "Mga Taga Filipos", "Mga Taga-Filipos", "Filipos", "Philippians",
    "Santiago", "James",
    "Mateo", "Matthew",
    "Juan", "John",
    "1 Mga Taga Corinto", "1 Mga Taga-Corinto", "1 Corinto", "1 Corinthians",
    "Mga Taga Efeso", "Mga Taga-Efeso", "Efeso", "Ephesians",
    "Mga Taga Colosas", "Mga Taga-Colosas", "Colosas", "Colossians",
    "Mga Hebreo", "Hebreo", "Hebrews",
    "1 Pedro", "1 Peter"
)

class HillShape(private val curveHeight: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val curvePx = with(density) { curveHeight.toPx() }

        val path = Path().apply {
            moveTo(0f, curvePx)
            quadraticBezierTo(
                size.width / 2f, -curvePx,
                size.width, curvePx
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

private val RANDOM_DIALOGUES = listOf(
    "Jesus Loves You",
    "Keep the fire up!",
    "Kaya mo yan!",
    "Kamusta ka?",
    "PRAISEEEEEEEEE!",
    "Have a blessed day!",
    "Magpapatuloy tayo!",
    "Huwag ka mangamba",
    "Pray tayo",
    "Kalma, S'ya na bahala sa'yo",
    "Streak tayo!",
    "Nice one kapatid!"
)

private const val PREFS_NAME = "splash_prefs"
private const val KEY_LAST_OPEN_MS = "last_open_ms"
private const val KEY_MORNING_GREETED_DATE = "morning_greeted_date"
private const val KEY_STREAK_COUNT = "streak_count"
private const val KEY_STREAK_LAST_DATE = "streak_last_date"

fun pickBubbleDialogue(context: android.content.Context): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    val now = System.currentTimeMillis()
    val lastOpenMs = prefs.getLong(KEY_LAST_OPEN_MS, 0L)

    // Save current time as last open
    prefs.edit().putLong(KEY_LAST_OPEN_MS, now).apply()

    // Check "Welcome back kapatid!" — 3+ days since last open
    val threeDaysMs = 3L * 24 * 60 * 60 * 1000
    if (lastOpenMs > 0L && (now - lastOpenMs) >= threeDaysMs) {
        return "Welcome back kapatid!"
    }

    // Check "God's Morning!" — first open of the day during morning hours (before noon)
    val calendar = java.util.Calendar.getInstance()
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val todayDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        .format(java.util.Date(now))
    val morningGreetedDate = prefs.getString(KEY_MORNING_GREETED_DATE, "")
    if (hour in 0..11 && morningGreetedDate != todayDateStr) {
        prefs.edit().putString(KEY_MORNING_GREETED_DATE, todayDateStr).apply()
        return "God's Morning!"
    }

    // Otherwise pick randomly from pool
    return RANDOM_DIALOGUES.random()
}

fun getOrUpdateStreak(context: android.content.Context): Int {
    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).apply {
        timeZone = java.util.TimeZone.getDefault()
    }

    val todayCalendar = java.util.Calendar.getInstance()
    todayCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
    todayCalendar.set(java.util.Calendar.MINUTE, 0)
    todayCalendar.set(java.util.Calendar.SECOND, 0)
    todayCalendar.set(java.util.Calendar.MILLISECOND, 0)

    val todayDateStr = sdf.format(todayCalendar.time)
    val lastDateStr = prefs.getString(KEY_STREAK_LAST_DATE, null)
    var streak = prefs.getInt(KEY_STREAK_COUNT, 0)

    if (lastDateStr == null) {
        // First time launch
        streak = 1
        prefs.edit()
            .putInt(KEY_STREAK_COUNT, streak)
            .putString(KEY_STREAK_LAST_DATE, todayDateStr)
            .apply()
    } else if (todayDateStr == lastDateStr) {
        // Already opened today -> do nothing
    } else {
        val lastDate = try { sdf.parse(lastDateStr) } catch (e: Exception) { null }
        if (lastDate != null) {
            val lastCalendar = java.util.Calendar.getInstance()
            lastCalendar.time = lastDate
            lastCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            lastCalendar.set(java.util.Calendar.MINUTE, 0)
            lastCalendar.set(java.util.Calendar.SECOND, 0)
            lastCalendar.set(java.util.Calendar.MILLISECOND, 0)

            // Add 1 day to last open date to check if today is consecutive
            lastCalendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            val expectedNextDateStr = sdf.format(lastCalendar.time)

            if (todayDateStr == expectedNextDateStr) {
                // Consecutive day open
                streak += 1
            } else {
                // Missed 1 or more days -> reset to 1
                streak = 1
            }
        } else {
            streak = 1
        }

        prefs.edit()
            .putInt(KEY_STREAK_COUNT, streak)
            .putString(KEY_STREAK_LAST_DATE, todayDateStr)
            .apply()
    }

    return streak
}

fun getSpiritualStatus(streak: Int): String {
    return when {
        streak in 0..2 -> "First Steps"
        streak in 3..6 -> "Seeking God"
        streak in 7..13 -> "Living the Word"
        streak in 14..29 -> "Spiritually Healthy"
        streak in 30..59 -> "Rooted in Christ"
        else -> "Christ-Centered Life"
    }
}

data class StatusColors(val text: Color, val bg: Color)

fun getSpiritualStatusColors(streak: Int): StatusColors {
    return when {
        streak in 0..2 -> StatusColors(
            text = Color(0xFF1F2937), // Dark Charcoal Grey
            bg = Color(0xFFD1D5DB)   // Cool Grey
        )
        streak in 3..6 -> StatusColors(
            text = Color(0xFF1E3A8A), // Dark Navy Blue
            bg = Color(0xFF60A5FA)   // Vibrant Medium Blue
        )
        streak in 7..13 -> StatusColors(
            text = Color(0xFF7F1D1D), // Dark Crimson Red
            bg = Color(0xFFF87171)   // Vibrant Coral Red
        )
        streak in 14..29 -> StatusColors(
            text = Color(0xFF7C2D12), // Dark Rust Orange
            bg = Color(0xFFFB923C)   // Vibrant Warm Orange
        )
        streak in 30..59 -> StatusColors(
            text = Color(0xFF064E3B), // Dark Forest Green
            bg = Color(0xFF4ADE80)   // Vibrant Leaf Green
        )
        else -> StatusColors(
            text = Color(0xFF4C1D95), // Dark Royal Violet
            bg = Color(0xFFC084FC)   // Vibrant Violet
        )
    }
}


@Composable
fun SplashScreen(
    isDbReady: Boolean,
    onEnter: () -> Unit,
    onOpenVerse: ((book: String, chapter: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Make top system status bar icons dark/black for the yellow background & ensure transparent system bars
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        onDispose {
            // BreadTheme's SideEffect will reassert the correct value on next recomposition
        }
    }

    var verseText by remember { mutableStateOf("") }
    var verseRef by remember { mutableStateOf("") }
    var targetBook by remember { mutableStateOf<String?>(null) }
    var targetChapter by remember { mutableStateOf<Int?>(null) }

    // Pick bubble dialogue based on conditions (morning, welcome back, or random)
    val bubbleDialogue = remember { pickBubbleDialogue(context) }

    // Calculate/update daily open streak count
    val streakCount = remember { getOrUpdateStreak(context) }

    // ── Numeric Font Weight Adjusters (100..900) ─────────────────────────────
    // 100 = Thin, 300 = Light, 400 = Normal, 500 = Medium, 600 = SemiBold, 700 = Bold, 800 = ExtraBold, 900 = Black
    var verseTextFontWeightValue by remember { mutableIntStateOf(700) } // Bible verse text weight (e.g. 600)
    var verseRefFontWeightValue  by remember { mutableIntStateOf(850) } // Book reference weight (e.g. 700)

    val verseAlpha by animateFloatAsState(
        targetValue = if (verseText.isNotEmpty()) 1f else 0f,
        animationSpec = tween(400),
        label = "verseAlpha"
    )

    val breadAnim = remember { Animatable(0f) }
    val verseAnim = remember { Animatable(0f) }
    val buttonAnim = remember { Animatable(0f) }
    val bubbleAnim = remember { Animatable(0f) }
    val streakAnim = remember { Animatable(0f) }
    val exitAnim = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    val handleExit = { action: () -> Unit ->
        coroutineScope.launch {
            exitAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing)
            )
            action()
        }
    }

    LaunchedEffect(Unit) {
        // 1. Bread appears first: comes from bottom & eases in smoothly
        breadAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing)
        )

        // 2. Speech bubble pops up right after the bread image finishes its enter transition
        launch {
            bubbleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.58f,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }

        // 3. More delay, then Daily Verse (fade in) & Read Bible button (slide up) enter IN SYNC
        delay(750)
        launch {
            verseAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }
        buttonAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )

        // 4. Streak indicator & status fade/pop in ONLY AFTER Read Bible button is in place
        streakAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val db = BibleDatabase.getInstance(context)
                val count = db.verseDao().getVerseCountFromBooks(ALLOWED_BOOKS)
                if (count > 0) {
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).apply {
                        timeZone = java.util.TimeZone.getDefault()
                    }
                    val todayString = dateFormat.format(java.util.Date())
                    
                    // SHA-256 avalanche hash ensures consecutive days pick completely random books & verses
                    val md = java.security.MessageDigest.getInstance("SHA-256")
                    val digest = md.digest(todayString.toByteArray(Charsets.UTF_8))
                    val seed = java.nio.ByteBuffer.wrap(digest).long
                    val offset = (kotlin.math.abs(seed) % count).toInt()
                    
                    val selectedVerse = db.verseDao().getVerseFromBooksAtOffset(ALLOWED_BOOKS, offset)
                    if (selectedVerse != null) {
                        withContext(Dispatchers.Main) {
                            targetBook = selectedVerse.book
                            targetChapter = selectedVerse.chapter
                            verseText = "\"${selectedVerse.text.trim()}\""
                            verseRef = "${selectedVerse.book} ${selectedVerse.chapter}:${selectedVerse.verse}"
                        }
                    }
                } else {
                    val random = db.verseDao().getRandomVerse()
                    if (random != null) {
                        withContext(Dispatchers.Main) {
                            targetBook = random.book
                            targetChapter = random.chapter
                            verseText = "\"${random.text.trim()}\""
                            verseRef = "${random.book} ${random.chapter}:${random.verse}"
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SplashScreen", "Error fetching VOTD: ${e.message}")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exitAnim.value }
            .background(Color(0xFFF8D134)) // #f8d134
    ) {
        // ── Dark bottom panel ────────────────────────────────────────────────
        val bottomInteractionSource = remember { MutableInteractionSource() }
        val isHovered by bottomInteractionSource.collectIsHoveredAsState()
        val isPressed by bottomInteractionSource.collectIsPressedAsState()

        val panelColor = when {
            !isDbReady -> Color(0xFF1A1A1A)
            isPressed -> Color(0xFF2C2C2C)
            isHovered -> Color(0xFF1A1A1A)
            else -> Color(0xFF000000)
        }

        // "Read Bible" button container + Streak indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .graphicsLayer {
                    translationY = (1f - buttonAnim.value) * 350f
                }
        ) {
            // Streak indicator above Read Bible button (appears only after button transition finishess)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, bottom = 10.dp)
                    .graphicsLayer {
                        alpha = streakAnim.value
                    }
            ) {
                // Left side: flame + count + "Streak"
                Icon(
                    painter = painterResource(id = R.drawable.ic_flame),
                    contentDescription = "Streak Flame",
                    tint = Color(0xFFFF6B00),
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(3.dp))

                Text(
                    text = "$streakCount",
                    color = Color(0xFF1A1A1A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = getInterFont(800)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "Streak",
                    color = Color(0xFF1A1A1A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = getInterFont(700)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Vertical separator between Streak and Spiritual Status
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(Color(0xFF1A1A1A).copy(alpha = 0.25f))
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Spiritual status pill — color-coded container with solid text
                val statusColors = getSpiritualStatusColors(streakCount)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(statusColors.bg, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getSpiritualStatus(streakCount),
                        color = statusColors.text,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = getInterFont(700)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(panelColor)
                    .hoverable(bottomInteractionSource, enabled = isDbReady)
                    .pointerHoverIcon(if (isDbReady) PointerIcon.Hand else PointerIcon.Default)
                    .clickable(
                        interactionSource = bottomInteractionSource,
                        indication = null,
                        enabled = isDbReady,
                        onClick = { handleExit { onEnter() } }
                    )
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                // Book icon + Vertical line separator + Read Bible text (Centered, slightly offset left)
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = (-12).dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_book_open),
                        contentDescription = null,
                        tint = Color(0xFFF8D134),
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(18.dp))

                    // Vertical line separator
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .height(26.dp)
                            .background(Color.White.copy(alpha = 0.35f))
                    )

                    Spacer(modifier = Modifier.width(18.dp))

                    Text(
                        text = "Read Bible",
                        color = Color(0xFFFFFFFF),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = getInterFont(700),
                        letterSpacing = 0.5.sp
                    )
                }

                // Chevron right icon aligned near the right edge of the button
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = Color(0xFFF8D134),
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterEnd)
                )
            }
        }

        // ── Main content ────────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 32.dp)
        ) {
            Spacer(modifier = Modifier.height(70.dp))

            // Speech bubble — pops up right next to the bread character
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .offset(y = 4.dp)
                    .graphicsLayer {
                        scaleX = bubbleAnim.value
                        scaleY = bubbleAnim.value
                        alpha = bubbleAnim.value.coerceIn(0f, 1f)
                        transformOrigin = TransformOrigin(0.5f, 1.0f)
                    }
                    .shadow(6.dp, RoundedCornerShape(24.dp))
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = bubbleDialogue,
                    color = Color.Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontFamily = getInterFont(700)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Bread character — comes from bottom & eases in smooth ───────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = (1f - breadAnim.value) * 220f
                        alpha = breadAnim.value
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Image(
                    painter = painterResource(id = R.drawable.reminderbread),
                    contentDescription = "Bread Character",
                    modifier = Modifier.size(245.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bible verse — fades in with a little delay
            val verseInteractionSource = remember { MutableInteractionSource() }
            val isVerseHovered by verseInteractionSource.collectIsHoveredAsState()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer {
                        this.alpha = verseAlpha * verseAnim.value
                        this.scaleX = if (isVerseHovered && targetBook != null) 1.03f else 1.0f
                        this.scaleY = if (isVerseHovered && targetBook != null) 1.03f else 1.0f
                    }
                    .hoverable(verseInteractionSource, enabled = isDbReady)
                    .pointerHoverIcon(if (isDbReady) PointerIcon.Hand else PointerIcon.Default)
                    .clickable(
                        interactionSource = verseInteractionSource,
                        indication = null,
                        enabled = isDbReady
                    ) {
                        val b = targetBook
                        val c = targetChapter
                        handleExit {
                            if (b != null && c != null && onOpenVerse != null) {
                                onOpenVerse(b, c)
                            } else {
                                onEnter()
                            }
                        }
                    }
            ) {
                // Daily Bread label
                Text(
                    text = "Daily Bread",
                    color = Color(0xFF8B7A00),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = getInterFont(700),
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Book Reference + Line Separator (dynamically matched to text width)
                Column(
                    modifier = Modifier.width(IntrinsicSize.Max),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = verseRef,
                        color = Color(0xFF1A1A1A),
                        fontSize = 18.sp,
                        fontWeight = FontWeight(900),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF1A1A1A).copy(alpha = 0.2f))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Daily Verse
                Text(
                    text = verseText,
                    color = Color(0xFF1A1A1A),
                    fontSize = 16.sp,
                    fontWeight = FontWeight(verseTextFontWeightValue),
                    fontFamily = getInterFont(verseTextFontWeightValue),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
