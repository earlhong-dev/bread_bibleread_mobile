package com.bibleread.bread.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    "Isaias", "Isaiah",
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

@Composable
fun SplashScreen(
    isDbReady: Boolean,
    onEnter: () -> Unit,
    onOpenVerse: ((book: String, chapter: Int) -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Make top system status bar icons dark/black for the yellow background
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            val originalLightStatus = insetsController.isAppearanceLightStatusBars
            insetsController.isAppearanceLightStatusBars = true

            onDispose {
                insetsController.isAppearanceLightStatusBars = originalLightStatus
            }
        } else {
            onDispose { }
        }
    }

    var verseText by remember { mutableStateOf("") }
    var verseRef by remember { mutableStateOf("") }
    var targetBook by remember { mutableStateOf<String?>(null) }
    var targetChapter by remember { mutableStateOf<Int?>(null) }

    // ── Numeric Font Weight Adjusters (100..900) ─────────────────────────────
    // 100 = Thin, 300 = Light, 400 = Normal, 500 = Medium, 600 = SemiBold, 700 = Bold, 800 = ExtraBold, 900 = Black
    var verseTextFontWeightValue by remember { mutableIntStateOf(700) } // Bible verse text weight (e.g. 600)
    var verseRefFontWeightValue  by remember { mutableIntStateOf(850) } // Book reference weight (e.g. 700)

    val verseAlpha by animateFloatAsState(
        targetValue = if (verseText.isNotEmpty()) 1f else 0f,
        animationSpec = tween(400),
        label = "verseAlpha"
    )

    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "splashAlpha"
    )

    LaunchedEffect(Unit) {
        visible = true
        withContext(Dispatchers.IO) {
            try {
                val db = BibleDatabase.getInstance(context)
                val count = db.verseDao().getVerseCountFromBooks(ALLOWED_BOOKS)
                if (count > 0) {
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).apply {
                        timeZone = java.util.TimeZone.getDefault()
                    }
                    val todayString = dateFormat.format(java.util.Date())
                    val dateHash = kotlin.math.abs(todayString.hashCode())
                    val offset = dateHash % count
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
            .background(Color(0xFFF8D134)) // #f8d134
    ) {
        // ── Dark bottom panel ────────────────────────────────────────────────
        // Tall enough to always extend off-screen at the bottom
        val bottomInteractionSource = remember { MutableInteractionSource() }
        val isHovered by bottomInteractionSource.collectIsHoveredAsState()
        val isPressed by bottomInteractionSource.collectIsPressedAsState()

        val panelColor = when {
            !isDbReady -> Color(0xFF2C2C2C)
            isPressed -> Color(0xFF454545)
            isHovered -> Color(0xFF383838)
            else -> Color(0xFF2C2C2C)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.BottomCenter)
                .clip(HillShape(curveHeight = 40.dp))
                .background(panelColor)
                .hoverable(bottomInteractionSource, enabled = isDbReady)
                .pointerHoverIcon(if (isDbReady) PointerIcon.Hand else PointerIcon.Default)
                .clickable(
                    interactionSource = bottomInteractionSource,
                    indication = null,
                    enabled = isDbReady,
                    onClick = onEnter
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "Read the Bible!",
                color = Color(0xFFF8D134),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = getInterFont(700),
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(top = 28.dp)
            )
        }

        // ── Main content (fades in) ──────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 32.dp)
                .graphicsLayer { this.alpha = alpha }
        ) {
            Spacer(modifier = Modifier.height(100.dp))

            // Speech bubble — offset right of the bread character
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 12.dp)
                    .shadow(6.dp, RoundedCornerShape(24.dp))
                    .background(Color(0xFFE8E8E8), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Nice One Bro!",
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Bread character ──────────────────────────────────────────────
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Image(
                    painter = painterResource(id = R.drawable.reminderbread),
                    contentDescription = "Bread Character",
                    modifier = Modifier.size(245.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bible verse
            val verseInteractionSource = remember { MutableInteractionSource() }
            val isVerseHovered by verseInteractionSource.collectIsHoveredAsState()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer {
                        this.alpha = verseAlpha
                        this.scaleX = if (isVerseHovered && targetBook != null) 1.03f else 1.0f
                        this.scaleY = if (isVerseHovered && targetBook != null) 1.03f else 1.0f
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .hoverable(verseInteractionSource, enabled = isDbReady)
                    .pointerHoverIcon(if (isDbReady) PointerIcon.Hand else PointerIcon.Default)
                    .clickable(
                        interactionSource = verseInteractionSource,
                        indication = null,
                        enabled = isDbReady
                    ) {
                        val b = targetBook
                        val c = targetChapter
                        if (b != null && c != null && onOpenVerse != null) {
                            onOpenVerse(b, c)
                        } else {
                            onEnter()
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 5.dp)
            ) {
                Text(
                    text = verseText,
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight(verseTextFontWeightValue),
                    fontFamily = getInterFont(verseTextFontWeightValue),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = verseRef,
                    color = Color.Black,
                    fontSize = 25.sp,
                    fontWeight = FontWeight(verseRefFontWeightValue),
                    fontFamily = getInterFont(verseRefFontWeightValue)
                )
            }
        }
    }
}
